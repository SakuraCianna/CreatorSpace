import { defineComponent, h } from 'vue'
import { RouterLink } from 'vue-router'

import { articleSamples, privacyLabels } from '@/data/sample-content'

export default defineComponent({
  name: 'HomeView',
  setup() {
    return () =>
      h('section', { class: 'home-hero' }, [
        h('div', { class: 'home-copy' }, [
          h('h1', '把博客、作品和灵感整理成一个真正的内容系统'),
          h(
            'p',
            'CreatorSpace 会用一个 Vue 前端承载前台展示和后台 CMS，先把路由、设计语言、权限规则和业务入口搭好。',
          ),
          h('div', { class: 'button-row' }, [
            h(RouterLink, { class: 'button button-filled', to: '/articles' }, { default: () => '查看文章' }),
            h(RouterLink, { class: 'button button-tonal', to: '/admin' }, { default: () => '进入后台' }),
          ]),
        ]),
        h('div', { class: 'preview-panel', 'aria-label': '内容系统预览' }, [
          h('div', { class: 'panel-header' }, [h('span', '今日内容'), h('strong', '3 条')]),
          ...articleSamples.map((article) =>
            h('article', { key: article.id, class: 'content-row' }, [
              h('div', [
                h('h2', article.title),
                h('p', `${article.updatedAt} · ${privacyLabels[article.privacy]}`),
              ]),
              h('span', { class: 'status-chip' }, article.status),
            ]),
          ),
        ]),
      ])
  },
})
