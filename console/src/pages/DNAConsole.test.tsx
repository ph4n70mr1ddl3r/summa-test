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
})
