import { useEffect, useState } from 'react'
import { api } from '../services/api'
import type { DataHold } from '../types'
import { formatDate, escapeHtml } from '../utils/formatting'
import { ErrorBanner } from '../components/ErrorBanner'
import { useEscapeKey } from '../hooks/useEscapeKey'
import { readSessionUser } from '../utils/session'
import { cancellableLoad, startLoad } from '../utils/loadState'

export default function DataHolds() {
  const [holds, setHolds] = useState<DataHold[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [actionId, setActionId] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [confirmReleaseId, setConfirmReleaseId] = useState<string | null>(null)
  // Releasing a hold is an admin act (the release door is admin-gated) — the
  // control is hidden rather than offered and then refused with a 403.
  const isAdmin = readSessionUser()?.rbac === 'admin'

  useEscapeKey(() => {
    setConfirmReleaseId(null)
  })

  const loadHolds = () => {
    setLoading(true)
    setError(null)
    let aborted = false
    const promise = api.governance.listHolds()
      .then((data) => { if (!aborted) { setHolds(Array.isArray(data) ? data : []); setLoading(false) } })
      .catch((err) => {
        if (!aborted) { setError(err instanceof Error ? err.message : String(err)); setLoading(false) }
        throw err
      })
    return cancellableLoad(promise, () => { aborted = true })
  }

  useEffect(() => startLoad(loadHolds), [])

  const handleRelease = async (id: string) => {
    setActionId(id)
    setActionError(null)
    try {
      await api.governance.releaseHold(id)
    } catch (err) {
      setActionError(err instanceof Error ? err.message : String(err))
    } finally {
      setActionId(null)
      setConfirmReleaseId(null)
    }
    // Reload in both cases — a refused release may still have changed server
    // state — and await it: the loader's failure is recorded in the page's own
    // error banner, so the list is never left silently stale.
    try { await loadHolds().promise } catch { /* the page's error banner shows it */ }
  }

  if (loading) return <div className="text-gray-400" role="status" aria-live="polite">Loading...</div>
  if (error) return <ErrorBanner message={error} onRetry={loadHolds} onDismiss={() => setError(null)} />

  return (
    <div className="space-y-6">
      {actionError && (
        <div className="rounded-lg p-3 text-sm bg-red-900/30 border border-red-700 text-red-400" role="alert">
          {actionError}
          <button type="button" onClick={() => setActionError(null)} className="ml-2 text-red-300 hover:text-white" aria-label="Dismiss error">×</button>
        </div>
      )}
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">Data Holds</h2>
        <span className="text-sm text-gray-400">{holds.length} holds</span>
      </div>
      {holds.length === 0 ? (
        <div className="bg-gray-800 rounded-lg p-8 border border-gray-700 text-center">
          <p className="text-gray-400">No active data holds.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {holds.map((hold) => (
            <div key={hold.id} className="bg-gray-800 rounded-lg p-4 border border-gray-700">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <span className="text-xs px-2 py-0.5 rounded bg-purple-900/50 text-purple-300 border border-purple-700">
                      {escapeHtml(hold.kind)}
                    </span>
                    <p className="font-medium text-gray-200">Subject: {escapeHtml(hold.subjectId)}</p>
                  </div>
                  <p className="text-sm text-gray-400 mt-1 whitespace-pre-wrap">{escapeHtml(hold.reasonMd)}</p>
                  <p className="text-xs text-gray-500 mt-1">
                    Created by {escapeHtml(hold.createdBy)} · {formatDate(hold.createdAt, { dateOnly: true })}
                  </p>
                </div>
                <div className="flex items-center gap-2 ml-4">
                  {isAdmin && (
                    <button
                      type="button"
                      onClick={() => setConfirmReleaseId(hold.id)}
                      disabled={actionId === hold.id}
                      className="px-2 py-1 bg-red-700 hover:bg-red-600 disabled:bg-gray-600 disabled:text-gray-800 rounded text-xs text-white"
                      aria-label={`Release hold ${hold.id}`}
                    >
                      {actionId === hold.id ? 'Releasing...' : 'Release'}
                    </button>
                  )}
                  {isAdmin && confirmReleaseId === hold.id && (
                    <div className="flex items-center gap-1">
                      <span className="text-xs text-yellow-400">Confirm?</span>
                      <button
                        type="button"
                        onClick={() => handleRelease(hold.id)}
                        className="px-1 py-0.5 bg-green-700 hover:bg-green-600 rounded text-xs text-white"
                        aria-label={`Confirm release hold ${hold.id}`}
                      >
                        Yes
                      </button>
                      <button
                        type="button"
                        onClick={() => setConfirmReleaseId(null)}
                        className="px-1 py-0.5 bg-gray-600 hover:bg-gray-500 rounded text-xs text-gray-300"
                        aria-label={`Cancel release hold ${hold.id}`}
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
      )}
    </div>
  )
}
