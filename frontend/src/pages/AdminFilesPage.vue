<template>
  <section class="files-page">
    <header class="files-header">
      <div>
        <h2>文件资源库</h2>
        <p>管理后台上传的封面、插图、附件和站点资源。</p>
      </div>
      <button class="button button-tonal" type="button" :disabled="loading" @click="loadFiles">
        <RefreshCw :size="16" />
        刷新
      </button>
    </header>

    <section class="files-grid">
      <form class="files-panel upload-panel" @submit.prevent="submitUpload">
        <div class="panel-title">
          <h3>上传资源</h3>
          <span>{{ uploadModuleLabel }}</span>
        </div>

        <label>
          归属模块
          <select v-model="uploadModule">
            <option v-for="module in uploadModules" :key="module.value" :value="module.value">
              {{ module.label }}
            </option>
          </select>
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

      <section class="files-panel library-panel">
        <div class="panel-title">
          <h3>资源列表</h3>
          <span>共 {{ total }} 个</span>
        </div>

        <div class="filter-bar">
          <select v-model="filterModule" @change="changeFilter">
            <option v-for="module in filterModules" :key="module.value" :value="module.value">
              {{ module.label }}
            </option>
          </select>
          <select v-model.number="pageSize" @change="changePageSize">
            <option :value="12">12 / 页</option>
            <option :value="24">24 / 页</option>
            <option :value="48">48 / 页</option>
          </select>
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
              <span>{{ moduleLabel(file.module) }} · {{ formatSize(file.fileSize) }} · {{ file.fileType }}</span>
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
import type { FileResource } from '../shared/domain'

type FileModule = 'AVATAR' | 'COVER' | 'ARTICLE' | 'PROJECT' | 'INSPIRATION' | 'OTHER'

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
const acceptedTypes = '.webp,.png,.jpg,.jpeg,.gif,.pdf,.txt,.md,.markdown'

const files = ref<FileResource[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(12)
const filterModule = ref<(typeof filterModules)[number]['value']>('ALL')
const uploadModule = ref<FileModule>('OTHER')
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

.files-header,
.files-panel {
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm);
  background: var(--tone-panel);
  box-shadow: var(--tone-shadow);
}

.files-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 22px 24px;
}

.files-header h2,
.panel-title h3 {
  margin: 0;
}

.files-header p {
  margin: 8px 0 0;
  color: var(--tone-muted);
  font-size: 14px;
}

.files-grid {
  display: grid;
  grid-template-columns: minmax(280px, 0.42fr) minmax(0, 1fr);
  gap: 14px;
}

.files-panel {
  padding: 16px;
}

.upload-panel,
.upload-panel label,
.library-panel {
  display: grid;
  gap: 12px;
}

.panel-title,
.filter-bar,
.pager,
.row-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.panel-title {
  justify-content: space-between;
  margin-bottom: 8px;
}

.panel-title span,
.resource-main span,
.file-drop span,
.pager {
  color: var(--tone-muted);
  font-size: 13px;
}

.upload-panel select,
.filter-bar select {
  min-height: 42px;
  border: 1px solid rgba(17, 24, 39, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.86);
  color: var(--tone-strong);
  font: inherit;
}

.upload-panel select {
  width: 100%;
  padding: 0 12px;
}

.filter-bar {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.filter-bar select {
  padding: 0 10px;
}

.file-drop {
  min-height: 148px;
  place-items: center;
  padding: 18px;
  border: 1px dashed color-mix(in srgb, var(--md-sys-color-primary) 42%, var(--tone-line));
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.62);
  color: var(--tone-strong);
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
  padding: 10px;
  border-radius: 8px;
  background: rgba(20, 21, 29, 0.04);
}

.resource-preview {
  display: grid;
  width: 74px;
  height: 58px;
  place-items: center;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.74);
  color: var(--tone-muted);
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
  color: #315bff;
  font-size: 12px;
}

.icon-button {
  display: inline-grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border: 1px solid var(--tone-line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.7);
  color: #315bff;
  cursor: pointer;
}

.icon-button.danger {
  color: #b91c1c;
}

.empty-state {
  display: grid;
  min-height: 180px;
  place-items: center;
  color: var(--tone-muted);
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

@media (max-width: 1020px) {
  .files-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .files-header,
  .panel-title,
  .filter-bar,
  .pager {
    align-items: flex-start;
    flex-direction: column;
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
