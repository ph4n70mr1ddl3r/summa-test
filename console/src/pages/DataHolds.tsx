import { useEffect, useState } from 'react'
import { api } from '../services/api'
import type { DataHold } from '../types'
import { formatDate, escapeHtml } from '../utils/formatting'
import { ErrorBanner } from '../components/ErrorBanner'
import { useEscapeKey } from '../hooks/useEscapeKey'

export default function DataHolds() {
  const [holds, setHolds] = useState<DataHold[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [actionId, setActionId] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [confirmReleaseId, setConfirmReleaseId] = useState<string | null>(null)

  useEscapeKey(() => {
    setConfirmReleaseId(null)
  })

  const loadHolds = () => {
    setLoading(true)
    setError(null)
    let aborted = false
    api.governance.listHolds()
      .then((data) => { if (!aborted) { setHolds(Array.isArray(data) ? data : []); setLoading(false) } })
      .catch((err) => { if (!aborted) { setError(err instanceof Error ? err.message : String(err)); setLoading(false) } })
    return () => { aborted = true }
  }

  useEffect(() => {
    const cancel = loadHolds()
    return cancel
  }, [])

  const handleRelease = async (id: string) => {
    setActionId(id)
    setActionError(null)
    try {
      await api.governance.releaseHold(id)
      await loadHolds()
    } catch (err) {
      setActionError(err instanceof Error ? err.message : String(err))
      try { await loadHolds(); setError(null) } catch { /* keep original action error */ }
    } finally {
      setActionId(null)
      setConfirmReleaseId(null)
    }
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
        <span className="text-sm text-gray-400">{holds.length === 1 ? '1 hold' : `${holds.length} holds`}</span>
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
                  <button
                    type="button"
                    onClick={() => setConfirmReleaseId(hold.id)}
                    disabled={actionId === hold.id}
                    className="px-2 py-1 bg-red-700 hover:bg-red-600 disabled:bg-gray-600 disabled:text-gray-800 rounded text-xs text-white"
                    aria-label={`Release hold ${hold.id}`}
                  >
                    {actionId === hold.id ? '…' : 'Release'}
                  </button>
                  {confirmReleaseId === hold.id && (
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
