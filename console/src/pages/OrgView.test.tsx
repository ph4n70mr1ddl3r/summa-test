import { describe, it, expect } from 'vitest'
import { render } from '@testing-library/react'
import OrgView from './OrgView'

describe('OrgView page', () => {
  it('renders the Organization heading', () => {
    const { container } = render(<OrgView />)
    expect(container.textContent).toContain('Organization')
  })

  it('shows members and groups sections', () => {
    const { container } = render(<OrgView />)
    expect(container.textContent).toContain('Members')
    expect(container.textContent).toContain('Groups')
  })

  it('shows RBAC role catalog', () => {
    const { container } = render(<OrgView />)
    expect(container.textContent).toContain('admin')
    expect(container.textContent).toContain('viewer')
  })

  it('shows role templates section', () => {
    const { container } = render(<OrgView />)
    expect(container.textContent).toContain('Role Templates')
  })
})
