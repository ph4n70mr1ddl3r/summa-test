import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, waitFor } from '@testing-library/react'
import DNAGoals from './DNAGoals'
import * as apiModule from '../services/api'

vi.mock('../services/api', () => ({
  api: {
    dna: {
      goals: vi.fn(),
    },
  },
}))

describe('DNAGoals page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the DNAGoals heading', async () => {
    vi.mocked(apiModule.api.dna.goals).mockResolvedValue([])
    const { container } = render(<DNAGoals />)
    await waitFor(() => {
      expect(container.textContent).toContain('DNA Goals')
    })
  })

  it('shows no-goals message when empty', async () => {
    vi.mocked(apiModule.api.dna.goals).mockResolvedValue([])
    const { container } = render(<DNAGoals />)
    await waitFor(() => {
      expect(container.textContent).toContain('No DNA goals.')
    })
  })

  it('displays goal items', async () => {
    const goals: apiModule.DnaGoal[] = [
      {
        id: 'g1',
        statementMd: 'Increase revenue',
        owner: 'h:abc',
        inject: 'linked',
        status: 'active' as apiModule.DnaGoalStatus,
        effectiveFrom: 1700000000,
      },
    ]
    vi.mocked(apiModule.api.dna.goals).mockResolvedValue(goals)
    const { container } = render(<DNAGoals />)
    await waitFor(() => {
      expect(container.textContent).toContain('Increase revenue')
      expect(container.textContent).toContain('active')
    })
  })

  it('shows API reference', async () => {
    vi.mocked(apiModule.api.dna.goals).mockResolvedValue([])
    const { container } = render(<DNAGoals />)
    await waitFor(() => {
      expect(container.textContent).toContain('PATCH /api/dna/goals/:id/status')
    })
  })
})
