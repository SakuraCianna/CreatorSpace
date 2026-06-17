import { defineComponent, h, onBeforeUnmount, onMounted } from 'vue'

import HeroUniverse from '@/components/home/HeroUniverse'
import MarqueeManifesto from '@/components/home/MarqueeManifesto'
import FeaturedArticles from '@/components/home/FeaturedArticles'
import PortfolioGallery from '@/components/home/PortfolioGallery'
import AIAgentShowcase from '@/components/home/AIAgentShowcase'
import ApproachProcess from '@/components/home/ApproachProcess'
import CreativeWall from '@/components/home/CreativeWall'
import ThemeUniverse from '@/components/home/ThemeUniverse'
import FieldNotes from '@/components/home/FieldNotes'
import FinalCTA from '@/components/home/FinalCTA'
import { useLenis } from '@/composables/useLenis'

import '@/styles/home.css'
export default defineComponent({
  name: 'HomeView',
  setup() {
    useLenis()
    onMounted(() => document.body.classList.add('cs-dark-body'))
    onBeforeUnmount(() => document.body.classList.remove('cs-dark-body'))

    return () =>
      h('div', { class: 'cs-home' }, [
        h(HeroUniverse),
        h(MarqueeManifesto),
        h(FeaturedArticles),
        h(PortfolioGallery),
        h(AIAgentShowcase),
        h(ApproachProcess),
        h(CreativeWall),
        h(ThemeUniverse),
        h(FieldNotes),
        h(FinalCTA),
      ])
  },
})
