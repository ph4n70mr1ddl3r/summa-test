import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, act } from '@testing-library/react'
import Groups from './Groups'
import * as apiModule from '../services/api'

vi.mock('../services/api', () => ({
  api: {
    groups: {
      list: vi.fn(),
      archive: vi.fn(),
    },
  },
}))

describe('Groups page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.removeItem('summa_user')
  })

  it('renders the Groups heading', async () => {
    vi.mocked(apiModule.api.groups.list).mockResolvedValue([])
    render(<Groups />)
    await waitFor(() => expect(screen.getByText('Groups')).toBeInTheDocument())
  })

  it('shows empty state when no groups', async () => {
    vi.mocked(apiModule.api.groups.list).mockResolvedValue([])
    render(<Groups />)
    await waitFor(() => {
      const el = screen.getByText(/no groups configured/i)
      expect(el).toBeInTheDocument()
    })
  })

  it('displays groups with status and archive button', async () => {
    // ORG-040: a group's archival is an admin act — the control is offered to an admin.
    localStorage.setItem('summa_user', JSON.stringify({ userId: 'admin-1', rbac: 'admin' }))
    vi.mocked(apiModule.api.groups.list).mockResolvedValue([
      { id: 'g1', name: 'Engineering', leaderMemberId: 'h1', status: 'active', createdAt: 0 },
    ])
    render(<Groups />)
    await waitFor(() => {
      expect(screen.getByText('Engineering')).toBeInTheDocument()
      expect(screen.getByText('active')).toBeInTheDocument()
      expect(screen.getByText('Archive')).toBeInTheDocument()
    })
  })

  it('shows loading state', () => {
    vi.mocked(apiModule.api.groups.list).mockReturnValue(new Promise(() => {}))
    const { container } = render(<Groups />)
    expect(container.textContent).toContain('Loading...')
  })

  it('calls archive on archive button click', async () => {
    localStorage.setItem('summa_user', JSON.stringify({ userId: 'admin-1', rbac: 'admin' }))
    vi.mocked(apiModule.api.groups.list).mockResolvedValue([
      { id: 'g1', name: 'Engineering', status: 'active', createdAt: 0 },
    ])
    vi.mocked(apiModule.api.groups.archive).mockResolvedValue({ id: 'g1', name: 'Engineering', status: 'archived' as const } as apiModule.Group)
    render(<Groups />)
    await waitFor(() => {
      expect(screen.getByText('Engineering')).toBeInTheDocument()
    })
    const archiveBtn = screen.getByText('Archive')
    await act(async () => {
      archiveBtn.click()
    })
    await waitFor(() => {
      expect(screen.getByText('Confirm?')).toBeInTheDocument()
    })
    const yesBtn = screen.getByRole('button', { name: /confirm archive engineering/i })
    await act(async () => {
      yesBtn.click()
    })
    await waitFor(() => {
      expect(apiModule.api.groups.archive).toHaveBeenCalledWith('g1')
    })
  })

  it('shows error when archive fails', async () => {
    localStorage.setItem('summa_user', JSON.stringify({ userId: 'admin-1', rbac: 'admin' }))
    vi.mocked(apiModule.api.groups.list).mockResolvedValue([
      { id: 'g1', name: 'Engineering', status: 'active', createdAt: 0 },
    ])
    vi.mocked(apiModule.api.groups.archive).mockRejectedValue(new Error('Permission denied'))
    render(<Groups />)
    await waitFor(() => {
      expect(screen.getByText('Engineering')).toBeInTheDocument()
    })
    const archiveBtn = screen.getByText('Archive')
    await act(async () => {
      archiveBtn.click()
    })
    await waitFor(() => {
      expect(screen.getByText('Confirm?')).toBeInTheDocument()
    })
    const yesBtn = screen.getByRole('button', { name: /confirm archive engineering/i })
    await act(async () => {
      yesBtn.click()
    })
    await waitFor(() => {
      expect(screen.getByText(/permission denied/i)).toBeInTheDocument()
    })
  })

  it('reloads groups after successful archive', async () => {
    localStorage.setItem('summa_user', JSON.stringify({ userId: 'admin-1', rbac: 'admin' }))
    const group = { id: 'g1', name: 'Engineering', status: 'active' as const, createdAt: 0 }
    vi.mocked(apiModule.api.groups.list).mockResolvedValue([group])
    vi.mocked(apiModule.api.groups.archive).mockResolvedValue({ ...group, status: 'archived' as const } as apiModule.Group)
    render(<Groups />)
    await waitFor(() => {
      expect(screen.getByText('Engineering')).toBeInTheDocument()
    })
    const archiveBtn = screen.getByText('Archive')
    await act(async () => {
      archiveBtn.click()
    })
    await waitFor(() => {
      expect(screen.getByText('Confirm?')).toBeInTheDocument()
    })
    const yesBtn = screen.getByRole('button', { name: /confirm archive engineering/i })
    await act(async () => {
      yesBtn.click()
    })
    await waitFor(() => {
      expect(apiModule.api.groups.archive).toHaveBeenCalledWith('g1')
      expect(apiModule.api.groups.list).toHaveBeenCalledTimes(2)
    })
  })

  it('hides the archive control from non-admins', async () => {
    localStorage.setItem('summa_user', JSON.stringify({ userId: 'member-1', rbac: 'member' }))
    vi.mocked(apiModule.api.groups.list).mockResolvedValue([
      { id: 'g1', name: 'Engineering', status: 'active', createdAt: 0 },
    ])
    render(<Groups />)
    await waitFor(() => {
      expect(screen.getByText('Engineering')).toBeInTheDocument()
    })
    expect(screen.queryByText('Archive')).not.toBeInTheDocument()
  })
})
