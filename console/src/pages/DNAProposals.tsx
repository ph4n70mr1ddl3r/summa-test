import { useEffect, useState } from 'react'
import { api } from '../services/api'
import type { DnaProposal } from '../types'
import { escapeHtml } from '../utils/escapeHtml'
import { formatDate } from '../utils/formatting'
import { ErrorBanner } from '../components/ErrorBanner'

const proposalKindColor: Record<string, string> = {
  card: 'bg-blue-900/50 text-blue-400',
  rule: 'bg-yellow-900/50 text-yellow-400',
  decision: 'bg-green-900/50 text-green-400',
  goal: 'bg-purple-900/50 text-purple-400',
  glossary: 'bg-pink-900/50 text-pink-400',
  edit: 'bg-gray-700 text-gray-300',
}

const proposalStatusColor: Record<string, string> = {
  open: 'bg-blue-900/50 text-blue-400',
  published: 'bg-green-900/50 text-green-400',
  rejected: 'bg-red-900/50 text-red-400',
  withdrawn: 'bg-gray-600 text-gray-400',
}

export default function DNAProposals() {
  const [proposals, setProposals] = useState<DnaProposal[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [filter, setFilter] = useState<string>('all')

  const loadProposals = () => {
    setLoading(true)
    setError(null)
    let aborted = false
    const params: Record<string, string> = {}
    if (filter !== 'all') params.status = filter
    api.dna.proposals(filter !== 'all' ? filter : undefined)
      .then((data) => { if (!aborted) { setProposals(data); setLoading(false) } })
      .catch((err) => { if (!aborted) { setError(err instanceof Error ? err.message : String(err)); setLoading(false) } })
    return () => { aborted = true }
  }

  useEffect(() => {
    const cancel = loadProposals()
    return cancel
  }, [filter])

  if (loading) return <div className="text-gray-400">Loading...</div>
  if (error) return <ErrorBanner message={escapeHtml(error)} onRetry={loadProposals} />

  const statusCounts: Record<string, number> = {}
  proposals.forEach(p => { statusCounts[p.status] = (statusCounts[p.status] || 0) + 1 })

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">DNA Proposals</h2>
        <div className="flex space-x-2 text-sm">
          <button
            onClick={() => setFilter('all')}
            className={`px-3 py-1 rounded ${filter === 'all' ? 'bg-blue-600 text-white' : 'bg-gray-700 text-gray-400 hover:text-white'}`}
          >
            All ({proposals.length})
          </button>
          {(['open', 'published', 'rejected', 'withdrawn'] as const).map(s => (
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

      {proposals.length === 0 ? (
        <div className="bg-gray-800 rounded-lg p-8 border border-gray-700 text-center">
          <p className="text-gray-400">No DNA proposals yet.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {proposals.map((p) => (
            <div key={p.id} className="bg-gray-800 rounded-lg p-4 border border-gray-700">
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-medium text-gray-200">
                    <span className={`text-xs px-2 py-0.5 rounded mr-2 ${proposalKindColor[p.kind] || 'bg-gray-700 text-gray-300'}`}>
                      {escapeHtml(p.kind)}
                    </span>
                    {escapeHtml(p.proposedBy)}
                  </p>
                  <p className="text-sm text-gray-400 mt-1">
                    {p.domainId ? `Domain: ${escapeHtml(p.domainId)}` : 'Organization-wide'}
                  </p>
                </div>
                <span className={`text-xs px-2 py-1 rounded ${proposalStatusColor[p.status] || 'bg-gray-700 text-gray-300'}`} aria-label={`Status: ${p.status}`}>
                  {escapeHtml(p.status)}
                </span>
              </div>
              <p className="text-xs text-gray-500 mt-2">
                Rev {p.revision} · Created {p.createdAt ? formatDate(p.createdAt, { dateOnly: true }) : '?'}
                {p.reviewedBy ? ` · Reviewed by ${escapeHtml(p.reviewedBy)}` : ''}
              </p>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
