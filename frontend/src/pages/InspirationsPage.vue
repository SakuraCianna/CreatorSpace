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

<style scoped>
.wall-page {
  display: grid;
  gap: 0;
  padding: 46px 0 84px;
}

.archive-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 420px);
  align-items: center;
  gap: 22px;
  min-height: clamp(220px, 23vw, 292px);
  padding: clamp(24px, 3vw, 42px);
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.88), rgba(255, 255, 255, 0.58)),
    radial-gradient(circle at 16% 20%, color-mix(in srgb, var(--hero-accent) 18%, transparent), transparent 34%),
    radial-gradient(circle at 88% 18%, color-mix(in srgb, var(--hero-accent-2) 18%, transparent), transparent 28%),
    linear-gradient(120deg, rgba(49, 91, 255, 0.08), rgba(194, 95, 58, 0.08), rgba(0, 124, 114, 0.1));
  box-shadow: var(--tone-shadow);
  isolation: isolate;
  overflow: hidden;
  position: relative;
}

.page-hero {
  --hero-accent: #007c72;
  --hero-accent-2: #984061;
  --hero-mark: "03";
}

.page-hero::before {
  content: "";
  position: absolute;
  inset: 16px;
  z-index: 0;
  border: 1px solid color-mix(in srgb, var(--hero-accent) 24%, transparent);
  background:
    linear-gradient(90deg, color-mix(in srgb, var(--hero-accent) 10%, transparent), transparent 38%),
    repeating-linear-gradient(90deg, rgba(20, 21, 29, 0.05) 0 1px, transparent 1px 42px),
    repeating-linear-gradient(0deg, rgba(20, 21, 29, 0.035) 0 1px, transparent 1px 34px);
  clip-path: polygon(0 0, calc(100% - 46px) 0, 100% 46px, 100% 100%, 46px 100%, 0 calc(100% - 46px));
  opacity: 0.9;
  pointer-events: none;
}

.page-hero::after {
  content: var(--hero-mark);
  position: absolute;
  top: clamp(16px, 4vw, 40px);
  right: clamp(24px, 6vw, 88px);
  z-index: 0;
  color: color-mix(in srgb, var(--hero-accent) 16%, transparent);
  font-size: clamp(86px, 12vw, 180px);
  font-weight: 900;
  line-height: 0.8;
  pointer-events: none;
}

.page-hero > * {
  position: relative;
  z-index: 1;
}

.page-hero h1 {
  position: relative;
  display: inline-block;
  justify-self: start;
  width: fit-content;
  max-width: 100%;
  padding-top: 24px;
  text-shadow: 0 16px 34px rgba(20, 21, 29, 0.1);
}

.page-hero h1::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: clamp(110px, 18vw, 240px);
  height: 8px;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--hero-accent), var(--hero-accent-2), transparent);
  box-shadow: 0 10px 28px color-mix(in srgb, var(--hero-accent) 22%, transparent);
}

.page-hero h1::after {
  content: "";
  position: absolute;
  top: -2px;
  right: -54px;
  width: 34px;
  height: 14px;
  border: 1px solid color-mix(in srgb, var(--hero-accent) 42%, transparent);
  border-radius: 999px;
  background: color-mix(in srgb, var(--hero-accent-2) 18%, transparent);
  box-shadow: 18px 0 0 color-mix(in srgb, var(--hero-accent) 18%, transparent);
}

.archive-hero h1 {
  max-width: 830px;
  margin: 0;
  color: var(--tone-ink);
  font-size: 44px;
  font-weight: 860;
  line-height: 1.08;
}

.archive-hero p:not(.page-kicker) {
  max-width: 680px;
  color: var(--tone-muted);
  font-size: 17px;
  line-height: 1.72;
}

.archive-search {
  position: relative;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  min-height: 58px;
  padding: 8px 8px 8px 16px;
  border: 1px solid color-mix(in srgb, var(--hero-accent) 22%, var(--tone-line-strong));
  border-radius: 999px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(248, 250, 252, 0.7)),
    color-mix(in srgb, var(--hero-accent) 4%, transparent);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.92),
    0 18px 44px rgba(32, 33, 36, 0.12);
  backdrop-filter: blur(18px);
  overflow: hidden;
}

.archive-search::before {
  content: "";
  position: absolute;
  inset: 5px;
  border-radius: inherit;
  background: linear-gradient(90deg, color-mix(in srgb, var(--hero-accent) 8%, transparent), transparent 36%);
  pointer-events: none;
}

.archive-search > * {
  position: relative;
  z-index: 1;
}

.archive-search svg {
  color: var(--hero-accent);
}

.archive-search input {
  width: 100%;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--tone-ink);
}

.topic-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 22px 0;
}

.topic-chip {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-height: 38px;
  padding: 0 14px;
  border: 1px solid var(--tone-line);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  color: var(--tone-muted);
  cursor: pointer;
  font-size: 13px;
  font-weight: 760;
}

.topic-chip.is-active,
.topic-chip:hover {
  border-color: rgba(49, 91, 255, 0.36);
  background: rgba(49, 91, 255, 0.1);
  color: var(--tone-ink);
}

.inspiration-masonry {
  column-count: 3;
  column-gap: 16px;
}

.inspiration-card {
  position: relative;
  display: inline-grid;
  width: 100%;
  gap: 12px;
  margin: 0 0 16px;
  padding: 18px;
  break-inside: avoid;
  border: 1px solid var(--tone-line);
  border-top: 5px solid var(--card-accent);
  border-radius: var(--app-radius-sm);
  background: var(--tone-panel);
  box-shadow: var(--tone-shadow);
  backdrop-filter: blur(18px);
  overflow: hidden;
}

.inspiration-card::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    linear-gradient(140deg, color-mix(in srgb, var(--card-accent) 18%, transparent), transparent 38%),
    repeating-linear-gradient(135deg, rgba(255, 255, 255, 0.18) 0 1px, transparent 1px 14px);
  opacity: 0.54;
}

.inspiration-card > * {
  position: relative;
  z-index: 1;
}

.inspiration-card img {
  width: 100%;
  border-radius: 8px;
  object-fit: cover;
}

.inspiration-card h2 {
  margin: 0;
  font-size: 22px;
}

.inspiration-card p {
  margin: 0;
  color: var(--tone-muted);
  line-height: 1.66;
}

.inspiration-card--code p {
  font-family: "SFMono-Regular", Consolas, monospace;
  font-size: 13px;
  color: #0f766e;
}

.card-kind {
  color: var(--tone-faint);
}

.inspiration-card a {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--tone-primary);
  font-size: 13px;
  font-weight: 760;
}

@media (max-width: 760px) {
  .wall-page {
    padding-top: 26px;
  }

  .archive-hero {
    grid-template-columns: 1fr;
    padding: 22px;
  }

  .archive-hero h1 {
    font-size: 32px;
  }

  .archive-search {
    grid-template-columns: auto minmax(0, 1fr);
    border-radius: 8px;
  }

  .archive-search button {
    grid-column: 1 / -1;
    width: 100%;
  }

  .inspiration-masonry {
    column-count: 1;
  }
}

@media (max-width: 520px) {
  .archive-hero h1 {
    font-size: 28px;
  }
}
</style>
