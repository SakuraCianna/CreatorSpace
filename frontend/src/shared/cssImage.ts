const allowedImageUrlPattern = /^(https?:|data:image\/|\/|\.\/|\.\.\/)/i

export function toCssImageUrl(url?: string | null) {
  const trimmed = url?.trim()
  if (!trimmed || !allowedImageUrlPattern.test(trimmed)) {
    return 'none'
  }

  const escaped = trimmed.replace(/\\/g, '\\\\').replace(/"/g, '\\"')
  return `url("${escaped}")`
}
