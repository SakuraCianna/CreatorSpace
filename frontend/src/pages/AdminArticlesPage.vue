<template>
  <section class="cms-page article-admin-page">
    <header class="cms-header">
      <div>
        <h2>文章管理</h2>
        <p>草稿、发布、置顶、推荐、分类、标签、封面和 Markdown 正文都在这里处理。</p>
      </div>
      <div class="header-actions">
        <button class="button button-tonal" type="button" :disabled="loading" @click="refreshAll">
          <RefreshCw :size="16" />
          刷新
        </button>
        <button class="button button-filled" type="button" @click="resetArticleForm">
          <Plus :size="16" />
          新建文章
        </button>
      </div>
    </header>

    <section class="cms-grid article-grid">
      <form class="cms-panel form-panel" @submit.prevent="saveArticle">
        <div class="panel-title">
          <h3>{{ editingArticleId ? '编辑文章' : '新建草稿' }}</h3>
          <span>{{ privacyLabel(articleForm.privacyType) }}</span>
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
            <select v-model="articleForm.categoryId">
              <option :value="null">不绑定分类</option>
              <option v-for="category in articleCategories" :key="category.id" :value="category.id">
                {{ category.name }}
              </option>
            </select>
          </label>
          <label>
            可见性
            <select v-model="articleForm.privacyType">
              <option v-for="privacy in articlePrivacies" :key="privacy" :value="privacy">
                {{ privacyLabel(privacy) }}
              </option>
            </select>
          </label>
        </div>

        <label>
          摘要
          <textarea v-model="articleForm.summary" rows="3" maxlength="1200" />
        </label>

        <label>
          封面地址
          <input v-model="articleForm.coverUrl" placeholder="/uploads/article/cover.png" />
        </label>

        <label>
          Markdown 正文
          <textarea v-model="articleForm.contentMarkdown" rows="12" />
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

      <div class="article-stack">
        <div class="cms-panel">
          <div class="panel-title">
            <h3>筛选文章</h3>
            <span>共 {{ total }} 篇</span>
          </div>
          <div class="filter-bar">
            <input v-model="keyword" placeholder="搜索标题 / 摘要 / 正文" @keyup.enter="loadArticles" />
            <select v-model="statusFilter" @change="loadArticles">
              <option value="ALL">全部状态</option>
              <option value="DRAFT">草稿</option>
              <option value="PENDING_REVIEW">待审核</option>
              <option value="PUBLISHED">已发布</option>
              <option value="PRIVATE">私密</option>
              <option value="REJECTED">已驳回</option>
              <option value="ARCHIVED">已归档</option>
            </select>
          </div>
        </div>

        <div class="cms-panel">
          <div class="panel-title">
            <h3>文章列表</h3>
            <span>第 {{ page }} / {{ totalPages }} 页</span>
          </div>
          <div class="list-stack">
            <article v-for="article in articles" :key="article.id" class="table-row table-row--rich">
              <div>
                <strong>{{ article.title }}</strong>
                <span>
                  {{ article.category?.name ?? '未分类' }} - {{ privacyLabel(article.privacyType) }} - {{ article.slug }}
                </span>
              </div>
              <div class="row-actions">
                <span class="status-chip">{{ contentStatusLabel(article.status) }}</span>
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
      </div>
    </section>

    <p v-if="notice" class="inline-notice">{{ notice }}</p>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { Plus, RefreshCw } from '@lucide/vue'

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
} from '@/services/content'
import { toUserMessage } from '@/services/http'
import type { ArticlePayload, ArticlePrivacy, ArticleSummary, CategorySummary, TagSummary } from '@/shared/domain'

const articlePrivacies: ArticlePrivacy[] = ['PUBLIC', 'SELF', 'FRIENDS', 'SELECTED_FRIENDS', 'EXCLUDED_FRIENDS']

const articles = ref<ArticleSummary[]>([])
const articleCategories = ref<CategorySummary[]>([])
const tags = ref<TagSummary[]>([])
const editingArticleId = ref<number | null>(null)
const keyword = ref('')
const statusFilter = ref<'ALL' | ArticleSummary['status']>('ALL')
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
    await loadArticle(article.id)
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
    await loadArticle(article.id)
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
    await loadArticle(article.id)
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
    }
    await loadArticles()
  } catch (error) {
    notice.value = toUserMessage(error, '文章删除失败')
  }
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

.article-grid {
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.15fr);
}

.article-stack {
  display: grid;
  gap: 14px;
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

.filter-bar {
  flex-wrap: wrap;
}

.pager {
  justify-content: flex-end;
  padding-top: 12px;
}

.article-admin-page .table-row > .row-actions {
  display: grid;
  justify-items: center;
  align-content: start;
  gap: 10px;
  min-width: 96px;
}

.article-admin-page .row-actions .text-button {
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

@media (max-width: 1020px) {
  .cms-header,
  .article-grid {
    grid-template-columns: 1fr;
  }

  .cms-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
