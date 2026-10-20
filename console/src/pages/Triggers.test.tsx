import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, act } from '@testing-library/react'
import Triggers from './Triggers'
import * as apiModule from '../services/api'

vi.mock('../services/api', () => ({
  api: {
    triggers: {
      list: vi.fn(),
      pause: vi.fn(),
      resume: vi.fn(),
      archive: vi.fn(),
    },
    agents: {
      list: vi.fn(),
    },
  },
}))

vi.mock('../hooks/useEscapeKey', () => ({
  useEscapeKey: vi.fn(),
}))

describe('Triggers page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.removeItem('summa_user')
    vi.mocked(apiModule.api.agents.list).mockResolvedValue([])
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
      expect(container.textContent).toContain('Failed to load')
    })
  })

  it('retry button calls loadTriggers on click', async () => {
    vi.mocked(apiModule.api.triggers.list).mockRejectedValue(new Error('failed'))
    const { getByRole, container } = render(<Triggers />)
    await waitFor(() => {
      expect(container.textContent).toContain('Failed to load')
    })
    const retryBtn = getByRole('button', { name: /retry/i })
    vi.mocked(apiModule.api.triggers.list).mockResolvedValue([])
    await act(async () => { retryBtn.click() })
    await waitFor(() => {
      expect(apiModule.api.triggers.list).toHaveBeenCalledTimes(2)
    })
  })

  it('pauses an active trigger', async () => {
    // TRG-002: the lifecycle controls belong to the trigger's agent owner or an
    // admin — an admin session sees them on every trigger.
    localStorage.setItem('summa_user', JSON.stringify({ userId: 'admin-1', rbac: 'admin' }))
    vi.mocked(apiModule.api.triggers.list).mockResolvedValue([
      { id: 'tr-1', name: 'Minute ticker', kind: 'schedule', expression: '* * * * *', agentId: 'a-1', status: 'active', criticality: 'standard' },
    ])
    vi.mocked(apiModule.api.triggers.pause).mockResolvedValue(undefined)
    render(<Triggers />)
    await waitFor(() => expect(screen.getByText('Minute ticker')).toBeInTheDocument())
    await act(async () => {
      screen.getByRole('button', { name: /pause trigger minute ticker/i }).click()
    })
    await waitFor(() => {
      expect(apiModule.api.triggers.pause).toHaveBeenCalledWith('tr-1')
    })
  })

  it('resumes a paused trigger', async () => {
    localStorage.setItem('summa_user', JSON.stringify({ userId: 'admin-1', rbac: 'admin' }))
    vi.mocked(apiModule.api.triggers.list).mockResolvedValue([
      { id: 'tr-2', name: 'Hourly tick', kind: 'schedule', expression: '0 * * * *', agentId: 'a-1', status: 'paused', criticality: 'standard' },
    ])
    vi.mocked(apiModule.api.triggers.resume).mockResolvedValue(undefined)
    render(<Triggers />)
    await waitFor(() => expect(screen.getByText('Hourly tick')).toBeInTheDocument())
    await act(async () => {
      screen.getByRole('button', { name: /resume trigger hourly tick/i }).click()
    })
    await waitFor(() => {
      expect(apiModule.api.triggers.resume).toHaveBeenCalledWith('tr-2')
    })
  })

  it('archives a non-archived trigger', async () => {
    localStorage.setItem('summa_user', JSON.stringify({ userId: 'admin-1', rbac: 'admin' }))
    vi.mocked(apiModule.api.triggers.list).mockResolvedValue([
      { id: 'tr-3', name: 'Daily job', kind: 'schedule', expression: '0 0 * * *', agentId: 'a-1', status: 'active', criticality: 'critical' },
    ])
    vi.mocked(apiModule.api.triggers.archive).mockResolvedValue(undefined)
    render(<Triggers />)
    await waitFor(() => expect(screen.getByText('Daily job')).toBeInTheDocument())
    await act(async () => {
      screen.getByRole('button', { name: /archive trigger daily job/i }).click()
    })
    await waitFor(() => {
      expect(apiModule.api.triggers.archive).toHaveBeenCalledWith('tr-3')
    })
  })

  it('offers the controls to the agent owner', async () => {
    localStorage.setItem('summa_user', JSON.stringify({ userId: 'owner-1', rbac: 'owner' }))
    vi.mocked(apiModule.api.agents.list).mockResolvedValue([
      { id: 'a-1', name: 'Worker', ownerHumanId: 'owner-1', class: 'persistent', status: 'active', kind: 'agent' },
    ])
    vi.mocked(apiModule.api.triggers.list).mockResolvedValue([
      { id: 'tr-5', name: 'Owned job', kind: 'schedule', expression: '* * * * *', agentId: 'a-1', status: 'active', criticality: 'standard' },
    ])
    render(<Triggers />)
    await waitFor(() => expect(screen.getByText('Owned job')).toBeInTheDocument())
    expect(screen.getByRole('button', { name: /pause trigger owned job/i })).toBeInTheDocument()
  })

  it('hides the controls from a member who owns no agent of the trigger', async () => {
    localStorage.setItem('summa_user', JSON.stringify({ userId: 'member-1', rbac: 'member' }))
    vi.mocked(apiModule.api.agents.list).mockResolvedValue([])
    vi.mocked(apiModule.api.triggers.list).mockResolvedValue([
      { id: 'tr-6', name: 'Someone else job', kind: 'schedule', expression: '* * * * *', agentId: 'a-9', status: 'active', criticality: 'standard' },
    ])
    render(<Triggers />)
    await waitFor(() => expect(screen.getByText('Someone else job')).toBeInTheDocument())
    expect(screen.queryByRole('button', { name: /pause trigger someone else job/i })).not.toBeInTheDocument()
  })

  it('does not show archive button for archived trigger', async () => {
    vi.mocked(apiModule.api.triggers.list).mockResolvedValue([
      { id: 'tr-4', name: 'Archived job', kind: 'schedule', expression: '0 0 * * *', agentId: 'a-1', status: 'archived', criticality: 'standard' },
    ])
    render(<Triggers />)
    await waitFor(() => expect(screen.getByText('Archived job')).toBeInTheDocument())
    expect(screen.queryByRole('button', { name: /archive trigger archived job/i })).not.toBeInTheDocument()
  })
})
