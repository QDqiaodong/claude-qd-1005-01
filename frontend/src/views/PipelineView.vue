<script setup>
import { computed, inject, onMounted, ref, watch } from 'vue'
import api from '../api'

const refreshHealth = inject('refreshHealth', () => {})

const rows = ref([])
const segments = ref([])
const capacities = ref([])
const error = ref('')
const tip = ref('')
const loading = ref(false)
const segmentFilter = ref('')

const lineTypes = ['电力', '通信', '给水', '排水', '热力', '燃气']
const pressureLevels = ['高压', '中压', '低压']
const statuses = ['在用', '检修']
// 无压管线：表单上不允许选压力级制（与后端 PRESSURE_FORBIDDEN 规则一致）
const noPressureTypes = ['通信', '排水']
// 仅可选中/低压的类型：热力不得高压（与后端 HEAT_NO_HIGH 规则一致）
const noHighPressureTypes = ['热力']
// 各类型允许的最大管径（mm），与后端 DIAMETER_OVERSIZE 规则一致，仅用于前端即时提示
const maxDiameter = { 电力: 1600, 通信: 400, 给水: 1200, 排水: 2000, 热力: 1000, 燃气: 800 }

const segMap = computed(() => {
  const m = {}
  segments.value.forEach(s => { m[s.id] = s.segCode })
  return m
})
// 舱段用量按 segmentId 索引（列表汇总的唯一数据源来自后端 /pipelines/capacities）
const capMap = computed(() => {
  const m = {}
  capacities.value.forEach(c => { m[c.segmentId] = c })
  return m
})
// 列表汇总：筛选为“全部舱段”时展示全部舱段用量，否则只展示所选舱段
const shownCapacities = computed(() =>
  segmentFilter.value ? capacities.value.filter(c => c.segmentId === segmentFilter.value) : capacities.value)

/**
 * 单条管线占用舱位：与后端「类型 × 压力级制」矩阵完全一致，
 * 仅用于列表即时展示；权威值仍以 /capacities 的 items[].slots 为准。
 */
function slotOf(lineType, pressureLevel) {
  const table = {
    电力: { 高压: 2, 中压: 1, 低压: 1 },
    通信: { '-': 1 },
    给水: { 高压: 3, 中压: 2, 低压: 1 },
    排水: { '-': 1 },
    热力: { 中压: 2, 低压: 1 },
    燃气: { 高压: 4, 中压: 3, 低压: 2 }
  }
  const key = noPressureTypes.includes(lineType) ? '-' : pressureLevel
  const s = table[lineType]?.[key]
  return s == null ? 1 : s
}
const slotByPipeline = computed(() => {
  const m = {}
  capacities.value.forEach(c => c.items.forEach(i => { m[i.id] = i.slots }))
  return m
})
function slotsOfRow(row) {
  return slotByPipeline.value[row.id] ?? slotOf(row.lineType, row.pressureLevel)
}

async function load() {
  loading.value = true; error.value = ''
  try {
    // 管线列表与容量汇总一起刷新：筛选结果、用量、已存管线三者始终一致
    const [data, caps] = await Promise.all([
      api.listPipelines(segmentFilter.value || ''),
      api.pipelineCapacities()
    ])
    rows.value = data || []
    capacities.value = caps || []
  } catch (e) { error.value = e.message } finally { loading.value = false }
}

// ---------------- 新建表单：容量面板 + 实时兼容校核 ----------------
const dialog = ref(false)
const saving = ref(false)
const form = ref(blank())
const formCapacity = ref(null)   // 当前所选舱段用量
const formCheck = ref(null)      // 预检结果（容量/规格/隔离/封控/停用）
let checkSeq = 0

function blank() {
  return { segmentId: '', lineType: '电力', diameterMm: '', pressureLevel: '中压', status: '在用' }
}

function openCreate() {
  form.value = blank()
  if (segments.value.length) form.value.segmentId = segments.value[0].id
  formCapacity.value = null
  formCheck.value = null
  dialog.value = true; tip.value = ''; error.value = ''
}

// 类型切换：无压管线自动清空压力级制；热力若停在高压则回落到中压，避免触发 HEAT_NO_HIGH
watch(() => form.value.lineType, (t) => {
  if (noPressureTypes.includes(t)) {
    form.value.pressureLevel = ''
  } else if (noHighPressureTypes.includes(t) && form.value.pressureLevel === '高压') {
    form.value.pressureLevel = '中压'
  }
})

// 表单任意影响校核的字段变化 → 拉取舱段用量 + 预检（同一后端规则链）
watch(
  () => [form.value.segmentId, form.value.lineType, form.value.diameterMm, form.value.pressureLevel, form.value.status],
  () => { if (dialog.value) refreshFormCheck() }
)

