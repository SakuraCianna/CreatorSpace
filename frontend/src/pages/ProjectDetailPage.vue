<template>
  <section class="detail-page">
    <RouterLink class="text-link detail-back" :to="{ name: 'projects' }">返回作品</RouterLink>

    <div v-if="isLoading" class="empty-state detail-state">
      <h2>作品加载中</h2>
    </div>
    <div v-else-if="errorMessage" class="empty-state detail-state">
      <h2>作品暂时不可用</h2>
      <p>{{ errorMessage }}</p>
    </div>
    <article v-else-if="project" class="detail-panel">
      <header class="detail-hero">
        <p class="page-kicker">{{ project.projectType }}</p>
        <h1>{{ project.title }}</h1>
        <p v-if="project.description" class="detail-summary">{{ project.description }}</p>
        <div class="detail-meta">
          <span>{{ project.status }}</span>
          <span v-if="project.recommended">推荐作品</span>
        </div>
        <div v-if="project.techStack.length" class="tag-row">
          <span v-for="tech in project.techStack" :key="tech">{{ tech }}</span>
        </div>
        <div class="detail-actions">
          <a v-if="safeDemoUrl" class="button button-filled" :href="safeDemoUrl" target="_blank" rel="noreferrer">
            查看演示
          </a>
          <a v-if="safeGithubUrl" class="button button-tonal" :href="safeGithubUrl" target="_blank" rel="noreferrer">
            查看代码
          </a>
        </div>
      </header>

      <div class="markdown-body">
        <template v-if="contentBlocks.length">
          <component
            :is="block.kind"
            v-for="block in contentBlocks"
            :key="block.id"
          >
            {{ block.text }}
          </component>
        </template>
        <p v-else>这个作品还没有详细说明，先从简介了解它。</p>
      </div>
    </article>
    <div v-else class="empty-state detail-state">
      <h2>没有找到这个作品</h2>
    </div>
  </section>
</template>

<script setup lang="ts">
/**
 * 作品详情页负责展示公开作品说明、技术标签和外部访问入口。
 */
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

import { fetchProjectBySlug } from '@/services/content'
import type { ProjectSummary } from '@/shared/domain'

interface TextBlock {
  id: string
  kind: 'h2' | 'p' | 'blockquote' | 'li'
  text: string
}

const route = useRoute()
const project = ref<ProjectSummary | null>(null)
const isLoading = ref(true)
const errorMessage = ref('')
const slug = computed(() => readRouteParam(route.params.slug))
const contentBlocks = computed(() => parseMarkdown(project.value?.contentMarkdown ?? project.value?.description ?? ''))
const safeDemoUrl = computed(() => safeExternalUrl(project.value?.demoUrl))
const safeGithubUrl = computed(() => safeExternalUrl(project.value?.githubUrl))

// 读取作品详情，保证路由进入时不会停留在列表数据。
async function loadProject() {
  if (!slug.value) {
    errorMessage.value = '作品地址缺少 slug'
    project.value = null
    isLoading.value = false
    return
  }

  isLoading.value = true
  errorMessage.value = ''
  try {
    project.value = await fetchProjectBySlug(slug.value)
  } catch (error) {
    project.value = null
    errorMessage.value = error instanceof Error ? error.message : '作品加载失败'
  } finally {
    isLoading.value = false
  }
}

// 路由参数可能是数组，这里统一收敛成单个 slug。
function readRouteParam(value: string | string[] | undefined): string {
  if (Array.isArray(value)) {
    return value[0] ?? ''
  }
  return value ?? ''
}

// 使用文本节点渲染 Markdown，避免详情页直接插入未清洗 HTML。
function parseMarkdown(value: string): TextBlock[] {
  return value
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line, index) => {
      if (line.startsWith('## ')) {
        return createBlock(index, 'h2', line.slice(3))
      }
      if (line.startsWith('> ')) {
        return createBlock(index, 'blockquote', line.slice(2))
      }
      if (line.startsWith('- ')) {
        return createBlock(index, 'li', line.slice(2))
      }
      return createBlock(index, 'p', line.replace(/^#+\s*/, ''))
    })
}

// 为 v-for 生成稳定键值。
function createBlock(index: number, kind: TextBlock['kind'], text: string): TextBlock {
  return {
    id: `${kind}-${index}-${text.slice(0, 12)}`,
    kind,
    text,
  }
}

// 外链只允许浏览器安全跳转到 http/https 协议。
function safeExternalUrl(value?: string | null): string {
  if (!value) {
    return ''
  }
  try {
    const url = new URL(value)
    return ['http:', 'https:'].includes(url.protocol) ? url.toString() : ''
  } catch {
    return ''
  }
}

onMounted(loadProject)
watch(slug, loadProject)
</script>
