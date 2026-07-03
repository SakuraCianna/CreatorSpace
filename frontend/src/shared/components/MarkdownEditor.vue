<template>
  <div class="markdown-editor">
    <div class="markdown-toolbar" aria-label="Markdown 快捷操作">
      <button v-for="action in actions" :key="action.label" type="button" :title="action.title" @click="applyAction(action)">
        {{ action.label }}
      </button>
    </div>
    <div class="markdown-workspace">
      <textarea
        ref="textareaRef"
        :value="modelValue"
        :rows="rows"
        @input="updateValue"
      />
      <article class="markdown-preview" v-html="previewHtml" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { renderSafeMarkdown } from '../markdown'

interface MarkdownAction {
  label: string
  title: string
  before: string
  after?: string
  placeholder: string
  block?: boolean
}

const props = withDefaults(defineProps<{
  modelValue: string
  rows?: number
}>(), {
  rows: 14,
})

const emit = defineEmits<{
  (event: 'update:modelValue', value: string): void
}>()

const textareaRef = ref<HTMLTextAreaElement | null>(null)
const actions: MarkdownAction[] = [
  { label: 'H1', title: '主标题', before: '# ', placeholder: '主标题', block: true },
  { label: 'H2', title: '二级标题', before: '## ', placeholder: '二级标题', block: true },
  { label: 'B', title: '加粗', before: '**', after: '**', placeholder: '加粗文字' },
  { label: 'U', title: '下划线', before: '++', after: '++', placeholder: '下划线文字' },
  { label: 'S', title: '中划线', before: '~~', after: '~~', placeholder: '中划线文字' },
  { label: '“”', title: '引用', before: '> ', placeholder: '引用内容', block: true },
  { label: '•', title: '无序列表', before: '- ', placeholder: '列表项', block: true },
  { label: '</>', title: '代码块', before: '```ts\n', after: '\n```', placeholder: 'const value = true' },
  { label: 'Link', title: '链接', before: '[', after: '](https://example.com)', placeholder: '链接文字' },
]

const previewHtml = computed(() => renderSafeMarkdown(props.modelValue))

function updateValue(event: Event) {
  emit('update:modelValue', (event.target as HTMLTextAreaElement).value)
}

function applyAction(action: MarkdownAction) {
  const textarea = textareaRef.value
  if (!textarea) {
    return
  }
  const value = props.modelValue
  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const selected = value.slice(start, end) || action.placeholder
  const beforeSelection = value.slice(0, start)
  const afterSelection = value.slice(end)
  const linePrefix = action.block && beforeSelection && !beforeSelection.endsWith('\n') ? '\n' : ''
  const lineSuffix = action.block && afterSelection && !afterSelection.startsWith('\n') ? '\n' : ''
  const replacement = `${linePrefix}${action.before}${selected}${action.after ?? ''}${lineSuffix}`
  const nextValue = `${beforeSelection}${replacement}${afterSelection}`
  const nextSelectionStart = beforeSelection.length + linePrefix.length + action.before.length
  const nextSelectionEnd = nextSelectionStart + selected.length

  emit('update:modelValue', nextValue)
  requestAnimationFrame(() => {
    textarea.focus()
    textarea.setSelectionRange(nextSelectionStart, nextSelectionEnd)
  })
}
</script>

<style scoped>
.markdown-editor {
  display: grid;
  gap: 10px;
}

.markdown-toolbar {
  display: flex;
  flex-wrap: nowrap;
  gap: 6px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.markdown-toolbar button {
  min-width: 38px;
  min-height: 34px;
  border: 0;
  border-radius: 999px;
  background: var(--admin-primary-soft, #e8f0fe);
  color: var(--admin-primary-strong, #1558d6);
  font: inherit;
  font-size: 13px;
  font-weight: 820;
  white-space: nowrap;
  cursor: pointer;
}

.markdown-workspace {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 0.9fr);
  gap: 12px;
}

.markdown-workspace textarea {
  width: 100%;
  min-height: 360px;
  resize: vertical;
}

.markdown-preview {
  min-height: 360px;
  max-height: 640px;
  overflow: auto;
  padding: 14px;
  border: 1px solid var(--admin-line, rgba(0, 0, 0, 0.08));
  border-radius: 12px;
  background: #fff;
  color: var(--admin-ink, #202124);
  line-height: 1.75;
}

.markdown-preview :deep(h1),
.markdown-preview :deep(h2),
.markdown-preview :deep(h3),
.markdown-preview :deep(p),
.markdown-preview :deep(ul),
.markdown-preview :deep(ol),
.markdown-preview :deep(blockquote),
.markdown-preview :deep(pre) {
  margin-top: 0;
}

.markdown-preview :deep(pre) {
  overflow: auto;
  padding: 12px;
  border-radius: 10px;
  background: #111827;
  color: #e5e7eb;
}

.markdown-preview :deep(blockquote) {
  margin-inline: 0;
  padding-left: 12px;
  border-left: 3px solid var(--admin-primary, #1a73e8);
  color: var(--admin-muted, #5f6368);
}

.markdown-preview :deep(u) {
  text-decoration-thickness: 2px;
  text-underline-offset: 3px;
}

@media (max-width: 1100px) {
  .markdown-workspace {
    grid-template-columns: 1fr;
  }
}
</style>
