import { describe, it, expect } from 'vitest'
import { formatDateTimeToSecond, formatDateToDay, formatMonthDay } from '../../shared/datetime'

describe('datetime', () => {
  it('formatDateTimeToSecond formats correctly', () => {
    expect(formatDateTimeToSecond('2026-07-01T10:13:22.844+08:00')).toBe('2026-07-01 10:13:22')
    expect(formatDateTimeToSecond(null)).toBe('未记录时间')
  })

  it('formatDateToDay formats correctly', () => {
    expect(formatDateToDay('2026-07-01T10:13:22.844+08:00')).toBe('2026-07-01')
    expect(formatDateToDay(null)).toBe('未定档')
  })

  it('formatMonthDay formats correctly', () => {
    expect(formatMonthDay('2026-07-01T10:13:22.844+08:00')).toBe('07-01')
    expect(formatMonthDay(null)).toBe('未定档')
  })
})
