<template>
  <section class="ai-page">
    <AdminPageHeader title="AI 创作助手" description="生成摘要、标签、审核意见和运营建议。所有结果只作为建议，最终动作由管理员确认。" theme="purple">
      <button class="icon-button" type="button" title="刷新建议" aria-label="刷新建议" @click="refreshSuggestions">
        <RefreshCw :size="18" />
      </button>
    </AdminPageHeader>

    <section class="ai-panel workflow-panel">
      <div class="board-title">
        <div>
          <h3>数据工作流</h3>
          <span>把搜索、访问、热门内容和审核队列整理成可采纳的运营建议。</span>
        </div>
        <label class="days-control">
          周期
          <input v-model.number="workflowDays" min="1" max="90" type="number" />
        </label>
      </div>
      <div class="workflow-actions">
        <button v-for="item in workflowOptions" :key="item.value" class="workflow-card" type="button" :disabled="submitting" @click="runWorkflow(item.value)">
          <component :is="item.icon" :size="18" />
          <strong>{{ item.label }}</strong>
          <span>{{ item.description }}</span>
        </button>
      </div>
    </section>

    <section class="ai-grid">
      <form class="ai-panel ai-composer" @submit.prevent="submitTask">
        <div class="panel-title">
          <div>
            <h3>创建 AI 任务</h3>
            <span>输入内容越具体，建议越容易被人工判断。</span>
          </div>
          <WandSparkles :size="20" />
        </div>

        <div class="task-options" role="tablist" aria-label="AI 任务类型">
          <button
            v-for="item in taskOptions"
            :key="item.value"
            type="button"
            :class="{ active: form.taskType === item.value }"
            @click="form.taskType = item.value"
          >
            <component :is="item.icon" :size="16" />
            {{ item.label }}
          </button>
        </div>

        <div class="form-line">
          <label>
            目标类型
            <BaseSelect v-model="form.targetType" :options="targetTypeOptions" />
          </label>
          <label>
            搜索目标
            <div class="target-search-row">
              <input
                v-model="targetSearch"
                type="search"
                :disabled="!canPickTarget"
                :placeholder="targetSearchPlaceholder"
                @input="scheduleTargetSearch"
                @keydown.enter.prevent="searchTargets"
              />
              <button class="button button-tonal" type="button" :disabled="!canPickTarget || loadingTargets" @click="searchTargets">
                搜索
              </button>
            </div>
          </label>
        </div>

        <div v-if="canPickTarget" class="target-picker">
          <div v-if="selectedTarget" class="target-picker__summary">
            <span>已选择：{{ selectedTarget.title }}</span>
            <button type="button" @click="clearTargetSelection">清除</button>
          </div>
          <div v-else class="target-picker__summary muted">
            <span>{{ loadingTargets ? '正在加载目标内容' : '未选择目标内容' }}</span>
          </div>
          <div v-if="targetOptions.length" class="target-options">
            <button
              v-for="item in targetOptions"
              :key="`${item.type}-${item.id}`"
              type="button"
              :class="{ active: selectedTarget?.type === item.type && selectedTarget?.id === item.id }"
              @click="selectTarget(item)"
            >
              <strong>{{ item.title }}</strong>
              <span>{{ item.meta }}</span>
              <small>{{ item.detail }}</small>
            </button>
          </div>
          <p v-else-if="!loadingTargets" class="target-picker__empty">没有找到匹配内容</p>
        </div>
        <label class="prompt-box">
          提示词
          <textarea v-model="form.prompt" maxlength="4000" rows="9" placeholder="例如：请根据这篇草稿生成摘要，并给出 3 个标签建议。" />
        </label>

        <div class="form-actions">
          <button class="button button-filled" type="submit" :disabled="submitting">
            <Sparkles :size="16" />
            {{ streamingReply ? '生成中' : '生成建议' }}
          </button>
          <button class="button button-tonal" type="button" @click="resetForm">
            <RotateCcw :size="16" />
            重置
          </button>
        </div>
      </form>

      <aside class="ai-panel ai-result">
        <div class="panel-title">
          <div>
            <h3>最近对话</h3>
            <span>{{ latestTask ? statusLabel(latestTask.status) : (loadingTasks ? '正在读取历史' : '等待选择') }}</span>
          </div>
          <button class="icon-button icon-button--small" type="button" title="刷新对话" aria-label="刷新对话" :disabled="loadingTasks" @click="loadTaskHistory">
            <RefreshCw :size="16" />
          </button>
        </div>

        <div v-if="taskHistory.length" class="task-history" aria-label="最近 AI 对话">
          <button
            v-for="task in taskHistory"
            :key="task.id"
            type="button"
            :class="{ active: latestTask?.id === task.id }"
            @click="selectTask(task.id)"
          >
            <strong>{{ taskLabel(task.taskType) }} · {{ targetText(task.targetType, task.targetId) }}</strong>
            <span>{{ taskPromptPreview(task) }}</span>
            <small>{{ statusLabel(task.status) }} · {{ formatDateTime(task.updatedAt || task.createdAt) }}</small>
          </button>
        </div>
        <p v-else-if="!loadingTasks" class="task-history-empty">暂无历史对话</p>

        <div v-if="latestTask" class="task-card">
          <div class="task-meta">
            <span>{{ taskLabel(latestTask.taskType) }}</span>
            <span>{{ targetText(latestTask.targetType, latestTask.targetId) }}</span>
            <span>{{ latestTask.provider }} / {{ latestTask.modelName || '-' }}</span>
            <span class="status-chip" :class="statusTone(latestTask.status)">{{ latestTask.status }}</span>
          </div>
          <p v-if="latestTask.notice" class="notice-card">{{ latestTask.notice }}</p>
          <div ref="messageWindow" class="message-window" aria-label="当前 AI 对话">
            <article v-for="message in latestTask.messages" :key="message.id" class="message-card" :class="message.role.toLowerCase()">
              <strong>{{ roleLabel(message.role) }}</strong>
              <p>{{ message.content }}</p>
            </article>
          </div>
          <form class="follow-up" @submit.prevent="submitFollowUp">
            <input v-model="followUpPrompt" maxlength="4000" type="text" placeholder="继续这个对话：例如，把建议拆成今天可执行的三步" />
            <button class="button button-filled" type="submit" :disabled="submitting || streamingReply || !followUpPrompt.trim()">
              <Send :size="16" />
              追问
            </button>
          </form>
        </div>

        <div v-else class="empty-state">
          <Bot :size="30" />
          <strong>还没有选中对话</strong>
          <span>创建新任务，或从上方最近对话中选择一条继续。</span>
        </div>
      </aside>
    </section>

    <section class="ai-panel suggestions-panel">
      <div class="board-title">
        <div>
          <h3>建议队列</h3>
          <span>共 {{ suggestions.total }} 条 · 第 {{ suggestions.page }} / {{ totalPages }} 页</span>
        </div>
        <div class="suggestion-filters">
          <BaseSelect v-model="suggestionStatus" :options="suggestionStatusOptions" @change="loadSuggestions(1)" />
        </div>
      </div>

      <div class="suggestion-list">
        <article v-for="item in suggestions.records" :key="item.id" class="suggestion-card">
          <div class="suggestion-main">
            <span class="suggestion-type">{{ suggestionTypeLabel(item.suggestionType) }}</span>
            <strong>{{ targetText(item.targetType, item.targetId) }}</strong>
            <p>{{ item.content }}</p>
            <small>{{ formatDateTime(item.createdAt) }}</small>
          </div>
          <div class="suggestion-actions">
            <span class="status-chip" :class="statusTone(item.status)">{{ suggestionStatusLabel(item.status) }}</span>
            <button class="button button-tonal" type="button" :disabled="item.status !== 'PENDING'" @click="handleSuggestion(item.id, 'ignore')">
              <X :size="16" />
              忽略
            </button>
            <button class="button button-filled" type="button" :disabled="item.status !== 'PENDING'" @click="handleSuggestion(item.id, 'adopt')">
              <Check :size="16" />
              采纳
            </button>
          </div>
        </article>
      </div>

      <div v-if="!loadingSuggestions && suggestions.records.length === 0" class="empty-state empty-state--wide">
        <Sparkles :size="28" />
        <strong>暂无建议</strong>
        <span>创建任务后，可采纳建议会出现在这里。</span>
      </div>

      <footer class="pager">
        <button class="button button-tonal" type="button" :disabled="suggestions.page <= 1 || loadingSuggestions" @click="loadSuggestions(suggestions.page - 1)">
          <ChevronLeft :size="16" />
          上一页
        </button>
        <span>{{ suggestionRange }}</span>
        <button class="button button-tonal" type="button" :disabled="suggestions.page >= totalPages || loadingSuggestions" @click="loadSuggestions(suggestions.page + 1)">
          下一页
          <ChevronRight :size="16" />
        </button>
      </footer>
    </section>

    <p v-if="notice" class="inline-notice">{{ notice }}</p>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import AdminPageHeader from '../components/admin/AdminPageHeader.vue'
