<template>
  <section ref="root" class="messages-page">
    <header class="messages-header" data-reveal>
      <div>
        <p class="page-kicker">Messages</p>
        <h1>我的私信</h1>
      </div>
      <button class="button button-tonal button-compact" type="button" :disabled="loadingConversations" @click="loadConversations">
        <RefreshCw :size="15" :class="{ spin: loadingConversations }" />
        刷新
      </button>
    </header>

    <p v-if="notice" class="inline-notice">{{ notice }}</p>

    <div class="messages-shell" data-reveal>
      <aside class="conversation-panel">
        <div v-if="loadingConversations" class="panel-state">
          <LoaderCircle class="spin" :size="22" />
          <span>正在加载会话</span>
        </div>
        <div v-else-if="conversations.length === 0" class="panel-state">
          <MessageCircle :size="30" />
          <strong>暂无私信</strong>
          <span>从用户主页发起第一条私信。</span>
        </div>
        <template v-else>
          <button
            v-for="item in conversations"
            :key="item.id"
            class="conversation-item"
            :class="{ 'is-active': item.id === activeConversationId }"
            type="button"
            @click="selectConversation(item.id)"
          >
            <span class="conversation-avatar">
              <img v-if="item.peerAvatarUrl" :src="item.peerAvatarUrl" alt="" />
              <UserRound v-else :size="18" />
            </span>
            <span class="conversation-copy">
              <span class="conversation-title">
                <strong>{{ peerName(item) }}</strong>
                <small>{{ formatDate(item.lastMessageAt) }}</small>
              </span>
              <span class="conversation-preview">{{ previewText(item) }}</span>
            </span>
            <span v-if="item.unreadCount > 0" class="unread-pill">{{ item.unreadCount > 99 ? '99+' : item.unreadCount }}</span>
          </button>
        </template>
      </aside>

      <main class="thread-panel">
        <template v-if="activeConversation || draftReceiver">
          <header class="thread-header">
            <div class="thread-peer">
              <span class="conversation-avatar conversation-avatar--large">
                <img v-if="activeConversation?.peerAvatarUrl || draftReceiver?.avatarUrl" :src="activeConversation?.peerAvatarUrl || draftReceiver?.avatarUrl || ''" alt="" />
                <UserRound v-else :size="20" />
              </span>
              <div>
                <strong>{{ activeConversation ? peerName(activeConversation) : draftReceiverName }}</strong>
                <span>{{ activeConversation ? `@${activeConversation.peerUsername}` : '新会话' }}</span>
              </div>
            </div>
            <RouterLink
              v-if="activeConversation"
              class="button button-ghost button-icon"
              :to="{ name: 'user-profile', params: { userId: activeConversation.peerId } }"
              title="查看主页"
            >
              <UserRound :size="16" />
            </RouterLink>
          </header>

          <div ref="threadBody" class="thread-body">
            <div v-if="loadingMessages" class="panel-state">
              <LoaderCircle class="spin" :size="22" />
              <span>正在加载消息</span>
            </div>
            <div v-else-if="messages.length === 0" class="empty-thread">
              <MessageCircle :size="36" />
              <h2>还没有消息</h2>
              <p>写下第一句就会创建会话。</p>
            </div>
            <article
              v-for="message in messages"
              v-else
              :key="message.id"
              class="message-bubble"
              :class="{ 'is-mine': message.senderId === currentUserId }"
            >
              <p>{{ message.content }}</p>
              <time>{{ formatDate(message.createdAt) }}</time>
            </article>
          </div>

          <form class="composer" @submit.prevent="handleSend">
            <textarea
              v-model="draft"
              maxlength="2000"
              rows="3"
              placeholder="输入私信内容"
              @keydown.ctrl.enter.prevent="handleSend"
              @keydown.meta.enter.prevent="handleSend"
            />
            <div class="composer-actions">
              <span>{{ draft.trim().length }}/2000</span>
              <button class="button button-filled button-compact" type="submit" :disabled="sending || !draft.trim()">
                <Send :size="15" />
                {{ sending ? '发送中' : '发送' }}
              </button>
            </div>
          </form>
        </template>

        <div v-else class="thread-placeholder">
          <MessageCircle :size="42" />
          <h2>选择一个会话</h2>
          <p>私信内容只对会话双方可见。</p>
        </div>
      </main>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { LoaderCircle, MessageCircle, RefreshCw, Send, UserRound } from '@lucide/vue'

