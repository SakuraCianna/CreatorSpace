const allowedImageUrlPattern = /^(https?:|data:image\/|\/|\.\/|\.\.\/)/i

// 将普通图片地址转换为符合 CSS url 格式的安全属性值
// 对封面图或横幅 URL 字符串进行安全字符转义包装为 css 属性专用的 url 值
// 对封面图或横幅 URL 字符串进行安全字符转义包装为 css 属性专用的 url 值
export function toCssImageUrl(url?: string | null) {
  const trimmed = url?.trim()
  if (!trimmed || !allowedImageUrlPattern.test(trimmed)) {
    return 'none'
  }

  const escaped = trimmed.replace(/\\/g, '\\\\').replace(/"/g, '\\"')
  return `url("${escaped}")`
}
