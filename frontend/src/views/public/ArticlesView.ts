import { defineComponent, h, onMounted, ref } from 'vue'

import { fetchArticles } from '@/api/content'
import type { ArticleSummary } from '@/types/domain'

const privacyLabels: Record<ArticleSummary['privacyType'], string> = {
  PUBLIC: '公开',
  SELF: '仅自己',
  FRIENDS: '仅好友',
  SELECTED_FRIENDS: '选中好友可见',
  EXCLUDED_FRIENDS: '排除选中好友',
}

export default defineComponent({
  name: 'ArticlesView',
  // 准备公开文章列表的加载、错误和空状态。
  setup() {
    const articles = ref<ArticleSummary[]>([])
    const isLoading = ref(true)
    const errorMessage = ref('')

    // 加载公开文章列表。
    async function loadArticles() {
      isLoading.value = true
      errorMessage.value = ''
      try {
        const page = await fetchArticles()
        articles.value = page.records
      } catch (error) {
        errorMessage.value = error instanceof Error ? error.message : '文章加载失败'
      } finally {
        isLoading.value = false
      }
    }

    onMounted(loadArticles)

    return () =>
      h('section', { class: 'page-section' }, [
        h('div', { class: 'section-heading' }, [
          h('h1', '文章'),
          h('p', '公开文章会从后端内容接口读取，草稿和私密文章不会展示在这里。'),
        ]),
        isLoading.value
          ? h('div', { class: 'empty-state' }, [h('h2', '文章加载中')])
          : errorMessage.value
            ? h('div', { class: 'empty-state' }, [h('h2', '文章暂时不可用'), h('p', errorMessage.value)])
            : articles.value.length === 0
              ? h('div', { class: 'empty-state' }, [h('h2', '暂无公开文章'), h('p', '发布公开文章后会显示在这里。')])
              : h(
          'div',
          { class: 'list-surface' },
          articles.value.map((article) =>
            h('article', { key: article.id, class: 'list-row' }, [
              h('div', [
                h('h2', article.title),
                h(
                  'p',
                  [
                    article.category?.name,
                    article.slug,
                    privacyLabels[article.privacyType],
                    ...article.tags.map((tag) => `#${tag.name}`),
                  ]
                    .filter(Boolean)
                    .join(' · '),
                ),
              ]),
              h('span', { class: 'status-chip' }, article.status),
            ]),
          ),
        ),
      ])
  },
})
