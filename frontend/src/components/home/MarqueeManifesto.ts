import { defineComponent, h, ref } from 'vue'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

import { useGsapContext } from '@/composables/useGsapScroll'
import { manifesto, marqueeWords } from '@/data/home-content'

gsap.registerPlugin(ScrollTrigger)
export default defineComponent({
  name: 'MarqueeManifesto',
  setup() {
    const root = ref<HTMLElement | null>(null)

    useGsapContext(root, ({ reduced }) => {
      if (!reduced) {
        gsap.from('.cs-mani__word', {
          opacity: 0.12,
          duration: 0.6,
          ease: 'none',
          stagger: 0.04,
          scrollTrigger: {
            trigger: '.cs-mani',
            start: 'top 80%',
            end: 'top 30%',
            scrub: true,
          },
        })
      }

      const rows = gsap.utils.toArray<HTMLElement>('.cs-marquee__row')
      rows.forEach((row, i) => {
        const inner = row.querySelector<HTMLElement>('.cs-marquee__inner')
        if (!inner) {
          return
        }
        const dir = i % 2 === 0 ? -1 : 1

        if (reduced) {
          gsap.set(inner, { xPercent: -25 })
          return
        }
        const wrap = gsap.utils.wrap(-50, 0)
        const from = dir < 0 ? 0 : -50
        const to = dir < 0 ? -50 : 0
        const baseTween = gsap.fromTo(
          inner,
          { xPercent: from },
          {
            xPercent: to,
            duration: 28,
            ease: 'none',
            repeat: -1,
            modifiers: {
              xPercent: (value) => `${wrap(parseFloat(value))}%`,
            },
          },
        )
        ScrollTrigger.create({
          trigger: root.value as Element,
          start: 'top bottom',
          end: 'bottom top',
          onUpdate: (self) => {
            const v = self.getVelocity()
            const scale = 1 + Math.min(Math.abs(v) / 1200, 3)
            baseTween.timeScale(dir * v < 0 ? scale * 0.4 : scale)
          },
        })
      })
    })

    /**
     * 渲染无缝滚动标语行。
     */
    const marqueeRow = (reverse: boolean) => {
      const items = [...marqueeWords, ...marqueeWords].map((word, i) =>
        h('span', { key: `${word}-${i}`, class: 'cs-marquee__item' }, [
          word,
          h('i', { class: 'cs-marquee__star', 'aria-hidden': 'true' }, '✦'),
        ]),
      )
      return h('div', { class: ['cs-marquee__row', { 'is-reverse': reverse }] }, [
        h('div', { class: 'cs-marquee__inner' }, items),
      ])
    }

    return () =>
      h('section', { ref: root, class: 'cs-marquee-sec' }, [
        h('div', { class: 'cs-marquee' }, [marqueeRow(false), marqueeRow(true)]),
        h('div', { class: 'cs-mani cs-section' }, [
          h('p', { class: 'cs-eyebrow' }, manifesto.lead),
          h(
            'p',
            { class: 'cs-mani__text' },
            manifesto.segments.flatMap((seg) =>
              seg.text.split(' ').filter(Boolean).map((word, wi) =>
                h(
                  'span',
                  {
                    key: `${word}-${wi}-${seg.accent}`,
                    class: ['cs-mani__word', { 'is-accent': seg.accent }],
                  },
                  `${word} `,
                ),
              ),
            ),
          ),
        ]),
      ])
  },
})
