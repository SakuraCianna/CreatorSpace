import { defineComponent, h } from 'vue'

import { adminMetrics, articleSamples, privacyLabels } from '@/data/sample-content'

export default defineComponent({
  name: 'AdminDashboardView',
  setup() {
    return () => [
      h(
        'section',
        { class: 'dashboard-grid' },
        adminMetrics.map((metric) =>
          h('article', { key: metric.label, class: 'metric-card' }, [
            h('span', metric.label),
            h('strong', metric.value),
            h('p', metric.trend),
          ]),
        ),
      ),
      h('section', { class: 'workspace-grid' }, [
        h('div', { class: 'workspace-panel' }, [
          h('div', { class: 'panel-title' }, [h('h2', '最近文章'), h('span', '隐私规则已对齐')]),
          ...articleSamples.map((article) =>
            h('article', { key: article.id, class: 'table-row' }, [
              h('div', [
                h('strong', article.title),
                h('span', `${privacyLabels[article.privacy]} · ${article.updatedAt}`),
              ]),
              h('span', { class: 'status-chip' }, article.status),
            ]),
          ),
        ]),
        h('div', { class: 'workspace-panel' }, [
          h('div', { class: 'panel-title' }, [h('h2', '互动规则'), h('span', '游客只读')]),
          h('ul', { class: 'rule-list' }, [
            h('li', '评论、点赞、收藏都要求登录用户身份。'),
            h('li', '评论系统按完整多级回复设计。'),
            h('li', '删除内容时清理未被引用的文件资源。'),
            h('li', '敏感词表和 pgvector 能力已在数据库层预留。'),
          ]),
        ]),
      ]),
    ]
  },
})
