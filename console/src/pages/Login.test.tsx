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
  ApiError: class ApiError extends Error {
    status: number
    body?: unknown
    constructor(message: string, status: number, body?: unknown) {
      super(message)
      this.name = 'ApiError'
      this.status = status
      this.body = body
    }
  },
  isAuthenticated: vi.fn(),
}))

const navigateMock = vi.fn()
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>()
  return {
    ...actual,
    useNavigate: () => navigateMock,
  }
})

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
    vi.mocked(apiModule.api.auth.login).mockRejectedValue(new apiModule.ApiError('Invalid credentials', 401))
    const { getByLabelText, getByText, findByText } = renderLogin()
    fireEvent.change(getByLabelText(/email/i), { target: { value: 'bad@example.com' } })
    fireEvent.change(getByLabelText(/password/i), { target: { value: 'wrong' } })
    fireEvent.click(getByText('Sign in'))
    await findByText('Invalid email or password')
  })

  it('reports a 403 gate as invalid credentials, not a service error', async () => {
    // The server refuses bad credentials with a 403 gate envelope — the page must
    // not report a mistyped password as an outage.
    vi.mocked(apiModule.api.auth.login).mockRejectedValue(new apiModule.ApiError('Invalid credentials', 403))
    const { getByLabelText, getByText, findByText, queryByText } = renderLogin()
    fireEvent.change(getByLabelText(/email/i), { target: { value: 'bad@example.com' } })
    fireEvent.change(getByLabelText(/password/i), { target: { value: 'wrong' } })
    fireEvent.click(getByText('Sign in'))
    await findByText('Invalid email or password')
    expect(queryByText(/authentication service error/i)).not.toBeInTheDocument()
  })

  it('shows loading state while submitting', async () => {
    let resolveLogin: (v: { token: string; userId: string; rbac: import('../services/api').RbacRole; name: string }) => void
    const loginPromise = new Promise<{ token: string; userId: string; rbac: import('../services/api').RbacRole; name: string }>((resolve) => {
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

  it('rejects open redirect to external domain', async () => {
    navigateMock.mockClear()
    vi.mocked(apiModule.api.auth.login).mockResolvedValue({
      token: 'fake-token',
      userId: 'u1',
      rbac: 'admin',
      name: 'Test User',
    })
    const { getByLabelText, getByText } = render(
      <MemoryRouter initialEntries={[{ pathname: '/login', state: { from: { pathname: 'https://evil.com' } } }]}>
        <Login />
      </MemoryRouter>
    )
    fireEvent.change(getByLabelText(/email/i), { target: { value: 'test@example.com' } })
    fireEvent.change(getByLabelText(/password/i), { target: { value: 'password123' } })
    fireEvent.click(getByText('Sign in'))
    await waitFor(() => {
      expect(navigateMock).not.toHaveBeenCalledWith('https://evil.com/', { replace: true })
      expect(navigateMock).toHaveBeenCalledWith('/', { replace: true })
    })
  })

  it('rejects open redirect via protocol-relative URL', async () => {
    navigateMock.mockClear()
    vi.mocked(apiModule.api.auth.login).mockResolvedValue({
      token: 'fake-token',
      userId: 'u1',
      rbac: 'admin',
      name: 'Test User',
    })
    const { getByLabelText, getByText } = render(
      <MemoryRouter initialEntries={[{ pathname: '/login', state: { from: { pathname: '//evil.com' } } }]}>
        <Login />
      </MemoryRouter>
    )
    fireEvent.change(getByLabelText(/email/i), { target: { value: 'test@example.com' } })
    fireEvent.change(getByLabelText(/password/i), { target: { value: 'password123' } })
    fireEvent.click(getByText('Sign in'))
    await waitFor(() => {
      expect(navigateMock).not.toHaveBeenCalledWith('//evil.com', { replace: true })
      expect(navigateMock).toHaveBeenCalledWith('/', { replace: true })
    })
  })

  it('displays rate-limit message on 429', async () => {
    vi.mocked(apiModule.api.auth.login).mockRejectedValue(new apiModule.ApiError('Too many attempts', 429))
    const { getByLabelText, getByText, findByText } = renderLogin()
    fireEvent.change(getByLabelText(/email/i), { target: { value: 'test@example.com' } })
    fireEvent.change(getByLabelText(/password/i), { target: { value: 'password123' } })
    fireEvent.click(getByText('Sign in'))
    await findByText('Too many login attempts. Please try again shortly.')
  })

  it('redirects to return path after login', async () => {
    vi.mocked(apiModule.api.auth.login).mockResolvedValue({
      token: 'fake-token',
      userId: 'u1',
      rbac: 'admin',
      name: 'Test User',
    })
    const { getByLabelText, getByText } = render(
      <MemoryRouter initialEntries={[{ pathname: '/login', state: { from: { pathname: '/orgs/abc' } } }]}>
        <Login />
      </MemoryRouter>
    )
    fireEvent.change(getByLabelText(/email/i), { target: { value: 'test@example.com' } })
    fireEvent.change(getByLabelText(/password/i), { target: { value: 'password123' } })
    fireEvent.click(getByText('Sign in'))
    await waitFor(() => {
      expect(navigateMock).toHaveBeenCalledWith('/orgs/abc', { replace: true })
    })
  })

  it('rejects open redirect via percent-encoded protocol', async () => {
    navigateMock.mockClear()
    vi.mocked(apiModule.api.auth.login).mockResolvedValue({
      token: 'fake-token',
      userId: 'u1',
      rbac: 'admin',
      name: 'Test User',
    })
    const { getByLabelText, getByText } = render(
      <MemoryRouter initialEntries={[{ pathname: '/login', state: { from: { pathname: '%2F%2Fevil.com' } } }]}>
        <Login />
      </MemoryRouter>
    )
    fireEvent.change(getByLabelText(/email/i), { target: { value: 'test@example.com' } })
    fireEvent.change(getByLabelText(/password/i), { target: { value: 'password123' } })
    fireEvent.click(getByText('Sign in'))
    await waitFor(() => {
      expect(navigateMock).not.toHaveBeenCalledWith('//evil.com', { replace: true })
      expect(navigateMock).toHaveBeenCalledWith('/', { replace: true })
    })
  })

  it('rejects open redirect via path with colon', async () => {
    navigateMock.mockClear()
    vi.mocked(apiModule.api.auth.login).mockResolvedValue({
      token: 'fake-token',
      userId: 'u1',
      rbac: 'admin',
      name: 'Test User',
    })
    const { getByLabelText, getByText } = render(
      <MemoryRouter initialEntries={[{ pathname: '/login', state: { from: { pathname: '/javascript:alert(1)' } } }]}>
        <Login />
      </MemoryRouter>
    )
    fireEvent.change(getByLabelText(/email/i), { target: { value: 'test@example.com' } })
    fireEvent.change(getByLabelText(/password/i), { target: { value: 'password123' } })
    fireEvent.click(getByText('Sign in'))
    await waitFor(() => {
      expect(navigateMock).not.toHaveBeenCalledWith('/javascript:alert(1)', { replace: true })
      expect(navigateMock).toHaveBeenCalledWith('/', { replace: true })
    })
  })

  it('surfaces the server message on a validation refusal', async () => {
    // A malformed address is the caller's to fix — reporting it as a service
    // outage sends the user into a retry loop that never succeeds.
    vi.mocked(apiModule.api.auth.login).mockRejectedValue(
      new apiModule.ApiError('email has invalid format', 422, { code: 'validation', message: 'email has invalid format' }),
    )
    const { getByLabelText, getByRole, findByText } = renderLogin()
    fireEvent.change(getByLabelText(/email/i), { target: { value: 'a@b' } })
    fireEvent.change(getByLabelText(/password/i), { target: { value: 'password123' } })
    await act(async () => {
      fireEvent.click(getByRole('button'))
    })
    expect(await findByText('email has invalid format')).toBeInTheDocument()
  })

  it('surfaces the deployment refusal when local auth is disabled', async () => {
    vi.mocked(apiModule.api.auth.login).mockRejectedValue(
      new apiModule.ApiError('Local authentication is not enabled.', 403, {
        code: 'gate',
        message: 'Local authentication is not enabled. Use OIDC/gateway auth instead.',
      }),
    )
    const { getByLabelText, getByRole, findByText } = renderLogin()
    fireEvent.change(getByLabelText(/email/i), { target: { value: 'a@b.com' } })
    fireEvent.change(getByLabelText(/password/i), { target: { value: 'password123' } })
    await act(async () => {
      fireEvent.click(getByRole('button'))
    })
    expect(await findByText(/Local authentication is not enabled/)).toBeInTheDocument()
  })
})
