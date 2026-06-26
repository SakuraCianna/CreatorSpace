<template>
<!-- 留言板互动墙页面 -->
  <section ref="root" class="guestbook-page">
    <header class="archive-hero page-hero page-hero--guestbook" data-reveal>
      <div>
        <h1>留言板</h1>
      </div>
      <p>留下你的想法、建议或问候，审核通过后会公开展示。</p>
    </header>

    <div class="guestbook-layout">
      <div class="guestbook-form-card" data-reveal>
        <p class="page-kicker">Leave a Message</p>
        <form class="comment-form" @submit.prevent="postMessage">
          <textarea
            v-model="draft"
            rows="4"
            maxlength="2000"
            placeholder="写下你想说的话..."
          />
          <button class="button button-filled button-compact" :disabled="!canPost" type="submit">
            {{ canPost ? '提交留言' : '登录后留言' }}
          </button>
        </form>
        <p v-if="formNotice" class="inline-notice">{{ formNotice }}</p>
      </div>

      <div class="guestbook-list" data-reveal>
        <div v-if="isLoading" class="empty-state">
          <LoaderCircle class="spin" :size="24" />
          <h2>正在加载留言</h2>
        </div>
        <div v-else-if="entries.length === 0" class="empty-state">
          <h2>还没有留言</h2>
          <p>成为第一个留言的人吧。</p>
        </div>
        <article v-for="entry in entries" :key="entry.id" class="guestbook-item">
          <div class="guestbook-item-header">
            <strong>{{ entry.displayName }}</strong>
            <span>{{ formatDate(entry.createdAt) }}</span>
          </div>
          <p class="guestbook-item-content">{{ entry.content }}</p>
          <div class="guestbook-item-footer">
            <span v-if="entry.likeCount > 0">{{ entry.likeCount }} 赞</span>
          </div>
        </article>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
// 导入 Composition API 与 Lucide 图标资源
import { computed, onMounted, ref } from 'vue'
import { LoaderCircle } from '@lucide/vue'

import { fetchGuestbook, submitGuestbook } from '@/services/content'
import { usePageReveal } from '@/shared/composables/usePageReveal'
import { useSessionStore } from '@/shared/sessionStore'

interface GuestbookEntry {
  id: number
  userId?: number | null
  displayName: string
  content: string
  status: string
  likeCount: number
  createdAt?: string | null
}

// 声明留言板所需的表单数据和响应式列表
const root = ref<HTMLElement | null>(null)
const entries = ref<GuestbookEntry[]>([])
const draft = ref('')
const formNotice = ref('')
const isLoading = ref(true)
const session = useSessionStore()

usePageReveal(root)

const canPost = computed(() => Boolean(session.accessToken))

// 异步拉取留言板审核通过的已公开留言列表数据
async function loadEntries() {
  isLoading.value = true
  try {
    const page = await fetchGuestbook({ pageSize: 50 })
    entries.value = page.records
  } catch {
    entries.value = []
  } finally {
    isLoading.value = false
  }
}

// 提交用户留言, 限制留言字数并在成功后刷新留言流列表
async function postMessage() {
  if (!canPost.value) {
    formNotice.value = '请先登录账号再留言'
    return
  }
  const content = draft.value.trim()
  if (content.length < 2) {
    formNotice.value = '留言内容至少 2 个字符'
    return
  }
  try {
    await submitGuestbook(content)
    draft.value = ''
    formNotice.value = '留言已提交，审核通过后会公开展示'
    await loadEntries()
  } catch (error) {
    formNotice.value = error instanceof Error ? error.message : '留言提交失败'
  }
}

function formatDate(value?: string | null): string {
  if (!value) return '刚刚'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

onMounted(loadEntries)
</script>

<style scoped>
.guestbook-page {
  display: grid;
  gap: 24px;
  padding: 46px 0 84px;
}

.page-hero--guestbook {
  --hero-accent: #7c3aed;
  --hero-accent-2: #f97316;
  --hero-mark: "GB";
}

.guestbook-layout {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 24px;
  align-items: start;
}

.guestbook-form-card {
  position: sticky;
  top: 100px;
  display: grid;
  gap: 12px;
  padding: 20px;
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm);
  background: var(--tone-panel);
  box-shadow: var(--tone-shadow);
  backdrop-filter: blur(18px);
}

.comment-form {
  display: grid;
  gap: 10px;
}

.comment-form textarea {
  width: 100%;
  border: 1px solid rgba(17, 24, 39, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.86);
  color: var(--tone-strong);
  font: inherit;
  padding: 12px;
  resize: vertical;
}

.guestbook-list {
  display: grid;
  gap: 14px;
}

.guestbook-item {
  padding: 18px;
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm);
  background: var(--tone-panel);
  box-shadow: var(--tone-shadow);
  backdrop-filter: blur(18px);
}

.guestbook-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.guestbook-item-header strong {
  color: var(--tone-ink);
  font-size: 15px;
}

.guestbook-item-header span {
  color: var(--tone-faint);
  font-size: 12px;
}

.guestbook-item-content {
  margin: 0;
  color: #475569;
  font-size: 15px;
  line-height: 1.72;
}

.guestbook-item-footer {
  margin-top: 8px;
  color: var(--tone-faint);
  font-size: 12px;
}

.inline-notice {
  margin: 8px 0 0;
  padding: 10px 12px;
  border-left: 3px solid var(--tone-coral);
  background: rgba(194, 95, 58, 0.08);
  color: #754226;
  font-size: 13px;
  line-height: 1.55;
}

.empty-state {
  display: grid;
  place-items: center;
  gap: 10px;
  min-height: 200px;
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm);
  background: var(--tone-panel);
}

.empty-state h2 {
  margin: 0;
  color: var(--tone-ink);
  font-size: 20px;
}

.empty-state p {
  color: var(--tone-muted);
  font-size: 14px;
}

@media (max-width: 1020px) {
  .guestbook-layout {
    grid-template-columns: 1fr;
  }

  .guestbook-form-card {
    position: static;
  }
}
</style>
