import { describe, it, expect } from 'vitest'

// Test buildQuery logic inline since it's not exported
function buildQuery(params?: Record<string, string | number | undefined>): string {
  const entries = Object.entries(params ?? {}).filter(([, v]) => v !== undefined && v !== '');
  if (entries.length === 0) return '';
  const qs = new URLSearchParams(entries.map(([k, v]) => [k, String(v)])).toString();
  return qs ? `?${qs}` : '';
}

describe('buildQuery', () => {
  it('returns empty string for undefined input', () => {
    expect(buildQuery()).toBe('')
  })

  it('returns empty string for empty object', () => {
    expect(buildQuery({})).toBe('')
  })

  it('returns query string for single param', () => {
    expect(buildQuery({ status: 'active' })).toBe('?status=active')
  })

  it('returns query string for multiple params', () => {
    const result = buildQuery({ status: 'active', limit: '10' })
    expect(result).toMatch(/^\?status=active&limit=10$/)
  })

  it('filters out undefined values', () => {
    expect(buildQuery({ status: 'active', limit: undefined })).toBe('?status=active')
  })

  it('filters out empty string values', () => {
    expect(buildQuery({ status: '', limit: '10' })).toBe('?limit=10')
  })

  it('encodes special characters', () => {
    expect(buildQuery({ q: 'hello world' })).toBe('?q=hello+world')
  })
})