async function refreshFormCheck() {
  const sid = form.value.segmentId
  if (!sid) { formCapacity.value = null; formCheck.value = null; return }
  const seq = ++checkSeq
  try {
    const payload = {
      segmentId: sid,
      lineType: form.value.lineType,
      diameterMm: form.value.diameterMm === '' ? null : Number(form.value.diameterMm),
      pressureLevel: form.value.pressureLevel || null,
      status: form.value.status
    }
    const [cap, check] = await Promise.all([
      api.pipelineCapacity(sid),
      api.checkPipeline(payload)
    ])
    // 只接受最后一次变更的结果，避免快速切换时旧结果覆盖新结果
    if (seq !== checkSeq) return
    formCapacity.value = cap
    formCheck.value = check
  } catch (e) {
    if (seq === checkSeq) { formCapacity.value = null; formCheck.value = null }
  }
}

const usagePercent = computed(() => {
  const c = formCapacity.value
  if (!c || !c.capacitySlots) return 0
  return Math.min(100, Math.round((c.usedSlots / c.capacitySlots) * 100))
})
const usageColor = computed(() => {
  const c = formCapacity.value
  if (!c) return '#90a4ae'
  const ratio = c.usedSlots / c.capacitySlots
  return ratio >= 1 ? '#e53935' : ratio >= 0.8 ? '#fb8c00' : '#43a047'
})
const checkAlert = computed(() => formCheck.value && !formCheck.value.compatible ? formCheck.value : null)

