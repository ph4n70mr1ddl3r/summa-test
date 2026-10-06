import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, waitFor, screen } from '@testing-library/react'
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
  loadWithFallback: async (fetchAll: () => Promise<unknown[]>, fetchIndividual: () => Promise<unknown[]>) => {
    try {
      const data = await fetchAll()
      return { data, error: null }
    } catch (e) {
      const results = await fetchIndividual()
      const hasError = results.some((r: unknown) => r === null)
      const error = hasError
        ? 'Some data could not be loaded: ' + (e instanceof Error ? e.message : String(e))
        : null
      return { data: results, error }
    }
  },
}))

describe('OrgView page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the Organization heading', async () => {
    vi.mocked(apiModule.api.org.members).mockResolvedValue({ members: [], total: 0 })
    vi.mocked(apiModule.api.groups.list).mockResolvedValue([])
    render(<OrgView />)
    await waitFor(() => {
      expect(screen.getByText('Organization')).toBeInTheDocument()
    })
  })

  it('shows members and groups sections', async () => {
    vi.mocked(apiModule.api.org.members).mockResolvedValue({ members: [], total: 0 })
    vi.mocked(apiModule.api.groups.list).mockResolvedValue([])
    render(<OrgView />)
    await waitFor(() => {
      expect(screen.getByText(/Humans/)).toBeInTheDocument()
      expect(screen.getByText(/Agents/)).toBeInTheDocument()
      expect(screen.getByText(/Groups/)).toBeInTheDocument()
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
    render(<OrgView />)
    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument()
      expect(screen.getByText('viewer')).toBeInTheDocument()
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
    render(<OrgView />)
    await waitFor(() => {
      expect(screen.getByText('Agent-One')).toBeInTheDocument()
      expect(screen.getByText('persistent')).toBeInTheDocument()
      expect(screen.getByText('active')).toBeInTheDocument()
    })
  })

  it('shows empty state when both APIs return empty', async () => {
    vi.mocked(apiModule.api.org.members).mockResolvedValue({ members: [], total: 0 })
    vi.mocked(apiModule.api.groups.list).mockResolvedValue([])
    render(<OrgView />)
    await waitFor(() => {
      expect(screen.getByText(/No humans yet/)).toBeInTheDocument()
      expect(screen.getByText(/No agents yet/)).toBeInTheDocument()
      expect(screen.getByText(/No groups configured/)).toBeInTheDocument()
    })
  })

  it('shows error state on API failure', async () => {
    vi.mocked(apiModule.api.org.members).mockRejectedValue(new Error('Network error'))
    vi.mocked(apiModule.api.groups.list).mockRejectedValue(new Error('Network error'))
    render(<OrgView />)
    await waitFor(() => {
      expect(screen.getByText(/Failed to load/)).toBeInTheDocument()
    })
  })
})
