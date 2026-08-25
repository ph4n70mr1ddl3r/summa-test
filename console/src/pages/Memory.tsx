import { useEffect, useState } from 'react'
import { api, type MemoryItem } from '../services/api'
import { escapeHtml } from '../utils/escapeHtml'

export default function Memory() {
  const [items, setItems] = useState<MemoryItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.memory.list()
      .then((data) => { setItems(data); setLoading(false) })
      .catch((err) => { setError(err instanceof Error ? err.message : String(err)); setLoading(false) })
  }, [])

  if (loading) return <div className="text-gray-400">Loading...</div>
  if (error) return <div className="text-red-400">Error: {error}</div>

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">Memory</h2>
        <span className="text-sm text-gray-400">{items.length} items</span>
      </div>
      {items.length === 0 ? (
        <div className="bg-gray-800 rounded-lg p-8 border border-gray-700 text-center">
          <p className="text-gray-400">No memory items.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {items.map((item) => (
            <div key={item.id} className="bg-gray-800 rounded-lg p-4 border border-gray-700">
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-medium text-gray-200">Tier: {item.tier}</p>
                  <p className="text-sm text-gray-400 mt-1">
                    {item.memberId ? `Member: ${item.memberId}` : 'System'}
                    {item.workspaceId ? ` | Workspace: ${item.workspaceId}` : ''}
                  </p>
                </div>
                <span className={`text-xs px-2 py-1 rounded ${item.tainted ? 'bg-red-900/50 text-red-400' : 'bg-gray-700 text-gray-300'}`}>
                  {item.tainted ? 'tainted' : 'clean'}
                </span>
              </div>
              <pre className="mt-2 text-xs text-gray-400 bg-gray-900 rounded p-3 overflow-x-auto whitespace-pre-wrap">
                {escapeHtml(item.contentMd).slice(0, 300)}{item.contentMd.length > 300 ? '...' : ''}
              </pre>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
