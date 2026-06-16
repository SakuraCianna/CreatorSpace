import { defineComponent, h } from 'vue'
import { RouterLink } from 'vue-router'

const navItems = [
  { to: '/admin', label: '概览' },
  { to: '/admin/articles', label: '文章' },
  { to: '/admin/projects', label: '作品' },
  { to: '/admin/comments', label: '评论' },
  { to: '/admin/files', label: '文件' },
  { to: '/admin/content-rules', label: '规则' },
  { to: '/admin/settings', label: '设置' },
]

export default defineComponent({
  name: 'AdminLayout',
  setup(_, { slots }) {
    return () =>
      h('div', { class: 'admin-shell' }, [
        h('aside', { class: 'admin-rail', 'aria-label': '后台导航' }, [
          h(
            RouterLink,
            { class: 'admin-brand', to: '/admin' },
            {
              default: () => [
                h('span', { class: 'brand-mark' }, 'CS'),
                h('span', 'CreatorSpace CMS'),
              ],
            },
          ),
          h(
            'nav',
            { class: 'admin-nav' },
            navItems.map((item) =>
              h(RouterLink, { key: item.to, to: item.to }, { default: () => item.label }),
            ),
          ),
        ]),
        h('section', { class: 'admin-workspace' }, [
          h('header', { class: 'admin-topbar' }, [
            h('div', [h('p', { class: 'eyebrow' }, 'Material 3 CMS'), h('h1', '内容运营工作台')]),
            h(RouterLink, { class: 'text-link', to: '/' }, { default: () => '返回前台' }),
          ]),
          h('main', { class: 'admin-main' }, slots.default?.()),
        ]),
      ])
  },
})
