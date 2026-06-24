import { appConfig } from '@/app/config'

export const ACCESS_TOKEN_KEY = 'creatorspace.accessToken'

export class HttpError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly requestPath: string,
    readonly method: string,
    readonly backendMessage = '',
  ) {
    super(message)
    this.name = 'HttpError'
  }
}

// 发送 JSON 请求并处理超时、请求头和统一错误信息
// 安全发送 JSON 请求并自动挂载授权 Token 请求头, 针对超时及后端连接异常进行统一拦截处理
export async function requestJson<T>(path: string, init?: RequestInit): Promise<T> {
  const controller = new AbortController()
  const timeoutId = window.setTimeout(() => controller.abort(), appConfig.apiTimeoutMs)
  const requestPath = path.startsWith('/') ? path : `/${path}`
  const method = init?.method?.toUpperCase() ?? 'GET'
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
      const backendMessage = await readErrorMessage(response)
      throw new HttpError(
        buildHttpErrorMessage(response.status, requestPath, method, backendMessage),
        response.status,
        requestPath,
        method,
        backendMessage,
      )
    }

    return response.json() as Promise<T>
  } catch (error) {
    if (error instanceof HttpError) {
      throw error
    }
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw new HttpError(
        `请求超时：${method} ${requestPath} 在 ${appConfig.apiTimeoutMs}ms 内没有响应，请确认后端服务是否启动并且接口没有卡住。`,
        0,
        requestPath,
        method,
      )
    }
    throw new HttpError(
      `无法连接后端服务：${method} ${requestPath} 请求没有到达后端，请确认 Vite 代理目标和 8080 后端服务是否正常。`,
      0,
      requestPath,
      method,
    )
  } finally {
    window.clearTimeout(timeoutId)
  }
}

// 从失败响应中读取错误信息
async function readErrorMessage(response: Response): Promise<string> {
  try {
    const contentType = response.headers.get('content-type') ?? ''
    if (contentType.includes('application/json')) {
      const body = (await response.json()) as { message?: unknown; error?: unknown }
      return readString(body.message) || readString(body.error)
    }
    return ''
  } catch {
    return ''
  }
}

function buildHttpErrorMessage(status: number, requestPath: string, method: string, backendMessage: string): string {
  const reason = backendMessage.trim() || statusMessage(status)
  const hint = statusHint(status, requestPath)
  return `${method} ${requestPath} 返回 ${status}：${reason}${hint ? `。${hint}` : ''}`
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

function statusHint(status: number, requestPath: string): string {
  if (status === 400) {
    return '通常是表单字段缺失、格式不符合后端校验，或请求参数名称不对'
  }
  if (status === 401) {
    if (requestPath.includes('/auth/login')) {
      return '这是登录校验失败，通常是用户名不存在或密码错误'
    }
    return '通常是用户名或密码不匹配，或者本地保存的登录令牌已经过期'
  }
  if (status === 403) {
    return '通常是当前账号缺少对应角色权限，或账号状态被后端禁用'
  }
  if (status === 404) {
    return '通常是接口地址、内容 slug 或记录 ID 不存在'
  }
  if (status === 409) {
    return '通常是用户名、slug、分类或其他唯一字段已经存在'
  }
  if (status >= 500) {
    return '通常是后端异常、数据库连接失败、迁移数据缺失或服务配置错误'
  }
  return ''
}

function readString(value: unknown): string {
  return typeof value === 'string' ? value.trim() : ''
}

// 把任意异常格式转化为用户可直接阅读的友善错误提示消息
export function toUserMessage(error: unknown, fallback: string): string {
  if (error instanceof HttpError && error.message) {
    return error.message
  }
  return fallback
}
