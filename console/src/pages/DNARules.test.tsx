import { describe, it, expect } from 'vitest'
import { render } from '@testing-library/react'
import DNARules from './DNARules'

describe('DNARules page', () => {
  it('renders the DNA Rules heading', () => {
    const { container } = render(<DNARules />)
    expect(container.textContent).toContain('DNA Rules')
  })

  it('shows API endpoints and states', () => {
    const { container } = render(<DNARules />)
    expect(container.textContent).toContain('superseded')
  })

  it('shows effective dates description', () => {
    const { container } = render(<DNARules />)
    expect(container.textContent).toContain('effective dates')
  })

  it('shows supersede endpoint', () => {
    const { container } = render(<DNARules />)
    expect(container.textContent).toContain('supersede')
  })
})
