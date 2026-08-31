import { useEffect, useState } from 'react'
import { api, type Group } from '../services/api'
import { escapeHtml } from '../utils/escapeHtml'

interface Member {
  id: string
  kind: 'human' | 'agent'
  name: string
  rbac?: string
  class?: string
  status?: string
  active?: boolean
}

export default function OrgView() {
  const [members, setMembers] = useState<Member[]>([])
  const [groups, setGroups] = useState<Group[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let aborted = false
    Promise.all([
      api.org.members(),
      api.groups.list(),
    ]).then(([m, g]) => {
      if (aborted) return
      setMembers(m.members.map((m: unknown) => ({
        id: (m as Record<string, unknown>).id as string,
        name: (m as Record<string, unknown>).name as string,
        kind: ((m as Record<string, unknown>).rbac as string | undefined) !== undefined ? 'human' : 'agent',
        rbac: (m as Record<string, unknown>).rbac as string | undefined,
        class: (m as Record<string, unknown>).class as string | undefined,
        status: (m as Record<string, unknown>).status as string | undefined,
        active: (m as Record<string, unknown>).active as boolean | undefined,
      } as Member)))
      setGroups(g)
      setLoading(false)
    }).catch((e) => {
      if (aborted) return
      setError('Failed to load org data: ' + (e?.message || String(e)))
      setLoading(false)
    })
    return () => { aborted = true }
  }, [])

  if (loading) return <div className="text-gray-400">Loading...</div>
  if (error) return <div className="text-red-400">Error: {error}</div>

  const humans = members.filter(m => m.kind === 'human')
  const agents = members.filter(m => m.kind === 'agent')

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">Organization</h2>
        <span className="text-sm text-gray-400">{members.length} members</span>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
          <h3 className="text-lg font-semibold text-green-300 mb-4">Humans ({humans.length})</h3>
          {humans.length === 0 ? (
            <p className="text-gray-500 text-sm">No humans yet.</p>
          ) : (
            <div className="space-y-2">
              {humans.map((h) => (
                <div key={h.id} className="flex items-center justify-between bg-gray-700 rounded px-3 py-2">
                  <span className="text-gray-200 text-sm">{escapeHtml(h.name)}</span>
                  <span className={`text-xs px-2 py-0.5 rounded ${
                    h.rbac === 'admin' ? 'bg-red-900/50 text-red-400' :
                    h.rbac === 'owner' ? 'bg-yellow-900/50 text-yellow-400' :
                    h.rbac === 'viewer' ? 'bg-gray-600 text-gray-400' :
                    'bg-blue-900/50 text-blue-400'
                  }`}>
                    {h.rbac}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
          <h3 className="text-lg font-semibold text-blue-300 mb-4">Agents ({agents.length})</h3>
          {agents.length === 0 ? (
            <p className="text-gray-500 text-sm">No agents yet.</p>
          ) : (
            <div className="space-y-2">
              {agents.map((a) => (
                <div key={a.id} className="flex items-center justify-between bg-gray-700 rounded px-3 py-2">
                  <span className="text-gray-200 text-sm">{escapeHtml(a.name)}</span>
                  <div className="flex items-center gap-2">
                    <span className="text-xs text-gray-500">{a.class}</span>
                    <span className={`text-xs px-2 py-0.5 rounded ${
                      a.status === 'active' ? 'bg-green-900/50 text-green-400' :
                      a.status === 'suspended' ? 'bg-yellow-900/50 text-yellow-400' :
                      'bg-gray-600 text-gray-400'
                    }`}>
                      {a.status}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
        <h3 className="text-lg font-semibold text-purple-300 mb-4">Groups ({groups.length})</h3>
        {groups.length === 0 ? (
          <p className="text-gray-500 text-sm">No groups configured.</p>
        ) : (
          <div className="space-y-2">
            {groups.map((g) => (
              <div key={g.id} className="flex items-center justify-between bg-gray-700 rounded px-3 py-2">
                <span className="text-gray-200 text-sm">{escapeHtml(g.name)}</span>
                <span className={`text-xs px-2 py-0.5 rounded ${
                  g.status === 'active' ? 'bg-green-900/50 text-green-400' : 'bg-gray-600 text-gray-400'
                }`}>
                  {g.status}
                </span>
              </div>
            ))}
          </div>
        )}
        <div className="mt-4 text-xs text-gray-500">
          <p>Endpoints: GET/POST /api/org/groups · PUT /api/org/groups/:id/leader</p>
        </div>
      </div>

      <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
        <h3 className="text-lg font-semibold text-yellow-300 mb-4">Nodes</h3>
        <p className="text-sm text-gray-400 mb-2">
          Enroll execution nodes via one-time token exchange; authenticate with keypair identity.
        </p>
        <div className="text-xs text-gray-500">
          <p>Endpoints: POST /api/nodes/enroll, POST /api/nodes/:id/heartbeat</p>
        </div>
      </div>

      <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
        <h3 className="text-lg font-semibold text-orange-300 mb-4">Role Templates</h3>
        <p className="text-sm text-gray-400 mb-2">
          Versioned catalog for agent roles; create/publish/retire are admin writes.
        </p>
        <div className="text-xs text-gray-500">
          <p>Endpoints: GET/POST /api/role-templates · POST /api/role-templates/:id/publish</p>
        </div>
      </div>
    </div>
  )
}
