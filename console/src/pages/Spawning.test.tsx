import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import Spawning from './Spawning'
import * as apiModule from '../services/api'

vi.mock('../services/api', () => ({
  api: {
    spawn: {
      list: vi.fn(),
      stats: vi.fn(),
    },
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
      expect(screen.getByText('Pending: 1')).toBeInTheDocument()
    })
  })

  it('shows gates section', async () => {
    vi.mocked(apiModule.api.spawn.list).mockResolvedValue([])
    vi.mocked(apiModule.api.spawn.stats).mockResolvedValue({ requested: 0, approved: 0, archived: 0 })
    render(<Spawning />)
    await waitFor(() => expect(screen.getByText('Gates')).toBeInTheDocument())
  })
})
