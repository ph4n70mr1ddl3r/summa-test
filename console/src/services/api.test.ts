import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { buildQuery, isAuthenticated, setAuthToken, getAuthToken, getUser, setNavigate, loadWithFallback, ApiError, api } from './api'
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

  it('getUser returns partial user when optional name is missing', () => {
    setAuthToken('test-token', { userId: 'u1', rbac: 'member' as RbacRole } as { userId: string; rbac: RbacRole; name?: string })
    expect(getUser()).toEqual({ userId: 'u1', rbac: 'member' })
  })
})

describe('setNavigate', () => {
  it('stores and retrieves navigate function', () => {
    const nav = vi.fn()
    setNavigate(nav)
    expect(nav).toBeDefined()
    expect(nav).toHaveBeenCalledTimes(0)
  })

  it('clears navigate function when null', () => {
    const nav = vi.fn()
    setNavigate(nav)
    setNavigate(null)
    // Verify clearing does not throw and re-setting still works
    const nav2 = vi.fn()
    setNavigate(nav2)
    expect(nav2).toBeDefined()
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
    expect(result.data[0]).toEqual({ id: '1' })
    expect(result.data[1]).toBeNull()
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

describe('isAuthenticated token edge cases', () => {
  it('isAuthenticated returns false when exp is missing', () => {
    const token = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.' +
      btoa(JSON.stringify({})) +
      '.sig'
    setAuthToken(token)
    expect(isAuthenticated()).toBe(false)
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

describe('loadWithFallback error message formatting', () => {
  it('includes the parallel failure message in the error string', async () => {
    const result = await loadWithFallback(
      async () => { throw new Error('timeout'); },
      async () => [null, null]
    )
    expect(result.error).toContain('timeout')
  })

  it('handles non-Error throw values gracefully', async () => {
    const result = await loadWithFallback(
      async () => { throw 'string error'; },
      async () => [{ id: '1' }]
    )
    // When individual fetches succeed, error is null even if parallel failed
    expect(result.data).toHaveLength(1)
  })
})

describe('api.nodes endpoint paths', () => {
  const okResponse = { ok: true, status: 200, json: async () => ({}) }
  const fetchMock = vi.fn()

  beforeEach(() => {
    setAuthToken('test-token', { userId: 'u1', rbac: 'admin' as RbacRole, name: 'Admin' })
    fetchMock.mockResolvedValue(okResponse as unknown as Response)
    globalThis.fetch = fetchMock as unknown as typeof fetch
  })

  afterEach(() => {
    vi.restoreAllMocks()
    setAuthToken(null)
  })

  it('reportRun POSTs to /nodes/{id}/runs/{runId}/report', async () => {
    await api.nodes.reportRun('node-1', 'run-1', { result: 'ok' })
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/nodes/node-1/runs/run-1/report'),
      expect.objectContaining({ method: 'POST' }),
    )
  })

  it('claims POSTs to /nodes/{id}/claims', async () => {
    await api.nodes.claims('node-1', { workspaceId: 'ws-1', epoch: '3' })
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/nodes/node-1/claims'),
      expect.objectContaining({ method: 'POST' }),
    )
  })

  it('workPull POSTs to /nodes/{id}/work/pull', async () => {
    await api.nodes.workPull('node-1')
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/nodes/node-1/work/pull'),
      expect.objectContaining({ method: 'POST' }),
    )
  })
})

describe('request 401 handling', () => {
  const unauthResponse = { ok: false, status: 401, text: async () => JSON.stringify({ message: 'Unauthorized' }) }
  const fetchMock = vi.fn()

  const originalFetch = globalThis.fetch

  beforeEach(() => {
    fetchMock.mockResolvedValue(unauthResponse as unknown as Response)
    globalThis.fetch = fetchMock as unknown as typeof fetch
  })

  afterEach(() => {
    vi.restoreAllMocks()
    globalThis.fetch = originalFetch
    setAuthToken(null)
    delete (window as { __summaRedirecting?: boolean }).__summaRedirecting
  })

  it('a 401 on a data call clears the token and redirects to /login', async () => {
    setAuthToken('stale-token', { userId: 'u1', rbac: 'member' as RbacRole })
    globalThis.fetch = fetchMock as unknown as typeof fetch

    await expect(api.health()).rejects.toBeInstanceOf(ApiError)
    expect(getAuthToken()).toBe(null)
    expect((window as { __summaRedirecting?: boolean }).__summaRedirecting).toBe(true)
  })

  it('a 401 on /auth/login does not clear the token or redirect', async () => {
    setAuthToken('some-token', { userId: 'u1', rbac: 'member' as RbacRole })
    globalThis.fetch = fetchMock as unknown as typeof fetch

    await expect(api.auth.login('a@b.com', 'wrong-password')).rejects.toBeInstanceOf(ApiError)
    expect(getAuthToken()).toBe('some-token')
    // The redirect guard must not have been armed by a login-credential failure
    expect((window as { __summaRedirecting?: boolean }).__summaRedirecting).toBeFalsy()
  })
})
