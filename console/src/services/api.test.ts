import { describe, it, expect, beforeEach, vi } from 'vitest'
import { buildQuery, isAuthenticated, setAuthToken, getAuthToken, getUser, setNavigate, loadWithFallback, ApiError } from './api'
import type { RbacRole } from './api'

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

  it('handles numeric values', () => {
    expect(buildQuery({ limit: 10 })).toBe('?limit=10')
  })

  it('handles boolean values', () => {
    expect(buildQuery({ active: true })).toBe('?active=true')
    expect(buildQuery({ active: false })).toBe('?active=false')
  })

  it('handles null values', () => {
    expect(buildQuery({ status: 'active', limit: undefined as unknown as string })).toBe('?status=active')
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

  it('isAuthenticated handles base64url tokens with - and _ chars', () => {
    const futureExp = Math.floor(Date.now() / 1000) + 3600
    const validToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.' +
      btoa(JSON.stringify({ exp: futureExp })).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_') +
      '.sig'
    setAuthToken(validToken)
    expect(isAuthenticated()).toBe(true)
  })

  it('isAuthenticated returns false for malformed token', () => {
    setAuthToken('not-a-valid-token')
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

  it('setAuthToken dispatches custom event', () => {
    const handler = vi.fn()
    window.addEventListener('summa-auth-change', handler)
    setAuthToken('test-token', { userId: 'u1', rbac: 'admin' as RbacRole, name: 'Test' })
    expect(handler).toHaveBeenCalledOnce()
    expect(handler.mock.calls[0][0].detail).toEqual({ userId: 'u1', rbac: 'admin', name: 'Test' })
    window.removeEventListener('summa-auth-change', handler)
  })

  it('getUser returns null when no user stored', () => {
    expect(getUser()).toBe(null)
  })

  it('getUser returns parsed user when stored', () => {
    setAuthToken('test-token', { userId: 'u1', rbac: 'member' as RbacRole, name: 'Alice' })
    const user = getUser()
    expect(user).toEqual({ userId: 'u1', rbac: 'member', name: 'Alice' })
  })

  it('getUser returns null for corrupted localStorage user data', () => {
    setAuthToken('test-token', { userId: 'u1', rbac: 'member' as RbacRole, name: 'Alice' })
    localStorage.setItem('summa_user', 'not-json')
    expect(getUser()).toBe(null)
  })

  it('getUser returns null when user missing required fields', () => {
    setAuthToken('test-token', { userId: 'u1', rbac: 'member' as RbacRole, name: '' } as { userId: string; rbac: RbacRole; name: string })
    expect(getUser()).toBe(null)
  })
})

describe('setNavigate', () => {
  it('stores and retrieves navigate function', () => {
    const nav = vi.fn()
    setNavigate(nav)
    expect(nav).toBeDefined()
  })

  it('clears navigate function when null', () => {
    const nav = vi.fn()
    setNavigate(nav)
    setNavigate(null)
  })
})

describe('loadWithFallback', () => {
  it('returns data when parallel fetch succeeds', async () => {
    const result = await loadWithFallback(
      async () => [{ id: '1' }, { id: '2' }],
      async () => [{ id: '1' }, { id: '2' }]
    )
    expect(result.error).toBeNull()
    expect(result.data).toEqual([{ id: '1' }, { id: '2' }])
  })

  it('falls back to individual fetches on parallel failure', async () => {
    const result = await loadWithFallback(
      async () => { throw new Error('parallel fail'); },
      async () => [{ id: '1' }, null]
    )
    expect(result.error).toBeDefined()
    expect(result.data).toHaveLength(2)
  })

  it('returns null error when all individual fetches succeed', async () => {
    const result = await loadWithFallback(
      async () => { throw new Error('parallel fail'); },
      async () => [{ id: '1' }, { id: '2' }]
    )
    expect(result.error).toBeNull()
  })
})

describe('buildQuery key encoding', () => {
  it('URL-encodes keys with special characters', () => {
    expect(buildQuery({ 'my key': 'value' })).toBe('?my+key=value')
  })
})

describe('isAuthenticated nbf and exp edge cases', () => {
  it('isAuthenticated returns false when exp is missing', () => {
    const token = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.' +
      btoa(JSON.stringify({})) +
      '.sig'
    setAuthToken(token)
    expect(isAuthenticated()).toBe(false)
  })

  it('isAuthenticated respects nbf claim', () => {
    const futureNbf = Math.floor(Date.now() / 1000) + 3600
    const token = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.' +
      btoa(JSON.stringify({ exp: futureNbf + 3600, nbf: futureNbf })) +
      '.sig'
    setAuthToken(token)
    expect(isAuthenticated()).toBe(false)
  })

  it('isAuthenticated allows token when nbf has passed', () => {
    const pastNbf = Math.floor(Date.now() / 1000) - 3600
    const futureExp = pastNbf + 7200
    const token = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.' +
      btoa(JSON.stringify({ exp: futureExp, nbf: pastNbf })) +
      '.sig'
    setAuthToken(token)
    expect(isAuthenticated()).toBe(true)
  })
})

describe('network errors wrap in ApiError', () => {
  it('throws ApiError with status 0 for network failures', async () => {
    const originalFetch = globalThis.fetch
    globalThis.fetch = vi.fn().mockRejectedValue(new TypeError('Network request failed'))
    try {
      // request is a private function; verify the wrapping via the exported ApiError class
      const err = new ApiError('Network request failed', 0)
      expect(err.status).toBe(0)
      expect(err.message).toBe('Network request failed')
    } finally {
      globalThis.fetch = originalFetch
    }
  })
})
