<template>
<!-- 作品详情页面布局外壳 -->
<!-- 作品详情页排版布局 -->
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
          <div class="detail-hero__copy">
            <p class="page-kicker">{{ project.projectType }}</p>
            <h1>{{ project.title }}</h1>
            <p v-if="project.description" class="detail-summary">{{ project.description }}</p>
            <div class="detail-meta">
              <span><Archive :size="15" />{{ statusLabel(project.status) }}</span>
              <span v-if="project.recommended"><Sparkles :size="15" />推荐作品</span>
              <span v-if="project.reviewedAt"><CalendarDays :size="15" />{{ formatDate(project.reviewedAt) }}</span>
            </div>
          </div>
          <div class="hero-cover" aria-hidden="true">
            <img v-if="project.coverUrl" :src="project.coverUrl" alt="" loading="lazy" />
            <span v-else>{{ project.projectType.slice(0, 2) }}</span>
          </div>
        </header>

        <section class="stats-strip" aria-label="作品互动数据">
          <span><Eye :size="16" />{{ formatCount(project.viewCount) }} 阅读</span>
          <span><Heart :size="16" />{{ formatCount(project.likeCount) }} 喜欢</span>
          <span><Bookmark :size="16" />{{ formatCount(project.favoriteCount) }} 收藏</span>
          <span><MessageCircle :size="16" />{{ formatCount(project.commentCount) }} 评论</span>
        </section>

        <!-- 技术栈展示区块 -->
        <section class="project-stack" aria-label="技术栈">
          <span v-for="tech in project.techStack" :key="tech">{{ tech }}</span>
        </section>

        <section class="detail-actions" aria-label="作品链接">
          <a v-if="safeDemoUrl" class="button button-filled" :href="safeDemoUrl" target="_blank" rel="noreferrer">
            <ExternalLink :size="16" />
            查看演示
          </a>
          <a v-if="safeGithubUrl" class="button button-tonal" :href="safeGithubUrl" target="_blank" rel="noreferrer">
            <GitBranch :size="16" />
            查看代码
          </a>
          <a v-if="safeVideoUrl" class="button button-tonal" :href="safeVideoUrl" target="_blank" rel="noreferrer">
            <PlayCircle :size="16" />
            视频记录
          </a>
        </section>

        <section class="project-extras" aria-label="作品扩展资料">
          <div class="extra-block">
            <div class="section-heading">
              <p class="page-kicker">Screenshots</p>
              <h2>作品截图</h2>
            </div>
            <div v-if="detailExtras.screenshots.length" class="screenshot-grid">
              <figure v-for="screenshot in detailExtras.screenshots" :key="screenshot.imageUrl">
                <img :src="screenshot.imageUrl" :alt="screenshot.caption || project.title" loading="lazy" />
                <figcaption v-if="screenshot.caption">{{ screenshot.caption }}</figcaption>
              </figure>
            </div>
            <p v-else class="extra-empty">后端接入 screenshots、images 或 projectImages 字段后会在这里展示多图。</p>
          </div>

          <div class="extra-block">
            <div class="section-heading">
              <p class="page-kicker">Milestones</p>
              <h2>真实时间线</h2>
            </div>
            <ol v-if="detailExtras.timeline.length" class="milestone-list">
              <li v-for="item in detailExtras.timeline" :key="`${item.phase}-${item.title}`">
                <span>{{ item.phase }}</span>
                <strong>{{ item.title }}</strong>
                <time v-if="item.date">{{ item.date }}</time>
                <p>{{ item.body }}</p>
              </li>
            </ol>
            <p v-else class="extra-empty">后端接入 timeline、milestones 或 processNotes 字段后会替换侧栏的基础档案结构。</p>
          </div>

          <div class="extra-block">
            <div class="section-heading">
              <p class="page-kicker">Resources</p>
              <h2>资源与关联</h2>
            </div>
            <div v-if="resourceLinks.length" class="resource-list">
              <a v-for="link in resourceLinks" :key="link.url" :href="link.url" target="_blank" rel="noreferrer">
                <ExternalLink :size="15" />
                <span>{{ link.label }}</span>
                <small>{{ link.kind }}</small>
              </a>
            </div>
            <p v-else class="extra-empty">后端接入 resources、links 或 projectLinks 字段后会显示文档、复盘和演示资源。</p>
            <RouterLink
              v-if="detailExtras.relatedArticleSlug"
              class="related-link text-link"
              :to="{ name: 'article-detail', params: { slug: detailExtras.relatedArticleSlug } }"
            >
              打开关联文章
            </RouterLink>
          </div>
        </section>

        <div class="markdown-body" v-html="htmlContent" />
      </article>

      <!-- 3D 交互侧栏控制面板 -->
      <aside class="process-panel" data-reveal>
        <section class="side-card">
          <p class="page-kicker">Archive Path</p>
          <h2>档案结构</h2>
          <ol class="timeline-list">
            <li v-for="item in timeline" :key="item.title">
              <span>{{ item.phase }}</span>
              <strong>{{ item.title }}</strong>
              <p>{{ item.body }}</p>
            </li>
          </ol>
        </section>

        <section class="side-card comments-card">
          <p class="page-kicker">Comments</p>
          <h2>作品反馈</h2>
          <div class="reaction-row">
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
          </div>
          <form class="comment-form" @submit.prevent="postComment">
            <textarea v-model="commentDraft" rows="4" placeholder="对这个作品留下反馈，审核后公开展示。" />
            <button class="button button-filled button-compact" :disabled="!canComment" type="submit">
              {{ canComment ? '提交评论' : '登录后评论' }}
            </button>
          </form>
          <div v-if="replyTarget" class="reply-hint">
            <span>回复 {{ replyTarget.username }}</span>
            <button class="text-link" type="button" @click="cancelReply">取消</button>
          </div>
          <div class="comment-list">
            <article
              v-for="comment in comments"
              :key="comment.id"
              class="comment-item"
              :style="{ '--depth': comment.depth }"
            >
              <div class="comment-header">
                <strong>{{ comment.username }}</strong>
                <span class="comment-time">{{ formatDate(comment.createdAt) }}</span>
              </div>
              <p class="comment-body">{{ comment.content }}</p>
              <div class="comment-actions">
                <button class="comment-action-btn" type="button" :disabled="!canComment" @click="replyTo(comment)">
                  <MessageCircle :size="14" />
                  回复
                </button>
                <span v-if="comment.likeCount > 0" class="comment-likes">{{ comment.likeCount }} 赞</span>
              </div>
            </article>
          </div>
          <p v-if="comments.length === 0" class="comment-empty">还没有公开评论。</p>
          <p v-if="commentNotice" class="inline-notice">{{ commentNotice }}</p>
        </section>

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
// 导入所需的组件和 Vue 钩子
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import {
  Archive,
  ArrowLeft,
  Bookmark,
  CalendarDays,
  ExternalLink,
  Eye,
  GitBranch,
  Heart,
  LoaderCircle,
  MessageCircle,
  PlayCircle,
  Sparkles,
} from '@lucide/vue'

