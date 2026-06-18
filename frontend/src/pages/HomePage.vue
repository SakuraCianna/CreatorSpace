<template>
  <div class="cs-home">
    <HeroUniverse />
    <MarqueeManifesto />
    <FeaturedArticles />
    <PortfolioGallery />
    <SiteStructureShowcase />
    <ApproachProcess />
    <CreativeWall />
    <ThemeUniverse />
    <FieldNotes />
    <FinalCTA />
  </div>
</template>

<script setup lang="ts">
import { defineComponent, h, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

import type { HeroSceneHandles } from '@/shared/heroScene'
import { useGsapContext } from '@/shared/composables/useGsapScroll'
import { useLenis } from '@/shared/composables/useLenis'
import { attachMagnetic } from '@/shared/composables/useMagnetic'
import { prefersReducedMotion } from '@/shared/composables/useReducedMotion'
import {
  agentCapabilities,
  approachSteps,
  counters,
  closing,
  creativeFragments,
  featuredArticles,
  fieldNotes,
  heroContent,
  manifesto,
  marqueeWords,
  portfolioProjects,
  siteConfig,
  themePresets,
  type AgentCapability,
  type ApproachStep,
  type CreativeFragment,
  type FeaturedArticle,
  type FieldNote,
  type PortfolioProject,
  type ThemePreset,
} from '@/content/home'

import '@/styles/home.css'

gsap.registerPlugin(ScrollTrigger)

useLenis()

onMounted(() => document.body.classList.add('cs-dark-body'))
onBeforeUnmount(() => document.body.classList.remove('cs-dark-body'))

const HeroWebGLScene = defineComponent({
  name: 'HeroWebGLScene',
  setup() {
    const host = ref<HTMLElement | null>(null)
    const isStatic = ref(false)
    let handles: HeroSceneHandles | null = null
    let io: IntersectionObserver | null = null
    let cancelled = false

    // 将指针位置换算成 WebGL 场景使用的标准坐标。
    const onPointerMove = (event: PointerEvent) => {
      if (!handles) {
        return
      }
      const nx = (event.clientX / window.innerWidth) * 2 - 1
      const ny = -((event.clientY / window.innerHeight) * 2 - 1)
      handles.setPointer(nx, ny)
    }

    onMounted(() => {
      const el = host.value
      if (!el) {
        return
      }
      const probe = document.createElement('canvas')
      const hasWebGL = !!(probe.getContext('webgl2') || probe.getContext('webgl'))
      if (prefersReducedMotion() || !hasWebGL) {
        isStatic.value = true
        return
      }
      import('@/shared/heroScene')
        .then(({ createHeroScene }) => {
          if (cancelled || !host.value) {
            return
          }
          handles = createHeroScene(host.value)
          window.addEventListener('pointermove', onPointerMove, { passive: true })
          io = new IntersectionObserver(
            (entries) => {
              const entry = entries[0]
              if (entry) {
                handles?.setPaused(!entry.isIntersecting)
              }
            },
            { threshold: 0 },
          )
          io.observe(host.value)
        })
        .catch(() => {
          isStatic.value = true
        })
    })

    onBeforeUnmount(() => {
      cancelled = true
      window.removeEventListener('pointermove', onPointerMove)
      io?.disconnect()
      io = null
      handles?.dispose()
      handles = null
    })

    return () =>
      h('div', {
        ref: host,
        class: ['cs-hero__canvas', { 'cs-hero__canvas--static': isStatic.value }],
        'aria-hidden': 'true',
      })
  },
})

// 渲染首页英雄区按钮。
function heroButton(to: string, label: string, ghost = false) {
  return h(
    RouterLink,
    { to, class: ['cs-btn', { 'cs-btn--ghost': ghost }] },
    {
      default: () => [
        h('span', { class: 'cs-btn__fill' }),
        h('span', { class: 'cs-btn__dot' }),
        h('span', label),
      ],
    },
  )
}

const HeroUniverse = defineComponent({
  name: 'HeroUniverse',
  setup() {
    const root = ref<HTMLElement | null>(null)
    useGsapContext(root, ({ reduced }) => {
      if (reduced) {
        return
      }
      const tl = gsap.timeline({ defaults: { ease: 'expo.out' }, delay: 0.15 })
      tl.from('.cs-hero__title .cs-line > span', {
        yPercent: 110,
        duration: 1.2,
        stagger: 0.12,
      })
        .from('.cs-hero__kicker', { y: 20, opacity: 0, duration: 0.8 }, '-=0.9')
        .from('.cs-hero__sub', { y: 24, opacity: 0, duration: 0.8 }, '-=0.7')
        .from('.cs-hero__actions > *', { y: 20, opacity: 0, duration: 0.7, stagger: 0.1 }, '-=0.6')
        .from('.cs-hero__stats .cs-stat', { y: 18, opacity: 0, duration: 0.7, stagger: 0.08 }, '-=0.5')
        .from('.cs-hero__scroll', { opacity: 0, duration: 0.6 }, '-=0.3')
    })

    // 渲染英雄标题行。
    const titleLine = (text: string) =>
      h('span', { class: 'cs-line' }, [h('span', text)])

    return () =>
      h('section', { ref: root, class: 'cs-hero cs-section' }, [
        h(HeroWebGLScene),
        h('div', { class: 'cs-hero__grain' }),
        h('div', { class: 'cs-hero__inner' }, [
          h('p', { class: 'cs-eyebrow cs-hero__kicker' }, heroContent.kicker),
          h('h1', { class: 'cs-hero__title' }, heroContent.titleLines.map(titleLine)),
          h('p', { class: 'cs-hero__sub' }, [
            heroContent.subtitle,
            h('span', { class: 'cs-zh' }, heroContent.description),
          ]),
          h('div', { class: 'cs-hero__actions' }, [
            heroButton(heroContent.primary.to, heroContent.primary.label),
            heroButton(heroContent.secondary.to, heroContent.secondary.label, true),
          ]),
          h(
            'div',
            { class: 'cs-hero__stats' },
            heroContent.stats.map((stat) =>
              h('div', { class: 'cs-stat', key: stat.label }, [
                h('div', { class: 'cs-stat__value' }, stat.value),
                h('div', { class: 'cs-stat__label' }, stat.label),
              ]),
            ),
          ),
        ]),
        h('div', { class: 'cs-hero__scroll' }, 'Scroll to explore'),
      ])
  },
})

const MarqueeManifesto = defineComponent({
  name: 'MarqueeManifesto',
  setup() {
    const root = ref<HTMLElement | null>(null)

    useGsapContext(root, ({ reduced }) => {
      if (!reduced) {
        gsap.from('.cs-mani__word', {
          opacity: 0.12,
          duration: 0.6,
          ease: 'none',
          stagger: 0.04,
          scrollTrigger: {
            trigger: '.cs-mani',
            start: 'top 80%',
            end: 'top 30%',
            scrub: true,
          },
        })
      }

      const rows = gsap.utils.toArray<HTMLElement>('.cs-marquee__row')
      rows.forEach((row, i) => {
        const inner = row.querySelector<HTMLElement>('.cs-marquee__inner')
        if (!inner) {
          return
        }
        const dir = i % 2 === 0 ? -1 : 1

        if (reduced) {
          gsap.set(inner, { xPercent: -25 })
          return
        }
        const wrap = gsap.utils.wrap(-50, 0)
        const from = dir < 0 ? 0 : -50
        const to = dir < 0 ? -50 : 0
        const baseTween = gsap.fromTo(
          inner,
          { xPercent: from },
          {
            xPercent: to,
            duration: 28,
            ease: 'none',
            repeat: -1,
            modifiers: {
              xPercent: (value) => `${wrap(parseFloat(value))}%`,
            },
          },
        )
        ScrollTrigger.create({
          trigger: root.value as Element,
          start: 'top bottom',
          end: 'bottom top',
          onUpdate: (self) => {
            const velocity = self.getVelocity()
            const scale = 1 + Math.min(Math.abs(velocity) / 1200, 3)
            baseTween.timeScale(dir * velocity < 0 ? scale * 0.4 : scale)
          },
        })
      })
    })

    // 渲染无缝滚动标语行。
    const marqueeRow = (reverse: boolean) => {
      const items = [...marqueeWords, ...marqueeWords].map((word, i) =>
        h('span', { key: `${word}-${i}`, class: 'cs-marquee__item' }, [
          word,
          h('i', { class: 'cs-marquee__star', 'aria-hidden': 'true' }, '✦'),
        ]),
      )
      return h('div', { class: ['cs-marquee__row', { 'is-reverse': reverse }] }, [
        h('div', { class: 'cs-marquee__inner' }, items),
      ])
    }

    return () =>
      h('section', { ref: root, id: 'manifesto', class: 'cs-marquee-sec' }, [
        h('div', { class: 'cs-marquee' }, [marqueeRow(false), marqueeRow(true)]),
        h('div', { class: 'cs-mani cs-section' }, [
          h('div', { class: 'cs-mani__copy' }, [
            h('p', { class: 'cs-eyebrow' }, manifesto.lead),
            h(
              'p',
              { class: 'cs-mani__text' },
              manifesto.segments.flatMap((segment) =>
                segment.text.split(' ').filter(Boolean).map((word, wordIndex) =>
                  h(
                    'span',
                    {
                      key: `${word}-${wordIndex}-${segment.accent}`,
                      class: ['cs-mani__word', { 'is-accent': segment.accent }],
                    },
                    `${word} `,
                  ),
                ),
              ),
            ),
          ]),
          h(
            'div',
            { class: 'cs-mani__cards' },
            manifesto.cards.map((card) =>
              h('article', { key: card.label, class: 'cs-mani-card' }, [
                h('span', card.label),
                h('strong', card.value),
                h('p', card.detail),
              ]),
            ),
          ),
        ]),
      ])
  },
})

// 根据文章类型决定卡片跨度样式。
function spanClass(article: FeaturedArticle, index: number): string {
  if (article.kind === 'feature') {
    return 'cs-article cs-article--feature'
  }
  return index % 2 === 0 ? 'cs-article cs-article--wide' : 'cs-article cs-article--tall'
}

const FeaturedArticles = defineComponent({
  name: 'FeaturedArticles',
  setup() {
    const root = ref<HTMLElement | null>(null)

    useGsapContext(root, ({ reduced }) => {
      const cards = gsap.utils.toArray<HTMLElement>('.cs-article')
      if (reduced) {
        cards.forEach((card) => gsap.set(card, { clipPath: 'inset(0 0 0% 0)' }))
        return
      }

      cards.forEach((card, i) => {
        const isFeature = card.classList.contains('cs-article--feature')
        gsap.fromTo(
          card,
          { clipPath: 'inset(0 0 100% 0)' },
          {
            clipPath: 'inset(0 0 0% 0)',
            duration: isFeature ? 1.25 : 0.9,
            delay: (i % 3) * 0.08,
            ease: 'power3.inOut',
            scrollTrigger: {
              trigger: card,
              start: 'top 85%',
            },
          },
        )
        gsap.from(card.querySelector('.cs-article__wash'), {
          yPercent: 12,
          duration: isFeature ? 1.4 : 1,
          ease: 'power3.out',
          scrollTrigger: { trigger: card, start: 'top 85%' },
        })
      })
    })

    // 根据指针位置给文章卡片添加轻微倾斜。
    const onTilt = (event: PointerEvent) => {
      const card = event.currentTarget as HTMLElement
      const rect = card.getBoundingClientRect()
      const px = (event.clientX - rect.left) / rect.width - 0.5
      const py = (event.clientY - rect.top) / rect.height - 0.5
      gsap.to(card, {
        rotateY: px * 5,
        rotateX: -py * 5,
        transformPerspective: 900,
        duration: 0.5,
        ease: 'power2.out',
      })
    }

    // 指针离开后复位文章卡片倾斜。
    const onTiltOut = (event: PointerEvent) => {
      gsap.to(event.currentTarget as HTMLElement, {
        rotateY: 0,
        rotateX: 0,
        duration: 0.7,
        ease: 'power3.out',
      })
    }

    // 渲染精选文章卡片。
    const renderCard = (article: FeaturedArticle, index: number) =>
      h(
        'a',
        {
          key: article.id,
          href: `/articles#${article.id}`,
          class: spanClass(article, index),
          style: { '--cs-from': article.cover[0], '--cs-to': article.cover[1] },
          onPointermove: onTilt,
          onPointerleave: onTiltOut,
        },
        [
          h('span', {
            class: 'cs-article__wash',
            style: { '--cs-from': article.cover[0], '--cs-to': article.cover[1] },
          }),
          h('div', { class: 'cs-article__body' }, [
            h(
              'div',
              { class: 'cs-article__tags' },
              article.tags.map((tag) => h('span', { class: 'cs-tag', key: tag }, tag)),
            ),
            h('h3', { class: 'cs-article__title' }, article.title),
            h('p', { class: 'cs-article__excerpt' }, article.excerpt),
            h('div', { class: 'cs-article__meta' }, [
              h('span', `${article.readingMinutes} 分钟阅读`),
              h('span', article.publishedAt),
            ]),
          ]),
        ],
      )

    return () =>
      h('section', { ref: root, class: 'cs-articles cs-section', id: 'articles' }, [
        h('div', { class: 'cs-head' }, [
          h('div', [
            h('p', { class: 'cs-eyebrow' }, 'Featured Writing'),
            h('h2', { class: 'cs-head__title' }, '让文章像主题房间，而不是普通列表'),
          ]),
          h(
            'p',
            { class: 'cs-head__note' },
            '精选文章会带着封面、标签和摘要出现，读者能先感受到这篇内容的气质。',
          ),
        ]),
        h(
          'div',
          { class: 'cs-articles__grid' },
          featuredArticles.map((article, index) => renderCard(article, index)),
        ),
      ])
  },
})

const PortfolioGallery = defineComponent({
  name: 'PortfolioGallery',
  setup() {
    const root = ref<HTMLElement | null>(null)
    const stacked = ref(isStackedViewport())
    const stackQuery =
      typeof window !== 'undefined' ? window.matchMedia('(max-width: 900px)') : null
    let matchMediaContext: gsap.MatchMedia | null = null

    // 判断当前是否应使用堆叠布局。
    function isStackedViewport(): boolean {
      if (typeof window === 'undefined') {
        return false
      }
      return window.matchMedia('(max-width: 900px)').matches
    }

    // 响应堆叠布局媒体查询变化。
    const onStackChange = (event: MediaQueryListEvent) => {
      stacked.value = event.matches
    }

    onMounted(() => {
      stackQuery?.addEventListener('change', onStackChange)
      matchMediaContext = gsap.matchMedia()
      matchMediaContext.add(
        '(min-width: 901px) and (prefers-reduced-motion: no-preference)',
        () => {
          const pin = root.value?.querySelector<HTMLElement>('.cs-gallery__pin')
          const track = root.value?.querySelector<HTMLElement>('.cs-gallery__track')
          if (!pin || !track) {
            return
          }
          const getScrollLength = () => Math.max(0, track.scrollWidth - window.innerWidth)

          const tween = gsap.to(track, {
            x: () => -getScrollLength(),
            ease: 'none',
            scrollTrigger: {
              trigger: pin,
              start: 'top top',
              end: () => `+=${getScrollLength()}`,
              pin: true,
              scrub: 0.6,
              invalidateOnRefresh: true,
              anticipatePin: 1,
            },
          })
          gsap.utils.toArray<HTMLElement>('.cs-poster').forEach((poster) => {
            const layer = poster.querySelector('.cs-poster__parallax')
            if (!layer) {
              return
            }
            gsap.fromTo(
              layer,
              { xPercent: -8 },
              {
                xPercent: 8,
                ease: 'none',
                scrollTrigger: {
                  trigger: poster,
                  containerAnimation: tween,
                  start: 'left right',
                  end: 'right left',
                  scrub: true,
                },
              },
            )
          })
        },
        root.value ?? undefined,
      )
    })

    onBeforeUnmount(() => {
      stackQuery?.removeEventListener('change', onStackChange)
      matchMediaContext?.revert()
      matchMediaContext = null
    })

    // 渲染作品海报卡片。
    const renderPoster = (project: PortfolioProject) =>
      h(
        'article',
        {
          key: project.id,
          class: 'cs-poster',
          style: {
            '--cs-from': project.palette.from,
            '--cs-to': project.palette.to,
            '--cs-accent': project.palette.accent,
          },
        },
        [
          h('span', { class: 'cs-poster__parallax', style: { '--cs-accent': project.palette.accent } }),
          h('span', { class: 'cs-poster__grain' }),
          h('div', { class: 'cs-poster__content' }, [
            h('div', { class: 'cs-poster__top' }, [
              h('span', { class: 'cs-poster__index' }, `${project.index} / ${project.year}`),
            ]),
            h('div', [
              h('span', { class: 'cs-poster__cat' }, project.category),
              h('h3', { class: 'cs-poster__title' }, project.title),
              h('p', { class: 'cs-poster__desc' }, project.description),
              h(
                'div',
                { class: 'cs-poster__stack' },
                project.stack.map((tech) => h('span', { key: tech }, tech)),
              ),
            ]),
          ]),
        ],
      )

    return () =>
      h(
        'section',
        {
          ref: root,
          id: 'works',
          class: ['cs-gallery', { 'cs-gallery--stacked': stacked.value }],
        },
        [
          h('div', { class: 'cs-gallery__pin' }, [
            h('div', { class: 'cs-gallery__track' }, [
              h('div', { class: 'cs-gallery__intro' }, [
                h('p', { class: 'cs-eyebrow' }, '作品橱窗'),
                h('h2', '作品是博客之外的另一种表达'),
                h(
                  'p',
                  '这里展示能看见形状的创作：页面、工具、视觉实验和长期打磨过的小作品。',
                ),
                h('span', { class: 'cs-gallery__hint' }, stacked.value ? '继续往下' : '横向浏览作品 →'),
              ]),
              ...portfolioProjects.map(renderPoster),
              h('div', { class: 'cs-gallery__outro' }, [
                h('p', { class: 'cs-eyebrow' }, 'End of wall'),
                h('h2', { class: 'cs-gallery__outro-title' }, '更多作品正在归档中'),
              ]),
            ]),
          ]),
        ],
      )
  },
})

const SiteStructureShowcase = defineComponent({
  name: 'SiteStructureShowcase',
  setup() {
    const root = ref<HTMLElement | null>(null)
    const stage = ref<HTMLElement | null>(null)
    const spineEl = ref<SVGPathElement | null>(null)
    const pulseEl = ref<SVGPathElement | null>(null)
    const svgW = ref(1200)
    const svgH = ref(360)
    const spineD = ref('')
    let resizeObserver: ResizeObserver | null = null

    // 根据节点位置生成平滑连线路径。
    function buildSpine(nodeXs: number[], width: number, y: number, amp: number): string {
      if (nodeXs.length === 0) {
        return `M 0 ${y} L ${width} ${y}`
      }
      const points = [0, ...nodeXs, width]
      let d = `M ${points[0]} ${y}`
      for (let i = 1; i < points.length; i++) {
        const prev = points[i - 1] as number
        const curr = points[i] as number
        const midX = (prev + curr) / 2
        const bow = i % 2 === 0 ? -amp : amp
        d += ` C ${midX} ${y + bow}, ${midX} ${y - bow}, ${curr} ${y}`
      }
      return d
    }

    // 重新测量卡片节点并更新路径。
    const remeasure = () => {
      const stageEl = stage.value
      if (!stageEl) {
        return
      }
      const rect = stageEl.getBoundingClientRect()
      svgW.value = rect.width
      svgH.value = rect.height
      const cards = Array.from(stageEl.querySelectorAll<HTMLElement>('.cs-agent'))
      const y = rect.height / 2
      const xs = cards.map((card) => {
        const cr = card.getBoundingClientRect()
        return cr.left - rect.left + cr.width / 2
      })
      spineD.value = buildSpine(xs, rect.width, y, Math.min(40, rect.height * 0.12))
      spineEl.value?.setAttribute('d', spineD.value)
      pulseEl.value?.setAttribute('d', spineD.value)
    }

    useGsapContext(root, ({ reduced }) => {
      remeasure()
      if (reduced) {
        return
      }
      const spine = spineEl.value
      const pulse = pulseEl.value
      if (spine && pulse) {
        const len = spine.getTotalLength()
        gsap.set(spine, { strokeDasharray: len, strokeDashoffset: len })
        gsap.to(spine, {
          strokeDashoffset: 0,
          duration: 1.4,
          ease: 'power2.inOut',
          scrollTrigger: { trigger: stage.value as Element, start: 'top 78%' },
        })
        const pulseLen = pulse.getTotalLength()
        const dash = Math.max(60, pulseLen * 0.08)
        pulse.setAttribute('stroke-dasharray', `${dash} ${pulseLen}`)
        gsap.fromTo(
          pulse,
          { strokeDashoffset: pulseLen },
          {
            strokeDashoffset: -dash,
            duration: 3.4,
            ease: 'none',
            repeat: -1,
            scrollTrigger: { trigger: stage.value as Element, start: 'top 80%' },
          },
        )
      }
      gsap.utils.toArray<HTMLElement>('.cs-agent').forEach((card, cardIndex) => {
        const nodes = Array.from(card.querySelectorAll<HTMLElement>('.cs-pipe__node'))
        const loop = gsap.timeline({ repeat: -1, repeatDelay: 0.6, delay: cardIndex * 0.5 })
        nodes.forEach((node) => {
          loop
            .call(() => node.classList.add('is-active'))
            .to({}, { duration: 0.55 })
            .call(() => node.classList.remove('is-active'))
        })
        ScrollTrigger.create({
          trigger: card,
          start: 'top 85%',
          onEnter: () => loop.play(0),
          onLeaveBack: () => loop.pause(0),
        })
      })
    })

    onMounted(() => {
      if (prefersReducedMotion()) {
        return
      }
      resizeObserver = new ResizeObserver(() => {
        remeasure()
        ScrollTrigger.refresh()
      })
      if (stage.value) {
        resizeObserver.observe(stage.value)
      }
    })

    onBeforeUnmount(() => {
      resizeObserver?.disconnect()
      resizeObserver = null
    })

    // 渲染站点能力卡片。
    const renderAgent = (agent: AgentCapability) =>
      h(
        'article',
        {
          key: agent.id,
          class: 'cs-agent',
          style: { '--cs-agent-accent': agent.accent },
        },
        [
          h('div', { class: 'cs-agent__top' }, [
            h('span', { class: 'cs-agent__glyph' }, [h('i')]),
            h('span', { class: 'cs-agent__role' }, agent.role),
          ]),
          h('h3', { class: 'cs-agent__name' }, agent.name),
          h('p', { class: 'cs-agent__summary' }, agent.summary),
          h(
            'div',
            { class: 'cs-pipe' },
            agent.pipeline.flatMap((step, i) => {
              const node = h('div', { class: 'cs-pipe__node', key: step.stage }, [
                h('div', { class: 'cs-pipe__dot' }),
                h('div', { class: 'cs-pipe__label' }, step.label),
              ])
              return i < agent.pipeline.length - 1
                ? [node, h('div', { class: 'cs-pipe__link', key: `${step.stage}-link` })]
                : [node]
            }),
          ),
          h(
            'div',
            { class: 'cs-agent__outputs' },
            agent.outputs.map((out) => h('span', { key: out }, out)),
          ),
        ],
      )

    return () =>
      h('section', { ref: root, class: 'cs-agents cs-section', id: 'agents' }, [
        h('div', { class: 'cs-head' }, [
          h('div', [
            h('p', { class: 'cs-eyebrow' }, 'Site Structure'),
            h('h2', { class: 'cs-head__title' }, '一个个人博客，需要自己的整理方式'),
          ]),
          h(
            'p',
            { class: 'cs-head__note' },
            '主题索引、作品橱窗和灵感卡片一起工作，让内容不只是被发布，而是被安放。',
          ),
        ]),
        h('div', { ref: stage, class: 'cs-agents__stage' }, [
          h(
            'svg',
            {
              class: 'cs-agents__wires',
              viewBox: `0 0 ${svgW.value} ${svgH.value}`,
              preserveAspectRatio: 'none',
            },
            [
              h('path', { ref: spineEl, class: 'cs-wire', d: spineD.value }),
              h('path', { ref: pulseEl, class: 'cs-wire cs-wire--live', d: spineD.value }),
            ],
          ),
          ...agentCapabilities.map(renderAgent),
        ]),
      ])
  },
})

const ApproachProcess = defineComponent({
  name: 'ApproachProcess',
  setup() {
    const root = ref<HTMLElement | null>(null)
    const activeIndex = ref(0)

    useGsapContext(root, ({ reduced }) => {
      const steps = gsap.utils.toArray<HTMLElement>('.cs-step')
      steps.forEach((step, index) => {
        ScrollTrigger.create({
          trigger: step,
          start: 'top 60%',
          end: 'bottom 60%',
          onToggle: (self) => {
            if (self.isActive) {
              activeIndex.value = index
            }
          },
        })
        if (!reduced) {
          gsap.from(step, {
            opacity: 0,
            y: 40,
            duration: 0.7,
            ease: 'power3.out',
            scrollTrigger: { trigger: step, start: 'top 82%' },
          })
        }
      })
      gsap.utils.toArray<HTMLElement>('.cs-counter__value').forEach((el) => {
        const target = Number(el.dataset.target || '0')
        const suffix = el.dataset.suffix || ''
        if (reduced) {
          el.textContent = `${target}${suffix}`
          return
        }
        const obj = { v: 0 }
        gsap.to(obj, {
          v: target,
          duration: 1.8,
          ease: 'power2.out',
          scrollTrigger: { trigger: el, start: 'top 88%' },
          onUpdate: () => {
            el.textContent = `${Math.round(obj.v)}${suffix}`
          },
        })
      })
    })

    // 渲染流程步骤。
    const renderStep = (step: ApproachStep) =>
      h('article', { key: step.no, class: 'cs-step' }, [
        h('div', { class: 'cs-step__head' }, [
          h('span', { class: 'cs-step__no' }, step.no),
          h('div', [
            h('h3', { class: 'cs-step__title' }, [
              step.title,
              h('span', { class: 'cs-step__en' }, step.en),
            ]),
          ]),
        ]),
        h('p', { class: 'cs-step__body' }, step.body),
        h(
          'div',
          { class: 'cs-step__tags' },
          step.tags.map((tag) => h('span', { key: tag, class: 'cs-tag-line' }, tag)),
        ),
      ])

    return () =>
      h('section', { ref: root, class: 'cs-approach cs-section', id: 'approach' }, [
        h('div', { class: 'cs-head' }, [
          h('div', [
            h('p', { class: 'cs-eyebrow' }, 'How It Works'),
            h('h2', { class: 'cs-head__title' }, '从碎片到作品的四步'),
          ]),
          h(
            'p',
            { class: 'cs-head__note' },
            'A creative pipeline — capture, connect, compose, curate. 系统让创作有迹可循。',
          ),
        ]),
        h('div', { class: 'cs-approach__grid' }, [
          h('aside', { class: 'cs-rail' }, [
            h('div', { class: 'cs-rail__num' }, approachSteps[activeIndex.value]?.no ?? '01'),
            h('div', { class: 'cs-rail__label' }, approachSteps[activeIndex.value]?.en ?? ''),
            h(
              'div',
              { class: 'cs-rail__track' },
              approachSteps.map((step, index) =>
                h('span', {
                  key: step.no,
                  class: ['cs-rail__tick', { 'is-active': index === activeIndex.value }],
                }),
              ),
            ),
          ]),
          h('div', { class: 'cs-steps' }, approachSteps.map(renderStep)),
        ]),
        h(
          'div',
          { class: 'cs-counters' },
          counters.map((counter) =>
            h('div', { class: 'cs-counter', key: counter.label }, [
              h(
                'span',
                {
                  class: 'cs-counter__value',
                  'data-target': String(counter.value),
                  'data-suffix': counter.suffix,
                },
                `0${counter.suffix}`,
              ),
              h('span', { class: 'cs-counter__label' }, counter.label),
            ]),
          ),
        ),
      ])
  },
})

const CreativeWall = defineComponent({
  name: 'CreativeWall',
  setup() {
    const root = ref<HTMLElement | null>(null)
    const detachers: Array<() => void> = []

    useGsapContext(root, ({ reduced }) => {
      const cards = gsap.utils.toArray<HTMLElement>('.cs-frag')

      if (!reduced) {
        gsap.from(cards, {
          opacity: 0,
          scale: 0.86,
          filter: 'blur(8px)',
          duration: 0.8,
          ease: 'power3.out',
          stagger: { each: 0.06, from: 'center', grid: 'auto' },
          scrollTrigger: { trigger: root.value as Element, start: 'top 72%' },
        })
      }
      cards.forEach((card) => {
        const inner = card.querySelector<HTMLElement>('.cs-frag__inner')
        detachers.push(attachMagnetic(card, { inner, strength: 0.22, innerStrength: 0.4 }))
      })
    })

    onBeforeUnmount(() => {
      detachers.forEach((off) => off())
      detachers.length = 0
    })

    // 渲染创意墙卡片。
    const renderFragment = (fragment: CreativeFragment) => {
      const style: Record<string, string> = {
        gridColumn: `span ${fragment.span.col}`,
        gridRow: `span ${fragment.span.row}`,
      }
      if (fragment.palette) {
        style['--cs-from'] = fragment.palette[0]
        style['--cs-to'] = fragment.palette[1]
      }

      const inner = [
        h('span', { class: 'cs-frag__kind' }, fragment.label),
        h('p', { class: 'cs-frag__body' }, fragment.body),
      ]
      if (fragment.kind === 'link') {
        inner.push(h('span', { class: 'cs-frag__link' }, '↗ open'))
      } else if (fragment.meta) {
        inner.push(h('span', { class: 'cs-frag__meta' }, fragment.meta))
      }

      return h(
        'div',
        { key: fragment.id, class: `cs-frag cs-frag--${fragment.kind}`, style },
        [
          fragment.palette ? h('span', { class: 'cs-frag__wash' }) : null,
          h('div', { class: 'cs-frag__inner' }, inner),
        ],
      )
    }

    return () =>
      h('section', { ref: root, class: 'cs-wall cs-section', id: 'wall' }, [
        h('div', { class: 'cs-head' }, [
          h('div', [
            h('p', { class: 'cs-eyebrow' }, 'Creative Wall'),
            h('h2', { class: 'cs-head__title' }, '灵感碎片，尚未成文的思考'),
          ]),
          h(
            'p',
            { class: 'cs-head__note' },
            'Prompts、片段、笔记与参考——创作开始之前的原材料。',
          ),
        ]),
        h('div', { class: 'cs-wall__grid' }, creativeFragments.map(renderFragment)),
      ])
  },
})

const ThemeUniverse = defineComponent({
  name: 'ThemeUniverse',
  setup() {
    const activeId = ref<string>(themePresets[0]?.id ?? '')
    const preview = ref<HTMLElement | null>(null)

    // 把主题变量写入预览容器。
    const applyVars = (preset: ThemePreset) => {
      const el = preview.value
      if (!el) {
        return
      }
      const vars = preset.vars
      el.style.setProperty('--tp-bg', vars.bg)
      el.style.setProperty('--tp-surface', vars.surface)
      el.style.setProperty('--tp-ink', vars.ink)
      el.style.setProperty('--tp-muted', vars.muted)
      el.style.setProperty('--tp-accent', vars.accent)
      el.style.setProperty('--tp-accent-soft', vars.accentSoft)
      el.style.setProperty('--tp-font', vars.font)
    }

    // 切换当前预览主题。
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

    // 保存主题预览容器引用。
    const setPreviewRef = (el: Element | null) => {
      preview.value = el as HTMLElement | null
      const first = themePresets.find((preset) => preset.id === activeId.value)
      if (first) {
        applyVars(first)
      }
    }

    onBeforeUnmount(() => {
      if (preview.value) {
        gsap.killTweensOf(preview.value.querySelectorAll('.cs-tp-fade'))
      }
    })

    // 渲染主题切换按钮。
    const renderThemeButton = (preset: ThemePreset) =>
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
            preset.swatches.map((color, index) => h('span', { key: index, style: { background: color } })),
          ),
          h('span', [
            h('span', { class: 'cs-theme-btn__name' }, preset.name),
            h('span', { class: 'cs-theme-btn__tag' }, preset.tagline),
          ]),
        ],
      )

    // 读取当前激活主题配置。
    const activePreset = () => themePresets.find((preset) => preset.id === activeId.value) ?? themePresets[0]

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
          h('div', { class: 'cs-themes__list' }, themePresets.map(renderThemeButton)),
          h('div', { class: 'cs-theme-preview', ref: setPreviewRef }, [
            h('p', { class: 'cs-theme-preview__eyebrow cs-tp-fade' }, active?.mood ?? ''),
            h('h3', { class: 'cs-theme-preview__title cs-tp-fade' }, active?.name ?? ''),
            h(
              'p',
              { class: 'cs-theme-preview__text cs-tp-fade' },
              '文章、作品与灵感会跟着主题重新着色，读起来像同一个人慢慢整理出的空间。',
            ),
            h('div', { class: 'cs-theme-preview__cards cs-tp-fade' }, [
              h('div', { class: 'cs-theme-preview__card' }, [
                h('strong', '128'),
                h('span', '文章'),
              ]),
              h('div', { class: 'cs-theme-preview__card' }, [
                h('strong', '36'),
                h('span', '作品'),
              ]),
            ]),
            h('span', { class: 'cs-theme-preview__chip cs-tp-fade' }, '应用主题'),
          ]),
        ]),
      ])
    }
  },
})

