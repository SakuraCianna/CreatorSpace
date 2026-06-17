import { onBeforeUnmount, onMounted, type Ref } from 'vue'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

import { prefersReducedMotion } from '@/composables/useReducedMotion'

gsap.registerPlugin(ScrollTrigger)

type ScopeEl = Ref<HTMLElement | null | undefined>
export function useGsapContext(
  scope: ScopeEl,
  setup: (ctx: { reduced: boolean }) => void,
) {
  let ctx: gsap.Context | null = null

  onMounted(() => {
    const root = scope.value
    if (!root) {
      return
    }
    const reduced = prefersReducedMotion()
    ctx = gsap.context(() => setup({ reduced }), root)
  })

  onBeforeUnmount(() => {
    ctx?.revert()
    ctx = null
  })
}
