// 声明首页内容块相关的静态数据结构和接口类型
// 声明文章的推荐类型, 分为精选与常规
export type ArticleKind = 'feature' | 'standard'

export interface FeaturedArticle {
  id: string
  slug: string
  kind: ArticleKind
  targetType: 'ARTICLE' | 'PROJECT'
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
  backgroundImage?: string
}

// 声明前台首屏展示的主题预设实体结构
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

// 站点基础身份描述的配置模型
export interface SiteConfig {
  brand: string
  wordmark: string
  navigation: { label: string; to: string; external?: boolean }[]
  social: { label: string; handle: string; href?: string }[]
}

export interface HomeHeroAction {
  label: string
  to: string
  external?: boolean
}

export interface HomeHeroContent {
  kicker: string
  titleLines: string[]
  subtitle: string
  description: string
  primary: HomeHeroAction | null
  secondary: HomeHeroAction | null
  stats: { value: string; label: string }[]
}

export interface ManifestoContent {
  lead: string
  segments: { text: string; accent: boolean }[]
  cards: { label: string; value: string; detail: string }[]
}

export interface ApproachStep {
  no: string
  title: string
  en: string
  body: string
  tags: string[]
}

export interface FieldNote {
  date: string
  tag: string
  title: string
  detail: string
}

export interface CounterItem {
  value: number
  suffix: string
  label: string
}
