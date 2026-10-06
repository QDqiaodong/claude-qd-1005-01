import axios from 'axios'

const http = axios.create({ baseURL: '/api', timeout: 20000 })

// 后端统一返回 {ok, message, data}；HTTP 400 的业务异常也要把中文提示带出来
http.interceptors.response.use(
  (response) => response,
  (error) => {
    const message = error?.response?.data?.message || error?.message || '请求失败'
    return Promise.reject(new Error(message))
  }
)

async function call(promise) {
  const response = await promise
  const body = response.data
  if (!body || body.ok === false) {
    throw new Error((body && body.message) || '请求失败')
  }
  return body.data
}

const params = (o) => ({ params: o })

export default {
  // ---------------- 看板健康度 ----------------
  dashboardHealth: () => call(http.get('/dashboard/health')),

  // ---------------- 廊体舱段 ----------------
  listSegments: (status) => call(http.get('/segments', params(status ? { status } : {}))),
  getSegment: (id) => call(http.get(`/segments/${id}`)),
  getSegmentWithChildren: (id) => call(http.get(`/segments/${id}/with-children`)),
  createSegment: (d) => call(http.post('/segments', d)),
  changeSegmentStatus: (id, status) => call(http.post(`/segments/${id}/status`, { status })),

  // ---------------- 入廊管线 ----------------
  listPipelines: (segmentId) => call(http.get('/pipelines', params(segmentId ? { segmentId } : {}))),
  // 全部舱段用量（舱容/已用/剩余/逐条占用）
  pipelineCapacities: () => call(http.get('/pipelines/capacities')),
  // 指定舱段用量（新建表单容量面板）
  pipelineCapacity: (segmentId) => call(http.get('/pipelines/capacity', params({ segmentId }))),
  // 提交前容量与规格兼容预检（不落库）
  checkPipeline: (d) => call(http.post('/pipelines/check', d)),
  createPipeline: (d) => call(http.post('/pipelines', d)),

  // ---------------- 巡检点 ----------------
  listPatrolPoints: (segmentId) => call(http.get('/patrol-points', params(segmentId ? { segmentId } : {}))),
  createPatrolPoint: (d) => call(http.post('/patrol-points', d)),
  registerPatrol: (id) => call(http.post(`/patrol-points/${id}/register`)),

  // ---------------- 附属设备 ----------------
  listFacilities: (segmentId) => call(http.get('/facilities', params(segmentId ? { segmentId } : {}))),
  createFacility: (d) => call(http.post('/facilities', d)),
  faultFacility: (id) => call(http.post(`/facilities/${id}/fault`)),
  restoreFacility: (id) => call(http.post(`/facilities/${id}/restore`)),

  // ---------------- 巡检异常 / 风险事件 ----------------
  listRiskEvents: (segmentId, status) => call(http.get('/risk-events', params({
    ...(segmentId ? { segmentId } : {}),
    ...(status ? { status } : {})
  }))),
  getRiskEvent: (id) => call(http.get(`/risk-events/${id}`)),
  createRiskEvent: (d) => call(http.post('/risk-events', d)),
  advanceRiskEvent: (id, handleNote) => call(http.post(`/risk-events/${id}/advance`, { handleNote })),
  resolveRiskEvent: (id, d) => call(http.post(`/risk-events/${id}/resolve`, d))
}
