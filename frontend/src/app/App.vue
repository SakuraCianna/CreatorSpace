<template>
  <component :is="layout">
    <RouterView />
  </component>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { RouterView, useRoute } from 'vue-router'

import AdminLayout from '@/app/layouts/AdminLayout.vue'
import ImmersiveLayout from '@/app/layouts/ImmersiveLayout.vue'
import PublicLayout from '@/app/layouts/PublicLayout.vue'
import { fetchCurrentTheme } from '@/services/content'
import { applyThemeConfig } from '@/shared/theme'

const route = useRoute()

// 根据路由元信息选择前台、后台或沉浸式布局。
const layout = computed(() => {
  if (route.meta.layout === 'admin') {
    return AdminLayout
  }
  if (route.meta.layout === 'immersive') {
    return ImmersiveLayout
  }
  return PublicLayout
})

onMounted(async () => {
  try {
    const theme = await fetchCurrentTheme()
    applyThemeConfig(theme)
  } catch {
    applyThemeConfig(null)
  }
})
</script>
