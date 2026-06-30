<template>
  <section ref="root" class="theme-page">
    <PublicPageHeader title="主题展厅" description="所见即所得的预览，展示颜色、字体、卡片和控件在各种状态下的表现。" kicker="THEME STUDIO" theme="fuchsia">
      <div class="active-theme-pill" :style="previewStyle">
        <div class="active-pulse"></div>
        <div class="pill-info">
          <span class="pill-label">CURRENT</span>
          <strong class="pill-name">{{ activeThemeName }}</strong>
        </div>
      </div>
    </PublicPageHeader>
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
          <img v-if="computedThemeConfig?.backgroundImage" :src="computedThemeConfig.backgroundImage" alt="" loading="lazy" />
          <Palette v-else :size="52" />
        </div>
        
        <div class="theme-preview__content">
          <div class="theme-preview__tabs">
            <button
              class="tab-btn"
              :class="{ 'is-active': activeTab === 'visual' }"
              type="button"
              @click="activeTab = 'visual'"
            >
              视觉呈现
            </button>
            <button
              class="tab-btn"
              :class="{ 'is-active': activeTab === 'json' }"
              type="button"
              @click="activeTab = 'json'"
            >
              配置 JSON
            </button>
          </div>
          <div v-if="activeTab === 'visual'" class="theme-visual-panel">
            <p class="page-kicker">{{ computedThemeConfig?.themeName }} ({{ selectedVersion }})</p>
            <h2>{{ computedThemeConfig?.displayName }}</h2>
            <p>{{ selectedMood }}</p>
            
            <div class="theme-tokens">
              <span v-for="token in selectedTokens" :key="token.label">
                <i :style="{ backgroundColor: token.color }" />
                {{ token.label }}
              </span>
            </div>
            <div class="theme-selectors">
              <div class="selector-row">
                <label>
                  布局密度
                  <BaseSelect v-model="selectedDensity" :options="densities" />
                </label>
                <label>
                  动效强度
                  <BaseSelect v-model="selectedMotion" :options="motions" />
                </label>
              </div>
              <div class="selector-row">
                <label>
                  圆角卡片
                  <BaseSelect v-model="selectedCardStyle" :options="cardStyles" />
                </label>
                <label>
                  历史版本
                  <BaseSelect v-model="selectedVersion" :options="mockVersions" />
                </label>
              </div>
            </div>
            <div class="theme-preview__cards">
              <article>
                <strong>文章卡片</strong>
                <span>布局间距: {{ selectedDensity === 'comfortable' ? '16px' : (selectedDensity === 'compact' ? '10px' : '24px') }}</span>
              </article>
              <article>
                <strong>作品陈列</strong>
                <span>卡片圆角: {{ selectedCardStyle === 'default' ? '8px' : (selectedCardStyle === 'sharp' ? '4px' : (selectedCardStyle === 'glass' ? '10px' : '18px')) }}</span>
              </article>
            </div>
          </div>
          <div v-else class="theme-json-panel">
            <div class="json-actions">
              <button class="icon-text-btn" type="button" @click="copyJson">
                <Copy :size="12" />
                {{ copyBtnText }}
              </button>
              <button class="icon-text-btn" type="button" @click="downloadJson">
                <Download :size="12" />
                下载 JSON
              </button>
            </div>
            <pre class="json-code-block"><code>{{ formattedJson }}</code></pre>
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
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import PublicPageHeader from '@/components/common/PublicPageHeader.vue'
import { CheckCircle, Copy, Download, LoaderCircle, Palette, RotateCcw, WandSparkles } from '@lucide/vue'
import { fetchCurrentTheme, fetchThemes } from '@/services/content'
import { toUserMessage } from '@/services/http'
import { usePageReveal } from '@/shared/composables/usePageReveal'
import { toCssImageUrl } from '@/shared/cssImage'
import type { PublicThemeConfig, ThemeConfig } from '@/shared/domain'
import { applyThemeConfig } from '@/shared/theme'
import BaseSelect from '@/shared/components/BaseSelect.vue'
interface ColorToken {
  label: string
  color: string
}
// 页面挂载的根 DOM 节点引用
// 初始化主题沙盒的相关色彩变量、字体族及调试用版本、布局密度变体快照配置
const root = ref<HTMLElement | null>(null)
// 公开的主题列表数据
const themes = ref<PublicThemeConfig[]>([])
// 当前选中的主题配置数据
const selectedTheme = ref<PublicThemeConfig | null>(null)
// 当前全站活动的主题配置数据
const currentTheme = ref<ThemeConfig | null>(null)
// 主题列表加载状态标识
const isLoading = ref(true)
// 页面全局提示消息文本
const notice = ref('')
// 是否已应用本地临时主题预览
const hasAppliedPreview = ref(false)
// 当前处于激活状态的控制面板标签页, visual - 视觉呈现, json - 变量树
const activeTab = ref<'visual' | 'json'>('visual')
// 当前预览选中的排版间距配置
const selectedDensity = ref<'comfortable' | 'compact' | 'spacious'>('comfortable')
// 当前预览选中的动画过渡配置
const selectedMotion = ref<'dynamic' | 'subtle' | 'static'>('dynamic')
// 当前预览选中的卡片圆角配置
const selectedCardStyle = ref<string>('default')
// 当前预览选中的模拟历史版本号
const selectedVersion = ref<string>('v1.2.0')
// 复制配置按钮的即时文案
const copyBtnText = ref('复制 JSON')
// 间距配置可选项数组
const densities = [
  { value: 'comfortable', label: '舒适 (Comfortable)' },
  { value: 'compact', label: '紧凑 (Compact)' },
  { value: 'spacious', label: '宽松 (Spacious)' },
]
// 动效配置可选项数组
const motions = [
  { value: 'dynamic', label: '灵动 (Dynamic)' },
  { value: 'subtle', label: '微动 (Subtle)' },
  { value: 'static', label: '无动效 (Static)' },
]
// 圆角配置可选项数组
const cardStyles = [
  { value: 'default', label: '默认圆角 (8px)' },
  { value: 'sharp', label: '直角风格 (4px)' },
  { value: 'glass', label: '拟物玻璃 (10px)' },
  { value: 'rounded', label: '极致圆润 (18px)' },
]
// 版本配置可选项数组
const mockVersions = [
  { value: 'v1.2.0', label: 'v1.2.0 (当前活动)' },
  { value: 'v1.1.0', label: 'v1.1.0 (微调版)' },
  { value: 'v1.0.0', label: 'v1.0.0 (初始发布)' },
]
// 注册页面元素渐显缓动效果
usePageReveal(root)
// 监听选中的主题变化, 自动重置并初始化对应的变体与版本配置
watch(selectedTheme, (newTheme) => {
  if (newTheme) {
    selectedDensity.value = (newTheme.config?.density as any) || 'comfortable'
    selectedMotion.value = (newTheme.config?.motion as any) || 'dynamic'
    selectedCardStyle.value = newTheme.cardStyle || 'default'
    selectedVersion.value = 'v1.2.0'
  }
})
// 根据选中的变体与历史版本, 动态计算输出并构建合并的主题配置树
const computedThemeConfig = computed<ThemeConfig | null>(() => {
  if (!selectedTheme.value) return null
  
  let primary = selectedTheme.value.primaryColor
  let configObj = { ...selectedTheme.value.config }
  
  if (selectedVersion.value === 'v1.1.0') {
    primary = shiftColor(primary, 15)
    if (configObj.accentColor) {
      configObj.accentColor = shiftColor(configObj.accentColor as string, -10)
    }
  } else if (selectedVersion.value === 'v1.0.0') {
    primary = shiftColor(primary, -30)
    if (configObj.accentColor) {
      configObj.accentColor = shiftColor(configObj.accentColor as string, 20)
    }
  }
  return {
    themeName: selectedTheme.value.themeName,
    displayName: selectedTheme.value.displayName,
    primaryColor: primary,
    backgroundType: selectedTheme.value.backgroundType,
    backgroundImage: selectedTheme.value.backgroundImage,
    fontFamily: selectedTheme.value.fontFamily,
    cardStyle: selectedCardStyle.value,
    layoutType: selectedTheme.value.layoutType,
    config: {
      ...configObj,
      density: selectedDensity.value,
      motion: selectedMotion.value,
    }
  }
})
// 对十六进制色值按增量做微调计算以模拟历史版本的细微色差
function shiftColor(hex: string, amount: number): string {
  if (!/^#[0-9a-fA-F]{6}$/.test(hex)) return hex
  let r = parseInt(hex.slice(1, 3), 16) + amount
  let g = parseInt(hex.slice(3, 5), 16) + amount
  let b = parseInt(hex.slice(5, 7), 16) + amount
  r = Math.min(255, Math.max(0, r))
  g = Math.min(255, Math.max(0, g))
  b = Math.min(255, Math.max(0, b))
  return `#${r.toString(16).padStart(2, '0').slice(-2)}${g.toString(16).padStart(2, '0').slice(-2)}${b.toString(16).padStart(2, '0').slice(-2)}`
}
// 读取恢复主题, 优先使用当前已保存活动主题或公开列表默认项
const restoreTheme = computed<ThemeConfig | null>(() => currentTheme.value ?? themes.value.find((theme) => theme.active) ?? null)
// 动态获取预览或活动主题的显示名称
const activeThemeName = computed(() => restoreTheme.value?.displayName || selectedTheme.value?.displayName || '默认主题')
// 动态获取预览或活动主题的气质短语
const activeThemeMood = computed(() => {
  const theme = restoreTheme.value ?? selectedTheme.value
  const config = readRecord(theme?.config)
  return readString(config.mood) || theme?.layoutType || '当前公开主题'
})
// 动态获取当前选中主题底座配置项
const selectedConfig = computed(() => readRecord(selectedTheme.value?.config))
// 动态获取当前选中主题的设计气质文案
const selectedMood = computed(() => readString(selectedConfig.value.mood) || readString(selectedConfig.value.tagline) || '主题预览')
// 动态提取计算后主题的核心配色变量数组
const selectedTokens = computed<ColorToken[]>(() => {
  const theme = computedThemeConfig.value
  if (!theme) {
    return []
  }
  return [
    { label: 'Primary', color: safeColor(theme.primaryColor, '#315bff') },
    { label: 'Accent', color: safeColor(readString(theme.config.accentColor), theme.primaryColor) },
    { label: 'Surface', color: safeColor(readString(theme.config.surfaceColor), '#ffffff') },
    { label: 'Ink', color: safeColor(readString(theme.config.inkColor), '#14151d') },
  ]
})
// 构建注入预览区域的实时 CSS 自定义属性对象
const previewStyle = computed(() => ({
  '--preview-primary': selectedTokens.value[0]?.color ?? '#315bff',
  '--preview-accent': selectedTokens.value[1]?.color ?? '#0f766e',
  '--preview-surface': selectedTokens.value[2]?.color ?? '#ffffff',
  '--preview-ink': selectedTokens.value[3]?.color ?? '#14151d',
  '--preview-bg': toCssImageUrl(computedThemeConfig.value?.backgroundImage),
}))
// 构建排版好的主题 JSON 变量字符串
const formattedJson = computed(() => {
  if (!computedThemeConfig.value) return ''
  return JSON.stringify(computedThemeConfig.value, null, 2)
})
// 将当前定制好的 JSON 树写入系统剪贴板并更新按钮提示
function copyJson() {
  if (!formattedJson.value) return
  navigator.clipboard.writeText(formattedJson.value)
    .then(() => {
      copyBtnText.value = '已复制！'
      setTimeout(() => {
        copyBtnText.value = '复制 JSON'
      }, 2000)
    })
}
// 将定制好的 JSON 配置树打包输出并以文件形式保存到本地
function downloadJson() {
  if (!formattedJson.value || !computedThemeConfig.value) return
  const blob = new Blob([formattedJson.value], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${computedThemeConfig.value.themeName}-config.json`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}
// 并行请求加载公开主题列表及当前的活动主题信息, 并在加载结束后初始化选中项
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
      ?? null
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
// 将当前定制预览的主题变量写入本地缓存, 并派发全局主题更新事件监听器
function previewSelectedTheme() {
  if (!computedThemeConfig.value) {
    return
  }
  applyThemeConfig(computedThemeConfig.value)
  window.localStorage.setItem('creatorspace_preview_theme', JSON.stringify(computedThemeConfig.value))
  window.dispatchEvent(new CustomEvent('creatorspace-theme-preview-change'))
  hasAppliedPreview.value = true
}
// 移出本地缓存的预览主题, 重新应用之前的默认活动主题
function restoreCurrentTheme() {
  window.localStorage.removeItem('creatorspace_preview_theme')
  window.dispatchEvent(new CustomEvent('creatorspace-theme-preview-change'))
  if (!restoreTheme.value) {
    notice.value = '当前主题读取失败，暂时无法恢复；刷新页面会重新读取主题配置'
    return
  }
  applyThemeConfig(restoreTheme.value)
  hasAppliedPreview.value = false
}
// 安全解析并读取对象记录
function readRecord(value: unknown): Record<string, unknown> {
  return value && typeof value === 'object' && !Array.isArray(value) ? value as Record<string, unknown> : {}
}
// 安全读取并格式化字符串
function readString(value: unknown): string {
  return typeof value === 'string' ? value.trim() : ''
}
// 安全解析并校验 Hex 或 RGBA 配色
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
// 挂载组件时自动加载主题列表
onMounted(loadThemes)
// 页面解挂卸载前如果开启过预览则还原全局默认配置
onBeforeUnmount(() => {
  if (hasAppliedPreview.value) {
    restoreCurrentTheme()
  }
})
</script>
<style scoped>
.themes-page {
  display: grid;
  gap: 24px;
  padding: 40px 0 84px;
}
.active-theme-pill {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 20px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--preview-surface) 90%, transparent);
  border: 1px solid color-mix(in srgb, var(--preview-primary) 20%, transparent);
  box-shadow: 0 8px 24px -8px color-mix(in srgb, var(--preview-primary) 30%, transparent);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}
.active-pulse {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--preview-primary);
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--preview-primary) 20%, transparent);
  animation: pulse 2s infinite cubic-bezier(0.4, 0, 0.2, 1);
}
@keyframes pulse {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 color-mix(in srgb, var(--preview-primary) 40%, transparent); }
  70% { transform: scale(1); box-shadow: 0 0 0 6px rgba(0,0,0,0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(0,0,0,0); }
}
.pill-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.pill-label {
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.1em;
  color: var(--preview-primary);
  opacity: 0.8;
}
.pill-name {
  font-size: 14px;
  font-weight: 700;
  color: var(--preview-ink);
}

.theme-list,
.theme-preview {
  border-radius: 20px;
}
.themes-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}
.theme-list {
  position: sticky;
  top: 100px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px;
  background: var(--surface-1, #ffffff);
  border: 1px solid rgba(0, 0, 0, 0.04);
  box-shadow: 0 4px 20px -8px rgba(0, 0, 0, 0.05);
}
.theme-option {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  min-height: 60px;
  padding: 12px;
  border: 1px solid transparent;
  border-radius: 12px;
  background: transparent;
  color: var(--tone-ink);
  cursor: pointer;
  text-align: left;
  transition: all 0.2s ease;
}
.theme-option.is-active,
.theme-option:hover {
  background: color-mix(in srgb, var(--preview-primary, #315bff) 8%, transparent);
}
.theme-option.is-active {
  border-color: color-mix(in srgb, var(--preview-primary, #315bff) 20%, transparent);
  box-shadow: 0 4px 12px -4px color-mix(in srgb, var(--preview-primary, #315bff) 20%, transparent);
}
.theme-option__swatch {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  box-shadow: inset 0 0 0 4px rgba(255, 255, 255, 0.5), 0 2px 8px rgba(0,0,0,0.1);
}
.theme-option span:not(.theme-option__swatch) {
  display: grid;
  gap: 2px;
}
.theme-option strong {
  font-size: 14px;
  font-weight: 600;
}
.theme-option small {
  color: var(--tone-muted);
  font-size: 11px;
}
.theme-preview {
  position: relative;
  display: grid;
  grid-template-columns: 1fr;
  min-height: 640px;
  overflow: hidden;
  background: var(--preview-surface);
  box-shadow: 0 24px 48px -12px rgba(0, 0, 0, 0.1);
  border: 1px solid color-mix(in srgb, var(--preview-ink) 8%, transparent);
}
.theme-preview__cover {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.15;
  background: linear-gradient(135deg, color-mix(in srgb, var(--preview-primary) 40%, transparent) 0%, transparent 100%);
  pointer-events: none;
  z-index: 1;
}
.theme-preview__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  mask-image: linear-gradient(to bottom, rgba(0,0,0,1), rgba(0,0,0,0));
  -webkit-mask-image: linear-gradient(to bottom, rgba(0,0,0,1), rgba(0,0,0,0));
}
.theme-preview__content {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  gap: 24px;
  margin: clamp(24px, 4vw, 40px);
  padding: clamp(24px, 4vw, 40px);
  background: color-mix(in srgb, var(--preview-surface) 75%, transparent);
  backdrop-filter: blur(32px);
  -webkit-backdrop-filter: blur(32px);
  border-radius: 20px;
  border: 1px solid color-mix(in srgb, var(--preview-ink) 8%, transparent);
  box-shadow: 0 8px 32px rgba(0,0,0,0.06);
}
.theme-preview__content h2 {
  margin: 0;
  color: var(--preview-ink);
  font-size: clamp(36px, 5vw, 48px);
  font-weight: 860;
  letter-spacing: -0.02em;
  line-height: 1.1;
}
.theme-preview__content p:not(.page-kicker) {
  max-width: 620px;
  margin: 0;
  color: color-mix(in srgb, var(--preview-ink) 72%, transparent);
  line-height: 1.6;
}
.theme-preview__tabs {
  display: flex;
  gap: 24px;
  border-bottom: 1px solid color-mix(in srgb, var(--preview-ink) 12%, transparent);
  padding-bottom: 0;
}
.tab-btn {
  border: 0;
  border-bottom: 2px solid transparent;
  padding: 12px 0;
  background: transparent;
  color: color-mix(in srgb, var(--preview-ink) 50%, transparent);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: -1px;
}
.tab-btn.is-active,
.tab-btn:hover {
  border-bottom-color: var(--preview-primary);
  color: var(--preview-ink);
}
.theme-selectors {
  display: grid;
  gap: 16px;
  margin: 16px 0;
}
.selector-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}
.selector-row label {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: color-mix(in srgb, var(--preview-ink) 70%, transparent);
  font-size: 12px;
  font-weight: 600;
}
.selector-row select {
  appearance: none;
  height: 42px;
  padding: 0 36px 0 14px;
  border: 1px solid color-mix(in srgb, var(--preview-ink) 16%, transparent);
  border-radius: 10px;
  background-color: color-mix(in srgb, var(--preview-surface) 90%, transparent);
  background-image:
    linear-gradient(45deg, transparent 50%, var(--preview-primary) 50%),
    linear-gradient(135deg, var(--preview-primary) 50%, transparent 50%);
  background-position:
    calc(100% - 18px) 50%,
    calc(100% - 13px) 50%;
  background-size:
    5px 5px,
    5px 5px;
  background-repeat: no-repeat;
  color: var(--preview-ink);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}
.selector-row select:hover {
  border-color: var(--preview-primary);
  background-color: color-mix(in srgb, var(--preview-primary) 4%, var(--preview-surface));
}
.selector-row select:focus {
  border-color: var(--preview-primary);
  outline: none;
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--preview-primary) 20%, transparent);
}
.theme-json-panel {
  display: grid;
  grid-template-rows: auto 1fr;
  gap: 12px;
  height: 400px;
}
.json-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
.icon-text-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 12px;
  border: 1px solid color-mix(in srgb, var(--preview-ink) 12%, transparent);
  border-radius: 8px;
  background: color-mix(in srgb, var(--preview-surface) 80%, transparent);
  color: color-mix(in srgb, var(--preview-ink) 80%, transparent);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}
.icon-text-btn:hover {
  background: color-mix(in srgb, var(--preview-ink) 6%, var(--preview-surface));
  color: var(--preview-ink);
  border-color: color-mix(in srgb, var(--preview-ink) 20%, transparent);
}
.json-code-block {
  margin: 0;
  padding: 16px;
  border: 1px solid color-mix(in srgb, var(--preview-ink) 12%, transparent);
  border-radius: 12px;
  background: #0f111a;
  color: #a6accd;
  font-family: ui-monospace, 'Space Mono', monospace;
  font-size: 12px;
  line-height: 1.6;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-all;
}
.theme-tokens {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.theme-tokens span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 36px;
  padding: 0 12px;
  border: 1px solid color-mix(in srgb, var(--preview-ink) 12%, transparent);
  border-radius: 999px;
  background: color-mix(in srgb, var(--preview-surface) 90%, transparent);
  color: color-mix(in srgb, var(--preview-ink) 80%, transparent);
  font-size: 13px;
  font-weight: 600;
}
.theme-tokens i {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  box-shadow: inset 0 0 0 2px rgba(255, 255, 255, 0.4), 0 2px 4px rgba(0,0,0,0.1);
}
.theme-preview__cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-top: 8px;
}
.theme-preview__cards article {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 20px;
  border: 1px solid color-mix(in srgb, var(--preview-ink) 12%, transparent);
  border-radius: 16px;
  background: color-mix(in srgb, var(--preview-surface) 85%, transparent);
  box-shadow: 0 4px 12px rgba(0,0,0,0.02);
  transition: transform 0.2s ease;
}
.theme-preview__cards article:hover {
  transform: translateY(-2px);
}
.theme-preview__cards strong {
  color: var(--preview-ink);
  font-size: 15px;
}
.theme-preview__cards span {
  color: color-mix(in srgb, var(--preview-ink) 60%, transparent);
  font-size: 13px;
}
.theme-actions {
  display: flex;
  gap: 12px;
  margin-top: 12px;
}
.inline-notice {
  margin: 0;
  padding: 12px 16px;
  border-left: 4px solid var(--tone-coral);
  border-radius: 0 8px 8px 0;
  background: color-mix(in srgb, var(--tone-coral) 10%, transparent);
  color: var(--tone-coral-dark, #c25f3a);
  font-size: 14px;
  font-weight: 500;
  line-height: 1.5;
}
@media (max-width: 1020px) {
  .themes-layout {
    grid-template-columns: 1fr;
  }
  .theme-list {
    position: static;
  }
}
@media (max-width: 760px) {
  .theme-preview__content {
    margin: 16px;
    padding: 20px;
  }
}
</style>
