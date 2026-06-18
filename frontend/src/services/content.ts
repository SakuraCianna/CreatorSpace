import { requestJson } from '@/services/http'
import type {
  ArticleSummary,
  AuthToken,
  CommentSummary,
  DashboardOverview,
  FileResource,
  InspirationCard,
  InspirationPayload,
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

// 调用普通用户登录接口。
export async function loginUser(payload: LoginPayload): Promise<AuthToken> {
  const response = await requestJson<ApiEnvelope<AuthToken>>('/api/auth/login', {
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

// 管理员查询灵感卡片。
export async function fetchAdminInspirations(options: {
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
    query ? `/api/admin/inspirations?${query}` : '/api/admin/inspirations',
  )
  return response.data
}

// 管理员创建灵感卡片。
export async function createInspiration(payload: InspirationPayload): Promise<InspirationCard> {
  const response = await requestJson<ApiEnvelope<InspirationCard>>('/api/admin/inspirations', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
  return response.data
}

// 管理员更新灵感卡片。
export async function updateInspiration(id: number, payload: InspirationPayload): Promise<InspirationCard> {
  const response = await requestJson<ApiEnvelope<InspirationCard>>(`/api/admin/inspirations/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
  return response.data
}

// 管理员删除灵感卡片。
export async function deleteInspiration(id: number): Promise<void> {
  await requestJson<ApiEnvelope<null>>(`/api/admin/inspirations/${id}`, { method: 'DELETE' })
}

// 查询公开评论。
export async function fetchComments(options: {
  targetType: 'ARTICLE' | 'PROJECT' | 'MESSAGE'
  targetId: number
  page?: number
  pageSize?: number
}): Promise<PageResponse<CommentSummary>> {
  const params = new URLSearchParams({
    targetType: options.targetType,
    targetId: String(options.targetId),
  })
  if (options.page) {
    params.set('page', String(options.page))
  }
  if (options.pageSize) {
    params.set('pageSize', String(options.pageSize))
  }
  const response = await requestJson<ApiEnvelope<PageResponse<CommentSummary>>>(`/api/comments?${params.toString()}`)
  return response.data
}

// 登录用户提交评论。
export async function submitComment(payload: {
  targetType: 'ARTICLE' | 'PROJECT' | 'MESSAGE'
  targetId: number
  parentId?: number | null
  content: string
}): Promise<CommentSummary> {
  const response = await requestJson<ApiEnvelope<CommentSummary>>('/api/comments', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
  return response.data
}

// 管理员查询评论审核队列。
export async function fetchAdminComments(options: {
  status?: CommentSummary['status'] | 'ALL'
  targetType?: CommentSummary['targetType'] | 'ALL'
  page?: number
  pageSize?: number
} = {}): Promise<PageResponse<CommentSummary>> {
  const params = new URLSearchParams()
  if (options.status && options.status !== 'ALL') {
    params.set('status', options.status)
  }
  if (options.targetType && options.targetType !== 'ALL') {
    params.set('targetType', options.targetType)
  }
  if (options.page) {
    params.set('page', String(options.page))
  }
  if (options.pageSize) {
    params.set('pageSize', String(options.pageSize))
  }
  const query = params.toString()
  const response = await requestJson<ApiEnvelope<PageResponse<CommentSummary>>>(
    query ? `/api/admin/comments?${query}` : '/api/admin/comments',
  )
  return response.data
}

// 管理员审核评论。
export async function reviewComment(id: number, action: 'approve' | 'reject'): Promise<CommentSummary> {
  const response = await requestJson<ApiEnvelope<CommentSummary>>(`/api/admin/comments/${id}/${action}`, {
    method: 'PUT',
  })
  return response.data
}

// 管理员查询文件资源。
export async function fetchAdminFiles(options: {
  module?: string
  page?: number
  pageSize?: number
} = {}): Promise<PageResponse<FileResource>> {
  const params = new URLSearchParams()
  if (options.module && options.module !== 'ALL') {
    params.set('module', options.module)
  }
  if (options.page) {
    params.set('page', String(options.page))
  }
  if (options.pageSize) {
    params.set('pageSize', String(options.pageSize))
  }
  const query = params.toString()
  const response = await requestJson<ApiEnvelope<PageResponse<FileResource>>>(
    query ? `/api/admin/files?${query}` : '/api/admin/files',
  )
  return response.data
}

// 管理员上传文件资源。
export async function uploadAdminFile(file: File, module: string): Promise<FileResource> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('module', module)
  const response = await requestJson<ApiEnvelope<FileResource>>('/api/admin/files/upload', {
    method: 'POST',
    body: formData,
  })
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
