<template>
  <section ref="root" class="admin-module">
    <header class="module-hero" data-reveal>
      <div>
        <h2>敏感词管理</h2>
        <p>新增、编辑、启用或禁用敏感词规则。评论和留言提交时自动匹配并处理。</p>
      </div>
      <button class="button button-filled" type="button" @click="startAdd">
        <Plus :size="16" />
        新增敏感词
      </button>
    </header>

    <form v-if="editingId !== null" class="workspace-grid" data-reveal @submit.prevent="saveWord">
      <div class="workspace-panel admin-form">
        <div class="panel-title">
          <h2>{{ editingId === -1 ? '新增敏感词' : '编辑敏感词' }}</h2>
          <span>{{ form.word || '新词' }}</span>
        </div>
        <label>
          敏感词
          <input v-model="form.word" maxlength="120" placeholder="输入敏感词内容" />
        </label>
        <div class="form-line">
          <label>
            匹配方式
            <select v-model="form.matchType">
              <option v-for="opt in matchTypeOptions" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </option>
            </select>
          </label>
          <label>
            处理级别
            <select v-model="form.severity">
              <option v-for="opt in severityOptions" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </option>
            </select>
          </label>
        </div>
        <label class="check-line">
          <input v-model="form.enabled" type="checkbox" />
          启用
        </label>
        <div class="form-actions">
          <button class="button button-filled" type="submit">
            {{ editingId === -1 ? '创建' : '保存' }}
          </button>
          <button class="button button-tonal" type="button" @click="cancelEdit">取消</button>
        </div>
      </div>
      <div class="workspace-panel">
        <div class="panel-title">
          <h2>匹配说明</h2>
          <span>规则参考</span>
        </div>
        <ul class="rule-list">
          <li><strong>精确匹配</strong> — 内容与敏感词完全相同才触发</li>
          <li><strong>包含匹配</strong> — 内容包含敏感词即触发（默认）</li>
          <li><strong>正则匹配</strong> — 用正则表达式匹配内容</li>
        </ul>
        <ul class="rule-list" style="margin-top: 16px;">
          <li><strong>人工审核</strong> — 标记为待审核</li>
          <li><strong>直接拒绝</strong> — 提交时拒绝请求</li>
          <li><strong>替换屏蔽</strong> — 用 * 替换敏感词（预留）</li>
        </ul>
      </div>
    </form>

    <div class="workspace-panel" data-reveal>
      <div class="panel-title">
        <h2>敏感词库</h2>
        <span>共 {{ words.length }} 条</span>
      </div>
      <article v-for="word in words" :key="word.id" class="table-row table-row--rich">
        <div>
          <strong>{{ word.word }}</strong>
          <span>{{ matchTypeLabel(word.matchType) }} · {{ severityLabel(word.severity) }} · {{ word.createdAt ?? '未记录时间' }}</span>
        </div>
        <div class="row-actions">
          <span class="status-chip" :class="word.enabled ? '' : 'status-chip--disabled'">
            {{ word.enabled ? '启用' : '禁用' }}
          </span>
          <button class="icon-text-button" type="button" @click="startEdit(word)">编辑</button>
          <button class="icon-text-button" type="button" @click="toggleWord(word)">{{ word.enabled ? '禁用' : '启用' }}</button>
          <button class="icon-text-button danger" type="button" @click="removeWord(word.id)">删除</button>
        </div>
      </article>
      <p v-if="words.length === 0" class="empty-editor">暂无敏感词，点击上方「新增敏感词」添加。</p>
    </div>

    <p v-if="notice" class="inline-notice">{{ notice }}</p>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Plus } from '@lucide/vue'

import {
  createSensitiveWord,
  deleteSensitiveWord,
  fetchAdminSensitiveWords,
  toggleSensitiveWord,
  updateSensitiveWord,
} from '@/services/content'
import { toUserMessage } from '@/services/http'
import { usePageReveal } from '@/shared/composables/usePageReveal'
import type { SensitiveWordSummary } from '@/shared/domain'

interface WordForm {
  word: string
  matchType: string
  severity: string
  enabled: boolean
}

const matchTypeOptions = [
  { value: 'CONTAINS', label: '包含匹配' },
  { value: 'EXACT', label: '精确匹配' },
  { value: 'REGEX', label: '正则匹配' },
]

const severityOptions = [
  { value: 'REJECT', label: '直接拒绝' },
  { value: 'REVIEW', label: '人工审核' },
  { value: 'MASK', label: '替换屏蔽' },
]

function matchTypeLabel(value: string) {
  return matchTypeOptions.find((opt) => opt.value === value)?.label ?? value
}

function severityLabel(value: string) {
  return severityOptions.find((opt) => opt.value === value)?.label ?? value
}

const root = ref<HTMLElement | null>(null)
const notice = ref('')
const words = ref<SensitiveWordSummary[]>([])
const editingId = ref<number | null>(null)
const form = reactive<WordForm>({
  word: '',
  matchType: 'CONTAINS',
  severity: 'REJECT',
  enabled: true,
})

usePageReveal(root)
onMounted(loadWords)

function resetForm() {
  form.word = ''
  form.matchType = 'CONTAINS'
  form.severity = 'REJECT'
  form.enabled = true
}

function startAdd() {
  resetForm()
  editingId.value = -1
}

function startEdit(word: SensitiveWordSummary) {
  form.word = word.word
  form.matchType = word.matchType
  form.severity = word.severity
  form.enabled = word.enabled
  editingId.value = word.id
}

function cancelEdit() {
  editingId.value = null
  resetForm()
}

