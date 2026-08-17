import { describe, it, expect } from 'vitest'
import { render } from '@testing-library/react'
import DNADecisions from './DNADecisions'

describe('DNADecisions page', () => {
  it('renders the DNA Decisions heading', () => {
    const { container } = render(<DNADecisions />)
    expect(container.textContent).toContain('DNA Decisions')
  })

  it('notes immutability', () => {
    const { container } = render(<DNADecisions />)
    expect(container.textContent).toContain('immutable')
  })
})
