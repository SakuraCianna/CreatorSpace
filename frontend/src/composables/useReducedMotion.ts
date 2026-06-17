import { onBeforeUnmount, ref } from 'vue'
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
export function prefersReducedMotion(): boolean {
  if (typeof window === 'undefined' || !window.matchMedia) {
    return false
  }
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}
