import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { RouteLocationNormalizedLoaded } from 'vue-router'

/** 标签卡 */
export interface TagView {
  /** 路由路径（唯一标识） */
  path: string
  /** 完整路径（含 query） */
  fullPath: string
  /** 标签标题 */
  title: string
  /** 路由名称 */
  name?: string
  /** 固定标签，不可关闭 */
  affix?: boolean
}

/** 固定标签：仪表盘 */
const AFFIX_TAG: TagView = {
  path: '/dashboard',
  fullPath: '/dashboard',
  title: '仪表盘',
  affix: true,
}

export const useTagsViewStore = defineStore('tagsView', () => {
  const visitedViews = ref<TagView[]>([{ ...AFFIX_TAG }])

  const tags = computed(() => visitedViews.value)

  /** 访问路由时追加标签（已存在则更新 fullPath） */
  function addView(route: RouteLocationNormalizedLoaded): void {
    const title = route.meta?.title as string | undefined
    if (!title || route.path === '/login') return
    const existed = visitedViews.value.find((view) => view.path === route.path)
    if (existed) {
      existed.fullPath = route.fullPath
      existed.title = title
      return
    }
    visitedViews.value.push({
      path: route.path,
      fullPath: route.fullPath,
      title,
      name: route.name ? String(route.name) : undefined,
    })
  }

  /** 关闭标签 */
  function delView(path: string): void {
    visitedViews.value = visitedViews.value.filter((view) => view.path !== path || view.affix)
  }

  /** 关闭其他标签 */
  function delOthers(path: string): void {
    visitedViews.value = visitedViews.value.filter((view) => view.path === path || view.affix)
  }

  /** 关闭全部标签（保留固定标签） */
  function delAll(): void {
    visitedViews.value = visitedViews.value.filter((view) => view.affix)
  }

  /** 重置（退出登录时调用） */
  function reset(): void {
    visitedViews.value = [{ ...AFFIX_TAG }]
  }

  return { visitedViews, tags, addView, delView, delOthers, delAll, reset }
})
