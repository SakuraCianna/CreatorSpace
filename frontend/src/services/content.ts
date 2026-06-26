import { requestJson } from '@/services/http'
import type {
  AdminThemeConfig,
  ArticleNeighbors,
  ArticleSummary,
  ArticlePayload,
  AuthToken,
  CategorySummary,
  CommentSummary,
  DashboardOverview,
  FileResource,
  InteractionRecord,
  InspirationCard,
  InspirationPayload,
  InspirationType,
  PageResponse,
  ProjectPayload,
  ProjectSummary,
  PublicThemeConfig,
  SearchParams,
  SearchResult,
  SiteStatisticsSummary,
  SiteSettings,
  SiteSettingsPayload,
  TagSummary,
  ThemeConfig,
  ThemePayload,
  UserSummary,
} from '@/shared/domain'

interface ApiEnvelope<T> {
  success: boolean
  data: T
  message: string
}

type ArticleStatusFilter = ArticleSummary['status'] | 'ALL'
type ProjectStatusFilter = ProjectSummary['status'] | 'ALL'
type InteractionTargetType = 'ARTICLE' | 'PROJECT' | 'COMMENT' | 'INSPIRATION'

interface RegisterPayload {
  username: string
  password: string
}

interface LoginPayload {
  username: string
  password: string
}

