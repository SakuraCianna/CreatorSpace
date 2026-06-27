function pad(value: number): string {
  return String(value).padStart(2, '0')
}

function parseDate(value?: string | null): Date | null {
  if (!value) {
    return null
  }
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}

function rawDateTimeToSecond(value?: string | null): string {
  const text = value?.trim() ?? ''
  const matched = text.match(/^(\d{4})[-/](\d{2})[-/](\d{2})[T\s](\d{2}):(\d{2})(?::(\d{2}))?(?:[.,]\d+)?(?:Z|[+-]\d{2}(?::?\d{2})?)?/)
  if (!matched) {
    return ''
  }
  const [, year, month, day, hour, minute, second = '00'] = matched
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`
}

export function formatDateTimeToSecond(value?: string | null, fallback = '未记录时间'): string {
  const date = parseDate(value)
  if (!date) {
    return rawDateTimeToSecond(value) || fallback
  }
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

export function formatDateToDay(value?: string | null, fallback = '未定档'): string {
  const date = parseDate(value)
  if (!date) {
    return fallback
  }
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

export function formatMonthDay(value?: string | null, fallback = '未定档'): string {
  const date = parseDate(value)
  if (!date) {
    return fallback
  }
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}
