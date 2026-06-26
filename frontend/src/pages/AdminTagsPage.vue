<template>
  <section class="cms-page">
    <header class="cms-header">
      <div>
        <h2>标签管理</h2>
        <p>维护文章、作品和灵感共用的内容标签。</p>
      </div>
      <button class="button button-tonal" type="button" @click="loadTags">刷新</button>
    </header>

    <section class="cms-grid">
      <form class="cms-panel form-panel" @submit.prevent="saveTag">
        <div class="panel-title">
          <h3>{{ editingId ? '编辑标签' : '新建标签' }}</h3>
          <span>{{ form.color || '无颜色' }}</span>
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

      <div class="cms-panel">
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
              <span>{{ tag.slug }} · 权重 {{ tag.weight }}</span>
            </div>
            <div class="row-actions">
              <button class="text-button" type="button" @click="editTag(tag)">编辑</button>
              <button class="text-button danger" type="button" @click="removeTag(tag)">删除</button>
            </div>
          </article>
          <p v-if="tags.length === 0" class="empty-hint">还没有标签。</p>
        </div>
      </div>
    </section>

    <p v-if="notice" class="inline-notice">{{ notice }}</p>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'

import { createTag, deleteTag, fetchTags, updateTag } from '@/services/content'
import { toUserMessage } from '@/services/http'
import type { TagPayload, TagSummary } from '@/shared/domain'

const tags = ref<TagSummary[]>([])
const editingId = ref<number | null>(null)
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
    }
    await loadTags()
    notice.value = '标签已删除'
  } catch (error) {
    notice.value = toUserMessage(error, '标签删除失败')
  }
}

function editTag(tag: TagSummary) {
  editingId.value = tag.id
  form.name = tag.name
  form.slug = tag.slug
  form.color = tag.color ?? ''
  form.weight = tag.weight
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

.form-line,
.form-actions,
.row-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.form-line > * {
  flex: 1;
  min-width: 140px;
}

.form-panel input {
  width: 100%;
  min-height: 42px;
  padding: 0 12px;
  border: 1px solid rgba(17, 24, 39, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.86);
  color: var(--tone-strong);
  font: inherit;
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

.tag-dot {
  display: inline-block;
  width: 12px;
  height: 12px;
  margin-right: 8px;
  border: 1px solid rgba(17, 24, 39, 0.14);
  border-radius: 50%;
  vertical-align: -1px;
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

.text-button.danger {
  color: #b91c1c;
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
