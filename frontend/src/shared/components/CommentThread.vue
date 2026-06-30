<template>
  <div class="comment-thread">
    <article class="comment-item">
      <span class="comment-avatar" aria-hidden="true">{{ initial }}</span>
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
            :class="{ 'is-active': liked }"
            type="button"
            :disabled="!canComment"
            @click="emit('like', comment)"
          >
            <Heart :size="14" />
            {{ comment.likeCount || '' }}
          </button>
          <button
            class="comment-action-btn"
            type="button"
            :disabled="!canComment"
            @click="emit('reply', comment)"
          >
            <MessageCircle :size="14" />
            回复
          </button>
        </div>
      </div>
    </article>

    <template v-if="comment.replies.length > 0">
      <button
        v-if="!expanded"
        class="show-replies-btn"
        type="button"
        @click="expanded = true"
      >
        <ChevronDown :size="14" />
        查看 {{ comment.replies.length }} 条回复
      </button>
      <div v-else class="replies-nest">
        <CommentThread
          v-for="reply in comment.replies"
          :key="reply.id"
          :comment="reply"
          :can-comment="canComment"
          :liked-map="likedMap"
          @reply="emit('reply', $event)"
          @like="emit('like', $event)"
        />
        <button class="hide-replies-btn" type="button" @click="expanded = false">
          <ChevronUp :size="14" />
          收起回复
        </button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { Reply, Heart, MessageCircle, ChevronDown, ChevronUp } from '@lucide/vue'
import { formatDateToDay } from '@/shared/datetime'
import type { CommentTree } from '@/shared/domain'
import type { CommentSummary } from '@/shared/domain'

defineOptions({ name: 'CommentThread' })

const props = defineProps<{
  comment: CommentTree
  canComment: boolean
  likedMap: Record<number, boolean>
}>()

const emit = defineEmits<{
  reply: [comment: CommentSummary]
  like: [comment: CommentSummary]
}>()

const expanded = ref(false)

const initial = computed(() => props.comment.username.trim().slice(0, 1).toUpperCase() || 'U')

const liked = computed(() => props.likedMap[props.comment.id] ?? false)

function formatDate(value?: string | null): string {
  return formatDateToDay(value)
}
</script>

<style scoped>
.comment-thread {
  display: grid;
  gap: 6px;
}

.comment-item {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 10px;
  padding: 12px;
  border: 1px solid color-mix(in srgb, var(--tone-line) 76%, transparent);
  border-radius: var(--app-radius-sm, 8px);
  background: color-mix(in srgb, var(--tone-panel-solid) 72%, transparent);
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

.comment-reply-to {
  display: flex;
  align-items: center;
  gap: 4px;
  margin: 2px 0 4px;
  font-size: 12px;
  color: var(--tone-ink-2);
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

.comment-action-btn.is-active {
  color: #e0455a;
}
.comment-action-btn.is-active svg {
  stroke: #e0455a;
  fill: #e0455a;
}

.replies-nest {
  display: grid;
  gap: 6px;
  margin-left: 14px;
  padding-left: 10px;
  border-left: 2px solid color-mix(in srgb, var(--tone-line) 60%, transparent);
}

.show-replies-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-left: 14px;
  padding: 6px 8px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--tone-primary);
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
}

.show-replies-btn:hover {
  background: color-mix(in srgb, var(--tone-primary) 8%, transparent);
}

.hide-replies-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--tone-muted);
  cursor: pointer;
  font-size: 12px;
}

.hide-replies-btn:hover {
  color: var(--tone-primary);
}
</style>
