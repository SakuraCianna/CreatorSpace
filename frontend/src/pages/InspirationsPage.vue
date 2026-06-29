<template>
<!-- 瀑布流灵感板页面 -->
<!-- 创作者公开灵感墙的瀑布流展厅 -->
  <section ref="root" class="wall-page">
    <header class="wall-hero page-hero" data-reveal>
      <div class="hero-copy">
        <p class="page-kicker">灵感收集墙</p>
        <h1>灵感墙</h1>
        <p>摘句、提示词、图片、代码和链接都在这里先被保存成碎片，再慢慢长成文章或作品。</p>
      </div>
      <form class="wall-search" @submit.prevent="loadInspirations">
        <Search :size="18" />
        <input v-model="keyword" placeholder="搜索灵感、来源或提示词" aria-label="搜索灵感" />
        <button class="button button-filled button-compact" type="submit">检索</button>
      </form>
    </header>

    <section class="wall-toolbar" data-reveal>
      <button
        v-for="item in filters"
        :key="item.value"
        class="topic-chip"
        :class="{ 'is-active': activeType === item.value }"
        type="button"
        @click="selectType(item.value)"
      >
        <component :is="item.icon" :size="15" />
        {{ item.label }}
        <span v-if="hasCompleteTypeCounts">{{ typeCount(item.value) }}</span>
      </button>
    </section>

    <div v-if="isLoading && !hasLoaded" class="empty-state showcase-state" data-reveal>
      <LoaderCircle class="spin" :size="24" />
      <h2>正在铺开灵感卡片</h2>
    </div>

    <section
      v-else-if="featuredCard"
      class="featured-inspiration"
      data-reveal
      role="button"
      tabindex="0"
      :aria-label="`查看灵感：${featuredCard.title}`"
      @click="openCard(featuredCard, $event)"
      @keydown.enter="openCard(featuredCard, $event)"
      @keydown.space="openCard(featuredCard, $event)"
    >
      <div class="featured-inspiration__visual" :style="cardStyle(featuredCard)">
        <img v-if="featuredCard.imageUrl" :src="featuredCard.imageUrl" alt="" loading="lazy" />
        <component :is="typeIcon(featuredCard.cardType)" v-else :size="42" />
      </div>
      <div class="featured-inspiration__body">
        <div class="card-flags">
          <span class="card-kind">
            <component :is="typeIcon(featuredCard.cardType)" :size="14" />
            {{ typeName(featuredCard.cardType) }}
          </span>
          <span
            v-if="showVisibilityBadge"
            class="visibility-badge"
            :class="{ 'visibility-badge--private': featuredCard.isPublic === false }"
          >
            <component :is="visibilityIcon(featuredCard)" :size="13" />
            {{ visibilityLabel(featuredCard) }}
          </span>
        </div>
        <h2>{{ featuredCard.title }}</h2>
        <p>{{ featuredCard.content || '这张灵感卡还没有正文。' }}</p>
        <div class="tag-row">
          <span v-for="tag in featuredCard.tags.slice(0, 4)" :key="tag.id">#{{ tag.name }}</span>
        </div>
        <div v-if="relationItems(featuredCard).length" class="relation-list">
          <template v-for="relation in relationItems(featuredCard)" :key="relation.key">
            <RouterLink v-if="relation.to" class="relation-chip" :to="relation.to" @click.stop>
              <GitBranch :size="14" />
              {{ relation.label }}
            </RouterLink>
            <span v-else class="relation-chip">
              <GitBranch :size="14" />
              {{ relation.label }}
            </span>
          </template>
        </div>
        <a
          v-if="safeSource(featuredCard.sourceUrl)"
          :href="safeSource(featuredCard.sourceUrl)"
          target="_blank"
          rel="noreferrer"
          @click.stop
        >
          <ExternalLink :size="15" />
          来源
        </a>
      </div>
    </section>

    <div v-if="!isLoading && cards.length === 0" class="empty-state showcase-state" data-reveal>
      <h2>没有匹配的灵感卡</h2>
      <p>换个类型或关键词试试。</p>
    </div>

    <div v-else class="inspiration-masonry">
      <article
        v-for="card in masonryCards"
        :key="card.id"
        class="inspiration-card"
        :class="`inspiration-card--${card.cardType.toLowerCase()}`"
        :style="cardStyle(card)"
        data-reveal
        role="button"
        tabindex="0"
        :aria-label="`查看灵感：${card.title}`"
        @click="openCard(card, $event)"
        @keydown.enter="openCard(card, $event)"
        @keydown.space="openCard(card, $event)"
      >
        <img v-if="card.imageUrl" :src="card.imageUrl" alt="" loading="lazy" />
        <div class="card-head">
          <span class="card-kind">
            <component :is="typeIcon(card.cardType)" :size="14" />
            {{ typeName(card.cardType) }}
          </span>
          <div class="card-badges">
            <span
              v-if="showVisibilityBadge"
              class="visibility-badge"
              :class="{ 'visibility-badge--private': card.isPublic === false }"
            >
              <component :is="visibilityIcon(card)" :size="13" />
              {{ visibilityLabel(card) }}
            </span>
            <span class="card-order">优先级 {{ card.sortOrder }}</span>
          </div>
        </div>
        <h2>{{ card.title }}</h2>
        <pre v-if="card.cardType === 'CODE'">{{ card.content }}</pre>
        <p v-else>{{ card.content || '这张灵感卡还没有正文。' }}</p>
        <div v-if="card.createdAt" class="card-meta">
          <span><CalendarDays :size="14" />{{ formatDate(card.createdAt) }}</span>
        </div>
        <div class="tag-row">
          <span v-for="tag in card.tags.slice(0, 3)" :key="tag.id">#{{ tag.name }}</span>
        </div>
        <div v-if="relationItems(card).length" class="relation-list">
          <template v-for="relation in relationItems(card)" :key="relation.key">
            <RouterLink v-if="relation.to" class="relation-chip" :to="relation.to" @click.stop>
              <GitBranch :size="14" />
              {{ relation.label }}
            </RouterLink>
            <span v-else class="relation-chip">
              <GitBranch :size="14" />
              {{ relation.label }}
            </span>
          </template>
        </div>
        <a v-if="safeSource(card.sourceUrl)" :href="safeSource(card.sourceUrl)" target="_blank" rel="noreferrer" @click.stop>
          <ExternalLink :size="15" />
          来源
        </a>
      </article>
    </div>

    <Teleport to="body">
      <div
        v-if="selectedCard"
        ref="dialogRef"
        class="inspiration-dialog"
        role="dialog"
        aria-modal="true"
        :aria-labelledby="dialogTitleId"
        @keydown.tab="trapDialogFocus"
      >
        <button class="dialog-backdrop" type="button" tabindex="-1" aria-label="关闭灵感详情" @click="closeCard" />
        <article class="dialog-panel" :style="cardStyle(selectedCard)">
          <header class="dialog-header">
            <div>
              <p class="page-kicker">Inspiration Detail</p>
              <h2 :id="dialogTitleId">{{ selectedCard.title }}</h2>
            </div>
            <button ref="dialogCloseRef" class="dialog-close" type="button" aria-label="关闭灵感详情" @click="closeCard">
              <X :size="18" />
            </button>
          </header>

          <div class="dialog-content">
            <figure v-if="selectedCard.imageUrl" class="dialog-media">
              <img :src="selectedCard.imageUrl" :alt="selectedCard.title" />
            </figure>
            <div v-else class="dialog-media dialog-media--empty" aria-hidden="true">
              <component :is="typeIcon(selectedCard.cardType)" :size="54" />
            </div>

            <section class="dialog-body">
              <div class="card-flags">
                <span class="card-kind">
                  <component :is="typeIcon(selectedCard.cardType)" :size="14" />
                  {{ typeName(selectedCard.cardType) }}
                </span>
                <span
                  v-if="showVisibilityBadge"
                  class="visibility-badge"
                  :class="{ 'visibility-badge--private': selectedCard.isPublic === false }"
                >
                  <component :is="visibilityIcon(selectedCard)" :size="13" />
                  {{ visibilityLabel(selectedCard) }}
                </span>
                <span v-if="selectedCard.createdAt" class="card-meta">
                  <CalendarDays :size="14" />
                  {{ formatDate(selectedCard.createdAt) }}
                </span>
              </div>

              <pre v-if="selectedCard.cardType === 'CODE'" class="dialog-code">{{ selectedCard.content || '这张灵感卡还没有正文。' }}</pre>
              <p v-else class="dialog-text">{{ selectedCard.content || '这张灵感卡还没有正文。' }}</p>

              <div v-if="selectedCard.tags.length" class="tag-row">
                <span v-for="tag in selectedCard.tags" :key="tag.id">#{{ tag.name }}</span>
              </div>

              <div v-if="relationItems(selectedCard).length" class="dialog-section">
                <h3>关联内容</h3>
                <div class="relation-list">
                  <template v-for="relation in relationItems(selectedCard)" :key="relation.key">
                    <RouterLink v-if="relation.to" class="relation-chip" :to="relation.to" @click="closeCard">
                      <GitBranch :size="14" />
                      {{ relation.label }}
                    </RouterLink>
                    <span v-else class="relation-chip">
                      <GitBranch :size="14" />
                      {{ relation.label }}
                    </span>
                  </template>
                </div>
              </div>

              <div class="dialog-actions">
                <a
                  v-if="safeSource(selectedCard.sourceUrl)"
                  class="button button-tonal button-compact"
                  :href="safeSource(selectedCard.sourceUrl)"
                  target="_blank"
                  rel="noreferrer"
                >
                  <ExternalLink :size="15" />
                  打开来源
                </a>
                <button class="button button-filled button-compact" type="button" @click="closeCard">回到灵感墙</button>
              </div>
            </section>
          </div>
        </article>
      </div>
    </Teleport>

    <p v-if="notice" class="inline-notice">{{ notice }}</p>
  </section>
