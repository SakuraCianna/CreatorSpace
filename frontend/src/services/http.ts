import { appConfig } from '@/app/config'

export const ACCESS_TOKEN_KEY = 'creatorspace.accessToken'

export class HttpError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message)
    this.name = 'HttpError'
  }
}

// 发送 JSON 请求并处理超时、请求头和统一错误信息。
export async function requestJson<T>(path: string, init?: RequestInit): Promise<T> {
  const controller = new AbortController()
  const timeoutId = window.setTimeout(() => controller.abort(), appConfig.apiTimeoutMs)
  const requestPath = path.startsWith('/') ? path : `/${path}`
  const token = window.localStorage.getItem(ACCESS_TOKEN_KEY)
  const isFormData = init?.body instanceof FormData

  try {
    const response = await fetch(`${appConfig.apiBaseUrl}${requestPath}`, {
      ...init,
      headers: {
        Accept: 'application/json',
        ...(isFormData ? {} : { 'Content-Type': 'application/json' }),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...init?.headers,
      },
      signal: init?.signal ?? controller.signal,
    })

    if (!response.ok) {
      const message = await readErrorMessage(response)
      throw new HttpError(message || statusMessage(response.status), response.status)
    }

    return response.json() as Promise<T>
  } catch (error) {
    if (error instanceof HttpError) {
      throw error
    }
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw new HttpError('请求超时，请确认后端服务是否已经启动', 0)
    }
    throw new HttpError('无法连接后端服务，请确认服务是否已经启动', 0)
  } finally {
    window.clearTimeout(timeoutId)
  }
}

// 从失败响应中读取错误信息。
async function readErrorMessage(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as { message?: string }
    return body.message ?? ''
  } catch {
    return ''
  }
}

function statusMessage(status: number): string {
  if (status === 400) {
    return '请求参数不完整或格式不正确'
  }
  if (status === 401) {
    return '登录信息不正确，或登录状态已过期'
  }
  if (status === 403) {
    return '当前账号没有权限执行这个操作'
  }
  if (status === 404) {
    return '请求的内容不存在'
  }
  if (status === 409) {
    return '数据已存在或状态冲突，请检查后再试'
  }
  if (status >= 500) {
    return '服务暂时不可用，请稍后再试'
  }
  return `请求失败，状态码 ${status}`
}

export function toUserMessage(error: unknown, fallback: string): string {
  if (error instanceof HttpError && error.message) {
    return error.message
  }
  return fallback
}
