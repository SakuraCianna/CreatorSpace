<template>
  <section ref="root" class="admin-module">
    <header class="module-hero" data-reveal>
      <div>
        <p class="eyebrow">{{ moduleConfig.eyebrow }}</p>
        <h2>{{ moduleConfig.title }}</h2>
        <p>{{ moduleConfig.description }}</p>
      </div>
      <button class="button button-filled" type="button" @click="handlePrimaryAction">
        <Plus :size="16" />
        {{ moduleConfig.primaryAction }}
      </button>
    </header>

    <section v-if="activeSection === 'articles'" class="workspace-grid">
      <form class="workspace-panel admin-form" data-reveal @submit.prevent="saveArticle">
        <div class="panel-title">
          <h2>{{ editingArticleId ? '编辑文章' : '新建文章' }}</h2>
          <span>{{ articleForm.privacyType }}</span>
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
              <option v-for="privacy in articlePrivacies" :key="privacy" :value="privacy">{{ privacy }}</option>
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
          <textarea v-model="articleForm.contentMarkdown" rows="8" />
        </label>
        <div class="tag-picker">
          <label v-for="tag in tags" :key="tag.id" class="check-line">
            <input v-model="articleForm.tagIds" type="checkbox" :value="tag.id" />
            {{ tag.name }}
          </label>
        </div>
        <div class="form-actions">
          <button class="button button-filled" type="submit">{{ editingArticleId ? '保存文章' : '创建草稿' }}</button>
          <button v-if="editingArticleId" class="button button-tonal" type="button" @click="resetArticleForm">取消编辑</button>
        </div>
      </form>

      <div class="workspace-panel" data-reveal>
        <div class="panel-title">
          <h2>文章队列</h2>
          <span>{{ articles.length }} items</span>
        </div>
        <div class="filter-bar">
          <input v-model="articleKeyword" placeholder="搜索文章" @keyup.enter="loadArticles" />
          <select v-model="articleStatus" @change="loadArticles">
            <option value="ALL">全部状态</option>
            <option value="DRAFT">草稿</option>
            <option value="PUBLISHED">公开</option>
            <option value="PRIVATE">私密</option>
            <option value="ARCHIVED">归档</option>
          </select>
          <button class="icon-text-button" type="button" @click="loadArticles">刷新</button>
        </div>
        <article v-for="article in articles" :key="article.id" class="table-row table-row--rich">
          <div>
            <strong>{{ article.title }}</strong>
            <span>
              {{ article.category?.name ?? '未分类' }} · {{ article.privacyType }} · {{ article.slug }}
            </span>
          </div>
          <div class="row-actions">
            <span class="status-chip">{{ article.status }}</span>
            <button class="icon-text-button" type="button" @click="editArticle(article.id)">编辑</button>
            <button class="icon-text-button" type="button" @click="toggleArticlePublish(article)">
              {{ article.status === 'DRAFT' ? '发布' : '撤回' }}
            </button>
            <button class="icon-text-button" type="button" @click="toggleArticleTop(article)">
              {{ article.top ? '取消置顶' : '置顶' }}
            </button>
            <button class="icon-text-button" type="button" @click="toggleArticleRecommend(article)">
              {{ article.recommended ? '取消推荐' : '推荐' }}
            </button>
            <button class="icon-text-button danger" type="button" @click="removeArticle(article.id)">删除</button>
          </div>
        </article>
      </div>
    </section>

    <section v-else-if="activeSection === 'projects'" class="workspace-grid">
      <form class="workspace-panel admin-form" data-reveal @submit.prevent="saveProject">
        <div class="panel-title">
          <h2>{{ editingProjectId ? '编辑作品' : '新增作品' }}</h2>
          <span>{{ projectForm.projectType || 'PROJECT' }}</span>
        </div>
        <div class="form-line">
          <label>
            标题
            <input v-model="projectForm.title" maxlength="200" />
          </label>
          <label>
            URL 标识
            <input v-model="projectForm.slug" maxlength="220" />
          </label>
        </div>
        <div class="form-line">
          <label>
            类型
            <input v-model="projectForm.projectType" maxlength="60" placeholder="WEB_APP" />
          </label>
          <label>
            技术栈
            <input v-model="projectTechStack" placeholder="Vue 3, Spring Boot" />
          </label>
        </div>
        <label>
          描述
          <textarea v-model="projectForm.description" rows="3" maxlength="2000" />
        </label>
        <label>
          封面地址
          <input v-model="projectForm.coverUrl" placeholder="/uploads/project/cover.png" />
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
        <div class="tag-picker">
          <label v-for="tag in tags" :key="tag.id" class="check-line">
            <input v-model="projectForm.tagIds" type="checkbox" :value="tag.id" />
            {{ tag.name }}
          </label>
        </div>
        <label class="check-line">
          <input v-model="projectForm.recommended" type="checkbox" />
          推荐展示
        </label>
        <div class="form-actions">
          <button class="button button-filled" type="submit">{{ editingProjectId ? '保存作品' : '创建作品' }}</button>
          <button v-if="editingProjectId" class="button button-tonal" type="button" @click="resetProjectForm">取消编辑</button>
        </div>
      </form>

      <div class="workspace-panel" data-reveal>
        <div class="panel-title">
          <h2>作品队列</h2>
          <span>{{ projects.length }} items</span>
        </div>
        <div class="filter-bar">
          <input v-model="projectKeyword" placeholder="搜索作品" @keyup.enter="loadProjects" />
          <select v-model="projectStatus" @change="loadProjects">
            <option value="ALL">全部状态</option>
            <option value="VISIBLE">展示</option>
            <option value="HIDDEN">隐藏</option>
            <option value="DRAFT">草稿</option>
            <option value="ARCHIVED">归档</option>
          </select>
          <button class="icon-text-button" type="button" @click="loadProjects">刷新</button>
        </div>
        <article v-for="project in projects" :key="project.id" class="table-row table-row--rich">
          <div>
            <strong>{{ project.title }}</strong>
            <span>{{ project.projectType }} · {{ project.techStack.join(' / ') || '未维护技术栈' }} · {{ project.slug }}</span>
          </div>
          <div class="row-actions">
            <span class="status-chip">{{ project.status }}</span>
            <button class="icon-text-button" type="button" @click="editProject(project.id)">编辑</button>
            <button class="icon-text-button" type="button" @click="toggleProjectVisible(project)">
              {{ project.status === 'VISIBLE' ? '隐藏' : '展示' }}
            </button>
            <button class="icon-text-button" type="button" @click="toggleProjectRecommend(project)">
              {{ project.recommended ? '取消推荐' : '推荐' }}
            </button>
            <button class="icon-text-button danger" type="button" @click="removeProject(project.id)">删除</button>
          </div>
        </article>
      </div>
    </section>

    <section v-else-if="activeSection === 'inspirations'" class="workspace-grid">
      <form class="workspace-panel admin-form" data-reveal @submit.prevent="saveInspiration">
        <div class="panel-title">
          <h2>{{ editingInspirationId ? '编辑灵感卡片' : '添加灵感卡片' }}</h2>
          <span>{{ inspirationForm.isPublic ? 'PUBLIC' : 'PRIVATE' }}</span>
        </div>
        <label>
          标题
          <input v-model="inspirationForm.title" maxlength="200" />
        </label>
        <label>
          类型
          <select v-model="inspirationForm.cardType">
            <option v-for="type in inspirationTypes" :key="type" :value="type">{{ type }}</option>
          </select>
        </label>
        <label>
          内容
          <textarea v-model="inspirationForm.content" rows="5" maxlength="5000" />
        </label>
        <label>
          来源链接
          <input v-model="inspirationForm.sourceUrl" placeholder="https://example.com/reference" />
        </label>
        <div class="form-line">
          <label>
            色彩
            <input v-model="inspirationForm.color" placeholder="#6ea8ff" />
          </label>
          <label>
            排序
            <input v-model.number="inspirationForm.sortOrder" type="number" />
          </label>
        </div>
        <label class="check-line">
          <input v-model="inspirationForm.isPublic" type="checkbox" />
          公开展示
        </label>
        <div class="form-actions">
          <button class="button button-filled" type="submit">{{ editingInspirationId ? '保存修改' : '创建卡片' }}</button>
          <button v-if="editingInspirationId" class="button button-tonal" type="button" @click="resetInspirationForm">取消编辑</button>
        </div>
      </form>

      <div class="workspace-panel" data-reveal>
        <div class="panel-title">
          <h2>灵感队列</h2>
          <span>{{ inspirations.length }} items</span>
        </div>
        <article v-for="card in inspirations" :key="card.id" class="table-row table-row--rich">
          <div>
            <strong>{{ card.title }}</strong>
            <span>{{ card.cardType }} · {{ card.isPublic ? '公开' : '私密' }} · {{ card.createdAt ?? '未记录时间' }}</span>
          </div>
          <div class="row-actions">
            <button class="icon-text-button" type="button" @click="editInspiration(card)">编辑</button>
            <button class="icon-text-button danger" type="button" @click="removeInspiration(card.id)">删除</button>
          </div>
        </article>
      </div>
    </section>

    <section v-else-if="activeSection === 'comments'" class="workspace-grid">
      <div class="workspace-panel" data-reveal>
        <div class="panel-title">
          <h2>审核队列</h2>
          <span>{{ comments.length }} items</span>
        </div>
        <div class="filter-bar">
          <select v-model="commentStatus" @change="loadComments">
            <option value="ALL">全部状态</option>
            <option value="PENDING">待审核</option>
            <option value="APPROVED">已通过</option>
            <option value="REJECTED">已驳回</option>
            <option value="SPAM">垃圾评论</option>
          </select>
          <select v-model="commentTargetType" @change="loadComments">
            <option value="ALL">全部目标</option>
            <option value="ARTICLE">文章</option>
            <option value="PROJECT">作品</option>
            <option value="MESSAGE">留言</option>
          </select>
        </div>
        <article v-for="comment in comments" :key="comment.id" class="table-row table-row--rich">
          <div>
            <strong>{{ comment.username }}: {{ comment.content }}</strong>
            <span>{{ comment.targetType }} #{{ comment.targetId }} · {{ comment.createdAt ?? '未记录时间' }}</span>
          </div>
          <div class="row-actions">
            <span class="status-chip">{{ comment.status }}</span>
            <button class="icon-text-button" type="button" @click="review(comment.id, 'approve')">通过</button>
            <button class="icon-text-button danger" type="button" @click="review(comment.id, 'reject')">驳回</button>
          </div>
        </article>
      </div>

      <aside class="workspace-panel" data-reveal>
        <div class="panel-title">
          <h2>审核原则</h2>
          <span>Policy</span>
        </div>
        <ul class="rule-list">
          <li>公开评论只展示 APPROVED 状态</li>
          <li>敏感词 REJECT 会在提交时直接拒绝</li>
          <li>回复必须挂在已通过评论下，避免噪音扩散</li>
        </ul>
      </aside>
    </section>

    <section v-else-if="activeSection === 'files'" class="workspace-grid">
      <form class="workspace-panel admin-form" data-reveal @submit.prevent="uploadFile">
        <div class="panel-title">
          <h2>上传资源</h2>
          <span>LOCAL</span>
        </div>
        <label>
          模块
          <select v-model="fileModule">
            <option v-for="module in fileModules" :key="module" :value="module">{{ module }}</option>
          </select>
        </label>
        <input type="file" @change="selectFile" />
        <button class="button button-filled" type="submit">上传文件</button>
      </form>

      <div class="workspace-panel" data-reveal>
        <div class="panel-title">
          <h2>资源列表</h2>
          <span>{{ files.length }} files</span>
        </div>
        <article v-for="file in files" :key="file.id" class="table-row table-row--rich">
          <div>
            <strong>{{ file.originalName }}</strong>
            <span>{{ file.module }} · {{ formatSize(file.fileSize) }} · {{ file.fileType }}</span>
          </div>
          <a class="icon-text-button" :href="file.publicUrl" target="_blank" rel="noreferrer">预览</a>
        </article>
      </div>
    </section>

    <section v-else-if="activeSection === 'themes'" class="workspace-grid">
      <form class="workspace-panel admin-form" data-reveal @submit.prevent="saveTheme">
        <div class="panel-title">
          <h2>{{ editingThemeId ? '编辑主题' : '选择主题' }}</h2>
          <span>{{ themeForm.primaryColor || 'M3' }}</span>
        </div>
        <div class="form-line">
          <label>
            主题标识
            <input v-model="themeForm.themeName" maxlength="80" />
          </label>
          <label>
            显示名称
            <input v-model="themeForm.displayName" maxlength="120" />
          </label>
        </div>
        <div class="form-line">
          <label>
            主色
            <input v-model="themeForm.primaryColor" type="color" />
          </label>
          <label>
            背景类型
            <select v-model="themeForm.backgroundType">
              <option value="color">color</option>
              <option value="solid">solid</option>
              <option value="image">image</option>
              <option value="gradient">gradient</option>
              <option value="star">star</option>
              <option value="webgl">webgl</option>
            </select>
          </label>
        </div>
        <label>
          背景资源
          <input v-model="themeForm.backgroundImage" placeholder="/uploads/demo/theme.webp" />
        </label>
        <div class="form-line">
          <label>
            字体
            <input v-model="themeForm.fontFamily" maxlength="120" placeholder="Inter, system-ui, sans-serif" />
          </label>
          <label>
            卡片样式
            <input v-model="themeForm.cardStyle" maxlength="40" />
          </label>
        </div>
        <label>
          布局类型
          <input v-model="themeForm.layoutType" maxlength="40" />
        </label>
        <label>
          主题变量 JSON
          <textarea v-model="themeConfigText" rows="9" spellcheck="false" />
        </label>
        <div class="form-actions">
          <button class="button button-filled" type="submit">保存主题</button>
          <button v-if="editingThemeId" class="button button-tonal" type="button" @click="switchCurrentTheme()">
            设为当前主题
          </button>
        </div>
      </form>

      <div class="workspace-panel" data-reveal>
        <div class="panel-title">
          <h2>主题库</h2>
          <span>{{ themes.length }} themes</span>
        </div>
        <article v-for="theme in themes" :key="theme.id" class="table-row table-row--rich">
          <div>
            <strong>
              <span class="theme-dot" :style="{ backgroundColor: theme.primaryColor }" />
              {{ theme.displayName }}
            </strong>
            <span>{{ theme.themeName }} · {{ theme.layoutType }} · {{ theme.cardStyle }}</span>
          </div>
          <div class="row-actions">
            <span class="status-chip">{{ theme.active ? 'ACTIVE' : 'PRESET' }}</span>
            <button class="icon-text-button" type="button" @click="editTheme(theme)">编辑</button>
            <button class="icon-text-button" type="button" @click="switchCurrentTheme(theme.id)">启用</button>
          </div>
        </article>
      </div>
    </section>

    <section v-else-if="activeSection === 'settings'" class="workspace-grid">
      <form class="workspace-panel admin-form" data-reveal @submit.prevent="saveSiteSettings">
        <div class="panel-title">
          <h2>站点身份</h2>
          <span>{{ siteProfileForm.profileKey }}</span>
        </div>
        <div class="form-line">
          <label>
            配置标识
            <input v-model="siteProfileForm.profileKey" maxlength="80" />
          </label>
          <label>
            显示名称
            <input v-model="siteProfileForm.displayName" maxlength="120" />
          </label>
        </div>
        <label>
          标语
          <input v-model="siteProfileForm.headline" maxlength="180" />
        </label>
        <label>
          头像地址
          <input v-model="siteProfileForm.avatarUrl" placeholder="/uploads/demo/avatar.webp" />
        </label>
        <label>
          简介
          <textarea v-model="siteProfileForm.bio" rows="4" maxlength="5000" />
        </label>
        <div class="form-line">
          <label>
            联系邮箱
            <input v-model="siteProfileForm.contactEmail" maxlength="180" />
          </label>
          <label>
            位置
            <input v-model="siteProfileForm.location" maxlength="120" />
          </label>
        </div>
        <label>
          资料扩展 JSON
          <textarea v-model="profileJsonText" rows="5" spellcheck="false" />
        </label>
        <button class="button button-filled" type="submit">保存站点设置</button>
      </form>

      <div class="workspace-panel admin-form" data-reveal>
        <div class="panel-title">
          <h2>导航 / 社交 / 页面</h2>
          <span>JSON Editor</span>
        </div>
        <label>
          导航项 JSON
          <textarea v-model="navigationText" rows="7" spellcheck="false" />
        </label>
        <label>
          社交链接 JSON
          <textarea v-model="socialLinksText" rows="7" spellcheck="false" />
        </label>
        <label>
          页面配置 JSON
          <textarea v-model="pagesText" rows="7" spellcheck="false" />
        </label>
        <label>
          站点配置 JSON
          <textarea v-model="siteConfigsText" rows="7" spellcheck="false" />
        </label>
      </div>
    </section>

    <section v-else class="workspace-grid">
      <div class="workspace-panel" data-reveal>
        <div class="panel-title">
          <h2>内容队列</h2>
          <span>{{ moduleConfig.rows.length }} items</span>
        </div>
        <article v-for="row in moduleConfig.rows" :key="row.title" class="table-row table-row--rich">
          <div>
            <strong>{{ row.title }}</strong>
            <span>{{ row.meta }}</span>
          </div>
          <span class="status-chip">{{ row.status }}</span>
        </article>
      </div>

      <aside class="workspace-panel" data-reveal>
        <div class="panel-title">
          <h2>模块能力</h2>
          <span>Next</span>
        </div>
        <ul class="rule-list">
          <li v-for="capability in moduleConfig.capabilities" :key="capability">{{ capability }}</li>
        </ul>
      </aside>
    </section>

    <p v-if="notice" class="inline-notice">{{ notice }}</p>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Plus } from '@lucide/vue'

