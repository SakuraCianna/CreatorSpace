<template>
  <header class="public-page-header" :class="[`theme-${theme}`]">
    <div class="header-content">
      <div class="header-copy">
        <p v-if="kicker" class="page-kicker">{{ kicker }}</p>
        <h1>{{ title }}</h1>
        <p v-if="description" class="page-description">{{ description }}</p>
      </div>
      <div class="header-actions" v-if="$slots.default">
        <slot></slot>
      </div>
    </div>
    <div class="header-decorations">
      <div class="decor-circle circle-1"></div>
      <div class="decor-circle circle-2"></div>
    </div>
  </header>
</template>

<script setup lang="ts">
interface Props {
  title: string
  description?: string
  kicker?: string
  theme?: string
}

withDefaults(defineProps<Props>(), {
  theme: 'blue',
})
</script>

<style scoped>
.public-page-header {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 120px;
  padding: 32px;
  border-radius: 16px;
  overflow: hidden;
  background: var(--surface-1, #ffffff);
  border: 1px solid rgba(0, 0, 0, 0.04);
  box-shadow: 0 4px 20px -8px rgba(0, 0, 0, 0.05);
  margin-bottom: 24px;
}

.header-content {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 24px;
}

.header-copy {
  flex: 1;
  min-width: 0;
}

.page-kicker {
  margin: 0 0 8px;
  font-size: 13px;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--theme-color-strong, var(--admin-primary-strong));
}

.header-copy h1 {
  margin: 0;
  font-size: 28px;
  font-weight: 860;
  line-height: 1.2;
  color: var(--text-1, #1a1a1a);
}

.page-description {
  margin: 8px 0 0;
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-2, #666666);
  max-width: 600px;
}

.header-actions {
  flex-shrink: 0;
}

.header-decorations {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 1;
  overflow: hidden;
}

.decor-circle {
  position: absolute;
  border-radius: 50%;
  filter: blur(40px);
  opacity: 0.5;
}

.circle-1 {
  width: 200px;
  height: 200px;
  top: -100px;
  left: -50px;
  background: var(--theme-color-soft, rgba(49, 91, 255, 0.15));
}

.circle-2 {
  width: 300px;
  height: 300px;
  bottom: -150px;
  right: -50px;
  background: var(--theme-color-muted, rgba(49, 91, 255, 0.08));
}

/* Themes */
.theme-blue {
  --theme-color-strong: #315bff;
  --theme-color-soft: rgba(49, 91, 255, 0.15);
  --theme-color-muted: rgba(49, 91, 255, 0.08);
}
.theme-emerald {
  --theme-color-strong: #059669;
  --theme-color-soft: rgba(5, 150, 105, 0.15);
  --theme-color-muted: rgba(5, 150, 105, 0.08);
}
.theme-purple {
  --theme-color-strong: #7c3aed;
  --theme-color-soft: rgba(124, 58, 237, 0.15);
  --theme-color-muted: rgba(124, 58, 237, 0.08);
}
.theme-rose {
  --theme-color-strong: #e11d48;
  --theme-color-soft: rgba(225, 29, 72, 0.15);
  --theme-color-muted: rgba(225, 29, 72, 0.08);
}
.theme-amber {
  --theme-color-strong: #d97706;
  --theme-color-soft: rgba(217, 119, 6, 0.15);
  --theme-color-muted: rgba(217, 119, 6, 0.08);
}
.theme-teal {
  --theme-color-strong: #0d9488;
  --theme-color-soft: rgba(13, 148, 136, 0.15);
  --theme-color-muted: rgba(13, 148, 136, 0.08);
}
.theme-fuchsia {
  --theme-color-strong: #c026d3;
  --theme-color-soft: rgba(192, 38, 211, 0.15);
  --theme-color-muted: rgba(192, 38, 211, 0.08);
}
.theme-slate {
  --theme-color-strong: #475569;
  --theme-color-soft: rgba(71, 85, 105, 0.15);
  --theme-color-muted: rgba(71, 85, 105, 0.08);
}
.theme-cyan {
  --theme-color-strong: #0891b2;
  --theme-color-soft: rgba(8, 145, 178, 0.15);
  --theme-color-muted: rgba(8, 145, 178, 0.08);
}
.theme-zinc {
  --theme-color-strong: #52525b;
  --theme-color-soft: rgba(82, 82, 91, 0.15);
  --theme-color-muted: rgba(82, 82, 91, 0.08);
}
.theme-orange {
  --theme-color-strong: #ea580c;
  --theme-color-soft: rgba(234, 88, 12, 0.15);
  --theme-color-muted: rgba(234, 88, 12, 0.08);
}

@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .header-actions {
    width: 100%;
  }
  
  .header-actions > form,
  .header-actions > div {
    width: 100%;
  }
}
</style>
