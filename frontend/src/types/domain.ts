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

export interface ArticleSummary {
  id: number
  title: string
  slug: string
  privacy: ArticlePrivacy
  status: ContentStatus
  updatedAt: string
}

export interface AdminMetric {
  label: string
  value: string
  trend: string
}
