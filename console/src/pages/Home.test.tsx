import { describe, it, expect } from 'vitest'
import { render } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import Home from './Home'

function renderWithRouter(ui: React.ReactNode) {
  return render(<BrowserRouter>{ui}</BrowserRouter>)
}

describe('Home page', () => {
  it('renders the Summa title', () => {
    const { container } = renderWithRouter(<Home />)
    expect(container.textContent).toContain('Summa')
  })

  it('shows navigation links', () => {
    const { container } = renderWithRouter(<Home />)
    expect(container.textContent).toContain('DNA')
    expect(container.textContent).toContain('Org')
    expect(container.textContent).toContain('Asks')
  })
})
