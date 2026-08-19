import { describe, it, expect } from 'vitest'
import { render } from '@testing-library/react'
import DNACards from './DNACards'

describe('DNACards page', () => {
  it('renders the DNA Cards heading', () => {
    const { container } = render(<DNACards />)
    expect(container.textContent).toContain('DNA Cards')
  })

  it('shows API endpoints', () => {
    const { container } = render(<DNACards />)
    expect(container.textContent).toContain('GET /api/dna/cards')
  })

  it('shows lifecycle states', () => {
    const { container } = render(<DNACards />)
    expect(container.textContent).toContain('draft')
    expect(container.textContent).toContain('active')
    expect(container.textContent).toContain('retired')
  })

  it('notes that cards are never deleted', () => {
    const { container } = render(<DNACards />)
    expect(container.textContent).toContain('never delete')
  })
})
