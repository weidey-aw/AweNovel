import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { getToken } from '@gal/shared'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/',
    component: () => import('@/layout/AdminLayout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { title: '仪表盘', icon: 'DataLine' },
      },
      {
        path: 'games',
        name: 'games',
        component: () => import('@/views/GameManageView.vue'),
        meta: { title: '游戏管理', icon: 'VideoGame' },
      },
      {
        path: 'brands',
        name: 'brands',
        component: () => import('@/views/BrandManageView.vue'),
        meta: { title: '会社管理', icon: 'OfficeBuilding' },
      },
      {
        path: 'tags',
        name: 'tags',
        component: () => import('@/views/TagManageView.vue'),
        meta: { title: '标签管理', icon: 'PriceTag' },
      },
      {
        path: 'resources',
        name: 'resources',
        component: () => import('@/views/ResourceManageView.vue'),
        meta: { title: '资源管理', icon: 'FolderOpened' },
      },
      {
        path: 'articles',
        name: 'articles',
        component: () => import('@/views/ArticleManageView.vue'),
        meta: { title: '文章管理', icon: 'Document' },
      },
      {
        path: 'comments',
        name: 'comments',
        component: () => import('@/views/CommentManageView.vue'),
        meta: { title: '评论管理', icon: 'ChatDotRound' },
      },
      {
        path: 'review',
        name: 'review',
        component: () => import('@/views/ReviewManageView.vue'),
        meta: { title: '内容审核', icon: 'Finished' },
      },
      {
        path: 'users',
        name: 'users',
        component: () => import('@/views/UserManageView.vue'),
        meta: { title: '用户管理', icon: 'User' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const title = to.meta.title as string | undefined
  document.title = title ? `${title} · 管理后台` : 'AweNovel 管理后台'
  if (to.matched.some((r) => r.meta.requiresAuth) && !getToken()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
