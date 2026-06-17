<template>
  <component :is="layout">
    <RouterView />
  </component>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'

import AdminLayout from '@/app/layouts/AdminLayout.vue'
import ImmersiveLayout from '@/app/layouts/ImmersiveLayout.vue'
import PublicLayout from '@/app/layouts/PublicLayout.vue'

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
</script>
