import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getInfo, getToken, removeToken, setToken } from '@gal/shared'
import type { UserInfo } from '@gal/shared'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(getToken())
  const userInfo = ref<UserInfo | null>(null)

  const isLoggedIn = computed(() => !!token.value)

  function saveToken(value: string) {
    token.value = value
    setToken(value)
  }

  function clearAuth() {
    token.value = null
    userInfo.value = null
    removeToken()
  }

  async function fetchUserInfo() {
    if (!token.value) return
    try {
      const res = (await getInfo()) as unknown as { user?: UserInfo; data?: { user?: UserInfo } }
      userInfo.value = (res?.user ?? res?.data?.user) ?? null
    } catch {
      userInfo.value = null
    }
  }

  return { token, userInfo, isLoggedIn, saveToken, clearAuth, fetchUserInfo }
})
