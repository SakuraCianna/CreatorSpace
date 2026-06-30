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
        <nav v-if="previousArticle || nextArticle" class="article-neighbors" aria-label="上一篇和下一篇文章">
          <RouterLink
            v-if="previousArticle"
            class="article-neighbor"
            :to="{ name: 'article-detail', params: { slug: previousArticle.slug } }"
          >
            <span><ArrowLeft :size="15" />上一篇</span>
            <strong>{{ previousArticle.title }}</strong>
          </RouterLink>
          <RouterLink
            v-if="nextArticle"
            class="article-neighbor article-neighbor--next"
            :to="{ name: 'article-detail', params: { slug: nextArticle.slug } }"
          >
            <span>下一篇<ArrowRight :size="15" /></span>
            <strong>{{ nextArticle.title }}</strong>
          </RouterLink>
        </nav>
        <div class="reaction-card">
          <button
            class="icon-button"
            :class="{ 'is-liked': liked }"
            type="button"
            :disabled="!canComment"
            @click="toggleLike(article?.id)"
          >
            <Heart :size="18" />
            <span>{{ liked ? '已喜欢' : '喜欢' }}</span>
          </button>
          <button
            class="icon-button"
            :class="{ 'is-favorited': favorited }"
            type="button"
            :disabled="!canComment"
            @click="toggleFavorite(article?.id)"
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
          <div class="comments-head">
            <p class="page-kicker">Comments</p>
            <span>{{ comments.length }} 条公开评论</span>
          </div>
          <div v-if="replyTarget" class="reply-hint">
            <span>正在回复 {{ replyTarget.username }}</span>
            <button class="text-link" type="button" @click="cancelReply">取消</button>
          </div>
          <form class="comment-form" @submit.prevent="postComment">
            <label class="sr-only" for="article-comment">写评论</label>
            <textarea
              id="article-comment"
              v-model="commentDraft"
              rows="4"
              :placeholder="replyTarget ? `回复 ${replyTarget.username}，审核后公开展示。` : '写下你的想法，审核后公开展示。'"
            />
            <div class="comment-form__footer">
              <span>{{ canComment ? '审核通过后会公开展示' : '登录后即可参与讨论' }}</span>
              <button class="button button-filled button-compact" :disabled="!canComment || !commentDraft.trim()" type="submit">
                {{ canComment ? '提交评论' : '登录后评论' }}
              </button>
            </div>
          </form>
          <div v-if="comments.length > 0" class="comment-list">
            <article v-for="comment in comments" :key="comment.id" class="comment-item" :style="{ '--depth': comment.depth }">
              <span class="comment-avatar" aria-hidden="true">{{ commentInitial(comment.username) }}</span>
              <div class="comment-content">
                <div class="comment-header">
                  <strong>{{ comment.username }}</strong>
                  <span class="comment-time">{{ formatDate(comment.createdAt) }}</span>
                </div>
                <p v-if="comment.replyToUsername" class="comment-reply-to">
                  <Reply :size="12" /> 回复 {{ comment.replyToUsername }}
                </p>
                <p class="comment-body">{{ comment.content }}</p>
                <div class="comment-actions">
                  <button
                    class="comment-action-btn"
                    :class="{ 'is-active': commentLiked[comment.id] }"
                    type="button"
                    :disabled="!canComment"
                    @click="toggleCommentLike(comment)"
                  >
                    <Heart :size="14" />
                    {{ comment.likeCount }}
                  </button>
                  <button
                    class="comment-action-btn"
                    type="button"
                    :disabled="!canComment"
                    @click="replyTo(comment)"
                  >
                    <MessageCircle :size="14" />
                    回复
                  </button>
                </div>
              </div>
            </article>
          </div>
          <div v-else class="comment-empty">
            <strong>还没有公开评论</strong>
            <span>成为第一个留下想法的人。</span>
          </div>
          <p v-if="commentNotice" class="inline-notice">{{ commentNotice }}</p>
        </div>
        <p v-if="notice" class="inline-notice">{{ notice }}</p>
      </article>
    </div>
    <div v-else class="empty-state detail-state" data-reveal>
      <h2>没有找到这篇文章</h2>
      <p>{{ notice || '它可能还没公开，或已经归档。' }}</p>
    </div>
  </section>
