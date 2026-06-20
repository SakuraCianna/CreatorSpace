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
import { useCinematicPageMotion } from '@/shared/composables/useCinematicPageMotion'
import { usePageReveal } from '@/shared/composables/usePageReveal'
import { toCssImageUrl } from '@/shared/cssImage'
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
const cinematic = useCinematicPageMotion(root)

usePageReveal(root)

const canComment = computed(() => Boolean(session.accessToken))
const htmlContent = computed(() => renderSafeMarkdown(project.value?.contentMarkdown ?? project.value?.description))
const safeDemoUrl = computed(() => safeExternalUrl(project.value?.demoUrl))
const safeGithubUrl = computed(() => safeExternalUrl(project.value?.githubUrl))
const projectCoverStyle = computed(() => ({
  '--detail-accent': project.value?.tags[0]?.color ?? '#b18cff',
  '--detail-cover': toCssImageUrl(project.value?.coverUrl),
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
  comments.value = []
  commentNotice.value = ''
  try {
    project.value = await fetchProjectBySlug(slug.value)
    await loadComments()
  } catch (error) {
    project.value = fallbackProjects.find((item) => item.slug === slug.value) ?? null
    notice.value = error instanceof Error ? `后端暂不可用，已尝试本地样例：${error.message}` : '后端暂不可用，已尝试本地样例。'
  } finally {
    isLoading.value = false
    void cinematic.play()
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

<style scoped>
.detail-page {
  display: grid;
  gap: 18px;
  padding: 46px 0 84px;
}

.detail-back {
  justify-self: start;
}

.project-record {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 18px;
  align-items: start;
}

.detail-panel {
  position: relative;
  overflow: hidden;
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(255, 255, 255, 0.78)),
    rgba(255, 255, 255, 0.88);
  box-shadow: var(--tone-shadow);
  backdrop-filter: blur(22px);
}

.detail-panel::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    linear-gradient(120deg, color-mix(in srgb, var(--detail-accent, var(--tone-primary)) 28%, transparent), transparent 34%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.2), transparent 22%);
  opacity: 0.72;
}

.detail-panel > * {
  position: relative;
  z-index: 1;
}

.detail-hero {
  position: relative;
  display: grid;
  gap: 18px;
  padding: 48px;
  background:
    linear-gradient(135deg, rgba(6, 8, 18, 0.96), rgba(6, 8, 18, 0.84) 48%, rgba(6, 8, 18, 0.7)),
    var(--detail-cover),
    linear-gradient(135deg, var(--detail-accent), #10131f);
  background-size: cover;
  background-position: center;
  color: #f8fafc;
  overflow: hidden;
}

.detail-hero::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at 18% 12%, rgba(255, 255, 255, 0.2), transparent 26%),
    linear-gradient(180deg, transparent, rgba(6, 8, 18, 0.32));
  z-index: 0;
}

.detail-hero::after {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    linear-gradient(90deg, rgba(255, 255, 255, 0.16), transparent 22%, rgba(255, 255, 255, 0.08) 48%, transparent 72%),
    repeating-linear-gradient(90deg, rgba(255, 255, 255, 0.06) 0 1px, transparent 1px 18px);
  mix-blend-mode: screen;
  opacity: 0.38;
}

.detail-hero > * {
  position: relative;
  z-index: 1;
}

.project-hero {
  grid-template-columns: minmax(0, 1fr) 260px;
  align-items: end;
}

.project-hero img {
  width: 260px;
  aspect-ratio: 4 / 3;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 8px;
  object-fit: cover;
}

.detail-hero h1 {
  max-width: 820px;
  margin: 0;
  font-size: 58px;
  font-weight: 860;
  line-height: 1.04;
  text-shadow: 0 16px 34px rgba(0, 0, 0, 0.42);
}

.detail-summary {
  max-width: 760px;
  margin: 0;
  color: rgba(248, 250, 252, 0.8);
  font-size: 18px;
  line-height: 1.74;
  text-shadow: 0 12px 28px rgba(0, 0, 0, 0.36);
}

.detail-hero .page-kicker,
.detail-hero .detail-meta,
.detail-hero .tag-row {
  text-shadow: 0 10px 24px rgba(0, 0, 0, 0.38);
}

.detail-hero .detail-meta span {
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(6, 8, 18, 0.42);
  color: rgba(248, 250, 252, 0.86);
}

.detail-hero .tag-row span {
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.14);
  color: #ffffff;
}

.project-stack {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 20px 48px 0;
}

.project-stack span {
  padding: 6px 10px;
  border-radius: 6px;
  background: rgba(0, 124, 114, 0.1);
  color: #055f57;
  font-size: 12px;
  font-weight: 740;
}

.detail-actions {
  padding: 0 48px;
}

.markdown-body {
  display: grid;
  gap: 18px;
  padding: 34px 48px 52px;
  color: #1f2937;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  max-width: 780px;
  margin: 14px 0 0;
  color: var(--tone-ink);
  line-height: 1.18;
}

.markdown-body :deep(h2) {
  font-size: 30px;
}

.markdown-body :deep(p),
.markdown-body :deep(li),
.markdown-body :deep(blockquote) {
  max-width: 780px;
  margin: 0;
  color: #475569;
  font-size: 16px;
  line-height: 1.88;
}

.markdown-body :deep(blockquote) {
  padding: 14px 18px;
  border-left: 4px solid var(--tone-teal);
  background: rgba(0, 124, 114, 0.08);
}

.markdown-body :deep(pre) {
  max-width: 100%;
  overflow-x: auto;
  border-radius: 8px;
}

.process-panel {
  position: sticky;
  top: 100px;
  display: grid;
  gap: 12px;
}

.process-panel {
  padding: 18px;
}

.timeline-list {
  display: grid;
  gap: 14px;
  margin: 16px 0 0;
  padding: 0;
  list-style: none;
}

.timeline-list li {
  display: grid;
  gap: 6px;
  padding-left: 14px;
  border-left: 3px solid var(--tone-primary);
}

.timeline-list span {
  color: var(--tone-faint);
  font-size: 12px;
  font-weight: 800;
}
@media (max-width: 1020px) {
  .project-record {
    grid-template-columns: 1fr;
  }

  .process-panel {
    position: static;
  }
}

@media (max-width: 760px) {
  .detail-page {
    padding-top: 26px;
  }

  .detail-hero {
    padding: 22px;
  }

  .detail-hero h1 {
    font-size: 32px;
  }

  .project-hero {
    grid-template-columns: 1fr;
  }

  .project-hero img {
    width: 100%;
  }

  .project-stack,
  .detail-actions,
  .markdown-body {
    padding-right: 24px;
    padding-left: 24px;
  }
}

@media (max-width: 520px) {
  .detail-hero h1 {
    font-size: 28px;
  }
}
</style>
