import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import DNACards from './DNACards'
import * as apiModule from '../services/api'

vi.mock('../services/api', () => ({
  api: {
    dna: {
      cards: vi.fn(),
    },
  },
}))

describe('DNACards page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the DNA Cards heading', async () => {
    vi.mocked(apiModule.api.dna.cards).mockResolvedValue([])
    render(<DNACards />)
    await waitFor(() => expect(screen.getByText('DNA Cards')).toBeInTheDocument())
  })

  it('shows empty state when no cards', async () => {
    vi.mocked(apiModule.api.dna.cards).mockResolvedValue([])
    render(<DNACards />)
    await waitFor(() => {
      const el = screen.getByText(/no knowledge cards/i)
      expect(el).toBeInTheDocument()
    })
  })

  it('displays cards with status', async () => {
    vi.mocked(apiModule.api.dna.cards).mockResolvedValue([
      { id: 'c1', domainId: 'd1', title: 'Test Card', definitionMd: 'Some definition', refs: '[]', provenance: '{}', version: 1, status: 'active' },
    ])
    render(<DNACards />)
    await waitFor(() => {
      expect(screen.getByText('1 cards')).toBeInTheDocument()
      expect(screen.getByText('Test Card')).toBeInTheDocument()
    })
  })

  it('shows loading state', () => {
    vi.mocked(apiModule.api.dna.cards).mockReturnValue(new Promise(() => {}))
    const { container } = render(<DNACards />)
    expect(container.textContent).toContain('Loading...')
  })
})
