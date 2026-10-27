import request from '@/utils/request'

/**
 * 运营后台模块：对应后端 AdminController / AdminStatsController
 * 覆盖 §3.3 第 17-29 个接口（商品、SKU 库存、秒杀活动、订单、退款、统计）
 */

// ---------- 商品 ----------
// GET /api/admin/products?page&size&keyword（product:manage）
export function getAdminProducts(params) {
  return request.get('/admin/products', { params })
}

// POST /api/admin/products 新增商品（含 SKU 列表），返回 {id}
export function createProduct(data) {
  return request.post('/admin/products', data)
}

// PUT /api/admin/products/{id} 编辑商品
export function updateProduct(id, data) {
  return request.put('/admin/products/' + id, data)
}

// DELETE /api/admin/products/{id} 逻辑删除
export function deleteProduct(id) {
  return request.delete('/admin/products/' + id)
}

// PUT /api/admin/skus/{id}/stock 调整 SKU 可售库存 {available}
export function updateSkuStock(id, available) {
  return request.put('/admin/skus/' + id + '/stock', { available })
}

// ---------- 秒杀活动 ----------
// GET /api/admin/seckill/activities（seckill:manage）
export function getAdminActivities() {
  return request.get('/admin/seckill/activities')
}

// POST /api/admin/seckill/activities 创建活动，返回 {id}
export function createActivity(data) {
  return request.post('/admin/seckill/activities', data)
}

// POST /api/admin/seckill/activities/{id}/preheat 预热：库存写入 Redis
export function preheatActivity(id) {
  return request.post('/admin/seckill/activities/' + id + '/preheat')
}

// ---------- 订单 ----------
// GET /api/admin/orders?page&size&status（order:manage）
export function getAdminOrders(params) {
  return request.get('/admin/orders', { params })
}

// ---------- 退款审核 ----------
// GET /api/admin/refunds?page&size&status（refund:audit）
export function getAdminRefunds(params) {
  return request.get('/admin/refunds', { params })
}

// POST /api/admin/refunds/{id}/audit {approved,remark}
export function auditRefund(id, data) {
  return request.post('/admin/refunds/' + id + '/audit', data)
}

// ---------- 统计 ----------
// GET /api/admin/stats/overview（stats:view）
export function getStatsOverview() {
  return request.get('/admin/stats/overview')
}

// GET /api/admin/stats/sales-trend?days（stats:view）
export function getSalesTrend(days) {
  return request.get('/admin/stats/sales-trend', { params: { days } })
}