import {
  Bot,
  Check,
  ClipboardList,
  ChevronLeft,
  ChevronRight,
  FileText,
  MessageSquareWarning,
  Newspaper,
  RefreshCw,
  RotateCcw,
  Send,
  Sparkles,
  Tags,
  TrendingUp,
  WandSparkles,
  X,
} from '@lucide/vue'
import BaseSelect from '../shared/components/BaseSelect.vue'

import { continueAiTask, createAiTask, createAiWorkflow, fetchAiSuggestions, updateAiSuggestionStatus } from '../services/content'
import { toUserMessage } from '../services/http'
import type { AiSuggestionStatus, AiSuggestionSummary, AiTaskPayload, AiTaskSummary, AiTaskType, AiWorkflowPayload, PageResponse } from '../shared/domain'

const taskOptions: Array<{ value: AiTaskType; label: string; icon: unknown }> = [
  { value: 'SUMMARY', label: '摘要', icon: FileText },
  { value: 'TAGS', label: '标签', icon: Tags },
  { value: 'REVIEW', label: '审核', icon: MessageSquareWarning },
  { value: 'OPERATION', label: '运营', icon: TrendingUp },
]

const form = reactive<AiTaskPayload>({
  taskType: 'SUMMARY',
  targetType: '',
  targetId: null,
  prompt: '',
})

