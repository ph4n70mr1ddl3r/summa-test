import { Outlet, NavLink } from 'react-router-dom'

interface NavItem {
  to: string
  label: string
}

const navItems: NavItem[] = [
  { to: '/', label: 'Home' },
  { to: '/dna', label: 'DNA' },
  { to: '/org', label: 'Org' },
  { to: '/asks', label: 'Asks' },
  { to: '/spawn', label: 'Spawn' },
  { to: '/runs', label: 'Runs' },
  { to: '/governance', label: 'Governance' },
]

export default function App() {
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
                {navItems.map(({ to, label }) => (
                  <NavLink
                    key={to}
                    to={to}
                    end={to === '/'}
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
              <span className="text-gray-400 text-sm">Single-process mode</span>
            </div>
          </div>
        </div>
      </header>

      <main id="main-content" className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <Outlet />
      </main>
    </div>
  )
}
