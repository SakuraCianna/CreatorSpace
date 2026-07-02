<template>
  <section class="cms-page page-config-page">
    <header class="cms-header">
      <div>
        <h2>页面配置</h2>
        <p>维护页面标题、URL 标识、SEO 信息、页面内容 JSON 和发布状态；不涉及主题配置。</p>
      </div>
      <div class="header-actions">
        <button class="button button-tonal" type="button" :disabled="loading" @click="loadSettings">
          <RefreshCw :size="16" />
          刷新
        </button>
        <button class="button button-filled" type="button" @click="resetForm">
          <Plus :size="16" />
          新建页面
        </button>
      </div>
    </header>

    <section class="cms-grid page-config-grid">
      <form class="cms-panel form-panel" @submit.prevent="savePage">
        <div class="panel-title">
          <h3>{{ editingId ? '编辑页面' : '新建页面' }}</h3>
          <span>{{ statusLabel(form.status) }}</span>
        </div>

        <div class="form-line">
          <label>
            页面键
            <input v-model="form.pageKey" maxlength="120" placeholder="home" />
          </label>
          <label>
            URL 标识
            <input v-model="form.slug" maxlength="180" placeholder="home" />
          </label>
        </div>

        <label>
          页面标题
          <input v-model="form.title" maxlength="180" />
        </label>

        <div class="form-line">
          <label>
            SEO 标题
            <input v-model="form.seoTitle" maxlength="180" />
          </label>
          <label>
            页面状态
            <select v-model="form.status">
              <option value="DRAFT">草稿</option>
              <option value="PUBLISHED">已发布</option>
              <option value="ARCHIVED">已归档</option>
            </select>
          </label>
        </div>

        <label>
          SEO 描述
          <textarea v-model="form.seoDescription" rows="3" maxlength="1000" />
        </label>

        <label>
          页面内容 JSON
          <textarea v-model="contentJsonText" class="json-editor" rows="14" spellcheck="false" />
        </label>

        <div class="json-tools">
          <button class="button button-tonal button-compact" type="button" @click="formatContentJson">
            <Braces :size="16" />
            格式化 JSON
          </button>
          <span :class="{ danger: !isJsonValid }">{{ jsonStatus }}</span>
        </div>

        <div class="form-actions">
          <button class="button button-filled" type="submit" :disabled="saving">
            {{ editingId ? '保存页面' : '创建页面' }}
          </button>
          <button class="button button-tonal" type="button" @click="resetForm">重置</button>
        </div>
      </form>

      <div class="cms-panel">
        <div class="panel-title">
          <h3>页面列表</h3>
          <span>共 {{ pages.length }} 条</span>
        </div>

        <div class="filter-bar">
          <input v-model="keyword" placeholder="搜索标题 / 页面键 / URL 标识" />
          <select v-model="statusFilter">
            <option value="ALL">全部状态</option>
            <option value="DRAFT">草稿</option>
            <option value="PUBLISHED">已发布</option>
            <option value="ARCHIVED">已归档</option>
          </select>
        </div>

        <div class="list-stack">
          <article v-for="page in filteredPages" :key="pageKey(page)" class="table-row table-row--rich">
            <div>
              <strong>{{ page.title }}</strong>
              <span>{{ page.pageKey }} - /{{ page.slug }}</span>
              <small v-if="page.seoTitle">{{ page.seoTitle }}</small>
            </div>
            <div class="row-actions">
              <span class="status-chip" :class="{ muted: page.status !== 'PUBLISHED' }">
                {{ statusLabel(page.status) }}
              </span>
              <button class="text-button" type="button" @click="editPage(page)">编辑</button>
              <button class="text-button" type="button" @click="duplicatePage(page)">复制</button>
            </div>
          </article>
          <p v-if="filteredPages.length === 0 && !loading" class="empty-hint">暂无页面配置。</p>
          <p v-if="loading" class="empty-hint">页面配置加载中...</p>
        </div>
      </div>
    </section>

    <p v-if="notice" class="inline-notice" :class="{ danger: noticeType === 'error' }">{{ notice }}</p>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Braces, Plus, RefreshCw } from '@lucide/vue'

import { fetchAdminSiteSettings, updateSiteSettings } from '../services/content'
import { toUserMessage } from '../services/http'
import type { PageConfig } from '../shared/domain'

