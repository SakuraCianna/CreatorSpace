<template>
  <div class="cs-shell">
    <nav class="cs-nav" aria-label="主导航">
      <RouterLink to="/" class="cs-nav__brand">
        <span class="cs-nav__mark">CS</span>
        <span>{{ siteConfig.brand }}</span>
      </RouterLink>
      <div class="cs-nav__links">
        <RouterLink v-for="item in siteConfig.navigation" :key="item.to" :to="item.to">
          {{ item.label }}
        </RouterLink>
      </div>
    </nav>
    <main>
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'

import { siteConfig } from '@/data/home-content'
</script>
