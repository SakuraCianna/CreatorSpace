

export type ArticleKind = 'feature' | 'standard'

export interface FeaturedArticle {
  id: string
  kind: ArticleKind
  title: string
  excerpt: string
  tags: string[]
  readingMinutes: number
  publishedAt: string
  cover: [string, string]
}

export interface PortfolioProject {
  id: string
  index: string
  title: string
  category: string
  year: string
  description: string
  stack: string[]
  palette: { from: string; to: string; accent: string }
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
    id: 'a-rag-native',
    kind: 'feature',
    title: '把博客重写成一个会检索自己的系统',
    excerpt:
      '当每一篇文章都被切片、向量化并接入检索层，写作就不再是时间线上的条目，而是一个可以被追问的知识体。这是 CreatorSpace 的第一性原理。',
    tags: ['RAG', 'Architecture', 'Vue'],
    readingMinutes: 11,
    publishedAt: '2026-06-16',
    cover: ['#1b2b4d', '#0a1326'],
  },
  {
    id: 'a-design-language',
    kind: 'standard',
    title: '为个人网站设计一套不像模板的视觉语言',
    excerpt: '留白、字号层级、克制的发光，以及每个区块只出现一次的核心动效。',
    tags: ['Design', 'Motion'],
    readingMinutes: 7,
    publishedAt: '2026-06-12',
    cover: ['#3a1d4d', '#160a26'],
  },
  {
    id: 'a-spring-pipeline',
    kind: 'standard',
    title: 'Spring Boot 内容流水线的取舍笔记',
    excerpt: '从草稿状态机到隐私可见性，后端如何为前台的沉浸式呈现兜底。',
    tags: ['Spring Boot', 'Backend'],
    readingMinutes: 9,
    publishedAt: '2026-06-08',
    cover: ['#123a37', '#08201f'],
  },
  {
    id: 'a-agent-loop',
    kind: 'standard',
    title: '让写作智能体理解“我的语气”',
    excerpt: '少量风格样本 + 检索增强，比一个巨大的系统提示更可靠。',
    tags: ['AI Agent', 'Prompt'],
    readingMinutes: 6,
    publishedAt: '2026-06-03',
    cover: ['#4d3312', '#261707'],
  },
]

export const agentCapabilities: AgentCapability[] = [
  {
    id: 'writing',
    name: 'Writing Agent',
    role: '写作助手',
    summary: '从一个想法生长出标题、摘要、标签、大纲，直到一篇可编辑的草稿。',
    pipeline: [
      { stage: 'route', label: '解析意图' },
      { stage: 'retrieve', label: '检索我的语气' },
      { stage: 'generate', label: '生成草稿' },
    ],
    outputs: ['标题 × 5', '摘要', '标签', '大纲', '正文草稿'],
    accent: '#6ea8ff',
  },
  {
    id: 'portfolio',
    name: 'Portfolio Agent',
    role: '作品叙述者',
    summary: '把一个项目翻译成项目介绍、简历条目、README 与面试讲法。',
    pipeline: [
      { stage: 'route', label: '读取项目' },
      { stage: 'retrieve', label: '聚合技术栈' },
      { stage: 'generate', label: '多视角输出' },
    ],
    outputs: ['项目介绍', '简历描述', 'README', '面试讲法'],
    accent: '#b18cff',
  },
  {
    id: 'site-qa',
    name: 'Site QA Agent',
    role: '站内问答',
    summary: '基于站内文章与作品做检索增强问答，回答里带着可点击的出处。',
    pipeline: [
      { stage: 'route', label: '理解问题' },
      { stage: 'retrieve', label: '向量检索' },
      { stage: 'generate', label: '引用并作答' },
    ],
    outputs: ['答案', '引用来源', '相关文章', '延伸追问'],
    accent: '#54e6c8',
  },
]

