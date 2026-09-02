import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
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
      expect(screen.getByText('1 decision')).toBeInTheDocument()
    })
  })
})
