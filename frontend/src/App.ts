import { computed, defineComponent, h } from 'vue'
import { RouterView, useRoute } from 'vue-router'

import AdminLayout from '@/layouts/admin/AdminLayout'
import ImmersiveLayout from '@/layouts/immersive/ImmersiveLayout'
import PublicLayout from '@/layouts/public/PublicLayout'

export default defineComponent({
  name: 'App',
  // 根据路由元信息选择前台、后台或沉浸式布局。
  setup() {
    const route = useRoute()
    const layout = computed(() => {
      if (route.meta.layout === 'admin') {
        return AdminLayout
      }
      if (route.meta.layout === 'immersive') {
        return ImmersiveLayout
      }
      return PublicLayout
    })

    return () => h(layout.value, null, { default: () => h(RouterView) })
  },
})
