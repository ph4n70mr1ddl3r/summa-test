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
  },
}))

vi.mock('../hooks/useEscapeKey', () => ({
  useEscapeKey: vi.fn(),
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

  it('does not show archive button for archived trigger', async () => {
    vi.mocked(apiModule.api.triggers.list).mockResolvedValue([
      { id: 'tr-4', name: 'Archived job', kind: 'schedule', expression: '0 0 * * *', agentId: 'a-1', status: 'archived', criticality: 'standard' },
    ])
    render(<Triggers />)
    await waitFor(() => expect(screen.getByText('Archived job')).toBeInTheDocument())
    expect(screen.queryByRole('button', { name: /archive trigger archived job/i })).not.toBeInTheDocument()
  })
})