import {
  changeArticlePublishState,
  createArticle,
  createInspiration,
  createProject,
  deleteArticle,
  deleteInspiration,
  deleteProject,
  fetchAdminSiteSettings,
  fetchAdminThemes,
  fetchAdminArticle,
  fetchAdminArticles,
  fetchAdminComments,
  fetchAdminFiles,
  fetchAdminInspirations,
  fetchAdminProject,
  fetchAdminProjects,
  fetchCategories,
  fetchTags,
  reviewComment,
  setArticleRecommend,
  setArticleTop,
  setProjectRecommend,
  setProjectStatus,
  switchTheme,
  updateArticle,
  updateInspiration,
  updateProject,
  updateSiteSettings,
  updateTheme,
  uploadAdminFile,
} from '@/services/content'
import { usePageReveal } from '@/shared/composables/usePageReveal'
import type {
  AdminThemeConfig,
  ArticlePayload,
  ArticlePrivacy,
  ArticleSummary,
  CategorySummary,
  CommentSummary,
  ContentStatus,
  FileResource,
  InspirationCard,
  InspirationPayload,
  InspirationType,
  NavigationItem,
  PageConfig,
  ProjectPayload,
  ProjectSummary,
  SiteConfigEntry,
  SiteProfile,
  SiteSettings,
  SocialLink,
  TagSummary,
  ThemePayload,
} from '@/shared/domain'

