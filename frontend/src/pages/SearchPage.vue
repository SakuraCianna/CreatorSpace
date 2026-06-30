<template>
<!-- 全站全文检索结果页面 -->
<!-- 站内多维检索页面 -->
  <section ref="root" class="search-page">
    <PublicPageHeader title="站内搜索" theme="cyan">
      <div style="display: flex; flex-direction: column; gap: 16px; width: 100%;">
        <form class="archive-search archive-search--large" @submit.prevent="runSearch">
          <Search :size="18" />
          <input v-model="keyword" placeholder="试试搜索 内容系统、动效、Prompt" aria-label="站内搜索" />
          <button class="button button-filled button-compact" type="submit">搜索</button>
        </form>
        <div class="search-controls" data-reveal>
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
    </PublicPageHeader>

    <section class="search-results" data-reveal>
      <article v-for="result in results" :key="`${result.type}-${result.slug}`" class="search-result">
        <span>{{ typeLabels[result.type] }}</span>
        <h2>{{ result.title }}</h2>
        <p>{{ result.description || fallbackDescription(result) }}</p>
        <RouterLink class="text-link" :to="resultTarget(result)">
          打开
          <ArrowRight :size="15" />
        </RouterLink>
      </article>
      <div v-if="!isLoading && results.length === 0" class="empty-state">
        <h2>{{ searched ? '没有找到匹配内容' : '输入关键词开始搜索' }}</h2>
        <p>搜索会覆盖公开文章、可见作品、公开灵感、标签、分类和公开页面配置。</p>
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
// 导入所需的组件和 Vue 钩子
import { ref } from 'vue'
import { RouterLink, type RouteLocationRaw } from 'vue-router'
import PublicPageHeader from '@/components/common/PublicPageHeader.vue'
import { ArrowRight, LoaderCircle, Search } from '@lucide/vue'
import BaseSelect from '@/shared/components/BaseSelect.vue'

import { searchContent } from '@/services/content'
import { toUserMessage } from '@/services/http'
import { usePageReveal } from '@/shared/composables/usePageReveal'
import type { SearchResult, SearchResultType, SearchSortType } from '@/shared/domain'

// 声明检索输入和搜索结果的响应式数据
const root = ref<HTMLElement | null>(null)
const keyword = ref('')
const activeType = ref<SearchResultType | ''>('')
const activeSort = ref<SearchSortType>('relevance')
const results = ref<SearchResult[]>([])
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
  { label: '全部', value: '' },
  { label: '文章', value: 'ARTICLE' },
  { label: '作品', value: 'PROJECT' },
  { label: '灵感', value: 'INSPIRATION' },
  { label: '标签', value: 'TAG' },
  { label: '分类', value: 'CATEGORY' },
  { label: '页面', value: 'PAGE' },
]

const sortOptions = [
  { label: '相关度', value: 'relevance' },
  { label: '最新', value: 'latest' },
  { label: '热度', value: 'popular' },
]

// 发起站内多维全文搜索, 覆盖文章、作品和灵感等多类型, 支持相关度、时间及流行度排序
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
  if (searched.value) {
    runSearch()
  }
}

// 根据搜索结果的目标类型构建对应的路由跳转信息
function resultTarget(result: SearchResult): RouteLocationRaw {
  if (result.type === 'ARTICLE') {
    return { name: 'article-detail', params: { slug: result.slug } }
  }
  if (result.type === 'PROJECT') {
    return { name: 'project-detail', params: { slug: result.slug } }
  }
  if (result.type === 'TAG') {
    return { name: 'articles', query: { keyword: result.title } }
  }
  if (result.type === 'CATEGORY') {
    return { name: 'articles', query: { keyword: result.title } }
  }
  if (result.type === 'PAGE' && result.slug === 'about') {
    return { name: 'about' }
  }
  return { name: 'inspirations' }
}

function fallbackDescription(result: SearchResult) {
  if (result.type === 'TAG') return `查看与 #${result.title} 相关的公开内容。`
  if (result.type === 'CATEGORY') return `查看 ${result.title} 分类下的公开内容。`
  if (result.type === 'PAGE') return '打开公开页面。'
  return '暂无摘要。'
}
</script>

<style scoped>
.search-page {
  padding: 46px 0 84px;
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

.search-controls {
  display: flex;
  grid-column: 1 / -1;
  flex-wrap: wrap;
  gap: 12px;
}

.search-controls label {
  display: grid;
  gap: 6px;
  min-width: 160px;
  color: var(--tone-muted);
  font-size: 12px;
  font-weight: 760;
}

.search-controls select {
  appearance: none;
  min-height: 40px;
  padding: 0 38px 0 12px;
  border: 1px solid var(--tone-line);
  border-radius: 8px;
  background:
    linear-gradient(45deg, transparent 50%, #315bff 50%),
    linear-gradient(135deg, #315bff 50%, transparent 50%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(246, 248, 255, 0.9));
  background-position:
    calc(100% - 18px) 50%,
    calc(100% - 13px) 50%,
    0 0;
  background-size:
    5px 5px,
    5px 5px,
    100% 100%;
  background-repeat: no-repeat;
  color: var(--tone-ink);
  font: inherit;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    background 0.18s ease;
}

.search-controls select:hover {
  border-color: rgba(49, 91, 255, 0.28);
}

.search-controls select:focus {
  border-color: rgba(49, 91, 255, 0.48);
  outline: none;
  box-shadow: 0 0 0 4px rgba(49, 91, 255, 0.1);
}

.search-controls select option {
  background: #fff;
  color: var(--tone-ink);
  font-size: 14px;
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



@media (max-width: 760px) {
  .search-page {
    padding-top: 26px;
  }


  .archive-search {
    grid-template-columns: auto minmax(0, 1fr);
    border-radius: 8px;
  }

  .archive-search button {
    grid-column: 1 / -1;
    width: 100%;
  }

  .search-controls {
    display: grid;
  }

  .search-result {
    grid-template-columns: 1fr;
  }
}


</style>