const latestTask = ref<AiTaskSummary | null>(null)
const messageWindow = ref<HTMLElement | null>(null)
const suggestions = ref<PageResponse<AiSuggestionSummary>>({ records: [], page: 1, pageSize: 10, total: 0 })
const suggestionStatus = ref<AiSuggestionStatus | 'ALL'>('PENDING')
const submitting = ref(false)
const loadingSuggestions = ref(false)
const loadingTasks = ref(false)
const streamingReply = ref(false)
const taskHistory = ref<AiTaskSummary[]>([])
const notice = ref('')
const followUpPrompt = ref('')
const workflowDays = ref(7)
const targetSearch = ref('')
const targetOptions = ref<SelectableTarget[]>([])
const selectedTarget = ref<SelectableTarget | null>(null)
const loadingTargets = ref(false)
let targetSearchTimer: ReturnType<typeof setTimeout> | undefined

const targetTypeOptions = [
  { label: '不绑定', value: '' },
  { label: '文章', value: 'ARTICLE' },
  { label: '作品', value: 'PROJECT' },
  { label: '评论', value: 'COMMENT' },
]

const suggestionStatusOptions = [
  { label: '待处理', value: 'PENDING' },
  { label: '已采纳', value: 'ADOPTED' },
  { label: '已忽略', value: 'REJECTED' },
  { label: '全部', value: 'ALL' },
]

const totalPages = computed(() => Math.max(1, Math.ceil(suggestions.value.total / suggestions.value.pageSize)))
const suggestionRange = computed(() => {
  if (suggestions.value.total === 0) return '0 - 0 / 0'
  const start = (suggestions.value.page - 1) * suggestions.value.pageSize + 1
  const end = Math.min(suggestions.value.page * suggestions.value.pageSize, suggestions.value.total)
  return `${start} - ${end} / ${suggestions.value.total}`
})
const canPickTarget = computed(() => isPickableTargetType(form.targetType))
const targetSearchPlaceholder = computed(() => {
  switch (form.targetType) {
    case 'ARTICLE': return '搜索文章标题、摘要'
    case 'PROJECT': return '搜索作品标题、描述'
    case 'COMMENT': return '搜索评论内容、用户或 ID'
    default: return '选择目标类型后搜索'
  }
})

watch(() => form.targetType, () => {
  clearTargetSelection()
  targetSearch.value = ''
  targetOptions.value = []
  if (canPickTarget.value) {
    void searchTargets()
  }
})

onMounted(() => {
  void loadSuggestions()
  void loadTaskHistory()
})

