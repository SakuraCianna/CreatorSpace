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
        class="topic-chip"
        :class="{ 'is-active': activeTagId === null }"
        type="button"
        @click="selectTag(null)"
      >
        全部
      </button>
      <button
        v-for="tag in availableTags"
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
import { fetchArticles, fetchTags } from '@/services/content'
import { usePageReveal } from '@/shared/composables/usePageReveal'
import type { ArticleSummary, TagSummary } from '@/shared/domain'

const coverPalettes = [
  ['#111827', '#6ea8ff', '#f8fafc'],
  ['#2f163f', '#b18cff', '#fff7ed'],
  ['#10312e', '#54e6c8', '#f7fee7'],
  ['#3a2508', '#ff9d6e', '#fff8db'],
  ['#172554', '#60a5fa', '#eef6ff'],
] as const

const root = ref<HTMLElement | null>(null)
const articles = ref<ArticleSummary[]>([])
const availableTags = ref<TagSummary[]>([])
const keyword = ref('')
const activeTagId = ref<number | null>(null)
const isLoading = ref(true)
const notice = ref('')

usePageReveal(root)

const allTags = computed(() => [...new Map(articles.value.flatMap((article) => article.tags).map((tag) => [tag.id, tag])).values()])
const categoryCount = computed(() => new Set(articles.value.map((article) => article.category?.id).filter(Boolean)).size)
const visibleArticles = computed(() => articles.value)
const featuredArticle = computed(() => visibleArticles.value[0] ?? null)
const regularArticles = computed(() => visibleArticles.value.slice(1))

function selectTag(tagId: number | null) {
  activeTagId.value = tagId
  loadArticles()
}

async function loadArticles() {
  isLoading.value = true
  notice.value = ''
  try {
    const page = await fetchArticles(keyword.value, activeTagId.value ?? undefined)
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

async function loadTags() {
  try {
    const tags = await fetchTags()
    availableTags.value = tags.sort((a, b) => b.weight - a.weight).slice(0, 12)
  } catch {
    availableTags.value = []
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

onMounted(async () => {
  await Promise.all([loadArticles(), loadTags()])
})
</script>

<style scoped>
.archive-page {
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
  --hero-accent-2: #006a60;
  --hero-mark: "01";
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

.archive-hero h1,
.journal-profile h2 {
  max-width: 830px;
  margin: 0;
  color: var(--tone-ink);
  font-size: 44px;
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

.journal-layout {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 28px;
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
  gap: 16px;
  padding: 22px;
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
  gap: 18px;
}

.journal-featured,
.journal-card {
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm);
  background: rgba(255, 255, 255, 0.78);
  box-shadow: var(--tone-shadow);
  transition: transform 180ms ease, border-color 180ms ease;
}

.journal-featured:hover,
.journal-card:hover {
  border-color: rgba(49, 91, 255, 0.34);
  transform: translateY(-3px);
}

.journal-featured {
  display: flex;
  min-height: 380px;
  flex-direction: column;
  justify-content: flex-end;
  padding: 34px;
  overflow: hidden;
  background:
    linear-gradient(145deg, rgba(5, 8, 22, 0.1), rgba(5, 8, 22, 0.88)),
    linear-gradient(135deg, var(--cover-from), var(--cover-accent));
  color: var(--cover-ink);
}

.article-label,
.article-date {
  color: rgba(248, 250, 252, 0.72);
  font-size: 12px;
  font-weight: 780;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.journal-featured h2 {
  max-width: 720px;
  margin: 14px 0 0;
  font-size: 48px;
  line-height: 1.02;
}

.journal-featured p {
  max-width: 640px;
  margin: 16px 0 0;
  color: rgba(248, 250, 252, 0.8);
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
  gap: 16px;
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

.journal-card img {
  width: 100%;
  height: 190px;
  object-fit: cover;
  background: linear-gradient(135deg, var(--cover-from), var(--cover-accent));
}

.journal-card > div {
  padding: 20px;
}

.journal-card .article-date {
  color: var(--tone-faint);
}

.journal-card h2 {
  margin: 12px 0 0;
  color: var(--tone-ink);
  font-size: 24px;
  line-height: 1.18;
}

.journal-card p {
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
