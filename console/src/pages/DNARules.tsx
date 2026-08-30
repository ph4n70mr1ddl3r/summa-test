import { useEffect, useState } from 'react'
import { api, type DnaRule } from '../services/api'
import { escapeHtml } from '../utils/escapeHtml'

export default function DNARules() {
  const [rules, setRules] = useState<DnaRule[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.dna.rules()
      .then((data) => { setRules(data); setLoading(false) })
      .catch((err) => { setError(err instanceof Error ? err.message : String(err)); setLoading(false) })
  }, [])

  if (loading) return <div className="text-gray-400">Loading...</div>
  if (error) return <div className="text-red-400">Error: {error}</div>

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">DNA Rules</h2>
        <span className="text-sm text-gray-400">{rules.length} rules</span>
      </div>
      {rules.length === 0 ? (
        <div className="bg-gray-800 rounded-lg p-8 border border-gray-700 text-center">
          <p className="text-gray-400">No rules configured yet.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {rules.map((rule) => (
            <div key={rule.id} className="bg-gray-800 rounded-lg p-4 border border-gray-700">
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-medium text-gray-200">Rule {rule.id.slice(0, 8)}</p>
                  <p className="text-sm text-gray-400 mt-1">Domain: {rule.domainId}</p>
                </div>
                <span className={`text-xs px-2 py-1 rounded ${
                  rule.status === 'active' ? 'bg-green-900/50 text-green-400' :
                  rule.status === 'superseded' ? 'bg-gray-700 text-gray-300' :
                  'bg-yellow-900/50 text-yellow-400'
                }`}>
                  {rule.status}
                </span>
              </div>
              <pre className="mt-2 text-xs text-gray-400 bg-gray-900 rounded p-3 overflow-x-auto whitespace-pre-wrap">
                {escapeHtml(rule.statementMd).slice(0, 200)}{escapeHtml(rule.statementMd).length > 200 ? '...' : ''}
              </pre>
              <p className="text-xs text-gray-500 mt-2">
                From: {rule.effectiveFrom ? new Date(rule.effectiveFrom * 1000).toLocaleDateString() : '∞'}
                {rule.effectiveTo ? ` — To: ${new Date(rule.effectiveTo * 1000).toLocaleDateString()}` : ''}
              </p>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
