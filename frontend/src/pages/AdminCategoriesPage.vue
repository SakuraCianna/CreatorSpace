<template>
  <section class="cms-page">
    <header class="cms-header">
      <div>
        <h2>分类管理</h2>
        <p>维护文章、作品和灵感模块的内容分类。</p>
      </div>
      <div class="module-tabs" role="tablist" aria-label="分类模块">
        <button
          v-for="item in moduleOptions"
          :key="item.value"
          type="button"
          :class="{ active: activeModule === item.value }"
          @click="switchModule(item.value)"
        >
          {{ item.label }}
        </button>
      </div>
    </header>

    <section class="cms-grid">
      <form class="cms-panel form-panel" @submit.prevent="saveCategory">
        <div class="panel-title">
          <h3>{{ editingId ? '编辑分类' : '新建分类' }}</h3>
          <span>{{ moduleLabel(form.module) }}</span>
        </div>
        <label>
          名称
          <input v-model="form.name" maxlength="80" />
        </label>
        <label>
          URL 标识
          <input v-model="form.slug" maxlength="120" placeholder="design-notes" />
        </label>
        <label>
          描述
          <textarea v-model="form.description" rows="4" maxlength="500" />
        </label>
        <div class="form-line">
          <label>
            排序
            <input v-model.number="form.sortOrder" type="number" />
          </label>
          <label class="check-line">
            <input v-model="form.enabled" type="checkbox" />
            启用
          </label>
        </div>
        <div class="form-actions">
          <button class="button button-filled" type="submit">{{ editingId ? '保存分类' : '创建分类' }}</button>
          <button class="button button-tonal" type="button" @click="resetForm">重置</button>
        </div>
      </form>

      <div class="cms-panel">
        <div class="panel-title">
          <h3>{{ moduleLabel(activeModule) }}分类</h3>
          <span>共 {{ categories.length }} 个</span>
        </div>
        <div class="list-stack">
          <article v-for="category in categories" :key="category.id" class="table-row">
            <div>
              <strong>{{ category.name }}</strong>
              <span>{{ category.slug }} · 排序 {{ category.sortOrder }}</span>
            </div>
            <div class="row-actions">
              <span class="status-chip" :class="{ muted: !category.enabled }">
                {{ category.enabled ? '启用' : '停用' }}
              </span>
              <button class="text-button" type="button" @click="editCategory(category)">编辑</button>
              <button class="text-button" type="button" @click="toggleEnabled(category)">
                {{ category.enabled ? '停用' : '启用' }}
              </button>
            </div>
          </article>
          <p v-if="categories.length === 0" class="empty-hint">当前模块还没有分类。</p>
        </div>
      </div>
    </section>

    <p v-if="notice" class="inline-notice">{{ notice }}</p>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'

import {
  createCategory,
  fetchAdminCategories,
  setCategoryEnabled,
  updateCategory,
} from '@/services/content'
import { toUserMessage } from '@/services/http'
import type { CategoryPayload, CategorySummary } from '@/shared/domain'

const moduleOptions: Array<{ value: CategorySummary['module']; label: string }> = [
  { value: 'ARTICLE', label: '文章' },
  { value: 'PROJECT', label: '作品' },
  { value: 'INSPIRATION', label: '灵感' },
]

const activeModule = ref<CategorySummary['module']>('ARTICLE')
const categories = ref<CategorySummary[]>([])
const editingId = ref<number | null>(null)
const notice = ref('')
const form = reactive<CategoryPayload>({
  module: 'ARTICLE',
  name: '',
  slug: '',
  description: '',
  sortOrder: 0,
  enabled: true,
})

onMounted(loadCategories)

async function loadCategories() {
  notice.value = ''
  try {
    categories.value = await fetchAdminCategories(activeModule.value)
  } catch (error) {
    notice.value = toUserMessage(error, '分类列表加载失败')
  }
}

async function saveCategory() {
  notice.value = ''
  const payload = normalizePayload()
  if (!payload) {
    return
  }
  try {
    if (editingId.value) {
      await updateCategory(editingId.value, payload)
      notice.value = '分类已保存'
    } else {
      await createCategory(payload)
      notice.value = '分类已创建'
    }
    resetForm()
    await loadCategories()
  } catch (error) {
    notice.value = toUserMessage(error, '分类保存失败')
  }
}

