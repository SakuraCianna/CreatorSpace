<template>
  <section ref="root" class="archive-page">
    <header class="archive-hero page-hero page-hero--articles" data-reveal>
      <div>
        <h1>文章归档</h1>
      </div>
      <form class="archive-search" @submit.prevent="loadArticles">
        <Search :size="18" />
        <input v-model="keyword" placeholder="搜索文章、摘要或正文" aria-label="搜索文章" />
        <button class="button button-filled button-compact" type="submit">检索</button>
      </form>
    </header>

    <div class="topic-strip" data-reveal>
      <button
        v-for="topic in topics"
        :key="topic"
        class="topic-chip"
        :class="{ 'is-active': activeTopic === topic }"
        type="button"
        @click="activeTopic = topic"
      >
        {{ topic }}
      </button>
    </div>

    <div v-if="isLoading" class="empty-state journal-state" data-reveal>
      <LoaderCircle class="spin" :size="24" />
      <h2>正在整理文章档案</h2>
    </div>
    <div v-else class="journal-layout">
      <aside class="journal-profile" data-reveal>
        <span class="profile-orbit" aria-hidden="true" />
        <p class="page-kicker">Reading Map</p>
        <h2>从主题进入，而不是从日期翻找</h2>
        <p>当前展示 {{ visibleArticles.length }} 篇公开文章，覆盖 {{ categoryCount }} 个主题栏目。</p>
        <dl class="profile-stats">
          <div>
            <dt>{{ articles.length }}</dt>
            <dd>公开文章</dd>
          </div>
          <div>
            <dt>{{ allTags.length }}</dt>
            <dd>标签节点</dd>
          </div>
        </dl>
        <p v-if="notice" class="inline-notice">{{ notice }}</p>
      </aside>

      <div class="journal-feed">
        <div v-if="visibleArticles.length === 0" class="empty-state journal-state" data-reveal>
          <h2>没有匹配的公开文章</h2>
          <p>换一个关键词或主题试试。</p>
        </div>

        <RouterLink
          v-if="featuredArticle"
          class="journal-featured"
          :style="articleCoverStyle(featuredArticle, 0)"
          :to="{ name: 'article-detail', params: { slug: featuredArticle.slug } }"
          data-reveal
        >
          <span class="article-label">精选档案</span>
          <h2>{{ featuredArticle.title }}</h2>
          <p>{{ featuredArticle.summary }}</p>
          <footer>
            <span>{{ formatDate(featuredArticle.publishTime) }}</span>
            <span>{{ featuredArticle.category?.name ?? '未分类' }}</span>
            <span>{{ featuredArticle.tags.length }} tags</span>
          </footer>
        </RouterLink>

        <div class="journal-grid">
          <RouterLink
            v-for="(article, index) in regularArticles"
            :key="article.id"
            class="journal-card"
            :style="articleCoverStyle(article, index + 1)"
            :to="{ name: 'article-detail', params: { slug: article.slug } }"
            data-reveal
          >
            <img v-if="article.coverUrl" :src="article.coverUrl" alt="" loading="lazy" />
            <div>
              <span class="article-date">{{ formatDate(article.publishTime) }}</span>
              <h2>{{ article.title }}</h2>
              <p>{{ article.summary }}</p>
              <div class="tag-row">
                <span v-for="tag in article.tags.slice(0, 3)" :key="tag.id">#{{ tag.name }}</span>
              </div>
            </div>
          </RouterLink>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { LoaderCircle, Search } from '@lucide/vue'

import { fallbackArticles } from '@/content/studio'
import { fetchArticles } from '@/services/content'
import { usePageReveal } from '@/shared/composables/usePageReveal'
import type { ArticleSummary } from '@/shared/domain'

const coverPalettes = [
  ['#111827', '#6ea8ff', '#f8fafc'],
  ['#2f163f', '#b18cff', '#fff7ed'],
  ['#10312e', '#54e6c8', '#f7fee7'],
  ['#3a2508', '#ff9d6e', '#fff8db'],
  ['#172554', '#60a5fa', '#eef6ff'],
] as const

const root = ref<HTMLElement | null>(null)
const articles = ref<ArticleSummary[]>([])
const keyword = ref('')
const activeTopic = ref('全部')
const isLoading = ref(true)
const notice = ref('')

usePageReveal(root)

const topics = computed(() => ['全部', ...new Set(articles.value.map((article) => article.category?.name).filter(Boolean) as string[])])
const allTags = computed(() => [...new Map(articles.value.flatMap((article) => article.tags).map((tag) => [tag.id, tag])).values()])
const categoryCount = computed(() => new Set(articles.value.map((article) => article.category?.id).filter(Boolean)).size)
const visibleArticles = computed(() => {
  if (activeTopic.value === '全部') {
    return articles.value
  }
  return articles.value.filter((article) => article.category?.name === activeTopic.value)
})
const featuredArticle = computed(() => visibleArticles.value[0] ?? null)
const regularArticles = computed(() => visibleArticles.value.slice(1))

async function loadArticles() {
  isLoading.value = true
  notice.value = ''
  try {
    const page = await fetchArticles(keyword.value)
    articles.value = page.records.length ? page.records : fallbackArticles
    if (!page.records.length) {
      notice.value = '已显示精选文章。'
    }
  } catch (error) {
    articles.value = fallbackArticles
    notice.value = '已显示精选文章。'
  } finally {
    isLoading.value = false
  }
}

function formatDate(value?: string | null): string {
  if (!value) {
    return '未定档'
  }
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
  }).format(new Date(value))
}

function articleCoverStyle(article: ArticleSummary, index: number) {
  const palette = coverPalettes[index % coverPalettes.length]
  return {
    '--cover-from': palette[0],
    '--cover-accent': article.tags[0]?.color ?? palette[1],
    '--cover-ink': palette[2],
  }
}

onMounted(loadArticles)
</script>
