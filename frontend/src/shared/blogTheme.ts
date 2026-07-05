import type {
  BlogBlockStyle,
  BlogCanvasType,
  BlogFontPreset,
  BlogLayoutStyle,
  BlogThemeConfig,
} from './domain'
import { toCssImageUrl } from './cssImage'

export const DEFAULT_BLOG_THEME: BlogThemeConfig = {
  displayName: '蓝墨手稿',
  fontPreset: 'literary-serif',
  accentColor: '#1d4ed8',
  titleColor: '#102033',
  bodyColor: '#455469',
  canvasType: 'soft-paper',
  canvasColor: '#eef4fb',
  canvasImage: null,
  paperColor: '#fbfdff',
  layoutStyle: 'editorial',
  blockStyle: 'quiet',
}

export const BLOG_FONT_OPTIONS: Array<{ label: string; value: BlogFontPreset }> = [
  { label: '文学衬线', value: 'literary-serif' },
  { label: '现代无衬线', value: 'neo-grotesk' },
  { label: '圆润日记', value: 'rounded-sans' },
  { label: '编辑器等宽', value: 'mono-editor' },
]

export const BLOG_CANVAS_OPTIONS: Array<{ label: string; value: BlogCanvasType }> = [
  { label: '柔纸', value: 'soft-paper' },
  { label: '织纹', value: 'linen' },
  { label: '渐变', value: 'gradient' },
  { label: '图片', value: 'image' },
]

export const BLOG_LAYOUT_OPTIONS: Array<{ label: string; value: BlogLayoutStyle }> = [
  { label: '杂志', value: 'editorial' },
  { label: '手账', value: 'notebook' },
  { label: '画廊', value: 'gallery' },
]

export const BLOG_BLOCK_OPTIONS: Array<{ label: string; value: BlogBlockStyle }> = [
  { label: '安静', value: 'quiet' },
  { label: '墨线', value: 'ink' },
  { label: '卡片', value: 'carded' },
]

export const BLOG_THEME_PRESETS: BlogThemeConfig[] = [
  {
    displayName: '蓝墨手稿',
    fontPreset: 'literary-serif',
    accentColor: '#1d4ed8',
    titleColor: '#102033',
    bodyColor: '#455469',
    canvasType: 'soft-paper',
    canvasColor: '#eef4fb',
    canvasImage: null,
    paperColor: '#fbfdff',
    layoutStyle: 'editorial',
    blockStyle: 'quiet',
  },
  {
    displayName: '松石画布',
    fontPreset: 'rounded-sans',
    accentColor: '#0f766e',
    titleColor: '#11312d',
    bodyColor: '#34514d',
    canvasType: 'gradient',
    canvasColor: '#edf7f4',
    canvasImage: null,
    paperColor: '#fbfffd',
    layoutStyle: 'gallery',
    blockStyle: 'carded',
  },
  {
    displayName: '铅字笔记',
    fontPreset: 'mono-editor',
    accentColor: '#b45309',
    titleColor: '#1f2937',
    bodyColor: '#4b5563',
    canvasType: 'linen',
    canvasColor: '#f7f1e7',
    canvasImage: null,
    paperColor: '#fffaf1',
    layoutStyle: 'notebook',
    blockStyle: 'ink',
  },
  {
    displayName: '夜航终端',
    fontPreset: 'mono-editor',
    accentColor: '#38bdf8',
    titleColor: '#e5f6ff',
    bodyColor: '#a9c3d8',
    canvasType: 'gradient',
    canvasColor: '#07111f',
    canvasImage: null,
    paperColor: '#0b1626',
    layoutStyle: 'editorial',
    blockStyle: 'ink',
  },
  {
    displayName: '春日杂志',
    fontPreset: 'neo-grotesk',
    accentColor: '#db2777',
    titleColor: '#2c1826',
    bodyColor: '#65495d',
    canvasType: 'soft-paper',
    canvasColor: '#fff1f5',
    canvasImage: null,
    paperColor: '#fffaf7',
    layoutStyle: 'gallery',
    blockStyle: 'carded',
  },
]

export const BLOG_THEME_PRESET_META: Record<string, { description: string; bestFor: string; rhythm: string }> = {
  蓝墨手稿: {
    description: '冷静、清透、适合长文和技术随笔。',
    bestFor: '教程 / 复盘 / 观点文',
    rhythm: '杂志式留白',
  },
  松石画布: {
    description: '柔和、轻快，像一块能贴作品截图的灵感板。',
    bestFor: '作品集 / 灵感 / 视觉记录',
    rhythm: '画廊式浏览',
  },
  铅字笔记: {
    description: '带一点纸张纹理和打字机感，强调记录和推敲。',
    bestFor: '日记 / 读书笔记 / 草稿复盘',
    rhythm: '手账式阅读',
  },
  夜航终端: {
    description: '深色、专注、有代码感，适合工程和实验内容。',
    bestFor: '源码解析 / 实验 / 工程日志',
    rhythm: '终端式聚焦',
  },
  春日杂志: {
    description: '明亮、有 editorial 气质，适合生活化创作和专栏。',
    bestFor: '专栏 / 影像 / 生活观察',
    rhythm: '杂志式卡片',
  },
}

