import { computed, defineComponent, h } from 'vue'
import { RouterView, useRoute } from 'vue-router'

import AdminLayout from '@/layouts/admin/AdminLayout'
import PublicLayout from '@/layouts/public/PublicLayout'

export default defineComponent({
  name: 'App',
  setup() {
    const route = useRoute()
    const layout = computed(() => (route.meta.layout === 'admin' ? AdminLayout : PublicLayout))

    return () => h(layout.value, null, { default: () => h(RouterView) })
  },
})
