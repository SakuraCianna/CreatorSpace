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
