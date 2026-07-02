import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import PublicPageHeader from '../../components/common/PublicPageHeader.vue'

describe('PublicPageHeader', () => {
  it('renders correctly', () => {
    const wrapper = mount(PublicPageHeader, {
      props: { title: 'Test Title' },
      global: {
        stubs: ['RouterLink']
      }
    })
    expect(wrapper.exists()).toBe(true)
  })
})
