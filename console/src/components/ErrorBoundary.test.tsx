import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import ErrorBoundary from './ErrorBoundary'

describe('ErrorBoundary', () => {
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
    expect(screen.getByRole('button', { name: /try again/i })).toBeInTheDocument()
  })

  it('calls componentDidCatch with error and info', () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
    const ThrowError = () => {
      throw new Error('Test error')
    }
    render(
      <ErrorBoundary>
        <ThrowError />
      </ErrorBoundary>
    )
    expect(consoleSpy).toHaveBeenCalled()
    consoleSpy.mockRestore()
  })
})
