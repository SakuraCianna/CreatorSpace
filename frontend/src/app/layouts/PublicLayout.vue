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
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { BookOpen, Images, Info, Lightbulb, Menu, Search, X } from '@lucide/vue'

const navOpen = ref(false)

const navItems = [
  { to: '/articles', label: '文章', icon: BookOpen },
  { to: '/projects', label: '作品', icon: Images },
  { to: '/inspirations', label: '灵感', icon: Lightbulb },
  { to: '/search', label: '搜索', icon: Search },
  { to: '/about', label: '关于', icon: Info },
]
</script>
