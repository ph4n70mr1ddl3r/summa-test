import { describe, it, expect } from 'vitest'
import { escapeHtml } from './escapeHtml'

describe('escapeHtml', () => {
  it('does not HTML-encode ampersand (React escapes on its own)', () => {
    expect(escapeHtml('a & b')).toBe('a & b')
  })

  it('does not HTML-encode angle brackets', () => {
    expect(escapeHtml('<script>alert(1)</script>')).toBe('<script>alert(1)</script>')
  })

  it('does not HTML-encode double quotes', () => {
    expect(escapeHtml('say "hello"')).toBe('say "hello"')
  })

  it('does not HTML-encode single quotes', () => {
    expect(escapeHtml("it's")).toBe("it's")
  })

  it('handles empty string', () => {
    expect(escapeHtml('')).toBe('')
  })

  it('handles string with no special characters', () => {
    expect(escapeHtml('hello world')).toBe('hello world')
  })

  it('preserves newlines as literal newline characters', () => {
    expect(escapeHtml('line1\nline2')).toBe('line1\nline2')
  })

  it('normalizes CRLF to LF', () => {
    expect(escapeHtml('line1\r\nline2')).toBe('line1\nline2')
  })

  it('does not HTML-encode mixed special characters', () => {
    expect(escapeHtml('<a href="x" onclick="alert(1)">click</a>')).toBe('<a href="x" onclick="alert(1)">click</a>')
  })

  it('returns empty string for null', () => {
    expect(escapeHtml(null)).toBe('')
  })

  it('returns empty string for undefined', () => {
    expect(escapeHtml(undefined)).toBe('')
  })

  it('does not encode backticks', () => {
    expect(escapeHtml('`code`')).toBe('`code`')
  })

  it('does not escape forward slashes', () => {
    expect(escapeHtml('http://example.com')).toBe('http://example.com')
  })

  it('strips control characters', () => {
    expect(escapeHtml('hello\x00world')).toBe('helloworld')
    expect(escapeHtml('line\x01\x02\x03')).toBe('line')
  })
})
