<template>
  <component :is="layout">
    <RouterView v-slot="{ Component, route }">
      <transition name="page-fade" mode="out-in">
        <component :is="Component" :key="route.path" />
      </transition>
    </RouterView>
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
// 根据路由元信息选择前台、后台或沉浸式布局
// 根据路由元信息动态判断切换后台布局、沉浸式布局或前台公共布局
const layout = computed(() => {
  if (route.meta.layout === 'admin') {
    return AdminLayout
  }
  if (route.meta.layout === 'immersive') {
    return ImmersiveLayout
  }
  return PublicLayout
})
// 挂载时首先读取本地临时缓存预览的主题配置, 若无则向后端发起 API 请求获取当前正式启用的全站主题并渲染应用
onMounted(async () => {
  const localPreview = window.localStorage.getItem('creatorspace_preview_theme')
  if (localPreview) {
    try {
      const theme = JSON.parse(localPreview)
      applyThemeConfig(theme)
      return
    } catch {
      window.localStorage.removeItem('creatorspace_preview_theme')
    }
  }
  try {
    const theme = await fetchCurrentTheme()
    applyThemeConfig(theme)
  } catch {
    applyThemeConfig(null)
  }
})
</script>
<style>
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.page-fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}
.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
