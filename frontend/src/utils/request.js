import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * 统一请求层：所有 api/*.js 都从这里发请求。
 * 职责：1) 拼 baseURL 与 Bearer token；2) 把后端 Result{code,msg,data} 解包成 data；
 * 3) code!==200 统一提示；4) 401 清 token 跳登录，403/429 提示。
 */
export const TOKEN_KEY = 'trade_token'

const service = axios.create({
  baseURL: '/api',
  timeout: 15000
})

const CODE_MESSAGE = {
  400: '请求参数有误',
  401: '登录已过期，请重新登录',
  403: '没有该操作的权限',
  404: '请求的资源不存在',
  409: '数据已被修改，请刷新后重试',
  429: '操作过于频繁，请稍后再试',
  500: '服务器异常，请稍后再试',
  503: '服务暂不可用，请稍后重试'
}

// 登录/注册接口本身就返回 401/400，不能触发跳转，否则用户看不到错误提示
const AUTH_API_PREFIX = ['/auth/login', '/auth/register']

function isAuthApi(url) {
  const u = url || ''
  return AUTH_API_PREFIX.some((prefix) => u.indexOf(prefix) === 0)
}

function goLogin() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem('trade_user')
  if (window.location.pathname === '/login') return
  const redirect = encodeURIComponent(window.location.pathname + window.location.search)
  window.location.href = '/login?redirect=' + redirect
}

function notify(code, msg) {
  const text = msg || CODE_MESSAGE[code] || '请求失败'
  ElMessage.error(text)
}

service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) config.headers.Authorization = 'Bearer ' + token
    return config
  },
  (error) => Promise.reject(error)
)

service.interceptors.response.use(
  (response) => {
    // 文件流（导出 CSV）不解包
    if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') {
      return response
    }
    const body = response.data
    if (body && typeof body.code === 'number') {
      if (body.code === 200) return body.data
      notify(body.code, body.msg)
      if (body.code === 401) goLogin()
      return Promise.reject(new Error(body.msg || '请求失败'))
    }
    return body
  },
  (error) => {
    const response = error.response
    const status = response ? response.status : 0
    const body = response ? response.data : null
    const msg = (body && body.msg) || CODE_MESSAGE[status] || (error.message || '网络异常')
    const url = (error.config && error.config.url) || ''
    if (status === 401) {
      ElMessage.error(msg)
      if (!isAuthApi(url)) goLogin()
    } else if (status === 403 || status === 429) {
      ElMessage.error(msg)
    } else if (status === 0) {
      ElMessage.error('网络异常，请检查后端服务是否启动')
    } else {
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

export default service
