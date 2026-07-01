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
  min-height: 96px;
  padding: 20px 28px;
  border: 1px solid var(--admin-line);
  border-radius: var(--app-radius-sm);
  background-color: var(--admin-panel);
  box-shadow: var(--admin-shadow);
  overflow: hidden;
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
  color: var(--admin-ink);
  font-size: 24px;
  font-weight: 860;
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

/* Themes */
.hero-theme-default {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.88), rgba(255, 255, 255, 0.58)),
              radial-gradient(circle at 16% 20%, rgba(49, 91, 255, 0.14), transparent 34%);
}
.hero-theme-blue {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(255, 255, 255, 0.6)),
              radial-gradient(circle at 10% 20%, rgba(49, 91, 255, 0.18), transparent 40%),
              radial-gradient(circle at 90% 80%, rgba(0, 124, 114, 0.1), transparent 30%);
}
.hero-theme-emerald {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(255, 255, 255, 0.6)),
              radial-gradient(circle at 90% 20%, rgba(16, 185, 129, 0.18), transparent 40%),
              radial-gradient(circle at 20% 80%, rgba(49, 91, 255, 0.08), transparent 30%);
}
.hero-theme-purple {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(255, 255, 255, 0.6)),
              radial-gradient(circle at 50% -20%, rgba(139, 92, 246, 0.22), transparent 50%),
              radial-gradient(circle at 0% 100%, rgba(236, 72, 153, 0.1), transparent 40%);
}
.hero-theme-orange {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(255, 255, 255, 0.6)),
              radial-gradient(circle at 10% 50%, rgba(249, 115, 22, 0.15), transparent 40%),
              radial-gradient(circle at 90% 50%, rgba(234, 179, 8, 0.1), transparent 40%);
}
.hero-theme-cyan {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(255, 255, 255, 0.6)),
              radial-gradient(circle at 80% -10%, rgba(6, 182, 212, 0.18), transparent 40%),
              radial-gradient(circle at 20% 110%, rgba(59, 130, 246, 0.1), transparent 40%);
}
.hero-theme-rose {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(255, 255, 255, 0.6)),
              radial-gradient(circle at 20% 20%, rgba(244, 63, 94, 0.15), transparent 40%),
              radial-gradient(circle at 80% 80%, rgba(225, 29, 72, 0.08), transparent 40%);
}
.hero-theme-amber {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(255, 255, 255, 0.6)),
              radial-gradient(circle at 90% 10%, rgba(245, 158, 11, 0.18), transparent 40%);
}
.hero-theme-teal {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(255, 255, 255, 0.6)),
              radial-gradient(circle at 50% 50%, rgba(20, 184, 166, 0.15), transparent 60%);
}
.hero-theme-fuchsia {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(255, 255, 255, 0.6)),
              radial-gradient(circle at 10% 90%, rgba(217, 70, 239, 0.18), transparent 40%);
}
.hero-theme-stone {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(255, 255, 255, 0.6)),
              radial-gradient(circle at 0% 0%, rgba(120, 113, 108, 0.15), transparent 40%),
              radial-gradient(circle at 100% 100%, rgba(168, 162, 158, 0.15), transparent 40%);
}
.hero-theme-red {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(255, 255, 255, 0.6)),
              radial-gradient(circle at 80% 20%, rgba(239, 68, 68, 0.18), transparent 40%),
              radial-gradient(circle at 20% 80%, rgba(220, 38, 38, 0.08), transparent 30%);
}
.hero-theme-violet {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(255, 255, 255, 0.6)),
              radial-gradient(circle at 20% 50%, rgba(139, 92, 246, 0.18), transparent 40%);
}
.hero-theme-zinc {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(255, 255, 255, 0.6)),
              radial-gradient(circle at 50% 100%, rgba(113, 113, 122, 0.15), transparent 50%);
}
.hero-theme-slate {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(255, 255, 255, 0.6)),
              radial-gradient(circle at 50% 0%, rgba(100, 116, 139, 0.18), transparent 50%);
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
    padding: 16px 20px;
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
