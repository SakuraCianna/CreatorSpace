<template>
  <section class="page-section">
    <div class="section-heading">
      <h1>文章</h1>
      <p>公开文章会从后端内容接口读取，草稿和私密文章不会展示在这里。</p>
    </div>
    <div v-if="isLoading" class="empty-state">
      <h2>文章加载中</h2>
    </div>
    <div v-else-if="errorMessage" class="empty-state">
      <h2>文章暂时不可用</h2>
      <p>{{ errorMessage }}</p>
    </div>
    <div v-else-if="articles.length === 0" class="empty-state">
      <h2>暂无公开文章</h2>
      <p>发布公开文章后会显示在这里。</p>
    </div>
    <div v-else class="list-surface">
      <article v-for="article in articles" :key="article.id" class="list-row">
        <div>
          <h2>{{ article.title }}</h2>
          <p>{{ articleMeta(article) }}</p>
        </div>
        <span class="status-chip">{{ article.status }}</span>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { fetchArticles } from '@/services/content'
import type { ArticleSummary } from '@/shared/domain'

const privacyLabels: Record<ArticleSummary['privacyType'], string> = {
  PUBLIC: '公开',
  SELF: '仅自己',
  FRIENDS: '仅好友',
  SELECTED_FRIENDS: '选中好友可见',
  EXCLUDED_FRIENDS: '排除选中好友',
}

const articles = ref<ArticleSummary[]>([])
const isLoading = ref(true)
const errorMessage = ref('')

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

// 拼接文章卡片的辅助信息。
function articleMeta(article: ArticleSummary) {
  return [
    article.category?.name,
    article.slug,
    privacyLabels[article.privacyType],
    ...article.tags.map((tag) => `#${tag.name}`),
  ]
    .filter(Boolean)
    .join(' · ')
}

onMounted(loadArticles)
</script>
