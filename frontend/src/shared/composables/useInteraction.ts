import { ref } from 'vue'

import {
  fetchFavoriteStatus,
  fetchLikeStatus,
  favoriteTarget,
  likeTarget,
  unfavoriteTarget,
  unlikeTarget,
} from '@/services/content'
import { useSessionStore } from '@/shared/sessionStore'

export function useInteraction(targetType: 'ARTICLE' | 'PROJECT' | 'COMMENT' | 'INSPIRATION') {
  const liked = ref(false)
  const favorited = ref(false)
  const session = useSessionStore()

  async function loadStatus(targetId: number) {
    if (!session.accessToken) {
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
    if (!targetId || !session.accessToken) return
    try {
      if (liked.value) {
        await unlikeTarget(targetType, targetId)
        liked.value = false
      } else {
        await likeTarget(targetType, targetId)
        liked.value = true
      }
    } catch {
      // ignore
    }
  }

  async function toggleFavorite(targetId?: number | null) {
    if (!targetId || !session.accessToken || targetType === 'COMMENT') return
    try {
      if (favorited.value) {
        await unfavoriteTarget(targetType, targetId)
        favorited.value = false
      } else {
        await favoriteTarget(targetType, targetId)
        favorited.value = true
      }
    } catch {
      // ignore
    }
  }

  return { liked, favorited, loadStatus, toggleLike, toggleFavorite }
}
