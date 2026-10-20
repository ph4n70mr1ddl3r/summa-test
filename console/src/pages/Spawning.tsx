import { useEffect, useState } from 'react'
import { api, loadWithFallback } from '../services/api'
import type { SpawnRequest, SpawnStats } from '../types'
import { spawnStatusColor, escapeHtml, formatDate } from '../utils/formatting'
import { ErrorBanner } from '../components/ErrorBanner'
import { cancellableLoad, hasAnyData, startLoad } from '../utils/loadState'
import { useEscapeKey } from '../hooks/useEscapeKey'

/** The same request can arrive from more than one status fetch — keep one row per id. */
function dedupeRequests(requests: SpawnRequest[]): SpawnRequest[] {
  const seen = new Set<string>()
  return requests.filter((request) => {
    if (seen.has(request.id)) return false
    seen.add(request.id)
    return true
  })
}

export default function Spawning() {
  const [requests, setRequests] = useState<SpawnRequest[]>([])
  const [stats, setStats] = useState<SpawnStats | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [hasData, setHasData] = useState(false)
  const [actionId, setActionId] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [confirmAction, setConfirmAction] = useState<Record<string, 'approve' | 'deny'>>({})

  useEscapeKey(() => {
    setConfirmAction({})
    setActionId(null)
  })

  const loadData = () => {
    setLoading(true)
    setError(null)
    let aborted = false
    // The backend's list without a status filter is the "requested" inbox only —
    // the other lifecycle states (approved, halted, archived) are fetched by
    // status, so the "other requests" panel is a live list and not a dead one.
    const fetchAllRequests = () => Promise.all([
      api.spawn.list('requested'),
      api.spawn.list('approved'),
      api.spawn.list('halted'),
      api.spawn.list('archived'),
    ]).then((lists) => dedupeRequests(lists.flatMap((list) => list ?? [])))
    const fetchAllRequestsLenient = () => Promise.all([
      api.spawn.list('requested').catch(() => null),
      api.spawn.list('approved').catch(() => null),
      api.spawn.list('halted').catch(() => null),
      api.spawn.list('archived').catch(() => null),
    ]).then((lists) => lists.some((list) => list === null)
      ? null
      : dedupeRequests(lists.flatMap((list) => list ?? [])))
    const promise = loadWithFallback(
      () => Promise.all([
        fetchAllRequests(),
        api.spawn.stats(),
      ]),
      () => Promise.all([
        fetchAllRequestsLenient(),
        api.spawn.stats().catch(() => null),
      ]),
    ).then(({ data, error: loadError }) => {
      if (aborted) { setLoading(false); return }
      setRequests(Array.isArray(data[0]) ? data[0] : [])
      setStats(data[1] != null && typeof data[1] === 'object' && !Array.isArray(data[1])
        ? data[1] : null)
      // A partial outage must not blank the page — the panels that did load
      // stay on screen with the banner above them.
      setHasData(hasAnyData(data))
      setError(loadError)
      setLoading(false)
    })
    return cancellableLoad(promise, () => { aborted = true })
  }

  useEffect(() => startLoad(loadData), [])

  const handleAction = async (id: string, action: 'approve' | 'deny') => {
    setActionId(id)
    setActionError(null)
    try {
      if (action === 'approve') await api.spawn.approve(id)
      else await api.spawn.deny(id)
    } catch (err) {
      setActionError(err instanceof Error ? err.message : String(err))
    } finally {
      setActionId(null)
      setConfirmAction(prev => { const next = { ...prev }; delete next[id]; return next })
    }
    // Reload in both cases — a refused action may still have changed server
    // state — and await it: the loader's failure is recorded in the page's own
    // error banner, so the rejection is only swallowed here.
    try { await loadData().promise } catch { /* the page's error banner shows it */ }
  }

  if (loading) return <div className="text-gray-400" role="status" aria-live="polite">Loading...</div>
  if (error && !hasData) return <ErrorBanner message={error} onRetry={loadData} onDismiss={() => setError(null)} />

  const requested = requests.filter(r => r.status === 'requested')
  const other = requests.filter(r => r.status !== 'requested')

  return (
    <div className="space-y-6">
      {error && (
        <ErrorBanner message={error} onRetry={loadData} onDismiss={() => setError(null)} />
      )}
      {actionError && (
        <div className="rounded-lg p-3 text-sm bg-red-900/30 border border-red-700 text-red-400" role="alert">
          {actionError}
          <button type="button" onClick={() => setActionError(null)} className="ml-2 text-red-300 hover:text-white" aria-label="Dismiss error">×</button>
        </div>
      )}
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">Spawning</h2>
        {stats && (
          <div className="flex space-x-4 text-sm">
            <span className="text-yellow-400">Requested: {stats.requested}</span>
            <span className="text-green-400">Approved: {stats.approved}</span>
            <span className="text-orange-400">Halted: {stats.halted}</span>
            <span className="text-gray-400">Archived: {stats.archived}</span>
          </div>
        )}
      </div>

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
                      Class: {escapeHtml(req.class)} · Requester: {escapeHtml(req.requesterId)}
                      {req.templateId ? ` · Template: ${escapeHtml(req.templateId)}` : ''}
                    </p>
                    {req.budgetCap != null && (
                      <p className="text-xs text-gray-500 mt-1">Budget cap: ${req.budgetCap}</p>
                    )}
                    {req.ttlHours != null && (
                      <p className="text-xs text-gray-500">TTL: {req.ttlHours}h</p>
                    )}
                    {req.createdAt != null && (
                      <p className="text-xs text-gray-500">Created: {formatDate(req.createdAt)}</p>
                    )}
                  </div>
                  <div className="flex items-center gap-2 ml-4">
                    <span className={`text-xs px-2 py-1 rounded ${spawnStatusColor(req.status)}`} aria-label={`Status: ${req.status}`}>
                      {req.status}
                    </span>
                    <button
                      type="button"
                      onClick={() => setConfirmAction(prev => ({ ...prev, [req.id]: 'approve' }))}
                      disabled={actionId === req.id}
                      className="px-2 py-1 bg-green-700 hover:bg-green-600 disabled:bg-gray-600 rounded text-xs text-white"
                      aria-label={`Approve ${req.purpose || 'request'}`}
                    >
                      Approve
                    </button>
                    <button
                      type="button"
                      onClick={() => setConfirmAction(prev => ({ ...prev, [req.id]: 'deny' }))}
                      disabled={actionId === req.id}
                      className="px-2 py-1 bg-red-700 hover:bg-red-600 disabled:bg-gray-600 rounded text-xs text-white"
                      aria-label={`Deny ${req.purpose || 'request'}`}
                    >
                      Deny
                    </button>
                    {confirmAction[req.id] && (
                      <div className="flex items-center gap-1 ml-2">
                        <span className="text-xs text-yellow-400">Confirm?</span>
                        <button
                          type="button"
                          onClick={() => handleAction(req.id, confirmAction[req.id])}
                          className="px-1 py-0.5 bg-green-700 hover:bg-green-600 rounded text-xs text-white"
                          aria-label={`Confirm ${confirmAction[req.id]} ${req.purpose || 'request'}`}
                        >
                          Yes
                        </button>
                        <button
                          type="button"
                          onClick={() => setConfirmAction(prev => { const next = { ...prev }; delete next[req.id]; return next })}
                          className="px-1 py-0.5 bg-gray-600 hover:bg-gray-500 rounded text-xs text-gray-300"
                          aria-label="Cancel action"
                        >
                          No
                        </button>
                      </div>
                    )}
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
                      Class: {escapeHtml(req.class)} · Requester: {escapeHtml(req.requesterId)}
                      {req.templateId ? ` · Template: ${escapeHtml(req.templateId)}` : ''}
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
