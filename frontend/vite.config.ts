import { fileURLToPath, URL } from 'node:url'

import vue from '@vitejs/plugin-vue'
import { defineConfig, loadEnv } from 'vite'

function readNumber(value: string | undefined, fallback: number): number {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : fallback
}

export default defineConfig(({ mode }) => {
  const envDir = fileURLToPath(new URL('..', import.meta.url))
  const env = loadEnv(mode, envDir, '')
  const apiTarget = env.VITE_API_PROXY_TARGET || env.API_BASE_URL || 'http://127.0.0.1:8080'
  const devServerHost = env.VITE_DEV_SERVER_HOST || '127.0.0.1'

  return {
    envDir,
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      host: devServerHost,
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
      chunkSizeWarningLimit: 520,
      rolldownOptions: {
        output: {
          codeSplitting: {
            groups: [
              {
                name: 'vendor-three',
                test: /node_modules[\\/]three[\\/]/,
              },
              {
                name: 'vendor-motion',
                test: /node_modules[\\/](gsap|lenis)[\\/]/,
              },
              {
                name: 'vendor-charts',
                test: /node_modules[\\/](echarts|zrender)[\\/]/,
              },
              {
                name: 'vendor-vue',
                test: /node_modules[\\/](@vue|vue|vue-router|pinia)[\\/]/,
              },
            ],
          },
        },
      },
    },
  }
})
