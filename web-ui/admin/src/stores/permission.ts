import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getRouters } from '@gal/shared'
import type { RouterVo } from '@gal/shared'

/** 侧边栏菜单项（由后端路由树转换而来，path 为完整路径） */
export interface SideMenuItem {
  path: string
  title: string
  icon?: string
  hidden?: boolean
  children?: SideMenuItem[]
}

/** 仪表盘：前端静态路由，固定排在导航第一位 */
export const DASHBOARD_MENU: SideMenuItem = {
  path: '/dashboard',
  title: '仪表盘',
  icon: 'dashboard',
}

/**
 * 把后端 /getRouters 返回的 RouterVo 树转换为侧边栏菜单树。
 * @param routes 后端路由
 * @param parentPath 父级完整路径
 */
function toMenuItems(routes: RouterVo[], parentPath = ''): SideMenuItem[] {
  const items: SideMenuItem[] = []
  for (const route of routes) {
    if (route.hidden) continue
    const path = resolvePath(parentPath, route.path)
    const children = route.children?.length ? toMenuItems(route.children, path) : undefined
    const visibleChildren = children?.filter((c) => !c.hidden)
    // 目录下的子项全部隐藏时，整个目录不展示
    if (!route.meta?.title) continue
    if (children && !visibleChildren?.length) continue
    items.push({
      path,
      title: route.meta?.title || path,
      icon: route.meta?.icon,
      children: visibleChildren,
    })
  }
  return items
}

/** 拼接父子路径为完整路径 */
export function resolvePath(parent: string, path: string): string {
  if (!path) return parent || '/'
  if (path.startsWith('/')) return path.replace(/\/+$/, '') || '/'
  if (!parent) return `/${path}`.replace(/\/+$/, '') || '/'
  return `${parent.replace(/\/+$/, '')}/${path}`
}

export const usePermissionStore = defineStore('permission', () => {
  /** 后端返回的原始路由（已过滤隐藏项前的数据） */
  const routes = ref<RouterVo[]>([])
  /** 是否已加载动态路由 */
  const loaded = ref(false)

  /** 侧边栏菜单：仪表盘 + 后端菜单 */
  const sidebarMenus = computed<SideMenuItem[]>(() => [
    DASHBOARD_MENU,
    ...toMenuItems(routes.value),
  ])

  /** 平铺所有可访问的完整路径（用于权限校验/调试） */
  const flatPaths = computed<string[]>(() => {
    const out: string[] = []
    const walk = (items: SideMenuItem[]) => {
      for (const item of items) {
        out.push(item.path)
        if (item.children?.length) walk(item.children)
      }
    }
    walk(sidebarMenus.value)
    return out
  })

  async function generateRoutes(): Promise<RouterVo[]> {
    const res = await getRouters()
    routes.value = res.data ?? []
    loaded.value = true
    return routes.value
  }

  function reset() {
    routes.value = []
    loaded.value = false
  }

  return { routes, loaded, sidebarMenus, flatPaths, generateRoutes, reset }
})
