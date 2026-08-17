import { describe, it, expect } from 'vitest'
import { render } from '@testing-library/react'
import AskInbox from './AskInbox'

describe('AskInbox page', () => {
  it('renders the Ask Inbox heading', () => {
    const { container } = render(<AskInbox />)
    expect(container.textContent).toContain('Ask Inbox')
  })

  it('shows ask kinds and SLA tiers', () => {
    const { container } = render(<AskInbox />)
    expect(container.textContent).toContain('approval')
    expect(container.textContent).toContain('critical')
  })
})