</template>

<script setup lang="ts">
// 导入所需的组件和 Vue 钩子
import { computed, nextTick, onBeforeUnmount, onMounted, ref, type Component } from 'vue'
import { RouterLink, type RouteLocationRaw } from 'vue-router'
import {
  BookOpen,
  CalendarDays,
  Code2,
  Eye,
  ExternalLink,
  GitBranch,
  Image,
  Link,
  LoaderCircle,
  Lock,
  MessageSquareQuote,
  Pencil,
  Search,
  StickyNote,
  X,
} from '@lucide/vue'

import { fetchAdminInspirations, fetchInspirations } from '@/services/content'
import { toUserMessage } from '@/services/http'
import { useCinematicPageMotion } from '@/shared/composables/useCinematicPageMotion'
import { usePageReveal } from '@/shared/composables/usePageReveal'
import { formatMonthDay } from '@/shared/datetime'
import { useSessionStore } from '@/shared/sessionStore'
import type { InspirationCard, InspirationRelation, InspirationType } from '@/shared/domain'

type InspirationFilter = InspirationType | 'ALL'

interface InspirationFilterItem {
  value: InspirationFilter
  label: string
  icon: Component
}

interface RelationChip {
  key: string
  label: string
  to?: RouteLocationRaw
}

// 初始化灵感墙的查询参数和卡片数据列表
const root = ref<HTMLElement | null>(null)
const cards = ref<InspirationCard[]>([])
const countCards = ref<InspirationCard[]>([])
const hasCompleteTypeCounts = ref(false)
const keyword = ref('')
const activeType = ref<InspirationFilter>('ALL')
const isLoading = ref(true)
const hasLoaded = ref(false)
const notice = ref('')
const selectedCard = ref<InspirationCard | null>(null)
const dialogRef = ref<HTMLElement | null>(null)
const dialogCloseRef = ref<HTMLButtonElement | null>(null)
const dialogTitleId = 'inspiration-dialog-title'
const cinematic = useCinematicPageMotion(root)
const session = useSessionStore()
let hasPlayedIntro = false
let lastTriggerElement: HTMLElement | null = null

