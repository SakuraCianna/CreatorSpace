<template>
<!-- 作品展厅前台列表与多维检索页面 -->
  <section ref="root" class="projects-page">
    <header class="projects-hero page-hero" data-reveal>
      <div class="hero-copy">
        <p class="page-kicker">Project Gallery</p>
        <h1>作品展厅</h1>
        <p>把公开作品按类型、技术栈和推荐度陈列出来，后端只要继续返回作品列表字段，前台就能自然接上。</p>
      </div>
      <form class="hero-search" @submit.prevent="loadProjects">
        <Search :size="18" />
        <input v-model="keyword" placeholder="搜索作品、说明或技术栈" aria-label="搜索作品" />
        <button class="button button-filled button-compact" type="submit">检索</button>
      </form>
    </header>

    <section class="gallery-tools" data-reveal>
      <div class="segmented-control" aria-label="作品类型筛选">
        <button
          v-for="type in projectTypes"
          :key="type"
          type="button"
          :class="{ 'is-active': activeType === type }"
          @click="activeType = type"
        >
          {{ typeLabel(type) }}
        </button>
      </div>
      <div class="tech-row" aria-label="技术栈筛选">
        <button
          v-for="tech in techFilters"
          :key="tech"
          class="tech-chip"
          type="button"
          :class="{ 'is-active': activeTech === tech }"
          @click="activeTech = tech"
        >
          {{ techLabel(tech) }}
        </button>
      </div>
    </section>

    <div v-if="featuredProject" class="featured-project" data-reveal>
      <RouterLink
        class="featured-project__visual"
        :style="projectCoverStyle(featuredProject, 0)"
        :to="{ name: 'project-detail', params: { slug: featuredProject.slug } }"
      >
        <img v-if="projectCoverSrc(featuredProject)" :src="projectCoverSrc(featuredProject)" alt="" loading="lazy" />
        <span>Featured</span>
      </RouterLink>
      <div class="featured-project__body">
        <p class="page-kicker">{{ featuredProject.projectType }}</p>
        <h2>{{ featuredProject.title }}</h2>
        <p>{{ featuredProject.description || '这个作品还没有填写简介。' }}</p>
        <div class="metric-row">
          <span><Eye :size="15" />{{ formatCount(featuredProject.viewCount) }} 阅读</span>
          <span><Heart :size="15" />{{ formatCount(featuredProject.likeCount) }} 喜欢</span>
          <span><MessageCircle :size="15" />{{ formatCount(featuredProject.commentCount) }} 评论</span>
        </div>
        <div class="tag-row">
          <span v-for="tech in featuredProject.techStack.slice(0, 5)" :key="tech">{{ tech }}</span>
        </div>
        <RouterLink class="text-link" :to="{ name: 'project-detail', params: { slug: featuredProject.slug } }">
          打开作品档案
          <ArrowRight :size="15" />
        </RouterLink>
      </div>
    </div>

    <div v-if="isLoading" class="empty-state showcase-state" data-reveal>
      <LoaderCircle class="spin" :size="24" />
      <h2>正在点亮作品展厅</h2>
    </div>
    <div v-else-if="filteredProjects.length === 0" class="empty-state showcase-state" data-reveal>
      <h2>暂无匹配作品</h2>
      <p>换一个类型、技术栈或关键词再试。</p>
    </div>
    <div v-else-if="visibleProjects.length > 0" class="showcase-grid">
      <RouterLink
        v-for="(project, index) in visibleProjects"
        :key="project.id"
        class="showcase-card"
        :style="projectCoverStyle(project, index + 1)"
        :to="{ name: 'project-detail', params: { slug: project.slug } }"
        @pointermove="tiltCard"
        @pointerleave="resetTilt"
        data-reveal
      >
        <div class="showcase-card__visual">
          <img v-if="projectCoverSrc(project)" :src="projectCoverSrc(project)" alt="" loading="lazy" />
          <span>{{ String(index + 1).padStart(2, '0') }}</span>
        </div>
        <div class="showcase-card__body">
          <div class="showcase-card__meta">
            <span>{{ project.projectType }}</span>
            <span v-if="project.recommended">推荐</span>
          </div>
          <h2>{{ project.title }}</h2>
          <p>{{ project.description || '这个作品还没有填写简介。' }}</p>
          <div class="mini-stats">
            <span><Eye :size="14" />{{ formatCount(project.viewCount) }}</span>
            <span><Heart :size="14" />{{ formatCount(project.likeCount) }}</span>
            <span><Bookmark :size="14" />{{ formatCount(project.favoriteCount) }}</span>
          </div>
          <div class="tag-row">
            <span v-for="tech in project.techStack.slice(0, 4)" :key="tech">{{ tech }}</span>
          </div>
        </div>
      </RouterLink>
    </div>

    <p v-if="notice" class="inline-notice">{{ notice }}</p>
  </section>
