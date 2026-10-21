<script setup lang="ts">
import { onMounted } from 'vue'
import NavBar from '@/components/NavBar.vue'
import Footer from '@/components/Footer.vue'
import ChatMaid from '@/components/ChatMaid.vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

/**
 * 刷新页面后仅剩 localStorage 中的 token，需要重新拉取用户信息，
 * 否则导航栏会表现成「未登录」（昵称 / 头像 / 管理入口丢失）。
 */
onMounted(() => {
  if (!userStore.isLoggedIn) return
  userStore.fetchUserInfo()
  userStore.fetchProfile()
  userStore.fetchUnread()
})
</script>

<template>
  <div class="app-shell">
    <NavBar />
    <main class="app-main">
      <router-view />
    </main>
    <Footer />
    <ChatMaid />
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.app-main {
  flex: 1;
  width: 100%;
}
</style>