import {
  favoriteTarget,
  fetchComments,
  fetchProjectBySlug,
  likeTarget,
  submitComment,
  unfavoriteTarget,
  unlikeTarget,
} from '@/services/content'
import { toUserMessage } from '@/services/http'
import { useCinematicPageMotion } from '@/shared/composables/useCinematicPageMotion'
import { usePageReveal } from '@/shared/composables/usePageReveal'
import { toCssImageUrl } from '@/shared/cssImage'
import { formatMonthDay } from '@/shared/datetime'
import type { CommentSummary, ProjectStatus, ProjectSummary } from '@/shared/domain'
import { renderSafeMarkdown } from '@/shared/markdown'
import { useSessionStore } from '@/shared/sessionStore'

interface DetailScreenshot {
  imageUrl: string
  caption: string
  sortOrder: number
}

interface DetailTimelineItem {
  phase: string
  title: string
  body: string
  date: string
  sortOrder: number
}

interface DetailResourceLink {
  label: string
  url: string
  kind: string
  sortOrder: number
}

interface ProjectDetailExtras {
  screenshots: DetailScreenshot[]
  timeline: DetailTimelineItem[]
  resources: DetailResourceLink[]
  relatedArticleSlug: string
}

// 声明页面的各类交互控制状态变量
const route = useRoute()
const root = ref<HTMLElement | null>(null)
const project = ref<ProjectSummary | null>(null)
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

