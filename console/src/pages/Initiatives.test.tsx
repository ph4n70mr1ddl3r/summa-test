import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, waitFor } from '@testing-library/react'
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
    const { container } = render(<Initiatives />)
    await waitFor(() => {
      expect(container.textContent).toContain('Initiatives')
    })
  })

  it('shows empty state when no initiatives', async () => {
    vi.mocked(apiModule.api.initiatives.list).mockResolvedValue([])
    const { container } = render(<Initiatives />)
    await waitFor(() => {
      expect(container.textContent).toContain('No initiatives yet')
    })
  })

  it('displays initiatives with status', async () => {
    vi.mocked(apiModule.api.initiatives.list).mockResolvedValue([
      { id: 'i1', title: 'Launch product', sponsor: 'alice', lead: 'bob', status: 'active' },
      { id: 'i2', title: 'Research phase', sponsor: 'carol', lead: 'dave', status: 'proposed' },
    ])
    const { container } = render(<Initiatives />)
    await waitFor(() => {
      expect(container.textContent).toContain('Launch product')
      expect(container.textContent).toContain('active')
      expect(container.textContent).toContain('proposed')
    })
  })

  it('shows sponsor and lead for each initiative', async () => {
    vi.mocked(apiModule.api.initiatives.list).mockResolvedValue([
      { id: 'i1', title: 'T1', sponsor: 's1', lead: 'l1', status: 'active' },
    ])
    const { container } = render(<Initiatives />)
    await waitFor(() => {
      expect(container.textContent).toContain('s1')
      expect(container.textContent).toContain('l1')
    })
  })

  it('shows error state on API failure', async () => {
    vi.mocked(apiModule.api.initiatives.list).mockRejectedValue(new Error('Network error'))
    const { container } = render(<Initiatives />)
    await waitFor(() => {
      expect(container.textContent).toContain('Error')
    })
  })
})
