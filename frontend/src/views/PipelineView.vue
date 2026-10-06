<script setup>
import { computed, inject, onMounted, ref } from 'vue'
import api from '../api'

const refreshHealth = inject('refreshHealth', () => {})

const rows = ref([])
const segments = ref([])
const segMap = computed(() => {
  const m = {}
  segments.value.forEach(s => { m[s.id] = s.segCode })
  return m
})
const error = ref('')
const tip = ref('')
const loading = ref(false)
const segmentFilter = ref('')

const lineTypes = ['电力', '通信', '给水', '排水', '热力', '燃气']
const pressureLevels = ['高压', '中压', '低压']
const statuses = ['在用', '检修']

const dialog = ref(false)
const saving = ref(false)
const form = ref(blank())
function blank() {
  return { segmentId: '', lineType: '电力', diameterMm: '', pressureLevel: '中压', status: '在用' }
}

async function load() {
  loading.value = true; error.value = ''
  try {
    const data = await api.listPipelines(segmentFilter.value || '')
    rows.value = data || []
  } catch (e) { error.value = e.message } finally { loading.value = false }
}

function openCreate() {
  form.value = blank()
  if (segments.value.length) form.value.segmentId = segments.value[0].id
  dialog.value = true; tip.value = ''; error.value = ''
}
async function save() {
  saving.value = true; error.value = ''
  try {
    const d = { ...form.value, diameterMm: form.value.diameterMm ? Number(form.value.diameterMm) : null }
    await api.createPipeline(d)
    tip.value = `管线（${d.lineType}）已入廊`
    dialog.value = false
    await Promise.all([load(), refreshHealth()])
  } catch (e) { error.value = e.message } finally { saving.value = false }
}

onMounted(async () => {
  try { segments.value = await api.listSegments() } catch (e) {}
  await load()
})
</script>

<template>
  <div>
    <div class="panel">
      <div class="panel-head">
        <span class="panel-title">入廊管线</span>
        <span class="panel-desc">同舱段已敷设「电力」时禁止同舱敷设「燃气」（防爆隔离）</span>
      </div>

      <div v-if="error" class="err">{{ error }}</div>
      <div v-if="tip" class="ok-tip">{{ tip }}</div>

      <div class="stat-row">
        <div class="stat"><div class="k">管线数</div><div class="v">{{ rows.length }}</div></div>
        <div class="stat"><div class="k">在用</div><div class="v">{{ rows.filter(r => r.status === '在用').length }}</div></div>
        <div class="stat"><div class="k">检修</div><div class="v">{{ rows.filter(r => r.status === '检修').length }}</div></div>
      </div>

      <div class="toolbar">
        <el-select v-model="segmentFilter" placeholder="全部舱段" clearable style="width: 160px" @change="load">
          <el-option v-for="s in segments" :key="s.id" :label="s.segCode" :value="s.id" />
        </el-select>
        <span class="spacer"></span>
        <el-button type="primary" @click="openCreate">新建管线</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column label="所属舱段" width="130">
          <template #default="{ row }">{{ segMap[row.segmentId] || row.segmentId }}</template>
        </el-table-column>
        <el-table-column prop="lineType" label="类型" width="100" />
        <el-table-column prop="diameterMm" label="管径(mm)" width="110" />
        <el-table-column prop="pressureLevel" label="压力级制" width="110" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '在用' ? 'success' : 'warning'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialog" title="新建入廊管线" width="560px">
      <el-form :model="form" label-width="110px" class="dialog-form">
        <el-form-item label="所属舱段">
          <el-select v-model="form.segmentId" style="width: 100%" placeholder="选择舱段">
            <el-option v-for="s in segments" :key="s.id" :label="s.segCode" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="管线类型">
          <el-select v-model="form.lineType" style="width: 100%">
            <el-option v-for="t in lineTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="管径(mm)"><el-input v-model="form.diameterMm" type="number" step="1" /></el-form-item>
        <el-form-item label="压力级制">
          <el-select v-model="form.pressureLevel" style="width: 100%" clearable>
            <el-option v-for="p in pressureLevels" :key="p" :label="p" :value="p" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" class="full">
          <el-radio-group v-model="form.status">
            <el-radio v-for="s in statuses" :key="s" :value="s">{{ s }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">入廊</el-button>
      </template>
    </el-dialog>
  </div>
</template>
