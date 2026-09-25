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
      setStats(data[1] as SpawnStats | null)
      setError(loadError)
      setLoading(false)
    })
    return () => { aborted = true }
  }

  useEffect(() => {
    const cancel = loadData()
    return cancel
  }, [])

  if (loading) return <div className="text-gray-400" role="status" aria-live="polite">Loading...</div>
  if (error) return <ErrorBanner message={error} onRetry={loadData} />

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">Spawning</h2>
        {stats && (
          <div className="flex space-x-4 text-sm">
            <span className="text-yellow-400">Requested: {stats.requested}</span>
            <span className="text-green-400">Approved: {stats.approved}</span>
            {stats.halted !== undefined && stats.halted > 0 && (
              <span className="text-orange-400">Halted: {stats.halted}</span>
            )}
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