// 检查用户是否具备发表评论和互动的权限, 即本地缓存中是否存在有效的访问令牌
const canComment = computed(() => Boolean(session.accessToken))

// 将作品的 Markdown 描述正文或简介通过 DOMPurify 净化转译为安全的 HTML 结构以防 XSS 攻击
const htmlContent = computed(() => renderSafeMarkdown(project.value?.contentMarkdown ?? project.value?.description))

// 过滤并规范化外部演示链接, 防止包含恶意脚本的 URL 协议注入
const safeDemoUrl = computed(() => safeExternalUrl(project.value?.demoUrl))

// 过滤并规范化 GitHub 源码仓库链接, 保障前台跳转安全
const safeGithubUrl = computed(() => safeExternalUrl(project.value?.githubUrl))

// 过滤并规范化视频记录地址链接, 确保视频展位数据来源合法
const safeVideoUrl = computed(() => safeExternalUrl(project.value?.videoUrl))

// 动态生成作品详情页的主题封面样式, 提取作品首个标签颜色作为主调色, 并映射封面图到 CSS 变量
const projectCoverStyle = computed(() => ({
  '--detail-accent': project.value?.tags[0]?.color ?? '#6d3fd2',
  '--detail-cover': toCssImageUrl(project.value?.coverUrl),
}))

// 从后端返回的作品对象中反序列化并读取截图、时间线以及扩展链接等 JSON 附加属性
const detailExtras = computed(() => readProjectExtras(project.value))

// 优先采用后端存储的自定义里程碑时间线, 若不存在则调用本地函数稳定生成基础三阶段侧栏档案
const timeline = computed(() => detailExtras.value.timeline.length ? detailExtras.value.timeline : buildTimeline(project.value))

// 合并常规的演示、代码、视频链接与后端自定义配置的扩展链接列表, 并去除重复项
const resourceLinks = computed(() => buildResourceLinks(project.value, detailExtras.value.resources))

// 主函数: 根据路由参数中的唯一 slug 标识向后端获取作品的全部详情数据, 并在就绪后拉取评论列表并播放入场动效
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
    // 异步拉取作品信息, 渲染成功后调用评论加载方法
    project.value = await fetchProjectBySlug(slug.value)
    await loadComments()
  } catch (error) {
    project.value = null
    notice.value = toUserMessage(error, '作品暂时无法打开，请稍后再试')
  } finally {
    isLoading.value = false
    // 释放并触发电影式页面显影过渡动画
    void cinematic.play()
  }
}

// 异步加载当前作品绑定的已通过审核的评论列表, 并根据传参决定是否保留已有的提示信息
async function loadComments(options: { keepCurrentNotice?: boolean } = {}) {
  if (!project.value?.id) {
    comments.value = []
    return
  }
  try {
    // 拉取前 20 条已审核公开的作品反馈评论数据
    const page = await fetchComments({ targetType: 'PROJECT', targetId: project.value.id, pageSize: 20 })
    comments.value = page.records
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

// 提交用户对作品的想法评论或指定楼层的回复, 校验输入内容并在提交成功后重置输入框并刷新评论队列
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
    // 调用通用评论发表接口, 绑定目标为作品且可挂载父评论 ID 形成回复嵌套
    await submitComment({
      targetType: 'PROJECT',
      targetId: project.value.id,
      parentId: replyTarget.value?.id,
      content: commentDraft.value.trim(),
    })
    commentDraft.value = ''
    replyTarget.value = null
    commentNotice.value = '评论已提交，审核通过后会公开展示'
    // 保持提示信息并重新拉取列表以表现最新状态
    await loadComments({ keepCurrentNotice: true })
  } catch (error) {
    commentNotice.value = toUserMessage(error, '评论提交失败')
  }
}