usePageReveal(root)

const filters: InspirationFilterItem[] = [
  { value: 'ALL', label: '全部', icon: StickyNote },
  { value: 'TEXT', label: '摘句', icon: MessageSquareQuote },
  { value: 'PROMPT', label: '提示词', icon: StickyNote },
  { value: 'IMAGE', label: '图片', icon: Image },
  { value: 'CODE', label: '代码', icon: Code2 },
  { value: 'LINK', label: '链接', icon: Link },
  { value: 'SKETCH', label: '草图', icon: Pencil },
  { value: 'REFERENCE', label: '参考资料', icon: BookOpen },
]

const sortedCards = computed(() => [...cards.value].sort((left, right) => right.sortOrder - left.sortOrder))
const featuredCard = computed(() => sortedCards.value[0] ?? null)
const masonryCards = computed(() => sortedCards.value.slice(featuredCard.value ? 1 : 0))
const showVisibilityBadge = computed(() => Boolean(session.accessToken))

function openCard(card: InspirationCard, event?: Event) {
  if (event && isInteractiveEventTarget(event.target, event.currentTarget)) {
    return
  }
  event?.preventDefault()
  lastTriggerElement = event?.currentTarget instanceof HTMLElement ? event.currentTarget : null
  selectedCard.value = card
  document.body.classList.add('is-dialog-open')
  nextTick(() => {
    dialogCloseRef.value?.focus()
  })
}