</template>

<script setup lang="ts">
// 导入 Vue 生命周期钩子和路由支持
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ArrowRight, Bookmark, Eye, Heart, LoaderCircle, MessageCircle, Search } from '@lucide/vue'

import { fetchProjectFilterRecommendations, fetchProjects } from '@/services/content'
import { toUserMessage } from '@/services/http'
import { usePageReveal } from '@/shared/composables/usePageReveal'
import type { ProjectSummary } from '@/shared/domain'

type ProjectTypeFilter = 'ALL' | string
type TechFilter = 'ALL' | string

const coverPalettes = [
  ['#16213e', '#47d5ff'],
  ['#2d1b3d', '#ff7ab6'],
  ['#0f2f2d', '#6ee7b7'],
  ['#332400', '#f6c453'],
  ['#111827', '#a3e635'],
] as const

// 初始化作品展示与检索过滤相关的状态变量
const root = ref<HTMLElement | null>(null)
const projects = ref<ProjectSummary[]>([])
const recommendedProjectTypes = ref<string[]>([])
const recommendedTechStacks = ref<string[]>([])
const keyword = ref('')
const activeType = ref<ProjectTypeFilter>('ALL')
const activeTech = ref<TechFilter>('ALL')
const isLoading = ref(true)
const notice = ref('')

usePageReveal(root)

const sortedProjects = computed(() => [...projects.value].sort(projectSort))
const projectTypes = computed<ProjectTypeFilter[]>(() => [
  'ALL',
  ...sortByRecommendation(unique(projects.value.map((project) => project.projectType)), recommendedProjectTypes.value),
])
const techFilters = computed<TechFilter[]>(() => [
  'ALL',
  ...sortByRecommendation(unique(projects.value.flatMap((project) => project.techStack)), recommendedTechStacks.value).slice(0, 10),
])
const filteredProjects = computed(() => sortedProjects.value.filter(matchesFilters))
const featuredProject = computed(() => filteredProjects.value[0] ?? null)
const visibleProjects = computed(() => filteredProjects.value.slice(featuredProject.value ? 1 : 0))

// 向后端发起 API 查询请求拉取所有公开作品列表
async function loadProjects() {
  isLoading.value = true
  notice.value = ''
  try {
    const page = await fetchProjects(keyword.value)
    projects.value = page.records
  } catch (error) {
    projects.value = []
    notice.value = toUserMessage(error, '作品接口暂不可用，请稍后再试')
  } finally {
    isLoading.value = false
  }
}

async function loadRecommendedFilters() {
  try {
    const recommendations = await fetchProjectFilterRecommendations(16)
    recommendedProjectTypes.value = recommendations.projectTypes
    recommendedTechStacks.value = recommendations.techStacks
  } catch {
    recommendedProjectTypes.value = []
    recommendedTechStacks.value = []
  }
}

// 校验某个作品实例是否匹配当前所选择的类型过滤项和技术栈过滤项
// 返回布尔类型代表是否通过匹配
// 校验某个作品实例是否匹配当前所选择的类型过滤项和技术栈过滤项
// 返回布尔类型代表是否通过匹配
function matchesFilters(project: ProjectSummary): boolean {
  const matchesType = activeType.value === 'ALL' || project.projectType === activeType.value
  const matchesTech = activeTech.value === 'ALL' || project.techStack.includes(activeTech.value)
  return matchesType && matchesTech
}

function projectSort(left: ProjectSummary, right: ProjectSummary): number {
  return scoreProject(right) - scoreProject(left)
}

// 依据作品是否为推荐状态以及各项浏览、点赞、收藏、评论次数加权计算出它的热度权重评分
function scoreProject(project: ProjectSummary): number {
  return (project.recommended ? 1000 : 0)
    + (project.viewCount ?? 0)
    + (project.likeCount ?? 0) * 3
    + (project.favoriteCount ?? 0) * 4
    + (project.commentCount ?? 0) * 5
}

function projectCoverStyle(project: ProjectSummary, index: number) {
  const palette = coverPalettes[index % coverPalettes.length] ?? coverPalettes[0]
  return {
    '--cover-from': palette[0],
    '--cover-accent': project.tags[0]?.color ?? palette[1],
  }
}

function projectCoverSrc(project: ProjectSummary): string {
  return project.coverUrl?.trim() ?? ''
}

