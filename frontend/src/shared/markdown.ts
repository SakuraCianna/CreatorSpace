import DOMPurify from 'dompurify'
import hljs from 'highlight.js/lib/core'
import bash from 'highlight.js/lib/languages/bash'
import css from 'highlight.js/lib/languages/css'
import java from 'highlight.js/lib/languages/java'
import javascript from 'highlight.js/lib/languages/javascript'
import markdownLanguage from 'highlight.js/lib/languages/markdown'
import typescript from 'highlight.js/lib/languages/typescript'
import xml from 'highlight.js/lib/languages/xml'
import markdownit from 'markdown-it'
import type MarkdownIt from 'markdown-it'

hljs.registerLanguage('bash', bash)
hljs.registerLanguage('css', css)
hljs.registerLanguage('html', xml)
hljs.registerLanguage('java', java)
hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('js', javascript)
hljs.registerLanguage('markdown', markdownLanguage)
hljs.registerLanguage('md', markdownLanguage)
hljs.registerLanguage('typescript', typescript)
hljs.registerLanguage('ts', typescript)
hljs.registerLanguage('vue', xml)

const markdown = markdownit({
  html: false,
  linkify: true,
  typographer: true,
  highlight(source, language) {
    if (language && hljs.getLanguage(language)) {
      try {
        return `<pre class="hljs"><code>${hljs.highlight(source, { language }).value}</code></pre>`
      } catch {
        return ''
      }
    }
    return `<pre class="hljs"><code>${markdown.utils.escapeHtml(source)}</code></pre>`
  },
})

const underlineRule: MarkdownIt.ParserInline.RuleInline = (state, silent) => {
  const start = state.pos
  const marker = '++'

  if (!state.src.startsWith(marker, start)) {
    return false
  }

  const end = state.src.indexOf(marker, start + marker.length)
  if (end < 0 || end === start + marker.length) {
    return false
  }

  if (!silent) {
    const originalPos = state.pos
    const originalMax = state.posMax

    const openToken = state.push('u_open', 'u', 1)
    openToken.markup = marker

    state.pos = start + marker.length
    state.posMax = end
    state.md.inline.tokenize(state)

    const closeToken = state.push('u_close', 'u', -1)
    closeToken.markup = marker

    state.pos = originalPos
    state.posMax = originalMax
  }

  state.pos = end + marker.length
  return true
}

markdown.inline.ruler.before('emphasis', 'underline', underlineRule)

export function normalizeMarkdownSource(value?: string | null): string {
  const source = value?.trim()
  if (!source) {
    return '这段内容还在整理中。'
  }
  return shouldUnescapeMarkdownNewlines(source)
    ? source.replace(/\\r\\n/g, '\n').replace(/\\n/g, '\n')
    : source
}

function shouldUnescapeMarkdownNewlines(source: string): boolean {
  if (!source.includes('\\n') || /[\r\n]/.test(source)) {
    return false
  }

  const unescaped = source.replace(/\\r\\n/g, '\n').replace(/\\n/g, '\n')
  const lines = unescaped
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)

  if (lines.length < 2) {
    return false
  }

  return lines.some((line) => /^(#{1,6}\s|>\s|[-*+]\s|\d+\.\s|```|!\[|\|)/.test(line))
}

// 将 Markdown 转成经过清洗的 HTML, 避免详情页直接插入不可信内容
// 基于 markdown-it 解析并利用 DOMPurify 净化 Markdown 富文本以抵御前端 XSS 攻击
export function renderSafeMarkdown(value?: string | null): string {
  const rawHtml = markdown.render(normalizeMarkdownSource(value))
  return DOMPurify.sanitize(rawHtml, {
    ADD_ATTR: ['target', 'rel'],
  })
}
