<script setup>
import { computed, inject, onMounted, ref } from 'vue'
import api from '../api'

const refreshHealth = inject('refreshHealth', () => {})

const rows = ref([])
const riskEvents = ref([])
const error = ref('')
const tip = ref('')
const loading = ref(false)
const statusFilter = ref('')
const shapes = ['圆形', '矩形', '马蹄形']
const statuses = ['运营', '维护', '停用']

const dialog = ref(false)
const saving = ref(false)
const form = ref(blank())
function blank() {
  return { segCode: '', segName: '', shape: '矩形', lengthM: '', capacitySlots: 8, status: '运营' }
}

const detail = ref(null)
const detailOpen = ref(false)

// 未解除风险事件按舱段分组：用于「封控」列与恢复运营的前端提示（后端仍会强校验）
const lockMap = computed(() => {
  const m = {}
  riskEvents.value.filter(e => e.status !== '已解除').forEach(e => {
    (m[e.segmentId] = m[e.segmentId] || []).push(e)
  })
  return m
})
function lockReasonOf(row) {
  const list = lockMap.value[row.id]
  if (!list || !list.length) return ''
  const latest = list[0] // 列表按 id 倒序，第一条即最近未解除事件
  return `【${latest.severity}】${latest.description}`
}

async function load() {
  loading.value = true; error.value = ''
  try {
    const [segs, risks] = await Promise.all([
      api.listSegments(statusFilter.value || ''),
      api.listRiskEvents()
    ])
    rows.value = segs || []
    riskEvents.value = risks || []
  } catch (e) { error.value = e.message } finally { loading.value = false }
}

function openCreate() { form.value = blank(); dialog.value = true; tip.value = ''; error.value = '' }
async function save() {
  saving.value = true; error.value = ''
  try {
    const d = { ...form.value, lengthM: Number(form.value.lengthM), capacitySlots: Number(form.value.capacitySlots) }
    await api.createSegment(d)
    tip.value = `舱段 ${d.segCode} 已建档（舱容 ${d.capacitySlots} 舱位）`
    dialog.value = false
    await Promise.all([load(), refreshHealth()])
  } catch (e) { error.value = e.message } finally { saving.value = false }
}

// 状态变更：维护/停用 → 运营 时后端校验封控（未解除风险事件），被拦会带出封控原因
async function changeStatus(row, target) {
  error.value = ''; tip.value = ''
  try {
    await api.changeSegmentStatus(row.id, target)
    tip.value = `${row.segCode} 已切换为「${target}」`
    await Promise.all([load(), refreshHealth()])
  } catch (e) { error.value = e.message }
}

async function openDetail(row) {
  error.value = ''
  try {
    detail.value = await api.getSegmentWithChildren(row.id)
    detailOpen.value = true
  } catch (e) { error.value = e.message }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="panel">
      <div class="panel-head">
        <span class="panel-title">廊体舱段</span>
        <span class="panel-desc">新建舱段长度须 &gt; 0、舱容为正整数；存在未解除风险事件的舱段被封控，不能恢复运营</span>
      </div>

      <div v-if="error" class="err">{{ error }}</div>
      <div v-if="tip" class="ok-tip">{{ tip }}</div>

      <div class="stat-row">
        <div class="stat"><div class="k">舱段数</div><div class="v">{{ rows.length }}</div></div>
        <div class="stat"><div class="k">运营</div><div class="v">{{ rows.filter(r => r.status === '运营').length }}</div></div>
        <div class="stat"><div class="k">维护</div><div class="v">{{ rows.filter(r => r.status === '维护').length }}</div></div>
        <div class="stat"><div class="k">停用</div><div class="v">{{ rows.filter(r => r.status === '停用').length }}</div></div>
      </div>

      <div class="toolbar">
        <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 140px" @change="load">
          <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
        </el-select>
        <span class="spacer"></span>
        <el-button type="primary" @click="openCreate">新建舱段</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="segCode" label="舱段编码" width="120" />
        <el-table-column prop="segName" label="名称" min-width="150" />
        <el-table-column prop="shape" label="断面" width="100" />
        <el-table-column prop="lengthM" label="长度(m)" width="110" />
        <el-table-column prop="capacitySlots" label="舱容(舱位)" width="110" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '运营' ? 'success' : row.status === '维护' ? 'warning' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="封控" min-width="200">
          <template #default="{ row }">
            <template v-if="lockMap[row.id]">
              <el-tag type="danger" size="small">封控中·{{ lockMap[row.id].length }}</el-tag>
              <span class="lock-reason"> {{ lockReasonOf(row) }}</span>
            </template>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">子项</el-button>
            <el-button v-if="row.status !== '运营'" size="small" type="success" plain @click="changeStatus(row, '运营')">恢复运营</el-button>
            <el-button v-if="row.status !== '维护'" size="small" type="warning" plain @click="changeStatus(row, '维护')">转维护</el-button>
            <el-button v-if="row.status !== '停用'" size="small" plain @click="changeStatus(row, '停用')">停用</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialog" title="新建舱段" width="560px">
      <el-form :model="form" label-width="110px" class="dialog-form">
        <el-form-item label="舱段编码"><el-input v-model="form.segCode" placeholder="如 SEG-05" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.segName" /></el-form-item>
        <el-form-item label="断面形状">
          <el-select v-model="form.shape" style="width: 100%">
            <el-option v-for="s in shapes" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="长度(m)"><el-input v-model="form.lengthM" type="number" step="0.1" /></el-form-item>
        <el-form-item label="舱容(舱位)">
          <el-input v-model="form.capacitySlots" type="number" step="1" min="1" placeholder="容量舱位上限，正整数" />
        </el-form-item>
        <el-form-item label="状态" class="full">
          <el-radio-group v-model="form.status">
            <el-radio v-for="s in statuses" :key="s" :value="s">{{ s }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">建档</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailOpen" title="舱段子项（嵌套查询）" width="880px">
      <div v-if="detail">
        <div class="panel-desc" style="margin-bottom:8px">
          {{ detail.segCode }} · {{ detail.segName }} · {{ detail.shape }} · 长度 {{ detail.lengthM }}m · 舱容 {{ detail.capacitySlots }} 舱位 · {{ detail.status }}
        </div>
        <div class="panel-title" style="margin:10px 0 4px">入廊管线（{{ detail.pipelines.length }}）</div>
        <el-table :data="detail.pipelines" border size="small">
          <el-table-column prop="lineType" label="类型" />
          <el-table-column prop="diameterMm" label="管径(mm)" />
          <el-table-column prop="pressureLevel" label="压力级制" />
          <el-table-column prop="status" label="状态" />
        </el-table>
        <div class="panel-title" style="margin:10px 0 4px">巡检点（{{ detail.patrolPoints.length }}）</div>
        <el-table :data="detail.patrolPoints" border size="small">
          <el-table-column prop="pointCode" label="点位" />
          <el-table-column prop="location" label="位置" />
          <el-table-column prop="lastPatrolAt" label="最近巡检" />
        </el-table>
        <div class="panel-title" style="margin:10px 0 4px">附属设备（{{ detail.facilities.length }}）</div>
        <el-table :data="detail.facilities" border size="small">
          <el-table-column prop="facilityType" label="类型" />
          <el-table-column prop="status" label="状态" />
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>
