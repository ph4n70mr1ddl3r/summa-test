import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, fireEvent, act } from '@testing-library/react'
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
    localStorage.removeItem('summa_user')
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
      { id: 'h1', kind: 'member', subjectId: 'user-123', reasonMd: 'Pending litigation', createdBy: 'admin-1', createdAt: 1700000000 },
    ])
    render(<DataHolds />)
    await waitFor(() => {
      expect(screen.getByText('1 holds')).toBeInTheDocument()
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

  it('retry button calls loadHolds on click', async () => {
    vi.mocked(apiModule.api.governance.listHolds).mockRejectedValue(new Error('Network error'))
    const { getByRole } = render(<DataHolds />)
    await waitFor(() => {
      expect(getByRole('button', { name: /retry/i })).toBeInTheDocument()
    })
    vi.mocked(apiModule.api.governance.listHolds).mockResolvedValue([])
    await act(async () => { getByRole('button', { name: /retry/i }).click() })
    await waitFor(() => {
      expect(apiModule.api.governance.listHolds).toHaveBeenCalledTimes(2)
    })
  })
  it('releases a hold on click', async () => {
    // Releasing a hold is an admin act — the control is only offered to an admin.
    localStorage.setItem('summa_user', JSON.stringify({ userId: 'admin-1', rbac: 'admin' }))
    vi.mocked(apiModule.api.governance.listHolds).mockResolvedValue([
      { id: 'h1', kind: 'member', subjectId: 'user-123', reasonMd: 'Pending litigation', createdBy: 'admin-1', createdAt: 1700000000 },
    ])
    vi.mocked(apiModule.api.governance.releaseHold).mockResolvedValue({ id: 'h1', kind: 'member', subjectId: 'user-123', reasonMd: '', createdBy: 'admin-1', createdAt: 1700000000 } as apiModule.DataHold)
    render(<DataHolds />)
    await waitFor(() => expect(screen.getByText('Release')).toBeInTheDocument())
    const releaseBtn = await screen.findByText('Release')
    fireEvent.click(releaseBtn)
    await waitFor(() => expect(screen.getByText('Confirm?')).toBeInTheDocument())
    const yesBtn = screen.getByText('Yes')
    fireEvent.click(yesBtn)
    await waitFor(() => expect(apiModule.api.governance.releaseHold).toHaveBeenCalledWith('h1'))
  })

  it('hides the release control from non-admins', async () => {
    localStorage.setItem('summa_user', JSON.stringify({ userId: 'member-1', rbac: 'member' }))
    vi.mocked(apiModule.api.governance.listHolds).mockResolvedValue([
      { id: 'h1', kind: 'member', subjectId: 'user-123', reasonMd: 'Pending litigation', createdBy: 'admin-1', createdAt: 1700000000 },
    ])
    render(<DataHolds />)
    await waitFor(() => expect(screen.getByText('Subject: user-123')).toBeInTheDocument())
    expect(screen.queryByText('Release')).not.toBeInTheDocument()
  })
})