interface ModuleConfig {
  eyebrow: string
  title: string
  description: string
  primaryAction: string
  capabilities: string[]
  rows: Array<{ title: string; meta: string; status: string }>
}

const route = useRoute()
const root = ref<HTMLElement | null>(null)
const notice = ref('')
const articles = ref<ArticleSummary[]>([])
const projects = ref<ProjectSummary[]>([])
const articleCategories = ref<CategorySummary[]>([])
const tags = ref<TagSummary[]>([])
const inspirations = ref<InspirationCard[]>([])
const comments = ref<CommentSummary[]>([])
const files = ref<FileResource[]>([])
const themes = ref<AdminThemeConfig[]>([])
const articleKeyword = ref('')
const projectKeyword = ref('')
const articleStatus = ref<ContentStatus | 'ALL'>('ALL')
const projectStatus = ref<ProjectSummary['status'] | 'ALL'>('ALL')
const commentStatus = ref<CommentSummary['status'] | 'ALL'>('PENDING')
const commentTargetType = ref<CommentSummary['targetType'] | 'ALL'>('ALL')
const fileModule = ref('OTHER')
const selectedFile = ref<File | null>(null)
const editingArticleId = ref<number | null>(null)
const editingProjectId = ref<number | null>(null)
const editingInspirationId = ref<number | null>(null)
const editingThemeId = ref<number | null>(null)
const projectTechStack = ref('')
const themeConfigText = ref('{}')
const profileJsonText = ref('{}')
const navigationText = ref('[]')
const socialLinksText = ref('[]')
const pagesText = ref('[]')
const siteConfigsText = ref('[]')