// 计算指针在卡片内所处的坐标比例, 动态调整 CSS 变形变量以渲染 3D 悬浮倾斜动效
function tiltCard(event: PointerEvent) {
  const card = event.currentTarget as HTMLElement
  const rect = card.getBoundingClientRect()
  const x = (event.clientX - rect.left) / rect.width - 0.5
  const y = (event.clientY - rect.top) / rect.height - 0.5
  card.style.setProperty('--tilt-x', `${-y * 4}deg`)
  card.style.setProperty('--tilt-y', `${x * 5}deg`)
}

// 指针移出作品卡片时复位 3D 倾斜变形变量
function resetTilt(event: PointerEvent) {
  const card = event.currentTarget as HTMLElement
  card.style.setProperty('--tilt-x', '0deg')
  card.style.setProperty('--tilt-y', '0deg')
}

function unique(values: string[]): string[] {
  return [...new Set(values.map((value) => value.trim()).filter(Boolean))]
}

function sortByRecommendation(values: string[], recommendations: string[]): string[] {
  const rank = new Map(recommendations.map((value, index) => [value, index]))
  return [...values].sort((left, right) => {
    const leftRank = rank.get(left) ?? Number.MAX_SAFE_INTEGER
    const rightRank = rank.get(right) ?? Number.MAX_SAFE_INTEGER
    return leftRank - rightRank || left.localeCompare(right, 'zh-CN')
  })
}

function typeLabel(type: ProjectTypeFilter): string {
  return type === 'ALL' ? '全部类型' : type
}

function techLabel(tech: TechFilter): string {
  return tech === 'ALL' ? '全部技术' : tech
}

function formatCount(value?: number | null): string {
  return new Intl.NumberFormat('zh-CN', { notation: 'compact' }).format(value ?? 0)
}

onMounted(() => {
  void Promise.all([loadProjects(), loadRecommendedFilters()])
})
</script>

<style scoped>
.projects-page {
  display: grid;
  gap: 18px;
  padding: 46px 0 84px;
}

.projects-hero {
  --hero-accent: #6d3fd2;
  --hero-accent-2: #0077b6;
  --hero-mark: "02";
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 420px);
  align-items: center;
  gap: 24px;
  min-height: clamp(230px, 24vw, 308px);
  padding: clamp(24px, 3vw, 42px);
  overflow: hidden;
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.9), rgba(255, 255, 255, 0.6)),
    radial-gradient(circle at 16% 20%, color-mix(in srgb, var(--hero-accent) 18%, transparent), transparent 34%),
    radial-gradient(circle at 88% 18%, color-mix(in srgb, var(--hero-accent-2) 18%, transparent), transparent 28%),
    linear-gradient(120deg, rgba(49, 91, 255, 0.08), rgba(194, 95, 58, 0.08), rgba(0, 124, 114, 0.1));
  box-shadow: var(--tone-shadow);
  isolation: isolate;
}

.projects-hero::before {
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
  pointer-events: none;
}

.projects-hero::after {
  content: var(--hero-mark);
  position: absolute;
  top: clamp(20px, 4vw, 42px);
  right: clamp(24px, 6vw, 88px);
  z-index: 0;
  color: color-mix(in srgb, var(--hero-accent) 16%, transparent);
  font-size: clamp(86px, 12vw, 180px);
  font-weight: 900;
  line-height: 0.8;
  pointer-events: none;
}

.projects-hero > * {
  position: relative;
  z-index: 1;
}

.hero-copy {
  display: grid;
  gap: 14px;
}

.hero-copy h1 {
  position: relative;
  width: fit-content;
  max-width: 100%;
  margin: 0;
  padding-top: 24px;
  color: var(--tone-ink);
  font-size: clamp(36px, 4vw, 54px);
  font-weight: 860;
  line-height: 1.08;
}

.hero-copy h1::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: clamp(110px, 18vw, 240px);
  height: 8px;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--hero-accent), var(--hero-accent-2), transparent);
}

.hero-copy p:not(.page-kicker) {
  max-width: 640px;
  margin: 0;
  color: var(--tone-muted);
  font-size: 16px;
  line-height: 1.72;
}

.hero-search {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  min-height: 58px;
  padding: 8px 8px 8px 16px;
  border: 1px solid color-mix(in srgb, var(--hero-accent) 22%, var(--tone-line-strong));
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 18px 44px rgba(32, 33, 36, 0.12);
  backdrop-filter: blur(18px);
}

.hero-search svg {
  color: var(--hero-accent);
}