// 调用注册接口创建用户
export async function registerUser(payload: RegisterPayload): Promise<UserSummary> {
  const response = await requestJson<ApiEnvelope<UserSummary>>('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
  return response.data
}

// 调用普通用户登录接口
export async function loginUser(payload: LoginPayload): Promise<AuthToken> {
  const response = await requestJson<ApiEnvelope<AuthToken>>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
  return response.data
}

// 调用后台管理员登录接口
export async function loginAdmin(payload: LoginPayload): Promise<AuthToken> {
  const response = await requestJson<ApiEnvelope<AuthToken>>('/api/admin/auth/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
  return response.data
}

// 调用公开文章列表接口
export async function fetchArticles(
  keyword = '',
  tagId?: number,
  options: { page?: number; pageSize?: number } = {},
): Promise<PageResponse<ArticleSummary>> {
  const params = new URLSearchParams()
  if (keyword.trim()) {
    params.set('keyword', keyword.trim())
  }
  if (tagId) {
    params.set('tagId', String(tagId))
  }
  if (options.page) {
    params.set('page', String(options.page))
  }
  if (options.pageSize) {
    params.set('pageSize', String(options.pageSize))
  }
  const path = params.toString() ? `/api/articles?${params.toString()}` : '/api/articles'
  const response = await requestJson<ApiEnvelope<PageResponse<ArticleSummary>>>(path)
  return response.data
}

// 按 URL 标识读取公开文章详情
// 通过特定的 slug 标识符异步拉取指定文章的详细信息
export async function fetchArticleBySlug(slug: string): Promise<ArticleSummary> {
  const response = await requestJson<ApiEnvelope<ArticleSummary>>(
    `/api/articles/slug/${encodeURIComponent(slug)}`,
  )
  return response.data
}

// 查询公开文章相邻导航
export async function fetchArticleNeighbors(slug: string): Promise<ArticleNeighbors> {
  const response = await requestJson<ApiEnvelope<ArticleNeighbors>>(
    `/api/articles/slug/${encodeURIComponent(slug)}/neighbors`,
  )
  return response.data
}

// 管理员查询全部文章
export async function fetchAdminArticles(options: {
  keyword?: string
  status?: ArticleStatusFilter
  page?: number
  pageSize?: number
} = {}): Promise<PageResponse<ArticleSummary>> {
  const params = new URLSearchParams()
  if (options.keyword?.trim()) {
    params.set('keyword', options.keyword.trim())
  }
  if (options.status && options.status !== 'ALL') {
    params.set('status', options.status)
  }
  if (options.page) {
    params.set('page', String(options.page))
  }
  if (options.pageSize) {
    params.set('pageSize', String(options.pageSize))
  }
  const query = params.toString()
  const response = await requestJson<ApiEnvelope<PageResponse<ArticleSummary>>>(
    query ? `/api/admin/articles?${query}` : '/api/admin/articles',
  )
  return response.data
}

// 管理员读取文章详情
export async function fetchAdminArticle(id: number): Promise<ArticleSummary> {
  const response = await requestJson<ApiEnvelope<ArticleSummary>>(`/api/admin/articles/${id}`)
  return response.data
}

// 管理员创建文章
export async function createArticle(payload: ArticlePayload): Promise<ArticleSummary> {
  const response = await requestJson<ApiEnvelope<ArticleSummary>>('/api/admin/articles', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
  return response.data
}

// 创作者查询自己的文章队列
export async function fetchCreatorArticles(options: {
  keyword?: string
  status?: ArticleStatusFilter
  page?: number
  pageSize?: number
} = {}): Promise<PageResponse<ArticleSummary>> {
  const params = new URLSearchParams()
  if (options.keyword?.trim()) {
    params.set('keyword', options.keyword.trim())
  }
  if (options.status && options.status !== 'ALL') {
    params.set('status', options.status)
  }
  if (options.page) {
    params.set('page', String(options.page))
  }
  if (options.pageSize) {
    params.set('pageSize', String(options.pageSize))
  }
  const query = params.toString()
  const response = await requestJson<ApiEnvelope<PageResponse<ArticleSummary>>>(
    query ? `/api/creator/articles?${query}` : '/api/creator/articles',
  )
  return response.data
}

// 创作者创建文章草稿
export async function createCreatorArticle(payload: ArticlePayload): Promise<ArticleSummary> {
  const response = await requestJson<ApiEnvelope<ArticleSummary>>('/api/creator/articles', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
  return response.data
}

// 创作者读取自己的文章详情
export async function fetchCreatorArticle(id: number): Promise<ArticleSummary> {
  const response = await requestJson<ApiEnvelope<ArticleSummary>>(`/api/creator/articles/${id}`)
  return response.data
}

// 创作者更新自己的文章
export async function updateCreatorArticle(id: number, payload: ArticlePayload): Promise<ArticleSummary> {
  const response = await requestJson<ApiEnvelope<ArticleSummary>>(`/api/creator/articles/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
  return response.data
}

// 创作者提交文章审核
export async function submitCreatorArticle(id: number): Promise<ArticleSummary> {
  const response = await requestJson<ApiEnvelope<ArticleSummary>>(`/api/creator/articles/${id}/submit`, {
    method: 'PUT',
  })
  return response.data
}

// 创作者删除自己的未公开文章
export async function deleteCreatorArticle(id: number): Promise<void> {
  await requestJson<ApiEnvelope<null>>(`/api/creator/articles/${id}`, { method: 'DELETE' })
}

// 管理员更新文章
export async function updateArticle(id: number, payload: ArticlePayload): Promise<ArticleSummary> {
  const response = await requestJson<ApiEnvelope<ArticleSummary>>(`/api/admin/articles/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
  return response.data
}

// 管理员删除文章
export async function deleteArticle(id: number): Promise<void> {
  await requestJson<ApiEnvelope<null>>(`/api/admin/articles/${id}`, { method: 'DELETE' })
}

// 管理员发布或撤回文章
export async function changeArticlePublishState(id: number, action: 'publish' | 'unpublish'): Promise<ArticleSummary> {
  const response = await requestJson<ApiEnvelope<ArticleSummary>>(`/api/admin/articles/${id}/${action}`, {
    method: 'PUT',
  })
  return response.data
}

// 管理员审核通过文章
export async function approveArticle(id: number): Promise<ArticleSummary> {
  const response = await requestJson<ApiEnvelope<ArticleSummary>>(`/api/admin/articles/${id}/approve`, {
    method: 'PUT',
  })
  return response.data
}

// 管理员驳回文章
export async function rejectArticle(id: number, reviewNote: string): Promise<ArticleSummary> {
  const response = await requestJson<ApiEnvelope<ArticleSummary>>(`/api/admin/articles/${id}/reject`, {
    method: 'PUT',
    body: JSON.stringify({ reviewNote }),
  })
  return response.data
}

// 管理员切换文章置顶
export async function setArticleTop(id: number, enabled: boolean): Promise<ArticleSummary> {
  const params = new URLSearchParams({ enabled: String(enabled) })
  const response = await requestJson<ApiEnvelope<ArticleSummary>>(`/api/admin/articles/${id}/top?${params.toString()}`, {
    method: 'PUT',
  })
  return response.data
}

// 管理员切换文章推荐
export async function setArticleRecommend(id: number, enabled: boolean): Promise<ArticleSummary> {
  const params = new URLSearchParams({ enabled: String(enabled) })
  const response = await requestJson<ApiEnvelope<ArticleSummary>>(
    `/api/admin/articles/${id}/recommend?${params.toString()}`,
    { method: 'PUT' },
  )
  return response.data
}

// 调用公开作品列表接口
export async function fetchProjects(keyword = ''): Promise<PageResponse<ProjectSummary>> {
  const params = new URLSearchParams()
  if (keyword.trim()) {
    params.set('keyword', keyword.trim())
  }
  const path = params.toString() ? `/api/projects?${params.toString()}` : '/api/projects'
  const response = await requestJson<ApiEnvelope<PageResponse<ProjectSummary>>>(path)
  return response.data
}

// 按 URL 标识读取公开作品详情
export async function fetchProjectBySlug(slug: string): Promise<ProjectSummary> {
  const response = await requestJson<ApiEnvelope<ProjectSummary>>(
    `/api/projects/slug/${encodeURIComponent(slug)}`,
  )
  return response.data
}

// 管理员查询全部作品
export async function fetchAdminProjects(options: {
  keyword?: string
  status?: ProjectStatusFilter
  page?: number
  pageSize?: number
} = {}): Promise<PageResponse<ProjectSummary>> {
  const params = new URLSearchParams()
  if (options.keyword?.trim()) {
    params.set('keyword', options.keyword.trim())
  }
  if (options.status && options.status !== 'ALL') {
    params.set('status', options.status)
  }
  if (options.page) {
    params.set('page', String(options.page))
  }
  if (options.pageSize) {
    params.set('pageSize', String(options.pageSize))
  }
  const query = params.toString()
  const response = await requestJson<ApiEnvelope<PageResponse<ProjectSummary>>>(
    query ? `/api/admin/projects?${query}` : '/api/admin/projects',
  )
  return response.data
}

// 管理员读取作品详情
export async function fetchAdminProject(id: number): Promise<ProjectSummary> {
  const response = await requestJson<ApiEnvelope<ProjectSummary>>(`/api/admin/projects/${id}`)
  return response.data
}

// 管理员创建作品
export async function createProject(payload: ProjectPayload): Promise<ProjectSummary> {
  const response = await requestJson<ApiEnvelope<ProjectSummary>>('/api/admin/projects', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
  return response.data
}

// 创作者查询自己的作品队列
export async function fetchCreatorProjects(options: {
  keyword?: string
  status?: ProjectStatusFilter
  page?: number
  pageSize?: number
} = {}): Promise<PageResponse<ProjectSummary>> {
  const params = new URLSearchParams()
  if (options.keyword?.trim()) {
    params.set('keyword', options.keyword.trim())
  }
  if (options.status && options.status !== 'ALL') {
    params.set('status', options.status)
  }
  if (options.page) {
    params.set('page', String(options.page))
  }
  if (options.pageSize) {
    params.set('pageSize', String(options.pageSize))
  }
  const query = params.toString()
  const response = await requestJson<ApiEnvelope<PageResponse<ProjectSummary>>>(
    query ? `/api/creator/projects?${query}` : '/api/creator/projects',
  )
  return response.data
}

// 创作者创建作品草稿
export async function createCreatorProject(payload: ProjectPayload): Promise<ProjectSummary> {
  const response = await requestJson<ApiEnvelope<ProjectSummary>>('/api/creator/projects', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
  return response.data
}

// 创作者读取自己的作品详情
export async function fetchCreatorProject(id: number): Promise<ProjectSummary> {
  const response = await requestJson<ApiEnvelope<ProjectSummary>>(`/api/creator/projects/${id}`)
  return response.data
}

// 创作者更新自己的作品
export async function updateCreatorProject(id: number, payload: ProjectPayload): Promise<ProjectSummary> {
  const response = await requestJson<ApiEnvelope<ProjectSummary>>(`/api/creator/projects/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
  return response.data
}

// 创作者提交作品审核
export async function submitCreatorProject(id: number): Promise<ProjectSummary> {
  const response = await requestJson<ApiEnvelope<ProjectSummary>>(`/api/creator/projects/${id}/submit`, {
    method: 'PUT',
  })
  return response.data
}

// 创作者删除自己的未公开作品
export async function deleteCreatorProject(id: number): Promise<void> {
  await requestJson<ApiEnvelope<null>>(`/api/creator/projects/${id}`, { method: 'DELETE' })
}

// 管理员更新作品
export async function updateProject(id: number, payload: ProjectPayload): Promise<ProjectSummary> {
  const response = await requestJson<ApiEnvelope<ProjectSummary>>(`/api/admin/projects/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
  return response.data
}

// 管理员删除作品
export async function deleteProject(id: number): Promise<void> {
  await requestJson<ApiEnvelope<null>>(`/api/admin/projects/${id}`, { method: 'DELETE' })
}

// 管理员切换作品状态
export async function setProjectStatus(id: number, status: string): Promise<ProjectSummary> {
  const params = new URLSearchParams({ status })
  const response = await requestJson<ApiEnvelope<ProjectSummary>>(`/api/admin/projects/${id}/status?${params.toString()}`, {
    method: 'PUT',
  })
  return response.data
}

// 管理员审核通过作品
export async function approveProject(id: number): Promise<ProjectSummary> {
  const response = await requestJson<ApiEnvelope<ProjectSummary>>(`/api/admin/projects/${id}/approve`, {
    method: 'PUT',
  })
  return response.data
}

// 管理员驳回作品
export async function rejectProject(id: number, reviewNote: string): Promise<ProjectSummary> {
  const response = await requestJson<ApiEnvelope<ProjectSummary>>(`/api/admin/projects/${id}/reject`, {
    method: 'PUT',
    body: JSON.stringify({ reviewNote }),
  })
  return response.data
}

// 管理员切换作品推荐
export async function setProjectRecommend(id: number, enabled: boolean): Promise<ProjectSummary> {
  const params = new URLSearchParams({ enabled: String(enabled) })
  const response = await requestJson<ApiEnvelope<ProjectSummary>>(
    `/api/admin/projects/${id}/recommend?${params.toString()}`,
    { method: 'PUT' },
  )
  return response.data
}

// 查询指定模块分类
export async function fetchCategories(module: CategorySummary['module']): Promise<CategorySummary[]> {
  const params = new URLSearchParams({ module })
  const response = await requestJson<ApiEnvelope<CategorySummary[]>>(`/api/categories?${params.toString()}`)
  return response.data
}

// 查询标签列表
export async function fetchTags(): Promise<TagSummary[]> {
  const response = await requestJson<ApiEnvelope<TagSummary[]>>('/api/tags')
  return response.data
}

// 查询公开灵感墙
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

// 管理员查询灵感卡片
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

// 管理员创建灵感卡片
export async function createInspiration(payload: InspirationPayload): Promise<InspirationCard> {
  const response = await requestJson<ApiEnvelope<InspirationCard>>('/api/admin/inspirations', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
  return response.data
}

// 管理员更新灵感卡片
export async function updateInspiration(id: number, payload: InspirationPayload): Promise<InspirationCard> {
  const response = await requestJson<ApiEnvelope<InspirationCard>>(`/api/admin/inspirations/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
  return response.data
}

// 管理员删除灵感卡片
export async function deleteInspiration(id: number): Promise<void> {
  await requestJson<ApiEnvelope<null>>(`/api/admin/inspirations/${id}`, { method: 'DELETE' })
}

// 查询公开评论
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

// 登录用户提交评论
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

// 管理员查询评论审核队列
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

// 管理员审核评论
export async function reviewComment(id: number, action: 'approve' | 'reject'): Promise<CommentSummary> {
  const response = await requestJson<ApiEnvelope<CommentSummary>>(`/api/admin/comments/${id}/${action}`, {
    method: 'PUT',
  })
  return response.data
}

// 管理员查询文件资源
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

// 创作者查询自己的文件资源
export async function fetchCreatorFiles(options: {
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
    query ? `/api/creator/files?${query}` : '/api/creator/files',
  )
  return response.data
}

// 管理员上传文件资源
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

// 创作者上传自己的文件资源
export async function uploadCreatorFile(file: File, module: string): Promise<FileResource> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('module', module)
  const response = await requestJson<ApiEnvelope<FileResource>>('/api/creator/files/upload', {
    method: 'POST',
    body: formData,
  })
  return response.data
}

// 登录用户点赞公开内容
export async function likeTarget(targetType: InteractionTargetType, targetId: number): Promise<InteractionRecord> {
  const response = await requestJson<ApiEnvelope<InteractionRecord>>('/api/me/likes', {
    method: 'POST',
    body: JSON.stringify({ targetType, targetId }),
  })
  return response.data
}

// 登录用户取消点赞
export async function unlikeTarget(targetType: InteractionTargetType, targetId: number): Promise<void> {
  const params = new URLSearchParams({ targetType, targetId: String(targetId) })
  await requestJson<ApiEnvelope<null>>(`/api/me/likes?${params.toString()}`, { method: 'DELETE' })
}

// 登录用户收藏公开内容
export async function favoriteTarget(targetType: Exclude<InteractionTargetType, 'COMMENT'>, targetId: number): Promise<InteractionRecord> {
  const response = await requestJson<ApiEnvelope<InteractionRecord>>('/api/me/favorites', {
    method: 'POST',
    body: JSON.stringify({ targetType, targetId }),
  })
  return response.data
}

// 登录用户取消收藏
export async function unfavoriteTarget(targetType: Exclude<InteractionTargetType, 'COMMENT'>, targetId: number): Promise<void> {
  const params = new URLSearchParams({ targetType, targetId: String(targetId) })
  await requestJson<ApiEnvelope<null>>(`/api/me/favorites?${params.toString()}`, { method: 'DELETE' })
}

// 登录用户查询自己的收藏
export async function fetchMyFavorites(targetType?: Exclude<InteractionTargetType, 'COMMENT'>): Promise<PageResponse<InteractionRecord>> {
  const params = new URLSearchParams()
  if (targetType) {
    params.set('targetType', targetType)
  }
  const query = params.toString()
  const response = await requestJson<ApiEnvelope<PageResponse<InteractionRecord>>>(
    query ? `/api/me/favorites?${query}` : '/api/me/favorites',
  )
  return response.data
}

// 查询站内公开内容
export async function searchContent(options: SearchParams | string): Promise<PageResponse<SearchResult>> {
  const params = new URLSearchParams()
  if (typeof options === 'string') {
    params.set('keyword', options)
  } else {
    params.set('keyword', options.keyword)
    if (options.type) params.set('type', options.type)
    if (options.sort) params.set('sort', options.sort)
    if (options.page) params.set('page', String(options.page))
    if (options.pageSize) params.set('pageSize', String(options.pageSize))
  }
  const response = await requestJson<ApiEnvelope<PageResponse<SearchResult>>>(`/api/search?${params.toString()}`)
  return response.data
}

// 读取后台概览
export async function fetchDashboardOverview(): Promise<DashboardOverview> {
  const response = await requestJson<ApiEnvelope<DashboardOverview>>('/api/admin/dashboard/overview')
  return response.data
}

// 查询公开留言
export async function fetchGuestbook(options: {
  page?: number
  pageSize?: number
} = {}): Promise<PageResponse<{ id: number; userId?: number | null; displayName: string; content: string; status: string; likeCount: number; createdAt?: string | null }>> {
  const params = new URLSearchParams()
  if (options.page) params.set('page', String(options.page))
  if (options.pageSize) params.set('pageSize', String(options.pageSize))
  const query = params.toString()
  const response = await requestJson<ApiEnvelope<PageResponse<{ id: number; userId?: number | null; displayName: string; content: string; status: string; likeCount: number; createdAt?: string | null }>>>(
    query ? `/api/guestbook?${query}` : '/api/guestbook'
  )
  return response.data
}

// 登录用户提交留言
export async function submitGuestbook(content: string): Promise<{ id: number; displayName: string; content: string; status: string; createdAt?: string | null }> {
  const response = await requestJson<ApiEnvelope<{ id: number; displayName: string; content: string; status: string; createdAt?: string | null }>>('/api/guestbook', {
    method: 'POST',
    body: JSON.stringify({ content }),
  })
  return response.data
}

// 管理员查询留言
export async function fetchAdminGuestbook(options: {
  status?: string
  page?: number
  pageSize?: number
} = {}): Promise<PageResponse<{ id: number; userId?: number | null; displayName: string; content: string; status: string; likeCount: number; createdAt?: string | null }>> {
  const params = new URLSearchParams()
  if (options.status && options.status !== 'ALL') params.set('status', options.status)
  if (options.page) params.set('page', String(options.page))
  if (options.pageSize) params.set('pageSize', String(options.pageSize))
  const query = params.toString()
  const response = await requestJson<ApiEnvelope<PageResponse<{ id: number; userId?: number | null; displayName: string; content: string; status: string; likeCount: number; createdAt?: string | null }>>>(
    query ? `/api/admin/guestbook?${query}` : '/api/admin/guestbook'
  )
  return response.data
}

// 管理员审核留言
export async function reviewGuestbook(id: number, action: 'approve' | 'reject'): Promise<void> {
  await requestJson<ApiEnvelope<null>>(`/api/admin/guestbook/${id}/${action}`, { method: 'PUT' })
}

// 读取站点配置 JSON
export async function fetchSiteConfig(): Promise<Record<string, unknown>> {
  const response = await requestJson<ApiEnvelope<Record<string, unknown>>>('/api/site/config')
  return response.data
}

// 读取公开站点访问统计摘要
export async function fetchSiteStatisticsSummary(): Promise<SiteStatisticsSummary> {
  const response = await requestJson<ApiEnvelope<SiteStatisticsSummary>>('/api/site/statistics/summary')
  return response.data
}

// 读取当前主题配置
export async function fetchCurrentTheme(): Promise<ThemeConfig | null> {
  const response = await requestJson<ApiEnvelope<ThemeConfig | null>>('/api/theme/current')
  return response.data
}

// 读取公开主题列表, 供前台主题展示区使用
export async function fetchThemes(): Promise<PublicThemeConfig[]> {
  const response = await requestJson<ApiEnvelope<PublicThemeConfig[]>>('/api/themes')
  return response.data
}

// 管理员查询全部主题配置
export async function fetchAdminThemes(): Promise<AdminThemeConfig[]> {
  const response = await requestJson<ApiEnvelope<AdminThemeConfig[]>>('/api/admin/themes')
  return response.data
}

// 管理员更新主题基础信息和扩展配置
export async function updateTheme(id: number, payload: ThemePayload): Promise<AdminThemeConfig> {
  const response = await requestJson<ApiEnvelope<AdminThemeConfig>>(`/api/admin/themes/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
  return response.data
}

// 管理员切换当前启用主题
export async function switchTheme(id: number): Promise<AdminThemeConfig> {
  const response = await requestJson<ApiEnvelope<AdminThemeConfig>>(`/api/admin/themes/${id}/switch`, {
    method: 'PUT',
  })
  return response.data
}

// 管理员读取站点设置工作台数据
export async function fetchAdminSiteSettings(): Promise<SiteSettings> {
  const response = await requestJson<ApiEnvelope<SiteSettings>>('/api/admin/site/settings')
  return response.data
}

// 管理员保存站点身份、导航、社交链接、页面和 JSON 配置
export async function updateSiteSettings(payload: SiteSettingsPayload): Promise<SiteSettings> {
  const response = await requestJson<ApiEnvelope<SiteSettings>>('/api/admin/site/settings', {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
  return response.data
}

