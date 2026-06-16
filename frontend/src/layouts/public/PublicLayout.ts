import { defineComponent, h } from 'vue'
import { RouterLink } from 'vue-router'

function navLink(to: string, label: string) {
  return h(RouterLink, { to }, { default: () => label })
}

export default defineComponent({
  name: 'PublicLayout',
  setup(_, { slots }) {
    return () =>
      h('div', { class: 'public-shell' }, [
        h('header', { class: 'public-header' }, [
          h(
            RouterLink,
            { class: 'brand', to: '/' },
            {
              default: () => [
                h('span', { class: 'brand-mark' }, 'CS'),
                h('span', 'CreatorSpace'),
              ],
            },
          ),
          h('nav', { class: 'public-nav', 'aria-label': '前台导航' }, [
            navLink('/articles', '文章'),
            navLink('/projects', '作品'),
            navLink('/register', '注册'),
            navLink('/admin', '后台'),
          ]),
        ]),
        h('main', { class: 'public-main' }, slots.default?.()),
      ])
  },
})
