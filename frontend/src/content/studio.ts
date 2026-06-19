import articleContentIndexCover from '../assets/showcase/article-content-index.svg'
import articleReadingMapCover from '../assets/showcase/article-reading-map.svg'
import articleThemeArchiveCover from '../assets/showcase/article-theme-archive.svg'
import articleVisitEntryCover from '../assets/showcase/article-visit-entry.svg'
import articleVisualLanguageCover from '../assets/showcase/article-visual-language.svg'
import articleWritingVoiceCover from '../assets/showcase/article-writing-voice.svg'
import workBlogFrontstagePoster from '../assets/showcase/work-blog-frontstage.svg'
import workContentIndexPoster from '../assets/showcase/work-content-index.svg'
import workContentDeskPoster from '../assets/showcase/work-content-desk.svg'
import workIdeaBoxPoster from '../assets/showcase/work-idea-box.svg'
import workReadingMotionPoster from '../assets/showcase/work-reading-motion.svg'
import type {
  ArticleSummary,
  DashboardOverview,
  InspirationCard,
  ProjectSummary,
  SearchResult,
} from '@/shared/domain'

const commonTags = {
  content: { id: 1, name: '内容系统', slug: 'content-system', color: '#6ea8ff', weight: 96 },
  motion: { id: 2, name: '动效', slug: 'motion', color: '#ff9d6e', weight: 82 },
  design: { id: 3, name: '产品设计', slug: 'product-design', color: '#b18cff', weight: 91 },
}

export const fallbackArticles: ArticleSummary[] = [
  {
    id: 1,
    title: '把个人站点做成主题档案馆',
    slug: 'demo-article-creator-hub',
    summary: '文章、作品和灵感不再各自漂浮，而是被组织成一个能长期生长的个人主题空间。',
    coverUrl: articleThemeArchiveCover,
    contentMarkdown:
      '## 主题空间\nCreatorSpace 的目标不是普通博客列表，而是把创作者的文章、作品、灵感和运营工作台放进同一个系统。\n\n> 好的个人站点应该让访客感觉自己走进了一间有主人气质的房间。\n\n## 结构原则\n- 首页负责建立气质\n- 文章负责沉淀主题\n- 作品负责展示过程\n- 灵感墙负责保存碎片',
    privacyType: 'PUBLIC',
    status: 'PUBLISHED',
    category: { id: 1, module: 'ARTICLE', name: '产品设计', slug: 'product-design', sortOrder: 1 },
    tags: [commonTags.content, commonTags.design],
    publishTime: '2026-06-16T10:00:00+08:00',
  },
  {
    id: 2,
    title: '内容后台如何服务前台阅读',
    slug: 'demo-article-admin-crud-phase-one',
    summary: '后台不是表单仓库，前台读者看到的秩序来自后台稳定的状态、标签和封面规则。',
    coverUrl: articleContentIndexCover,
    contentMarkdown: '## 后台与前台\n后台管理动作最终会变成访客看到的阅读路径、作品陈列和主题秩序。',
    privacyType: 'PUBLIC',
    status: 'PUBLISHED',
    category: { id: 2, module: 'ARTICLE', name: '工程札记', slug: 'engineering', sortOrder: 2 },
    tags: [commonTags.content],
    publishTime: '2026-06-12T10:00:00+08:00',
  },
  {
    id: 3,
    title: '为个人博客设计一套不像模板的视觉语言',
    slug: 'demo-article-homepage-motion-budget',
    summary: '封面、字体、留白和滚动节奏共同决定这个站点像谁，而不是像哪个模板。',
    coverUrl: articleVisualLanguageCover,
    contentMarkdown: '## 视觉语言\n每个页面都应该保留独立记忆点，但共享同一套内容气质。',
    privacyType: 'PUBLIC',
    status: 'PUBLISHED',
    category: { id: 1, module: 'ARTICLE', name: '产品设计', slug: 'product-design', sortOrder: 1 },
    tags: [commonTags.design, commonTags.motion],
    publishTime: '2026-06-08T10:00:00+08:00',
  },
  {
    id: 4,
    title: '写作语气样本如何整理',
    slug: 'demo-article-writing-voice',
    summary: '把常用开场、句子长度、标题习惯和内容语气整理出来，让站点保留稳定的作者感。',
    coverUrl: articleWritingVoiceCover,
    contentMarkdown:
      '## 语气样本\n内容系统可以管理标题、摘要和标签，但真正让读者记住作者的是持续一致的表达方式。\n\n## 整理方法\n- 收集常用开场\n- 标记句子节奏\n- 保留个人判断\n- 删除模板化空话',
    privacyType: 'PUBLIC',
    status: 'PUBLISHED',
    category: { id: 1, module: 'ARTICLE', name: '产品设计', slug: 'product-design', sortOrder: 1 },
    tags: [commonTags.design],
    publishTime: '2026-06-03T10:00:00+08:00',
  },
  {
    id: 5,
    title: '给文章做一张阅读地图',
    slug: 'demo-article-reading-map',
    summary: '把相邻主题串成路径，让读者从一篇文章自然走向下一篇。',
    coverUrl: articleReadingMapCover,
    contentMarkdown:
      '## 阅读路径\n文章详情页不应该只负责显示正文，也要给读者下一步方向。\n\n## 地图结构\n- 当前主题\n- 关联标签\n- 推荐下一篇\n- 可回到作品或灵感墙的入口',
    privacyType: 'PUBLIC',
    status: 'PUBLISHED',
    category: { id: 2, module: 'ARTICLE', name: '工程札记', slug: 'engineering', sortOrder: 2 },
    tags: [commonTags.content],
    publishTime: '2026-06-01T10:00:00+08:00',
  },
  {
    id: 6,
    title: '从入口开始设计一次访问',
    slug: 'demo-article-visit-entry',
    summary: '游客先看到什么、点击哪里、在哪里停留，决定了个人站点会不会被继续浏览。',
    coverUrl: articleVisitEntryCover,
    contentMarkdown:
      '## 访问入口\n首页是主题入口，不是功能清单。第一屏需要同时交代作者气质、内容入口和下一步动作。\n\n## 入口判断\n- 文章入口是否足够清楚\n- 作品入口是否有展示欲\n- 登录注册是否容易找到\n- 移动端是否能快速理解站点',
    privacyType: 'PUBLIC',
    status: 'PUBLISHED',
    category: { id: 1, module: 'ARTICLE', name: '产品设计', slug: 'product-design', sortOrder: 1 },
    tags: [commonTags.design, commonTags.motion],
    publishTime: '2026-05-28T10:00:00+08:00',
  },
]

