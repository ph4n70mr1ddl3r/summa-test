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

  it('lists content model items', () => {
    const { container } = render(<DnaConsole />)
    expect(container.textContent).toContain('Cards')
    expect(container.textContent).toContain('Goals')
  })

  it('shows DNA proposal endpoints', () => {
    const { container } = render(<DnaConsole />)
    expect(container.textContent).toContain('/api/dna/proposals')
  })
})
