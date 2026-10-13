import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, waitFor, screen, act } from '@testing-library/react'
import Initiatives from './Initiatives'
import * as apiModule from '../services/api'

vi.mock('../services/api', () => ({
  api: {
    initiatives: {
      list: vi.fn(),
    },
  },
}))

describe('Initiatives page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the Initiatives heading', async () => {
    vi.mocked(apiModule.api.initiatives.list).mockResolvedValue([])
    render(<Initiatives />)
    await waitFor(() => {
      expect(screen.getByText('Initiatives')).toBeInTheDocument()
    })
  })

  it('shows loading state while fetching', async () => {
    vi.mocked(apiModule.api.initiatives.list).mockReturnValue(new Promise(() => {}))
    render(<Initiatives />)
    await waitFor(() => {
      expect(screen.getByRole('status')).toBeInTheDocument()
    })
  })

  it('shows empty state when no initiatives', async () => {
    vi.mocked(apiModule.api.initiatives.list).mockResolvedValue([])
    render(<Initiatives />)
    await waitFor(() => {
      expect(screen.getByText(/No initiatives yet/)).toBeInTheDocument()
    })
  })

  it('displays initiatives with status', async () => {
    vi.mocked(apiModule.api.initiatives.list).mockResolvedValue([
      { id: 'i1', title: 'Launch product', sponsor: 'alice', lead: 'bob', status: 'active' },
      { id: 'i2', title: 'Research phase', sponsor: 'carol', lead: 'dave', status: 'proposed' },
    ])
    render(<Initiatives />)
    await waitFor(() => {
      expect(screen.getByText('Launch product')).toBeInTheDocument()
      expect(screen.getByText('active')).toBeInTheDocument()
      expect(screen.getByText('proposed')).toBeInTheDocument()
    })
  })

  it('shows sponsor and lead for each initiative', async () => {
    vi.mocked(apiModule.api.initiatives.list).mockResolvedValue([
      { id: 'i1', title: 'T1', sponsor: 's1', lead: 'l1', status: 'active' },
    ])
    render(<Initiatives />)
    await waitFor(() => {
      expect(screen.getByText(/s1/)).toBeInTheDocument()
      expect(screen.getByText(/l1/)).toBeInTheDocument()
    })
  })

  it('shows error state on API failure', async () => {
    vi.mocked(apiModule.api.initiatives.list).mockRejectedValue(new Error('Network error'))
    render(<Initiatives />)
    await waitFor(() => {
      expect(screen.getByText(/Failed to load/)).toBeInTheDocument()
    })
  })

  it('retry button calls loadData on click', async () => {
    vi.mocked(apiModule.api.initiatives.list).mockRejectedValue(new Error('Network error'))
    const { getByRole } = render(<Initiatives />)
    await waitFor(() => {
      expect(getByRole('button', { name: /retry/i })).toBeInTheDocument()
    })
    vi.mocked(apiModule.api.initiatives.list).mockResolvedValue([])
    await act(async () => { getByRole('button', { name: /retry/i }).click() })
    await waitFor(() => {
      expect(apiModule.api.initiatives.list).toHaveBeenCalledTimes(2)
    })
  })
})
