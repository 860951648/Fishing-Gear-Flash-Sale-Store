/**
 * Vue 版本兼容层（被 vite.config.js 的 resolve.alias 里 `vue` -> 本文件 接管）。
 *
 * 背景：项目锁 Vue 3.2.38，而 element-plus 2.14.3 / pinia 2.1.x 的 ESM 产物会从
 * 'vue' 里具名导入 Vue 3.3+ 才提供的能力（toValue、hasInjectionContext），
 * Rollup 打包时会报 "xxx is not exported by vue"，build 直接失败。
 * 处理：不牺牲锁死的 Vue 版本，也不降级 Element Plus，这里按 Vue 3.3 的语义补两个等价实现，
 * 其余 API 原样透传真实 Vue，保证和业务代码用的是同一个 Vue 运行时实例。
 */
export * from 'vue-real'

import { isRef } from 'vue-real'

// 与 Vue 3.3 toValue 等价：ref -> .value，函数 -> 调用取值，其余原样返回
export function toValue(source) {
  if (typeof source === 'function') return source()
  return isRef(source) ? source.value : source
}

// Vue 3.3 用它判断能否安全 inject；Vue 3.2 只有在组件上下文里才有 inject，这里放行由 Vue 自身兜底
export function hasInjectionContext() {
  return true
}