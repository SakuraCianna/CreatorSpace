<template>
  <section class="cms-page article-admin-page">
    <header class="cms-header">
      <div>
        <h2>文章管理</h2>
        <p>先筛选和处理文章列表，需要写作或修改时再进入编辑工作区。</p>
      </div>
      <div class="header-actions">
        <button class="button button-tonal" type="button" :disabled="loading" @click="refreshAll">
          <RefreshCw :size="16" />
          刷新
        </button>
        <button class="button button-filled" type="button" @click="openNewArticle">
          <Plus :size="16" />
          新建文章
        </button>
      </div>
    </header>

    <section class="article-mode-tabs" aria-label="文章管理模式">
      <button
        class="article-mode-tab"
        :class="{ 'is-active': articleMode === 'list' }"
        type="button"
        @click="showArticleList"
      >
        列表与筛选
      </button>
      <button
        class="article-mode-tab"
        :class="{ 'is-active': articleMode === 'editor' }"
        type="button"
        @click="openEditor"
      >
        {{ editingArticleId ? '编辑文章' : '写作工作区' }}
      </button>
    </section>

    <form v-if="articleMode === 'editor'" class="cms-panel form-panel article-editor-panel" @submit.prevent="saveArticle">
      <div class="editor-heading">
        <div class="panel-title">
          <h3>{{ editingArticleId ? '编辑文章' : '新建草稿' }}</h3>
          <span>{{ privacyLabel(articleForm.privacyType) }}</span>
        </div>
        <button class="button button-tonal button-compact" type="button" @click="showArticleList">
          返回列表
        </button>
      </div>

      <div class="form-line">
        <label>
          标题
          <input v-model="articleForm.title" maxlength="200" />
        </label>
        <label>
          URL 标识
          <input v-model="articleForm.slug" maxlength="220" />
        </label>
      </div>

      <div class="form-line">
        <label>
          分类
          <BaseSelect v-model="articleForm.categoryId" :options="categoryOptions" />
        </label>
        <label>
          可见性
          <BaseSelect v-model="articleForm.privacyType" :options="privacyOptions" />
        </label>
      </div>

      <label>
        摘要
        <textarea v-model="articleForm.summary" rows="3" maxlength="1200" />
      </label>

      <label>
        封面图片
        <FileUpload v-model="articleForm.coverUrl" module="COVER" accept="image/*" hint="最大 10MB" @error="handleUploadError" />
      </label>

      <label>
        Markdown 正文
        <MarkdownEditor v-model="articleForm.contentMarkdown" :rows="16" />
      </label>

      <div class="tag-picker">
        <label v-for="tag in tags" :key="tag.id" class="check-line">
          <input v-model="articleForm.tagIds" type="checkbox" :value="tag.id" />
          {{ tag.name }}
        </label>
      </div>

      <div class="form-actions">
        <button class="button button-filled" type="submit">{{ editingArticleId ? '保存文章' : '保存草稿' }}</button>
        <button v-if="editingArticleId" class="button button-tonal" type="button" @click="cancelEditing">取消编辑</button>
      </div>
    </form>

    <section v-else class="article-list-workspace">
      <div class="cms-panel article-filter-panel">
        <div class="panel-title">
          <h3>筛选文章</h3>
          <span>共 {{ total }} 篇</span>
        </div>
        <div class="filter-bar article-filter-bar">
          <input v-model="keyword" placeholder="搜索标题 / 摘要 / 正文" @keyup.enter="loadArticles" />
          <BaseSelect v-model="statusFilter" :options="statusOptions" @change="loadArticles" />
        </div>
      </div>

      <div class="cms-panel">
        <div class="panel-title">
          <h3>文章列表</h3>
          <span>第 {{ page }} / {{ totalPages }} 页</span>
        </div>
        <div class="list-stack article-list">
          <article v-for="article in articles" :key="article.id" class="table-row table-row--rich article-row">
            <div class="article-row__content">
              <strong>{{ article.title }}</strong>
              <span>
                {{ article.category?.name ?? '未分类' }} - {{ privacyLabel(article.privacyType) }} - {{ article.slug }}
              </span>
            </div>
            <div class="article-row__meta">
              <span class="status-chip">{{ contentStatusLabel(article.status) }}</span>
            </div>
            <div class="row-actions">
              <button class="text-button" type="button" @click="loadArticle(article.id)">编辑</button>
              <RouterLink class="text-button" :to="{ name: 'admin-article-versions', params: { id: article.id } }">版本</RouterLink>
              <button class="text-button" type="button" @click="togglePublish(article)">{{ articlePublishActionLabel(article) }}</button>
              <button class="text-button" type="button" @click="toggleTop(article)">{{ article.top ? '取消置顶' : '置顶' }}</button>
              <button class="text-button" type="button" @click="toggleRecommend(article)">{{ article.recommended ? '取消推荐' : '推荐' }}</button>
              <button class="text-button danger" type="button" @click="removeArticle(article.id)">删除</button>
            </div>
          </article>
          <p v-if="articles.length === 0 && !loading" class="empty-hint">暂无文章。</p>
        </div>
        <div class="pager">
          <button class="button button-tonal" type="button" :disabled="page <= 1 || loading" @click="changePage(page - 1)">上一页</button>
          <button class="button button-tonal" type="button" :disabled="page >= totalPages || loading" @click="changePage(page + 1)">下一页</button>
        </div>
      </div>
    </section>

    <p v-if="notice" class="inline-notice">{{ notice }}</p>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { Plus, RefreshCw } from '@lucide/vue'

