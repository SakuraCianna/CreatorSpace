<template>
  <section class="cms-page project-admin-page">
    <header class="cms-header">
      <div>
        <h2>作品管理</h2>
        <p>维护项目作品集的展示状态、推荐位、技术栈、外链、封面、标签和 Markdown 详情。</p>
      </div>
      <div class="header-actions">
        <button class="button button-tonal" type="button" :disabled="loading" @click="refreshAll">
          <RefreshCw :size="16" />
          刷新
        </button>
        <button class="button button-filled" type="button" @click="resetProjectForm">
          <Plus :size="16" />
          新建作品
        </button>
      </div>
    </header>

    <section class="cms-grid project-grid">
      <form class="cms-panel form-panel" @submit.prevent="saveProject">
        <div class="panel-title">
          <h3>{{ editingProjectId ? '编辑作品' : '新建作品' }}</h3>
          <span>{{ projectForm.projectType || '未设置类型' }}</span>
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
            <input v-model="projectForm.projectType" maxlength="60" placeholder="WEB_APP / DESIGN / TOOL" />
          </label>
          <label>
            技术栈
            <input v-model="projectTechStack" placeholder="Vue 3, Spring Boot, PostgreSQL" />
          </label>
        </div>

        <label>
          描述
          <textarea v-model="projectForm.description" rows="3" maxlength="2000" />
        </label>

        <label>
          封面图片 (建议比例 16:9)
          <FileUpload v-model="projectForm.coverUrl" module="COVER" accept="image/*" hint="最大 10MB" />
        </label>

        <div class="form-line">
          <label>
            GitHub
            <input v-model="projectForm.githubUrl" placeholder="https://github.com/..." />
          </label>
          <label>
            演示地址 (可选 URL)
            <input v-model="projectForm.demoUrl" placeholder="https://demo.example.com" />
          </label>
        </div>

        <label>
          演示视频 (支持 MP4/WebM，最大 100MB)
          <FileUpload v-model="projectForm.videoUrl" module="PROJECT" accept="video/mp4,video/webm,video/quicktime,video/x-matroska" hint="最大 100MB" />
        </label>

        <label>
          Markdown 详情
          <textarea v-model="projectForm.contentMarkdown" rows="12" />
        </label>

        <div class="tag-picker">
          <label v-for="tag in tags" :key="tag.id" class="check-line">
            <input v-model="projectForm.tagIds" type="checkbox" :value="tag.id" />
            {{ tag.name }}
          </label>
        </div>

        <label class="check-line recommend-line">
          <input v-model="projectForm.recommended" type="checkbox" />
          推荐展示
        </label>

        <div class="form-actions">
          <button class="button button-filled" type="submit">{{ editingProjectId ? '保存作品' : '创建作品' }}</button>
          <button v-if="editingProjectId" class="button button-tonal" type="button" @click="cancelEditing">取消编辑</button>
        </div>
      </form>

      <div class="project-stack">
        <div class="cms-panel">
          <div class="panel-title">
            <h3>筛选作品</h3>
            <span>共 {{ total }} 个</span>
          </div>
          <div class="filter-bar">
            <input v-model="keyword" placeholder="搜索标题 / 描述 / 详情" @keyup.enter="loadProjects" />
            <select v-model="statusFilter" @change="loadProjects">
              <option value="ALL">全部状态</option>
              <option value="DRAFT">草稿</option>
              <option value="PENDING_REVIEW">待审核</option>
              <option value="VISIBLE">展示中</option>
              <option value="HIDDEN">已隐藏</option>
              <option value="REJECTED">已驳回</option>
              <option value="ARCHIVED">已归档</option>
            </select>
          </div>
        </div>

        <div class="cms-panel">
          <div class="panel-title">
            <h3>作品列表</h3>
            <span>第 {{ page }} / {{ totalPages }} 页</span>
          </div>
          <div class="list-stack">
            <article v-for="project in projects" :key="project.id" class="table-row table-row--rich">
              <div>
                <strong>{{ project.title }}</strong>
                <span>{{ project.projectType }} - {{ project.techStack.join(' / ') || '未维护技术栈' }} - {{ project.slug }}</span>
                <small v-if="project.reviewNote">{{ project.reviewNote }}</small>
              </div>
              <div class="row-actions">
                <span class="status-chip" :class="{ muted: project.status === 'HIDDEN' || project.status === 'ARCHIVED' }">
                  {{ projectStatusLabel(project.status) }}
                </span>
                <button class="text-button" type="button" @click="loadProject(project.id)">编辑</button>
                <button
                  v-if="project.status === 'PENDING_REVIEW'"
                  class="text-button"
                  type="button"
                  @click="approveProjectReview(project)"
                >
                  通过
                </button>
                <button
                  v-if="project.status === 'PENDING_REVIEW'"
                  class="text-button danger"
                  type="button"
                  @click="rejectProjectReview(project)"
                >
                  驳回
                </button>
                <button class="text-button" type="button" @click="toggleProjectVisible(project)">
                  {{ project.status === 'VISIBLE' ? '隐藏' : '展示' }}
                </button>
                <button class="text-button" type="button" @click="toggleProjectRecommend(project)">
                  {{ project.recommended ? '取消推荐' : '推荐' }}
                </button>
                <button class="text-button danger" type="button" @click="removeProject(project.id)">删除</button>
              </div>
            </article>
            <p v-if="projects.length === 0 && !loading" class="empty-hint">暂无作品。</p>
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
import { Plus, RefreshCw } from '@lucide/vue'
import FileUpload from '../components/common/FileUpload.vue'

