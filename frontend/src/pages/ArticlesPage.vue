<template>
<!-- 前台文章归档列表检索页 -->
<!-- 公开文章归档与检索引导页面 -->
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

    <div ref="topicStrip" class="topic-strip" data-reveal>
      <button
        class="topic-chip"
        :class="{ 'is-active': activeTagId === null }"
        type="button"
        @click="selectTag(null)"
      >
        全部
      </button>
      <button
        v-for="tag in visibleTopicTags"
        :key="tag.id"
        class="topic-chip"
        :class="{ 'is-active': activeTagId === tag.id }"
        type="button"
        @click="selectTag(tag.id)"
      >
        #{{ tag.name }}
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
            <span v-if="featuredArticle.authorId">
              <RouterLink
                class="author-badge"
                :to="{ name: 'user-profile', params: { userId: featuredArticle.authorId } }"
              >
                <span class="author-badge__avatar">
                  <img
                    v-if="featuredArticle.authorAvatar"
                    :src="featuredArticle.authorAvatar"
                    alt=""
                  />
                  <UserRound v-else :size="11" />
                </span>
                {{ featuredArticle.authorName || '匿名' }}
              </RouterLink>
            </span>
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
            <div class="journal-card__visual" aria-hidden="true">
              <img
                v-if="showArticleCover(article)"
                :src="articleCoverSrc(article)"
                alt=""
                loading="lazy"
                @error="markArticleCoverBroken(article.id)"
              />
              <span v-else>{{ article.category?.name?.slice(0, 2) || article.title.slice(0, 2) }}</span>
            </div>
            <div>
              <div class="article-meta-row">
                <span v-if="article.authorId" class="article-author">
                  <RouterLink
                    class="author-badge"
                    :to="{ name: 'user-profile', params: { userId: article.authorId } }"
                  >
                    <span class="author-badge__avatar">
                      <img
                        v-if="article.authorAvatar"
                        :src="article.authorAvatar"
                        alt=""
                      />
                      <UserRound v-else :size="11" />
                    </span>
                    {{ article.authorName || '匿名' }}
                  </RouterLink>
                </span>
                <span class="article-date">{{ formatDate(article.publishTime) }}</span>
              </div>
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
// 导入组件生命周期钩子和相关组件
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { LoaderCircle, Search, UserRound } from '@lucide/vue'

import { fetchArticles, fetchTags } from '@/services/content'
import { toUserMessage } from '@/services/http'
import { usePageReveal } from '@/shared/composables/usePageReveal'
import { formatMonthDay } from '@/shared/datetime'
import type { ArticleSummary, TagSummary } from '@/shared/domain'

const coverPalettes = [
  ['#111827', '#6ea8ff', '#f8fafc'],
  ['#2f163f', '#b18cff', '#fff7ed'],
  ['#10312e', '#54e6c8', '#f7fee7'],
  ['#3a2508', '#ff9d6e', '#fff8db'],
  ['#172554', '#60a5fa', '#eef6ff'],
] as const

// 页面展示状态和交互控制的响应式数据
const root = ref<HTMLElement | null>(null)
const topicStrip = ref<HTMLElement | null>(null)
const route = useRoute()
const router = useRouter()
const articles = ref<ArticleSummary[]>([])
const tagCatalog = ref<TagSummary[]>([])
const topicInterest = ref<Record<string, number>>({})
const topicStripWidth = ref(0)
const keyword = ref('')
const activeTagId = ref<number | null>(null)
const isLoading = ref(true)
const notice = ref('')
const brokenArticleCoverIds = ref(new Set<number>())
let topicResizeObserver: ResizeObserver | null = null

usePageReveal(root)

const allTags = computed(() => [...new Map(articles.value.flatMap((article) => article.tags).map((tag) => [tag.id, tag])).values()])
const articleTopicScore = computed(() => {
  const scores = new Map<number, number>()
  articles.value.forEach((article) => {
    const score = contentInterestScore(article)
    article.tags.forEach((tag) => {
      scores.set(tag.id, (scores.get(tag.id) ?? 0) + score)
    })
  })
  return scores
})
const availableTags = computed(() => {
  const source = tagCatalog.value.length > 0 ? tagCatalog.value : allTags.value
  return [...source]
    .sort((a, b) => topicScore(b) - topicScore(a) || b.weight - a.weight || a.name.localeCompare(b.name, 'zh-CN'))
})
const visibleTopicTags = computed(() => pickTwoRowTopicTags(availableTags.value))
const categoryCount = computed(() => new Set(articles.value.map((article) => article.category?.id).filter(Boolean)).size)
const visibleArticles = computed(() => articles.value)
const featuredArticle = computed(() => visibleArticles.value[0] ?? null)
const regularArticles = computed(() => visibleArticles.value.slice(1))

