import { defineComponent, h } from 'vue'
import { RouterLink } from 'vue-router'

import { siteConfig } from '@/data/home-content'
export default defineComponent({
  name: 'ImmersiveLayout',
  setup(_, { slots }) {
    return () =>
      h('div', { class: 'cs-shell' }, [
        h('nav', { class: 'cs-nav', 'aria-label': '主导航' }, [
          h(
            RouterLink,
            { to: '/', class: 'cs-nav__brand' },
            {
              default: () => [
                h('span', { class: 'cs-nav__mark' }, 'CS'),
                h('span', siteConfig.brand),
              ],
            },
          ),
          h(
            'div',
            { class: 'cs-nav__links' },
            siteConfig.navigation.map((item) =>
              h(RouterLink, { key: item.to, to: item.to }, { default: () => item.label }),
            ),
          ),
        ]),
        h('main', slots.default?.()),
      ])
  },
})