import {
  fetchMessageConversation,
  fetchMessageConversations,
  fetchUserProfile,
  sendPrivateMessage,
} from '../services/content'
import { toUserMessage } from '../services/http'
import { usePageReveal } from '../shared/composables/usePageReveal'
import { formatDateToDay } from '../shared/datetime'
import { useSessionStore } from '../shared/sessionStore'
import type { PrivateConversationSummary, PrivateMessageRecord, UserProfile } from '../shared/domain'

const root = ref<HTMLElement | null>(null)
const threadBody = ref<HTMLElement | null>(null)
const route = useRoute()
const router = useRouter()
const session = useSessionStore()

const conversations = ref<PrivateConversationSummary[]>([])
const messages = ref<PrivateMessageRecord[]>([])
const activeConversationId = ref<number | null>(null)
const draftReceiver = ref<UserProfile | null>(null)
const draft = ref('')
const notice = ref('')
const loadingConversations = ref(false)
const loadingMessages = ref(false)
const sending = ref(false)

const currentUserId = computed(() => session.currentUser?.id ?? 0)
const activeConversation = computed(() => conversations.value.find((item) => item.id === activeConversationId.value) ?? null)
const draftReceiverName = computed(() => draftReceiver.value?.nickname || draftReceiver.value?.username || '新会话')

usePageReveal(root)

onMounted(async () => {
  await loadConversations()
  await applyRouteTarget()
})

watch(
  () => route.query.to,
  async () => {
    await applyRouteTarget()
  },
)

async function loadConversations() {
  loadingConversations.value = true
  notice.value = ''
  try {
    const page = await fetchMessageConversations({ pageSize: 50 })
    conversations.value = page.records
    if (!activeConversationId.value && conversations.value.length > 0 && !route.query.to) {
      await selectConversation(conversations.value[0].id)
    }
  } catch (error) {
    notice.value = toUserMessage(error, '私信会话加载失败')
    conversations.value = []
  } finally {
    loadingConversations.value = false
  }
}

async function applyRouteTarget() {
  const raw = route.query.to
  const targetId = typeof raw === 'string' ? Number(raw) : 0
  if (!Number.isFinite(targetId) || targetId <= 0) return
  if (targetId === currentUserId.value) {
    notice.value = '不能给自己发送私信'
    return
  }
  const existing = conversations.value.find((item) => item.peerId === targetId)
  if (existing) {
    draftReceiver.value = null
    await selectConversation(existing.id)
    return
  }
  activeConversationId.value = null
  messages.value = []
  try {
    draftReceiver.value = await fetchUserProfile(targetId)
  } catch (error) {
    notice.value = toUserMessage(error, '目标用户不存在或暂不可用')
    draftReceiver.value = null
  }
}

async function selectConversation(id: number) {
  activeConversationId.value = id
  draftReceiver.value = null
  loadingMessages.value = true
  notice.value = ''
  try {
    const detail = await fetchMessageConversation(id, { pageSize: 100 })
    upsertConversation({ ...detail.conversation, unreadCount: 0 })
    messages.value = detail.messages.records
    await router.replace({ name: 'my-messages', query: {} })
    await scrollToBottom()
  } catch (error) {
    notice.value = toUserMessage(error, '消息加载失败')
    messages.value = []
  } finally {
    loadingMessages.value = false
  }
}

async function handleSend() {
  const content = draft.value.trim()
  const receiverId = activeConversation.value?.peerId ?? draftReceiver.value?.id
  if (!receiverId || !content || sending.value) return
  sending.value = true
  notice.value = ''
  try {
    const result = await sendPrivateMessage(receiverId, content)
    draft.value = ''
    upsertConversation(result.conversation)
    activeConversationId.value = result.conversation.id
    draftReceiver.value = null
    const created = result.messages.records[0]
    if (created && !messages.value.some((item) => item.id === created.id)) {
      messages.value.push(created)
    }
    await router.replace({ name: 'my-messages', query: {} })
    await scrollToBottom()
  } catch (error) {
    notice.value = toUserMessage(error, '私信发送失败')
  } finally {
    sending.value = false
  }
}

function upsertConversation(conversation: PrivateConversationSummary) {
  const index = conversations.value.findIndex((item) => item.id === conversation.id)
  if (index >= 0) {
    conversations.value.splice(index, 1, conversation)
  } else {
    conversations.value.unshift(conversation)
  }
  conversations.value.sort((a, b) => new Date(b.lastMessageAt ?? 0).getTime() - new Date(a.lastMessageAt ?? 0).getTime())
}

async function scrollToBottom() {
  await nextTick()
  if (threadBody.value) {
    threadBody.value.scrollTop = threadBody.value.scrollHeight
  }
}

function peerName(item: PrivateConversationSummary): string {
  return item.peerNickname || item.peerUsername
}

