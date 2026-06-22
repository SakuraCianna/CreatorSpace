<template>
  <section ref="root" class="about-page">
    <header class="about-hero page-hero page-hero--about" data-reveal>
      <h1>关于我</h1>
    </header>

    <div class="about-grid">
      <article v-if="profile" class="about-panel about-panel--bio" data-reveal>
        <UserRound :size="22" />
        <h2>{{ profile.displayName }}</h2>
        <p>{{ profile.bio }}</p>
      </article>
      <article v-else class="about-panel about-panel--bio" data-reveal>
        <UserRound :size="22" />
        <h2>暂无公开资料</h2>
        <p>请在后台维护站点资料后刷新页面。</p>
      </article>
      <article v-if="contactLinks.length" class="about-panel" data-reveal>
        <Mail :size="22" />
        <h2>联系入口</h2>
        <p>欢迎从文章或作品进入，也可以通过公开联系方式继续交流。</p>
        <div class="detail-actions">
          <a
            v-for="link in contactLinks"
            :key="link.url"
            class="button"
            :class="link.platform === 'github' ? 'button-filled' : 'button-tonal'"
            :href="link.url"
            target="_blank"
            rel="noreferrer"
          >
            {{ link.label }}
          </a>
        </div>
      </article>
    </div>

    <section v-if="skills.length" class="skills-band" data-reveal>
      <div>
        <p class="page-kicker">Toolbox</p>
        <h2>技术栈与表达方式</h2>
      </div>
      <div class="skill-cloud">
        <span v-for="skill in skills" :key="skill">{{ skill }}</span>
      </div>
    </section>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Mail, UserRound } from '@lucide/vue'

import { fetchSiteConfig } from '@/services/content'
import { useCinematicPageMotion } from '@/shared/composables/useCinematicPageMotion'
import { usePageReveal } from '@/shared/composables/usePageReveal'

interface AboutProfile {
  displayName: string
  headline: string
  bio: string
}

interface ContactLink {
  platform: string
  label: string
  url: string
}

const root = ref<HTMLElement | null>(null)
const cinematic = useCinematicPageMotion(root)
const profile = ref<AboutProfile | null>(null)
const contactLinks = ref<ContactLink[]>([])

usePageReveal(root)

const skills = ref<string[]>([])

async function loadSiteProfile() {
  try {
    const config = await fetchSiteConfig()
    const siteProfile = readRecord(config['site.profile.active'])
    const profileJson = readRecord(siteProfile.profileJson)
    const socialLinks = readArray(config['site.socialLinks'])
      .map(readContactLink)
      .filter((link): link is ContactLink => Boolean(link))

    profile.value = {
      displayName: readString(siteProfile.displayName) || '未命名资料',
      headline: readString(siteProfile.headline),
      bio: readString(siteProfile.bio),
    }
    if (socialLinks.length) {
      contactLinks.value = socialLinks
    }
    const focus = readStringArray(profileJson.focus)
    if (focus.length) {
      skills.value = focus.slice(0, 12)
    }
  } catch {
    profile.value = null
    contactLinks.value = []
    skills.value = []
  } finally {
    void cinematic.play()
  }
}

function readRecord(value: unknown): Record<string, unknown> {
  return value && typeof value === 'object' && !Array.isArray(value) ? value as Record<string, unknown> : {}
}

function readArray(value: unknown): unknown[] {
  return Array.isArray(value) ? value : []
}

function readString(value: unknown): string {
  return typeof value === 'string' ? value : ''
}

function readStringArray(value: unknown): string[] {
  return Array.isArray(value) ? value.filter((item): item is string => typeof item === 'string') : []
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

onMounted(loadSiteProfile)
</script>

<style scoped>
.about-page {
  padding: 46px 0 84px;
}

.about-hero {
  display: grid;
  grid-template-columns: 1fr;
  align-items: center;
  gap: 22px;
  min-height: clamp(220px, 23vw, 292px);
  padding: clamp(24px, 3vw, 42px);
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.88), rgba(255, 255, 255, 0.58)),
    radial-gradient(circle at 16% 20%, color-mix(in srgb, var(--hero-accent) 18%, transparent), transparent 34%),
    radial-gradient(circle at 88% 18%, color-mix(in srgb, var(--hero-accent-2) 18%, transparent), transparent 28%),
    linear-gradient(120deg, rgba(49, 91, 255, 0.08), rgba(194, 95, 58, 0.08), rgba(0, 124, 114, 0.1));
  box-shadow: var(--tone-shadow);
  isolation: isolate;
  overflow: hidden;
  position: relative;
}

