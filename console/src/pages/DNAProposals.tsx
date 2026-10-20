import { useEffect, useState, useMemo, useCallback } from 'react'
import { api } from '../services/api'
import type { DnaProposal, DnaProposalStatus } from '../types'
import { formatDate, proposalKindColor, proposalStatusColor, escapeHtml } from '../utils/formatting'
import { ErrorBanner } from '../components/ErrorBanner'
import { readSessionUser } from '../utils/session'

const PROPOSAL_STATUSES: DnaProposalStatus[] = ['open', 'breached', 'published', 'rejected', 'withdrawn']

/** Strip a DAT-120 keyed-union prefix (h:/a:) so both spellings compare equal. */
function stripKey(ref?: string | null): string {
  if (!ref) return ''
  return ref.startsWith('h:') || ref.startsWith('a:') ? ref.slice(2) : ref
}

export default function DNAProposals() {
  const [proposals, setProposals] = useState<DnaProposal[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [actionId, setActionId] = useState<string | null>(null)
  const [filter, setFilter] = useState<'all' | DnaProposalStatus>('all')

  const user = readSessionUser()
  const userId = user?.userId ?? ''
  const isAdmin = user?.rbac === 'admin'

  // Load the full lifecycle list once and filter client-side: the backend default
  // only returns the review inbox (open + breached), which would hide published/
  // rejected/withdrawn history and zero the other status counts (making their
  // filter buttons disappear mid-session).
  const loadProposals = useCallback(() => {
    setLoading(true)
    setError(null)
    let aborted = false
    api.dna.proposals('all')
       .then((data) => { if (!aborted) { setProposals(Array.isArray(data) ? data : []); setLoading(false) } })
      .catch((err) => { if (!aborted) { setError(err instanceof Error ? err.message : String(err)); setLoading(false) } })
    return () => { aborted = true }
  }, [])

  useEffect(() => {
    const cancel = loadProposals()
    return cancel
  }, [loadProposals])

  const visible = useMemo(
    () => (filter === 'all' ? proposals : proposals.filter(p => p.status === filter)),
    [proposals, filter],
  )

  const statusCounts = useMemo(() => {
    const counts: Record<string, number> = {}
    proposals.forEach(p => { counts[p.status] = (counts[p.status] || 0) + 1 })
    return counts
  }, [proposals])

  // DWP-003: the publish/reject door belongs to the proposal's reviewing owner
  // (review_by) or an admin. DWP-051: the proposer may withdraw their own.
  const canReview = (p: DnaProposal) => isAdmin || (!!p.reviewBy && stripKey(p.reviewBy) === userId)
  const canWithdraw = (p: DnaProposal) => isAdmin || stripKey(p.proposedBy) === userId

  const handleAction = async (id: string, action: 'publish' | 'reject' | 'withdraw') => {
    setActionId(id)
    setActionError(null)
    try {
      if (action === 'withdraw') await api.dna.withdrawProposal(id)
      else await api.dna.reviewProposal(id, action)
      await loadProposals()
    } catch (err) {
      setActionError(err instanceof Error ? err.message : String(err))
      try { await loadProposals() } catch (reloadErr) {
        setActionError(reloadErr instanceof Error ? reloadErr.message : String(reloadErr))
      }
    } finally {
      setActionId(null)
    }
  }

  if (loading) return <div className="text-gray-400" role="status" aria-live="polite">Loading...</div>
  if (error) return <ErrorBanner message={error} onRetry={loadProposals} onDismiss={() => setError(null)} />

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">DNA Proposals</h2>
        <div className="flex space-x-2 text-sm" role="group" aria-label="Filter proposals by status">
          <button
            onClick={() => setFilter('all')}
            className={`px-3 py-1 rounded ${filter === 'all' ? 'bg-blue-600 text-white' : 'bg-gray-700 text-gray-400 hover:text-white'}`}
            aria-pressed={filter === 'all'}
          >
            All ({proposals.length})
          </button>
          {PROPOSAL_STATUSES.map(s => (
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

      {actionError && (
        <div className="rounded-lg p-3 text-sm bg-red-900/30 border border-red-700 text-red-400" role="alert">
          {actionError}
          <button type="button" onClick={() => setActionError(null)} className="ml-2 text-red-300 hover:text-white" aria-label="Dismiss proposal action error">×</button>
        </div>
      )}

      {visible.length === 0 ? (
        <div className="bg-gray-800 rounded-lg p-8 border border-gray-700 text-center">
          <p className="text-gray-400">No DNA proposals yet.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {visible.map((p) => (
            <div key={p.id} className="bg-gray-800 rounded-lg p-4 border border-gray-700">
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-medium text-gray-200">
                    <span className={`text-xs px-2 py-0.5 rounded mr-2 ${proposalKindColor(p.kind)}`}>
                      {escapeHtml(p.kind)}
                    </span>
                    {escapeHtml(p.proposedBy)}
                  </p>
                  <p className="text-sm text-gray-400 mt-1">
                    {p.domainId ? `Domain: ${escapeHtml(p.domainId)}` : 'Organization-wide'}
                  </p>
                </div>
                <span className={`text-xs px-2 py-1 rounded ${proposalStatusColor(p.status)}`} aria-label={`Status: ${p.status}`}>
                  {p.status}
                </span>
              </div>
              <p className="text-xs text-gray-500 mt-2">
                Rev {p.revision} · Created {p.createdAt ? formatDate(p.createdAt, { dateOnly: true }) : '?'}
                {p.reviewedBy ? ` · Reviewed by ${escapeHtml(p.reviewedBy)}` : ''}
                {p.reviewBy && !p.reviewedBy ? ` · Assignee: ${escapeHtml(p.reviewBy)}` : ''}
              </p>
              {(p.status === 'open' || p.status === 'breached') && (canReview(p) || canWithdraw(p)) && (
                <div className="flex items-center gap-2 mt-3">
                  {canReview(p) && (
                    <>
                      <button
                        type="button"
                        onClick={() => handleAction(p.id, 'publish')}
                        disabled={actionId === p.id}
                        className="px-2 py-1 bg-green-700 hover:bg-green-600 disabled:bg-gray-600 rounded text-xs text-white"
                        aria-label={`Publish proposal ${p.id}`}
                      >
                        Publish
                      </button>
                      <button
                        type="button"
                        onClick={() => handleAction(p.id, 'reject')}
                        disabled={actionId === p.id}
                        className="px-2 py-1 bg-red-700 hover:bg-red-600 disabled:bg-gray-600 rounded text-xs text-white"
                        aria-label={`Reject proposal ${p.id}`}
                      >
                        Reject
                      </button>
                    </>
                  )}
                  {canWithdraw(p) && (
                    <button
                      type="button"
                      onClick={() => handleAction(p.id, 'withdraw')}
                      disabled={actionId === p.id}
                      className="px-2 py-1 bg-gray-600 hover:bg-gray-500 disabled:bg-gray-600 rounded text-xs text-white"
                      aria-label={`Withdraw proposal ${p.id}`}
                    >
                      Withdraw
                    </button>
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
