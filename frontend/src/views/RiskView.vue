<script setup>
import { inject, onMounted, ref } from 'vue'
import api from '../api'
import RiskRegisterDialog from '../components/RiskRegisterDialog.vue'

const refreshHealth = inject('refreshHealth', () => {})

const rows = ref([])
const segments = ref([])
const error = ref('')
const tip = ref('')
const loading = ref(false)
const segmentFilter = ref('')
const statusFilter = ref('')

const statuses = ['待处置', '处理中', '已解除']

// ---------- 登记 ----------
const registerOpen = ref(false)

// ---------- 推进 / 解除 ----------
const advanceOpen = ref(false)
const resolveOpen = ref(false)
const actionSaving = ref(false)
const current = ref(null)
const advanceNote = ref('')
const resolveForm = ref({ handleNote: '', resolvedBy: '' })

// ---------- 详情 ----------
const detail = ref(null)
const detailOpen = ref(false)

async function load() {
  loading.value = true; error.value = ''
  try {
    const data = await api.listRiskEvents(segmentFilter.value || '', statusFilter.value || '')
    rows.value = data || []
  } catch (e) { error.value = e.message } finally { loading.value = false }
}

function severityTag(s) {
  return s === '重大' ? 'danger' : s === '较大' ? 'warning' : 'info'
}
function statusTag(s) {
  return s === '已解除' ? 'success' : s === '处理中' ? 'warning' : 'danger'
}

function openAdvance(row) {
  current.value = row
  advanceNote.value = ''
  advanceOpen.value = true
  error.value = ''
}
async function doAdvance() {
  actionSaving.value = true; error.value = ''
  try {
    await api.advanceRiskEvent(current.value.id, advanceNote.value)
    tip.value = `事件 #${current.value.id} 已推进为处理中`
    advanceOpen.value = false
    await Promise.all([load(), refreshHealth()])
  } catch (e) { error.value = e.message; advanceOpen.value = false } finally { actionSaving.value = false }
}

function openResolve(row) {
  current.value = row
  resolveForm.value = { handleNote: '', resolvedBy: '' }
  resolveOpen.value = true
  error.value = ''
}
async function doResolve() {
  error.value = ''
  if (!resolveForm.value.handleNote || !resolveForm.value.handleNote.trim()) {
    error.value = '解除风险必须填写处置说明'
    return
  }
  if (!resolveForm.value.resolvedBy || !resolveForm.value.resolvedBy.trim()) {
    error.value = '解除风险必须填写解除人'
    return
  }
  actionSaving.value = true
  try {
    await api.resolveRiskEvent(current.value.id, {
      handleNote: resolveForm.value.handleNote.trim(),
      resolvedBy: resolveForm.value.resolvedBy.trim()
    })
    tip.value = `事件 #${current.value.id} 已解除（解除人：${resolveForm.value.resolvedBy.trim()}）`
    resolveOpen.value = false
    await Promise.all([load(), refreshHealth()])
  } catch (e) { error.value = e.message; resolveOpen.value = false } finally { actionSaving.value = false }
}

async function openDetail(row) {
  error.value = ''
  try {
    detail.value = await api.getRiskEvent(row.id)
    detailOpen.value = true
  } catch (e) { error.value = e.message }
}

