import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import { ACCESS_TOKEN_KEY } from '@/services/http'
import type { UserSummary } from '@/shared/domain'

export const USER_SUMMARY_KEY = 'creatorspace.currentUser'

export const useSessionStore = defineStore('session', () => {
  const currentUser = ref<UserSummary | null>(readStoredUser())
  const accessToken = ref(window.localStorage.getItem(ACCESS_TOKEN_KEY) ?? '')

  const isAuthenticated = computed(() => currentUser.value !== null)
  const isAdmin = computed(() => currentUser.value?.roles.includes('ADMIN') ?? false)

  // 保存登录态，供后台接口自动携带 JWT。
  function setSession(token: string, user: UserSummary) {
    accessToken.value = token
    currentUser.value = user
    window.localStorage.setItem(ACCESS_TOKEN_KEY, token)
    window.localStorage.setItem(USER_SUMMARY_KEY, JSON.stringify(user))
  }

  // 清空当前会话。
  function logout() {
    currentUser.value = null
    accessToken.value = ''
    window.localStorage.removeItem(ACCESS_TOKEN_KEY)
    window.localStorage.removeItem(USER_SUMMARY_KEY)
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

function readStoredUser(): UserSummary | null {
  try {
    const raw = window.localStorage.getItem(USER_SUMMARY_KEY)
    if (!raw) {
      return null
    }
    const parsed = JSON.parse(raw) as Partial<UserSummary>
    if (typeof parsed.id === 'number' && typeof parsed.username === 'string' && Array.isArray(parsed.roles)) {
      return {
        id: parsed.id,
        username: parsed.username,
        roles: parsed.roles.filter((role): role is UserSummary['roles'][number] => role === 'ADMIN' || role === 'USER'),
      }
    }
  } catch {
    window.localStorage.removeItem(USER_SUMMARY_KEY)
  }
  return null
}