const articlePrivacies: ArticlePrivacy[] = ['PUBLIC', 'SELF', 'FRIENDS', 'SELECTED_FRIENDS', 'EXCLUDED_FRIENDS']
const inspirationTypes: InspirationType[] = ['TEXT', 'PROMPT', 'IMAGE', 'CODE', 'LINK']
const fileModules = ['AVATAR', 'COVER', 'ARTICLE', 'PROJECT', 'INSPIRATION', 'OTHER']
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
const inspirationForm = reactive<InspirationPayload>({
  title: '',
  content: '',
  imageUrl: '',
  cardType: 'TEXT',
  sourceUrl: '',
  color: '#6ea8ff',
  isPublic: true,
  sortOrder: 0,
  tagIds: [],
})
const themeForm = reactive<ThemePayload>({
  themeName: '',
  displayName: '',
  primaryColor: '#1a73e8',
  backgroundType: 'color',
  backgroundImage: '',
  fontFamily: 'Inter, system-ui, sans-serif',
  cardStyle: 'material',
  layoutType: 'editorial',
  config: {},
})
const siteProfileForm = reactive<SiteProfile>({
  profileKey: 'default',
  displayName: '',
  headline: '',
  avatarUrl: '',
  bio: '',
  contactEmail: '',
  location: '',
  profileJson: {},
})

usePageReveal(root)

const activeSection = computed(() => route.params.section?.toString() || 'articles')
const moduleConfig = computed(() => configs[activeSection.value] ?? configs.articles)

watch(activeSection, () => {
  notice.value = ''
  loadActiveModule()
})

onMounted(loadActiveModule)

async function loadActiveModule() {
  if (activeSection.value === 'articles') {
    await loadArticles()
  } else if (activeSection.value === 'projects') {
    await loadProjects()
  } else if (activeSection.value === 'inspirations') {
    await loadInspirations()
  } else if (activeSection.value === 'comments') {
    await loadComments()
  } else if (activeSection.value === 'files') {
    await loadFiles()
  } else if (activeSection.value === 'themes') {
    await loadThemes()
  } else if (activeSection.value === 'settings') {
    await loadSiteSettings()
  }
}

