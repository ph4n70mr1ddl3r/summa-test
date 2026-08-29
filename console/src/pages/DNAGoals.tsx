import { useEffect, useState } from 'react'
import { api, type DnaGoal } from '../services/api'

export default function DNAGoals() {
  const [goals, setGoals] = useState<DnaGoal[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.dna.goals()
      .then((data) => { setGoals(data); setLoading(false) })
      .catch((err) => { setError(err instanceof Error ? err.message : String(err)); setLoading(false) })
  }, [])

  if (loading) return <div className="text-gray-400">Loading...</div>
  if (error) return <div className="text-red-400">Error: {error}</div>

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
                  <p className="font-medium text-gray-200">{goal.statementMd}</p>
                  <p className="text-sm text-gray-400 mt-1">
                    Owner: {goal.owner} | Inject: {goal.inject}
                    {goal.quarter && <span> | Q{goal.quarter}</span>}
                  </p>
                  {goal.effectiveFrom && (
                    <p className="text-xs text-gray-500 mt-1">
                      Effective: {new Date(goal.effectiveFrom * 1000).toLocaleDateString()}
                      {goal.effectiveTo && ` — ${new Date(goal.effectiveTo * 1000).toLocaleDateString()}`}
                    </p>
                  )}
                </div>
                <span className="text-xs px-2 py-1 rounded bg-gray-700 text-gray-300">{goal.status}</span>
              </div>
            </div>
          ))}
        </div>
      )}
      <div className="bg-gray-800 rounded-lg p-4 border border-gray-700">
        <h3 className="text-sm font-semibold text-gray-300 mb-2">API</h3>
        <code className="text-xs text-gray-400">
          GET /api/dna/goals{'\n'}
          PATCH /api/dna/goals/:id/status{'\n'}
          PATCH /api/dna/goals/:id/window
        </code>
      </div>
    </div>
  )
}
