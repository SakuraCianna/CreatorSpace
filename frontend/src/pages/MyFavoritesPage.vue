<template>
  <section ref="root" class="my-favorites-page">
    <header class="archive-hero page-hero" data-reveal>
      <div>
        <p class="page-kicker">My Favorites</p>
        <h1>我的收藏</h1>
      </div>
    </header>

    <div v-if="isLoading" class="empty-state">
      <LoaderCircle class="spin" :size="24" />
      <h2>正在加载</h2>
    </div>

    <div v-else-if="favorites.length === 0" class="empty-state">
      <Star :size="40" />
      <h2>暂无收藏</h2>
      <p>去发现一些感兴趣的内容并收藏吧。</p>
    </div>

    <div v-else class="journal-grid" data-reveal>
      <RouterLink
        v-for="(item, index) in favorites"
        :key="item.id"
        :to="favoriteRoute(item)"
        class="journal-card"
        :style="coverStyle(index)"
      >
        <div class="journal-card__visual" aria-hidden="true">
          <img v-if="item.coverUrl" :src="item.coverUrl" alt="" loading="lazy" />
          <span v-else>{{ coverFallback(item) }}</span>
        </div>
        <div>
          <div class="article-meta-row">
            <span class="favorite-type-badge">{{ item.targetType === 'ARTICLE' ? '文章' : '作品' }}</span>
            <span class="article-date">{{ formatDate(item.createdAt) }}</span>
          </div>
          <h2>{{ item.title || '未命名' }}</h2>
        </div>
      </RouterLink>
    </div>

    <div v-if="totalPages > 1" class="pagination-row" data-reveal>
      <button
        class="button button-tonal button-compact"
        :disabled="currentPage <= 1"
        type="button"
        @click="goPage(currentPage - 1)"
      >
        <ArrowLeft :size="15" />上一页
      </button>
      <span class="pagination-info">{{ currentPage }} / {{ totalPages }}</span>
      <button
        class="button button-tonal button-compact"
        :disabled="currentPage >= totalPages"
        type="button"
        @click="goPage(currentPage + 1)"
      >
        下一页<ArrowRight :size="15" />
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ArrowLeft, ArrowRight, LoaderCircle, Star } from '@lucide/vue'

import { fetchMyFavorites } from '../services/content'
import { usePageReveal } from '../shared/composables/usePageReveal'
import { formatDateToDay } from '../shared/datetime'
import type { FavoriteRecord } from '../shared/domain'

const coverPalettes = [
  ['#111827', '#6ea8ff', '#f8fafc'],
  ['#2f163f', '#b18cff', '#fff7ed'],
  ['#10312e', '#54e6c8', '#f7fee7'],
  ['#3a2508', '#ff9d6e', '#fff8db'],
  ['#172554', '#60a5fa', '#eef6ff'],
]

const root = ref<HTMLElement | null>(null)
const favorites = ref<FavoriteRecord[]>([])
const isLoading = ref(true)
const currentPage = ref(1)
const totalPages = ref(1)

usePageReveal(root)

onMounted(() => { loadFavorites() })

async function loadFavorites() {
  isLoading.value = true
  try {
    const page = await fetchMyFavorites()
    favorites.value = page.records
    currentPage.value = page.current
    totalPages.value = page.totalPages
  } catch {
    favorites.value = []
  } finally {
    isLoading.value = false
  }
}

async function goPage(page: number) {
  currentPage.value = page
  isLoading.value = true
  try {
    const pageResult = await fetchMyFavorites()
    favorites.value = pageResult.records
    totalPages.value = pageResult.totalPages
  } catch {
    favorites.value = []
  } finally {
    isLoading.value = false
  }
}

function favoriteRoute(item: FavoriteRecord) {
  if (item.targetType === 'ARTICLE') {
    return { name: 'article-detail', params: { slug: item.slug } }
  }
  return { name: 'project-detail', params: { slug: item.slug } }
}

function coverStyle(index: number) {
  const palette = coverPalettes[index % coverPalettes.length] ?? coverPalettes[0]
  return {
    '--cover-from': palette[0],
    '--cover-accent': palette[1],
    '--cover-ink': palette[2],
  }
}

function coverFallback(item: FavoriteRecord): string {
  return (item.title || '收藏').slice(0, 2)
}

function formatDate(value?: string | null): string {
  return formatDateToDay(value)
}
</script>

<style scoped>
.my-favorites-page {
  max-width: 1020px;
  margin: 0 auto;
  padding: 32px 24px 60px;
}

.journal-grid {
  display: grid;
  gap: var(--theme-density-spacing, 16px);
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.journal-card {
  display: grid;
  grid-template-rows: 190px minmax(0, 1fr);
  min-height: 260px;
  overflow: hidden;
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm);
  background: var(--tone-panel);
  text-decoration: none;
  color: inherit;
  box-shadow: var(--tone-shadow);
  transition: transform var(--transition-time, 180ms) ease, border-color var(--transition-time, 180ms) ease, box-shadow var(--transition-time, 180ms) ease;
}

.journal-card:hover {
  border-color: rgba(49, 91, 255, 0.34);
  box-shadow: 0 26px 68px rgba(20, 24, 38, 0.14);
  transform: translateY(-3px);
}

.journal-card__visual {
  position: relative;
  display: grid;
  height: 190px;
  place-items: center;
  overflow: hidden;
  background: radial-gradient(circle at 78% 18%, color-mix(in srgb, var(--cover-accent) 42%, transparent), transparent 34%),
              linear-gradient(135deg, color-mix(in srgb, var(--cover-from) 86%, #ffffff), color-mix(in srgb, var(--cover-accent) 74%, #f8fbff));
}

.journal-card__visual::after {
  content: "";
  position: absolute;
  inset: 12px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.12), transparent 42%),
              repeating-linear-gradient(90deg, rgba(255, 255, 255, 0.08) 0 1px, transparent 1px 20px);
  pointer-events: none;
}

.journal-card__visual img {
  width: 100%;
  height: 190px;
  object-fit: cover;
}

.journal-card__visual span {
  position: relative;
  z-index: 1;
  color: rgba(248, 251, 255, 0.88);
  font-size: 42px;
  font-weight: 900;
  text-shadow: 0 16px 34px rgba(0, 0, 0, 0.26);
}

.journal-card > div:last-child {
  display: grid;
  align-content: start;
  gap: 10px;
  padding: calc(var(--theme-density-spacing, 16px) * 1.25);
}

.journal-card h2 {
  margin: 0;
  color: var(--tone-ink);
  font-size: clamp(18px, 2vw, 22px);
  line-height: 1.25;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-meta-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.favorite-type-badge {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--tone-primary);
}

.article-date {
  color: var(--tone-muted);
  font-size: 13px;
}

.pagination-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 32px;
}

.pagination-info {
  font-size: 13px;
  color: var(--tone-muted);
}

@media (max-width: 760px) {
  .journal-grid {
    grid-template-columns: 1fr;
  }
}
</style>
