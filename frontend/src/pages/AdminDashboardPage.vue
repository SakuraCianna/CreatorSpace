<template>
<!-- 后台指标数据概览面板 -->
  <section ref="root" class="admin-dashboard">
    <!-- 核心数据度量网格 -->
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
            <p class="eyebrow">近七日趋势</p>
            <h2>访问与搜索趋势</h2>
          </div>
          <span>访问 {{ totalPv }} 次 · 搜索 {{ totalSearch }} 次</span>
        </div>
        <!-- 近七日流量与搜索趋势图表卡片 -->
        <div ref="visitChartRef" class="visit-chart" aria-label="近七日访问与搜索趋势图" />
      </div>

      <div class="workspace-panel" data-reveal>
        <div class="panel-title">
          <div>
            <p class="eyebrow">操作记录</p>
            <h2>最近操作</h2>
          </div>
          <span>审计留痕</span>
        </div>
        <article v-for="activity in overview.recentActivities" :key="activity.operation + activity.createdAt" class="table-row">
          <div>
            <strong>{{ activity.operation }}</strong>
            <span>{{ activity.module }} · {{ formatDateTimeToSecond(activity.createdAt) }}</span>
          </div>
          <span class="status-chip">记录</span>
        </article>
        <span v-if="overview.recentActivities.length === 0" class="empty-hint">暂无操作记录</span>
      </div>
    </section>

    <section class="workspace-grid workspace-grid--even">
      <div class="workspace-panel" data-reveal>
        <div class="panel-title">
          <h2>热门文章</h2>
          <span>前 5 条</span>
        </div>
        <article v-for="item in overview.hotArticles" :key="item.slug" class="table-row">
          <div>
            <strong>{{ item.title }}</strong>
            <span>{{ item.views }} 次浏览 · {{ item.likes }} 次点赞</span>
          </div>
          <RouterLink class="text-link" :to="{ name: 'article-detail', params: { slug: item.slug } }">查看</RouterLink>
        </article>
        <span v-if="overview.hotArticles.length === 0" class="empty-hint">暂无热门文章</span>
      </div>
      <div class="workspace-panel" data-reveal>
        <div class="panel-title">
          <h2>热门作品</h2>
          <span>前 5 条</span>
        </div>
        <article v-for="item in overview.hotProjects" :key="item.slug" class="table-row">
          <div>
            <strong>{{ item.title }}</strong>
            <span>{{ item.views }} 次浏览 · {{ item.likes }} 次点赞</span>
          </div>
          <RouterLink class="text-link" :to="{ name: 'project-detail', params: { slug: item.slug } }">查看</RouterLink>
        </article>
        <span v-if="overview.hotProjects.length === 0" class="empty-hint">暂无热门作品</span>
      </div>
    </section>

    <section class="workspace-grid workspace-grid--even">
      <div class="workspace-panel" data-reveal>
        <div class="panel-title">
          <h2>热门搜索</h2>
          <span>前 10 条</span>
        </div>
        <article v-for="item in overview.hotSearchKeywords" :key="item.keyword" class="table-row">
          <div>
            <strong>{{ item.keyword }}</strong>
            <span>{{ item.count }} 次搜索</span>
          </div>
          <span class="status-chip">热门</span>
        </article>
        <span v-if="overview.hotSearchKeywords.length === 0" class="empty-hint">暂无搜索记录</span>
      </div>
      <div class="workspace-panel" data-reveal>
        <div class="panel-title">
          <h2>内容统计</h2>
          <span>数据汇总</span>
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
import type { EChartsType } from 'echarts/core'
import { graphic, init, use } from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

import { fetchDashboardOverview } from '@/services/content'
import { HttpError, toUserMessage } from '@/services/http'
import { usePageReveal } from '@/shared/composables/usePageReveal'
import { formatDateTimeToSecond } from '@/shared/datetime'
import type { DashboardOverview } from '@/shared/domain'
import { useSessionStore } from '@/shared/sessionStore'

const root = ref<HTMLElement | null>(null)
const visitChartRef = ref<HTMLDivElement | null>(null)
let visitChart: EChartsType | null = null
let visitChartObserver: ResizeObserver | null = null
const emptyOverview: DashboardOverview = {
  metrics: [],
  hotArticles: [],
  hotProjects: [],
  hotSearchKeywords: [],
  visitTrend: [],
  searchTrend: [],
  recentActivities: [],
}
const overview = ref<DashboardOverview>(emptyOverview)
const notice = ref('')
const router = useRouter()
const session = useSessionStore()

use([BarChart, GridComponent, TooltipComponent, CanvasRenderer])
usePageReveal(root)

const totalPv = computed(() => overview.value.visitTrend.reduce((sum, item) => sum + item.pv, 0))
const totalSearch = computed(() => overview.value.searchTrend.reduce((sum, item) => sum + item.pv, 0))

// 获取并渲染后台概览统计数据
// 向后端发起 API 请求获取总计访问、热门文章和热门作品等核心度量概览数据, 并渲染 ECharts 趋势图
async function loadOverview() {
  notice.value = ''
  try {
    overview.value = await fetchDashboardOverview()
    await nextTick()
    renderVisitChart()
  } catch (error) {
    overview.value = emptyOverview
    if (error instanceof HttpError && [401, 403].includes(error.status)) {
      session.logout()
      router.replace({ name: 'login', query: { redirect: '/admin' } })
      return
    }
    notice.value = `后台概览接口暂不可用：${toUserMessage(error, '请稍后再试')}`
  }
}