import {
  approveProject,
  createProject,
  deleteProject,
  fetchAdminProject,
  fetchAdminProjects,
  fetchTags,
  rejectProject,
  setProjectRecommend,
  setProjectStatus,
  updateProject,
} from '../services/content'
import { toUserMessage } from '../services/http'
import type { ProjectPayload, ProjectStatus, ProjectSummary, TagSummary } from '../shared/domain'

type ProjectStatusFilter = ProjectStatus | 'ALL'

const projects = ref<ProjectSummary[]>([])
const tags = ref<TagSummary[]>([])
const editingProjectId = ref<number | null>(null)
const keyword = ref('')
const statusFilter = ref<ProjectStatusFilter>('ALL')
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const loading = ref(false)
const notice = ref('')
const projectTechStack = ref('')

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

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

onMounted(async () => {
  await Promise.all([ensureTags(), loadProjects()])
})

async function ensureTags() {
  if (tags.value.length === 0) {
    tags.value = await fetchTags()
  }
}

async function refreshAll() {
  page.value = 1
  await loadProjects()
}

async function loadProjects() {
  loading.value = true
  notice.value = ''
  try {
    await ensureTags()
    const result = await fetchAdminProjects({
      keyword: keyword.value,
      status: statusFilter.value,
      page: page.value,
      pageSize: pageSize.value,
    })
    projects.value = result.records
    total.value = result.total
    page.value = result.page
  } catch (error) {
    notice.value = toUserMessage(error, '作品列表加载失败')
  } finally {
    loading.value = false
  }
}

async function loadProject(id: number) {
  notice.value = ''
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
    notice.value = toUserMessage(error, '作品读取失败')
  }
}

async function saveProject() {
  notice.value = ''
  if (!projectForm.title.trim() || !projectForm.slug.trim() || !projectForm.projectType.trim()) {
    notice.value = '请填写作品标题、URL 标识和类型'
    return
  }
  const payload: ProjectPayload = {
    ...projectForm,
    techStack: splitTechStack(projectTechStack.value),
  }
  try {
    if (editingProjectId.value) {
      await updateProject(editingProjectId.value, payload)
      notice.value = '作品已保存'
    } else {
      const created = await createProject(payload)
      editingProjectId.value = created.id
      notice.value = '作品已创建'
    }
    await loadProjects()
    if (editingProjectId.value) {
      await loadProject(editingProjectId.value)
    }
  } catch (error) {
    notice.value = toUserMessage(error, '作品保存失败')
  }
}

