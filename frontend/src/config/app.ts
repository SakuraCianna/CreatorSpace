function readNumber(value: string | undefined, fallback: number): number {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : fallback
}

function normalizeBaseUrl(value: string | undefined): string {
  if (!value) {
    return ''
  }
  return value.endsWith('/') ? value.slice(0, -1) : value
}

export const appConfig = {
  appName: import.meta.env.VITE_APP_NAME || 'CreatorSpace',
  appEnv: import.meta.env.VITE_APP_ENV || import.meta.env.MODE,
  apiBaseUrl: normalizeBaseUrl(import.meta.env.VITE_API_BASE_URL),
  apiTimeoutMs: readNumber(import.meta.env.VITE_API_TIMEOUT_MS, 15000),
  uploadMaxFileSizeMb: readNumber(import.meta.env.VITE_UPLOAD_MAX_FILE_SIZE_MB, 20),
} as const
