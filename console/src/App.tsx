import { useEffect, useState } from 'react'
import { Outlet, NavLink, useNavigate, useLocation, Navigate } from 'react-router-dom'
import { setAuthToken, isAuthenticated, getUser, setNavigate } from './services/api'

interface NavItem {
  to: string
  label: string
  end?: boolean
}

const navItems: NavItem[] = [
  { to: '/', label: 'Home', end: true },
  { to: '/dna', label: 'DNA', end: true },
  { to: '/dna/proposals', label: 'Proposals' },
  { to: '/dna/cards', label: 'Cards' },
  { to: '/dna/rules', label: 'Rules' },
  { to: '/dna/decisions', label: 'Decisions' },
  { to: '/dna/goals', label: 'Goals' },
  { to: '/org', label: 'Org', end: true },
  { to: '/groups', label: 'Groups' },
  { to: '/asks', label: 'Asks' },
  { to: '/board-tasks', label: 'Board' },
  { to: '/triggers', label: 'Triggers' },
  { to: '/workspaces', label: 'Workspaces' },
  { to: '/spawn', label: 'Spawn' },
  { to: '/runs', label: 'Runs' },
  { to: '/governance', label: 'Governance' },
  { to: '/nodes', label: 'Nodes' },
  { to: '/role-templates', label: 'Roles' },
  { to: '/memory', label: 'Memory' },
  { to: '/initiatives', label: 'Initiatives' },
]

function useAuthState(): boolean {
  const [authed, setAuthed] = useState(isAuthenticated())
  useEffect(() => {
    const onChange = () => setAuthed(isAuthenticated())
    window.addEventListener('summa-auth-change', onChange)
    return () => window.removeEventListener('summa-auth-change', onChange)
  }, [])
  return authed
}

function AuthGuard({ children }: { children: React.ReactNode }) {
  const location = useLocation()
  const authed = useAuthState()

  if (!authed) {
    setAuthToken(null)
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  return <>{children}</>
}

function LogoutButton() {
  const navigate = useNavigate()
  function handleLogout() {
    setAuthToken(null)
    setNavigate(null)
    navigate('/login', { replace: true })
  }
  return (
    <button
      onClick={handleLogout}
      className="text-gray-400 hover:text-white text-sm px-2 py-1 rounded hover:bg-gray-700 transition-colors"
    >
      Sign out
    </button>
  )
}

function ModeLabel() {
  return (
    <span className="text-gray-400 text-sm">
      {import.meta.env.VITE_SUMMA_MODE === 'multi-node' ? 'Multi-node mode' : 'Single-process mode'}
    </span>
  )
}

export default function App() {
  const authed = useAuthState()
  const navigate = useNavigate()
  useEffect(() => {
    setNavigate(navigate)
  }, [])
  return (
    <div className="min-h-screen bg-gray-900 text-gray-100">
      <a href="#main-content" className="sr-only focus:not-sr-only focus:absolute focus:z-50 focus:bg-blue-600 focus:text-white focus:p-2">
        Skip to main content
      </a>
      <header className="bg-gray-800 border-b border-gray-700">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center space-x-8">
              <h1 className="text-xl font-bold text-blue-400">Summa</h1>
              <nav className="flex space-x-4">
                {navItems.map(({ to, label, end }) => (
                  <NavLink
                    key={to}
                    to={to}
                    end={end}
                    className={({ isActive }) =>
                      isActive
                        ? 'text-white border-b-2 border-blue-400 px-1 py-2'
                        : 'text-gray-400 hover:text-white px-1 py-2'
                    }
                  >
                    {({ isActive }) => (
                      <span aria-current={isActive ? 'page' : undefined}>{label}</span>
                    )}
                  </NavLink>
                ))}
              </nav>
            </div>
            <div className="flex items-center space-x-4">
              <ModeLabel />
              {authed && (
                <span className="text-gray-500 text-xs">
                  {getUser()?.name ?? 'authenticated'}
                </span>
              )}
              {authed && <LogoutButton />}
            </div>
          </div>
        </div>
      </header>

      <main id="main-content" className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <AuthGuard>
          <Outlet />
        </AuthGuard>
      </main>
    </div>
  )
}
