<template>
  <section ref="root" class="themes-page">
    <header class="themes-hero page-hero" data-reveal>
      <div class="hero-copy">
        <p class="page-kicker">Theme Studio</p>
        <h1>主题展厅</h1>
        <p>公开读取主题预设，展示颜色、字体、卡片和布局气质；访客预览只影响当前浏览器。</p>
      </div>
      <div class="active-theme-card" :style="previewStyle">
        <span>ACTIVE</span>
        <strong>{{ activeThemeName }}</strong>
        <p>{{ activeThemeMood }}</p>
      </div>
    </header>

    <div v-if="isLoading" class="empty-state showcase-state" data-reveal>
      <LoaderCircle class="spin" :size="24" />
      <h2>正在读取主题列表</h2>
    </div>

    <div v-else-if="themes.length === 0" class="empty-state showcase-state" data-reveal>
      <h2>暂无公开主题</h2>
      <p>{{ notice || '请在后台主题管理中配置公开主题。' }}</p>
    </div>

    <div v-else class="themes-layout">
      <section class="theme-list" aria-label="主题列表" data-reveal>
        <button
          v-for="theme in themes"
          :key="theme.themeName"
          class="theme-option"
          :class="{ 'is-active': selectedTheme?.themeName === theme.themeName }"
          type="button"
          @click="selectedTheme = theme"
        >
          <span class="theme-option__swatch" :style="{ backgroundColor: safeColor(theme.primaryColor, '#315bff') }" />
          <span>
            <strong>{{ theme.displayName }}</strong>
            <small>{{ theme.layoutType }} · {{ theme.cardStyle }}</small>
          </span>
          <CheckCircle v-if="theme.active" :size="16" />
        </button>
      </section>

      <section class="theme-preview" :style="previewStyle" data-reveal>
        <div class="theme-preview__cover">
          <img v-if="selectedTheme?.backgroundImage" :src="selectedTheme.backgroundImage" alt="" loading="lazy" />
          <Palette v-else :size="52" />
        </div>
        <div class="theme-preview__content">
          <p class="page-kicker">{{ selectedTheme?.themeName }}</p>
          <h2>{{ selectedTheme?.displayName }}</h2>
          <p>{{ selectedMood }}</p>
          <div class="theme-tokens">
            <span v-for="token in selectedTokens" :key="token.label">
              <i :style="{ backgroundColor: token.color }" />
              {{ token.label }}
            </span>
          </div>
          <div class="theme-preview__cards">
            <article>
              <strong>文章卡片</strong>
              <span>{{ selectedDensity }}</span>
            </article>
            <article>
              <strong>作品陈列</strong>
              <span>{{ selectedTheme?.backgroundType || 'color' }}</span>
            </article>
          </div>
          <div class="theme-actions">
            <button class="button button-filled" type="button" @click="previewSelectedTheme">
              <WandSparkles :size="16" />
              预览此主题
            </button>
            <button class="button button-tonal" type="button" @click="restoreCurrentTheme">
              <RotateCcw :size="16" />
              恢复当前
            </button>
          </div>
        </div>
      </section>
    </div>

    <p v-if="notice && themes.length > 0" class="inline-notice">{{ notice }}</p>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { CheckCircle, LoaderCircle, Palette, RotateCcw, WandSparkles } from '@lucide/vue'

import { fetchCurrentTheme, fetchThemes } from '@/services/content'
import { toUserMessage } from '@/services/http'
import { usePageReveal } from '@/shared/composables/usePageReveal'
import { toCssImageUrl } from '@/shared/cssImage'
import type { PublicThemeConfig, ThemeConfig } from '@/shared/domain'
import { applyThemeConfig } from '@/shared/theme'

interface ColorToken {
  label: string
  color: string
}

const root = ref<HTMLElement | null>(null)
const themes = ref<PublicThemeConfig[]>([])
const selectedTheme = ref<PublicThemeConfig | null>(null)
const currentTheme = ref<ThemeConfig | null>(null)
const isLoading = ref(true)
const notice = ref('')
const hasAppliedPreview = ref(false)

