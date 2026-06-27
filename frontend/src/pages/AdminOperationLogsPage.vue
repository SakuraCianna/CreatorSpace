<template>
  <section class="audit-page">
    <header class="audit-hero">
      <div>
        <span class="eyebrow">AUDIT TRAIL</span>
        <h2>操作日志</h2>
        <p>按模块、动作、操作人和时间范围追踪后台关键变更。</p>
      </div>
      <button class="icon-button" type="button" title="刷新日志" aria-label="刷新日志" @click="loadLogs">
        <RefreshCw :size="18" />
      </button>
    </header>

    <form class="filter-bar" @submit.prevent="applyFilters">
      <label>
        模块
        <select v-model="filters.module">
          <option v-for="item in moduleOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
      </label>
      <label>
        操作
        <input v-model.trim="filters.operation" maxlength="80" placeholder="审核、更新、删除" />
      </label>
      <label>
        操作人 ID
        <input v-model.number="filters.operatorId" min="1" type="number" placeholder="全部" />
      </label>
      <label>
        开始时间
        <input v-model="filters.startTime" type="datetime-local" />
      </label>
      <label>
        结束时间
        <input v-model="filters.endTime" type="datetime-local" />
      </label>
      <label>
        每页
        <select v-model.number="filters.pageSize">
          <option :value="10">10</option>
          <option :value="20">20</option>
          <option :value="50">50</option>
          <option :value="100">100</option>
        </select>
      </label>
      <div class="filter-actions">
        <button class="button button-filled" type="submit">
          <Search :size="16" />
          筛选
        </button>
        <button class="button button-tonal" type="button" @click="resetFilters">
          <RotateCcw :size="16" />
          重置
        </button>
      </div>
    </form>

    <section class="audit-board">
      <div class="board-title">
        <div>
          <h3>审计记录</h3>
          <span>共 {{ page.total }} 条 · 第 {{ page.page }} / {{ totalPages }} 页</span>
        </div>
        <span class="status-chip">{{ loading ? '加载中' : '已同步' }}</span>
      </div>

      <div class="audit-table" role="table" aria-label="操作日志列表">
        <div class="audit-row audit-row--head" role="row">
          <span>时间</span>
          <span>模块</span>
          <span>操作</span>
          <span>请求</span>
          <span>操作者</span>
          <span>目标</span>
          <span></span>
        </div>
        <article v-for="log in page.records" :key="log.id" class="audit-item">
          <button class="audit-row" type="button" :aria-expanded="expandedId === log.id" @click="toggleExpanded(log.id)">
            <span class="time-cell">{{ formatDateTime(log.createdAt) }}</span>
            <span>
              <strong>{{ moduleLabel(log.module) }}</strong>
              <small>{{ log.module }}</small>
            </span>
            <span>
              <strong>{{ log.operation }}</strong>
              <small>{{ log.requestPath || '无路径' }}</small>
            </span>
            <span>
              <i :class="['method-pill', methodTone(log.requestMethod)]">{{ log.requestMethod || '-' }}</i>
            </span>
            <span>{{ log.operatorId ?? '系统' }}</span>
            <span>{{ targetText(log) }}</span>
            <ChevronDown :class="{ open: expandedId === log.id }" :size="18" />
          </button>
          <div v-if="expandedId === log.id" class="audit-detail">
            <dl>
              <div>
                <dt>IP 地址</dt>
                <dd>{{ log.ipAddress || '-' }}</dd>
              </div>
              <div>
                <dt>User-Agent</dt>
                <dd>{{ log.userAgent || '-' }}</dd>
              </div>
              <div>
                <dt>请求路径</dt>
                <dd>{{ log.requestPath || '-' }}</dd>
              </div>
              <div>
                <dt>详情 JSON</dt>
                <dd><code>{{ prettyDetail(log.detailJson) }}</code></dd>
              </div>
            </dl>
          </div>
        </article>
      </div>

      <div v-if="!loading && page.records.length === 0" class="empty-state">
        <ShieldCheck :size="28" />
        <strong>暂无匹配日志</strong>
        <span>调整筛选条件后再试。</span>
      </div>

      <footer class="pager">
        <button class="button button-tonal" type="button" :disabled="page.page <= 1 || loading" @click="goPage(page.page - 1)">
          <ChevronLeft :size="16" />
          上一页
        </button>
        <span>{{ rangeText }}</span>
        <button class="button button-tonal" type="button" :disabled="page.page >= totalPages || loading" @click="goPage(page.page + 1)">
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
import {
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  RefreshCw,
  RotateCcw,
  Search,
  ShieldCheck,
} from '@lucide/vue'