function isPickableTargetType(value: AiTaskPayload['targetType']): value is PickableTargetType {
  return value === 'ARTICLE' || value === 'PROJECT' || value === 'COMMENT'
}

function scheduleTargetSearch() {
  if (targetSearchTimer) clearTimeout(targetSearchTimer)
  targetSearchTimer = setTimeout(() => {
    void searchTargets()
  }, 260)
}

async function searchTargets() {
  if (!isPickableTargetType(form.targetType)) return
  const targetType = form.targetType
  const keyword = targetSearch.value.trim()
  loadingTargets.value = true
  try {
    if (targetType === 'ARTICLE') {
      const page = await fetchAdminArticles({ keyword, status: 'ALL', page: 1, pageSize: 8 })
      targetOptions.value = page.records.map(toArticleTarget)
    } else if (targetType === 'PROJECT') {
      const page = await fetchAdminProjects({ keyword, status: 'ALL', page: 1, pageSize: 8 })
      targetOptions.value = page.records.map(toProjectTarget)
    } else {
      const page = await fetchAdminComments({ status: 'ALL', targetType: 'ALL', page: 1, pageSize: keyword ? 50 : 8 })
      targetOptions.value = page.records
        .filter((item) => matchesCommentTarget(item, keyword))
        .slice(0, 8)
        .map(toCommentTarget)
    }
  } catch (error) {
    notice.value = toUserMessage(error, '目标内容加载失败')
  } finally {
    loadingTargets.value = false
  }
}

function selectTarget(target: SelectableTarget) {
  selectedTarget.value = target
  form.targetType = target.type
  form.targetId = target.id
}

function clearTargetSelection() {
  selectedTarget.value = null
  form.targetId = null
}

function toArticleTarget(article: ArticleSummary): SelectableTarget {
  return {
    type: 'ARTICLE',
    id: article.id,
    title: article.title || `文章 #${article.id}`,
    meta: compactMeta(['文章', `#${article.id}`, article.status, article.category?.name]),
    detail: excerpt(article.summary || article.contentMarkdown || article.slug),
  }
}

function toProjectTarget(project: ProjectSummary): SelectableTarget {
  return {
    type: 'PROJECT',
    id: project.id,
    title: project.title || `作品 #${project.id}`,
    meta: compactMeta(['作品', `#${project.id}`, project.status, project.projectType]),
    detail: excerpt(project.description || project.contentMarkdown || project.slug),
  }
}

function toCommentTarget(comment: CommentSummary): SelectableTarget {
  return {
    type: 'COMMENT',
    id: comment.id,
    title: `${comment.username} 的评论`,
    meta: compactMeta(['评论', `#${comment.id}`, comment.status, targetText(comment.targetType, comment.targetId)]),
    detail: excerpt(comment.content),
  }
}

function matchesCommentTarget(comment: CommentSummary, keyword: string) {
  if (!keyword) return true
  const haystack = `${comment.id} ${comment.username} ${comment.content} ${comment.status}`.toLowerCase()
  return haystack.includes(keyword.toLowerCase())
}

function compactMeta(values: Array<string | number | null | undefined>) {
  return values.filter((value) => value !== null && value !== undefined && String(value).trim()).join(' · ')
}

function excerpt(value: string | null | undefined) {
  const normalized = (value || '').replace(/\s+/g, ' ').trim()
  return normalized ? (normalized.length > 90 ? `${normalized.slice(0, 90)}...` : normalized) : '暂无摘要'
}
function beginStreamTask(payload: AiTaskPayload, prompt: string) {
  const now = new Date().toISOString()
  latestTask.value = {
    id: 0,
    taskType: payload.taskType,
    targetType: payload.targetType || null,
    targetId: payload.targetId ?? null,
    prompt,
    status: 'RUNNING',
    provider: 'AI',
    modelName: null,
    createdBy: null,
    createdAt: now,
    updatedAt: now,
    suggestions: [],
    messages: [
      { id: -Date.now(), taskId: 0, role: 'USER', content: prompt, createdAt: now },
      { id: -Date.now() - 1, taskId: 0, role: 'ASSISTANT', content: '', createdAt: now },
    ],
  }
  streamingReply.value = true
  scrollMessagesToBottom()
}

