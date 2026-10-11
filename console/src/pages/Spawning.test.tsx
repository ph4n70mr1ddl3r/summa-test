import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, act } from '@testing-library/react'
import Spawning from './Spawning'
import * as apiModule from '../services/api'

vi.mock('../services/api', () => ({
  api: {
    spawn: {
      list: vi.fn(),
      stats: vi.fn(),
      approve: vi.fn(),
      deny: vi.fn(),
    },
  },
  loadWithFallback: async (fetchAll: () => Promise<unknown[]>, fetchIndividual: () => Promise<unknown[]>) => {
    try {
      const data = await fetchAll()
      return { data, error: null }
    } catch (e) {
      const results = await fetchIndividual()
      const hasError = results.some((r: unknown) => r === null)
      const error = hasError
        ? 'Some data could not be loaded: ' + (e instanceof Error ? e.message : String(e))
        : null
      return { data: results, error }
    }
  },
}))

describe('Spawning page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the Spawning heading', async () => {
    vi.mocked(apiModule.api.spawn.list).mockResolvedValue([])
    vi.mocked(apiModule.api.spawn.stats).mockResolvedValue({ requested: 0, approved: 0, archived: 0 })
    render(<Spawning />)
    await waitFor(() => expect(screen.getByText('Spawning')).toBeInTheDocument())
  })

  it('shows empty state when no requests', async () => {
    vi.mocked(apiModule.api.spawn.list).mockResolvedValue([])
    vi.mocked(apiModule.api.spawn.stats).mockResolvedValue({ requested: 0, approved: 0, archived: 0 })
    render(<Spawning />)
    await waitFor(() => {
      const el = screen.getByText(/no spawn requests/i)
      expect(el).toBeInTheDocument()
    })
  })

  it('displays spawn requests', async () => {
    vi.mocked(apiModule.api.spawn.list).mockResolvedValue([
      { id: 's1', requesterId: 'a1', class: 'ephemeral', purpose: 'Test task', status: 'requested' as const },
    ])
    vi.mocked(apiModule.api.spawn.stats).mockResolvedValue({ requested: 1, approved: 0, archived: 0 })
    render(<Spawning />)
    await waitFor(() => {
      expect(screen.getByText('Requested: 1')).toBeInTheDocument()
    })
  })

  it('shows gates section', async () => {
    vi.mocked(apiModule.api.spawn.list).mockResolvedValue([])
    vi.mocked(apiModule.api.spawn.stats).mockResolvedValue({ requested: 0, approved: 0, archived: 0 })
    render(<Spawning />)
    await waitFor(() => expect(screen.getByText('Gates')).toBeInTheDocument())
  })

  it('shows error state on API failure', async () => {
    vi.mocked(apiModule.api.spawn.list).mockRejectedValue(new Error('Network error'))
    render(<Spawning />)
    await waitFor(() => {
      expect(screen.getByText(/error/i)).toBeInTheDocument()
    })
  })

  it('shows confirm dialog per request when multiple pending exist', async () => {
    vi.mocked(apiModule.api.spawn.list).mockResolvedValue([
      { id: 's1', requesterId: 'a1', class: 'ephemeral', purpose: 'Task 1', status: 'requested' as const },
      { id: 's2', requesterId: 'a2', class: 'ephemeral', purpose: 'Task 2', status: 'requested' as const },
    ])
    vi.mocked(apiModule.api.spawn.stats).mockResolvedValue({ requested: 2, approved: 0, archived: 0 })
    render(<Spawning />)
    await waitFor(() => expect(screen.getByText('Requested: 2')).toBeInTheDocument())

    // Click Approve on first request
    const approveButtons = screen.getAllByText('Approve')
    expect(approveButtons).toHaveLength(2)
    await act(async () => {
      approveButtons[0].click()
    })

    // Confirm dialog should appear for first request only
    await waitFor(() => {
      expect(screen.getByText('Confirm?')).toBeInTheDocument()
    })
  })
})
