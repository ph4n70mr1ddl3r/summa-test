import { describe, it, expect } from 'vitest'
import { render } from '@testing-library/react'
import Home from './Home'

describe('Home page', () => {
  it('renders the Summa title', () => {
    const { container } = render(<Home />)
    expect(container.textContent).toContain('Summa')
  })

  it('shows navigation links', () => {
    const { container } = render(<Home />)
    expect(container.textContent).toContain('DNA')
    expect(container.textContent).toContain('Org')
    expect(container.textContent).toContain('Asks')
  })
})
