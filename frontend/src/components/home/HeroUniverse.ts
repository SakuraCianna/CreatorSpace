import { defineComponent, h, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { gsap } from 'gsap'

import HeroWebGLScene from '@/components/visuals/HeroWebGLScene'
import { useGsapContext } from '@/composables/useGsapScroll'
import { heroContent } from '@/data/home-content'

/**
 * 渲染首页英雄区按钮。
 */
function heroButton(to: string, label: string, ghost = false) {
  return h(
    RouterLink,
    { to, class: ['cs-btn', { 'cs-btn--ghost': ghost }] },
    {
      default: () => [
        h('span', { class: 'cs-btn__fill' }),
        h('span', { class: 'cs-btn__dot' }),
        h('span', label),
      ],
    },
  )
}

export default defineComponent({
  name: 'HeroUniverse',
  setup() {
    const root = ref<HTMLElement | null>(null)
    useGsapContext(root, ({ reduced }) => {
      if (reduced) {
        return
      }
      const tl = gsap.timeline({ defaults: { ease: 'expo.out' }, delay: 0.15 })
      tl.from('.cs-hero__title .cs-line > span', {
        yPercent: 110,
        duration: 1.2,
        stagger: 0.12,
      })
        .from('.cs-hero__kicker', { y: 20, opacity: 0, duration: 0.8 }, '-=0.9')
        .from('.cs-hero__sub', { y: 24, opacity: 0, duration: 0.8 }, '-=0.7')
        .from('.cs-hero__actions > *', { y: 20, opacity: 0, duration: 0.7, stagger: 0.1 }, '-=0.6')
        .from('.cs-hero__stats .cs-stat', { y: 18, opacity: 0, duration: 0.7, stagger: 0.08 }, '-=0.5')
        .from('.cs-hero__scroll', { opacity: 0, duration: 0.6 }, '-=0.3')
    })

    /**
     * 渲染英雄标题行。
     */
    const titleLine = (text: string) =>
      h('span', { class: 'cs-line' }, [h('span', text)])

    return () =>
      h('section', { ref: root, class: 'cs-hero cs-section' }, [
        h(HeroWebGLScene),
        h('div', { class: 'cs-hero__grain' }),
        h('div', { class: 'cs-hero__inner' }, [
          h('p', { class: 'cs-eyebrow cs-hero__kicker' }, heroContent.kicker),
          h('h1', { class: 'cs-hero__title' }, [titleLine('CreatorSpace'), titleLine('AI')]),
          h('p', { class: 'cs-hero__sub' }, [
            heroContent.subtitle,
            h('span', { class: 'cs-zh' }, heroContent.description),
          ]),
          h('div', { class: 'cs-hero__actions' }, [
            heroButton(heroContent.primary.to, heroContent.primary.label),
            heroButton(heroContent.secondary.to, heroContent.secondary.label, true),
          ]),
          h(
            'div',
            { class: 'cs-hero__stats' },
            heroContent.stats.map((stat) =>
              h('div', { class: 'cs-stat', key: stat.label }, [
                h('div', { class: 'cs-stat__value' }, stat.value),
                h('div', { class: 'cs-stat__label' }, stat.label),
              ]),
            ),
          ),
        ]),
        h('div', { class: 'cs-hero__scroll' }, 'Scroll to explore'),
      ])
  },
})