function closeCard() {
  selectedCard.value = null
  document.body.classList.remove('is-dialog-open')
  nextTick(() => {
    lastTriggerElement?.focus()
    lastTriggerElement = null
  })
}

function handleDialogKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && selectedCard.value) {
    closeCard()
  }
}

function trapDialogFocus(event: KeyboardEvent) {
  const panel = dialogRef.value
  if (!panel) {
    return
  }
  const focusableItems = Array.from(
    panel.querySelectorAll<HTMLElement>(
      'a[href], button:not([disabled]):not(.dialog-backdrop), textarea:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])',
    ),
  ).filter((item) => item.offsetParent !== null)
  if (focusableItems.length === 0) {
    event.preventDefault()
    return
  }
  const firstItem = focusableItems[0]
  const lastItem = focusableItems[focusableItems.length - 1]
  if (event.shiftKey && document.activeElement === firstItem) {
    event.preventDefault()
    lastItem.focus()
  } else if (!event.shiftKey && document.activeElement === lastItem) {
    event.preventDefault()
    firstItem.focus()
  }
}

function isInteractiveEventTarget(value: EventTarget | null, boundary: EventTarget | null): boolean {
  if (!(value instanceof Element)) {
    return false
  }
  const interactiveElement = value.closest('a, button, input, textarea, select, [role="button"]')
  return Boolean(interactiveElement && interactiveElement !== boundary)
}

// 切换灵感过滤分类页签并重新发起数据拉取
function selectType(type: InspirationFilter) {
  if (activeType.value === type) {
    return
  }
  activeType.value = type
  loadInspirations()
}

// 并发发起当前分类列表拉取和全部类别总数统计拉取, 数据就绪后触发入场显影动效
async function loadInspirations() {
  isLoading.value = true
  notice.value = ''
  try {
    const loadCards = session.isAdmin ? fetchAdminInspirations : fetchInspirations
    const [pageResult, countResult] = await Promise.allSettled([
      loadCards({ keyword: keyword.value, type: activeType.value, pageSize: 30 }),
      loadCards({ keyword: keyword.value, type: 'ALL', pageSize: 200 }),
    ])
    if (pageResult.status === 'rejected') {
      throw pageResult.reason
    }
    cards.value = pageResult.value.records
    if (countResult.status === 'fulfilled') {
      countCards.value = countResult.value.records
      hasCompleteTypeCounts.value = true
    } else if (activeType.value === 'ALL') {
      countCards.value = pageResult.value.records
      hasCompleteTypeCounts.value = true
    } else {
      countCards.value = []
      hasCompleteTypeCounts.value = false
    }
  } catch (error) {
    cards.value = []
    countCards.value = []
    hasCompleteTypeCounts.value = false
    notice.value = toUserMessage(error, '灵感接口暂不可用，请稍后再试')
  } finally {
    isLoading.value = false
    hasLoaded.value = true
    if (!hasPlayedIntro) {
      hasPlayedIntro = true
      void cinematic.play()
    }
  }
}

// 根据拉取的灵感集合数据过滤出指定类型卡片的具体计数
function typeCount(type: InspirationFilter): number {
  const source = countCards.value.length > 0 ? countCards.value : cards.value
  if (type === 'ALL') {
    return source.length
  }
  return source.filter((card) => card.cardType === type).length
}

// 动态绑定单张灵感卡片的色调变体样式
function cardStyle(card: InspirationCard) {
  return {
    '--card-accent': card.color ?? typeColor(card.cardType),
  }
}

function typeIcon(type: InspirationType): Component {
  const icons: Record<InspirationType, Component> = {
    TEXT: MessageSquareQuote,
    PROMPT: StickyNote,
    IMAGE: Image,
    CODE: Code2,
    LINK: Link,
    SKETCH: Pencil,
    REFERENCE: BookOpen,
  }
  return icons[type]
}

