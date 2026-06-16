import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import type { UserSummary } from '@/types/domain'

export const useSessionStore = defineStore('session', () => {
  const currentUser = ref<UserSummary | null>(null)

  const isAuthenticated = computed(() => currentUser.value !== null)
  const isAdmin = computed(() => currentUser.value?.roles.includes('ADMIN') ?? false)

  function previewAsAdmin() {
    currentUser.value = {
      id: 1,
      username: 'admin',
      roles: ['ADMIN'],
    }
  }

  function logout() {
    currentUser.value = null
  }

  return {
    currentUser,
    isAuthenticated,
    isAdmin,
    previewAsAdmin,
    logout,
  }
})