usePageReveal(root)

const restoreTheme = computed<ThemeConfig | null>(() => currentTheme.value ?? themes.value.find((theme) => theme.active) ?? null)
const activeThemeName = computed(() => restoreTheme.value?.displayName || selectedTheme.value?.displayName || '默认主题')
const activeThemeMood = computed(() => {
  const theme = restoreTheme.value ?? selectedTheme.value
  const config = readRecord(theme?.config)
  return readString(config.mood) || theme?.layoutType || '当前公开主题'
})
const selectedConfig = computed(() => readRecord(selectedTheme.value?.config))
const selectedMood = computed(() => readString(selectedConfig.value.mood) || readString(selectedConfig.value.tagline) || '主题预览')
const selectedDensity = computed(() => readString(selectedConfig.value.density) || 'comfortable')
const selectedTokens = computed<ColorToken[]>(() => {
  const theme = selectedTheme.value
  if (!theme) {
    return []
  }
  return [
    { label: 'Primary', color: safeColor(theme.primaryColor, '#315bff') },
    { label: 'Accent', color: safeColor(readString(selectedConfig.value.accentColor), theme.primaryColor) },
    { label: 'Surface', color: safeColor(readString(selectedConfig.value.surfaceColor), '#ffffff') },
    { label: 'Ink', color: safeColor(readString(selectedConfig.value.inkColor), '#14151d') },
  ]
})
const previewStyle = computed(() => ({
  '--preview-primary': selectedTokens.value[0]?.color ?? '#315bff',
  '--preview-accent': selectedTokens.value[1]?.color ?? '#0f766e',
  '--preview-surface': selectedTokens.value[2]?.color ?? '#ffffff',
  '--preview-ink': selectedTokens.value[3]?.color ?? '#14151d',
  '--preview-bg': toCssImageUrl(selectedTheme.value?.backgroundImage),
}))

async function loadThemes() {
  isLoading.value = true
  notice.value = ''
  try {
    const [themesResult, currentResult] = await Promise.allSettled([fetchThemes(), fetchCurrentTheme()])
    themes.value = themesResult.status === 'fulfilled' ? themesResult.value : []
    currentTheme.value = currentResult.status === 'fulfilled' ? currentResult.value : null
    selectedTheme.value = themes.value.find((theme) => theme.active)
      ?? themes.value.find((theme) => theme.themeName === currentTheme.value?.themeName)
      ?? themes.value[0]
    if (themesResult.status === 'rejected') {
      notice.value = toUserMessage(themesResult.reason, '主题列表暂不可用')
    }
  } catch (error) {
    themes.value = []
    currentTheme.value = null
    selectedTheme.value = null
    notice.value = toUserMessage(error, '主题接口暂不可用')
  } finally {
    isLoading.value = false
  }
}

// 公开主题页只进行浏览器本地预览，实际启用仍由后台主题接口控制。
function previewSelectedTheme() {
  if (!selectedTheme.value) {
    return
  }
  applyThemeConfig(selectedTheme.value)
  hasAppliedPreview.value = true
}

function restoreCurrentTheme() {
  if (!restoreTheme.value) {
    notice.value = '当前主题读取失败，暂时无法恢复；刷新页面会重新读取主题配置'
    return
  }
  applyThemeConfig(restoreTheme.value)
  hasAppliedPreview.value = false
}

function readRecord(value: unknown): Record<string, unknown> {
  return value && typeof value === 'object' && !Array.isArray(value) ? value as Record<string, unknown> : {}
}

function readString(value: unknown): string {
  return typeof value === 'string' ? value.trim() : ''
}

function safeColor(value: string | undefined | null, fallback: string): string {
  const color = value?.trim() ?? ''
  if (/^#[0-9a-fA-F]{3}([0-9a-fA-F]{3})?$/.test(color)) {
    return color
  }
  if (/^rgba?\(\s*[\d.%\s,-]+\)$/.test(color)) {
    return color
  }
  return fallback
}

