<template>
<!-- 创作者中心个人工作台 -->
<!-- 创作者个人控制台工作台 -->
  <section ref="root" class="creator-page">
    <header class="creator-hero page-hero" data-reveal>
      <div>
        <p class="page-kicker">Creator Desk</p>
        <h1>创作中心</h1>
      </div>
      <div class="creator-hero__stats">
        <span>{{ articles.length }} 篇文章</span>
        <span>{{ projects.length }} 个作品</span>
        <span>{{ files.length }} 张素材</span>
      </div>
    </header>

    <div class="creator-tabs" data-reveal>
      <RouterLink v-for="tab in tabs" :key="tab.to" :to="tab.to">
        <component :is="tab.icon" :size="16" />
        {{ tab.label }}
      </RouterLink>
    </div>

    <section v-if="activeSection === 'articles'" class="creator-grid">
      <form class="creator-panel creator-form" data-reveal @submit.prevent="saveArticle">
        <div class="panel-title">
          <h2>{{ editingArticleId ? '编辑文章' : '写一篇博客' }}</h2>
          <span>{{ articleForm.privacyType }}</span>
        </div>
        <div class="form-line">
          <label>
            标题
            <input v-model="articleForm.title" maxlength="200" />
          </label>
          <label>
            URL 标识
            <input v-model="articleForm.slug" maxlength="220" placeholder="my-first-post" />
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
              <option value="PUBLIC">公开</option>
              <option value="SELF">仅自己</option>
              <option value="FRIENDS">好友可见</option>
            </select>
          </label>
        </div>
        <label>
          摘要
          <textarea v-model="articleForm.summary" rows="3" maxlength="1200" />
        </label>
        <label>
          封面地址
          <input v-model="articleForm.coverUrl" placeholder="/uploads/article/2026/06/cover.webp" />
        </label>
        <label>
          Markdown 正文
          <textarea v-model="articleForm.contentMarkdown" rows="9" />
        </label>
        <div class="tag-picker-wrap">
          <label class="tag-picker-label">标签</label>
          <div class="tag-picker">
            <label v-for="tag in tags" :key="tag.id" class="check-line">
              <input v-model="articleForm.tagIds" type="checkbox" :value="tag.id" />
              {{ tag.name }}
            </label>
          </div>
          <div class="tag-creator">
            <input v-model="newTagName" placeholder="输入新标签" maxlength="50" @keydown.enter.prevent="createNewTag('article')" />
            <button type="button" class="button button-tonal button-compact" :disabled="!newTagName.trim()" @click="createNewTag('article')">添加</button>
          </div>
        </div>
        <div class="form-actions">
          <button class="button button-filled" type="submit">{{ editingArticleId ? '保存草稿' : '创建草稿' }}</button>
          <button v-if="editingArticleId" class="button button-tonal" type="button" @click="resetArticleForm">取消</button>
        </div>
      </form>

      <div class="creator-panel" data-reveal>
        <div class="panel-title">
          <h2>我的博客</h2>
          <button class="icon-text-button" type="button" @click="loadArticles">刷新</button>
        </div>
        <article v-for="article in articles" :key="article.id" class="desk-row">
          <div>
            <strong>{{ article.title }}</strong>
            <span>{{ statusLabel(article.status) }} · {{ article.slug }}</span>
            <small v-if="article.reviewNote">{{ article.reviewNote }}</small>
          </div>
          <div class="row-actions">
            <button class="icon-text-button" type="button" @click="editArticle(article)">编辑</button>
            <button
              v-if="canSubmitContent(article.status)"
              class="icon-text-button"
              type="button"
              @click="submitArticle(article.id)"
            >
              提交审核
            </button>
            <button
              v-if="canDeleteContent(article.status)"
              class="icon-text-button danger"
              type="button"
              @click="removeArticle(article.id)"
            >
              删除
            </button>
          </div>
        </article>
      </div>
    </section>

    <section v-else-if="activeSection === 'projects'" class="creator-grid">
      <form class="creator-panel creator-form" data-reveal @submit.prevent="saveProject">
        <div class="panel-title">
          <h2>{{ editingProjectId ? '编辑作品' : '上传创意作品' }}</h2>
          <span>{{ projectForm.projectType || 'PROJECT' }}</span>
        </div>
        <div class="form-line">
          <label>
            标题
            <input v-model="projectForm.title" maxlength="200" />
          </label>
          <label>
            URL 标识
            <input v-model="projectForm.slug" maxlength="220" placeholder="my-creative-work" />
          </label>
        </div>
        <div class="form-line">
          <label>
            类型
            <input v-model="projectForm.projectType" maxlength="60" placeholder="WEB_APP" />
          </label>
          <label>
            技术栈
            <input v-model="projectTechStack" placeholder="Vue 3, Spring Boot, Three.js" />
          </label>
        </div>
        <label>
          描述
          <textarea v-model="projectForm.description" rows="3" maxlength="2000" />
        </label>
        <label>
          封面地址
          <input v-model="projectForm.coverUrl" placeholder="/uploads/project/2026/06/cover.webp" />
        </label>
        <div class="form-line">
          <label>
            GitHub
            <input v-model="projectForm.githubUrl" placeholder="https://github.com/..." />
          </label>
          <label>
            Demo
            <input v-model="projectForm.demoUrl" placeholder="https://demo.example.com" />
          </label>
        </div>
        <label>
          Markdown 详情
          <textarea v-model="projectForm.contentMarkdown" rows="8" />
        </label>
        <div class="tag-picker-wrap">
          <label class="tag-picker-label">标签</label>
          <div class="tag-picker">
            <label v-for="tag in tags" :key="tag.id" class="check-line">
              <input v-model="projectForm.tagIds" type="checkbox" :value="tag.id" />
              {{ tag.name }}
            </label>
          </div>
          <div class="tag-creator">
            <input v-model="newTagName" placeholder="输入新标签" maxlength="50" @keydown.enter.prevent="createNewTag('project')" />
            <button type="button" class="button button-tonal button-compact" :disabled="!newTagName.trim()" @click="createNewTag('project')">添加</button>
          </div>
        </div>
        <div class="form-actions">
          <button class="button button-filled" type="submit">{{ editingProjectId ? '保存作品' : '创建作品' }}</button>
          <button v-if="editingProjectId" class="button button-tonal" type="button" @click="resetProjectForm">取消</button>
        </div>
      </form>

      <div class="creator-panel" data-reveal>
        <div class="panel-title">
          <h2>我的作品</h2>
          <button class="icon-text-button" type="button" @click="loadProjects">刷新</button>
        </div>
        <article v-for="project in projects" :key="project.id" class="desk-row">
          <div>
            <strong>{{ project.title }}</strong>
            <span>{{ statusLabel(project.status) }} · {{ project.projectType }}</span>
            <small v-if="project.reviewNote">{{ project.reviewNote }}</small>
          </div>
          <div class="row-actions">
            <button class="icon-text-button" type="button" @click="editProject(project)">编辑</button>
            <button
              v-if="canSubmitContent(project.status)"
              class="icon-text-button"
              type="button"
              @click="submitProject(project.id)"
            >
              提交审核
            </button>
            <button
              v-if="canDeleteContent(project.status)"
              class="icon-text-button danger"
              type="button"
              @click="removeProject(project.id)"
            >
              删除
            </button>
          </div>
        </article>
      </div>
    </section>

    <section v-else-if="activeSection === 'files'" class="creator-grid">
      <form class="creator-panel creator-form" data-reveal @submit.prevent="uploadFile">
        <div class="panel-title">
          <h2>上传图片素材</h2>
          <span>{{ fileModule }}</span>
        </div>
        <label>
          模块
          <select v-model="fileModule">
            <option value="ARTICLE">ARTICLE</option>
            <option value="PROJECT">PROJECT</option>
            <option value="COVER">COVER</option>
            <option value="AVATAR">AVATAR</option>
            <option value="INSPIRATION">INSPIRATION</option>
            <option value="OTHER">OTHER</option>
          </select>
        </label>
        <input type="file" accept="image/*" @change="selectFile" />
        <button class="button button-filled" type="submit">上传图片</button>
      </form>

      <div class="creator-panel" data-reveal>
        <div class="panel-title">
          <h2>我的素材</h2>
          <button class="icon-text-button" type="button" @click="loadFiles">刷新</button>
        </div>
        <article v-for="file in files" :key="file.id" class="desk-row">
          <div>
            <strong>{{ file.originalName }}</strong>
            <span>{{ file.module }} · {{ formatSize(file.fileSize) }}</span>
            <small>{{ file.publicUrl }}</small>
          </div>
          <a class="icon-text-button" :href="file.publicUrl" target="_blank" rel="noreferrer">预览</a>
        </article>
      </div>
    </section>

    <section v-else class="creator-panel creator-favorites" data-reveal>
      <div class="panel-title">
        <h2>我的收藏</h2>
        <button class="icon-text-button" type="button" @click="loadFavorites">刷新</button>
      </div>
      <article v-for="favorite in favorites" :key="favorite.id" class="desk-row">
        <div>
          <strong>{{ favorite.targetType }} #{{ favorite.targetId }}</strong>
          <span>{{ formatDateTimeToSecond(favorite.createdAt, '刚刚') }}</span>
        </div>
      </article>
      <p v-if="favorites.length === 0" class="muted-line">还没有收藏内容。</p>
    </section>

    <p v-if="notice" class="inline-notice">{{ notice }}</p>
  </section>
