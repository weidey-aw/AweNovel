import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const TARGET = 'http://localhost:9090'

/**
 * 代理约定：
 *   /api/**    → 后端接口（剥离 /api 前缀后转发，如 /api/login → http://localhost:9090/login）
 *   /profile/**→ 后端静态资源（上传文件）
 *
 * 注意：不要再代理 /login、/system 等 SPA 路由前缀，否则浏览器刷新页面时
 * 文档请求会被转给后端并返回 401 JSON（表现为「刷新页面提示认证失败」）。
 */
const proxy = {
  '/api': {
    target: TARGET,
    changeOrigin: true,
    rewrite: (path: string) => path.replace(/^\/api/, ''),
  },
  '/profile': { target: TARGET, changeOrigin: true },
  // 数据监控（Druid 控制台，iframe 内嵌）
  '/druid': { target: TARGET, changeOrigin: true },
}

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    host: '0.0.0.0',
    port: 5174,
    proxy,
  },
  build: {
    chunkSizeWarningLimit: 1600,
  },
})
