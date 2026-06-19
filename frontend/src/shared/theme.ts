import type { ThemeConfig } from '@/shared/domain'

const COLOR_KEYS = [
  '--tone-primary',
  '--md-sys-color-primary',
  '--tone-teal',
  '--md-sys-color-secondary',
] as const

const DEFAULT_RADIUS = '8px'
const WRITTEN_THEME_KEYS = [
  '--theme-font-family',
  '--app-radius-sm',
  '--app-radius-md',
  '--app-radius-lg',
  '--md-sys-color-primary-container',
  '--md-sys-color-on-primary-container',
  '--tone-coral',
  '--tone-panel-solid',
  ...COLOR_KEYS,
] as const

export interface AppliedThemeSnapshot {
  name: string
  displayName: string
}

// 把后台主题安全映射到前台 CSS 变量，未知 config 不直接写入 DOM。
export function applyThemeConfig(theme: ThemeConfig | null): AppliedThemeSnapshot | null {
  const root = document.documentElement
  root.removeAttribute('data-theme-layout')
  clearAppliedThemeVars(root)

  if (!theme) {
    return null
  }

  const primaryColor = normalizeCssColor(theme.primaryColor)
  if (primaryColor) {
    COLOR_KEYS.forEach((key) => root.style.setProperty(key, primaryColor))
    root.style.setProperty('--md-sys-color-primary-container', mixHex(primaryColor, '#ffffff', 0.82))
    root.style.setProperty('--md-sys-color-on-primary-container', mixHex(primaryColor, '#050816', 0.22))
  }

  const fontFamily = normalizeFontFamily(theme.fontFamily)
  if (fontFamily) {
    root.style.setProperty('--theme-font-family', fontFamily)
  }

  const cardRadius = readCardRadius(theme.cardStyle)
  root.style.setProperty('--app-radius-sm', cardRadius.sm)
  root.style.setProperty('--app-radius-md', cardRadius.md)
  root.style.setProperty('--app-radius-lg', cardRadius.lg)

  const layout = normalizeIdentifier(theme.layoutType)
  if (layout) {
    root.setAttribute('data-theme-layout', layout)
  }

  applySupportedConfig(theme.config)

  return {
    name: theme.themeName,
    displayName: theme.displayName,
  }
}

function clearAppliedThemeVars(root: HTMLElement) {
  WRITTEN_THEME_KEYS.forEach((key) => root.style.removeProperty(key))
}

function applySupportedConfig(config: Record<string, unknown>) {
  const root = document.documentElement
  const accent = normalizeCssColor(readConfigString(config, 'accentColor'))
  if (accent) {
    root.style.setProperty('--tone-coral', accent)
  }

  const surface = normalizeCssColor(readConfigString(config, 'surfaceColor'))
  if (surface) {
    root.style.setProperty('--tone-panel-solid', surface)
  }
}

function readConfigString(config: Record<string, unknown>, key: string): string {
  const value = config[key]
  return typeof value === 'string' ? value : ''
}

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

function normalizeFontFamily(value?: string | null): string {
  const font = value?.trim() ?? ''
  if (!font || font.length > 120 || /[;{}<>]/.test(font)) {
    return ''
  }
  return font
}

function normalizeIdentifier(value?: string | null): string {
  const id = value?.trim().toLowerCase() ?? ''
  return /^[a-z0-9-]{1,32}$/.test(id) ? id : ''
}

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

function expandHex(value: string): string {
  if (value.length === 4) {
    return `#${value[1]}${value[1]}${value[2]}${value[2]}${value[3]}${value[3]}`
  }
  return value
}

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

function hexToRgb(value: string) {
  return {
    r: parseInt(value.slice(1, 3), 16),
    g: parseInt(value.slice(3, 5), 16),
    b: parseInt(value.slice(5, 7), 16),
  }
}

function rgbToHex(color: { r: number; g: number; b: number }): string {
  return `#${toHex(color.r)}${toHex(color.g)}${toHex(color.b)}`
}

function toHex(value: number): string {
  return value.toString(16).padStart(2, '0')
}
