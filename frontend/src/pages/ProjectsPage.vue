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

<style scoped>
.studio-page {
  padding: 46px 0 84px;
}

.gallery-hero {
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
    radial-gradient(circle at 16% 20%, color-mix(in srgb, var(--hero-accent) 18%, transparent), transparent 34%),
    radial-gradient(circle at 88% 18%, color-mix(in srgb, var(--hero-accent-2) 18%, transparent), transparent 28%),
    linear-gradient(120deg, rgba(49, 91, 255, 0.08), rgba(194, 95, 58, 0.08), rgba(0, 124, 114, 0.1));
  box-shadow: var(--tone-shadow);
  isolation: isolate;
  overflow: hidden;
  position: relative;
}

.page-hero {
  --hero-accent: #6d3fd2;
  --hero-accent-2: #0077b6;
  --hero-mark: "02";
}

.page-hero::before {
  content: "";
  position: absolute;
  inset: 16px;
  z-index: 0;
  border: 1px solid color-mix(in srgb, var(--hero-accent) 24%, transparent);
  background:
    linear-gradient(90deg, color-mix(in srgb, var(--hero-accent) 10%, transparent), transparent 38%),
    repeating-linear-gradient(90deg, rgba(20, 21, 29, 0.05) 0 1px, transparent 1px 42px),
    repeating-linear-gradient(0deg, rgba(20, 21, 29, 0.035) 0 1px, transparent 1px 34px);
  clip-path: polygon(0 0, calc(100% - 46px) 0, 100% 46px, 100% 100%, 46px 100%, 0 calc(100% - 46px));
  opacity: 0.9;
  pointer-events: none;
}

.page-hero::after {
  content: var(--hero-mark);
  position: absolute;
  top: clamp(16px, 4vw, 40px);
  right: clamp(24px, 6vw, 88px);
  z-index: 0;
  color: color-mix(in srgb, var(--hero-accent) 16%, transparent);
  font-size: clamp(86px, 12vw, 180px);
  font-weight: 900;
  line-height: 0.8;
  pointer-events: none;
}

.page-hero > * {
  position: relative;
  z-index: 1;
}

.page-hero h1 {
  position: relative;
  display: inline-block;
  justify-self: start;
  width: fit-content;
  max-width: 100%;
  padding-top: 24px;
  text-shadow: 0 16px 34px rgba(20, 21, 29, 0.1);
}

.page-hero h1::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: clamp(110px, 18vw, 240px);
  height: 8px;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--hero-accent), var(--hero-accent-2), transparent);
  box-shadow: 0 10px 28px color-mix(in srgb, var(--hero-accent) 22%, transparent);
}

.page-hero h1::after {
  content: "";
  position: absolute;
  top: -2px;
  right: -54px;
  width: 34px;
  height: 14px;
  border: 1px solid color-mix(in srgb, var(--hero-accent) 42%, transparent);
  border-radius: 999px;
  background: color-mix(in srgb, var(--hero-accent-2) 18%, transparent);
  box-shadow: 18px 0 0 color-mix(in srgb, var(--hero-accent) 18%, transparent);
}

.gallery-hero h1 {
  max-width: 830px;
  margin: 0;
  color: var(--tone-ink);
  font-size: 44px;
  font-weight: 860;
  line-height: 1.08;
}

.topic-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 22px 0;
}

.topic-chip {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-height: 38px;
  padding: 0 14px;
  border: 1px solid var(--tone-line);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  color: var(--tone-muted);
  cursor: pointer;
  font-size: 13px;
  font-weight: 760;
}

.topic-chip.is-active,
.topic-chip:hover {
  border-color: rgba(49, 91, 255, 0.36);
  background: rgba(49, 91, 255, 0.1);
  color: var(--tone-ink);
}

.empty-state,
.showcase-card {
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm);
  background: rgba(255, 255, 255, 0.78);
  box-shadow: var(--tone-shadow);
  transition: transform 180ms ease, border-color 180ms ease;
}

.showcase-card:hover {
  border-color: rgba(49, 91, 255, 0.34);
  transform: translateY(-3px);
}

.showcase-card__meta {
  color: rgba(248, 250, 252, 0.72);
  font-size: 12px;
  font-weight: 780;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.showcase-grid {
  display: grid;
  gap: 16px;
}

.showcase-card__meta {
  color: var(--tone-faint);
}

.showcase-card h2 {
  margin: 12px 0 0;
  color: var(--tone-ink);
  font-size: 24px;
  line-height: 1.18;
}

.showcase-card p {
  color: var(--tone-muted);
  line-height: 1.68;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.tag-row span {
  padding: 6px 10px;
  border-radius: 6px;
  background: rgba(0, 124, 114, 0.1);
  color: #055f57;
  font-size: 12px;
  font-weight: 740;
}

.studio-intro {
  margin-bottom: 0;
}

.showcase-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  perspective: 1200px;
}

.showcase-card {
  display: grid;
  grid-template-rows: 230px minmax(0, 1fr);
  overflow: hidden;
  transform: rotateX(var(--tilt-x, 0deg)) rotateY(var(--tilt-y, 0deg));
}

.showcase-card--feature {
  grid-column: span 2;
}

.showcase-card__visual {
  position: relative;
  display: flex;
  align-items: flex-end;
  min-height: 230px;
  padding: 22px;
  overflow: hidden;
  background:
    linear-gradient(145deg, rgba(3, 7, 18, 0.08), rgba(3, 7, 18, 0.66)),
    linear-gradient(135deg, var(--cover-from), var(--cover-accent));
  color: #fff;
}

.showcase-card__visual img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0.72;
}

.showcase-card__visual span {
  position: relative;
  z-index: 1;
  font-size: 52px;
  font-weight: 860;
  line-height: 0.9;
}

.showcase-card__body {
  padding: 20px;
}

.showcase-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

@media (min-width: 761px) {
  .gallery-hero h1 {
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

@media (max-width: 1020px) {
  .gallery-hero,
  .showcase-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .studio-page {
    padding-top: 26px;
  }

  .gallery-hero {
    padding: 22px;
  }

  .gallery-hero h1 {
    font-size: 32px;
  }

  .showcase-grid {
    grid-template-columns: 1fr;
  }

  .showcase-card--feature {
    grid-column: span 1;
  }
}

@media (max-width: 520px) {
  .gallery-hero h1 {
    font-size: 28px;
  }
}
</style>