const FieldNotes = defineComponent({
  name: 'FieldNotes',
  setup() {
    const root = ref<HTMLElement | null>(null)

    useGsapContext(root, ({ reduced }) => {
      if (reduced) {
        return
      }
      gsap.utils.toArray<HTMLElement>('.cs-note').forEach((row, index) => {
        gsap.from(row, {
          yPercent: 40,
          opacity: 0,
          duration: 0.7,
          ease: 'power3.out',
          delay: (index % 4) * 0.05,
          scrollTrigger: { trigger: row, start: 'top 88%' },
        })
        const rule = row.querySelector('.cs-note__rule')
        if (rule) {
          gsap.from(rule, {
            scaleX: 0,
            transformOrigin: 'left',
            duration: 0.9,
            ease: 'power3.inOut',
            scrollTrigger: { trigger: row, start: 'top 88%' },
          })
        }
      })
    })

    // 渲染创作记录条目。
    const renderNote = (note: FieldNote, index: number) =>
      h('article', { key: index, class: 'cs-note' }, [
        h('span', { class: 'cs-note__rule' }),
        h('div', { class: 'cs-note__row' }, [
          h('span', { class: 'cs-note__date' }, note.date),
          h('span', { class: 'cs-note__tag' }, note.tag),
          h('h3', { class: 'cs-note__title' }, note.title),
          h('span', { class: 'cs-note__arrow', 'aria-hidden': 'true' }, '→'),
        ]),
        h('p', { class: 'cs-note__detail' }, note.detail),
      ])

    return () =>
      h('section', { ref: root, class: 'cs-notes cs-section', id: 'notes' }, [
        h('div', { class: 'cs-head' }, [
          h('div', [
            h('p', { class: 'cs-eyebrow' }, 'Development Log'),
            h('h2', { class: 'cs-head__title' }, '按模块记录开发历程'),
          ]),
          h(
            'p',
            { class: 'cs-head__note' },
            '这里只记录模块级进展，方便回看这个个人博客与作品平台是怎样一步步搭起来的。',
          ),
        ]),
        h('div', { class: 'cs-notes__list' }, fieldNotes.map(renderNote)),
      ])
  },
})

