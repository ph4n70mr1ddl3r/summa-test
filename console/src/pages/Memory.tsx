import { useEffect, useState } from 'react'
import { api, type MemoryItem } from '../services/api'
import { escapeHtml } from '../utils/escapeHtml'

export default function Memory() {
  const [items, setItems] = useState<MemoryItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [filter, setFilter] = useState<'all' | 'tainted'>('all')
  const [reviewingId, setReviewingId] = useState<string | null>(null)
  const [reviewResult, setReviewResult] = useState<string | null>(null)

  const loadItems = () => {
    setLoading(true)
    setError(null)
    const params: Record<string, string> = {}
    if (filter === 'tainted') params.tainted = 'true'
    api.memory.list(params)
      .then((data) => { setItems(data); setLoading(false) })
      .catch((err) => { setError(err instanceof Error ? err.message : String(err)); setLoading(false) })
  }

  useEffect(() => {
    loadItems()
  }, [filter])

  const handleReview = async (id: string) => {
    setReviewResult(null)
    try {
      await api.memory.review(id, '')
      setReviewResult('Item reviewed and taint cleared')
      setReviewingId(null)
      loadItems()
    } catch (err) {
      setReviewResult(err instanceof Error ? err.message : String(err))
    }
  }

  if (loading) return <div className="text-gray-400">Loading...</div>
  if (error) return <div className="text-red-400">Error: {error}</div>

  const taintedCount = items.filter(i => i.tainted).length

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">Memory</h2>
        <div className="flex items-center gap-3">
          <div className="flex rounded-lg overflow-hidden border border-gray-700">
            <button
              onClick={() => setFilter('all')}
              className={`px-3 py-1 text-sm ${filter === 'all' ? 'bg-blue-700 text-white' : 'bg-gray-800 text-gray-400 hover:text-white'}`}
            >
              All
            </button>
            <button
              onClick={() => setFilter('tainted')}
              className={`px-3 py-1 text-sm ${filter === 'tainted' ? 'bg-red-700 text-white' : 'bg-gray-800 text-gray-400 hover:text-white'}`}
            >
              Tainted ({taintedCount})
            </button>
          </div>
          <span className="text-sm text-gray-400">{items.length} items</span>
        </div>
      </div>

      {reviewResult && (
        <div className={`rounded-lg p-3 text-sm ${
          reviewResult.startsWith('Item reviewed')
            ? 'bg-green-900/30 border border-green-700 text-green-400'
            : 'bg-red-900/30 border border-red-700 text-red-400'
        }`}>
          {escapeHtml(reviewResult)}
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
                    <span className="font-medium text-gray-200">Tier: {item.tier}</span>
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
                </div>
                {item.tainted && (
                  <button
                    onClick={() => setReviewingId(item.id)}
                    className="px-3 py-1 bg-yellow-700 hover:bg-yellow-600 rounded text-sm text-yellow-100"
                  >
                    Review
                  </button>
                )}
              </div>
              <pre className="mt-2 text-xs text-gray-400 bg-gray-900 rounded p-3 overflow-x-auto whitespace-pre-wrap">
                {escapeHtml(item.contentMd).slice(0, 300)}{escapeHtml(item.contentMd).length > 300 ? '...' : ''}
              </pre>
              {reviewingId === item.id && (
                <div className="mt-3 space-y-2">
                  <p className="text-sm text-yellow-400">Review this item to clear taint?</p>
                  <div className="flex gap-2">
                    <button
                      onClick={() => handleReview(item.id)}
                      className="px-3 py-1 bg-green-700 hover:bg-green-600 rounded text-sm text-green-100"
                    >
                      Confirm Review
                    </button>
                    <button
                      onClick={() => setReviewingId(null)}
                      className="px-3 py-1 bg-gray-700 hover:bg-gray-600 rounded text-sm text-gray-300"
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
        <div className="mt-4 text-xs text-gray-500">
          <p>Endpoints: GET /api/memory · POST /api/memory · POST /api/memory/:id/review</p>
        </div>
      </div>
    </div>
  )
}