import FileUpload from '../components/common/FileUpload.vue'
import BaseSelect from '../shared/components/BaseSelect.vue'
import MarkdownEditor from '../shared/components/MarkdownEditor.vue'
import {
  changeArticlePublishState,
  createArticle,
  deleteArticle,
  fetchAdminArticle,
  fetchAdminArticles,
  fetchCategories,
  fetchTags,
  setArticleRecommend,
  setArticleTop,
  updateArticle,
} from '../services/content'
import { toUserMessage } from '../services/http'
import type { ArticlePayload, ArticlePrivacy, ArticleSummary, CategorySummary, TagSummary } from '../shared/domain'

type ArticleStatusFilter = 'ALL' | ArticleSummary['status']
type ArticleMode = 'list' | 'editor'

const articlePrivacies: ArticlePrivacy[] = ['PUBLIC', 'SELF', 'FRIENDS', 'SELECTED_FRIENDS', 'EXCLUDED_FRIENDS']

const articles = ref<ArticleSummary[]>([])
const articleCategories = ref<CategorySummary[]>([])
const tags = ref<TagSummary[]>([])
const editingArticleId = ref<number | null>(null)
const keyword = ref('')
const statusFilter = ref<ArticleStatusFilter>('ALL')
const articleMode = ref<ArticleMode>('list')
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const loading = ref(false)
const notice = ref('')

const articleForm = reactive<ArticlePayload>({
  title: '',
  slug: '',
  summary: '',
  contentMarkdown: '',
  coverUrl: '',
  categoryId: null,
  tagIds: [],
  privacyType: 'PUBLIC',
})

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))
const categoryOptions = computed(() => [
  { label: '不绑定分类', value: null },
  ...articleCategories.value.map((category) => ({ label: category.name, value: category.id })),
])
const privacyOptions = computed(() => articlePrivacies.map((privacy) => ({ label: privacyLabel(privacy), value: privacy })))
const statusOptions = [
  { label: '全部状态', value: 'ALL' },
  { label: '草稿', value: 'DRAFT' },
  { label: '待审核', value: 'PENDING_REVIEW' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '私密', value: 'PRIVATE' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '已归档', value: 'ARCHIVED' },
] as const

onMounted(async () => {
  await Promise.all([ensureTaxonomies(), loadArticles()])
})

async function ensureTaxonomies() {
  if (articleCategories.value.length === 0) {
    articleCategories.value = await fetchCategories('ARTICLE')
  }
  if (tags.value.length === 0) {
    tags.value = await fetchTags()
  }
}

async function refreshAll() {
  page.value = 1
  await loadArticles()
}