async function loadWords() {
  try {
    const page = await fetchAdminSensitiveWords({ pageSize: 100 })
    words.value = page.records
  } catch (error) {
    notice.value = `加载失败：${toUserMessage(error, '请稍后再试')}`
  }
}

async function saveWord() {
  if (!form.word.trim()) {
    notice.value = '请填写敏感词内容'
    return
  }
  try {
    if (editingId.value === -1) {
      await createSensitiveWord({
        word: form.word.trim(),
        matchType: form.matchType,
        severity: form.severity,
        enabled: form.enabled,
      })
      notice.value = '敏感词已创建'
    } else if (editingId.value !== null) {
      await updateSensitiveWord(editingId.value, {
        word: form.word.trim(),
        matchType: form.matchType,
        severity: form.severity,
        enabled: form.enabled,
      })
      notice.value = '敏感词已更新'
    }
    cancelEdit()
    await loadWords()
  } catch (error) {
    notice.value = `保存失败：${toUserMessage(error, '请稍后再试')}`
  }
}

async function toggleWord(word: SensitiveWordSummary) {
  try {
    await toggleSensitiveWord(word.id)
    notice.value = word.enabled ? '敏感词已禁用' : '敏感词已启用'
    await loadWords()
  } catch (error) {
    notice.value = `操作失败：${toUserMessage(error, '请稍后再试')}`
  }
}

async function removeWord(id: number) {
  if (!window.confirm('确定删除该敏感词？')) {
    return
  }
  try {
    await deleteSensitiveWord(id)
    notice.value = '敏感词已删除'
    if (editingId.value === id) {
      cancelEdit()
    }
    await loadWords()
  } catch (error) {
    notice.value = `删除失败：${toUserMessage(error, '请稍后再试')}`
  }
}
</script>

<style scoped>
.module-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 16px;
  min-height: 152px;
  padding: 24px 28px;
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.88), rgba(255, 255, 255, 0.58)),
    radial-gradient(circle at 16% 20%, rgba(49, 91, 255, 0.14), transparent 34%),
    radial-gradient(circle at 88% 18%, rgba(0, 124, 114, 0.16), transparent 28%),
    linear-gradient(120deg, rgba(49, 91, 255, 0.08), rgba(194, 95, 58, 0.08), rgba(0, 124, 114, 0.1));
  box-shadow: var(--tone-shadow);
}

.module-hero h2 {
  max-width: 830px;
  margin: 0;
  color: var(--tone-ink);
  font-size: 32px;
  font-weight: 860;
  line-height: 1.16;
}

.module-hero p {
  max-width: 680px;
  margin: 10px 0 0;
  color: var(--tone-muted);
  font-size: 14px;
  line-height: 1.65;
}

.admin-module {
  display: grid;
  gap: 18px;
}

.workspace-panel {
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm);
  background: var(--tone-panel);
  box-shadow: var(--tone-shadow);
  backdrop-filter: blur(18px);
  padding: 16px;
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(300px, 0.75fr);
  gap: 12px;
  margin-top: 12px;
}

.admin-form {
  display: grid;
  gap: 12px;
}

.admin-form label {
  display: grid;
  gap: 8px;
}

.admin-form input,
.admin-form select {
  width: 100%;
  min-height: 42px;
  padding: 0 12px;
  border: 1px solid rgba(17, 24, 39, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.86);
  color: var(--tone-strong);
  font: inherit;
}

.form-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.form-line > * {
  flex: 1;
  min-width: 160px;
}

.form-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.check-line {
  display: flex !important;
  grid-template-columns: none;
  align-items: center;
  gap: 10px;
  color: var(--tone-muted);
  font-size: 13px;
  font-weight: 760;
}

.check-line input {
  width: 18px;
  min-height: 18px;
  min-width: unset;
  padding: 0;
}

.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 12px;
}

.panel-title h2 {
  margin: 0;
  font-size: 18px;
}

.panel-title span {
  color: var(--tone-muted);
  font-size: 13px;
}

.table-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  background: rgba(20, 21, 29, 0.04);
}

.table-row + .table-row {
  margin-top: 10px;
}

.table-row div {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.table-row strong {
  font-size: 15px;
}

.table-row span {
  color: var(--tone-muted);
  font-size: 13px;
}

.row-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(49, 91, 255, 0.12);
  color: #172554;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.status-chip--disabled {
  background: rgba(107, 114, 128, 0.16);
  color: #6b7280;
}

.icon-text-button {
  border: 0;
  background: transparent;
  color: #315bff;
  font: inherit;
  font-size: 13px;
  font-weight: 800;
  text-decoration: none;
  cursor: pointer;
}

.icon-text-button.danger {
  color: #b91c1c;
}

.empty-editor {
  margin: 16px 0;
  color: var(--tone-muted);
  font-size: 13px;
  text-align: center;
}

.inline-notice {
  margin-top: 6px;
  padding: 12px 14px;
  border-left: 3px solid var(--tone-coral);
  background: rgba(194, 95, 58, 0.08);
  color: #754226;
  font-size: 13px;
  line-height: 1.55;
}

.rule-list {
  display: grid;
  gap: 10px;
  margin: 0;
  padding-left: 18px;
}

.rule-list li {
  font-size: 13px;
  line-height: 1.5;
  color: var(--tone-muted);
}

.rule-list li strong {
  color: var(--tone-strong);
}

@media (max-width: 1020px) {
  .module-hero,
  .workspace-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .module-hero {
    align-items: flex-start;
    padding: 18px;
  }

  .module-hero h2 {
    font-size: 28px;
  }

  .panel-title {
    align-items: flex-start;
    flex-direction: column;
  }

  .table-row,
  .row-actions {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
