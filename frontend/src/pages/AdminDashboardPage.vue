<template>
  <section ref="root" class="admin-dashboard">
    <div class="dashboard-grid">
      <article v-for="metric in overview.metrics" :key="metric.label" class="metric-card" data-reveal>
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
        <p>{{ metric.note }}</p>
      </article>
    </div>

    <section class="workspace-grid">
      <div class="workspace-panel" data-reveal>
        <div class="panel-title">
          <div>
            <p class="eyebrow">7 day visits</p>
            <h2>访问趋势</h2>
          </div>
          <span>{{ totalPv }} PV</span>
        </div>
        <div class="trend-bars">
          <span
            v-for="point in overview.visitTrend"
            :key="point.date"
            :style="{ '--bar-height': `${barHeight(point.pv)}%` }"
          >
            <i />
            <small>{{ formatDay(point.date) }}</small>
          </span>
        </div>
      </div>

      <div class="workspace-panel" data-reveal>
        <div class="panel-title">
          <div>
            <p class="eyebrow">Review Queue</p>
            <h2>最近操作</h2>
          </div>
          <span>审计留痕</span>
        </div>
        <article v-for="activity in overview.recentActivities" :key="activity.operation + activity.createdAt" class="table-row">
          <div>
            <strong>{{ activity.operation }}</strong>
            <span>{{ activity.module }} · {{ activity.createdAt }}</span>
          </div>
          <span class="status-chip">LOG</span>
        </article>
      </div>
    </section>

    <section class="workspace-grid workspace-grid--even">
      <div class="workspace-panel" data-reveal>
        <div class="panel-title">
          <h2>热门文章</h2>
          <span>Top 5</span>
        </div>
        <article v-for="item in overview.hotArticles" :key="item.slug" class="table-row">
          <div>
            <strong>{{ item.title }}</strong>
            <span>{{ item.views }} views · {{ item.likes }} likes</span>
          </div>
          <RouterLink class="text-link" :to="{ name: 'article-detail', params: { slug: item.slug } }">查看</RouterLink>
        </article>
      </div>
      <div class="workspace-panel" data-reveal>
        <div class="panel-title">
          <h2>推荐作品</h2>
          <span>Gallery</span>
        </div>
        <article v-for="item in overview.hotProjects" :key="item.slug" class="table-row">
          <div>
            <strong>{{ item.title }}</strong>
            <span>作品档案 · {{ item.slug }}</span>
          </div>
          <RouterLink class="text-link" :to="{ name: 'project-detail', params: { slug: item.slug } }">查看</RouterLink>
        </article>
      </div>
    </section>

    <p v-if="notice" class="inline-notice">{{ notice }}</p>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

import { fetchDashboardOverview } from '@/services/content'
import { HttpError } from '@/services/http'
import { usePageReveal } from '@/shared/composables/usePageReveal'
import type { DashboardOverview } from '@/shared/domain'
import { useSessionStore } from '@/shared/sessionStore'

const root = ref<HTMLElement | null>(null)
const emptyOverview: DashboardOverview = {
  metrics: [],
  hotArticles: [],
  hotProjects: [],
  visitTrend: [],
  recentActivities: [],
}
const overview = ref<DashboardOverview>(emptyOverview)
const notice = ref('')
const router = useRouter()
const session = useSessionStore()

usePageReveal(root)

const maxPv = computed(() => Math.max(...overview.value.visitTrend.map((item) => item.pv), 1))
const totalPv = computed(() => overview.value.visitTrend.reduce((sum, item) => sum + item.pv, 0))

async function loadOverview() {
  notice.value = ''
  try {
    overview.value = await fetchDashboardOverview()
  } catch (error) {
    overview.value = emptyOverview
    if (error instanceof HttpError && [401, 403].includes(error.status)) {
      session.logout()
      router.replace({ name: 'login', query: { redirect: '/admin' } })
      return
    }
    notice.value = error instanceof Error ? `后台概览接口暂不可用：${error.message}` : '后台概览接口暂不可用。'
  }
}

function barHeight(value: number) {
  return Math.max(12, Math.round((value / maxPv.value) * 100))
}

function formatDay(value: string) {
  if (value.includes('-')) {
    return value.slice(5)
  }
  return value
}

onMounted(loadOverview)
</script>
