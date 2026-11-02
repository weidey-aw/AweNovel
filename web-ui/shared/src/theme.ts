/**
 * 主题切换（前台 + 后台共用）
 *
 * - `light` 白蓝主题（默认）
 * - `dark`  暗夜色（原黑紫主题）
 *
 * 通过给 `<html>` 设置 `data-theme` 生效，同时切换 `dark` 类以启用 Element Plus 暗色变量。
 */

export type ThemeName = 'light' | 'dark'

export const THEME_STORAGE_KEY = 'gal-theme'

export const THEME_LABELS: Record<ThemeName, string> = {
  light: '白蓝',
  dark: '暗夜色',
}

/** 读取已保存的主题（默认白蓝） */
export function getStoredTheme(): ThemeName {
  try {
    const saved = localStorage.getItem(THEME_STORAGE_KEY)
    if (saved === 'light' || saved === 'dark') return saved
  } catch {
    /* 隐私模式等场景忽略 */
  }
  return 'light'
}

/** 应用主题并持久化 */
export function applyTheme(theme: ThemeName, persist = true): void {
  const root = document.documentElement
  root.dataset.theme = theme
  root.classList.toggle('dark', theme === 'dark')
  if (persist) {
    try {
      localStorage.setItem(THEME_STORAGE_KEY, theme)
    } catch {
      /* ignore */
    }
  }
}

/** 切换主题，返回切换后的主题 */
export function toggleTheme(): ThemeName {
  const next: ThemeName = getStoredTheme() === 'dark' ? 'light' : 'dark'
  applyTheme(next)
  return next
}

/** 首屏初始化（可以在 main.ts 里同步调用，避免主题闪烁） */
export function initTheme(): ThemeName {
  const theme = getStoredTheme()
  applyTheme(theme, false)
  return theme
}
