import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import { ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY, clearAuth, USER_SUMMARY_KEY } from '@/services/http'
import type { UserSummary } from '@/shared/domain'

// 读者和管理员会话状态仓储, 保存凭证 Token 与简要画像并同步到 localStorage 缓存中
export const useSessionStore = defineStore('session', () => {
  const currentUser = ref<UserSummary | null>(readStoredUser())
  const accessToken = ref(window.localStorage.getItem(ACCESS_TOKEN_KEY) ?? '')
  const refreshToken = ref(window.localStorage.getItem(REFRESH_TOKEN_KEY) ?? '')

  const isAuthenticated = computed(() => currentUser.value !== null)
  const isAdmin = computed(() => currentUser.value?.roles.includes('ADMIN') ?? false)

  // 保存登录态, 供后台接口自动携带 JWT
  function setSession(token: string, user: UserSummary, refresh = '') {
    accessToken.value = token
    currentUser.value = user
    refreshToken.value = refresh
    window.localStorage.setItem(ACCESS_TOKEN_KEY, token)
    window.localStorage.setItem(USER_SUMMARY_KEY, JSON.stringify(user))
    if (refresh) {
      window.localStorage.setItem(REFRESH_TOKEN_KEY, refresh)
    }
  }

  // 更新访问令牌（静默刷新时使用）
  function updateAccessToken(token: string) {
    accessToken.value = token
    window.localStorage.setItem(ACCESS_TOKEN_KEY, token)
  }

  // 当 http.ts 因后端返回 401 自动清空 token 时，同步更新 Pinia 状态
  window.addEventListener('auth:cleared', () => {
    currentUser.value = null
    accessToken.value = ''
    refreshToken.value = ''
  })

  // 清空当前会话
  function logout() {
    currentUser.value = null
    accessToken.value = ''
    refreshToken.value = ''
    clearAuth()
  }

  return {
    currentUser,
    accessToken,
    refreshToken,
    isAuthenticated,
    isAdmin,
    setSession,
    updateAccessToken,
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
