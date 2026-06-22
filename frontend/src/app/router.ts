import { createRouter, createWebHistory } from 'vue-router'

import { ACCESS_TOKEN_KEY } from '@/services/http'
import { USER_SUMMARY_KEY } from '@/shared/sessionStore'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/pages/HomePage.vue'),
      meta: { layout: 'immersive' },
    },
    {
      path: '/articles',
      name: 'articles',
      component: () => import('@/pages/ArticlesPage.vue'),
      meta: { layout: 'public' },
    },
    {
      path: '/articles/:slug',
      name: 'article-detail',
      component: () => import('@/pages/ArticleDetailPage.vue'),
      meta: { layout: 'public' },
    },
    {
      path: '/projects',
      name: 'projects',
      component: () => import('@/pages/ProjectsPage.vue'),
      meta: { layout: 'public' },
    },
    {
      path: '/projects/:slug',
      name: 'project-detail',
      component: () => import('@/pages/ProjectDetailPage.vue'),
      meta: { layout: 'public' },
    },
    {
      path: '/inspirations',
      name: 'inspirations',
      component: () => import('@/pages/InspirationsPage.vue'),
      meta: { layout: 'public' },
    },
    {
      path: '/search',
      name: 'search',
      component: () => import('@/pages/SearchPage.vue'),
      meta: { layout: 'public' },
    },
    {
      path: '/about',
      name: 'about',
      component: () => import('@/pages/AboutPage.vue'),
      meta: { layout: 'public' },
    },
    {
      path: '/guestbook',
      name: 'guestbook',
      component: () => import('@/pages/GuestbookPage.vue'),
      meta: { layout: 'public' },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/pages/RegisterPage.vue'),
      meta: { layout: 'public' },
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/pages/LoginPage.vue'),
      meta: { layout: 'public' },
    },
    {
      path: '/creator',
      redirect: '/creator/articles',
    },
    {
      path: '/creator/:section',
      name: 'creator-center',
      component: () => import('@/pages/CreatorCenterPage.vue'),
      meta: { layout: 'public', requiresAuth: true },
    },
    {
      path: '/admin',
      name: 'admin-dashboard',
      component: () => import('@/pages/AdminDashboardPage.vue'),
      meta: { layout: 'admin', requiresAdmin: true },
    },
    {
      path: '/admin/content-rules',
      name: 'admin-content-rules',
      component: () => import('@/pages/ContentRulesPage.vue'),
      meta: { layout: 'admin', requiresAdmin: true },
    },
    {
      path: '/admin/:section',
      name: 'admin-placeholder',
      component: () => import('@/pages/AdminPlaceholderPage.vue'),
      meta: { layout: 'admin', requiresAdmin: true },
    },
  ],
})

router.beforeEach((to) => {
  const requiresAdmin = to.matched.some((record) => record.meta.requiresAdmin)
  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth)
  const hasToken = Boolean(window.localStorage.getItem(ACCESS_TOKEN_KEY))
  const roles = readStoredRoles()

  if (hasToken && roles.length === 0) {
    window.localStorage.removeItem(ACCESS_TOKEN_KEY)
    window.localStorage.removeItem(USER_SUMMARY_KEY)
    if (requiresAdmin || requiresAuth) {
      return requiresAdmin
        ? { name: 'login', query: { redirect: to.fullPath, mode: 'admin' } }
        : { name: 'login', query: { redirect: to.fullPath } }
    }
    return true
  }

  if ((requiresAdmin || requiresAuth) && !hasToken) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (requiresAdmin && !roles.includes('ADMIN')) {
    return { name: 'login', query: { redirect: to.fullPath, mode: 'admin' } }
  }

  if (to.name === 'login' && hasToken) {
    const redirect = readRedirect(to.query.redirect)
    if (redirect.startsWith('/admin') && !roles.includes('ADMIN')) {
      return true
    }
    if (redirect && !redirect.startsWith('/admin')) {
      return redirect
    }
    if (!roles.includes('ADMIN')) {
      return { name: 'articles' }
    }
    return { name: 'admin-dashboard' }
  }

  return true
})

function readStoredRoles(): string[] {
  try {
    const raw = window.localStorage.getItem(USER_SUMMARY_KEY)
    if (!raw) {
      return []
    }
    const parsed = JSON.parse(raw) as { roles?: unknown }
    return Array.isArray(parsed.roles)
      ? parsed.roles.filter((role): role is string => role === 'ADMIN' || role === 'USER')
      : []
  } catch {
    return []
  }
}

function readRedirect(value: unknown): string {
  if (Array.isArray(value)) {
    return typeof value[0] === 'string' ? value[0] : ''
  }
  return typeof value === 'string' ? value : ''
}

export default router