function previewText(item: PrivateConversationSummary): string {
  if (!item.lastMessageContent) return '暂无消息'
  return item.lastSenderId === currentUserId.value ? `我：${item.lastMessageContent}` : item.lastMessageContent
}

function formatDate(value?: string | null): string {
  if (!value) return ''
  return formatDateToDay(value)
}
</script>

<style scoped>
.messages-page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 40px 24px 80px;
}

.messages-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
  margin-bottom: 20px;
}

.messages-header h1 {
  margin: 6px 0 0;
  font-size: clamp(28px, 4vw, 44px);
}

.inline-notice {
  margin: 0 0 16px;
  padding: 12px 14px;
  border-radius: 8px;
  color: #8a3d00;
  background: #fff7ed;
  border: 1px solid #fed7aa;
}

.messages-shell {
  display: grid;
  grid-template-columns: minmax(260px, 340px) minmax(0, 1fr);
  min-height: 620px;
  border: 1px solid rgba(15, 23, 42, 0.1);
  border-radius: 8px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.9);
}

.conversation-panel {
  display: flex;
  flex-direction: column;
  min-width: 0;
  border-right: 1px solid rgba(15, 23, 42, 0.08);
  background: #f8fafc;
}

.panel-state,
.thread-placeholder,
.empty-thread {
  display: grid;
  place-items: center;
  align-content: center;
  gap: 10px;
  min-height: 220px;
  padding: 24px;
  text-align: center;
  color: #64748b;
}

.panel-state strong,
.thread-placeholder h2,
.empty-thread h2 {
  margin: 0;
  color: #0f172a;
}

.panel-state span,
.thread-placeholder p,
.empty-thread p {
  margin: 0;
  font-size: 14px;
}

.conversation-item {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  width: 100%;
  min-height: 74px;
  padding: 12px 14px;
  border: 0;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.conversation-item:hover,
.conversation-item.is-active {
  background: #ffffff;
}

.conversation-item.is-active {
  box-shadow: inset 3px 0 0 #315bff;
}

.conversation-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  color: #334155;
  background: #e2e8f0;
  overflow: hidden;
}

.conversation-avatar--large {
  width: 46px;
  height: 46px;
}

.conversation-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.conversation-copy {
  min-width: 0;
}

.conversation-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.conversation-title strong,
.conversation-preview {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conversation-title strong {
  font-size: 14px;
  color: #0f172a;
}

.conversation-title small,
.conversation-preview {
  font-size: 12px;
  color: #64748b;
}

.conversation-preview {
  display: block;
  margin-top: 4px;
}

.unread-pill {
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  background: #ef4444;
  font-size: 12px;
  font-weight: 700;
}

.thread-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  min-width: 0;
  background: #ffffff;
}

.thread-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 72px;
  padding: 14px 18px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
}

.thread-peer {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.thread-peer div {
  display: grid;
  gap: 3px;
}

.thread-peer strong {
  color: #0f172a;
}

.thread-peer span {
  font-size: 13px;
  color: #64748b;
}

.thread-body {
  min-height: 0;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}

.message-bubble {
  max-width: min(68%, 620px);
  align-self: flex-start;
  padding: 11px 13px;
  border-radius: 8px;
  background: #eef2ff;
  color: #0f172a;
}

.message-bubble.is-mine {
  align-self: flex-end;
  color: #ffffff;
  background: #315bff;
}

.message-bubble p {
  margin: 0;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  line-height: 1.55;
}

.message-bubble time {
  display: block;
  margin-top: 6px;
  font-size: 11px;
  color: color-mix(in srgb, currentColor 70%, transparent);
}

.composer {
  padding: 14px;
  border-top: 1px solid rgba(15, 23, 42, 0.08);
  background: #ffffff;
}

.composer textarea {
  width: 100%;
  resize: vertical;
  min-height: 82px;
  max-height: 180px;
  padding: 12px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font: inherit;
  line-height: 1.5;
  color: #0f172a;
  background: #ffffff;
}

.composer textarea:focus {
  outline: 2px solid color-mix(in srgb, #315bff 25%, transparent);
  border-color: #315bff;
}

.composer-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
}

.composer-actions span {
  font-size: 12px;
  color: #64748b;
}

.spin {
  animation: spin 0.9s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 760px) {
  .messages-page {
    padding: 28px 16px 64px;
  }

  .messages-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .messages-shell {
    grid-template-columns: 1fr;
  }

  .conversation-panel {
    max-height: 320px;
    border-right: 0;
    border-bottom: 1px solid rgba(15, 23, 42, 0.08);
    overflow-y: auto;
  }

  .thread-panel {
    min-height: 560px;
  }

  .message-bubble {
    max-width: 88%;
  }
}
</style>
