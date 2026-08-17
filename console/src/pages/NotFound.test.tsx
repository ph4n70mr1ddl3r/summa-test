import { describe, it, expect } from 'vitest'
import { render } from '@testing-library/react'
import NotFound from './NotFound'

describe('NotFound page', () => {
  it('renders the 404 heading', () => {
    const { container } = render(<NotFound />)
    expect(container.textContent).toContain('404')
  })

  it('shows a return link', () => {
    const { container } = render(<NotFound />)
    expect(container.textContent).toContain('Return home')
  })
})
