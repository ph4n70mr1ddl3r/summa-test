import { NavLink } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { api } from '../services/api'

export default function Home() {
  return (
    <div className="space-y-8">
      <div className="text-center py-12">
        <h2 className="text-4xl font-bold text-blue-400 mb-4">Summa</h2>
        <p className="text-xl text-gray-400 mb-8">
          The operating system for a hybrid human + AI company
        </p>
        <div className="flex justify-center space-x-4">
          <NavLink
            to="/dna"
            className="px-4 py-2 bg-blue-600 hover:bg-blue-700 rounded text-white"
          >
            Get Started
          </NavLink>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
          <h3 className="text-lg font-semibold text-blue-300 mb-2">DNA Console</h3>
          <p className="text-gray-400">
            Browse and review the Company DNA — knowledge cards, rules, decisions, and glossary.
          </p>
          <NavLink to="/dna" className="mt-4 inline-block text-blue-400 hover:text-blue-300">
            Open DNA →
          </NavLink>
        </div>

        <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
          <h3 className="text-lg font-semibold text-green-300 mb-2">Organization</h3>
          <p className="text-gray-400">
            Manage humans, agents, groups, and RBAC roles across the organization.
          </p>
          <NavLink to="/org" className="mt-4 inline-block text-green-400 hover:text-green-300">
            Open Org →
          </NavLink>
        </div>

        <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
          <h3 className="text-lg font-semibold text-yellow-300 mb-2">Ask Inbox</h3>
          <p className="text-gray-400">
            Review pending approvals, questions, and assignments from agents and the system.
          </p>
          <NavLink to="/asks" className="mt-4 inline-block text-yellow-400 hover:text-yellow-300">
            Open Asks →
          </NavLink>
        </div>
      </div>

      <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
        <h3 className="text-lg font-semibold text-gray-200 mb-4">API Health</h3>
        <ApiStatusCheck />
      </div>
    </div>
  )
}

function ApiStatusCheck() {
  const [status, setStatus] = useState<'ok' | 'error' | 'loading'>('loading')

  useEffect(() => {
    let cancelled = false
    api.health()
      .then(() => { if (!cancelled) setStatus('ok') })
      .catch(() => { if (!cancelled) setStatus('error') })
    return () => { cancelled = true }
  }, [])

  return (
    <div className="flex items-center space-x-2 text-sm">
      <span className={`w-2 h-2 rounded-full ${
        status === 'ok' ? 'bg-green-400' : status === 'error' ? 'bg-red-400' : 'bg-yellow-400'
      }`}></span>
      <span className="text-gray-400">
        {status === 'ok' ? 'Backend: Running' : status === 'error' ? 'Backend: Unreachable' : 'Backend: Checking...'}
      </span>
    </div>
  )
}
