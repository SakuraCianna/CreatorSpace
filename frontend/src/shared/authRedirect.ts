const FALLBACK_PUBLIC_PATH = '/articles'

// 过滤并规范化登录成功后的重定向路径, 保障安全性
// 校验登录重定向地址参数是否安全, 避开非斜杠开头的第三方钓鱼 URL
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

// 判断重定向路由是否为站点内部合规路径
function isSafeInternalPath(path: string): boolean {
  return path.startsWith('/') && !path.startsWith('// ')
}

function isAuthPagePath(path: string): boolean {
  const [pathname = ''] = path.split(/[?#]/, 1)
  return pathname === '/login' || pathname === '/register'
}
