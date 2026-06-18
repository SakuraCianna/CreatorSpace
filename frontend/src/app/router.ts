import { createRouter, createWebHistory } from 'vue-router'

import { ACCESS_TOKEN_KEY } from '@/services/http'

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
  const hasToken = Boolean(window.localStorage.getItem(ACCESS_TOKEN_KEY))

  if (requiresAdmin && !hasToken) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.name === 'login' && hasToken) {
    return { name: 'admin-dashboard' }
  }

  return true
})

export default router
