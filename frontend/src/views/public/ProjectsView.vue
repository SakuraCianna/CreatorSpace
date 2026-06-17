<template>
  <section class="page-section">
    <div class="section-heading">
      <h1>作品</h1>
      <p>作品集页面展示后台创建并设置为可见的项目。</p>
    </div>
    <div v-if="isLoading" class="empty-state">
      <h2>作品加载中</h2>
    </div>
    <div v-else-if="errorMessage" class="empty-state">
      <h2>作品暂时不可用</h2>
      <p>{{ errorMessage }}</p>
    </div>
    <div v-else-if="projects.length === 0" class="empty-state">
      <h2>暂无公开作品</h2>
      <p>新增可见作品后会显示在这里。</p>
    </div>
    <div v-else class="list-surface">
      <article v-for="project in projects" :key="project.id" class="list-row">
        <div>
          <h2>{{ project.title }}</h2>
          <p>{{ projectMeta(project) }}</p>
        </div>
        <span class="status-chip">{{ project.recommended ? '推荐' : project.status }}</span>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { fetchProjects } from '@/api/content'
import type { ProjectSummary } from '@/types/domain'

const projects = ref<ProjectSummary[]>([])
const isLoading = ref(true)
const errorMessage = ref('')

// 加载公开作品列表。
async function loadProjects() {
  isLoading.value = true
  errorMessage.value = ''
  try {
    const page = await fetchProjects()
    projects.value = page.records
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '作品加载失败'
  } finally {
    isLoading.value = false
  }
}

// 拼接作品卡片的辅助信息。
function projectMeta(project: ProjectSummary) {
  return [
    project.projectType,
    ...project.techStack,
    ...project.tags.map((tag) => `#${tag.name}`),
  ].join(' · ')
}

onMounted(loadProjects)
</script>
