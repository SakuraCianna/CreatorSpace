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
      <RouterLink class="brand" to="/" :aria-label="`返回 ${siteName} 首页`">
        <span class="brand-mark">
          <img src="/public.svg" alt="" aria-hidden="true" />
        </span>
        <span class="brand-copy">
          <strong>{{ siteName }}</strong>
          <small>{{ siteSlogan }}</small>
        </span>
      </RouterLink>
      <button class="icon-button nav-toggle" type="button" :aria-expanded="navOpen" @click="navOpen = !navOpen">
        <Menu v-if="!navOpen" :size="19" />
        <X v-else :size="19" />
        <span class="sr-only">切换导航</span>
      </button>
      <nav class="public-nav" :class="{ 'is-open': navOpen }" aria-label="前台导航">
        <template v-for="item in navItems" :key="item.to">
          <a
            v-if="item.external"
            :href="item.to"
            target="_blank"
            rel="noreferrer"
            @click="navOpen = false"
          >
            <component :is="item.icon" :size="16" />
            <span>{{ item.label }}</span>
          </a>
          <RouterLink v-else :to="item.to" @click="navOpen = false">
            <component :is="item.icon" :size="16" />
            <span>{{ item.label }}</span>
          </RouterLink>
        </template>
        <RouterLink
          v-if="!session.isAuthenticated"
          class="mobile-auth-action mobile-auth-action--tonal"
          to="/login"
          @click="navOpen = false"
        >
          登录
        </RouterLink>
        <RouterLink
          v-if="!session.isAuthenticated"
          class="mobile-auth-action mobile-auth-action--filled"
          to="/register"
          @click="navOpen = false"
        >
          注册
        </RouterLink>
        <RouterLink
          v-if="session.isAuthenticated"
          class="mobile-auth-action mobile-auth-action--tonal"
          :to="accountActionRoute"
          @click="navOpen = false"
        >
          {{ accountActionLabel }}
        </RouterLink>
        <button
          v-if="session.isAuthenticated"
          class="mobile-auth-action mobile-auth-action--filled"
          type="button"
          @click="handleLogout"
        >
          退出
        </button>
      </nav>
      <div class="public-actions">
        <template v-if="session.isAuthenticated">
          <RouterLink class="button button-tonal button-compact" :to="accountActionRoute">
            <ShieldCheck v-if="session.isAdmin" :size="16" />
            {{ accountActionLabel }}
          </RouterLink>
          <button class="button button-filled button-compact" type="button" @click="handleLogout">退出</button>
        </template>
        <template v-else>
          <RouterLink class="button button-tonal button-compact" to="/login">登录</RouterLink>
          <RouterLink class="button button-filled button-compact" to="/register">注册</RouterLink>
        </template>
      </div>
    </header>
    <main class="public-main">
      <slot />
    </main>
    <ThemeHUD />
  </div>
