/**
 * 菜单图标解析：把后端 sys_menu.icon 的值映射为 Element Plus 图标组件。
 *
 * 支持三种写法：
 *   1) 语义别名（推荐，见 ALIAS）：system / monitor / operation
 *   2) kebab-case（Element Plus 图标名）：user-filled → UserFilled、video-play → VideoPlay
 *   3) PascalCase：DataLine、Setting
 */
import type { Component } from 'vue'
import * as ElementPlusIcons from '@element-plus/icons-vue'

/** 语义别名 → Element Plus 图标名 */
const ALIAS: Record<string, string> = {
  system: 'Setting',
  monitor: 'Monitor',
  operation: 'Star',
  dashboard: 'DataLine',
}

const FALLBACK = 'Menu'

const icons = ElementPlusIcons as unknown as Record<string, Component>

function toPascal(name: string): string {
  return name
    .split(/[-_\s]+/)
    .filter(Boolean)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join('')
}

/** 解析图标名称为组件；无法识别时返回默认图标 */
export function iconOf(name?: string): Component {
  if (!name) return icons[FALLBACK]
  const alias = ALIAS[name.toLowerCase()]
  if (alias && icons[alias]) return icons[alias]
  const pascal = toPascal(name)
  if (icons[pascal]) return icons[pascal]
  // 已经是 PascalCase 时 toPascal 会改变大小写（如 DataLine → DataLine 保持不变，OK）
  if (icons[name]) return icons[name]
  return icons[FALLBACK]
}