// 点击标签卡片切换当前激活 of 过滤分类节点, 并触发列表重载与滚屏复位
function selectTag(tagId: number | null) {
  activeTagId.value = tagId
  if (tagId !== null) {
    rememberTopicInterest(tagId)
  }
  syncTagQuery(tagId)
  loadArticles()
}

// 向后端异步请求拉取当前分类标签或关键词匹配的公开文章归档列表数据
async function loadArticles() {
  isLoading.value = true
  notice.value = ''
  try {
    const page = await fetchArticles(keyword.value, activeTagId.value ?? undefined)
    articles.value = page.records
    brokenArticleCoverIds.value = new Set()
  } catch (error) {
    articles.value = []
    notice.value = toUserMessage(error, '文章接口暂不可用，请稍后再试')
  } finally {
    isLoading.value = false
  }
}

async function loadTags() {
  try {
    const tags = await fetchTags()
    tagCatalog.value = tags
  } catch {
    tagCatalog.value = []
  }
}

function syncTagQuery(tagId: number | null) {
  const query = { ...route.query }
  if (tagId === null) {
    delete query.tagId
  } else {
    query.tagId = String(tagId)
  }
  void router.replace({ name: 'articles', query })
}

function readQueryTagId(value: unknown): number | null {
  const raw = Array.isArray(value) ? value[0] : value
  if (typeof raw !== 'string' || !raw.trim()) {
    return null
  }
  const tagId = Number(raw)
  return Number.isInteger(tagId) && tagId > 0 ? tagId : null
}

function topicScore(tag: TagSummary): number {
  const personalScore = (topicInterest.value[String(tag.id)] ?? 0) * 8
  return tag.weight + personalScore + (articleTopicScore.value.get(tag.id) ?? 0)
}

function contentInterestScore(article: ArticleSummary): number {
  return (article.top ? 12 : 0)
    + (article.recommended ? 10 : 0)
    + Math.log10(Math.max(0, article.viewCount ?? 0) + 1) * 3
    + Math.log10(Math.max(0, article.likeCount ?? 0) * 2 + 1) * 3
    + Math.log10(Math.max(0, article.commentCount ?? 0) * 3 + 1) * 2
    + recencyScore(article.publishTime)
}

function recencyScore(value?: string | null): number {
  if (!value) {
    return 0
  }
  const timestamp = new Date(value).getTime()
  if (!Number.isFinite(timestamp)) {
    return 0
  }
  const days = Math.max(0, (Date.now() - timestamp) / 86_400_000)
  return Math.max(0, 8 - days / 14)
}

function rememberTopicInterest(tagId: number) {
  const key = String(tagId)
  topicInterest.value = {
    ...topicInterest.value,
    [key]: Math.min((topicInterest.value[key] ?? 0) + 1, 20),
  }
  writeTopicInterest(topicInterest.value)
}

function readTopicInterest(): Record<string, number> {
  try {
    const raw = localStorage.getItem('creatorspace:article-topic-interest')
    const parsed: unknown = raw ? JSON.parse(raw) : {}
    if (!isRecord(parsed)) {
      return {}
    }
    return Object.fromEntries(
      Object.entries(parsed)
        .filter((entry): entry is [string, number] => typeof entry[1] === 'number' && Number.isFinite(entry[1]))
        .map(([key, value]) => [key, Math.max(0, Math.min(value, 20))]),
    )
  } catch {
    return {}
  }
}

function writeTopicInterest(value: Record<string, number>) {
  try {
    localStorage.setItem('creatorspace:article-topic-interest', JSON.stringify(value))
  } catch {
    // 本地偏好不可写时跳过, 仍按内容热度排序推荐话题
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value)
}

