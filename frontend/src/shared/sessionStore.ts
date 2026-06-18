import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import { ACCESS_TOKEN_KEY } from '@/services/http'
import type { UserSummary } from '@/shared/domain'

export const useSessionStore = defineStore('session', () => {
  const currentUser = ref<UserSummary | null>(null)
  const accessToken = ref(window.localStorage.getItem(ACCESS_TOKEN_KEY) ?? '')

  const isAuthenticated = computed(() => currentUser.value !== null)
  const isAdmin = computed(() => currentUser.value?.roles.includes('ADMIN') ?? false)

  // 保存登录态，供后台接口自动携带 JWT。
  function setSession(token: string, user: UserSummary) {
    accessToken.value = token
    currentUser.value = user
    window.localStorage.setItem(ACCESS_TOKEN_KEY, token)
  }

  // 清空当前会话。
  function logout() {
    currentUser.value = null
    accessToken.value = ''
    window.localStorage.removeItem(ACCESS_TOKEN_KEY)
  }

  return {
    currentUser,
    accessToken,
    isAuthenticated,
    isAdmin,
    setSession,
    logout,
  }
})
