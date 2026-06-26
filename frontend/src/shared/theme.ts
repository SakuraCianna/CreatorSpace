import type { ThemeConfig } from '@/shared/domain'

// 预设色彩的 CSS 变量名数组
const COLOR_KEYS = [
  '--tone-primary',
  '--md-sys-color-primary',
  '--tone-teal',
  '--md-sys-color-secondary',
] as const

const DEFAULT_RADIUS = '8px'

// 全站写入的主题相关 CSS 变量白名单数组
const WRITTEN_THEME_KEYS = [
  '--theme-font-family',
  '--app-radius-sm',
  '--app-radius-md',
  '--app-radius-lg',
  '--md-sys-color-primary-container',
  '--md-sys-color-on-primary-container',
  '--tone-coral',
  '--tone-panel-solid',
  '--tone-accent-soft',
  '--tone-paper',
  '--tone-ink',
  '--tone-muted',
  '--theme-density-spacing',
  '--theme-motion-duration',
  ...COLOR_KEYS,
] as const

// 已应用的主题快照接口
export interface AppliedThemeSnapshot {
  name: string
  displayName: string
}

// 将后台配置的主题映射到前台 CSS 变量, 未知配置项不写入 DOM
// 全局解析主题快照, 将色彩、字体、间距和圆角样式动态注入到 HTML 根 DOM 中以重新渲染整站皮肤
export function applyThemeConfig(theme: ThemeConfig | null): AppliedThemeSnapshot | null {
  const root = document.documentElement
  root.removeAttribute('data-theme-layout')
  clearAppliedThemeVars(root)

  if (!theme) {
    return null
  }

  // 校验并设置主色调
  const primaryColor = normalizeCssColor(theme.primaryColor)
  if (primaryColor) {
    COLOR_KEYS.forEach((key) => root.style.setProperty(key, primaryColor))
    root.style.setProperty('--md-sys-color-primary-container', mixHex(primaryColor, '#ffffff', 0.82))
    root.style.setProperty('--md-sys-color-on-primary-container', mixHex(primaryColor, '#050816', 0.22))
  }

  // 校验并设置字体族
  const fontFamily = normalizeFontFamily(theme.fontFamily)
  if (fontFamily) {
    root.style.setProperty('--theme-font-family', fontFamily)
  }

  // 读取并应用圆角大小
  const cardRadius = readCardRadius(theme.cardStyle)
  root.style.setProperty('--app-radius-sm', cardRadius.sm)
  root.style.setProperty('--app-radius-md', cardRadius.md)
  root.style.setProperty('--app-radius-lg', cardRadius.lg)

  // 读取并设置布局类型
  const layout = normalizeIdentifier(theme.layoutType)
  if (layout) {
    root.setAttribute('data-theme-layout', layout)
  }

  // 应用其他支持的子配置项
  applySupportedConfig(theme.config)

  return {
    name: theme.themeName,
    displayName: theme.displayName,
  }
}

// 从文档根节点清除上一次写入的主题变量
function clearAppliedThemeVars(root: HTMLElement) {
  WRITTEN_THEME_KEYS.forEach((key) => root.style.removeProperty(key))
}

// 处理并应用主题内的额外自定义配置
function applySupportedConfig(config: Record<string, unknown>) {
  const root = document.documentElement
  
  // 提取并设置辅助色
  const accent = normalizeCssColor(readConfigString(config, 'accentColor'))
  if (accent) {
    root.style.setProperty('--tone-coral', accent)
  }

  // 提取并设置淡色高亮色
  const accentSoft = normalizeCssColor(readConfigString(config, 'accentSoftColor'))
  if (accentSoft) {
    root.style.setProperty('--tone-accent-soft', accentSoft)
  }

  // 主题的 surfaceColor 只用于主题预览与主题编辑数据, 不写入全局面板色。
  // 后台可能配置深色 surfaceColor, 若直接覆盖 --tone-panel-solid 会导致后台和公共页面暗底灰字。

  // 提取并设置全局背景底色
  const bg = normalizeCssColor(readConfigString(config, 'backgroundColor'))
  if (bg) {
    root.style.setProperty('--tone-paper', bg)
  }

  // 主题配置里的 inkColor/mutedColor 不写入全局文字色。
  // 这些颜色适合主题预览容器, 直接覆盖全站会让标题和正文在部分主题下接近透明。

  // 根据间距配置应用不同的布局密度数值
  const density = readConfigString(config, 'density')
  if (density === 'compact') {
    root.style.setProperty('--theme-density-spacing', '10px')
  } else if (density === 'spacious') {
    root.style.setProperty('--theme-density-spacing', '24px')
  } else if (density === 'comfortable') {
    root.style.setProperty('--theme-density-spacing', '16px')
  }

  // 根据动效设置应用不同的过渡时长
  const motion = readConfigString(config, 'motion')
  if (motion === 'static') {
    root.style.setProperty('--theme-motion-duration', '0s')
  } else if (motion === 'subtle') {
    root.style.setProperty('--theme-motion-duration', '0.15s')
  } else if (motion === 'dynamic') {
    root.style.setProperty('--theme-motion-duration', '0.3s')
  }
}

