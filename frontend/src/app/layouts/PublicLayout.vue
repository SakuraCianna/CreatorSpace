<template>
  <div class="public-shell">
    <header class="public-header">
      <RouterLink class="brand" to="/">
        <span class="brand-mark">CS</span>
        <span>CreatorSpace</span>
      </RouterLink>
      <nav class="public-nav" aria-label="前台导航">
        <RouterLink to="/articles">文章</RouterLink>
        <RouterLink to="/projects">作品</RouterLink>
        <RouterLink to="/register">注册</RouterLink>
        <RouterLink to="/admin">后台</RouterLink>
      </nav>
    </header>
    <main class="public-main">
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>
