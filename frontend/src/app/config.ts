// 解析数字类型的环境变量, 在解析失败或无效时返回安全默认值
function readNumber(value: string | undefined, fallback: number): number {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : fallback
}

// 格式化并去除 API 基础路径末尾的斜杠以保障请求路径 of 规整性
function normalizeBaseUrl(value: string | undefined): string {
  if (!value) {
    return ''
  }
  return value.endsWith('/') ? value.slice(0, -1) : value
}

// 站点全局配置对象, 包含应用名称, 运行环境, API 基础路径及超时限制等
export const appConfig = {
  appName: import.meta.env.VITE_APP_NAME || 'CreatorSpace',
  appEnv: import.meta.env.VITE_APP_ENV || import.meta.env.MODE,
  apiBaseUrl: normalizeBaseUrl(import.meta.env.VITE_API_BASE_URL),
  apiTimeoutMs: readNumber(import.meta.env.VITE_API_TIMEOUT_MS, 15000),
  uploadMaxFileSizeMb: readNumber(import.meta.env.VITE_UPLOAD_MAX_FILE_SIZE_MB, 20),
} as const
