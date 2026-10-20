import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, waitFor, fireEvent } from '@testing-library/react'
import AskInbox from './AskInbox'
import { api, type AskKind, type AskTier, getUser } from '../services/api'

vi.mock('../services/api', () => ({
  api: {
    asks: {
      list: vi.fn(),
      listByStatus: vi.fn(),
      respond: vi.fn(),
      withdraw: vi.fn(),
    },
  },
  getUser: vi.fn(() => ({ userId: 'agent-1', rbac: 'member', name: 'Test Agent' })),
}))

describe('AskInbox page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the Ask Inbox heading', async () => {
    vi.mocked(api.asks.list).mockResolvedValue([])
    const { container } = render(<AskInbox />)
    await waitFor(() => {
      expect(container.textContent).toContain('Ask Inbox')
    })
  })

  it('shows ask kinds and SLA tiers after load', async () => {
    vi.mocked(api.asks.list).mockResolvedValue([])
    const { container } = render(<AskInbox />)
    await waitFor(() => {
      expect(container.textContent).toContain('approval')
      expect(container.textContent).toContain('critical')
    })
  })

  it('calls respond API when submitting a response', async () => {
    const ask = { id: 'ask-1', kind: 'question' as AskKind, slaTier: 'standard' as AskTier, from: 'agent-1', to: 'human-1', payload: '{}', deadline: Math.floor(Date.now() / 1000) + 86400, status: 'pending' as const }
    vi.mocked(api.asks.list).mockResolvedValue([ask])
    vi.mocked(api.asks.respond).mockResolvedValue(ask)
    const { getByText, getAllByPlaceholderText } = render(<AskInbox />)
    await waitFor(() => {
      expect(getByText('Ask Inbox')).toBeInTheDocument()
    })
    fireEvent.click(getByText('Respond'))
    const textarea = getAllByPlaceholderText(/type your response/i)[0] as HTMLTextAreaElement
    fireEvent.change(textarea, { target: { value: 'approved' } })
    fireEvent.click(getByText('Accept'))
    await waitFor(() => {
      expect(api.asks.respond).toHaveBeenCalledWith('ask-1', 'approved', 'accept')
    })
  })

  it('displays submit error when respond fails', async () => {
    const ask = { id: 'ask-1', kind: 'question' as AskKind, slaTier: 'standard' as AskTier, from: 'agent-1', to: 'human-1', payload: '{}', deadline: Math.floor(Date.now() / 1000) + 86400, status: 'pending' as const }
    vi.mocked(api.asks.list).mockResolvedValue([ask])
    vi.mocked(api.asks.respond).mockRejectedValue(new Error('Not eligible'))
    const { getByText, getAllByPlaceholderText } = render(<AskInbox />)
    await waitFor(() => {
      expect(getByText('Ask Inbox')).toBeInTheDocument()
    })
    fireEvent.click(getByText('Respond'))
    const textarea = getAllByPlaceholderText(/type your response/i)[0] as HTMLTextAreaElement
    fireEvent.change(textarea, { target: { value: 'approved' } })
    fireEvent.click(getByText('Accept'))
    await waitFor(() => {
      expect(getByText('Not eligible')).toBeInTheDocument()
    })
  })

  it('displays submit error when withdraw fails', async () => {
    const ask = { id: 'ask-1', kind: 'question' as AskKind, slaTier: 'standard' as AskTier, from: 'agent-1', to: 'human-1', payload: '{}', deadline: Math.floor(Date.now() / 1000) + 86400, status: 'pending' as const }
    vi.mocked(api.asks.list).mockResolvedValue([ask])
    vi.mocked(api.asks.withdraw).mockRejectedValue(new Error('Not allowed'))
    const { getByText } = render(<AskInbox />)
    await waitFor(() => {
      expect(getByText('Ask Inbox')).toBeInTheDocument()
    })
    fireEvent.click(getByText('Withdraw'))
    await waitFor(() => {
      expect(getByText('Not allowed')).toBeInTheDocument()
    })
  })

  it('reloads asks after successful withdraw', async () => {
    const ask = { id: 'ask-1', kind: 'question' as AskKind, slaTier: 'standard' as AskTier, from: 'agent-1', to: 'human-1', payload: '{}', deadline: Math.floor(Date.now() / 1000) + 86400, status: 'pending' as const }
    vi.mocked(api.asks.list).mockResolvedValue([ask])
    vi.mocked(api.asks.withdraw).mockResolvedValue(ask)
    const { getByText } = render(<AskInbox />)
    await waitFor(() => {
      expect(getByText('Ask Inbox')).toBeInTheDocument()
    })
    fireEvent.click(getByText('Withdraw'))
    await waitFor(() => {
      expect(api.asks.withdraw).toHaveBeenCalledWith('ask-1')
      expect(api.asks.list).toHaveBeenCalledTimes(2)
    })
  })

  it('disables withdraw when user prefix is stripped and IDs match', async () => {
    vi.mocked(getUser).mockReturnValue({ userId: 'h:user-1', rbac: 'member', name: 'Test User' })
    const ask = { id: 'ask-1', kind: 'question' as AskKind, slaTier: 'standard' as AskTier, from: 'h:user-1', to: 'human-2', payload: '{}', deadline: Math.floor(Date.now() / 1000) + 86400, status: 'pending' as const }
    vi.mocked(api.asks.list).mockResolvedValue([ask])
    const { getByRole, getByText } = render(<AskInbox />)
    await waitFor(() => {
      expect(getByText('Ask Inbox')).toBeInTheDocument()
    })
    const withdrawBtn = getByRole('button', { name: /withdraw ask from/i })
    expect(withdrawBtn).not.toBeDisabled()
  })

  it('disables withdraw when user does not match ask originator', async () => {
    vi.mocked(getUser).mockReturnValue({ userId: 'h:other-user', rbac: 'member', name: 'Other' })
    const ask = { id: 'ask-1', kind: 'question' as AskKind, slaTier: 'standard' as AskTier, from: 'h:user-1', to: 'human-2', payload: '{}', deadline: Math.floor(Date.now() / 1000) + 86400, status: 'pending' as const }
    vi.mocked(api.asks.list).mockResolvedValue([ask])
    const { getByRole, getByText } = render(<AskInbox />)
    await waitFor(() => {
      expect(getByText('Ask Inbox')).toBeInTheDocument()
    })
    const withdrawBtn = getByRole('button', { name: /withdraw ask from/i })
    expect(withdrawBtn).toBeDisabled()
  })
})
