import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import Governance from './Governance'
import * as apiModule from '../services/api'

vi.mock('../services/api', () => ({
  api: {
    governance: {
      policies: vi.fn(),
      quotas: vi.fn(),
      spend: vi.fn(),
    },
  },
}))

describe('Governance page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the Governance heading', async () => {
    vi.mocked(apiModule.api.governance.policies).mockResolvedValue({})
    vi.mocked(apiModule.api.governance.quotas).mockResolvedValue({})
    vi.mocked(apiModule.api.governance.spend).mockResolvedValue(undefined as unknown as import('../services/api').SpendSnapshot)
    render(<Governance />)
    await waitFor(() => expect(screen.getByText('Governance')).toBeInTheDocument())
  })

  it('shows policies section', async () => {
    vi.mocked(apiModule.api.governance.policies).mockResolvedValue({ spawn_quota: 10 })
    vi.mocked(apiModule.api.governance.quotas).mockResolvedValue({})
    vi.mocked(apiModule.api.governance.spend).mockResolvedValue(undefined as unknown as import('../services/api').SpendSnapshot)
    render(<Governance />)
    await waitFor(() => expect(screen.getByText('Policies (1)')).toBeInTheDocument())
  })

  it('shows quotas section', async () => {
    vi.mocked(apiModule.api.governance.policies).mockResolvedValue({})
    vi.mocked(apiModule.api.governance.quotas).mockResolvedValue({ max_concurrent: 5 })
    vi.mocked(apiModule.api.governance.spend).mockResolvedValue(undefined as unknown as import('../services/api').SpendSnapshot)
    render(<Governance />)
    await waitFor(() => expect(screen.getByText('Quotas (1)')).toBeInTheDocument())
  })

  it('shows spend snapshot', async () => {
    vi.mocked(apiModule.api.governance.policies).mockResolvedValue({})
    vi.mocked(apiModule.api.governance.quotas).mockResolvedValue({})
    vi.mocked(apiModule.api.governance.spend).mockResolvedValue({ reserved: 100, settled: 50, ceiling: 1000, utilization: '5%', halted: false })
    render(<Governance />)
    await waitFor(() => expect(screen.getByText('Spend')).toBeInTheDocument())
  })
})
