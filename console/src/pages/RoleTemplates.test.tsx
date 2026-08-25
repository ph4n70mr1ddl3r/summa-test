import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, waitFor } from '@testing-library/react'
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
      expect(container.textContent).toContain('Error')
    })
  })
})
