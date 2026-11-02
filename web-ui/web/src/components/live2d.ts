/**
 * Live2D 渲染封装（Cubism 4）
 *
 * - 模型目录不入 git，由 Vite 插件把 `src/locales/live2d` 映射为 `/live2d/*`；
 * - Cubism Core 放在 `public/live2d-core`，运行时按需加载；
 * - 任一环节不可用（无模型 / 无 Core / WebGL 失败）都会返回 null，
 *   由调用方降级为静态形象，保证「无立绘状态」也完整可用。
 */

/** Cubism Core 脚本地址 */
const CORE_SRC = '/live2d-core/live2dcubismcore.min.js'

let corePromise: Promise<boolean> | null = null

type CubismWindow = Window & { Live2DCubismCore?: unknown }

function loadScript(src: string): Promise<void> {
  return new Promise((resolve, reject) => {
    const existing = document.querySelector<HTMLScriptElement>(`script[data-src="${src}"]`)
    if (existing) {
      if (existing.dataset.loaded === '1') resolve()
      else {
        existing.addEventListener('load', () => resolve(), { once: true })
        existing.addEventListener('error', () => reject(new Error('load failed')), { once: true })
      }
      return
    }
    const script = document.createElement('script')
    script.src = src
    script.async = true
    script.dataset.src = src
    script.addEventListener('load', () => {
      script.dataset.loaded = '1'
      resolve()
    })
    script.addEventListener('error', () => reject(new Error('load failed')))
    document.head.appendChild(script)
  })
}

/** 加载 Cubism Core（只加载一次） */
export function loadCubismCore(): Promise<boolean> {
  if ((window as CubismWindow).Live2DCubismCore) return Promise.resolve(true)
  if (!corePromise) {
    corePromise = loadScript(CORE_SRC)
      .then(() => Boolean((window as CubismWindow).Live2DCubismCore))
      .catch(() => {
        corePromise = null
        return false
      })
  }
  return corePromise
}

/** 探测模型是否存在（缺失时不发起 Live2D 渲染） */
export async function modelExists(url: string): Promise<boolean> {
  try {
    const resp = await fetch(url, { method: 'HEAD' })
    if (!resp.ok) return false
    return (resp.headers.get('content-type') ?? '').includes('json')
  } catch {
    return false
  }
}

export interface Live2DHandle {
  destroy(): void
  /** 视线跟随（相对画布坐标） */
  focus(x: number, y: number): void
}

/**
 * 在容器内创建 Live2D 模型
 *
 * @param container 目标容器（尺寸由 CSS 决定）
 * @param modelUrl  model3.json 地址
 */
export async function createLive2D(container: HTMLElement, modelUrl: string): Promise<Live2DHandle | null> {
  if (!(await modelExists(modelUrl))) return null
  if (!(await loadCubismCore())) return null

  let app: import('pixi.js').Application | null = null
  try {
    const PIXI = await import('pixi.js')
    const { Live2DModel } = await import('pixi-live2d-display/cubism4')

    const width = Math.max(container.clientWidth, 240)
    const height = Math.max(container.clientHeight, 160)

    app = new PIXI.Application({
      transparent: true,
      width,
      height,
      antialias: true,
      autoDensity: true,
      resolution: Math.min(window.devicePixelRatio || 1, 2),
      backgroundAlpha: 0,
    })
    const view = app.view as unknown as HTMLCanvasElement
    view.style.width = '100%'
    view.style.height = '100%'
    view.style.display = 'block'
    container.appendChild(view)

    const model = await Live2DModel.from(modelUrl, { autoInteract: false })

    // 等比缩放并按「底部居中」摆放
    const fit = () => {
      const w = container.clientWidth || width
      const h = container.clientHeight || height
      app?.renderer.resize(w, h)
      // 注意：model.width 会随 scale 变化，必须用 internalModel 的原始尺寸计算基准缩放
      const iw = model.internalModel.width || 1
      const ih = model.internalModel.height || 1
      const baseScale = Math.min(w / iw, h / ih)
      model.scale.set(baseScale * 1.08)
      model.anchor.set(0.5, 1)
      model.position.set(w / 2, h)
    }
    app.stage.addChild(model as unknown as import('pixi.js').DisplayObject)
    fit()

    const observer = new ResizeObserver(() => fit())
    observer.observe(container)

    // 视线跟随：指针移动时头部/眼睛轻微朝向光标
    const onMove = (event: MouseEvent) => {
      const rect = container.getBoundingClientRect()
      const x = ((event.clientX - rect.left) / rect.width) * width
      const y = ((event.clientY - rect.top) / rect.height) * height
      try {
        model.focus(x, y)
      } catch {
        /* 模型未就绪时忽略 */
      }
    }
    window.addEventListener('mousemove', onMove, { passive: true })

    return {
      focus: (x: number, y: number) => {
        try {
          model.focus(x, y)
        } catch {
          /* ignore */
        }
      },
      destroy: () => {
        window.removeEventListener('mousemove', onMove)
        observer.disconnect()
        try {
          model.destroy()
        } catch {
          /* ignore */
        }
        try {
          app?.destroy(true, { children: true })
        } catch {
          /* ignore */
        }
        app = null
        view.remove()
      },
    }
  } catch (err) {
    console.warn('[live2d] 无法初始化立绘，将使用静态形象：', err)
    try {
      app?.destroy(true, { children: true })
    } catch {
      /* ignore */
    }
    container.innerHTML = ''
    return null
  }
}
