<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { SwitchButton } from '@element-plus/icons-vue'
import { logout } from '@gal/shared'
import { useAuthStore } from '@/stores/auth'
import { usePermissionStore } from '@/stores/permission'
import { removeDynamicRoutes } from '@/router'
import MenuItem from './components/MenuItem.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const permission = usePermissionStore()

/** 侧边栏菜单：仪表盘 + 后端下发的模块菜单 */
const menus = computed(() => permission.sidebarMenus)

const activeMenu = computed(() => route.path)

/** 当前展开的一级菜单（按路径首段推断） */
const defaultOpeneds = computed(() => {
  const segment = route.path.split('/').filter(Boolean)[0]
  return segment ? [`/${segment}`] : ['/dashboard']
})

const currentTitle = computed(() => {
  const matched = route.matched[route.matched.length - 1]
  return (matched?.meta?.title as string) || '管理后台'
})

async function handleLogout() {
  try {
    await logout()
  } catch {
    /* 忽略登出接口异常，本地状态照常清理 */
  }
  auth.clearAuth()
  permission.reset()
  removeDynamicRoutes()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<template>
  <el-container class="layout">
    <!-- 侧边栏 -->
    <el-aside width="220px" class="aside">
      <div class="logo">
        <span class="logo-icon">✦</span>
        <div class="logo-text">
          AweNovel
          <em>管理后台</em>
        </div>
      </div>
      <el-scrollbar class="menu-scroll">
        <el-menu
          :default-active="activeMenu"
          :default-openeds="defaultOpeneds"
          router
          unique-opened
          background-color="transparent"
          text-color="#a9a9c4"
          active-text-color="#ffffff"
          class="side-menu"
        >
          <menu-item v-for="item in menus" :key="item.path" :item="item" />
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container>
      <!-- 顶栏 -->
      <el-header class="header">
        <div class="header-title">{{ currentTitle }}</div>
        <div class="header-right">
          <el-dropdown trigger="click">
            <span class="user-chip">
              <el-avatar :size="30" :src="auth.userInfo?.avatar || undefined" class="user-avatar">
                {{ (auth.userInfo?.nickName || auth.userInfo?.userName || 'A').slice(0, 1) }}
              </el-avatar>
              <span class="user-name">{{ auth.userInfo?.nickName || auth.userInfo?.userName || '管理员' }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  {{ auth.roles.length ? auth.roles.join(' / ') : '无角色' }}
                </el-dropdown-item>
                <el-dropdown-item divided :icon="SwitchButton" @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容 -->
      <el-main class="main">
        <router-view v-slot="{ Component }">
          <component :is="Component" />
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout {
  height: 100vh;
}

.aside {
  background: linear-gradient(180deg, #1e1e30, #161624);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 18px;
  color: #fff;
  flex-shrink: 0;
}

.logo-icon {
  font-size: 22px;
  background: var(--grad-main);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.logo-text {
  font-size: 15px;
  font-weight: 700;
  line-height: 1.3;
}

.logo-text em {
  font-style: normal;
  display: block;
  font-size: 11px;
  font-weight: 400;
  color: #8a8aa8;
}

.menu-scroll {
  flex: 1;
  overflow-y: auto;
}

.side-menu {
  border-right: none;
  padding-bottom: 18px;
}

.side-menu :deep(.el-menu-item),
.side-menu :deep(.el-sub-menu__title) {
  height: 46px;
  margin: 2px 10px;
  border-radius: 8px;
}

.side-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(120deg, #8b5cf6, #ec4899);
  color: #fff !important;
  box-shadow: 0 4px 14px rgba(139, 92, 246, 0.4);
}

.side-menu :deep(.el-menu-item:hover),
.side-menu :deep(.el-sub-menu__title:hover) {
  background: rgba(139, 92, 246, 0.18);
  color: #fff;
}

.side-menu :deep(.el-sub-menu .el-menu-item) {
  margin-left: 16px;
  min-width: auto;
}

.header {
  background: #fff;
  border-bottom: 1px solid #ececf5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
}

.header-title {
  font-size: 16px;
  font-weight: 700;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 10px;
  border-radius: 999px;
}

.user-chip:hover {
  background: #f3f3fa;
}

.user-avatar {
  background: var(--grad-main);
  color: #fff;
  font-weight: 700;
}

.user-name {
  font-size: 13.5px;
}

.main {
  background: #f3f4f9;
  padding: 0;
  overflow-y: auto;
}
</style>