async function onRegistered() {
  tip.value = '风险事件已登记，舱段进入封控'
  await Promise.all([load(), refreshHealth()])
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
        <span class="panel-title">巡检异常 / 风险事件</span>
        <span class="panel-desc">待处置 → 处理中 → 已解除；存在未解除事件的舱段被封控，不能恢复运营</span>
      </div>

      <div v-if="error" class="err">{{ error }}</div>
      <div v-if="tip" class="ok-tip">{{ tip }}</div>

      <div class="stat-row">
        <div class="stat"><div class="k">事件数</div><div class="v">{{ rows.length }}</div></div>
        <div class="stat"><div class="k">待处置</div><div class="v">{{ rows.filter(r => r.status === '待处置').length }}</div></div>
        <div class="stat"><div class="k">处理中</div><div class="v">{{ rows.filter(r => r.status === '处理中').length }}</div></div>
        <div class="stat"><div class="k">已解除</div><div class="v">{{ rows.filter(r => r.status === '已解除').length }}</div></div>
      </div>

      <div class="toolbar">
        <el-select v-model="segmentFilter" placeholder="全部舱段" clearable style="width: 160px" @change="load">
          <el-option v-for="s in segments" :key="s.id" :label="s.segCode" :value="s.id" />
        </el-select>
        <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 140px" @change="load">
          <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
        </el-select>
        <span class="spacer"></span>
        <el-button type="primary" @click="registerOpen = true">登记风险事件</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column label="舱段" width="100">
          <template #default="{ row }">{{ row.segCode }}</template>
        </el-table-column>
        <el-table-column label="发现对象" width="170">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.sourceType }}</el-tag>
            {{ row.sourceName || `#${row.sourceId}` }}
          </template>
        </el-table-column>
        <el-table-column label="严重等级" width="90">
          <template #default="{ row }">
            <el-tag :type="severityTag(row.severity)">{{ row.severity }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="风险描述" min-width="180" show-overflow-tooltip />
        <el-table-column prop="foundAt" label="发现时间" width="170" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="当前封控原因" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.lockReason" class="lock-reason">{{ row.lockReason }}</span>
            <span v-else class="muted">已解除</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">详情</el-button>
            <el-button size="small" type="warning" plain :disabled="row.status !== '待处置'" @click="openAdvance(row)">推进</el-button>
            <el-button size="small" type="success" plain :disabled="row.status === '已解除'" @click="openResolve(row)">解除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <risk-register-dialog v-model="registerOpen" @saved="onRegistered" />

    <el-dialog v-model="advanceOpen" title="推进为处理中" width="480px">
      <div class="panel-desc" style="margin-bottom: 10px">
        事件 #{{ current?.id }} · {{ current?.segCode }} · {{ current?.description }}
      </div>
      <el-form label-width="90px">
        <el-form-item label="处置说明">
          <el-input v-model="advanceNote" type="textarea" :rows="3" placeholder="选填：已采取的措施、责任人等" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="advanceOpen = false">取消</el-button>
        <el-button type="primary" :loading="actionSaving" @click="doAdvance">推进</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resolveOpen" title="解除风险" width="480px">
      <div class="panel-desc" style="margin-bottom: 10px">
        事件 #{{ current?.id }} · {{ current?.segCode }} · {{ current?.description }}
      </div>
      <el-form label-width="90px">
        <el-form-item label="处置说明" required>
          <el-input v-model="resolveForm.handleNote" type="textarea" :rows="3" placeholder="必填：处置过程与结论" />
        </el-form-item>
        <el-form-item label="解除人" required>
          <el-input v-model="resolveForm.resolvedBy" placeholder="必填：解除人姓名" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resolveOpen = false">取消</el-button>
        <el-button type="success" :loading="actionSaving" @click="doResolve">确认解除</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailOpen" title="风险事件详情" width="640px">
      <div v-if="detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="事件编号">#{{ detail.event.id }}</el-descriptions-item>
          <el-descriptions-item label="关联舱段">{{ detail.event.segCode }} · {{ detail.event.segName }}</el-descriptions-item>
          <el-descriptions-item label="严重等级">
            <el-tag :type="severityTag(detail.event.severity)" size="small">{{ detail.event.severity }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTag(detail.event.status)" size="small">{{ detail.event.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="发现时间">{{ detail.event.foundAt }}</el-descriptions-item>
          <el-descriptions-item label="登记时间">{{ detail.event.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="风险描述" :span="2">{{ detail.event.description }}</el-descriptions-item>
          <el-descriptions-item label="处置说明" :span="2">{{ detail.event.handleNote || '—' }}</el-descriptions-item>
          <el-descriptions-item label="解除人">{{ detail.event.resolvedBy || '—' }}</el-descriptions-item>
          <el-descriptions-item label="解除时间">{{ detail.event.resolvedAt || '—' }}</el-descriptions-item>
          <el-descriptions-item label="当前封控原因" :span="2">
            <span v-if="detail.event.lockReason" class="lock-reason">{{ detail.event.lockReason }}</span>
            <span v-else class="muted">已解除，不构成封控</span>
          </el-descriptions-item>
        </el-descriptions>

        <template v-if="detail.patrolPoint">
          <div class="panel-title" style="margin: 12px 0 6px">关联巡检点</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="点位编码">{{ detail.patrolPoint.pointCode }}</el-descriptions-item>
            <el-descriptions-item label="位置">{{ detail.patrolPoint.location || '—' }}</el-descriptions-item>
            <el-descriptions-item label="最近巡检" :span="2">{{ detail.patrolPoint.lastPatrolAt || '尚未巡检' }}</el-descriptions-item>
          </el-descriptions>
        </template>
        <template v-if="detail.facility">
          <div class="panel-title" style="margin: 12px 0 6px">关联附属设备</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="设备类型">{{ detail.facility.facilityType }}</el-descriptions-item>
            <el-descriptions-item label="设备状态">{{ detail.facility.status }}</el-descriptions-item>
          </el-descriptions>
        </template>
      </div>
    </el-dialog>
  </div>
</template>
