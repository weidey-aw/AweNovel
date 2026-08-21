import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const TARGET = 'http://localhost:9090'

// 后端 context-path 为 `/`，真实接口路径即 /login、/community/...，开发环境精确代理到 9090
const PROXY_PATHS = [
  '/login',
  '/register',
  '/captchaImage',
  '/getInfo',
  '/getRouters',
  '/logout',
  '/community',
  '/ai',
  '/system',
  '/monitor',
  '/profile',
]

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: Object.fromEntries(
      PROXY_PATHS.map((p) => [p, { target: TARGET, changeOrigin: true }]),
    ),
  },
  build: {
    chunkSizeWarningLimit: 1600,
  },
})
