<template>
  <div class="page">
    <h3 class="page-title">商品列表</h3>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="按商品名称搜索" clearable style="width: 240px" @keyup.enter="search" />
      <el-button type="primary" @click="search">搜索</el-button>
      <el-button @click="load">刷新</el-button>
      <el-button type="danger" plain @click="router.push('/seckill')">去秒杀专区</el-button>
    </div>
    <el-table v-loading="loading" :data="records" border stripe @row-dblclick="goDetail">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="商品名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="subtitle" label="副标题" min-width="220" show-overflow-tooltip />
      <el-table-column label="价格" width="120" align="right">
        <template #default="{ row }">
          <span class="money">￥{{ money(row.price) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="goDetail(row)">查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :current="page" :size="size" :total="total" @change="onPageChange" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getProducts } from '@/api/product'
import { money } from '@/utils/format'
import Pagination from '@/components/Pagination.vue'

const router = useRouter()
const loading = ref(false)
const keyword = ref('')
const records = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

async function load() {
  loading.value = true
  try {
    const data = await getProducts({ page: page.value, size: size.value, keyword: keyword.value || undefined })
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

function goDetail(row) {
  router.push('/products/' + row.id)
}

onMounted(load)
</script>
