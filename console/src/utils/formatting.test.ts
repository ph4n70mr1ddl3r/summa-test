import { describe, it, expect } from 'vitest'
import { formatDate, tierColor, spawnStatusColor } from './formatting'

describe('formatDate', () => {
  it('returns ? for null', () => {
    expect(formatDate(null)).toBe('?')
  })

  it('returns ? for undefined', () => {
    expect(formatDate(undefined)).toBe('?')
  })

  it('formats epoch seconds as local datetime', () => {
    const d = formatDate(1700000000)
    expect(d).not.toBe('?')
    expect(d.length).toBeGreaterThan(0)
  })

  it('formats date-only when requested', () => {
    const result = formatDate(1700000000, { dateOnly: true })
    expect(result).not.toBe('?')
  })
})

describe('tierColor', () => {
  it('returns red for critical', () => {
    expect(tierColor('critical')).toContain('red')
  })

  it('returns yellow for standard', () => {
    expect(tierColor('standard')).toContain('yellow')
  })

  it('returns gray for bulk', () => {
    expect(tierColor('bulk')).toContain('gray')
  })

  it('defaults to gray', () => {
    expect(tierColor('unknown')).toContain('gray')
  })
})

describe('spawnStatusColor', () => {
  it('returns yellow for requested', () => {
    expect(spawnStatusColor('requested')).toContain('yellow')
  })

  it('returns green for approved', () => {
    expect(spawnStatusColor('approved')).toContain('green')
  })

  it('returns red for denied', () => {
    expect(spawnStatusColor('denied')).toContain('red')
  })

  it('defaults to gray', () => {
    expect(spawnStatusColor('unknown')).toContain('gray')
  })
})
