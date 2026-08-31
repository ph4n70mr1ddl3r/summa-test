import { useEffect, useState } from 'react'
import { api, type SpendSnapshot } from '../services/api'

export default function Governance() {
  const [policies, setPolicies] = useState<Record<string, unknown>>({})
  const [quotas, setQuotas] = useState<Record<string, unknown>>({})
  const [spend, setSpend] = useState<SpendSnapshot | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    Promise.all([
      api.governance.policies().catch((e) => { console.error('Failed to load policies:', e); return {}; }),
      api.governance.quotas().catch((e) => { console.error('Failed to load quotas:', e); return {}; }),
      api.governance.spend().catch((e) => { console.error('Failed to load spend:', e); return null; }),
    ]).then(([p, q, s]) => {
      setPolicies(p)
      setQuotas(q)
      setSpend(s)
      setLoading(false)
    }).catch(() => {
      setError('Failed to load governance data')
      setLoading(false)
    })
  }, [])

  if (loading) return <div className="text-gray-400">Loading...</div>
  if (error) return <div className="text-red-400">Error: {error}</div>

  const policyEntries = Object.entries(policies)
  const quotaEntries = Object.entries(quotas)

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold">Governance</h2>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
          <h3 className="text-lg font-semibold text-purple-300 mb-4">Policies ({policyEntries.length})</h3>
          {policyEntries.length === 0 ? (
            <p className="text-gray-500 text-sm">No policies configured.</p>
          ) : (
            <div className="space-y-1">
              {policyEntries.slice(0, 10).map(([k, v]) => (
                <div key={k} className="flex justify-between text-sm">
                  <span className="text-gray-400">{k}</span>
                  <span className="text-gray-200 font-mono">{String(v)}</span>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
          <h3 className="text-lg font-semibold text-purple-300 mb-4">Quotas ({quotaEntries.length})</h3>
          {quotaEntries.length === 0 ? (
            <p className="text-gray-500 text-sm">No quotas configured.</p>
          ) : (
            <div className="space-y-1">
              {quotaEntries.slice(0, 10).map(([k, v]) => (
                <div key={k} className="flex justify-between text-sm">
                  <span className="text-gray-400">{k}</span>
                  <span className="text-gray-200 font-mono">{String(v)}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {spend && (
        <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
          <h3 className="text-lg font-semibold text-purple-300 mb-4">Spend</h3>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div>
              <p className="text-2xl font-bold text-green-400">{spend.reserved}</p>
              <p className="text-xs text-gray-500">Reserved</p>
            </div>
            <div>
              <p className="text-2xl font-bold text-blue-400">{spend.settled}</p>
              <p className="text-xs text-gray-500">Settled</p>
            </div>
            <div>
              <p className="text-2xl font-bold text-yellow-400">{spend.ceiling}</p>
              <p className="text-xs text-gray-500">Ceiling</p>
            </div>
            <div>
              <p className={`text-2xl font-bold ${spend.halted ? 'text-red-400' : 'text-gray-300'}`}>
                {spend.utilization}
              </p>
              <p className="text-xs text-gray-500">
                {spend.halted ? 'HALTED' : 'Utilization'}
              </p>
            </div>
          </div>
        </div>
      )}

      <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
        <h3 className="text-lg font-semibold text-purple-300 mb-4">API Reference</h3>
        <div className="text-xs text-gray-500 space-y-1">
          <p>GET/PUT /api/governance/policies · GET/PUT /api/governance/quotas</p>
          <p>GET /api/governance/spend · POST /api/governance/holds</p>
          <p>POST /api/governance/holds/:id/release</p>
        </div>
      </div>
    </div>
  )
}