.hero-search input {
  width: 100%;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--tone-ink);
}

.gallery-tools {
  display: grid;
  gap: 12px;
}

.segmented-control,
.tech-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.segmented-control button,
.tech-chip {
  min-height: 38px;
  padding: 0 14px;
  border: 1px solid var(--tone-line);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.74);
  color: var(--tone-muted);
  cursor: pointer;
  font-size: 13px;
  font-weight: 760;
}

.segmented-control button.is-active,
.tech-chip.is-active,
.segmented-control button:hover,
.tech-chip:hover {
  border-color: rgba(49, 91, 255, 0.36);
  background: #e8efff;
  color: #174ea6;
}

.featured-project,
.showcase-card {
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm);
  background: var(--tone-panel);
  box-shadow: var(--tone-shadow);
  backdrop-filter: blur(18px);
}

.featured-project {
  display: grid;
  grid-template-columns: minmax(280px, 0.82fr) minmax(0, 1fr);
  overflow: hidden;
}

.featured-project__visual {
  position: relative;
  display: grid;
  min-height: 320px;
  place-items: end start;
  padding: 26px;
  overflow: hidden;
  background:
    linear-gradient(145deg, rgba(3, 7, 18, 0.08), rgba(3, 7, 18, 0.66)),
    linear-gradient(135deg, var(--cover-from), var(--cover-accent));
  color: #fff;
}

.featured-project__visual img,
.showcase-card__visual img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0.74;
}

.featured-project__visual span {
  position: relative;
  z-index: 1;
  font-size: clamp(44px, 7vw, 86px);
  font-weight: 900;
  line-height: 0.9;
}

.featured-project__body {
  display: grid;
  align-content: center;
  gap: 14px;
  padding: clamp(24px, 4vw, 44px);
}

.featured-project__body h2,
.showcase-card h2 {
  margin: 0;
  color: var(--tone-ink);
  line-height: 1.18;
}

.featured-project__body h2 {
  font-size: clamp(28px, 3.2vw, 44px);
}

.featured-project__body p,
.showcase-card p {
  margin: 0;
  color: var(--tone-muted);
  line-height: 1.68;
}

.metric-row,
.mini-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.metric-row span,
.mini-stats span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--tone-muted);
  font-size: 13px;
  font-weight: 720;
}

.showcase-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--theme-density-spacing, 16px);
  perspective: 1200px;
}

.showcase-card {
  display: grid;
  grid-template-rows: 214px minmax(0, 1fr);
  overflow: hidden;
  transform: rotateX(var(--tilt-x, 0deg)) rotateY(var(--tilt-y, 0deg));
  transition: transform var(--transition-time, 180ms) ease, border-color var(--transition-time, 180ms) ease;
}

.showcase-card:hover {
  border-color: rgba(49, 91, 255, 0.34);
  transform: translateY(-3px) rotateX(var(--tilt-x, 0deg)) rotateY(var(--tilt-y, 0deg));
}

.showcase-card__visual {
  position: relative;
  display: flex;
  align-items: flex-end;
  min-height: 214px;
  padding: 22px;
  overflow: hidden;
  background:
    linear-gradient(145deg, rgba(3, 7, 18, 0.08), rgba(3, 7, 18, 0.66)),
    linear-gradient(135deg, var(--cover-from), var(--cover-accent));
  color: #fff;
}

.showcase-card__visual span {
  position: relative;
  z-index: 1;
  font-size: 52px;
  font-weight: 860;
  line-height: 0.9;
}

.showcase-card__body {
  display: grid;
  gap: 12px;
  padding: calc(var(--theme-density-spacing, 16px) * 1.25);
}

.showcase-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--tone-faint);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.showcase-card h2 {
  font-size: 24px;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-row span {
  padding: 6px 10px;
  border-radius: 6px;
  background: rgba(0, 124, 114, 0.1);
  color: #055f57;
  font-size: 12px;
  font-weight: 740;
}

.inline-notice {
  margin: 0;
  padding: 10px 12px;
  border-left: 3px solid var(--tone-coral);
  background: rgba(194, 95, 58, 0.08);
  color: #754226;
  font-size: 13px;
  line-height: 1.55;
}

@media (max-width: 1020px) {
  .projects-hero,
  .featured-project,
  .showcase-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .projects-page {
    padding-top: 26px;
  }

  .projects-hero {
    padding: 22px;
  }

  .hero-search {
    grid-template-columns: auto minmax(0, 1fr);
    border-radius: 8px;
  }

  .hero-search button {
    grid-column: 1 / -1;
    width: 100%;
  }
}
</style>
