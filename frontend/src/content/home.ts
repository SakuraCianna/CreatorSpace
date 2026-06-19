

import articleContentIndexCover from '../assets/showcase/article-content-index.svg'
import articleReadingMapCover from '../assets/showcase/article-reading-map.svg'
import articleThemeArchiveCover from '../assets/showcase/article-theme-archive.svg'
import articleVisitEntryCover from '../assets/showcase/article-visit-entry.svg'
import articleVisualLanguageCover from '../assets/showcase/article-visual-language.svg'
import articleWritingVoiceCover from '../assets/showcase/article-writing-voice.svg'
import workBlogFrontstagePoster from '../assets/showcase/work-blog-frontstage.svg'
import workContentDeskPoster from '../assets/showcase/work-content-desk.svg'
import workContentIndexPoster from '../assets/showcase/work-content-index.svg'
import workIdeaBoxPoster from '../assets/showcase/work-idea-box.svg'
import workReadingMotionPoster from '../assets/showcase/work-reading-motion.svg'

export type ArticleKind = 'feature' | 'standard'

export interface FeaturedArticle {
  id: string
  slug: string
  kind: ArticleKind
  title: string
  excerpt: string
  tags: string[]
  readingMinutes: number
  publishedAt: string
  cover: [string, string]
  coverImage: string
}

export interface PortfolioProject {
  id: string
  slug: string
  index: string
  title: string
  category: string
  year: string
  description: string
  stack: string[]
  palette: { from: string; to: string; accent: string }
  posterImage: string
}

export type AgentStage = 'route' | 'retrieve' | 'generate'

export interface AgentCapability {
  id: string
  name: string
  role: string
  summary: string
  pipeline: { stage: AgentStage; label: string }[]
  outputs: string[]
  accent: string
}

export type FragmentKind = 'prompt' | 'image' | 'note' | 'code' | 'link'

export interface CreativeFragment {
  id: string
  kind: FragmentKind
  span: { col: number; row: number }
  label: string
  body: string
  meta?: string
  palette?: [string, string]
}

export interface ThemePreset {
  id: string
  name: string
  tagline: string
  mood: string
  vars: {
    bg: string
    surface: string
    ink: string
    muted: string
    accent: string
    accentSoft: string
    font: string
  }
  swatches: string[]
}

export interface SiteConfig {
  brand: string
  wordmark: string
  navigation: { label: string; to: string }[]
  social: { label: string; handle: string }[]
}

export const featuredArticles: FeaturedArticle[] = [
  {
    id: 'a-theme-archive',
    slug: 'demo-article-creator-hub',
    kind: 'feature',
    title: '把个人站点做成主题档案馆',
    excerpt:
      '每篇文章都有封面、主题、标签和延伸入口，读者不是从时间线里翻找，而是在一个有气质的个人空间里慢慢逛。',
    tags: ['个人博客', '主题档案', '内容结构'],
    readingMinutes: 11,
    publishedAt: '2026-06-16',
    cover: ['#1b2b4d', '#0a1326'],
    coverImage: articleThemeArchiveCover,
  },
  {
    id: 'a-design-language',
    slug: 'demo-article-homepage-motion-budget',
    kind: 'standard',
    title: '为个人博客设计一套不像模板的视觉语言',
    excerpt: '封面色、字号层级、卡片密度和滚动节奏一起决定这个站点像谁，而不是像哪个模板。',
    tags: ['视觉风格', '封面', '动效'],
    readingMinutes: 7,
    publishedAt: '2026-06-12',
    cover: ['#3a1d4d', '#160a26'],
    coverImage: articleVisualLanguageCover,
  },
  {
    id: 'a-spring-pipeline',
    slug: 'demo-article-admin-crud-phase-one',
    kind: 'standard',
    title: '内容后台如何服务前台阅读',
    excerpt: '草稿、公开、私密、分类和标签都不是后台术语，它们最后会变成前台读者看到的秩序。',
    tags: ['内容后台', '分类', '权限'],
    readingMinutes: 9,
    publishedAt: '2026-06-08',
    cover: ['#123a37', '#08201f'],
    coverImage: articleContentIndexCover,
  },
  {
    id: 'a-voice-notes',
    slug: 'demo-article-writing-voice',
    kind: 'standard',
    title: '写作语气样本如何整理',
    excerpt: '把常用开场、句子长度、标题习惯和常见比喻整理出来，站点才不会变成无主语的内容仓库。',
    tags: ['写作语气', '手记'],
    readingMinutes: 6,
    publishedAt: '2026-06-03',
    cover: ['#4d3312', '#261707'],
    coverImage: articleWritingVoiceCover,
  },
  {
    id: 'a-reading-map',
    slug: 'demo-article-reading-map',
    kind: 'standard',
    title: '给文章做一张阅读地图',
    excerpt: '把相邻主题串成路径，让读者从一篇文章自然走到下一篇，而不是只看一张时间倒序表。',
    tags: ['阅读路径', '归档'],
    readingMinutes: 5,
    publishedAt: '2026-06-01',
    cover: ['#2f2a57', '#121028'],
    coverImage: articleReadingMapCover,
  },
  {
    id: 'a-visit-entry',
    slug: 'demo-article-visit-entry',
    kind: 'standard',
    title: '从入口开始设计一次访问',
    excerpt: '游客先看到什么、点击哪里、在哪里停留，决定了这个个人站点是被匆匆扫过，还是被认真读完。',
    tags: ['入口动线', '访客视角'],
    readingMinutes: 4,
    publishedAt: '2026-05-28',
    cover: ['#24334b', '#07111f'],
    coverImage: articleVisitEntryCover,
  },
]

