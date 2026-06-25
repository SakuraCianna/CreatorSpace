<template>
<!-- 内容管理规则与发布协议页 -->
<!-- 确认的业务规则列表页面 -->
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
// 导入需要的 Vue 钩子和 Lucide 图标
import { ref } from 'vue'
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

.module-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 22px;
  min-height: clamp(220px, 23vw, 292px);
  padding: clamp(24px, 3vw, 42px);
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.88), rgba(255, 255, 255, 0.58)),
    radial-gradient(circle at 16% 20%, rgba(49, 91, 255, 0.14), transparent 34%),
    radial-gradient(circle at 88% 18%, rgba(0, 124, 114, 0.16), transparent 28%),
    linear-gradient(120deg, rgba(49, 91, 255, 0.08), rgba(194, 95, 58, 0.08), rgba(0, 124, 114, 0.1));
  box-shadow: var(--tone-shadow);
}

.module-hero h2 {
  max-width: 830px;
  margin: 0;
  color: var(--tone-ink);
  font-size: 44px;
  font-weight: 860;
  line-height: 1.08;
}

.module-hero p:not(.eyebrow) {
  max-width: 680px;
  color: var(--tone-muted);
  font-size: 17px;
  line-height: 1.72;
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

@media (min-width: 761px) {
  .module-hero h2 {
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

@media (max-width: 1020px) {
  .module-hero {
    grid-template-columns: 1fr;
  }

  .rules-grid--cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .module-hero {
    padding: 22px;
  }

  .module-hero h2 {
    font-size: 30px;
  }

  .rules-grid--cards {
    grid-template-columns: 1fr;
  }
}
</style>
