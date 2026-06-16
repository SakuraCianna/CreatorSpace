import { appConfig } from '@/config/app'

export async function requestJson<T>(path: string, init?: RequestInit): Promise<T> {
  const controller = new AbortController()
  const timeoutId = window.setTimeout(() => controller.abort(), appConfig.apiTimeoutMs)
  const requestPath = path.startsWith('/') ? path : `/${path}`

  try {
    const response = await fetch(`${appConfig.apiBaseUrl}${requestPath}`, {
      ...init,
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        ...init?.headers,
      },
      signal: init?.signal ?? controller.signal,
    })

    if (!response.ok) {
      throw new Error(`Request failed with status ${response.status}`)
    }

    return response.json() as Promise<T>
  } finally {
    window.clearTimeout(timeoutId)
  }
}
