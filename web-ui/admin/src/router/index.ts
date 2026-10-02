import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { getToken } from '@gal/shared'
import type { RouterVo } from '@gal/shared'
import AdminLayout from '@/layout/AdminLayout.vue'
import ParentView from '@/components/ParentView.vue'
import InnerLink from '@/components/InnerLink.vue'
import NotFound from '@/views/error/not-found.vue'
import { useAuthStore } from '@/stores/auth'
import { usePermissionStore } from '@/stores/permission'

/**
 * 静态路由（前端固定）
 * 仪表盘固定在导航第一位，其余菜单全部由后端 /getRouters 动态下发。
 */
export const constantRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/',
    component: AdminLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '仪表盘', icon: 'dashboard' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'notFound',
    component: NotFound,
    meta: { title: '页面不存在' },
  },
]

/** 所有视图组件（用于把后端 component 字符串解析为组件；排除 error 兜底页） */
const viewModules = import.meta.glob(['../views/**/*.vue', '!../views/error/**'])

/** 后端 component 字符串 → 组件 */
function resolveComponent(component?: string) {
  if (!component) return undefined
  if (component === 'Layout') return AdminLayout
  if (component === 'ParentView') return ParentView
  if (component === 'InnerLink') return InnerLink
  return viewModules[`../views/${component}.vue`]
}

/** 拼接完整路径 */
function joinPath(parent: string, path: string): string {
  if (!path) return parent || '/'
  if (path.startsWith('/')) return path
  if (!parent || parent === '/') return `/${path}`
  return `${parent.replace(/\/+$/, '')}/${path}`
}

interface MutableRoute {
  path: string
  name?: string
  component?: unknown
  redirect?: string
  meta: Record<string, unknown>
  children: MutableRoute[]
}

/**
 * 后端路由树 → 路由记录
 * 约定（与后端 SysMenuServiceImpl.buildMenus 对齐）：
 *   - 一级目录：component = 'Layout'，children 为子菜单
 *   - 二级菜单：path 为相对路径，component 为视图路径（如 system/user/index）
 *   - redirect = 'noRedirect'：转换为「重定向到第一个子路由」
 *   - 组件文件缺失：渲染缺失提示页并在 meta 中标记，避免整棵路由树失效
 */
export function buildRoutes(routes: RouterVo[]): RouteRecordRaw[] {
  const walk = (list: RouterVo[]): MutableRoute[] =>
    list.map((route) => {
      const meta: Record<string, unknown> = {
        title: route.meta?.title,
        icon: route.meta?.icon,
        hidden: route.hidden === true,
        noCache: route.meta?.noCache === true,
        link: route.meta?.link,
      }

      const record: MutableRoute = {
        path: route.path,
        name: route.name || undefined,
        meta,
        children: [],
      }

      const component = resolveComponent(route.component)
      if (component) {
        record.component = component
      } else if (route.component) {
        record.component = NotFound
        meta.missingComponent = route.component
      }

      if (route.children?.length) {
        record.children = walk(route.children)
        if (!route.redirect || route.redirect === 'noRedirect') {
          const first = route.children.find((child) => !child.hidden)
          if (first) record.redirect = joinPath(route.path, first.path)
        } else {
          record.redirect = route.redirect
        }
      }

      return record
    })

  return walk(routes) as unknown as RouteRecordRaw[]
}

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes,
})

/** 已注册的动态路由名（登出时移除，避免重复注册与权限残留） */
let dynamicRouteNames: string[] = []

export function addDynamicRoutes(routes: RouteRecordRaw[]): void {
  dynamicRouteNames = []
  for (const route of routes) {
    router.addRoute(route)
    if (route.name) dynamicRouteNames.push(String(route.name))
  }
}

export function removeDynamicRoutes(): void {
  for (const name of dynamicRouteNames) {
    if (router.hasRoute(name)) router.removeRoute(name)
  }
  dynamicRouteNames = []
}

router.beforeEach(async (to) => {
  const title = to.meta.title as string | undefined
  document.title = title ? `${title} · AweNovel 管理后台` : 'AweNovel 管理后台'

  const token = getToken()

  // 登录页：已登录直接进入后台
  if (to.path === '/login') {
    return token ? { path: '/' } : true
  }

  // 未登录：跳登录页并记录来源
  if (!token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  const auth = useAuthStore()
  const permission = usePermissionStore()

  // 首次进入：拉取用户信息 + 后端菜单，注册动态路由后重新导航
  if (!permission.loaded) {
    try {
      await auth.fetchUserInfo()
      addDynamicRoutes(buildRoutes(await permission.generateRoutes()))
      return { ...to, replace: true }
    } catch (err) {
      auth.clearAuth()
      permission.reset()
      removeDynamicRoutes()
      const msg = err instanceof Error ? err.message : '获取菜单失败'
      return { path: '/login', query: { redirect: to.fullPath, error: msg } }
    }
  }

  return true
})

export default router
