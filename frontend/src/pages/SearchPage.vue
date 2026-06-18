<template>
  <section ref="root" class="search-page">
    <header class="archive-hero" data-reveal>
      <div>
        <p class="page-kicker">Site Search</p>
        <h1>在文章、作品和灵感之间找到关联</h1>
        <p>第一阶段使用 PostgreSQL 原生搜索能力，后续可以继续演进到全文排序和 AI 问答。</p>
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
import { onMounted, ref } from 'vue'
import { RouterLink, type RouteLocationRaw } from 'vue-router'
import { ArrowRight, LoaderCircle, Search } from '@lucide/vue'

import { fallbackSearchResults } from '@/content/studio'
import { searchContent } from '@/services/content'
import { usePageReveal } from '@/shared/composables/usePageReveal'
import type { SearchResult } from '@/shared/domain'

const root = ref<HTMLElement | null>(null)
const keyword = ref('内容系统')
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
    results.value = fallbackSearchResults.filter((item) =>
      `${item.title} ${item.description ?? ''}`.toLowerCase().includes(value.toLowerCase()),
    )
    notice.value = error instanceof Error ? `后端暂不可用，已展示本地样例：${error.message}` : '后端暂不可用，已展示本地样例。'
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

onMounted(runSearch)
</script>
