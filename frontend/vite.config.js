import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

const srcDir = fileURLToPath(new URL('./src', import.meta.url))
// Vue 3.2.38 兼容层，见 src/shims/vue-compat.js 顶部说明
const vueCompat = fileURLToPath(new URL('./src/shims/vue-compat.js', import.meta.url))

// A 项目前端（端口 5173）：/api 全部代理到 8080 后端，开发期不跨域
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: [
      // 精确匹配 'vue'，避免把 'vue-router'、'vue-demi' 之类一起改掉
      { find: /^vue$/, replacement: vueCompat },
      { find: 'vue-real', replacement: 'vue/dist/vue.runtime.esm-bundler.js' },
      { find: '@', replacement: srcDir }
    ]
  },
  server: {
    port: 5173,
    host: '127.0.0.1',
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    chunkSizeWarningLimit: 2000
  }
})