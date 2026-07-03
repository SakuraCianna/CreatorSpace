import type {
  BlogBlockStyle,
  BlogCanvasType,
  BlogFontPreset,
  BlogLayoutStyle,
  BlogThemeConfig,
} from './domain'
import { toCssImageUrl } from './cssImage'

export const DEFAULT_BLOG_THEME: BlogThemeConfig = {
  displayName: '我的主题',
  fontPreset: 'literary-serif',
  accentColor: '#2563eb',
  titleColor: '#111827',
  bodyColor: '#374151',
  canvasType: 'soft-paper',
  canvasColor: '#f8fafc',
  canvasImage: null,
  paperColor: '#ffffff',
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
    accentColor: '#2563eb',
    titleColor: '#111827',
    bodyColor: '#374151',
    canvasType: 'soft-paper',
    canvasColor: '#f8fafc',
    canvasImage: null,
    paperColor: '#ffffff',
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
]

const FONT_STACKS: Record<BlogFontPreset, string> = {
  'literary-serif': 'Georgia, "Times New Roman", "Noto Serif SC", serif',
  'neo-grotesk': '"Aptos", "Segoe UI", "Microsoft YaHei", sans-serif',
  'rounded-sans': '"Nunito", "Microsoft YaHei UI", "Microsoft YaHei", sans-serif',
  'mono-editor': '"Cascadia Code", "SFMono-Regular", Consolas, monospace',
}

export function normalizeBlogTheme(theme?: BlogThemeConfig | null): BlogThemeConfig {
  return {
    ...DEFAULT_BLOG_THEME,
    ...(theme ?? {}),
  }
}

export function blogThemeToStyle(theme?: BlogThemeConfig | null): Record<string, string> {
  const normalized = normalizeBlogTheme(theme)
  return {
    '--blog-font': FONT_STACKS[normalized.fontPreset] ?? FONT_STACKS['literary-serif'],
    '--blog-accent': normalized.accentColor,
    '--blog-title': normalized.titleColor,
    '--blog-body': normalized.bodyColor,
    '--blog-canvas': normalized.canvasColor,
    '--blog-paper': normalized.paperColor,
    '--blog-canvas-image': toCssImageUrl(normalized.canvasImage),
  }
}