async function loadArticles() {
  loading.value = true
  notice.value = ''
  try {
    await ensureTaxonomies()
    const result = await fetchAdminArticles({
      keyword: keyword.value,
      status: statusFilter.value,
      page: page.value,
      pageSize: pageSize.value,
    })
    articles.value = result.records
    total.value = result.total
    page.value = result.page
  } catch (error) {
    notice.value = toUserMessage(error, '文章列表加载失败')
  } finally {
    loading.value = false
  }
}

async function loadArticle(id: number) {
  notice.value = ''
  try {
    const article = await fetchAdminArticle(id)
    editingArticleId.value = article.id
    articleForm.title = article.title
    articleForm.slug = article.slug
    articleForm.summary = article.summary ?? ''
    articleForm.contentMarkdown = article.contentMarkdown ?? ''
    articleForm.coverUrl = article.coverUrl ?? ''
    articleForm.categoryId = article.category?.id ?? null
    articleForm.tagIds = article.tags.map((tag) => tag.id)
    articleForm.privacyType = article.privacyType
    articleMode.value = 'editor'
  } catch (error) {
    notice.value = toUserMessage(error, '文章读取失败')
  }
}

async function saveArticle() {
  notice.value = ''
  if (!articleForm.title.trim() || !articleForm.slug.trim() || !articleForm.contentMarkdown.trim()) {
    notice.value = '请填写标题、URL 标识和 Markdown 正文'
    return
  }
  try {
    if (editingArticleId.value) {
      await updateArticle(editingArticleId.value, { ...articleForm })
      notice.value = '文章已保存'
    } else {
      const created = await createArticle({ ...articleForm })
      editingArticleId.value = created.id
      notice.value = '草稿已创建'
    }
    await loadArticles()
    if (editingArticleId.value) {
      await loadArticle(editingArticleId.value)
    }
  } catch (error) {
    notice.value = toUserMessage(error, '文章保存失败')
  }
}

async function togglePublish(article: ArticleSummary) {
  notice.value = ''
  try {
    const action = article.status === 'PUBLISHED' || article.status === 'PRIVATE' ? 'unpublish' : 'publish'
    await changeArticlePublishState(article.id, action)
    notice.value = action === 'publish' ? '文章已发布' : '文章已撤回'
    await loadArticles()
    if (editingArticleId.value === article.id) {
      await loadArticle(article.id)
    }
  } catch (error) {
    notice.value = toUserMessage(error, '文章状态更新失败')
  }
}

async function toggleTop(article: ArticleSummary) {
  notice.value = ''
  try {
    await setArticleTop(article.id, !article.top)
    notice.value = article.top ? '已取消置顶' : '文章已置顶'
    await loadArticles()
    if (editingArticleId.value === article.id) {
      await loadArticle(article.id)
    }
  } catch (error) {
    notice.value = toUserMessage(error, '置顶状态更新失败')
  }
}

async function toggleRecommend(article: ArticleSummary) {
  notice.value = ''
  try {
    await setArticleRecommend(article.id, !article.recommended)
    notice.value = article.recommended ? '已取消推荐' : '文章已推荐'
    await loadArticles()
    if (editingArticleId.value === article.id) {
      await loadArticle(article.id)
    }
  } catch (error) {
    notice.value = toUserMessage(error, '推荐状态更新失败')
  }
}

async function removeArticle(id: number) {
  notice.value = ''
  if (!window.confirm('确认删除这篇文章吗？')) {
    return
  }
  try {
    await deleteArticle(id)
    notice.value = '文章已删除'
    if (editingArticleId.value === id) {
      resetArticleForm()
      articleMode.value = 'list'
    }
    await loadArticles()
  } catch (error) {
    notice.value = toUserMessage(error, '文章删除失败')
  }
}

function openNewArticle() {
  resetArticleForm()
  articleMode.value = 'editor'
}

function openEditor() {
  articleMode.value = 'editor'
}

function showArticleList() {
  articleMode.value = 'list'
}

function handleUploadError(message: string) {
  notice.value = message
}

function resetArticleForm() {
  editingArticleId.value = null
  articleForm.title = ''
  articleForm.slug = ''
  articleForm.summary = ''
  articleForm.contentMarkdown = ''
  articleForm.coverUrl = ''
  articleForm.categoryId = null
  articleForm.tagIds = []
  articleForm.privacyType = 'PUBLIC'
}