function appendFollowUpPrompt(prompt: string) {
  if (!latestTask.value) return
  const now = new Date().toISOString()
  latestTask.value = {
    ...latestTask.value,
    status: 'RUNNING',
    updatedAt: now,
    messages: [
      ...latestTask.value.messages,
      { id: -Date.now(), taskId: latestTask.value.id, role: 'USER', content: prompt, createdAt: now },
      { id: -Date.now() - 1, taskId: latestTask.value.id, role: 'ASSISTANT', content: '', createdAt: now },
    ],
  }
  streamingReply.value = true
  scrollMessagesToBottom()
}

function appendStreamDelta(delta: string) {
  if (!latestTask.value || !delta) return
  const messages = [...latestTask.value.messages]
  let assistantIndex = -1
  for (let index = messages.length - 1; index >= 0; index -= 1) {
    if (messages[index].role === 'ASSISTANT') {
      assistantIndex = index
      break
    }
  }
  if (assistantIndex === -1) {
    const now = new Date().toISOString()
    messages.push({ id: -Date.now(), taskId: latestTask.value.id, role: 'ASSISTANT', content: delta, createdAt: now })
  } else {
    const current = messages[assistantIndex]
    messages[assistantIndex] = { ...current, content: `${current.content}${delta}` }
  }
  latestTask.value = { ...latestTask.value, messages }
  scrollMessagesToBottom()
}

function finishStreamTask(task: AiTaskSummary) {
  latestTask.value = task
  streamingReply.value = false
  if (task.notice) notice.value = task.notice
  scrollMessagesToBottom()
}

function scrollMessagesToBottom() {
  requestAnimationFrame(() => {
    const element = messageWindow.value
    if (element) element.scrollTop = element.scrollHeight
  })
}
async function loadTaskHistory() {
  loadingTasks.value = true
  try {
    const page = await fetchAiTasks({ page: 1, pageSize: 8 })
    taskHistory.value = page.records
    if (!latestTask.value && page.records.length > 0) {
      latestTask.value = page.records[0]
    } else if (latestTask.value) {
      const current = page.records.find((task) => task.id === latestTask.value?.id)
      if (current) latestTask.value = current
    }
  } catch (error) {
    notice.value = toUserMessage(error, 'AI 对话历史加载失败')
  } finally {
    loadingTasks.value = false
  }
}

async function selectTask(id: number) {
  notice.value = ''
  loadingTasks.value = true
  try {
    latestTask.value = await fetchAiTask(id)
    followUpPrompt.value = ''
  } catch (error) {
    notice.value = toUserMessage(error, 'AI 对话加载失败')
  } finally {
    loadingTasks.value = false
  }
}

function taskPromptPreview(task: AiTaskSummary) {
  return excerpt(task.prompt || task.messages.find((message) => message.role === 'USER')?.content || '')
}
async function runWorkflow(workflowType: AiWorkflowPayload['workflowType']) {
  notice.value = ''
  submitting.value = true
  const prompt = `生成 ${workflowDays.value} 天周期的数据工作流建议`
  beginStreamTask({ taskType: workflowType, targetType: 'SITE', targetId: null, prompt }, prompt)
  try {
    await streamAiWorkflow(
      { workflowType, days: workflowDays.value },
      {
        onDelta: appendStreamDelta,
        onDone: finishStreamTask,
      },
    )
    await loadSuggestions(1)
    await loadTaskHistory()
  } catch (error) {
    streamingReply.value = false
    notice.value = toUserMessage(error, 'AI 工作流创建失败')
  } finally {
    submitting.value = false
  }
}

async function submitTask() {
  notice.value = ''
  const prompt = form.prompt.trim()
  if (!prompt) {
    notice.value = '请先填写提示词'
    return
  }
  const payload: AiTaskPayload = {
    taskType: form.taskType,
    targetType: form.targetType || undefined,
    targetId: Number.isFinite(Number(form.targetId)) && Number(form.targetId) > 0 ? Number(form.targetId) : null,
    prompt,
  }
  submitting.value = true
  beginStreamTask(payload, prompt)
  try {
    await streamAiTask(payload, {
      onDelta: appendStreamDelta,
      onDone: finishStreamTask,
    })
    await loadSuggestions(1)
    await loadTaskHistory()
  } catch (error) {
    streamingReply.value = false
    notice.value = toUserMessage(error, 'AI 任务创建失败')
  } finally {
    submitting.value = false
  }
}

