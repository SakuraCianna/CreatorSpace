<template>
  <section class="studio-page">
    <div class="studio-intro">
      <p class="page-kicker">Creative Works</p>
      <h1>创意作品橱窗</h1>
      <p>这里放能被看见的创作成果：页面、工具、视觉实验和长期打磨过的小作品。</p>
    </div>

    <div v-if="isLoading" class="empty-state showcase-state">
      <h2>作品加载中</h2>
    </div>
    <div v-else-if="errorMessage" class="empty-state showcase-state">
      <h2>作品暂时不可用</h2>
      <p>{{ errorMessage }}</p>
    </div>
    <div v-else-if="projects.length === 0" class="empty-state showcase-state">
      <h2>暂无公开作品</h2>
    </div>
    <div v-else class="showcase-grid">
      <RouterLink
        v-for="(project, index) in projects"
        :key="project.id"
        class="showcase-card"
        :class="{ 'showcase-card--feature': index === 0 }"
        :style="projectCoverStyle(project, index)"
        :to="{ name: 'project-detail', params: { slug: project.slug } }"
      >
        <div class="showcase-card__visual">
          <span>{{ String(index + 1).padStart(2, '0') }}</span>
        </div>
        <div class="showcase-card__body">
          <div class="showcase-card__meta">
            <span>{{ project.projectType }}</span>
            <span v-if="project.recommended">推荐</span>
          </div>
          <h2>{{ project.title }}</h2>
          <p>{{ project.description }}</p>
          <div class="tag-row">
            <span v-for="tech in project.techStack.slice(0, 4)" :key="tech">{{ tech }}</span>
          </div>
        </div>
      </RouterLink>
    </div>
  </section>
</template>

<script setup lang="ts">
/**
 * 公开作品页用作品集橱窗结构展示内容，强调个性主题和视觉识别。
 */
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'

import { fetchProjects } from '@/services/content'
import type { ProjectSummary } from '@/shared/domain'

const coverPalettes = [
  ['#16213e', '#47d5ff'],
  ['#2d1b3d', '#ff7ab6'],
  ['#0f2f2d', '#6ee7b7'],
  ['#332400', '#f6c453'],
  ['#111827', '#a3e635'],
] as const

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

// 生成稳定的作品封面色，作为真实封面资源加载前的视觉底板。
function projectCoverStyle(project: ProjectSummary, index: number) {
  const palette = coverPalettes[index % coverPalettes.length]
  return {
    '--cover-from': palette[0],
    '--cover-accent': project.tags[0]?.color ?? palette[1],
  }
}

onMounted(loadProjects)
</script>
