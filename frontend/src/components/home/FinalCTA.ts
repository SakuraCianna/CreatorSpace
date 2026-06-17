import { defineComponent, h, onBeforeUnmount, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { gsap } from 'gsap'

import { useGsapContext } from '@/composables/useGsapScroll'
import { prefersReducedMotion } from '@/composables/useReducedMotion'
import { siteConfig } from '@/data/home-content'
const CLOSING_LINE = 'Enter the universe.'

export default defineComponent({
  name: 'FinalCTA',
  setup() {
    const root = ref<HTMLElement | null>(null)
    const glow = ref<HTMLElement | null>(null)
    let glowX: gsap.QuickToFunc | null = null
    let glowY: gsap.QuickToFunc | null = null

    useGsapContext(root, ({ reduced }) => {
      if (reduced) {
        return
      }
      gsap.from('.cs-cta__line .cs-char', {
        yPercent: 120,
        opacity: 0,
        duration: 0.8,
        ease: 'expo.out',
        stagger: 0.02,
        scrollTrigger: { trigger: root.value as Element, start: 'top 75%' },
      })
      if (glow.value) {
        glowX = gsap.quickTo(glow.value, 'x', { duration: 0.8, ease: 'power3.out' })
        glowY = gsap.quickTo(glow.value, 'y', { duration: 0.8, ease: 'power3.out' })
      }
    })

    /**
     * 根据指针位置移动光晕。
     */
    const onMove = (event: PointerEvent) => {
      const el = root.value
      if (!el || !glowX || !glowY) {
        return
      }
      const rect = el.getBoundingClientRect()
      glowX(event.clientX - rect.left - rect.width / 2)
      glowY(event.clientY - rect.top - rect.height / 2)
    }
    /**
     * 处理单字悬停动效。
     */
    const onCharOver = (event: PointerEvent) => {
      if (prefersReducedMotion()) {
        return
      }
      const target = event.target as HTMLElement
      if (target.classList.contains('cs-char')) {
        gsap.to(target, { yPercent: -18, color: 'var(--cs-accent-3)', duration: 0.3, ease: 'power2.out' })
        gsap.to(target, { yPercent: 0, color: 'inherit', duration: 0.6, delay: 0.15, ease: 'power3.out' })
      }
    }

    onBeforeUnmount(() => {
      if (glow.value) {
        gsap.killTweensOf(glow.value)
      }
    })

    /**
     * 拆分并渲染行动标题字符。
     */
    const chars = () =>
      CLOSING_LINE.split('').map((ch, i) =>
        ch === ' '
          ? h('span', { key: i, class: 'cs-char', innerHTML: '&nbsp;' })
          : h('span', { key: i, class: 'cs-char' }, ch),
      )

    return () =>
      h('section', { ref: root, class: 'cs-cta-wrap', id: 'enter', onPointermove: onMove }, [
        h('div', { class: 'cs-cta' }, [
          h('div', { ref: glow, class: 'cs-cta__glow' }),
          h('p', { class: 'cs-eyebrow cs-cta__eyebrow' }, 'The Exit / Or The Beginning'),
          h('h2', { class: 'cs-cta__line', onPointerover: onCharOver }, chars()),
          h(
            'p',
            { class: 'cs-cta__sub' },
            '用系统记录思考，用作品证明成长。欢迎来到我的个人创作宇宙。',
          ),
          h('div', { class: 'cs-cta__action' }, [
            h(
              RouterLink,
              { to: '/articles', class: 'cs-btn' },
              {
                default: () => [
                  h('span', { class: 'cs-btn__fill' }),
                  h('span', { class: 'cs-btn__dot' }),
                  h('span', 'Start Exploring'),
                ],
              },
            ),
          ]),
        ]),
        renderFooter(),
      ])
  },
})

/**
 * 渲染首页页脚。
 */
function renderFooter() {
  return h('footer', { class: 'cs-footer' }, [
    h('span', `© ${new Date().getFullYear()} ${siteConfig.wordmark}`),
    h(
      'div',
      { class: 'cs-footer__social' },
      siteConfig.social.map((item) =>
        h('span', { key: item.label }, `${item.label} · ${item.handle}`),
      ),
    ),
    h('span', 'Built with Vue · Three.js · GSAP'),
  ])
}
