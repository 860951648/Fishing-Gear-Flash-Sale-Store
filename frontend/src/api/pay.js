import request from '@/utils/request'

/** 支付模块：对应后端 PayController（§3.3 第 13、14 个接口） */

// POST /api/pay/mock/{orderNo} 本地模拟网关，内部走同一套回调逻辑
export function mockPay(orderNo) {
  return request.post('/pay/mock/' + orderNo)
}

// POST /api/pay/callback 网关回调（前端仅用于联调，真实由网关调用）
export function payCallback(data) {
  return request.post('/pay/callback', data)
}
