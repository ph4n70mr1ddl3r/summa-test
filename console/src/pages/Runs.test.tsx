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
    expect(container.textContent).toContain('failed')
  })

  it('shows metering info', () => {
    const { container } = render(<Runs />)
    expect(container.textContent).toContain('cost_tokens')
    expect(container.textContent).toContain('Audit event on every transition')
  })

  it('shows triggers section', () => {
    const { container } = render(<Runs />)
    expect(container.textContent).toContain('Triggers')
  })
})
