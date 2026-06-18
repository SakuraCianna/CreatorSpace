<template>
  <section ref="root" class="about-page">
    <header class="about-hero" data-reveal>
      <p class="page-kicker">About Creator</p>
      <h1>{{ profile.headline }}</h1>
      <p>{{ profile.bio }}</p>
    </header>

    <div class="about-grid">
      <article class="about-panel about-panel--bio" data-reveal>
        <UserRound :size="22" />
        <h2>{{ profile.displayName }}</h2>
        <p>{{ profile.bio }}</p>
      </article>
      <article class="about-panel" data-reveal>
        <PenTool :size="22" />
        <h2>写作方向</h2>
        <p>工程复盘、产品设计、AI 工作流、个人知识库和创作者工具。</p>
      </article>
      <article class="about-panel" data-reveal>
        <Sparkles :size="22" />
        <h2>作品方向</h2>
        <p>Web 应用、内容系统、沉浸式前台、后台 CMS、创意工具和动效实验。</p>
      </article>
      <article class="about-panel" data-reveal>
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

    <section class="skills-band" data-reveal>
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
import { Mail, PenTool, Sparkles, UserRound } from '@lucide/vue'

import { fetchSiteConfig } from '@/services/content'
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
const profile = ref<AboutProfile>({
  displayName: '个人介绍',
  headline: '这里展示的不是模板站点，而是一个长期整理创作的个人空间',
  bio: 'CreatorSpace 把文章、作品、灵感和后台工作台放在同一套内容系统里。前台负责表达气质，后台负责让它持续可维护。',
})
const contactLinks = ref<ContactLink[]>([
  { platform: 'github', label: 'GitHub', url: 'https://github.com/SakuraCianna/CreatorSpace' },
  { platform: 'mail', label: 'Mail', url: 'mailto:754515922@qq.com' },
])

usePageReveal(root)

const skills = ref([
  'Vue 3',
  'TypeScript',
  'Spring Boot',
  'PostgreSQL',
  'Redis',
  'GSAP',
  'anime.js',
  'Three.js',
  'Material Design 3',
  'Markdown',
])

async function loadSiteProfile() {
  try {
    const config = await fetchSiteConfig()
    const siteProfile = readRecord(config['site.profile.active'])
    const profileJson = readRecord(siteProfile.profileJson)
    const socialLinks = readArray(config['site.socialLinks'])
      .map(readContactLink)
      .filter((link): link is ContactLink => Boolean(link))

    profile.value = {
      displayName: readString(siteProfile.displayName) || profile.value.displayName,
      headline: readString(siteProfile.headline) || profile.value.headline,
      bio: readString(siteProfile.bio) || profile.value.bio,
    }
    if (socialLinks.length) {
      contactLinks.value = socialLinks
    }
    const focus = readStringArray(profileJson.focus)
    if (focus.length) {
      skills.value = [...focus, ...skills.value].slice(0, 12)
    }
  } catch {
    // 站点配置不可用时保留本地兜底文案。
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
