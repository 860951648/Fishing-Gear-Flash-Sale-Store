<template>
  <div class="page">
    <h3 class="page-title">秒杀活动</h3>
    <div class="toolbar">
      <el-button type="primary" @click="load">刷新</el-button>
      <el-button type="success" @click="openCreate">新建活动</el-button>
      <span class="muted">活动创建后需「预热」，把库存写入 Redis，秒杀才能扣减</span>
    </div>

    <el-table v-loading="loading" :data="activities" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="活动名称" min-width="180" />
      <el-table-column label="开始时间" width="180">
        <template #default="{ row }">{{ datetime(row.startTime) }}</template>
      </el-table-column>
      <el-table-column label="结束时间" width="180">
        <template #default="{ row }">{{ datetime(row.endTime) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" align="center">
        <template #default="{ row }">
          <el-button link type="warning" @click="preheat(row)">预热库存</el-button>
          <el-button link type="primary" @click="router.push('/seckill')">前台查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="新建秒杀活动" width="720px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="活动名称" required>
          <el-input v-model="form.name" placeholder="如 双十一渔具秒杀" />
        </el-form-item>
        <el-form-item label="活动时间" required>
          <el-date-picker
            v-model="range"
            type="datetimerange"
            value-format="YYYY-MM-DDTHH:mm:ss"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
          />
        </el-form-item>
        <el-form-item label="秒杀商品">
          <div style="width: 100%">
            <el-table :data="form.skus" size="small" border>
              <el-table-column label="SKU ID" width="140">
                <template #default="{ row }">
                  <el-input-number v-model="row.skuId" :min="1" size="small" controls-position="right" />
                </template>
              </el-table-column>
              <el-table-column label="秒杀价">
                <template #default="{ row }">
                  <el-input-number v-model="row.seckillPrice" :precision="2" :min="0.01" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="秒杀库存" width="140">
                <template #default="{ row }">
                  <el-input-number v-model="row.stock" :min="1" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80" align="center">
                <template #default="{ $index }">
                  <el-button link type="danger" @click="form.skus.splice($index, 1)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-button size="small" style="margin-top: 8px" @click="addSku">+ 添加秒杀 SKU</el-button>
            <div class="muted" style="font-size: 12px">SKU ID 可在「商品管理 → 编辑」的 SKU 列表里查到。</div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminActivities, createActivity, preheatActivity } from '@/api/admin'
import { datetime } from '@/utils/format'

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const activities = ref([])
const dialogVisible = ref(false)
const range = ref([])
const form = reactive({ name: '', skus: [{ skuId: 1, seckillPrice: 9.9, stock: 100 }] })

function statusText(status) {
  if (status === 1) return '进行中'
  if (status === 0) return '未开始'
  return '已结束'
}

async function load() {
  loading.value = true
  try {
    const data = await getAdminActivities()
    activities.value = Array.isArray(data) ? data : (data && data.records) || []
  } finally {
    loading.value = false
  }
}

function addSku() {
  form.skus.push({ skuId: 1, seckillPrice: 9.9, stock: 100 })
}

function openCreate() {
  form.name = ''
  form.skus = [{ skuId: 1, seckillPrice: 9.9, stock: 100 }]
  range.value = []
  dialogVisible.value = true
}

async function submit() {
  if (!form.name.trim()) {
    ElMessage.warning('请填写活动名称')
    return
  }
  if (!range.value || range.value.length !== 2) {
    ElMessage.warning('请选择活动时间')
    return
  }
  submitting.value = true
  try {
    const data = await createActivity({
      name: form.name,
      startTime: range.value[0],
      endTime: range.value[1],
      skus: form.skus.map((item) => ({
        skuId: item.skuId,
        seckillPrice: item.seckillPrice,
        stock: item.stock
      }))
    })
    ElMessage.success('活动已创建，ID：' + ((data && data.id) || '-'))
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function preheat(row) {
  try {
    await ElMessageBox.confirm('确认预热活动「' + row.name + '」的库存到 Redis？', '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await preheatActivity(row.id)
  ElMessage.success('预热完成')
  load()
}

onMounted(load)
</script>