function pickTwoRowTopicTags(tags: TagSummary[]): TagSummary[] {
  const sortedTags = prioritizeActiveTag(tags)
  const containerWidth = Math.max(topicStripWidth.value, 280)
  const rowGap = 10
  const rowRemaining = [
    Math.max(0, containerWidth - estimateControlWidth('全部') - rowGap),
    containerWidth,
  ]
  const visible: TagSummary[] = []
  let rowIndex = 0

  sortedTags.forEach((tag) => {
    if (rowIndex > 1) {
      return
    }
    const remaining = rowRemaining[rowIndex] ?? 0
    const chipWidth = estimateControlWidth(`#${tag.name}`)
    if (chipWidth > remaining && rowIndex < 1) {
      rowIndex += 1
    }
    const currentRemaining = rowRemaining[rowIndex] ?? 0
    if (chipWidth <= currentRemaining) {
      visible.push(tag)
      rowRemaining[rowIndex] = currentRemaining - (chipWidth + rowGap)
    }
  })

  return visible
}

function prioritizeActiveTag(tags: TagSummary[]): TagSummary[] {
  if (activeTagId.value === null) {
    return tags
  }
  const activeTag = tags.find((tag) => tag.id === activeTagId.value)
  if (!activeTag) {
    return tags
  }
  return [activeTag, ...tags.filter((tag) => tag.id !== activeTag.id)]
}

function estimateControlWidth(label: string): number {
  const textWidth = Array.from(label).reduce((total, char) => total + (char.charCodeAt(0) > 255 ? 18 : 10), 0)
  return Math.min(190, Math.max(72, textWidth + 38))
}

function observeTopicStrip() {
  const target = topicStrip.value
  if (!target) {
    return
  }
  topicStripWidth.value = target.clientWidth
  topicResizeObserver = new ResizeObserver((entries) => {
    const width = entries[0]?.contentRect.width
    if (typeof width === 'number') {
      topicStripWidth.value = width
    }
  })
  topicResizeObserver.observe(target)
}

function formatDate(value?: string | null): string {
  return formatMonthDay(value)
}

function articleCoverStyle(article: ArticleSummary, index: number) {
  const palette = coverPalettes[index % coverPalettes.length] ?? coverPalettes[0]
  return {
    '--cover-from': palette[0],
    '--cover-accent': article.tags[0]?.color ?? palette[1],
    '--cover-ink': palette[2],
  }
}

function showArticleCover(article: ArticleSummary): boolean {
  return Boolean(articleCoverSrc(article)) && !brokenArticleCoverIds.value.has(article.id)
}

function articleCoverSrc(article: ArticleSummary): string {
  return article.coverUrl?.trim() ?? ''
}

function markArticleCoverBroken(articleId: number) {
  brokenArticleCoverIds.value = new Set([...brokenArticleCoverIds.value, articleId])
}

onMounted(async () => {
  topicInterest.value = readTopicInterest()
  activeTagId.value = readQueryTagId(route.query.tagId)
  observeTopicStrip()
  await Promise.all([loadArticles(), loadTags()])
})

watch(
  () => route.query.tagId,
  (value) => {
    const tagId = readQueryTagId(value)
    if (tagId === activeTagId.value) {
      return
    }
    activeTagId.value = tagId
    loadArticles()
  },
)

onBeforeUnmount(() => {
  topicResizeObserver?.disconnect()
  topicResizeObserver = null
})
</script>

<style scoped>
.archive-page {
  padding: 36px 0 78px;
}

.archive-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(300px, 390px);
  align-items: center;
  gap: 18px;
  min-height: clamp(156px, 17vw, 214px);
  padding: clamp(18px, 2.5vw, 30px);
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
  --hero-accent-2: #006a60;
  --hero-mark: "01";
}

.page-hero::before {
  content: "";
  position: absolute;
  inset: 12px;
  z-index: 0;
  border: 1px solid color-mix(in srgb, var(--hero-accent) 24%, transparent);
  background:
    linear-gradient(90deg, color-mix(in srgb, var(--hero-accent) 10%, transparent), transparent 38%),
    repeating-linear-gradient(90deg, rgba(20, 21, 29, 0.05) 0 1px, transparent 1px 42px),
    repeating-linear-gradient(0deg, rgba(20, 21, 29, 0.035) 0 1px, transparent 1px 34px);
  clip-path: polygon(0 0, calc(100% - 34px) 0, 100% 34px, 100% 100%, 34px 100%, 0 calc(100% - 34px));
  opacity: 0.9;
  pointer-events: none;
}