async function submitFollowUp() {
  if (!latestTask.value || !followUpPrompt.value.trim()) return
  notice.value = ''
  const taskId = latestTask.value.id
  const prompt = followUpPrompt.value.trim()
  followUpPrompt.value = ''
  submitting.value = true
  appendFollowUpPrompt(prompt)
  try {
    await streamAiFollowUp(taskId, prompt, {
      onDelta: appendStreamDelta,
      onDone: finishStreamTask,
    })
    await loadSuggestions(1)
    await loadTaskHistory()
  } catch (error) {
    streamingReply.value = false
    notice.value = toUserMessage(error, 'AI 追问失败')
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  form.taskType = 'SUMMARY'
  form.targetType = ''
  clearTargetSelection()
  targetSearch.value = ''
  targetOptions.value = []
  form.prompt = ''
}

function refreshSuggestions() {
  void loadSuggestions(suggestions.value.page)
}

async function loadSuggestions(page: unknown = suggestions.value.page) {
  const pageNumber = typeof page === 'number' && Number.isFinite(page)
    ? Math.max(1, Math.trunc(page))
    : suggestions.value.page
  loadingSuggestions.value = true
  try {
    suggestions.value = await fetchAiSuggestions({ status: suggestionStatus.value, page: pageNumber, pageSize: 10 })
  } catch (error) {
    notice.value = toUserMessage(error, 'AI 建议加载失败')
  } finally {
    loadingSuggestions.value = false
  }
}

async function handleSuggestion(id: number, action: 'adopt' | 'ignore') {
  notice.value = ''
  try {
    await updateAiSuggestionStatus(id, action)
    await loadSuggestions(suggestions.value.page)
  } catch (error) {
    notice.value = toUserMessage(error, 'AI 建议状态更新失败')
  }
}

function taskLabel(type: string) {
  return taskOptions.find((item) => item.value === type)?.label ?? type
}

function roleLabel(role: string) {
  const labels: Record<string, string> = { USER: '管理员输入', ASSISTANT: 'AI 回复', SYSTEM: '系统', TOOL: '工具' }
  return labels[role] ?? role
}

function statusLabel(status: string) {
  const labels: Record<string, string> = {
    PENDING: '等待处理',
    RUNNING: '生成中',
    SUCCEEDED: '已生成',
    FAILED: '未完成',
    DISCARDED: '已丢弃',
  }
  return labels[status] ?? status
}

function suggestionStatusLabel(status: string) {
  const labels: Record<string, string> = { PENDING: '待处理', ADOPTED: '已采纳', REJECTED: '已忽略', EXPIRED: '已过期' }
  return labels[status] ?? status
}

function suggestionTypeLabel(type: string) {
  const labels: Record<string, string> = { SUMMARY: '摘要建议', TAG: '标签建议', REVIEW_NOTE: '审核建议', OPERATION_IDEA: '运营建议', GENERAL: '通用建议' }
  return labels[type] ?? type
}

function targetText(targetType?: string | null, targetId?: number | null) {
  const labels: Record<string, string> = { ARTICLE: '文章', PROJECT: '作品', COMMENT: '评论' }
  const label = targetType ? labels[targetType] ?? targetType : '未绑定对象'
  return targetId ? `${label} ID ${targetId}` : label
}

function statusTone(status: string) {
  return {
    ok: status === 'SUCCEEDED' || status === 'ADOPTED',
    warn: status === 'PENDING' || status === 'RUNNING',
    muted: status === 'FAILED' || status === 'REJECTED' || status === 'EXPIRED' || status === 'DISCARDED',
  }
}

function formatDateTime(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN', { hour12: false })
}
</script>

<style scoped>
.ai-page {
  display: grid;
  gap: 16px;
}

.ai-panel {
  border: 1px solid var(--admin-line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--admin-shadow);
}

.panel-title h3,
.board-title h3 {
  margin: 0;
  color: var(--admin-ink);
}

.panel-title span,
.board-title span {
  margin: 8px 0 0;
  color: var(--admin-muted);
  font-size: 14px;
}

.eyebrow {
  color: var(--admin-primary-strong);
  font-size: 12px;
  font-weight: 820;
  letter-spacing: 0;
}

.icon-button {
  display: inline-grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border: 1px solid var(--admin-line);
  border-radius: 8px;
  background: var(--admin-primary-soft);
  color: var(--admin-primary-strong);
  cursor: pointer;
}

.workflow-panel {
  gap: 16px;
}

.workflow-actions {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.workflow-card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 6px 10px;
  min-height: 104px;
  padding: 14px;
  border: 1px solid rgba(17, 24, 39, 0.08);
  border-radius: 8px;
  background: linear-gradient(145deg, #ffffff, #f7fbff);
  color: var(--admin-ink);
  text-align: left;
  cursor: pointer;
}

.workflow-card svg {
  grid-row: span 2;
  color: var(--admin-primary-strong);
}

.workflow-card strong {
  min-width: 0;
  font-size: 15px;
}

.workflow-card span {
  min-width: 0;
  color: var(--admin-muted);
  font-size: 12px;
  line-height: 1.45;
}

.workflow-card:hover:not(:disabled) {
  border-color: rgba(49, 91, 255, 0.28);
  box-shadow: 0 12px 30px rgba(49, 91, 255, 0.1);
}

.workflow-card:disabled {
  opacity: 0.52;
  cursor: not-allowed;
}

.days-control {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--admin-muted);
  font-size: 12px;
  font-weight: 760;
}

.days-control input {
  width: 72px;
  min-height: 34px;
  border: 1px solid var(--admin-line);
  border-radius: 8px;
  padding: 0 8px;
  font: inherit;
}

.ai-grid {
  display: grid;
  grid-template-columns: minmax(360px, 0.95fr) minmax(0, 1.05fr);
  gap: 14px;
  align-items: start;
}

.ai-panel {
  display: grid;
  gap: 14px;
  padding: 18px;
}

.panel-title,
.board-title,
.form-actions,
.suggestion-actions,
.pager {
  display: flex;
  align-items: center;
  gap: 10px;
}

.panel-title,
.board-title {
  justify-content: space-between;
}

.task-options {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.task-options button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 38px;
  border: 1px solid var(--admin-line);
  border-radius: 8px;
  background: #fff;
  color: #344154;
  font: inherit;
  font-weight: 760;
  cursor: pointer;
}

.task-options button.active,
.task-options button:hover {
  background: var(--admin-primary-soft);
  color: var(--admin-primary-strong);
}

.form-line {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.ai-composer label {
  display: grid;
  gap: 6px;
  color: var(--admin-muted);
  font-size: 12px;
  font-weight: 760;
}

.ai-composer input,
.ai-composer select,
.ai-composer textarea,
.suggestion-filters select {
  width: 100%;
  border: 1px solid var(--admin-line);
  border-radius: 8px;
  background: #fff;
  color: var(--admin-ink);
  font: inherit;
  font-size: 14px;
}

.ai-composer input,
.ai-composer select,
.suggestion-filters select {
  min-height: 38px;
  padding: 0 10px;
}

.ai-composer textarea {
  resize: vertical;
  min-height: 180px;
  padding: 12px;
  line-height: 1.65;
}

.target-search-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
}

.target-search-row input:disabled {
  background: #f8fafc;
  color: var(--admin-muted);
}

.target-picker {
  display: grid;
  gap: 10px;
  padding: 12px;
  border: 1px solid rgba(49, 91, 255, 0.14);
  border-radius: 8px;
  background: rgba(248, 250, 255, 0.72);
}

.target-picker__summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: var(--admin-primary-strong);
  font-weight: 820;
}

