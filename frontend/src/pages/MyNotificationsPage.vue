<template>
  <section ref="root" class="my-notifications-page">
    <header class="archive-hero page-hero" data-reveal>
      <div>
        <p class="page-kicker">Notifications</p>
        <h1>我的通知</h1>
      </div>
    </header>

    <div class="notifications-toolbar">
      <button class="button button-tonal button-compact" :disabled="unreadCount === 0" type="button" @click="handleMarkAllRead">
        <CheckCheck :size="15" />全部已读
      </button>
    </div>

    <div v-if="isLoading" class="empty-state">
      <LoaderCircle class="spin" :size="24" />
      <h2>正在加载</h2>
    </div>

    <div v-else-if="notifications.length === 0" class="empty-state">
      <Bell :size="40" />
      <h2>暂无通知</h2>
      <p>当有人评论、点赞或收藏你的内容时，你会在这里收到通知。</p>
    </div>

    <ul v-else class="notification-list">
      <li
        v-for="item in notifications"
        :key="item.id"
        class="notification-item"
        :class="{ 'notification-unread': !item.isRead }"
      >
        <div class="notification-indicator" :class="{ 'is-unread': !item.isRead }" />
        <div class="notification-body">
          <p class="notification-text">
            <strong v-if="item.actorName">{{ item.actorName }}</strong>
            {{ notificationAction(item.type) }}
          </p>
          <p v-if="item.title" class="notification-ref-title">{{ item.title }}</p>
          <p class="notification-meta">
            <time>{{ formatDate(item.createdAt) }}</time>
          </p>
        </div>
        <div class="notification-actions">
          <button v-if="!item.isRead" class="button button-ghost button-icon" title="标记已读" type="button" @click="handleMarkRead(item.id)">
            <Check :size="15" />
          </button>
          <button class="button button-ghost button-icon button-danger" title="删除" type="button" @click="handleDelete(item.id)">
            <Trash2 :size="15" />
          </button>
        </div>
      </li>
    </ul>

    <div v-if="totalPages > 1" class="pagination-row" data-reveal>
      <button
        class="button button-tonal button-compact"
        :disabled="currentPage <= 1"
        type="button"
        @click="goPage(currentPage - 1)"
      >
        <ArrowLeft :size="15" />上一页
      </button>
      <span class="pagination-info">{{ currentPage }} / {{ totalPages }}</span>
      <button
        class="button button-tonal button-compact"
        :disabled="currentPage >= totalPages"
        type="button"
        @click="goPage(currentPage + 1)"
      >
        下一页<ArrowRight :size="15" />
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ArrowLeft, ArrowRight, Bell, Check, CheckCheck, LoaderCircle, Trash2 } from '@lucide/vue'

import { deleteNotification, fetchNotifications, markAllNotificationsRead, markNotificationRead } from '../services/content'
import { usePageReveal } from '../shared/composables/usePageReveal'
import { formatDateToDay } from '../shared/datetime'
import type { NotificationRecord } from '../shared/domain'

const root = ref<HTMLElement | null>(null)
const notifications = ref<NotificationRecord[]>([])
const unreadCount = ref(0)
const isLoading = ref(true)
const currentPage = ref(1)
const totalPages = ref(1)

usePageReveal(root)

onMounted(() => { loadNotifications() })

async function loadNotifications() {
  isLoading.value = true
  try {
    const page = await fetchNotifications({ page: currentPage.value })
    notifications.value = page.records
    currentPage.value = page.current
    totalPages.value = page.totalPages
    unreadCount.value = page.records.filter(n => !n.isRead).length
  } catch {
    notifications.value = []
  } finally {
    isLoading.value = false
  }
}

async function goPage(page: number) {
  currentPage.value = page
  await loadNotifications()
}

async function handleMarkRead(id: number) {
  await markNotificationRead(id)
  const item = notifications.value.find(n => n.id === id)
  if (item) {
    item.isRead = true
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  }
}

async function handleMarkAllRead() {
  await markAllNotificationsRead()
  notifications.value.forEach(n => { n.isRead = true })
  unreadCount.value = 0
}

async function handleDelete(id: number) {
  await deleteNotification(id)
  notifications.value = notifications.value.filter(n => n.id !== id)
}

function notificationAction(type: string): string {
  const actions: Record<string, string> = {
    COMMENT_ON_ARTICLE: ' 评论了你的文章',
    REPLY_COMMENT: ' 回复了你的评论',
    LIKE_ARTICLE: ' 赞了你的文章',
    FAVORITE_ARTICLE: ' 收藏了你的文章',
    LIKE_COMMENT: ' 赞了你的评论',
    PENDING_REVIEW: '',
  }
  return actions[type] || ' 与你互动'
}

function formatDate(value?: string | null): string {
  if (!value) return ''
  return formatDateToDay(value)
}
</script>

<style scoped>
.my-notifications-page {
  max-width: 820px;
  margin: 0 auto;
  padding: 40px 24px 80px;
}

.notifications-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.notification-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 16px;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 20px;
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid rgba(0, 0, 0, 0.04);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.03);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.notification-item:hover {
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.05);
  transform: translateY(-2px);
  border-color: rgba(0, 0, 0, 0.08);
}

.notification-item.notification-unread {
  background: color-mix(in srgb, #315bff 3%, #ffffff);
  border-left: 4px solid #315bff;
}

.notification-indicator {
  flex-shrink: 0;
  width: 10px;
  height: 10px;
  margin-top: 5px;
  border-radius: 50%;
  background: transparent;
}

.notification-indicator.is-unread {
  background: #315bff;
  box-shadow: 0 0 0 4px color-mix(in srgb, #315bff 15%, transparent);
}

.notification-body {
  flex: 1;
  min-width: 0;
}

.notification-text {
  margin: 0 0 6px;
  font-size: 15px;
  line-height: 1.5;
  color: #0f172a;
}

.notification-text strong {
  font-weight: 700;
  color: #1e293b;
}

.notification-ref-title {
  margin: 0 0 8px;
  font-size: 14px;
  color: #315bff;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-meta {
  margin: 0;
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
}

.notification-actions {
  display: flex;
  flex-shrink: 0;
  gap: 4px;
}

.pagination-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 32px;
}

.pagination-info {
  font-size: 13px;
  color: var(--tone-muted);
}
</style>
