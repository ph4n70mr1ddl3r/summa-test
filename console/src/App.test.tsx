import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import App from './App'
import * as apiModule from './services/api'

vi.mock('./services/api', () => ({
  setAuthToken: vi.fn(),
  isAuthenticated: vi.fn().mockReturnValue(true),
  getUser: vi.fn().mockReturnValue({ userId: 'u1', rbac: 'admin', name: 'Test User' }),
  api: {},
}))

const wrapper = (children: React.ReactNode) => (
  <MemoryRouter>
    <Routes>
      <Route path="/" element={children} />
      <Route path="/login" element={<div>Login</div>} />
    </Routes>
  </MemoryRouter>
)

describe('App', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(apiModule.isAuthenticated).mockReturnValue(true)
  })

  it('renders navigation items', () => {
    render(wrapper(<App />))
    expect(screen.getByText('Home')).toBeInTheDocument()
    expect(screen.getByText('DNA')).toBeInTheDocument()
    expect(screen.getByText('Org')).toBeInTheDocument()
    expect(screen.getByText('Asks')).toBeInTheDocument()
  })

  it('renders user name when authenticated', () => {
    vi.mocked(apiModule.getUser).mockReturnValue({ userId: 'u1', rbac: 'admin', name: 'Alice' })
    render(wrapper(<App />))
    expect(screen.getByText('Alice')).toBeInTheDocument()
  })

  it('shows sign out button when authenticated', () => {
    render(wrapper(<App />))
    expect(screen.getByText('Sign out')).toBeInTheDocument()
  })

  it('redirects to login when not authenticated', () => {
    vi.mocked(apiModule.isAuthenticated).mockReturnValue(false)
    const { getByText } = render(wrapper(<App />))
    // AuthGuard redirects to /login
    expect(getByText('Login')).toBeInTheDocument()
  })
})
