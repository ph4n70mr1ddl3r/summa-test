import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, waitFor, act } from '@testing-library/react'
import RoleTemplates from './RoleTemplates'
import * as apiModule from '../services/api'

vi.mock('../services/api', () => ({
  api: {
    roleTemplates: {
      list: vi.fn(),
    },
  },
}))

describe('RoleTemplates page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the Role Templates heading', async () => {
    vi.mocked(apiModule.api.roleTemplates.list).mockResolvedValue([])
    const { container } = render(<RoleTemplates />)
    await waitFor(() => {
      expect(container.textContent).toContain('Role Templates')
    })
  })

  it('shows loading state while fetching', async () => {
    vi.mocked(apiModule.api.roleTemplates.list).mockReturnValue(new Promise(() => {}))
    const { container } = render(<RoleTemplates />)
    await waitFor(() => {
      expect(container.textContent).toContain('Loading...')
    })
  })

  it('shows empty state when no templates', async () => {
    vi.mocked(apiModule.api.roleTemplates.list).mockResolvedValue([])
    const { container } = render(<RoleTemplates />)
    await waitFor(() => {
      expect(container.textContent).toContain('No role templates')
    })
  })

  it('renders template cards with data', async () => {
    vi.mocked(apiModule.api.roleTemplates.list).mockResolvedValue([
      { id: 'rt-1', name: 'Ephemeral Worker', version: 1, class: 'ephemeral-subagent', status: 'active' },
    ])
    const { container } = render(<RoleTemplates />)
    await waitFor(() => {
      expect(container.textContent).toContain('Ephemeral Worker')
      expect(container.textContent).toContain('ephemeral-subagent')
      expect(container.textContent).toContain('Version: 1')
      expect(container.textContent).toContain('active')
    })
  })

  it('shows error state on API failure', async () => {
    vi.mocked(apiModule.api.roleTemplates.list).mockRejectedValue(new Error('failed'))
    const { container } = render(<RoleTemplates />)
    await waitFor(() => {
      expect(container.textContent).toContain('Failed to load')
    })
  })

  it('retry button calls loadTemplates on click', async () => {
    vi.mocked(apiModule.api.roleTemplates.list).mockRejectedValue(new Error('failed'))
    const { getByRole, container } = render(<RoleTemplates />)
    await waitFor(() => {
      expect(container.textContent).toContain('Failed to load')
    })
    const retryBtn = getByRole('button', { name: /retry/i })
    vi.mocked(apiModule.api.roleTemplates.list).mockResolvedValue([])
    await act(async () => { retryBtn.click() })
    await waitFor(() => {
      expect(apiModule.api.roleTemplates.list).toHaveBeenCalledTimes(2)
    })
  })
})