import { fetchAdminOperationLogs } from '@/services/content'
import { toUserMessage } from '@/services/http'
import type { OperationLogModule, OperationLogQuery, OperationLogSummary, PageResponse } from '@/shared/domain'

const moduleOptions: Array<{ value: OperationLogModule; label: string }> = [
  { value: 'ALL', label: '全部模块' },
  { value: 'ARTICLE', label: '文章' },
  { value: 'PROJECT', label: '作品' },
  { value: 'COMMENT', label: '评论' },
  { value: 'FILE', label: '文件' },
  { value: 'THEME', label: '主题' },
  { value: 'SITE', label: '站点' },
  { value: 'INSPIRATION', label: '灵感' },
  { value: 'GUESTBOOK', label: '留言' },
  { value: 'CATEGORY', label: '分类' },
  { value: 'TAG', label: '标签' },
  { value: 'ADMIN', label: '后台' },
]

const filters = reactive<Required<Pick<OperationLogQuery, 'module' | 'operation' | 'startTime' | 'endTime' | 'pageSize'>> & { operatorId: number | null }>({
  module: 'ALL',
  operation: '',
  operatorId: null,
  startTime: '',
  endTime: '',
  pageSize: 20,
})

const page = ref<PageResponse<OperationLogSummary>>({ records: [], page: 1, pageSize: 20, total: 0 })
const loading = ref(false)
const notice = ref('')
const expandedId = ref<number | null>(null)

const totalPages = computed(() => Math.max(1, Math.ceil(page.value.total / page.value.pageSize)))
const rangeText = computed(() => {
  if (page.value.total === 0) return '0 - 0 / 0'
  const start = (page.value.page - 1) * page.value.pageSize + 1
  const end = Math.min(page.value.page * page.value.pageSize, page.value.total)
  return `${start} - ${end} / ${page.value.total}`
})

onMounted(loadLogs)

async function loadLogs(targetPage = page.value.page) {
  loading.value = true
  notice.value = ''
  try {
    page.value = await fetchAdminOperationLogs(buildQuery(targetPage))
    expandedId.value = null
  } catch (error) {
    notice.value = toUserMessage(error, '操作日志加载失败')
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  loadLogs(1)
}

function resetFilters() {
  filters.module = 'ALL'
  filters.operation = ''
  filters.operatorId = null
  filters.startTime = ''
  filters.endTime = ''
  filters.pageSize = 20
  loadLogs(1)
}

function goPage(nextPage: number) {
  if (nextPage < 1 || nextPage > totalPages.value) return
  loadLogs(nextPage)
}

function buildQuery(targetPage: number): OperationLogQuery {
  return {
    module: filters.module,
    operation: filters.operation,
    operatorId: Number.isFinite(Number(filters.operatorId)) && Number(filters.operatorId) > 0 ? Number(filters.operatorId) : null,
    startTime: toBackendTime(filters.startTime),
    endTime: toBackendTime(filters.endTime),
    page: targetPage,
    pageSize: filters.pageSize,
  }
}

function toBackendTime(value: string) {
  return value ? new Date(value).toISOString() : ''
}

function toggleExpanded(id: number) {
  expandedId.value = expandedId.value === id ? null : id
}

function moduleLabel(module: string) {
  return moduleOptions.find((item) => item.value === module)?.label ?? module
}

function targetText(log: OperationLogSummary) {
  if (!log.targetType && !log.targetId) return '-'
  return `${log.targetType ?? 'TARGET'}${log.targetId ? ` #${log.targetId}` : ''}`
}

function formatDateTime(value: string) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', { hour12: false })
}

function methodTone(method?: string | null) {
  return `method-${(method ?? 'GET').toLowerCase()}`
}

function prettyDetail(value?: string | null) {
  if (!value) return '{}'
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}
</script>

<style scoped>
.audit-page {
  display: grid;
  gap: 16px;
}

.audit-hero,
.filter-bar,
.audit-board {
  border: 1px solid var(--admin-line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--admin-shadow);
}

.audit-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 24px;
}

.audit-hero h2,
.board-title h3 {
  margin: 0;
  color: var(--admin-ink);
}

.audit-hero h2 {
  font-size: 24px;
}

