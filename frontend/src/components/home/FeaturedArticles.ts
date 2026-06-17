import { defineComponent, h, ref } from 'vue'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

import { useGsapContext } from '@/composables/useGsapScroll'
import { featuredArticles, type FeaturedArticle } from '@/data/home-content'

gsap.registerPlugin(ScrollTrigger)
function spanClass(article: FeaturedArticle, index: number): string {
  if (article.kind === 'feature') {
    return 'cs-article cs-article--feature'
  }
  return index % 2 === 0 ? 'cs-article cs-article--wide' : 'cs-article cs-article--tall'
}

export default defineComponent({
  name: 'FeaturedArticles',
  setup() {
    const root = ref<HTMLElement | null>(null)

    useGsapContext(root, ({ reduced }) => {
      const cards = gsap.utils.toArray<HTMLElement>('.cs-article')
      if (reduced) {
        cards.forEach((c) => gsap.set(c, { clipPath: 'inset(0 0 0% 0)' }))
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
    const onTiltOut = (event: PointerEvent) => {
      gsap.to(event.currentTarget as HTMLElement, {
        rotateY: 0,
        rotateX: 0,
        duration: 0.7,
        ease: 'power3.out',
      })
    }
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
              h('span', `${article.readingMinutes} min read`),
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
            h('h2', { class: 'cs-head__title' }, '博客不是时间线，是一个思想的宇宙'),
          ]),
          h(
            'p',
            { class: 'cs-head__note' },
            'Selected articles — 每一篇都被切片、连接，并可被站内智能体追问。',
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
