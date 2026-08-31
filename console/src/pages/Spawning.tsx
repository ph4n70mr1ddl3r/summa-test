import { useEffect, useState } from 'react'
import { api, type SpawnRequest } from '../services/api'
import { escapeHtml } from '../utils/escapeHtml'

export default function Spawning() {
  const [requests, setRequests] = useState<SpawnRequest[]>([])
  const [stats, setStats] = useState<{ requested: number; approved: number; archived: number } | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let aborted = false
    Promise.all([
      api.spawn.list(),
      api.spawn.stats(),
    ]).then(([r, s]) => {
      if (aborted) return
      setRequests(r)
      setStats(s as { requested: number; approved: number; archived: number } | null)
      setLoading(false)
    }).catch((e) => {
      if (aborted) return
      setError('Failed to load spawn data: ' + (e?.message || String(e)))
      setLoading(false)
    })
    return () => { aborted = true }
  }, [])

  if (loading) return <div className="text-gray-400">Loading...</div>
  if (error) return <div className="text-red-400">Error: {error}</div>

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">Spawning</h2>
        {stats && (
          <div className="flex space-x-4 text-sm">
            <span className="text-yellow-400">Pending: {stats.requested}</span>
            <span className="text-green-400">Approved: {stats.approved}</span>
            <span className="text-gray-400">Archived: {stats.archived}</span>
          </div>
        )}
      </div>

      {requests.length === 0 ? (
        <div className="bg-gray-800 rounded-lg p-8 border border-gray-700 text-center">
          <p className="text-gray-400">No spawn requests.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {requests.map((req) => (
            <div key={req.id} className="bg-gray-800 rounded-lg p-4 border border-gray-700">
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-medium text-gray-200">{escapeHtml(req.purpose || 'Untitled request')}</p>
                  <p className="text-sm text-gray-400 mt-1">
                    Class: {escapeHtml(req.class)} · Requester: {escapeHtml(req.requesterId)}
                    {req.templateId ? ` · Template: ${escapeHtml(req.templateId)}` : ''}
                  </p>
                </div>
                <span className={`text-xs px-2 py-1 rounded ${
                  req.status === 'requested' ? 'bg-yellow-900/50 text-yellow-400' :
                  req.status === 'approved' ? 'bg-green-900/50 text-green-400' :
                  req.status === 'denied' ? 'bg-red-900/50 text-red-400' :
                  'bg-gray-700 text-gray-300'
                }`}>
                  {req.status}
                </span>
              </div>
              {req.budgetCap && (
                <p className="text-xs text-gray-500 mt-1">Budget cap: ${req.budgetCap}</p>
              )}
              {req.ttlHours && (
                <p className="text-xs text-gray-500">TTL: {req.ttlHours}h</p>
              )}
            </div>
          ))}
        </div>
      )}

      <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
        <h3 className="text-lg font-semibold text-purple-300 mb-4">Gates</h3>
        <div className="text-xs text-gray-500 space-y-1">
          <p>Spend circuit-breaker · Quota caps (CFG-040/017/018)</p>
          <p>Depth limit (default 2) · Template class match</p>
        </div>
      </div>
    </div>
  )
}
