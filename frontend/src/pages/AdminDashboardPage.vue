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

    <section class="workspace-grid workspace-grid--even">
      <div class="workspace-panel" data-reveal>
        <div class="panel-title">
          <h2>热门搜索</h2>
          <span>Top 10</span>
        </div>
        <article v-for="item in overview.hotSearchKeywords" :key="item.keyword" class="table-row">
          <div>
            <strong>{{ item.keyword }}</strong>
            <span>{{ item.count }} 次搜索</span>
          </div>
          <span class="status-chip">HOT</span>
        </article>
        <span v-if="overview.hotSearchKeywords.length === 0" class="empty-hint">暂无搜索记录</span>
      </div>
      <div class="workspace-panel" data-reveal>
        <div class="panel-title">
          <h2>内容统计</h2>
          <span>Summary</span>
        </div>
        <div class="summary-grid">
          <div v-for="metric in overview.metrics" :key="metric.label" class="summary-item">
            <span>{{ metric.label }}</span>
            <strong>{{ metric.value }}</strong>
          </div>
        </div>
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
  hotSearchKeywords: [],
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

<style scoped>
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

@media (min-width: 1200px) {
  .dashboard-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
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

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.summary-item {
  display: grid;
  gap: 4px;
  padding: 12px;
  border-radius: 8px;
  background: rgba(49, 91, 255, 0.06);
}

.summary-item span {
  color: var(--tone-muted);
  font-size: 12px;
}

.summary-item strong {
  font-size: 24px;
  line-height: 1;
}

.empty-hint {
  color: var(--tone-muted);
  font-size: 13px;
}

.inline-notice {
  margin-top: 16px;
  padding: 12px 14px;
  border-left: 3px solid var(--tone-coral);
  background: rgba(194, 95, 58, 0.08);
  color: #754226;
  font-size: 13px;
  line-height: 1.55;
}
@media (max-width: 1020px) {
  .workspace-grid,
  .workspace-grid--even {
    grid-template-columns: 1fr;
  }

  .dashboard-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .panel-title {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
