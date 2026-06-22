<template>
  <div class="cs-shell">
    <nav class="cs-nav" aria-label="主导航">
      <RouterLink to="/" class="cs-nav__brand">
        <span class="cs-nav__mark">
          <img src="/public.svg" alt="" aria-hidden="true" />
        </span>
        <span>{{ brandName }}</span>
      </RouterLink>
      <div class="cs-nav__links">
        <RouterLink v-for="item in immersiveNavItems" :key="item.to" :to="item.to">
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
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'

import { fetchSiteConfig } from '@/services/content'

interface ImmersiveNavItem {
  label: string
  to: string
}

const brandName = ref('CreatorSpace')
const immersiveNavItems: ImmersiveNavItem[] = [
  { label: '游客', to: '/articles' },
  { label: '登录', to: '/login' },
  { label: '注册', to: '/register' },
]

onMounted(async () => {
  try {
    const config = await fetchSiteConfig()
    const identity = readRecord(config['site.identity'])
    const profile = readRecord(config['site.profile.active'])
    brandName.value = readString(identity.name) || readString(profile.displayName) || brandName.value
  } catch {
    brandName.value = 'CreatorSpace'
  }
})

function readRecord(value: unknown): Record<string, unknown> {
  return value && typeof value === 'object' && !Array.isArray(value) ? value as Record<string, unknown> : {}
}

function readString(value: unknown): string {
  return typeof value === 'string' ? value.trim() : ''
}
</script>

<style scoped>
.cs-shell {

  --cs-bg: #06070d;
  --cs-bg-raise: #0b0d18;
  --cs-bg-veil: #10131f;
  --cs-ink: #f3f5ff;
  --cs-ink-dim: rgba(226, 230, 247, 0.66);
  --cs-ink-faint: rgba(206, 212, 240, 0.4);
  --cs-line: rgba(150, 165, 220, 0.16);
  --cs-line-soft: rgba(150, 165, 220, 0.09);


  --cs-accent: #6ea8ff;
  --cs-accent-2: #b18cff;
  --cs-accent-3: #54e6c8;
  --cs-accent-warm: #ff9d6e;

  --cs-edge: clamp(20px, 5vw, 92px);
  --cs-maxw: 1440px;
  --cs-font-display: 'Space Grotesk', 'Sora', system-ui, sans-serif;
  --cs-font-body: 'Sora', system-ui, sans-serif;
  --cs-font-serif: 'Newsreader', Georgia, serif;
  --cs-font-mono: 'Space Mono', ui-monospace, monospace;
}

.cs-shell {
  position: relative;
  min-height: 100vh;
  background: var(--cs-bg);
}

.cs-nav {
  position: fixed;
  top: 0;
  left: 0;
  z-index: 40;
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  max-width: var(--cs-maxw);
  margin-inline: auto;
  inset-inline: 0;
  padding: 22px var(--cs-edge);
  transition: transform 0.5s cubic-bezier(0.22, 1, 0.36, 1);
}

.cs-nav__brand {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  font-family: var(--cs-font-display);
  font-weight: 600;
  letter-spacing: 0.02em;
  color: var(--cs-ink);
}

.cs-nav__mark {
  display: inline-grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 11px;
  border: 1px solid rgba(255, 255, 255, 0.58);
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.92), rgba(232, 240, 255, 0.72)),
    rgba(255, 255, 255, 0.76);
  box-shadow: 0 8px 26px rgba(110, 168, 255, 0.24);
}

.cs-nav__mark img {
  width: 72%;
  height: 72%;
  object-fit: contain;
}

.cs-nav__links {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px;
  border: 1px solid var(--cs-line);
  border-radius: 999px;
  background: rgba(12, 15, 26, 0.5);
  backdrop-filter: blur(14px);
}

.cs-nav__links a {
  padding: 8px 16px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 500;
  color: var(--cs-ink-dim);
  transition: color 0.25s ease, background 0.25s ease;
}

.cs-nav__links a:hover {
  color: var(--cs-ink);
  background: rgba(150, 165, 220, 0.12);
}

@media (max-width: 760px) {
  .cs-nav__links {
    display: none;
  }
}
</style>