const FONT_STACKS: Record<BlogFontPreset, { body: string; heading: string; mono: string }> = {
  'literary-serif': {
    body: '"Noto Serif SC", Georgia, "Times New Roman", serif',
    heading: '"Noto Serif SC", Georgia, "Times New Roman", serif',
    mono: '"Cascadia Code", "SFMono-Regular", Consolas, monospace',
  },
  'neo-grotesk': {
    body: '"Aptos", "Segoe UI", "Microsoft YaHei", sans-serif',
    heading: '"Aptos Display", "Segoe UI", "Microsoft YaHei", sans-serif',
    mono: '"Cascadia Code", "SFMono-Regular", Consolas, monospace',
  },
  'rounded-sans': {
    body: '"Nunito", "Microsoft YaHei UI", "Microsoft YaHei", sans-serif',
    heading: '"Nunito", "Microsoft YaHei UI", "Microsoft YaHei", sans-serif',
    mono: '"Cascadia Code", "SFMono-Regular", Consolas, monospace',
  },
  'mono-editor': {
    body: '"Cascadia Code", "SFMono-Regular", Consolas, monospace',
    heading: '"Cascadia Code", "SFMono-Regular", Consolas, monospace',
    mono: '"Cascadia Code", "SFMono-Regular", Consolas, monospace',
  },
}

const LAYOUT_TOKENS: Record<BlogLayoutStyle, Record<string, string>> = {
  editorial: {
    '--blog-page-width': '1440px',
    '--blog-reading-width': '820px',
    '--blog-card-gap': '24px',
    '--blog-hero-min': '420px',
    '--blog-sidebar-width': '320px',
  },
  notebook: {
    '--blog-page-width': '1120px',
    '--blog-reading-width': '760px',
    '--blog-card-gap': '18px',
    '--blog-hero-min': '300px',
    '--blog-sidebar-width': '280px',
  },
  gallery: {
    '--blog-page-width': '1500px',
    '--blog-reading-width': '860px',
    '--blog-card-gap': '18px',
    '--blog-hero-min': '560px',
    '--blog-sidebar-width': '260px',
  },
}

const BLOCK_TOKENS: Record<BlogBlockStyle, Record<string, string>> = {
  quiet: {
    '--blog-block-border': 'color-mix(in srgb, var(--blog-line) 76%, transparent)',
    '--blog-block-bg': 'color-mix(in srgb, var(--blog-paper) 92%, transparent)',
    '--blog-block-shadow': '0 12px 30px rgba(17, 24, 39, 0.05)',
  },
  ink: {
    '--blog-block-border': 'color-mix(in srgb, var(--blog-title) 30%, transparent)',
    '--blog-block-bg': 'transparent',
    '--blog-block-shadow': 'none',
  },
  carded: {
    '--blog-block-border': 'color-mix(in srgb, var(--blog-accent) 24%, transparent)',
    '--blog-block-bg': 'color-mix(in srgb, var(--blog-paper) 88%, transparent)',
    '--blog-block-shadow': '0 18px 45px rgba(17, 24, 39, 0.08)',
  },
}

function colorLuminance(value: string): number {
  const hex = value.trim().replace('#', '')
  if (!/^[0-9a-fA-F]{6}$/.test(hex)) return 1
  const channels = [0, 2, 4].map((index) => {
    const channel = Number.parseInt(hex.slice(index, index + 2), 16) / 255
    return channel <= 0.03928 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4
  })
  return 0.2126 * (channels[0] ?? 1) + 0.7152 * (channels[1] ?? 1) + 0.0722 * (channels[2] ?? 1)
}

