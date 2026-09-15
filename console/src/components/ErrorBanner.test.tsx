import { describe, it, expect } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { ErrorBanner } from './ErrorBanner'

describe('ErrorBanner', () => {
  it('displays the error message', () => {
    render(<ErrorBanner message="Something went wrong" />)
    expect(screen.getByText('Failed to load: Something went wrong')).toBeInTheDocument()
  })

  it('does not show retry button when onRetry is omitted', () => {
    render(<ErrorBanner message="Connection failed" />)
    expect(screen.getByText('Failed to load: Connection failed')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /retry/i })).not.toBeInTheDocument()
  })

  it('shows retry button when onRetry is provided', () => {
    const handleRetry = vi.fn()
    render(<ErrorBanner message="Timeout" onRetry={handleRetry} />)
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument()
  })

  it('calls onRetry when retry button is clicked', () => {
    const handleRetry = vi.fn()
    render(<ErrorBanner message="Network error" onRetry={handleRetry} />)
    fireEvent.click(screen.getByRole('button', { name: /retry/i }))
    expect(handleRetry).toHaveBeenCalledTimes(1)
  })

  it('renders special characters in message safely', () => {
    render(<ErrorBanner message="<script>alert(1)</script>" />)
    expect(screen.getByText(/<script>alert\(1\)<\/script>/)).toBeInTheDocument()
  })
})