async function toggleProjectVisible(project: ProjectSummary) {
  notice.value = ''
  try {
    if (project.status === 'PENDING_REVIEW') {
      await approveProject(project.id)
      notice.value = '作品审核通过'
    } else {
      const nextStatus = project.status === 'VISIBLE' ? 'HIDDEN' : 'VISIBLE'
      await setProjectStatus(project.id, nextStatus)
      notice.value = nextStatus === 'VISIBLE' ? '作品已展示' : '作品已隐藏'
    }
    await loadProjects()
    if (editingProjectId.value === project.id) {
      await loadProject(project.id)
    }
  } catch (error) {
    notice.value = toUserMessage(error, '作品状态更新失败')
  }
}

async function approveProjectReview(project: ProjectSummary) {
  notice.value = ''
  try {
    await approveProject(project.id)
    notice.value = '作品审核通过'
    await loadProjects()
    if (editingProjectId.value === project.id) {
      await loadProject(project.id)
    }
  } catch (error) {
    notice.value = toUserMessage(error, '作品审核失败')
  }
}

async function rejectProjectReview(project: ProjectSummary) {
  notice.value = ''
  const reviewNote = window.prompt('请输入驳回原因', project.reviewNote ?? '请补充素材授权、创作说明或演示链接。')?.trim()
  if (!reviewNote) {
    return
  }
  try {
    await rejectProject(project.id, reviewNote)
    notice.value = '作品已驳回'
    await loadProjects()
    if (editingProjectId.value === project.id) {
      await loadProject(project.id)
    }
  } catch (error) {
    notice.value = toUserMessage(error, '作品驳回失败')
  }
}

async function toggleProjectRecommend(project: ProjectSummary) {
  notice.value = ''
  try {
    await setProjectRecommend(project.id, !project.recommended)
    notice.value = project.recommended ? '已取消推荐' : '作品已推荐'
    await loadProjects()
    if (editingProjectId.value === project.id) {
      await loadProject(project.id)
    }
  } catch (error) {
    notice.value = toUserMessage(error, '作品推荐状态更新失败')
  }
}

async function removeProject(id: number) {
  notice.value = ''
  if (!window.confirm('确认删除这个作品吗？')) {
    return
  }
  try {
    await deleteProject(id)
    notice.value = '作品已删除'
    if (editingProjectId.value === id) {
      resetProjectForm()
    }
    if (projects.value.length === 1 && page.value > 1) {
      page.value -= 1
    }
    await loadProjects()
  } catch (error) {
    notice.value = toUserMessage(error, '作品删除失败')
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

function cancelEditing() {
  resetProjectForm()
}

function changePage(nextPage: number) {
  page.value = nextPage
  loadProjects()
}

function splitTechStack(value: string) {
  return value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}



function projectStatusLabel(value: string) {
  return {
    DRAFT: '草稿',
    PENDING_REVIEW: '待审核',
    VISIBLE: '展示中',
    HIDDEN: '已隐藏',
    REJECTED: '已驳回',
    ARCHIVED: '已归档',
  }[value] ?? value
}
</script>

<style scoped>
.project-admin-page {
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

.project-grid {
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.15fr);
}

.project-stack {
  display: grid;
  gap: 14px;
}



.tag-picker {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-picker .check-line,
.recommend-line {
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

.project-admin-page .table-row > .row-actions {
  display: grid;
  justify-items: center;
  align-content: start;
  gap: 10px;
  min-width: 96px;
}

.project-admin-page .row-actions .text-button {
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

.table-row small {
  color: var(--admin-danger);
  font-size: 12px;
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
  .project-grid {
    grid-template-columns: 1fr;
  }

  .cms-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
