import { onBeforeUnmount, onMounted, type Ref } from 'vue'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

import { prefersReducedMotion } from '@/shared/composables/useReducedMotion'

gsap.registerPlugin(ScrollTrigger)

type ScopeEl = Ref<HTMLElement | null | undefined>

// 在组件挂载后创建 GSAP 上下文, 并在卸载时统一回收动画
// 动态加载并挂载 GSAP 滚动视差控制器, 监听滚动事件更新视口图形
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