async function ensureTaxonomies() {
  if (articleCategories.value.length === 0) {
    articleCategories.value = await fetchCategories('ARTICLE')
  }
  if (tags.value.length === 0) {
    tags.value = await fetchTags()
  }
}

async function loadArticles() {
  try {
    await ensureTaxonomies()
    const page = await fetchAdminArticles({
      keyword: articleKeyword.value,
      status: articleStatus.value,
      pageSize: 50,
    })
    articles.value = page.records
  } catch (error) {
    notice.value = readError(error, '文章队列加载失败')
  }
}

async function saveArticle() {
  if (!articleForm.title.trim() || !articleForm.slug.trim() || !articleForm.contentMarkdown.trim()) {
    notice.value = '请填写文章标题、URL 标识和正文'
    return
  }
  try {
    if (editingArticleId.value) {
      await updateArticle(editingArticleId.value, { ...articleForm })
      notice.value = '文章已更新'
    } else {
      await createArticle({ ...articleForm })
      notice.value = '文章草稿已创建'
    }
    resetArticleForm()
    await loadArticles()
  } catch (error) {
    notice.value = readError(error, '文章保存失败')
  }
}

async function editArticle(id: number) {
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
    notice.value = readError(error, '文章读取失败')
  }
}

async function toggleArticlePublish(article: ArticleSummary) {
  try {
    await changeArticlePublishState(article.id, article.status === 'DRAFT' ? 'publish' : 'unpublish')
    notice.value = article.status === 'DRAFT' ? '文章已发布' : '文章已撤回'
    await loadArticles()
  } catch (error) {
    notice.value = readError(error, '文章状态更新失败')
  }
}

async function toggleArticleTop(article: ArticleSummary) {
  try {
    await setArticleTop(article.id, !article.top)
    notice.value = article.top ? '已取消置顶' : '文章已置顶'
    await loadArticles()
  } catch (error) {
    notice.value = readError(error, '置顶状态更新失败')
  }
}

async function toggleArticleRecommend(article: ArticleSummary) {
  try {
    await setArticleRecommend(article.id, !article.recommended)
    notice.value = article.recommended ? '已取消推荐' : '文章已推荐'
    await loadArticles()
  } catch (error) {
    notice.value = readError(error, '推荐状态更新失败')
  }
}