.target-picker__summary.muted {
  color: var(--admin-muted);
}

.target-picker__summary button {
  border: 0;
  background: transparent;
  color: var(--admin-primary-strong);
  font: inherit;
  font-size: 12px;
  font-weight: 820;
  cursor: pointer;
}

.target-options {
  display: grid;
  gap: 8px;
  max-height: 320px;
  overflow: auto;
}

.target-options button {
  display: grid;
  gap: 5px;
  width: 100%;
  padding: 10px 12px;
  border: 1px solid rgba(17, 24, 39, 0.08);
  border-radius: 8px;
  background: #fff;
  color: var(--admin-ink);
  text-align: left;
  cursor: pointer;
}

.target-options button:hover,
.target-options button.active {
  border-color: rgba(49, 91, 255, 0.34);
  background: var(--admin-primary-soft);
}

.target-options strong {
  font-size: 14px;
}

.target-options span,
.target-options small,
.target-picker__empty {
  margin: 0;
  color: var(--admin-muted);
  line-height: 1.45;
}

.task-history {
  display: grid;
  gap: 8px;
  max-height: 260px;
  overflow: auto;
  padding-right: 2px;
}

.task-history button {
  display: grid;
  gap: 5px;
  width: 100%;
  padding: 10px 12px;
  border: 1px solid rgba(17, 24, 39, 0.08);
  border-radius: 8px;
  background: #fff;
  color: var(--admin-ink);
  text-align: left;
  cursor: pointer;
}

