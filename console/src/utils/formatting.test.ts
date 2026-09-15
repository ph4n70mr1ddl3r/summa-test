import { describe, it, expect } from 'vitest'
import { formatDate, tierColor, spawnStatusColor, triggerStatusColor, boardTaskStatusColor, roleTemplateStatusColor } from './formatting'

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

describe('triggerStatusColor', () => {
  it('returns green for active', () => {
    expect(triggerStatusColor('active')).toContain('green')
  })

  it('returns yellow for paused', () => {
    expect(triggerStatusColor('paused')).toContain('yellow')
  })

  it('returns gray for archived', () => {
    expect(triggerStatusColor('archived')).toContain('gray')
  })

  it('defaults to gray', () => {
    expect(triggerStatusColor('unknown')).toContain('gray')
  })
})

describe('boardTaskStatusColor', () => {
  it('returns green for done', () => {
    expect(boardTaskStatusColor('done')).toContain('green')
  })

  it('returns blue for in_progress', () => {
    expect(boardTaskStatusColor('in_progress')).toContain('blue')
  })

  it('returns red for cancelled', () => {
    expect(boardTaskStatusColor('cancelled')).toContain('red')
  })

  it('returns yellow for open', () => {
    expect(boardTaskStatusColor('open')).toContain('yellow')
  })

  it('defaults to gray', () => {
    expect(boardTaskStatusColor('unknown')).toContain('gray')
  })
})

describe('roleTemplateStatusColor', () => {
  it('returns green for active', () => {
    expect(roleTemplateStatusColor('active')).toContain('green')
  })

  it('returns yellow for draft', () => {
    expect(roleTemplateStatusColor('draft')).toContain('yellow')
  })

  it('returns gray for retired', () => {
    expect(roleTemplateStatusColor('retired')).toContain('gray')
  })

  it('defaults to gray', () => {
    expect(roleTemplateStatusColor('unknown')).toContain('gray')
  })
})
