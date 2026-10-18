import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, act } from '@testing-library/react'
import DNAProposals from './DNAProposals'
import * as apiModule from '../services/api'

vi.mock('../services/api', () => ({
  api: {
    dna: {
      proposals: vi.fn(),
    },
  },
}))

describe('DNAProposals page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the DNA Proposals heading', async () => {
    vi.mocked(apiModule.api.dna.proposals).mockResolvedValue([])
    render(<DNAProposals />)
    await waitFor(() => expect(screen.getByText('DNA Proposals')).toBeInTheDocument())
  })

  it('shows loading state while fetching', async () => {
    vi.mocked(apiModule.api.dna.proposals).mockReturnValue(new Promise(() => {}))
    render(<DNAProposals />)
    await waitFor(() => {
      expect(screen.getByRole('status')).toBeInTheDocument()
    })
  })

  it('shows empty state when no proposals', async () => {
    vi.mocked(apiModule.api.dna.proposals).mockResolvedValue([])
    render(<DNAProposals />)
    await waitFor(() => {
      expect(screen.getByText('No DNA proposals yet.')).toBeInTheDocument()
    })
  })

  it('displays proposals with kind and status', async () => {
    vi.mocked(apiModule.api.dna.proposals).mockResolvedValue([
      { id: 'p1', kind: 'rule', payload: '{}', revision: 1, proposedBy: 'h1', provenance: '', status: 'open' },
    ])
    render(<DNAProposals />)
    await waitFor(() => {
      expect(screen.getByText('rule')).toBeInTheDocument()
      expect(screen.getByText('open')).toBeInTheDocument()
    })
  })

  it('shows error state on API failure', async () => {
    vi.mocked(apiModule.api.dna.proposals).mockRejectedValue(new Error('Network error'))
    render(<DNAProposals />)
    await waitFor(() => {
      expect(screen.getByText('Failed to load: Network error')).toBeInTheDocument()
    })
  })

  it('retry button calls loadProposals on click', async () => {
    vi.mocked(apiModule.api.dna.proposals).mockRejectedValue(new Error('Network error'))
    const { getByRole } = render(<DNAProposals />)
    await waitFor(() => {
      expect(getByRole('button', { name: /retry/i })).toBeInTheDocument()
    })
    vi.mocked(apiModule.api.dna.proposals).mockResolvedValue([])
    await act(async () => { getByRole('button', { name: /retry/i }).click() })
    await waitFor(() => {
      expect(apiModule.api.dna.proposals).toHaveBeenCalledTimes(2)
    })
  })

  it('keeps all status filter buttons visible after selecting one', async () => {
    // Regression: server-side filtering reloaded a status-only subset, zeroing
    // the other counts and making the other filter buttons disappear.
    vi.mocked(apiModule.api.dna.proposals).mockResolvedValue([
      { id: 'p1', kind: 'rule', payload: '{}', revision: 1, proposedBy: 'h1', provenance: '', status: 'open' },
      { id: 'p2', kind: 'card', payload: '{}', revision: 1, proposedBy: 'h1', provenance: '', status: 'published' },
    ])
    render(<DNAProposals />)
    await waitFor(() => expect(screen.getByText('open (1)')).toBeInTheDocument())

    await act(async () => { screen.getByRole('button', { name: /open \(1\)/i }).click() })

    // Both buttons must still be present after filtering.
    expect(screen.getByRole('button', { name: /open \(1\)/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /published \(1\)/i })).toBeInTheDocument()
    // Only the filtered row is rendered.
    expect(screen.getByText('rule')).toBeInTheDocument()
    expect(screen.queryByText('card')).not.toBeInTheDocument()
  })

  it('renders breached proposals with a status badge and filter', async () => {
    vi.mocked(apiModule.api.dna.proposals).mockResolvedValue([
      { id: 'p1', kind: 'rule', payload: '{}', revision: 1, proposedBy: 'h1', provenance: '', status: 'breached' },
    ])
    render(<DNAProposals />)
    await waitFor(() => expect(screen.getByText('breached (1)')).toBeInTheDocument())
    expect(screen.getByLabelText('Status: breached')).toBeInTheDocument()
  })
})