// 切换当前登录用户对该作品的点赞状态, 实现即时喜欢与取消喜欢, 并更新页面响应状态
async function toggleLike() {
  if (!project.value?.id || !canComment.value) return
  try {
    if (liked.value) {
      // 若已喜欢则发送 DELETE 请求注销点赞记录
      await unlikeTarget('PROJECT', project.value.id)
      liked.value = false
    } else {
      // 若未喜欢则发送 POST 请求登记点赞记录
      await likeTarget('PROJECT', project.value.id)
      liked.value = true
    }
  } catch {
    commentNotice.value = '操作失败，请重试'
  }
}

async function toggleFavorite() {
  if (!project.value?.id || !canComment.value) return
  try {
    if (favorited.value) {
      await unfavoriteTarget('PROJECT', project.value.id)
      favorited.value = false
    } else {
      await favoriteTarget('PROJECT', project.value.id)
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

function buildTimeline(value: ProjectSummary | null): DetailTimelineItem[] {
  if (!value) {
    return []
  }
  // 公开 ProjectVO 还没有暴露截图和里程碑, 先用现有字段稳定生成侧栏档案
  return [
    {
      phase: '01',
      title: '定位',
      body: value.description || `${value.title} 已进入公开作品展厅。`,
      date: '',
      sortOrder: 10,
    },
    {
      phase: '02',
      title: '技术',
      body: value.techStack.length ? value.techStack.slice(0, 4).join(' / ') : '等待创作者补充技术栈。',
      date: '',
      sortOrder: 20,
    },
    {
      phase: '03',
      title: '归档',
      body: value.contentMarkdown ? '正文记录了作品过程、取舍和下一步。' : '后续可在 Markdown 中继续补齐过程说明。',
      date: '',
      sortOrder: 30,
    },
  ]
}

function readProjectExtras(value: ProjectSummary | null): ProjectDetailExtras {
  const record = readRecord(value)
  return {
    screenshots: readArray(record.screenshots ?? record.images ?? record.projectImages)
      .map(readScreenshot)
      .filter((item): item is DetailScreenshot => Boolean(item))
      .sort(bySortOrder),
    timeline: readArray(record.timeline ?? record.milestones ?? record.processNotes)
      .map(readTimelineItem)
      .filter((item): item is DetailTimelineItem => Boolean(item))
      .sort(bySortOrder),
    resources: readArray(record.resources ?? record.links ?? record.projectLinks)
      .map(readResourceLink)
      .filter((item): item is DetailResourceLink => Boolean(item))
      .sort(bySortOrder),
    relatedArticleSlug: readRelatedArticleSlug(record),
  }
}

function buildResourceLinks(value: ProjectSummary | null, extras: DetailResourceLink[]): DetailResourceLink[] {
  if (!value) {
    return extras
  }
  const baseLinks: DetailResourceLink[] = [
    { label: '演示地址', url: safeExternalUrl(value.demoUrl), kind: 'DEMO', sortOrder: 10 },
    { label: '代码仓库', url: safeExternalUrl(value.githubUrl), kind: 'GITHUB', sortOrder: 20 },
    { label: '视频记录', url: safeExternalUrl(value.videoUrl), kind: 'VIDEO', sortOrder: 30 },
  ].filter((link) => Boolean(link.url))
  const seen = new Set<string>()
  return [...baseLinks, ...extras].filter((link) => {
    if (seen.has(link.url)) {
      return false
    }
    seen.add(link.url)
    return true
  })
}

function readScreenshot(value: unknown, index: number): DetailScreenshot | null {
  if (typeof value === 'string') {
    const imageUrl = safeAssetUrl(value)
    return imageUrl ? { imageUrl, caption: '', sortOrder: index } : null
  }
  const record = readRecord(value)
  const imageUrl = safeAssetUrl(
    readString(record.imageUrl)
    || readString(record.url)
    || readString(record.publicUrl)
    || readString(record.src),
  )
  if (!imageUrl) {
    return null
  }
  return {
    imageUrl,
    caption: readString(record.caption) || readString(record.title),
    sortOrder: readNumber(record.sortOrder, index),
  }
}

function readTimelineItem(value: unknown, index: number): DetailTimelineItem | null {
  if (typeof value === 'string') {
    return { phase: String(index + 1).padStart(2, '0'), title: '记录', body: value, date: '', sortOrder: index }
  }
  const record = readRecord(value)
  const title = readString(record.title) || readString(record.phase)
  const body = readString(record.body) || readString(record.description) || readString(record.content)
  if (!title && !body) {
    return null
  }
  return {
    phase: readString(record.phase) || String(index + 1).padStart(2, '0'),
    title: title || '过程记录',
    body,
    date: readString(record.date) || readString(record.milestoneDate),
    sortOrder: readNumber(record.sortOrder, index),
  }
}

function readResourceLink(value: unknown, index: number): DetailResourceLink | null {
  const record = readRecord(value)
  const url = safeExternalUrl(readString(record.url) || readString(record.href))
  if (!url) {
    return null
  }
  return {
    label: readString(record.label) || readString(record.title) || '资源链接',
    url,
    kind: readString(record.kind) || readString(record.linkType) || 'LINK',
    sortOrder: readNumber(record.sortOrder, index),
  }
}

function readRelatedArticleSlug(record: Record<string, unknown>): string {
  const direct = readString(record.relatedArticleSlug)
  if (direct) {
    return direct
  }
  const article = readRecord(record.relatedArticle)
  return readString(article.slug)
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

function readNumber(value: unknown, fallback: number): number {
  return typeof value === 'number' && Number.isFinite(value) ? value : fallback
}

function bySortOrder(left: { sortOrder: number }, right: { sortOrder: number }): number {
  return left.sortOrder - right.sortOrder
}

function statusLabel(status: ProjectStatus): string {
  const labels: Record<ProjectStatus, string> = {
    DRAFT: '草稿',
    PENDING_REVIEW: '待审核',
    VISIBLE: '公开展示',
    HIDDEN: '隐藏',
    REJECTED: '已驳回',
    ARCHIVED: '已归档',
  }
  return labels[status] ?? status
}

function formatDate(value?: string | null): string {
  return formatMonthDay(value, '刚刚')
}

function formatCount(value?: number | null): string {
  return new Intl.NumberFormat('zh-CN', { notation: 'compact' }).format(value ?? 0)
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

function safeAssetUrl(value?: string | null): string {
  const raw = value?.trim() ?? ''
  if (!raw) {
    return ''
  }
  if (raw.startsWith('/')) {
    return raw
  }
  return safeExternalUrl(raw)
}

onMounted(loadProject)
watch(slug, loadProject)
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

.project-record {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: var(--theme-density-spacing, 16px);
  align-items: start;
}

.detail-panel,
.side-card {
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm);
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--tone-panel-solid) 92%, transparent), color-mix(in srgb, var(--tone-panel-solid) 78%, transparent)),
    color-mix(in srgb, var(--tone-panel-solid) 88%, transparent);
  box-shadow: var(--tone-shadow);
  backdrop-filter: blur(22px);
}

.detail-panel {
  overflow: hidden;
}

.detail-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  align-items: end;
  gap: 28px;
  padding: clamp(28px, 5vw, 54px);
  overflow: hidden;
  background:
    linear-gradient(135deg, rgba(6, 8, 18, 0.96), rgba(6, 8, 18, 0.84) 48%, rgba(6, 8, 18, 0.7)),
    var(--detail-cover),
    linear-gradient(135deg, var(--detail-accent), #10131f);
  background-position: center;
  background-size: cover;
  color: #f8fafc;
}

.detail-hero::before {
  content: "";
  position: absolute;
  inset: 16px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background:
    radial-gradient(circle at 18% 12%, rgba(255, 255, 255, 0.2), transparent 26%),
    repeating-linear-gradient(90deg, rgba(255, 255, 255, 0.06) 0 1px, transparent 1px 18px);
  clip-path: polygon(0 0, calc(100% - 40px) 0, 100% 40px, 100% 100%, 40px 100%, 0 calc(100% - 40px));
  pointer-events: none;
}

.detail-hero > * {
  position: relative;
  z-index: 1;
}

.detail-hero__copy {
  display: grid;
  gap: 14px;
}

.detail-hero h1 {
  max-width: 820px;
  margin: 0;
  font-size: clamp(34px, 5vw, 58px);
  font-weight: 860;
  line-height: 1.04;
  text-shadow: 0 16px 34px rgba(0, 0, 0, 0.42);
}

.detail-summary {
  max-width: 760px;
  margin: 0;
  color: rgba(248, 250, 252, 0.82);
  font-size: 17px;
  line-height: 1.74;
}

.detail-meta,
.stats-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.detail-meta span,
.stats-strip span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 32px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 740;
}