onMounted(loadThemes)
onBeforeUnmount(() => {
  if (hasAppliedPreview.value) {
    restoreCurrentTheme()
  }
})
</script>

<style scoped>
.themes-page {
  display: grid;
  gap: 18px;
  padding: 46px 0 84px;
}

.themes-hero {
  --hero-accent: #315bff;
  --hero-accent-2: #0f766e;
  --hero-mark: "06";
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  align-items: center;
  gap: 24px;
  min-height: clamp(230px, 24vw, 308px);
  padding: clamp(24px, 3vw, 42px);
  overflow: hidden;
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.9), rgba(255, 255, 255, 0.6)),
    radial-gradient(circle at 16% 20%, color-mix(in srgb, var(--hero-accent) 18%, transparent), transparent 34%),
    radial-gradient(circle at 88% 18%, color-mix(in srgb, var(--hero-accent-2) 18%, transparent), transparent 28%),
    linear-gradient(120deg, rgba(49, 91, 255, 0.08), rgba(194, 95, 58, 0.08), rgba(0, 124, 114, 0.1));
  box-shadow: var(--tone-shadow);
  isolation: isolate;
}

.themes-hero::before {
  content: "";
  position: absolute;
  inset: 16px;
  z-index: 0;
  border: 1px solid color-mix(in srgb, var(--hero-accent) 24%, transparent);
  background:
    linear-gradient(90deg, color-mix(in srgb, var(--hero-accent) 10%, transparent), transparent 38%),
    repeating-linear-gradient(90deg, rgba(20, 21, 29, 0.05) 0 1px, transparent 1px 42px),
    repeating-linear-gradient(0deg, rgba(20, 21, 29, 0.035) 0 1px, transparent 1px 34px);
  clip-path: polygon(0 0, calc(100% - 46px) 0, 100% 46px, 100% 100%, 46px 100%, 0 calc(100% - 46px));
  pointer-events: none;
}

.themes-hero::after {
  content: var(--hero-mark);
  position: absolute;
  top: clamp(20px, 4vw, 42px);
  right: clamp(24px, 6vw, 88px);
  z-index: 0;
  color: color-mix(in srgb, var(--hero-accent) 16%, transparent);
  font-size: clamp(86px, 12vw, 180px);
  font-weight: 900;
  line-height: 0.8;
  pointer-events: none;
}

.themes-hero > * {
  position: relative;
  z-index: 1;
}

.hero-copy {
  display: grid;
  gap: 14px;
}

.hero-copy h1 {
  position: relative;
  width: fit-content;
  max-width: 100%;
  margin: 0;
  padding-top: 24px;
  color: var(--tone-ink);
  font-size: clamp(36px, 4vw, 54px);
  font-weight: 860;
  line-height: 1.08;
}

.hero-copy h1::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: clamp(110px, 18vw, 240px);
  height: 8px;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--hero-accent), var(--hero-accent-2), transparent);
}

.hero-copy p:not(.page-kicker) {
  max-width: 640px;
  margin: 0;
  color: var(--tone-muted);
  font-size: 16px;
  line-height: 1.72;
}

.active-theme-card,
.theme-list,
.theme-preview {
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm);
  background: var(--tone-panel);
  box-shadow: var(--tone-shadow);
  backdrop-filter: blur(18px);
}

.active-theme-card {
  display: grid;
  gap: 8px;
  min-height: 190px;
  align-content: end;
  padding: 22px;
  background:
    linear-gradient(145deg, color-mix(in srgb, var(--preview-primary) 24%, transparent), transparent 54%),
    linear-gradient(135deg, var(--preview-surface), color-mix(in srgb, var(--preview-accent) 18%, var(--preview-surface)));
}

.active-theme-card span {
  color: var(--preview-primary);
  font-size: 12px;
  font-weight: 860;
  letter-spacing: 0.12em;
}

.active-theme-card strong {
  color: var(--preview-ink);
  font-size: 26px;
  line-height: 1.14;
}

.active-theme-card p {
  margin: 0;
  color: color-mix(in srgb, var(--preview-ink) 72%, transparent);
  line-height: 1.58;
}

