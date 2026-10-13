import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, waitFor, act } from '@testing-library/react'
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

  it('shows loading state while fetching', async () => {
    vi.mocked(apiModule.api.dna.goals).mockReturnValue(new Promise(() => {}))
    const { container } = render(<DNAGoals />)
    await waitFor(() => {
      expect(container.textContent).toContain('Loading...')
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

  it('shows error state on API failure', async () => {
    vi.mocked(apiModule.api.dna.goals).mockRejectedValue(new Error('Network error'))
    const { container } = render(<DNAGoals />)
    await waitFor(() => {
      expect(container.textContent).toContain('Failed to load')
    })
  })

  it('retry button calls loadGoals on click', async () => {
    vi.mocked(apiModule.api.dna.goals).mockRejectedValue(new Error('Network error'))
    const { getByRole, container } = render(<DNAGoals />)
    await waitFor(() => {
      expect(container.textContent).toContain('Failed to load')
    })
    const retryBtn = getByRole('button', { name: /retry/i })
    vi.mocked(apiModule.api.dna.goals).mockResolvedValue([])
    await act(async () => { retryBtn.click() })
    await waitFor(() => {
      expect(apiModule.api.dna.goals).toHaveBeenCalledTimes(2)
    })
  })
})