</template>

<script setup lang="ts">
// 导入 Composition API 与路由依赖
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { BookOpen, FileImage, Images, Star } from '@lucide/vue'

import {
  createCreatorArticle,
  createCreatorProject,
  deleteCreatorArticle,
  deleteCreatorProject,
  fetchCategories,
  fetchCreatorArticle,
  fetchCreatorArticles,
  fetchCreatorFiles,
  fetchCreatorProject,
  fetchCreatorProjects,
  fetchMyFavorites,
  fetchTags,
  createTag,
  submitCreatorArticle,
  submitCreatorProject,
  updateCreatorArticle,
  updateCreatorProject,
  uploadCreatorFile,
} from '@/services/content'
import { toUserMessage } from '@/services/http'
import { usePageReveal } from '@/shared/composables/usePageReveal'
import { formatDateTimeToSecond } from '@/shared/datetime'
import type {
  ArticlePayload,
  ArticleSummary,
  CategorySummary,
  FileResource,
  InteractionRecord,
  ProjectPayload,
  ProjectSummary,
  TagSummary,
} from '@/shared/domain'

// 初始化创作者工作台的响应式状态数据
const root = ref<HTMLElement | null>(null)
const route = useRoute()
const notice = ref('')
const articles = ref<ArticleSummary[]>([])
const projects = ref<ProjectSummary[]>([])
const files = ref<FileResource[]>([])
const favorites = ref<InteractionRecord[]>([])
const tags = ref<TagSummary[]>([])
const articleCategories = ref<CategorySummary[]>([])
const editingArticleId = ref<number | null>(null)
const editingProjectId = ref<number | null>(null)
const projectTechStack = ref('')
const selectedFile = ref<File | null>(null)
const fileModule = ref('PROJECT')
const newTagName = ref('')

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

