import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import DataHolds from './DataHolds'
import * as apiModule from '../services/api'

vi.mock('../services/api', () => ({
  api: {
    governance: {
      listHolds: vi.fn(),
      releaseHold: vi.fn(),
    },
  },
}))

describe('DataHolds page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the Data Holds heading', async () => {
    vi.mocked(apiModule.api.governance.listHolds).mockResolvedValue([])
    render(<DataHolds />)
    await waitFor(() => expect(screen.getByText('Data Holds')).toBeInTheDocument())
  })

  it('shows empty state when no holds', async () => {
    vi.mocked(apiModule.api.governance.listHolds).mockResolvedValue([])
    render(<DataHolds />)
    await waitFor(() => {
      const el = screen.getByText(/no active data holds/i)
      expect(el).toBeInTheDocument()
    })
  })

  it('displays holds', async () => {
    vi.mocked(apiModule.api.governance.listHolds).mockResolvedValue([
      { id: 'h1', kind: 'legal', subjectId: 'user-123', reasonMd: 'Pending litigation', createdBy: 'admin-1', createdAt: 1700000000 },
    ])
    render(<DataHolds />)
    await waitFor(() => {
      expect(screen.getByText('1 hold')).toBeInTheDocument()
      expect(screen.getByText('Subject: user-123')).toBeInTheDocument()
    })
  })

  it('shows error state on API failure', async () => {
    vi.mocked(apiModule.api.governance.listHolds).mockRejectedValue(new Error('Network error'))
    render(<DataHolds />)
    await waitFor(() => {
      expect(screen.getByText(/network error/i)).toBeInTheDocument()
    })
  })

  it('releases a hold on click', async () => {
    vi.mocked(apiModule.api.governance.listHolds).mockResolvedValue([
      { id: 'h1', kind: 'legal', subjectId: 'user-123', reasonMd: 'Pending litigation', createdBy: 'admin-1', createdAt: 1700000000 },
    ])
    vi.mocked(apiModule.api.governance.releaseHold).mockResolvedValue({})
    render(<DataHolds />)
    await waitFor(() => expect(screen.getByText('Release')).toBeInTheDocument())
    const releaseBtn = await screen.findByText('Release')
    fireEvent.click(releaseBtn)
    await waitFor(() => expect(apiModule.api.governance.releaseHold).toHaveBeenCalledWith('h1'))
  })
})
