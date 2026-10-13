import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, waitFor, act } from '@testing-library/react'
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

  it('shows loading state while fetching', async () => {
    vi.mocked(apiModule.api.boardTasks.list).mockReturnValue(new Promise(() => {}))
    const { container } = render(<BoardTasks />)
    await waitFor(() => {
      expect(container.textContent).toContain('Loading...')
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
      expect(container.textContent).toContain('Open')
      expect(container.textContent).toContain('Priority: 1')
    })
  })

  it('shows error state on API failure', async () => {
    vi.mocked(apiModule.api.boardTasks.list).mockRejectedValue(new Error('failed'))
    const { container } = render(<BoardTasks />)
    await waitFor(() => {
      expect(container.textContent).toContain('Failed to load')
    })
  })

  it('retry button calls loadTasks on click', async () => {
    vi.mocked(apiModule.api.boardTasks.list).mockRejectedValue(new Error('failed'))
    const { getByRole, container } = render(<BoardTasks />)
    await waitFor(() => {
      expect(container.textContent).toContain('Failed to load')
    })
    const retryBtn = getByRole('button', { name: /retry/i })
    vi.mocked(apiModule.api.boardTasks.list).mockResolvedValue([])
    await act(async () => { retryBtn.click() })
    await waitFor(() => {
      expect(apiModule.api.boardTasks.list).toHaveBeenCalledTimes(2)
    })
  })
})
