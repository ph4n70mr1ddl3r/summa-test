import { Outlet, NavLink } from 'react-router-dom'

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
                <NavLink
                  to="/"
                  className={({ isActive }) => isActive ? 'text-white border-b-2 border-blue-400 px-1 py-2' : 'text-gray-400 hover:text-white px-1 py-2'}
                  aria-current={isActive ? 'page' : undefined}
                >
                  Home
                </NavLink>
                <NavLink
                  to="/dna"
                  className={({ isActive }) => isActive ? 'text-white border-b-2 border-blue-400 px-1 py-2' : 'text-gray-400 hover:text-white px-1 py-2'}
                  aria-current={isActive ? 'page' : undefined}
                >
                  DNA
                </NavLink>
                <NavLink
                  to="/org"
                  className={({ isActive }) => isActive ? 'text-white border-b-2 border-blue-400 px-1 py-2' : 'text-gray-400 hover:text-white px-1 py-2'}
                  aria-current={isActive ? 'page' : undefined}
                >
                  Org
                </NavLink>
                <NavLink
                  to="/asks"
                  className={({ isActive }) => isActive ? 'text-white border-b-2 border-blue-400 px-1 py-2' : 'text-gray-400 hover:text-white px-1 py-2'}
                  aria-current={isActive ? 'page' : undefined}
                >
                  Asks
                </NavLink>
              <NavLink
                to="/spawn"
                className={({ isActive }) => isActive ? 'text-white border-b-2 border-blue-400 px-1 py-2' : 'text-gray-400 hover:text-white px-1 py-2'}
                aria-current={isActive ? 'page' : undefined}
              >
                Spawn
              </NavLink>
              <NavLink
                to="/runs"
                className={({ isActive }) => isActive ? 'text-white border-b-2 border-blue-400 px-1 py-2' : 'text-gray-400 hover:text-white px-1 py-2'}
                aria-current={isActive ? 'page' : undefined}
              >
                Runs
              </NavLink>
              <NavLink
                to="/governance"
                className={({ isActive }) => isActive ? 'text-white border-b-2 border-blue-400 px-1 py-2' : 'text-gray-400 hover:text-white px-1 py-2'}
                aria-current={isActive ? 'page' : undefined}
              >
                Governance
              </NavLink>
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
