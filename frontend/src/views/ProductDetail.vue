<template>
  <div class="page" v-loading="loading">
    <el-page-header content="商品详情" @back="router.back()" />
    <el-card v-if="detail" class="detail-card" style="margin-top: 16px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="商品名称">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="价格">
          <span class="money">￥{{ money(detail.price) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="副标题" :span="2">{{ detail.subtitle || '-' }}</el-descriptions-item>
        <el-descriptions-item label="商品编号">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status === 1 ? '上架' : '下架' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card header="SKU 规格" class="detail-card">
      <el-table :data="skus" border>
        <el-table-column prop="id" label="SKU ID" width="90" />
        <el-table-column prop="name" label="规格名称" min-width="180" />
        <el-table-column label="价格" width="130" align="right">
          <template #default="{ row }">
            <span class="money">￥{{ money(row.price) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center">
          <template #default>
            <el-button size="small" type="danger" plain @click="router.push('/seckill')">参加秒杀</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!skus.length" description="该商品暂无 SKU" />
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductDetail } from '@/api/product'
import { money } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref(null)

const skus = computed(() => (detail.value && detail.value.skus) || [])

async function load() {
  loading.value = true
  try {
    detail.value = await getProductDetail(route.params.id)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
