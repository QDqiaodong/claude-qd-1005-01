<script setup>
import { computed, inject, onMounted, ref } from 'vue'
import api from '../api'

const refreshHealth = inject('refreshHealth', () => {})

const rows = ref([])
const segments = ref([])
const usages = ref([])
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

// ---- 容量与规格兼容规则（与后端 Dict 保持一致；后端落库前仍会强校验）----
const BASE_OCCUPANCY = { 电力: 2, 通信: 1, 给水: 2, 排水: 3, 热力: 3, 燃气: 3 }
const PRESSURE_EXTRA = { 高压: 2, 中压: 1, 低压: 0 }
const PRESSURE_REQUIRED_TYPES = ['给水', '热力', '燃气']
function occupancyOf(lineType, pressureLevel) {
  return (BASE_OCCUPANCY[lineType] ?? 1) + (pressureLevel ? (PRESSURE_EXTRA[pressureLevel] ?? 0) : 0)
}

const dialog = ref(false)
const saving = ref(false)
const form = ref(blank())
const segPipelines = ref([]) // 对话框当前所选舱段的已存管线（用于防爆隔离提示）
function blank() {
  return { segmentId: '', lineType: '电力', diameterMm: '', pressureLevel: '中压', status: '在用' }
}

// 列表汇总：用量汇总跟随舱段筛选，与筛选出的管线列表口径一致
const summaryUsages = computed(() => {
  if (!segmentFilter.value) return usages.value
  return usages.value.filter(u => u.segmentId === segmentFilter.value)
})
function pctOf(u) {
  return u.capacity > 0 ? Math.round((u.used * 100) / u.capacity) : 0
}
function progressStatus(u) {
  if (u.remaining <= 0) return 'exception'
  if (pctOf(u) >= 80) return 'warning'
  return ''
}

// ---- 新建表单：所选舱段用量与本次占用实时提示 ----
const curUsage = computed(() => usages.value.find(u => u.segmentId === form.value.segmentId) || null)
const occupancyPreview = computed(() => occupancyOf(form.value.lineType, form.value.pressureLevel))
const afterRemaining = computed(() => (curUsage.value ? curUsage.value.remaining - occupancyPreview.value : null))

// 规格兼容预检：与后端同序判定，触发即给出规则名并禁止提交
const compatError = computed(() => {
  const f = form.value
  if (!f.segmentId) return ''
  if (PRESSURE_REQUIRED_TYPES.includes(f.lineType) && !f.pressureLevel) {
    return `触发规则「压力级制适配」：${f.lineType}为压力管线，必须填写压力级制（高压 / 中压 / 低压）`
  }
  if (f.lineType === '排水' && f.pressureLevel === '高压') {
    return '触发规则「压力级制适配」：排水为重力流管线，不得使用高压'
  }
  const types = segPipelines.value.map(p => p.lineType)
  if (f.lineType === '燃气' && types.includes('电力')) {
    return '触发规则「防爆隔离」：同舱段已敷设电力管线，燃气须分舱敷设，禁止同舱敷设'
  }
  if (f.lineType === '电力' && types.includes('燃气')) {
    return '触发规则「防爆隔离」：同舱段已敷设燃气管线，电力须分舱敷设，禁止同舱敷设'
  }
  if (curUsage.value && occupancyPreview.value > curUsage.value.remaining) {
    const u = curUsage.value
    return `触发规则「舱段容量」：舱段 ${u.segCode} 剩余容量 ${u.remaining}，本次敷设需占用 ${occupancyPreview.value}（已用 ${u.used} / 总容量 ${u.capacity}），超容量禁止入廊`
  }
  return ''
})

async function load() {
  loading.value = true; error.value = ''
  try {
    const [pipes, usageRows] = await Promise.all([
      api.listPipelines(segmentFilter.value || ''),
      api.pipelineUsage()
    ])
    rows.value = pipes || []
    usages.value = usageRows || []
  } catch (e) { error.value = e.message } finally { loading.value = false }
}

async function loadSegPipelines() {
  if (!form.value.segmentId) { segPipelines.value = []; return }
  try { segPipelines.value = await api.listPipelines(form.value.segmentId) || [] } catch (e) { segPipelines.value = [] }
}

