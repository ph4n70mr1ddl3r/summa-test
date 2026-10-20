import type { RbacRole } from '../types'

export interface SessionUser {
  userId: string
  rbac: RbacRole
  name?: string
}

const USER_KEY = 'summa_user'

const VALID_ROLES: RbacRole[] = ['admin', 'owner', 'member', 'viewer']

/**
 * Read the cached session identity.
 *
 * Lives outside `services/api` on purpose: pages that only need the current
 * identity (to decide which controls to offer) should not have to pull in the
 * whole API client — and a test that mocks the API client does not have to
 * stub the session to render a page.
 */
export function readSessionUser(): SessionUser | null {
  try {
    const raw = localStorage.getItem(USER_KEY)
    if (!raw) return null
    const user = JSON.parse(raw) as Partial<SessionUser> | null
    if (!user?.userId || !user?.rbac) return null
    if (!VALID_ROLES.includes(user.rbac)) return null
    return { userId: user.userId, rbac: user.rbac, name: user.name }
  } catch {
    return null
  }
}
