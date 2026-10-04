import { useEffect, useState } from 'react'
import { api, loadWithFallback } from '../services/api'
import type { Initiative } from '../types'
import { formatDate, initiativeStatusColor, escapeHtml } from '../utils/formatting'
import { ErrorBanner } from '../components/ErrorBanner'

export default function Initiatives() {
  const [initiatives, setInitiatives] = useState<Initiative[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadData = () => {
    setLoading(true)
    setError(null)
    let aborted = false
    loadWithFallback(
      () => api.initiatives.list().then(d => Array.isArray(d) ? d : []),
      () => api.initiatives.list().then(d => Array.isArray(d) ? d : []).catch(() => null),
    ).then(({ data, error: loadError }) => {
      if (aborted) return
      const result = Array.isArray(data) ? data : []
      setInitiatives(result as Initiative[])
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
        <h2 className="text-2xl font-bold">Initiatives</h2>
        <span className="text-sm text-gray-400">{initiatives.length} initiatives</span>
      </div>
      {initiatives.length === 0 ? (
        <div className="bg-gray-800 rounded-lg p-8 border border-gray-700 text-center">
          <p className="text-gray-400">No initiatives yet.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {initiatives.map((ini) => (
            <div key={ini.id} className="bg-gray-800 rounded-lg p-4 border border-gray-700">
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-medium text-gray-200">{escapeHtml(ini.title)}</p>
                  <p className="text-sm text-gray-400 mt-1">
                    Sponsor: {escapeHtml(ini.sponsor)} | Lead: {escapeHtml(ini.lead)}
                    {ini.goalRef && <span className="ml-2">Goal: {escapeHtml(ini.goalRef)}</span>}
                    {ini.deadline != null && (
                      <span className="ml-2 text-xs text-gray-500">
                        Deadline: {formatDate(ini.deadline, { dateOnly: true })}
                      </span>
                    )}
                  </p>
                </div>
                <span className={`text-xs px-2 py-0.5 rounded ${initiativeStatusColor(ini.status)}`} aria-label={`Status: ${ini.status}`}>
                  {ini.status}
                </span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
