<template>
  <section ref="root" class="search-page">
    <PublicPageHeader title="站内搜索" kicker="GLOBAL SEARCH" theme="blue">
      <div class="search-interface">
        <form class="main-search-form" @submit.prevent="runSearch">
          <Search class="search-icon" :size="20" />
          <input
            v-model="keyword"
            type="text"
            placeholder="搜索 文章、作品、灵感..."
          />
          <button type="submit">搜索</button>
        </form>
      </div>
    </PublicPageHeader>

    <div class="search-controls-bar" data-reveal>
      <div class="search-controls">
        <label>
          <span>类型</span>
          <BaseSelect v-model="activeType" :options="typeOptions" @change="rerunIfSearched" />
        </label>
        <label>
          <span>排序</span>
          <BaseSelect v-model="activeSort" :options="sortOptions" @change="rerunIfSearched" />
        </label>
      </div>
    </div>

    <section class="search-results-section" data-reveal>
      <!-- Before Searching: Show Latest Content -->
      <div v-if="!searched && !isLoading" class="default-content">
        <h3 class="section-title">最新发布</h3>
        <div class="default-grid">
          <RouterLink
            v-for="article in latestArticles"
            :key="article.id"
            :to="{ name: 'article-detail', params: { slug: article.slug } }"
            class="default-card"
          >
            <div class="default-card__meta">
              <span class="badge">文章</span>
              <time>{{ formatDate(article.publishTime) }}</time>
            </div>
            <h4>{{ article.title }}</h4>
            <p>{{ article.summary || '暂无摘要' }}</p>
          </RouterLink>
        </div>
      </div>

      <!-- Loading State -->
      <div v-if="isLoading" class="empty-state">
        <LoaderCircle class="spin" :size="24" />
        <h2>正在检索中...</h2>
      </div>

      <!-- No Results -->
      <div v-if="searched && !isLoading && results.length === 0" class="empty-state">
        <h2>没有找到相关的匹配内容</h2>
        <p>尝试更换关键词，或者放宽搜索类型限制。</p>
      </div>

      <!-- Search Results -->
      <div v-if="searched && !isLoading && results.length > 0" class="results-list">
        <article v-for="result in results" :key="`${result.type}-${result.slug}`" class="result-item">
          <div class="result-meta">
            <span class="type-badge">{{ typeLabels[result.type] }}</span>
            <time v-if="result.occurredAt">{{ formatDate(result.occurredAt) }}</time>
          </div>
          <RouterLink class="result-link" :to="resultTarget(result)">
            <h2>{{ result.title }}</h2>
          </RouterLink>
          <p>{{ result.description || fallbackDescription(result) }}</p>
        </article>
      </div>
    </section>
    <p v-if="notice" class="inline-notice">{{ notice }}</p>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { RouteLocationRaw } from 'vue-router'
import PublicPageHeader from '../components/common/PublicPageHeader.vue'
import BaseSelect from '../shared/components/BaseSelect.vue'
import { LoaderCircle, Search } from '@lucide/vue'
import { searchContent, fetchArticles } from '../services/content'
import { toUserMessage } from '../services/http'
import { usePageReveal } from '../shared/composables/usePageReveal'
import { formatDateToDay } from '../shared/datetime'
import type { SearchResult, SearchResultType, SearchSortType, ArticleSummary } from '../shared/domain'

const root = ref<HTMLElement | null>(null)
const keyword = ref('')
const activeType = ref<SearchResultType | ''>('')
const activeSort = ref<SearchSortType>('relevance')
const results = ref<SearchResult[]>([])
const latestArticles = ref<ArticleSummary[]>([])
const isLoading = ref(false)
const searched = ref(false)
const notice = ref('')

usePageReveal(root)

const typeLabels: Record<SearchResult['type'], string> = {
  ARTICLE: '文章',
  PROJECT: '作品',
  INSPIRATION: '灵感',
  TAG: '标签',
  CATEGORY: '分类',
  PAGE: '页面',
}

const typeOptions: Array<{ label: string; value: SearchResultType | '' }> = [
  { label: '全部内容', value: '' },
  { label: '文章', value: 'ARTICLE' },
  { label: '作品', value: 'PROJECT' },
  { label: '灵感', value: 'INSPIRATION' },
  { label: '标签', value: 'TAG' },
  { label: '分类', value: 'CATEGORY' },
  { label: '页面', value: 'PAGE' },
]

const sortOptions = [
  { label: '最相关', value: 'relevance' },
  { label: '最新发布', value: 'latest' },
  { label: '热度最高', value: 'popular' },
]

onMounted(() => {
  loadLatestArticles()
})

async function loadLatestArticles() {
  try {
    const page = await fetchArticles('', undefined, { pageSize: 6 })
    latestArticles.value = page.records
  } catch (e) {
    console.error('Failed to load latest articles', e)
  }
}

async function runSearch() {
  const value = keyword.value.trim()
  isLoading.value = true
  searched.value = true
  notice.value = ''
  try {
    const page = await searchContent({
      keyword: value,
      type: activeType.value,
      sort: activeSort.value,
      pageSize: 24,
    })
    results.value = page.records
  } catch (error) {
    results.value = []
    notice.value = toUserMessage(error, '搜索接口暂不可用，请稍后再试')
  } finally {
    isLoading.value = false
  }
}

