import { gsap } from 'gsap'

import { prefersReducedMotion } from '@/shared/composables/useReducedMotion'

interface MagneticOptions {
  strength?: number
  inner?: HTMLElement | null
  innerStrength?: number
}

// 给元素绑定磁吸跟随效果，并返回清理函数。
export function attachMagnetic(el: HTMLElement, options: MagneticOptions = {}) {
  const coarse =
    typeof window !== 'undefined' &&
    window.matchMedia &&
    window.matchMedia('(pointer: coarse)').matches

  if (prefersReducedMotion() || coarse) {
    return () => {}
  }

  const strength = options.strength ?? 0.32
  const innerStrength = options.innerStrength ?? 0.55
  const setX = gsap.quickTo(el, 'x', { duration: 0.6, ease: 'power3.out' })
  const setY = gsap.quickTo(el, 'y', { duration: 0.6, ease: 'power3.out' })

  const inner = options.inner ?? null
  const setIX = inner ? gsap.quickTo(inner, 'x', { duration: 0.7, ease: 'power3.out' }) : null
  const setIY = inner ? gsap.quickTo(inner, 'y', { duration: 0.7, ease: 'power3.out' }) : null

  // 根据指针位置移动卡片和内部元素。
  const onMove = (event: PointerEvent) => {
    const rect = el.getBoundingClientRect()
    const relX = event.clientX - (rect.left + rect.width / 2)
    const relY = event.clientY - (rect.top + rect.height / 2)
    setX(relX * strength)
    setY(relY * strength)
    setIX?.(relX * innerStrength)
    setIY?.(relY * innerStrength)
  }

  // 指针离开后复位磁吸偏移。
  const onLeave = () => {
    setX(0)
    setY(0)
    setIX?.(0)
    setIY?.(0)
  }

  el.addEventListener('pointermove', onMove)
  el.addEventListener('pointerleave', onLeave)

  // 解除监听并清理 GSAP 动画。
  return () => {
    el.removeEventListener('pointermove', onMove)
    el.removeEventListener('pointerleave', onLeave)
    gsap.killTweensOf(el)
    if (inner) {
      gsap.killTweensOf(inner)
    }
  }
}
