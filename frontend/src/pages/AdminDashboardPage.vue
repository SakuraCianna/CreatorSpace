<template>
  <section class="dashboard-grid">
    <article v-for="metric in adminMetrics" :key="metric.label" class="metric-card">
      <span>{{ metric.label }}</span>
      <strong>{{ metric.value }}</strong>
      <p>{{ metric.trend }}</p>
    </article>
  </section>
  <section class="workspace-grid">
    <div class="workspace-panel">
      <div class="panel-title">
        <h2>最近文章</h2>
        <span>隐私规则已对齐</span>
      </div>
      <article v-for="article in articleSamples" :key="article.id" class="table-row">
        <div>
          <strong>{{ article.title }}</strong>
          <span>{{ privacyLabels[article.privacyType] }} · {{ article.publishTime ?? '未发布' }}</span>
        </div>
        <span class="status-chip">{{ article.status }}</span>
      </article>
    </div>
    <div class="workspace-panel">
      <div class="panel-title">
        <h2>互动规则</h2>
        <span>游客只读</span>
      </div>
      <ul class="rule-list">
        <li>评论、点赞、收藏都要求登录用户身份。</li>
        <li>评论系统按完整多级回复设计。</li>
        <li>删除内容时清理未被引用的文件资源。</li>
        <li>敏感词表和 pgvector 能力已在数据库层预留。</li>
      </ul>
    </div>
  </section>
</template>

<script setup lang="ts">
import { adminMetrics, articleSamples, privacyLabels } from '@/content/sample'
</script>