export const agentCapabilities: AgentCapability[] = [
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

export const creativeFragments: CreativeFragment[] = [
  {
    id: 'f-prompt-voice',
    kind: 'prompt',
    span: { col: 2, row: 2 },
    label: '摘句',
    body: '“个人博客不是简历扩展页。它应该像一间房间，能看出主人最近在读什么、写什么、反复想什么。”',
    meta: '写作语气 · 开场样本',
    palette: ['#1d2c52', '#0b1428'],
  },
  {
    id: 'f-cover-sketch',
    kind: 'image',
    span: { col: 2, row: 1 },
    label: '封面草图',
    body: '深蓝底、手写线、两张半透明纸片。用来表达“主题档案馆”的第一眼气质。',
    meta: 'cover study · 01',
    palette: ['#263e70', '#0b1226'],
  },
  {
    id: 'f-note-archive',
    kind: 'note',
    span: { col: 1, row: 1 },
    label: '手记',
    body: '博客不是时间线，是一个可以回来的主题空间。',
    meta: '站点原则',
  },
  {
    id: 'f-reading-map',
    kind: 'note',
    span: { col: 1, row: 2 },
    label: '阅读路径',
    body: '先读“主题档案馆”，再读“视觉语言”，最后走到“阅读地图”。三篇文章应该能互相引路。',
    meta: 'reader journey',
    palette: ['#3a1d4d', '#0a0f1f'],
  },
  {
    id: 'f-link-awwwards',
    kind: 'link',
    span: { col: 1, row: 1 },
    label: '收藏',
    body: '长文章页面的滚动节奏收藏夹：标题先出现，卡片再像展品一样慢慢展开。',
    meta: 'editorial motion',
  },
  {
    id: 'f-title-seed',
    kind: 'prompt',
    span: { col: 1, row: 1 },
    label: '选题',
    body: '“为什么个人博客应该先有气质，再有功能列表？”',
    meta: 'article seed · title',
  },
  {
    id: 'f-note-tone',
    kind: 'note',
    span: { col: 2, row: 1 },
    label: '语气规则',
    body: '少解释功能，多写感受；少讲系统，多讲读者怎样进入、停留、再回来。',
    meta: 'copy rule',
    palette: ['#123a37', '#07201e'],
  },
  {
    id: 'f-cover-palette',
    kind: 'image',
    span: { col: 1, row: 1 },
    label: '色票',
    body: '夜蓝、纸白、青绿和一点点暖橙；不把整站做成单一蓝紫色。',
    meta: 'palette · blog mood',
    palette: ['#2c4a7a', '#18111c'],
  },
]

export const portfolioProjects: PortfolioProject[] = [
  {
    id: 'p-creatorspace',
    slug: 'demo-project-immersive-homepage',
    index: '01',
    title: '主题博客前台',
    category: 'Blog Frontstage',
    year: '2026',
    description: '把文章、作品和灵感用同一套视觉语言展示出来，本站首页就是第一版样张。',
    stack: ['Vue 3', 'Spring Boot', 'PostgreSQL'],
    palette: { from: '#1f3b6e', to: '#0a1226', accent: '#6ea8ff' },
    posterImage: workBlogFrontstagePoster,
  },
  {
    id: 'p-content-index',
    slug: 'demo-project-content-index',
    index: '02',
    title: '内容索引',
    category: 'Content Index',
    year: '2026',
    description: '让分类、标签、封面和摘要一起工作，帮助读者从一个主题找到下一篇文章。',
    stack: ['Java', 'Category', 'Tag'],
    palette: { from: '#3c1f63', to: '#120825', accent: '#b18cff' },
    posterImage: workContentIndexPoster,
  },
  {
    id: 'p-motion-kit',
    slug: 'demo-project-reading-motion',
    index: '03',
    title: '阅读动效实验',
    category: 'Reading Motion',
    year: '2025',
    description: '用滚动、显隐和横向画廊给长页面分段，让读者知道自己正逛到哪里。',
    stack: ['GSAP', 'Three.js', 'Lenis'],
    palette: { from: '#0f4a44', to: '#08201d', accent: '#54e6c8' },
    posterImage: workReadingMotionPoster,
  },
  {
    id: 'p-studio-cms',
    slug: 'demo-project-content-console',
    index: '04',
    title: '内容整理后台',
    category: 'Content Desk',
    year: '2025',
    description: '面向单一创作者的整理台：草稿、公开、私密、分类和标签先保持稳定。',
    stack: ['TypeScript', 'Pinia', 'REST'],
    palette: { from: '#5c2f12', to: '#241206', accent: '#ff9d5c' },
    posterImage: workContentDeskPoster,
  },
  {
    id: 'p-topic-box',
    slug: 'demo-project-idea-box',
    index: '05',
    title: '选题卡片盒',
    category: 'Idea Box',
    year: '2025',
    description: '把还没成文的标题、摘句、链接和参考图先收起来，留给之后的文章继续发芽。',
    stack: ['Notes', 'Links', 'Images'],
    palette: { from: '#5a1d3a', to: '#220b16', accent: '#ff7eb0' },
    posterImage: workIdeaBoxPoster,
  },
]

export const themePresets: ThemePreset[] = [
  {
    id: 'cyber-night',
    name: 'Cyber Night',
    tagline: '霓虹与深空',
    mood: '适合技术长文与实验记录',
    vars: {
      bg: '#070b18',
      surface: 'rgba(120, 170, 255, 0.08)',
      ink: '#eaf1ff',
      muted: 'rgba(200, 214, 255, 0.6)',
      accent: '#6ea8ff',
      accentSoft: 'rgba(110, 168, 255, 0.22)',
      font: '"Space Grotesk", Inter, sans-serif',
    },
    swatches: ['#6ea8ff', '#9d7bff', '#0b1430'],
  },
  {
    id: 'glass-space',
    name: 'Glass Space',
    tagline: '半透与微光',
    mood: '适合作品集与视觉叙事',
    vars: {
      bg: '#12101d',
      surface: 'rgba(177, 140, 255, 0.1)',
      ink: '#f3effb',
      muted: 'rgba(222, 212, 255, 0.6)',
      accent: '#b18cff',
      accentSoft: 'rgba(177, 140, 255, 0.24)',
      font: '"Sora", Inter, sans-serif',
    },
    swatches: ['#b18cff', '#ff9bd2', '#1a1430'],
  },
  {
    id: 'minimal-white',
    name: 'Minimal White',
    tagline: '克制与留白',
    mood: '适合阅读与长期归档',
    vars: {
      bg: '#f4f2ee',
      surface: 'rgba(20, 20, 24, 0.04)',
      ink: '#16151a',
      muted: 'rgba(22, 21, 26, 0.55)',
      accent: '#1d6f5c',
      accentSoft: 'rgba(29, 111, 92, 0.16)',
      font: '"Newsreader", Georgia, serif',
    },
    swatches: ['#1d6f5c', '#c2603d', '#16151a'],
  },
  {
    id: 'pixel-dream',
    name: 'Pixel Dream',
    tagline: '复古与噪点',
    mood: '适合灵感墙与创意碎片',
    vars: {
      bg: '#1a1224',
      surface: 'rgba(255, 158, 200, 0.1)',
      ink: '#ffeef7',
      muted: 'rgba(255, 214, 235, 0.62)',
      accent: '#ff7eb0',
      accentSoft: 'rgba(255, 126, 176, 0.24)',
      font: '"VT323", "Space Mono", monospace',
    },
    swatches: ['#ff7eb0', '#ffd166', '#2a1838'],
  },
]

export const siteConfig: SiteConfig = {
  brand: 'CreatorSpace',
  wordmark: 'CreatorSpace',
  navigation: [
    { label: '游客', to: '/articles' },
    { label: '注册', to: '/register' },
    { label: '登录', to: '/login' },
  ],
  social: [
    { label: 'GitHub', handle: 'github.com/SakuraCianna/CreatorSpace' },
    { label: 'Mail', handle: '754515922@qq.com' },
  ],
}
export const heroContent = {
  kicker: 'Personal Theme Blog · Creative Portfolio',
  titleLines: ['CreatorSpace', '创作主页'],
  subtitle: '一个有主题风格的个人博客与作品展示平台。',
  description:
    '这里不是模板化列表，而是一个带有主题气质的个人站点：文章记录思考，作品呈现过程，灵感碎片连接长期创作。',
  primary: { label: '进入博客', to: '/articles' },
  secondary: { label: '浏览作品', to: '/projects' },
  stats: [
    { value: '128', label: '主题文章' },
    { value: '36', label: '创意作品' },
    { value: '248', label: '灵感碎片' },
  ],
}

/* ---- Manifesto marquee (after hero) ------------------------------------- */
export const marqueeWords = [
  'Articles',
  'Works',
  'Notes',
  'Covers',
  'Tags',
  'Themes',
  'Archive',
  'Reading',
]

export const manifesto = {
  lead: '(01) — 站点宣言',
  segments: [
    { text: '这里先是一座个人主题博客。 ', accent: false },
    { text: '文章负责把想法说清楚， ', accent: true },
    { text: '作品负责把过程留下来， ', accent: false },
    { text: '灵感卡片负责等下一次回头。', accent: true },
  ],
  cards: [
    { label: '阅读路径', value: '主题 / 标签 / 归档', detail: '不让读者只靠时间线找文章。' },
    { label: '视觉气质', value: '封面色 / 字号 / 留白', detail: '每个页面都要像同一个人整理出来的。' },
    { label: '内容边界', value: '公开 / 私密 / 草稿', detail: '写给别人看的内容和写给自己的内容分开放。' },
  ],
}

/* ---- Approach / process -------------------------------------------------- */
export interface ApproachStep {
  no: string
  title: string
  en: string
  body: string
  tags: string[]
}

export const approachSteps: ApproachStep[] = [
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

export const counters = [
  { value: 128, suffix: '', label: '公开文章' },
  { value: 36, suffix: '', label: '展示作品' },
  { value: 2480, suffix: '', label: '灵感碎片' },
  { value: 99, suffix: '%', label: '内容可维护' },
]

/* ---- Field notes / changelog (before CTA) ------------------------------- */
export interface FieldNote {
  date: string
  tag: string
  title: string
  detail: string
}

export const fieldNotes: FieldNote[] = [
  {
    date: '2026 · 06',
    tag: '前台',
    title: '主题博客前台成形',
    detail: '首页、文章索引、作品橱窗和访客入口完成第一轮统一，先把个人站点的气质立住。',
  },
  {
    date: '2026 · 05',
    tag: '内容',
    title: '文章与作品基础闭环',
    detail: '文章、作品、分类和标签进入同一套内容结构，前台展示不再依赖临时样例。',
  },
  {
    date: '2026 · 04',
    tag: '后台',
    title: '内容管理模块落地',
    detail: '后台围绕创作者、内容状态和基础 CRUD 建立边界，为后续编辑体验留出接口。',
  },
  {
    date: '2026 · 03',
    tag: '基线',
    title: '项目结构与迁移基线',
    detail: '前后端目录、数据库迁移、测试入口和开发约束定下来，后续模块按同一节奏推进。',
  },
]

export const closing = {
  eyebrow: '继续阅读',
}
