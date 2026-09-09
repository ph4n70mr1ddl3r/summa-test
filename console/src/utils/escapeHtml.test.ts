import { describe, it, expect } from 'vitest'
import { escapeHtml } from './escapeHtml'

describe('escapeHtml', () => {
  it('escapes ampersand', () => {
    expect(escapeHtml('a & b')).toBe('a &amp; b')
  })

  it('escapes angle brackets', () => {
    expect(escapeHtml('<script>alert(1)</script>')).toBe('&lt;script&gt;alert(1)&lt;/script&gt;')
  })

  it('escapes double quotes', () => {
    expect(escapeHtml('say "hello"')).toBe('say &quot;hello&quot;')
  })

  it('escapes single quotes', () => {
    expect(escapeHtml("it's")).toBe('it&#x27;s')
  })

  it('handles empty string', () => {
    expect(escapeHtml('')).toBe('')
  })

  it('handles string with no special characters', () => {
    expect(escapeHtml('hello world')).toBe('hello world')
  })

  it('escapes all special characters together', () => {
    expect(escapeHtml('<a href="x" onclick="alert(1)">click</a>')).toBe(
      '&lt;a href=&quot;x&quot; onclick=&quot;alert(1)&quot;&gt;click&lt;/a&gt;'
    )
  })
})
