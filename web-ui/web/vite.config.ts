import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath, URL } from 'node:url'
import { defineConfig, type Plugin } from 'vite'
import vue from '@vitejs/plugin-vue'

/** 后端地址：可用环境变量 VITE_API_TARGET 覆盖（便于本地/联调切换端口） */
const TARGET = process.env.VITE_API_TARGET || 'http://localhost:9090'

/**
 * 代理约定：
 *   /api/**     → 后端接口（剥除 /api 前缀后转发，如 /api/login → http://localhost:9090/login）
 *   /profile/** → 后端静态资源（上传文件）
 *
 * 注意：不要再代理 /login、/community 等 SPA 路由前缀，否则浏览器刷新页面时
 * 文档请求会被转给后端并返回 401/405 JSON（表现为「刷新页面提示认证失败」）。
 */
const proxy = {
  '/api': {
    target: TARGET,
    changeOrigin: true,
    rewrite: (path: string) => path.replace(/^\/api/, ''),
  },
  '/profile': { target: TARGET, changeOrigin: true },
}

/** Live2D 模型目录（体积大且含版权素材，不入 git；缺失时看板娘自动降级为静态形象） */
const LIVE2D_DIR = fileURLToPath(new URL('./src/locales/live2d', import.meta.url))

const MIME: Record<string, string> = {
  '.json': 'application/json; charset=utf-8',
  '.moc3': 'application/octet-stream',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.webp': 'image/webp',
  '.wav': 'audio/wav',
  '.mp3': 'audio/mpeg',
}

/** 递归扫描模型目录，生成动作 / 表情清单（路径相对模型根目录，统一用 / 分隔） */
function scanLive2D(dir: string, base = ''): { motions: string[]; expressions: string[] } {
  const result = { motions: [] as string[], expressions: [] as string[] }
  if (!fs.existsSync(dir)) return result
  for (const name of fs.readdirSync(dir)) {
    const abs = path.join(dir, name)
    const rel = base ? `${base}/${name}` : name
    if (fs.statSync(abs).isDirectory()) {
      const sub = scanLive2D(abs, rel)
      result.motions.push(...sub.motions)
      result.expressions.push(...sub.expressions)
    } else if (name.endsWith('.motion3.json')) {
      result.motions.push(rel)
    } else if (name.endsWith('.exp3.json')) {
      result.expressions.push(rel)
    }
  }
  return result
}

/**
 * 开发时把 /live2d/* 指向 src/locales/live2d，
 * 构建时把该目录复制到 dist/live2d（目录不存在则跳过）。
 * 同时提供 /live2d/manifest.json，前端据此注入待机动作与表情（模型自带的 model3.json 常常没写）。
 */
function live2dAssets(): Plugin {
  return {
    name: 'gal-live2d-assets',
    configureServer(server) {
      server.middlewares.use('/live2d', (req, res, next) => {
        if (!fs.existsSync(LIVE2D_DIR)) return next()
        const urlPath = decodeURIComponent((req.url || '/').split('?')[0])
        if (urlPath === '/manifest.json') {
          res.setHeader('Content-Type', 'application/json; charset=utf-8')
          res.setHeader('Cache-Control', 'no-cache')
          res.end(JSON.stringify(scanLive2D(LIVE2D_DIR)))
          return
        }
        const filePath = path.join(LIVE2D_DIR, urlPath)
        if (!filePath.startsWith(LIVE2D_DIR)) return next()
        if (!fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) return next()
        res.setHeader('Content-Type', MIME[path.extname(filePath).toLowerCase()] ?? 'application/octet-stream')
        res.setHeader('Cache-Control', 'no-cache')
        fs.createReadStream(filePath).pipe(res)
      })
    },
    closeBundle() {
      if (!fs.existsSync(LIVE2D_DIR)) return
      const outDir = path.join(fileURLToPath(new URL('.', import.meta.url)), 'dist', 'live2d')
      fs.rmSync(outDir, { recursive: true, force: true })
      fs.cpSync(LIVE2D_DIR, outDir, { recursive: true })
      fs.writeFileSync(path.join(outDir, 'manifest.json'), JSON.stringify(scanLive2D(LIVE2D_DIR)), 'utf8')
      console.log(`[live2d] 已复制模型资源到 ${outDir}`)
    },
  }
}

export default defineConfig({
  plugins: [vue(), live2dAssets()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy,
  },
  build: {
    chunkSizeWarningLimit: 1600,
  },
})
