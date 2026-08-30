import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
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
      { id: 'r1', agentId: 'a1', status: 'completed' as const },
      { id: 'r2', agentId: 'a1', status: 'failed' as const },
    ])
    render(<Runs />)
    await waitFor(() => {
      expect(screen.getByText('All (2)')).toBeInTheDocument()
    })
  })

  it('filters by status', async () => {
    vi.mocked(apiModule.api.runs.list).mockResolvedValue([
      { id: 'r1', agentId: 'a1', status: 'running' as const },
    ])
    render(<Runs />)
    await waitFor(() => expect(screen.getByText('running (1)')).toBeInTheDocument())
  })
})