function typeName(type: InspirationType): string {
  const names: Record<InspirationType, string> = {
    TEXT: '摘句',
    PROMPT: '提示词',
    IMAGE: '图片',
    CODE: '代码',
    LINK: '链接',
    SKETCH: '草图',
    REFERENCE: '参考资料',
  }
  return names[type]
}

function typeColor(type: InspirationType): string {
  const colors: Record<InspirationType, string> = {
    TEXT: '#0f766e',
    PROMPT: '#6d3fd2',
    IMAGE: '#c25f3a',
    CODE: '#0b57d0',
    LINK: '#805610',
    SKETCH: '#984061',
    REFERENCE: '#52652c',
  }
  return colors[type]
}

function visibilityIcon(card: InspirationCard): Component {
  return card.isPublic === false ? Lock : Eye
}

function visibilityLabel(card: InspirationCard): string {
  return card.isPublic === false ? '私密' : '公开'
}

function relationItems(card: InspirationCard): RelationChip[] {
  return (card.relations ?? []).map((relation, index) => ({
    key: `${relation.targetType}-${relation.targetId}-${relation.relationType}-${index}`,
    label: relationLabel(relation),
    to: relationRoute(relation),
  }))
}

function relationLabel(relation: InspirationRelation): string {
  const targetName = relationTargetName(relation.targetType)
  const title = relation.targetTitle?.trim() || `${targetName} #${relation.targetId}`
  if (relation.relationType === 'SEED') {
    return `源于${targetName}：${title}`
  }
  if (relation.relationType === 'DERIVED_FROM') {
    return relation.targetType === 'PROJECT' ? `转化为作品：${title}` : `衍生自${targetName}：${title}`
  }
  return `参考${targetName}：${title}`
}

function relationTargetName(targetType: InspirationRelation['targetType']): string {
  const names: Record<InspirationRelation['targetType'], string> = {
    ARTICLE: '文章',
    PROJECT: '作品',
    INSPIRATION: '灵感',
  }
  return names[targetType]
}

function relationRoute(relation: InspirationRelation): RouteLocationRaw | undefined {
  const slug = relation.targetSlug?.trim()
  if (!slug) {
    return undefined
  }
  if (relation.targetType === 'ARTICLE') {
    return { name: 'article-detail', params: { slug } }
  }
  if (relation.targetType === 'PROJECT') {
    return { name: 'project-detail', params: { slug } }
  }
  return undefined
}

function safeSource(value?: string | null): string {
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

function formatDate(value?: string | null): string {
  return formatMonthDay(value, '')
}

onMounted(loadInspirations)

onMounted(() => {
  window.addEventListener('keydown', handleDialogKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleDialogKeydown)
  document.body.classList.remove('is-dialog-open')
})
</script>

<style scoped>
.wall-page {
  display: grid;
  gap: 18px;
  padding: 46px 0 84px;
}

.wall-hero {
  --hero-accent: #007c72;
  --hero-accent-2: #984061;
  --hero-mark: "03";
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 420px);
  align-items: center;
  gap: 24px;
  min-height: clamp(230px, 24vw, 308px);
  padding: clamp(24px, 3vw, 42px);
  overflow: hidden;
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.9), rgba(255, 255, 255, 0.6)),
    radial-gradient(circle at 16% 20%, color-mix(in srgb, var(--hero-accent) 18%, transparent), transparent 34%),
    radial-gradient(circle at 88% 18%, color-mix(in srgb, var(--hero-accent-2) 18%, transparent), transparent 28%),
    linear-gradient(120deg, rgba(49, 91, 255, 0.08), rgba(194, 95, 58, 0.08), rgba(0, 124, 114, 0.1));
  box-shadow: var(--tone-shadow);
  isolation: isolate;
}

.wall-hero::before {
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
  pointer-events: none;
}

.wall-hero::after {
  content: var(--hero-mark);
  position: absolute;
  top: clamp(20px, 4vw, 42px);
  right: clamp(24px, 6vw, 88px);
  z-index: 0;
  color: color-mix(in srgb, var(--hero-accent) 16%, transparent);
  font-size: clamp(86px, 12vw, 180px);
  font-weight: 900;
  line-height: 0.8;
  pointer-events: none;
}

