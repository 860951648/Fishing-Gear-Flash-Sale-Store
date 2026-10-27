import request from '@/utils/request'

/** 退款模块：对应后端 RefundController（§3.3 第 15、16 个接口） */

// POST /api/refunds 买家申请退款，返回 {refundNo}
export function applyRefund(data) {
  return request.post('/refunds', data)
}

// GET /api/refunds?page&size 我的退款列表
export function getRefunds(params) {
  return request.get('/refunds', { params })
}
