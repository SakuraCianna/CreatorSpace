import type { AdminMetric, ArticleSummary } from '@/types/domain'

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
    privacy: 'PUBLIC',
    status: 'PUBLISHED',
    updatedAt: '2026-06-16',
  },
  {
    id: 2,
    title: '仅好友可见的项目复盘',
    slug: 'friends-only-review',
    privacy: 'FRIENDS',
    status: 'PRIVATE',
    updatedAt: '2026-06-15',
  },
  {
    id: 3,
    title: '给选中好友看的实验草案',
    slug: 'selected-friends-draft',
    privacy: 'SELECTED_FRIENDS',
    status: 'DRAFT',
    updatedAt: '2026-06-14',
  },
]

export const privacyLabels: Record<ArticleSummary['privacy'], string> = {
  PUBLIC: '公开',
  SELF: '仅自己',
  FRIENDS: '仅好友',
  SELECTED_FRIENDS: '选中好友可见',
  EXCLUDED_FRIENDS: '排除选中好友',
}