export const creativeFragments: CreativeFragment[] = [
  {
    id: 'f-prompt-voice',
    kind: 'prompt',
    span: { col: 2, row: 2 },
    label: 'Prompt',
    body: '“用我过去三篇文章的语气，写一段关于失败实验的开场。不要总结，直接进入现场。”',
    meta: 'writing · voice',
    palette: ['#1d2c52', '#0b1428'],
  },
  {
    id: 'f-code-shader',
    kind: 'code',
    span: { col: 2, row: 1 },
    label: 'GLSL',
    body: 'float n = noise(p * 1.7 + time * 0.2);\ncol = mix(deep, glow, smoothstep(0.2, 0.8, n));',
    meta: 'hero · energy core',
  },
  {
    id: 'f-note-archive',
    kind: 'note',
    span: { col: 1, row: 1 },
    label: 'Note',
    body: '博客不是时间线，是一个可以被追问的知识体。',
    meta: 'first principle',
  },
  {
    id: 'f-image-gradient',
    kind: 'image',
    span: { col: 1, row: 2 },
    label: 'Study',
    body: '夜空蓝 → 深空黑的双色渐变研究，用作文章封面的底色。',
    meta: 'palette · 02',
    palette: ['#3a1d4d', '#0a0f1f'],
  },
  {
    id: 'f-link-awwwards',
    kind: 'link',
    span: { col: 1, row: 1 },
    label: 'Reference',
    body: 'awwwards.com — editorial motion 收藏夹',
    meta: 'inspiration',
  },
  {
    id: 'f-prompt-readme',
    kind: 'prompt',
    span: { col: 1, row: 1 },
    label: 'Prompt',
    body: '“为这个项目写一段面试讲法，30 秒能讲完，突出取舍而不是功能清单。”',
    meta: 'portfolio',
  },
  {
    id: 'f-note-motion',
    kind: 'note',
    span: { col: 2, row: 1 },
    label: 'Rule',
    body: '一种核心动效只出现一次。粒子、横向滚动、clip-path、字符扰动——各归其位。',
    meta: 'motion budget',
    palette: ['#123a37', '#07201e'],
  },
  {
    id: 'f-code-route',
    kind: 'code',
    span: { col: 1, row: 1 },
    label: 'TS',
    body: 'meta: { layout: "immersive" }',
    meta: 'router',
  },
]

