import request from '@/utils/request'

// 建单前：把自由描述解析成结构化槽位（不写库，由前端回填后随建单一并提交）
export function aiIntake(data) {
  return request({
    url: '/it/ai/ticket/intake',
    method: 'post',
    data: data,
    timeout: 12000
  })
}

// 处理中：检索历史同类工单（仅 IT/管理员有权限；返回真实工单号与真实处理说明）
export function aiSimilar(data) {
  return request({
    url: '/it/ai/kb/similar',
    method: 'post',
    data: data
  })
}

// 某工单的 AI 调用审计（详情页展示"AI 已辅助"的凭证）
export function aiLogs(ticketId) {
  return request({
    url: '/it/ai/log/' + ticketId,
    method: 'get'
  })
}

// 当前用户今日剩余额度（-1 表示不限）
export function aiQuota() {
  return request({
    url: '/it/ai/quota',
    method: 'get'
  })
}
