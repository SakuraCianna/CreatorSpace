<template>
  <section ref="root" class="detail-page">
    <RouterLink class="detail-back text-link" :to="{ name: 'projects' }" data-reveal>
      <ArrowLeft :size="16" />
      返回作品展厅
    </RouterLink>

    <div v-if="isLoading" class="empty-state detail-state" data-reveal>
      <LoaderCircle class="spin" :size="24" />
      <h2>正在调取作品档案</h2>
    </div>
    <div v-else-if="project" class="project-record">
      <article class="detail-panel" data-reveal>
        <header class="detail-hero project-hero" :style="projectCoverStyle">
          <div>
            <p class="page-kicker">{{ project.projectType }}</p>
            <h1>{{ project.title }}</h1>
            <p v-if="project.description" class="detail-summary">{{ project.description }}</p>
            <div class="detail-meta">
              <span><Archive :size="15" />{{ project.status }}</span>
              <span v-if="project.recommended"><Sparkles :size="15" />推荐作品</span>
            </div>
          </div>
          <img v-if="project.coverUrl" :src="project.coverUrl" alt="" loading="lazy" />
        </header>

        <div class="project-stack">
          <span v-for="tech in project.techStack" :key="tech">{{ tech }}</span>
        </div>

        <div class="detail-actions">
          <a v-if="safeDemoUrl" class="button button-filled" :href="safeDemoUrl" target="_blank" rel="noreferrer">
            <ExternalLink :size="16" />
            查看演示
          </a>
          <a v-if="safeGithubUrl" class="button button-tonal" :href="safeGithubUrl" target="_blank" rel="noreferrer">
            <GitBranch :size="16" />
            查看代码
          </a>
        </div>

        <div class="markdown-body" v-html="htmlContent" />
      </article>

      <aside class="process-panel" data-reveal>
        <p class="page-kicker">Process Notes</p>
        <h2>作品过程</h2>
        <ol class="timeline-list">
          <li v-for="item in timeline" :key="item.title">
            <span>{{ item.phase }}</span>
            <strong>{{ item.title }}</strong>
            <p>{{ item.body }}</p>
          </li>
        </ol>
        <p v-if="notice" class="inline-notice">{{ notice }}</p>
      </aside>
    </div>
    <div v-else class="empty-state detail-state" data-reveal>
      <h2>没有找到这个作品</h2>
      <p>{{ notice || '它可能还没有公开展示。' }}</p>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import {
  Archive,
  ArrowLeft,
  ExternalLink,
  GitBranch,
  LoaderCircle,
  Sparkles,
} from '@lucide/vue'

import { fallbackProjects } from '@/content/studio'
import { fetchProjectBySlug } from '@/services/content'
import { usePageReveal } from '@/shared/composables/usePageReveal'
import type { ProjectSummary } from '@/shared/domain'
import { renderSafeMarkdown } from '@/shared/markdown'

const route = useRoute()
const root = ref<HTMLElement | null>(null)
const project = ref<ProjectSummary | null>(null)
const isLoading = ref(true)
const notice = ref('')
const slug = computed(() => readRouteParam(route.params.slug))

usePageReveal(root)

const htmlContent = computed(() => renderSafeMarkdown(project.value?.contentMarkdown ?? project.value?.description))
const safeDemoUrl = computed(() => safeExternalUrl(project.value?.demoUrl))
const safeGithubUrl = computed(() => safeExternalUrl(project.value?.githubUrl))
const projectCoverStyle = computed(() => ({
  '--detail-accent': project.value?.tags[0]?.color ?? '#b18cff',
  '--detail-cover': project.value?.coverUrl ? `url(${project.value.coverUrl})` : 'none',
}))
const timeline = computed(() => [
  { phase: '01', title: '问题定义', body: `${project.value?.title ?? '作品'} 先明确展示对象、读者和维护边界。` },
  { phase: '02', title: '视觉陈列', body: '补齐封面、技术标签和关键截图，让作品不像普通链接列表。' },
  { phase: '03', title: '过程归档', body: '把取舍、难点和下一步记录进 Markdown，方便之后复盘。' },
])

async function loadProject() {
  if (!slug.value) {
    project.value = null
    notice.value = '作品地址缺少 slug'
    isLoading.value = false
    return
  }

  isLoading.value = true
  notice.value = ''
  try {
    project.value = await fetchProjectBySlug(slug.value)
  } catch (error) {
    project.value = fallbackProjects.find((item) => item.slug === slug.value) ?? null
    notice.value = error instanceof Error ? `后端暂不可用，已尝试本地样例：${error.message}` : '后端暂不可用，已尝试本地样例。'
  } finally {
    isLoading.value = false
  }
}

function readRouteParam(value: string | string[] | undefined): string {
  if (Array.isArray(value)) {
    return value[0] ?? ''
  }
  return value ?? ''
}

function safeExternalUrl(value?: string | null): string {
  if (!value) {
    return ''
  }
  try {
    const url = new URL(value)
    return ['http:', 'https:'].includes(url.protocol) ? url.toString() : ''
  } catch {
    return ''
  }
}

onMounted(loadProject)
watch(slug, loadProject)
</script>
