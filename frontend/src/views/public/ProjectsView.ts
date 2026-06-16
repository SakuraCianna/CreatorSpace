import { defineComponent, h } from 'vue'

export default defineComponent({
  name: 'ProjectsView',
  setup() {
    return () =>
      h('section', { class: 'page-section' }, [
        h('div', { class: 'section-heading' }, [
          h('h1', '作品'),
          h('p', '作品集页面会展示项目、截图、视频、附件和外部链接。'),
        ]),
        h('div', { class: 'empty-state' }, [
          h('h2', '作品框架已预留'),
          h('p', '下一步接入作品列表、推荐作品、文件引用和详情页 Markdown。'),
        ]),
      ])
  },
})
