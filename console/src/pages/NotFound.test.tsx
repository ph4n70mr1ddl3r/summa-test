import { describe, it, expect } from 'vitest'
import { render } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import NotFound from './NotFound'

function renderNotFound() {
  return render(
    <MemoryRouter>
      <NotFound />
    </MemoryRouter>
  )
}

describe('NotFound page', () => {
  it('renders the 404 heading', () => {
    const { container } = renderNotFound()
    expect(container.textContent).toContain('404')
  })

  it('shows a return link', () => {
    const { container } = renderNotFound()
    expect(container.textContent).toContain('Return home')
  })
})
