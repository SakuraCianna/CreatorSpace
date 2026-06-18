<template>
  <div class="public-shell">
    <header class="public-header">
      <RouterLink class="brand" to="/" aria-label="返回 CreatorSpace 首页">
        <span class="brand-mark">CS</span>
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
import { onMounted, ref, type Component } from 'vue'
import { RouterLink } from 'vue-router'
import { BookOpen, Home, Images, Info, Lightbulb, Menu, Search, X } from '@lucide/vue'

import { fetchSiteConfig } from '@/services/content'

const navOpen = ref(false)

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
