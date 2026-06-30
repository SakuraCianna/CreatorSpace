<template>
  <header
    ref="headerRef"
    class="public-page-header"
    :class="[`theme-${theme}`]"
    @mouseenter="onHover"
    @mouseleave="onLeave"
  >
    <!-- Background layer -->
    <div class="header-bg">
      <div class="decor-circle circle-1" ref="circle1"></div>
      <div class="decor-circle circle-2" ref="circle2"></div>
      <div class="decor-glass"></div>
      <div class="border-glow" ref="borderGlow"></div>
    </div>

    <!-- Content layer -->
    <div class="header-content" ref="contentRef">
      <div class="header-copy">
        <p v-if="kicker" class="page-kicker">{{ kicker }}</p>
        <h1 class="page-title">{{ title }}</h1>
        <p v-if="description" class="page-description">{{ description }}</p>
      </div>
      <div class="header-actions" v-if="$slots.default">
        <slot></slot>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import gsap from 'gsap'

interface Props {
  title: string
  description?: string
  kicker?: string
  theme?: string
}

withDefaults(defineProps<Props>(), {
  theme: 'blue',
})

const headerRef = ref<HTMLElement | null>(null)
const circle1 = ref<HTMLElement | null>(null)
const circle2 = ref<HTMLElement | null>(null)
const borderGlow = ref<HTMLElement | null>(null)
const contentRef = ref<HTMLElement | null>(null)

let ambientCtx: gsap.Context | null = null
let hoverCtx: gsap.Context | null = null

onMounted(() => {
  ambientCtx = gsap.context(() => {
    gsap.to(circle1.value, {
      y: 'random(-15, 15)',
      x: 'random(-15, 15)',
      scale: 'random(0.9, 1.1)',
      duration: 3,
      repeat: -1,
      yoyo: true,
      ease: 'sine.inOut'
    })
    gsap.to(circle2.value, {
      y: 'random(-25, 25)',
      x: 'random(-25, 25)',
      scale: 'random(0.85, 1.15)',
      duration: 4,
      repeat: -1,
      yoyo: true,
      ease: 'sine.inOut',
      delay: 1
    })

    gsap.from(headerRef.value, {
      y: 12,
      opacity: 0,
      duration: 0.8,
      ease: 'power3.out'
    })
    
    if (contentRef.value) {
      const children = contentRef.value.querySelectorAll('.page-kicker, .page-title, .page-description, .header-actions')
      gsap.from(children, {
        y: 10,
        opacity: 0,
        duration: 0.6,
        stagger: 0.08,
        ease: 'power2.out',
        delay: 0.1
      })
    }
  }, headerRef.value)
})

onBeforeUnmount(() => {
  ambientCtx?.revert()
  hoverCtx?.revert()
})

const onHover = () => {
  if (hoverCtx) hoverCtx.revert()
  hoverCtx = gsap.context(() => {
    gsap.to(headerRef.value, {
      boxShadow: '0 12px 30px -10px var(--theme-color-soft)',
      duration: 0.4,
      ease: 'power2.out'
    })
    gsap.to(borderGlow.value, {
      opacity: 1,
      duration: 0.4
    })
  }, headerRef.value)
}

const onLeave = () => {
  if (hoverCtx) hoverCtx.revert()
  hoverCtx = gsap.context(() => {
    gsap.to(headerRef.value, {
      boxShadow: '0 4px 20px -8px rgba(0,0,0,0.05)',
      duration: 0.4,
      ease: 'power2.out'
    })
    gsap.to(borderGlow.value, {
      opacity: 0,
      duration: 0.4
    })
  }, headerRef.value)
}
</script>

<style scoped>
.public-page-header {
  position: relative;
  height: 140px;
  display: flex;
  align-items: center;
  border-radius: 20px;
  margin-bottom: 24px;
  background: var(--surface-1, #ffffff);
  border: 1px solid rgba(0, 0, 0, 0.04);
  box-shadow: 0 4px 20px -8px rgba(0, 0, 0, 0.05);
  will-change: transform, box-shadow;
}

.header-bg {
  position: absolute;
  inset: 0;
  border-radius: 20px;
  overflow: hidden;
  z-index: 1;
}

.decor-glass {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255,255,255,0.4) 0%, rgba(255,255,255,0.85) 100%);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  z-index: 2;
}