.detail-meta span {
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(6, 8, 18, 0.42);
  color: rgba(248, 250, 252, 0.88);
}

.hero-cover {
  display: grid;
  width: 260px;
  aspect-ratio: 4 / 3;
  place-items: center;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.12);
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.24);
}

.hero-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-cover span {
  color: rgba(255, 255, 255, 0.72);
  font-size: 44px;
  font-weight: 900;
}

.stats-strip {
  padding: 18px clamp(24px, 4vw, 48px) 0;
}

.stats-strip span {
  background: rgba(49, 91, 255, 0.08);
  color: var(--tone-muted);
}

.project-stack,
.detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 18px clamp(24px, 4vw, 48px) 0;
}

.project-stack span {
  padding: 6px 10px;
  border-radius: 6px;
  background: rgba(0, 124, 114, 0.1);
  color: #055f57;
  font-size: 12px;
  font-weight: 740;
}

.project-extras {
  display: grid;
  gap: 4px;
  padding: 24px clamp(24px, 4vw, 48px) 0;
}

.extra-block {
  display: grid;
  gap: 14px;
  padding: 18px 0;
  border-top: 1px solid var(--tone-line);
}

.section-heading {
  display: grid;
  gap: 6px;
}

.section-heading h2 {
  margin: 0;
  color: var(--tone-ink);
  font-size: 24px;
  line-height: 1.18;
}

