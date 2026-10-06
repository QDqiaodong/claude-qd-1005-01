<script setup>
import { computed, ref, watch } from 'vue'
import api from '../api'

// ------------------------------------------------------------------
// 风险登记对话框（共用）：
//  - RiskView 直接登记：自选舱段 → 发现对象类型 → 发现对象；
//  - PatrolView / FacilityView 传 preset 预填并锁定发现对象（从巡检点/设备一键登记）。
// 后端会再次校验：发现对象必须存在且属于关联舱段。
// ------------------------------------------------------------------
const props = defineProps({
  modelValue: { type: Boolean, default: false },
  // { segmentId, sourceType, sourceId, sourceLabel }
  preset: { type: Object, default: null }
})
const emit = defineEmits(['update:modelValue', 'saved'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const severities = ['一般', '较大', '重大']
const sourceTypes = ['巡检点', '附属设备']

const segments = ref([])
const sources = ref([])
const saving = ref(false)
const error = ref('')
const form = ref(blank())

function blank() {
  return { segmentId: '', sourceType: '巡检点', sourceId: '', severity: '一般', description: '', foundAt: '' }
}

// preset 模式下舱段与发现对象锁定，不可改
const locked = computed(() => !!props.preset)

async function loadSources() {
  sources.value = []
  if (locked.value || !form.value.segmentId) return
  try {
    if (form.value.sourceType === '巡检点') {
      const list = await api.listPatrolPoints(form.value.segmentId)
      sources.value = (list || []).map(p => ({ id: p.id, label: `${p.pointCode} · ${p.location || '无位置描述'}` }))
    } else {
      const list = await api.listFacilities(form.value.segmentId)
      sources.value = (list || []).map(f => ({ id: f.id, label: `${f.facilityType}（${f.status}）#${f.id}` }))
    }
  } catch (e) { error.value = e.message }
}

watch(() => props.modelValue, async (open) => {
  if (!open) return
  error.value = ''
  form.value = blank()
  if (props.preset) {
    form.value.segmentId = props.preset.segmentId
    form.value.sourceType = props.preset.sourceType
    form.value.sourceId = props.preset.sourceId
    return
  }
  try {
    segments.value = await api.listSegments()
    if (segments.value.length) form.value.segmentId = segments.value[0].id
    await loadSources()
  } catch (e) { error.value = e.message }
})

async function onSegmentOrTypeChange() {
  form.value.sourceId = ''
  await loadSources()
}

async function save() {
  error.value = ''
  if (!form.value.segmentId) { error.value = '请选择关联舱段'; return }
  if (!form.value.sourceId) { error.value = '请选择发现对象'; return }
  if (!form.value.description || !form.value.description.trim()) { error.value = '请填写风险描述'; return }
  if (!form.value.foundAt) { error.value = '请选择发现时间'; return }
  saving.value = true
  try {
    await api.createRiskEvent({ ...form.value, description: form.value.description.trim() })
    emit('saved')
    visible.value = false
  } catch (e) { error.value = e.message } finally { saving.value = false }
}
</script>

<template>
  <el-dialog v-model="visible" title="登记风险事件" width="560px">
    <div v-if="error" class="err">{{ error }}</div>
    <el-form :model="form" label-width="110px" class="dialog-form">
      <template v-if="locked">
        <el-form-item label="发现对象" class="full">
          <el-input :model-value="`${preset.sourceType} · ${preset.sourceLabel}`" disabled />
        </el-form-item>
      </template>
      <template v-else>
        <el-form-item label="关联舱段">
          <el-select v-model="form.segmentId" style="width: 100%" placeholder="选择舱段" @change="onSegmentOrTypeChange">
            <el-option v-for="s in segments" :key="s.id" :label="`${s.segCode} ${s.segName || ''}`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="对象类型">
          <el-radio-group v-model="form.sourceType" @change="onSegmentOrTypeChange">
            <el-radio v-for="t in sourceTypes" :key="t" :value="t">{{ t }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="发现对象" class="full">
          <el-select v-model="form.sourceId" style="width: 100%" placeholder="选择巡检点 / 设备">
            <el-option v-for="o in sources" :key="o.id" :label="o.label" :value="o.id" />
          </el-select>
        </el-form-item>
      </template>
      <el-form-item label="严重等级">
        <el-select v-model="form.severity" style="width: 100%">
          <el-option v-for="s in severities" :key="s" :label="s" :value="s" />
        </el-select>
      </el-form-item>
      <el-form-item label="发现时间">
        <el-date-picker
          v-model="form.foundAt"
          type="datetime"
          style="width: 100%"
          placeholder="选择发现时间"
          value-format="YYYY-MM-DDTHH:mm:ss"
        />
      </el-form-item>
      <el-form-item label="风险描述" class="full">
        <el-input v-model="form.description" type="textarea" :rows="3" placeholder="异常情况、位置、影响范围等" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">登记</el-button>
    </template>
  </el-dialog>
</template>
