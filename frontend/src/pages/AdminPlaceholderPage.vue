<template>
  <section ref="root" class="admin-module">
    <header class="module-hero" data-reveal>
      <div>
        <p class="eyebrow">{{ moduleConfig.eyebrow }}</p>
        <h2>{{ moduleConfig.title }}</h2>
        <p>{{ moduleConfig.description }}</p>
      </div>
      <button class="button button-filled" type="button">
        <Plus :size="16" />
        {{ moduleConfig.primaryAction }}
      </button>
    </header>

    <section class="workspace-grid">
      <div class="workspace-panel" data-reveal>
        <div class="panel-title">
          <h2>内容队列</h2>
          <span>{{ moduleConfig.rows.length }} items</span>
        </div>
        <article v-for="row in moduleConfig.rows" :key="row.title" class="table-row table-row--rich">
          <div>
            <strong>{{ row.title }}</strong>
            <span>{{ row.meta }}</span>
          </div>
          <span class="status-chip">{{ row.status }}</span>
        </article>
      </div>

      <aside class="workspace-panel" data-reveal>
        <div class="panel-title">
          <h2>模块能力</h2>
          <span>Next</span>
        </div>
        <ul class="rule-list">
          <li v-for="capability in moduleConfig.capabilities" :key="capability">{{ capability }}</li>
        </ul>
      </aside>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Plus } from '@lucide/vue'

import { usePageReveal } from '@/shared/composables/usePageReveal'

interface ModuleConfig {
  eyebrow: string
  title: string
  description: string
  primaryAction: string
  capabilities: string[]
  rows: Array<{ title: string; meta: string; status: string }>
}

const route = useRoute()
const root = ref<HTMLElement | null>(null)

usePageReveal(root)

const configs: Record<string, ModuleConfig> = {
  articles: {
    eyebrow: 'Writing Desk',
    title: '文章管理',
    description: '草稿、发布、私密可见性、分类、标签和版本记录都在这里完成。',
    primaryAction: '新建文章',
    capabilities: ['Markdown 编辑', '封面上传', '推荐/置顶', '私密可见名单', '版本回滚预留'],
    rows: [
      { title: '把个人站点做成主题档案馆', meta: '产品设计 · 公开 · 2 天前', status: 'PUBLISHED' },
      { title: '朋友可见：季度复盘片段', meta: '个人知识库 · 选中好友', status: 'PRIVATE' },
      { title: '演示草稿：作品上传流程', meta: '幕后日志 · 未发布', status: 'DRAFT' },
    ],
  },
  projects: {
    eyebrow: 'Gallery Desk',
    title: '作品管理',
    description: '维护作品封面、截图、过程、时间线、技术栈和外部链接。',
    primaryAction: '新增作品',
    capabilities: ['作品截图', 'Demo/GitHub/视频链接', '过程记录', '里程碑时间线', '推荐展示'],
    rows: [
      { title: '内容整理后台', meta: 'CMS Console · Vue 3 / Spring Boot', status: 'VISIBLE' },
      { title: '主题博客前台', meta: 'Blog Frontstage · WebGL / GSAP', status: 'VISIBLE' },
      { title: '阅读动效实验', meta: 'Motion Study · anime.js', status: 'DRAFT' },
    ],
  },
  inspirations: {
    eyebrow: 'Idea Box',
    title: '灵感墙管理',
    description: '摘句、图片、链接、Prompt、草图和参考资料可以快速沉淀。',
    primaryAction: '添加卡片',
    capabilities: ['类型筛选', '来源链接', '标签绑定', '公开/私密', '关联文章或作品'],
    rows: [
      { title: '写作桌摘句', meta: 'TEXT · 公开', status: 'PUBLIC' },
      { title: 'Prompt：文章摘要改写', meta: 'PROMPT · 内容策略', status: 'PUBLIC' },
      { title: '卡片瀑布流参考', meta: 'IMAGE · Awwwards', status: 'PUBLIC' },
    ],
  },
  comments: {
    eyebrow: 'Review Flow',
    title: '评论审核',
    description: '评论、回复、点赞和敏感词审核进入统一互动工作流。',
    primaryAction: '批量审核',
    capabilities: ['多级回复', '敏感词过滤', '审核通过/驳回', '评论点赞', 'AI 审核建议预留'],
    rows: [
      { title: '这篇创作中枢的拆解很适合做产品介绍页。', meta: 'ARTICLE · demo_reader_01', status: 'APPROVED' },
      { title: '希望后续能看到完整的后台原型。', meta: 'MESSAGE · demo_reader_04', status: 'PENDING' },
    ],
  },
  files: {
    eyebrow: 'Asset Library',
    title: '文件资源',
    description: '本地文件、封面、截图、附件和引用关系集中管理。',
    primaryAction: '上传文件',
    capabilities: ['文件预览', '引用统计', '未引用清理', '上传大小限制', '本地存储路径'],
    rows: [
      { title: 'demo-project-cover-console.webp', meta: 'PROJECT · 210 KB', status: 'USED' },
      { title: 'demo-avatar-lin.webp', meta: 'AVATAR · 51 KB', status: 'USED' },
    ],
  },
  themes: {
    eyebrow: 'Theme Lab',
    title: '主题配置',
    description: '主题不仅是换色，还包含字体、密度、卡片、动效和首页模块顺序。',
    primaryAction: '保存主题',
    capabilities: ['主题变量 JSON', '主题版本', '资源绑定', '预览模式', '当前主题切换'],
    rows: [
      { title: 'Glass Space', meta: '玻璃星空风 · 当前启用', status: 'ACTIVE' },
      { title: 'Cyber Night', meta: '暗色科技风 · 预设', status: 'PRESET' },
      { title: 'Minimal White', meta: '极简阅读风 · 预设', status: 'PRESET' },
    ],
  },
  settings: {
    eyebrow: 'Site Config',
    title: '站点设置',
    description: '站点身份、导航、社交链接、SEO、首页编排和关于页内容从后台配置。',
    primaryAction: '保存配置',
    capabilities: ['导航配置', 'SEO 描述', '社交链接', '首页模块排序', '关于页内容块'],
    rows: [
      { title: '站点身份', meta: 'CreatorSpace · Personal Theme Archive', status: 'READY' },
      { title: '首页推荐', meta: '文章 / 作品 / 灵感', status: 'READY' },
    ],
  },
}

const moduleConfig = computed(() => configs[route.params.section?.toString() || ''] ?? configs.articles)
</script>
