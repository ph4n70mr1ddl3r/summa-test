import { useEffect, useState, useMemo } from 'react'
import { api } from '../services/api'
import type { Run } from '../types'
import { runStatusColor, formatDate } from '../utils/formatting'
import { ErrorBanner } from '../components/ErrorBanner'

export default function Runs() {
  const [runs, setRuns] = useState<Run[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [filter, setFilter] = useState<Run['status'] | 'all'>('all')

  const loadRuns = () => {
    setLoading(true)
    setError(null)
    let aborted = false
    const params: Record<string, string | number> = {}
    if (filter !== 'all') params.status = filter
    api.runs.list(params)
      .then((data) => { if (!aborted) { setRuns(data); setLoading(false) } })
      .catch((err) => { if (!aborted) { setError(err instanceof Error ? err.message : String(err)); setLoading(false) } })
    return () => { aborted = true }
  }

  useEffect(() => {
    const cancel = loadRuns()
    return cancel
  }, [filter])

  const statusCounts = useMemo(() => {
    const counts: Record<string, number> = {}
    runs.forEach(r => { counts[r.status] = (counts[r.status] || 0) + 1 })
    return counts
  }, [runs])

  if (loading) return <div className="text-gray-400" role="status" aria-live="polite">Loading...</div>
  if (error) return <ErrorBanner message={error} onRetry={loadRuns} />

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">Runs</h2>
        <div className="flex space-x-2 text-sm">
          <button
            onClick={() => setFilter('all')}
            className={`px-3 py-1 rounded ${filter === 'all' ? 'bg-blue-600 text-white' : 'bg-gray-700 text-gray-400 hover:text-white'}`}
            aria-pressed={filter === 'all'}
          >
            All ({runs.length})
          </button>
          {(['queued', 'running', 'suspended', 'completed', 'failed', 'cancelled'] as const).map(s => (
            statusCounts[s] > 0 && (
              <button
                key={s}
                onClick={() => setFilter(s)}
                className={`px-3 py-1 rounded ${filter === s ? 'bg-blue-600 text-white' : 'bg-gray-700 text-gray-400 hover:text-white'}`}
                aria-pressed={filter === s}
              >
                {s} ({statusCounts[s]})
              </button>
            )
          ))}
        </div>
      </div>

      {runs.length === 0 ? (
        <div className="bg-gray-800 rounded-lg p-8 border border-gray-700 text-center">
          <p className="text-gray-400">No runs found.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {runs.map((run) => (
            <div key={run.id} className="bg-gray-800 rounded-lg p-4 border border-gray-700">
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-medium text-gray-200">Run {run.id.slice(0, 8)}</p>
                  <p className="text-sm text-gray-400 mt-1">
                    Agent: {run.agentId}
                    {run.workspaceId ? ` · Workspace: ${run.workspaceId}` : ''}
                    {run.initiativeId ? ` · Initiative: ${run.initiativeId}` : ''}
                  </p>
                  {run.prompt && (
                    <p className="text-xs text-gray-500 mt-1 line-clamp-2">{run.prompt}</p>
                  )}
                </div>
                <span className={`text-xs px-2 py-1 rounded ${runStatusColor(run.status)}`} aria-label={`Status: ${run.status}`}>
                  {run.status}
                </span>
              </div>
              <div className="flex items-center space-x-4 mt-2 text-xs text-gray-500">
                {run.costTokens != null && <span>Tokens: {run.costTokens}</span>}
                {run.costUsd != null && Number.isFinite(run.costUsd) && <span>Cost: ${run.costUsd.toFixed(2)}</span>}
                {run.startedAt && <span>Started: {formatDate(run.startedAt, { dateOnly: true })}</span>}
                {run.completedAt && <span>Completed: {formatDate(run.completedAt, { dateOnly: true })}</span>}
              </div>
              {run.errorMessage && (
                <p className="text-xs text-red-400 mt-1">Error: {run.errorMessage}</p>
              )}
              {run.result && (
                <p className="text-xs text-gray-500 mt-1 line-clamp-2">{run.result}</p>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
