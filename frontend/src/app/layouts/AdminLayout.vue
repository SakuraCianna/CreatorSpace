<template>
  <div class="admin-shell">
    <aside class="admin-rail" aria-label="后台导航">
      <RouterLink class="admin-brand" to="/admin">
        <span class="brand-mark">CS</span>
        <span>
          <strong>CreatorSpace CMS</strong>
          <small>Content Operating Desk</small>
        </span>
      </RouterLink>

      <nav class="admin-nav">
        <RouterLink v-for="item in navItems" :key="item.to" :to="item.to">
          <component :is="item.icon" :size="18" />
          <span>{{ item.label }}</span>
          <small>{{ item.badge }}</small>
        </RouterLink>
      </nav>

      <div class="admin-rail__note">
        <Sparkles :size="18" />
        <p>主题、内容、灵感和互动都在同一个工作台维护。</p>
      </div>
    </aside>

    <section class="admin-workspace">
      <header class="admin-topbar">
        <div>
          <p class="eyebrow">Material 3 CMS</p>
          <h1>{{ currentTitle }}</h1>
        </div>
        <div class="admin-topbar__actions">
          <RouterLink class="button button-tonal button-compact" to="/">
            <Home :size="16" />
            返回前台
          </RouterLink>
          <button class="button button-filled button-compact" type="button" @click="logout">
            <LogOut :size="16" />
            退出
          </button>
        </div>
      </header>
      <main class="admin-main">
        <slot />
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  BarChart3,
  FileImage,
  FileText,
  Home,
  Images,
  Lightbulb,
  LogOut,
  MessageSquare,
  Palette,
  Settings,
  ShieldCheck,
  Sparkles,
} from '@lucide/vue'

import { useSessionStore } from '@/shared/sessionStore'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()

const navItems = [
  { to: '/admin', label: '概览', badge: 'LIVE', icon: BarChart3 },
  { to: '/admin/articles', label: '文章', badge: 'Draft', icon: FileText },
  { to: '/admin/projects', label: '作品', badge: 'Gallery', icon: Images },
  { to: '/admin/inspirations', label: '灵感', badge: 'Wall', icon: Lightbulb },
  { to: '/admin/comments', label: '评论', badge: 'Review', icon: MessageSquare },
  { to: '/admin/files', label: '文件', badge: 'Local', icon: FileImage },
  { to: '/admin/themes', label: '主题', badge: 'M3', icon: Palette },
  { to: '/admin/content-rules', label: '规则', badge: 'Policy', icon: ShieldCheck },
  { to: '/admin/settings', label: '设置', badge: 'Site', icon: Settings },
]

const titleMap = new Map(navItems.map((item) => [item.to, item.label]))
const currentTitle = computed(() => {
  if (route.path === '/admin') {
    return '内容运营工作台'
  }
  return `${titleMap.get(route.path) ?? '模块'}管理`
})

function logout() {
  session.logout()
  router.push('/login')
}
</script>
