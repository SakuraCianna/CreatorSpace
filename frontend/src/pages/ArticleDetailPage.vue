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
          <button
            class="icon-button"
            :class="{ 'is-active': liked }"
            type="button"
            :disabled="!canComment"
            @click="toggleLike"
          >
            <Heart :size="18" />
            <span>{{ liked ? '已喜欢' : '喜欢' }}</span>
          </button>
          <button
            class="icon-button"
            :class="{ 'is-active': favorited }"
            type="button"
            :disabled="!canComment"
            @click="toggleFavorite"
          >
            <Bookmark :size="18" />
            <span>{{ favorited ? '已收藏' : '收藏' }}</span>
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
          <article v-for="comment in comments" :key="comment.id" class="comment-item" :style="{ '--depth': comment.depth }">
            <div class="comment-header">
              <strong>{{ comment.username }}</strong>
              <span class="comment-time">{{ formatDate(comment.createdAt) }}</span>
            </div>
            <p class="comment-body">{{ comment.content }}</p>
            <div class="comment-actions">
              <button
                class="comment-action-btn"
                type="button"
                :disabled="!canComment"
                @click="replyTo(comment)"
              >
                <MessageCircle :size="14" />
                回复
              </button>
              <span v-if="comment.likeCount > 0" class="comment-likes">{{ comment.likeCount }} 赞</span>
            </div>
          </article>
          <form v-if="replyTarget" class="reply-hint" @submit.prevent="() => {}">
            <span>回复 {{ replyTarget.username }}：</span>
            <button class="text-link" type="button" @click="cancelReply">取消</button>
          </form>
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
  Bookmark,
  BookOpen,
  CalendarDays,
  Eye,
  Heart,
  LoaderCircle,
  MessageCircle,
} from '@lucide/vue'

import {
  fetchArticleBySlug,
  fetchComments,
  submitComment,
  likeTarget,
  unlikeTarget,
  favoriteTarget,
  unfavoriteTarget,
} from '@/services/content'
import { toUserMessage } from '@/services/http'
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
const favorited = ref(false)
const replyTarget = ref<CommentSummary | null>(null)
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
    article.value = null
    notice.value = toUserMessage(error, '文章暂时无法打开，请稍后再试')
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
      parentId: replyTarget.value?.id,
      content: commentDraft.value.trim(),
    })
    commentDraft.value = ''
    replyTarget.value = null
    commentNotice.value = '评论已提交，审核通过后会公开展示'
    await loadComments()
  } catch (error) {
    commentNotice.value = toUserMessage(error, '评论提交失败')
  }
}

async function toggleLike() {
  if (!article.value?.id || !canComment.value) return
  try {
    if (liked.value) {
      await unlikeTarget('ARTICLE', article.value.id)
      liked.value = false
    } else {
      await likeTarget('ARTICLE', article.value.id)
      liked.value = true
    }
  } catch {
    commentNotice.value = '操作失败，请重试'
  }
}

async function toggleFavorite() {
  if (!article.value?.id || !canComment.value) return
  try {
    if (favorited.value) {
      await unfavoriteTarget('ARTICLE', article.value.id)
      favorited.value = false
    } else {
      await favoriteTarget('ARTICLE', article.value.id)
      favorited.value = true
    }
  } catch {
    commentNotice.value = '操作失败，请重试'
  }
}

function replyTo(comment: CommentSummary) {
  replyTarget.value = comment
  commentDraft.value = ''
}

function cancelReply() {
  replyTarget.value = null
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

<style scoped>
.detail-page {
  display: grid;
  gap: 18px;
  padding: 46px 0 84px;
}

.detail-back {
  justify-self: start;
}

.reading-layout {
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

.reading-aside {
  position: sticky;
  top: 100px;
  display: grid;
  gap: 12px;
}

.toc-card,
.reaction-card {
  padding: 18px;
}

.toc-card {
  display: grid;
  gap: 10px;
}

.toc-card a,
.toc-card span {
  color: var(--tone-muted);
  font-size: 14px;
}

.reaction-card {
  display: grid;
  gap: 10px;
}

.reaction-card .icon-button {
  justify-content: flex-start;
}

.reaction-card .icon-button.is-active {
  color: var(--tone-primary);
  background: rgba(49, 91, 255, 0.1);
}

.comment-item {
  padding: 12px;
  margin-left: calc(var(--depth, 0) * 20px);
  border-left: 2px solid var(--tone-line);
}

.comment-item[style*="--depth: 1"],
.comment-item[style*="--depth: 2"],
.comment-item[style*="--depth: 3"],
.comment-item[style*="--depth: 4"] {
  background: rgba(0, 0, 0, 0.02);
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}

.comment-header strong {
  color: var(--tone-ink);
  font-size: 14px;
}

.comment-time {
  color: var(--tone-faint);
  font-size: 12px;
}

.comment-body {
  margin: 4px 0 6px;
  color: #475569;
  font-size: 14px;
  line-height: 1.6;
}

.comment-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.comment-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 6px;
  border: none;
  background: transparent;
  color: var(--tone-muted);
  cursor: pointer;
  font-size: 12px;
}

.comment-action-btn:hover {
  color: var(--tone-primary);
}

.comment-likes {
  color: var(--tone-faint);
  font-size: 12px;
}

.reply-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 6px;
  background: rgba(49, 91, 255, 0.06);
  font-size: 13px;
  color: var(--tone-muted);
}

@media (max-width: 1020px) {
  .reading-layout {
    grid-template-columns: 1fr;
  }

  .reading-aside {
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
