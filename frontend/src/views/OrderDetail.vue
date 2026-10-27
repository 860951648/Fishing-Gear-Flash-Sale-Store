<template>
  <div class="page" v-loading="loading">
    <el-page-header content="订单详情" @back="router.back()" />
    <template v-if="detail">
      <el-card class="detail-card" style="margin-top: 16px">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="orderStatusType(detail.status)" size="small">{{ orderStatusText(detail.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="订单金额">
            <span class="money">￥{{ money(detail.totalAmount) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="实付金额">
            <span class="money">￥{{ money(detail.payAmount) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ datetime(detail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ datetime(detail.payTime) }}</el-descriptions-item>
        </el-descriptions>
        <div style="margin-top: 16px">
          <el-button v-if="detail.status === 0" type="success" @click="pay">模拟支付</el-button>
          <el-button v-if="detail.status === 0" type="info" @click="cancel">取消订单</el-button>
          <el-button v-if="[1, 2, 3].indexOf(detail.status) > -1" type="warning" @click="refundVisible = true">
            申请退款
          </el-button>
          <el-button @click="router.push('/orders')">返回列表</el-button>
        </div>
      </el-card>

      <el-card header="订单明细" class="detail-card">
        <el-table :data="detail.items || []" border>
          <el-table-column prop="productName" label="商品" min-width="180" />
          <el-table-column prop="skuName" label="规格" min-width="140" />
          <el-table-column label="单价" width="120" align="right">
            <template #default="{ row }">
              <span class="money">￥{{ money(row.price) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="90" align="center" />
        </el-table>
      </el-card>
    </template>

    <el-dialog v-model="refundVisible" title="申请退款" width="420px">
      <el-form label-width="80px">
        <el-form-item label="退款原因">
          <el-input v-model="refundReason" type="textarea" :rows="3" placeholder="请填写退款原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitRefund">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderDetail, cancelOrder } from '@/api/order'
import { mockPay } from '@/api/pay'
import { applyRefund } from '@/api/refund'
import { datetime, money, orderStatusText, orderStatusType } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref(null)
const refundVisible = ref(false)
const refundReason = ref('')
const submitting = ref(false)

async function load() {
  loading.value = true
  try {
    detail.value = await getOrderDetail(route.params.orderNo)
  } finally {
    loading.value = false
  }
}

async function pay() {
  await mockPay(detail.value.orderNo)
  ElMessage.success('支付成功')
  load()
}

async function cancel() {
  try {
    await ElMessageBox.confirm('确定取消该订单？', '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await cancelOrder(detail.value.orderNo)
  ElMessage.success('订单已取消')
  load()
}

async function submitRefund() {
  if (!refundReason.value.trim()) {
    ElMessage.warning('请填写退款原因')
    return
  }
  submitting.value = true
  try {
    const data = await applyRefund({ orderNo: detail.value.orderNo, reason: refundReason.value })
    ElMessage.success('退款申请已提交，退款单号：' + ((data && data.refundNo) || '-'))
    refundVisible.value = false
    refundReason.value = ''
    load()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>
