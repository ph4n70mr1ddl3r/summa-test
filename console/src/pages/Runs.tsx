import { useEffect, useState } from 'react'
import { api } from '../services/api'
import type { Run } from '../types'
import { escapeHtml } from '../utils/escapeHtml'
import { runStatusColor, formatDate } from '../utils/formatting'
import { ErrorBanner } from '../components/ErrorBanner'

export default function Runs() {
  const [runs, setRuns] = useState<Run[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [filter, setFilter] = useState<string>('all')

  useEffect(() => {
    let aborted = false
    const params: Record<string, string | number> = {}
    if (filter !== 'all') params.status = filter
    api.runs.list(params)
      .then((data) => { if (!aborted) { setRuns(data); setLoading(false) } })
      .catch((err) => { if (!aborted) { setError(err instanceof Error ? err.message : String(err)); setLoading(false) } })
    return () => { aborted = true }
  }, [filter])

  if (loading) return <div className="text-gray-400">Loading...</div>
  if (error) return <ErrorBanner message={escapeHtml(error)} onRetry={() => { setLoading(true); setError(null) }} />

  const statusCounts: Record<string, number> = {}
  runs.forEach(r => { statusCounts[r.status] = (statusCounts[r.status] || 0) + 1 })

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">Runs</h2>
        <div className="flex space-x-2 text-sm">
          <button
            onClick={() => setFilter('all')}
            className={`px-3 py-1 rounded ${filter === 'all' ? 'bg-blue-600 text-white' : 'bg-gray-700 text-gray-400 hover:text-white'}`}
          >
            All ({runs.length})
          </button>
          {(['queued', 'running', 'suspended', 'completed', 'failed', 'cancelled'] as const).map(s => (
            statusCounts[s] > 0 && (
              <button
                key={s}
                onClick={() => setFilter(s)}
                className={`px-3 py-1 rounded ${filter === s ? 'bg-blue-600 text-white' : 'bg-gray-700 text-gray-400 hover:text-white'}`}
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
                  <p className="font-medium text-gray-200">Run {escapeHtml(run.id.slice(0, 8))}</p>
                  <p className="text-sm text-gray-400 mt-1">
                    Agent: {escapeHtml(run.agentId)}
                    {run.workspaceId ? ` · Workspace: ${escapeHtml(run.workspaceId)}` : ''}
                    {run.initiativeId ? ` · Initiative: ${escapeHtml(run.initiativeId)}` : ''}
                  </p>
                  {run.prompt && (
                    <p className="text-xs text-gray-500 mt-1 line-clamp-2">{escapeHtml(run.prompt)}</p>
                  )}
                </div>
                <span className={`text-xs px-2 py-1 rounded ${runStatusColor(run.status)}`} aria-label={`Status: ${run.status}`}>
                  {escapeHtml(run.status)}
                </span>
              </div>
              <div className="flex items-center space-x-4 mt-2 text-xs text-gray-500">
                {run.costTokens != null && <span>Tokens: {run.costTokens}</span>}
                {run.costUsd != null && <span>Cost: ${run.costUsd}</span>}
                {run.startedAt && <span>Started: {formatDate(run.startedAt)}</span>}
                {run.completedAt && <span>Completed: {formatDate(run.completedAt)}</span>}
              </div>
              {run.errorMessage && (
                <p className="text-xs text-red-400 mt-1">Error: {escapeHtml(run.errorMessage)}</p>
              )}
              {run.result && (
                <p className="text-xs text-gray-500 mt-1 line-clamp-2">{escapeHtml(run.result)}</p>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
