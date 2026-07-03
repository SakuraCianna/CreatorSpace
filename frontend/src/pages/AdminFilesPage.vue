<template>
  <section class="files-page">
    <header class="files-header">
      <div>
        <h2>文件资源库</h2>
        <p>管理后台上传的封面、插图、附件和站点资源。</p>
      </div>
      <div class="header-actions">
        <button class="button button-tonal" type="button" :disabled="loading" @click="loadFiles">
          <RefreshCw :size="16" />
          刷新
        </button>
        <button class="button button-filled" type="button" @click="mode = 'upload'">
          <Upload :size="16" />
          上传资源
        </button>
      </div>
    </header>

    <section class="file-mode-tabs" aria-label="文件管理模式">
      <button class="file-mode-tab" :class="{ 'is-active': mode === 'library' }" type="button" @click="mode = 'library'">
        资源列表
      </button>
      <button class="file-mode-tab" :class="{ 'is-active': mode === 'upload' }" type="button" @click="mode = 'upload'">
        上传资源
      </button>
    </section>

    <form v-if="mode === 'upload'" class="files-panel upload-panel" @submit.prevent="submitUpload">
      <div class="editor-heading">
        <div class="panel-title">
          <h3>上传资源</h3>
          <span>{{ uploadModuleLabel }}</span>
        </div>
        <button class="button button-tonal button-compact" type="button" @click="mode = 'library'">返回列表</button>
      </div>

      <label>
        归属模块
        <BaseSelect v-model="uploadModule" :options="uploadModules" />
      </label>

      <label class="file-drop">
        <input ref="fileInput" type="file" :accept="acceptedTypes" @change="selectFile" />
        <UploadCloud :size="24" />
        <strong>{{ selectedFile?.name ?? '选择文件' }}</strong>
        <span>{{ selectedFile ? formatSize(selectedFile.size) : `最大 ${appConfig.uploadMaxFileSizeMb} MB` }}</span>
      </label>

      <button class="button button-filled" type="submit" :disabled="uploading">
        <Upload :size="16" />
        {{ uploading ? '上传中' : '上传文件' }}
      </button>
    </form>

    <section v-else class="files-panel library-panel">
      <div class="panel-title">
        <h3>资源列表</h3>
        <span>共 {{ total }} 个</span>
      </div>

      <div class="filter-bar">
        <BaseSelect v-model="filterModule" :options="filterModules" @change="changeFilter" />
        <BaseSelect v-model="pageSize" :options="pageSizeOptions" @change="changePageSize" />
      </div>

      <div v-if="loading" class="empty-state">加载中...</div>
      <div v-else-if="files.length === 0" class="empty-state">当前筛选下还没有文件。</div>
      <div v-else class="resource-list">
        <article v-for="file in files" :key="file.id" class="resource-row">
          <a class="resource-preview" :href="file.publicUrl" target="_blank" rel="noreferrer">
            <img v-if="isImage(file)" :src="file.publicUrl" :alt="file.originalName" loading="lazy" />
            <FileText v-else :size="24" />
          </a>

          <div class="resource-main">
            <strong :title="file.originalName">{{ file.originalName }}</strong>
            <span>{{ moduleLabel(file.module) }} - {{ formatSize(file.fileSize) }} - {{ file.fileType }}</span>
            <code>{{ file.publicUrl }}</code>
          </div>

          <div class="row-actions">
            <button class="icon-button" type="button" title="复制 URL" @click="copyUrl(file.publicUrl)">
              <Copy :size="16" />
            </button>
            <a class="icon-button" :href="file.publicUrl" target="_blank" rel="noreferrer" title="打开文件">
              <ExternalLink :size="16" />
            </a>
            <button class="icon-button danger" type="button" title="删除文件" @click="removeFile(file)">
              <Trash2 :size="16" />
            </button>
          </div>
        </article>
      </div>

      <footer class="pager">
        <button class="button button-tonal" type="button" :disabled="page <= 1 || loading" @click="goPage(page - 1)">
          <ChevronLeft :size="16" />
          上一页
        </button>
        <span>第 {{ page }} / {{ totalPages }} 页</span>
        <button class="button button-tonal" type="button" :disabled="page >= totalPages || loading" @click="goPage(page + 1)">
          下一页
          <ChevronRight :size="16" />
        </button>
      </footer>
    </section>

    <p v-if="notice" class="inline-notice">{{ notice }}</p>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ChevronLeft, ChevronRight, Copy, ExternalLink, FileText, RefreshCw, Trash2, Upload, UploadCloud } from '@lucide/vue'

