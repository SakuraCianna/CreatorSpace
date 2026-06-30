<template>
<!-- 后台管理中心布局容器 -->
<!-- 后台布局侧边栏与主工作区容器 -->
  <div class="admin-shell" :class="{ 'admin-shell--collapsed': sidebarCollapsed }">
    <!-- 侧边栏导航铁轨区域 -->
    <aside class="admin-rail" aria-label="后台导航">
      <button
        class="admin-brand"
        type="button"
        :aria-label="sidebarCollapsed ? '展开后台侧边栏' : '收起后台侧边栏'"
        :aria-expanded="!sidebarCollapsed"
        @click="toggleSidebar"
      >
        <span class="brand-mark">
          <img src="/public.svg" alt="" aria-hidden="true" />
        </span>
        <span class="brand-copy">
          <strong>创作空间后台</strong>
          <small>{{ siteSlogan }}</small>
        </span>
      </button>

      <nav class="admin-nav">
        <RouterLink v-for="item in navItems" :key="item.to" :to="item.to" :title="item.label" :class="{ 'has-badge': item.badgeCount && item.badgeCount.value > 0 }">
          <component :is="item.icon" :size="18" />
          <span>{{ item.label }}</span>
          <span v-if="item.badgeCount && item.badgeCount.value > 0" class="nav-badge">{{ item.badgeCount.value }}</span>
        </RouterLink>
      </nav>

      <div class="admin-rail__note">
        <Sparkles :size="18" />
        <p>主题、内容、灵感和互动都在同一个工作台维护。</p>
      </div>
    </aside>

    <!-- 主工作区内容展示面板 -->
    <section class="admin-workspace">
      <header class="admin-topbar">
        <div>
          <h1>{{ currentTitle }}</h1>
        </div>
        <div class="admin-topbar__actions">
          <RouterLink class="button button-tonal button-compact" to="/articles">
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
import { fetchPendingReview } from '@/services/content'
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  Ban,
  BarChart3,
  FileImage,
  FileText,
  Home,
  Images,
  Lightbulb,
  LogOut,
  MessageSquare,
  Palette,
  ScrollText,
  Settings,
  ShieldCheck,
  Sparkles,
  Tags,
} from '@lucide/vue'

import { useSessionStore } from '@/shared/sessionStore'
import { useSiteIdentity } from '@/shared/siteIdentity'

// 初始化后台导航状态和路由相关参数
const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const { siteSlogan } = useSiteIdentity()
const sidebarCollapsed = ref(false)
const pendingCounts = ref({ pendingComments: 0, pendingGuestbook: 0, pendingArticles: 0, pendingProjects: 0, total: 0 })

// 后台菜单配置项
// 后台侧边栏菜单项及图标配置映射表
const navItems = [
  { to: '/admin', label: '概览', icon: BarChart3 },
  { to: '/admin/articles', label: '文章', icon: FileText, badgeCount: computed(() => pendingCounts.value.pendingArticles) },
  { to: '/admin/projects', label: '作品', icon: Images, badgeCount: computed(() => pendingCounts.value.pendingProjects) },
  { to: '/admin/categories', label: '分类', icon: Tags },
  { to: '/admin/tags', label: '标签', icon: Tags },
  { to: '/admin/inspirations', label: '灵感', icon: Lightbulb },
  { to: '/admin/comments', label: '评论', icon: MessageSquare, badgeCount: computed(() => pendingCounts.value.pendingComments) },
  { to: '/admin/guestbook', label: '留言', icon: MessageSquare, badgeCount: computed(() => pendingCounts.value.pendingGuestbook) },
  { to: '/admin/files', label: '文件', icon: FileImage },
  { to: '/admin/themes', label: '主题', icon: Palette },
  { to: '/admin/content-rules', label: '规则', icon: ShieldCheck },
  { to: '/admin/sensitive-words', label: '敏感词', icon: Ban },
  { to: '/admin/operation-logs', label: '日志', icon: ScrollText },
  { to: '/admin/ai-assistant', label: 'AI 助手', icon: Sparkles },
  { to: '/admin/settings', label: '设置', icon: Settings },
] as const