type PageStatus = PageConfig['status']
type PageStatusFilter = PageStatus | 'ALL'

const pages = ref<PageConfig[]>([])
const editingId = ref<number | null>(null)
const keyword = ref('')
const statusFilter = ref<PageStatusFilter>('ALL')
const contentJsonText = ref('{}')
const notice = ref('')
const noticeType = ref<'success' | 'error'>('success')
const loading = ref(false)
const saving = ref(false)
const preservedLayoutJson = ref<Record<string, unknown>>({})

const form = reactive({
  pageKey: '',
  title: '',
  slug: '',
  seoTitle: '',
  seoDescription: '',
  status: 'DRAFT' as PageStatus,
})

const filteredPages = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  return pages.value.filter((page) => {
    const matchesStatus = statusFilter.value === 'ALL' || page.status === statusFilter.value
    const haystack = `${page.pageKey} ${page.title} ${page.slug} ${page.seoTitle ?? ''}`.toLowerCase()
    return matchesStatus && (!q || haystack.includes(q))
  })
})

const isJsonValid = computed(() => Boolean(parseJsonObject(contentJsonText.value)))
const jsonStatus = computed(() => (isJsonValid.value ? 'JSON 有效' : 'JSON 无效'))

onMounted(loadSettings)

async function loadSettings() {
  loading.value = true
  clearNotice()
  try {
    const settings = await fetchAdminSiteSettings()
    pages.value = settings.pages ?? []
    if (editingId.value) {
      const current = pages.value.find((page) => page.id === editingId.value)
      if (current) {
        editPage(current)
      }
    }
  } catch (error) {
    showError(toUserMessage(error, '页面配置加载失败'))
  } finally {
    loading.value = false
  }
}

async function savePage() {
  clearNotice()
  const payload = normalizePage()
  if (!payload) {
    return
  }
  saving.value = true
  try {
    const nextPages = upsertLocalPage(payload)
    const settings = await updateSiteSettings({ pages: nextPages })
    pages.value = settings.pages ?? []
    editingId.value = payload.id ?? pages.value.find((page) => page.pageKey === payload.pageKey)?.id ?? null
    showSuccess('页面配置已保存')
  } catch (error) {
    showError(toUserMessage(error, '页面配置保存失败'))
  } finally {
    saving.value = false
  }
}

function editPage(page: PageConfig) {
  editingId.value = page.id ?? null
  form.pageKey = page.pageKey
  form.title = page.title
  form.slug = page.slug
  form.seoTitle = page.seoTitle ?? ''
  form.seoDescription = page.seoDescription ?? ''
  form.status = page.status
  preservedLayoutJson.value = cloneJsonObject(page.layoutJson)
  contentJsonText.value = stringifyJson(page.contentJson)
}

function duplicatePage(page: PageConfig) {
  editingId.value = null
  form.pageKey = `${page.pageKey}-copy`
  form.title = `${page.title} 副本`
  form.slug = `${page.slug}-copy`
  form.seoTitle = page.seoTitle ?? ''
  form.seoDescription = page.seoDescription ?? ''
  form.status = 'DRAFT'
  preservedLayoutJson.value = cloneJsonObject(page.layoutJson)
  contentJsonText.value = stringifyJson(page.contentJson)
  showSuccess('已复制为草稿，请修改页面键和 URL 标识后保存')
}

function resetForm() {
  editingId.value = null
  form.pageKey = ''
  form.title = ''
  form.slug = ''
  form.seoTitle = ''
  form.seoDescription = ''
  form.status = 'DRAFT'
  preservedLayoutJson.value = {}
  contentJsonText.value = '{}'
  clearNotice()
}

function formatContentJson() {
  const parsed = parseJsonObject(contentJsonText.value)
  if (!parsed) {
    showError('内容 JSON 必须是合法的 JSON 对象')
    return
  }
  contentJsonText.value = stringifyJson(parsed)
  clearNotice()
}

