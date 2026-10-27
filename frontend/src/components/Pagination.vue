<template>
  <div class="pager">
    <el-pagination
      :current-page="current"
      :page-size="size"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      background
      @current-change="(page) => emit('change', { page, size })"
      @size-change="(pageSize) => emit('change', { page: 1, size: pageSize })"
    />
  </div>
</template>

<script setup>
// 分页封装：统一 page/size 参数与后端 PageResult{total,pages,current,size,records} 对齐
defineProps({
  current: { type: Number, default: 1 },
  size: { type: Number, default: 10 },
  total: { type: Number, default: 0 }
})
const emit = defineEmits(['change'])
</script>
