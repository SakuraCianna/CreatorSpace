<template>
  <section ref="root" class="about-page">
    <header class="about-hero page-hero" data-reveal>
      <div class="hero-copy">
        <p class="page-kicker">About Creator</p>
        <h1>{{ profile?.displayName || siteName }}</h1>
        <p>{{ profile?.headline || siteSlogan }}</p>
      </div>
      <div class="profile-card">
        <div class="profile-avatar">
          <img v-if="profile?.avatarUrl" :src="profile.avatarUrl" alt="" loading="lazy" />
          <UserRound v-else :size="32" />
        </div>
        <strong>{{ profile?.displayName || '暂无公开资料' }}</strong>
        <span>{{ profile?.location || 'CreatorSpace' }}</span>
      </div>
    </header>

    <section class="about-grid">
      <article class="about-panel about-panel--bio" data-reveal>
        <UserRound :size="22" />
        <h2>{{ profile?.displayName || '公开资料' }}</h2>
        <p>{{ profile?.bio || '请在后台维护站点资料后刷新页面。' }}</p>
        <div v-if="focusTags.length" class="skill-cloud">
          <span v-for="tag in focusTags" :key="tag">{{ tag }}</span>
        </div>
      </article>

      <article class="about-panel" data-reveal>
        <ShieldCheck :size="22" />
        <h2>内容边界</h2>
        <p>{{ policySummary }}</p>
        <div class="role-list">
          <span><BookOpen :size="15" />文章</span>
          <span><Images :size="15" />作品</span>
          <span><Lightbulb :size="15" />灵感</span>
        </div>
      </article>

      <article class="about-panel" data-reveal>
        <Palette :size="22" />
        <h2>主题体验</h2>
        <p>{{ themeSummary }}</p>
        <RouterLink class="text-link" :to="{ name: 'themes' }">
          查看主题展厅
          <ArrowRight :size="15" />
        </RouterLink>
      </article>

      <article v-if="contactLinks.length" class="about-panel about-panel--contact" data-reveal>
        <Mail :size="22" />
        <h2>联系入口</h2>
        <p>公开联系方式来自后台站点配置。</p>
        <div class="contact-actions">
          <a
            v-for="link in contactLinks"
            :key="link.url"
            class="button"
            :class="link.platform === 'github' ? 'button-filled' : 'button-tonal'"
            :href="link.url"
            target="_blank"
            rel="noreferrer"
          >
            <ExternalLink :size="15" />
            {{ link.label }}
          </a>
        </div>
      </article>
    </section>

    <section class="workflow-band" data-reveal>
      <div>
        <p class="page-kicker">Creator Flow</p>
        <h2>从碎片到公开展品</h2>
      </div>
      <ol>
        <li v-for="step in workflowSteps" :key="step.title">
          <span>{{ step.index }}</span>
          <strong>{{ step.title }}</strong>
          <p>{{ step.body }}</p>
        </li>
      </ol>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
  ArrowRight,
  BookOpen,
  ExternalLink,
  Images,
  Lightbulb,
  Mail,
  Palette,
  ShieldCheck,
  UserRound,
} from '@lucide/vue'

import { fetchSiteConfig, fetchThemes } from '@/services/content'
import { useCinematicPageMotion } from '@/shared/composables/useCinematicPageMotion'
import { usePageReveal } from '@/shared/composables/usePageReveal'
import { DEFAULT_SITE_IDENTITY, syncSiteIdentityFromConfig, useSiteIdentity } from '@/shared/siteIdentity'
import type { PublicThemeConfig } from '@/shared/domain'

interface AboutProfile {
  displayName: string
  headline: string
  bio: string
  avatarUrl: string
  location: string
  profileJson: Record<string, unknown>
}

interface ContactLink {
  platform: string
  label: string
  url: string
}

interface WorkflowStep {
  index: string
  title: string
  body: string
}

const root = ref<HTMLElement | null>(null)
const cinematic = useCinematicPageMotion(root)
const profile = ref<AboutProfile | null>(null)
const contactLinks = ref<ContactLink[]>([])
const themes = ref<PublicThemeConfig[]>([])
const aboutPage = ref<Record<string, unknown>>({})
const { siteName, siteSlogan } = useSiteIdentity({ load: false })

usePageReveal(root)

const focusTags = computed(() => readStringArray(profile.value?.profileJson.focus).slice(0, 12))
const activeTheme = computed(() => themes.value.find((theme) => theme.active) ?? themes.value[0] ?? null)
const themeSummary = computed(() => {
  if (!activeTheme.value) {
    return '主题列表暂未公开。'
  }
  const config = readRecord(activeTheme.value.config)
  const mood = readString(config.mood)
  return `${activeTheme.value.displayName} · ${mood || activeTheme.value.layoutType}`
})
const policySummary = computed(() => {
  return readString(aboutPage.value.seoDescription)
    || '提交后由站点规则决定审核与公开范围，公开页只展示已经允许展示的内容。'
})
const workflowSteps = computed<WorkflowStep[]>(() => [
  { index: '01', title: '记录灵感', body: '把摘句、Prompt、链接和图片先放到灵感墙。' },
  { index: '02', title: '形成内容', body: '把灵感沉淀成文章，或整理成可展示的作品档案。' },
  { index: '03', title: '审核公开', body: policySummary.value },
])

async function loadAbout() {
  const [configResult, themesResult] = await Promise.allSettled([fetchSiteConfig(), fetchThemes()])
  if (configResult.status === 'fulfilled') {
    const config = configResult.value
    syncSiteIdentityFromConfig(config)
    profile.value = readProfile(config['site.profile.active'])
    contactLinks.value = readArray(config['site.socialLinks'])
      .map(readContactLink)
      .filter((link): link is ContactLink => Boolean(link))
    aboutPage.value = readRecord(config['page.about'])
  } else {
    profile.value = null
    contactLinks.value = []
    aboutPage.value = {}
  }
  themes.value = themesResult.status === 'fulfilled' ? themesResult.value : []
  void cinematic.play()
}

