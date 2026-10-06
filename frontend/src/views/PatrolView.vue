<script setup>
import { computed, inject, onMounted, ref } from 'vue'
import api from '../api'
import RiskRegisterDialog from '../components/RiskRegisterDialog.vue'

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

const dialog = ref(false)
const saving = ref(false)
const form = ref(blank())
function blank() {
  return { segmentId: '', pointCode: '', location: '', lastPatrolAt: '' }
}

// 从巡检点登记风险：预填并锁定发现对象
const riskOpen = ref(false)
const riskPreset = ref(null)

async function load() {
  loading.value = true; error.value = ''
  try {
    const data = await api.listPatrolPoints(segmentFilter.value || '')
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
    await api.createPatrolPoint(form.value)
    tip.value = `巡检点 ${form.value.pointCode} 已建`
    dialog.value = false
    await Promise.all([load(), refreshHealth()])
  } catch (e) { error.value = e.message } finally { saving.value = false }
}

async function register(row) {
  error.value = ''; tip.value = ''
  try {
    await api.registerPatrol(row.id)
    tip.value = `${row.pointCode} 巡检已登记`
    await Promise.all([load(), refreshHealth()])
  } catch (e) { error.value = e.message }
}

function openRisk(row) {
  riskPreset.value = {
    segmentId: row.segmentId,
    sourceType: '巡检点',
    sourceId: row.id,
    sourceLabel: `${row.pointCode} · ${row.location || '无位置描述'}`
  }
  riskOpen.value = true
  error.value = ''
}
async function onRiskSaved() {
  tip.value = '风险事件已登记，可在「风险事件」模块处置'
  await refreshHealth()
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
        <span class="panel-title">巡检点</span>
        <span class="panel-desc">登记巡检会刷新「最近巡检时间」为当前时间</span>
      </div>

      <div v-if="error" class="err">{{ error }}</div>
      <div v-if="tip" class="ok-tip">{{ tip }}</div>

      <div class="stat-row">
        <div class="stat"><div class="k">巡检点</div><div class="v">{{ rows.length }}</div></div>
        <div class="stat"><div class="k">已巡检</div><div class="v">{{ rows.filter(r => r.lastPatrolAt).length }}</div></div>
        <div class="stat"><div class="k">待巡检</div><div class="v">{{ rows.filter(r => !r.lastPatrolAt).length }}</div></div>
      </div>

      <div class="toolbar">
        <el-select v-model="segmentFilter" placeholder="全部舱段" clearable style="width: 160px" @change="load">
          <el-option v-for="s in segments" :key="s.id" :label="s.segCode" :value="s.id" />
        </el-select>
        <span class="spacer"></span>
        <el-button type="primary" @click="openCreate">新建巡检点</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column label="所属舱段" width="130">
          <template #default="{ row }">{{ segMap[row.segmentId] || row.segmentId }}</template>
        </el-table-column>
        <el-table-column prop="pointCode" label="点位编码" width="120" />
        <el-table-column prop="location" label="位置" min-width="160" />
        <el-table-column prop="lastPatrolAt" label="最近巡检" min-width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="register(row)">巡检登记</el-button>
            <el-button size="small" type="danger" plain @click="openRisk(row)">登记风险</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialog" title="新建巡检点" width="560px">
      <el-form :model="form" label-width="110px" class="dialog-form">
        <el-form-item label="所属舱段">
          <el-select v-model="form.segmentId" style="width: 100%" placeholder="选择舱段">
            <el-option v-for="s in segments" :key="s.id" :label="s.segCode" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="点位编码"><el-input v-model="form.pointCode" placeholder="如 P-06" /></el-form-item>
        <el-form-item label="位置" class="full"><el-input v-model="form.location" placeholder="如 桩号 K3+100" /></el-form-item>
        <el-form-item label="初始巡检时间" class="full">
          <el-input v-model="form.lastPatrolAt" placeholder="留空表示尚未巡检" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">建档</el-button>
      </template>
    </el-dialog>

    <risk-register-dialog v-model="riskOpen" :preset="riskPreset" @saved="onRiskSaved" />
  </div>
</template>
