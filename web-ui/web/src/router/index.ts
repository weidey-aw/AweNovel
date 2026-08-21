import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { getToken } from '@gal/shared'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'home',
    component: () => import('@/views/HomeView.vue'),
    meta: { title: '首页' },
  },
  {
    path: '/games',
    name: 'games',
    component: () => import('@/views/GameListView.vue'),
    meta: { title: '游戏库' },
  },
  {
    path: '/game/:id',
    name: 'gameDetail',
    component: () => import('@/views/GameDetailView.vue'),
    meta: { title: '游戏详情' },
  },
  {
    path: '/articles',
    name: 'articles',
    component: () => import('@/views/ArticleListView.vue'),
    meta: { title: '文章' },
  },
  {
    path: '/article/:id',
    name: 'articleDetail',
    component: () => import('@/views/ArticleDetailView.vue'),
    meta: { title: '文章详情' },
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/RegisterView.vue'),
    meta: { title: '注册' },
  },
  {
    path: '/forget',
    name: 'forget',
    component: () => import('@/views/ForgetPwdView.vue'),
    meta: { title: '忘记密码' },
  },
  {
    path: '/user',
    name: 'user',
    component: () => import('@/views/UserCenterView.vue'),
    meta: { title: '用户中心', requiresAuth: true },
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
  document.title = title ? `${title} · AweNovel` : 'AweNovel'
  if (to.meta.requiresAuth && !getToken()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
