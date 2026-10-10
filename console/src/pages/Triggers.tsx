import { useEffect, useState } from 'react'
import { api } from '../services/api'
import type { Trigger } from '../types'
import { triggerStatusColor, triggerCriticalityColor, escapeHtml, formatDate } from '../utils/formatting'
import { ErrorBanner } from '../components/ErrorBanner'

export default function Triggers() {
  const [triggers, setTriggers] = useState<Trigger[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const loadTriggers = () => {
    setLoading(true)
    setError(null)
    setActionError(null)
    let aborted = false
    api.triggers.list()
      .then((data) => { if (!aborted) { setTriggers(Array.isArray(data) ? data : []); setLoading(false) } })
      .catch((err) => { if (!aborted) { setError(err instanceof Error ? err.message : String(err)); setLoading(false) } })
    return () => { aborted = true }
  }

  useEffect(() => {
    const cancel = loadTriggers()
    return cancel
  }, [])

  const handleLifecycle = async (id: string, action: 'pause' | 'resume' | 'archive') => {
    setActionError(null)
    try {
      if (action === 'pause') await api.triggers.pause(id)
      else if (action === 'resume') await api.triggers.resume(id)
      else await api.triggers.archive(id)
      await loadTriggers()
    } catch (err) {
      setActionError(err instanceof Error ? err.message : String(err))
    }
  }

  if (loading) return <div className="text-gray-400" role="status" aria-live="polite">Loading...</div>
  if (error) return <ErrorBanner message={error} onRetry={loadTriggers} />

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">Triggers</h2>
        <span className="text-sm text-gray-400">{triggers.length} triggers</span>
      </div>
      {actionError && (
        <div className="rounded-lg p-3 text-sm bg-red-900/30 border border-red-700 text-red-400" role="alert">
          {actionError}
          <button type="button" onClick={() => setActionError(null)} className="ml-2 text-red-300 hover:text-white" aria-label="Dismiss error">×</button>
        </div>
      )}
      {triggers.length === 0 ? (
        <div className="bg-gray-800 rounded-lg p-8 border border-gray-700 text-center">
          <p className="text-gray-400">No triggers configured.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {triggers.map((t) => (
            <div key={t.id} className="bg-gray-800 rounded-lg p-4 border border-gray-700">
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-medium text-gray-200">{escapeHtml(t.name)}</p>
                  <p className="text-sm text-gray-400 mt-1">Kind: {t.kind} | Agent: {escapeHtml(t.agentId)}</p>
                  {t.expression && (
                    <p className="text-xs text-gray-500 mt-1">Expression: {escapeHtml(t.expression)}</p>
                  )}
                  {t.config && (
                    <p className="text-xs text-gray-500 mt-1">Config: {escapeHtml(t.config)}</p>
                  )}
                  {t.lastFiredAt != null && (
                    <p className="text-xs text-gray-500">Last fired: {formatDate(t.lastFiredAt)}</p>
                  )}
                </div>
                <div className="flex items-center gap-2">
                  <span className={`text-xs px-2 py-1 rounded ${triggerStatusColor(t.status)}`} aria-label={`Status: ${t.status}`}>{t.status}</span>
                  <span className={`text-xs px-2 py-1 rounded border ${triggerCriticalityColor(t.criticality)}`} aria-label={`Criticality: ${t.criticality}`}>
                    {t.criticality}
                  </span>
                  {t.status === 'active' && (
                    <button
                      type="button"
                      onClick={() => handleLifecycle(t.id, 'pause')}
                      className="ml-2 px-2 py-1 text-xs bg-yellow-700 hover:bg-yellow-600 rounded text-yellow-100"
                      aria-label={`Pause trigger ${t.name}`}
                    >
                      Pause
                    </button>
                  )}
                  {t.status === 'paused' && (
                    <button
                      type="button"
                      onClick={() => handleLifecycle(t.id, 'resume')}
                      className="ml-2 px-2 py-1 text-xs bg-green-700 hover:bg-green-600 rounded text-green-100"
                      aria-label={`Resume trigger ${t.name}`}
                    >
                      Resume
                    </button>
                  )}
                  {t.status !== 'archived' && (
                    <button
                      type="button"
                      onClick={() => handleLifecycle(t.id, 'archive')}
                      className="ml-2 px-2 py-1 text-xs bg-gray-700 hover:bg-gray-600 rounded text-gray-300"
                      aria-label={`Archive trigger ${t.name}`}
                    >
                      Archive
                    </button>
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
