<template>
  <div class="admin-shell">
    <aside class="admin-rail" aria-label="后台导航">
      <RouterLink class="admin-brand" to="/admin">
        <span class="brand-mark">
          <img src="/public.svg" alt="" aria-hidden="true" />
        </span>
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

<style scoped>
.admin-brand {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  font-weight: 760;
}

.admin-brand span:last-child {
  display: grid;
  gap: 2px;
}

.admin-brand small {
  color: var(--tone-muted);
  font-size: 11px;
  font-weight: 650;
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

.admin-nav {
  display: flex;
  align-items: stretch;
  flex-direction: column;
  gap: 6px;
}

.admin-nav a {
  display: grid;
  grid-template-columns: 20px minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  width: 100%;
  min-height: 40px;
  padding: 9px 12px;
  border-radius: 999px;
  color: var(--tone-muted);
  font-size: 14px;
  font-weight: 690;
  transition: background 180ms ease, color 180ms ease, transform 180ms ease;
}

.admin-nav a.router-link-active,
.admin-nav a:hover {
  background: color-mix(in srgb, var(--md-sys-color-primary) 12%, transparent);
  color: var(--md-sys-color-primary);
}

.admin-topbar__actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.admin-shell {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  min-height: 100vh;
  background:
    linear-gradient(120deg, rgba(49, 91, 255, 0.08), transparent 36%),
    #f7f4f0;
}

.admin-rail {
  display: flex;
  flex-direction: column;
  gap: 22px;
  padding: 22px;
  border-right: 1px solid var(--tone-line);
  background: rgba(255, 255, 255, 0.82);
}

.admin-nav {
  align-items: stretch;
  flex-direction: column;
}

.admin-nav a {
  display: grid;
  grid-template-columns: 20px minmax(0, 1fr) auto;
  width: 100%;
  border-radius: 999px;
}

.admin-nav small {
  color: var(--tone-faint);
  font-size: 10px;
  font-weight: 800;
}

.admin-rail__note {
  display: grid;
  gap: 10px;
  margin-top: auto;
  padding: 16px;
  border-radius: 8px;
  background: var(--tone-night);
  color: #fff;
}

.admin-rail__note p {
  margin: 0;
  color: rgba(255, 255, 255, 0.78);
  font-size: 13px;
  line-height: 1.6;
}

.admin-workspace {
  min-width: 0;
}

.admin-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  min-height: 98px;
  padding: 22px 32px;
  border-bottom: 1px solid var(--tone-line);
  background: rgba(255, 251, 254, 0.82);
  backdrop-filter: blur(18px);
}

.admin-topbar h1 {
  margin: 0;
  font-size: 30px;
  line-height: 1.2;
}

.admin-main {
  padding: 28px 32px 48px;
}

@media (max-width: 1020px) {
  .admin-shell {
    grid-template-columns: 1fr;
  }

  .admin-rail {
    border-right: 0;
    border-bottom: 1px solid var(--tone-line);
  }

  .admin-nav {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .admin-nav {
    grid-template-columns: 1fr;
  }

  .admin-topbar,
  .admin-topbar__actions {
    align-items: flex-start;
    flex-direction: column;
  }

  .admin-main,
  .admin-topbar {
    padding-right: 20px;
    padding-left: 20px;
  }
}
</style>
