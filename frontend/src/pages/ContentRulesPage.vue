<template>
  <section ref="root" class="rules-page">
    <header class="module-hero" data-reveal>
      <div>
        <p class="eyebrow">Content Policy</p>
        <h2>已确认业务规则</h2>
        <p>这些规则来自当前需求文档和产品确认，用于约束后续接口、页面和数据库演进。</p>
      </div>
    </header>

    <div class="rules-grid rules-grid--cards">
      <article v-for="rule in rules" :key="rule.title" data-reveal>
        <component :is="rule.icon" :size="22" />
        <h3>{{ rule.title }}</h3>
        <p>{{ rule.body }}</p>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { FileLock2, MessageSquare, ShieldCheck, UploadCloud, UserPlus } from '@lucide/vue'

import { usePageReveal } from '@/shared/composables/usePageReveal'

const root = ref<HTMLElement | null>(null)

usePageReveal(root)

const rules = [
  { title: '注册', body: '默认开放，只填写用户名和密码，不需要邮箱。', icon: UserPlus },
  { title: '私密文章', body: '支持公开、仅自己、仅好友、仅选中好友、排除选中好友。', icon: FileLock2 },
  { title: '评论与收藏', body: '游客只读，评论、点赞、收藏都要求登录。', icon: MessageSquare },
  { title: '文件', body: '允许泛文件上传，删除内容时清理未被引用的文件。', icon: UploadCloud },
  { title: '安全', body: 'JWT 密钥和第三方 API Key 只允许写入本地或部署密钥系统。', icon: ShieldCheck },
]
</script>
