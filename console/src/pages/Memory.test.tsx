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
    dna: {
      domains: vi.fn(),
    },
    workspaces: {
      list: vi.fn(),
    },
  },
}))

describe('Memory page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.removeItem('summa_user')
    vi.mocked(apiModule.api.dna.domains).mockResolvedValue([])
    vi.mocked(apiModule.api.workspaces.list).mockResolvedValue([])
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
    localStorage.setItem('summa_user', JSON.stringify({ userId: 'member-1', rbac: 'member' }))
    let resolveReview: () => void
    const reviewPromise = new Promise<void>((resolve) => { resolveReview = resolve })
    vi.mocked(apiModule.api.memory.list).mockResolvedValue([
      { id: 'm-1', tier: 'personal', memberId: 'member-1', contentMd: 'Tainted content', provenance: 'test', tainted: true },
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

  it('hides the review control on a personal item belonging to another member', async () => {
    // SUB-041: personal-tier taint is cleared by the item's member alone — the
    // control is offered only where the door would admit the caller.
    localStorage.setItem('summa_user', JSON.stringify({ userId: 'member-1', rbac: 'member' }))
    vi.mocked(apiModule.api.memory.list).mockResolvedValue([
      { id: 'm-2', tier: 'personal', memberId: 'member-2', contentMd: 'Tainted content', provenance: 'test', tainted: true },
    ])
    const { queryByText } = render(<Memory />)
    await waitFor(() => {
      expect(queryByText('tainted')).toBeInTheDocument()
    })
    expect(queryByText('Review')).toBeNull()
  })

  it('hides the review control from a viewer', async () => {
    // A viewer holds no write surface — the backend's write gate refuses the
    // review door for every viewer.
    localStorage.setItem('summa_user', JSON.stringify({ userId: 'viewer-1', rbac: 'viewer' }))
    vi.mocked(apiModule.api.memory.list).mockResolvedValue([
      { id: 'm-3', tier: 'proposal', contentMd: 'Tainted content', provenance: 'test', tainted: true },
    ])
    const { queryByText } = render(<Memory />)
    await waitFor(() => {
      expect(queryByText('tainted')).toBeInTheDocument()
    })
    expect(queryByText('Review')).toBeNull()
  })

  it('offers the review control on a project item the caller owns a domain of', async () => {
    localStorage.setItem('summa_user', JSON.stringify({ userId: 'owner-1', rbac: 'owner' }))
    vi.mocked(apiModule.api.dna.domains).mockResolvedValue([
      { id: 'domain-1', name: 'Core', ownerHumanId: 'owner-1', access: 'public', status: 'active' },
    ])
    vi.mocked(apiModule.api.workspaces.list).mockResolvedValue([
      { id: 'ws-1', name: 'Project', kind: 'project', initiativeIds: '[]', domainIds: '["domain-1"]', claimEpoch: 0, participants: '[]' },
    ])
    vi.mocked(apiModule.api.memory.list).mockResolvedValue([
      { id: 'm-4', tier: 'project', workspaceId: 'ws-1', contentMd: 'Tainted content', provenance: 'test', tainted: true },
    ])
    const { getByText } = render(<Memory />)
    await waitFor(() => {
      expect(getByText('Review')).toBeInTheDocument()
    })
  })
})
