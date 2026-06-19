<template>
  <section ref="root" class="detail-page">
    <RouterLink class="detail-back text-link" :to="{ name: 'articles' }" data-reveal>
      <ArrowLeft :size="16" />
      返回文章档案
    </RouterLink>

    <div v-if="isLoading" class="empty-state detail-state" data-reveal>
      <LoaderCircle class="spin" :size="24" />
      <h2>正在打开文章</h2>
    </div>
    <div v-else-if="article" class="reading-layout">
      <article class="detail-panel" data-reveal>
        <header class="detail-hero" :style="articleCoverStyle">
          <p class="page-kicker">{{ article.category?.name ?? 'Creator Journal' }}</p>
          <h1>{{ article.title }}</h1>
          <p v-if="article.summary" class="detail-summary">{{ article.summary }}</p>
          <div class="detail-meta">
            <span><CalendarDays :size="15" />{{ formatDate(article.publishTime) }}</span>
            <span><Eye :size="15" />公开阅读</span>
            <span><BookOpen :size="15" />{{ readingMinutes }} 分钟</span>
          </div>
          <div v-if="article.tags.length" class="tag-row">
            <span v-for="tag in article.tags" :key="tag.id">#{{ tag.name }}</span>
          </div>
        </header>

        <div class="markdown-body" v-html="htmlContent" />
      </article>

      <aside class="reading-aside" data-reveal>
        <div class="toc-card">
          <p class="page-kicker">On this page</p>
          <a v-for="item in toc" :key="item" href="#" @click.prevent="scrollToHeading(item)">{{ item }}</a>
          <span v-if="toc.length === 0">正文还没有二级标题。</span>
        </div>
        <div class="reaction-card">
          <button class="icon-button" type="button">
            <Heart :size="18" />
            <span>{{ liked ? '已喜欢' : '喜欢' }}</span>
          </button>
          <button class="icon-button" type="button">
            <MessageCircle :size="18" />
            <span>{{ comments.length }} 条评论</span>
          </button>
        </div>
        <div class="comments-card">
          <p class="page-kicker">Comments</p>
          <form class="comment-form" @submit.prevent="postComment">
            <textarea v-model="commentDraft" rows="4" placeholder="写下你的想法，审核后公开展示。" />
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
      <h2>没有找到这篇文章</h2>
      <p>{{ notice || '它可能还没公开，或已经归档。' }}</p>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import {
  ArrowLeft,
  BookOpen,
  CalendarDays,
  Eye,
  Heart,
  LoaderCircle,
  MessageCircle,
} from '@lucide/vue'

import { fallbackArticles } from '@/content/studio'
import { fetchArticleBySlug, fetchComments, submitComment } from '@/services/content'
import { useCinematicPageMotion } from '@/shared/composables/useCinematicPageMotion'
import { usePageReveal } from '@/shared/composables/usePageReveal'
import { toCssImageUrl } from '@/shared/cssImage'
import type { ArticleSummary, CommentSummary } from '@/shared/domain'
import { renderSafeMarkdown } from '@/shared/markdown'
import { useSessionStore } from '@/shared/sessionStore'

const route = useRoute()
const root = ref<HTMLElement | null>(null)
const article = ref<ArticleSummary | null>(null)
const comments = ref<CommentSummary[]>([])
const commentDraft = ref('')
const commentNotice = ref('')
const isLoading = ref(true)
const notice = ref('')
const liked = ref(false)
const slug = computed(() => readRouteParam(route.params.slug))
const session = useSessionStore()
const cinematic = useCinematicPageMotion(root)

usePageReveal(root)

const canComment = computed(() => Boolean(session.accessToken))
const htmlContent = computed(() => renderSafeMarkdown(article.value?.contentMarkdown ?? article.value?.summary))
const readingMinutes = computed(() => Math.max(1, Math.ceil((article.value?.contentMarkdown?.length ?? 500) / 420)))
const toc = computed(() =>
  (article.value?.contentMarkdown ?? '')
    .split(/\r?\n/)
    .filter((line) => line.startsWith('## '))
    .map((line) => line.replace(/^##\s*/, '').trim())
    .filter(Boolean),
)
const articleCoverStyle = computed(() => ({
  '--detail-accent': article.value?.tags[0]?.color ?? '#6ea8ff',
  '--detail-cover': toCssImageUrl(article.value?.coverUrl),
}))

async function loadArticle() {
  if (!slug.value) {
    article.value = null
    notice.value = '文章地址缺少 slug'
    isLoading.value = false
    return
  }

  isLoading.value = true
  notice.value = ''
  comments.value = []
  commentNotice.value = ''
  try {
    article.value = await fetchArticleBySlug(slug.value)
    await loadComments()
  } catch (error) {
    article.value = fallbackArticles.find((item) => item.slug === slug.value) ?? null
    notice.value = error instanceof Error ? `后端暂不可用，已尝试本地样例：${error.message}` : '后端暂不可用，已尝试本地样例。'
  } finally {
    isLoading.value = false
    void cinematic.play()
  }
}

async function loadComments() {
  if (!article.value?.id) {
    comments.value = []
    return
  }
  try {
    const page = await fetchComments({ targetType: 'ARTICLE', targetId: article.value.id, pageSize: 20 })
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
  if (!article.value?.id || !commentDraft.value.trim()) {
    commentNotice.value = '请填写评论内容'
    return
  }
  try {
    await submitComment({
      targetType: 'ARTICLE',
      targetId: article.value.id,
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

function formatDate(value?: string | null): string {
  if (!value) {
    return '未定档'
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date(value))
}

function scrollToHeading(title: string) {
  const headings = Array.from(root.value?.querySelectorAll<HTMLHeadingElement>('.markdown-body h2') ?? [])
  const target = headings.find((heading) => heading.textContent?.trim() === title)
  target?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

onMounted(loadArticle)
watch(slug, loadArticle)
</script>
