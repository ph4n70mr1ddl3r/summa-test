import { describe, it, expect } from 'vitest'
import { render } from '@testing-library/react'
import Governance from './Governance'

describe('Governance page', () => {
  it('renders the Governance heading', () => {
    const { container } = render(<Governance />)
    expect(container.textContent).toContain('Governance')
  })

  it('shows policies and quotas sections', () => {
    const { container } = render(<Governance />)
    expect(container.textContent).toContain('Policies')
    expect(container.textContent).toContain('Quotas')
  })
})
