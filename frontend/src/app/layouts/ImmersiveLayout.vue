<template>
<!-- 沉浸式无边框排版布局 -->
<!-- 沉浸式布局外壳容器 -->
  <div class="cs-shell">
    <!-- 顶部固定透明导航条 -->
    <nav class="cs-nav" aria-label="主导航">
      <RouterLink to="/" class="cs-nav__brand">
        <span class="cs-nav__mark">
          <img src="/public.svg" alt="" aria-hidden="true" />
        </span>
        <span>{{ siteName }}</span>
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
    <ThemeHUD />
  </div>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'

import ThemeHUD from '@/shared/components/ThemeHUD.vue'
import { useSiteIdentity } from '@/shared/siteIdentity'

interface ImmersiveNavItem {
  label: string
  to: string
}

// 获取站点名称并声明导航菜单项
// 获取全局配置中的站点名称并声明导航菜单项
const { siteName } = useSiteIdentity({ load: false })
const immersiveNavItems: ImmersiveNavItem[] = [
  { label: '游客', to: '/articles' },
  { label: '登录', to: '/login' },
  { label: '注册', to: '/register' },
]
</script>

<style scoped>
.cs-shell {

  --cs-bg: #06070d;
  --cs-bg-raise: #0b0d18;
  --cs-bg-veil: #10131f;
  --cs-ink: #f3f5ff;
  --cs-ink-dim: rgba(226, 230, 247, 0.78);
  --cs-ink-faint: rgba(206, 212, 240, 0.52);
  --cs-line: rgba(150, 165, 220, 0.24);
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
  color: var(--cs-ink);
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
  text-shadow: 0 1px 14px rgba(0, 0, 0, 0.42);
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
  background: rgba(8, 11, 22, 0.72);
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.28), inset 0 1px 0 rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(18px);
}

.cs-nav__links a {
  min-width: 54px;
  padding: 8px 16px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 680;
  color: rgba(243, 245, 255, 0.82);
  text-align: center;
  transition: color 0.25s ease, background 0.25s ease, box-shadow 0.25s ease;
}

.cs-nav__links a.router-link-active,
.cs-nav__links a:hover {
  color: #06111f;
  background: linear-gradient(135deg, var(--cs-accent), var(--cs-accent-3));
  box-shadow: 0 10px 26px rgba(110, 168, 255, 0.28);
}

.cs-nav__links a:focus-visible {
  outline: 2px solid var(--cs-accent-3);
  outline-offset: 3px;
}

@media (max-width: 760px) {
  .cs-nav__links {
    display: none;
  }
}
</style>