async function save() {
  saving.value = true; error.value = ''
  try {
    const d = {
      ...form.value,
      diameterMm: form.value.diameterMm === '' ? null : Number(form.value.diameterMm),
      pressureLevel: form.value.pressureLevel || null
    }
    const created = await api.createPipeline(d)
    const occupied = slotOf(created.lineType, created.pressureLevel)
    tip.value = `管线（${created.lineType}）已入廊，本次占用 ${occupied} 舱位`
    dialog.value = false
    await Promise.all([load(), refreshHealth()])
  } catch (e) {
    // 后端落库是最后一道闸：超容量/不兼容会带出被触发的规则编号
    error.value = e.message
    await load()
  } finally { saving.value = false }
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
        <span class="panel-desc">舱段容量按「类型 × 压力级制」计舱位；电力/燃气防爆隔离分舱；封控、停用舱段禁止入廊</span>
      </div>

      <div v-if="error" class="err">{{ error }}</div>
      <div v-if="tip" class="ok-tip">{{ tip }}</div>

      <div class="stat-row">
        <div class="stat"><div class="k">管线数</div><div class="v">{{ rows.length }}</div></div>
        <div class="stat"><div class="k">在用</div><div class="v">{{ rows.filter(r => r.status === '在用').length }}</div></div>
        <div class="stat"><div class="k">检修</div><div class="v">{{ rows.filter(r => r.status === '检修').length }}</div></div>
        <div class="stat" v-for="c in shownCapacities" :key="c.segmentId"
             :class="{ 'stat-full': c.remainSlots <= 0, 'stat-warn': c.remainSlots > 0 && c.remainSlots / c.capacitySlots <= 0.2 }">
          <div class="k">{{ c.segCode }} 舱容（已用/总）</div>
          <div class="v">{{ c.usedSlots }} / {{ c.capacitySlots }}</div>
          <div class="stat-sub">剩余 {{ c.remainSlots }} 舱位
            <el-tag v-if="c.unresolvedRiskCount > 0" type="danger" size="small" effect="plain" style="margin-left:4px">封控·{{ c.unresolvedRiskCount }}</el-tag>
            <el-tag v-else-if="c.status === '停用'" type="info" size="small" effect="plain" style="margin-left:4px">停用</el-tag>
            <el-tag v-if="c.hasPower && c.hasGas" type="danger" size="small" effect="plain" style="margin-left:4px">隔离异常</el-tag>
          </div>
        </div>
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
        <el-table-column prop="lineType" label="类型" width="90" />
        <el-table-column prop="diameterMm" label="管径(mm)" width="100" />
        <el-table-column label="压力级制" width="100">
          <template #default="{ row }">{{ row.pressureLevel || '无压' }}</template>
        </el-table-column>
        <el-table-column label="本次占用(舱位)" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="slotsOfRow(row) >= 3 ? 'warning' : 'info'">{{ slotsOfRow(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="舱段用量" min-width="170">
          <template #default="{ row }">
            <template v-if="capMap[row.segmentId]">
              <span style="font-weight:700">{{ capMap[row.segmentId].usedSlots }}/{{ capMap[row.segmentId].capacitySlots }}</span>
              <span class="muted">（剩 {{ capMap[row.segmentId].remainSlots }}）</span>
              <el-progress :percentage="Math.round(capMap[row.segmentId].usedSlots / capMap[row.segmentId].capacitySlots * 100)"
                           :stroke-width="6" :show-text="false"
                           :color="capMap[row.segmentId].remainSlots <= 0 ? '#e53935' : '#546e7a'" />
            </template>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === '在用' ? 'success' : 'warning'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialog" title="新建入廊管线" width="600px">
      <!-- 舱段用量面板：已用 / 剩余 / 本次占用，全部来自后端实时计算 -->
      <div v-if="formCapacity" class="cap-panel">
        <div class="cap-head">
          <span class="panel-title" style="font-size:14px">{{ formCapacity.segCode }} 舱段用量</span>
          <span class="muted">舱容 {{ formCapacity.capacitySlots }} 舱位</span>
          <el-tag v-if="formCapacity.unresolvedRiskCount > 0" type="danger" size="small">封控中·{{ formCapacity.unresolvedRiskCount }} 条未解除</el-tag>
          <el-tag v-else-if="formCapacity.status === '停用'" type="info" size="small">舱段停用</el-tag>
          <el-tag v-else-if="formCapacity.status === '维护'" type="warning" size="small">维护中</el-tag>
          <el-tag v-else type="success" size="small">运营中</el-tag>
        </div>
        <el-progress :percentage="usagePercent" :color="usageColor" :stroke-width="14"
                     :format="() => `${formCapacity.usedSlots} / ${formCapacity.capacitySlots}`" />
        <div class="cap-grid">
          <div><span class="muted">已用舱位</span><b>{{ formCapacity.usedSlots }}</b></div>
          <div><span class="muted">剩余舱位</span>
            <b :style="{ color: formCapacity.remainSlots <= 0 ? '#e53935' : '#37474f' }">{{ formCapacity.remainSlots }}</b>
          </div>
          <div><span class="muted">本次占用</span>
            <b :style="{ color: checkAlert ? '#a32b20' : '#37474f' }">{{ formCheck?.requestSlots ?? '—' }}</b>
          </div>
        </div>
        <div class="cap-flags">
          <el-tag v-if="formCapacity.hasPower" type="warning" size="small" effect="plain">舱内已有电力（禁燃气）</el-tag>
          <el-tag v-if="formCapacity.hasGas" type="warning" size="small" effect="plain">舱内已有燃气（禁电力）</el-tag>
          <el-tag v-if="formCapacity.lockReason" type="danger" size="small" effect="plain">
            封控原因：{{ formCapacity.lockReason }}
          </el-tag>
        </div>
      </div>

      <!-- 兼容校核结论：明确展示被触发的规则编号与名称 -->
      <div v-if="checkAlert" class="cap-reject">
        <div>不可入廊：<b>{{ checkAlert.ruleName }}</b>
          <span class="rule-code">[{{ checkAlert.ruleCode }}]</span>
        </div>
        <div class="cap-reject-msg">{{ checkAlert.message }}</div>
      </div>
      <div v-else-if="formCheck?.compatible" class="cap-ok">
        {{ formCheck.message }}
      </div>

      <el-form :model="form" label-width="110px" class="dialog-form" style="margin-top:12px">
        <el-form-item label="所属舱段">
          <el-select v-model="form.segmentId" style="width: 100%" placeholder="选择舱段">
            <el-option v-for="s in segments" :key="s.id"
                       :label="`${s.segCode}${s.segName ? ' · ' + s.segName : ''}`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="管线类型">
          <el-select v-model="form.lineType" style="width: 100%">
            <el-option v-for="t in lineTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="管径(mm)">
          <el-input v-model="form.diameterMm" type="number" step="1"
                    :placeholder="`≤ ${maxDiameter[form.lineType]}`" />
        </el-form-item>
        <el-form-item label="压力级制">
          <el-select v-model="form.pressureLevel" style="width: 100%"
                     :clearable="true" :disabled="noPressureTypes.includes(form.lineType)"
                     :placeholder="noPressureTypes.includes(form.lineType) ? '无压管线（不分级）' : '必选'">
            <el-option v-for="p in pressureLevels" :key="p" :label="p" :value="p"
                       :disabled="p === '高压' && noHighPressureTypes.includes(form.lineType)" />
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
        <el-button type="primary" :loading="saving" :disabled="checkAlert != null" @click="save">入廊</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.stat-sub { font-size: 12px; color: var(--primary-soft); margin-top: 3px; display: flex; align-items: center; flex-wrap: wrap; gap: 2px; }
.stat-full { background: #fdecea !important; border-color: #f5c6c2 !important; }
.stat-full .v { color: #a32b20 !important; }
.stat-warn { background: #fff4e5 !important; border-color: #ffd9a8 !important; }
.cap-panel {
  background: var(--primary-wash); border: 1px solid #d7dee1; border-radius: 10px;
  padding: 12px 14px; margin-bottom: 10px;
}
.cap-head { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; flex-wrap: wrap; }
.cap-grid { display: flex; gap: 28px; margin-top: 10px; }
.cap-grid > div { display: flex; flex-direction: column; gap: 2px; }
.cap-grid b { font-size: 18px; }
.cap-flags { display: flex; gap: 6px; flex-wrap: wrap; margin-top: 8px; }
.cap-reject {
  background: #fdecea; border: 1px solid #f5c6c2; color: #a32b20;
  border-radius: 8px; padding: 8px 12px; font-size: 13px;
}
.cap-reject .rule-code { font-family: ui-monospace, monospace; font-size: 12px; opacity: 0.85; }
.cap-reject-msg { margin-top: 3px; font-size: 12px; }
.cap-ok {
  background: #e8f5e9; border: 1px solid #bfe3c2; color: #2e7d32;
  border-radius: 8px; padding: 8px 12px; font-size: 13px;
}
</style>
