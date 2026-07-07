<template>
  <section
    ref="root"
    class="detail-page article-detail-page"
    :style="articleThemeStyle"
    :data-blog-canvas="articleBlogTheme.canvasType"
    :data-blog-layout="articleBlogTheme.layoutStyle"
    :data-blog-block="articleBlogTheme.blockStyle"
  >
    <RouterLink class="detail-back text-link" :to="{ name: 'articles' }" data-reveal>
      <ArrowLeft :size="16" />
      返回文章档案
    </RouterLink>
    <div v-if="isLoading" class="empty-state detail-state" data-reveal>
      <LoaderCircle class="spin" :size="24" />
      <h2>正在打开文章</h2>
    </div>
    <div v-else-if="article" class="reading-layout" :class="{ 'reading-layout--no-left': !hasLeftRail }">
      <aside v-if="hasLeftRail" class="article-left-rail">
        <section v-if="article?.authorId" class="author-card">
          <RouterLink
            class="author-card__link"
            :to="{ name: 'user-profile', params: { userId: article.authorId } }"
          >
            <div class="author-card__avatar">
              <img
                v-if="article.authorAvatar"
                :src="article.authorAvatar"
                alt=""
                loading="lazy"
              />
              <span v-else>{{ authorInitial }}</span>
            </div>
            <div class="author-card__info">
              <strong>{{ article.authorName || '匿名作者' }}</strong>
              <span>{{ article.authorBio || 'CreatorSpace 创作者' }}</span>
            </div>
          </RouterLink>
          <div class="author-stats" :class="{ 'author-stats--four': authorProfile }">
            <template v-if="authorProfile">
              <span><strong>{{ formatCount(authorProfile.articleCount) }}</strong><small>文章</small></span>
              <span><strong>{{ formatCount(authorProfile.followerCount) }}</strong><small>粉丝</small></span>
              <span><strong>{{ formatCount(authorProfile.followingCount) }}</strong><small>关注</small></span>
              <span><strong>{{ formatCount(authorProfile.friendCount) }}</strong><small>好友</small></span>
            </template>
            <template v-else>
              <span><strong>{{ formatCount(article.viewCount) }}</strong><small>阅读</small></span>
              <span><strong>{{ formatCount(article.likeCount) }}</strong><small>喜欢</small></span>
              <span><strong>{{ formatCount(article.commentCount ?? comments.length) }}</strong><small>评论</small></span>
            </template>
          </div>
          <div class="author-card__actions" v-if="canFollowAuthor || (session.currentUser?.id !== article.authorId)">
            <button
              v-if="canFollowAuthor"
              class="author-card__follow button button-compact"
              :class="following ? 'button-outline' : 'button-filled'"
              type="button"
              @click="toggleFollow(article.authorId)"
            >
              <UserPlus v-if="!following" :size="13" />
              <UserCheck v-else :size="13" />
              {{ isFriend ? '互相关注' : following ? '已关注' : '关注' }}
            </button>
            <RouterLink
              v-if="session.currentUser?.id !== article.authorId"
              class="author-card__message button button-outline button-compact"
              :to="{ name: 'my-messages', query: { to: article.authorId } }"
            >
              <MessageCircle :size="13" />
              私信
            </RouterLink>
          </div>
        </section>

        <section v-if="authorArticles && authorArticles.length > 0" class="side-card author-articles-card">
          <h2>TA的精选</h2>
          <div class="author-articles-list">
            <RouterLink
              v-for="item in authorArticles"
              :key="item.id"
              class="author-article-link"
              :to="{ name: 'article-detail', params: { slug: item.slug } }"
            >
              <strong class="author-article-title">{{ item.title }}</strong>
              <span class="author-article-views">
                <Eye :size="12" />
                {{ formatCount(item.viewCount) }} 阅读
              </span>
            </RouterLink>
          </div>
        </section>
      </aside>

      <article class="detail-panel">
        <header class="detail-hero" :style="articleCoverStyle">
          <p class="page-kicker">{{ article.category?.name ?? 'Creator Journal' }}</p>
          <h1>{{ article.title }}</h1>
          <p v-if="article.summary" class="detail-summary">{{ article.summary }}</p>
          <div class="detail-meta">
            <span class="original-badge">原创</span>
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

      <aside class="article-right-rail">
        <section class="side-card toc-card">
          <h2>目录</h2>
          <div v-if="articleToc.length" class="toc-list">
            <button
              v-for="item in articleToc"
              :key="item.id"
              class="toc-link"
              :class="`toc-link--level-${item.level}`"
              type="button"
              @click="scrollToHeading(item.id)"
            >
              {{ item.text }}
            </button>
          </div>
          <span v-else class="toc-empty">正文标题会在这里生成目录</span>
        </section>

        <section v-if="previousArticle || nextArticle" class="side-card related-card">
          <h2>相邻阅读</h2>
          <RouterLink
            v-if="previousArticle"
            class="related-link"
            :to="{ name: 'article-detail', params: { slug: previousArticle.slug } }"
          >
            <span>上一篇</span>
            <strong>{{ previousArticle.title }}</strong>
          </RouterLink>
          <RouterLink
            v-if="nextArticle"
            class="related-link"
            :to="{ name: 'article-detail', params: { slug: nextArticle.slug } }"
          >
            <span>下一篇</span>
            <strong>{{ nextArticle.title }}</strong>
          </RouterLink>
        </section>

        <section class="side-card reaction-card">
          <h2>互动</h2>
          <button
            class="icon-button"
            :class="{ 'is-liked': liked }"
            type="button"
            :disabled="!canComment"
            @click="toggleLike(article?.id)"
          >
            <Heart :size="18" :fill="liked ? '#f43f5e' : 'none'" :color="liked ? '#f43f5e' : 'currentColor'" />
            <span>{{ liked ? '已喜欢' : '喜欢' }}</span>
          </button>
          <button
            class="icon-button"
            :class="{ 'is-favorited': favorited }"
            type="button"
            :disabled="!canComment"
            @click="toggleFavorite(article?.id)"
          >
            <Bookmark :size="18" :fill="favorited ? '#eab308' : 'none'" :color="favorited ? '#eab308' : 'currentColor'" />
            <span>{{ favorited ? '已收藏' : '收藏' }}</span>
          </button>
          <button class="icon-button" type="button" @click="scrollToComments">
            <MessageCircle :size="18" />
            <span>{{ comments.length }} 条评论</span>
          </button>
        </section>
      </aside>

      <section ref="commentsSection" class="comments-card">
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
            ref="commentInput"
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
        <div v-if="commentTree.length > 0" class="comment-list">
          <CommentThread
            v-for="comment in commentTree"
            :key="comment.id"
            :comment="comment"
            :can-comment="canComment"
            :liked-map="commentLiked"
            @reply="replyTo"
            @like="toggleCommentLike"
          />
        </div>
        <div v-else class="comment-empty">
          <strong>还没有公开评论</strong>
          <span>成为第一个留下想法的人。</span>
        </div>
        <p v-if="commentNotice" class="inline-notice">{{ commentNotice }}</p>
        <p v-if="notice" class="inline-notice">{{ notice }}</p>
      </section>
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
  Bookmark,
  BookOpen,
  CalendarDays,
  Eye,
  Heart,
  LoaderCircle,
  MessageCircle,
  UserCheck,
  UserPlus,
} from '@lucide/vue'
import {
  fetchArticleNeighbors,
  fetchArticleBySlug,
  fetchCommentReactionsBatch,
  fetchComments,
  reactToComment,
  submitComment,
  unreactFromComment,
  fetchUserProfile,
  fetchUserArticles,
} from '../services/content'
import { HttpError, toUserMessage } from '../services/http'
import { useCinematicPageMotion } from '../shared/composables/useCinematicPageMotion'
import { useFollow } from '../shared/composables/useFollow'
import { useInteraction } from '../shared/composables/useInteraction'
import { usePageReveal } from '../shared/composables/usePageReveal'
import { toCssImageUrl } from '../shared/cssImage'
import { formatDateToDay } from '../shared/datetime'
import type { ArticleSummary, CommentSummary, CommentTree, UserProfile } from '../shared/domain'
import { buildCommentTree } from '../shared/domain'
import CommentThread from '../shared/components/CommentThread.vue'
import { blogThemeToStyle, normalizeBlogTheme } from '../shared/blogTheme'
import { normalizeMarkdownSource, renderSafeMarkdown } from '../shared/markdown'
import { useSessionStore } from '../shared/sessionStore'
// 初始化文章数据与交互控制状态
const route = useRoute()
const root = ref<HTMLElement | null>(null)
const commentsSection = ref<HTMLElement | null>(null)
const commentInput = ref<HTMLTextAreaElement | null>(null)
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
const commentTree = computed<CommentTree[]>(() => buildCommentTree(comments.value))
const slug = computed(() => readRouteParam(route.params.slug))
const session = useSessionStore()
const cinematic = useCinematicPageMotion(root)
const { liked, favorited, loadStatus: loadInteractionStatus, toggleLike, toggleFavorite } = useInteraction('ARTICLE')
const { following, isFriend, loadStatus: loadFollowStatus, toggleFollow } = useFollow()

