import { useEffect, useState } from 'react'
import { api } from '../services/api'
import type { DnaDecision } from '../types'
import { escapeHtml } from '../utils/escapeHtml'
import { formatDate, truncateSnippet } from '../utils/formatting'
import { ErrorBanner } from '../components/ErrorBanner'

export default function DNADecisions() {
  const [decisions, setDecisions] = useState<DnaDecision[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadDecisions = () => {
    setLoading(true)
    setError(null)
    let aborted = false
    api.dna.decisions()
      .then((data) => { if (!aborted) { setDecisions(data); setLoading(false) } })
      .catch((err) => { if (!aborted) { setError(err instanceof Error ? err.message : String(err)); setLoading(false) } })
    return () => { aborted = true }
  }

  useEffect(() => {
    const cancel = loadDecisions()
    return cancel
  }, [])

  if (loading) return <div className="text-gray-400" role="status" aria-live="polite">Loading...</div>
  if (error) return <ErrorBanner message={error} onRetry={loadDecisions} />

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">DNA Decisions</h2>
        <span className="text-sm text-gray-400">{decisions.length === 1 ? '1 decision' : `${decisions.length} decisions`}</span>
      </div>
      {decisions.length === 0 ? (
        <div className="bg-gray-800 rounded-lg p-8 border border-gray-700 text-center">
          <p className="text-gray-400">No decisions recorded yet.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {decisions.map((d) => {
            const contextPreview = escapeHtml(d.contextMd)
            const outcomePreview = escapeHtml(d.outcomeMd)
            return (
            <div key={d.id} className="bg-gray-800 rounded-lg p-4 border border-gray-700">
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-medium text-gray-200">Decision {escapeHtml(d.id.slice(0, 8))}</p>
                  <p className="text-sm text-gray-400 mt-1">Domain: {escapeHtml(d.domainId)} · By: {escapeHtml(d.decidedBy)}</p>
                </div>
              </div>
              <pre className="mt-2 text-xs text-gray-400 bg-gray-900 rounded p-3 overflow-x-auto whitespace-pre-wrap">
                {truncateSnippet(contextPreview, 150)}
              </pre>
              <pre className="mt-1 text-xs text-gray-500 bg-gray-900 rounded p-3 overflow-x-auto whitespace-pre-wrap">
                {truncateSnippet(outcomePreview, 150)}
              </pre>
              <p className="text-xs text-gray-500 mt-2">
                Decided: {formatDate(d.decidedAt, { dateOnly: true })}
              </p>
            </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
