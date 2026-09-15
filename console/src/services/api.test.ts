import { describe, it, expect, beforeEach } from 'vitest'
import { buildQuery, isAuthenticated, setAuthToken, getAuthToken } from './api'

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

describe('auth helpers', () => {
  beforeEach(() => {
    setAuthToken(null)
  })

  it('isAuthenticated returns false when no token', () => {
    expect(isAuthenticated()).toBe(false)
  })

  it('isAuthenticated returns false for expired token', () => {
    const expiredToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.' +
      btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) - 3600 })) +
      '.sig'
    setAuthToken(expiredToken)
    expect(isAuthenticated()).toBe(false)
  })

  it('isAuthenticated returns true for valid token', () => {
    const futureExp = Math.floor(Date.now() / 1000) + 3600
    const validToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.' +
      btoa(JSON.stringify({ exp: futureExp })) +
      '.sig'
    setAuthToken(validToken)
    expect(isAuthenticated()).toBe(true)
  })

  it('isAuthenticated rejects alg=none', () => {
    const token = 'eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.' +
      btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 })) +
      '.sig'
    setAuthToken(token)
    expect(isAuthenticated()).toBe(false)
  })

  it('setAuthToken stores and retrieves token', () => {
    setAuthToken('test-token')
    expect(getAuthToken()).toBe('test-token')
  })

  it('setAuthToken clears token when null', () => {
    setAuthToken('test-token')
    setAuthToken(null)
    expect(getAuthToken()).toBe(null)
  })
})

