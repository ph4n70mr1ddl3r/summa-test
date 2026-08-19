import { useEffect, useState } from 'react'
import { api, type Workspace } from '../services/api'

export default function Workspaces() {
  const [workspaces, setWorkspaces] = useState<Workspace[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.workspaces.list()
      .then((data) => { setWorkspaces(data); setLoading(false) })
      .catch((err) => { setError(err instanceof Error ? err.message : String(err)); setLoading(false) })
  }, [])

  if (loading) return <div className="text-gray-400">Loading...</div>
  if (error) return <div className="text-red-400">Error: {error}</div>

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">Workspaces</h2>
        <span className="text-sm text-gray-400">{workspaces.length} workspaces</span>
      </div>
      {workspaces.length === 0 ? (
        <div className="bg-gray-800 rounded-lg p-8 border border-gray-700 text-center">
          <p className="text-gray-400">No workspaces.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {workspaces.map((ws) => (
            <div key={ws.id} className="bg-gray-800 rounded-lg p-4 border border-gray-700">
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-medium text-gray-200">{ws.name}</p>
                  <p className="text-sm text-gray-400 mt-1">Kind: {ws.kind} | Epoch: {ws.claimEpoch}</p>
                </div>
                <span className="text-xs px-2 py-1 rounded bg-gray-700 text-gray-300">{ws.participants.length} participants</span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
