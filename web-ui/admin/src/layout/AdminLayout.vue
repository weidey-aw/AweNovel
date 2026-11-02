<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Moon, Sunny, SwitchButton } from '@element-plus/icons-vue'
import { getStoredTheme, logout, THEME_LABELS, toggleTheme, type ThemeName } from '@gal/shared'
import { useAuthStore } from '@/stores/auth'
import { usePermissionStore } from '@/stores/permission'
import { useTagsViewStore } from '@/stores/tagsView'
import { removeDynamicRoutes } from '@/router'
import MenuItem from './components/MenuItem.vue'
import TagsView from './components/TagsView.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const permission = usePermissionStore()
const tagsView = useTagsViewStore()

/** 主题：白蓝（默认） / 暗夜色 */
const theme = ref<ThemeName>(getStoredTheme())
const menuTextColor = computed(() => (theme.value === 'dark' ? '#a9a9c4' : '#4a5c7a'))
const menuActiveTextColor = computed(() => (theme.value === 'dark' ? '#ffffff' : '#1d4fd0'))

function switchTheme() {
  theme.value = toggleTheme()
  ElMessage.success(`已切换为「${THEME_LABELS[theme.value]}」主题`)
}

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

/** 视图刷新：改变 key 强制重建当前页面 */
const refreshSeq = ref(0)
const viewKey = computed(() => `${route.fullPath}#${refreshSeq.value}`)

function refreshView() {
  refreshSeq.value += 1
}

async function handleLogout() {
  try {
    await logout()
  } catch {
    /* 忽略登出接口异常，本地状态照常清理 */
  }
  auth.clearAuth()
  permission.reset()
  tagsView.reset()
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
          :text-color="menuTextColor"
          :active-text-color="menuActiveTextColor"
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
          <el-tooltip :content="`主题：${THEME_LABELS[theme]}（点击切换）`" placement="bottom">
            <el-button text circle aria-label="切换主题" @click="switchTheme">
              <el-icon :size="18">
                <Sunny v-if="theme === 'dark'" />
                <Moon v-else />
              </el-icon>
            </el-button>
          </el-tooltip>
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

      <!-- 路由标签卡 -->
      <tags-view @refresh="refreshView" />

      <!-- 主内容 -->
      <el-main class="main">
        <router-view v-slot="{ Component }">
          <component :is="Component" :key="viewKey" />
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
  background: var(--sidebar-bg);
  border-right: 1px solid var(--sidebar-border);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 18px;
  color: var(--logo-color);
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
  color: var(--logo-sub);
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
  background: var(--grad-main);
  color: #fff !important;
  box-shadow: var(--shadow-menu-active);
}

.side-menu :deep(.el-menu-item:hover),
.side-menu :deep(.el-sub-menu__title:hover) {
  background: var(--sidebar-hover-bg);
  color: var(--sidebar-text-active);
}

.side-menu :deep(.el-sub-menu .el-menu-item) {
  margin-left: 16px;
  min-width: auto;
}

.header {
  background: var(--header-bg);
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
}

.header-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-main);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 4px;
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
  background: var(--bg-soft);
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
  background: var(--main-bg);
  padding: 0;
  overflow-y: auto;
}
</style>
