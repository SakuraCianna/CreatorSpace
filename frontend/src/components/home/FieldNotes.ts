import { defineComponent, h, ref } from 'vue'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

import { useGsapContext } from '@/composables/useGsapScroll'
import { fieldNotes, type FieldNote } from '@/data/home-content'

gsap.registerPlugin(ScrollTrigger)
export default defineComponent({
  name: 'FieldNotes',
  setup() {
    const root = ref<HTMLElement | null>(null)

    useGsapContext(root, ({ reduced }) => {
      if (reduced) {
        return
      }
      gsap.utils.toArray<HTMLElement>('.cs-note').forEach((row, i) => {
        gsap.from(row, {
          yPercent: 40,
          opacity: 0,
          duration: 0.7,
          ease: 'power3.out',
          delay: (i % 4) * 0.05,
          scrollTrigger: { trigger: row, start: 'top 88%' },
        })
        const rule = row.querySelector('.cs-note__rule')
        if (rule) {
          gsap.from(rule, {
            scaleX: 0,
            transformOrigin: 'left',
            duration: 0.9,
            ease: 'power3.inOut',
            scrollTrigger: { trigger: row, start: 'top 88%' },
          })
        }
      })
    })
    const renderNote = (note: FieldNote, i: number) =>
      h('article', { key: i, class: 'cs-note' }, [
        h('span', { class: 'cs-note__rule' }),
        h('div', { class: 'cs-note__row' }, [
          h('span', { class: 'cs-note__date' }, note.date),
          h('span', { class: 'cs-note__tag' }, note.tag),
          h('h3', { class: 'cs-note__title' }, note.title),
          h('span', { class: 'cs-note__arrow', 'aria-hidden': 'true' }, '→'),
        ]),
        h('p', { class: 'cs-note__detail' }, note.detail),
      ])

    return () =>
      h('section', { ref: root, class: 'cs-notes cs-section', id: 'notes' }, [
        h('div', { class: 'cs-head' }, [
          h('div', [
            h('p', { class: 'cs-eyebrow' }, 'Field Notes / Changelog'),
            h('h2', { class: 'cs-head__title' }, '一个持续生长的空间'),
          ]),
          h(
            'p',
            { class: 'cs-head__note' },
            'Recent moves — 这个创作宇宙在记录它自己的演化。',
          ),
        ]),
        h('div', { class: 'cs-notes__list' }, fieldNotes.map(renderNote)),
      ])
  },
})