.themes-layout {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.theme-list {
  position: sticky;
  top: 96px;
  display: grid;
  gap: 10px;
  padding: 14px;
}

.theme-option {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  min-height: 66px;
  padding: 10px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.54);
  color: var(--tone-ink);
  cursor: pointer;
  text-align: left;
}

.theme-option.is-active,
.theme-option:hover {
  border-color: rgba(49, 91, 255, 0.34);
  background: rgba(49, 91, 255, 0.08);
}

.theme-option__swatch {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  box-shadow: inset 0 0 0 4px rgba(255, 255, 255, 0.72);
}

.theme-option span:not(.theme-option__swatch) {
  display: grid;
  gap: 3px;
}

.theme-option small {
  color: var(--tone-muted);
  font-size: 12px;
}

.theme-preview {
  display: grid;
  grid-template-columns: 0.86fr minmax(0, 1fr);
  overflow: hidden;
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--preview-surface) 92%, transparent), color-mix(in srgb, var(--preview-accent) 16%, var(--preview-surface))),
    var(--preview-bg);
  background-position: center;
  background-size: cover;
}

.theme-preview__cover {
  display: grid;
  min-height: 520px;
  place-items: center;
  overflow: hidden;
  background:
    radial-gradient(circle at 22% 20%, color-mix(in srgb, var(--preview-primary) 34%, transparent), transparent 32%),
    linear-gradient(145deg, color-mix(in srgb, var(--preview-ink) 90%, transparent), color-mix(in srgb, var(--preview-primary) 60%, transparent));
  color: var(--preview-surface);
}

.theme-preview__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0.82;
}

.theme-preview__content {
  display: grid;
  align-content: center;
  gap: 18px;
  padding: clamp(24px, 4vw, 48px);
}

.theme-preview__content h2 {
  margin: 0;
  color: var(--preview-ink);
  font-size: clamp(32px, 4vw, 54px);
  line-height: 1.06;
}

.theme-preview__content p:not(.page-kicker) {
  max-width: 620px;
  margin: 0;
  color: color-mix(in srgb, var(--preview-ink) 72%, transparent);
  line-height: 1.72;
}

.theme-tokens,
.theme-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.theme-tokens span {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-height: 34px;
  padding: 0 10px;
  border: 1px solid color-mix(in srgb, var(--preview-ink) 12%, transparent);
  border-radius: 999px;
  background: color-mix(in srgb, var(--preview-surface) 82%, transparent);
  color: color-mix(in srgb, var(--preview-ink) 78%, transparent);
  font-size: 12px;
  font-weight: 760;
}

.theme-tokens i {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  box-shadow: inset 0 0 0 2px rgba(255, 255, 255, 0.7);
}

.theme-preview__cards {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.theme-preview__cards article {
  display: grid;
  gap: 8px;
  min-height: 120px;
  align-content: end;
  padding: 16px;
  border: 1px solid color-mix(in srgb, var(--preview-ink) 12%, transparent);
  border-radius: var(--app-radius-sm);
  background: color-mix(in srgb, var(--preview-surface) 76%, transparent);
}

.theme-preview__cards strong {
  color: var(--preview-ink);
}

.theme-preview__cards span {
  color: color-mix(in srgb, var(--preview-ink) 62%, transparent);
  font-size: 13px;
}

.inline-notice {
  margin: 0;
  padding: 10px 12px;
  border-left: 3px solid var(--tone-coral);
  background: rgba(194, 95, 58, 0.08);
  color: #754226;
  font-size: 13px;
  line-height: 1.55;
}

@media (max-width: 1020px) {
  .themes-hero,
  .themes-layout,
  .theme-preview {
    grid-template-columns: 1fr;
  }

  .theme-list {
    position: static;
  }
}

@media (max-width: 760px) {
  .themes-page {
    padding-top: 26px;
  }

  .themes-hero {
    padding: 22px;
  }

  .theme-preview__cards {
    grid-template-columns: 1fr;
  }
}
</style>
