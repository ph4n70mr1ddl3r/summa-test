import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, waitFor } from '@testing-library/react'
import Workspaces from './Workspaces'
import * as apiModule from '../services/api'

vi.mock('../services/api', () => ({
  api: {
    workspaces: {
      list: vi.fn(),
    },
  },
}))

describe('Workspaces page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the Workspaces heading', async () => {
    vi.mocked(apiModule.api.workspaces.list).mockResolvedValue([])
    const { container } = render(<Workspaces />)
    await waitFor(() => {
      expect(container.textContent).toContain('Workspaces')
    })
  })

  it('shows empty state when no workspaces', async () => {
    vi.mocked(apiModule.api.workspaces.list).mockResolvedValue([])
    const { container } = render(<Workspaces />)
    await waitFor(() => {
      expect(container.textContent).toContain('No workspaces')
    })
  })

  it('renders workspace cards with data', async () => {
    vi.mocked(apiModule.api.workspaces.list).mockResolvedValue([
      { id: 'ws-1', name: 'Project Alpha', kind: 'project', initiativeIds: '[]', domainIds: '[]', claimEpoch: 1, participants: '["u1","u2"]' },
    ])
    const { container } = render(<Workspaces />)
    await waitFor(() => {
      expect(container.textContent).toContain('Project Alpha')
      expect(container.textContent).toContain('project')
      expect(container.textContent).toContain('2 participants')
    })
  })

  it('shows error state on API failure', async () => {
    vi.mocked(apiModule.api.workspaces.list).mockRejectedValue(new Error('failed'))
    const { container } = render(<Workspaces />)
    await waitFor(() => {
      expect(container.textContent).toContain('Error')
    })
  })
})
