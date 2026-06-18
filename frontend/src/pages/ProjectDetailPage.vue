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
        <div class="comments-card">
          <p class="page-kicker">Comments</p>
          <form class="comment-form" @submit.prevent="postComment">
            <textarea v-model="commentDraft" rows="4" placeholder="对这个作品留下反馈，审核后公开展示。" />
            <button class="button button-filled button-compact" :disabled="!canComment" type="submit">
              {{ canComment ? '提交评论' : '登录后评论' }}
            </button>
          </form>
          <article v-for="comment in comments" :key="comment.id" class="comment-item">
            <strong>{{ comment.username }}</strong>
            <p>{{ comment.content }}</p>
            <span>{{ comment.createdAt ?? '刚刚' }}</span>
          </article>
          <span v-if="comments.length === 0">还没有公开评论。</span>
          <p v-if="commentNotice" class="inline-notice">{{ commentNotice }}</p>
        </div>
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
import { fetchComments, fetchProjectBySlug, submitComment } from '@/services/content'
import { usePageReveal } from '@/shared/composables/usePageReveal'
import type { CommentSummary, ProjectSummary } from '@/shared/domain'
import { renderSafeMarkdown } from '@/shared/markdown'
import { useSessionStore } from '@/shared/sessionStore'

const route = useRoute()
const root = ref<HTMLElement | null>(null)
const project = ref<ProjectSummary | null>(null)
const comments = ref<CommentSummary[]>([])
const commentDraft = ref('')
const commentNotice = ref('')
const isLoading = ref(true)
const notice = ref('')
const slug = computed(() => readRouteParam(route.params.slug))
const session = useSessionStore()

usePageReveal(root)

const canComment = computed(() => Boolean(session.accessToken))
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
    await loadComments()
  } catch (error) {
    project.value = fallbackProjects.find((item) => item.slug === slug.value) ?? null
    notice.value = error instanceof Error ? `后端暂不可用，已尝试本地样例：${error.message}` : '后端暂不可用，已尝试本地样例。'
  } finally {
    isLoading.value = false
  }
}

async function loadComments() {
  if (!project.value?.id) {
    comments.value = []
    return
  }
  try {
    const page = await fetchComments({ targetType: 'PROJECT', targetId: project.value.id, pageSize: 20 })
    comments.value = page.records
    commentNotice.value = ''
  } catch {
    comments.value = []
    commentNotice.value = '评论暂时不可用'
  }
}

async function postComment() {
  if (!canComment.value) {
    commentNotice.value = '请先登录账号再评论'
    return
  }
  if (!project.value?.id || !commentDraft.value.trim()) {
    commentNotice.value = '请填写评论内容'
    return
  }
  try {
    await submitComment({
      targetType: 'PROJECT',
      targetId: project.value.id,
      content: commentDraft.value.trim(),
    })
    commentDraft.value = ''
    commentNotice.value = '评论已提交，审核通过后会公开展示'
    await loadComments()
  } catch (error) {
    commentNotice.value = error instanceof Error ? error.message : '评论提交失败'
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
