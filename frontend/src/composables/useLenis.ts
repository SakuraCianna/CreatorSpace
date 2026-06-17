import { onBeforeUnmount, onMounted, shallowRef } from 'vue'
import Lenis from 'lenis'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

import { prefersReducedMotion } from '@/composables/useReducedMotion'

gsap.registerPlugin(ScrollTrigger)
export function useLenis() {
  const lenis = shallowRef<Lenis | null>(null)

  onMounted(() => {
    if (prefersReducedMotion()) {
      ScrollTrigger.refresh()
      return
    }

    const instance = new Lenis({
      duration: 1.05,
      easing: (t: number) => Math.min(1, 1.001 - Math.pow(2, -10 * t)),
      smoothWheel: true,
    })
    lenis.value = instance

    instance.on('scroll', ScrollTrigger.update)
    const onTick = (time: number) => {
      instance.raf(time * 1000)
    }
    gsap.ticker.add(onTick)
    gsap.ticker.lagSmoothing(0)
    onBeforeUnmount(() => {
      gsap.ticker.remove(onTick)
      instance.off('scroll', ScrollTrigger.update)
      instance.destroy()
      lenis.value = null
    })
    if (typeof document !== 'undefined' && 'fonts' in document) {
      document.fonts.ready.then(() => ScrollTrigger.refresh()).catch(() => {})
    }

    ScrollTrigger.refresh()
  })

  return { lenis }
}
