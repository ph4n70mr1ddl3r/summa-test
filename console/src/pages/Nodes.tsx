import { useEffect, useState } from 'react'
import { api } from '../services/api'
import type { Node } from '../types'
import { escapeHtml } from '../utils/escapeHtml'
import { formatDate, nodeStatusColor } from '../utils/formatting'
import { ErrorBanner } from '../components/ErrorBanner'

export default function Nodes() {
  const [nodes, setNodes] = useState<Node[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadNodes = () => {
    setLoading(true)
    setError(null)
    let aborted = false
    api.nodes.list()
      .then((data) => { if (!aborted) { setNodes(data); setLoading(false) } })
      .catch((err) => { if (!aborted) { setError(err instanceof Error ? err.message : String(err)); setLoading(false) } })
    return () => { aborted = true }
  }

  useEffect(() => {
    const cancel = loadNodes()
    return cancel
  }, [])

  if (loading) return <div className="text-gray-400">Loading...</div>
  if (error) return <ErrorBanner message={escapeHtml(error)} onRetry={loadNodes} />

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">Nodes</h2>
        <span className="text-sm text-gray-400">{nodes.length} nodes</span>
      </div>
      {nodes.length === 0 ? (
        <div className="bg-gray-800 rounded-lg p-8 border border-gray-700 text-center">
          <p className="text-gray-400">No trusted nodes.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {nodes.map((n) => (
            <div key={n.id} className="bg-gray-800 rounded-lg p-4 border border-gray-700">
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-medium text-gray-200">{escapeHtml(n.name)}</p>
                  <p className="text-sm text-gray-400 mt-1">
                    Kind: {escapeHtml(n.kind)} | Region: {escapeHtml(n.region ?? 'default')}
                  </p>
                  <p className="text-xs text-gray-500 mt-1">
                    Pubkey: {n.pubkey ? escapeHtml(n.pubkey.slice(0, 16)) : '?'}… | Enrolled: {n.enrolledAt != null ? formatDate(n.enrolledAt, { dateOnly: true }) : '—'}
                  </p>
                </div>
                <span className={`text-xs px-2 py-1 rounded ${nodeStatusColor(n.status)}`} aria-label={`Status: ${n.status}`}>
                  {escapeHtml(n.status)}
                </span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
