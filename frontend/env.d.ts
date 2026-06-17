/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_APP_NAME?: string
  readonly VITE_APP_ENV?: string
  readonly VITE_API_BASE_URL?: string
  readonly VITE_API_TIMEOUT_MS?: string
  readonly VITE_API_PROXY_TARGET?: string
  readonly VITE_DEV_SERVER_HOST?: string
  readonly VITE_DEV_SERVER_PORT?: string
  readonly VITE_UPLOAD_MAX_FILE_SIZE_MB?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