function formatDay(value: string) {
  if (value.includes('-')) {
    return value.slice(5)
  }
  return value
}

// 使用 ECharts 配置并绘制访问和搜索趋势柱状图
// 初始化并实例化 ECharts 图表, 绘制过去 7 日内的全站浏览量与搜索量的柱状趋势对比图
function renderVisitChart() {
  if (!visitChartRef.value) {
    return
  }
  visitChart ??= init(visitChartRef.value)
  const trend = overview.value.visitTrend
  const searchTrend = overview.value.searchTrend
  const values = trend.map((item) => item.pv)
  const searchValues = trend.map((item, index) => searchTrend[index]?.pv ?? 0)
  const maxValue = Math.max(...values, ...searchValues, 1)
  visitChart.setOption({
    color: ['#0f766e', '#f59e0b'],
    grid: { top: 24, right: 18, bottom: 34, left: 34 },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(17, 24, 39, 0.92)',
      borderWidth: 0,
      textStyle: { color: '#fff', fontSize: 12 },
      axisPointer: { type: 'shadow', shadowStyle: { color: 'rgba(49, 91, 255, 0.08)' } },
      formatter(params: unknown) {
        const points = params as Array<{ axisValue: string; seriesName: string; value: number }>
        return points
          .map((point, index) => `${index === 0 ? `${point.axisValue}<br/>` : ''}${point.seriesName}：${point.value} 次`)
          .join('<br/>')
      },
    },
    xAxis: {
      type: 'category',
      data: trend.map((item) => formatDay(item.date)),
      axisTick: { show: false },
      axisLine: { lineStyle: { color: 'rgba(15, 23, 42, 0.12)' } },
      axisLabel: { color: '#7b8495', fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      max: Math.max(1, Math.ceil(maxValue * 1.25)),
      splitLine: { lineStyle: { color: 'rgba(15, 23, 42, 0.08)', type: 'dashed' } },
      axisLabel: { color: '#8b93a3', fontSize: 11 },
    },
    series: [
      {
        name: '访问量',
        type: 'bar',
        data: values,
        barMaxWidth: 38,
        itemStyle: {
          borderRadius: [8, 8, 3, 3],
          color: new graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#2563eb' },
            { offset: 0.56, color: '#0f7c86' },
            { offset: 1, color: '#047857' },
          ]),
        },
        emphasis: {
          itemStyle: {
            color: new graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#38bdf8' },
              { offset: 1, color: '#0f766e' },
            ]),
          },
        },
      },
      {
        name: '搜索量',
        type: 'bar',
        data: searchValues,
        barMaxWidth: 38,
        itemStyle: {
          borderRadius: [8, 8, 3, 3],
          color: '#f59e0b',
        },
      },
    ],
  })
}
function resizeVisitChart() {
  visitChart?.resize()
}

watch(
  () => [overview.value.visitTrend, overview.value.searchTrend],
  () => {
    nextTick(renderVisitChart)
  },
  { deep: true },
)

// 生命周期挂载时初始化图表并绑定大小缩放监听器
onMounted(() => {
  loadOverview()
  window.addEventListener('resize', resizeVisitChart)
  if (visitChartRef.value) {
    visitChartObserver = new ResizeObserver(resizeVisitChart)
    visitChartObserver.observe(visitChartRef.value)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeVisitChart)
  visitChartObserver?.disconnect()
  visitChartObserver = null
  visitChart?.dispose()
  visitChart = null
})
</script>

<style scoped>
.admin-dashboard,
.admin-module {
  display: grid;
  gap: 18px;
}

.workspace-panel,
.metric-card {
  border: 1px solid var(--admin-line);
  border-radius: var(--app-radius-sm);
  background:
    linear-gradient(180deg, var(--admin-panel), var(--admin-panel-soft));
  color: var(--admin-ink);
  box-shadow: var(--admin-shadow);
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
  min-height: 112px;
  padding: 16px;
}

.metric-card span {
  color: var(--admin-muted);
  font-weight: 720;
}

.metric-card span,
.metric-card p,
.panel-title span,
.table-row span {
  color: var(--admin-muted);
  font-size: 13px;
}

.metric-card strong {
  display: block;
  margin-top: 12px;
  color: var(--admin-ink);
  font-size: 30px;
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
  padding: 16px;
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
  border: 1px solid rgba(17, 24, 39, 0.16);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.96);
  color: var(--admin-ink);
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
  background: rgba(255, 255, 255, 0.9);
  color: #344154;
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
  color: var(--admin-ink);
  font-size: 18px;
}

.table-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px;
  border-radius: 8px;
  background: var(--admin-panel-soft);
  color: var(--admin-ink);
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
  background: var(--admin-primary-soft);
  color: #173b87;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.icon-text-button {
  min-height: 30px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: var(--admin-primary);
  font: inherit;
  font-size: 13px;
  font-weight: 800;
  padding: 0 10px;
  text-decoration: none;
  cursor: pointer;
}

.icon-text-button:hover {
  background: var(--admin-primary-soft);
  color: var(--admin-primary-strong);
}

.icon-text-button.danger {
  color: var(--admin-danger);
}

.icon-text-button.danger:hover {
  background: var(--admin-danger-soft);
  color: #991b1b;
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

.visit-chart {
  width: 100%;
  height: 254px;
  min-height: 220px;
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