export const fallbackProjects: ProjectSummary[] = [
  {
    id: 1,
    title: '主题博客前台',
    slug: 'demo-project-immersive-homepage',
    description: '把文章、作品、灵感和主题系统放进同一套沉浸式前台体验。',
    coverUrl: workBlogFrontstagePoster,
    projectType: 'Blog Frontstage',
    techStack: ['Vue 3', 'GSAP', 'Three.js', 'Spring Boot'],
    githubUrl: 'https://github.com/SakuraCianna/CreatorSpace',
    demoUrl: 'https://example.com/creatorspace-demo',
    videoUrl: null,
    contentMarkdown:
      '## 项目背景\n首页是主题入口，不是营销页。\n\n## 过程\n- 用 WebGL 建立第一眼气质\n- 用 GSAP 分配每个区块的动效语言\n- 用后台配置承接后续内容变化',
    status: 'VISIBLE',
    recommended: true,
    tags: [commonTags.content, commonTags.motion],
  },
  {
    id: 2,
    title: '内容整理后台',
    slug: 'demo-project-content-console',
    description: '面向个人创作者的 CMS 工作台，管理文章、作品、灵感、评论和主题。',
    coverUrl: workContentDeskPoster,
    projectType: 'CMS Console',
    techStack: ['Material 3', 'Pinia', 'JWT', 'PostgreSQL'],
    githubUrl: null,
    demoUrl: null,
    videoUrl: null,
    contentMarkdown: '## 项目目标\n让内容管理可扫描、可重复操作，并能服务长期创作节奏。',
    status: 'VISIBLE',
    recommended: true,
    tags: [commonTags.content, commonTags.design],
  },
  {
    id: 3,
    title: '阅读动效实验',
    slug: 'demo-project-reading-motion',
    description: '用滚动、封面和目录节奏让长文阅读更像策展路线。',
    coverUrl: workReadingMotionPoster,
    projectType: 'Motion Study',
    techStack: ['anime.js', 'GSAP', 'Markdown'],
    githubUrl: null,
    demoUrl: null,
    videoUrl: null,
    contentMarkdown: '## 实验说明\n页面不依赖单一动效，而是按信息节奏选择不同动作。',
    status: 'VISIBLE',
    recommended: false,
    tags: [commonTags.motion],
  },
  {
    id: 4,
    title: '内容索引',
    slug: 'demo-project-content-index',
    description: '让分类、标签、封面和摘要一起工作，帮助读者从一个主题找到下一篇文章。',
    coverUrl: workContentIndexPoster,
    projectType: 'Content Index',
    techStack: ['Category', 'Tag', 'Reading Path'],
    githubUrl: null,
    demoUrl: null,
    videoUrl: null,
    contentMarkdown: '## 展示目标\n内容索引把文章从时间线里捞出来，重新放回主题、标签和阅读路径里。',
    status: 'VISIBLE',
    recommended: false,
    tags: [commonTags.content],
  },
  {
    id: 5,
    title: '选题卡片盒',
    slug: 'demo-project-idea-box',
    description: '把还没成文的标题、摘句、链接和参考图先收起来，留给之后的文章继续生长。',
    coverUrl: workIdeaBoxPoster,
    projectType: 'Idea Box',
    techStack: ['Notes', 'Links', 'Images'],
    githubUrl: null,
    demoUrl: null,
    videoUrl: null,
    contentMarkdown: '## 创作过程\n灵感墙先保存碎片，等主题足够清晰时再沉淀成文章或作品档案。',
    status: 'VISIBLE',
    recommended: false,
    tags: [commonTags.design],
  },
]

