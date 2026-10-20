import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import DNAConsole from './DNAConsole'
import * as apiModule from '../services/api'

vi.mock('../services/api', () => ({
  api: {
    dna: {
      domains: vi.fn(),
      cards: vi.fn(),
      goals: vi.fn(),
      reviewQueue: vi.fn(),
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

describe('DNAConsole page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.removeItem('summa_user')
  })

  const wrapper = (children: React.ReactNode) => (
    <MemoryRouter>{children}</MemoryRouter>
  )

  it('renders the DNA Console heading', async () => {
    vi.mocked(apiModule.api.dna.domains).mockResolvedValue([])
    vi.mocked(apiModule.api.dna.cards).mockResolvedValue([])
    vi.mocked(apiModule.api.dna.goals).mockResolvedValue([])
    vi.mocked(apiModule.api.dna.reviewQueue).mockResolvedValue([])

    render(wrapper(<DNAConsole />))
    await waitFor(() => expect(screen.getByText('DNA Console')).toBeInTheDocument())
  })

  it('shows domain count', async () => {
    vi.mocked(apiModule.api.dna.domains).mockResolvedValue([
      { id: 'd1', name: 'Engineering', ownerHumanId: 'h1', access: 'public', status: 'active' },
    ])
    vi.mocked(apiModule.api.dna.cards).mockResolvedValue([])
    vi.mocked(apiModule.api.dna.goals).mockResolvedValue([])
    vi.mocked(apiModule.api.dna.reviewQueue).mockResolvedValue([])

    render(wrapper(<DNAConsole />))
    await waitFor(() => expect(screen.getByText('Domains (1)')).toBeInTheDocument())
  })

  it('shows review queue count', async () => {
    // DWP-022: the review queue is an admin surface — the console asks for it
    // only as an admin.
    localStorage.setItem('summa_user', JSON.stringify({ userId: 'admin-1', rbac: 'admin' }))
    vi.mocked(apiModule.api.dna.domains).mockResolvedValue([])
    vi.mocked(apiModule.api.dna.cards).mockResolvedValue([])
    vi.mocked(apiModule.api.dna.goals).mockResolvedValue([])
    vi.mocked(apiModule.api.dna.reviewQueue).mockResolvedValue([
      { id: 'p1', kind: 'rule', payload: '{}', revision: 1, proposedBy: 'h1', provenance: '', status: 'open' },
    ])

    render(wrapper(<DNAConsole />))
    await waitFor(() => expect(screen.getByText('Review Queue (1 open)')).toBeInTheDocument())
  })

  it('shows summary cards', async () => {
    vi.mocked(apiModule.api.dna.domains).mockResolvedValue([])
    vi.mocked(apiModule.api.dna.cards).mockResolvedValue([])
    vi.mocked(apiModule.api.dna.goals).mockResolvedValue([])
    vi.mocked(apiModule.api.dna.reviewQueue).mockResolvedValue([])

    render(wrapper(<DNAConsole />))
    await waitFor(() => {
      // Should show three "0" counts for cards, goals, domains
      const allZero = screen.queryAllByText('0')
      expect(allZero.length).toBeGreaterThanOrEqual(3)
    })
  })

  it('shows loading state while fetching', async () => {
    vi.mocked(apiModule.api.dna.domains).mockReturnValue(new Promise(() => {}))
    vi.mocked(apiModule.api.dna.cards).mockReturnValue(new Promise(() => {}))
    vi.mocked(apiModule.api.dna.goals).mockReturnValue(new Promise(() => {}))
    vi.mocked(apiModule.api.dna.reviewQueue).mockReturnValue(new Promise(() => {}))

    render(wrapper(<DNAConsole />))
    await waitFor(() => {
      expect(screen.getByRole('status')).toBeInTheDocument()
    })
  })

  it('keeps the loaded panels when one sub-request fails', async () => {
    // The review queue is admin-gated — an admin's console must still show the
    // domains and cards that did load, with the failure reported alongside.
    localStorage.setItem('summa_user', JSON.stringify({ userId: 'admin-1', rbac: 'admin' }))
    vi.mocked(apiModule.api.dna.domains).mockResolvedValue([
      { id: 'd1', name: 'Engineering', ownerHumanId: 'h1', access: 'public', status: 'active' },
    ])
    vi.mocked(apiModule.api.dna.cards).mockResolvedValue([])
    vi.mocked(apiModule.api.dna.goals).mockResolvedValue([])
    vi.mocked(apiModule.api.dna.reviewQueue).mockRejectedValue(new Error('Admin access required'))

    render(wrapper(<DNAConsole />))
    await waitFor(() => expect(screen.getByText('Domains (1)')).toBeInTheDocument())
    expect(screen.getByText(/admin access required/i)).toBeInTheDocument()
  })

  it('shows only the error banner when nothing could be loaded', async () => {
    localStorage.setItem('summa_user', JSON.stringify({ userId: 'admin-1', rbac: 'admin' }))
    vi.mocked(apiModule.api.dna.domains).mockRejectedValue(new Error('Network error'))
    vi.mocked(apiModule.api.dna.cards).mockRejectedValue(new Error('Network error'))
    vi.mocked(apiModule.api.dna.goals).mockRejectedValue(new Error('Network error'))
    vi.mocked(apiModule.api.dna.reviewQueue).mockRejectedValue(new Error('Network error'))

    render(wrapper(<DNAConsole />))
    await waitFor(() => expect(screen.getByText(/network error/i)).toBeInTheDocument())
    expect(screen.queryByText('DNA Console')).not.toBeInTheDocument()
  })

  it('does not ask for the admin review queue as a non-admin', async () => {
    // The queue is an admin surface — a member's console must not ask for it,
    // so the console shows no error banner at all.
    localStorage.setItem('summa_user', JSON.stringify({ userId: 'member-1', rbac: 'member' }))
    vi.mocked(apiModule.api.dna.domains).mockResolvedValue([
      { id: 'd1', name: 'Engineering', ownerHumanId: 'h1', access: 'public', status: 'active' },
    ])
    vi.mocked(apiModule.api.dna.cards).mockResolvedValue([])
    vi.mocked(apiModule.api.dna.goals).mockResolvedValue([])

    render(wrapper(<DNAConsole />))
    await waitFor(() => expect(screen.getByText('Domains (1)')).toBeInTheDocument())
    expect(apiModule.api.dna.reviewQueue).not.toHaveBeenCalled()
    expect(screen.queryByText(/failed to load/i)).not.toBeInTheDocument()
  })
})