import { appConfig } from '../app/config'
import { deleteAdminFile, fetchAdminFiles, uploadAdminFile } from '../services/content'
import { toUserMessage } from '../services/http'
import BaseSelect from '../shared/components/BaseSelect.vue'
import type { FileResource } from '../shared/domain'

type FileModule = 'AVATAR' | 'COVER' | 'ARTICLE' | 'PROJECT' | 'INSPIRATION' | 'OTHER'
type ViewMode = 'library' | 'upload'

const moduleOptions: Array<{ value: FileModule; label: string }> = [
  { value: 'AVATAR', label: '头像' },
  { value: 'COVER', label: '封面' },
  { value: 'ARTICLE', label: '文章' },
  { value: 'PROJECT', label: '作品' },
  { value: 'INSPIRATION', label: '灵感' },
  { value: 'OTHER', label: '其他' },
]

const filterModules = [{ value: 'ALL', label: '全部模块' }, ...moduleOptions] as const
const uploadModules = moduleOptions
const pageSizeOptions = [
  { label: '12 / 页', value: 12 },
  { label: '24 / 页', value: 24 },
  { label: '48 / 页', value: 48 },
]
const acceptedTypes = '.webp,.png,.jpg,.jpeg,.gif,.pdf,.txt,.md,.markdown'

const files = ref<FileResource[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(12)
const filterModule = ref<(typeof filterModules)[number]['value']>('ALL')
const uploadModule = ref<FileModule>('OTHER')
const mode = ref<ViewMode>('library')
const selectedFile = ref<File | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)
const loading = ref(false)
const uploading = ref(false)
const notice = ref('')

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))
const uploadModuleLabel = computed(() => moduleLabel(uploadModule.value))

onMounted(loadFiles)

async function loadFiles() {
  loading.value = true
  notice.value = ''
  try {
    const result = await fetchAdminFiles({
      module: filterModule.value,
      page: page.value,
      pageSize: pageSize.value,
    })
    files.value = result.records
    total.value = result.total
    page.value = result.page
  } catch (error) {
    notice.value = toUserMessage(error, '文件资源加载失败')
  } finally {
    loading.value = false
  }
}

async function submitUpload() {
  notice.value = ''
  if (!selectedFile.value) {
    notice.value = '请选择文件'
    return
  }
  const maxBytes = appConfig.uploadMaxFileSizeMb * 1024 * 1024
  if (selectedFile.value.size > maxBytes) {
    notice.value = `文件不能超过 ${appConfig.uploadMaxFileSizeMb} MB`
    return
  }
  uploading.value = true
  try {
    await uploadAdminFile(selectedFile.value, uploadModule.value)
    selectedFile.value = null
    if (fileInput.value) {
      fileInput.value.value = ''
    }
    page.value = 1
    mode.value = 'library'
    notice.value = '文件已上传'
    await loadFiles()
  } catch (error) {
    notice.value = toUserMessage(error, '文件上传失败')
  } finally {
    uploading.value = false
  }
}

function selectFile(event: Event) {
  selectedFile.value = (event.target as HTMLInputElement).files?.[0] ?? null
}

function changeFilter() {
  page.value = 1
  loadFiles()
}

function changePageSize() {
  page.value = 1
  loadFiles()
}

function goPage(nextPage: number) {
  page.value = Math.min(Math.max(1, nextPage), totalPages.value)
  loadFiles()
}

async function copyUrl(url: string) {
  notice.value = ''
  try {
    await navigator.clipboard.writeText(url)
    notice.value = 'URL 已复制'
  } catch {
    fallbackCopy(url)
  }
}

async function removeFile(file: FileResource) {
  notice.value = ''
  const confirmed = window.confirm(`确认删除文件「${file.originalName}」吗？已被内容引用的文件不会被删除。`)
  if (!confirmed) {
    return
  }
  try {
    await deleteAdminFile(file.id)
    notice.value = '文件已删除'
    if (files.value.length === 1 && page.value > 1) {
      page.value -= 1
    }
    await loadFiles()
  } catch (error) {
    notice.value = toUserMessage(error, '文件删除失败')
  }
}

function fallbackCopy(value: string) {
  const textarea = document.createElement('textarea')
  textarea.value = value
  textarea.setAttribute('readonly', '')
  textarea.style.position = 'fixed'
  textarea.style.opacity = '0'
  document.body.appendChild(textarea)
  textarea.select()
  const copied = document.execCommand('copy')
  document.body.removeChild(textarea)
  notice.value = copied ? 'URL 已复制' : value
}

function isImage(file: FileResource) {
  return file.fileType.startsWith('image/')
}