async function toggleEnabled(category: CategorySummary) {
  notice.value = ''
  try {
    await setCategoryEnabled(category.id, !category.enabled)
    await loadCategories()
  } catch (error) {
    notice.value = toUserMessage(error, '分类状态更新失败')
  }
}

function switchModule(module: CategorySummary['module']) {
  activeModule.value = module
  resetForm()
  loadCategories()
}

function editCategory(category: CategorySummary) {
  editingId.value = category.id
  form.module = category.module
  form.name = category.name
  form.slug = category.slug
  form.description = category.description ?? ''
  form.sortOrder = category.sortOrder
  form.enabled = category.enabled ?? true
}

function resetForm() {
  editingId.value = null
  form.module = activeModule.value
  form.name = ''
  form.slug = ''
  form.description = ''
  form.sortOrder = 0
  form.enabled = true
}

function normalizePayload(): CategoryPayload | null {
  const name = form.name.trim()
  const slug = form.slug.trim().toLowerCase()
  if (!name || !slug) {
    notice.value = '分类名称和 URL 标识不能为空'
    return null
  }
  return {
    module: form.module,
    name,
    slug,
    description: form.description?.trim() || null,
    sortOrder: Number.isFinite(Number(form.sortOrder)) ? Number(form.sortOrder) : 0,
    enabled: Boolean(form.enabled),
  }
}

function moduleLabel(module: CategorySummary['module']) {
  return moduleOptions.find((item) => item.value === module)?.label ?? module
}
</script>

<style scoped>
.cms-page {
  display: grid;
  gap: 18px;
}

.cms-header,
.cms-panel {
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm);
  background: var(--tone-panel);
  box-shadow: var(--tone-shadow);
}

.cms-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 22px 24px;
}

.cms-header h2,
.panel-title h3 {
  margin: 0;
}

.cms-header p {
  margin: 8px 0 0;
  color: var(--tone-muted);
  font-size: 14px;
}

.module-tabs,
.form-actions,
.row-actions,
.form-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.module-tabs button {
  min-height: 36px;
  padding: 0 14px;
  border: 1px solid var(--tone-line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.72);
  color: var(--tone-muted);
  font: inherit;
  font-weight: 760;
  cursor: pointer;
}

.module-tabs button.active {
  border-color: color-mix(in srgb, var(--md-sys-color-primary) 34%, transparent);
  background: color-mix(in srgb, var(--md-sys-color-primary) 12%, transparent);
  color: var(--md-sys-color-primary);
}

.cms-grid {
  display: grid;
  grid-template-columns: minmax(300px, 0.7fr) minmax(0, 1fr);
  gap: 14px;
}

.cms-panel {
  padding: 16px;
}

.form-panel,
.list-stack,
.form-panel label {
  display: grid;
  gap: 12px;
}

.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.panel-title span,
.table-row span,
.empty-hint {
  color: var(--tone-muted);
  font-size: 13px;
}

.form-panel input,
.form-panel textarea {
  width: 100%;
  border: 1px solid rgba(17, 24, 39, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.86);
  color: var(--tone-strong);
  font: inherit;
}

.form-panel input {
  min-height: 42px;
  padding: 0 12px;
}

.form-panel textarea {
  resize: vertical;
  padding: 12px;
}

.form-line > * {
  flex: 1;
  min-width: 140px;
}

.check-line {
  display: flex !important;
  align-items: center;
  gap: 10px;
}

.check-line input {
  width: 18px;
  min-height: 18px;
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

.table-row div {
  display: grid;
  gap: 4px;
  min-width: 0;
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

.status-chip.muted {
  background: rgba(100, 116, 139, 0.12);
  color: #475569;
}

.text-button {
  border: 0;
  background: transparent;
  color: #315bff;
  font: inherit;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.inline-notice {
  margin: 0;
  padding: 12px 14px;
  border-left: 3px solid var(--tone-coral);
  background: rgba(194, 95, 58, 0.08);
  color: #754226;
  font-size: 13px;
}

@media (max-width: 1020px) {
  .cms-header,
  .cms-grid {
    grid-template-columns: 1fr;
  }

  .cms-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
