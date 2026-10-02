import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getInfo, getToken, removeToken, setToken } from '@gal/shared'
import type { UserInfo } from '@gal/shared'

/** 登录用户信息 + 角色 + 权限（数据来自 GET /getInfo） */
export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(getToken())
  const userInfo = ref<UserInfo | null>(null)
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])

  const isLoggedIn = computed(() => !!token.value)
  /** 超级管理员拥有 *:*:* */
  const isSuperAdmin = computed(() => permissions.value.includes('*:*:*'))

  function saveToken(value: string) {
    token.value = value
    setToken(value)
  }

  function clearAuth() {
    token.value = null
    userInfo.value = null
    roles.value = []
    permissions.value = []
    removeToken()
  }

  async function fetchUserInfo() {
    if (!token.value) return
    const res = await getInfo()
    const payload = (res?.data ?? res) as {
      user?: UserInfo
      roles?: string[]
      permissions?: string[]
    }
    userInfo.value = payload?.user ?? null
    roles.value = payload?.roles ?? []
    permissions.value = payload?.permissions ?? []
  }

  /** 权限校验（用于按钮级控制） */
  function hasPermi(perm: string): boolean {
    return permissions.value.includes('*:*:*') || permissions.value.includes(perm)
  }

  return {
    token,
    userInfo,
    roles,
    permissions,
    isLoggedIn,
    isSuperAdmin,
    saveToken,
    clearAuth,
    fetchUserInfo,
    hasPermi,
  }
})
