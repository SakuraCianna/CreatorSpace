<template>
<!-- 内容管理规则与发布协议页 -->
<!-- 确认的业务规则列表页面 -->
  <section ref="root" class="rules-page">
    <AdminPageHeader title="已确认业务规则" description="这些规则来自当前需求文档和产品确认，用于约束后续接口、页面和数据库演进。" kicker="Content Policy" theme="indigo" />

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
// 导入需要的 Vue 钩子和 Lucide 图标
import { ref } from 'vue'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import { FileLock2, MessageSquare, ShieldCheck, UploadCloud, UserPlus } from '@lucide/vue'

import { usePageReveal } from '@/shared/composables/usePageReveal'

// 声明根节点引用以执行入场动效
const root = ref<HTMLElement | null>(null)

// 触发电影式页面入场显影动效
usePageReveal(root)

const rules = [
  { title: '注册', body: '默认开放，只填写用户名和密码，不需要邮箱。', icon: UserPlus },
  { title: '私密文章', body: '支持公开、仅自己、仅好友、仅选中好友、排除选中好友。', icon: FileLock2 },
  { title: '评论与收藏', body: '游客只读，评论、点赞、收藏都要求登录。', icon: MessageSquare },
  { title: '文件', body: '允许泛文件上传，删除内容时清理未被引用的文件。', icon: UploadCloud },
  { title: '安全', body: 'JWT 密钥和第三方 API Key 只允许写入本地或部署密钥系统。', icon: ShieldCheck },
]
</script>

<style scoped>
.rules-page {
  display: grid;
  gap: 18px;
}



.rule-list {
  display: grid;
  gap: 10px;
  margin: 0;
  padding-left: 18px;
  color: var(--tone-muted);
  line-height: 1.68;
}

.rules-grid--cards {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.rules-grid--cards article {
  display: grid;
  gap: 12px;
  padding: 20px;
  border: 1px solid var(--tone-line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: var(--tone-shadow);
}

.rules-grid--cards h3,
.rules-grid--cards p {
  margin: 0;
}

.rules-grid--cards p {
  color: var(--tone-muted);
  line-height: 1.65;
}



@media (max-width: 1020px) {

  .rules-grid--cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {

  .rules-grid--cards {
    grid-template-columns: 1fr;
  }
}
</style>
