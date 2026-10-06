import { createRouter, createWebHashHistory } from 'vue-router'

// 注意：main.js 里没有 app.use(router)，路由实例由 App.vue 直接驱动，
// 所以这里全部用静态 import —— <component :is> 拿到的是组件对象本身。
import SegmentView from '../views/SegmentView.vue'
import PipelineView from '../views/PipelineView.vue'
import PatrolView from '../views/PatrolView.vue'
import FacilityView from '../views/FacilityView.vue'
import RiskView from '../views/RiskView.vue'

const routes = [
  { path: '/', redirect: '/segment' },
  { path: '/segment', name: 'segment', component: SegmentView },
  { path: '/pipeline', name: 'pipeline', component: PipelineView },
  { path: '/patrol', name: 'patrol', component: PatrolView },
  { path: '/facility', name: 'facility', component: FacilityView },
  { path: '/risk', name: 'risk', component: RiskView },
  { path: '/:pathMatch(.*)*', redirect: '/segment' }
]

export default createRouter({
  history: createWebHashHistory(),
  routes
})
