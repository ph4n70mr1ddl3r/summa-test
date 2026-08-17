import { describe, it, expect } from 'vitest'
import { render } from '@testing-library/react'
import Runs from './Runs'

describe('Runs page', () => {
  it('renders the Runs heading', () => {
    const { container } = render(<Runs />)
    expect(container.textContent).toContain('Runs')
  })

  it('shows run lifecycle states', () => {
    const { container } = render(<Runs />)
    expect(container.textContent).toContain('queued')
    expect(container.textContent).toContain('completed')
  })
})
