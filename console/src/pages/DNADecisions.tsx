import { useEffect, useState } from 'react'
import { api, type DnaDecision } from '../services/api'
import { escapeHtml } from '../utils/escapeHtml'

export default function DNADecisions() {
  const [decisions, setDecisions] = useState<DnaDecision[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let aborted = false
    api.dna.decisions()
      .then((data) => { if (!aborted) { setDecisions(data); setLoading(false) } })
      .catch((err) => { if (!aborted) { setError(err instanceof Error ? err.message : String(err)); setLoading(false) } })
    return () => { aborted = true }
  }, [])

  if (loading) return <div className="text-gray-400">Loading...</div>
  if (error) return <div className="text-red-400">Error: {error}</div>

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
          {decisions.map((d) => (
            <div key={d.id} className="bg-gray-800 rounded-lg p-4 border border-gray-700">
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-medium text-gray-200">Decision {d.id.slice(0, 8)}</p>
                  <p className="text-sm text-gray-400 mt-1">Domain: {escapeHtml(d.domainId)} · By: {escapeHtml(d.decidedBy)}</p>
                </div>
              </div>
              <pre className="mt-2 text-xs text-gray-400 bg-gray-900 rounded p-3 overflow-x-auto whitespace-pre-wrap">
                {escapeHtml(d.contextMd).slice(0, 150)}{escapeHtml(d.contextMd).length > 150 ? '...' : ''}
              </pre>
              <pre className="mt-1 text-xs text-gray-500 bg-gray-900 rounded p-3 overflow-x-auto whitespace-pre-wrap">
                {escapeHtml(d.outcomeMd).slice(0, 150)}{escapeHtml(d.outcomeMd).length > 150 ? '...' : ''}
              </pre>
              <p className="text-xs text-gray-500 mt-2">
                Decided: {new Date(d.decidedAt * 1000).toLocaleString()}
              </p>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
