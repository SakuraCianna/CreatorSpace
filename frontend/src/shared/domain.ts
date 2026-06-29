// 定义全站共享的数据实体和接口模型
// 站点角色分为系统管理员与普通授权读者
export type UserRole = 'ADMIN' | 'USER'

export type ArticlePrivacy =
  | 'PUBLIC'
  | 'SELF'
  | 'FRIENDS'
  | 'SELECTED_FRIENDS'
  | 'EXCLUDED_FRIENDS'

export type ContentStatus =
  | 'DRAFT'
  | 'PENDING_REVIEW'
  | 'PUBLISHED'
  | 'PRIVATE'
  | 'REJECTED'
  | 'SCHEDULED'
  | 'ARCHIVED'

export type ProjectStatus = 'DRAFT' | 'PENDING_REVIEW' | 'VISIBLE' | 'HIDDEN' | 'REJECTED' | 'ARCHIVED'

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
  enabled?: boolean | null
}

export interface TagSummary {
  id: number
  name: string
  slug: string
  color?: string | null
  weight: number
}

export interface CategoryPayload {
  module: CategorySummary['module']
  name: string
  slug: string
  description?: string | null
  sortOrder?: number | null
  enabled?: boolean | null
}

export interface TagPayload {
  name: string
  slug: string
  color?: string | null
  weight?: number | null
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
  viewCount?: number | null
  likeCount?: number | null
  commentCount?: number | null
  top?: boolean | null
  recommended?: boolean | null
  category?: CategorySummary | null
  tags: TagSummary[]
  publishTime?: string | null
  authorId?: number | null
  submittedAt?: string | null
  reviewedAt?: string | null
  reviewNote?: string | null
}

export interface ArticleNeighbors {
  previousArticle?: ArticleSummary | null
  nextArticle?: ArticleSummary | null
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
  status: ProjectStatus
  recommended: boolean
  viewCount?: number | null
  likeCount?: number | null
  favoriteCount?: number | null
  commentCount?: number | null
  tags: TagSummary[]
  authorId?: number | null
  submittedAt?: string | null
  reviewedAt?: string | null
  reviewNote?: string | null
  screenshots?: ProjectScreenshot[]
  milestones?: ProjectMilestone[]
  resources?: ProjectResourceLink[]
  processNotes?: ProjectProcessNote[]
}

export interface ProjectFilterRecommendations {
  projectTypes: string[]
  techStacks: string[]
}

export interface ProjectScreenshot {
  imageUrl: string
  caption?: string | null
  sortOrder: number
}

export interface ProjectMilestone {
  title: string
  description?: string | null
  milestoneDate?: string | null
  sortOrder: number
}

export interface ProjectResourceLink {
  kind: string
  label: string
  url: string
  sortOrder: number
}

export interface ProjectProcessNote {
  phase: string
  title: string
  body: string
  sortOrder: number
}

export type InspirationType = 'IMAGE' | 'TEXT' | 'PROMPT' | 'CODE' | 'LINK' | 'SKETCH' | 'REFERENCE'

export interface InspirationRelation {
  targetType: 'ARTICLE' | 'PROJECT' | 'INSPIRATION'
  targetId: number
  relationType: 'REFERENCE' | 'SEED' | 'DERIVED_FROM'
  targetTitle?: string | null
  targetSlug?: string | null
}

export interface InspirationCard {
  id: number
  title: string
  content?: string | null
  imageUrl?: string | null
  cardType: InspirationType
  sourceUrl?: string | null
  color?: string | null
  isPublic?: boolean | null
  sortOrder: number
  createdAt?: string | null
  tags: TagSummary[]
  relations?: InspirationRelation[]
}

export interface InspirationPayload {
  title: string
  content?: string | null
  imageUrl?: string | null
  cardType: InspirationType
  sourceUrl?: string | null
  color?: string | null
  isPublic: boolean
  sortOrder: number
  tagIds: number[]
}

export interface ArticlePayload {
  title: string
  slug: string
  summary?: string | null
  contentMarkdown: string
  coverUrl?: string | null
  categoryId?: number | null
  tagIds: number[]
  privacyType: ArticlePrivacy
}

export interface ProjectPayload {
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
  tagIds: number[]
  recommended: boolean
}

// 站点评论实体卡片接口模型
export interface CommentSummary {
  id: number
  targetType: 'ARTICLE' | 'PROJECT' | 'MESSAGE'
  targetId: number
  parentId?: number | null
  rootId?: number | null
  userId: number
  username: string
  content: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'SPAM'
  depth: number
  replyCount: number
  likeCount: number
  createdAt?: string | null
}

export interface FileResource {
  id: number
  fileName: string
  originalName: string
  relativePath: string
  publicUrl: string
  fileType: string
  fileSize: number
  storageType: string
  module: string
  createdAt?: string | null
}

export interface InteractionRecord {
  id: number
  targetType: 'ARTICLE' | 'PROJECT' | 'COMMENT' | 'INSPIRATION'
  targetId: number
  createdAt?: string | null
}

export type SearchResultType = 'ARTICLE' | 'PROJECT' | 'INSPIRATION' | 'TAG' | 'CATEGORY' | 'PAGE'
export type SearchSortType = 'relevance' | 'latest' | 'popular'

