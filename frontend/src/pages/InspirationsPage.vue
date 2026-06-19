<template>
  <section ref="root" class="wall-page">
    <header class="archive-hero page-hero page-hero--inspirations" data-reveal>
      <div>
        <h1>灵感墙</h1>
      </div>
      <form class="archive-search" @submit.prevent="loadInspirations">
        <Search :size="18" />
        <input v-model="keyword" placeholder="搜索灵感、来源或 Prompt" aria-label="搜索灵感" />
        <button class="button button-filled button-compact" type="submit">检索</button>
      </form>
    </header>

    <div class="topic-strip" data-reveal>
      <button
        v-for="item in filters"
        :key="item.value"
        class="topic-chip"
        :class="{ 'is-active': activeType === item.value }"
        type="button"
        @click="selectType(item.value)"
      >
        <component :is="item.icon" :size="15" />
        {{ item.label }}
      </button>
    </div>

    <div v-if="isLoading && !hasLoaded" class="empty-state showcase-state" data-reveal>
      <LoaderCircle class="spin" :size="24" />
      <h2>正在铺开灵感卡片</h2>
    </div>

    <div v-else class="inspiration-masonry">
      <article
        v-for="card in cards"
        :key="card.id"
        class="inspiration-card"
        :class="`inspiration-card--${card.cardType.toLowerCase()}`"
        :style="{ '--card-accent': card.color ?? '#6ea8ff' }"
        data-reveal
      >
        <img v-if="card.imageUrl" :src="card.imageUrl" alt="" loading="lazy" />
        <span class="card-kind">{{ card.cardType }}</span>
        <h2>{{ card.title }}</h2>
        <p>{{ card.content }}</p>
        <div class="tag-row">
          <span v-for="tag in card.tags.slice(0, 3)" :key="tag.id">#{{ tag.name }}</span>
        </div>
        <a v-if="safeSource(card.sourceUrl)" :href="safeSource(card.sourceUrl)" target="_blank" rel="noreferrer">
          <ExternalLink :size="15" />
          来源
        </a>
      </article>
    </div>

    <div v-if="!isLoading && cards.length === 0" class="empty-state showcase-state" data-reveal>
      <h2>没有匹配的灵感卡</h2>
      <p>换个类型或关键词试试。</p>
    </div>
    <p v-if="notice" class="inline-notice">{{ notice }}</p>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import {
  Code2,
  ExternalLink,
  Image,
  Link,
  LoaderCircle,
  MessageSquareQuote,
  Search,
  StickyNote,
} from '@lucide/vue'

import { fallbackInspirations } from '@/content/studio'
import { fetchInspirations } from '@/services/content'
import { useCinematicPageMotion } from '@/shared/composables/useCinematicPageMotion'
import { usePageReveal } from '@/shared/composables/usePageReveal'
import type { InspirationCard, InspirationType } from '@/shared/domain'

type InspirationFilter = InspirationType | 'ALL'

const root = ref<HTMLElement | null>(null)
const cards = ref<InspirationCard[]>([])
const keyword = ref('')
const activeType = ref<InspirationFilter>('ALL')
const isLoading = ref(true)
const hasLoaded = ref(false)
const notice = ref('')
const cinematic = useCinematicPageMotion(root)
let hasPlayedIntro = false

usePageReveal(root)

const filters = [
  { value: 'ALL' as const, label: '全部', icon: StickyNote },
  { value: 'TEXT' as const, label: '摘句', icon: MessageSquareQuote },
  { value: 'PROMPT' as const, label: 'Prompt', icon: StickyNote },
  { value: 'IMAGE' as const, label: '图片', icon: Image },
  { value: 'CODE' as const, label: '代码', icon: Code2 },
  { value: 'LINK' as const, label: '链接', icon: Link },
]

function selectType(type: InspirationFilter) {
  if (activeType.value === type) {
    return
  }
  activeType.value = type
  loadInspirations()
}

async function loadInspirations() {
  isLoading.value = true
  notice.value = ''
  try {
    const page = await fetchInspirations({ keyword: keyword.value, type: activeType.value, pageSize: 30 })
    cards.value = page.records.length ? page.records : filterFallback()
    if (!page.records.length) {
      notice.value = '已显示精选灵感。'
    }
  } catch (error) {
    cards.value = filterFallback()
    notice.value = '已显示精选灵感。'
  } finally {
    isLoading.value = false
    hasLoaded.value = true
    if (!hasPlayedIntro) {
      hasPlayedIntro = true
      void cinematic.play()
    }
  }
}

function filterFallback() {
  return fallbackInspirations.filter((card) => {
    const typeMatch = activeType.value === 'ALL' || card.cardType === activeType.value
    const keywordMatch =
      !keyword.value.trim() ||
      `${card.title} ${card.content ?? ''}`.toLowerCase().includes(keyword.value.trim().toLowerCase())
    return typeMatch && keywordMatch
  })
}

function safeSource(value?: string | null): string {
  if (!value) {
    return ''
  }
  try {
    const url = new URL(value)
    return ['http:', 'https:'].includes(url.protocol) ? url.toString() : ''
  } catch {
    return ''
  }
}

onMounted(loadInspirations)
</script>