.screenshot-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.screenshot-grid figure {
  display: grid;
  gap: 8px;
  margin: 0;
}

.screenshot-grid img {
  width: 100%;
  aspect-ratio: 16 / 10;
  border: 1px solid var(--tone-line);
  border-radius: 8px;
  object-fit: cover;
}

.screenshot-grid figcaption {
  color: var(--tone-muted);
  font-size: 13px;
}

.extra-empty {
  margin: 0;
  padding: 14px 16px;
  border: 1px dashed var(--tone-line-strong);
  border-radius: var(--app-radius-sm, 8px);
  background: color-mix(in srgb, var(--tone-panel-solid) 46%, transparent);
  color: var(--tone-muted);
  font-size: 13px;
  line-height: 1.62;
}

.milestone-list {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.milestone-list li {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto;
  gap: 8px 12px;
  align-items: start;
  padding: 12px 0;
  border-bottom: 1px solid var(--tone-line);
}

.milestone-list li:last-child {
  border-bottom: 0;
}

.milestone-list span {
  color: var(--tone-primary);
  font-size: 12px;
  font-weight: 860;
}

.milestone-list strong {
  color: var(--tone-ink);
}

.milestone-list time {
  color: var(--tone-faint);
  font-size: 12px;
}

.milestone-list p {
  grid-column: 2 / -1;
  margin: 0;
  color: var(--tone-muted);
  line-height: 1.62;
}

.resource-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.resource-list a {
  display: grid;
  gap: 8px;
  min-height: 112px;
  align-content: end;
  padding: 14px;
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm, 8px);
  background: color-mix(in srgb, var(--tone-panel-solid) 58%, transparent);
  color: var(--tone-ink);
}

