import request from '@/utils/request'

/** 认证模块：对应后端 AuthController（§3.3 第 1-4 个接口） */

// POST /api/auth/register 公开注册
export function register(data) {
  return request.post('/auth/register', data)
}

// POST /api/auth/login 返回 {token,user}
export function login(data) {
  return request.post('/auth/login', data)
}

// POST /api/auth/logout 删除 Redis 白名单
export function logout() {
  return request.post('/auth/logout')
}

// GET /api/auth/me 当前登录用户
export function getMe() {
  return request.get('/auth/me')
}
