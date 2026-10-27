import request from '@/utils/request'

/** 订单模块：对应后端 OrderController（§3.3 第 10-12 个接口） */

// GET /api/orders?page&size&status
export function getOrders(params) {
  return request.get('/orders', { params })
}

// GET /api/orders/{orderNo} 订单详情（含明细）
export function getOrderDetail(orderNo) {
  return request.get('/orders/' + orderNo)
}

// POST /api/orders/{orderNo}/cancel 取消待支付订单
export function cancelOrder(orderNo) {
  return request.post('/orders/' + orderNo + '/cancel')
}