export interface SearchParams {
  keyword: string
  type?: SearchResultType | ''
  sort?: SearchSortType
  page?: number
  pageSize?: number
}

export interface SearchResult {
  type: SearchResultType
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

export interface AdminThemeConfig extends ThemeConfig {
  id: number
  active: boolean
  createdAt?: string | null
  updatedAt?: string | null
}

export interface PublicThemeConfig extends ThemeConfig {
  active: boolean
}

export interface ThemePayload {
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

export interface SiteProfile {
  id?: number | null
  profileKey: string
  displayName: string
  headline?: string | null
  avatarUrl?: string | null
  bio?: string | null
  contactEmail?: string | null
  location?: string | null
  profileJson: Record<string, unknown>
  active?: boolean | null
  updatedAt?: string | null
}

export interface NavigationItem {
  id?: number | null
  label: string
  path: string
  icon?: string | null
  groupName: string
  sortOrder: number
  visible: boolean
  extraJson?: Record<string, unknown>
}

export interface SocialLink {
  id?: number | null
  platform: string
  label: string
  url: string
  icon?: string | null
  sortOrder: number
  visible: boolean
}

export interface PageConfig {
  id?: number | null
  pageKey: string
  title: string
  slug: string
  seoTitle?: string | null
  seoDescription?: string | null
  contentJson: Record<string, unknown>
  layoutJson: Record<string, unknown>
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
}

export interface SiteConfigEntry {
  id?: number | null
  configKey: string
  configValue: Record<string, unknown>
  description?: string | null
}

export interface SiteSettings {
  profile: SiteProfile | null
  navigationItems: NavigationItem[]
  socialLinks: SocialLink[]
  pages: PageConfig[]
  configs: SiteConfigEntry[]
}

export interface SiteSettingsPayload {
  profile?: SiteProfile | null
  navigationItems?: NavigationItem[]
  socialLinks?: SocialLink[]
  pages?: PageConfig[]
  configs?: SiteConfigEntry[]
}

export type SensitiveWordMatchType = 'EXACT' | 'CONTAINS' | 'REGEX'
export type SensitiveWordSeverity = 'REVIEW' | 'REJECT' | 'MASK'

export interface SensitiveWordSummary {
  id: number
  word: string
  matchType: SensitiveWordMatchType
  severity: SensitiveWordSeverity
  enabled: boolean
  createdAt?: string | null
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

export interface DashboardSearchKeyword {
  keyword: string
  count: number
}

export interface DashboardOverview {
  metrics: DashboardMetric[]
  hotArticles: DashboardRank[]
  hotProjects: DashboardRank[]
  hotSearchKeywords: DashboardSearchKeyword[]
  visitTrend: DashboardTrendPoint[]
  searchTrend: DashboardTrendPoint[]
  recentActivities: DashboardActivity[]
}
export type OperationLogModule =
  | 'ALL'
  | 'ARTICLE'
  | 'PROJECT'
  | 'COMMENT'
  | 'FILE'
  | 'THEME'
  | 'SITE'
  | 'INSPIRATION'
  | 'GUESTBOOK'
  | 'CATEGORY'
  | 'TAG'
  | 'ADMIN'

export interface OperationLogQuery {
  module?: OperationLogModule | ''
  operation?: string
  operatorId?: number | null
  startTime?: string
  endTime?: string
  page?: number
  pageSize?: number
}

export interface OperationLogSummary {
  id: number
  operatorId?: number | null
  operation: string
  module: Exclude<OperationLogModule, 'ALL'> | string
  targetType?: string | null
  targetId?: number | null
  requestMethod?: string | null
  requestPath?: string | null
  ipAddress?: string | null
  userAgent?: string | null
  detailJson?: string | null
  createdAt: string
}
export type AiTaskType = 'SUMMARY' | 'TAGS' | 'REVIEW' | 'OPERATION' | 'GENERAL'
export type AiTargetType = 'ARTICLE' | 'PROJECT' | 'COMMENT' | ''
export type AiTaskStatus = 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'DISCARDED'
export type AiSuggestionStatus = 'PENDING' | 'ADOPTED' | 'REJECTED' | 'EXPIRED'

export interface AiTaskPayload {
  taskType: AiTaskType
  targetType?: AiTargetType
  targetId?: number | null
  prompt: string
}

export interface AiMessageSummary {
  id: number
  taskId: number
  role: 'SYSTEM' | 'USER' | 'ASSISTANT' | 'TOOL' | string
  content: string
  tokenCount?: number | null
  createdAt: string
}

export interface AiSuggestionSummary {
  id: number
  taskId?: number | null
  targetType?: string | null
  targetId?: number | null
  suggestionType: string
  content: string
  status: AiSuggestionStatus | string
  adoptedBy?: number | null
  adoptedAt?: string | null
  createdAt: string
}

export interface AiTaskSummary {
  id: number
  taskType: string
  targetType?: string | null
  targetId?: number | null
  prompt: string
  status: AiTaskStatus | string
  provider: string
  modelName?: string | null
  createdBy?: number | null
  createdAt: string
  updatedAt: string
  messages: AiMessageSummary[]
  suggestions: AiSuggestionSummary[]
  notice?: string | null
}

export interface SiteStatisticsSummary {
  totalPv: number
  totalUv: number
  todayPv: number
  todayUv: number
  contentViews: number
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