.wall-hero > * {
  position: relative;
  z-index: 1;
}

.hero-copy {
  display: grid;
  gap: 14px;
}

.hero-copy h1 {
  position: relative;
  width: fit-content;
  max-width: 100%;
  margin: 0;
  padding-top: 24px;
  color: var(--tone-ink);
  font-size: clamp(36px, 4vw, 54px);
  font-weight: 860;
  line-height: 1.08;
}

.hero-copy h1::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: clamp(110px, 18vw, 240px);
  height: 8px;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--hero-accent), var(--hero-accent-2), transparent);
}

.hero-copy p:not(.page-kicker) {
  max-width: 640px;
  margin: 0;
  color: var(--tone-muted);
  font-size: 16px;
  line-height: 1.72;
}

.wall-search {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  min-height: 58px;
  padding: 8px 8px 8px 16px;
  border: 1px solid color-mix(in srgb, var(--hero-accent) 22%, var(--tone-line-strong));
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 18px 44px rgba(32, 33, 36, 0.12);
  backdrop-filter: blur(18px);
}

.wall-search svg {
  color: var(--hero-accent);
}

.wall-search input {
  width: 100%;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--tone-ink);
}

.wall-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.topic-chip {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-height: 38px;
  padding: 0 14px;
  border: 1px solid var(--tone-line);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  color: var(--tone-muted);
  cursor: pointer;
  font-size: 13px;
  font-weight: 760;
}

.topic-chip span {
  color: var(--tone-faint);
}

.topic-chip.is-active,
.topic-chip:hover {
  border-color: rgba(49, 91, 255, 0.36);
  background: #e8efff;
  color: #174ea6;
}

.featured-inspiration,
.inspiration-card {
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm);
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--tone-panel-solid) 92%, transparent), color-mix(in srgb, var(--tone-panel-solid) 76%, transparent)),
    var(--tone-panel);
  box-shadow: 0 18px 52px rgba(20, 21, 29, 0.08);
  backdrop-filter: blur(18px);
}

.featured-inspiration {
  display: grid;
  grid-template-columns: minmax(260px, 0.58fr) minmax(0, 1fr);
  overflow: hidden;
}