</template>
<script setup lang="ts">
// 引入状态生命周期钩子和相关组件
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import {
  ArrowLeft,
  ArrowRight,
  Bookmark,
  BookOpen,
  CalendarDays,
  Eye,
  Heart,
  LoaderCircle,
  MessageCircle,
  Reply,
} from '@lucide/vue'
import {
  fetchArticleNeighbors,
  fetchArticleBySlug,
  fetchCommentReactionsBatch,
  fetchComments,
  reactToComment,
  submitComment,
  unreactFromComment,
} from '@/services/content'
import { HttpError, toUserMessage } from '@/services/http'
import { useCinematicPageMotion } from '@/shared/composables/useCinematicPageMotion'
import { useInteraction } from '@/shared/composables/useInteraction'
import { usePageReveal } from '@/shared/composables/usePageReveal'
import { toCssImageUrl } from '@/shared/cssImage'
import { formatDateToDay } from '@/shared/datetime'
import type { ArticleSummary, CommentSummary } from '@/shared/domain'
import { normalizeMarkdownSource, renderSafeMarkdown } from '@/shared/markdown'
import { useSessionStore } from '@/shared/sessionStore'
// 初始化文章数据与交互控制状态
const route = useRoute()
const root = ref<HTMLElement | null>(null)
const article = ref<ArticleSummary | null>(null)
const comments = ref<CommentSummary[]>([])
const previousArticle = ref<ArticleSummary | null>(null)
const nextArticle = ref<ArticleSummary | null>(null)
const commentDraft = ref('')
const commentNotice = ref('')
const isLoading = ref(true)
const notice = ref('')
const replyTarget = ref<CommentSummary | null>(null)
const commentLiked = ref<Record<number, boolean>>({})
const slug = computed(() => readRouteParam(route.params.slug))
const session = useSessionStore()
const cinematic = useCinematicPageMotion(root)
const { liked, favorited, loadStatus: loadInteractionStatus, toggleLike, toggleFavorite } = useInteraction('ARTICLE')
usePageReveal(root)
const canComment = computed(() => Boolean(session.accessToken))
const articleMarkdown = computed(() => normalizeMarkdownSource(article.value?.contentMarkdown ?? article.value?.summary))
const htmlContent = computed(() => renderSafeMarkdown(articleMarkdown.value))
const readingMinutes = computed(() => Math.max(1, Math.ceil(articleMarkdown.value.length / 420)))
const toc = computed(() =>
  articleMarkdown.value
    .split(/\r?\n/)
    .filter((line) => line.startsWith('## '))
    .map((line) => line.replace(/^##\s*/, '').trim())
    .filter(Boolean),
)
const articleCoverStyle = computed(() => ({
  '--detail-accent': article.value?.tags[0]?.color ?? '#6ea8ff',
  '--detail-cover': toCssImageUrl(article.value?.coverUrl),
}))
// 依据路由 slug 标识读取文章全量详情数据, 渲染完成后加载关联评论并播放电影式页面入场显影动效
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
  previousArticle.value = null
  nextArticle.value = null
  commentNotice.value = ''
  try {
    const [detail, neighbors] = await Promise.all([
      fetchArticleBySlug(slug.value),
      fetchArticleNeighbors(slug.value),
    ])
    article.value = detail
    previousArticle.value = neighbors.previousArticle ?? null
    nextArticle.value = neighbors.nextArticle ?? null
    await Promise.all([loadComments(), loadInteractionStatus(detail.id)])
  } catch (error) {
    article.value = null
    notice.value = toUserMessage(error, '文章暂时无法打开，请稍后再试')
  } finally {
    isLoading.value = false
    void cinematic.play()
  }
}
// 向后端异步获取针对本文章审核通过的已公开评论反馈列表
async function loadComments(options: { keepCurrentNotice?: boolean } = {}) {
  if (!article.value?.id) {
    comments.value = []
    return
  }
  try {
    const page = await fetchComments({ targetType: 'ARTICLE', targetId: article.value.id, pageSize: 20 })
    comments.value = page.records
    await loadCommentLikes()
    if (!options.keepCurrentNotice) {
      commentNotice.value = ''
    }
  } catch {
    comments.value = []
    if (!options.keepCurrentNotice) {
      commentNotice.value = '评论暂时不可用'
    }
  }
}
// 提交用户对本文章的观点评论或指定楼层的回复, 校验完成后清空输入框并刷新评论流列表
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
    const result = await submitComment({
      targetType: 'ARTICLE',
      targetId: article.value.id,
      parentId: replyTarget.value?.id,
      content: commentDraft.value.trim(),
    })
    commentDraft.value = ''
    replyTarget.value = null
    if (result.status === 'APPROVED') {
      commentNotice.value = '评论已发布'
      comments.value.unshift(result)
    } else {
      commentNotice.value = '评论已提交，等待审核'
    }
  } catch (error) {
    commentNotice.value = error instanceof HttpError && error.backendMessage
      ? error.backendMessage
      : toUserMessage(error, '评论提交失败')
  }
}
function replyTo(comment: CommentSummary) {
  replyTarget.value = comment
  commentDraft.value = ''
}
async function toggleCommentLike(comment: CommentSummary) {
  if (!canComment.value) return
  const liked = commentLiked.value[comment.id]
  try {
    if (liked) {
      await unreactFromComment(comment.id)
      commentLiked.value = { ...commentLiked.value, [comment.id]: false }
      comment.likeCount = Math.max(0, comment.likeCount - 1)
    } else {
      await reactToComment(comment.id)
      commentLiked.value = { ...commentLiked.value, [comment.id]: true }
      comment.likeCount += 1
    }
  } catch (e) {
    console.error('toggleCommentLike error:', e)
  }
}
async function loadCommentLikes() {
  if (!canComment.value || comments.value.length === 0) return
  const ids = comments.value.map(c => c.id)
  try {
    const statuses = await fetchCommentReactionsBatch(ids)
    commentLiked.value = statuses
  } catch {
    commentLiked.value = {}
  }
}
function cancelReply() {
  replyTarget.value = null
}
function commentInitial(username: string): string {
  return username.trim().slice(0, 1).toUpperCase() || 'U'
}
function readRouteParam(value: string | string[] | undefined): string {
  if (Array.isArray(value)) {
    return value[0] ?? ''
  }
  return value ?? ''
}
function formatDate(value?: string | null): string {
  return formatDateToDay(value)
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
  gap: var(--theme-density-spacing, 16px);
  padding: 46px 0 84px;
}
.detail-back {
  justify-self: start;
}
.reading-layout {
  display: grid;
  grid-template-columns: 1fr;
  gap: var(--theme-density-spacing, 16px);
  align-items: start;
}
.detail-panel {
  position: relative;
  overflow: hidden;
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm);
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--tone-panel-solid) 92%, transparent), color-mix(in srgb, var(--tone-panel-solid) 78%, transparent)),
    color-mix(in srgb, var(--tone-panel-solid) 88%, transparent);
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
  gap: 16px;
  padding: clamp(30px, 4vw, 46px);
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
  max-width: 860px;
  margin: 0;
  font-size: clamp(34px, 4.4vw, 52px);
  font-weight: 860;
  line-height: 1.12;
  letter-spacing: 0;
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
.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}
.detail-meta span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 34px;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 720;
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
  display: block;
  padding: clamp(32px, 4vw, 54px);
  color: var(--tone-ink);
  background: color-mix(in srgb, var(--tone-panel-solid) 72%, transparent);
}
.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin: 32px 0 12px;
  color: var(--tone-ink);
  line-height: 1.24;
}
.markdown-body :deep(h1:first-child),
.markdown-body :deep(h2:first-child),
.markdown-body :deep(h3:first-child) {
  margin-top: 0;
}
.markdown-body :deep(h2) {
  font-size: 28px;
}
.markdown-body :deep(p),
.markdown-body :deep(li),
.markdown-body :deep(blockquote) {
  margin: 0 0 16px;
  color: var(--tone-muted);
  font-size: 17px;
  line-height: 1.86;
}
.markdown-body :deep(blockquote) {
  padding: 16px 18px;
  border: 1px solid color-mix(in srgb, var(--tone-teal) 20%, transparent);
  border-left: 4px solid var(--tone-teal);
  border-radius: 8px;
  background: rgba(0, 124, 114, 0.07);
}
.markdown-body :deep(a) {
  color: var(--tone-primary);
  font-weight: 720;
}
.markdown-body :deep(code:not(pre code)) {
  padding: 2px 6px;
  border-radius: 6px;
  background: rgba(11, 87, 208, 0.08);
  color: #0b57d0;
  font-size: 0.92em;
}
.markdown-body :deep(pre) {
  max-width: 100%;
  overflow-x: auto;
  border-radius: 8px;
  margin: 18px 0;
}
.article-neighbors {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  padding: 0 clamp(32px, 4vw, 54px) clamp(32px, 4vw, 54px);
  background: color-mix(in srgb, var(--tone-panel-solid) 72%, transparent);
}
.article-neighbor {
  display: grid;
  gap: 8px;
  min-height: 118px;
  padding: 16px;
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm, 8px);
  background: color-mix(in srgb, var(--tone-panel-solid) 84%, transparent);
  color: var(--tone-ink);
  text-decoration: none;
}
.article-neighbor--next {
  text-align: right;
}
.article-neighbor span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--tone-faint);
  font-size: 12px;
  font-weight: 800;
}
.article-neighbor--next span {
  justify-content: flex-end;
}
.article-neighbor strong {
  font-size: 17px;
  line-height: 1.36;
}
.reading-aside {
  position: sticky;
  top: 100px;
  display: grid;
  gap: calc(var(--theme-density-spacing, 16px) * 0.75);
}
.toc-card,
.reaction-card,
.comments-card {
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm);
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--tone-panel-solid) 90%, transparent), color-mix(in srgb, var(--tone-panel-solid) 72%, transparent)),
    var(--tone-panel);
  box-shadow: 0 16px 40px rgba(32, 33, 36, 0.08);
  backdrop-filter: blur(18px);
}
.toc-card,
.reaction-card {
  padding: calc(var(--theme-density-spacing, 16px) * 1.125);
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
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}
.reaction-card .icon-button {
  flex: 1;
  justify-content: center;
}
.reaction-card .icon-button.is-liked {
  color: #e0455a;
  background: rgba(224, 69, 90, 0.1);
}
.reaction-card .icon-button.is-liked svg {
  stroke: #e0455a;
  fill: #e0455a;
}
.reaction-card .icon-button.is-favorited {
  color: var(--tone-violet);
  background: rgba(103, 80, 164, 0.1);
}
.reaction-card .icon-button.is-favorited svg {
  stroke: var(--tone-violet);
  fill: var(--tone-violet);
}
.comment-action-btn.is-active {
  color: #e0455a;
}
.comment-action-btn.is-active svg {
  stroke: #e0455a;
  fill: #e0455a;
}
.comments-card {
  display: grid;
  gap: var(--theme-density-spacing, 16px);
  padding: calc(var(--theme-density-spacing, 16px) * 1.125);
}
.comments-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
.comments-head span {
  color: var(--tone-faint);
  font-size: 12px;
  font-weight: 720;
}
.comment-form {
  display: grid;
  gap: 10px;
}
.comment-form textarea {
  width: 100%;
  min-height: 112px;
  padding: 12px 14px;
  border: 1px solid var(--tone-line-strong);
  border-radius: var(--app-radius-sm, 8px);
  outline: none;
  resize: vertical;
  background: color-mix(in srgb, var(--tone-panel-solid) 84%, transparent);
  color: var(--tone-ink);
  font: inherit;
  line-height: 1.65;
  transition: border-color var(--transition-time, 180ms) ease, box-shadow var(--transition-time, 180ms) ease, background var(--transition-time, 180ms) ease;
}
.comment-form textarea:focus {
  border-color: color-mix(in srgb, var(--tone-primary) 48%, var(--tone-line-strong));
  background: var(--tone-panel-solid);
  box-shadow: 0 0 0 4px rgba(11, 87, 208, 0.08);
}
.comment-form__footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.comment-form__footer span {
  color: var(--tone-faint);
  font-size: 12px;
  line-height: 1.5;
}
.comment-list {
  display: grid;
  gap: 10px;
}
.comment-item {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 10px;
  padding: 12px;
  margin-left: calc(var(--depth, 0) * 20px);
  border: 1px solid color-mix(in srgb, var(--tone-line) 76%, transparent);
  border-radius: var(--app-radius-sm, 8px);
  background: color-mix(in srgb, var(--tone-panel-solid) 72%, transparent);
}
.comment-item[style*="--depth: 1"],
.comment-item[style*="--depth: 2"],
.comment-item[style*="--depth: 3"],
.comment-item[style*="--depth: 4"] {
  background: rgba(0, 0, 0, 0.02);
}
.comment-avatar {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 999px;
  background: color-mix(in srgb, var(--tone-primary) 12%, #ffffff);
  color: var(--tone-primary);
  font-size: 13px;
  font-weight: 820;
}
.comment-reply-to {
  display: flex;
  align-items: center;
  gap: 4px;
  margin: 2px 0 4px;
  font-size: 12px;
  color: var(--tone-ink-2);
}
.comment-content {
  min-width: 0;
}
.comment-header {
  display: flex;
  flex-wrap: wrap;
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
  justify-content: space-between;
  gap: 8px;
  padding: 10px 12px;
  border: 1px solid rgba(49, 91, 255, 0.14);
  border-radius: 8px;
  background: rgba(49, 91, 255, 0.06);
  font-size: 13px;
  color: var(--tone-muted);
}
.comment-empty {
  display: grid;
  gap: 4px;
  padding: 14px;
  border: 1px dashed var(--tone-line-strong);
  border-radius: var(--app-radius-sm, 8px);
  background: color-mix(in srgb, var(--tone-panel-solid) 46%, transparent);
}
.comment-empty strong {
  color: var(--tone-ink);
  font-size: 14px;
}
.comment-empty span {
  color: var(--tone-muted);
  font-size: 13px;
}
@media (max-width: 1020px) {
  .reading-layout {
    grid-template-columns: 1fr;
  }
  .reading-aside {
    position: static;
  }
  .article-neighbors {
    grid-template-columns: 1fr;
  }
  .article-neighbor--next {
    text-align: left;
  }
  .article-neighbor--next span {
    justify-content: flex-start;
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
