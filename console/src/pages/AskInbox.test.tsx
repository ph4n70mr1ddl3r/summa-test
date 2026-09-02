import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, waitFor, fireEvent } from '@testing-library/react'
import AskInbox from './AskInbox'
import { api, type AskKind, type AskTier } from '../services/api'

vi.mock('../services/api', () => ({
  api: {
    asks: {
      list: vi.fn(),
      listByStatus: vi.fn(),
      respond: vi.fn(),
      withdraw: vi.fn(),
    },
  },
  getCurrentActor: vi.fn().mockReturnValue('system'),
}))

describe('AskInbox page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the Ask Inbox heading', async () => {
    vi.mocked(api.asks.listByStatus).mockResolvedValue([])
    const { container } = render(<AskInbox />)
    await waitFor(() => {
      expect(container.textContent).toContain('Ask Inbox')
    })
  })

  it('shows ask kinds and SLA tiers after load', async () => {
    vi.mocked(api.asks.listByStatus).mockResolvedValue([])
    const { container } = render(<AskInbox />)
    await waitFor(() => {
      expect(container.textContent).toContain('approval')
      expect(container.textContent).toContain('critical')
    })
  })

  it('calls respond API when submitting a response', async () => {
    const ask = { id: 'ask-1', kind: 'question' as AskKind, slaTier: 'standard' as AskTier, from: 'agent-1', to: 'human-1', payload: '{}', deadline: Math.floor(Date.now() / 1000) + 86400, status: 'pending' as const }
    vi.mocked(api.asks.listByStatus).mockResolvedValue([ask])
    vi.mocked(api.asks.respond).mockResolvedValue(ask)
    const { getByText, getAllByPlaceholderText } = render(<AskInbox />)
    await waitFor(() => {
      expect(getByText('Ask Inbox')).toBeInTheDocument()
    })
    fireEvent.click(getByText('Respond'))
    const textarea = getAllByPlaceholderText(/type your response/i)[0] as HTMLTextAreaElement
    fireEvent.change(textarea, { target: { value: 'approved' } })
    fireEvent.click(getByText('Submit'))
    await waitFor(() => {
      expect(api.asks.respond).toHaveBeenCalledWith('ask-1', 'approved', 'system')
    })
  })

  it('displays submit error when respond fails', async () => {
    const ask = { id: 'ask-1', kind: 'question' as AskKind, slaTier: 'standard' as AskTier, from: 'agent-1', to: 'human-1', payload: '{}', deadline: Math.floor(Date.now() / 1000) + 86400, status: 'pending' as const }
    vi.mocked(api.asks.listByStatus).mockResolvedValue([ask])
    vi.mocked(api.asks.respond).mockRejectedValue(new Error('Not eligible'))
    const { getByText, getAllByPlaceholderText } = render(<AskInbox />)
    await waitFor(() => {
      expect(getByText('Ask Inbox')).toBeInTheDocument()
    })
    fireEvent.click(getByText('Respond'))
    const textarea = getAllByPlaceholderText(/type your response/i)[0] as HTMLTextAreaElement
    fireEvent.change(textarea, { target: { value: 'approved' } })
    fireEvent.click(getByText('Submit'))
    await waitFor(() => {
      expect(getByText('Not eligible')).toBeInTheDocument()
    })
  })
})
