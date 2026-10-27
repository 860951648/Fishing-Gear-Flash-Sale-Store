import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const Layout = () => import('@/components/Layout.vue')

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/products',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'products',
        name: 'ProductList',
        component: () => import('@/views/ProductList.vue'),
        meta: { title: '商品列表' }
      },
      {
        path: 'products/:id',
        name: 'ProductDetail',
        component: () => import('@/views/ProductDetail.vue'),
        meta: { title: '商品详情' }
      },
      {
        path: 'seckill',
        name: 'Seckill',
        component: () => import('@/views/Seckill.vue'),
        meta: { title: '秒杀专区' }
      },
      {
        path: 'orders',
        name: 'OrderList',
        component: () => import('@/views/OrderList.vue'),
        meta: { title: '我的订单' }
      },
      {
        path: 'orders/:orderNo',
        name: 'OrderDetail',
        component: () => import('@/views/OrderDetail.vue'),
        meta: { title: '订单详情' }
      }
    ]
  },
  {
    path: '/admin',
    component: Layout,
    redirect: '/admin/dashboard',
    meta: { requiresAuth: true, roles: ['ADMIN'] },
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/AdminDashboard.vue'),
        meta: { title: '运营看板', roles: ['ADMIN'] }
      },
      {
        path: 'products',
        name: 'AdminProduct',
        component: () => import('@/views/AdminProduct.vue'),
        meta: { title: '商品管理', roles: ['ADMIN'] }
      },
      {
        path: 'activities',
        name: 'AdminActivity',
        component: () => import('@/views/AdminActivity.vue'),
        meta: { title: '秒杀活动', roles: ['ADMIN'] }
      },
      {
        path: 'orders',
        name: 'AdminOrder',
        component: () => import('@/views/AdminOrder.vue'),
        meta: { title: '订单管理', roles: ['ADMIN'] }
      },
      {
        path: 'refunds',
        name: 'AdminRefund',
        component: () => import('@/views/AdminRefund.vue'),
        meta: { title: '退款审核', roles: ['ADMIN'] }
      }
    ]
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/Forbidden.vue'),
    meta: { title: '无权限' }
  },
  { path: '/:pathMatch(.*)*', redirect: '/products' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  const userStore = useUserStore()
  document.title = (to.meta.title ? to.meta.title + ' · ' : '') + '渔具秒杀商城'

  if (to.path === '/login' && userStore.token) return { path: '/' }
  if (!to.meta.requiresAuth) return true
  if (!userStore.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  // 刷新后 store 里只有 token，补拉一次用户信息；失败说明 token 失效
  if (!userStore.user) {
    try {
      await userStore.fetchMe()
    } catch (e) {
      userStore.reset()
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }
  const roles = to.meta.roles
  if (roles && roles.length && !roles.some((role) => userStore.roles.indexOf(role) > -1)) {
    return { path: '/403' }
  }
  return true
})

export default router