const projectForm = reactive<ProjectPayload>({
  title: '',
  slug: '',
  description: '',
  coverUrl: '',
  projectType: 'WEB_APP',
  techStack: [],
  githubUrl: '',
  demoUrl: '',
  videoUrl: '',
  contentMarkdown: '',
  tagIds: [],
  recommended: false,
})

const tabs = [
  { to: '/creator/articles', label: '文章', icon: BookOpen },
  { to: '/creator/projects', label: '作品', icon: Images },
  { to: '/creator/files', label: '素材', icon: FileImage },
  { to: '/creator/favorites', label: '收藏', icon: Star },
]

const activeSection = computed(() => {
  const section = typeof route.params.section === 'string' ? route.params.section : 'articles'
  return ['articles', 'projects', 'files', 'favorites'].includes(section) ? section : 'articles'
})

usePageReveal(root)

onMounted(async () => {
  await Promise.all([loadBaseData(), loadArticles(), loadProjects(), loadFiles(), loadFavorites()])
})

// 异步加载后台分类列表和全部标签数据, 并进行多字段映射绑定
async function loadBaseData() {
  try {
    const [articleCategoryList, tagList] = await Promise.all([fetchCategories('ARTICLE'), fetchTags()])
    articleCategories.value = articleCategoryList
    tags.value = tagList
  } catch (error) {
    notice.value = readError(error, '基础数据加载失败')
  }
}