.featured-inspiration__visual {
  position: relative;
  display: grid;
  min-height: 260px;
  place-items: center;
  overflow: hidden;
  background:
    linear-gradient(140deg, color-mix(in srgb, var(--card-accent, #315bff) 24%, #10131f), rgba(6, 8, 18, 0.88)),
    color-mix(in srgb, var(--card-accent, #315bff) 22%, transparent);
  color: #fff;
}

.featured-inspiration__visual img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.featured-inspiration__body {
  display: grid;
  align-content: center;
  gap: 14px;
  padding: clamp(24px, 4vw, 44px);
}

.featured-inspiration h2,
.inspiration-card h2 {
  margin: 0;
  color: var(--tone-ink);
  line-height: 1.18;
}

.featured-inspiration h2 {
  font-size: clamp(28px, 3vw, 42px);
}

.inspiration-card h2 {
  font-size: clamp(22px, 2vw, 28px);
}

.featured-inspiration p,
.inspiration-card p {
  margin: 0;
  color: var(--tone-muted);
  line-height: 1.66;
}

.inspiration-masonry {
  column-count: 3;
  column-gap: var(--theme-density-spacing, 16px);
}

.inspiration-card {
  position: relative;
  display: inline-grid;
  width: 100%;
  gap: 13px;
  margin: 0 0 var(--theme-density-spacing, 16px);
  padding: calc(var(--theme-density-spacing, 16px) * 1.25);
  break-inside: avoid;
  overflow: hidden;
  transition:
    border-color var(--transition-time, 180ms) ease,
    box-shadow var(--transition-time, 180ms) ease,
    transform var(--transition-time, 180ms) ease;
}

.inspiration-card::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at 12% 0%, color-mix(in srgb, var(--card-accent, #315bff) 12%, transparent), transparent 34%),
    linear-gradient(135deg, color-mix(in srgb, var(--card-accent, #315bff) 7%, transparent), transparent 48%);
  opacity: 0.9;
}

.inspiration-card::after {
  content: "";
  position: absolute;
  inset: 0 0 auto;
  height: 4px;
  pointer-events: none;
  background: linear-gradient(90deg, var(--card-accent, #315bff), color-mix(in srgb, var(--card-accent, #315bff) 22%, transparent), transparent 84%);
  opacity: 0.7;
}

.inspiration-card:hover {
  border-color: color-mix(in srgb, var(--card-accent, #315bff) 30%, var(--tone-line));
  box-shadow: 0 24px 60px rgba(20, 21, 29, 0.12);
  transform: translateY(-2px);
}

.featured-inspiration,
.inspiration-card {
  cursor: pointer;
}

.featured-inspiration:focus-visible,
.inspiration-card:focus-visible {
  border-color: color-mix(in srgb, var(--card-accent, #315bff) 48%, var(--tone-line));
  outline: none;
  box-shadow:
    0 0 0 4px color-mix(in srgb, var(--card-accent, #315bff) 14%, transparent),
    0 24px 60px rgba(20, 21, 29, 0.12);
}

.inspiration-card > * {
  position: relative;
  z-index: 1;
}

.inspiration-card img {
  width: 100%;
  max-height: 260px;
  border-radius: var(--app-radius-sm, 8px);
  object-fit: cover;
}

.inspiration-card pre {
  max-width: 100%;
  margin: 0;
  overflow: hidden;
  border-radius: var(--app-radius-sm, 8px);
  padding: 14px;
  border: 1px solid rgba(167, 243, 208, 0.1);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.04), transparent),
    rgba(7, 12, 18, 0.92);
  color: #b7f7d4;
  font-family: "SFMono-Regular", Consolas, monospace;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.card-flags,
.card-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.card-badges {
  justify-content: flex-end;
}

.card-kind,
.card-meta,
.card-order,
.visibility-badge,
.relation-chip,
.inspiration-card a,
.featured-inspiration a {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.card-kind {
  width: fit-content;
  color: color-mix(in srgb, var(--card-accent, #315bff) 74%, var(--tone-muted));
  font-size: 12px;
  font-weight: 820;
}

.card-meta {
  flex-wrap: wrap;
  color: var(--tone-faint);
  font-size: 12px;
}

.card-order {
  padding: 5px 8px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--card-accent, #315bff) 9%, rgba(255, 255, 255, 0.78));
  color: var(--tone-faint);
  font-size: 11px;
  font-weight: 760;
  white-space: nowrap;
}

.visibility-badge {
  padding: 5px 8px;
  border-radius: 999px;
  background: rgba(0, 124, 114, 0.1);
  color: #055f57;
  font-size: 11px;
  font-weight: 780;
  white-space: nowrap;
}

.visibility-badge--private {
  background: rgba(194, 95, 58, 0.1);
  color: #854629;
}

.inspiration-card a,
.featured-inspiration a {
  width: fit-content;
  color: color-mix(in srgb, var(--card-accent, #315bff) 76%, var(--tone-primary));
  font-size: 13px;
  font-weight: 760;
}

.relation-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.relation-chip {
  width: fit-content;
  max-width: 100%;
  padding: 7px 10px;
  border: 1px solid color-mix(in srgb, var(--card-accent, #315bff) 18%, var(--tone-line));
  border-radius: 8px;
  background: color-mix(in srgb, var(--card-accent, #315bff) 8%, rgba(255, 255, 255, 0.82));
  color: color-mix(in srgb, var(--card-accent, #315bff) 74%, var(--tone-ink));
  font-size: 12px;
  font-weight: 740;
  line-height: 1.35;
  text-decoration: none;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-row span {
  padding: 6px 10px;
  border-radius: 6px;
  background: color-mix(in srgb, var(--card-accent, #315bff) 10%, rgba(255, 255, 255, 0.84));
  color: color-mix(in srgb, var(--card-accent, #315bff) 74%, #12312f);
  font-size: 12px;
  font-weight: 740;
}

:global(body.is-dialog-open) {
  overflow: hidden;
}

.inspiration-dialog {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: grid;
  place-items: center;
  padding: clamp(16px, 3vw, 34px);
}

.dialog-backdrop {
  position: absolute;
  inset: 0;
  border: 0;
  background:
    radial-gradient(circle at 18% 12%, color-mix(in srgb, var(--card-accent, #315bff) 18%, transparent), transparent 32%),
    rgba(9, 13, 24, 0.58);
  cursor: pointer;
  backdrop-filter: blur(16px);
}

.dialog-panel {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 18px;
  width: min(1040px, 100%);
  max-height: min(780px, calc(100vh - 32px));
  overflow: hidden;
  border: 1px solid color-mix(in srgb, var(--card-accent, #315bff) 22%, rgba(255, 255, 255, 0.7));
  border-radius: var(--app-radius-sm);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(246, 248, 255, 0.92)),
    radial-gradient(circle at 8% 0%, color-mix(in srgb, var(--card-accent, #315bff) 12%, transparent), transparent 30%);
  box-shadow: 0 30px 90px rgba(7, 12, 24, 0.34);
}

.dialog-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 24px 24px 0;
}

.dialog-header h2 {
  margin: 6px 0 0;
  color: var(--tone-ink);
  font-size: clamp(28px, 3vw, 42px);
  line-height: 1.12;
}

.dialog-close {
  display: inline-grid;
  width: 38px;
  height: 38px;
  flex: 0 0 auto;
  place-items: center;
  border: 1px solid color-mix(in srgb, var(--card-accent, #315bff) 18%, var(--tone-line));
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.88);
  color: var(--tone-ink);
  cursor: pointer;
  transition:
    background 0.18s ease,
    color 0.18s ease,
    transform 0.18s ease;
}

.dialog-close:hover {
  background: color-mix(in srgb, var(--card-accent, #315bff) 12%, #fff);
  color: color-mix(in srgb, var(--card-accent, #315bff) 74%, #172033);
  transform: translateY(-1px);
}

.dialog-content {
  display: grid;
  grid-template-columns: minmax(280px, 0.88fr) minmax(0, 1fr);
  min-height: 0;
  overflow: auto;
  padding: 0 24px 24px;
  gap: 20px;
}

.dialog-media {
  display: grid;
  min-height: 420px;
  overflow: hidden;
  place-items: center;
  border-radius: var(--app-radius-sm);
  background:
    linear-gradient(140deg, color-mix(in srgb, var(--card-accent, #315bff) 30%, #10131f), rgba(6, 8, 18, 0.92)),
    color-mix(in srgb, var(--card-accent, #315bff) 22%, transparent);
  color: #fff;
}

.dialog-media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.dialog-media--empty {
  min-height: 320px;
}

.dialog-body {
  display: grid;
  align-content: start;
  gap: 16px;
  min-width: 0;
}

.dialog-text {
  margin: 0;
  color: var(--tone-strong);
  font-size: 16px;
  line-height: 1.78;
  white-space: pre-wrap;
}

.dialog-code {
  max-height: 320px;
  margin: 0;
  overflow: auto;
  border: 1px solid rgba(167, 243, 208, 0.12);
  border-radius: var(--app-radius-sm);
  padding: 16px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.04), transparent),
    #07101a;
  color: #c6f7df;
  font-family: "SFMono-Regular", Consolas, monospace;
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.dialog-section {
  display: grid;
  gap: 10px;
  padding-top: 4px;
}

.dialog-section h3 {
  margin: 0;
  color: var(--tone-ink);
  font-size: 15px;
}

.dialog-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding-top: 6px;
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
  .wall-hero,
  .featured-inspiration {
    grid-template-columns: 1fr;
  }

  .inspiration-masonry {
    column-count: 2;
  }

  .dialog-content {
    grid-template-columns: 1fr;
  }

  .dialog-media {
    min-height: 300px;
  }
}

@media (max-width: 760px) {
  .wall-page {
    padding-top: 26px;
  }

  .wall-hero {
    padding: 22px;
  }

  .wall-search {
    grid-template-columns: auto minmax(0, 1fr);
    border-radius: 8px;
  }

  .wall-search button {
    grid-column: 1 / -1;
    width: 100%;
  }

  .inspiration-masonry {
    column-count: 1;
  }

  .card-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .card-badges {
    justify-content: flex-start;
  }

  .inspiration-dialog {
    align-items: end;
    padding: 10px;
  }

  .dialog-panel {
    max-height: calc(100vh - 20px);
    border-radius: 8px;
  }

  .dialog-header {
    padding: 18px 18px 0;
  }

  .dialog-content {
    padding: 0 18px 18px;
    gap: 16px;
  }

  .dialog-media {
    min-height: 220px;
  }

  .dialog-actions > * {
    width: 100%;
    justify-content: center;
  }
}
</style>
