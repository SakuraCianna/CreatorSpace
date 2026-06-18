import { requestJson } from '@/services/http'
import type {
  ArticleSummary,
  AuthToken,
  DashboardOverview,
  InspirationCard,
  InspirationType,
  PageResponse,
  ProjectSummary,
  SearchResult,
  ThemeConfig,
  UserSummary,
} from '@/shared/domain'

interface ApiEnvelope<T> {
  success: boolean
  data: T
  message: string
}

interface RegisterPayload {
  username: string
  password: string
}

interface LoginPayload {
  username: string
  password: string
}

// 调用注册接口创建用户。
export async function registerUser(payload: RegisterPayload): Promise<UserSummary> {
  const response = await requestJson<ApiEnvelope<UserSummary>>('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
  return response.data
}

// 调用后台管理员登录接口。
export async function loginAdmin(payload: LoginPayload): Promise<AuthToken> {
  const response = await requestJson<ApiEnvelope<AuthToken>>('/api/admin/auth/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
  return response.data
}

// 调用公开文章列表接口。
export async function fetchArticles(keyword = ''): Promise<PageResponse<ArticleSummary>> {
  const params = new URLSearchParams()
  if (keyword.trim()) {
    params.set('keyword', keyword.trim())
  }
  const path = params.toString() ? `/api/articles?${params.toString()}` : '/api/articles'
  const response = await requestJson<ApiEnvelope<PageResponse<ArticleSummary>>>(path)
  return response.data
}

// 按 URL 标识读取公开文章详情。
export async function fetchArticleBySlug(slug: string): Promise<ArticleSummary> {
  const response = await requestJson<ApiEnvelope<ArticleSummary>>(
    `/api/articles/slug/${encodeURIComponent(slug)}`,
  )
  return response.data
}

// 调用公开作品列表接口。
export async function fetchProjects(keyword = ''): Promise<PageResponse<ProjectSummary>> {
  const params = new URLSearchParams()
  if (keyword.trim()) {
    params.set('keyword', keyword.trim())
  }
  const path = params.toString() ? `/api/projects?${params.toString()}` : '/api/projects'
  const response = await requestJson<ApiEnvelope<PageResponse<ProjectSummary>>>(path)
  return response.data
}

// 按 URL 标识读取公开作品详情。
export async function fetchProjectBySlug(slug: string): Promise<ProjectSummary> {
  const response = await requestJson<ApiEnvelope<ProjectSummary>>(
    `/api/projects/slug/${encodeURIComponent(slug)}`,
  )
  return response.data
}

// 查询公开灵感墙。
export async function fetchInspirations(options: {
  keyword?: string
  type?: InspirationType | 'ALL'
  page?: number
  pageSize?: number
} = {}): Promise<PageResponse<InspirationCard>> {
  const params = new URLSearchParams()
  if (options.keyword?.trim()) {
    params.set('keyword', options.keyword.trim())
  }
  if (options.type && options.type !== 'ALL') {
    params.set('type', options.type)
  }
  if (options.page) {
    params.set('page', String(options.page))
  }
  if (options.pageSize) {
    params.set('pageSize', String(options.pageSize))
  }
  const query = params.toString()
  const response = await requestJson<ApiEnvelope<PageResponse<InspirationCard>>>(
    query ? `/api/inspirations?${query}` : '/api/inspirations',
  )
  return response.data
}

// 查询站内公开内容。
export async function searchContent(keyword: string): Promise<PageResponse<SearchResult>> {
  const params = new URLSearchParams({ keyword })
  const response = await requestJson<ApiEnvelope<PageResponse<SearchResult>>>(`/api/search?${params.toString()}`)
  return response.data
}

// 读取后台概览。
export async function fetchDashboardOverview(): Promise<DashboardOverview> {
  const response = await requestJson<ApiEnvelope<DashboardOverview>>('/api/admin/dashboard/overview')
  return response.data
}

// 读取站点配置 JSON。
export async function fetchSiteConfig(): Promise<Record<string, unknown>> {
  const response = await requestJson<ApiEnvelope<Record<string, unknown>>>('/api/site/config')
  return response.data
}

// 读取当前主题配置。
export async function fetchCurrentTheme(): Promise<ThemeConfig | null> {
  const response = await requestJson<ApiEnvelope<ThemeConfig | null>>('/api/theme/current')
  return response.data
}
