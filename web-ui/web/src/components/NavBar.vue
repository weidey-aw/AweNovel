<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, User, SwitchButton, Bell, Setting } from '@element-plus/icons-vue'
import { logout } from '@gal/shared'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 管理端地址（生产可经 VITE_ADMIN_URL 配置） */
const ADMIN_URL = (import.meta.env.VITE_ADMIN_URL as string | undefined) || 'http://localhost:5174'

const keyword = ref('')
const menus = [
  { path: '/', label: '首页' },
  { path: '/games', label: '游戏库' },
  { path: '/articles', label: '文章' },
]

function isActive(path: string) {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

function goSearch() {
  router.push({ path: '/games', query: keyword.value.trim() ? { keyword: keyword.value.trim() } : {} })
}

function openAdmin() {
  window.open(ADMIN_URL, '_blank')
}

async function handleLogout() {
  try {
    await logout()
  } catch {
    /* 忽略后端登出失败 */
  }
  userStore.clearAuth()
  ElMessage.success('已退出登录')
  router.push('/')
}
</script>

<template>
  <header class="nav">
    <div class="nav-inner">
      <router-link to="/" class="logo">
        <span class="logo-icon">✦</span>
        <span class="logo-text">Awe<em>Novel</em></span>
      </router-link>

      <nav class="menu">
        <router-link
          v-for="m in menus"
          :key="m.path"
          :to="m.path"
          class="menu-item"
          :class="{ active: isActive(m.path) }"
        >
          {{ m.label }}
        </router-link>
      </nav>

      <div class="nav-right">
        <el-input
          v-model="keyword"
          class="nav-search"
          placeholder="搜索游戏 / 文章"
          clearable
          size="default"
          @keyup.enter="goSearch"
          @clear="goSearch"
        >
          <template #suffix>
            <el-icon class="search-icon" @click="goSearch"><Search /></el-icon>
          </template>
        </el-input>

        <template v-if="userStore.isLoggedIn">
          <el-badge :value="userStore.unread" :hidden="!userStore.unread" class="nav-badge">
            <el-button text circle @click="router.push('/user?tab=messages')">
              <el-icon :size="18"><Bell /></el-icon>
            </el-button>
          </el-badge>
          <el-dropdown trigger="click">
            <span class="user-chip">
              <el-avatar :size="30" :src="userStore.userInfo?.avatar || undefined" class="user-avatar">
                {{ (userStore.userInfo?.nickName || userStore.userInfo?.userName || 'U').slice(0, 1) }}
              </el-avatar>
              <span class="user-name">{{ userStore.userInfo?.nickName || userStore.userInfo?.userName || '用户' }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-if="userStore.isAdmin" :icon="Setting" @click="openAdmin">后台管理</el-dropdown-item>
                <el-dropdown-item :icon="User" @click="router.push('/user')">用户中心</el-dropdown-item>
                <el-dropdown-item :icon="SwitchButton" divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>

        <template v-else>
          <el-button text class="nav-link-btn" @click="router.push('/login')">登录</el-button>
          <el-button class="grad-btn" @click="router.push('/register')">注册</el-button>
        </template>
      </div>
    </div>
  </header>
</template>

<style scoped>
.nav {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(14, 14, 22, 0.85);
  backdrop-filter: blur(14px);
  border-bottom: 1px solid var(--border-glow);
}

.nav-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  height: 62px;
  display: flex;
  align-items: center;
  gap: 28px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 1px;
  flex-shrink: 0;
}

.logo-icon {
  background: var(--grad-main);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  font-size: 22px;
}

.logo-text em {
  font-style: normal;
  background: var(--grad-main);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.menu {
  display: flex;
  gap: 6px;
  flex: 1;
}

.menu-item {
  padding: 8px 16px;
  border-radius: 8px;
  color: var(--text-sub);
  font-size: 15px;
  transition: all 0.2s;
}

.menu-item:hover {
  color: var(--text-main);
  background: rgba(139, 92, 246, 0.1);
}

.menu-item.active {
  color: #fff;
  background: var(--grad-main);
  box-shadow: 0 4px 14px rgba(139, 92, 246, 0.35);
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.nav-search {
  width: 200px;
}

.search-icon {
  cursor: pointer;
  color: var(--text-sub);
}

.nav-badge :deep(.el-badge__content) {
  border: none;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 999px;
  transition: background 0.2s;
}

.user-chip:hover {
  background: rgba(139, 92, 246, 0.12);
}

.user-avatar {
  background: var(--grad-main);
  color: #fff;
  font-weight: 700;
}

.user-name {
  max-width: 90px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}

.nav-link-btn {
  color: var(--text-sub);
}

@media (max-width: 768px) {
  .nav-search {
    display: none;
  }
  .nav-inner {
    gap: 12px;
  }
  .menu-item {
    padding: 8px 10px;
    font-size: 14px;
  }
}
</style>
