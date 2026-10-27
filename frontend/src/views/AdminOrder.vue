<template>
  <div class="page">
    <h3 class="page-title">订单管理</h3>
    <div class="toolbar">
      <el-select v-model="status" placeholder="全部状态" clearable style="width: 160px" @change="search">
        <el-option v-for="(item, key) in ORDER_STATUS" :key="key" :label="item.text" :value="Number(key)" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="load">刷新</el-button>
    </div>
    <el-table v-loading="loading" :data="records" border stripe>
      <el-table-column prop="orderNo" label="订单号" min-width="200" show-overflow-tooltip />
      <el-table-column prop="userId" label="用户 ID" width="100" align="center" />
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
      <el-table-column label="支付时间" width="180">
        <template #default="{ row }">{{ datetime(row.payTime) }}</template>
      </el-table-column>
    </el-table>
    <Pagination :current="page" :size="size" :total="total" @change="onPageChange" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getAdminOrders } from '@/api/admin'
import { ORDER_STATUS, datetime, money, orderStatusText, orderStatusType } from '@/utils/format'
import Pagination from '@/components/Pagination.vue'

const loading = ref(false)
const records = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const status = ref(null)

async function load() {
  loading.value = true
  try {
    const data = await getAdminOrders({
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

onMounted(load)
</script>
