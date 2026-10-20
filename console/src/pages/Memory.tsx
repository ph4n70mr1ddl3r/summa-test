import { useEffect, useState, useCallback } from 'react'
import { api } from '../services/api'
import type { MemoryItem, DnaDomain, Workspace } from '../types'
import { truncateSnippet, escapeHtml, formatDate } from '../utils/formatting'
import { ErrorBanner } from '../components/ErrorBanner'
import { cancellableLoad, startLoad } from '../utils/loadState'
import { readSessionUser, canWrite } from '../utils/session'
import { stripMemberPrefix } from '../utils/formatting'

export default function Memory() {
  const [items, setItems] = useState<MemoryItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [filter, setFilter] = useState<'all' | 'tainted'>('all')
  const [reviewingId, setReviewingId] = useState<string | null>(null)
  const [reviewingForId, setReviewingForId] = useState<string | null>(null)
  const [isProcessingReview, setIsProcessingReview] = useState(false)
  const [reviewSuccess, setReviewSuccess] = useState(false)
  const [reviewError, setReviewError] = useState<string | null>(null)
  // The review door's authority depends on the item's tier (SUB-041), so the
  // control is offered only where the door would admit the caller: a personal
  // item to its member, a project item to a domain owner (or an admin), a
  // proposal item to any writer.
  const [ownedDomainIds, setOwnedDomainIds] = useState<Set<string>>(new Set())
  const [workspaceDomains, setWorkspaceDomains] = useState<Record<string, string[]>>({})
  const user = readSessionUser()
  const isAdmin = user?.rbac === 'admin'
  const userId = stripMemberPrefix(user?.userId ?? '')

  useEffect(() => {
    if (reviewSuccess) {
      const timer = setTimeout(() => setReviewSuccess(false), 3000)
      return () => clearTimeout(timer)
    }
  }, [reviewSuccess])

  const loadItems = useCallback(() => {
    setLoading(true)
    setError(null)
    let aborted = false
    const params: { memberId?: string; workspaceId?: string; tainted?: string } = {}
    if (filter === 'tainted') params.tainted = 'true'
    const promise = api.memory.list(params)
      .then((data) => { if (!aborted) { setItems(Array.isArray(data) ? data : []); setLoading(false) } })
      .catch((err) => {
        if (!aborted) { setError(err instanceof Error ? err.message : String(err)); setLoading(false) }
        throw err
      })
    return cancellableLoad(promise, () => { aborted = true })
  }, [filter])

  useEffect(() => startLoad(loadItems), [loadItems])

  // Load the reader-side inputs for the project-tier review rule: the domains
  // the caller owns and each workspace's domain bindings.
  useEffect(() => {
    let aborted = false
    Promise.all([
      api.dna.domains().catch(() => [] as DnaDomain[]).then((list) => list ?? []),
      api.workspaces.list().catch(() => [] as Workspace[]).then((list) => list ?? []),
    ]).then(([domains, workspaces]) => {
      if (aborted) return
      const owned = new Set<string>()
      for (const domain of domains) {
        if (stripMemberPrefix(domain.ownerHumanId) === userId) owned.add(domain.id)
      }
      setOwnedDomainIds(owned)
      const bindings: Record<string, string[]> = {}
      for (const workspace of workspaces) {
        try {
          const parsed = JSON.parse(workspace.domainIds || '[]')
          bindings[workspace.id] = Array.isArray(parsed) ? parsed : []
        } catch {
          bindings[workspace.id] = []
        }
      }
      setWorkspaceDomains(bindings)
    })
    return () => { aborted = true }
  }, [userId])

  const canReview = (item: MemoryItem): boolean => {
    if (!canWrite(user)) return false
    if (item.tier === 'personal') {
      return item.memberId != null && stripMemberPrefix(item.memberId) === userId
    }
    if (item.tier === 'project') {
      if (isAdmin) return true
      const domains = item.workspaceId ? workspaceDomains[item.workspaceId] ?? [] : []
      return domains.some((domainId) => ownedDomainIds.has(domainId))
    }
    return true
  }

  const handleReview = async (id: string) => {
    if (isProcessingReview) return
    setIsProcessingReview(true)
    setReviewSuccess(false)
    setReviewError(null)
    setReviewingForId(id)
    try {
      await api.memory.review(id)
      setReviewSuccess(true)
      setReviewingForId(null)
      setReviewingId(null)
    } catch (err) {
      setReviewError(err instanceof Error ? err.message : String(err))
      setReviewSuccess(false)
      setReviewingForId(null)
      setReviewingId(null)
    } finally {
      setIsProcessingReview(false)
    }
    // Reload in both cases and await it: the loader's failure is recorded in the
    // page's own error banner, so the rejection is only swallowed here.
    try { await loadItems().promise } catch { /* the page's error banner shows it */ }
  }

  const requestReview = (id: string) => {
    if (isProcessingReview || reviewingId === id || reviewingForId === id) return
    setReviewingId(id)
  }

  if (loading) return <div className="text-gray-400" role="status" aria-live="polite">Loading...</div>
  if (error) return <ErrorBanner message={error} onRetry={loadItems} onDismiss={() => setError(null)} />

  const taintedCount = items.filter(i => i.tainted).length

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">Memory</h2>
        <div className="flex items-center gap-3">
          <div className="flex rounded-lg overflow-hidden border border-gray-700" role="group" aria-label="Filter memory items">
            <button
              onClick={() => setFilter('all')}
              className={`px-3 py-1 text-sm ${filter === 'all' ? 'bg-blue-600 text-white' : 'bg-gray-800 text-gray-400 hover:text-white'}`}
              aria-pressed={filter === 'all'}
            >
              All
            </button>
            <button
              onClick={() => setFilter('tainted')}
              className={`px-3 py-1 text-sm ${filter === 'tainted' ? 'bg-red-700 text-white' : 'bg-gray-800 text-gray-400 hover:text-white'}`}
              aria-pressed={filter === 'tainted'}
            >
              Tainted ({taintedCount})
            </button>
          </div>
          <span className="text-sm text-gray-400">{items.length} items</span>
        </div>
      </div>

      {reviewSuccess && (
        <div className="rounded-lg p-3 text-sm bg-green-900/30 border border-green-700 text-green-400" role="alert">
          Item reviewed and taint cleared
        </div>
      )}
      {reviewError && (
        <div className="rounded-lg p-3 text-sm bg-red-900/30 border border-red-700 text-red-400" role="alert">
          {reviewError}
          <button type="button" onClick={() => setReviewError(null)} className="ml-2 text-red-300 hover:text-white" aria-label="Dismiss error">×</button>
        </div>
      )}

      {items.length === 0 ? (
        <div className="bg-gray-800 rounded-lg p-8 border border-gray-700 text-center">
          <p className="text-gray-400">No memory items{filter === 'tainted' ? ' (tainted)' : ''}.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {items.map((item) => (
            <div key={item.id} className="bg-gray-800 rounded-lg p-4 border border-gray-700">
              <div className="flex items-start justify-between">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-medium text-gray-200">Tier: {escapeHtml(item.tier)}</span>
                    <span className={`text-xs px-2 py-0.5 rounded ${
                      item.tainted ? 'bg-red-900/50 text-red-400' : 'bg-gray-700 text-gray-300'
                    }`}>
                      {item.tainted ? 'tainted' : 'clean'}
                    </span>
                  </div>
                  <p className="text-sm text-gray-400 mt-1">
                    {item.memberId ? `Member: ${escapeHtml(item.memberId)}` : 'System'}
                    {item.workspaceId ? ` · Workspace: ${escapeHtml(item.workspaceId)}` : ''}
                    {item.reviewedBy ? ` · Reviewed by: ${escapeHtml(item.reviewedBy)}` : ''}
                  </p>
                  {item.createdAt != null && (
                    <p className="text-xs text-gray-500 mt-1">Created: {formatDate(item.createdAt)}</p>
                  )}
                  {item.reviewedAt != null && (
                    <p className="text-xs text-gray-500">Reviewed: {formatDate(item.reviewedAt)}</p>
                  )}
                </div>
                {item.tainted && canReview(item) && (
                    <button
                      type="button"
                      onClick={() => requestReview(item.id)}
                      disabled={(reviewingForId !== null && reviewingForId !== item.id) || reviewingId === item.id || isProcessingReview}
                      className="px-3 py-1 bg-yellow-700 hover:bg-yellow-600 disabled:bg-gray-700 disabled:text-gray-500 rounded text-sm text-yellow-100"
                      aria-label={`Review tainted item ${item.id.slice(0, 8)}`}
                    >
                      Review
                    </button>
                )}
              </div>
              <pre className="mt-2 text-xs text-gray-400 bg-gray-900 rounded p-3 overflow-x-auto whitespace-pre-wrap">
                {escapeHtml(truncateSnippet(item.contentMd, 300))}
              </pre>
              {reviewingId === item.id && (
                <div className="mt-3 space-y-2">
                  <p className="text-sm text-yellow-400">Review this item to clear taint?</p>
                  <div className="flex gap-2">
                      <button
                        type="button"
                        onClick={() => handleReview(item.id)}
                        disabled={reviewingForId !== null || isProcessingReview}
                        className="px-3 py-1 bg-green-700 hover:bg-green-600 disabled:bg-gray-700 disabled:text-gray-500 rounded text-sm text-green-100"
                        aria-label={reviewingForId === item.id ? 'Reviewing item' : `Confirm review of item ${item.id.slice(0, 8)}`}
                      >
                       {reviewingForId === item.id ? 'Reviewing...' : 'Confirm Review'}
                     </button>
                     <button
                       type="button"
                       onClick={() => setReviewingId(null)}
                       className="px-3 py-1 bg-gray-700 hover:bg-gray-600 rounded text-sm text-gray-300"
                       aria-label="Cancel review"
                     >
                       Cancel
                     </button>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
        <h3 className="text-lg font-semibold text-purple-300 mb-4">Memory Tiers</h3>
        <ul className="text-gray-400 space-y-1 text-sm">
          <li>• personal — member-owned, only owner can review taint</li>
          <li>• project — workspace-scoped, domain owner or admin can review</li>
          <li>• proposal — org-scoped, any writer with write access can review</li>
        </ul>
      </div>
    </div>
  )
}
