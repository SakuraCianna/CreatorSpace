import { ref } from 'vue'

import {
  fetchFavoriteStatus,
  fetchLikeStatus,
  favoriteTarget,
  likeTarget,
  unfavoriteTarget,
  unlikeTarget,
} from '@/services/content'
import { ACCESS_TOKEN_KEY } from '@/services/http'

export function useInteraction(targetType: 'ARTICLE' | 'PROJECT' | 'COMMENT' | 'INSPIRATION') {
  const liked = ref(false)
  const favorited = ref(false)

  function hasToken(): boolean {
    return Boolean(window.localStorage.getItem(ACCESS_TOKEN_KEY))
  }

  async function loadStatus(targetId: number) {
    if (!hasToken()) {
      liked.value = false
      favorited.value = false
      return
    }
    try {
      const [likeStatus, favStatus] = await Promise.all([
        fetchLikeStatus(targetType, targetId),
        targetType !== 'COMMENT' ? fetchFavoriteStatus(targetType, targetId) : Promise.resolve(false),
      ])
      liked.value = likeStatus
      favorited.value = favStatus
    } catch {
      liked.value = false
      favorited.value = false
    }
  }

  async function toggleLike(targetId?: number | null) {
    if (!targetId || !hasToken()) return
    try {
      if (liked.value) {
        await unlikeTarget(targetType, targetId)
        liked.value = false
      } else {
        await likeTarget(targetType, targetId)
        liked.value = true
      }
    } catch (e) {
      console.error('toggleLike error:', e)
    }
  }

  async function toggleFavorite(targetId?: number | null) {
    if (!targetId || !hasToken() || targetType === 'COMMENT') return
    try {
      if (favorited.value) {
        await unfavoriteTarget(targetType, targetId)
        favorited.value = false
      } else {
        await favoriteTarget(targetType, targetId)
        favorited.value = true
      }
    } catch (e) {
      console.error('toggleFavorite error:', e)
    }
  }

  return { liked, favorited, loadStatus, toggleLike, toggleFavorite }
}