function normalizePage(): PageConfig | null {
  const pageKey = form.pageKey.trim()
  const title = form.title.trim()
  const slug = form.slug.trim().toLowerCase()
  if (!pageKey || !title || !slug) {
    showError('页面键、标题和 URL 标识不能为空')
    return null
  }
  if (!/^[a-z0-9]+(?:[._-][a-z0-9]+)*$/.test(pageKey)) {
    showError('页面键只支持小写字母、数字、点、下划线和连字符')
    return null
  }
  if (!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(slug)) {
    showError('URL 标识只支持小写字母、数字和连字符')
    return null
  }
  const contentJson = parseJsonObject(contentJsonText.value)
  if (!contentJson) {
    showError('内容 JSON 必须是合法的 JSON 对象')
    return null
  }
  return {
    id: editingId.value,
    pageKey,
    title,
    slug,
    seoTitle: form.seoTitle.trim() || null,
    seoDescription: form.seoDescription.trim() || null,
    contentJson,
    layoutJson: preservedLayoutJson.value,
    status: form.status,
  }
}

function upsertLocalPage(payload: PageConfig) {
  const index = pages.value.findIndex((page) => {
    if (payload.id && page.id === payload.id) {
      return true
    }
    return page.pageKey === payload.pageKey
  })
  if (index >= 0) {
    return pages.value.map((page, currentIndex) => currentIndex === index ? payload : page)
  }
  return [...pages.value, payload]
}

function parseJsonObject(value: string): Record<string, unknown> | null {
  try {
    const parsed = JSON.parse(value || '{}')
    if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
      return null
    }
    return parsed as Record<string, unknown>
  } catch {
    return null
  }
}

function cloneJsonObject(value: Record<string, unknown> | undefined) {
  return parseJsonObject(JSON.stringify(value ?? {})) ?? {}
}

function stringifyJson(value: Record<string, unknown> | undefined) {
  return JSON.stringify(value ?? {}, null, 2)
}

function pageKey(page: PageConfig) {
  return page.id ?? page.pageKey
}

function statusLabel(status: PageStatus) {
  return {
    DRAFT: '草稿',
    PUBLISHED: '已发布',
    ARCHIVED: '已归档',
  }[status] ?? status
}

function showSuccess(message: string) {
  noticeType.value = 'success'
  notice.value = message
}

function showError(message: string) {
  noticeType.value = 'error'
  notice.value = message
}

function clearNotice() {
  notice.value = ''
  noticeType.value = 'success'
}
</script>

<style scoped>
.page-config-page {
  display: grid;
  gap: 18px;
}

.cms-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 22px 24px;
}

.cms-header p {
  margin: 8px 0 0;
  color: var(--admin-muted);
  font-size: 14px;
}

.header-actions {
  display: inline-flex;
  gap: 10px;
  flex-wrap: wrap;
}

.cms-grid {
  display: grid;
  align-items: start;
  gap: 14px;
}

.page-config-grid {
  grid-template-columns: minmax(320px, 0.82fr) minmax(0, 1fr);
}

.json-editor {
  min-height: 280px;
  font-family: "JetBrains Mono", "Cascadia Code", Consolas, monospace;
  font-size: 13px;
  line-height: 1.55;
}

.json-tools,
.filter-bar,
.row-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.json-tools {
  justify-content: space-between;
  color: var(--admin-muted);
  font-size: 13px;
  font-weight: 720;
}

.filter-bar {
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.filter-bar input {
  flex: 1 1 220px;
}

.filter-bar select {
  flex: 0 0 170px;
}

.page-config-page .table-row > .row-actions {
  display: grid;
  justify-items: center;
  align-content: start;
  gap: 10px;
  min-width: 96px;
}

.page-config-page .row-actions .text-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 72px;
  min-height: 30px;
  box-sizing: border-box;
  line-height: 1;
  text-align: center;
  text-decoration: none;
}

.table-row small {
  color: var(--admin-muted);
  font-size: 12px;
}

.empty-hint {
  color: var(--admin-muted);
  font-size: 13px;
}

.inline-notice {
  margin: 0;
  color: var(--admin-primary-strong);
  font-size: 14px;
  font-weight: 760;
}

.danger {
  color: var(--admin-danger);
}

@media (max-width: 1020px) {
  .cms-header,
  .page-config-grid {
    grid-template-columns: 1fr;
  }

  .cms-header {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 640px) {
  .json-tools,
  .filter-bar {
    align-items: stretch;
    flex-direction: column;
  }

  .filter-bar select {
    flex-basis: auto;
  }
}
</style>
