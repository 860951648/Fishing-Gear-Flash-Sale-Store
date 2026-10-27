<template>
  <div class="page" v-loading="loading">
    <h3 class="page-title">运营看板</h3>
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-label">今日销售额</div>
          <div class="stat-value money">￥{{ money(overview.todaySales) }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-label">今日订单数</div>
          <div class="stat-value">{{ overview.orderCount || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-label">待审核退款</div>
          <div class="stat-value warning">{{ overview.pendingRefund || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-label">库存预警 SKU</div>
          <div class="stat-value danger">{{ overview.stockWarn || 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 16px">
      <template #header>
        <div class="card-header">
          <span>销售趋势</span>
          <div>
            <el-select v-model="days" style="width: 120px" @change="loadTrend">
              <el-option :value="7" label="近 7 天" />
              <el-option :value="15" label="近 15 天" />
              <el-option :value="30" label="近 30 天" />
            </el-select>
            <el-button style="margin-left: 8px" @click="loadAll">刷新</el-button>
          </div>
        </div>
      </template>
      <el-table :data="trend" border>
        <el-table-column prop="date" label="日期" width="180" />
        <el-table-column label="销售额" min-width="200">
          <template #default="{ row }">
            <div class="bar-wrap">
              <div class="bar" :style="{ width: barWidth(row.amount) }"></div>
              <span class="money">￥{{ money(row.amount) }}</span>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!trend.length" description="暂无销售数据（先跑主流程下单支付）" />
    </el-card>

    <el-card style="margin-top: 16px">
      <template #header>快捷入口</template>
      <el-button type="primary" @click="router.push('/admin/products')">商品管理</el-button>
      <el-button type="warning" @click="router.push('/admin/activities')">秒杀活动</el-button>
      <el-button @click="router.push('/admin/orders')">订单管理</el-button>
      <el-button type="danger" @click="router.push('/admin/refunds')">退款审核</el-button>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getStatsOverview, getSalesTrend } from '@/api/admin'
import { money } from '@/utils/format'

const router = useRouter()
const loading = ref(false)
const overview = ref({})
const trend = ref([])
const days = ref(7)

function barWidth(amount) {
  const max = Math.max.apply(null, [1].concat(trend.value.map((item) => Number(item.amount) || 0)))
  const percent = ((Number(amount) || 0) / max) * 100
  return Math.max(percent, 2) + '%'
}

async function loadOverview() {
  overview.value = (await getStatsOverview()) || {}
}

async function loadTrend() {
  const data = await getSalesTrend(days.value)
  trend.value = Array.isArray(data) ? data : []
}

async function loadAll() {
  loading.value = true
  try {
    await Promise.all([loadOverview(), loadTrend()])
  } finally {
    loading.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
.stat-label {
  color: #909399;
  font-size: 13px;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
}

.warning {
  color: #e6a23c;
}

.danger {
  color: #f56c6c;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.bar-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.bar {
  height: 10px;
  background: #409eff;
  border-radius: 5px;
  min-width: 8px;
}
</style>
