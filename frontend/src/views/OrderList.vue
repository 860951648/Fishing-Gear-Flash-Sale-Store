<template>
  <div class="page">
    <h3 class="page-title">我的订单</h3>
    <div class="toolbar">
      <el-select v-model="status" placeholder="全部状态" clearable style="width: 160px" @change="search">
        <el-option v-for="(item, key) in ORDER_STATUS" :key="key" :label="item.text" :value="Number(key)" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="load">刷新</el-button>
    </div>
    <el-table v-loading="loading" :data="records" border stripe>
      <el-table-column prop="orderNo" label="订单号" min-width="200" show-overflow-tooltip />
      <el-table-column label="金额" width="120" align="right">
        <template #default="{ row }">
          <span class="money">￥{{ money(row.payAmount != null ? row.payAmount : row.totalAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="orderStatusType(row.status)" size="small">{{ orderStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="下单时间" width="180">
        <template #default="{ row }">{{ datetime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="230" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push('/orders/' + row.orderNo)">详情</el-button>
          <el-button v-if="row.status === 0" link type="success" @click="pay(row)">支付</el-button>
          <el-button v-if="row.status === 0" link type="info" @click="cancel(row)">取消</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :current="page" :size="size" :total="total" @change="onPageChange" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrders, cancelOrder } from '@/api/order'
import { mockPay } from '@/api/pay'
import { ORDER_STATUS, datetime, money, orderStatusText, orderStatusType } from '@/utils/format'
import Pagination from '@/components/Pagination.vue'

const router = useRouter()
const loading = ref(false)
const records = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const status = ref(null)

async function load() {
  loading.value = true
  try {
    const data = await getOrders({
      page: page.value,
      size: size.value,
      status: status.value === null || status.value === '' ? undefined : status.value
    })
    records.value = (data && data.records) || []
    total.value = (data && data.total) || 0
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

function onPageChange(payload) {
  page.value = payload.page
  size.value = payload.size
  load()
}

async function pay(row) {
  await mockPay(row.orderNo)
  ElMessage.success('支付成功')
  load()
}

async function cancel(row) {
  try {
    await ElMessageBox.confirm('确定取消该订单？', '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await cancelOrder(row.orderNo)
  ElMessage.success('订单已取消')
  load()
}

onMounted(load)
</script>
