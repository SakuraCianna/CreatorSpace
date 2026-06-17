import { defineComponent, h, onBeforeUnmount, onMounted, ref } from 'vue'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

import { portfolioProjects, type PortfolioProject } from '@/data/home-content'

gsap.registerPlugin(ScrollTrigger)
export default defineComponent({
  name: 'PortfolioGallery',
  setup() {
    const root = ref<HTMLElement | null>(null)
    const stacked = ref(isStackedViewport())
    const stackQuery =
      typeof window !== 'undefined' ? window.matchMedia('(max-width: 900px)') : null
    let mm: gsap.MatchMedia | null = null

    /**
     * 判断当前是否应使用堆叠布局。
     */
    function isStackedViewport(): boolean {
      if (typeof window === 'undefined') {
        return false
      }
      return window.matchMedia('(max-width: 900px)').matches
    }

    /**
     * 响应堆叠布局媒体查询变化。
     */
    const onStackChange = (event: MediaQueryListEvent) => {
      stacked.value = event.matches
    }

    onMounted(() => {
      stackQuery?.addEventListener('change', onStackChange)
      mm = gsap.matchMedia()
      mm.add(
        '(min-width: 901px) and (prefers-reduced-motion: no-preference)',
        () => {
          const pin = root.value?.querySelector<HTMLElement>('.cs-gallery__pin')
          const track = root.value?.querySelector<HTMLElement>('.cs-gallery__track')
          if (!pin || !track) {
            return
          }
          const getScrollLength = () => Math.max(0, track.scrollWidth - window.innerWidth)

          const tween = gsap.to(track, {
            x: () => -getScrollLength(),
            ease: 'none',
            scrollTrigger: {
              trigger: pin,
              start: 'top top',
              end: () => `+=${getScrollLength()}`,
              pin: true,
              scrub: 0.6,
              invalidateOnRefresh: true,
              anticipatePin: 1,
            },
          })
          gsap.utils.toArray<HTMLElement>('.cs-poster').forEach((poster) => {
            const layer = poster.querySelector('.cs-poster__parallax')
            if (!layer) {
              return
            }
            gsap.fromTo(
              layer,
              { xPercent: -8 },
              {
                xPercent: 8,
                ease: 'none',
                scrollTrigger: {
                  trigger: poster,
                  containerAnimation: tween,
                  start: 'left right',
                  end: 'right left',
                  scrub: true,
                },
              },
            )
          })
        },
        root.value ?? undefined,
      )
    })

    onBeforeUnmount(() => {
      stackQuery?.removeEventListener('change', onStackChange)
      mm?.revert()
      mm = null
    })

    /**
     * 渲染作品海报卡片。
     */
    const renderPoster = (project: PortfolioProject) =>
      h(
        'article',
        {
          key: project.id,
          class: 'cs-poster',
          style: { '--cs-from': project.palette.from, '--cs-to': project.palette.to, '--cs-accent': project.palette.accent },
        },
        [
          h('span', { class: 'cs-poster__parallax', style: { '--cs-accent': project.palette.accent } }),
          h('span', { class: 'cs-poster__grain' }),
          h('div', { class: 'cs-poster__content' }, [
            h('div', { class: 'cs-poster__top' }, [
              h('span', { class: 'cs-poster__index' }, `${project.index} / ${project.year}`),
            ]),
            h('div', [
              h('span', { class: 'cs-poster__cat' }, project.category),
              h('h3', { class: 'cs-poster__title' }, project.title),
              h('p', { class: 'cs-poster__desc' }, project.description),
              h(
                'div',
                { class: 'cs-poster__stack' },
                project.stack.map((tech) => h('span', { key: tech }, tech)),
              ),
            ]),
          ]),
        ],
      )

    return () =>
      h(
        'section',
        {
          ref: root,
          id: 'works',
          class: ['cs-gallery', { 'cs-gallery--stacked': stacked.value }],
        },
        [
          h('div', { class: 'cs-gallery__pin' }, [
            h('div', { class: 'cs-gallery__track' }, [
              h('div', { class: 'cs-gallery__intro' }, [
                h('p', { class: 'cs-eyebrow' }, 'Selected Works'),
                h('h2', '每个项目，都是一次学习与表达的轨迹'),
                h(
                  'p',
                  'A digital exhibition of full-stack platforms, AI tooling and creative front-end experiments.',
                ),
                h('span', { class: 'cs-gallery__hint' }, stacked.value ? 'Scroll ↓' : 'Scroll to walk the gallery →'),
              ]),
              ...portfolioProjects.map(renderPoster),
              h('div', { class: 'cs-gallery__outro' }, [
                h('p', { class: 'cs-eyebrow' }, 'End of wall'),
                h('h2', { style: { fontFamily: 'var(--cs-font-display)', fontSize: '34px', marginTop: '14px' } }, '更多作品正在归档中'),
              ]),
            ]),
          ]),
        ],
      )
  },
})