// 从配置记录中安全读取字符串
function readConfigString(config: Record<string, unknown>, key: string): string {
  const value = config[key]
  return typeof value === 'string' ? value : ''
}

// 校验并格式化 CSS 颜色值
function normalizeCssColor(value?: string | null): string {
  const color = value?.trim() ?? ''
  if (/^#[0-9a-fA-F]{3}([0-9a-fA-F]{3})?$/.test(color)) {
    return expandHex(color)
  }
  if (/^(rgb|hsl)a?\(\s*[\d.%\s,-]+\)$/.test(color)) {
    return color
  }
  return ''
}

// 校验并格式化字体族名称
function normalizeFontFamily(value?: string | null): string {
  const font = value?.trim() ?? ''
  if (!font || font.length > 120 || /[;{}<>]/.test(font)) {
    return ''
  }
  return font
}

// 校验并格式化标识符
function normalizeIdentifier(value?: string | null): string {
  const id = value?.trim().toLowerCase() ?? ''
  return /^[a-z0-9-]{1,32}$/.test(id) ? id : ''
}

// 根据卡片风格标识返回不同尺寸的圆角数值
function readCardRadius(cardStyle: string) {
  const style = normalizeIdentifier(cardStyle)
  if (style === 'sharp') {
    return { sm: '4px', md: '6px', lg: '12px' }
  }
  if (style === 'soft' || style === 'rounded') {
    return { sm: '12px', md: '18px', lg: '30px' }
  }
  if (style === 'glass') {
    return { sm: '10px', md: '16px', lg: '28px' }
  }
  return { sm: DEFAULT_RADIUS, md: '12px', lg: '28px' }
}

// 扩展 3 位 hex 颜色值为 6 位 hex 颜色值
function expandHex(value: string): string {
  if (value.length === 4) {
    return `#${value[1]}${value[1]}${value[2]}${value[2]}${value[3]}${value[3]}`
  }
  return value
}

// 按权重混合两个十六进制颜色值
function mixHex(from: string, to: string, weight: number): string {
  if (!/^#[0-9a-fA-F]{6}$/.test(from) || !/^#[0-9a-fA-F]{6}$/.test(to)) {
    return to
  }
  const a = hexToRgb(from)
  const b = hexToRgb(to)
  const ratio = Math.min(Math.max(weight, 0), 1)
  return rgbToHex({
    r: Math.round(a.r * (1 - ratio) + b.r * ratio),
    g: Math.round(a.g * (1 - ratio) + b.g * ratio),
    b: Math.round(a.b * (1 - ratio) + b.b * ratio),
  })
}

// 十六进制颜色转换为 RGB 对象
function hexToRgb(value: string) {
  return {
    r: parseInt(value.slice(1, 3), 16),
    g: parseInt(value.slice(3, 5), 16),
    b: parseInt(value.slice(5, 7), 16),
  }
}

// RGB 对象转换为十六进制颜色字符串
function rgbToHex(color: { r: number; g: number; b: number }): string {
  return `#${toHex(color.r)}${toHex(color.g)}${toHex(color.b)}`
}

// 数值转换为双位十六进制字符串
function toHex(value: number): string {
  return value.toString(16).padStart(2, '0')
}
