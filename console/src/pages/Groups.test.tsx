import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import Groups from './Groups'
import * as apiModule from '../services/api'

vi.mock('../services/api', () => ({
  api: {
    groups: {
      list: vi.fn(),
      archive: vi.fn(),
    },
  },
}))

describe('Groups page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the Groups heading', async () => {
    vi.mocked(apiModule.api.groups.list).mockResolvedValue([])
    render(<Groups />)
    await waitFor(() => expect(screen.getByText('Groups')).toBeInTheDocument())
  })

  it('shows empty state when no groups', async () => {
    vi.mocked(apiModule.api.groups.list).mockResolvedValue([])
    render(<Groups />)
    await waitFor(() => {
      const el = screen.getByText(/no groups configured/i)
      expect(el).toBeInTheDocument()
    })
  })

  it('displays groups with status and archive button', async () => {
    vi.mocked(apiModule.api.groups.list).mockResolvedValue([
      { id: 'g1', name: 'Engineering', leaderMemberId: 'h1', status: 'active', createdAt: 0 },
    ])
    render(<Groups />)
    await waitFor(() => {
      expect(screen.getByText('Engineering')).toBeInTheDocument()
      expect(screen.getByText('active')).toBeInTheDocument()
      expect(screen.getByText('Archive')).toBeInTheDocument()
    })
  })

  it('shows loading state', () => {
    vi.mocked(apiModule.api.groups.list).mockReturnValue(new Promise(() => {}))
    const { container } = render(<Groups />)
    expect(container.textContent).toContain('Loading...')
  })

  it('calls archive on archive button click', async () => {
    vi.mocked(apiModule.api.groups.list).mockResolvedValue([
      { id: 'g1', name: 'Engineering', status: 'active', createdAt: 0 },
    ])
    vi.mocked(apiModule.api.groups.archive).mockResolvedValue({ id: 'g1', name: 'Engineering', status: 'archived' } as apiModule.Group)
    render(<Groups />)
    await waitFor(() => {
      expect(screen.getByText('Engineering')).toBeInTheDocument()
    })
    const archiveBtn = screen.getByText('Archive')
    archiveBtn.click()
    await waitFor(() => {
      expect(apiModule.api.groups.archive).toHaveBeenCalledWith('g1')
    })
  })

  it('shows API reference section', async () => {
    vi.mocked(apiModule.api.groups.list).mockResolvedValue([])
    render(<Groups />)
    await waitFor(() => {
      expect(screen.getByText('API Reference')).toBeInTheDocument()
      expect(screen.getByText(/GET.*api\/org\/groups/i)).toBeInTheDocument()
    })
  })
})
