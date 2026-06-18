import { requestJson } from '@/services/http'
import type { ArticleSummary, PageResponse, ProjectSummary, UserSummary } from '@/shared/domain'

interface ApiEnvelope<T> {
  success: boolean
  data: T
  message: string
}

interface RegisterPayload {
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
