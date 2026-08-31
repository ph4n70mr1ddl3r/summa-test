import { useEffect, useState } from 'react'
import { api, type Node } from '../services/api'
import { escapeHtml } from '../utils/escapeHtml'

export default function Nodes() {
  const [nodes, setNodes] = useState<Node[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.nodes.list()
      .then((data) => { setNodes(data); setLoading(false) })
      .catch((err) => { setError(err instanceof Error ? err.message : String(err)); setLoading(false) })
  }, [])

  if (loading) return <div className="text-gray-400">Loading...</div>
  if (error) return <div className="text-red-400">Error: {error}</div>

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
                    Kind: {escapeHtml(n.kind)} | Region: {n.region ?? 'default'}
                  </p>
                  <p className="text-xs text-gray-500 mt-1">
                    Pubkey: {n.pubkey.slice(0, 16)}… | Enrolled: {n.enrolledAt ? new Date(n.enrolledAt * 1000).toLocaleString() : '—'}
                  </p>
                </div>
                <span className={`text-xs px-2 py-1 rounded ${
                  n.status === 'trusted' ? 'bg-green-900/50 text-green-400' : 'bg-red-900/50 text-red-400'
                }`}>
                  {n.status}
                </span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
