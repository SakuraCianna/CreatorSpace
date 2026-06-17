<template>
  <section class="empty-state">
    <h2>{{ section }} 模块骨架已就位</h2>
    <p>后续会按接口优先级补齐列表、筛选、编辑表单、批量操作和权限校验。</p>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

// 根据路由参数展示后台模块名称。
const section = computed(() => route.params.section?.toString() || '模块')
</script>
