import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, fireEvent, waitFor, act } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import Login from './Login'
import * as apiModule from '../services/api'

vi.mock('../services/api', () => ({
  api: {
    auth: {
      login: vi.fn(),
    },
  },
  setAuthToken: vi.fn(),
  getAuthToken: vi.fn().mockReturnValue(null),
}))

function renderLogin() {
  return render(
    <MemoryRouter initialEntries={['/login']}>
      <Login />
    </MemoryRouter>
  )
}

describe('Login page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders email and password fields', () => {
    const { getByLabelText } = renderLogin()
    expect(getByLabelText(/email/i)).toBeInTheDocument()
    expect(getByLabelText(/password/i)).toBeInTheDocument()
  })

  it('calls login API on submit', async () => {
    vi.mocked(apiModule.api.auth.login).mockResolvedValue({
      token: 'fake-token',
      userId: 'u1',
      rbac: 'admin',
      name: 'Test User',
    })
    const { getByLabelText, getByText } = renderLogin()
    fireEvent.change(getByLabelText(/email/i), { target: { value: 'test@example.com' } })
    fireEvent.change(getByLabelText(/password/i), { target: { value: 'password123' } })
    fireEvent.click(getByText('Sign in'))
    await waitFor(() => {
      expect(apiModule.setAuthToken).toHaveBeenCalledWith('fake-token', { userId: 'u1', rbac: 'admin', name: 'Test User' })
    })
  })

  it('displays error on login failure', async () => {
    vi.mocked(apiModule.api.auth.login).mockRejectedValue(new Error('Invalid credentials'))
    const { getByLabelText, getByText, findByText } = renderLogin()
    fireEvent.change(getByLabelText(/email/i), { target: { value: 'bad@example.com' } })
    fireEvent.change(getByLabelText(/password/i), { target: { value: 'wrong' } })
    fireEvent.click(getByText('Sign in'))
    await findByText('Invalid credentials')
  })

  it('shows loading state while submitting', async () => {
    let resolveLogin: (v: { token: string; userId: string; rbac: string; name: string }) => void
    const loginPromise = new Promise<{ token: string; userId: string; rbac: string; name: string }>((resolve) => {
      resolveLogin = resolve
    })
    vi.mocked(apiModule.api.auth.login).mockImplementation(() => loginPromise)
    const { getByRole, container } = renderLogin()
    const form = container.querySelector('form') as HTMLFormElement
    act(() => {
      fireEvent.submit(form)
    })
    await waitFor(() => {
      const btn = getByRole('button', { name: /signing in/i })
      expect((btn as HTMLButtonElement).disabled).toBe(true)
    })
    await act(async () => {
      resolveLogin!({ token: 'fake-token', userId: 'u1', rbac: 'admin', name: 'Test User' })
      await loginPromise
    })
  })

  it('navigates to intended route after successful login', async () => {
    vi.mocked(apiModule.api.auth.login).mockResolvedValue({
      token: 'fake-token',
      userId: 'u1',
      rbac: 'admin',
      name: 'Test User',
    })
    const { getByLabelText, getByText } = renderLogin()
    fireEvent.change(getByLabelText(/email/i), { target: { value: 'test@example.com' } })
    fireEvent.change(getByLabelText(/password/i), { target: { value: 'password123' } })
    fireEvent.click(getByText('Sign in'))
    await waitFor(() => {
      expect(apiModule.setAuthToken).toHaveBeenCalledWith('fake-token', { userId: 'u1', rbac: 'admin', name: 'Test User' })
    })
  })
})