.audit-hero p {
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

.filter-bar {
  display: grid;
  grid-template-columns: minmax(130px, 0.8fr) minmax(180px, 1.1fr) minmax(120px, 0.7fr) repeat(2, minmax(180px, 1fr)) minmax(92px, 0.5fr) auto;
  align-items: end;
  gap: 12px;
  padding: 16px;
}

.filter-bar label {
  display: grid;
  gap: 6px;
  color: var(--admin-muted);
  font-size: 12px;
  font-weight: 760;
}

.filter-bar input,
.filter-bar select {
  min-height: 38px;
  width: 100%;
  border: 1px solid var(--admin-line);
  border-radius: 8px;
  background: #fff;
  color: var(--admin-ink);
  font: inherit;
  font-size: 14px;
  padding: 0 10px;
}

.filter-actions,
.pager,
.board-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.audit-board {
  display: grid;
  gap: 12px;
  padding: 18px;
}

.board-title {
  justify-content: space-between;
}

.board-title span {
  color: var(--admin-muted);
  font-size: 13px;
}

.audit-table {
  display: grid;
  gap: 8px;
}

.audit-row {
  display: grid;
  grid-template-columns: 150px 112px minmax(240px, 1.5fr) 78px 82px 116px 24px;
  align-items: center;
  gap: 12px;
  width: 100%;
  min-height: 54px;
  padding: 10px 12px;
  border: 1px solid rgba(17, 24, 39, 0.08);
  border-radius: 8px;
  background: rgba(248, 250, 255, 0.74);
  color: var(--admin-ink);
  text-align: left;
}

button.audit-row {
  cursor: pointer;
}

button.audit-row:hover {
  border-color: rgba(49, 91, 255, 0.24);
  background: #fff;
}

.audit-row--head {
  min-height: 36px;
  background: transparent;
  color: var(--admin-muted);
  font-size: 12px;
  font-weight: 820;
}

.audit-row strong,
.audit-row small {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-row small {
  margin-top: 3px;
  color: var(--admin-muted);
  font-size: 12px;
}

.time-cell {
  color: #243044;
  font-variant-numeric: tabular-nums;
}

.method-pill,
.status-chip {
  display: inline-grid;
  min-height: 26px;
  place-items: center;
  border-radius: 999px;
  font-size: 12px;
  font-style: normal;
  font-weight: 820;
}

.method-pill {
  width: 58px;
  background: #eef2ff;
  color: #315bff;
}

.method-post,
.method-put,
.method-patch {
  background: #e7f8ef;
  color: #047857;
}

.method-delete {
  background: #fee2e2;
  color: #b91c1c;
}

.audit-row svg {
  justify-self: end;
  color: var(--admin-muted);
  transition: transform 160ms ease;
}

.audit-row svg.open {
  transform: rotate(180deg);
}

.audit-detail {
  margin: -2px 8px 8px;
  padding: 14px;
  border: 1px solid rgba(49, 91, 255, 0.12);
  border-radius: 8px;
  background: #fbfcff;
}

.audit-detail dl {
  display: grid;
  gap: 10px;
  margin: 0;
}

.audit-detail div {
  display: grid;
  grid-template-columns: 100px minmax(0, 1fr);
  gap: 12px;
}

.audit-detail dt {
  color: var(--admin-muted);
  font-size: 12px;
  font-weight: 820;
}

.audit-detail dd {
  min-width: 0;
  margin: 0;
  color: var(--admin-ink);
  overflow-wrap: anywhere;
}

.audit-detail code {
  display: block;
  max-height: 220px;
  overflow: auto;
  padding: 10px;
  border-radius: 8px;
  background: #111827;
  color: #e5e7eb;
  font-size: 12px;
  line-height: 1.55;
  white-space: pre-wrap;
}

.empty-state {
  display: grid;
  place-items: center;
  gap: 8px;
  min-height: 180px;
  color: var(--admin-muted);
  text-align: center;
}

.empty-state strong {
  color: var(--admin-ink);
}

.pager {
  justify-content: flex-end;
  padding-top: 4px;
}

.pager span {
  min-width: 120px;
  color: var(--admin-muted);
  font-size: 13px;
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

.inline-notice {
  margin: 0;
  color: var(--admin-danger);
  font-weight: 760;
}

@media (max-width: 1180px) {
  .filter-bar {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .filter-actions {
    grid-column: 1 / -1;
  }

  .audit-table {
    overflow-x: auto;
  }

  .audit-row {
    min-width: 900px;
  }
}

@media (max-width: 720px) {
  .audit-hero,
  .board-title,
  .pager {
    align-items: stretch;
    flex-direction: column;
  }

  .filter-bar {
    grid-template-columns: 1fr;
  }
}
</style>