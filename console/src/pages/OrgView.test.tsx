import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, waitFor } from '@testing-library/react'
import OrgView from './OrgView'
import * as apiModule from '../services/api'

vi.mock('../services/api', () => ({
  api: {
    org: {
      members: vi.fn(),
    },
    groups: {
      list: vi.fn(),
    },
  },
}))

describe('OrgView page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the Organization heading', async () => {
    vi.mocked(apiModule.api.org.members).mockResolvedValue({ members: [], total: 0 })
    vi.mocked(apiModule.api.groups.list).mockResolvedValue([])
    const { container } = render(<OrgView />)
    await waitFor(() => {
      expect(container.textContent).toContain('Organization')
    })
  })

  it('shows members and groups sections', async () => {
    vi.mocked(apiModule.api.org.members).mockResolvedValue({ members: [], total: 0 })
    vi.mocked(apiModule.api.groups.list).mockResolvedValue([])
    const { container } = render(<OrgView />)
    await waitFor(() => {
      expect(container.textContent).toContain('Humans')
      expect(container.textContent).toContain('Agents')
      expect(container.textContent).toContain('Groups')
    })
  })

  it('shows RBAC roles for human members', async () => {
    vi.mocked(apiModule.api.org.members).mockResolvedValue({
      members: [
        { id: 'h1', kind: 'human', name: 'Alice', email: 'alice@example.com', rbac: 'admin', active: true },
        { id: 'h2', kind: 'human', name: 'Bob', email: 'bob@example.com', rbac: 'viewer', active: true },
      ],
      total: 2,
    } as unknown as { members: (import('../services/api').Human | import('../services/api').Agent)[]; total: number })
    vi.mocked(apiModule.api.groups.list).mockResolvedValue([])
    const { container } = render(<OrgView />)
    await waitFor(() => {
      expect(container.textContent).toContain('admin')
      expect(container.textContent).toContain('viewer')
    })
  })

  it('shows role templates section', async () => {
    vi.mocked(apiModule.api.org.members).mockResolvedValue({ members: [], total: 0 })
    vi.mocked(apiModule.api.groups.list).mockResolvedValue([])
    const { container } = render(<OrgView />)
    await waitFor(() => {
      expect(container.textContent).toContain('Role Templates')
    })
  })

  it('shows agents with class and status', async () => {
    vi.mocked(apiModule.api.org.members).mockResolvedValue({
      members: [
        { id: 'a1', kind: 'agent', name: 'Agent-One', ownerHumanId: 'h1', class: 'persistent', status: 'active' },
      ],
      total: 1,
    } as unknown as { members: (import('../services/api').Human | import('../services/api').Agent)[]; total: number })
    vi.mocked(apiModule.api.groups.list).mockResolvedValue([])
    const { container } = render(<OrgView />)
    await waitFor(() => {
      expect(container.textContent).toContain('Agent-One')
      expect(container.textContent).toContain('persistent')
      expect(container.textContent).toContain('active')
    })
  })

  it('shows empty state when both APIs return empty', async () => {
    vi.mocked(apiModule.api.org.members).mockResolvedValue({ members: [], total: 0 })
    vi.mocked(apiModule.api.groups.list).mockResolvedValue([])
    const { container } = render(<OrgView />)
    await waitFor(() => {
      expect(container.textContent).toContain('No humans yet')
      expect(container.textContent).toContain('No agents yet')
      expect(container.textContent).toContain('No groups configured')
    })
  })
})
