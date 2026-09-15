import { useEffect, useState, useRef } from 'react'
import { api } from '../services/api'
import type { Group } from '../types'
import { escapeHtml } from '../utils/escapeHtml'
import { ErrorBanner } from '../components/ErrorBanner'

export default function Groups() {
  const [groups, setGroups] = useState<Group[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const abortedRef = useRef(false)

  const loadGroups = () => {
    setLoading(true)
    setError(null)
    abortedRef.current = false
    api.groups.list()
      .then((data) => { if (!abortedRef.current) { setGroups(data); setLoading(false) } })
      .catch((err) => { if (!abortedRef.current) { setError(err instanceof Error ? err.message : String(err)); setLoading(false) } })
  }

  useEffect(() => {
    loadGroups()
    return () => { abortedRef.current = true }
  }, [])

  const handleArchive = async (id: string) => {
    try {
      await api.groups.archive(id)
      loadGroups()
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    }
  }

  if (loading) return <div className="text-gray-400">Loading...</div>
  if (error) return <ErrorBanner message={escapeHtml(error)} onRetry={loadGroups} />

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">Groups</h2>
        <span className="text-sm text-gray-400">{groups.length} groups</span>
      </div>

      {groups.length === 0 ? (
        <div className="bg-gray-800 rounded-lg p-8 border border-gray-700 text-center">
          <p className="text-gray-400">No groups configured.</p>
          <p className="text-sm text-gray-500 mt-2">
            Groups mix humans and agents; archival is the terminal act — no bare delete.
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {groups.map((g) => (
            <div key={g.id} className="bg-gray-800 rounded-lg p-4 border border-gray-700">
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-medium text-gray-200">{escapeHtml(g.name)}</p>
                  <p className="text-sm text-gray-400 mt-1">
                    {g.leaderMemberId ? `Leader: ${escapeHtml(g.leaderMemberId)}` : 'No leader assigned'}
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <span className={`text-xs px-2 py-1 rounded ${
                    g.status === 'active' ? 'bg-green-900/50 text-green-400' : 'bg-gray-700 text-gray-300'
                  }`} aria-label={`Status: ${g.status}`}>
                    {escapeHtml(g.status)}
                  </span>
                  {g.status === 'active' && (
                    <button
                      onClick={() => handleArchive(g.id)}
                      className="px-2 py-1 bg-gray-700 hover:bg-gray-600 rounded text-xs text-gray-400"
                    >
                      Archive
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
        <h3 className="text-lg font-semibold text-purple-300 mb-4">API Reference</h3>
        <div className="text-xs text-gray-500 space-y-1">
          <p>GET /api/org/groups · POST /api/org/groups</p>
          <p>POST /api/org/groups/:id/archive</p>
          <p>PUT /api/org/groups/:id/leader</p>
        </div>
      </div>
    </div>
  )
}
