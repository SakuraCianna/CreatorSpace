import { onBeforeUnmount, ref } from 'vue'

// 监听系统低动效偏好, 并在组件卸载时释放监听器
export function useReducedMotion() {
  const reduced = ref(false)

  if (typeof window === 'undefined' || !window.matchMedia) {
    return { reduced }
  }

  const query = window.matchMedia('(prefers-reduced-motion: reduce)')
  reduced.value = query.matches
  const onChange = (event: MediaQueryListEvent) => {
    reduced.value = event.matches
  }

  query.addEventListener('change', onChange)
  onBeforeUnmount(() => query.removeEventListener('change', onChange))

  return { reduced }
}

// 即时读取系统低动效偏好, 供非响应式流程使用
// 检测用户的操作系统级设置是否要求减少动效以实现无障碍降级保护
// 检测用户的操作系统级设置是否要求减少动效以实现无障碍降级保护
export function prefersReducedMotion(): boolean {
  if (typeof window === 'undefined' || !window.matchMedia) {
    return false
  }
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}
