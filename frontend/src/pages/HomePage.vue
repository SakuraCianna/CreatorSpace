<template>
  <div class="cs-home">
    <HeroUniverse />
    <MarqueeManifesto />
    <FeaturedArticles />
    <PortfolioGallery />
    <SiteStructureShowcase />
    <ApproachProcess />
    <CreativeWall />
    <ThemeUniverse />
    <FinalCTA />
  </div>
</template>

<script setup lang="ts">
import { defineComponent, h, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

import type { HeroSceneHandles } from '@/shared/heroScene'
import { fetchArticles, fetchCurrentTheme, fetchInspirations, fetchProjects, fetchSiteConfig, fetchThemes } from '@/services/content'
import { useGsapContext } from '@/shared/composables/useGsapScroll'
import { useLenis } from '@/shared/composables/useLenis'
import { attachMagnetic } from '@/shared/composables/useMagnetic'
import { prefersReducedMotion } from '@/shared/composables/useReducedMotion'
import { toCssImageUrl } from '@/shared/cssImage'
import inspirationBg01 from '@/assets/homepage/inspiration-bg-01.svg'
import inspirationBg02 from '@/assets/homepage/inspiration-bg-02.svg'
import inspirationBg03 from '@/assets/homepage/inspiration-bg-03.svg'
import inspirationBg04 from '@/assets/homepage/inspiration-bg-04.svg'
import inspirationBg05 from '@/assets/homepage/inspiration-bg-05.svg'
import inspirationBg06 from '@/assets/homepage/inspiration-bg-06.svg'
import {
  type AgentCapability,
  type ApproachStep,
  type CounterItem,
  type CreativeFragment,
  type FeaturedArticle,
  type HomeHeroAction,
  type HomeHeroContent,
  type ManifestoContent,
  type PortfolioProject,
  type SiteConfig,
  type ThemePreset,
} from '@/content/home'
import type { ArticleSummary, InspirationCard, ProjectSummary, PublicThemeConfig, TagSummary } from '@/shared/domain'


gsap.registerPlugin(ScrollTrigger)

useLenis()

const emptyTotals = {
  articleTotal: 0,
  projectTotal: 0,
  inspirationTotal: 0,
}

const runtimeSiteConfig = ref<SiteConfig | null>(null)
const runtimeHeroContent = ref<HomeHeroContent>(resolveHeroContent(emptyTotals))
const runtimeMarqueeWords = ref<string[]>(buildMarqueeWords())
const runtimeManifesto = ref<ManifestoContent>(buildManifesto())
const runtimeArticles = ref<FeaturedArticle[]>([])
const runtimeProjects = ref<PortfolioProject[]>([])
const runtimeStructureCards = ref<AgentCapability[]>(buildStructureCards())
const runtimeApproachSteps = ref<ApproachStep[]>(buildApproachSteps())
const runtimeCounters = ref<CounterItem[]>(buildCounters(emptyTotals))
const runtimeFragments = ref<CreativeFragment[]>([])
const runtimeThemePresets = ref<ThemePreset[]>([])
const runtimeCurrentThemeName = ref('读取中')

onMounted(() => {
  document.body.classList.add('cs-dark-body')
  document.documentElement.classList.add('cs-dark-scroll')
  void loadHomeRuntimeData()
})
onBeforeUnmount(() => {
  document.body.classList.remove('cs-dark-body')
  document.documentElement.classList.remove('cs-dark-scroll')
})

const HeroWebGLScene = defineComponent({
  name: 'HeroWebGLScene',
  setup() {
    const host = ref<HTMLElement | null>(null)
    const isStatic = ref(false)
    let handles: HeroSceneHandles | null = null
    let io: IntersectionObserver | null = null
    let cancelled = false

    // 将指针位置换算成 WebGL 场景使用的标准坐标。
    const onPointerMove = (event: PointerEvent) => {
      if (!handles) {
        return
      }
      const nx = (event.clientX / window.innerWidth) * 2 - 1
      const ny = -((event.clientY / window.innerHeight) * 2 - 1)
      handles.setPointer(nx, ny)
    }

    onMounted(() => {
      const el = host.value
      if (!el) {
        return
      }
      const probe = document.createElement('canvas')
      const hasWebGL = !!(probe.getContext('webgl2') || probe.getContext('webgl'))
      if (prefersReducedMotion() || !hasWebGL) {
        isStatic.value = true
        return
      }
      import('@/shared/heroScene')
        .then(({ createHeroScene }) => {
          if (cancelled || !host.value) {
            return
          }
          handles = createHeroScene(host.value)
          window.addEventListener('pointermove', onPointerMove, { passive: true })
          io = new IntersectionObserver(
            (entries) => {
              const entry = entries[0]
              if (entry) {
                handles?.setPaused(!entry.isIntersecting)
              }
            },
            { threshold: 0 },
          )
          io.observe(host.value)
        })
        .catch(() => {
          isStatic.value = true
        })
    })

    onBeforeUnmount(() => {
      cancelled = true
      window.removeEventListener('pointermove', onPointerMove)
      io?.disconnect()
      io = null
      handles?.dispose()
      handles = null
    })

    return () =>
      h('div', {
        ref: host,
        class: ['cs-hero__canvas', { 'cs-hero__canvas--static': isStatic.value }],
        'aria-hidden': 'true',
      })
  },
})

// 渲染首页英雄区按钮。
function heroButton(action: HomeHeroAction, ghost = false) {
  const children = [
    h('span', { class: 'cs-btn__fill' }),
    h('span', { class: 'cs-btn__dot' }),
    h('span', action.label),
  ]
  if (action.external) {
    return h(
      'a',
      {
        href: action.to,
        class: ['cs-btn', { 'cs-btn--ghost': ghost }],
        target: '_blank',
        rel: 'noreferrer',
      },
      children,
    )
  }
  return h(
    RouterLink,
    { to: action.to, class: ['cs-btn', { 'cs-btn--ghost': ghost }] },
    { default: () => children },
  )
}

async function loadHomeRuntimeData() {
  const [configResult, articlesResult, projectsResult, inspirationsResult, themesResult, currentThemeResult] =
    await Promise.allSettled([
      fetchSiteConfig(),
      fetchArticles(),
      fetchProjects(),
      fetchInspirations({ pageSize: HOME_INSPIRATION_LIMIT }),
      fetchThemes(),
      fetchCurrentTheme(),
    ])

  const config = configResult.status === 'fulfilled' ? configResult.value : {}
  const articles = articlesResult.status === 'fulfilled' ? articlesResult.value.records : []
  const projects = projectsResult.status === 'fulfilled' ? projectsResult.value.records : []
  const inspirations = inspirationsResult.status === 'fulfilled' ? inspirationsResult.value.records : []
  const themes = themesResult.status === 'fulfilled' ? themesResult.value : []
  const currentTheme = currentThemeResult.status === 'fulfilled' ? currentThemeResult.value : null
  const totals = {
    articleTotal: articlesResult.status === 'fulfilled' ? articlesResult.value.total : 0,
    projectTotal: projectsResult.status === 'fulfilled' ? projectsResult.value.total : 0,
    inspirationTotal: inspirationsResult.status === 'fulfilled' ? inspirationsResult.value.total : 0,
  }
  const nextSiteConfig = resolveSiteConfig(config)

  runtimeSiteConfig.value = nextSiteConfig
  runtimeHeroContent.value = resolveHeroContent(totals)
  runtimeArticles.value = buildFeaturedCards(articles, projects)
  runtimeProjects.value = buildPortfolioProjects(projects)
  runtimeCounters.value = buildCounters(totals)
  runtimeFragments.value = inspirations.slice(0, HOME_INSPIRATION_LIMIT).map(inspirationToFragment)
  runtimeThemePresets.value = themes.map(themeToPreset)
  runtimeCurrentThemeName.value = currentTheme?.displayName
    ?? themes.find((theme) => theme.active)?.displayName
    ?? (themes.length > 0 ? '默认主题' : '暂无主题')
}

const validHexColorPattern = /^#(?:[\da-f]{3,4}|[\da-f]{6}|[\da-f]{8})$/i
const HOME_ARTICLE_LIMIT = 6
const HOME_PROJECT_LIMIT = 5
const HOME_INSPIRATION_LIMIT = 6
const defaultArticlePalettes = [
  ['#1b2b4d', '#0a1326'],
  ['#3a1d4d', '#160a26'],
  ['#123a37', '#08201f'],
  ['#4d3312', '#261707'],
  ['#2f2a57', '#121028'],
] as const
const defaultProjectPalettes = [
  { from: '#16233f', to: '#080f1d', accent: '#6ea8ff' },
  { from: '#24143d', to: '#0e071d', accent: '#b18cff' },
  { from: '#10312e', to: '#061918', accent: '#54e6c8' },
  { from: '#3a2508', to: '#160d04', accent: '#ff9d6e' },
] as const
const fragmentLayouts = [
  { span: { col: 2, row: 2 }, to: '#0b1428' },
  { span: { col: 2, row: 1 }, to: '#0b1226' },
  { span: { col: 1, row: 1 }, to: '#0a1326' },
  { span: { col: 1, row: 2 }, to: '#0a0f1f' },
] as const
const fragmentKindMap = {
  IMAGE: 'image',
  TEXT: 'note',
  PROMPT: 'prompt',
  CODE: 'code',
  LINK: 'link',
} as const satisfies Record<InspirationCard['cardType'], CreativeFragment['kind']>
const homepageFragmentBackgrounds = [
  inspirationBg01,
  inspirationBg02,
  inspirationBg03,
  inspirationBg04,
  inspirationBg05,
  inspirationBg06,
] as const

type SiteTotals = {
  articleTotal: number
  projectTotal: number
  inspirationTotal: number
}

function readRecord(value: unknown): Record<string, unknown> {
  return value && typeof value === 'object' && !Array.isArray(value) ? value as Record<string, unknown> : {}
}

function readArray(value: unknown): unknown[] {
  return Array.isArray(value) ? value : []
}

function readString(value: unknown): string {
  return typeof value === 'string' ? value.trim() : ''
}

function readStringList(value: unknown): string[] {
  return readArray(value)
    .map((item) => readString(item))
    .filter(Boolean)
}

function resolveSiteConfig(config: Record<string, unknown>): SiteConfig {
  const identity = readRecord(config['site.identity'])
  const profile = readRecord(config['site.profile.active'])
  const profileJson = readRecord(profile.profileJson)
  const brand = readString(identity.name)
    || readString(profile.displayName)
    || readString(identity.title)
  const wordmark = readString(identity.wordmark) || readString(profileJson.signature) || brand
  return {
    brand,
    wordmark,
    navigation: readNavigation(config['site.navigationItems']),
    social: readSocial(config['site.socialLinks']),
  }
}

function readNavigation(value: unknown): SiteConfig['navigation'] {
  return readArray(value)
    .map((item) => {
      const record = readRecord(item)
      const label = readString(record.label)
      const to = readString(record.path)
      if (!label || !to || to.startsWith('//')) {
        return null
      }
      return { label, to, external: isExternalUrl(to) }
    })
    .filter((item): item is SiteConfig['navigation'][number] => item !== null)
}

function readSocial(value: unknown): SiteConfig['social'] {
  return readArray(value)
    .map((item) => {
      const record = readRecord(item)
      const href = safeSocialHref(readString(record.url))
      const label = readString(record.label) || readString(record.platform)
      if (!label || !href) {
        return null
      }
      return { label, handle: socialHandle(href), href }
    })
    .filter((item): item is SiteConfig['social'][number] => item !== null)
}

function safeSocialHref(value: string): string {
  if (value.startsWith('mailto:')) {
    return value
  }
  try {
    const url = new URL(value)
    return ['http:', 'https:'].includes(url.protocol) ? url.toString() : ''
  } catch {
    return ''
  }
}

function socialHandle(href: string): string {
  if (href.startsWith('mailto:')) {
    return href.replace(/^mailto:/, '')
  }
  try {
    const url = new URL(href)
    return `${url.hostname}${url.pathname}`.replace(/\/$/, '')
  } catch {
    return href
  }
}

function isExternalUrl(value: string): boolean {
  return /^https?:\/\//i.test(value)
}

function resolveHeroContent(totals: SiteTotals): HomeHeroContent {
  return {
    kicker: 'Personal Theme Blog · Creative Portfolio',
    titleLines: ['CreatorSpace', '创作主页'],
    subtitle: '一个有主题风格的个人博客与作品展示平台。',
    description: '这里不是模板化列表，而是一个带有主题气质的个人站点：文章记录思考，作品呈现过程，灵感碎片连接长期创作。',
    primary: { label: '进入博客', to: '/articles' },
    secondary: { label: '浏览作品', to: '/projects' },
    stats: [
      { value: String(totals.articleTotal), label: '主题文章' },
      { value: String(totals.projectTotal), label: '创意作品' },
      { value: String(totals.inspirationTotal), label: '灵感碎片' },
    ],
  }
}

function buildMarqueeWords(): string[] {
  return ['Articles', 'Works', 'Notes', 'Covers', 'Tags', 'Themes', 'Archive', 'Reading']
}

function buildManifesto(): ManifestoContent {
  return {
    lead: '(01) — 站点宣言',
    segments: [
      { text: '这里先是一座个人主题博客。 ', accent: false },
      { text: '文章负责把想法说清楚， ', accent: true },
      { text: '作品负责把过程留下来， ', accent: false },
      { text: '灵感卡片负责等下一次回头。', accent: true },
    ],
    cards: [
      {
        label: '阅读路径',
        value: '主题 / 标签 / 归档',
        detail: '不让读者只靠时间线找文章。',
      },
      {
        label: '视觉气质',
        value: '封面色 / 字号 / 留白',
        detail: '每个页面都要像同一个人整理出来的。',
      },
      {
        label: '内容边界',
        value: '公开 / 私密 / 草稿',
        detail: '写给别人看的内容和写给自己的内容分开放。',
      },
    ],
  }
}

function buildFeaturedCards(articles: ArticleSummary[], projects: ProjectSummary[]): FeaturedArticle[] {
  const selectedArticles = weightedShuffle(articles, featuredArticleScore)
    .slice(0, HOME_ARTICLE_LIMIT)
  const remainingSlots = Math.max(0, HOME_ARTICLE_LIMIT - selectedArticles.length)
  const supplementalProjects = remainingSlots > 0
    ? weightedShuffle(projects.slice(HOME_PROJECT_LIMIT), featuredProjectScore).slice(0, remainingSlots)
    : []
  return [
    ...selectedArticles.map(articleToFeatured),
    ...supplementalProjects.map((project, index) =>
      projectToFeatured(project, selectedArticles.length + index),
    ),
  ]
}

function weightedShuffle<T>(items: T[], score: (item: T) => number): T[] {
  return [...items]
    .map((item, index) => ({
      item,
      index,
      rank: Math.random() * (Math.max(1, score(item)) + 1),
    }))
    .sort((left, right) => right.rank - left.rank || left.index - right.index)
    .map((entry) => entry.item)
}

function featuredArticleScore(article: ArticleSummary): number {
  return (article.top ? 5 : 0)
    + (article.recommended ? 4 : 0)
    + Math.min(article.tags.length, 4)
    + heatScore(article.viewCount, article.likeCount, 0, article.commentCount)
    + recencyScore(article.publishTime)
}

function featuredProjectScore(project: ProjectSummary): number {
  return (project.recommended ? 5 : 0)
    + Math.min(project.tags.length, 4)
    + heatScore(project.viewCount, project.likeCount, project.favoriteCount, project.commentCount)
    + recencyScore(project.reviewedAt ?? project.submittedAt)
}

function heatScore(
  viewCount: number | null | undefined,
  likeCount: number | null | undefined,
  favoriteCount: number | null | undefined,
  commentCount: number | null | undefined,
): number {
  return Math.log10(Math.max(0, viewCount ?? 0) + 1)
    + Math.log10(Math.max(0, likeCount ?? 0) * 3 + 1)
    + Math.log10(Math.max(0, favoriteCount ?? 0) * 3 + 1)
    + Math.log10(Math.max(0, commentCount ?? 0) * 4 + 1)
}

function recencyScore(value: string | null | undefined): number {
  if (!value) {
    return 0
  }
  const time = new Date(value).getTime()
  if (Number.isNaN(time)) {
    return 0
  }
  const ageDays = Math.max(0, (Date.now() - time) / 86400000)
  return Math.max(0, 3 - ageDays / 60)
}

function buildPortfolioProjects(projects: ProjectSummary[]): PortfolioProject[] {
  return projects.slice(0, HOME_PROJECT_LIMIT).map(projectToPortfolio)
}

function buildStructureCards(): AgentCapability[] {
  return [
    {
      id: 'topics',
      name: '主题索引',
      role: '站点结构',
      summary: '把文章、作品和灵感放到清晰主题下，让访客能按兴趣进入，而不是被一条时间线淹没。',
      pipeline: [
        { stage: 'route', label: '选择主题' },
        { stage: 'retrieve', label: '归类内容' },
        { stage: 'generate', label: '形成入口' },
      ],
      outputs: ['主题页', '文章集合', '标签入口', '延伸阅读'],
      accent: '#6ea8ff',
    },
    {
      id: 'gallery',
      name: '作品橱窗',
      role: '创作展示',
      summary: '作品不只放截图，也要写清背景、过程、取舍和完成后的状态，像一面可翻阅的展示墙。',
      pipeline: [
        { stage: 'route', label: '整理封面' },
        { stage: 'retrieve', label: '补齐过程' },
        { stage: 'generate', label: '陈列作品' },
      ],
      outputs: ['作品封面', '过程记录', '技术标签', '成品链接'],
      accent: '#b18cff',
    },
    {
      id: 'fragments',
      name: '灵感卡片',
      role: '素材回收',
      summary: '没写成文章的句子、图片参考、链接和代码片段先被收好，之后再慢慢长成内容。',
      pipeline: [
        { stage: 'route', label: '快速记录' },
        { stage: 'retrieve', label: '补充来源' },
        { stage: 'generate', label: '关联主题' },
      ],
      outputs: ['摘句', '参考图', '链接', '代码片段'],
      accent: '#54e6c8',
    },
  ]
}

function buildApproachSteps(): ApproachStep[] {
  return [
    {
      no: '01',
      title: '捕捉',
      en: 'Capture',
      body: '句子、链接、截图和代码片段先落进灵感盒，不急着整理，也不让它们丢掉。',
      tags: ['灵感盒', '摘句', '参考'],
    },
    {
      no: '02',
      title: '整理',
      en: 'Organize',
      body: '用主题、分类和标签把内容放回合适的位置，让一篇文章自然通向下一篇。',
      tags: ['主题', '分类', '标签'],
    },
    {
      no: '03',
      title: '写作',
      en: 'Write',
      body: '把草稿写成能公开阅读的文章，补齐摘要、封面、标签和必要的上下文。',
      tags: ['草稿', '摘要', '封面'],
    },
    {
      no: '04',
      title: '展示',
      en: 'Curate',
      body: '文章和作品被编排进前台，读者看到的是一个有风格、有边界、能继续长大的个人空间。',
      tags: ['前台', '作品橱窗', '归档'],
    },
  ]
}

function buildCounters(totals: SiteTotals): CounterItem[] {
  return [
    { value: totals.articleTotal, suffix: '', label: '主题文章' },
    { value: totals.projectTotal, suffix: '', label: '创意作品' },
    { value: totals.inspirationTotal, suffix: '', label: '灵感碎片' },
  ]
}

function themeToPreset(theme: PublicThemeConfig): ThemePreset {
  const config = readRecord(theme.config)
  const accent = cssColor(config.accentColor, theme.primaryColor || '#6ea8ff')
  const background = cssColor(config.backgroundColor, '#070b18')
  const surface = cssColor(config.surfaceColor, '#11182a')
  const ink = cssColor(config.inkColor, '#eaf1ff')
  const muted = cssColor(config.mutedColor, '#99a6c4')
  return {
    id: theme.themeName,
    name: theme.displayName,
    tagline: readString(config.tagline) || theme.layoutType,
    mood: readString(config.mood) || theme.backgroundType,
    vars: {
      bg: background,
      surface,
      ink,
      muted,
      accent,
      accentSoft: cssColor(config.accentSoftColor, transparentColor(accent, 0.22)),
      font: theme.fontFamily || 'Sora',
    },
    swatches: uniqueText([background, surface, accent, muted, theme.primaryColor]).slice(0, 5),
  }
}

function cssColor(value: unknown, fallback: string): string {
  const color = readString(value)
  return color && (validHexColorPattern.test(color) || /^rgba?\(/i.test(color)) ? color : fallback
}

function transparentColor(color: string, alpha: number): string {
  const hex = color.replace('#', '')
  if (![3, 6].includes(hex.length) || !/^[\da-f]+$/i.test(hex)) {
    return color
  }
  const full = hex.length === 3 ? hex.split('').map((char) => `${char}${char}`).join('') : hex
  const r = Number.parseInt(full.slice(0, 2), 16)
  const g = Number.parseInt(full.slice(2, 4), 16)
  const b = Number.parseInt(full.slice(4, 6), 16)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

function uniqueText(values: Array<string | null | undefined>): string[] {
  const seen = new Set<string>()
  return values.filter((value): value is string => {
    const text = value?.trim()
    if (!text || seen.has(text)) {
      return false
    }
    seen.add(text)
    return true
  })
}

function safeHexColor(value: string | null | undefined, fallback: string) {
  const color = value?.trim()
  return color && validHexColorPattern.test(color) ? color : fallback
}

function firstTagColor(tags: TagSummary[], fallback: string) {
  return safeHexColor(tags.find((tag) => tag.color)?.color, fallback)
}

function formatHomeDate(value: string | null | undefined, fallback = '未定档') {
  if (!value) {
    return fallback
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return fallback
  }
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).replaceAll('/', '-')
}

function plainExcerpt(value: string | null | undefined, fallback: string) {
  const text = value
    ?.replace(/```[\s\S]*?```/g, '')
    .replace(/[#>*_`[\]()~-]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
  return text ? text.slice(0, 96) : fallback
}

function articleToFeatured(article: ArticleSummary, index: number): FeaturedArticle {
  const paletteSeed = defaultArticlePalettes[index % defaultArticlePalettes.length]
  const palette: [string, string] = [
    firstTagColor(article.tags, paletteSeed?.[0] ?? '#1b2b4d'),
    paletteSeed?.[1] ?? '#0a1326',
  ]
  return {
    id: `article-${article.id}`,
    slug: article.slug,
    kind: index === 0 ? 'feature' : 'standard',
    targetType: 'ARTICLE',
    title: article.title,
    excerpt: article.summary ?? plainExcerpt(article.contentMarkdown, '这篇文章还在整理摘要。'),
    tags: article.tags.length > 0
      ? article.tags.slice(0, 3).map((tag) => tag.name)
      : [article.category?.name ?? '主题文章'],
    readingMinutes: Math.max(1, Math.ceil((article.contentMarkdown?.length ?? article.summary?.length ?? 420) / 420)),
    publishedAt: formatHomeDate(article.publishTime),
    cover: palette,
    coverImage: article.coverUrl ?? '',
  }
}

function projectToFeatured(project: ProjectSummary, index: number): FeaturedArticle {
  const paletteSeed = defaultProjectPalettes[index % defaultProjectPalettes.length]
  const palette: [string, string] = [
    firstTagColor(project.tags, paletteSeed?.from ?? '#16233f'),
    paletteSeed?.to ?? '#080f1d',
  ]
  return {
    id: `project-feature-${project.id}`,
    slug: project.slug,
    kind: index === 0 ? 'feature' : 'standard',
    targetType: 'PROJECT',
    title: project.title,
    excerpt: project.description ?? plainExcerpt(project.contentMarkdown, '这个作品还在补充过程说明。'),
    tags: project.tags.length > 0
      ? project.tags.slice(0, 3).map((tag) => tag.name)
      : [project.projectType || '创意作品'],
    readingMinutes: Math.max(1, Math.ceil((project.contentMarkdown?.length ?? project.description?.length ?? 420) / 420)),
    publishedAt: formatHomeDate(project.reviewedAt ?? project.submittedAt, '展示中'),
    cover: palette,
    coverImage: project.coverUrl ?? '',
  }
}

function projectToPortfolio(project: ProjectSummary, index: number): PortfolioProject {
  const palette = defaultProjectPalettes[index % defaultProjectPalettes.length]
  const accent = firstTagColor(project.tags, palette?.accent ?? '#6ea8ff')
  return {
    id: `project-${project.id}`,
    slug: project.slug,
    index: String(index + 1).padStart(2, '0'),
    title: project.title,
    category: project.projectType || 'PROJECT',
    year: formatHomeDate(project.reviewedAt ?? project.submittedAt, 'LIVE'),
    description: project.description ?? '这个作品还在补充过程说明。',
    stack: project.techStack.length > 0
      ? project.techStack.slice(0, 4)
      : project.tags.slice(0, 4).map((tag) => tag.name),
    palette: {
      from: palette?.from ?? '#16233f',
      to: palette?.to ?? '#080f1d',
      accent,
    },
    posterImage: project.coverUrl ?? '',
  }
}

function inspirationToFragment(card: InspirationCard, index: number): CreativeFragment {
  const layout = fragmentLayouts[index % fragmentLayouts.length]
  const accent = safeHexColor(card.color, '#263e70')
  const backgroundImage = homepageFragmentBackgrounds[index % homepageFragmentBackgrounds.length]
  return {
    id: `inspiration-${card.id}`,
    kind: fragmentKindMap[card.cardType],
    span: layout?.span ?? { col: 1, row: 1 },
    label: card.tags[0]?.name ?? card.title,
    body: card.content ?? card.title,
    meta: card.sourceUrl ? 'source link' : formatHomeDate(card.createdAt, '灵感卡片'),
    palette: [accent, layout?.to ?? '#0b1428'],
    backgroundImage,
  }
}

const HeroUniverse = defineComponent({
  name: 'HeroUniverse',
  setup() {
    const root = ref<HTMLElement | null>(null)
    useGsapContext(root, ({ reduced }) => {
      if (reduced) {
        return
      }
      const tl = gsap.timeline({ defaults: { ease: 'expo.out' }, delay: 0.15 })
      tl.from('.cs-hero__title .cs-line > span', {
        yPercent: 110,
        duration: 1.2,
        stagger: 0.12,
      })
        .from('.cs-hero__kicker', { y: 20, opacity: 0, duration: 0.8 }, '-=0.9')
        .from('.cs-hero__sub', { y: 24, opacity: 0, duration: 0.8 }, '-=0.7')
        .from('.cs-hero__actions > *', { y: 20, opacity: 0, duration: 0.7, stagger: 0.1 }, '-=0.6')
        .from('.cs-hero__stats .cs-stat', { y: 18, opacity: 0, duration: 0.7, stagger: 0.08 }, '-=0.5')
        .from('.cs-hero__scroll', { opacity: 0, duration: 0.6 }, '-=0.3')
    })

    // 渲染英雄标题行。
    const titleLine = (text: string) =>
      h('span', { class: 'cs-line' }, [h('span', text)])

    return () => {
      const hero = runtimeHeroContent.value
      const actions = [hero?.primary, hero?.secondary].filter((item): item is HomeHeroAction => item !== null && item !== undefined)
      return h('section', { ref: root, class: 'cs-hero cs-section' }, [
        h(HeroWebGLScene),
        h('div', { class: 'cs-hero__grain' }),
        h('div', { class: 'cs-hero__inner' }, [
          h('p', { class: 'cs-eyebrow cs-hero__kicker' }, hero?.kicker ?? ''),
          h('h1', { class: 'cs-hero__title' }, (hero?.titleLines ?? []).map(titleLine)),
          h('p', { class: 'cs-hero__sub' }, [
            hero?.subtitle ?? '',
            h('span', { class: 'cs-zh' }, hero?.description ?? ''),
          ]),
          h('div', { class: 'cs-hero__actions' }, actions.map((action, index) => heroButton(action, index > 0))),
          h(
            'div',
            { class: 'cs-hero__stats' },
            (hero?.stats ?? []).map((stat) =>
              h('div', { class: 'cs-stat', key: stat.label }, [
                h('div', { class: 'cs-stat__value' }, stat.value),
                h('div', { class: 'cs-stat__label' }, stat.label),
              ]),
            ),
          ),
        ]),
        h('div', { class: 'cs-hero__scroll' }, 'Scroll to explore'),
      ])
    }
  },
})

const MarqueeManifesto = defineComponent({
  name: 'MarqueeManifesto',
  setup() {
    const root = ref<HTMLElement | null>(null)

    useGsapContext(root, ({ reduced }) => {
      if (!reduced) {
        gsap.from('.cs-mani__word', {
          opacity: 0.12,
          duration: 0.6,
          ease: 'none',
          stagger: 0.04,
          scrollTrigger: {
            trigger: '.cs-mani',
            start: 'top 80%',
            end: 'top 30%',
            scrub: true,
          },
        })
      }

      const rows = gsap.utils.toArray<HTMLElement>('.cs-marquee__row')
      rows.forEach((row, i) => {
        const inner = row.querySelector<HTMLElement>('.cs-marquee__inner')
        if (!inner) {
          return
        }
        const dir = i % 2 === 0 ? -1 : 1

        if (reduced) {
          gsap.set(inner, { xPercent: -25 })
          return
        }
        const wrap = gsap.utils.wrap(-50, 0)
        const from = dir < 0 ? 0 : -50
        const to = dir < 0 ? -50 : 0
        const baseTween = gsap.fromTo(
          inner,
          { xPercent: from },
          {
            xPercent: to,
            duration: 28,
            ease: 'none',
            repeat: -1,
            modifiers: {
              xPercent: (value) => `${wrap(parseFloat(value))}%`,
            },
          },
        )
        ScrollTrigger.create({
          trigger: root.value as Element,
          start: 'top bottom',
          end: 'bottom top',
          onUpdate: (self) => {
            const velocity = self.getVelocity()
            const scale = 1 + Math.min(Math.abs(velocity) / 1200, 3)
            baseTween.timeScale(dir * velocity < 0 ? scale * 0.4 : scale)
          },
        })
      })
    })

    // 渲染无缝滚动标语行。
    const marqueeRow = (reverse: boolean) => {
      const words = runtimeMarqueeWords.value
      const items = [...words, ...words].map((word, i) =>
        h('span', { key: `${word}-${i}`, class: 'cs-marquee__item' }, [
          word,
          h('i', { class: 'cs-marquee__star', 'aria-hidden': 'true' }, '✦'),
        ]),
      )
      return h('div', { class: ['cs-marquee__row', { 'is-reverse': reverse }] }, [
        h('div', { class: 'cs-marquee__inner' }, items),
      ])
    }

    return () => {
      const content = runtimeManifesto.value
      const segments = content?.segments ?? []
      const cards = content?.cards ?? []
      return h('section', { ref: root, id: 'manifesto', class: 'cs-marquee-sec' }, [
        h('div', { class: 'cs-marquee' }, [marqueeRow(false), marqueeRow(true)]),
        h('div', { class: 'cs-mani cs-section' }, [
          h('div', { class: 'cs-mani__copy' }, [
            h('p', { class: 'cs-eyebrow' }, content?.lead ?? ''),
            h(
              'p',
              { class: 'cs-mani__text' },
              segments.flatMap((segment) =>
                segment.text.split(' ').filter(Boolean).map((word, wordIndex) =>
                  h(
                    'span',
                    {
                      key: `${word}-${wordIndex}-${segment.accent}`,
                      class: ['cs-mani__word', { 'is-accent': segment.accent }],
                    },
                    `${word} `,
                  ),
                ),
              ),
            ),
          ]),
          h(
            'div',
            { class: 'cs-mani__cards' },
            cards.map((card) =>
              h('article', { key: card.label, class: 'cs-mani-card' }, [
                h('span', card.label),
                h('strong', card.value),
                h('p', card.detail),
              ]),
            ),
          ),
        ]),
      ])
    }
  },
})

// 根据文章类型决定卡片跨度样式。
function spanClass(article: FeaturedArticle, index: number): string {
  if (article.kind === 'feature') {
    return 'cs-article cs-article--feature'
  }
  return index % 2 === 0 ? 'cs-article cs-article--wide' : 'cs-article cs-article--tall'
}

const FeaturedArticles = defineComponent({
  name: 'FeaturedArticles',
  setup() {
    const root = ref<HTMLElement | null>(null)
    const articles = runtimeArticles
    let articleRevealTweens: gsap.core.Tween[] = []

    const killArticleReveal = () => {
      articleRevealTweens.forEach((tween) => tween.kill())
      articleRevealTweens = []
    }

    const revealCurrentCards = () => {
      const section = root.value
      if (!section) {
        return
      }
      killArticleReveal()

      const cards = Array.from(section.querySelectorAll<HTMLElement>('.cs-article'))
      const reduced = prefersReducedMotion()
      if (reduced) {
        cards.forEach((card) => {
          gsap.set(card, { clipPath: 'inset(0 0 0% 0)' })
          const wash = card.querySelector('.cs-article__wash')
          if (wash) {
            gsap.set(wash, { yPercent: 0 })
          }
        })
        return
      }

      cards.forEach((card, i) => {
        const isFeature = card.classList.contains('cs-article--feature')
        articleRevealTweens.push(
          gsap.fromTo(
            card,
            { clipPath: 'inset(0 0 100% 0)' },
            {
              clipPath: 'inset(0 0 0% 0)',
              duration: isFeature ? 1.25 : 0.9,
              delay: (i % 3) * 0.08,
              ease: 'power3.inOut',
              scrollTrigger: {
                trigger: card,
                start: 'top 85%',
              },
            },
          ),
        )
        const wash = card.querySelector('.cs-article__wash')
        if (wash) {
          articleRevealTweens.push(
            gsap.from(wash, {
              yPercent: 12,
              duration: isFeature ? 1.4 : 1,
              ease: 'power3.out',
              scrollTrigger: { trigger: card, start: 'top 85%' },
            }),
          )
        }
      })
    }

    // 根据指针位置给文章卡片添加轻微倾斜。
    const onTilt = (event: PointerEvent) => {
      const card = event.currentTarget as HTMLElement
      const rect = card.getBoundingClientRect()
      const px = (event.clientX - rect.left) / rect.width - 0.5
      const py = (event.clientY - rect.top) / rect.height - 0.5
      gsap.to(card, {
        rotateY: px * 5,
        rotateX: -py * 5,
        transformPerspective: 900,
        duration: 0.5,
        ease: 'power2.out',
      })
    }

    // 指针离开后复位文章卡片倾斜。
    const onTiltOut = (event: PointerEvent) => {
      gsap.to(event.currentTarget as HTMLElement, {
        rotateY: 0,
        rotateX: 0,
        duration: 0.7,
        ease: 'power3.out',
      })
    }

    onMounted(() => {
      revealCurrentCards()
    })

    watch(articles, async () => {
      await nextTick()
      revealCurrentCards()
      requestAnimationFrame(() => ScrollTrigger.refresh())
    }, { flush: 'post' })

    onBeforeUnmount(() => {
      killArticleReveal()
    })

    // 渲染精选文章卡片。
    const renderCard = (article: FeaturedArticle, index: number) =>
      h(
        RouterLink,
        {
          key: article.id,
          to: {
            name: article.targetType === 'PROJECT' ? 'project-detail' : 'article-detail',
            params: { slug: article.slug },
          },
          class: spanClass(article, index),
          style: { '--cs-from': article.cover[0], '--cs-to': article.cover[1] },
          onPointermove: onTilt,
          onPointerleave: onTiltOut,
        },
        {
          default: () => [
            h('span', {
              class: 'cs-article__wash',
              style: {
                '--cs-from': article.cover[0],
                '--cs-to': article.cover[1],
                '--cs-cover': toCssImageUrl(article.coverImage),
              },
            }),
            h('div', { class: 'cs-article__body' }, [
              h(
                'div',
                { class: 'cs-article__tags' },
                article.tags.map((tag) => h('span', { class: 'cs-tag', key: tag }, tag)),
              ),
              h('h3', { class: 'cs-article__title' }, article.title),
              h('p', { class: 'cs-article__excerpt' }, article.excerpt),
              h('div', { class: 'cs-article__meta' }, [
                h('span', article.targetType === 'PROJECT' ? '作品记录' : `${article.readingMinutes} 分钟阅读`),
                h('span', article.publishedAt),
              ]),
            ]),
          ],
        },
      )

    return () =>
      h('section', { ref: root, class: 'cs-articles cs-section', id: 'articles' }, [
        h('div', { class: 'cs-head' }, [
          h('div', [
            h('p', { class: 'cs-eyebrow' }, 'Featured Writing'),
            h('h2', { class: 'cs-head__title' }, '让文章像主题房间，而不是普通列表'),
          ]),
          h(
            'p',
            { class: 'cs-head__note' },
            '精选文章会带着封面、标签和摘要出现，读者能先感受到这篇内容的气质。',
          ),
        ]),
        h(
          'div',
          { class: 'cs-articles__grid' },
          articles.value.map((article, index) => renderCard(article, index)),
        ),
      ])
  },
})

const PortfolioGallery = defineComponent({
  name: 'PortfolioGallery',
  setup() {
    const root = ref<HTMLElement | null>(null)
    const projects = runtimeProjects
    const stacked = ref(isStackedViewport())
    const stackQuery =
      typeof window !== 'undefined' ? window.matchMedia('(max-width: 900px)') : null
    let matchMediaContext: gsap.MatchMedia | null = null

    // 判断当前是否应使用堆叠布局。
    function isStackedViewport(): boolean {
      if (typeof window === 'undefined') {
        return false
      }
      return window.matchMedia('(max-width: 900px)').matches
    }

    // 响应堆叠布局媒体查询变化。
    const onStackChange = (event: MediaQueryListEvent) => {
      stacked.value = event.matches
    }

    onMounted(() => {
      stackQuery?.addEventListener('change', onStackChange)
      matchMediaContext = gsap.matchMedia()
      matchMediaContext.add(
        '(min-width: 901px) and (prefers-reduced-motion: no-preference)',
        () => {
          const pin = root.value?.querySelector<HTMLElement>('.cs-gallery__pin')
          const track = root.value?.querySelector<HTMLElement>('.cs-gallery__track')
          if (!pin || !track) {
            return
          }
          const getScrollLength = () => Math.max(0, track.scrollWidth - window.innerWidth)

          const tween = gsap.to(track, {
            x: () => -getScrollLength(),
            ease: 'none',
            scrollTrigger: {
              trigger: pin,
              start: 'top top',
              end: () => `+=${getScrollLength()}`,
              pin: true,
              scrub: 0.6,
              invalidateOnRefresh: true,
              anticipatePin: 1,
            },
          })
          gsap.utils.toArray<HTMLElement>('.cs-poster').forEach((poster) => {
            const layer = poster.querySelector('.cs-poster__parallax')
            if (!layer) {
              return
            }
            gsap.fromTo(
              layer,
              { xPercent: -8 },
              {
                xPercent: 8,
                ease: 'none',
                scrollTrigger: {
                  trigger: poster,
                  containerAnimation: tween,
                  start: 'left right',
                  end: 'right left',
                  scrub: true,
                },
              },
            )
          })
        },
        root.value ?? undefined,
      )
    })

    watch(projects, async () => {
      await nextTick()
      ScrollTrigger.refresh()
    }, { flush: 'post' })

    onBeforeUnmount(() => {
      stackQuery?.removeEventListener('change', onStackChange)
      matchMediaContext?.revert()
      matchMediaContext = null
    })

    // 渲染作品海报卡片。
    const renderPoster = (project: PortfolioProject) =>
      h(
        RouterLink,
        {
          key: project.id,
          to: { name: 'project-detail', params: { slug: project.slug } },
          class: 'cs-poster',
          style: {
            '--cs-from': project.palette.from,
            '--cs-to': project.palette.to,
            '--cs-accent': project.palette.accent,
            '--cs-poster': toCssImageUrl(project.posterImage),
          },
        },
        {
          default: () => [
            h('span', { class: 'cs-poster__parallax', style: { '--cs-accent': project.palette.accent } }),
            h('span', { class: 'cs-poster__grain' }),
            h('div', { class: 'cs-poster__content' }, [
              h('div', { class: 'cs-poster__top' }, [
                h('span', { class: 'cs-poster__index' }, `${project.index} / ${project.year}`),
              ]),
              h('div', [
                h('span', { class: 'cs-poster__cat' }, project.category),
                h('h3', { class: 'cs-poster__title' }, project.title),
                h('p', { class: 'cs-poster__desc' }, project.description),
                h(
                  'div',
                  { class: 'cs-poster__stack' },
                  project.stack.map((tech) => h('span', { key: tech }, tech)),
                ),
              ]),
            ]),
          ],
        },
      )

    return () =>
      h(
        'section',
        {
          ref: root,
          id: 'works',
          class: ['cs-gallery', { 'cs-gallery--stacked': stacked.value }],
        },
        [
          h('div', { class: 'cs-gallery__pin' }, [
            h('div', { class: 'cs-gallery__track' }, [
              h('div', { class: 'cs-gallery__intro' }, [
                h('p', { class: 'cs-eyebrow' }, '作品橱窗'),
                h('h2', '作品是博客之外的另一种表达'),
                h(
                  'p',
                  '这里展示能看见形状的创作：页面、工具、视觉实验和长期打磨过的小作品。',
                ),
                h('span', { class: 'cs-gallery__hint' }, stacked.value ? '继续往下' : '横向浏览作品 →'),
              ]),
              ...projects.value.map(renderPoster),
              h('div', { class: 'cs-gallery__outro' }, [
                h('p', { class: 'cs-eyebrow' }, 'End of wall'),
                h('h2', { class: 'cs-gallery__outro-title' }, '更多作品正在归档中'),
              ]),
            ]),
          ]),
        ],
      )
  },
})

const SiteStructureShowcase = defineComponent({
  name: 'SiteStructureShowcase',
  setup() {
    const root = ref<HTMLElement | null>(null)
    const stage = ref<HTMLElement | null>(null)
    const spineEl = ref<SVGPathElement | null>(null)
    const pulseEl = ref<SVGPathElement | null>(null)
    const svgW = ref(1200)
    const svgH = ref(360)
    const spineD = ref('')
    const showFlowPulses = !prefersReducedMotion()
    let resizeObserver: ResizeObserver | null = null

    // 根据节点位置生成平滑连线路径。
    function buildSpine(nodeXs: number[], width: number, y: number, amp: number): string {
      if (nodeXs.length === 0) {
        return `M 0 ${y} L ${width} ${y}`
      }
      const points = [0, ...nodeXs, width]
      let d = `M ${points[0]} ${y}`
      for (let i = 1; i < points.length; i++) {
        const prev = points[i - 1] as number
        const curr = points[i] as number
        const midX = (prev + curr) / 2
        const bow = i % 2 === 0 ? -amp : amp
        d += ` C ${midX} ${y + bow}, ${midX} ${y - bow}, ${curr} ${y}`
      }
      return d
    }

    // 重新测量卡片节点并更新路径。
    const remeasure = () => {
      const stageEl = stage.value
      if (!stageEl) {
        return
      }
      const rect = stageEl.getBoundingClientRect()
      svgW.value = rect.width
      svgH.value = rect.height
      const cards = Array.from(stageEl.querySelectorAll<HTMLElement>('.cs-agent'))
      const y = rect.height / 2
      const xs = cards.map((card) => {
        const cr = card.getBoundingClientRect()
        return cr.left - rect.left + cr.width / 2
      })
      spineD.value = buildSpine(xs, rect.width, y, Math.min(40, rect.height * 0.12))
      spineEl.value?.setAttribute('d', spineD.value)
      pulseEl.value?.setAttribute('d', spineD.value)
    }

    useGsapContext(root, ({ reduced }) => {
      remeasure()
      if (reduced) {
        return
      }
      const spine = spineEl.value
      const pulse = pulseEl.value
      if (spine && pulse) {
        const len = spine.getTotalLength()
        gsap.set(spine, { strokeDasharray: len, strokeDashoffset: len })
        gsap.to(spine, {
          strokeDashoffset: 0,
          duration: 1.4,
          ease: 'power2.inOut',
          scrollTrigger: { trigger: stage.value as Element, start: 'top 78%' },
        })
        const pulseLen = pulse.getTotalLength()
        const dash = Math.max(60, pulseLen * 0.08)
        pulse.setAttribute('stroke-dasharray', `${dash} ${pulseLen}`)
        gsap.fromTo(
          pulse,
          { strokeDashoffset: pulseLen },
          {
            strokeDashoffset: -dash,
            duration: 3.4,
            ease: 'none',
            repeat: -1,
            scrollTrigger: { trigger: stage.value as Element, start: 'top 80%' },
          },
        )
      }
      gsap.utils.toArray<HTMLElement>('.cs-agent').forEach((card, cardIndex) => {
        const nodes = Array.from(card.querySelectorAll<HTMLElement>('.cs-pipe__node'))
        const loop = gsap.timeline({ repeat: -1, repeatDelay: 0.6, delay: cardIndex * 0.5 })
        nodes.forEach((node) => {
          loop
            .call(() => node.classList.add('is-active'))
            .to({}, { duration: 0.55 })
            .call(() => node.classList.remove('is-active'))
        })
        ScrollTrigger.create({
          trigger: card,
          start: 'top 85%',
          onEnter: () => loop.play(0),
          onLeaveBack: () => loop.pause(0),
        })
      })
    })

    onMounted(() => {
      if (prefersReducedMotion()) {
        return
      }
      resizeObserver = new ResizeObserver(() => {
        remeasure()
        ScrollTrigger.refresh()
      })
      if (stage.value) {
        resizeObserver.observe(stage.value)
      }
    })

    watch(runtimeStructureCards, async () => {
      await nextTick()
      remeasure()
      ScrollTrigger.refresh()
    }, { flush: 'post' })

    onBeforeUnmount(() => {
      resizeObserver?.disconnect()
      resizeObserver = null
    })

    // 渲染站点能力卡片。
    const renderAgent = (agent: AgentCapability) =>
      h(
        'article',
        {
          key: agent.id,
          class: 'cs-agent',
          style: { '--cs-agent-accent': agent.accent },
        },
        [
          h('div', { class: 'cs-agent__top' }, [
            h('span', { class: 'cs-agent__glyph' }, [h('i')]),
            h('span', { class: 'cs-agent__role' }, agent.role),
          ]),
          h('h3', { class: 'cs-agent__name' }, agent.name),
          h('p', { class: 'cs-agent__summary' }, agent.summary),
          h(
            'div',
            { class: 'cs-pipe' },
            agent.pipeline.flatMap((step, i) => {
              const node = h('div', { class: 'cs-pipe__node', key: step.stage }, [
                h('div', { class: 'cs-pipe__dot' }),
                h('div', { class: 'cs-pipe__label' }, step.label),
              ])
              return i < agent.pipeline.length - 1
                ? [node, h('div', { class: 'cs-pipe__link', key: `${step.stage}-link` })]
                : [node]
            }),
          ),
          h(
            'div',
            { class: 'cs-agent__outputs' },
            agent.outputs.map((out) => h('span', { key: out }, out)),
          ),
        ],
      )

    return () =>
      h('section', { ref: root, class: 'cs-agents cs-section', id: 'agents' }, [
        h('div', { class: 'cs-head' }, [
          h('div', [
            h('p', { class: 'cs-eyebrow' }, 'AI Flow'),
            h('h2', { class: 'cs-head__title' }, '让内容像在时间轴里被调度'),
          ]),
          h(
            'p',
            { class: 'cs-head__note' },
            '主题、作品和灵感沿同一条 SVG 路径流动，表现内容从采集、理解到编排的工作节奏。',
          ),
        ]),
        h('div', { ref: stage, class: 'cs-agents__stage' }, [
          h(
            'svg',
            {
              class: 'cs-agents__wires',
              viewBox: `0 0 ${svgW.value} ${svgH.value}`,
              preserveAspectRatio: 'none',
            },
            [
              h('defs', [
                h('linearGradient', { id: 'cs-flow-gradient', x1: '0%', y1: '0%', x2: '100%', y2: '0%' }, [
                  h('stop', { offset: '0%', 'stop-color': '#6ea8ff' }),
                  h('stop', { offset: '52%', 'stop-color': '#54e6c8' }),
                  h('stop', { offset: '100%', 'stop-color': '#ff9d6e' }),
                ]),
              ]),
              h('path', { ref: spineEl, class: 'cs-wire', d: spineD.value }),
              h('path', { ref: pulseEl, class: 'cs-wire cs-wire--live', d: spineD.value }),
              ...(showFlowPulses && spineD.value
                ? [0, 1.15, 2.3].map((begin) =>
                    h('circle', { key: begin, class: 'cs-wire__pulse', r: 4 }, [
                      h('animateMotion', {
                        dur: '4.6s',
                        begin: `${begin}s`,
                        repeatCount: 'indefinite',
                        path: spineD.value,
                      }),
                    ]),
                  )
                : []),
            ],
          ),
          ...runtimeStructureCards.value.map(renderAgent),
        ]),
      ])
  },
})

const ApproachProcess = defineComponent({
  name: 'ApproachProcess',
  setup() {
    const root = ref<HTMLElement | null>(null)
    const activeIndex = ref(0)
    let removeRailListeners: (() => void) | null = null

    useGsapContext(root, ({ reduced }) => {
      const section = root.value
      if (!section) {
        return
      }
      const steps = Array.from(section.querySelectorAll<HTMLElement>('.cs-step'))
      let railRaf = 0
      const updateActiveStep = () => {
        if (steps.length === 0) {
          return
        }
        const railNumber = section.querySelector<HTMLElement>('.cs-rail__num')
        const railRect = railNumber?.getBoundingClientRect()
        const readingLine = railRect ? railRect.top + railRect.height * 0.5 : Math.min(window.innerHeight * 0.38, 360)
        const nextIndex = steps.reduce((current, step, index) => {
          const heading = step.querySelector<HTMLElement>('.cs-step__head') ?? step
          return heading.getBoundingClientRect().top <= readingLine ? index : current
        }, 0)
        if (nextIndex !== activeIndex.value) {
          activeIndex.value = nextIndex
        }
      }
      const requestActiveStepUpdate = () => {
        if (railRaf) {
          return
        }
        railRaf = requestAnimationFrame(() => {
          railRaf = 0
          updateActiveStep()
        })
      }
      removeRailListeners = () => {
        window.removeEventListener('scroll', requestActiveStepUpdate)
        window.removeEventListener('resize', requestActiveStepUpdate)
        if (railRaf) {
          cancelAnimationFrame(railRaf)
          railRaf = 0
        }
      }
      window.addEventListener('scroll', requestActiveStepUpdate, { passive: true })
      window.addEventListener('resize', requestActiveStepUpdate)

      updateActiveStep()

      steps.forEach((step) => {
        if (!reduced) {
          gsap.from(step, {
            opacity: 0,
            y: 40,
            duration: 0.7,
            ease: 'power3.out',
            scrollTrigger: { trigger: step, start: 'top 82%' },
          })
        }
      })
    })

    onBeforeUnmount(() => {
      removeRailListeners?.()
      removeRailListeners = null
    })

    // 渲染流程步骤。
    const renderStep = (step: ApproachStep) =>
      h('article', { key: step.no, class: 'cs-step' }, [
        h('div', { class: 'cs-step__head' }, [
          h('span', { class: 'cs-step__no' }, step.no),
          h('div', [
            h('h3', { class: 'cs-step__title' }, [
              step.title,
              h('span', { class: 'cs-step__en' }, step.en),
            ]),
          ]),
        ]),
        h('p', { class: 'cs-step__body' }, step.body),
        h(
          'div',
          { class: 'cs-step__tags' },
          step.tags.map((tag) => h('span', { key: tag, class: 'cs-tag-line' }, tag)),
        ),
      ])

    return () => {
      const steps = runtimeApproachSteps.value
      return h('section', { ref: root, class: 'cs-approach cs-section', id: 'approach' }, [
        h('div', { class: 'cs-head' }, [
          h('div', [
            h('p', { class: 'cs-eyebrow' }, 'How It Works'),
            h('h2', { class: 'cs-head__title' }, '从碎片到作品的四步'),
          ]),
          h(
            'p',
            { class: 'cs-head__note' },
            'A creative pipeline — capture, connect, compose, curate. 系统让创作有迹可循。',
          ),
        ]),
        h('div', { class: 'cs-approach__grid' }, [
          h('aside', { class: 'cs-rail' }, [
            h('div', { class: 'cs-rail__num' }, steps[activeIndex.value]?.no ?? '01'),
            h('div', { class: 'cs-rail__label' }, steps[activeIndex.value]?.en ?? ''),
            h(
              'div',
              { class: 'cs-rail__track' },
              steps.map((step, index) =>
                h('span', {
                  key: step.no,
                  class: ['cs-rail__tick', { 'is-active': index === activeIndex.value }],
                }),
              ),
            ),
          ]),
          h('div', { class: 'cs-steps' }, steps.map(renderStep)),
        ]),
      ])
    }
  },
})

const CreativeWall = defineComponent({
  name: 'CreativeWall',
  setup() {
    const root = ref<HTMLElement | null>(null)
    const fragments = runtimeFragments
    const detachers: Array<() => void> = []

    const bindMagneticCards = () => {
      detachers.forEach((off) => off())
      detachers.length = 0
      const cards = Array.from(root.value?.querySelectorAll<HTMLElement>('.cs-frag') ?? [])
      cards.forEach((card) => {
        const inner = card.querySelector<HTMLElement>('.cs-frag__inner')
        detachers.push(attachMagnetic(card, { inner, strength: 0.22, innerStrength: 0.4 }))
      })
    }

    useGsapContext(root, ({ reduced }) => {
      const cards = gsap.utils.toArray<HTMLElement>('.cs-frag')

      if (!reduced) {
        gsap.from(cards, {
          opacity: 0,
          scale: 0.86,
          filter: 'blur(8px)',
          duration: 0.8,
          ease: 'power3.out',
          stagger: { each: 0.06, from: 'center', grid: 'auto' },
          scrollTrigger: { trigger: root.value as Element, start: 'top 72%' },
        })
      }
    })

    onMounted(() => {
      bindMagneticCards()
    })

    watch(fragments, async () => {
      await nextTick()
      bindMagneticCards()
      ScrollTrigger.refresh()
    }, { flush: 'post' })

    onBeforeUnmount(() => {
      detachers.forEach((off) => off())
      detachers.length = 0
    })

    // 渲染创意墙卡片。
    const renderFragment = (fragment: CreativeFragment) => {
      const style: Record<string, string> = {
        gridColumn: `span ${fragment.span.col}`,
        gridRow: `span ${fragment.span.row}`,
      }
      if (fragment.palette) {
        style['--cs-from'] = fragment.palette[0]
        style['--cs-to'] = fragment.palette[1]
      }
      if (fragment.backgroundImage) {
        style['--cs-frag-bg'] = toCssImageUrl(fragment.backgroundImage)
      }

      const inner = [
        h('span', { class: 'cs-frag__kind' }, fragment.label),
        h('p', { class: 'cs-frag__body' }, fragment.body),
      ]
      if (fragment.kind === 'link') {
        inner.push(h('span', { class: 'cs-frag__link' }, '↗ open'))
      } else if (fragment.meta) {
        inner.push(h('span', { class: 'cs-frag__meta' }, fragment.meta))
      }

      return h(
        'div',
        { key: fragment.id, class: `cs-frag cs-frag--${fragment.kind}`, style },
        [
          fragment.palette || fragment.backgroundImage ? h('span', { class: 'cs-frag__wash' }) : null,
          h('div', { class: 'cs-frag__inner' }, inner),
        ],
      )
    }

    return () =>
      h('section', { ref: root, class: 'cs-wall cs-section', id: 'wall' }, [
        h('div', { class: 'cs-head' }, [
          h('div', [
            h('p', { class: 'cs-eyebrow' }, 'Writing Desk'),
            h('h2', { class: 'cs-head__title' }, '灵感碎片，先放在写作桌上'),
          ]),
          h(
            'p',
            { class: 'cs-head__note' },
            '摘句、封面草图、阅读路径和语气规则，都是文章长出来之前的原材料。',
          ),
        ]),
        h('div', { class: 'cs-wall__grid' }, fragments.value.map(renderFragment)),
      ])
  },
})

const ThemeUniverse = defineComponent({
  name: 'ThemeUniverse',
  setup() {
    const activeId = ref<string>('')
    const preview = ref<HTMLElement | null>(null)

    // 把主题变量写入预览容器。
    const applyVars = (preset: ThemePreset) => {
      const el = preview.value
      if (!el) {
        return
      }
      const vars = preset.vars
      el.style.setProperty('--tp-bg', vars.bg)
      el.style.setProperty('--tp-surface', vars.surface)
      el.style.setProperty('--tp-ink', vars.ink)
      el.style.setProperty('--tp-muted', vars.muted)
      el.style.setProperty('--tp-accent', vars.accent)
      el.style.setProperty('--tp-accent-soft', vars.accentSoft)
      el.style.setProperty('--tp-font', vars.font)
    }

    // 切换当前预览主题。
    const select = (preset: ThemePreset) => {
      if (preset.id === activeId.value) {
        return
      }
      activeId.value = preset.id
      applyVars(preset)
      if (prefersReducedMotion() || !preview.value) {
        return
      }
      gsap.fromTo(
        preview.value.querySelectorAll('.cs-tp-fade'),
        { opacity: 0.3, y: 10 },
        { opacity: 1, y: 0, duration: 0.6, ease: 'power2.out', stagger: 0.05 },
      )
    }

    // 保存主题预览容器引用。
    const setPreviewRef = (el: Element | null) => {
      preview.value = el as HTMLElement | null
      const first = runtimeThemePresets.value.find((preset) => preset.id === activeId.value)
      if (first) {
        applyVars(first)
      }
    }

    onBeforeUnmount(() => {
      if (preview.value) {
        gsap.killTweensOf(preview.value.querySelectorAll('.cs-tp-fade'))
      }
    })

    watch(runtimeThemePresets, (presets) => {
      if (presets.length === 0) {
        activeId.value = ''
        return
      }
      const active = presets.find((preset) => preset.name === runtimeCurrentThemeName.value) ?? presets[0]
      if (active && !presets.some((preset) => preset.id === activeId.value)) {
        activeId.value = active.id
      }
      const selected = presets.find((preset) => preset.id === activeId.value) ?? active
      if (selected) {
        applyVars(selected)
      }
    }, { immediate: true, flush: 'post' })

    // 渲染主题切换按钮。
    const renderThemeButton = (preset: ThemePreset) =>
      h(
        'button',
        {
          key: preset.id,
          type: 'button',
          class: ['cs-theme-btn', { 'is-active': preset.id === activeId.value }],
          onClick: () => select(preset),
        },
        [
          h(
            'span',
            { class: 'cs-theme-btn__swatch' },
            preset.swatches.map((color, index) => h('span', { key: index, style: { background: color } })),
          ),
          h('span', [
            h('span', { class: 'cs-theme-btn__name' }, preset.name),
            h('span', { class: 'cs-theme-btn__tag' }, preset.tagline),
          ]),
        ],
      )

    // 读取当前激活主题配置。
    const activePreset = () =>
      runtimeThemePresets.value.find((preset) => preset.id === activeId.value) ?? runtimeThemePresets.value[0]

    const previewCount = (keyword: string) =>
      runtimeCounters.value.find((counter) => counter.label.includes(keyword))

    return () => {
      const active = activePreset()
      const articleCounter = previewCount('文章')
      const projectCounter = previewCount('作品')
      return h('section', { class: 'cs-themes cs-section', id: 'themes' }, [
        h('div', { class: 'cs-head' }, [
          h('div', [
            h('p', { class: 'cs-eyebrow' }, 'Theme Universe'),
            h('h2', { class: 'cs-head__title' }, '一个空间，多种气质'),
          ]),
          h(
            'p',
            { class: 'cs-head__note' },
            '主题是系统能力，不只是皮肤。点击切换，预览区会实时重新着色。',
          ),
        ]),
        h('div', { class: 'cs-themes__layout' }, [
          h('div', { class: 'cs-themes__list' }, runtimeThemePresets.value.map(renderThemeButton)),
          h('div', { class: 'cs-theme-preview', ref: setPreviewRef }, [
            h('p', { class: 'cs-theme-preview__eyebrow cs-tp-fade' }, active?.mood ?? ''),
            h('h3', { class: 'cs-theme-preview__title cs-tp-fade' }, active?.name ?? ''),
            h(
              'p',
              { class: 'cs-theme-preview__text cs-tp-fade' },
              '文章、作品与灵感会跟着主题重新着色，读起来像同一个人慢慢整理出的空间。',
            ),
            h('div', { class: 'cs-theme-preview__cards cs-tp-fade' }, [
              h('div', { class: 'cs-theme-preview__card' }, [
                h('strong', articleCounter ? `${articleCounter.value}${articleCounter.suffix}` : '0'),
                h('span', articleCounter?.label ?? '文章'),
              ]),
              h('div', { class: 'cs-theme-preview__card' }, [
                h('strong', projectCounter ? `${projectCounter.value}${projectCounter.suffix}` : '0'),
                h('span', projectCounter?.label ?? '作品'),
              ]),
            ]),
            h('span', { class: 'cs-theme-preview__chip cs-tp-fade' }, `当前启用 · ${runtimeCurrentThemeName.value}`),
          ]),
        ]),
      ])
    }
  },
})

const CLOSING_LINE = '从一篇文章开始'

const FinalCTA = defineComponent({
  name: 'FinalCTA',
  setup() {
    const root = ref<HTMLElement | null>(null)
    const glow = ref<HTMLElement | null>(null)
    let glowX: gsap.QuickToFunc | null = null
    let glowY: gsap.QuickToFunc | null = null

    useGsapContext(root, ({ reduced }) => {
      if (reduced) {
        return
      }
      gsap.from('.cs-cta__line', {
        y: 16,
        opacity: 0,
        duration: 0.8,
        ease: 'expo.out',
        scrollTrigger: { trigger: root.value as Element, start: 'top 75%' },
      })
      if (glow.value) {
        glowX = gsap.quickTo(glow.value, 'x', { duration: 0.8, ease: 'power3.out' })
        glowY = gsap.quickTo(glow.value, 'y', { duration: 0.8, ease: 'power3.out' })
      }
    })

    // 根据指针位置移动光晕。
    const onMove = (event: PointerEvent) => {
      const el = root.value
      if (!el || !glowX || !glowY) {
        return
      }
      const rect = el.getBoundingClientRect()
      glowX(event.clientX - rect.left - rect.width / 2)
      glowY(event.clientY - rect.top - rect.height / 2)
    }

    // 处理单字悬停动效。
    const onCharOver = (event: PointerEvent) => {
      if (prefersReducedMotion()) {
        return
      }
      const target = event.target as HTMLElement
      if (target.classList.contains('cs-char')) {
        gsap.to(target, { yPercent: -18, color: 'var(--cs-accent-3)', duration: 0.3, ease: 'power2.out' })
        gsap.to(target, { yPercent: 0, color: 'inherit', duration: 0.6, delay: 0.15, ease: 'power3.out' })
      }
    }

    onBeforeUnmount(() => {
      if (glow.value) {
        gsap.killTweensOf(glow.value)
      }
    })

    // 拆分并渲染行动标题字符。
    const chars = () =>
      CLOSING_LINE.split('').map((char, index) =>
        char === ' '
          ? h('span', { key: index, class: 'cs-char', innerHTML: '&nbsp;' })
          : h('span', { key: index, class: 'cs-char' }, char),
      )

    return () =>
      h('section', { ref: root, class: 'cs-cta-wrap', id: 'enter', onPointermove: onMove }, [
        h('div', { class: 'cs-cta' }, [
          h('div', { ref: glow, class: 'cs-cta__glow' }),
          h('p', { class: 'cs-eyebrow cs-cta__eyebrow' }, runtimeSiteConfig.value?.brand ?? ''),
          h('h2', { class: 'cs-cta__line', onPointerover: onCharOver }, chars()),
          h(
            'p',
            { class: 'cs-cta__sub' },
            '先随便读一篇，再决定要不要留下来逛作品和灵感卡片。',
          ),
          h('div', { class: 'cs-cta__action' }, [
            h(
              RouterLink,
              { to: '/articles', class: 'cs-btn' },
              {
                default: () => [
                  h('span', { class: 'cs-btn__fill' }),
                  h('span', { class: 'cs-btn__dot' }),
                  h('span', '进入博客'),
                ],
              },
            ),
          ]),
        ]),
        renderFooter(),
      ])
  },
})

// 渲染首页页脚。
function renderFooter() {
  const site = runtimeSiteConfig.value
  const links = (site?.social ?? []).slice(0, 2)

  return h('footer', { class: 'cs-footer' }, [
    h('span', `© ${new Date().getFullYear()} ${site?.wordmark ?? ''}`),
    ...links.map((link) =>
      h(
        'a',
        {
          key: link.href,
          href: link.href,
          target: link.href?.startsWith('mailto:') ? undefined : '_blank',
          rel: link.href?.startsWith('mailto:') ? undefined : 'noreferrer',
        },
        `${link.label} · ${link.handle}`,
      ),
    ),
  ])
}
</script>

<style scoped>
@property --tp-bg {
  syntax: '<color>';
  inherits: true;
  initial-value: #070b18;
}

@property --tp-surface {
  syntax: '<color>';
  inherits: true;
  initial-value: rgba(120, 170, 255, 0.08);
}

@property --tp-ink {
  syntax: '<color>';
  inherits: true;
  initial-value: #eaf1ff;
}

@property --tp-muted {
  syntax: '<color>';
  inherits: true;
  initial-value: rgba(200, 214, 255, 0.6);
}

@property --tp-accent {
  syntax: '<color>';
  inherits: true;
  initial-value: #6ea8ff;
}

@property --tp-accent-soft {
  syntax: '<color>';
  inherits: true;
  initial-value: rgba(110, 168, 255, 0.22);
}

.cs-home {

  --cs-bg: #06070d;
  --cs-bg-raise: #0b0d18;
  --cs-bg-veil: #10131f;
  --cs-ink: #f3f5ff;
  --cs-ink-dim: rgba(226, 230, 247, 0.66);
  --cs-ink-faint: rgba(206, 212, 240, 0.4);
  --cs-line: rgba(150, 165, 220, 0.16);
  --cs-line-soft: rgba(150, 165, 220, 0.09);


  --cs-accent: #6ea8ff;
  --cs-accent-2: #b18cff;
  --cs-accent-3: #54e6c8;
  --cs-accent-warm: #ff9d6e;

  --cs-edge: clamp(20px, 5vw, 92px);
  --cs-maxw: 1440px;
  --cs-font-display: 'Space Grotesk', 'Sora', system-ui, sans-serif;
  --cs-font-body: 'Sora', system-ui, sans-serif;
  --cs-font-serif: 'Newsreader', Georgia, serif;
  --cs-font-mono: 'Space Mono', ui-monospace, monospace;
}

.cs-home {
  position: relative;
  width: 100%;
  background: var(--cs-bg);
  color: var(--cs-ink);
  font-family: var(--cs-font-body);
  -webkit-font-smoothing: antialiased;
  overflow-x: clip;
}

.cs-home :deep(*),
.cs-home :deep(*::before),
.cs-home :deep(*::after) {
  box-sizing: border-box;
}


.cs-home::before {
  content: '';
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background:
    radial-gradient(120% 80% at 50% -10%, rgba(110, 168, 255, 0.08), transparent 60%),
    radial-gradient(90% 60% at 100% 100%, rgba(177, 140, 255, 0.06), transparent 55%);
}

.cs-home :deep(.cs-section) {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: var(--cs-maxw);
  margin: 0 auto;
  padding-inline: var(--cs-edge);
}

.cs-home :deep(.cs-eyebrow) {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin: 0;
  font-family: var(--cs-font-mono);
  font-size: 12px;
  letter-spacing: 0.32em;
  text-transform: uppercase;
  color: var(--cs-ink-faint);
}

.cs-home :deep(.cs-eyebrow::before) {
  content: '';
  width: 26px;
  height: 1px;
  background: linear-gradient(90deg, var(--cs-accent), transparent);
}


:global(body.cs-dark-body) {
  background: #06070d;
}

:global(html.cs-dark-scroll) {
  scrollbar-width: none;
  scrollbar-gutter: auto;
}

:global(html.cs-dark-scroll::-webkit-scrollbar) {
  width: 0;
  height: 0;
}





















.cs-home :deep(.cs-hero) {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
  max-width: none;
  min-height: 100vh;
  margin: 0;
  padding-left: max(var(--cs-edge), calc((100vw - var(--cs-maxw)) / 2 + var(--cs-edge)));
  padding-right: max(var(--cs-edge), calc((100vw - var(--cs-maxw)) / 2 + var(--cs-edge)));
  padding-top: 120px;
  padding-bottom: 96px;
  overflow: hidden;
}


.cs-home :deep(.cs-hero::before) {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at 9% 18%, rgba(166, 196, 255, 0.75) 0 1px, transparent 1.7px),
    radial-gradient(circle at 16% 66%, rgba(166, 196, 255, 0.38) 0 1px, transparent 1.8px),
    radial-gradient(circle at 28% 34%, rgba(166, 196, 255, 0.48) 0 1px, transparent 1.8px),
    radial-gradient(circle at 43% 76%, rgba(166, 196, 255, 0.34) 0 1px, transparent 1.8px),
    radial-gradient(circle at 64% 22%, rgba(166, 196, 255, 0.52) 0 1px, transparent 1.8px),
    radial-gradient(circle at 82% 58%, rgba(166, 196, 255, 0.42) 0 1px, transparent 1.8px);
  background-size: 360px 260px, 420px 330px, 520px 370px, 460px 300px, 560px 420px, 380px 290px;
  opacity: 0.54;
}


.cs-home :deep(.cs-hero__canvas) {
  position: absolute;
  top: 0;
  right: -6%;
  bottom: 0;
  left: 38%;
  z-index: 0;
}

.cs-home :deep(.cs-hero__canvas canvas) {
  display: block;
  width: 100% !important;
  height: 100% !important;
}


.cs-home :deep(.cs-hero__canvas--static) {
  background:
    radial-gradient(55% 55% at 60% 45%, rgba(110, 168, 255, 0.3), transparent 70%),
    radial-gradient(45% 50% at 55% 55%, rgba(177, 140, 255, 0.24), transparent 72%);
  filter: blur(8px);
}


.cs-home :deep(.cs-hero__grain) {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  background:
    radial-gradient(52% 72% at 20% 48%, rgba(6, 7, 13, 0.76), rgba(6, 7, 13, 0.18) 58%, transparent 76%),
    linear-gradient(90deg, rgba(6, 7, 13, 0.24), transparent 38%, rgba(6, 7, 13, 0.08) 100%),
    linear-gradient(180deg, transparent 60%, var(--cs-bg) 100%);
}

.cs-home :deep(.cs-hero__inner) {
  position: relative;
  z-index: 2;
  max-width: min(980px, 100%);
}

.cs-home :deep(.cs-hero__title) {
  margin: 24px 0 0;
  font-family: var(--cs-font-display);
  font-weight: 700;

  font-size: clamp(44px, 8vw, 112px);
  line-height: 0.92;
  letter-spacing: -0.035em;
}

.cs-home :deep(.cs-hero__title .cs-line) {
  display: block;

  overflow: hidden;
  padding: 0.08em 0.04em;
  margin: -0.08em -0.04em;
}

.cs-home :deep(.cs-hero__title .cs-line > span) {
  display: block;
  width: max-content;
  max-width: 100%;
  padding-bottom: 0.06em;
  background: linear-gradient(96deg, var(--cs-ink) 28%, var(--cs-accent) 72%, var(--cs-accent-2));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  will-change: transform;
}

.cs-home :deep(.cs-hero__sub) {
  max-width: 480px;
  margin: 28px 0 0;
  font-size: clamp(16px, 1.5vw, 19px);
  line-height: 1.6;
  color: var(--cs-ink-dim);
}

.cs-home :deep(.cs-hero__sub .cs-zh) {
  display: block;
  margin-top: 10px;
  font-size: 15px;
  color: var(--cs-ink-faint);
}

.cs-home :deep(.cs-hero__actions) {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 38px;
}

.cs-home :deep(.cs-hero__stats) {
  display: flex;
  gap: clamp(24px, 4vw, 56px);
  margin-top: 56px;
  padding-top: 28px;
  border-top: 1px solid var(--cs-line-soft);
}

.cs-home :deep(.cs-stat__value) {
  font-family: var(--cs-font-display);
  font-size: clamp(26px, 3vw, 38px);
  font-weight: 600;
  color: var(--cs-ink);
}

.cs-home :deep(.cs-stat__label) {
  margin-top: 4px;
  font-family: var(--cs-font-mono);
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--cs-ink-faint);
}

.cs-home :deep(.cs-hero__scroll) {
  position: absolute;
  bottom: 28px;
  left: max(var(--cs-edge), calc((100vw - var(--cs-maxw)) / 2 + var(--cs-edge)));
  z-index: 2;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-family: var(--cs-font-mono);
  font-size: 11px;
  letter-spacing: 0.24em;
  text-transform: uppercase;
  color: var(--cs-ink-faint);
}

.cs-home :deep(.cs-hero__scroll::after) {
  content: '';
  width: 46px;
  height: 1px;
  background: var(--cs-ink-faint);
  transform-origin: left;
  animation: cs-scroll-pulse 2.4s ease-in-out infinite;
}

@keyframes cs-scroll-pulse {
  0%, 100% { transform: scaleX(0.3); opacity: 0.4; }
  50% { transform: scaleX(1); opacity: 1; }
}


.cs-home :deep(.cs-btn) {
  --cs-btn-bg: var(--cs-accent);
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 14px 26px;
  border: 0;
  border-radius: 999px;
  font-family: var(--cs-font-display);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.01em;
  cursor: pointer;
  overflow: hidden;
  isolation: isolate;
  color: #070a16;
  background: var(--cs-btn-bg);
  transition: transform 0.4s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.4s ease;
}

.cs-home :deep(.cs-btn__fill) {
  position: absolute;
  inset: 0;
  z-index: -1;
  background: linear-gradient(120deg, var(--cs-accent), var(--cs-accent-2));
  transform: translateY(101%);
  transition: transform 0.5s cubic-bezier(0.22, 1, 0.36, 1);
}

.cs-home :deep(.cs-btn:hover) {
  transform: translateY(-2px);
  box-shadow: 0 16px 40px rgba(110, 168, 255, 0.36);
}

.cs-home :deep(.cs-btn:hover .cs-btn__fill) {
  transform: translateY(0);
}

.cs-home :deep(.cs-btn__dot) {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #070a16;
}

.cs-home :deep(.cs-btn--ghost) {
  color: var(--cs-ink);
  background: transparent;
  border: 1px solid var(--cs-line);
}

.cs-home :deep(.cs-btn--ghost .cs-btn__dot) {
  background: var(--cs-accent-3);
}

.cs-home :deep(.cs-btn--ghost .cs-btn__fill) {
  background: rgba(150, 165, 220, 0.12);
}

.cs-home :deep(.cs-btn--ghost:hover) {
  box-shadow: none;
  border-color: rgba(150, 165, 220, 0.4);
}


.cs-home :deep(.cs-head) {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 32px;
  padding-block: clamp(80px, 12vh, 150px) 44px;
}

.cs-home :deep(.cs-head__title) {
  max-width: 16ch;
  margin: 14px 0 0;
  font-family: var(--cs-font-display);
  font-weight: 600;
  font-size: clamp(32px, 5vw, 66px);
  line-height: 1.02;
  letter-spacing: -0.02em;
}

.cs-home :deep(.cs-head__note) {
  max-width: 34ch;
  margin: 0;
  font-size: 15px;
  line-height: 1.7;
  color: var(--cs-ink-dim);
}

@media (max-width: 900px) {

  .cs-home :deep(.cs-hero__canvas) {
    left: 0;
    right: -20%;
    opacity: 0.7;
  }
  .cs-home :deep(.cs-hero__grain) {
    background:
      linear-gradient(180deg, rgba(6, 7, 13, 0.4) 0%, rgba(6, 7, 13, 0.7) 55%, var(--cs-bg) 100%);
  }
  .cs-home :deep(.cs-hero__sub) {
    max-width: 100%;
  }
}

@media (max-width: 760px) {
  .cs-home :deep(.cs-head) {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  .cs-home :deep(.cs-hero) {
    padding-top: 110px;
  }
}


.cs-home :deep(.cs-articles__grid) {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: clamp(14px, 1.6vw, 22px);
  padding-bottom: clamp(60px, 10vh, 130px);
}

.cs-home :deep(.cs-article) {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  min-height: 280px;
  padding: 26px;
  border: 1px solid var(--cs-line);
  border-radius: 20px;
  overflow: hidden;
  background: var(--cs-bg-raise);

  clip-path: inset(0 0 0% 0);
  text-decoration: none;
  color: inherit;
  transition: transform 0.6s cubic-bezier(0.22, 1, 0.36, 1), border-color 0.4s ease;
}

.cs-home :deep(.cs-article__wash) {
  position: absolute;
  inset: 0;
  z-index: 0;
  opacity: 0.9;
  background: linear-gradient(155deg, var(--cs-from), var(--cs-to));
  transition: transform 0.7s cubic-bezier(0.22, 1, 0.36, 1);
}

.cs-home :deep(.cs-article__wash::before) {
  content: '';
  position: absolute;
  inset: 0;
  background-image: var(--cs-cover);
  background-position: center;
  background-size: cover;
  opacity: 0.82;
}

.cs-home :deep(.cs-article__wash::after) {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, rgba(5, 8, 16, 0.08) 0%, rgba(5, 8, 16, 0.78) 72%, rgba(5, 8, 16, 0.92) 100%),
    radial-gradient(120% 90% at 80% 0%, rgba(255, 255, 255, 0.14), transparent 60%);
}

.cs-home :deep(.cs-article__body) {
  position: relative;
  z-index: 1;
}

.cs-home :deep(.cs-article--feature) {
  grid-column: span 4;
  grid-row: span 2;
  min-height: 560px;
}

.cs-home :deep(.cs-article--wide) {
  grid-column: span 2;
}

.cs-home :deep(.cs-article--tall) {
  grid-column: span 2;
}

.cs-home :deep(.cs-article__tags) {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.cs-home :deep(.cs-tag) {
  padding: 5px 11px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 999px;
  font-family: var(--cs-font-mono);
  font-size: 11px;
  letter-spacing: 0.04em;
  color: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(4px);
}

.cs-home :deep(.cs-article__title) {
  margin: 0;
  font-family: var(--cs-font-display);
  font-weight: 600;
  font-size: clamp(19px, 2vw, 24px);
  line-height: 1.18;
  letter-spacing: -0.01em;
}

.cs-home :deep(.cs-article--feature .cs-article__title) {
  font-size: clamp(28px, 3.4vw, 46px);
  line-height: 1.04;
}

.cs-home :deep(.cs-article__excerpt) {
  margin: 12px 0 0;
  font-size: 14px;
  line-height: 1.6;
  color: rgba(244, 246, 255, 0.78);
}

.cs-home :deep(.cs-article--feature .cs-article__excerpt) {
  max-width: 46ch;
  font-size: 16px;
}

.cs-home :deep(.cs-article__meta) {
  display: flex;
  gap: 16px;
  margin-top: 18px;
  font-family: var(--cs-font-mono);
  font-size: 11px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: rgba(244, 246, 255, 0.6);
}

.cs-home :deep(.cs-article:hover) {
  border-color: rgba(255, 255, 255, 0.32);
}

.cs-home :deep(.cs-article:hover .cs-article__wash) {
  transform: scale(1.06);
}

@media (max-width: 980px) {
  .cs-home :deep(.cs-articles__grid) {
    grid-template-columns: repeat(2, 1fr);
  }
  .cs-home :deep(.cs-article--feature) {
    grid-column: span 2;
    grid-row: auto;
    min-height: 420px;
  }
  .cs-home :deep(.cs-article--wide),
.cs-home :deep(.cs-article--tall) {
    grid-column: span 1;
  }
}

@media (max-width: 560px) {
  .cs-home :deep(.cs-articles__grid) {
    grid-template-columns: 1fr;
  }
  .cs-home :deep(.cs-article--feature),
.cs-home :deep(.cs-article--wide),
.cs-home :deep(.cs-article--tall) {
    grid-column: span 1;
  }
}


.cs-home :deep(.cs-gallery) {
  position: relative;
}

.cs-home :deep(.cs-gallery__pin) {
  position: relative;
  height: 100vh;
  overflow: hidden;
}

.cs-home :deep(.cs-gallery__track) {
  display: flex;
  align-items: center;
  height: 100%;
  gap: clamp(20px, 2.4vw, 40px);
  padding-inline: var(--cs-edge);
  will-change: transform;
}

.cs-home :deep(.cs-gallery__intro) {
  flex: 0 0 auto;
  width: min(34vw, 460px);
}

.cs-home :deep(.cs-gallery__intro h2) {
  margin: 16px 0 0;
  font-family: var(--cs-font-display);
  font-weight: 600;
  font-size: clamp(34px, 4vw, 60px);
  line-height: 1.02;
  letter-spacing: -0.02em;
}

.cs-home :deep(.cs-gallery__intro p) {
  max-width: 32ch;
  margin-top: 18px;
  font-size: 15px;
  line-height: 1.7;
  color: var(--cs-ink-dim);
}

.cs-home :deep(.cs-gallery__hint) {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  margin-top: 30px;
  font-family: var(--cs-font-mono);
  font-size: 11px;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: var(--cs-ink-faint);
}

.cs-home :deep(.cs-poster) {
  position: relative;
  flex: 0 0 auto;
  width: clamp(300px, 42vw, 560px);
  height: clamp(420px, 70vh, 640px);
  border-radius: 24px;
  overflow: hidden;
  border: 1px solid var(--cs-line);
  background: linear-gradient(160deg, var(--cs-from), var(--cs-to));
}

.cs-home :deep(.cs-poster::before) {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 0;
  background-image: var(--cs-poster);
  background-position: center;
  background-size: cover;
  opacity: 0.86;
  transition: transform 0.8s cubic-bezier(0.22, 1, 0.36, 1);
}

.cs-home :deep(.cs-poster:hover::before) {
  transform: scale(1.04);
}

.cs-home :deep(.cs-poster__parallax) {
  position: absolute;
  inset: -12% -18%;
  z-index: 1;
  background:
    radial-gradient(50% 50% at 30% 20%, var(--cs-accent), transparent 60%),
    radial-gradient(40% 60% at 80% 90%, rgba(255, 255, 255, 0.16), transparent 60%);
  opacity: 0.34;
  will-change: transform;
}

.cs-home :deep(.cs-poster__grain) {
  position: absolute;
  inset: 0;
  z-index: 2;
  background:
    linear-gradient(180deg, rgba(4, 6, 12, 0.08) 0%, rgba(4, 6, 12, 0.22) 42%, rgba(4, 6, 12, 0.86) 100%),
    radial-gradient(90% 70% at 30% 90%, rgba(4, 6, 12, 0.28), transparent 72%);
}

.cs-home :deep(.cs-poster__content) {
  position: absolute;
  inset: 0;
  z-index: 3;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 30px;
}

.cs-home :deep(.cs-poster__index) {
  font-family: var(--cs-font-mono);
  font-size: 13px;
  letter-spacing: 0.2em;
  color: rgba(255, 255, 255, 0.7);
}

.cs-home :deep(.cs-poster__cat) {
  align-self: flex-start;
  display: inline-flex;
  margin-bottom: 18px;
  padding: 6px 13px;
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: 999px;
  font-family: var(--cs-font-mono);
  font-size: 11px;
  letter-spacing: 0.08em;
  color: #fff;
}

.cs-home :deep(.cs-poster__title) {
  margin: 0;
  font-family: var(--cs-font-display);
  font-weight: 600;
  font-size: clamp(30px, 3.4vw, 52px);
  line-height: 1;
  letter-spacing: -0.02em;
}

.cs-home :deep(.cs-poster__desc) {
  max-width: 38ch;
  margin: 14px 0 0;
  font-size: 14px;
  line-height: 1.6;
  color: rgba(244, 246, 255, 0.82);
}

.cs-home :deep(.cs-poster__stack) {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 18px;
}

.cs-home :deep(.cs-poster__stack span) {
  padding: 5px 10px;
  border-radius: 7px;
  background: rgba(7, 10, 22, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.14);
  font-family: var(--cs-font-mono);
  font-size: 11px;
  color: rgba(255, 255, 255, 0.86);
}

.cs-home :deep(.cs-gallery__outro) {
  flex: 0 0 auto;
  width: 40vw;
  max-width: 360px;
}


.cs-home :deep(.cs-gallery--stacked .cs-gallery__pin) {
  height: auto;
  overflow: visible;
}

.cs-home :deep(.cs-gallery--stacked .cs-gallery__track) {
  flex-direction: column;
  align-items: stretch;
  height: auto;
  padding-block: 40px 100px;
}

.cs-home :deep(.cs-gallery--stacked .cs-poster),
.cs-home :deep(.cs-gallery--stacked .cs-gallery__intro),
.cs-home :deep(.cs-gallery--stacked .cs-gallery__outro) {
  width: 100%;
  max-width: none;
}

.cs-home :deep(.cs-gallery--stacked .cs-gallery__outro) {
  display: none;
}


.cs-home :deep(.cs-agents) {
  position: relative;
  padding-bottom: clamp(70px, 11vh, 150px);
}

.cs-home :deep(.cs-agents__stage) {
  position: relative;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: clamp(16px, 2vw, 30px);
}


.cs-home :deep(.cs-agents__wires) {
  position: absolute;
  inset: 0;
  z-index: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  overflow: visible;
}

.cs-home :deep(.cs-wire) {
  fill: none;
  stroke: var(--cs-line);
  stroke-width: 1.5;
}

.cs-home :deep(.cs-wire--live) {
  stroke: url(#cs-flow-gradient);
  filter: drop-shadow(0 0 8px rgba(110, 168, 255, 0.7));
}

.cs-home :deep(.cs-wire__pulse) {
  fill: url(#cs-flow-gradient);
  filter: drop-shadow(0 0 10px rgba(84, 230, 200, 0.9));
}

.cs-home :deep(.cs-agent) {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  padding: 28px;
  border: 1px solid var(--cs-line);
  border-radius: 22px;
  background: linear-gradient(180deg, var(--cs-bg-raise), rgba(11, 13, 24, 0.6));
  transition: border-color 0.4s ease, transform 0.4s ease;
}

.cs-home :deep(.cs-agent:hover) {
  border-color: color-mix(in srgb, var(--cs-agent-accent) 60%, transparent);
  transform: translateY(-4px);
}

.cs-home :deep(.cs-agent__top) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.cs-home :deep(.cs-agent__glyph) {
  display: inline-grid;
  place-items: center;
  width: 46px;
  height: 46px;
  border-radius: 13px;
  background: color-mix(in srgb, var(--cs-agent-accent) 18%, transparent);
  border: 1px solid color-mix(in srgb, var(--cs-agent-accent) 40%, transparent);
}

.cs-home :deep(.cs-agent__glyph i) {
  width: 14px;
  height: 14px;
  border-radius: 4px;
  background: var(--cs-agent-accent);
  box-shadow: 0 0 14px var(--cs-agent-accent);
}

.cs-home :deep(.cs-agent__role) {
  font-family: var(--cs-font-mono);
  font-size: 11px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--cs-ink-faint);
}

.cs-home :deep(.cs-agent__name) {
  margin: 20px 0 0;
  font-family: var(--cs-font-display);
  font-weight: 600;
  font-size: 23px;
  letter-spacing: -0.01em;
}

.cs-home :deep(.cs-agent__summary) {
  margin: 12px 0 0;
  font-size: 14px;
  line-height: 1.62;
  color: var(--cs-ink-dim);
}


.cs-home :deep(.cs-pipe) {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 24px 0 0;
  padding: 14px;
  border-radius: 14px;
  background: rgba(7, 10, 22, 0.45);
  border: 1px solid var(--cs-line-soft);
}

.cs-home :deep(.cs-pipe__node) {
  flex: 1;
  text-align: center;
}

.cs-home :deep(.cs-pipe__dot) {
  width: 9px;
  height: 9px;
  margin: 0 auto 7px;
  border-radius: 50%;
  background: var(--cs-ink-faint);
  transition: background 0.3s ease, box-shadow 0.3s ease, transform 0.3s ease;
}

.cs-home :deep(.cs-pipe__node.is-active .cs-pipe__dot) {
  background: var(--cs-agent-accent);
  box-shadow: 0 0 12px var(--cs-agent-accent);
  transform: scale(1.5);
}

.cs-home :deep(.cs-pipe__label) {
  font-family: var(--cs-font-mono);
  font-size: 10px;
  letter-spacing: 0.06em;
  color: var(--cs-ink-faint);
  transition: color 0.3s ease;
}

.cs-home :deep(.cs-pipe__node.is-active .cs-pipe__label) {
  color: var(--cs-ink);
}

.cs-home :deep(.cs-pipe__link) {
  width: 14px;
  height: 1px;
  background: var(--cs-line);
}

.cs-home :deep(.cs-agent__outputs) {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 20px;
}

.cs-home :deep(.cs-agent__outputs span) {
  padding: 5px 11px;
  border-radius: 999px;
  border: 1px solid var(--cs-line);
  font-size: 12px;
  color: var(--cs-ink-dim);
}

@media (max-width: 900px) {
  .cs-home :deep(.cs-agents__stage) {
    grid-template-columns: 1fr;
  }
  .cs-home :deep(.cs-agents__wires) {
    display: none;
  }
}


.cs-home :deep(.cs-wall__grid) {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  grid-auto-rows: 150px;
  gap: clamp(12px, 1.4vw, 18px);
  padding-bottom: clamp(60px, 10vh, 130px);
}

.cs-home :deep(.cs-frag) {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 20px;
  border: 1px solid var(--cs-line);
  border-radius: 18px;
  background: var(--cs-bg-raise);
  overflow: hidden;
  cursor: default;
  will-change: transform;
}

.cs-home :deep(.cs-frag__inner) {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  justify-content: space-between;
  text-shadow: 0 1px 16px rgba(2, 6, 18, 0.52);
  will-change: transform;
}

.cs-home :deep(.cs-frag__kind) {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-family: var(--cs-font-mono);
  font-size: 10px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--cs-ink-faint);
}

.cs-home :deep(.cs-frag__kind::before) {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 2px;
  background: var(--cs-accent);
}

.cs-home :deep(.cs-frag--prompt .cs-frag__kind::before) { background: var(--cs-accent); }
.cs-home :deep(.cs-frag--code .cs-frag__kind::before) { background: var(--cs-accent-3); }
.cs-home :deep(.cs-frag--note .cs-frag__kind::before) { background: var(--cs-accent-warm); }
.cs-home :deep(.cs-frag--image .cs-frag__kind::before) { background: var(--cs-accent-2); }
.cs-home :deep(.cs-frag--link .cs-frag__kind::before) { background: #ffd166; }

.cs-home :deep(.cs-frag__body) {
  margin: 12px 0 0;
  font-size: 14px;
  line-height: 1.5;
  color: var(--cs-ink);
}

.cs-home :deep(.cs-frag--prompt .cs-frag__body) {
  font-family: var(--cs-font-serif);
  font-size: 16px;
  font-style: italic;
  line-height: 1.45;
  color: var(--cs-ink);
}

.cs-home :deep(.cs-frag--code .cs-frag__body) {
  font-family: var(--cs-font-mono);
  font-size: 12.5px;
  line-height: 1.6;
  white-space: pre-wrap;
  color: var(--cs-accent-3);
}

.cs-home :deep(.cs-frag__meta) {
  margin-top: 14px;
  font-family: var(--cs-font-mono);
  font-size: 10px;
  letter-spacing: 0.08em;
  color: var(--cs-ink-faint);
}

.cs-home :deep(.cs-frag--image) {
  border: 1px solid var(--cs-line);
}

.cs-home :deep(.cs-frag__wash) {
  position: absolute;
  inset: 0;
  z-index: 0;
  background-image:
    linear-gradient(155deg, rgba(6, 9, 20, 0.18), rgba(6, 9, 20, 0.74)),
    var(--cs-frag-bg, linear-gradient(155deg, var(--cs-from), var(--cs-to)));
  background-size: cover;
  background-position: center;
}

.cs-home :deep(.cs-frag--image .cs-frag__body),
.cs-home :deep(.cs-frag--image .cs-frag__meta) {
  color: rgba(255, 255, 255, 0.9);
}

.cs-home :deep(.cs-frag__link) {
  align-self: flex-start;
  margin-top: auto;
  color: var(--cs-accent);
  font-size: 13px;
}

@media (max-width: 900px) {
  .cs-home :deep(.cs-wall__grid) {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 520px) {
  .cs-home :deep(.cs-wall__grid) {
    grid-template-columns: 1fr;
    grid-auto-rows: auto;
  }
  .cs-home :deep(.cs-frag) {
    min-height: 130px;
  }
}


.cs-home :deep(.cs-themes__layout) {
  display: grid;
  grid-template-columns: 340px 1fr;
  gap: clamp(20px, 3vw, 48px);
  align-items: start;
  padding-bottom: clamp(70px, 11vh, 150px);
}

.cs-home :deep(.cs-themes__list) {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.cs-home :deep(.cs-theme-btn) {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  padding: 16px 18px;
  border: 1px solid var(--cs-line);
  border-radius: 16px;
  background: var(--cs-bg-raise);
  color: var(--cs-ink);
  text-align: left;
  cursor: pointer;
  transition: border-color 0.35s ease, background 0.35s ease;
}

.cs-home :deep(.cs-theme-btn:hover) {
  border-color: rgba(150, 165, 220, 0.36);
}

.cs-home :deep(.cs-theme-btn.is-active) {
  border-color: var(--cs-accent);
  background: rgba(110, 168, 255, 0.08);
}

.cs-home :deep(.cs-theme-btn__swatch) {
  display: flex;
  flex-shrink: 0;
  width: 46px;
  height: 46px;
  border-radius: 12px;
  overflow: hidden;
}

.cs-home :deep(.cs-theme-btn__swatch span) {
  flex: 1;
}

.cs-home :deep(.cs-theme-btn__name) {
  display: block;
  font-family: var(--cs-font-display);
  font-weight: 600;
  font-size: 16px;
}

.cs-home :deep(.cs-theme-btn__tag) {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: var(--cs-ink-faint);
}


.cs-home :deep(.cs-theme-preview) {
  position: relative;
  min-height: 440px;
  padding: 36px;
  border-radius: 24px;
  overflow: hidden;
  border: 1px solid var(--cs-line);
  background: var(--tp-bg);
  color: var(--tp-ink);
  font-family: var(--tp-font);
  transition:
    --tp-bg 0.72s ease,
    --tp-surface 0.72s ease,
    --tp-ink 0.72s ease,
    --tp-muted 0.72s ease,
    --tp-accent 0.72s ease,
    --tp-accent-soft 0.72s ease,
    background 0.72s ease,
    color 0.72s ease,
    border-color 0.72s ease;
}

.cs-home :deep(.cs-theme-preview::before) {
  content: '';
  position: absolute;
  inset: -40% -20% auto;
  height: 62%;
  background:
    radial-gradient(circle at 20% 30%, var(--tp-accent-soft), transparent 34%),
    linear-gradient(90deg, transparent, var(--tp-accent-soft), transparent);
  filter: blur(18px);
  transform: translateY(-10%);
  transition: background 0.72s ease, opacity 0.72s ease;
  opacity: 0.88;
}

.cs-home :deep(.cs-theme-preview > *) {
  position: relative;
  z-index: 1;
}

.cs-home :deep(.cs-theme-preview__eyebrow) {
  font-size: 12px;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--tp-accent);
}

.cs-home :deep(.cs-theme-preview__title) {
  margin: 16px 0 0;
  font-size: clamp(30px, 4vw, 52px);
  line-height: 1.04;
  font-family: var(--tp-font);
}

.cs-home :deep(.cs-theme-preview__text) {
  max-width: 46ch;
  margin: 16px 0 0;
  font-size: 15px;
  line-height: 1.7;
  color: var(--tp-muted);
}

.cs-home :deep(.cs-theme-preview__cards) {
  display: flex;
  gap: 14px;
  margin-top: 30px;
}

.cs-home :deep(.cs-theme-preview__card) {
  flex: 1;
  padding: 18px;
  border-radius: 14px;
  background: var(--tp-surface);
  border: 1px solid var(--tp-accent-soft);
}

.cs-home :deep(.cs-theme-preview__card strong) {
  display: block;
  font-size: 22px;
  color: var(--tp-ink);
}

.cs-home :deep(.cs-theme-preview__card span) {
  font-size: 12px;
  color: var(--tp-muted);
}

.cs-home :deep(.cs-theme-preview__chip) {
  display: inline-flex;
  margin-top: 26px;
  padding: 10px 18px;
  border-radius: 999px;
  background: var(--tp-accent);
  color: var(--tp-bg);
  font-size: 13px;
  font-weight: 600;
}

@media (max-width: 860px) {
  .cs-home :deep(.cs-themes__layout) {
    grid-template-columns: 1fr;
  }
}


.cs-home :deep(.cs-cta-wrap) {
  position: relative;
  overflow: hidden;
}

.cs-home :deep(.cs-cta) {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 22px;
  min-height: 82vh;
  padding: 130px var(--cs-edge) 90px;
  text-align: center;
}

.cs-home :deep(.cs-cta__glow) {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 560px;
  height: 560px;
  margin: -280px 0 0 -280px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(110, 168, 255, 0.16), transparent 66%);
  filter: blur(24px);
  pointer-events: none;
  will-change: transform;
}

.cs-home :deep(.cs-cta__eyebrow) {
  position: relative;
  z-index: 1;
}

.cs-home :deep(.cs-cta__line) {
  position: relative;
  z-index: 1;
  max-width: 12ch;
  margin: 0;
  font-family: var(--cs-font-display);
  font-weight: 600;
  font-size: clamp(36px, 6vw, 86px);
  line-height: 1.12;
  letter-spacing: 0;
}


.cs-home :deep(.cs-char) {
  display: inline-block;
  transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1), color 0.3s ease;
  will-change: transform;
}

.cs-home :deep(.cs-char:hover) {
  transform: translateY(-0.12em);
  color: var(--cs-accent);
}

.cs-home :deep(.cs-cta__sub) {
  position: relative;
  z-index: 1;
  max-width: 42ch;
  margin: 0;
  font-size: 16px;
  line-height: 1.7;
  color: var(--cs-ink-dim);
}

.cs-home :deep(.cs-cta__action) {
  position: relative;
  z-index: 1;
  margin-top: 4px;
}

.cs-home :deep(.cs-footer) {
  position: relative;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  max-width: var(--cs-maxw);
  margin: 0 auto;
  padding: 36px var(--cs-edge) 48px;
  border-top: 1px solid var(--cs-line-soft);
  font-family: var(--cs-font-mono);
  font-size: 12px;
  letter-spacing: 0.06em;
  color: var(--cs-ink-faint);
}

.cs-home :deep(.cs-footer a) {
  color: var(--cs-ink-dim);
  transition: color 0.25s ease;
}

.cs-home :deep(.cs-footer a:hover) {
  color: var(--cs-accent);
}

.cs-home :deep(.cs-footer__social) {
  display: flex;
  gap: 22px;
}


.cs-home :deep(.cs-marquee-sec) {
  position: relative;
  z-index: 1;
  padding: clamp(40px, 8vh, 90px) 0 clamp(70px, 12vh, 150px);
}

.cs-home :deep(.cs-marquee) {
  display: flex;
  flex-direction: column;
  gap: 6px;
  user-select: none;
}

.cs-home :deep(.cs-marquee__row) {
  overflow: hidden;
  white-space: nowrap;
}

.cs-home :deep(.cs-marquee__inner) {
  display: inline-flex;
  align-items: center;
  will-change: transform;
}

.cs-home :deep(.cs-marquee__item) {
  display: inline-flex;
  align-items: center;
  font-family: var(--cs-font-display);
  font-weight: 700;
  font-size: clamp(40px, 8vw, 116px);
  line-height: 1.04;
  letter-spacing: -0.03em;
  color: transparent;
  -webkit-text-stroke: 1px rgba(170, 185, 235, 0.34);
  text-stroke: 1px rgba(170, 185, 235, 0.34);
}

.cs-home :deep(.cs-marquee__row.is-reverse .cs-marquee__item) {
  color: var(--cs-ink);
  -webkit-text-stroke: 0;
  text-stroke: 0;
  opacity: 0.94;
}

.cs-home :deep(.cs-marquee__star) {
  margin: 0 clamp(22px, 3vw, 52px);
  font-size: 0.4em;
  font-style: normal;
  color: var(--cs-accent);
  -webkit-text-stroke: 0;
  text-stroke: 0;
  transform: translateY(-0.2em);
}

.cs-home :deep(.cs-mani) {
  margin-top: clamp(56px, 9vh, 120px);
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(280px, 0.8fr);
  gap: clamp(28px, 6vw, 96px);
  align-items: start;
}

.cs-home :deep(.cs-mani__text) {
  max-width: 18ch;
  margin: 22px 0 0;
  font-family: var(--cs-font-serif);
  font-size: clamp(28px, 4vw, 58px);
  line-height: 1.28;
  letter-spacing: -0.01em;
  color: var(--cs-ink-faint);
}

.cs-home :deep(.cs-mani__word) {
  display: inline;
  transition: color 0.3s ease;
}

.cs-home :deep(.cs-mani__word.is-accent) {
  color: var(--cs-ink);
}

.cs-home :deep(.cs-mani__cards) {
  display: grid;
  gap: 14px;
  padding-top: clamp(18px, 5vh, 72px);
}

.cs-home :deep(.cs-mani-card) {
  padding: 20px;
  border: 1px solid var(--cs-line);
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(150, 165, 220, 0.08), rgba(150, 165, 220, 0.03)),
    rgba(11, 13, 24, 0.62);
}

.cs-home :deep(.cs-mani-card span) {
  display: block;
  font-family: var(--cs-font-mono);
  font-size: 11px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--cs-accent);
}

.cs-home :deep(.cs-mani-card strong) {
  display: block;
  margin-top: 10px;
  font-family: var(--cs-font-display);
  font-size: clamp(20px, 2vw, 28px);
  line-height: 1.16;
  color: var(--cs-ink);
}

.cs-home :deep(.cs-mani-card p) {
  margin: 10px 0 0;
  color: var(--cs-ink-dim);
  font-size: 14px;
  line-height: 1.6;
}

@media (max-width: 900px) {
  .cs-home :deep(.cs-mani) {
    grid-template-columns: 1fr;
  }

  .cs-home :deep(.cs-mani__text) {
    max-width: 100%;
  }

  .cs-home :deep(.cs-mani__cards) {
    padding-top: 0;
  }
}


.cs-home :deep(.cs-approach__grid) {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: clamp(30px, 6vw, 100px);
}

.cs-home :deep(.cs-rail) {
  position: sticky;
  top: 18vh;
  align-self: start;
  height: max-content;
}

.cs-home :deep(.cs-rail__num) {
  font-family: var(--cs-font-display);
  font-weight: 700;
  font-size: clamp(90px, 12vw, 170px);
  line-height: 0.9;
  letter-spacing: -0.04em;
  background: linear-gradient(150deg, var(--cs-ink) 30%, var(--cs-accent));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.cs-home :deep(.cs-rail__label) {
  margin-top: 8px;
  font-family: var(--cs-font-mono);
  font-size: 13px;
  letter-spacing: 0.24em;
  text-transform: uppercase;
  color: var(--cs-ink-faint);
}

.cs-home :deep(.cs-rail__track) {
  display: flex;
  gap: 8px;
  margin-top: 28px;
}

.cs-home :deep(.cs-rail__tick) {
  width: 40px;
  height: 3px;
  border-radius: 2px;
  background: var(--cs-line);
  transition: background 0.4s ease, transform 0.4s ease;
  transform-origin: left;
}

.cs-home :deep(.cs-rail__tick.is-active) {
  background: var(--cs-accent);
  transform: scaleY(1.8);
}

.cs-home :deep(.cs-steps) {
  display: flex;
  flex-direction: column;
}

.cs-home :deep(.cs-step) {
  padding: clamp(34px, 6vh, 70px) 0;
  border-top: 1px solid var(--cs-line-soft);
}

.cs-home :deep(.cs-step:first-child) {
  border-top: 0;
  padding-top: 0;
}

.cs-home :deep(.cs-step__head) {
  display: flex;
  align-items: baseline;
  gap: 18px;
}

.cs-home :deep(.cs-step__no) {
  font-family: var(--cs-font-mono);
  font-size: 14px;
  color: var(--cs-accent);
}

.cs-home :deep(.cs-step__title) {
  margin: 0;
  font-family: var(--cs-font-display);
  font-weight: 600;
  font-size: clamp(28px, 3.4vw, 46px);
  line-height: 1.05;
  letter-spacing: -0.02em;
}

.cs-home :deep(.cs-step__en) {
  margin-left: 14px;
  font-family: var(--cs-font-mono);
  font-size: 0.42em;
  font-weight: 400;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--cs-ink-faint);
  vertical-align: middle;
}

.cs-home :deep(.cs-step__body) {
  max-width: 52ch;
  margin: 18px 0 0;
  font-size: clamp(15px, 1.4vw, 17px);
  line-height: 1.7;
  color: var(--cs-ink-dim);
}

.cs-home :deep(.cs-step__tags) {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 22px;
}

.cs-home :deep(.cs-tag-line) {
  font-family: var(--cs-font-mono);
  font-size: 11px;
  letter-spacing: 0.08em;
  color: var(--cs-ink-faint);
  padding-left: 16px;
  position: relative;
}

.cs-home :deep(.cs-tag-line::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  width: 8px;
  height: 1px;
  background: var(--cs-accent);
}

@media (max-width: 860px) {
  .cs-home :deep(.cs-approach__grid) {
    grid-template-columns: 1fr;
    gap: 18px;
  }
  .cs-home :deep(.cs-rail) {
    position: static;
    display: flex;
    align-items: baseline;
    gap: 18px;
  }
  .cs-home :deep(.cs-rail__num) {
    font-size: 64px;
  }
  .cs-home :deep(.cs-rail__track) {
    display: none;
  }
}


.cs-home :deep(.cs-cta__eyebrow) {
  margin-bottom: 4px;
}




@media (prefers-reduced-motion: reduce) {
  .cs-home :deep(*),
.cs-home :deep(*::before),
.cs-home :deep(*::after) {
    animation-duration: 0.001ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.001ms !important;
    scroll-behavior: auto !important;
  }


  .cs-home :deep(.cs-article) {
    clip-path: none !important;
  }

  .cs-home :deep(.cs-hero__title .cs-line > span) {
    transform: none !important;
  }

  .cs-home :deep(.cs-hero__scroll::after) {
    animation: none;
  }


  .cs-home :deep(.cs-mani__word) {
    color: var(--cs-ink-dim) !important;
  }

  .cs-home :deep(.cs-mani__word.is-accent) {
    color: var(--cs-ink) !important;
  }

}
</style>
