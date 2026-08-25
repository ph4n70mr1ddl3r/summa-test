import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, waitFor } from '@testing-library/react'
import Nodes from './Nodes'
import * as apiModule from '../services/api'

vi.mock('../services/api', () => ({
  api: {
    nodes: {
      list: vi.fn(),
    },
  },
}))

describe('Nodes page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the Nodes heading', async () => {
    vi.mocked(apiModule.api.nodes.list).mockResolvedValue([])
    const { container } = render(<Nodes />)
    await waitFor(() => {
      expect(container.textContent).toContain('Nodes')
    })
  })

  it('shows empty state when no nodes', async () => {
    vi.mocked(apiModule.api.nodes.list).mockResolvedValue([])
    const { container } = render(<Nodes />)
    await waitFor(() => {
      expect(container.textContent).toContain('No trusted nodes')
    })
  })

  it('renders node cards with data', async () => {
    vi.mocked(apiModule.api.nodes.list).mockResolvedValue([
      { id: 'n-1', name: 'us-east-1', kind: 'remote', capabilities: {}, pubkey: 'abc123def456', enrolledAt: 1704067200, status: 'trusted' },
    ])
    const { container } = render(<Nodes />)
    await waitFor(() => {
      expect(container.textContent).toContain('us-east-1')
      expect(container.textContent).toContain('remote')
      expect(container.textContent).toContain('trusted')
    })
  })

  it('shows revoked node styling', async () => {
    vi.mocked(apiModule.api.nodes.list).mockResolvedValue([
      { id: 'n-2', name: 'eu-west-1', kind: 'remote', capabilities: {}, pubkey: 'xyz789', enrolledAt: 1704067200, status: 'revoked' },
    ])
    const { container } = render(<Nodes />)
    await waitFor(() => {
      expect(container.textContent).toContain('revoked')
    })
  })

  it('shows error state on API failure', async () => {
    vi.mocked(apiModule.api.nodes.list).mockRejectedValue(new Error('failed'))
    const { container } = render(<Nodes />)
    await waitFor(() => {
      expect(container.textContent).toContain('Error')
    })
  })
})
