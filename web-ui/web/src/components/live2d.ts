/**
 * Live2D 渲染封装（Cubism 4）
 *
 * - 模型目录不入 git，由 Vite 插件把 `src/locales/live2d` 映射为 `/live2d/*`，
 *   并额外提供 `/live2d/manifest.json`（扫描出的动作 / 表情清单）；
 * - 很多 VTube 导出的 `model3.json` 不写 `Motions` / `Expressions`，这里按清单自动注入，
 *   从而让立绘真正「动起来」（待机呼吸、点击动作、表情切换）；
 * - Cubism Core 放在 `public/live2d-core`，运行时按需加载；
 * - 任一环节不可用（无模型 / 无 Core / WebGL 失败）都会返回 null，
 *   由调用方降级为静态形象，保证「无立绘状态」也完整可用。
 */

/** Cubism Core 脚本地址 */
const CORE_SRC = '/live2d-core/live2dcubismcore.min.js'

/** 默认模型（可被调用方覆盖） */
export const DEFAULT_MODEL_URL = '/live2d/c_0120.model3.json'

/** 动作 / 表情清单结构（由 vite 插件生成） */
export interface Live2DManifest {
  motions: string[]
  expressions: string[]
}

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

/** 加载 Cubism Core（只加载一次，失败后允许重试） */
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

async function fetchJson<T>(url: string): Promise<T | null> {
  try {
    const resp = await fetch(url)
    if (!resp.ok) return null
    return (await resp.json()) as T
  } catch {
    return null
  }
}

/** 探测模型是否存在（缺失时不发起 Live2D 渲染） */
export async function modelExists(url: string): Promise<boolean> {
  try {
    const resp = await fetch(url, { method: 'HEAD' })
    return resp.ok
  } catch {
    return false
  }
}

/** 文件名（去掉 .motion3.json / .exp3.json 后缀） */
function fileStem(file: string): string {
  return (file.split('/').pop() ?? file).replace(/\.(motion3|exp3)\.json$/i, '')
}

export interface Live2DHandle {
  destroy(): void
  /** 视线跟随（相对画布坐标） */
  focus(x: number, y: number): void
  /** 随机播放一个动作（点击/交互时调用） */
  tap(): void
  /** 当前模型是否有动作（用于外部判断「是否会动」） */
  hasMotions(): boolean
}

export interface CreateLive2DOptions {
  modelUrl?: string
  /** 是否允许指针视线跟随，默认 true */
  followPointer?: boolean
}

/**
 * 在容器内创建 Live2D 模型
 *
 * @param container 目标容器（尺寸由 CSS 决定）
 * @param options   模型地址等选项
 */
