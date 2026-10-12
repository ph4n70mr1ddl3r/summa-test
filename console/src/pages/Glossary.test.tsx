import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import Glossary from './Glossary'
import * as apiModule from '../services/api'

vi.mock('../services/api', () => ({
  api: {
    dna: {
      glossary: vi.fn(),
    },
  },
}))

describe('Glossary page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the DNA Glossary heading', async () => {
    vi.mocked(apiModule.api.dna.glossary).mockResolvedValue([])
    render(<Glossary />)
    await waitFor(() => expect(screen.getByText('DNA Glossary')).toBeInTheDocument())
  })

  it('shows empty state when no entries', async () => {
    vi.mocked(apiModule.api.dna.glossary).mockResolvedValue([])
    render(<Glossary />)
    await waitFor(() => {
      const el = screen.getByText(/no glossary entries/i)
      expect(el).toBeInTheDocument()
    })
  })

  it('displays glossary entries', async () => {
    vi.mocked(apiModule.api.dna.glossary).mockResolvedValue([
      { id: 'g1', domainId: 'd1', term: 'Agent', definition: 'An AI worker', aliases: 'bot', status: 'active', createdAt: 1700000000, updatedAt: 1700000000 },
    ])
    render(<Glossary />)
    await waitFor(() => {
      expect(screen.getByText('1 entries')).toBeInTheDocument()
      expect(screen.getByText('Agent')).toBeInTheDocument()
      expect(screen.getByText('An AI worker')).toBeInTheDocument()
    })
  })

  it('shows error state on API failure', async () => {
    vi.mocked(apiModule.api.dna.glossary).mockRejectedValue(new Error('Network error'))
    render(<Glossary />)
    await waitFor(() => {
      expect(screen.getByText(/error/i)).toBeInTheDocument()
    })
  })
})
