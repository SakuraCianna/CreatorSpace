<template>
  <div class="public-shell">
    <div ref="sceneHost" class="frontstage-webgl" aria-hidden="true" />
    <div class="public-frame" aria-hidden="true">
      <span class="public-frame__line public-frame__line--top" />
      <span class="public-frame__line public-frame__line--right" />
      <span class="public-frame__line public-frame__line--bottom" />
      <span class="public-frame__line public-frame__line--left" />
    </div>
    <div class="scroll-rail" aria-hidden="true">
      <span :style="{ transform: `scaleY(${scrollProgress})` }" />
    </div>
    <header class="public-header">
      <RouterLink class="brand" to="/" aria-label="返回 CreatorSpace 首页">
        <span class="brand-mark">
          <img src="/public.svg" alt="" aria-hidden="true" />
        </span>
        <span class="brand-copy">
          <strong>CreatorSpace</strong>
          <small>Personal Theme Archive</small>
        </span>
      </RouterLink>

      <button class="icon-button nav-toggle" type="button" :aria-expanded="navOpen" @click="navOpen = !navOpen">
        <Menu v-if="!navOpen" :size="19" />
        <X v-else :size="19" />
        <span class="sr-only">切换导航</span>
      </button>

      <nav class="public-nav" :class="{ 'is-open': navOpen }" aria-label="前台导航">
        <RouterLink v-for="item in navItems" :key="item.to" :to="item.to" @click="navOpen = false">
          <component :is="item.icon" :size="16" />
          <span>{{ item.label }}</span>
        </RouterLink>
        <RouterLink class="mobile-auth-action mobile-auth-action--tonal" to="/login" @click="navOpen = false">
          登录
        </RouterLink>
        <RouterLink class="mobile-auth-action mobile-auth-action--filled" to="/register" @click="navOpen = false">
          注册
        </RouterLink>
      </nav>

      <div class="public-actions">
        <RouterLink class="button button-tonal button-compact" to="/login">登录</RouterLink>
        <RouterLink class="button button-filled button-compact" to="/register">注册</RouterLink>
      </div>
    </header>

    <main class="public-main">
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch, type Component } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { BookOpen, Home, Images, Info, Lightbulb, Menu, Search, X } from '@lucide/vue'

import { fetchSiteConfig } from '@/services/content'
import { prefersReducedMotion } from '@/shared/composables/useReducedMotion'

const navOpen = ref(false)
const sceneHost = ref<HTMLElement | null>(null)
const scrollProgress = ref(0)
const route = useRoute()
let disposeScene: (() => void) | null = null
let setScenePaused: ((paused: boolean) => void) | null = null
let setScenePointer: ((nx: number, ny: number) => void) | null = null
let progressRaf = 0

interface PublicNavItem {
  to: string
  label: string
  icon: Component
}

const iconMap: Record<string, Component> = {
  'book-open': BookOpen,
  home: Home,
  images: Images,
  info: Info,
  lightbulb: Lightbulb,
  search: Search,
}

const fallbackNavItems: PublicNavItem[] = [
  { to: '/articles', label: '文章', icon: BookOpen },
  { to: '/projects', label: '作品', icon: Images },
  { to: '/inspirations', label: '灵感', icon: Lightbulb },
  { to: '/search', label: '搜索', icon: Search },
  { to: '/about', label: '关于', icon: Info },
]

const navItems = ref<PublicNavItem[]>(fallbackNavItems)

onMounted(() => {
  mountFrontstageScene()
  requestScrollProgressUpdate()
  window.addEventListener('scroll', requestScrollProgressUpdate, { passive: true })
  window.addEventListener('resize', requestScrollProgressUpdate, { passive: true })
})

onMounted(async () => {
  try {
    const config = await fetchSiteConfig()
    const configuredItems = readConfiguredNavigation(config['site.navigationItems'])
    if (configuredItems.length > 0) {
      navItems.value = configuredItems
    }
  } catch {
    navItems.value = fallbackNavItems
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('pointermove', handlePointerMove)
  window.removeEventListener('scroll', requestScrollProgressUpdate)
  window.removeEventListener('resize', requestScrollProgressUpdate)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  if (progressRaf) {
    cancelAnimationFrame(progressRaf)
  }
  disposeScene?.()
  disposeScene = null
  setScenePaused = null
  setScenePointer = null
})

watch(
  () => route.fullPath,
  async () => {
    navOpen.value = false
    await nextTick()
    requestScrollProgressUpdate()
  },
)

function requestScrollProgressUpdate() {
  if (progressRaf) {
    return
  }
  progressRaf = window.requestAnimationFrame(() => {
    progressRaf = 0
    const doc = document.documentElement
    const max = Math.max(0, doc.scrollHeight - window.innerHeight)
    scrollProgress.value = max > 0 ? Math.min(Math.max(window.scrollY / max, 0), 1) : 0
  })
}

async function mountFrontstageScene() {
  const host = sceneHost.value
  if (!host || prefersReducedMotion() || !hasWebGL()) {
    return
  }
  try {
    const { createFrontstageScene } = await import('@/shared/frontstageScene')
    if (!sceneHost.value) {
      return
    }
    const scene = createFrontstageScene(sceneHost.value)
    disposeScene = scene.dispose
    setScenePaused = scene.setPaused
    setScenePointer = scene.setPointer
    window.addEventListener('pointermove', handlePointerMove, { passive: true })
    document.addEventListener('visibilitychange', handleVisibilityChange)
  } catch {
    disposeScene?.()
    disposeScene = null
  }
}

function handlePointerMove(event: PointerEvent) {
  const nx = (event.clientX / window.innerWidth) * 2 - 1
  const ny = -((event.clientY / window.innerHeight) * 2 - 1)
  setScenePointer?.(nx, ny)
}

function handleVisibilityChange() {
  setScenePaused?.(document.visibilityState !== 'visible')
}

function hasWebGL(): boolean {
  const canvas = document.createElement('canvas')
  return Boolean(canvas.getContext('webgl2') || canvas.getContext('webgl'))
}

function readConfiguredNavigation(value: unknown): PublicNavItem[] {
  if (!Array.isArray(value)) {
    return []
  }
  return value
    .map((item) => readNavigationItem(item))
    .filter((item): item is PublicNavItem => item !== null)
}

function readNavigationItem(value: unknown): PublicNavItem | null {
  if (!isRecord(value)) {
    return null
  }
  const label = typeof value.label === 'string' ? value.label.trim() : ''
  const path = typeof value.path === 'string' ? value.path.trim() : ''
  const iconName = typeof value.icon === 'string' ? value.icon.trim().toLowerCase() : ''
  if (!label || !path || path.startsWith('//')) {
    return null
  }
  return {
    to: path,
    label,
    icon: iconMap[iconName] ?? Info,
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value)
}
</script>
