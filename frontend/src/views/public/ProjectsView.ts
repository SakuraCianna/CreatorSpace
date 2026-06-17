import { defineComponent, h, onMounted, ref } from 'vue'

import { fetchProjects } from '@/api/content'
import type { ProjectSummary } from '@/types/domain'

export default defineComponent({
  name: 'ProjectsView',
  // 准备公开作品列表的加载、错误和空状态。
  setup() {
    const projects = ref<ProjectSummary[]>([])
    const isLoading = ref(true)
    const errorMessage = ref('')

    // 加载公开作品列表。
    async function loadProjects() {
      isLoading.value = true
      errorMessage.value = ''
      try {
        const page = await fetchProjects()
        projects.value = page.records
      } catch (error) {
        errorMessage.value = error instanceof Error ? error.message : '作品加载失败'
      } finally {
        isLoading.value = false
      }
    }

    onMounted(loadProjects)

    return () =>
      h('section', { class: 'page-section' }, [
        h('div', { class: 'section-heading' }, [
          h('h1', '作品'),
          h('p', '作品集页面展示后台创建并设置为可见的项目。'),
        ]),
        isLoading.value
          ? h('div', { class: 'empty-state' }, [h('h2', '作品加载中')])
          : errorMessage.value
            ? h('div', { class: 'empty-state' }, [h('h2', '作品暂时不可用'), h('p', errorMessage.value)])
            : projects.value.length === 0
              ? h('div', { class: 'empty-state' }, [h('h2', '暂无公开作品'), h('p', '新增可见作品后会显示在这里。')])
              : h(
                  'div',
                  { class: 'list-surface' },
                  projects.value.map((project) =>
                    h('article', { key: project.id, class: 'list-row' }, [
                      h('div', [
                        h('h2', project.title),
                        h(
                          'p',
                          [
                            project.projectType,
                            ...project.techStack,
                            ...project.tags.map((tag) => `#${tag.name}`),
                          ].join(' · '),
                        ),
                      ]),
                      h('span', { class: 'status-chip' }, project.recommended ? '推荐' : project.status),
                    ]),
                  ),
                ),
      ])
  },
})
