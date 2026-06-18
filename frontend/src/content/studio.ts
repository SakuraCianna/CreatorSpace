import articleContentIndexCover from '../assets/showcase/article-content-index.svg'
import articleReadingMapCover from '../assets/showcase/article-reading-map.svg'
import articleThemeArchiveCover from '../assets/showcase/article-theme-archive.svg'
import articleVisualLanguageCover from '../assets/showcase/article-visual-language.svg'
import workBlogFrontstagePoster from '../assets/showcase/work-blog-frontstage.svg'
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
