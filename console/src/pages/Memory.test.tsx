import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, waitFor, act } from '@testing-library/react'
import Memory from './Memory'
import * as apiModule from '../services/api'

vi.mock('../services/api', () => ({
  api: {
    memory: {
      list: vi.fn(),
      review: vi.fn(),
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
      expect(container.textContent).toContain('Failed to load')
    })
  })

  it('shows success and reloads after reviewing a tainted item', async () => {
    let resolveReview: () => void
    const reviewPromise = new Promise<void>((resolve) => { resolveReview = resolve })
    vi.mocked(apiModule.api.memory.list).mockResolvedValue([
      { id: 'm-1', tier: 'personal', contentMd: 'Tainted content', provenance: 'test', tainted: true },
    ])
    vi.mocked(apiModule.api.memory.review).mockImplementation(() => reviewPromise as unknown as Promise<apiModule.MemoryItem>)
    const { getByText, container } = render(<Memory />)
    await waitFor(() => {
      expect(getByText('tainted')).toBeInTheDocument()
    })
    // Click Review to open the review panel
    const reviewBtn = getByText('Review')
    await act(async () => {
      reviewBtn.click()
    })
    await waitFor(() => {
      expect(getByText('Confirm Review')).toBeInTheDocument()
    })
    // Click Confirm Review to start the review process
    const confirmBtn = getByText('Confirm Review')
    await act(async () => {
      confirmBtn.click()
    })
    // Button should be disabled while reviewing
    expect(confirmBtn).toHaveAttribute('disabled')
    // Resolve the review promise to simulate successful API response
    await act(async () => {
      resolveReview!()
    })
    await waitFor(() => {
      expect(container.textContent).toContain('Item reviewed and taint cleared')
    })
  })
})
