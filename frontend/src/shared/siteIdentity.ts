import { computed, onMounted, ref, type ComputedRef, type Ref } from 'vue'

import { fetchSiteConfig } from '@/services/content'
import type { SiteConfigEntry, SiteSettings } from '@/shared/domain'

export interface SiteIdentity {
  name: string
  wordmark: string
  slogan: string
}

export const DEFAULT_SITE_IDENTITY: SiteIdentity = {
  name: 'CreatorSpace',
  wordmark: 'CreatorSpace',
  slogan: '创作者先行的主题博客与作品展厅',
}

interface SiteIdentityBindings {
  identity: Ref<SiteIdentity>
  siteName: ComputedRef<string>
  siteWordmark: ComputedRef<string>
  siteSlogan: ComputedRef<string>
}

interface UseSiteIdentityOptions {
  load?: boolean
}

const currentSiteIdentity = ref<SiteIdentity>({ ...DEFAULT_SITE_IDENTITY })
let siteIdentityRequest: Promise<SiteIdentity> | null = null

// 用于在组件内获取站点标识的组合式钩子
// 站点身份品牌标志全局仓储, 同步更新前台展示的标题、宣传语和 Logo 配置
export function useSiteIdentity(options: UseSiteIdentityOptions = {}): SiteIdentityBindings {
  if (options.load !== false) {
    onMounted(() => {
      void loadSiteIdentity()
    })
  }

  return {
    identity: currentSiteIdentity,
    siteName: computed(() => currentSiteIdentity.value.name),
    siteWordmark: computed(() => currentSiteIdentity.value.wordmark),
    siteSlogan: computed(() => currentSiteIdentity.value.slogan),
  }
}

export async function loadSiteIdentity(): Promise<SiteIdentity> {
  if (siteIdentityRequest) {
    return siteIdentityRequest
  }
  siteIdentityRequest = fetchAndSyncSiteIdentity()
  try {
    return await siteIdentityRequest
  } finally {
    siteIdentityRequest = null
  }
}

async function fetchAndSyncSiteIdentity(): Promise<SiteIdentity> {
  try {
    return syncSiteIdentityFromConfig(await fetchSiteConfig())
  } catch {
    applyDocumentSiteIdentity(currentSiteIdentity.value)
    return currentSiteIdentity.value
  }
}

// 从配置对象中同步站点标识信息并更新网页 document 标题
export function syncSiteIdentityFromConfig(config: Record<string, unknown>): SiteIdentity {
  const identity = resolveSiteIdentity(config)
  currentSiteIdentity.value = identity
  applyDocumentSiteIdentity(identity)
  return identity
}

export function syncSiteIdentityFromSettings(settings: SiteSettings): SiteIdentity {
  return syncSiteIdentityFromConfig({
    'site.identity': readIdentityConfig(settings.configs),
    'site.profile.active': settings.profile,
  })
}

export function resolveSiteIdentity(config: Record<string, unknown>): SiteIdentity {
  const identity = readRecord(config['site.identity'])
  const profile = readRecord(config['site.profile.active'])
  const profileJson = readRecord(profile.profileJson)
  const name = readString(identity.name)
    || readString(profile.displayName)
    || readString(identity.title)
    || DEFAULT_SITE_IDENTITY.name
  const wordmark = readString(identity.wordmark)
    || readString(profileJson.signature)
    || name
  const slogan = readString(identity.slogan)
    || readString(profile.headline)
    || DEFAULT_SITE_IDENTITY.slogan

  return {
    name,
    wordmark,
    slogan,
  }
}

export function siteAccountLabel(identity: SiteIdentity): string {
  return `${identity.name} Account`
}

export function siteCmsLabel(identity: SiteIdentity): string {
  return `${identity.name} CMS`
}

function readIdentityConfig(configs: SiteConfigEntry[]): Record<string, unknown> {
  return configs.find((config) => config.configKey === 'site.identity')?.configValue ?? {}
}

function readRecord(value: unknown): Record<string, unknown> {
  return value && typeof value === 'object' && !Array.isArray(value)
    ? value as Record<string, unknown>
    : {}
}

function readString(value: unknown): string {
  return typeof value === 'string' ? value.trim() : ''
}

function applyDocumentSiteIdentity(identity: SiteIdentity) {
  document.title = identity.name
  const description = document.querySelector<HTMLMetaElement>('meta[name="description"]')
  if (description) {
    description.content = identity.slogan ? `${identity.name} ${identity.slogan}` : identity.name
  }
}
