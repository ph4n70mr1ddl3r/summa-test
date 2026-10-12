import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, act, fireEvent } from '@testing-library/react'
import Governance from './Governance'
import * as apiModule from '../services/api'

vi.mock('../services/api', () => ({
  api: {
    governance: {
      policies: vi.fn(),
      quotas: vi.fn(),
      spend: vi.fn(),
      updatePolicies: vi.fn(),
      updateQuotas: vi.fn(),
    },
  },
  loadWithFallback: async (fetchAll: () => Promise<unknown[]>, fetchIndividual: () => Promise<unknown[]>) => {
    try {
      const data = await fetchAll()
      return { data, error: null }
    } catch (e) {
      const results = await fetchIndividual()
      const hasError = results.some((r: unknown) => r === null)
      const error = hasError
        ? 'Some data could not be loaded: ' + (e instanceof Error ? e.message : String(e))
        : null
      return { data: results, error }
    }
  },
}))

describe('Governance page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the Governance heading', async () => {
    vi.mocked(apiModule.api.governance.policies).mockResolvedValue({})
    vi.mocked(apiModule.api.governance.quotas).mockResolvedValue({})
    vi.mocked(apiModule.api.governance.spend).mockResolvedValue({ reserved: 0, settled: 0, ceiling: 1000000, utilization: '0%', halted: false })
    render(<Governance />)
    await waitFor(() => expect(screen.getByText('Governance')).toBeInTheDocument())
  })

  it('shows policies section', async () => {
    vi.mocked(apiModule.api.governance.policies).mockResolvedValue({ spawn_quota: 10 })
    vi.mocked(apiModule.api.governance.quotas).mockResolvedValue({})
    vi.mocked(apiModule.api.governance.spend).mockResolvedValue({ reserved: 0, settled: 0, ceiling: 1000000, utilization: '0%', halted: false })
    render(<Governance />)
    await waitFor(() => expect(screen.getByText('Policies (1)')).toBeInTheDocument())
  })

  it('shows quotas section', async () => {
    vi.mocked(apiModule.api.governance.policies).mockResolvedValue({})
    vi.mocked(apiModule.api.governance.quotas).mockResolvedValue({ max_concurrent: 5 })
    vi.mocked(apiModule.api.governance.spend).mockResolvedValue({ reserved: 0, settled: 0, ceiling: 1000000, utilization: '0%', halted: false })
    render(<Governance />)
    await waitFor(() => expect(screen.getByText('Quotas (1)')).toBeInTheDocument())
  })

  it('shows spend snapshot', async () => {
    vi.mocked(apiModule.api.governance.policies).mockResolvedValue({})
    vi.mocked(apiModule.api.governance.quotas).mockResolvedValue({})
    vi.mocked(apiModule.api.governance.spend).mockResolvedValue({ reserved: 100, settled: 50, ceiling: 1000, utilization: '5%', halted: false })
    render(<Governance />)
    await waitFor(() => expect(screen.getByText('Spend')).toBeInTheDocument())
  })

  it('shows error state on API failure', async () => {
    vi.mocked(apiModule.api.governance.policies).mockRejectedValue(new Error('Network error'))
    vi.mocked(apiModule.api.governance.quotas).mockRejectedValue(new Error('Network error'))
    vi.mocked(apiModule.api.governance.spend).mockRejectedValue(new Error('Network error'))
    render(<Governance />)
    await waitFor(() => {
      expect(screen.getByText(/network error/i)).toBeInTheDocument()
    })
  })

  it('enters edit mode when clicking a policy cell', async () => {
    vi.mocked(apiModule.api.governance.policies).mockResolvedValue({ spawn_quota: 10 })
    vi.mocked(apiModule.api.governance.quotas).mockResolvedValue({})
    vi.mocked(apiModule.api.governance.spend).mockResolvedValue({ reserved: 0, settled: 0, ceiling: 1000000, utilization: '0%', halted: false })
    render(<Governance />)
    await waitFor(() => expect(screen.getByText('Policies (1)')).toBeInTheDocument())

    const editButton = screen.getByRole('button', { name: /edit spawn_quota: 10/i })
    await act(async () => {
      editButton.click()
    })

    await waitFor(() => {
      const input = screen.getByLabelText('Edit spawn_quota')
      expect(input).toBeInTheDocument()
      expect(input).toHaveValue('10')
    })
  })

  it('saves an edited policy value', async () => {
    vi.mocked(apiModule.api.governance.policies).mockResolvedValue({ spawn_quota: 10 })
    vi.mocked(apiModule.api.governance.quotas).mockResolvedValue({})
    vi.mocked(apiModule.api.governance.spend).mockResolvedValue({ reserved: 0, settled: 0, ceiling: 1000000, utilization: '0%', halted: false })
    vi.mocked(apiModule.api.governance.updatePolicies).mockResolvedValue(undefined)
    render(<Governance />)
    await waitFor(() => expect(screen.getByText('Policies (1)')).toBeInTheDocument())

    const editButton = screen.getByRole('button', { name: /edit spawn_quota: 10/i })
    await act(async () => {
      editButton.click()
    })

    const input = await screen.findByLabelText('Edit spawn_quota')
    await act(async () => {
      fireEvent.change(input, { target: { value: '20' } })
    })

    const saveButton = screen.getByRole('button', { name: 'Save spawn_quota' })
    await act(async () => {
      saveButton.click()
    })

    await waitFor(() => {
      expect(apiModule.api.governance.updatePolicies).toHaveBeenCalledWith({ spawn_quota: 20 })
    })
  })

  it('clears an edited policy value', async () => {
    vi.mocked(apiModule.api.governance.policies).mockResolvedValue({ spawn_quota: 10 })
    vi.mocked(apiModule.api.governance.quotas).mockResolvedValue({})
    vi.mocked(apiModule.api.governance.spend).mockResolvedValue({ reserved: 0, settled: 0, ceiling: 1000000, utilization: '0%', halted: false })
    vi.mocked(apiModule.api.governance.updatePolicies).mockResolvedValue(undefined)
    render(<Governance />)
    await waitFor(() => expect(screen.getByText('Policies (1)')).toBeInTheDocument())

    const editButton = screen.getByRole('button', { name: /edit spawn_quota: 10/i })
    await act(async () => {
      editButton.click()
    })

    const input = await screen.findByLabelText('Edit spawn_quota')
    await act(async () => {
      fireEvent.change(input, { target: { value: '' } })
    })

    const saveButton = screen.getByRole('button', { name: 'Save spawn_quota' })
    await act(async () => {
      saveButton.click()
    })

    await waitFor(() => {
      expect(apiModule.api.governance.updatePolicies).toHaveBeenCalledWith({ spawn_quota: null })
    })
  })

  it('cancels editing when clicking the cancel button', async () => {
    vi.mocked(apiModule.api.governance.policies).mockResolvedValue({ spawn_quota: 10 })
    vi.mocked(apiModule.api.governance.quotas).mockResolvedValue({})
    vi.mocked(apiModule.api.governance.spend).mockResolvedValue({ reserved: 0, settled: 0, ceiling: 1000000, utilization: '0%', halted: false })
    render(<Governance />)
    await waitFor(() => expect(screen.getByText('Policies (1)')).toBeInTheDocument())

    const editButton = screen.getByRole('button', { name: /edit spawn_quota: 10/i })
    await act(async () => {
      editButton.click()
    })

    await waitFor(() => {
      expect(screen.getByLabelText('Edit spawn_quota')).toBeInTheDocument()
    })

    const cancelButton = screen.getByRole('button', { name: /cancel edit spawn_quota/i })
    await act(async () => {
      cancelButton.click()
    })

    await waitFor(() => {
      expect(screen.queryByLabelText('Edit spawn_quota')).not.toBeInTheDocument()
    })
  })

  it('enters edit mode when clicking a quota cell', async () => {
    vi.mocked(apiModule.api.governance.policies).mockResolvedValue({})
    vi.mocked(apiModule.api.governance.quotas).mockResolvedValue({ max_concurrent: 5 })
    vi.mocked(apiModule.api.governance.spend).mockResolvedValue({ reserved: 0, settled: 0, ceiling: 1000000, utilization: '0%', halted: false })
    render(<Governance />)
    await waitFor(() => expect(screen.getByText('Quotas (1)')).toBeInTheDocument())

    const editButton = screen.getByRole('button', { name: /edit max_concurrent: 5/i })
    await act(async () => {
      editButton.click()
    })

    await waitFor(() => {
      const input = screen.getByLabelText('Edit max_concurrent')
      expect(input).toBeInTheDocument()
      expect(input).toHaveValue('5')
    })
  })

  it('saves an edited quota value', async () => {
    vi.mocked(apiModule.api.governance.policies).mockResolvedValue({})
    vi.mocked(apiModule.api.governance.quotas).mockResolvedValue({ max_concurrent: 5 })
    vi.mocked(apiModule.api.governance.spend).mockResolvedValue({ reserved: 0, settled: 0, ceiling: 1000000, utilization: '0%', halted: false })
    vi.mocked(apiModule.api.governance.updateQuotas).mockResolvedValue(undefined)
    render(<Governance />)
    await waitFor(() => expect(screen.getByText('Quotas (1)')).toBeInTheDocument())

    const editButton = screen.getByRole('button', { name: /edit max_concurrent: 5/i })
    await act(async () => {
      editButton.click()
    })

    const input = await screen.findByLabelText('Edit max_concurrent')
    await act(async () => {
      fireEvent.change(input, { target: { value: '10' } })
    })

    const saveButton = screen.getByRole('button', { name: 'Save max_concurrent' })
    await act(async () => {
      saveButton.click()
    })

    await waitFor(() => {
      expect(apiModule.api.governance.updateQuotas).toHaveBeenCalledWith({ max_concurrent: 10 })
    })
  })

  it('clears an edited quota value', async () => {
    vi.mocked(apiModule.api.governance.policies).mockResolvedValue({})
    vi.mocked(apiModule.api.governance.quotas).mockResolvedValue({ max_concurrent: 5 })
    vi.mocked(apiModule.api.governance.spend).mockResolvedValue({ reserved: 0, settled: 0, ceiling: 1000000, utilization: '0%', halted: false })
    vi.mocked(apiModule.api.governance.updateQuotas).mockResolvedValue(undefined)
    render(<Governance />)
    await waitFor(() => expect(screen.getByText('Quotas (1)')).toBeInTheDocument())

    const editButton = screen.getByRole('button', { name: /edit max_concurrent: 5/i })
    await act(async () => {
      editButton.click()
    })

    const input = await screen.findByLabelText('Edit max_concurrent')
    await act(async () => {
      fireEvent.change(input, { target: { value: '' } })
    })

    const saveButton = screen.getByRole('button', { name: 'Save max_concurrent' })
    await act(async () => {
      saveButton.click()
    })

    await waitFor(() => {
      expect(apiModule.api.governance.updateQuotas).toHaveBeenCalledWith({ max_concurrent: null })
    })
  })

  it('cancels quota editing when clicking the cancel button', async () => {
    vi.mocked(apiModule.api.governance.policies).mockResolvedValue({})
    vi.mocked(apiModule.api.governance.quotas).mockResolvedValue({ max_concurrent: 5 })
    vi.mocked(apiModule.api.governance.spend).mockResolvedValue({ reserved: 0, settled: 0, ceiling: 1000000, utilization: '0%', halted: false })
    render(<Governance />)
    await waitFor(() => expect(screen.getByText('Quotas (1)')).toBeInTheDocument())

    const editButton = screen.getByRole('button', { name: /edit max_concurrent: 5/i })
    await act(async () => {
      editButton.click()
    })

    await waitFor(() => {
      expect(screen.getByLabelText('Edit max_concurrent')).toBeInTheDocument()
    })

    const cancelButton = screen.getByRole('button', { name: /cancel edit max_concurrent/i })
    await act(async () => {
      cancelButton.click()
    })

    await waitFor(() => {
      expect(screen.queryByLabelText('Edit max_concurrent')).not.toBeInTheDocument()
    })
  })
})
