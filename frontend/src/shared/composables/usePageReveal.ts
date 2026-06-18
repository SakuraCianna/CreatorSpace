import { onBeforeUnmount, onMounted, type Ref } from 'vue'
import { animate, stagger, type JSAnimation } from 'animejs'

import { prefersReducedMotion } from '@/shared/composables/useReducedMotion'

// 用 anime.js 处理非主页页面的轻量入场，区别于主页的滚动叙事动画。
export function usePageReveal(root: Ref<HTMLElement | null | undefined>, selector = '[data-reveal]') {
  let animation: JSAnimation | null = null

  onMounted(() => {
    const scope = root.value
    if (!scope || prefersReducedMotion()) {
      return
    }
    const targets = Array.from(scope.querySelectorAll<HTMLElement>(selector))
    if (!targets.length) {
      return
    }
    animation = animate(targets, {
      opacity: [0, 1],
      translateY: [24, 0],
      duration: 760,
      delay: stagger(70),
      ease: 'outCubic',
    })
  })

  onBeforeUnmount(() => {
    animation?.pause()
    animation = null
  })
}
