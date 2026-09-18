import { useEffect, useState } from 'react'
import { api } from '../services/api'
import type { DnaRule } from '../types'
import { escapeHtml } from '../utils/escapeHtml'
import { formatDate, dnaRuleStatusColor, truncateSnippet } from '../utils/formatting'
import { ErrorBanner } from '../components/ErrorBanner'

export default function DNARules() {
  const [rules, setRules] = useState<DnaRule[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadRules = () => {
    setLoading(true)
    setError(null)
    let aborted = false
    api.dna.rules()
      .then((data) => { if (!aborted) { setRules(data); setLoading(false) } })
      .catch((err) => { if (!aborted) { setError(err instanceof Error ? err.message : String(err)); setLoading(false) } })
    return () => { aborted = true }
  }

  useEffect(() => {
    const cancel = loadRules()
    return cancel
  }, [])

  if (loading) return <div className="text-gray-400" role="status" aria-live="polite">Loading...</div>
  if (error) return <ErrorBanner message={escapeHtml(error)} onRetry={loadRules} />

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">DNA Rules</h2>
        <span className="text-sm text-gray-400">{rules.length === 1 ? '1 rule' : `${rules.length} rules`}</span>
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
                  <p className="font-medium text-gray-200">Rule {escapeHtml(rule.id.slice(0, 8))}</p>
                  <p className="text-sm text-gray-400 mt-1">Domain: {escapeHtml(rule.domainId)}</p>
                </div>
                <span className={`text-xs px-2 py-1 rounded ${dnaRuleStatusColor(rule.status)}`} aria-label={`Status: ${rule.status}`}>
                  {escapeHtml(rule.status)}
                </span>
              </div>
              <pre className="mt-2 text-xs text-gray-400 bg-gray-900 rounded p-3 overflow-x-auto whitespace-pre-wrap">
                {truncateSnippet(escapeHtml(rule.statementMd), 200)}
              </pre>
              <p className="text-xs text-gray-500 mt-2">
                From: {rule.effectiveFrom != null ? formatDate(rule.effectiveFrom, { dateOnly: true }) : '∞'}
                {rule.effectiveTo != null ? ` — To: ${formatDate(rule.effectiveTo, { dateOnly: true })}` : ''}
              </p>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