function openCreate() {
  form.value = blank()
  if (segments.value.length) form.value.segmentId = segments.value[0].id
  dialog.value = true; tip.value = ''; error.value = ''
  loadSegPipelines()
}
async function save() {
  if (compatError.value) { error.value = compatError.value; return }
  saving.value = true; error.value = ''
  try {
    const d = { ...form.value, diameterMm: form.value.diameterMm ? Number(form.value.diameterMm) : null }
    await api.createPipeline(d)
    tip.value = `管线（${d.lineType}）已入廊，本次占用 ${occupancyPreview.value}`
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
        <span class="panel-desc">容量校核：本次占用 = 类型基础占用 + 压力级制附加，不得超过舱段剩余容量；电力与燃气防爆隔离（分舱敷设）</span>
      </div>

      <div v-if="error" class="err">{{ error }}</div>
      <div v-if="tip" class="ok-tip">{{ tip }}</div>

      <div class="stat-row">
        <div class="stat"><div class="k">管线数</div><div class="v">{{ rows.length }}</div></div>
        <div class="stat"><div class="k">在用</div><div class="v">{{ rows.filter(r => r.status === '在用').length }}</div></div>
        <div class="stat"><div class="k">检修</div><div class="v">{{ rows.filter(r => r.status === '检修').length }}</div></div>
        <div class="stat"><div class="k">列表占用合计</div><div class="v">{{ rows.reduce((s, r) => s + (r.occupancy || 0), 0) }}</div></div>
      </div>

      <div class="toolbar">
        <el-select v-model="segmentFilter" placeholder="全部舱段" clearable style="width: 160px" @change="load">
          <el-option v-for="s in segments" :key="s.id" :label="s.segCode" :value="s.id" />
        </el-select>
        <span class="spacer"></span>
        <el-button type="primary" @click="openCreate">新建管线</el-button>
      </div>

      <div class="usage-strip">
        <div v-for="u in summaryUsages" :key="u.segmentId" class="usage-item">
          <span class="usage-label">{{ u.segCode }}</span>
          <el-progress class="usage-bar" :percentage="pctOf(u)" :status="progressStatus(u)" :stroke-width="10" />
          <span class="usage-num">已用 {{ u.used }}/{{ u.capacity }} · 剩余 {{ u.remaining }} · 管线 {{ u.pipelineCount }} 条</span>
        </div>
        <div v-if="!summaryUsages.length" class="muted">暂无舱段用量数据</div>
      </div>

      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column label="所属舱段" width="130">
          <template #default="{ row }">{{ segMap[row.segmentId] || row.segmentId }}</template>
        </el-table-column>
        <el-table-column prop="lineType" label="类型" width="100" />
        <el-table-column prop="diameterMm" label="管径(mm)" width="110" />
        <el-table-column prop="pressureLevel" label="压力级制" width="110" />
        <el-table-column prop="occupancy" label="占用容量" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '在用' ? 'success' : 'warning'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialog" title="新建入廊管线" width="560px">
      <div v-if="error" class="err">{{ error }}</div>
      <el-form :model="form" label-width="110px" class="dialog-form">
        <el-form-item label="所属舱段">
          <el-select v-model="form.segmentId" style="width: 100%" placeholder="选择舱段" @change="loadSegPipelines">
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

        <div v-if="curUsage" class="full capacity-panel">
          <div class="cap-cell"><div class="k">已用容量</div><div class="v">{{ curUsage.used }}</div></div>
          <div class="cap-cell"><div class="k">剩余容量</div><div class="v">{{ curUsage.remaining }}</div></div>
          <div class="cap-cell"><div class="k">本次占用</div><div class="v">{{ occupancyPreview }}</div></div>
          <div class="cap-cell">
            <div class="k">敷设后剩余</div>
            <div class="v" :class="{ 'cap-over': afterRemaining < 0 }">{{ afterRemaining }}</div>
          </div>
          <div class="cap-total">舱段 {{ curUsage.segCode }} 总容量 {{ curUsage.capacity }}；本次占用 = 类型基础 + 压力附加（{{ form.lineType }} {{ BASE_OCCUPANCY[form.lineType] }} + {{ form.pressureLevel || '未填' }} {{ form.pressureLevel ? PRESSURE_EXTRA[form.pressureLevel] : 0 }}）</div>
        </div>
        <div v-if="compatError" class="full err compat-err">{{ compatError }}</div>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="!!compatError || !form.segmentId" @click="save">入廊</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.usage-strip {
  border: 1px solid var(--line);
  border-radius: 9px;
  padding: 8px 14px;
  margin-bottom: 12px;
  background: #fafbfc;
}
.usage-item { display: flex; align-items: center; gap: 12px; padding: 4px 0; }
.usage-item + .usage-item { border-top: 1px dashed var(--line); }
.usage-label { font-weight: 800; color: var(--primary-deep); width: 70px; }
.usage-bar { flex: 1; }
.usage-num { font-size: 12px; color: var(--muted); white-space: nowrap; }

.capacity-panel {
  display: flex; flex-wrap: wrap; gap: 10px; align-items: center;
  border: 1px solid #d7dee1; border-radius: 9px;
  background: var(--primary-wash); padding: 10px 12px;
}
.cap-cell { min-width: 86px; }
.cap-cell .k { font-size: 12px; color: var(--primary-soft); }
.cap-cell .v { font-size: 19px; font-weight: 800; color: var(--primary-deep); }
.cap-cell .v.cap-over { color: #a32b20; }
.cap-total { flex-basis: 100%; font-size: 12px; color: var(--muted); }
.compat-err { margin-bottom: 0; }
</style>