function rerunIfSearched() {
  runSearch()
}

function resultTarget(result: SearchResult): RouteLocationRaw {
  if (result.type === 'ARTICLE') return { name: 'article-detail', params: { slug: result.slug } }
  if (result.type === 'PROJECT') return { name: 'project-detail', params: { slug: result.slug } }
  if (result.type === 'TAG') return { name: 'articles', query: { keyword: result.title } }
  if (result.type === 'CATEGORY') return { name: 'articles', query: { keyword: result.title } }
  if (result.type === 'PAGE' && result.slug === 'about') return { name: 'about' }
  return { name: 'inspirations' }
}

function fallbackDescription(result: SearchResult) {
  if (result.type === 'TAG') return `查看与 #${result.title} 相关的公开内容。`
  if (result.type === 'CATEGORY') return `查看 ${result.title} 分类下的公开内容。`
  if (result.type === 'PAGE') return '打开公开页面。'
  return '暂无摘要。'
}

function formatDate(value?: string | null): string {
  if (!value) return ''
  return formatDateToDay(value)
}
</script>

<style scoped>
.search-page {
  padding: 46px 0 84px;
}

.search-interface {
  width: 100%;
  min-width: 320px;
}

.main-search-form {
  display: flex;
  align-items: center;
  width: 100%;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 6px;
  transition: all 0.3s ease;
}

.main-search-form:focus-within {
  border-color: #315bff;
  box-shadow: 0 0 0 4px rgba(49, 91, 255, 0.1);
  background: #ffffff;
}

.search-icon {
  color: #94a3b8;
  margin: 0 12px;
  flex-shrink: 0;
}

.main-search-form input {
  flex: 1;
  border: none;
  background: transparent;
  padding: 8px 0;
  font-size: 15px;
  color: #0f172a;
  outline: none;
  min-width: 0;
}

.main-search-form input::placeholder {
  color: #94a3b8;
}

.main-search-form button {
  background: #0f172a;
  color: #ffffff;
  border: none;
  padding: 10px 24px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.main-search-form button:hover {
  background: #1e293b;
  transform: translateY(-1px);
}

.search-controls-bar {
  position: relative;
  z-index: 20;
  margin-top: 24px;
  margin-bottom: 32px;
}

.search-controls {
  display: flex;
  gap: 20px;
  align-items: center;
  justify-content: flex-end;
}

.search-controls label {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #475569;
  font-size: 14px;
  font-weight: 500;
}

.search-controls label span {
  white-space: nowrap;
  flex-shrink: 0;
}

.search-controls :deep(.base-select) {
  min-width: 130px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  color: #0f172a;
}

.search-controls :deep(.base-select:hover) {
  background: #ffffff;
}

.search-results-section {
  position: relative;
  z-index: 10;
}

.section-title {
  font-size: 20px;
  font-weight: 800;
  color: #0f172a;
  margin: 0 0 24px;
}

.default-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 24px;
}

.default-card {
  display: flex;
  flex-direction: column;
  padding: 24px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  text-decoration: none;
  color: inherit;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.02);
}

.default-card:hover {
  border-color: #cbd5e1;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.05);
  transform: translateY(-4px);
}

.default-card__meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.badge {
  font-size: 12px;
  font-weight: 800;
  padding: 6px 12px;
  border-radius: 8px;
  background: #f0f9ff;
  color: #0ea5e9;
  letter-spacing: 0.05em;
}

.default-card__meta time {
  font-size: 13px;
  color: #94a3b8;
  font-weight: 600;
}

.default-card h4 {
  margin: 0 0 12px;
  font-size: 18px;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.default-card p {
  margin: 0;
  font-size: 15px;
  color: #64748b;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.results-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.result-item {
  padding: 28px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  transition: all 0.3s ease;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.02);
}

.result-item:hover {
  border-color: #cbd5e1;
  box-shadow: 0 12px 24px -8px rgba(0, 0, 0, 0.05);
  transform: translateY(-2px);
}

.result-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}

.type-badge {
  font-size: 13px;
  font-weight: 800;
  color: #ffffff;
  background: #0ea5e9;
  padding: 4px 12px;
  border-radius: 6px;
  letter-spacing: 0.05em;
}

.result-meta time {
  font-size: 14px;
  color: #94a3b8;
  font-weight: 600;
}

.result-link {
  text-decoration: none;
  display: inline-block;
}

.result-link h2 {
  margin: 0 0 12px;
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
  transition: color 0.2s ease;
}

.result-link:hover h2 {
  color: #0ea5e9;
}

.result-item p {
  margin: 0;
  font-size: 16px;
  color: #475569;
  line-height: 1.7;
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
  background: #ffffff;
  border-radius: 20px;
  border: 1px dashed #cbd5e1;
  margin-top: 20px;
}

.empty-state h2 {
  font-size: 20px;
  font-weight: 800;
  color: #0f172a;
  margin: 16px 0 8px;
}

.empty-state p {
  color: #64748b;
  font-size: 15px;
}

@media (max-width: 760px) {
  .search-interface {
    min-width: auto;
  }

  .main-search-form {
    flex-direction: column;
    padding: 12px;
    gap: 12px;
  }
  
  .main-search-form button {
    width: 100%;
  }
  
  .search-controls {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }
}
</style>