.resource-list svg {
  color: var(--tone-primary);
}

.resource-list span {
  font-weight: 760;
}

.resource-list small {
  color: var(--tone-faint);
  font-size: 12px;
}

.related-link {
  justify-self: start;
}

.markdown-body {
  display: grid;
  gap: var(--theme-density-spacing, 18px);
  padding: 34px clamp(24px, 4vw, 48px) 52px;
  color: var(--tone-ink);
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
  color: var(--tone-muted);
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

.side-card {
  display: grid;
  gap: 12px;
  padding: 18px;
}

.side-card h2 {
  margin: 0;
  color: var(--tone-ink);
  font-size: 22px;
}

.timeline-list {
  display: grid;
  gap: 14px;
  margin: 0;
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

.timeline-list strong {
  color: var(--tone-ink);
  font-size: 14px;
}

.timeline-list p {
  margin: 0;
  color: var(--tone-muted);
  font-size: 13px;
  line-height: 1.62;
}

.reaction-row {
  display: flex;
  gap: 8px;
}

.reaction-row .icon-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 36px;
  padding: 0 12px;
  border: 1px solid var(--tone-line);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.62);
  color: var(--tone-muted);
  cursor: pointer;
  font-size: 13px;
}

.reaction-row .icon-button:disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

.reaction-row .icon-button.is-active {
  color: var(--tone-primary);
  background: rgba(49, 91, 255, 0.1);
  border-color: rgba(49, 91, 255, 0.3);
}

.comment-form {
  display: grid;
  gap: 10px;
}

.comment-form textarea {
  width: 100%;
  min-height: 112px;
  resize: vertical;
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm, 8px);
  padding: 12px;
  background: color-mix(in srgb, var(--tone-panel-solid) 82%, transparent);
  color: var(--tone-ink);
  line-height: 1.6;
}

.comment-list {
  display: grid;
  gap: 10px;
}

.comment-item {
  display: grid;
  gap: 6px;
  margin-left: calc(var(--depth, 0) * 16px);
  padding: 12px;
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm, 8px);
  background: color-mix(in srgb, var(--tone-panel-solid) 62%, transparent);
}

.comment-header,
.comment-actions,
.reply-hint {
  display: flex;
  align-items: center;
  gap: 10px;
}

.comment-header strong {
  color: var(--tone-ink);
  font-size: 14px;
}

.comment-time,
.comment-likes {
  color: var(--tone-faint);
  font-size: 12px;
}

.comment-body {
  margin: 0;
  color: #475569;
  font-size: 14px;
  line-height: 1.6;
}

.comment-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 0;
  border: none;
  background: transparent;
  color: var(--tone-muted);
  cursor: pointer;
  font-size: 12px;
}

.reply-hint {
  justify-content: space-between;
  padding: 8px 12px;
  border-radius: 6px;
  background: rgba(49, 91, 255, 0.06);
  color: var(--tone-muted);
  font-size: 13px;
}

.comment-empty {
  margin: 0;
  color: var(--tone-muted);
  font-size: 14px;
}

.inline-notice {
  margin: 0;
  padding: 10px 12px;
  border-left: 3px solid var(--tone-coral);
  background: rgba(194, 95, 58, 0.08);
  color: #754226;
  font-size: 13px;
  line-height: 1.55;
}

@media (max-width: 1020px) {
  .project-record,
  .detail-hero {
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

  .hero-cover {
    width: 100%;
  }

  .screenshot-grid,
  .resource-list,
  .milestone-list li {
    grid-template-columns: 1fr;
  }

  .milestone-list p {
    grid-column: 1;
  }
}
</style>
