import { useEffect, useState } from 'react'
import { api, loadWithFallback } from '../services/api'
import type { Group, Member } from '../types'
import { escapeHtml } from '../utils/escapeHtml'
import { groupStatusColor, rbacRoleColor, agentStatusColor } from '../utils/formatting'
import { ErrorBanner } from '../components/ErrorBanner'

export default function OrgView() {
  const [members, setMembers] = useState<Member[]>([])
  const [groups, setGroups] = useState<Group[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadData = () => {
    setLoading(true)
    setError(null)
    let aborted = false
    loadWithFallback(
      () => Promise.all([
        api.org.members(),
        api.groups.list(),
      ]),
      () => Promise.all([
        api.org.members().catch(() => null),
        api.groups.list().catch(() => null),
      ]),
    ).then(({ data, error: loadError }) => {
      if (aborted) return
      const membersData = data[0]
      setMembers(membersData != null && 'members' in membersData ? (membersData as { members: Member[] }).members : [])
      setGroups(data[1] as Group[])
      setError(loadError)
      setLoading(false)
    })
    return () => { aborted = true }
  }

  useEffect(() => {
    const cancel = loadData()
    return cancel
  }, [])

  if (loading) return <div className="text-gray-400" role="status" aria-live="polite">Loading...</div>
  if (error) return <ErrorBanner message={error} onRetry={loadData} />

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
                  <span className={`text-xs px-2 py-0.5 rounded ${rbacRoleColor(h.rbac)}`} aria-label={`RBAC role: ${h.rbac}`}>
                    {escapeHtml(h.rbac)}
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
                    <span className="text-xs text-gray-500">{escapeHtml(a.class)}</span>
                    <span className={`text-xs px-2 py-0.5 rounded ${agentStatusColor(a.status)}`} aria-label={`Status: ${a.status}`}>
                      {escapeHtml(a.status)}
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
                <span className={`text-xs px-2 py-0.5 rounded ${groupStatusColor(g.status)}`} aria-label={`Status: ${g.status}`}>
                  {escapeHtml(g.status)}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
