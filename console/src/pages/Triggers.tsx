import { useEffect, useState } from 'react'
import { api } from '../services/api'
import type { Trigger } from '../types'
import { triggerStatusColor, triggerCriticalityColor } from '../utils/formatting'
import { ErrorBanner } from '../components/ErrorBanner'

export default function Triggers() {
  const [triggers, setTriggers] = useState<Trigger[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadTriggers = () => {
    setLoading(true)
    setError(null)
    let aborted = false
    api.triggers.list()
      .then((data) => { if (!aborted) { setTriggers(data); setLoading(false) } })
      .catch((err) => { if (!aborted) { setError(err instanceof Error ? err.message : String(err)); setLoading(false) } })
    return () => { aborted = true }
  }

  useEffect(() => {
    const cancel = loadTriggers()
    return cancel
  }, [])

  if (loading) return <div className="text-gray-400" role="status" aria-live="polite">Loading...</div>
  if (error) return <ErrorBanner message={error} onRetry={loadTriggers} />

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">Triggers</h2>
        <span className="text-sm text-gray-400">{triggers.length} triggers</span>
      </div>
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
                  <p className="font-medium text-gray-200">{t.name}</p>
                  <p className="text-sm text-gray-400 mt-1">Kind: {t.kind} | Agent: {t.agentId}</p>
                  {t.expression && (
                    <p className="text-xs text-gray-500 mt-1">Expression: {t.expression}</p>
                  )}
                  {t.config && (
                    <p className="text-xs text-gray-500 mt-1">Config: {t.config}</p>
                  )}
                </div>
                <span className={`text-xs px-2 py-1 rounded ${triggerStatusColor(t.status)}`} aria-label={`Status: ${t.status}`}>{t.status}</span>
                <span className={`ml-2 text-xs px-2 py-1 rounded border ${triggerCriticalityColor(t.criticality)}`} aria-label={`Criticality: ${t.criticality}`}>
                  {t.criticality}
                </span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
