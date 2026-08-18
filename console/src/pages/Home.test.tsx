import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, waitFor } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import Home from './Home'
import * as apiModule from '../services/api'

vi.mock('../services/api', () => ({
  api: {
    health: vi.fn(),
  },
}))

function renderWithRouter(ui: React.ReactNode) {
  return render(<BrowserRouter>{ui}</BrowserRouter>)
}

describe('Home page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(apiModule.api.health).mockResolvedValue({ status: 'ok' })
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
})
