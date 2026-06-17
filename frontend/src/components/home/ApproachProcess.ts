import { defineComponent, h, ref } from 'vue'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

import { useGsapContext } from '@/composables/useGsapScroll'
import { approachSteps, counters, type ApproachStep } from '@/data/home-content'

gsap.registerPlugin(ScrollTrigger)
export default defineComponent({
  name: 'ApproachProcess',
  setup() {
    const root = ref<HTMLElement | null>(null)
    const activeIndex = ref(0)

    useGsapContext(root, ({ reduced }) => {
      const steps = gsap.utils.toArray<HTMLElement>('.cs-step')
      steps.forEach((step, i) => {
        ScrollTrigger.create({
          trigger: step,
          start: 'top 60%',
          end: 'bottom 60%',
          onToggle: (self) => {
            if (self.isActive) {
              activeIndex.value = i
            }
          },
        })
        if (!reduced) {
          gsap.from(step, {
            opacity: 0,
            y: 40,
            duration: 0.7,
            ease: 'power3.out',
            scrollTrigger: { trigger: step, start: 'top 82%' },
          })
        }
      })
      gsap.utils.toArray<HTMLElement>('.cs-counter__value').forEach((el) => {
        const target = Number(el.dataset.target || '0')
        const suffix = el.dataset.suffix || ''
        if (reduced) {
          el.textContent = `${target}${suffix}`
          return
        }
        const obj = { v: 0 }
        gsap.to(obj, {
          v: target,
          duration: 1.8,
          ease: 'power2.out',
          scrollTrigger: { trigger: el, start: 'top 88%' },
          onUpdate: () => {
            el.textContent = `${Math.round(obj.v)}${suffix}`
          },
        })
      })
    })
    const renderStep = (step: ApproachStep) =>
      h('article', { key: step.no, class: 'cs-step' }, [
        h('div', { class: 'cs-step__head' }, [
          h('span', { class: 'cs-step__no' }, step.no),
          h('div', [
            h('h3', { class: 'cs-step__title' }, [
              step.title,
              h('span', { class: 'cs-step__en' }, step.en),
            ]),
          ]),
        ]),
        h('p', { class: 'cs-step__body' }, step.body),
        h(
          'div',
          { class: 'cs-step__tags' },
          step.tags.map((t) => h('span', { key: t, class: 'cs-tag-line' }, t)),
        ),
      ])

    return () =>
      h('section', { ref: root, class: 'cs-approach cs-section', id: 'approach' }, [
        h('div', { class: 'cs-head' }, [
          h('div', [
            h('p', { class: 'cs-eyebrow' }, 'How It Works'),
            h('h2', { class: 'cs-head__title' }, '从碎片到作品的四步'),
          ]),
          h(
            'p',
            { class: 'cs-head__note' },
            'A creative pipeline — capture, connect, compose, curate. 系统让创作有迹可循。',
          ),
        ]),
        h('div', { class: 'cs-approach__grid' }, [
          h('aside', { class: 'cs-rail' }, [
            h('div', { class: 'cs-rail__num' }, approachSteps[activeIndex.value]?.no ?? '01'),
            h('div', { class: 'cs-rail__label' }, approachSteps[activeIndex.value]?.en ?? ''),
            h(
              'div',
              { class: 'cs-rail__track' },
              approachSteps.map((s, i) =>
                h('span', {
                  key: s.no,
                  class: ['cs-rail__tick', { 'is-active': i === activeIndex.value }],
                }),
              ),
            ),
          ]),
          h('div', { class: 'cs-steps' }, approachSteps.map(renderStep)),
        ]),
        h(
          'div',
          { class: 'cs-counters' },
          counters.map((c) =>
            h('div', { class: 'cs-counter', key: c.label }, [
              h(
                'span',
                {
                  class: 'cs-counter__value',
                  'data-target': String(c.value),
                  'data-suffix': c.suffix,
                },
                `0${c.suffix}`,
              ),
              h('span', { class: 'cs-counter__label' }, c.label),
            ]),
          ),
        ),
      ])
  },
})
