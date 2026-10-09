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
    let shouldThrow = true
    const ThrowError = () => {
      if (shouldThrow) throw new Error('Render failure')
      return <div data-testid="recovered">Recovered</div>
    }
    const { getByRole, getByTestId, container } = render(
      <ErrorBoundary>
        <ThrowError />
      </ErrorBoundary>
    )
    expect(container.querySelector('[role="alert"]')).toBeInTheDocument()
    expect(screen.getByText('Something went wrong')).toBeInTheDocument()
    const btn = getByRole('button', { name: /retry loading the page/i })
    expect(btn).toBeInTheDocument()
    shouldThrow = false
    btn.click()
    await waitFor(() => {
      expect(getByTestId('recovered')).toBeInTheDocument()
    })
  })
})