export async function createLive2D(
  container: HTMLElement,
  options: CreateLive2DOptions = {},
): Promise<Live2DHandle | null> {
  const modelUrl = options.modelUrl ?? DEFAULT_MODEL_URL
  if (!(await modelExists(modelUrl))) return null
  if (!(await loadCubismCore())) return null

  let app: import('pixi.js').Application | null = null
  try {
    const PIXI = await import('pixi.js')
    const { Live2DModel } = await import('pixi-live2d-display/cubism4')

    const width = Math.max(container.clientWidth, 200)
    const height = Math.max(container.clientHeight, 240)

    // ---------- 读取模型配置并注入动作 / 表情 ----------
    const settings = await fetchJson<Record<string, unknown>>(modelUrl)
    if (!settings) return null
    const manifest = await fetchJson<Live2DManifest>('/live2d/manifest.json')
    const motionFiles = manifest?.motions ?? []
    const expressionFiles = manifest?.expressions ?? []
    const idleFiles = motionFiles.filter((f) => /idle|待机/i.test(fileStem(f)))
    const actionFiles = motionFiles.filter((f) => !idleFiles.includes(f))

    if (idleFiles.length || actionFiles.length) {
      const motions: Record<string, unknown[]> = {}
      if (idleFiles.length) {
        motions.Idle = idleFiles.map((file) => ({ File: file, FadeInTime: 0.8, FadeOutTime: 0.8 }))
      }
      if (actionFiles.length) {
        motions.Tap = actionFiles.slice(0, 8).map((file) => ({ File: file }))
      }
      settings.Motions = motions
    }
    if (expressionFiles.length) {
      settings.Expressions = expressionFiles.slice(0, 60).map((file) => ({ Name: fileStem(file), File: file }))
    }
    // 相对路径基于模型 json 的地址解析
    settings.url = new URL(modelUrl, window.location.href).href

    // ---------- 创建渲染器 ----------
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

    const model = await Live2DModel.from(settings, {
      autoInteract: false,
      idleMotionGroup: idleFiles.length ? 'Idle' : undefined,
    })

    // 手动驱动渲染，便于叠加「无动作时」的轻微待机摆动
    model.autoUpdate = false

    const fit = () => {
      const w = container.clientWidth || width
      const h = container.clientHeight || height
      app?.renderer.resize(w, h)
      const iw = model.internalModel.width || 1
      const ih = model.internalModel.height || 1
      const scale = Math.min(w / iw, h / ih) * 1.02
      model.scale.set(scale)
      model.anchor.set(0.5, 1)
      model.position.set(w / 2, h)
    }
    app.stage.addChild(model as unknown as import('pixi.js').DisplayObject)
    fit()

    const observer = new ResizeObserver(() => fit())
    observer.observe(container)

    // ---------- 待机摆动（没有 Idle 动作时也能「活」起来） ----------
    const coreModel = model.internalModel.coreModel as unknown as {
      setParameterValueById?: (id: string, value: number, weight?: number) => void
    }
    const setParam = (id: string, value: number) => {
      try {
        coreModel.setParameterValueById?.(id, value)
      } catch {
        /* 参数不存在时忽略 */
      }
    }
    const hasIdleMotion = idleFiles.length > 0
    let elapsed = 0
    let nextBlink = 2200 + Math.random() * 1800
    let blinkUntil = 0

    app.ticker.add(() => {
      const delta = app?.ticker.deltaMS ?? 16
      elapsed += delta
      const t = elapsed / 1000
      if (!hasIdleMotion) {
        setParam('ParamAngleX', Math.sin(t * 0.7) * 12)
        setParam('ParamAngleY', Math.sin(t * 0.9 + 1) * 6)
        setParam('ParamAngleZ', Math.sin(t * 0.5) * 3)
        setParam('ParamBodyAngleX', Math.sin(t * 0.7) * 6)
        setParam('ParamEyeBallX', Math.sin(t * 0.6) * 0.4)
        setParam('ParamEyeBallY', Math.sin(t * 0.8) * 0.3)
        setParam('ParamBreath', (Math.sin(t * 1.6) + 1) / 2)
      }
      // 补充眨眼（部分模型没有 EyeBlink 组）
      if (elapsed > nextBlink) {
        blinkUntil = elapsed + 120
        nextBlink = elapsed + 2400 + Math.random() * 2600
      }
      if (elapsed < blinkUntil) {
        setParam('ParamEyeLOpen', 0)
        setParam('ParamEyeROpen', 0)
      }
      try {
        model.update(delta)
      } catch {
        /* 模型未就绪时忽略 */
      }
    })

    // ---------- 交互：视线跟随 + 点击动作 ----------
    const onMove = (event: MouseEvent) => {
      if (options.followPointer === false) return
      const rect = container.getBoundingClientRect()
      const x = ((event.clientX - rect.left) / rect.width) * width
      const y = ((event.clientY - rect.top) / rect.height) * height
      try {
        model.focus(x, y)
      } catch {
        /* ignore */
      }
    }
    window.addEventListener('mousemove', onMove, { passive: true })

    const tap = () => {
      try {
        if (actionFiles.length) model.motion('Tap')
        if (expressionFiles.length) model.expression(Math.floor(Math.random() * expressionFiles.length))
        // 轻微跳动，让点击有反馈
        model.y -= 6
        setTimeout(() => {
          try {
            fit()
          } catch {
            /* ignore */
          }
        }, 160)
      } catch {
        /* ignore */
      }
    }
    view.addEventListener('pointerdown', tap)

    // 空闲一段时间后自动播放一次动作，保持「活着」的感觉
    let ticks = 0
    const autoAction = window.setInterval(() => {
      ticks += 1
      if (actionFiles.length && ticks % 3 === 0) {
        try {
          model.motion('Tap')
        } catch {
          /* ignore */
        }
      }
    }, 12000)

    return {
      focus: (x: number, y: number) => {
        try {
          model.focus(x, y)
        } catch {
          /* ignore */
        }
      },
      tap,
      hasMotions: () => motionFiles.length > 0,
      destroy: () => {
        window.clearInterval(autoAction)
        window.removeEventListener('mousemove', onMove)
        view.removeEventListener('pointerdown', tap)
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
