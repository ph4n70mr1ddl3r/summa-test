import { useEffect, useState } from 'react'
import { api, loadWithFallback } from '../services/api'
import type { SpawnRequest, SpawnStats } from '../types'
import { spawnStatusColor } from '../utils/formatting'
import { ErrorBanner } from '../components/ErrorBanner'

export default function Spawning() {
  const [requests, setRequests] = useState<SpawnRequest[]>([])
  const [stats, setStats] = useState<SpawnStats | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [actionId, setActionId] = useState<string | null>(null)

  const loadData = () => {
    setLoading(true)
    setError(null)
    let aborted = false
    loadWithFallback(
      () => Promise.all([
        api.spawn.list(),
        api.spawn.stats(),
      ]),
      () => Promise.all([
        api.spawn.list().catch(() => null),
        api.spawn.stats().catch(() => null),
      ]),
    ).then(({ data, error: loadError }) => {
      if (aborted) return
      setRequests(Array.isArray(data[0]) ? data[0] as SpawnRequest[] : [])
      setStats(data[1] != null && typeof data[1] === 'object' && !Array.isArray(data[1]) ? data[1] as SpawnStats : null)
      setError(loadError)
      setLoading(false)
    })
    return () => { aborted = true }
  }

  useEffect(() => {
    const cancel = loadData()
    return cancel
  }, [])

  const handleAction = async (id: string, action: 'approve' | 'deny') => {
    setActionId(id)
    setError(null)
    try {
      if (action === 'approve') await api.spawn.approve(id)
      else await api.spawn.deny(id)
      await loadData()
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      setActionId(null)
    }
  }

  if (loading) return <div className="text-gray-400" role="status" aria-live="polite">Loading...</div>
  if (error) return <ErrorBanner message={error} onRetry={loadData} />

  const requested = requests.filter(r => r.status === 'requested')
  const other = requests.filter(r => r.status !== 'requested')

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">Spawning</h2>
        {stats && (
          <div className="flex space-x-4 text-sm">
            <span className="text-yellow-400">Requested: {stats.requested}</span>
            <span className="text-green-400">Approved: {stats.approved}</span>
            <span className="text-gray-400">Archived: {stats.archived}</span>
          </div>
        )}
      </div>

      {error && (
        <ErrorBanner message={error} onRetry={() => setError(null)} />
      )}

      {requested.length > 0 && (
        <div>
          <h3 className="text-lg font-semibold text-yellow-300 mb-3">Pending Requests ({requested.length})</h3>
          <div className="space-y-3">
            {requested.map((req) => (
              <div key={req.id} className="bg-gray-800 rounded-lg p-4 border border-yellow-700/50">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <p className="font-medium text-gray-200">{req.purpose || 'Untitled request'}</p>
                    <p className="text-sm text-gray-400 mt-1">
                      Class: {req.class} · Requester: {req.requesterId}
                      {req.templateId ? ` · Template: ${req.templateId}` : ''}
                    </p>
                    {req.budgetCap && (
                      <p className="text-xs text-gray-500 mt-1">Budget cap: ${req.budgetCap}</p>
                    )}
                    {req.ttlHours && (
                      <p className="text-xs text-gray-500">TTL: {req.ttlHours}h</p>
                    )}
                  </div>
                  <div className="flex items-center gap-2 ml-4">
                    <span className={`text-xs px-2 py-1 rounded ${spawnStatusColor(req.status)}`} aria-label={`Status: ${req.status}`}>
                      {req.status}
                    </span>
                    <button
                      type="button"
                      onClick={() => handleAction(req.id, 'approve')}
                      disabled={actionId !== req.id}
                      className="px-2 py-1 bg-green-700 hover:bg-green-600 disabled:bg-gray-600 rounded text-xs text-white"
                      aria-label={`Approve ${req.purpose || 'request'}`}
                    >
                      Approve
                    </button>
                    <button
                      type="button"
                      onClick={() => handleAction(req.id, 'deny')}
                      disabled={actionId !== req.id}
                      className="px-2 py-1 bg-red-700 hover:bg-red-600 disabled:bg-gray-600 rounded text-xs text-white"
                      aria-label={`Deny ${req.purpose || 'request'}`}
                    >
                      Deny
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {other.length > 0 && (
        <div>
          <h3 className="text-lg font-semibold text-gray-300 mb-3">Other Requests ({other.length})</h3>
          <div className="space-y-3">
            {other.map((req) => (
              <div key={req.id} className="bg-gray-800 rounded-lg p-4 border border-gray-700">
                <div className="flex items-start justify-between">
                  <div>
                    <p className="font-medium text-gray-200">{req.purpose || 'Untitled request'}</p>
                    <p className="text-sm text-gray-400 mt-1">
                      Class: {req.class} · Requester: {req.requesterId}
                      {req.templateId ? ` · Template: ${req.templateId}` : ''}
                    </p>
                  </div>
                  <span className={`text-xs px-2 py-1 rounded ${spawnStatusColor(req.status)}`} aria-label={`Status: ${req.status}`}>
                    {req.status}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {requested.length === 0 && other.length === 0 && (
        <div className="bg-gray-800 rounded-lg p-8 border border-gray-700 text-center">
          <p className="text-gray-400">No spawn requests.</p>
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
