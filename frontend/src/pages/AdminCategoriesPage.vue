<template>
  <section class="cms-page">
    <AdminPageHeader title="分类管理" description="维护文章、作品和灵感模块的内容分类。" theme="orange">
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
    </AdminPageHeader>

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
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'

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


.module-tabs button {
  min-height: 36px;
  padding: 0 14px;
  border: 1px solid var(--admin-line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.72);
  color: #344154;
  font: inherit;
  font-weight: 760;
  cursor: pointer;
}

.module-tabs button.active,
.module-tabs button:hover {
  border-color: rgba(49, 91, 255, 0.36);
  background: var(--admin-primary-soft);
  color: var(--admin-primary-strong);
}

.cms-grid {
  display: grid;
  grid-template-columns: minmax(300px, 0.7fr) minmax(0, 1fr);
  align-items: start;
  gap: 14px;
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

@media (max-width: 1020px) {
  .cms-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .cms-grid {
    gap: 12px;
  }
}
</style>
