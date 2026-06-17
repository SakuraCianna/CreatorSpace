import { defineComponent, h, onBeforeUnmount, onMounted, ref } from 'vue'

import type { HeroSceneHandles } from '@/components/visuals/heroScene'
import { prefersReducedMotion } from '@/composables/useReducedMotion'
export default defineComponent({
  name: 'HeroWebGLScene',
  setup() {
    const host = ref<HTMLElement | null>(null)
    const isStatic = ref(false)
    let handles: HeroSceneHandles | null = null
    let io: IntersectionObserver | null = null
    let cancelled = false
    const onPointerMove = (event: PointerEvent) => {
      if (!handles) {
        return
      }
      const nx = (event.clientX / window.innerWidth) * 2 - 1
      const ny = -((event.clientY / window.innerHeight) * 2 - 1)
      handles.setPointer(nx, ny)
    }

    onMounted(() => {
      const el = host.value
      if (!el) {
        return
      }
      const probe = document.createElement('canvas')
      const hasWebGL = !!(probe.getContext('webgl2') || probe.getContext('webgl'))
      if (prefersReducedMotion() || !hasWebGL) {
        isStatic.value = true
        return
      }
      import('@/components/visuals/heroScene')
        .then(({ createHeroScene }) => {
          if (cancelled || !host.value) {
            return
          }
          handles = createHeroScene(host.value)
          window.addEventListener('pointermove', onPointerMove, { passive: true })
          io = new IntersectionObserver(
            (entries) => {
              const entry = entries[0]
              if (entry) {
                handles?.setPaused(!entry.isIntersecting)
              }
            },
            { threshold: 0 },
          )
          io.observe(host.value)
        })
        .catch(() => {
          isStatic.value = true
        })
    })

    onBeforeUnmount(() => {
      cancelled = true
      window.removeEventListener('pointermove', onPointerMove)
      io?.disconnect()
      io = null
      handles?.dispose()
      handles = null
    })

    return () =>
      h('div', {
        ref: host,
        class: ['cs-hero__canvas', { 'cs-hero__canvas--static': isStatic.value }],
        'aria-hidden': 'true',
      })
  },
})