function themeProfile(theme: BlogThemeConfig) {
  const paperLuminance = colorLuminance(theme.paperColor)
  const canvasLuminance = colorLuminance(theme.canvasColor)
  const isDark = paperLuminance < 0.18 || canvasLuminance < 0.18
  const isNotebook = theme.layoutStyle === 'notebook'
  const isGallery = theme.layoutStyle === 'gallery'
  const isCarded = theme.blockStyle === 'carded'
  const isInk = theme.blockStyle === 'ink'
  const isMono = theme.fontPreset === 'mono-editor'
  const radius = isNotebook || isMono ? '6px' : isGallery || isCarded ? '16px' : '10px'
  const radiusLg = isNotebook || isMono ? '10px' : isGallery || isCarded ? '24px' : '18px'

  if (isDark) {
    return {
      radius,
      radiusLg,
      shadow: '0 22px 70px rgba(0, 0, 0, 0.34)',
      muted: 'color-mix(in srgb, var(--blog-body) 82%, #ffffff)',
      line: 'color-mix(in srgb, var(--blog-accent) 30%, #0f172a)',
      card: 'color-mix(in srgb, var(--blog-paper) 86%, #0f172a)',
      cardStrong: 'color-mix(in srgb, var(--blog-paper) 72%, var(--blog-accent))',
      chip: 'color-mix(in srgb, var(--blog-accent) 18%, var(--blog-paper))',
      codeBg: '#020817',
      codeText: 'color-mix(in srgb, var(--blog-accent) 38%, #ffffff)',
      heroOverlay: 'linear-gradient(135deg, rgba(2, 8, 23, 0.94), rgba(8, 47, 73, 0.72))',
      coverGradient: 'linear-gradient(135deg, var(--blog-paper), var(--blog-accent))',
      patternOpacity: isInk ? '0.42' : '0.3',
      surfaceBlur: '18px',
      headingWeight: isMono ? '760' : '820',
    }
  }

  return {
    radius,
    radiusLg,
    shadow: isCarded
      ? '0 24px 60px color-mix(in srgb, var(--blog-accent) 14%, transparent)'
      : '0 22px 58px color-mix(in srgb, var(--blog-accent) 10%, transparent)',
    muted: 'color-mix(in srgb, var(--blog-body) 76%, var(--blog-paper))',
    line: 'color-mix(in srgb, var(--blog-accent) 18%, #d4d4d8)',
    card: 'color-mix(in srgb, var(--blog-paper) 82%, var(--blog-canvas))',
    cardStrong: 'color-mix(in srgb, var(--blog-paper) 96%, #ffffff)',
    chip: 'color-mix(in srgb, var(--blog-accent) 12%, var(--blog-paper))',
    codeBg: 'color-mix(in srgb, var(--blog-accent) 10%, var(--blog-paper))',
    codeText: 'var(--blog-accent)',
    heroOverlay: 'linear-gradient(135deg, color-mix(in srgb, var(--blog-title) 84%, transparent), color-mix(in srgb, var(--blog-accent) 46%, transparent))',
    coverGradient: 'linear-gradient(135deg, color-mix(in srgb, var(--blog-accent) 12%, var(--blog-paper)), color-mix(in srgb, var(--blog-accent) 52%, var(--blog-canvas)))',
    patternOpacity: isNotebook || isInk ? '0.3' : '0.18',
    surfaceBlur: isNotebook ? '0px' : '16px',
    headingWeight: isMono || isInk ? '760' : '820',
  }
}

export function normalizeBlogTheme(theme?: BlogThemeConfig | null): BlogThemeConfig {
  return {
    ...DEFAULT_BLOG_THEME,
    ...(theme ?? {}),
  }
}

export function blogThemeToStyle(theme?: BlogThemeConfig | null): Record<string, string> {
  const normalized = normalizeBlogTheme(theme)
  const fonts = FONT_STACKS[normalized.fontPreset] ?? FONT_STACKS['literary-serif']
  const profile = themeProfile(normalized)
  return {
    '--blog-font': fonts.body,
    '--blog-font-body': fonts.body,
    '--blog-font-heading': fonts.heading,
    '--blog-font-mono': fonts.mono,
    '--blog-accent': normalized.accentColor,
    '--blog-title': normalized.titleColor,
    '--blog-body': normalized.bodyColor,
    '--blog-canvas': normalized.canvasColor,
    '--blog-paper': normalized.paperColor,
    '--blog-canvas-image': toCssImageUrl(normalized.canvasImage),
    '--blog-muted': profile.muted,
    '--blog-line': profile.line,
    '--blog-card': profile.card,
    '--blog-card-strong': profile.cardStrong,
    '--blog-chip': profile.chip,
    '--blog-shadow': profile.shadow,
    '--blog-radius': profile.radius,
    '--blog-radius-lg': profile.radiusLg,
    '--blog-code-bg': profile.codeBg,
    '--blog-code-text': profile.codeText,
    '--blog-hero-overlay': profile.heroOverlay,
    '--blog-cover-gradient': profile.coverGradient,
    '--blog-pattern-opacity': profile.patternOpacity,
    '--blog-surface-blur': profile.surfaceBlur,
    '--blog-heading-weight': profile.headingWeight,
    '--blog-body-size': normalized.fontPreset === 'mono-editor' ? '15px' : '16px',
    '--blog-body-line': normalized.layoutStyle === 'notebook' ? '1.92' : '1.82',
    ...LAYOUT_TOKENS[normalized.layoutStyle],
    ...BLOCK_TOKENS[normalized.blockStyle],
  }
}
