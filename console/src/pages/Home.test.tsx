import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import Home from './Home'
import * as apiModule from '../services/api'

vi.mock('../services/api', () => ({
  api: {
    health: vi.fn(),
  },
}))

function renderWithRouter(ui: React.ReactNode) {
  return render(<MemoryRouter>{ui}</MemoryRouter>)
}

describe('Home page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(apiModule.api.health).mockResolvedValue({ status: 'UP', service: 'summa', mode: 'single-process', checks: { database: 'up', git_store: 'up' } })
  })

  it('renders the Summa title', async () => {
    const { container } = renderWithRouter(<Home />)
    await waitFor(() => {
      expect(container.textContent).toContain('Summa')
    })
  })

  it('shows navigation links', async () => {
    const { container } = renderWithRouter(<Home />)
    await waitFor(() => {
      expect(container.textContent).toContain('DNA')
      expect(container.textContent).toContain('Org')
      expect(container.textContent).toContain('Asks')
    })
  })

  it('shows API health status', async () => {
    const { container } = renderWithRouter(<Home />)
    await waitFor(() => {
      expect(container.textContent).toContain('Backend')
    })
  })

  it('shows error state on API health failure', async () => {
    vi.mocked(apiModule.api.health).mockRejectedValue(new Error('Service unavailable'))
    const { container } = renderWithRouter(<Home />)
    await waitFor(() => {
      expect(container.textContent).toContain('Failed to load: Service unavailable')
    })
  })

  it('recovers to running state when Retry succeeds after a health-check failure', async () => {
    const user = userEvent.setup()
    vi.mocked(apiModule.api.health)
      .mockRejectedValueOnce(new Error('Service unavailable'))
      .mockResolvedValue({ status: 'UP', service: 'summa', mode: 'single-process', checks: { database: 'up', git_store: 'up' } })

    const { container } = renderWithRouter(<Home />)
    await waitFor(() => {
      expect(container.textContent).toContain('Service unavailable')
    })

    const retry = screen.getByRole('button', { name: 'Retry loading' })
    await user.click(retry)

    await waitFor(() => {
      expect(container.textContent).toContain('Backend: Running')
    })
    expect(container.textContent).not.toContain('Service unavailable')
  })
})
