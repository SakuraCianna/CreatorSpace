import { createRouter, createWebHistory } from 'vue-router'

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
      path: '/projects',
      name: 'projects',
      component: () => import('@/pages/ProjectsPage.vue'),
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
      meta: { layout: 'admin' },
    },
    {
      path: '/admin/content-rules',
      name: 'admin-content-rules',
      component: () => import('@/pages/ContentRulesPage.vue'),
      meta: { layout: 'admin' },
    },
    {
      path: '/admin/:section',
      name: 'admin-placeholder',
      component: () => import('@/pages/AdminPlaceholderPage.vue'),
      meta: { layout: 'admin' },
    },
  ],
})

export default router
