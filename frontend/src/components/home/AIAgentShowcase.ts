import { defineComponent, h, onBeforeUnmount, onMounted, ref } from 'vue'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

import { useGsapContext } from '@/composables/useGsapScroll'
import { prefersReducedMotion } from '@/composables/useReducedMotion'
import { agentCapabilities, type AgentCapability } from '@/data/home-content'

gsap.registerPlugin(ScrollTrigger)

/**
 * 根据节点位置生成平滑连线路径。
 */
function buildSpine(nodeXs: number[], width: number, y: number, amp: number): string {
  if (nodeXs.length === 0) {
    return `M 0 ${y} L ${width} ${y}`
  }
  const pts = [0, ...nodeXs, width]
  let d = `M ${pts[0]} ${y}`
  for (let i = 1; i < pts.length; i++) {
    const prev = pts[i - 1] as number
    const curr = pts[i] as number
    const midX = (prev + curr) / 2
    const bow = i % 2 === 0 ? -amp : amp
    d += ` C ${midX} ${y + bow}, ${midX} ${y - bow}, ${curr} ${y}`
  }
  return d
}

export default defineComponent({
  name: 'AIAgentShowcase',
  setup() {
    const root = ref<HTMLElement | null>(null)
    const stage = ref<HTMLElement | null>(null)
    const spineEl = ref<SVGPathElement | null>(null)
    const pulseEl = ref<SVGPathElement | null>(null)
    const svgW = ref(1200)
    const svgH = ref(360)
    const spineD = ref('')

    let ro: ResizeObserver | null = null
    /**
     * 重新测量卡片节点并更新路径。
     */
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
        const pLen = pulse.getTotalLength()
        const dash = Math.max(60, pLen * 0.08)
        pulse.setAttribute('stroke-dasharray', `${dash} ${pLen}`)
        gsap.fromTo(
          pulse,
          { strokeDashoffset: pLen },
          {
            strokeDashoffset: -dash,
            duration: 3.4,
            ease: 'none',
            repeat: -1,
            scrollTrigger: { trigger: stage.value as Element, start: 'top 80%' },
          },
        )
      }
      gsap.utils.toArray<HTMLElement>('.cs-agent').forEach((card, ci) => {
        const nodes = Array.from(card.querySelectorAll<HTMLElement>('.cs-pipe__node'))
        const loop = gsap.timeline({ repeat: -1, repeatDelay: 0.6, delay: ci * 0.5 })
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
      ro = new ResizeObserver(() => {
        remeasure()
        ScrollTrigger.refresh()
      })
      if (stage.value) {
        ro.observe(stage.value)
      }
    })

    onBeforeUnmount(() => {
      ro?.disconnect()
      ro = null
    })

    /**
     * 渲染 AI 能力卡片。
     */
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
            h('p', { class: 'cs-eyebrow' }, 'AI Native System'),
            h('h2', { class: 'cs-head__title' }, '不是聊天机器人，是创作助手控制台'),
          ]),
          h(
            'p',
            { class: 'cs-head__note' },
            '三个智能体在站内文章与作品之上协作：路由意图、检索上下文、生成结果。',
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
