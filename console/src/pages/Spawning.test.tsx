import { describe, it, expect } from 'vitest'
import { render } from '@testing-library/react'
import Spawning from './Spawning'

describe('Spawning page', () => {
  it('renders the Spawning heading', () => {
    const { container } = render(<Spawning />)
    expect(container.textContent).toContain('Spawning')
  })

  it('shows spawn API and gates', () => {
    const { container } = render(<Spawning />)
    expect(container.textContent).toContain('GET /api/spawn')
    expect(container.textContent).toContain('Spend circuit-breaker')
  })
})
