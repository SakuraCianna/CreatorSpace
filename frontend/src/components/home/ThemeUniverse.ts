import { defineComponent, h, onBeforeUnmount, ref } from 'vue'
import { gsap } from 'gsap'

import { prefersReducedMotion } from '@/composables/useReducedMotion'
import { themePresets, type ThemePreset } from '@/data/home-content'
export default defineComponent({
  name: 'ThemeUniverse',
  setup() {
    const activeId = ref<string>(themePresets[0]?.id ?? '')
    const preview = ref<HTMLElement | null>(null)

    /**
     * 把主题变量写入预览容器。
     */
    const applyVars = (preset: ThemePreset) => {
      const el = preview.value
      if (!el) {
        return
      }
      const v = preset.vars
      el.style.setProperty('--tp-bg', v.bg)
      el.style.setProperty('--tp-surface', v.surface)
      el.style.setProperty('--tp-ink', v.ink)
      el.style.setProperty('--tp-muted', v.muted)
      el.style.setProperty('--tp-accent', v.accent)
      el.style.setProperty('--tp-accent-soft', v.accentSoft)
      el.style.setProperty('--tp-font', v.font)
    }

    /**
     * 切换当前预览主题。
     */
    const select = (preset: ThemePreset) => {
      if (preset.id === activeId.value) {
        return
      }
      activeId.value = preset.id
      applyVars(preset)
      if (prefersReducedMotion() || !preview.value) {
        return
      }
      gsap.fromTo(
        preview.value.querySelectorAll('.cs-tp-fade'),
        { opacity: 0.3, y: 10 },
        { opacity: 1, y: 0, duration: 0.6, ease: 'power2.out', stagger: 0.05 },
      )
    }
    /**
     * 保存主题预览容器引用。
     */
    const setPreviewRef = (el: Element | null) => {
      preview.value = el as HTMLElement | null
      const first = themePresets.find((p) => p.id === activeId.value)
      if (first) {
        applyVars(first)
      }
    }

    onBeforeUnmount(() => {
      if (preview.value) {
        gsap.killTweensOf(preview.value.querySelectorAll('.cs-tp-fade'))
      }
    })

    /**
     * 渲染主题切换按钮。
     */
    const renderThemeBtn = (preset: ThemePreset) =>
      h(
        'button',
        {
          key: preset.id,
          type: 'button',
          class: ['cs-theme-btn', { 'is-active': preset.id === activeId.value }],
          onClick: () => select(preset),
        },
        [
          h(
            'span',
            { class: 'cs-theme-btn__swatch' },
            preset.swatches.map((c, i) => h('span', { key: i, style: { background: c } })),
          ),
          h('span', [
            h('span', { class: 'cs-theme-btn__name' }, preset.name),
            h('span', { class: 'cs-theme-btn__tag' }, preset.tagline),
          ]),
        ],
      )

    /**
     * 读取当前激活主题配置。
     */
    const activePreset = () => themePresets.find((p) => p.id === activeId.value) ?? themePresets[0]

    return () => {
      const active = activePreset()
      return h('section', { class: 'cs-themes cs-section', id: 'themes' }, [
        h('div', { class: 'cs-head' }, [
          h('div', [
            h('p', { class: 'cs-eyebrow' }, 'Theme Universe'),
            h('h2', { class: 'cs-head__title' }, '一个空间，多种气质'),
          ]),
          h(
            'p',
            { class: 'cs-head__note' },
            '主题是系统能力，不只是皮肤。点击切换，预览区会实时重新着色。',
          ),
        ]),
        h('div', { class: 'cs-themes__layout' }, [
          h('div', { class: 'cs-themes__list' }, themePresets.map(renderThemeBtn)),
          h('div', { class: 'cs-theme-preview', ref: setPreviewRef }, [
            h('p', { class: 'cs-theme-preview__eyebrow cs-tp-fade' }, active?.mood ?? ''),
            h('h3', { class: 'cs-theme-preview__title cs-tp-fade' }, active?.name ?? ''),
            h(
              'p',
              { class: 'cs-theme-preview__text cs-tp-fade' },
              'The quick brown fox writes a thoughtful post. 文章、作品与灵感，在同一套设计语言下被重新呈现。',
            ),
            h('div', { class: 'cs-theme-preview__cards cs-tp-fade' }, [
              h('div', { class: 'cs-theme-preview__card' }, [
                h('strong', '128'),
                h('span', 'Articles'),
              ]),
              h('div', { class: 'cs-theme-preview__card' }, [
                h('strong', '36'),
                h('span', 'Works'),
              ]),
            ]),
            h('span', { class: 'cs-theme-preview__chip cs-tp-fade' }, 'Apply this theme'),
          ]),
        ]),
      ])
    }
  },
})
