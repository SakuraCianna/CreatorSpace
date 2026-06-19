<template>
  <section ref="root" class="studio-page">
    <header class="studio-intro gallery-hero page-hero page-hero--projects" data-reveal>
      <h1>作品展厅</h1>
    </header>

    <div class="topic-strip" data-reveal>
      <button
        v-for="type in projectTypes"
        :key="type"
        class="topic-chip"
        :class="{ 'is-active': activeType === type }"
        type="button"
        @click="activeType = type"
      >
        {{ type }}
      </button>
    </div>

    <div v-if="isLoading" class="empty-state showcase-state" data-reveal>
      <LoaderCircle class="spin" :size="24" />
      <h2>正在点亮作品展厅</h2>
    </div>
    <div v-else-if="visibleProjects.length === 0" class="empty-state showcase-state" data-reveal>
      <h2>暂无匹配作品</h2>
      <p>换一个类型或稍后再来。</p>
    </div>
    <div v-else class="showcase-grid">
      <RouterLink
        v-for="(project, index) in visibleProjects"
        :key="project.id"
        class="showcase-card"
        :class="{ 'showcase-card--feature': index === 0 }"
        :style="projectCoverStyle(project, index)"
        :to="{ name: 'project-detail', params: { slug: project.slug } }"
        @pointermove="tiltCard"
        @pointerleave="resetTilt"
        data-reveal
      >
        <div class="showcase-card__visual">
          <img v-if="project.coverUrl" :src="project.coverUrl" alt="" loading="lazy" />
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

    <p v-if="notice" class="inline-notice">{{ notice }}</p>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { LoaderCircle } from '@lucide/vue'

import { fallbackProjects } from '@/content/studio'
import { fetchProjects } from '@/services/content'
import { usePageReveal } from '@/shared/composables/usePageReveal'
import type { ProjectSummary } from '@/shared/domain'

const coverPalettes = [
  ['#16213e', '#47d5ff'],
  ['#2d1b3d', '#ff7ab6'],
  ['#0f2f2d', '#6ee7b7'],
  ['#332400', '#f6c453'],
  ['#111827', '#a3e635'],
] as const

const root = ref<HTMLElement | null>(null)
const projects = ref<ProjectSummary[]>([])
const activeType = ref('全部')
const isLoading = ref(true)
const notice = ref('')

usePageReveal(root)

const projectTypes = computed(() => ['全部', ...new Set(projects.value.map((project) => project.projectType))])
const visibleProjects = computed(() => {
  if (activeType.value === '全部') {
    return projects.value
  }
  return projects.value.filter((project) => project.projectType === activeType.value)
})

async function loadProjects() {
  isLoading.value = true
  notice.value = ''
  try {
    const page = await fetchProjects()
    projects.value = page.records.length ? page.records : fallbackProjects
    if (!page.records.length) {
      notice.value = '已显示精选作品。'
    }
  } catch (error) {
    projects.value = fallbackProjects
    notice.value = '已显示精选作品。'
  } finally {
    isLoading.value = false
  }
}

function projectCoverStyle(project: ProjectSummary, index: number) {
  const palette = coverPalettes[index % coverPalettes.length]
  return {
    '--cover-from': palette[0],
    '--cover-accent': project.tags[0]?.color ?? palette[1],
  }
}

function tiltCard(event: PointerEvent) {
  const card = event.currentTarget as HTMLElement
  const rect = card.getBoundingClientRect()
  const x = (event.clientX - rect.left) / rect.width - 0.5
  const y = (event.clientY - rect.top) / rect.height - 0.5
  card.style.setProperty('--tilt-x', `${-y * 4}deg`)
  card.style.setProperty('--tilt-y', `${x * 5}deg`)
}

function resetTilt(event: PointerEvent) {
  const card = event.currentTarget as HTMLElement
  card.style.setProperty('--tilt-x', '0deg')
  card.style.setProperty('--tilt-y', '0deg')
}

onMounted(loadProjects)
</script>
