import request from '@/utils/request'

/** 秒杀模块：对应后端 SeckillController（§3.3 第 7-9 个接口） */

// GET /api/seckill/activities 活动列表
export function getActivities() {
  return request.get('/seckill/activities')
}

// GET /api/seckill/activities/{id} 活动详情（含 Redis 实时库存）
export function getActivityDetail(id) {
  return request.get('/seckill/activities/' + id)
}

// POST /api/seckill/{activityId}/skus/{skuId} 秒杀下单，返回 {orderNo}
export function seckillOrder(activityId, skuId) {
  return request.post('/seckill/' + activityId + '/skus/' + skuId)
}