.page-hero::after {
  content: var(--hero-mark);
  position: absolute;
  top: clamp(12px, 3vw, 28px);
  right: clamp(20px, 5vw, 70px);
  z-index: 0;
  color: color-mix(in srgb, var(--hero-accent) 16%, transparent);
  font-size: clamp(72px, 9vw, 132px);
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
  padding-top: 18px;
  text-shadow: 0 16px 34px rgba(20, 21, 29, 0.1);
}

.page-hero h1::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: clamp(96px, 16vw, 190px);
  height: 6px;
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

.archive-hero h1,
.journal-profile h2 {
  max-width: 830px;
  margin: 0;
  color: var(--tone-ink);
  font-size: clamp(34px, 4vw, 42px);
  font-weight: 860;
  line-height: 1.08;
}

.archive-hero p:not(.page-kicker),
.journal-profile p {
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
  min-height: 52px;
  padding: 7px 7px 7px 15px;
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
  --topic-chip-height: 38px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-content: flex-start;
  margin: 18px 0 22px;
}

.topic-chip {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-height: var(--topic-chip-height);
  max-width: min(190px, 48vw);
  padding: 0 14px;
  border: 1px solid var(--tone-line);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  color: var(--tone-muted);
  cursor: pointer;
  font-size: 13px;
  font-weight: 760;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.topic-chip.is-active,
.topic-chip:hover {
  border-color: rgba(49, 91, 255, 0.36);
  background: #e8efff;
  color: #174ea6;
}

.journal-layout {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: calc(var(--theme-density-spacing, 16px) * 1.75);
}

.journal-profile,
.empty-state {
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm);
  background: var(--tone-panel);
  box-shadow: var(--tone-shadow);
  backdrop-filter: blur(18px);
}

.journal-profile {
  position: sticky;
  top: 100px;
  align-self: start;
  display: grid;
  gap: var(--theme-density-spacing, 16px);
  padding: calc(var(--theme-density-spacing, 16px) * 1.375);
  overflow: hidden;
}

.profile-orbit {
  width: 100%;
  height: 6px;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--tone-primary), var(--tone-coral), var(--tone-teal));
}

.profile-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin: 0;
}

.profile-stats div {
  padding: 12px;
  border-radius: 8px;
  background: var(--tone-night);
  color: #fff;
}

.profile-stats dt {
  font-size: 26px;
  font-weight: 850;
}

.profile-stats dd {
  margin: 4px 0 0;
  color: rgba(255, 255, 255, 0.66);
  font-size: 12px;
}

.inline-notice {
  margin: 18px 0 0;
  padding: 12px 14px;
  border-left: 3px solid var(--tone-coral);
  background: rgba(194, 95, 58, 0.08);
  color: #754226;
  font-size: 13px;
  line-height: 1.55;
}

.journal-feed {
  display: grid;
  gap: calc(var(--theme-density-spacing, 16px) * 1.125);
}

.journal-featured,
.journal-card {
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm);
  background: var(--tone-panel);
  box-shadow: var(--tone-shadow);
  transition:
    transform var(--transition-time, 180ms) ease,
    border-color var(--transition-time, 180ms) ease,
    box-shadow var(--transition-time, 180ms) ease;
}

.journal-featured:hover,
.journal-card:hover {
  border-color: rgba(49, 91, 255, 0.34);
  box-shadow: 0 26px 68px rgba(20, 24, 38, 0.14);
  transform: translateY(-3px);
}

.journal-featured {
  display: flex;
  min-height: 292px;
  flex-direction: column;
  justify-content: flex-end;
  padding: 28px;
  overflow: hidden;
  background:
    radial-gradient(circle at 86% 18%, color-mix(in srgb, var(--cover-accent) 32%, transparent), transparent 32%),
    linear-gradient(110deg, rgba(5, 8, 22, 0.98), rgba(9, 21, 56, 0.94) 56%, rgba(7, 12, 28, 0.96)),
    linear-gradient(135deg, var(--cover-from), var(--cover-accent));
  color: #f8fbff;
  isolation: isolate;
  position: relative;
}

