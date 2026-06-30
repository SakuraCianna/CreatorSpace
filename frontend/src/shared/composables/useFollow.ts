import { ref } from 'vue'
import { followUser, unfollowUser, fetchFollowStatus } from '@/services/content'
import { ACCESS_TOKEN_KEY } from '@/services/http'

export function useFollow() {
  const following = ref(false)
  const isFriend = ref(false)

  function hasToken(): boolean {
    return Boolean(window.localStorage.getItem(ACCESS_TOKEN_KEY))
  }

  async function loadStatus(userId: number) {
    if (!hasToken() || !userId) {
      following.value = false
      isFriend.value = false
      return
    }
    try {
      const status = await fetchFollowStatus(userId)
      following.value = status.following
      isFriend.value = status.friend
    } catch {
      following.value = false
      isFriend.value = false
    }
  }

  async function toggleFollow(userId: number) {
    if (!hasToken() || !userId) return
    try {
      if (following.value) {
        await unfollowUser(userId)
        following.value = false
        isFriend.value = false
      } else {
        await followUser(userId)
        following.value = true
      }
    } catch (e) {
      console.error('toggleFollow error:', e)
    }
  }

  return { following, isFriend, loadStatus, toggleFollow }
}