async function removeArticle(id: number) {
  try {
    await deleteArticle(id)
    notice.value = '文章已删除'
    if (editingArticleId.value === id) {
      resetArticleForm()
    }
    await loadArticles()
  } catch (error) {
    notice.value = readError(error, '文章删除失败')
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

async function loadProjects() {
  try {
    await ensureTaxonomies()
    const page = await fetchAdminProjects({
      keyword: projectKeyword.value,
      status: projectStatus.value,
      pageSize: 50,
    })
    projects.value = page.records
  } catch (error) {
    notice.value = readError(error, '作品队列加载失败')
  }
}

async function saveProject() {
  if (!projectForm.title.trim() || !projectForm.slug.trim() || !projectForm.projectType.trim()) {
    notice.value = '请填写作品标题、URL 标识和类型'
    return
  }
  const payload = { ...projectForm, techStack: splitTechStack(projectTechStack.value) }
  try {
    if (editingProjectId.value) {
      await updateProject(editingProjectId.value, payload)
      notice.value = '作品已更新'
    } else {
      await createProject(payload)
      notice.value = '作品已创建'
    }
    resetProjectForm()
    await loadProjects()
  } catch (error) {
    notice.value = readError(error, '作品保存失败')
  }
}

async function editProject(id: number) {
  try {
    const project = await fetchAdminProject(id)
    editingProjectId.value = project.id
    projectForm.title = project.title
    projectForm.slug = project.slug
    projectForm.description = project.description ?? ''
    projectForm.coverUrl = project.coverUrl ?? ''
    projectForm.projectType = project.projectType
    projectForm.techStack = project.techStack
    projectForm.githubUrl = project.githubUrl ?? ''
    projectForm.demoUrl = project.demoUrl ?? ''
    projectForm.videoUrl = project.videoUrl ?? ''
    projectForm.contentMarkdown = project.contentMarkdown ?? ''
    projectForm.tagIds = project.tags.map((tag) => tag.id)
    projectForm.recommended = project.recommended
    projectTechStack.value = project.techStack.join(', ')
  } catch (error) {
    notice.value = readError(error, '作品读取失败')
  }
}

async function toggleProjectVisible(project: ProjectSummary) {
  try {
    await setProjectStatus(project.id, project.status === 'VISIBLE' ? 'HIDDEN' : 'VISIBLE')
    notice.value = project.status === 'VISIBLE' ? '作品已隐藏' : '作品已展示'
    await loadProjects()
  } catch (error) {
    notice.value = readError(error, '作品状态更新失败')
  }
}

async function toggleProjectRecommend(project: ProjectSummary) {
  try {
    await setProjectRecommend(project.id, !project.recommended)
    notice.value = project.recommended ? '已取消推荐' : '作品已推荐'
    await loadProjects()
  } catch (error) {
    notice.value = readError(error, '作品推荐状态更新失败')
  }
}

async function removeProject(id: number) {
  try {
    await deleteProject(id)
    notice.value = '作品已删除'
    if (editingProjectId.value === id) {
      resetProjectForm()
    }
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

async function loadInspirations() {
  try {
    const page = await fetchAdminInspirations({ pageSize: 50 })
    inspirations.value = page.records
  } catch (error) {
    notice.value = readError(error, '灵感卡片加载失败')
  }
}

async function saveInspiration() {
  if (!inspirationForm.title.trim()) {
    notice.value = '请填写灵感标题'
    return
  }
  try {
    if (editingInspirationId.value) {
      await updateInspiration(editingInspirationId.value, { ...inspirationForm })
      notice.value = '灵感卡片已更新'
    } else {
      await createInspiration({ ...inspirationForm })
      notice.value = '灵感卡片已创建'
    }
    resetInspirationForm()
    await loadInspirations()
  } catch (error) {
    notice.value = readError(error, '灵感卡片保存失败')
  }
}

function editInspiration(card: InspirationCard) {
  editingInspirationId.value = card.id
  inspirationForm.title = card.title
  inspirationForm.content = card.content ?? ''
  inspirationForm.imageUrl = card.imageUrl ?? ''
  inspirationForm.cardType = card.cardType
  inspirationForm.sourceUrl = card.sourceUrl ?? ''
  inspirationForm.color = card.color ?? '#6ea8ff'
  inspirationForm.isPublic = card.isPublic ?? true
  inspirationForm.sortOrder = card.sortOrder
  inspirationForm.tagIds = card.tags.map((tag) => tag.id)
}

async function removeInspiration(id: number) {
  try {
    await deleteInspiration(id)
    notice.value = '灵感卡片已删除'
    await loadInspirations()
  } catch (error) {
    notice.value = readError(error, '灵感卡片删除失败')
  }
}

function resetInspirationForm() {
  editingInspirationId.value = null
  inspirationForm.title = ''
  inspirationForm.content = ''
  inspirationForm.imageUrl = ''
  inspirationForm.cardType = 'TEXT'
  inspirationForm.sourceUrl = ''
  inspirationForm.color = '#6ea8ff'
  inspirationForm.isPublic = true
  inspirationForm.sortOrder = 0
  inspirationForm.tagIds = []
}

async function loadComments() {
  try {
    const page = await fetchAdminComments({
      status: commentStatus.value,
      targetType: commentTargetType.value,
      pageSize: 50,
    })
    comments.value = page.records
  } catch (error) {
    notice.value = readError(error, '评论队列加载失败')
  }
}

async function review(id: number, action: 'approve' | 'reject') {
  try {
    await reviewComment(id, action)
    notice.value = action === 'approve' ? '评论已通过' : '评论已驳回'
    await loadComments()
  } catch (error) {
    notice.value = readError(error, '评论审核失败')
  }
}

async function loadFiles() {
  try {
    const page = await fetchAdminFiles({ pageSize: 50 })
    files.value = page.records
  } catch (error) {
    notice.value = readError(error, '文件资源加载失败')
  }
}

function selectFile(event: Event) {
  selectedFile.value = (event.target as HTMLInputElement).files?.[0] ?? null
}

async function uploadFile() {
  if (!selectedFile.value) {
    notice.value = '请选择文件'
    return
  }
  try {
    await uploadAdminFile(selectedFile.value, fileModule.value)
    selectedFile.value = null
    notice.value = '文件已上传'
    await loadFiles()
  } catch (error) {
    notice.value = readError(error, '文件上传失败')
  }
}

async function loadThemes() {
  try {
    themes.value = await fetchAdminThemes()
    const selected = themes.value.find((theme) => theme.id === editingThemeId.value)
      ?? themes.value.find((theme) => theme.active)
      ?? themes.value[0]
    if (selected) {
      editTheme(selected)
    }
  } catch (error) {
    notice.value = readError(error, '主题配置加载失败')
  }
}

function editTheme(theme: AdminThemeConfig) {
  editingThemeId.value = theme.id
  themeForm.themeName = theme.themeName
  themeForm.displayName = theme.displayName
  themeForm.primaryColor = theme.primaryColor
  themeForm.backgroundType = theme.backgroundType
  themeForm.backgroundImage = theme.backgroundImage ?? ''
  themeForm.fontFamily = theme.fontFamily ?? ''
  themeForm.cardStyle = theme.cardStyle
  themeForm.layoutType = theme.layoutType
  themeForm.config = normalizeRecord(theme.config)
  themeConfigText.value = prettyJson(themeForm.config)
}

async function saveTheme() {
  if (!editingThemeId.value) {
    notice.value = '请先从主题库选择一个主题'
    return
  }
  const config = parseRecord(themeConfigText.value, '主题变量 JSON')
  if (!config) {
    return
  }
  try {
    const saved = await updateTheme(editingThemeId.value, {
      ...themeForm,
      backgroundImage: themeForm.backgroundImage || null,
      fontFamily: themeForm.fontFamily || null,
      config,
    })
    notice.value = '主题已保存'
    await loadThemes()
    editTheme(saved)
  } catch (error) {
    notice.value = readError(error, '主题保存失败')
  }
}

async function switchCurrentTheme(id = editingThemeId.value) {
  if (!id) {
    notice.value = '请先选择主题'
    return
  }
  try {
    const switched = await switchTheme(id)
    notice.value = '当前主题已切换'
    await loadThemes()
    editTheme(switched)
  } catch (error) {
    notice.value = readError(error, '主题切换失败')
  }
}

async function loadSiteSettings() {
  try {
    applySiteSettings(await fetchAdminSiteSettings())
  } catch (error) {
    notice.value = readError(error, '站点设置加载失败')
  }
}

function applySiteSettings(settings: SiteSettings) {
  const profile = settings.profile
  siteProfileForm.profileKey = profile?.profileKey ?? 'default'
  siteProfileForm.displayName = profile?.displayName ?? ''
  siteProfileForm.headline = profile?.headline ?? ''
  siteProfileForm.avatarUrl = profile?.avatarUrl ?? ''
  siteProfileForm.bio = profile?.bio ?? ''
  siteProfileForm.contactEmail = profile?.contactEmail ?? ''
  siteProfileForm.location = profile?.location ?? ''
  siteProfileForm.profileJson = normalizeRecord(profile?.profileJson)
  profileJsonText.value = prettyJson(siteProfileForm.profileJson)
  navigationText.value = prettyJson(settings.navigationItems)
  socialLinksText.value = prettyJson(settings.socialLinks)
  pagesText.value = prettyJson(settings.pages)
  siteConfigsText.value = prettyJson(settings.configs)
}

async function saveSiteSettings() {
  const profileJson = parseRecord(profileJsonText.value, '资料扩展 JSON')
  const navigationItems = parseArray<NavigationItem>(navigationText.value, '导航项 JSON')
  const socialLinks = parseArray<SocialLink>(socialLinksText.value, '社交链接 JSON')
  const pages = parseArray<PageConfig>(pagesText.value, '页面配置 JSON')
  const configs = parseArray<SiteConfigEntry>(siteConfigsText.value, '站点配置 JSON')
  if (!profileJson || !navigationItems || !socialLinks || !pages || !configs) {
    return
  }
  try {
    const settings = await updateSiteSettings({
      profile: {
        ...siteProfileForm,
        avatarUrl: siteProfileForm.avatarUrl || null,
        headline: siteProfileForm.headline || null,
        bio: siteProfileForm.bio || null,
        contactEmail: siteProfileForm.contactEmail || null,
        location: siteProfileForm.location || null,
        profileJson,
      },
      navigationItems,
      socialLinks,
      pages,
      configs,
    })
    notice.value = '站点设置已保存'
    applySiteSettings(settings)
  } catch (error) {
    notice.value = readError(error, '站点设置保存失败')
  }
}

function handlePrimaryAction() {
  if (activeSection.value === 'articles') {
    resetArticleForm()
  } else if (activeSection.value === 'projects') {
    resetProjectForm()
  } else if (activeSection.value === 'inspirations') {
    resetInspirationForm()
  } else if (activeSection.value === 'comments') {
    commentStatus.value = 'PENDING'
    loadComments()
  } else if (activeSection.value === 'files') {
    loadFiles()
  } else if (activeSection.value === 'themes') {
    saveTheme()
  } else if (activeSection.value === 'settings') {
    saveSiteSettings()
  }
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

function splitTechStack(value: string) {
  return value
    .split(/[,，\n]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function prettyJson(value: unknown) {
  return JSON.stringify(value ?? {}, null, 2)
}

function parseRecord(value: string, label: string): Record<string, unknown> | null {
  try {
    const parsed: unknown = JSON.parse(value)
    if (isRecord(parsed)) {
      return parsed
    }
    notice.value = `${label} 必须是 JSON 对象`
    return null
  } catch {
    notice.value = `${label} 不是合法 JSON`
    return null
  }
}

function parseArray<T>(value: string, label: string): T[] | null {
  try {
    const parsed: unknown = JSON.parse(value)
    if (Array.isArray(parsed)) {
      return parsed as T[]
    }
    notice.value = `${label} 必须是 JSON 数组`
    return null
  } catch {
    notice.value = `${label} 不是合法 JSON`
    return null
  }
}

function normalizeRecord(value: unknown): Record<string, unknown> {
  return isRecord(value) ? value : {}
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value)
}

function readError(error: unknown, fallback: string) {
  return error instanceof Error ? `${fallback}: ${error.message}` : fallback
}

const configs: Record<string, ModuleConfig> = {
  articles: {
    eyebrow: 'Writing Desk',
    title: '文章管理',
    description: '草稿、发布、私密可见性、分类、标签和版本记录都在这里完成。',
    primaryAction: '新建文章',
    capabilities: ['Markdown 编辑', '封面上传', '推荐/置顶', '私密可见名单', '版本回滚预留'],
    rows: [
      { title: '把个人站点做成主题档案馆', meta: '产品设计 · 公开 · 2 天前', status: 'PUBLISHED' },
      { title: '朋友可见：季度复盘片段', meta: '个人知识库 · 选中好友', status: 'PRIVATE' },
      { title: '演示草稿：作品上传流程', meta: '幕后日志 · 未发布', status: 'DRAFT' },
    ],
  },
  projects: {
    eyebrow: 'Gallery Desk',
    title: '作品管理',
    description: '维护作品封面、截图、过程、时间线、技术栈和外部链接。',
    primaryAction: '新增作品',
    capabilities: ['作品截图', 'Demo/GitHub/视频链接', '过程记录', '里程碑时间线', '推荐展示'],
    rows: [
      { title: '内容整理后台', meta: 'CMS Console · Vue 3 / Spring Boot', status: 'VISIBLE' },
      { title: '主题博客前台', meta: 'Blog Frontstage · WebGL / GSAP', status: 'VISIBLE' },
      { title: '阅读动效实验', meta: 'Motion Study · anime.js', status: 'DRAFT' },
    ],
  },
  inspirations: {
    eyebrow: 'Idea Box',
    title: '灵感墙管理',
    description: '摘句、图片、链接、Prompt、草图和参考资料可以快速沉淀。',
    primaryAction: '重置表单',
    capabilities: [],
    rows: [],
  },
  comments: {
    eyebrow: 'Review Flow',
    title: '评论审核',
    description: '评论、回复、点赞和敏感词审核进入统一互动工作流。',
    primaryAction: '查看待审',
    capabilities: [],
    rows: [],
  },
  files: {
    eyebrow: 'Asset Library',
    title: '文件资源',
    description: '本地文件、封面、截图、附件和引用关系集中管理。',
    primaryAction: '刷新列表',
    capabilities: [],
    rows: [],
  },
  themes: {
    eyebrow: 'Theme Lab',
    title: '主题配置',
    description: '主题不仅是换色，还包含字体、密度、卡片、动效和首页模块顺序。',
    primaryAction: '保存主题',
    capabilities: ['主题变量 JSON', '主题版本', '资源绑定', '预览模式', '当前主题切换'],
    rows: [
      { title: 'Glass Space', meta: '玻璃星空风 · 当前启用', status: 'ACTIVE' },
      { title: 'Cyber Night', meta: '暗色科技风 · 预设', status: 'PRESET' },
      { title: 'Minimal White', meta: '极简阅读风 · 预设', status: 'PRESET' },
    ],
  },
  settings: {
    eyebrow: 'Site Config',
    title: '站点设置',
    description: '站点身份、导航、社交链接、SEO、首页编排和关于页内容从后台配置。',
    primaryAction: '保存配置',
    capabilities: ['导航配置', 'SEO 描述', '社交链接', '首页模块排序', '关于页内容块'],
    rows: [
      { title: '站点身份', meta: 'CreatorSpace · Personal Theme Archive', status: 'READY' },
      { title: '首页推荐', meta: '文章 / 作品 / 灵感', status: 'READY' },
    ],
  },
}
</script>

<style scoped>
.module-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 22px;
  min-height: clamp(220px, 23vw, 292px);
  padding: clamp(24px, 3vw, 42px);
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.88), rgba(255, 255, 255, 0.58)),
    radial-gradient(circle at 16% 20%, rgba(49, 91, 255, 0.14), transparent 34%),
    radial-gradient(circle at 88% 18%, rgba(0, 124, 114, 0.16), transparent 28%),
    linear-gradient(120deg, rgba(49, 91, 255, 0.08), rgba(194, 95, 58, 0.08), rgba(0, 124, 114, 0.1));
  box-shadow: var(--tone-shadow);
}

.module-hero h2 {
  max-width: 830px;
  margin: 0;
  color: var(--tone-ink);
  font-size: 44px;
  font-weight: 860;
  line-height: 1.08;
}

.module-hero p:not(.eyebrow) {
  max-width: 680px;
  color: var(--tone-muted);
  font-size: 17px;
  line-height: 1.72;
}

.admin-dashboard,
.admin-module {
  display: grid;
  gap: 18px;
}

.workspace-panel,
.metric-card {
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm);
  background: var(--tone-panel);
  box-shadow: var(--tone-shadow);
  backdrop-filter: blur(18px);
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  min-height: 136px;
  padding: 20px;
}

.metric-card span,
.metric-card p,
.panel-title span,
.table-row span {
  color: var(--tone-muted);
  font-size: 13px;
}

.metric-card strong {
  display: block;
  margin-top: 18px;
  font-size: 36px;
  line-height: 1;
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(300px, 0.75fr);
  gap: 14px;
  margin-top: 16px;
}

.workspace-grid--even {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.workspace-panel {
  padding: 20px;
}

.admin-form {
  display: grid;
  gap: 14px;
}

.admin-form label,
.comment-form {
  display: grid;
  gap: 8px;
}

.admin-form input,
.admin-form select,
.admin-form textarea,
.filter-bar input,
.filter-bar select,
.comment-form textarea {
  width: 100%;
  border: 1px solid rgba(17, 24, 39, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.86);
  color: var(--tone-strong);
  font: inherit;
}

.admin-form input,
.admin-form select,
.filter-bar input,
.filter-bar select {
  min-height: 42px;
  padding: 0 12px;
}

.admin-form textarea,
.comment-form textarea {
  resize: vertical;
  padding: 12px;
}

.form-line,
.filter-bar,
.form-actions,
.row-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.form-line > * {
  flex: 1;
  min-width: 160px;
}

.tag-picker {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-picker .check-line {
  min-height: 34px;
  padding: 6px 10px;
  border: 1px solid var(--tone-line);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.58);
  color: var(--tone-muted);
  font-size: 12px;
  font-weight: 760;
}

.check-line {
  display: flex !important;
  grid-template-columns: none;
  align-items: center;
  gap: 10px;
}

.check-line input {
  width: 18px;
  min-height: 18px;
}

.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 14px;
}

.panel-title h2 {
  margin: 0;
  font-size: 20px;
}

.table-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px;
  border-radius: 8px;
  background: rgba(20, 21, 29, 0.04);
}

.table-row + .table-row {
  margin-top: 10px;
}

.table-row div {
  display: grid;
  gap: 4px;
}

.theme-dot {
  display: inline-block;
  width: 12px;
  height: 12px;
  margin-right: 8px;
  border: 1px solid rgba(17, 24, 39, 0.16);
  border-radius: 50%;
  vertical-align: -1px;
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

.comments-card {
  display: grid;
  gap: 12px;
  margin-top: 16px;
  padding: 16px;
  border: 1px solid var(--tone-line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.72);
}

.comment-item {
  display: grid;
  gap: 4px;
  padding: 12px 0;
  border-top: 1px solid var(--tone-line);
}

.comment-item p {
  margin: 0;
}

.comment-item span,
.comments-card > span {
  color: var(--tone-muted);
  font-size: 13px;
}

.trend-bars {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  align-items: end;
  gap: 10px;
  min-height: 230px;
}

.trend-bars span {
  display: grid;
  align-items: end;
  gap: 8px;
  height: 210px;
}

.trend-bars i {
  display: block;
  height: var(--bar-height);
  min-height: 18px;
  border-radius: 8px 8px 3px 3px;
  background: linear-gradient(180deg, var(--tone-primary), var(--tone-teal));
}

.trend-bars small {
  text-align: center;
  color: var(--tone-faint);
  font-size: 11px;
}

@media (min-width: 761px) {
  .module-hero h2 {
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

@media (max-width: 1020px) {
  .module-hero,
  .workspace-grid,
  .workspace-grid--even {
    grid-template-columns: 1fr;
  }

  .dashboard-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .module-hero {
    padding: 22px;
  }

  .module-hero h2 {
    font-size: 30px;
  }

  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .panel-title {
    align-items: flex-start;
    flex-direction: column;
  }
}

.rule-list {
  display: grid;
  gap: 10px;
  margin: 0;
  padding-left: 18px;
}
</style>