// 异步加载当前登录创作者本人的文章列表队列, 每次最多拉取前 50 条数据
async function loadArticles() {
  try {
    articles.value = (await fetchCreatorArticles({ pageSize: 50 })).records
  } catch (error) {
    notice.value = readError(error, '文章队列加载失败')
  }
}

// 异步加载当前登录创作者本人的创意作品列表, 限制一次最多返回 50 条
async function loadProjects() {
  try {
    projects.value = (await fetchCreatorProjects({ pageSize: 50 })).records
  } catch (error) {
    notice.value = readError(error, '作品队列加载失败')
  }
}

// 异步加载当前创作者已上传的文件资源素材队列, 便于插入文章或作为封面
async function loadFiles() {
  try {
    files.value = (await fetchCreatorFiles({ pageSize: 50 })).records
  } catch (error) {
    notice.value = readError(error, '素材列表加载失败')
  }
}

// 异步拉取当前登录读者的收藏历史记录列表, 发生异常时静默降级为空数组
async function loadFavorites() {
  try {
    favorites.value = (await fetchMyFavorites()).records
  } catch {
    favorites.value = []
  }
}

// 保存当前正在编辑的文章草稿, 根据是否带有编辑 ID 决定是发起 PUT 还是 POST 请求
async function saveArticle() {
  if (!articleForm.title.trim() || !articleForm.slug.trim() || !articleForm.contentMarkdown.trim()) {
    notice.value = '请填写文章标题、URL 标识和正文'
    return
  }
  try {
    if (editingArticleId.value) {
      // 执行 PUT 操作更新已有草稿
      await updateCreatorArticle(editingArticleId.value, { ...articleForm })
      notice.value = '文章草稿已保存'
    } else {
      // 执行 POST 操作创建全新草稿
      await createCreatorArticle({ ...articleForm })
      notice.value = '文章草稿已创建'
    }
    resetArticleForm()
    await loadArticles()
  } catch (error) {
    notice.value = readError(error, '文章保存失败')
  }
}

// 获取创作者指定草稿的完整正文和配置参数, 并将反序列化后的字段绑定回输入表单
async function editArticle(article: ArticleSummary) {
  try {
    const detail = await fetchCreatorArticle(article.id)
    editingArticleId.value = detail.id
    articleForm.title = detail.title
    articleForm.slug = detail.slug
    articleForm.summary = detail.summary ?? ''
    articleForm.contentMarkdown = detail.contentMarkdown ?? ''
    articleForm.coverUrl = detail.coverUrl ?? ''
    articleForm.categoryId = detail.category?.id ?? null
    articleForm.tagIds = detail.tags.map((tag) => tag.id)
    articleForm.privacyType = detail.privacyType
  } catch (error) {
    notice.value = readError(error, '文章详情读取失败')
  }
}

