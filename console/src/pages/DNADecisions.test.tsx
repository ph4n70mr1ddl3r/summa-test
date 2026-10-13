import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, act } from '@testing-library/react'
import DNADecisions from './DNADecisions'
import * as apiModule from '../services/api'

vi.mock('../services/api', () => ({
  api: {
    dna: {
      decisions: vi.fn(),
    },
  },
}))

describe('DNADecisions page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the DNA Decisions heading', async () => {
    vi.mocked(apiModule.api.dna.decisions).mockResolvedValue([])
    render(<DNADecisions />)
    await waitFor(() => expect(screen.getByText('DNA Decisions')).toBeInTheDocument())
  })

  it('shows loading state while fetching', async () => {
    vi.mocked(apiModule.api.dna.decisions).mockReturnValue(new Promise(() => {}))
    render(<DNADecisions />)
    await waitFor(() => {
      expect(screen.getByRole('status')).toBeInTheDocument()
    })
  })

  it('shows empty state when no decisions', async () => {
    vi.mocked(apiModule.api.dna.decisions).mockResolvedValue([])
    render(<DNADecisions />)
    await waitFor(() => {
      const el = screen.getByText(/no decisions recorded/i)
      expect(el).toBeInTheDocument()
    })
  })

  it('displays decisions', async () => {
    vi.mocked(apiModule.api.dna.decisions).mockResolvedValue([
      { id: 'd1', domainId: 'd1', contextMd: 'Context', outcomeMd: 'Outcome', decidedBy: 'h1', decidedAt: 1700000000 },
    ])
    render(<DNADecisions />)
    await waitFor(() => {
      expect(screen.getByText('1 decisions')).toBeInTheDocument()
    })
  })

  it('shows error state on API failure', async () => {
    vi.mocked(apiModule.api.dna.decisions).mockRejectedValue(new Error('Network error'))
    render(<DNADecisions />)
    await waitFor(() => {
      expect(screen.getByText(/error/i)).toBeInTheDocument()
    })
  })

  it('retry button calls loadDecisions on click', async () => {
    vi.mocked(apiModule.api.dna.decisions).mockRejectedValue(new Error('Network error'))
    const { getByRole } = render(<DNADecisions />)
    await waitFor(() => {
      expect(getByRole('button', { name: /retry/i })).toBeInTheDocument()
    })
    vi.mocked(apiModule.api.dna.decisions).mockResolvedValue([])
    await act(async () => { getByRole('button', { name: /retry/i }).click() })
    await waitFor(() => {
      expect(apiModule.api.dna.decisions).toHaveBeenCalledTimes(2)
    })
  })
})
