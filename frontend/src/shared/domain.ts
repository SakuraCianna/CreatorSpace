export type UserRole = 'ADMIN' | 'USER'

export type ArticlePrivacy =
  | 'PUBLIC'
  | 'SELF'
  | 'FRIENDS'
  | 'SELECTED_FRIENDS'
  | 'EXCLUDED_FRIENDS'

export type ContentStatus = 'DRAFT' | 'PUBLISHED' | 'PRIVATE' | 'SCHEDULED' | 'ARCHIVED'

export interface UserSummary {
  id: number
  username: string
  roles: UserRole[]
}

export interface AuthToken {
  accessToken: string
  user: UserSummary
}

export interface CategorySummary {
  id: number
  module: 'ARTICLE' | 'PROJECT' | 'INSPIRATION'
  name: string
  slug: string
  description?: string | null
  sortOrder: number
}

export interface TagSummary {
  id: number
  name: string
  slug: string
  color?: string | null
  weight: number
}

export interface ArticleSummary {
  id: number
  title: string
  slug: string
  summary?: string | null
  coverUrl?: string | null
  contentMarkdown?: string | null
  privacyType: ArticlePrivacy
  status: ContentStatus
  category?: CategorySummary | null
  tags: TagSummary[]
  publishTime?: string | null
}

export interface ProjectSummary {
  id: number
  title: string
  slug: string
  description?: string | null
  coverUrl?: string | null
  projectType: string
  techStack: string[]
  githubUrl?: string | null
  demoUrl?: string | null
  videoUrl?: string | null
  contentMarkdown?: string | null
  status: string
  recommended: boolean
  tags: TagSummary[]
}

export type InspirationType = 'IMAGE' | 'TEXT' | 'PROMPT' | 'CODE' | 'LINK'

export interface InspirationCard {
  id: number
  title: string
  content?: string | null
  imageUrl?: string | null
  cardType: InspirationType
  sourceUrl?: string | null
  color?: string | null
  sortOrder: number
  createdAt?: string | null
  tags: TagSummary[]
}

export interface SearchResult {
  type: 'ARTICLE' | 'PROJECT' | 'INSPIRATION'
  title: string
  slug: string
  description?: string | null
  coverUrl?: string | null
  occurredAt?: string | null
  score: number
}

export interface ThemeConfig {
  themeName: string
  displayName: string
  primaryColor: string
  backgroundType: string
  backgroundImage?: string | null
  fontFamily?: string | null
  cardStyle: string
  layoutType: string
  config: Record<string, unknown>
}

export interface DashboardMetric {
  label: string
  value: string
  note: string
}

export interface DashboardRank {
  title: string
  slug: string
  views: number
  likes: number
}

export interface DashboardTrendPoint {
  date: string
  pv: number
}

export interface DashboardActivity {
  operation: string
  module: string
  createdAt: string
}

export interface DashboardOverview {
  metrics: DashboardMetric[]
  hotArticles: DashboardRank[]
  hotProjects: DashboardRank[]
  visitTrend: DashboardTrendPoint[]
  recentActivities: DashboardActivity[]
}

export interface PageResponse<T> {
  records: T[]
  page: number
  pageSize: number
  total: number
}

export interface AdminMetric {
  label: string
  value: string
  trend: string
}
