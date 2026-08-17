import { describe, it, expect, vi } from 'vitest'
import { render, waitFor } from '@testing-library/react'
import AskInbox from './AskInbox'
import * as apiModule from '../services/api'

vi.mock('../services/api', () => ({
  api: {
    asks: {
      list: vi.fn(),
    },
  },
}))

describe('AskInbox page', () => {
  it('renders the Ask Inbox heading', async () => {
    vi.mocked(apiModule.api.asks.list).mockResolvedValue([])
    const { container } = render(<AskInbox />)
    expect(container.textContent).toContain('Ask Inbox')
  })

  it('shows ask kinds and SLA tiers after load', async () => {
    vi.mocked(apiModule.api.asks.list).mockResolvedValue([])
    const { container } = render(<AskInbox />)
    await waitFor(() => {
      expect(container.textContent).toContain('approval')
      expect(container.textContent).toContain('critical')
    })
  })
})
