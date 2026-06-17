import type { AdminMetric, ArticleSummary } from '@/shared/domain'

export const adminMetrics: AdminMetric[] = [
  { label: '文章', value: '128', trend: '12 篇草稿待整理' },
  { label: '作品', value: '36', trend: '5 个推荐展示中' },
  { label: '评论', value: '418', trend: '24 条待审核' },
  { label: '文件', value: '1.8GB', trend: '引用清理已预留' },
]

export const articleSamples: ArticleSummary[] = [
  {
    id: 1,
    title: '用内容系统管理个人创作资产',
    slug: 'content-system-for-creator',
    privacyType: 'PUBLIC',
    status: 'PUBLISHED',
    summary: '内容系统样例',
    category: null,
    tags: [],
    publishTime: '2026-06-16',
  },
  {
    id: 2,
    title: '仅好友可见的项目复盘',
    slug: 'friends-only-review',
    privacyType: 'FRIENDS',
    status: 'PRIVATE',
    summary: '私密文章样例',
    category: null,
    tags: [],
    publishTime: '2026-06-15',
  },
  {
    id: 3,
    title: '给选中好友看的实验草案',
    slug: 'selected-friends-draft',
    privacyType: 'SELECTED_FRIENDS',
    status: 'DRAFT',
    summary: '草稿样例',
    category: null,
    tags: [],
    publishTime: '2026-06-14',
  },
]

export const privacyLabels: Record<ArticleSummary['privacyType'], string> = {
  PUBLIC: '公开',
  SELF: '仅自己',
  FRIENDS: '仅好友',
  SELECTED_FRIENDS: '选中好友可见',
  EXCLUDED_FRIENDS: '排除选中好友',
}
