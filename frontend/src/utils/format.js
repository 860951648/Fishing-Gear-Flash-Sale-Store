/** 通用格式化与字典：金额、时间、订单状态 */

export function money(value) {
  const num = Number(value)
  if (!isFinite(num)) return '0.00'
  return num.toFixed(2)
}

export function datetime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').substring(0, 19)
}

export const ORDER_STATUS = {
  0: { text: '待支付', type: 'warning' },
  1: { text: '已支付', type: 'success' },
  2: { text: '已发货', type: 'primary' },
  3: { text: '已完成', type: 'success' },
  4: { text: '已关闭', type: 'info' },
  5: { text: '退款中', type: 'warning' },
  6: { text: '已退款', type: 'danger' }
}

export function orderStatusText(status) {
  const item = ORDER_STATUS[status]
  return item ? item.text : '未知'
}

export function orderStatusType(status) {
  const item = ORDER_STATUS[status]
  return item ? item.type : 'info'
}

export const REFUND_STATUS = {
  0: { text: '待审核', type: 'warning' },
  1: { text: '已通过', type: 'success' },
  2: { text: '已驳回', type: 'danger' }
}

export function refundStatusText(status) {
  const item = REFUND_STATUS[status]
  return item ? item.text : '未知'
}

export function refundStatusType(status) {
  const item = REFUND_STATUS[status]
  return item ? item.type : 'info'
}
