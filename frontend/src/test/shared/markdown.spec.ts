import { describe, it, expect } from 'vitest'
import { normalizeMarkdownSource, renderSafeMarkdown } from '../../shared/markdown'

describe('markdown', () => {
  it('normalizeMarkdownSource works', () => {
    expect(normalizeMarkdownSource(null)).toBe('这段内容还在整理中。')
    expect(normalizeMarkdownSource('test')).toBe('test')
    expect(normalizeMarkdownSource('# title\\n\\ncontent')).toBe('# title\n\ncontent')
  })

  it('renderSafeMarkdown sanitizes script tags', () => {
    const html = renderSafeMarkdown('<script>alert("xss")</script>')
    expect(html).not.toContain('<script>')
  })

  it('renderSafeMarkdown renders basic markdown', () => {
    const html = renderSafeMarkdown('# Hello')
    expect(html).toContain('<h1')
    expect(html).toContain('Hello')
  })
})