.border-glow {
  position: absolute;
  inset: 0;
  border-radius: 20px;
  padding: 2px;
  background: linear-gradient(135deg, var(--theme-color-strong) 0%, transparent 50%, var(--theme-color-strong) 100%);
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  opacity: 0;
  z-index: 3;
  pointer-events: none;
  transition: opacity 0.4s ease;
}

.decor-circle {
  position: absolute;
  border-radius: 50%;
  filter: blur(45px);
  opacity: 0.75;
  z-index: 1;
  mix-blend-mode: multiply;
  will-change: transform;
}

.circle-1 {
  width: 250px;
  height: 250px;
  top: -80px;
  left: 10%;
  background: var(--theme-color-soft, rgba(49, 91, 255, 0.25));
}

.circle-2 {
  width: 320px;
  height: 320px;
  bottom: -150px;
  right: -50px;
  background: var(--theme-color-muted, rgba(49, 91, 255, 0.15));
}

.header-content {
  position: relative;
  z-index: 4;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  height: 100%;
  padding: 0 40px;
  gap: 24px;
}

.header-copy {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.page-kicker {
  margin: 0 0 6px;
  font-size: 13px;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--theme-color-strong, #315bff);
  opacity: 0.95;
}

.page-title {
  margin: 0;
  font-size: 34px;
  font-weight: 860;
  line-height: 1.2;
  color: #111827;
  letter-spacing: -0.02em;
}

.page-description {
  margin: 6px 0 0;
  font-size: 14px;
  line-height: 1.5;
  color: #4b5563;
  max-width: 540px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.header-actions {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

/* Responsive */
@media (max-width: 768px) {
  .public-page-header {
    height: auto;
    min-height: 160px;
  }
  .header-content {
    flex-direction: column;
    align-items: stretch;
    padding: 24px;
    gap: 16px;
  }
  .page-description {
    white-space: normal;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    line-clamp: 2;
    -webkit-box-orient: vertical;
  }
}

/* Themes */
.theme-blue {
  --theme-color-strong: #2563eb;
  --theme-color-soft: rgba(37, 99, 235, 0.25);
  --theme-color-muted: rgba(37, 99, 235, 0.15);
}
.theme-emerald {
  --theme-color-strong: #059669;
  --theme-color-soft: rgba(5, 150, 105, 0.25);
  --theme-color-muted: rgba(5, 150, 105, 0.15);
}
.theme-purple {
  --theme-color-strong: #7c3aed;
  --theme-color-soft: rgba(124, 58, 237, 0.25);
  --theme-color-muted: rgba(124, 58, 237, 0.15);
}
.theme-rose {
  --theme-color-strong: #e11d48;
  --theme-color-soft: rgba(225, 29, 72, 0.25);
  --theme-color-muted: rgba(225, 29, 72, 0.15);
}
.theme-amber {
  --theme-color-strong: #d97706;
  --theme-color-soft: rgba(217, 119, 6, 0.25);
  --theme-color-muted: rgba(217, 119, 6, 0.15);
}
.theme-cyan {
  --theme-color-strong: #0891b2;
  --theme-color-soft: rgba(8, 145, 178, 0.25);
  --theme-color-muted: rgba(8, 145, 178, 0.15);
}
.theme-slate {
  --theme-color-strong: #475569;
  --theme-color-soft: rgba(71, 85, 105, 0.25);
  --theme-color-muted: rgba(71, 85, 105, 0.15);
}
.theme-fuchsia {
  --theme-color-strong: #c026d3;
  --theme-color-soft: rgba(192, 38, 211, 0.25);
  --theme-color-muted: rgba(192, 38, 211, 0.15);
}
</style>
