import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/public/HomeView.vue'),
      meta: { layout: 'immersive' },
    },
    {
      path: '/articles',
      name: 'articles',
      component: () => import('@/views/public/ArticlesView.vue'),
      meta: { layout: 'public' },
    },
    {
      path: '/projects',
      name: 'projects',
      component: () => import('@/views/public/ProjectsView.vue'),
      meta: { layout: 'public' },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/auth/RegisterView.vue'),
      meta: { layout: 'public' },
    },
    {
      path: '/admin',
      name: 'admin-dashboard',
      component: () => import('@/views/admin/AdminDashboardView.vue'),
      meta: { layout: 'admin' },
    },
    {
      path: '/admin/content-rules',
      name: 'admin-content-rules',
      component: () => import('@/views/admin/ContentRulesView.vue'),
      meta: { layout: 'admin' },
    },
    {
      path: '/admin/:section',
      name: 'admin-placeholder',
      component: () => import('@/views/admin/AdminPlaceholderView.vue'),
      meta: { layout: 'admin' },
    },
  ],
})

export default router
