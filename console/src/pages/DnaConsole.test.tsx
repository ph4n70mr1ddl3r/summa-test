import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import DnaConsole from './DnaConsole'
import * as apiModule from '../services/api'

vi.mock('../services/api', () => ({
  api: {
    dna: {
      domains: vi.fn(),
      cards: vi.fn(),
      goals: vi.fn(),
      proposals: vi.fn(),
    },
  },
}))

describe('DnaConsole page', () => {
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
    vi.mocked(apiModule.api.dna.proposals).mockResolvedValue([])

    render(wrapper(<DnaConsole />))
    await waitFor(() => expect(screen.getByText('DNA Console')).toBeInTheDocument())
  })

  it('shows domain count', async () => {
    vi.mocked(apiModule.api.dna.domains).mockResolvedValue([
      { id: 'd1', name: 'Engineering', ownerHumanId: 'h1', access: 'public', status: 'active' },
    ])
    vi.mocked(apiModule.api.dna.cards).mockResolvedValue([])
    vi.mocked(apiModule.api.dna.goals).mockResolvedValue([])
    vi.mocked(apiModule.api.dna.proposals).mockResolvedValue([])

    render(wrapper(<DnaConsole />))
    await waitFor(() => expect(screen.getByText('Domains (1)')).toBeInTheDocument())
  })

  it('shows review queue count', async () => {
    vi.mocked(apiModule.api.dna.domains).mockResolvedValue([])
    vi.mocked(apiModule.api.dna.cards).mockResolvedValue([])
    vi.mocked(apiModule.api.dna.goals).mockResolvedValue([])
    vi.mocked(apiModule.api.dna.proposals).mockResolvedValue([
      { id: 'p1', kind: 'rule', payload: '{}', revision: 1, proposedBy: 'h1', provenance: '', status: 'open' },
    ])

    render(wrapper(<DnaConsole />))
    await waitFor(() => expect(screen.getByText('Review Queue (1 open)')).toBeInTheDocument())
  })

  it('shows summary cards', async () => {
    vi.mocked(apiModule.api.dna.domains).mockResolvedValue([])
    vi.mocked(apiModule.api.dna.cards).mockResolvedValue([])
    vi.mocked(apiModule.api.dna.goals).mockResolvedValue([])
    vi.mocked(apiModule.api.dna.proposals).mockResolvedValue([])

    render(wrapper(<DnaConsole />))
    await waitFor(() => {
      // Should show three "0" counts for cards, goals, domains
      const allZero = screen.queryAllByText('0')
      expect(allZero.length).toBeGreaterThanOrEqual(3)
    })
  })
})
