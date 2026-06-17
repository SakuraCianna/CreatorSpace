<template>
  <div class="admin-shell">
    <aside class="admin-rail" aria-label="后台导航">
      <RouterLink class="admin-brand" to="/admin">
        <span class="brand-mark">CS</span>
        <span>CreatorSpace CMS</span>
      </RouterLink>
      <nav class="admin-nav">
        <RouterLink v-for="item in navItems" :key="item.to" :to="item.to">
          {{ item.label }}
        </RouterLink>
      </nav>
    </aside>
    <section class="admin-workspace">
      <header class="admin-topbar">
        <div>
          <p class="eyebrow">Material 3 CMS</p>
          <h1>内容运营工作台</h1>
        </div>
        <RouterLink class="text-link" to="/">返回前台</RouterLink>
      </header>
      <main class="admin-main">
        <slot />
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'

const navItems = [
  { to: '/admin', label: '概览' },
  { to: '/admin/articles', label: '文章' },
  { to: '/admin/projects', label: '作品' },
  { to: '/admin/comments', label: '评论' },
  { to: '/admin/files', label: '文件' },
  { to: '/admin/content-rules', label: '规则' },
  { to: '/admin/settings', label: '设置' },
]
</script>
