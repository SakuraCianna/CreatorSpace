<template>
  <section ref="root" class="search-page">
    <header class="archive-hero page-hero page-hero--search" data-reveal>
      <div>
        <h1>站内搜索</h1>
      </div>
      <form class="archive-search archive-search--large" @submit.prevent="runSearch">
        <Search :size="18" />
        <input v-model="keyword" placeholder="试试搜索 内容系统、动效、Prompt" aria-label="站内搜索" />
        <button class="button button-filled button-compact" type="submit">搜索</button>
      </form>
    </header>

    <section class="search-results" data-reveal>
      <article v-for="result in results" :key="`${result.type}-${result.slug}`" class="search-result">
        <span>{{ typeLabels[result.type] }}</span>
        <h2>{{ result.title }}</h2>
        <p>{{ result.description }}</p>
        <RouterLink class="text-link" :to="resultTarget(result)">
          打开
          <ArrowRight :size="15" />
        </RouterLink>
      </article>
      <div v-if="!isLoading && results.length === 0" class="empty-state">
        <h2>{{ searched ? '没有找到匹配内容' : '输入关键词开始搜索' }}</h2>
        <p>搜索会覆盖公开文章、可见作品和公开灵感卡片。</p>
      </div>
      <div v-if="isLoading" class="empty-state">
        <LoaderCircle class="spin" :size="24" />
        <h2>正在搜索</h2>
      </div>
    </section>

    <p v-if="notice" class="inline-notice">{{ notice }}</p>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, type RouteLocationRaw } from 'vue-router'
import { ArrowRight, LoaderCircle, Search } from '@lucide/vue'

import { searchContent } from '@/services/content'
import { toUserMessage } from '@/services/http'
import { usePageReveal } from '@/shared/composables/usePageReveal'
import type { SearchResult } from '@/shared/domain'

const root = ref<HTMLElement | null>(null)
const keyword = ref('')
const results = ref<SearchResult[]>([])
const isLoading = ref(false)
const searched = ref(false)
const notice = ref('')

usePageReveal(root)

const typeLabels: Record<SearchResult['type'], string> = {
  ARTICLE: '文章',
  PROJECT: '作品',
  INSPIRATION: '灵感',
}

async function runSearch() {
  const value = keyword.value.trim()
  if (!value) {
    results.value = []
    searched.value = false
    return
  }
  isLoading.value = true
  searched.value = true
  notice.value = ''
  try {
    const page = await searchContent(value)
    results.value = page.records
  } catch (error) {
    results.value = []
    notice.value = toUserMessage(error, '搜索接口暂不可用，请稍后再试')
  } finally {
    isLoading.value = false
  }
}

function resultTarget(result: SearchResult): RouteLocationRaw {
  if (result.type === 'ARTICLE') {
    return { name: 'article-detail', params: { slug: result.slug } }
  }
  if (result.type === 'PROJECT') {
    return { name: 'project-detail', params: { slug: result.slug } }
  }
  return { name: 'inspirations' }
}
</script>

<style scoped>
.search-page {
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
  --hero-accent: #0b57d0;
  --hero-accent-2: #805610;
  --hero-mark: "04";
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

.archive-search {
  position: relative;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  grid-column: 1 / -1;
  align-items: center;
  gap: 10px;
  max-width: 760px;
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

.search-page,
.search-results {
  display: grid;
  gap: 16px;
}

.search-result {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr) auto;
  align-items: center;
  gap: 18px;
  padding: 18px;
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm);
  background: var(--tone-panel);
  box-shadow: var(--tone-shadow);
  backdrop-filter: blur(18px);
}

.search-result > span {
  color: var(--tone-teal);
  font-size: 12px;
  font-weight: 850;
  letter-spacing: 0.12em;
}

.search-result h2,
.search-result p {
  margin: 0;
}

.search-result h2 {
  color: var(--tone-ink);
  font-size: 24px;
  line-height: 1.18;
}

.search-result p {
  color: var(--tone-muted);
  line-height: 1.68;
}

@media (min-width: 761px) {
  .archive-hero h1 {
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

@media (max-width: 1020px) {
  .archive-hero {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .search-page {
    padding-top: 26px;
  }

  .archive-hero {
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

  .search-result {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 520px) {
  .archive-hero h1 {
    font-size: 28px;
  }
}
</style>