const titleMap = new Map(navItems.map((item) => [item.to, item.label]))
// 根据当前激活的路由路径映射解析出后台顶栏显示的主页签管理标题
const currentTitle = computed(() => {
  if (route.path === '/admin') {
    return '内容运营工作台'
  }
  return `${titleMap.get(route.path) ?? '模块'}管理`
})

// 清除当前管理员的登录状态缓存和凭证令牌, 并重定向跳转回登录页
function logout() {
  session.logout()
  router.push('/login')
}

// 切换左侧导航铁轨侧边栏的展开收起状态
function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

onMounted(async () => {
  try {
    pendingCounts.value = await fetchPendingReview()
  } catch {
    // 静默忽略
  }
})
</script>

<style scoped>
.admin-brand {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  min-width: 0;
  border: 0;
  background: transparent;
  color: var(--admin-ink);
  font-weight: 760;
  text-align: left;
  cursor: pointer;
}

.brand-copy {
  display: grid;
  gap: 2px;
  min-width: 0;
  max-width: 168px;
  overflow: hidden;
  opacity: 1;
  transform: translateX(0);
  transition:
    max-width 240ms cubic-bezier(0.2, 0, 0, 1),
    opacity 180ms ease,
    transform 240ms cubic-bezier(0.2, 0, 0, 1);
}

.admin-brand small {
  color: var(--admin-muted);
  font-size: 11px;
  font-weight: 650;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
  transition:
    transform 240ms cubic-bezier(0.2, 0, 0, 1),
    box-shadow 180ms ease;
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
  min-height: 38px;
  padding: 8px 12px;
  border-radius: 999px;
  color: #344154;
  font-size: 14px;
  font-weight: 690;
  overflow: hidden;
  transition:
    grid-template-columns 240ms cubic-bezier(0.2, 0, 0, 1),
    background 180ms ease,
    color 180ms ease,
    padding 240ms cubic-bezier(0.2, 0, 0, 1),
    transform 180ms ease;
}

.admin-nav a span:not(.nav-badge) {
  min-width: 0;
  max-width: 88px;
  overflow: hidden;
  opacity: 1;
  text-overflow: ellipsis;
  white-space: nowrap;
  transform: translateX(0);
  transition:
    max-width 240ms cubic-bezier(0.2, 0, 0, 1),
    opacity 180ms ease,
    transform 240ms cubic-bezier(0.2, 0, 0, 1);
}

.admin-nav a svg {
  transition: transform 220ms cubic-bezier(0.2, 0, 0, 1);
}

.admin-nav a.router-link-active,
.admin-nav a:hover {
  background: var(--admin-primary-soft);
  color: var(--admin-primary-strong);
  box-shadow: inset 0 0 0 1px var(--admin-line);
}

.admin-nav a.has-badge {
  position: relative;
}

.nav-badge {
  justify-self: end;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  font-size: 10px;
  font-weight: 700;
  line-height: 16px;
  text-align: center;
  border-radius: 999px;
  background: #e0455a;
  color: #fff;
  pointer-events: none;
}

.admin-topbar__actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.admin-shell {
  --admin-bg: #f7f8fc;
  --admin-rail: rgba(255, 255, 255, 0.94);
  --admin-panel: #ffffff;
  --admin-panel-soft: #f7f9ff;
  --admin-ink: #111827;
  --admin-muted: #526079;
  --admin-line: rgba(49, 91, 255, 0.12);
  --admin-primary: #315bff;
  --admin-primary-strong: #174ea6;
  --admin-primary-soft: #e8efff;
  --admin-danger: #b91c1c;
  --admin-danger-soft: #fee2e2;
  --admin-shadow: 0 18px 44px rgba(31, 41, 55, 0.1);
  display: grid;
  grid-template-columns: 250px minmax(0, 1fr);
  min-height: 100vh;
  background:
    linear-gradient(120deg, rgba(49, 91, 255, 0.08), transparent 36%),
    var(--admin-bg);
  transition: grid-template-columns 280ms cubic-bezier(0.2, 0, 0, 1);
}

