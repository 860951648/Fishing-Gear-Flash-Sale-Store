import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi, getMe } from '@/api/auth'
import { TOKEN_KEY } from '@/utils/request'

const ADMIN_PERMISSIONS = ['product:manage', 'seckill:manage', 'order:manage', 'refund:audit', 'stats:view']

/**
 * 从后端返回的 user / permissions 里推导角色，供路由 meta.roles 使用。
 * 后端若直接给 user.roles=['ADMIN'] 最省事；没有时用权限码与用户名兜底，
 * 避免演示环境因为少一个字段进不去运营后台。
 */
function normalizeRoles(user, permissions) {
  const roles = []
  if (user) {
    if (Array.isArray(user.roles)) roles.push(...user.roles)
    if (user.role) roles.push(user.role)
    if (user.isAdmin) roles.push('ADMIN')
    if (user.username === 'admin') roles.push('ADMIN')
  }
  if (Array.isArray(permissions) && permissions.some((code) => ADMIN_PERMISSIONS.indexOf(code) > -1)) {
    roles.push('ADMIN')
  }
  return Array.from(new Set(roles.filter(Boolean)))
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: null,
    permissions: [],
    roles: []
  }),
  getters: {
    isLogin: (state) => !!state.token,
    isAdmin: (state) => state.roles.indexOf('ADMIN') > -1,
    nickname: (state) => (state.user && (state.user.nickname || state.user.username)) || '未登录'
  },
  actions: {
    async login(form) {
      const data = await loginApi(form)
      this.token = data.token
      localStorage.setItem(TOKEN_KEY, data.token)
      this.user = data.user || null
      this.permissions = data.permissions || []
      this.roles = normalizeRoles(this.user, this.permissions)
      return data
    },
    async fetchMe() {
      const data = await getMe()
      if (data && data.user) {
        this.user = data.user
        this.permissions = data.permissions || []
      } else {
        this.user = data
      }
      this.roles = normalizeRoles(this.user, this.permissions)
      return this.user
    },
    async logout() {
      try {
        await logoutApi()
      } catch (e) {
        // 登出接口失败也要清本地状态，否则用户卡在登录态
      }
      this.reset()
    },
    reset() {
      this.token = ''
      this.user = null
      this.permissions = []
      this.roles = []
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem('trade_user')
    }
  }
})
