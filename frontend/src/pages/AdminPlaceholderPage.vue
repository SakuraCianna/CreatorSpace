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

    <section v-if="activeSection === 'inspirations'" class="workspace-grid">
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
  createInspiration,
  deleteInspiration,
  fetchAdminComments,
  fetchAdminFiles,
  fetchAdminInspirations,
  reviewComment,
  updateInspiration,
  uploadAdminFile,
} from '@/services/content'
import { usePageReveal } from '@/shared/composables/usePageReveal'
import type { CommentSummary, FileResource, InspirationCard, InspirationPayload, InspirationType } from '@/shared/domain'

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
const inspirations = ref<InspirationCard[]>([])
const comments = ref<CommentSummary[]>([])
const files = ref<FileResource[]>([])
const commentStatus = ref<CommentSummary['status'] | 'ALL'>('PENDING')
const commentTargetType = ref<CommentSummary['targetType'] | 'ALL'>('ALL')
const fileModule = ref('OTHER')
const selectedFile = ref<File | null>(null)
const editingInspirationId = ref<number | null>(null)

const inspirationTypes: InspirationType[] = ['TEXT', 'PROMPT', 'IMAGE', 'CODE', 'LINK']
const fileModules = ['AVATAR', 'COVER', 'ARTICLE', 'PROJECT', 'INSPIRATION', 'OTHER']
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

usePageReveal(root)

const activeSection = computed(() => route.params.section?.toString() || 'articles')
const moduleConfig = computed(() => configs[activeSection.value] ?? configs.articles)

watch(activeSection, () => {
  notice.value = ''
  loadActiveModule()
})

onMounted(loadActiveModule)

async function loadActiveModule() {
  if (activeSection.value === 'inspirations') {
    await loadInspirations()
  } else if (activeSection.value === 'comments') {
    await loadComments()
  } else if (activeSection.value === 'files') {
    await loadFiles()
  }
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

function handlePrimaryAction() {
  if (activeSection.value === 'inspirations') {
    resetInspirationForm()
  } else if (activeSection.value === 'comments') {
    commentStatus.value = 'PENDING'
    loadComments()
  } else if (activeSection.value === 'files') {
    loadFiles()
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
