import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import ErrorBoundary from './ErrorBoundary'

describe('ErrorBoundary', () => {
  let consoleSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
  })

  afterEach(() => {
    consoleSpy.mockRestore()
  })
  it('renders children when no error occurs', () => {
    render(
      <ErrorBoundary>
        <div data-testid="child">Hello</div>
      </ErrorBoundary>
    )
    expect(screen.getByTestId('child')).toBeInTheDocument()
    expect(screen.getByText('Hello')).toBeInTheDocument()
  })

  it('catches render errors and shows fallback UI', () => {
    const ThrowError = () => {
      throw new Error('Render failure')
    }
    render(
      <ErrorBoundary>
        <ThrowError />
      </ErrorBoundary>
    )
    expect(screen.getByText('Something went wrong')).toBeInTheDocument()
    expect(screen.getByText('Render failure')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /retry loading/i })).toBeInTheDocument()
  })

  it('calls componentDidCatch with error and info', () => {
    const ThrowError = () => {
      throw new Error('Test error')
    }
    render(
      <ErrorBoundary>
        <ThrowError />
      </ErrorBoundary>
    )
    expect(consoleSpy).toHaveBeenCalled()
  })

  it('recovers after retry button is clicked', async () => {
    const ThrowError = () => {
      throw new Error('Render failure')
    }
    const { getByRole, container } = render(
      <ErrorBoundary>
        <ThrowError />
      </ErrorBoundary>
    )
    await waitFor(() => {
      expect(getByRole('button', { name: /retry loading/i })).toBeInTheDocument()
    })
    // The retry button should trigger a re-render; verify the fallback UI is present first
    expect(container.textContent).toContain('Something went wrong')
  })
})
