<template>
  <div class="page">
    <h3 class="page-title">商品管理</h3>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="按商品名称搜索" clearable style="width: 240px" @keyup.enter="search" />
      <el-button type="primary" @click="search">搜索</el-button>
      <el-button type="success" @click="openCreate">新增商品</el-button>
    </div>

    <el-table v-loading="loading" :data="records" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="商品名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="subtitle" label="副标题" min-width="180" show-overflow-tooltip />
      <el-table-column label="价格" width="120" align="right">
        <template #default="{ row }"><span class="money">￥{{ money(row.price) }}</span></template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '上架' : '下架' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="warning" @click="openStock(row)">调整库存</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :current="page" :size="size" :total="total" @change="onPageChange" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑商品' : '新增商品'" width="720px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="商品名称" required>
          <el-input v-model="form.name" placeholder="商品名称" />
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="form.subtitle" placeholder="副标题" />
        </el-form-item>
        <el-form-item label="封面 URL">
          <el-input v-model="form.cover" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="价格" required>
          <el-input-number v-model="form.price" :precision="2" :min="0.01" :step="1" />
        </el-form-item>
        <el-form-item label="SKU 列表">
          <div style="width: 100%">
            <el-table :data="form.skus" size="small" border>
              <el-table-column label="规格名称">
                <template #default="{ row }">
                  <el-input v-model="row.name" placeholder="如 3.6 米 / 5.4 米" />
                </template>
              </el-table-column>
              <el-table-column label="价格" width="150">
                <template #default="{ row }">
                  <el-input-number v-model="row.price" :precision="2" :min="0.01" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="库存" width="130">
                <template #default="{ row }">
                  <el-input-number v-model="row.stock" :min="0" size="small" :disabled="!!row.id" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80" align="center">
                <template #default="{ $index }">
                  <el-button link type="danger" @click="form.skus.splice($index, 1)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-button size="small" style="margin-top: 8px" @click="addSku">+ 添加 SKU</el-button>
            <div class="muted" style="font-size: 12px">已有 SKU 的库存请用列表里的「调整库存」，避免覆盖并发扣减结果。</div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="stockVisible" title="调整 SKU 库存" width="500px">
      <el-form label-width="100px">
        <el-form-item label="商品">{{ stockForm.productName }}</el-form-item>
        <el-form-item label="选择 SKU">
          <el-select v-model="stockForm.skuId" style="width: 100%" placeholder="选择 SKU" @change="onStockSkuChange">
            <el-option
              v-for="sku in stockForm.skus"
              :key="sku.id"
              :label="sku.name + '（当前可售 ' + sku.available + '）'"
              :value="sku.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="可售库存">
          <el-input-number v-model="stockForm.available" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitStock">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAdminProducts,
  createProduct,
  updateProduct,
  deleteProduct,
  updateSkuStock
} from '@/api/admin'
import { getProductDetail } from '@/api/product'
import { money } from '@/utils/format'
import Pagination from '@/components/Pagination.vue'

const loading = ref(false)
const submitting = ref(false)
const records = ref([])
const keyword = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const stockVisible = ref(false)
const form = reactive({ id: null, name: '', subtitle: '', cover: '', price: 1, skus: [] })
const stockForm = reactive({ productName: '', skus: [], skuId: null, available: 0 })

async function load() {
  loading.value = true
  try {
    const data = await getAdminProducts({ page: page.value, size: size.value, keyword: keyword.value || undefined })
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

function resetForm() {
  form.id = null
  form.name = ''
  form.subtitle = ''
  form.cover = ''
  form.price = 1
  form.skus = [{ name: '', price: 1, stock: 100 }]
}

function addSku() {
  form.skus.push({ name: '', price: 1, stock: 100 })
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row) {
  resetForm()
  form.id = row.id
  form.name = row.name
  form.subtitle = row.subtitle || ''
  form.cover = row.cover || ''
  form.price = Number(row.price) || 1
  const detail = await getProductDetail(row.id)
  const skus = (detail && detail.skus) || []
  form.skus = skus.map((sku) => ({
    id: sku.id,
    name: sku.name,
    price: Number(sku.price),
    // 后端 SkuVO 的库存字段是 available（t_stock.available），不是 stock
    stock: Number(sku.available != null ? sku.available : 0)
  }))
  if (!form.skus.length) addSku()
  dialogVisible.value = true
}

async function submit() {
  if (!form.name.trim()) {
    ElMessage.warning('请填写商品名称')
    return
  }
  const payload = {
    name: form.name,
    subtitle: form.subtitle,
    cover: form.cover,
    price: form.price,
    skus: form.skus.filter((sku) => sku.name && sku.name.trim()).map((sku) => ({
      name: sku.name,
      price: sku.price,
      stock: sku.stock
    }))
  }
  submitting.value = true
  try {
    if (form.id) {
      await updateProduct(form.id, payload)
      ElMessage.success('商品已更新')
    } else {
      const data = await createProduct(payload)
      ElMessage.success('商品已创建，ID：' + ((data && data.id) || '-'))
    }
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function openStock(row) {
  // 商品列表只有商品维度，SKU 库存要单独拉一次详情：t_stock.available 才是可售库存
  const detail = await getProductDetail(row.id)
  const skus = ((detail && detail.skus) || []).map((sku) => ({
    id: sku.id,
    name: sku.name,
    available: Number(sku.available != null ? sku.available : 0)
  }))
  if (!skus.length) {
    ElMessage.warning('该商品还没有 SKU')
    return
  }
  stockForm.productName = row.name
  stockForm.skus = skus
  stockForm.skuId = skus[0].id
  stockForm.available = skus[0].available
  stockVisible.value = true
}

function onStockSkuChange(skuId) {
  const target = stockForm.skus.find((sku) => sku.id === skuId)
  stockForm.available = target ? target.available : 0
}

async function submitStock() {
  if (!stockForm.skuId) {
    ElMessage.warning('请选择 SKU')
    return
  }
  submitting.value = true
  try {
    await updateSkuStock(stockForm.skuId, stockForm.available)
    ElMessage.success('库存已更新')
    stockVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm('确定删除商品「' + row.name + '」？该操作是逻辑删除。', '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await deleteProduct(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
