<script setup>
import { computed, onMounted, provide, ref, watch } from 'vue'
import router from './router'
import api from './api'

import SegmentView from './views/SegmentView.vue'
import PipelineView from './views/PipelineView.vue'
import PatrolView from './views/PatrolView.vue'
import FacilityView from './views/FacilityView.vue'
import RiskView from './views/RiskView.vue'

// ------------------------------------------------------------------
// 导航形态：顶部「半圆仪表」展示全廊体健康度（SVG 半圆 + 填充 + 指针），
// 其下为五个模块入口卡片。点击卡片经 <component :is> 切换业务视图。
// ------------------------------------------------------------------

const viewMap = {
  '/segment': SegmentView,
  '/pipeline': PipelineView,
  '/patrol': PatrolView,
  '/facility': FacilityView,
  '/risk': RiskView
}

const tabs = [
  { path: '/segment', label: '廊体舱段', sub: '断面 · 长度 · 运营/维护/停用' },
  { path: '/pipeline', label: '入廊管线', sub: '电力/通信/给水/排水/热力/燃气' },
  { path: '/patrol', label: '巡检点', sub: '点位 · 位置 · 最近巡检' },
  { path: '/facility', label: '附属设备', sub: '风机/排水泵/照明/监控' },
  { path: '/risk', label: '风险事件', sub: '登记 · 处置 · 解除封控' }
]

const currentPath = ref(router.currentRoute.value.path)
const health = ref({ score: 100, totalSegments: 0, operating: 0, maintaining: 0, stopped: 0, totalFacilities: 0, faultyFacilities: 0 })
const counts = ref({ segment: 0, pipeline: 0, patrol: 0, facility: 0, risk: 0 })

const currentView = computed(() => viewMap[currentPath.value] || SegmentView)

// ---------- 半圆仪表几何 ----------
const cx = 100, cy = 100, r = 80
function polar(deg, radius) {
  const a = (deg * Math.PI) / 180
  return { x: cx + radius * Math.cos(a), y: cy - radius * Math.sin(a) }
}
function arc(startDeg, endDeg, radius) {
  const s = polar(startDeg, radius)
  const e = polar(endDeg, radius)
  const large = Math.abs(startDeg - endDeg) > 180 ? 1 : 0
  return `M ${s.x} ${s.y} A ${radius} ${radius} 0 ${large} 1 ${e.x} ${e.y}`
}
const scoreDeg = computed(() => 180 - (health.value.score || 0) * 1.8)
const trackPath = computed(() => arc(180, 0, r))
const valuePath = computed(() => arc(180, scoreDeg.value, r))
const needle = computed(() => polar(scoreDeg.value, r - 14))
const needleColor = computed(() => {
  const s = health.value.score || 0
  return s >= 80 ? '#43a047' : s >= 50 ? '#fb8c00' : '#e53935'
})

async function loadHealth() {
  try {
    health.value = await api.dashboardHealth()
  } catch (e) { /* 看板失败不影响主流程 */ }
}

async function loadCounts() {
  try {
    const [s, p, pt, f, r] = await Promise.all([
      api.listSegments(), api.listPipelines(), api.listPatrolPoints(), api.listFacilities(), api.listRiskEvents()
    ])
    counts.value = { segment: s.length, pipeline: p.length, patrol: pt.length, facility: f.length, risk: r.length }
  } catch (e) { /* 计数失败不影响主流程 */ }
}

async function refresh() {
  await Promise.all([loadHealth(), loadCounts()])
}
provide('refreshHealth', refresh)

function go(path) {
  router.push(path)
  currentPath.value = path
}

watch(() => router.currentRoute.value.path, (p) => { currentPath.value = p })
onMounted(async () => {
  currentPath.value = router.currentRoute.value.path
  await refresh()
})
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <div class="brand">
        <span class="brand-mark">廊</span>
        <div class="brand-text">
          <div class="brand-title">城市综合管廊 · 廊体舱段与入廊管理</div>
          <div class="brand-sub">地下城市管廊 · 多舱体 · 管线 / 巡检 / 设备</div>
        </div>
      </div>

      <div class="gauge-wrap">
        <svg class="gauge-svg" viewBox="0 0 200 120" width="170" height="102">
          <path :d="trackPath" fill="none" stroke="#cfd8dc" stroke-width="14" stroke-linecap="round" />
          <path :d="valuePath" fill="none" :stroke="needleColor" stroke-width="14" stroke-linecap="round" />
          <line :x1="cx" :y1="cy" :x2="needle.x" :y2="needle.y" :stroke="needleColor" stroke-width="3" />
          <circle :cx="cx" :cy="cy" r="6" :fill="needleColor" />
        </svg>
        <div class="gauge-readout">
          <div class="gauge-score" :style="{ color: needleColor }">{{ health.score }}</div>
          <div class="gauge-label">全廊体健康度</div>
        </div>
      </div>
    </header>

    <nav class="module-nav">
      <div
        v-for="t in tabs"
        :key="t.path"
        class="module-card"
        :class="{ on: currentPath === t.path }"
        @click="go(t.path)"
      >
        <span class="m-count">{{ counts[t.path.slice(1)] }}</span>
        <div class="m-label">{{ t.label }}</div>
        <div class="m-sub">{{ t.sub }}</div>
      </div>
    </nav>

    <main class="content">
      <component :is="currentView" />
    </main>
  </div>
</template>