// 提交创作者草稿申请审核, 改变草稿状态为 PENDING_REVIEW 待管理员批复
async function submitArticle(id: number) {
  try {
    await submitCreatorArticle(id)
    notice.value = '文章已提交审核'
    await loadArticles()
  } catch (error) {
    notice.value = readError(error, '文章提交失败')
  }
}

// 创作者物理删除自己未公开的草稿或被驳回的记录, 并重置当前表单输入框
async function removeArticle(id: number) {
  try {
    await deleteCreatorArticle(id)
    notice.value = '文章已删除'
    resetArticleForm()
    await loadArticles()
  } catch (error) {
    notice.value = readError(error, '文章删除失败')
  }
}

// 清理表单响应式状态字段, 重置表单为新建的默认参数
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

async function saveProject() {
  if (!projectForm.title.trim() || !projectForm.slug.trim() || !projectForm.projectType.trim()) {
    notice.value = '请填写作品标题、URL 标识和类型'
    return
  }
  try {
    projectForm.techStack = splitTechStack(projectTechStack.value)
    projectForm.recommended = false
    if (editingProjectId.value) {
      await updateCreatorProject(editingProjectId.value, { ...projectForm })
      notice.value = '作品草稿已保存'
    } else {
      await createCreatorProject({ ...projectForm })
      notice.value = '作品草稿已创建'
    }
    resetProjectForm()
    await loadProjects()
  } catch (error) {
    notice.value = readError(error, '作品保存失败')
  }
}

async function editProject(project: ProjectSummary) {
  try {
    const detail = await fetchCreatorProject(project.id)
    editingProjectId.value = detail.id
    projectForm.title = detail.title
    projectForm.slug = detail.slug
    projectForm.description = detail.description ?? ''
    projectForm.coverUrl = detail.coverUrl ?? ''
    projectForm.projectType = detail.projectType
    projectForm.techStack = detail.techStack
    projectForm.githubUrl = detail.githubUrl ?? ''
    projectForm.demoUrl = detail.demoUrl ?? ''
    projectForm.videoUrl = detail.videoUrl ?? ''
    projectForm.contentMarkdown = detail.contentMarkdown ?? ''
    projectForm.tagIds = detail.tags.map((tag) => tag.id)
    projectForm.recommended = false
    projectTechStack.value = detail.techStack.join(', ')
  } catch (error) {
    notice.value = readError(error, '作品详情读取失败')
  }
}

async function submitProject(id: number) {
  try {
    await submitCreatorProject(id)
    notice.value = '作品已提交审核'
    await loadProjects()
  } catch (error) {
    notice.value = readError(error, '作品提交失败')
  }
}

async function removeProject(id: number) {
  try {
    await deleteCreatorProject(id)
    notice.value = '作品已删除'
    resetProjectForm()
    await loadProjects()
  } catch (error) {
    notice.value = readError(error, '作品删除失败')
  }
}

function resetProjectForm() {
  editingProjectId.value = null
  projectForm.title = ''
  projectForm.slug = ''
  projectForm.description = ''
  projectForm.coverUrl = ''
  projectForm.projectType = 'WEB_APP'
  projectForm.techStack = []
  projectForm.githubUrl = ''
  projectForm.demoUrl = ''
  projectForm.videoUrl = ''
  projectForm.contentMarkdown = ''
  projectForm.tagIds = []
  projectForm.recommended = false
  projectTechStack.value = ''
}

function selectFile(event: Event) {
  selectedFile.value = (event.target as HTMLInputElement).files?.[0] ?? null
}

async function uploadFile() {
  if (!selectedFile.value) {
    notice.value = '请选择图片'
    return
  }
  try {
    const file = await uploadCreatorFile(selectedFile.value, fileModule.value)
    selectedFile.value = null
    notice.value = `图片已上传：${file.publicUrl}`
    await loadFiles()
  } catch (error) {
    notice.value = readError(error, '图片上传失败')
  }
}

