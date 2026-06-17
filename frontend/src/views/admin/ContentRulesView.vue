<template>
  <section class="workspace-panel">
    <div class="panel-title">
      <h2>已确认业务规则</h2>
      <span>用于后续接口验收</span>
    </div>
    <div class="rules-grid">
      <article v-for="rule in rules" :key="rule.title">
        <h3>{{ rule.title }}</h3>
        <p>{{ rule.body }}</p>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
const rules = [
  { title: '注册', body: '默认开放，只填写用户名和密码，不需要邮箱。' },
  { title: '私密文章', body: '支持公开、仅自己、仅好友、仅选中好友、排除选中好友。' },
  { title: '评论与收藏', body: '游客只读，评论、点赞、收藏都要求登录。' },
  { title: '文件', body: '允许泛文件上传，删除内容时清理未被引用的文件。' },
]
</script>