function cancelEditing() {
  resetArticleForm()
  articleMode.value = 'list'
}

function changePage(nextPage: number) {
  page.value = nextPage
  loadArticles()
}

function articlePublishActionLabel(article: ArticleSummary) {
  return article.status === 'PUBLISHED' || article.status === 'PRIVATE' ? '撤回' : '发布'
}

function privacyLabel(value: string) {
  return {
    PUBLIC: '公开',
    SELF: '仅自己',
    FRIENDS: '好友可见',
    SELECTED_FRIENDS: '指定好友',
    EXCLUDED_FRIENDS: '排除好友',
  }[value] ?? value
}

function contentStatusLabel(value: string) {
  return {
    DRAFT: '草稿',
    PENDING_REVIEW: '待审核',
    PUBLISHED: '已公开',
    PRIVATE: '私密',
    REJECTED: '已驳回',
    SCHEDULED: '定时发布',
    ARCHIVED: '已归档',
  }[value] ?? value
}
</script>

<style scoped>
.article-admin-page {
  display: grid;
  gap: 18px;
}

.cms-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 0;
}

.cms-header p {
  margin: 8px 0 0;
  color: var(--admin-muted);
  font-size: 14px;
}

.header-actions {
  display: inline-flex;
  flex-wrap: nowrap;
  gap: 10px;
}

.article-mode-tabs {
  display: inline-grid;
  justify-self: start;
  grid-template-columns: repeat(2, minmax(116px, 1fr));
  gap: 4px;
  padding: 4px;
  border: 1px solid var(--admin-line);
  border-radius: 999px;
  background: var(--admin-panel);
  box-shadow: var(--md-sys-elevation-1);
}

.article-mode-tab {
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
  transition:
    background 180ms ease,
    color 180ms ease,
    box-shadow 180ms ease;
}

.article-mode-tab.is-active {
  background: var(--admin-primary-soft);
  color: var(--admin-primary-strong);
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--admin-primary) 10%, transparent);
}

.article-list-workspace,
.article-editor-panel {
  display: grid;
  gap: 14px;
}

.article-filter-panel {
  padding-bottom: 14px;
}

.article-filter-bar {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) minmax(180px, 240px);
}

.editor-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.editor-heading .panel-title {
  margin-bottom: 0;
}

.article-list {
  gap: 10px;
}

.article-row {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) minmax(92px, auto) minmax(420px, auto);
  align-items: center;
  gap: 16px;
}

.article-row__content {
  min-width: 0;
}

.article-row__meta {
  display: flex;
  justify-content: flex-start;
}

.tag-picker {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-picker .check-line {
  min-height: 34px;
  padding: 6px 10px;
  border: 1px solid var(--admin-line);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: #344154;
  font-size: 12px;
  font-weight: 760;
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

.filter-bar,
.row-actions,
.pager {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pager {
  justify-content: flex-end;
  padding-top: 12px;
}

.article-admin-page .table-row > .row-actions {
  display: grid;
  grid-template-columns: repeat(6, minmax(64px, auto));
  justify-content: end;
  gap: 6px;
  min-width: 0;
}

.article-admin-page .row-actions .text-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 64px;
  min-height: 30px;
  box-sizing: border-box;
  line-height: 1;
  text-align: center;
  text-decoration: none;
  white-space: nowrap;
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

@media (max-width: 1280px) {
  .article-row {
    grid-template-columns: minmax(0, 1fr);
  }

  .article-row__meta,
  .article-admin-page .table-row > .row-actions {
    justify-content: start;
  }
}

@media (max-width: 1020px) {
  .cms-header {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 760px) {
  .header-actions,
  .editor-heading,
  .article-admin-page .table-row > .row-actions {
    width: 100%;
  }

  .header-actions,
  .editor-heading {
    flex-direction: column;
  }

  .article-mode-tabs,
  .article-filter-bar {
    grid-template-columns: 1fr;
  }

  .article-admin-page .table-row > .row-actions {
    overflow-x: auto;
    justify-content: flex-start;
  }

  .article-mode-tabs {
    justify-self: stretch;
    border-radius: 20px;
  }
}
</style>
