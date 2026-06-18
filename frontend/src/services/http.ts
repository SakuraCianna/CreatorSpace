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

  try {
    const response = await fetch(`${appConfig.apiBaseUrl}${requestPath}`, {
      ...init,
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...init?.headers,
      },
      signal: init?.signal ?? controller.signal,
    })

    if (!response.ok) {
      const message = await readErrorMessage(response)
      throw new HttpError(message || `Request failed with status ${response.status}`, response.status)
    }

    return response.json() as Promise<T>
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
