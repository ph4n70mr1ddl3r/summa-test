import { describe, it, expect } from 'vitest'
import { canWrite, readSessionUser } from './session'

/**
 * ORG-020: a viewer holds no write surface — the backend's write gate refuses
 * every viewer write, so a write control offered to one can only ever answer
 * 403. The console asks the same question before it offers the control.
 */
describe('session write surface', () => {
  it('refuses a viewer', () => {
    expect(canWrite({ userId: 'viewer-1', rbac: 'viewer' })).toBe(false)
  })

  it('admits every other role', () => {
    expect(canWrite({ userId: 'member-1', rbac: 'member' })).toBe(true)
    expect(canWrite({ userId: 'owner-1', rbac: 'owner' })).toBe(true)
    expect(canWrite({ userId: 'admin-1', rbac: 'admin' })).toBe(true)
  })

  it('refuses an unknown identity', () => {
    expect(canWrite(null)).toBe(false)
  })

  it('reads a cached session identity', () => {
    localStorage.setItem('summa_user', JSON.stringify({ userId: 'admin-1', rbac: 'admin' }))
    expect(readSessionUser()).toEqual({ userId: 'admin-1', rbac: 'admin', name: undefined })
    localStorage.removeItem('summa_user')
    expect(readSessionUser()).toBeNull()
  })

  it('refuses a cached identity with an unknown role', () => {
    localStorage.setItem('summa_user', JSON.stringify({ userId: 'x', rbac: 'wizard' }))
    expect(readSessionUser()).toBeNull()
    localStorage.removeItem('summa_user')
  })
})