export const fallbackInspirations: InspirationCard[] = [
  {
    id: 1,
    title: '写作桌摘句',
    content: '个人博客不是简历扩展页，它应该像一间房间，能看见主人反复想什么。',
    imageUrl: null,
    cardType: 'TEXT',
    sourceUrl: null,
    color: '#6ea8ff',
    sortOrder: 1,
    createdAt: '2026-06-16T10:00:00+08:00',
    tags: [commonTags.design],
  },
  {
    id: 2,
    title: 'Prompt：文章摘要改写',
    content: '请把这段技术复盘改写成更像个人博客开头的摘要，保留判断，不要变成营销文案。',
    imageUrl: null,
    cardType: 'PROMPT',
    sourceUrl: null,
    color: '#b18cff',
    sortOrder: 2,
    createdAt: '2026-06-15T10:00:00+08:00',
    tags: [commonTags.content],
  },
  {
    id: 3,
    title: '卡片瀑布流参考',
    content: '灵感墙要像素材板，卡片高度和信息密度可以不同，但操作入口要稳定。',
    imageUrl: workIdeaBoxPoster,
    cardType: 'IMAGE',
    sourceUrl: 'https://www.awwwards.com/websites/portfolio/',
    color: '#54e6c8',
    sortOrder: 3,
    createdAt: '2026-06-14T10:00:00+08:00',
    tags: [commonTags.design, commonTags.motion],
  },
  {
    id: 4,
    title: '代码片段：安全外链',
    content: "const safe = ['http:', 'https:'].includes(new URL(value).protocol)",
    imageUrl: null,
    cardType: 'CODE',
    sourceUrl: null,
    color: '#ff9d6e',
    sortOrder: 4,
    createdAt: '2026-06-13T10:00:00+08:00',
    tags: [commonTags.content],
  },
]

export const dashboardFallback: DashboardOverview = {
  metrics: [
    { label: '文章', value: '18', note: '公开、私密和草稿都在内容管线中' },
    { label: '作品', value: '12', note: '推荐作品正在前台展厅展示' },
    { label: '灵感', value: '14', note: 'Prompt、图片、摘句和链接可筛选' },
    { label: '待审核', value: '6', note: '评论进入审核队列后再展示' },
  ],
  hotArticles: [
    { title: '把个人站点做成主题档案馆', slug: 'demo-article-creator-hub', views: 2680, likes: 142 },
    { title: 'AI 辅助写作的五段式流程', slug: 'demo-article-ai-writing-flow', views: 2418, likes: 131 },
  ],
  hotProjects: [
    { title: '内容整理后台', slug: 'demo-project-content-console', views: 0, likes: 0 },
    { title: '主题博客前台', slug: 'demo-project-immersive-homepage', views: 0, likes: 0 },
  ],
  visitTrend: Array.from({ length: 7 }, (_, index) => ({
    date: `06-${String(12 + index).padStart(2, '0')}`,
    pv: 28 + index * 9,
  })),
  recentActivities: [
    { operation: '发布文章', module: 'ARTICLE', createdAt: '2026-06-18 09:40' },
    { operation: '调整主题', module: 'THEME', createdAt: '2026-06-18 08:12' },
    { operation: '审核评论', module: 'COMMENT', createdAt: '2026-06-17 21:30' },
  ],
}

export const fallbackSearchResults: SearchResult[] = [
  ...fallbackArticles.map((article) => ({
    type: 'ARTICLE' as const,
    title: article.title,
    slug: article.slug,
    description: article.summary,
    coverUrl: article.coverUrl,
    occurredAt: article.publishTime,
    score: 1,
  })),
  ...fallbackProjects.map((project) => ({
    type: 'PROJECT' as const,
    title: project.title,
    slug: project.slug,
    description: project.description,
    coverUrl: project.coverUrl,
    occurredAt: null,
    score: 0.8,
  })),
]