async function createNewTag(target: 'article' | 'project') {
  if (!newTagName.value.trim()) return
  try {
    const tag = await createTag({
      name: newTagName.value.trim(),
      slug: newTagName.value.trim().toLowerCase().replace(/\s+/g, '-'),
      groupName: '用户创建'
    })
    tags.value.push(tag)
    if (target === 'article') {
      articleForm.tagIds.push(tag.id)
    } else {
      projectForm.tagIds.push(tag.id)
    }
    newTagName.value = ''
    notice.value = '新标签已创建'
  } catch (error) {
    notice.value = readError(error, '标签创建失败')
  }
}

function canSubmitContent(status: string) {
  return status === 'DRAFT' || status === 'REJECTED'
}

function canDeleteContent(status: string) {
  return status === 'DRAFT' || status === 'REJECTED' || status === 'PENDING_REVIEW'
}

function statusLabel(status: string) {
  const labels: Record<string, string> = {
    DRAFT: '草稿',
    PENDING_REVIEW: '待审核',
    PUBLISHED: '已公开',
    PRIVATE: '私密',
    VISIBLE: '已展示',
    HIDDEN: '已隐藏',
    REJECTED: '已驳回',
    ARCHIVED: '已归档',
  }
  return labels[status] ?? status
}

function splitTechStack(value: string) {
  return value
    .split(/[,，\n]/)
    .map((item) => item.trim())
    .filter(Boolean)
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

function readError(error: unknown, fallback: string) {
  return `${fallback}: ${toUserMessage(error, '请稍后再试')}`
}
</script>

<style scoped>
.creator-page {
  display: grid;
  gap: 18px;
  padding: 46px 0 84px;
}

.creator-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: end;
  gap: 22px;
  min-height: clamp(220px, 23vw, 292px);
  padding: clamp(24px, 3vw, 42px);
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.88), rgba(255, 255, 255, 0.58)),
    radial-gradient(circle at 16% 18%, rgba(49, 91, 255, 0.18), transparent 34%),
    radial-gradient(circle at 86% 22%, rgba(0, 124, 114, 0.18), transparent 30%),
    repeating-linear-gradient(90deg, rgba(20, 21, 29, 0.04) 0 1px, transparent 1px 42px);
  box-shadow: var(--tone-shadow);
  overflow: hidden;
}

.creator-hero h1 {
  margin: 0;
  color: var(--tone-ink);
  font-size: 44px;
  line-height: 1.06;
}

.creator-hero__stats {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.creator-hero__stats span,
.creator-tabs a,
.desk-row small,
.muted-line {
  color: var(--tone-muted);
  font-size: 13px;
}

.creator-hero__stats span,
.creator-tabs a {
  min-height: 36px;
  padding: 9px 12px;
  border: 1px solid var(--tone-line);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.68);
  font-weight: 760;
}

.creator-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.creator-tabs a {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.creator-tabs a.router-link-active,
.creator-tabs a:hover {
  border-color: rgba(49, 91, 255, 0.34);
  background: rgba(49, 91, 255, 0.1);
  color: var(--tone-ink);
}

.creator-grid {
  display: grid;
  grid-template-columns: 1fr;
  align-items: start;
  gap: 16px;
}

.creator-panel {
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm);
  background: rgba(255, 255, 255, 0.78);
  box-shadow: var(--tone-shadow);
  backdrop-filter: blur(20px);
}

.creator-form,
.creator-favorites,
.creator-panel:not(.creator-form) {
  padding: 20px;
}

.creator-form {
  display: grid;
  align-content: start;
  gap: 14px;
}

.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.panel-title h2 {
  margin: 0;
  font-size: 20px;
}

.panel-title span,
.desk-row span {
  color: var(--tone-muted);
  font-size: 13px;
}