// 把公开站点配置收敛成页面可直接消费的字段，后端新增字段时不会破坏页面。
function readProfile(value: unknown): AboutProfile | null {
  const record = readRecord(value)
  if (Object.keys(record).length === 0) {
    return null
  }
  return {
    displayName: readString(record.displayName) || DEFAULT_SITE_IDENTITY.name,
    headline: readString(record.headline) || DEFAULT_SITE_IDENTITY.slogan,
    bio: readString(record.bio),
    avatarUrl: safeAssetUrl(readString(record.avatarUrl)),
    location: readString(record.location),
    profileJson: readRecord(record.profileJson),
  }
}

function readContactLink(value: unknown): ContactLink | null {
  const record = readRecord(value)
  const url = safeContactUrl(readString(record.url))
  if (!url) {
    return null
  }
  return {
    platform: readString(record.platform),
    label: readString(record.label) || url,
    url,
  }
}

function readRecord(value: unknown): Record<string, unknown> {
  return value && typeof value === 'object' && !Array.isArray(value) ? value as Record<string, unknown> : {}
}

function readArray(value: unknown): unknown[] {
  return Array.isArray(value) ? value : []
}

function readString(value: unknown): string {
  return typeof value === 'string' ? value.trim() : ''
}

function readStringArray(value: unknown): string[] {
  return Array.isArray(value) ? value.filter((item): item is string => typeof item === 'string') : []
}

function safeAssetUrl(value: string): string {
  if (!value || value.startsWith('/')) {
    return value
  }
  return safeContactUrl(value)
}

function safeContactUrl(value: string): string {
  if (value.startsWith('mailto:')) {
    return value
  }
  try {
    const url = new URL(value)
    return ['http:', 'https:'].includes(url.protocol) ? url.toString() : ''
  } catch {
    return ''
  }
}

onMounted(loadAbout)
</script>

<style scoped>
.about-page {
  display: grid;
  gap: 18px;
  padding: 46px 0 84px;
}

.about-hero {
  --hero-accent: #1b1b1f;
  --hero-accent-2: #0b57d0;
  --hero-mark: "05";
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
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

.about-hero::before {
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

.about-hero::after {
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

.about-hero > * {
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

.profile-card,
.about-panel,
.workflow-band {
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm);
  background: var(--tone-panel);
  box-shadow: var(--tone-shadow);
  backdrop-filter: blur(18px);
}

.profile-card {
  display: grid;
  gap: 8px;
  justify-items: center;
  padding: 22px;
  text-align: center;
}

.profile-avatar {
  display: grid;
  width: 86px;
  height: 86px;
  place-items: center;
  overflow: hidden;
  border: 1px solid color-mix(in srgb, var(--tone-primary) 24%, transparent);
  border-radius: 18px;
  background: rgba(49, 91, 255, 0.08);
  color: var(--tone-primary);
}

.profile-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-card strong {
  color: var(--tone-ink);
  font-size: 18px;
}

.profile-card span {
  color: var(--tone-muted);
  font-size: 13px;
}

.about-grid {
  display: grid;
  grid-template-columns: 1.08fr 0.92fr;
  gap: 16px;
}

.about-panel {
  position: relative;
  display: grid;
  gap: 12px;
  min-height: 220px;
  padding: 22px;
  overflow: hidden;
}

.about-panel::after {
  content: "";
  position: absolute;
  inset: auto 16px 12px auto;
  width: 90px;
  height: 2px;
  background: linear-gradient(90deg, transparent, color-mix(in srgb, var(--tone-primary) 54%, transparent));
}

.about-panel svg {
  color: var(--tone-primary);
}

.about-panel h2,
.workflow-band h2 {
  margin: 0;
  color: var(--tone-ink);
}

.about-panel p,
.workflow-band p {
  margin: 0;
  color: var(--tone-muted);
  line-height: 1.68;
}

.about-panel--bio {
  grid-row: span 2;
  min-height: 360px;
  align-content: end;
  background:
    linear-gradient(145deg, rgba(20, 21, 29, 0.86), rgba(0, 124, 114, 0.72)),
    var(--tone-night);
  color: #fff;
}

.about-panel--bio h2,
.about-panel--bio p,
.about-panel--bio svg {
  color: #fff;
}

.about-panel--contact {
  min-height: 190px;
}

.skill-cloud,
.role-list,
.contact-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.skill-cloud span,
.role-list span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.14);
  color: #fff;
  font-size: 12px;
  font-weight: 740;
}

.role-list span {
  background: rgba(0, 124, 114, 0.1);
  color: #055f57;
}

.workflow-band {
  display: grid;
  grid-template-columns: 0.58fr minmax(0, 1fr);
  gap: 22px;
  padding: 24px;
}

.workflow-band ol {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.workflow-band li {
  display: grid;
  gap: 8px;
  padding: 16px;
  border: 1px solid var(--tone-line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.62);
}

.workflow-band li span {
  color: var(--tone-faint);
  font-size: 12px;
  font-weight: 860;
}

.workflow-band li strong {
  color: var(--tone-ink);
}

.workflow-band li p {
  font-size: 13px;
}

@media (max-width: 1020px) {
  .about-hero,
  .about-grid,
  .workflow-band,
  .workflow-band ol {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .about-page {
    padding-top: 26px;
  }

  .about-hero {
    padding: 22px;
  }
}
</style>
