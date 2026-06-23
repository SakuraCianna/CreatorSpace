const FALLBACK_PUBLIC_PATH = '/articles'

export function normalizeAuthRedirect(value: unknown, fallback = FALLBACK_PUBLIC_PATH): string {
  const redirect = readFirstQueryValue(value).trim()
  if (!isSafeInternalPath(redirect) || isAuthPagePath(redirect)) {
    return fallback
  }
  return redirect
}

export function isAdminRedirect(value: unknown): boolean {
  return normalizeAuthRedirect(value).startsWith('/admin')
}

function readFirstQueryValue(value: unknown): string {
  if (Array.isArray(value)) {
    return typeof value[0] === 'string' ? value[0] : ''
  }
  return typeof value === 'string' ? value : ''
}

function isSafeInternalPath(path: string): boolean {
  return path.startsWith('/') && !path.startsWith('//')
}

function isAuthPagePath(path: string): boolean {
  const [pathname = ''] = path.split(/[?#]/, 1)
  return pathname === '/login' || pathname === '/register'
}
