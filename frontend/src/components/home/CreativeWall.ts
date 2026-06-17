import { defineComponent, h, onBeforeUnmount, ref } from 'vue'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

import { useGsapContext } from '@/composables/useGsapScroll'
import { attachMagnetic } from '@/composables/useMagnetic'
import { creativeFragments, type CreativeFragment } from '@/data/home-content'

gsap.registerPlugin(ScrollTrigger)
export default defineComponent({
  name: 'CreativeWall',
  setup() {
    const root = ref<HTMLElement | null>(null)
    const detachers: Array<() => void> = []

    useGsapContext(root, ({ reduced }) => {
      const cards = gsap.utils.toArray<HTMLElement>('.cs-frag')

      if (!reduced) {
        gsap.from(cards, {
          opacity: 0,
          scale: 0.86,
          filter: 'blur(8px)',
          duration: 0.8,
          ease: 'power3.out',
          stagger: { each: 0.06, from: 'center', grid: 'auto' },
          scrollTrigger: { trigger: root.value as Element, start: 'top 72%' },
        })
      }
      cards.forEach((card) => {
        const inner = card.querySelector<HTMLElement>('.cs-frag__inner')
        detachers.push(attachMagnetic(card, { inner, strength: 0.22, innerStrength: 0.4 }))
      })
    })

    onBeforeUnmount(() => {
      detachers.forEach((off) => off())
      detachers.length = 0
    })

    /**
     * 渲染创意墙卡片。
     */
    const renderFragment = (fragment: CreativeFragment) => {
      const style: Record<string, string> = {
        gridColumn: `span ${fragment.span.col}`,
        gridRow: `span ${fragment.span.row}`,
      }
      if (fragment.palette) {
        style['--cs-from'] = fragment.palette[0]
        style['--cs-to'] = fragment.palette[1]
      }

      const inner: ReturnType<typeof h>[] = [
        h('span', { class: 'cs-frag__kind' }, fragment.label),
        h('p', { class: 'cs-frag__body' }, fragment.body),
      ]
      if (fragment.kind === 'link') {
        inner.push(h('span', { class: 'cs-frag__link' }, '↗ open'))
      } else if (fragment.meta) {
        inner.push(h('span', { class: 'cs-frag__meta' }, fragment.meta))
      }

      return h(
        'div',
        { key: fragment.id, class: `cs-frag cs-frag--${fragment.kind}`, style },
        [
          fragment.palette ? h('span', { class: 'cs-frag__wash' }) : null,
          h('div', { class: 'cs-frag__inner' }, inner),
        ],
      )
    }

    return () =>
      h('section', { ref: root, class: 'cs-wall cs-section', id: 'wall' }, [
        h('div', { class: 'cs-head' }, [
          h('div', [
            h('p', { class: 'cs-eyebrow' }, 'Creative Wall'),
            h('h2', { class: 'cs-head__title' }, '灵感碎片，尚未成文的思考'),
          ]),
          h(
            'p',
            { class: 'cs-head__note' },
            'Prompts、片段、笔记与参考——创作开始之前的原材料。',
          ),
        ]),
        h('div', { class: 'cs-wall__grid' }, creativeFragments.map(renderFragment)),
      ])
  },
})
