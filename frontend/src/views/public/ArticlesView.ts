import { defineComponent, h } from 'vue'

import { articleSamples, privacyLabels } from '@/data/sample-content'

export default defineComponent({
  name: 'ArticlesView',
  setup() {
    return () =>
      h('section', { class: 'page-section' }, [
        h('div', { class: 'section-heading' }, [
          h('h1', '文章'),
          h('p', '前台列表后续会接入分类、标签、搜索和私密文章访问控制。'),
        ]),
        h(
          'div',
          { class: 'list-surface' },
          articleSamples.map((article) =>
            h('article', { key: article.id, class: 'list-row' }, [
              h('div', [
                h('h2', article.title),
                h('p', `${article.slug} · ${privacyLabels[article.privacy]}`),
              ]),
              h('span', { class: 'status-chip' }, article.status),
            ]),
          ),
        ),
      ])
  },
})
