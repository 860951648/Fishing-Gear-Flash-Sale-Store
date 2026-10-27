<template>
  <div class="page">
    <h3 class="page-title">退款审核</h3>
    <div class="toolbar">
      <el-select v-model="status" placeholder="全部状态" clearable style="width: 160px" @change="search">
        <el-option label="待审核" :value="0" />
        <el-option label="已通过" :value="1" />
        <el-option label="已驳回" :value="2" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="load">刷新</el-button>
    </div>
    <el-table v-loading="loading" :data="records" border stripe>
      <el-table-column prop="refundNo" label="退款单号" min-width="200" show-overflow-tooltip />
      <el-table-column prop="orderNo" label="订单号" min-width="200" show-overflow-tooltip />
      <el-table-column label="退款金额" width="120" align="right">
        <template #default="{ row }"><span class="money">￥{{ money(row.amount) }}</span></template>
      </el-table-column>
      <el-table-column prop="reason" label="退款原因" min-width="180" show-overflow-tooltip />
      <el-table-column label="状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="refundStatusType(row.status)" size="small">{{ refundStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="申请时间" width="180">
        <template #default="{ row }">{{ datetime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="130" align="center">
        <template #default="{ row }">
          <el-button v-if="row.status === 0" link type="primary" @click="openAudit(row)">审核</el-button>
          <span v-else class="muted">已处理</span>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :current="page" :size="size" :total="total" @change="onPageChange" />

    <el-dialog v-model="dialogVisible" title="退款审核" width="460px">
      <el-form label-width="90px">
        <el-form-item label="退款单号">{{ current.refundNo }}</el-form-item>
        <el-form-item label="审核结果">
          <el-radio-group v-model="auditForm.approved">
            <el-radio-button :label="true">通过</el-radio-button>
            <el-radio-button :label="false">驳回</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input v-model="auditForm.remark" type="textarea" :rows="3" placeholder="审核意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAudit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminRefunds, auditRefund } from '@/api/admin'
import { datetime, money, refundStatusText, refundStatusType } from '@/utils/format'
import Pagination from '@/components/Pagination.vue'

const loading = ref(false)
const submitting = ref(false)
const records = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const status = ref(null)
const dialogVisible = ref(false)
const current = ref({})
const auditForm = reactive({ approved: true, remark: '' })

async function load() {
  loading.value = true
  try {
    const data = await getAdminRefunds({
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

function openAudit(row) {
  current.value = row
  auditForm.approved = true
  auditForm.remark = ''
  dialogVisible.value = true
}

async function submitAudit() {
  submitting.value = true
  try {
    await auditRefund(current.value.id, { approved: auditForm.approved, remark: auditForm.remark })
    ElMessage.success(auditForm.approved ? '已通过退款' : '已驳回退款')
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>