const authorProfile = ref<UserProfile | null>(null)
const authorArticles = ref<ArticleSummary[]>([])

interface ArticleTocItem {
  id: string
  level: number
  text: string
}

usePageReveal(root)
const canComment = computed(() => Boolean(session.accessToken))
const canFollowAuthor = computed(() => {
  if (!canComment.value || !article.value?.authorId) return false
  return session.currentUser?.id !== article.value.authorId
})
const articleMarkdown = computed(() => normalizeMarkdownSource(article.value?.contentMarkdown ?? article.value?.summary))
const articleRender = computed(() => buildArticleRender(articleMarkdown.value))
const htmlContent = computed(() => articleRender.value.html)
const readingMinutes = computed(() => Math.max(1, Math.ceil(articleMarkdown.value.length / 420)))
const articleToc = computed(() => articleRender.value.toc)
const authorInitial = computed(() => commentInitial(article.value?.authorName || article.value?.authorBio || 'C'))
const hasLeftRail = computed(() => Boolean(article.value?.authorId || previousArticle.value || nextArticle.value))
const articleBlogTheme = computed(() => normalizeBlogTheme(article.value?.authorTheme))
const articleThemeStyle = computed(() => blogThemeToStyle(articleBlogTheme.value))
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
    await Promise.all([
      loadComments(),
      loadInteractionStatus(detail.id),
      loadFollowStatus(detail.authorId ?? 0),
      loadAuthorProfile(detail.authorId ?? 0),
    ])
  } catch (error) {
    article.value = null
    notice.value = toUserMessage(error, '文章暂时无法打开，请稍后再试')
  } finally {
    isLoading.value = false
    void cinematic.play()
  }
}

