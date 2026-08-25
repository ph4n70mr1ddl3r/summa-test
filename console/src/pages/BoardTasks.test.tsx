import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, waitFor } from '@testing-library/react'
import BoardTasks from './BoardTasks'
import * as apiModule from '../services/api'

vi.mock('../services/api', () => ({
  api: {
    boardTasks: {
      list: vi.fn(),
    },
  },
}))

describe('BoardTasks page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the Board Tasks heading', async () => {
    vi.mocked(apiModule.api.boardTasks.list).mockResolvedValue([])
    const { container } = render(<BoardTasks />)
    await waitFor(() => {
      expect(container.textContent).toContain('Board Tasks')
    })
  })

  it('shows empty state when no tasks', async () => {
    vi.mocked(apiModule.api.boardTasks.list).mockResolvedValue([])
    const { container } = render(<BoardTasks />)
    await waitFor(() => {
      expect(container.textContent).toContain('No board tasks')
    })
  })

  it('renders task cards with data', async () => {
    vi.mocked(apiModule.api.boardTasks.list).mockResolvedValue([
      { id: 't-1', title: 'Fix login bug', description: '', assigneeMemberId: 'u1', status: 'open', priority: 1, createdBy: 'u1' },
    ])
    const { container } = render(<BoardTasks />)
    await waitFor(() => {
      expect(container.textContent).toContain('Fix login bug')
      expect(container.textContent).toContain('open')
      expect(container.textContent).toContain('Priority: 1')
    })
  })

  it('shows error state on API failure', async () => {
    vi.mocked(apiModule.api.boardTasks.list).mockRejectedValue(new Error('failed'))
    const { container } = render(<BoardTasks />)
    await waitFor(() => {
      expect(container.textContent).toContain('Error')
    })
  })
})
