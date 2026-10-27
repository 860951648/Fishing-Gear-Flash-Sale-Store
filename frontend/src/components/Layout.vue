<template>
  <el-container class="layout">
    <el-header class="layout-header">
      <div class="brand">渔具秒杀商城</div>
      <div class="header-right">
        <el-tag v-if="userStore.isAdmin" type="danger" effect="dark" size="small">运营</el-tag>
        <el-dropdown @command="onCommand">
          <span class="user-name">
            {{ userStore.nickname }}
            <el-icon><arrow-down /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="orders">我的订单</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>
    <el-container>
      <el-aside width="208px" class="layout-aside">
        <el-menu :default-active="activeMenu" router unique-opened>
          <el-menu-item index="/products">商品列表</el-menu-item>
          <el-menu-item index="/seckill">秒杀专区</el-menu-item>
          <el-menu-item index="/orders">我的订单</el-menu-item>
          <el-sub-menu v-if="userStore.isAdmin" index="admin">
            <template #title>运营后台</template>
            <el-menu-item index="/admin/dashboard">运营看板</el-menu-item>
            <el-menu-item index="/admin/products">商品管理</el-menu-item>
            <el-menu-item index="/admin/activities">秒杀活动</el-menu-item>
            <el-menu-item index="/admin/orders">订单管理</el-menu-item>
            <el-menu-item index="/admin/refunds">退款审核</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-aside>
      <el-main class="layout-main">
        <router-view v-slot="{ Component }">
          <component :is="Component" />
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 详情页高亮父级菜单
const activeMenu = computed(() => {
  const path = route.path
  if (path.indexOf('/products') === 0 && path !== '/products') return '/products'
  if (path.indexOf('/orders') === 0 && path !== '/orders') return '/orders'
  return path
})

async function onCommand(command) {
  if (command === 'orders') {
    router.push('/orders')
    return
  }
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
    } catch (e) {
      return
    }
    await userStore.logout()
    ElMessage.success('已退出登录')
    router.replace('/login')
  }
}
</script>

<style scoped>
.layout {
  height: 100%;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #1f2d3d;
  color: #fff;
}

.brand {
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 1px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-name {
  color: #fff;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.layout-aside {
  background: #fff;
  border-right: 1px solid #e4e7ed;
}

.layout-main {
  background: #f5f7fa;
  padding: 0;
}
</style>
