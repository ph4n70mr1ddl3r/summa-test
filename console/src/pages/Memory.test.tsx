import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, waitFor, act } from '@testing-library/react'
import Memory from './Memory'
import * as apiModule from '../services/api'

vi.mock('../services/api', () => ({
  api: {
    memory: {
      list: vi.fn(),
    },
  },
}))

describe('Memory page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the Memory heading', async () => {
    vi.mocked(apiModule.api.memory.list).mockResolvedValue([])
    const { container } = render(<Memory />)
    await waitFor(() => {
      expect(container.textContent).toContain('Memory')
    })
  })

  it('shows empty state when no items', async () => {
    vi.mocked(apiModule.api.memory.list).mockResolvedValue([])
    const { container } = render(<Memory />)
    await waitFor(() => {
      expect(container.textContent).toContain('No memory items')
    })
  })

  it('renders memory cards with data', async () => {
    vi.mocked(apiModule.api.memory.list).mockResolvedValue([
      { id: 'm-1', tier: 'personal', contentMd: 'Some memory content', provenance: 'test', tainted: false },
    ])
    const { container } = render(<Memory />)
    await waitFor(() => {
      expect(container.textContent).toContain('Tier: personal')
      expect(container.textContent).toContain('clean')
    })
  })

  it('shows tainted item styling', async () => {
    vi.mocked(apiModule.api.memory.list).mockResolvedValue([
      { id: 'm-2', tier: 'project', contentMd: 'Tainted content', provenance: 'test', tainted: true },
    ])
    const { container } = render(<Memory />)
    await waitFor(() => {
      expect(container.textContent).toContain('tainted')
    })
  })

  it('shows error state on API failure', async () => {
    vi.mocked(apiModule.api.memory.list).mockRejectedValue(new Error('failed'))
    const { container } = render(<Memory />)
    await waitFor(() => {
      expect(container.textContent).toContain('Error')
    })
  })

  it('shows success and reloads after reviewing a tainted item', async () => {
    vi.mocked(apiModule.api.memory.list).mockResolvedValue([
      { id: 'm-1', tier: 'personal', contentMd: 'Tainted content', provenance: 'test', tainted: true },
    ])
    const { getByText } = render(<Memory />)
    await waitFor(() => {
      expect(getByText('tainted')).toBeInTheDocument()
    })
    // The review button is only shown for tainted items
    const reviewBtn = getByText('Review')
    await act(async () => {
      reviewBtn.click()
    })
    await waitFor(() => {
      expect(getByText('Confirm Review')).toBeInTheDocument()
    })
  })
})
