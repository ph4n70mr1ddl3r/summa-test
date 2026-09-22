import { useEffect, useState } from 'react'
import { api } from '../services/api'
import type { DnaGoal } from '../types'
import { escapeHtml } from '../utils/escapeHtml'
import { formatDate, dnaGoalStatusColor } from '../utils/formatting'
import { ErrorBanner } from '../components/ErrorBanner'

export default function DNAGoals() {
  const [goals, setGoals] = useState<DnaGoal[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadGoals = () => {
    setLoading(true)
    setError(null)
    let aborted = false
    api.dna.goals()
      .then((data) => { if (!aborted) { setGoals(data); setLoading(false) } })
      .catch((err) => { if (!aborted) { setError(err instanceof Error ? err.message : String(err)); setLoading(false) } })
    return () => { aborted = true }
  }

  useEffect(() => {
    const cancel = loadGoals()
    return cancel
  }, [])

  if (loading) return <div className="text-gray-400" role="status" aria-live="polite">Loading...</div>
  if (error) return <ErrorBanner message={error} onRetry={loadGoals} />

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">DNA Goals</h2>
        <span className="text-sm text-gray-400">{goals.length} goals</span>
      </div>
      {goals.length === 0 ? (
        <div className="bg-gray-800 rounded-lg p-8 border border-gray-700 text-center">
          <p className="text-gray-400">No DNA goals.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {goals.map((goal) => (
            <div key={goal.id} className="bg-gray-800 rounded-lg p-4 border border-gray-700">
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-medium text-gray-200">{escapeHtml(goal.statementMd)}</p>
                  <p className="text-sm text-gray-400 mt-1">
                    Owner: {escapeHtml(goal.owner)} | Inject: {escapeHtml(goal.inject)}
                    {goal.quarter && <span> | Q{escapeHtml(goal.quarter)}</span>}
                  </p>
                  {goal.effectiveFrom != null && (
                    <p className="text-xs text-gray-500 mt-1">
                      Effective: {formatDate(goal.effectiveFrom, { dateOnly: true })}
                      {goal.effectiveTo != null && ` — ${formatDate(goal.effectiveTo, { dateOnly: true })}`}
                    </p>
                  )}
                </div>
                <span className={`text-xs px-2 py-1 rounded ${dnaGoalStatusColor(goal.status)}`} aria-label={`Status: ${goal.status}`}>{escapeHtml(goal.status)}</span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