const CLOSING_LINE = '从一篇文章开始'

const FinalCTA = defineComponent({
  name: 'FinalCTA',
  setup() {
    const root = ref<HTMLElement | null>(null)
    const glow = ref<HTMLElement | null>(null)
    let glowX: gsap.QuickToFunc | null = null
    let glowY: gsap.QuickToFunc | null = null

    useGsapContext(root, ({ reduced }) => {
      if (reduced) {
        return
      }
      gsap.from('.cs-cta__line', {
        y: 16,
        opacity: 0,
        duration: 0.8,
        ease: 'expo.out',
        scrollTrigger: { trigger: root.value as Element, start: 'top 75%' },
      })
      if (glow.value) {
        glowX = gsap.quickTo(glow.value, 'x', { duration: 0.8, ease: 'power3.out' })
        glowY = gsap.quickTo(glow.value, 'y', { duration: 0.8, ease: 'power3.out' })
      }
    })

    // 根据指针位置移动光晕。
    const onMove = (event: PointerEvent) => {
      const el = root.value
      if (!el || !glowX || !glowY) {
        return
      }
      const rect = el.getBoundingClientRect()
      glowX(event.clientX - rect.left - rect.width / 2)
      glowY(event.clientY - rect.top - rect.height / 2)
    }

    // 处理单字悬停动效。
    const onCharOver = (event: PointerEvent) => {
      if (prefersReducedMotion()) {
        return
      }
      const target = event.target as HTMLElement
      if (target.classList.contains('cs-char')) {
        gsap.to(target, { yPercent: -18, color: 'var(--cs-accent-3)', duration: 0.3, ease: 'power2.out' })
        gsap.to(target, { yPercent: 0, color: 'inherit', duration: 0.6, delay: 0.15, ease: 'power3.out' })
      }
    }

    onBeforeUnmount(() => {
      if (glow.value) {
        gsap.killTweensOf(glow.value)
      }
    })

    // 拆分并渲染行动标题字符。
    const chars = () =>
      CLOSING_LINE.split('').map((char, index) =>
        char === ' '
          ? h('span', { key: index, class: 'cs-char', innerHTML: '&nbsp;' })
          : h('span', { key: index, class: 'cs-char' }, char),
      )

    return () =>
      h('section', { ref: root, class: 'cs-cta-wrap', id: 'enter', onPointermove: onMove }, [
        h('div', { class: 'cs-cta' }, [
          h('div', { ref: glow, class: 'cs-cta__glow' }),
          h('p', { class: 'cs-eyebrow cs-cta__eyebrow' }, closing.eyebrow),
          h('h2', { class: 'cs-cta__line', onPointerover: onCharOver }, chars()),
          h(
            'p',
            { class: 'cs-cta__sub' },
            '先随便读一篇，再决定要不要留下来逛作品和灵感卡片。',
          ),
          h('div', { class: 'cs-cta__action' }, [
            h(
              RouterLink,
              { to: '/articles', class: 'cs-btn' },
              {
                default: () => [
                  h('span', { class: 'cs-btn__fill' }),
                  h('span', { class: 'cs-btn__dot' }),
                  h('span', '进入博客'),
                ],
              },
            ),
          ]),
        ]),
        renderFooter(),
      ])
  },
})

// 渲染首页页脚。
function renderFooter() {
  const github = siteConfig.social.find((item) => item.label === 'GitHub')
  const mail = siteConfig.social.find((item) => item.label === 'Mail')

  return h('footer', { class: 'cs-footer' }, [
    h('span', `© ${new Date().getFullYear()} ${siteConfig.wordmark}`),
    h(
      'a',
      {
        href: 'https://github.com/SakuraCianna/CreatorSpace',
        target: '_blank',
        rel: 'noreferrer',
      },
      `GitHub · ${github?.handle ?? 'github.com/SakuraCianna/CreatorSpace'}`,
    ),
    h(
      'a',
      {
        href: 'mailto:754515922@qq.com',
      },
      `Mail · ${mail?.handle ?? '754515922@qq.com'}`,
    ),
  ])
}
</script>
