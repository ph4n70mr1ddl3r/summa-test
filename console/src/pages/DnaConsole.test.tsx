import { describe, it, expect } from 'vitest'
import { render } from '@testing-library/react'
import DnaConsole from './DnaConsole'

describe('DnaConsole page', () => {
  it('renders the DNA Console heading', () => {
    const { container } = render(<DnaConsole />)
    expect(container.textContent).toContain('DNA Console')
  })

  it('shows domain and review queue sections', () => {
    const { container } = render(<DnaConsole />)
    expect(container.textContent).toContain('Domains')
    expect(container.textContent).toContain('Review Queue')
  })
})