export const portfolioProjects: PortfolioProject[] = [
  {
    id: 'p-creatorspace',
    index: '01',
    title: 'CreatorSpace AI',
    category: 'Full-stack Platform',
    year: '2026',
    description: '融合博客、作品集与 AI 智能体的个人创作系统，本站即是它的前台。',
    stack: ['Vue 3', 'Spring Boot', 'PostgreSQL', 'pgvector'],
    palette: { from: '#1f3b6e', to: '#0a1226', accent: '#6ea8ff' },
  },
  {
    id: 'p-archive-engine',
    index: '02',
    title: 'Archive Engine',
    category: 'RAG / Retrieval',
    year: '2026',
    description: '把站内文章与作品切片入库，为问答智能体提供可引用的上下文。',
    stack: ['Java', 'LangChain4j', 'Embedding'],
    palette: { from: '#3c1f63', to: '#120825', accent: '#b18cff' },
  },
  {
    id: 'p-motion-kit',
    index: '03',
    title: 'Motion Kit',
    category: 'Creative Front-end',
    year: '2025',
    description: '一套可复用的滚动编排与 WebGL 装置，服务于沉浸式叙事页面。',
    stack: ['GSAP', 'Three.js', 'Lenis'],
    palette: { from: '#0f4a44', to: '#08201d', accent: '#54e6c8' },
  },
  {
    id: 'p-studio-cms',
    index: '04',
    title: 'Studio CMS',
    category: 'Content Tooling',
    year: '2025',
    description: '面向单一创作者的后台：草稿状态机、隐私可见性与文件引用治理。',
    stack: ['TypeScript', 'Pinia', 'REST'],
    palette: { from: '#5c2f12', to: '#241206', accent: '#ff9d5c' },
  },
  {
    id: 'p-prompt-lab',
    index: '05',
    title: 'Prompt Lab',
    category: 'AI Workbench',
    year: '2025',
    description: '收集、版本化并评测提示词，把灵感碎片沉淀成可复用的资产。',
    stack: ['Node', 'SQLite', 'Eval'],
    palette: { from: '#5a1d3a', to: '#220b16', accent: '#ff7eb0' },
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
  wordmark: 'CreatorSpace AI',
  navigation: [
    { label: 'Works', to: '/projects' },
    { label: 'Articles', to: '/articles' },
    { label: 'Admin', to: '/admin' },
  ],
  social: [
    { label: 'GitHub', handle: 'SakuraCianna' },
    { label: 'Mail', handle: 'hello@creatorspace.ai' },
  ],
}
export const heroContent = {
  kicker: 'Personal Creative Universe · AI Native',
  title: 'CreatorSpace AI',
  subtitle: 'AI-powered personal blog, creative archive and portfolio universe.',
  description:
    '一个融合博客、作品集、创意灵感与 AI 智能体的个人创作空间。文章、作品、提示词与碎片，由一套 AI 原生的系统彼此连接。',
  primary: { label: 'Explore Works', to: '/projects' },
  secondary: { label: 'Ask My AI', to: '/articles' },
  stats: [
    { value: '128', label: 'Articles' },
    { value: '36', label: 'Works' },
    { value: '3', label: 'AI Agents' },
  ],
}

/* ---- Manifesto marquee (after hero) ------------------------------------- */
export const marqueeWords = [
  'Articles',
  'Works',
  'Prompts',
  'Fragments',
  'Experiments',
  'Notes',
  'Archives',
  'Ideas',
]

export const manifesto = {
  lead: '(01) — Manifesto',
  segments: [
    { text: 'Blog is not a timeline. ', accent: false },
    { text: 'It is a universe of thinking — ', accent: true },
    { text: 'where articles, works, prompts and fragments are connected by an ', accent: false },
    { text: 'AI-native creative system', accent: true },
    { text: ', and every project becomes a trace of how I learn, build and imagine.', accent: false },
  ],
}

/* ---- Approach / process (after AI agents) ------------------------------- */
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
    body: '灵感、提示词、链接与代码片段先落进创意墙，不评判、不丢失。原材料越杂越好。',
    tags: ['Inbox', 'Fragments', 'Prompts'],
  },
  {
    no: '02',
    title: '连接',
    en: 'Connect',
    body: '内容被切片、向量化并接入检索层，文章与作品之间形成可被追问的语义网络。',
    tags: ['Embedding', 'RAG', 'Graph'],
  },
  {
    no: '03',
    title: '创作',
    en: 'Compose',
    body: '写作与作品智能体在我的语气与上下文之上协作，把碎片生长成草稿、项目与表达。',
    tags: ['Drafting', 'Agents', 'Voice'],
  },
  {
    no: '04',
    title: '展示',
    en: 'Curate',
    body: '成品被编排进这个沉浸式前台——文章、展厅与主题，构成持续生长的数字空间。',
    tags: ['Editorial', 'Gallery', 'Themes'],
  },
]

export const counters = [
  { value: 128, suffix: '', label: 'Articles published' },
  { value: 36, suffix: '', label: 'Projects archived' },
  { value: 2480, suffix: '', label: 'Fragments captured' },
  { value: 99, suffix: '%', label: 'Built in the open' },
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
    tag: 'System',
    title: '重写首页为沉浸式创作宇宙',
    detail: '用 Three.js、GSAP 与 Lenis 编排七个区块，每个区块只保留一种核心动效。',
  },
  {
    date: '2026 · 05',
    tag: 'AI',
    title: '站内问答智能体接入检索层',
    detail: '文章与作品被切片入库，回答开始携带可点击的出处与延伸追问。',
  },
  {
    date: '2026 · 04',
    tag: 'Writing',
    title: '写作智能体学会“我的语气”',
    detail: '用少量风格样本做检索增强，草稿不再像通用模板，而像我自己写的。',
  },
  {
    date: '2026 · 03',
    tag: 'Backend',
    title: 'Spring Boot 内容流水线上线',
    detail: '草稿状态机、隐私可见性与文件引用治理，为前台的沉浸式呈现兜底。',
  },
]

export const closing = {
  eyebrow: 'The Exit / Or The Beginning',
}
