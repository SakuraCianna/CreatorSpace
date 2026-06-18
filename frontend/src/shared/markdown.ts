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

// 将 Markdown 转成经过清洗的 HTML，避免详情页直接插入不可信内容。
export function renderSafeMarkdown(value?: string | null): string {
  const rawHtml = markdown.render(value?.trim() || '这段内容还在整理中。')
  return DOMPurify.sanitize(rawHtml, {
    ADD_ATTR: ['target', 'rel'],
  })
}
