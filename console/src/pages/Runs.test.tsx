import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import Runs from './Runs'
import * as apiModule from '../services/api'

vi.mock('../services/api', () => ({
  api: {
    runs: {
      list: vi.fn(),
    },
  },
}))

describe('Runs page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the Runs heading', async () => {
    vi.mocked(apiModule.api.runs.list).mockResolvedValue([])
    render(<Runs />)
    await waitFor(() => expect(screen.getByText('Runs')).toBeInTheDocument())
  })

  it('shows empty state when no runs', async () => {
    vi.mocked(apiModule.api.runs.list).mockResolvedValue([])
    render(<Runs />)
    await waitFor(() => {
      const el = screen.getByText(/no runs found/i)
      expect(el).toBeInTheDocument()
    })
  })

  it('displays runs with status', async () => {
    vi.mocked(apiModule.api.runs.list).mockResolvedValue([
      { id: 'r1', agentId: 'a1', status: 'completed' as const, costTokens: 0, costUsd: 0 },
      { id: 'r2', agentId: 'a1', status: 'failed' as const, costTokens: 0, costUsd: 0 },
    ])
    render(<Runs />)
    await waitFor(() => {
      expect(screen.getByText('All (2)')).toBeInTheDocument()
    })
  })

  it('filters by status', async () => {
    vi.mocked(apiModule.api.runs.list).mockResolvedValue([
      { id: 'r1', agentId: 'a1', status: 'running' as const, costTokens: 0, costUsd: 0 },
    ])
    render(<Runs />)
    await waitFor(() => expect(screen.getByText('running (1)')).toBeInTheDocument())
  })

  it('shows error state on API failure', async () => {
    vi.mocked(apiModule.api.runs.list).mockRejectedValue(new Error('Network error'))
    render(<Runs />)
    await waitFor(() => {
      expect(screen.getByText('Failed to load: Network error')).toBeInTheDocument()
    })
  })

  it('retry button calls loadRuns on click', async () => {
    vi.mocked(apiModule.api.runs.list).mockRejectedValue(new Error('Network error'))
    const { getByRole } = render(<Runs />)
    await waitFor(() => {
      expect(getByRole('button', { name: /retry/i })).toBeInTheDocument()
    })
    vi.mocked(apiModule.api.runs.list).mockResolvedValue([])
    fireEvent.click(getByRole('button', { name: /retry/i }))
    await waitFor(() => {
      expect(apiModule.api.runs.list).toHaveBeenCalledTimes(2)
    })
  })

  it('keeps all status filter buttons visible after selecting one', async () => {
    // Regression: server-side filtering reloaded a status-only subset, zeroing
    // the other counts and making the other filter buttons disappear.
    vi.mocked(apiModule.api.runs.list).mockResolvedValue([
      { id: 'r1', agentId: 'a1', status: 'running' as const, costTokens: 0, costUsd: 0 },
      { id: 'r2', agentId: 'a1', status: 'completed' as const, costTokens: 0, costUsd: 0 },
    ])
    render(<Runs />)
    await waitFor(() => expect(screen.getByText('running (1)')).toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: /running \(1\)/i }))

    // Both buttons must still be present after filtering.
    expect(screen.getByRole('button', { name: /running \(1\)/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /completed \(1\)/i })).toBeInTheDocument()
    // Only the filtered row is rendered.
    expect(screen.getAllByText(/r1|a1/).length).toBeGreaterThan(0)
    expect(screen.queryByLabelText('Status: completed')).not.toBeInTheDocument()
    expect(screen.getByLabelText('Status: running')).toBeInTheDocument()
  })
})
