<template>
  <div class="page">
    <h3 class="page-title">秒杀专区</h3>
    <el-alert
      type="warning"
      :closable="false"
      show-icon
      title="秒杀链路：Lua 令牌桶限流 → Lua 原子扣减 Redis 库存 → 同步事务落库，抢到后 30 分钟内未支付自动关单"
      style="margin-bottom: 16px"
    />
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card header="活动列表">
          <div v-loading="loadingActivities">
            <el-empty v-if="!activities.length" description="暂无秒杀活动，请运营在后台创建" />
            <div
              v-for="item in activities"
              :key="item.id"
              class="activity-item"
              :class="{ active: currentId === item.id }"
              @click="selectActivity(item.id)"
            >
              <div class="activity-name">{{ item.name }}</div>
              <div class="muted small">
                {{ datetime(item.startTime) }} ~ {{ datetime(item.endTime) }}
              </div>
              <el-tag size="small" :type="item.status === 1 ? 'success' : 'info'">
                {{ activityStatusText(item.status) }}
              </el-tag>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card v-loading="loadingDetail" :header="detail ? detail.name : '活动详情'">
          <el-empty v-if="!detail" description="选择左侧活动查看秒杀商品" />
          <template v-else>
            <el-descriptions :column="3" border style="margin-bottom: 16px">
              <el-descriptions-item label="开始时间">{{ datetime(detail.startTime) }}</el-descriptions-item>
              <el-descriptions-item label="结束时间">{{ datetime(detail.endTime) }}</el-descriptions-item>
              <el-descriptions-item label="状态">{{ activityStatusText(detail.status) }}</el-descriptions-item>
            </el-descriptions>
            <el-table :data="detail.skus || []" border>
              <el-table-column prop="skuId" label="SKU" width="90" />
              <el-table-column prop="skuName" label="规格" min-width="150" />
              <el-table-column label="秒杀价" width="120" align="right">
                <template #default="{ row }">
                  <span class="money">￥{{ money(row.seckillPrice) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="剩余库存（Redis 实时）" width="170" align="center">
                <template #default="{ row }">
                  <el-tag :type="stockOf(row) > 0 ? 'success' : 'danger'" size="small">
                    {{ stockOf(row) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="150" align="center">
                <template #default="{ row }">
                  <el-button
                    type="danger"
                    size="small"
                    :loading="submitting === row.skuId"
                    :disabled="stockOf(row) <= 0"
                    @click="onSeckill(row)"
                  >
                    {{ stockOf(row) > 0 ? '立即秒杀' : '已抢光' }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </template>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="successVisible" title="抢购成功" width="420px">
      <p>订单号：<strong>{{ orderNo }}</strong></p>
      <p class="muted">请在 30 分钟内完成支付，超时订单自动关闭。</p>
      <template #footer>
        <el-button @click="goOrders">查看订单</el-button>
        <el-button type="primary" @click="payNow">立即支付（模拟）</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getActivities, getActivityDetail, seckillOrder } from '@/api/seckill'
import { mockPay } from '@/api/pay'
import { datetime, money } from '@/utils/format'

const router = useRouter()
const loadingActivities = ref(false)
const loadingDetail = ref(false)
const activities = ref([])
const detail = ref(null)
const currentId = ref(null)
const submitting = ref(null)
const successVisible = ref(false)
const orderNo = ref('')

function activityStatusText(status) {
  if (status === 1) return '进行中'
  if (status === 0) return '未开始'
  return '已结束'
}

// 后端 SeckillSkuVO 给的是 redisStock（Redis 实时库存）与 dbStock（数据库库存），优先展示实时值
function stockOf(row) {
  if (!row) return 0
  const value = row.redisStock != null ? row.redisStock : row.dbStock
  return Number(value) || 0
}

async function loadActivities() {
  loadingActivities.value = true
  try {
    const data = await getActivities()
    activities.value = Array.isArray(data) ? data : (data && data.records) || []
    if (activities.value.length && !currentId.value) {
      await selectActivity(activities.value[0].id)
    }
  } finally {
    loadingActivities.value = false
  }
}

async function selectActivity(id) {
  currentId.value = id
  loadingDetail.value = true
  try {
    detail.value = await getActivityDetail(id)
  } finally {
    loadingDetail.value = false
  }
}

async function onSeckill(row) {
  try {
    await ElMessageBox.confirm('确认以秒杀价 ￥' + money(row.seckillPrice) + ' 抢购该规格？', '秒杀确认', {
      type: 'warning'
    })
  } catch (e) {
    return
  }
  submitting.value = row.skuId
  try {
    const data = await seckillOrder(currentId.value, row.skuId)
    orderNo.value = (data && data.orderNo) || data || '-'
    successVisible.value = true
    ElMessage.success('抢购成功，请尽快支付')
    await selectActivity(currentId.value)
  } finally {
    submitting.value = null
  }
}

async function payNow() {
  await mockPay(orderNo.value)
  ElMessage.success('支付成功')
  successVisible.value = false
  router.push('/orders/' + orderNo.value)
}

function goOrders() {
  successVisible.value = false
  router.push('/orders')
}

onMounted(loadActivities)
</script>

<style scoped>
.activity-item {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 10px 12px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: border-color 0.2s;
}

.activity-item.active {
  border-color: #f56c6c;
  background: #fef0f0;
}

.activity-name {
  font-weight: 600;
  margin-bottom: 4px;
}

.small {
  font-size: 12px;
  margin-bottom: 6px;
}
</style>
