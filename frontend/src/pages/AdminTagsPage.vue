<template>
  <section class="cms-page tag-admin-page">
    <AdminPageHeader title="标签管理" description="维护文章、作品和灵感共用的内容标签。" theme="cyan">
      <div class="header-actions">
        <button class="button button-tonal" type="button" @click="loadTags">刷新</button>
        <button class="button button-filled" type="button" @click="startCreate">新建标签</button>
      </div>
    </AdminPageHeader>

    <form v-if="mode === 'form'" class="cms-panel form-panel tag-form" @submit.prevent="saveTag">
      <div class="editor-heading">
        <div class="panel-title">
          <h3>{{ editingId ? '编辑标签' : '新建标签' }}</h3>
          <span>{{ form.color || '无颜色' }}</span>
        </div>
        <button class="button button-tonal button-compact" type="button" @click="showList">返回列表</button>
      </div>
      <label>
        名称
        <input v-model="form.name" maxlength="80" />
      </label>
      <label>
        URL 标识
        <input v-model="form.slug" maxlength="120" placeholder="vue" />
      </label>
      <div class="form-line">
        <label>
          颜色
          <input v-model="form.color" maxlength="32" placeholder="#315bff" />
        </label>
        <label>
          权重
          <input v-model.number="form.weight" type="number" />
        </label>
      </div>
      <div class="form-actions">
        <button class="button button-filled" type="submit">{{ editingId ? '保存标签' : '创建标签' }}</button>
        <button class="button button-tonal" type="button" @click="resetForm">重置</button>
      </div>
    </form>

    <div v-else class="cms-panel">
      <div class="panel-title">
        <h3>标签列表</h3>
        <span>共 {{ tags.length }} 个</span>
      </div>
      <div class="list-stack">
        <article v-for="tag in tags" :key="tag.id" class="table-row">
          <div>
            <strong>
              <span class="tag-dot" :style="{ backgroundColor: tag.color || '#94a3b8' }" />
              {{ tag.name }}
            </strong>
            <span>{{ tag.slug }} - 权重 {{ tag.weight }}</span>
          </div>
          <div class="row-actions">
            <button class="text-button" type="button" @click="editTag(tag)">编辑</button>
            <button class="text-button danger" type="button" @click="removeTag(tag)">删除</button>
          </div>
        </article>
        <p v-if="tags.length === 0" class="empty-hint">还没有标签。</p>
      </div>
    </div>

    <p v-if="notice" class="inline-notice">{{ notice }}</p>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import AdminPageHeader from '../components/admin/AdminPageHeader.vue'

import { createTag, deleteTag, fetchTags, updateTag } from '../services/content'
import { toUserMessage } from '../services/http'
import type { TagPayload, TagSummary } from '../shared/domain'

type ViewMode = 'list' | 'form'

const tags = ref<TagSummary[]>([])
const editingId = ref<number | null>(null)
const mode = ref<ViewMode>('list')
const notice = ref('')
const form = reactive<TagPayload>({
  name: '',
  slug: '',
  color: '',
  weight: 0,
})

onMounted(loadTags)

async function loadTags() {
  notice.value = ''
  try {
    tags.value = await fetchTags()
  } catch (error) {
    notice.value = toUserMessage(error, '标签列表加载失败')
  }
}

async function saveTag() {
  notice.value = ''
  const payload = normalizePayload()
  if (!payload) {
    return
  }
  try {
    if (editingId.value) {
      await updateTag(editingId.value, payload)
      notice.value = '标签已保存'
    } else {
      await createTag(payload)
      notice.value = '标签已创建'
    }
    resetForm()
    mode.value = 'list'
    await loadTags()
  } catch (error) {
    notice.value = toUserMessage(error, '标签保存失败')
  }
}

async function removeTag(tag: TagSummary) {
  notice.value = ''
  try {
    await deleteTag(tag.id)
    if (editingId.value === tag.id) {
      resetForm()
      mode.value = 'list'
    }
    await loadTags()
    notice.value = '标签已删除'
  } catch (error) {
    notice.value = toUserMessage(error, '标签删除失败')
  }
}

function startCreate() {
  resetForm()
  mode.value = 'form'
}

function showList() {
  mode.value = 'list'
}

function editTag(tag: TagSummary) {
  editingId.value = tag.id
  form.name = tag.name
  form.slug = tag.slug
  form.color = tag.color ?? ''
  form.weight = tag.weight
  mode.value = 'form'
}

function resetForm() {
  editingId.value = null
  form.name = ''
  form.slug = ''
  form.color = ''
  form.weight = 0
}

function normalizePayload(): TagPayload | null {
  const name = form.name.trim()
  const slug = form.slug.trim().toLowerCase()
  const color = form.color?.trim()
  if (!name || !slug) {
    notice.value = '标签名称和 URL 标识不能为空'
    return null
  }
  return {
    name,
    slug,
    color: color || null,
    weight: Number.isFinite(Number(form.weight)) ? Number(form.weight) : 0,
  }
}
</script>

<style scoped>
.tag-admin-page {
  display: grid;
  gap: 18px;
}

.header-actions,
.editor-heading,
.row-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-actions,
.row-actions {
  flex-wrap: nowrap;
}

.editor-heading {
  justify-content: space-between;
}

.editor-heading .panel-title {
  margin-bottom: 0;
}

.tag-form {
  display: grid;
  gap: 12px;
}

.tag-dot {
  display: inline-block;
  width: 12px;
  height: 12px;
  margin-right: 8px;
  border: 1px solid rgba(17, 24, 39, 0.14);
  border-radius: 50%;
  vertical-align: -1px;
}

@media (max-width: 760px) {
  .header-actions,
  .editor-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .row-actions {
    justify-content: flex-start;
  }
}
</style>
