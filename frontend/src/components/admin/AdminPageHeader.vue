<template>
  <header class="admin-page-hero" :class="themeClass" data-reveal>
    <div class="hero-content">
      <div class="hero-text">
        <h2>{{ title }}</h2>
        <p>{{ description }}</p>
      </div>
      <div class="hero-actions" v-if="$slots.default">
        <slot></slot>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  title: string
  description: string
  theme?: string
}>()

const themeClass = computed(() => props.theme ? `hero-theme-${props.theme}` : 'hero-theme-default')
</script>

<style scoped>
.admin-page-hero {
  display: flex;
  align-items: center;
  min-height: auto;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  position: relative;
}

.hero-content {
  position: relative;
  z-index: 2;
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  gap: 16px;
}

.hero-text h2 {
  max-width: 830px;
  margin: 0;
  color: var(--admin-primary-strong);
  font-size: 24px;
  font-weight: 780;
  line-height: 1.16;
}

.hero-text p {
  max-width: 680px;
  margin: 6px 0 0;
  color: var(--admin-muted);
  font-size: 14px;
  line-height: 1.5;
}

.hero-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

@media (min-width: 761px) {
  .hero-text h2 {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

@media (max-width: 760px) {
  .admin-page-hero {
    align-items: flex-start;
    padding: 0;
    min-height: auto;
  }
  .hero-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  .hero-text h2 { font-size: 20px; }
  .hero-actions { width: 100%; }
}
</style>