.admin-shell--collapsed {
  grid-template-columns: 78px minmax(0, 1fr);
}

.admin-rail {
  position: sticky;
  top: 0;
  align-self: start;
  display: flex;
  flex-direction: column;
  gap: 18px;
  height: 100vh;
  padding: 18px 16px;
  border-right: 1px solid var(--admin-line);
  background: var(--admin-rail);
  overflow-y: auto;
  transition:
    padding 280ms cubic-bezier(0.2, 0, 0, 1),
    background 180ms ease;
}

.admin-rail__note {
  display: grid;
  gap: 10px;
  margin-top: auto;
  max-height: 160px;
  padding: 16px;
  border-radius: 8px;
  background: var(--tone-night);
  color: #fff;
  overflow: hidden;
  opacity: 1;
  transform: translateY(0) scale(1);
  transition:
    max-height 240ms cubic-bezier(0.2, 0, 0, 1),
    padding 240ms cubic-bezier(0.2, 0, 0, 1),
    opacity 160ms ease,
    transform 220ms cubic-bezier(0.2, 0, 0, 1);
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

.admin-shell--collapsed .admin-rail {
  align-items: center;
  padding-right: 12px;
  padding-left: 12px;
}

.admin-shell--collapsed .admin-brand {
  justify-content: center;
}

.admin-shell--collapsed .brand-mark {
  transform: scale(0.94);
}

.admin-shell--collapsed .brand-copy,
.admin-shell--collapsed .admin-nav a span:not(.nav-badge) {
  max-width: 0;
  opacity: 0;
  pointer-events: none;
  transform: translateX(-8px);
}

.admin-shell--collapsed .admin-nav a .nav-badge {
  display: none;
}

.admin-shell--collapsed .admin-rail__note {
  max-height: 0;
  padding-top: 0;
  padding-bottom: 0;
  opacity: 0;
  pointer-events: none;
  transform: translateY(8px) scale(0.98);
}

.admin-shell--collapsed .admin-nav {
  width: 100%;
}

.admin-shell--collapsed .admin-nav a {
  grid-template-columns: 1fr;
  justify-items: center;
  padding-right: 0;
  padding-left: 0;
}

.admin-shell--collapsed .admin-nav a svg {
  transform: scale(1.04);
}

.admin-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  min-height: 82px;
  padding: 18px 28px;
  border-bottom: 1px solid var(--admin-line);
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(18px);
}

.admin-topbar h1 {
  margin: 0;
  color: var(--admin-ink);
  font-size: 26px;
  font-weight: 860;
  line-height: 1.2;
}

.admin-main {
  padding: 24px 28px 42px;
}

@media (max-width: 1020px) {
  .admin-shell {
    grid-template-columns: 1fr;
  }

  .admin-shell--collapsed {
    grid-template-columns: 1fr;
  }

  .admin-rail {
    position: static;
    height: auto;
    overflow: visible;
    border-right: 0;
    border-bottom: 1px solid var(--admin-line);
  }

  .admin-shell--collapsed .brand-copy {
    display: grid;
    max-width: 168px;
    opacity: 1;
    pointer-events: auto;
    transform: none;
  }

  .admin-shell--collapsed .admin-nav a span:not(.nav-badge) {
    display: inline;
    max-width: 88px;
    opacity: 1;
    pointer-events: auto;
    transform: none;
  }

  .admin-shell--collapsed .admin-nav a .nav-badge {
    display: inline;
  }

  .admin-shell--collapsed .admin-rail__note {
    max-height: 160px;
    padding: 16px;
    opacity: 1;
    pointer-events: auto;
    transform: none;
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

@media (prefers-reduced-motion: reduce) {
  .admin-shell,
  .admin-rail,
  .admin-brand,
  .brand-copy,
  .brand-mark,
  .admin-nav a,
  .admin-nav a span,
  .admin-nav a svg,
  .admin-rail__note {
    transition: none;
  }
}
</style>