async function loadAuthorProfile(authorId: number) {
  if (!authorId) return
  try {
    const [profile, articlesPage] = await Promise.all([
      fetchUserProfile(authorId),
      fetchUserArticles(authorId, { pageSize: 6 }),
    ])
    authorProfile.value = profile
    authorArticles.value = (articlesPage.records || [])
      .filter((item) => item.id !== article.value?.id)
      .slice(0, 5)
  } catch {
    authorProfile.value = null
    authorArticles.value = []
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
    const flat = comments.value.find(c => c.id === comment.id)
    if (flat) flat.likeCount = comment.likeCount
  } catch {
    await loadCommentLikes()
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

function formatCount(value?: number | null): string {
  const count = Math.max(0, value ?? 0)
  if (count >= 10000) return `${(count / 10000).toFixed(count >= 100000 ? 0 : 1)}万`
  if (count >= 1000) return `${(count / 1000).toFixed(count >= 10000 ? 0 : 1)}k`
  return `${count}`
}

function buildArticleRender(markdown: string): { html: string; toc: ArticleTocItem[] } {
  const html = renderSafeMarkdown(markdown)
  if (!html || typeof document === 'undefined') {
    return { html, toc: [] }
  }

  const template = document.createElement('template')
  template.innerHTML = html
  const usedIds = new Map<string, number>()
  const toc: ArticleTocItem[] = []

  template.content.querySelectorAll<HTMLHeadingElement>('h2, h3').forEach((heading) => {
    const text = normalizeHeadingText(heading.textContent ?? '')
    if (!text) return

    const baseId = slugifyHeading(text) || `section-${toc.length + 1}`
    const currentCount = usedIds.get(baseId) ?? 0
    const id = currentCount === 0 ? baseId : `${baseId}-${currentCount + 1}`
    usedIds.set(baseId, currentCount + 1)
    heading.id = id
    toc.push({
      id,
      level: Number(heading.tagName.replace('H', '')),
      text,
    })
  })

  return {
    html: template.innerHTML,
    toc: toc.slice(0, 12),
  }
}

function normalizeHeadingText(value: string): string {
  return value.replace(/\s+/g, ' ').trim()
}

function slugifyHeading(value: string): string {
  return normalizeHeadingText(value)
    .toLowerCase()
    .replace(/[^\p{L}\p{N}]+/gu, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 64)
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

function scrollToHeading(id: string) {
  const selector = typeof CSS !== 'undefined' && CSS.escape
    ? `#${CSS.escape(id)}`
    : `[id="${id.replace(/\\/g, '\\\\').replace(/"/g, '\\"')}"]`
  const target = root.value?.querySelector<HTMLElement>(selector)
  target?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function scrollToComments() {
  commentsSection.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  window.setTimeout(() => {
    commentInput.value?.focus()
  }, 320)
}

onMounted(loadArticle)
watch(slug, loadArticle)
</script>
<style scoped>
/* CSDN-inspired reading frame, adapted to CreatorSpace theme tokens. */
.detail-page {
  position: relative;
  display: grid;
  gap: 14px;
  padding: 22px 0 72px;
  background: transparent;
  color: var(--blog-body, #374151);
  font-family: var(--blog-font-body, var(--blog-font, inherit));
}

.detail-page::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 600px;
  background: radial-gradient(circle at 10% 0%, color-mix(in srgb, var(--blog-accent, #2563eb) 8%, transparent), transparent 28%);
  pointer-events: none;
  z-index: 0;
}

.detail-page[data-blog-canvas='linen'] {
  background:
    repeating-linear-gradient(90deg, color-mix(in srgb, var(--blog-title, #111827) 5%, transparent) 0 1px, transparent 1px 22px),
    repeating-linear-gradient(0deg, color-mix(in srgb, var(--blog-title, #111827) 4%, transparent) 0 1px, transparent 1px 22px),
    linear-gradient(180deg, color-mix(in srgb, var(--blog-canvas, #f8fafc) 90%, #ffffff), #ffffff);
}

.detail-page[data-blog-canvas='gradient'] {
  background:
    radial-gradient(circle at 88% 8%, color-mix(in srgb, var(--blog-accent, #2563eb) 18%, transparent), transparent 30%),
    linear-gradient(135deg, color-mix(in srgb, var(--blog-canvas, #f8fafc) 78%, #ffffff), color-mix(in srgb, var(--blog-accent, #2563eb) 8%, #ffffff));
}

.detail-page[data-blog-canvas='image'] {
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--blog-canvas, #ffffff) 86%, transparent), color-mix(in srgb, var(--blog-canvas, #ffffff) 94%, transparent)),
    var(--blog-canvas-image),
    var(--blog-canvas, #f8fafc);
  background-size: cover;
  background-attachment: fixed;
}

.detail-back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  width: min(100% - 32px, 1500px);
  margin-inline: auto;
  min-height: 36px;
  color: var(--blog-muted, #667085);
  font-size: 14px;
  font-weight: 700;
}

.reading-layout,
.detail-page[data-blog-layout='notebook'] .reading-layout,
.detail-page[data-blog-layout='gallery'] .reading-layout {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr) 260px;
  gap: 16px;
  align-items: start;
  width: min(100% - 48px, 1780px);
  margin-inline: auto;
}

.reading-layout--no-left,
.detail-page[data-blog-layout='notebook'] .reading-layout--no-left,
.detail-page[data-blog-layout='gallery'] .reading-layout--no-left {
  grid-template-columns: minmax(0, 1fr) 280px;
}

.article-left-rail,
.article-right-rail {
  position: sticky;
  top: 92px;
  display: grid;
  gap: 12px;
  min-width: 0;
}

.detail-panel {
  overflow: hidden;
  border: 1px solid color-mix(in srgb, var(--blog-line, #e5e7eb) 86%, transparent);
  border-radius: var(--blog-radius, 8px);
  background: var(--blog-paper, #ffffff);
  box-shadow: 0 10px 30px color-mix(in srgb, var(--blog-title, #111827) 5%, transparent);
  backdrop-filter: none;
}

.detail-panel::before,
.detail-hero::before,
.detail-hero::after {
  content: none;
}

.detail-hero,
.detail-page[data-blog-layout='notebook'] .detail-hero,
.detail-page[data-blog-layout='gallery'] .detail-hero {
  display: grid;
  gap: 14px;
  min-height: 0;
  padding: 36px 48px 24px;
  border-bottom: 1px solid color-mix(in srgb, var(--blog-line, #e5e7eb) 78%, transparent);
  background:
    linear-gradient(90deg, color-mix(in srgb, var(--blog-accent, #2563eb) 7%, transparent), transparent 44%),
    var(--blog-paper, #ffffff);
  color: var(--blog-title, #111827);
  overflow: visible;
}

.detail-hero h1 {
  max-width: 100%;
  margin: 0;
  color: var(--blog-title, #111827);
  font-size: 40px;
  font-weight: var(--blog-heading-weight, 820);
  line-height: 1.18;
  letter-spacing: 0;
  text-shadow: none;
}

.page-kicker {
  margin: 0;
  color: var(--blog-accent, #2563eb);
  font-size: 13px;
  font-weight: 850;
}

.detail-summary {
  max-width: 100%;
  margin: 0;
  color: var(--blog-muted, #64748b);
  font-size: 17px;
  line-height: 1.8;
  text-shadow: none;
}

.detail-hero .page-kicker,
.detail-hero .detail-meta,
.detail-hero .tag-row {
  text-shadow: none;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.detail-hero .detail-meta span,
.detail-page[data-blog-layout='notebook'] .detail-hero .detail-meta span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 28px;
  padding: 4px 8px;
  border: none;
  background: transparent;
  color: var(--blog-muted, #667085);
  font-size: 13px;
  font-weight: 650;
}

.detail-hero .detail-meta span svg {
  flex-shrink: 0;
  display: block;
}

.detail-hero .detail-meta .original-badge {
  border-radius: 4px;
  background: color-mix(in srgb, var(--blog-accent, #2563eb) 10%, #ffffff);
  color: var(--blog-accent, #2563eb);
  font-weight: 800;
}

.detail-hero .tag-row span,
.detail-page[data-blog-layout='notebook'] .detail-hero .tag-row span {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 4px 9px;
  border-radius: 6px;
  border: 1px solid color-mix(in srgb, var(--blog-accent, #2563eb) 14%, transparent);
  background: color-mix(in srgb, var(--blog-accent, #2563eb) 7%, transparent);
  color: var(--blog-accent, #2563eb);
  font-size: 13px;
  font-weight: 760;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.markdown-body {
  padding: 36px 48px 52px;
  background: var(--blog-paper, #ffffff);
  color: var(--blog-body, #374151);
  font-size: var(--blog-body-size, 16px);
  line-height: var(--blog-body-line, 1.82);
}

.detail-page[data-blog-layout='gallery'] .markdown-body {
  max-width: none;
  margin-left: 0;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin: 30px 0 12px;
  color: var(--blog-title, #111827);
  font-family: var(--blog-font-heading, inherit);
  font-weight: var(--blog-heading-weight, 780);
  line-height: 1.3;
  scroll-margin-top: 96px;
}

.markdown-body :deep(h1:first-child),
.markdown-body :deep(h2:first-child),
.markdown-body :deep(h3:first-child) {
  margin-top: 0;
}

.markdown-body :deep(h2) {
  padding-top: 8px;
  font-size: 25px;
}

.markdown-body :deep(h3) {
  font-size: 20px;
}

.markdown-body :deep(p),
.markdown-body :deep(li) {
  color: var(--blog-body, #374151);
  font-size: var(--blog-body-size, 16px);
  line-height: var(--blog-body-line, 1.82);
}

.markdown-body :deep(p) {
  margin: 0 0 16px;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin: 0 0 18px;
  padding-left: 24px;
}

.markdown-body :deep(blockquote) {
  margin: 20px 0;
  padding: 14px 18px;
  border: 1px solid color-mix(in srgb, var(--blog-accent, #2563eb) 18%, transparent);
  border-left: 4px solid var(--blog-accent, #2563eb);
  border-radius: var(--blog-radius, 8px);
  background: color-mix(in srgb, var(--blog-accent, #2563eb) 6%, var(--blog-paper, #ffffff));
  color: var(--blog-body, #374151);
}

.markdown-body :deep(a) {
  color: var(--blog-accent, #2563eb);
  font-weight: 720;
}

.markdown-body :deep(code:not(pre code)) {
  padding: 2px 6px;
  border-radius: 5px;
  background: color-mix(in srgb, var(--blog-accent, #2563eb) 8%, var(--blog-paper, #ffffff));
  color: var(--blog-accent, #2563eb);
  font-size: 0.92em;
  font-family: var(--blog-font-mono, ui-monospace, SFMono-Regular, Consolas, monospace);
}

.markdown-body :deep(pre) {
  max-width: 100%;
  overflow-x: auto;
  margin: 18px 0 24px;
  padding: 16px;
  border: 1px solid color-mix(in srgb, var(--blog-line, #e5e7eb) 86%, transparent);
  border-radius: var(--blog-radius, 8px);
  background: color-mix(in srgb, var(--blog-title, #111827) 7%, var(--blog-paper, #ffffff));
  color: var(--blog-title, #111827);
}

.markdown-body :deep(pre code) {
  padding: 0;
  background: transparent;
  color: inherit;
  font-family: var(--blog-font-mono, ui-monospace, SFMono-Regular, Consolas, monospace);
}

.markdown-body :deep(img) {
  display: block;
  max-width: 100%;
  height: auto;
  margin: 20px auto;
  border-radius: var(--blog-radius, 8px);
}

.markdown-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 18px 0 26px;
  overflow: hidden;
  border: 1px solid color-mix(in srgb, var(--blog-line, #e5e7eb) 90%, transparent);
  border-radius: var(--blog-radius, 8px);
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  padding: 12px 14px;
  border: 1px solid color-mix(in srgb, var(--blog-line, #e5e7eb) 90%, transparent);
  text-align: left;
  vertical-align: top;
}

.markdown-body :deep(th) {
  background: color-mix(in srgb, var(--blog-accent, #2563eb) 7%, var(--blog-paper, #ffffff));
  color: var(--blog-title, #111827);
  font-weight: 800;
}

.side-card,
.author-card,
.reaction-card,
.comments-card {
  border: 1px solid color-mix(in srgb, var(--blog-line, #e5e7eb) 86%, transparent);
  border-radius: var(--blog-radius, 8px);
  background: color-mix(in srgb, var(--blog-paper, #ffffff) 96%, #ffffff);
  box-shadow: 0 8px 24px color-mix(in srgb, var(--blog-title, #111827) 4%, transparent);
  backdrop-filter: none;
}

.side-card,
.author-card,
.reaction-card {
  padding: 16px;
}

.side-card h2,
.reaction-card h2 {
  margin: 0 0 12px;
  color: var(--blog-title, #111827);
  font-size: 16px;
  font-weight: 820;
}

.author-card {
  display: grid;
  position: static;
  gap: 14px;
}

.author-card__link {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  color: inherit;
  text-decoration: none;
}

.author-card__avatar {
  width: 56px;
  height: 56px;
  flex: 0 0 auto; /* 防止在 flex 布局中被压缩变形 */
  border-radius: 12px;
  overflow: hidden;
  background: var(--blog-cover-gradient, var(--blog-accent, #2563eb));
  color: #ffffff;
  font-size: 20px;
  font-weight: 860;
}

.author-card__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.author-card__avatar span {
  display: grid;
  width: 100%;
  height: 100%;
  place-items: center;
}

.author-card__info {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.author-card__info strong {
  font-size: 17px;
  line-height: 1.25;
}

.author-card__info span {
  display: -webkit-box;
  overflow: hidden;
  color: var(--blog-muted, #667085);
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.author-card__follow {
  width: 100%;
}

.author-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  border-radius: var(--blog-radius, 8px);
  background: color-mix(in srgb, var(--blog-accent, #2563eb) 5%, var(--blog-paper, #ffffff));
}

.author-stats span {
  display: grid;
  gap: 3px;
  padding: 10px 6px;
  text-align: center;
}

.author-stats strong {
  color: var(--blog-title, #111827);
  font-size: 16px;
}

.author-stats small {
  color: var(--blog-muted, #667085);
  font-size: 12px;
}

.related-card {
  gap: 8px;
}

.related-link {
  display: grid;
  gap: 4px;
  padding: 10px 0;
  border-top: 1px solid color-mix(in srgb, var(--blog-line, #e5e7eb) 78%, transparent);
  text-decoration: none;
}

.related-link span {
  color: var(--blog-accent, #2563eb);
  font-size: 12px;
  font-weight: 800;
}

.related-link strong {
  color: var(--blog-title, #111827);
  font-size: 14px;
  line-height: 1.45;
}

.toc-card {
  position: static;
}

.toc-list {
  display: grid;
}

.toc-link {
  width: 100%;
  padding: 9px 10px;
  border: none;
  border-left: 3px solid transparent;
  background: transparent;
  color: var(--blog-body, #475569);
  cursor: pointer;
  font: inherit;
  font-size: 14px;
  line-height: 1.45;
  text-align: left;
}

.toc-link:hover {
  border-left-color: var(--blog-accent, #2563eb);
  background: color-mix(in srgb, var(--blog-accent, #2563eb) 7%, transparent);
  color: var(--blog-accent, #2563eb);
}

.toc-link--level-3 {
  padding-left: 22px;
  color: var(--blog-muted, #667085);
  font-size: 13px;
}

.toc-empty {
  color: var(--blog-muted, #667085);
  font-size: 13px;
  line-height: 1.6;
}

.reaction-card {
  display: grid;
  gap: 8px;
}

.reaction-card .icon-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  justify-content: flex-start;
  border-radius: var(--blog-radius, 8px);
  line-height: 1;
}

.reaction-card .icon-button svg {
  flex-shrink: 0;
  display: block;
}

.reaction-card .icon-button.is-liked {
  background: color-mix(in srgb, #f43f5e 10%, var(--blog-paper, #ffffff)) !important;
  color: #e11d48 !important;
  border-color: color-mix(in srgb, #f43f5e 25%, transparent) !important;
}

.reaction-card .icon-button.is-favorited {
  background: color-mix(in srgb, #eab308 12%, var(--blog-paper, #ffffff)) !important;
  color: #b45309 !important;
  border-color: color-mix(in srgb, #eab308 25%, transparent) !important;
}

.comments-card {
  grid-column: 2;
  display: grid;
  gap: var(--theme-density-spacing, 16px);
  padding: 18px;
  scroll-margin-top: 96px;
}

.reading-layout--no-left .comments-card {
  grid-column: 1;
}

.comments-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.comments-head span {
  color: var(--blog-muted, #667085);
  font-size: 13px;
  font-weight: 720;
}

.reply-hint {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid color-mix(in srgb, var(--blog-accent, #2563eb) 18%, transparent);
  border-radius: var(--blog-radius, 8px);
  background: color-mix(in srgb, var(--blog-accent, #2563eb) 7%, transparent);
  color: var(--blog-muted, #667085);
  font-size: 13px;
}

.comment-form {
  display: grid;
  gap: 10px;
}

.comment-form textarea {
  width: 100%;
  min-height: 112px;
  padding: 12px 14px;
  border: 1px solid color-mix(in srgb, var(--blog-line, #e5e7eb) 90%, transparent);
  border-radius: var(--blog-radius, 8px);
  outline: none;
  resize: vertical;
  background: var(--blog-paper, #ffffff);
  color: var(--blog-title, #111827);
  font: inherit;
  line-height: 1.65;
}

.comment-form textarea:focus {
  border-color: color-mix(in srgb, var(--blog-accent, #2563eb) 48%, var(--blog-line, #e5e7eb));
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--blog-accent, #2563eb) 12%, transparent);
}

.comment-form__footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.comment-form__footer span {
  color: var(--blog-muted, #667085);
  font-size: 12px;
}

.comment-list {
  display: grid;
  gap: 12px;
}

.comment-empty {
  display: grid;
  gap: 4px;
  padding: 14px;
  border: 1px dashed color-mix(in srgb, var(--blog-line, #e5e7eb) 92%, transparent);
  border-radius: var(--blog-radius, 8px);
  background: color-mix(in srgb, var(--blog-accent, #2563eb) 4%, var(--blog-paper, #ffffff));
}

.comment-empty strong {
  color: var(--blog-title, #111827);
  font-size: 14px;
}

.comment-empty span,
.inline-notice {
  color: var(--blog-muted, #667085);
  font-size: 13px;
}

@media (max-width: 1280px) {
  .reading-layout,
  .detail-page[data-blog-layout='notebook'] .reading-layout,
  .detail-page[data-blog-layout='gallery'] .reading-layout {
    grid-template-columns: minmax(0, 1fr) 270px;
  }

  .reading-layout--no-left,
  .detail-page[data-blog-layout='notebook'] .reading-layout--no-left,
  .detail-page[data-blog-layout='gallery'] .reading-layout--no-left {
    grid-template-columns: minmax(0, 1fr) 270px;
  }

  .article-left-rail {
    grid-column: 1 / -1;
    position: static;
    grid-template-columns: minmax(0, 1fr) minmax(240px, 320px);
  }

  .comments-card {
    grid-column: 1;
  }
}

@media (max-width: 980px) {
  .reading-layout,
  .detail-page[data-blog-layout='notebook'] .reading-layout,
  .detail-page[data-blog-layout='gallery'] .reading-layout {
    grid-template-columns: 1fr;
    width: min(100% - 24px, 760px);
  }

  .article-left-rail,
  .article-right-rail {
    position: static;
    grid-column: auto;
    grid-template-columns: 1fr;
  }

  .comments-card {
    grid-column: auto;
  }

  .detail-panel {
    order: 1;
  }

  .article-left-rail {
    order: 2;
  }

  .article-right-rail {
    order: 3;
  }

  .comments-card {
    order: 4;
  }

  .reaction-card {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .reaction-card h2 {
    grid-column: 1 / -1;
  }

  .reaction-card .icon-button {
    justify-content: center;
  }
}

@media (max-width: 640px) {
  .detail-page {
    padding-top: 14px;
  }

  .detail-back,
  .reading-layout,
  .detail-page[data-blog-layout='notebook'] .reading-layout,
  .detail-page[data-blog-layout='gallery'] .reading-layout {
    width: min(100% - 20px, 760px);
  }

  .detail-hero {
    padding: 22px 18px 18px;
  }

  .detail-hero h1 {
    font-size: 30px;
  }

  .markdown-body {
    padding: 24px 18px 34px;
  }

  .reaction-card {
    grid-template-columns: 1fr;
  }
}

.author-card__actions {
  display: flex;
  gap: 8px;
  width: 100%;
}

.author-card__actions > * {
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  min-height: 32px;
}

.author-stats--four {
  grid-template-columns: repeat(4, 1fr) !important;
}

.author-articles-card {
  gap: 8px;
}

.author-articles-list {
  display: grid;
  gap: 12px;
}

.author-article-link {
  display: grid;
  gap: 4px;
  padding: 8px 0;
  border-bottom: 1px solid color-mix(in srgb, var(--blog-line, #e5e7eb) 78%, transparent);
  text-decoration: none;
  text-align: left;
}

.author-article-link:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.author-article-title {
  color: var(--blog-title, #111827);
  font-size: 14px;
  line-height: 1.4;
  font-weight: 600;
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.author-article-title:hover {
  color: var(--blog-accent, #2563eb);
}

.author-article-views {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--blog-muted, #667085);
  font-size: 12px;
}
</style>

<style>
/* 突破父级布局 .public-main 的宽度限制以展示极宽的正文设计，并确保背景完全透明 */
.public-shell:has(.article-detail-page) .public-main {
  width: 100% !important;
  max-width: none !important;
  margin: 0 !important;
  background: transparent !important;
}
</style>
