import { useEffect, useState } from 'react'
import { api, type Initiative } from '../services/api'
import { escapeHtml } from '../utils/escapeHtml'

export default function Initiatives() {
  const [initiatives, setInitiatives] = useState<Initiative[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let aborted = false
    api.initiatives.list()
      .then((data) => { if (!aborted) { setInitiatives(data); setLoading(false) } })
      .catch((err) => { if (!aborted) { setError(err instanceof Error ? err.message : String(err)); setLoading(false) } })
    return () => { aborted = true }
  }, [])

  if (loading) return <div className="text-gray-400">Loading...</div>
  if (error) return <div className="text-red-400">Error: {error}</div>

  const statusColor = (status: string) => {
    switch (status) {
      case 'proposed': return 'bg-blue-900/50 text-blue-400'
      case 'active': return 'bg-green-900/50 text-green-400'
      case 'paused': return 'bg-yellow-900/50 text-yellow-400'
      case 'closed': return 'bg-gray-600 text-gray-400'
      default: return 'bg-gray-700 text-gray-300'
    }
  }

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
                    {ini.deadline && (
                      <span className="ml-2 text-xs text-gray-500">
                        Deadline: {new Date(ini.deadline * 1000).toLocaleDateString()}
                      </span>
                    )}
                  </p>
                </div>
                <span className={`text-xs px-2 py-0.5 rounded ${statusColor(ini.status)}`}>
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
