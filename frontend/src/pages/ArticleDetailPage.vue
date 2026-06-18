<template>
  <section class="detail-page">
    <RouterLink class="text-link detail-back" :to="{ name: 'articles' }">返回文章</RouterLink>

    <div v-if="isLoading" class="empty-state detail-state">
      <h2>文章加载中</h2>
    </div>
    <div v-else-if="errorMessage" class="empty-state detail-state">
      <h2>文章暂时不可用</h2>
      <p>{{ errorMessage }}</p>
    </div>
    <article v-else-if="article" class="detail-panel">
      <header class="detail-hero">
        <p class="page-kicker">{{ article.category?.name ?? 'Creator Journal' }}</p>
        <h1>{{ article.title }}</h1>
        <p v-if="article.summary" class="detail-summary">{{ article.summary }}</p>
        <div class="detail-meta">
          <span>{{ formatDate(article.publishTime) }}</span>
          <span>{{ article.status }}</span>
        </div>
        <div v-if="article.tags.length" class="tag-row">
          <span v-for="tag in article.tags" :key="tag.id">#{{ tag.name }}</span>
        </div>
      </header>

      <div class="markdown-body">
        <template v-if="contentBlocks.length">
          <component
            :is="block.kind"
            v-for="block in contentBlocks"
            :key="block.id"
          >
            {{ block.text }}
          </component>
        </template>
        <p v-else>这篇文章还没有正文，先从摘要开始阅读。</p>
      </div>
    </article>
    <div v-else class="empty-state detail-state">
      <h2>没有找到这篇文章</h2>
    </div>
  </section>
</template>

<script setup lang="ts">
/**
 * 文章详情页负责读取公开文章正文，并用安全的文本块渲染 Markdown 摘要。
 */
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

import { fetchArticleBySlug } from '@/services/content'
import type { ArticleSummary } from '@/shared/domain'

interface TextBlock {
  id: string
  kind: 'h2' | 'p' | 'blockquote' | 'li'
  text: string
}

const route = useRoute()
const article = ref<ArticleSummary | null>(null)
const isLoading = ref(true)
const errorMessage = ref('')
const slug = computed(() => readRouteParam(route.params.slug))
const contentBlocks = computed(() => parseMarkdown(article.value?.contentMarkdown ?? article.value?.summary ?? ''))

// 读取文章详情，路由参数变化时同步刷新。
async function loadArticle() {
  if (!slug.value) {
    errorMessage.value = '文章地址缺少 slug'
    article.value = null
    isLoading.value = false
    return
  }

  isLoading.value = true
  errorMessage.value = ''
  try {
    article.value = await fetchArticleBySlug(slug.value)
  } catch (error) {
    article.value = null
    errorMessage.value = error instanceof Error ? error.message : '文章加载失败'
  } finally {
    isLoading.value = false
  }
}

// 路由参数可能是数组，这里统一收敛成单个 slug。
function readRouteParam(value: string | string[] | undefined): string {
  if (Array.isArray(value)) {
    return value[0] ?? ''
  }
  return value ?? ''
}

// 使用文本节点渲染 Markdown，避免详情页直接插入未清洗 HTML。
function parseMarkdown(value: string): TextBlock[] {
  return value
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line, index) => {
      if (line.startsWith('## ')) {
        return createBlock(index, 'h2', line.slice(3))
      }
      if (line.startsWith('> ')) {
        return createBlock(index, 'blockquote', line.slice(2))
      }
      if (line.startsWith('- ')) {
        return createBlock(index, 'li', line.slice(2))
      }
      return createBlock(index, 'p', line.replace(/^#+\s*/, ''))
    })
}

// 为 v-for 生成稳定键值。
function createBlock(index: number, kind: TextBlock['kind'], text: string): TextBlock {
  return {
    id: `${kind}-${index}-${text.slice(0, 12)}`,
    kind,
    text,
  }
}

// 格式化发布时间。
function formatDate(value?: string | null): string {
  if (!value) {
    return '未定档'
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date(value))
}

onMounted(loadArticle)
watch(slug, loadArticle)
</script>
