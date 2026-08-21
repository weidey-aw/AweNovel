import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  getInfo,
  getToken,
  getUserProfile,
  getUnreadCount,
  removeToken,
  setToken,
} from '@gal/shared'
import type { UserInfo, UserProfile } from '@gal/shared'

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(getToken())
  const userInfo = ref<UserInfo | null>(null)
  const profile = ref<UserProfile | null>(null)
  const unread = ref(0)
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])

  const isLoggedIn = computed(() => !!token.value)

  /** 是否为管理员（角色 admin / 权限通配 / 超级管理员用户ID） */
  const isAdmin = computed(() => {
    if (!token.value) return false
    if (userInfo.value?.userId === 1) return true
    return (
      roles.value.includes('admin') ||
      roles.value.includes('*') ||
      permissions.value.includes('*:*:*')
    )
  })

  function saveToken(value: string) {
    token.value = value
    setToken(value)
  }

  function clearAuth() {
    token.value = null
    userInfo.value = null
    profile.value = null
    unread.value = 0
    roles.value = []
    permissions.value = []
    removeToken()
  }

  async function fetchUserInfo() {
    if (!token.value) return
    try {
      const res = (await getInfo()) as unknown as {
        user?: UserInfo
        roles?: string[]
        permissions?: string[]
        data?: { user?: UserInfo; roles?: string[]; permissions?: string[] }
      }
      userInfo.value = (res?.user ?? res?.data?.user) ?? null
      roles.value = (res?.roles ?? res?.data?.roles ?? []) as string[]
      permissions.value = (res?.permissions ?? res?.data?.permissions ?? []) as string[]
    } catch {
      userInfo.value = null
    }
  }

  async function fetchProfile() {
    if (!token.value) return
    try {
      const res = (await getUserProfile()) as unknown as { data?: UserProfile } | UserProfile
      profile.value = ('data' in res && res.data ? res.data : res) as UserProfile
    } catch {
      profile.value = null
    }
  }

  async function fetchUnread() {
    if (!token.value) return
    try {
      const res = (await getUnreadCount()) as unknown as { data?: number } | number
      const n = typeof res === 'number' ? res : res?.data
      unread.value = Number(n) || 0
    } catch {
      unread.value = 0
    }
  }

  return {
    token,
    userInfo,
    profile,
    unread,
    roles,
    permissions,
    isLoggedIn,
    isAdmin,
    saveToken,
    clearAuth,
    fetchUserInfo,
    fetchProfile,
    fetchUnread,
  }
})