.journal-featured::before {
  content: "";
  position: absolute;
  inset: 18px;
  z-index: 0;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background:
    linear-gradient(90deg, rgba(255, 255, 255, 0.07), transparent 44%),
    repeating-linear-gradient(90deg, rgba(255, 255, 255, 0.055) 0 1px, transparent 1px 24px),
    repeating-linear-gradient(0deg, rgba(255, 255, 255, 0.035) 0 1px, transparent 1px 32px);
  pointer-events: none;
}

.journal-featured > * {
  position: relative;
  z-index: 1;
}

.article-label,
.article-date {
  color: rgba(248, 250, 252, 0.72);
  font-size: 12px;
  font-weight: 780;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.author-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: inherit;
  text-decoration: none;
  text-transform: none;
  letter-spacing: 0;
  font-weight: 600;
}

.author-badge:hover {
  text-decoration: underline;
}

.author-badge__avatar {
  display: inline-flex;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  overflow: hidden;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.15);
  flex-shrink: 0;
}

.author-badge__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.article-meta-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.article-author {
  display: inline-flex;
  align-items: center;
}

.journal-card .article-date {
  color: var(--tone-primary);
}

.journal-card .article-author {
  color: var(--tone-muted);
}

.journal-featured h2 {
  max-width: 720px;
  margin: 14px 0 0;
  color: #f8fbff;
  font-size: clamp(32px, 4.2vw, 42px);
  line-height: 1.08;
  text-shadow: 0 18px 38px rgba(0, 0, 0, 0.45);
}

.journal-featured p {
  max-width: 640px;
  margin: 16px 0 0;
  color: rgba(238, 244, 255, 0.84);
  line-height: 1.72;
}

.journal-featured footer {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 22px;
}

.journal-featured footer span {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-height: 30px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.76);
  font-size: 12px;
  font-weight: 740;
}

.journal-grid {
  display: grid;
  gap: var(--theme-density-spacing, 16px);
}

.journal-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.journal-card {
  display: grid;
  grid-template-rows: 190px minmax(0, 1fr);
  min-height: 340px;
  overflow: hidden;
}

.journal-card__visual {
  position: relative;
  display: grid;
  height: 190px;
  place-items: center;
  overflow: hidden;
  background:
    radial-gradient(circle at 78% 18%, color-mix(in srgb, var(--cover-accent) 42%, transparent), transparent 34%),
    linear-gradient(135deg, color-mix(in srgb, var(--cover-from) 86%, #ffffff), color-mix(in srgb, var(--cover-accent) 74%, #f8fbff));
}

.journal-card__visual::after {
  content: "";
  position: absolute;
  inset: 12px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  background:
    linear-gradient(90deg, rgba(255, 255, 255, 0.12), transparent 42%),
    repeating-linear-gradient(90deg, rgba(255, 255, 255, 0.08) 0 1px, transparent 1px 20px);
  pointer-events: none;
}

.journal-card__visual img {
  width: 100%;
  height: 190px;
  object-fit: cover;
}

.journal-card__visual span {
  position: relative;
  z-index: 1;
  color: rgba(248, 251, 255, 0.88);
  font-size: 42px;
  font-weight: 900;
  text-shadow: 0 16px 34px rgba(0, 0, 0, 0.26);
}

.journal-card > div {
  display: grid;
  align-content: start;
  gap: 10px;
  padding: calc(var(--theme-density-spacing, 16px) * 1.25);
}

.journal-card h2 {
  margin: 0;
  color: var(--tone-ink);
  font-size: clamp(20px, 2vw, 24px);
  line-height: 1.22;
}

.journal-card p {
  margin: 0;
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
  .archive-hero,
  .journal-layout {
    grid-template-columns: 1fr;
  }

  .journal-profile {
    position: static;
  }
}

@media (max-width: 760px) {
  .archive-page {
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

  .journal-grid {
    grid-template-columns: 1fr;
  }

  .journal-profile h2 {
    font-size: 32px;
  }
}

@media (max-width: 520px) {
  .archive-hero h1,
  .journal-profile h2 {
    font-size: 28px;
  }

  .journal-featured h2 {
    font-size: 34px;
  }
}
</style>