.task-history button:hover,
.task-history button.active {
  border-color: rgba(49, 91, 255, 0.34);
  background: var(--admin-primary-soft);
}

.task-history strong {
  min-width: 0;
  font-size: 13px;
}

.task-history span,
.task-history small,
.task-history-empty {
  margin: 0;
  color: var(--admin-muted);
  line-height: 1.45;
}

.icon-button--small {
  width: 34px;
  height: 34px;
}

.task-card,
.suggestion-list {
  display: grid;
  gap: 10px;
}

.message-window {
  display: grid;
  gap: 10px;
  max-height: min(58vh, 620px);
  min-height: 220px;
  overflow: auto;
  padding-right: 4px;
  overscroll-behavior: contain;
}

.task-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.task-meta span,
.suggestion-type {
  min-height: 26px;
  padding: 5px 9px;
  border-radius: 999px;
  background: #eef2ff;
  color: var(--admin-primary-strong);
  font-size: 12px;
  font-weight: 820;
}

.notice-card,
.inline-notice {
  margin: 0;
  color: var(--admin-danger);
  font-weight: 760;
}

.notice-card {
  padding: 12px;
  border-radius: 8px;
  background: var(--admin-danger-soft);
}

.message-card {
  padding: 14px;
  border: 1px solid rgba(17, 24, 39, 0.08);
  border-radius: 8px;
  background: #fbfcff;
}

.message-card.assistant {
  border-color: rgba(49, 91, 255, 0.18);
  background: linear-gradient(145deg, #f7fbff, #ffffff);
}

.message-card strong {
  color: var(--admin-ink);
}

.follow-up {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
}

.follow-up input {
  min-width: 0;
  min-height: 38px;
  border: 1px solid var(--admin-line);
  border-radius: 8px;
  padding: 0 12px;
  font: inherit;
}

.message-card p,
.suggestion-card p {
  margin: 8px 0 0;
  color: #243044;
  line-height: 1.65;
  white-space: pre-wrap;
}

.message-card.assistant p:empty::after {
  content: '正在生成...';
  color: var(--admin-muted);
}

.suggestion-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  padding: 14px;
  border: 1px solid rgba(17, 24, 39, 0.08);
  border-radius: 8px;
  background: rgba(248, 250, 255, 0.74);
}

.suggestion-main {
  display: grid;
  gap: 6px;
}

.suggestion-main small {
  color: var(--admin-muted);
}

.suggestion-actions {
  justify-content: flex-end;
}

.status-chip {
  display: inline-grid;
  min-height: 26px;
  padding: 4px 10px;
  place-items: center;
  border-radius: 999px;
  background: #eef2ff;
  color: var(--admin-primary-strong);
  font-size: 12px;
  font-weight: 820;
}

.status-chip.ok {
  background: #e7f8ef;
  color: #047857;
}

.status-chip.warn {
  background: #fff7ed;
  color: #c2410c;
}

.status-chip.muted {
  background: #f1f5f9;
  color: #64748b;
}

.empty-state {
  display: grid;
  place-items: center;
  gap: 8px;
  min-height: 260px;
  color: var(--admin-muted);
  text-align: center;
}

.empty-state strong {
  color: var(--admin-ink);
}

.empty-state--wide {
  min-height: 160px;
}

.pager {
  justify-content: flex-end;
}

.pager span {
  min-width: 120px;
  color: var(--admin-muted);
  text-align: center;
}

.button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 38px;
  border-radius: 8px;
  white-space: nowrap;
}

.button:disabled {
  opacity: 0.48;
  cursor: not-allowed;
}

@media (max-width: 1080px) {
  .workflow-actions,
  .ai-grid,
  .suggestion-card {
    grid-template-columns: 1fr;
  }

  .suggestion-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .panel-title,
  .board-title,
  .pager {
    align-items: stretch;
    flex-direction: column;
  }

  .task-options,
  .form-line,
  .target-search-row,
  .follow-up {
    grid-template-columns: 1fr;
  }
}
</style>