function moduleLabel(value: string) {
  return moduleOptions.find((item) => item.value === value)?.label ?? value
}

function formatSize(value: number) {
  if (value < 1024) {
    return `${value} B`
  }
  if (value < 1024 * 1024) {
    return `${(value / 1024).toFixed(1)} KB`
  }
  return `${(value / 1024 / 1024).toFixed(1)} MB`
}
</script>

<style scoped>
.files-page {
  display: grid;
  gap: 18px;
}

.files-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.files-header h2,
.panel-title h3 {
  margin: 0;
}

.files-header h2 {
  color: var(--admin-primary-strong);
  font-size: 24px;
  font-weight: 780;
}

.files-header p {
  margin: 8px 0 0;
  color: var(--admin-muted);
  font-size: 14px;
}

.header-actions {
  display: inline-flex;
  flex-wrap: nowrap;
  gap: 10px;
}

.file-mode-tabs {
  display: inline-grid;
  justify-self: start;
  grid-template-columns: repeat(2, minmax(104px, 1fr));
  gap: 4px;
  padding: 4px;
  border: 1px solid var(--admin-line);
  border-radius: 999px;
  background: var(--admin-panel);
  box-shadow: var(--md-sys-elevation-1);
}

.file-mode-tab {
  min-height: 38px;
  padding: 0 16px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: var(--admin-muted);
  font: inherit;
  font-size: 13px;
  font-weight: 760;
  white-space: nowrap;
  cursor: pointer;
}

.file-mode-tab.is-active {
  background: var(--admin-primary-soft);
  color: var(--admin-primary-strong);
}

.files-panel {
  padding: 16px;
  border: 1px solid rgba(17, 24, 39, 0.04);
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 4px 20px -2px rgba(0, 0, 0, 0.04), 0 1px 3px rgba(0, 0, 0, 0.02);
}

.upload-panel,
.upload-panel label,
.library-panel {
  display: grid;
  gap: 12px;
}

.editor-heading,
.panel-title,
.filter-bar,
.pager {
  display: flex;
  align-items: center;
  gap: 12px;
}

.editor-heading,
.panel-title {
  justify-content: space-between;
}

.row-actions {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  min-width: 0;
}

.panel-title {
  margin-bottom: 8px;
}

.panel-title span,
.resource-main span,
.file-drop span,
.pager {
  color: var(--admin-muted);
  font-size: 13px;
}

.filter-bar {
  justify-content: flex-end;
}

.filter-bar > * {
  width: min(220px, 100%);
}

.file-drop {
  min-height: 148px;
  place-items: center;
  padding: 18px;
  border: 1px dashed color-mix(in srgb, var(--md-sys-color-primary) 42%, var(--admin-line));
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.62);
  color: var(--admin-ink);
  text-align: center;
  cursor: pointer;
}

.file-drop input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
}

.resource-list {
  display: grid;
  gap: 10px;
}

.resource-row {
  display: grid;
  grid-template-columns: 74px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid rgba(17, 24, 39, 0.06);
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
}

.resource-preview {
  display: grid;
  width: 74px;
  height: 58px;
  place-items: center;
  border-radius: 10px;
  background: var(--admin-primary-soft);
  color: var(--admin-muted);
  overflow: hidden;
}

.resource-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.resource-main {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.resource-main strong,
.resource-main code {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resource-main code {
  color: var(--admin-primary-strong);
  font-size: 12px;
}

.icon-button {
  display: inline-grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border: 1px solid var(--admin-line);
  border-radius: 999px;
  background: transparent;
  color: var(--admin-primary-strong);
  cursor: pointer;
}

.icon-button.danger {
  color: var(--admin-danger);
}

.empty-state {
  display: grid;
  min-height: 180px;
  place-items: center;
  color: var(--admin-muted);
  font-size: 14px;
}

.pager {
  justify-content: flex-end;
  padding-top: 12px;
}

.inline-notice {
  margin: 0;
  color: var(--md-sys-color-primary);
  font-size: 14px;
  font-weight: 760;
}

@media (max-width: 760px) {
  .files-header,
  .header-actions,
  .editor-heading,
  .panel-title,
  .filter-bar,
  .pager {
    align-items: flex-start;
    flex-direction: column;
  }

  .file-mode-tabs {
    justify-self: stretch;
    grid-template-columns: 1fr;
    border-radius: 20px;
  }

  .resource-row {
    grid-template-columns: 58px minmax(0, 1fr);
  }

  .resource-preview {
    width: 58px;
    height: 48px;
  }

  .row-actions {
    grid-column: 1 / -1;
    justify-content: flex-end;
  }
}
</style>
