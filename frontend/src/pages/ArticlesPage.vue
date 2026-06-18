<template>
  <section class="journal-page">
    <aside class="journal-profile">
      <p class="page-kicker">Creator Journal</p>
      <h1>Sakura 的主题笔记</h1>
      <p>把工程复盘、产品观察和创意灵感放进同一个长期档案，用清晰的标签和封面让每篇文章都有自己的气质。</p>
      <dl class="profile-stats">
        <div>
          <dt>{{ articles.length }}</dt>
          <dd>公开文章</dd>
        </div>
        <div>
          <dt>{{ categoryCount }}</dt>
          <dd>主题栏目</dd>
        </div>
      </dl>
    </aside>

    <div class="journal-feed">
      <div v-if="isLoading" class="empty-state journal-state">
        <h2>文章加载中</h2>
      </div>
      <div v-else-if="errorMessage" class="empty-state journal-state">
        <h2>文章暂时不可用</h2>
        <p>{{ errorMessage }}</p>
      </div>
      <div v-else-if="articles.length === 0" class="empty-state journal-state">
        <h2>暂无公开文章</h2>
      </div>
      <template v-else>
        <RouterLink
          v-if="featuredArticle"
          class="journal-featured"
          :style="articleCoverStyle(featuredArticle, 0)"
          :to="{ name: 'article-detail', params: { slug: featuredArticle.slug } }"
        >
          <span class="article-label">精选笔记</span>
          <h2>{{ featuredArticle.title }}</h2>
          <p>{{ featuredArticle.summary }}</p>
          <footer>
            <span>{{ formatDate(featuredArticle.publishTime) }}</span>
            <span>{{ featuredArticle.category?.name ?? '未分类' }}</span>
          </footer>
        </RouterLink>

        <div class="journal-grid">
          <RouterLink
            v-for="(article, index) in regularArticles"
            :key="article.id"
            class="journal-card"
            :style="articleCoverStyle(article, index + 1)"
            :to="{ name: 'article-detail', params: { slug: article.slug } }"
          >
            <span class="article-date">{{ formatDate(article.publishTime) }}</span>
            <h2>{{ article.title }}</h2>
            <p>{{ article.summary }}</p>
            <div class="tag-row">
              <span v-for="tag in article.tags.slice(0, 3)" :key="tag.id">#{{ tag.name }}</span>
            </div>
          </RouterLink>
        </div>
      </template>
    </div>
  </section>
</template>

<script setup lang="ts">
/**
 * 公开文章页以个人主题博客的方式展示内容，避免后台列表感。
 */
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'

import { fetchArticles } from '@/services/content'
import type { ArticleSummary } from '@/shared/domain'

const coverPalettes = [
  ['#0f172a', '#20c997', '#f8fafc'],
  ['#231942', '#ff7a59', '#fff7ed'],
  ['#132a13', '#8bd450', '#f7fee7'],
  ['#2f2504', '#f6c453', '#fff8db'],
  ['#172554', '#60a5fa', '#eef6ff'],
] as const

const articles = ref<ArticleSummary[]>([])
const isLoading = ref(true)
const errorMessage = ref('')

const featuredArticle = computed(() => articles.value[0] ?? null)
const regularArticles = computed(() => articles.value.slice(1))
const categoryCount = computed(() => new Set(articles.value.map((article) => article.category?.id).filter(Boolean)).size)

// 加载公开文章列表。
async function loadArticles() {
  isLoading.value = true
  errorMessage.value = ''
  try {
    const page = await fetchArticles()
    articles.value = page.records
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '文章加载失败'
  } finally {
    isLoading.value = false
  }
}

// 格式化发布时间，缺失时保持界面干净。
function formatDate(value?: string | null): string {
  if (!value) {
    return '未定档'
  }
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
  }).format(new Date(value))
}

// 生成稳定的主题封面色，让缺图文章也有独立识别度。
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