.creator-form label {
  display: grid;
  gap: 8px;
  color: var(--tone-muted);
  font-size: 13px;
  font-weight: 760;
}

.creator-form input,
.creator-form select,
.creator-form textarea {
  width: 100%;
  border: 1px solid rgba(17, 24, 39, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.9);
  color: var(--tone-strong);
  font: inherit;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    background 0.18s ease;
}

.creator-form input,
.creator-form select {
  min-height: 42px;
  padding: 0 12px;
}

.creator-form input:not([type="checkbox"]):not([type="radio"]):hover,
.creator-form select:hover,
.creator-form textarea:hover {
  border-color: rgba(49, 91, 255, 0.28);
  background: #fff;
}

.creator-form input:not([type="checkbox"]):not([type="radio"]):focus,
.creator-form select:focus,
.creator-form textarea:focus {
  border-color: rgba(49, 91, 255, 0.48);
  outline: none;
  box-shadow: 0 0 0 4px rgba(49, 91, 255, 0.1);
}

.tag-picker-wrap {
  display: grid;
  gap: 8px;
}

.tag-creator {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-top: 4px;
}

.tag-creator input {
  flex: 1;
}

.creator-form select {
  appearance: none;
  padding-right: 38px;
  background-image:
    linear-gradient(45deg, transparent 50%, #315bff 50%),
    linear-gradient(135deg, #315bff 50%, transparent 50%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(246, 248, 255, 0.96));
  background-position:
    calc(100% - 18px) 50%,
    calc(100% - 13px) 50%,
    0 0;
  background-size:
    5px 5px,
    5px 5px,
    100% 100%;
  background-repeat: no-repeat;
  cursor: pointer;
}

.creator-form select option {
  background: #fff;
  color: var(--tone-strong);
  font-size: 14px;
}

.creator-form input[type="file"] {
  min-height: 48px;
  padding: 6px 10px;
  border-style: dashed;
  border-color: rgba(49, 91, 255, 0.24);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(246, 248, 255, 0.88));
  color: var(--tone-muted);
  cursor: pointer;
}

.creator-form input[type="file"]::file-selector-button {
  min-height: 34px;
  margin-right: 12px;
  padding: 0 14px;
  border: 0;
  border-radius: 999px;
  background: rgba(49, 91, 255, 0.12);
  color: #2448d8;
  font: inherit;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
  transition:
    background 0.18s ease,
    color 0.18s ease,
    transform 0.18s ease;
}

.creator-form input[type="file"]:hover::file-selector-button {
  background: #315bff;
  color: #fff;
  transform: translateY(-1px);
}

.creator-form textarea {
  resize: vertical;
  padding: 12px;
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
  min-width: 180px;
}

.tag-picker {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-picker .check-line {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 34px;
  padding: 6px 10px;
  border: 1px solid var(--tone-line);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.58);
}

.tag-picker input {
  width: 16px;
  min-height: 16px;
}

.desk-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 14px;
  border-radius: 8px;
  background: rgba(20, 21, 29, 0.04);
}

.desk-row + .desk-row {
  margin-top: 10px;
}

.desk-row div {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.icon-text-button {
  border: 0;
  background: transparent;
  color: #315bff;
  font: inherit;
  font-size: 13px;
  font-weight: 800;
  text-decoration: none;
  cursor: pointer;
}

.icon-text-button.danger {
  color: #b91c1c;
}

@media (max-width: 1020px) {
  .creator-hero,
  .creator-grid {
    grid-template-columns: 1fr;
  }

  .creator-hero__stats {
    justify-content: flex-start;
  }
}

@media (max-width: 760px) {
  .creator-page {
    padding-top: 26px;
  }

  .creator-hero {
    padding: 22px;
  }

  .creator-hero h1 {
    font-size: 32px;
  }

  .desk-row,
  .panel-title {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
