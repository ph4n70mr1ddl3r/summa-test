import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, waitFor } from '@testing-library/react'
import Triggers from './Triggers'
import * as apiModule from '../services/api'

vi.mock('../services/api', () => ({
  api: {
    triggers: {
      list: vi.fn(),
    },
  },
}))

describe('Triggers page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the Triggers heading', async () => {
    vi.mocked(apiModule.api.triggers.list).mockResolvedValue([])
    const { container } = render(<Triggers />)
    await waitFor(() => {
      expect(container.textContent).toContain('Triggers')
    })
  })

  it('shows empty state when no triggers', async () => {
    vi.mocked(apiModule.api.triggers.list).mockResolvedValue([])
    const { container } = render(<Triggers />)
    await waitFor(() => {
      expect(container.textContent).toContain('No triggers configured')
    })
  })

  it('renders trigger cards with data', async () => {
    vi.mocked(apiModule.api.triggers.list).mockResolvedValue([
      { id: 'tr-1', name: 'Minute ticker', kind: 'schedule', expression: '* * * * *', agentId: 'a-1', status: 'active', criticality: 'standard' },
    ])
    const { container } = render(<Triggers />)
    await waitFor(() => {
      expect(container.textContent).toContain('Minute ticker')
      expect(container.textContent).toContain('schedule')
      expect(container.textContent).toContain('active')
    })
  })

  it('shows error state on API failure', async () => {
    vi.mocked(apiModule.api.triggers.list).mockRejectedValue(new Error('failed'))
    const { container } = render(<Triggers />)
    await waitFor(() => {
      expect(container.textContent).toContain('Error')
    })
  })
})