.page-hero {
  --hero-accent: #1b1b1f;
  --hero-accent-2: #0b57d0;
  --hero-mark: "05";
}

.page-hero::before {
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
  opacity: 0.9;
  pointer-events: none;
}

.page-hero::after {
  content: var(--hero-mark);
  position: absolute;
  top: clamp(16px, 4vw, 40px);
  right: clamp(24px, 6vw, 88px);
  z-index: 0;
  color: color-mix(in srgb, var(--hero-accent) 16%, transparent);
  font-size: clamp(86px, 12vw, 180px);
  font-weight: 900;
  line-height: 0.8;
  pointer-events: none;
}

.page-hero > * {
  position: relative;
  z-index: 1;
}

.page-hero h1 {
  position: relative;
  display: inline-block;
  justify-self: start;
  width: fit-content;
  max-width: 100%;
  padding-top: 24px;
  text-shadow: 0 16px 34px rgba(20, 21, 29, 0.1);
}

.page-hero h1::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: clamp(110px, 18vw, 240px);
  height: 8px;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--hero-accent), var(--hero-accent-2), transparent);
  box-shadow: 0 10px 28px color-mix(in srgb, var(--hero-accent) 22%, transparent);
}

.page-hero h1::after {
  content: "";
  position: absolute;
  top: -2px;
  right: -54px;
  width: 34px;
  height: 14px;
  border: 1px solid color-mix(in srgb, var(--hero-accent) 42%, transparent);
  border-radius: 999px;
  background: color-mix(in srgb, var(--hero-accent-2) 18%, transparent);
  box-shadow: 18px 0 0 color-mix(in srgb, var(--hero-accent) 18%, transparent);
}

.about-hero h1 {
  max-width: 830px;
  margin: 0;
  color: var(--tone-ink);
  font-size: 44px;
  font-weight: 860;
  line-height: 1.08;
}

.about-hero p:not(.page-kicker) {
  max-width: 680px;
  color: var(--tone-muted);
  font-size: 17px;
  line-height: 1.72;
}

.about-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: 1.1fr 0.9fr;
  margin-top: 18px;
}

.about-panel {
  position: relative;
  display: grid;
  gap: 12px;
  padding: 22px;
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm);
  background: var(--tone-panel);
  box-shadow: var(--tone-shadow);
  backdrop-filter: blur(18px);
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

.about-panel svg,
.rules-grid--cards svg {
  color: var(--tone-primary);
}

.about-panel--bio {
  grid-row: span 2;
  min-height: 320px;
  align-content: end;
  background:
    linear-gradient(145deg, rgba(20, 21, 29, 0.86), rgba(0, 124, 114, 0.7)),
    var(--tone-night);
  color: #fff;
}

.about-panel--bio h2,
.about-panel--bio p,
.about-panel--bio svg {
  color: #fff;
}

.skills-band {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-top: 18px;
  padding: 24px;
  border: 1px solid var(--tone-line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.74);
  box-shadow: var(--tone-shadow);
}

.skills-band h2 {
  margin: 8px 0 0;
  font-size: 28px;
}

.skill-cloud {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.skill-cloud span {
  padding: 6px 10px;
  border-radius: 6px;
  background: rgba(0, 124, 114, 0.1);
  color: #055f57;
  font-size: 12px;
  font-weight: 740;
}

@media (max-width: 1020px) {
  .about-hero {
    grid-template-columns: 1fr;
  }

  .about-grid {
    grid-template-columns: 1fr;
  }

  .skills-band {
    display: grid;
    grid-template-columns: 1fr;
  }

  .skill-cloud {
    justify-content: flex-start;
  }
}

@media (min-width: 761px) {
  .about-hero h1 {
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

@media (max-width: 760px) {
  .about-page {
    padding-top: 26px;
  }

  .about-hero {
    padding: 22px;
  }

  .about-hero h1 {
    font-size: 32px;
  }
}

@media (max-width: 520px) {
  .about-hero h1 {
    font-size: 28px;
  }
}
</style>