</template>
<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch, type Component } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { BookOpen, Home, Images, Info, Lightbulb, Menu, Palette, PenLine, Search, ShieldCheck, X } from '@lucide/vue'
import ThemeHUD from '@/shared/components/ThemeHUD.vue'
import { fetchSiteConfig } from '@/services/content'
import { prefersReducedMotion } from '@/shared/composables/useReducedMotion'
import { useSessionStore } from '@/shared/sessionStore'
import { syncSiteIdentityFromConfig, useSiteIdentity } from '@/shared/siteIdentity'
// 声明前台公共布局的状态变量
const navOpen = ref(false)
const sceneHost = ref<HTMLElement | null>(null)
const scrollProgress = ref(0)
const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const currentUsername = computed(() => session.currentUser?.username ?? '创作中心')
const accountActionRoute = computed(() => (session.isAdmin ? '/admin' : '/creator'))
const accountActionLabel = computed(() => (session.isAdmin ? '返回后台' : currentUsername.value))
let disposeScene: (() => void) | null = null
let setScenePaused: ((paused: boolean) => void) | null = null
let setScenePointer: ((nx: number, ny: number) => void) | null = null
let progressRaf = 0
interface PublicNavItem {
  to: string
  label: string
  icon: Component
  external: boolean
}
const iconMap: Record<string, Component> = {
  'book-open': BookOpen,
  home: Home,
  images: Images,
  info: Info,
  lightbulb: Lightbulb,
  palette: Palette,
  'pen-line': PenLine,
  pen: PenLine,
  search: Search,
}
const { siteName, siteSlogan } = useSiteIdentity({ load: false })
const navItems = ref<PublicNavItem[]>(withRequiredPublicEntries([]))
// 页面加载时初始化氛围背景和滚动进度计算
// 页面加载时初始化 WebGL 氛围背景粒子和滚动进度计算监听器
onMounted(() => {
  mountFrontstageScene()
  requestScrollProgressUpdate()
  window.addEventListener('scroll', requestScrollProgressUpdate, { passive: true })
  window.addEventListener('resize', requestScrollProgressUpdate, { passive: true })
})
onMounted(async () => {
  try {
    const config = await fetchSiteConfig()
    syncSiteIdentityFromConfig(config)
    const configuredItems = readConfiguredNavigation(config['site.navigationItems'])
    if (configuredItems.length > 0) {
      navItems.value = withRequiredPublicEntries(configuredItems)
    }
  } catch {
    navItems.value = withRequiredPublicEntries([])
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
// 使用 rAF 节流机制实时计算当前页面垂直滚动的进度比例, 渲染顶端阅读进度指示条
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
// 动态按需加载 WebGL 氛围粒子层场景
// 动态按需加载 WebGL 氛围粒子层场景, 并注册鼠标移动和页面可见性切换的监听器
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
// 将鼠标在视口中的绝对坐标映射为 -1 到 1 的标准化 3D 空间坐标以投递给粒子系统
function handlePointerMove(event: PointerEvent) {
  const nx = (event.clientX / window.innerWidth) * 2 - 1
  const ny = -((event.clientY / window.innerHeight) * 2 - 1)
  setScenePointer?.(nx, ny)
}
// 当标签页切到后台或标签隐藏时暂停 WebGL 渲染循环, 以节省显卡功耗
function handleVisibilityChange() {
  setScenePaused?.(document.visibilityState !== 'visible')
}
// 创建临时 canvas 上下文校验当前运行浏览器是否支持 WebGL 渲染
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
function readString(value: unknown): string {
  return typeof value === 'string' ? value.trim() : ''
}
function readNavigationItem(value: unknown): PublicNavItem | null {
  if (!isRecord(value)) {
    return null
  }
  const label = typeof value.label === 'string' ? value.label.trim() : ''
  const path = typeof value.path === 'string' ? value.path.trim() : ''
  const iconName = typeof value.icon === 'string' ? value.icon.trim().toLowerCase() : ''
  if (!label || !path || path.startsWith('// ')) {
    return null
  }
  return {
    to: path,
    label,
    icon: iconMap[iconName] ?? Info,
    external: isExternalUrl(path),
  }
}
function withRequiredPublicEntries(items: PublicNavItem[]): PublicNavItem[] {
  const nextItems = items.length > 0 ? [...items] : [
    { to: '/articles', label: '文章', icon: BookOpen, external: false },
    { to: '/projects', label: '作品', icon: Images, external: false },
    { to: '/inspirations', label: '灵感', icon: Lightbulb, external: false },
    { to: '/search', label: '搜索', icon: Search, external: false },
    { to: '/about', label: '关于', icon: Info, external: false },
  ]
  if (!nextItems.some((item) => item.to === '/themes')) {
    const aboutIndex = nextItems.findIndex((item) => item.to === '/about')
    const insertAt = aboutIndex >= 0 ? aboutIndex : nextItems.length
    nextItems.splice(insertAt, 0, { to: '/themes', label: '主题', icon: Palette, external: false })
  }
  if (!nextItems.some((item) => item.to === '/creator' || item.to.startsWith('/creator/'))) {
    const searchIndex = nextItems.findIndex((item) => item.to === '/search')
    const insertAt = searchIndex >= 0 ? searchIndex : nextItems.length
    nextItems.splice(insertAt, 0, { to: '/creator', label: '创作', icon: PenLine, external: false })
  }
  return nextItems
}
function isExternalUrl(value: string) {
  return /^https?:\/\//i.test(value)
}
function handleLogout() {
  session.logout()
  navOpen.value = false
  router.push('/')
}
function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value)
}
</script>
<style scoped>
.public-shell {
  position: relative;
  min-height: 100vh;
  overflow: clip;
}
.public-shell::before {
  content: "";
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(20, 21, 29, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(20, 21, 29, 0.028) 1px, transparent 1px);
  background-size: 52px 52px;
  mask-image: linear-gradient(to bottom, rgba(0, 0, 0, 0.82), rgba(0, 0, 0, 0.08));
}
.frontstage-webgl {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  opacity: 0.6;
  mix-blend-mode: screen;
}
.public-header {
  position: sticky;
  top: 0;
  z-index: 30;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  width: 100%;
  min-height: 72px;
  margin: 0;
  padding: 0 clamp(18px, 5vw, 72px);
  gap: 24px;
  border-bottom: 1px solid rgba(116, 119, 127, 0.18);
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(22px);
  box-shadow: 0 10px 34px rgba(30, 38, 64, 0.08);
}
.brand {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  font-weight: 760;
  color: var(--tone-ink);
}
.brand-copy {
  display: grid;
  gap: 2px;
}
.brand-copy strong {
  color: #1a2233;
}
.brand-copy small {
  color: #566174;
  font-size: 11px;
  font-weight: 720;
}
.brand-mark {
  display: inline-grid;
  width: 40px;
  height: 40px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 12px;
  border: 1px solid color-mix(in srgb, var(--md-sys-color-primary) 18%, transparent);
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.96), rgba(235, 242, 255, 0.78)),
    var(--md-sys-color-surface-container-lowest);
  box-shadow: var(--md-sys-elevation-2);
}
.brand-mark img {
  width: 72%;
  height: 72%;
  object-fit: contain;
}
.public-nav {
  display: flex;
  align-items: center;
  gap: 6px;
}
.public-nav {
  justify-content: center;
}
.public-nav a {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 40px;
  padding: 9px 12px;
  border-radius: 999px;
  color: #3d4658;
  font-size: 14px;
  font-weight: 730;
  transition: background 180ms ease, color 180ms ease, transform 180ms ease;
}
.public-nav a.router-link-active,
.public-nav a:hover {
  background: color-mix(in srgb, var(--md-sys-color-primary) 13%, #ffffff);
  color: #174ea6;
}
.public-nav a:focus-visible {
  outline: 2px solid var(--md-sys-color-primary);
  outline-offset: 3px;
}
.public-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.mobile-auth-action {
  display: none !important;
}
button.mobile-auth-action {
  font: inherit;
  cursor: pointer;
}
.nav-toggle {
  display: none;
  justify-self: end;
}
.public-main {
  position: relative;
  z-index: 1;
  width: min(1200px, calc(100vw - 40px));
  margin: 0 auto;
}
@media (max-width: 1040px) {
  .public-header {
    grid-template-columns: auto auto;
  }
  .nav-toggle {
    display: inline-flex;
  }
  .public-nav,
  .public-actions {
    display: none;
  }
  .public-nav.is-open {
    grid-column: 1 / -1;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    justify-content: stretch;
    padding-bottom: 12px;
  }
  .public-nav.is-open a {
    justify-content: center;
    border: 1px solid var(--tone-line);
    background: rgba(255, 255, 255, 0.72);
  }
  .public-nav.is-open .mobile-auth-action {
    display: inline-flex !important;
    min-height: 42px;
    border-radius: 999px;
    font-weight: 780;
  }
  .public-nav.is-open .mobile-auth-action--tonal {
    background: var(--md-sys-color-primary-container);
    color: var(--md-sys-color-on-primary-container);
  }
  .public-nav.is-open .mobile-auth-action--filled {
    background: var(--md-sys-color-primary);
    color: var(--md-sys-color-on-primary);
  }
}
@media (max-width: 760px) {
  .public-nav.is-open {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 520px) {
  .public-header,
  .public-main {
    width: min(100vw - 24px, 1200px);
  }
  .brand-copy small {
    display: none;
  }
}
</style>
