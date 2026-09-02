import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import DNARules from './DNARules'
import * as apiModule from '../services/api'

vi.mock('../services/api', () => ({
  api: {
    dna: {
      rules: vi.fn(),
    },
  },
}))

describe('DNARules page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the DNA Rules heading', async () => {
    vi.mocked(apiModule.api.dna.rules).mockResolvedValue([])
    render(<DNARules />)
    await waitFor(() => expect(screen.getByText('DNA Rules')).toBeInTheDocument())
  })

  it('shows empty state when no rules', async () => {
    vi.mocked(apiModule.api.dna.rules).mockResolvedValue([])
    render(<DNARules />)
    await waitFor(() => {
      const el = screen.getByText(/no rules configured/i)
      expect(el).toBeInTheDocument()
    })
  })

  it('displays rules with status', async () => {
    vi.mocked(apiModule.api.dna.rules).mockResolvedValue([
      { id: 'r1', domainId: 'd1', statementMd: 'All agents must have owners', effectiveFrom: 1700000000, status: 'active' as const },
    ])
    render(<DNARules />)
    await waitFor(() => {
      expect(screen.getByText('1 rule')).toBeInTheDocument()
    })
  })
})
