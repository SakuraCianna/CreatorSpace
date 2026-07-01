<template>
  <section ref="root" class="my-comments-page">
    <PublicPageHeader title="我的评论" description="查看和管理我在所有文章下的评论与互动记录。" kicker="MY COMMENTS" theme="blue" />

    <div v-if="isLoading" class="empty-state">
      <LoaderCircle class="spin" :size="24" />
      <h2>正在加载</h2>
    </div>

    <div v-else-if="comments.length === 0" class="empty-state">
      <h2>暂无评论</h2>
      <p>你还没有发表过评论。</p>
    </div>

    <div v-else class="my-comments-list" data-reveal>
      <article v-for="item in comments" :key="item.id" class="my-comment-item">
        <header class="my-comment-item-header">
          <span class="my-comment-target">
            <component :is="item.targetType === 'ARTICLE' ? BookOpen : Images" :size="14" />
            {{ item.targetType === 'ARTICLE' ? '文章' : '作品' }}
            <span class="my-comment-target-id">#{{ item.targetId }}</span>
          </span>
          <span :class="['status-badge', `status-${item.status.toLowerCase()}`]">
            {{ statusLabel(item.status) }}
          </span>
        </header>
        <p class="my-comment-content">{{ item.content }}</p>
        <footer class="my-comment-item-footer">
          <span>{{ formatDate(item.createdAt) }}</span>
          <button
            v-if="item.status === 'PENDING'"
            class="button button-tonal button-compact"
            type="button"
            @click="editComment(item)"
          >
            编辑
          </button>
        </footer>
      </article>
    </div>

    <div v-if="totalPages > 1" class="pagination-row" data-reveal>
      <button
        class="button button-tonal button-compact"
        :disabled="currentPage <= 1"
        @click="goPage(currentPage - 1)"
      >
        上一页
      </button>
      <span>{{ currentPage }} / {{ totalPages }}</span>
      <button
        class="button button-tonal button-compact"
        :disabled="currentPage >= totalPages"
        @click="goPage(currentPage + 1)"
      >
        下一页
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import PublicPageHeader from '@/components/common/PublicPageHeader.vue'
import { BookOpen, Images, LoaderCircle } from '@lucide/vue'

import { fetchMyComments } from '@/services/content'
import { usePageReveal } from '@/shared/composables/usePageReveal'
import { formatDateTimeToSecond } from '@/shared/datetime'
import type { CommentSummary } from '@/shared/domain'

const root = ref<HTMLElement | null>(null)
usePageReveal(root)

const comments = ref<CommentSummary[]>([])
const isLoading = ref(true)
const currentPage = ref(1)
const totalPages = ref(1)
const pageSize = 20

function statusLabel(status: string): string {
  switch (status) {
    case 'APPROVED': return '已发布'
    case 'PENDING': return '等待审核'
    case 'REJECTED': return '被拒绝'
    default: return status
  }
}

function formatDate(raw: string | null | undefined): string {
  return raw ? formatDateTimeToSecond(raw) : ''
}

async function loadMyComments(page: number) {
  isLoading.value = true
  try {
    const result = await fetchMyComments({ page, pageSize })
    comments.value = result.records
    currentPage.value = result.page
    totalPages.value = Math.max(1, Math.ceil(result.total / result.size))
  } catch {
    comments.value = []
  } finally {
    isLoading.value = false
  }
}

function goPage(page: number) {
  if (page < 1 || (totalPages.value > 0 && page > totalPages.value)) return
  loadMyComments(page)
}

function editComment(item: CommentSummary) {
  // TODO: navigate to the target article/project page
}

onMounted(() => {
  loadMyComments(1)
})
</script>

<style scoped>
.my-comments-page {
  width: min(800px, calc(100% - 40px));
  margin: 0 auto;
  padding: 40px 0 80px;
}

.my-comments-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.my-comment-item {
  padding: 16px 20px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid var(--tone-line);
}

.my-comment-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  gap: 12px;
}

.my-comment-target {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--tone-ink-2);
}

.my-comment-target-id {
  color: var(--tone-ink-3);
}

.status-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 650;
}

.status-approved {
  background: #e6f7e6;
  color: #1a7d1a;
}

.status-pending {
  background: #fff3cd;
  color: #856404;
}

.status-rejected {
  background: #fde8e8;
  color: #b91c1c;
}

.my-comment-content {
  margin: 0;
  line-height: 1.6;
  color: var(--tone-ink);
}

.my-comment-item-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
  font-size: 13px;
  color: var(--tone-ink-2);
}

.pagination-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 40px;
}
</style>
