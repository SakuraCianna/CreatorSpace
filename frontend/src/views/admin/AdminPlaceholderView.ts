import { computed, defineComponent, h } from 'vue'
import { useRoute } from 'vue-router'

export default defineComponent({
  name: 'AdminPlaceholderView',
  setup() {
    const route = useRoute()
    const section = computed(() => route.params.section?.toString() || '模块')

    return () =>
      h('section', { class: 'empty-state' }, [
        h('h2', `${section.value} 模块骨架已就位`),
        h('p', '后续会按接口优先级补齐列表、筛选、编辑表单、批量操作和权限校验。'),
      ])
  },
})
