import { fileURLToPath, URL } from 'node:url'

import { defineConfig, loadEnv } from 'vite'

function readNumber(value: string | undefined, fallback: number): number {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : fallback
}

export default defineConfig(({ mode }) => {
  const envDir = fileURLToPath(new URL('..', import.meta.url))
  const env = loadEnv(mode, envDir, '')
  const apiTarget = env.VITE_API_PROXY_TARGET || env.API_BASE_URL || 'http://localhost:8080'

  return {
    envDir,
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      host: env.VITE_DEV_SERVER_HOST || '127.0.0.1',
      port: readNumber(env.VITE_DEV_SERVER_PORT, 5173),
      proxy: {
        '/api': {
          target: apiTarget,
          changeOrigin: true,
        },
      },
    },
    build: {
      target: 'es2022',
    },
  }
})
