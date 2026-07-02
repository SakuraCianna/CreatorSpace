import { nextTick, onBeforeUnmount, type Ref } from 'vue'
import { gsap } from 'gsap'

import { prefersReducedMotion } from './useReducedMotion'

type ScopeEl = Ref<HTMLElement | null | undefined>

// 给非首页公开页面补一层电影式显影, 和首页 GSAP 叙事保持同一动效气质
// 页面无缝电影开场转场特效, 注入淡入与微型上移浮现动画
export function useCinematicPageMotion(root: ScopeEl) {
  let context: gsap.Context | null = null

  const play = async () => {
    await nextTick()
    const scope = root.value
    if (!scope || prefersReducedMotion()) {
      return
    }

    context?.revert()
    context = gsap.context(() => {
      const hero = scope.querySelector<HTMLElement>('.archive-hero, .about-hero, .detail-hero')
      if (hero) {
        gsap.fromTo(
          hero,
          { clipPath: 'inset(0 0 18% 0)', filter: 'saturate(0.82) brightness(0.95)' },
          { clipPath: 'inset(0 0 0% 0)', filter: 'saturate(1) brightness(1)', duration: 0.9, ease: 'power3.out' },
        )
      }

      const panels = Array.from(
        scope.querySelectorAll<HTMLElement>(
          '.detail-panel, .reading-aside > *, .process-panel, .inspiration-card, .about-panel, .skills-band',
        ),
      )
      if (panels.length > 0) {
        gsap.fromTo(
          panels,
          { opacity: 0, y: 28, scale: 0.985 },
          { opacity: 1, y: 0, scale: 1, duration: 0.72, ease: 'power3.out', stagger: 0.045 },
        )
      }

      const markdownBlocks = Array.from(scope.querySelectorAll<HTMLElement>('.markdown-body > *'))
      if (markdownBlocks.length > 0) {
        gsap.fromTo(
          markdownBlocks,
          { opacity: 0, y: 16 },
          { opacity: 1, y: 0, duration: 0.52, ease: 'power2.out', stagger: 0.025, delay: 0.08 },
        )
      }
    }, scope)
  }

  onBeforeUnmount(() => {
    // We do not call context?.revert() here because it strips inline styles instantly,
    // which causes a harsh flicker during Vue's out-in page transitions.
    // The DOM nodes will be destroyed by Vue anyway.
    context = null
  })

  return { play }
}
