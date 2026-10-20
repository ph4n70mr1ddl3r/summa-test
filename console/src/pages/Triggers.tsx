import { useEffect, useState, useCallback } from 'react'
import { api } from '../services/api'
import type { Agent, Trigger } from '../types'
import { triggerStatusColor, triggerCriticalityColor, escapeHtml, formatDate } from '../utils/formatting'
import { ErrorBanner } from '../components/ErrorBanner'
import { useEscapeKey } from '../hooks/useEscapeKey'
import { readSessionUser } from '../utils/session'
import { cancellableLoad } from '../utils/loadState'
import { stripMemberPrefix } from '../utils/formatting'

export default function Triggers() {
  const [triggers, setTriggers] = useState<Trigger[]>([])
  const [ownedAgentIds, setOwnedAgentIds] = useState<Set<string>>(new Set())
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [actionId, setActionId] = useState<string | null>(null)

  const user = readSessionUser()
  const isAdmin = user?.rbac === 'admin'
  const userId = user?.userId ?? ''

  useEscapeKey(() => {
    setActionId(null)
  })

  const loadTriggers = useCallback(() => {
    setLoading(true)
    setError(null)
    setActionError(null)
    let aborted = false
    // TRG-002: the trigger lifecycle doors belong to the trigger's agent owner or
    // an admin. The console asks the backend which agents the caller owns, so
    // the control is offered exactly where the door would admit it.
    const promise = Promise.all([
      api.triggers.list(),
      userId ? api.agents.list({ ownerId: userId }).catch(() => [] as Agent[]) : Promise.resolve([] as Agent[]),
    ])
      .then(([triggerData, agentData]) => {
        if (aborted) { setLoading(false); return }
        setTriggers(Array.isArray(triggerData) ? triggerData : [])
        setOwnedAgentIds(new Set((Array.isArray(agentData) ? agentData : []).map(a => a.id)))
        setLoading(false)
      })
      .catch((err) => {
        if (!aborted) { setError(err instanceof Error ? err.message : String(err)); setLoading(false) }
        throw err
      })
    return cancellableLoad(promise, () => { aborted = true })
  }, [userId])

  useEffect(() => {
    const cancel = loadTriggers()
    return cancel
  }, [loadTriggers])

  // TRG-002: an admin controls every trigger; anyone else controls only the
  // triggers of the agents they own.
  const canControl = (t: Trigger) => isAdmin || ownedAgentIds.has(stripMemberPrefix(t.agentId))

  const handleLifecycle = async (id: string, action: 'pause' | 'resume' | 'archive') => {
    setActionId(id)
    setActionError(null)
    try {
      if (action === 'pause') await api.triggers.pause(id)
      else if (action === 'resume') await api.triggers.resume(id)
      else await api.triggers.archive(id)
      // The reload is genuinely awaited: the loader's handle carries its promise,
      // so the action's button re-enables against the refreshed list.
      try { await loadTriggers().promise } catch { /* the page's error banner shows it */ }
    } catch (err) {
      setActionError(err instanceof Error ? err.message : String(err))
    } finally {
      setActionId(null)
    }
  }

  if (loading) return <div className="text-gray-400" role="status" aria-live="polite">Loading...</div>
  if (error) return <ErrorBanner message={error} onRetry={loadTriggers} onDismiss={() => setError(null)} />

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">Triggers</h2>
        <span className="text-sm text-gray-400">{triggers.length} triggers</span>
      </div>
      {actionError && (
        <div className="rounded-lg p-3 text-sm bg-red-900/30 border border-red-700 text-red-400" role="alert">
          {actionError}
          <button type="button" onClick={() => setActionError(null)} className="ml-2 text-red-300 hover:text-white" aria-label="Dismiss error">×</button>
        </div>
      )}
      {triggers.length === 0 ? (
        <div className="bg-gray-800 rounded-lg p-8 border border-gray-700 text-center">
          <p className="text-gray-400">No triggers configured.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {triggers.map((t) => (
            <div key={t.id} className="bg-gray-800 rounded-lg p-4 border border-gray-700">
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-medium text-gray-200">{escapeHtml(t.name)}</p>
                  <p className="text-sm text-gray-400 mt-1">Kind: {escapeHtml(t.kind)} | Agent: {escapeHtml(t.agentId)}</p>
                  {t.expression && (
                    <p className="text-xs text-gray-500 mt-1">Expression: {escapeHtml(t.expression)}</p>
                  )}
                  {t.config && (
                    <p className="text-xs text-gray-500 mt-1">Config: {escapeHtml(t.config)}</p>
                  )}
                  {t.lastFiredAt != null && (
                    <p className="text-xs text-gray-500">Last fired: {formatDate(t.lastFiredAt)}</p>
                  )}
                </div>
                <div className="flex items-center gap-2">
                  <span className={`text-xs px-2 py-1 rounded ${triggerStatusColor(t.status)}`} aria-label={`Status: ${t.status}`}>{t.status}</span>
                  <span className={`text-xs px-2 py-1 rounded border ${triggerCriticalityColor(t.criticality)}`} aria-label={`Criticality: ${t.criticality}`}>
                    {t.criticality}
                  </span>
                  {t.status === 'active' && canControl(t) && (
                    <button
                      type="button"
                      onClick={() => handleLifecycle(t.id, 'pause')}
                      disabled={actionId === t.id}
                      className="ml-2 px-2 py-1 text-xs bg-yellow-700 hover:bg-yellow-600 disabled:bg-gray-600 rounded text-yellow-100"
                      aria-label={`Pause trigger ${t.name}`}
                    >
                      {actionId === t.id ? 'Pausing...' : 'Pause'}
                    </button>
                  )}
                  {t.status === 'paused' && canControl(t) && (
                    <button
                      type="button"
                      onClick={() => handleLifecycle(t.id, 'resume')}
                      disabled={actionId === t.id}
                      className="ml-2 px-2 py-1 text-xs bg-green-700 hover:bg-green-600 disabled:bg-gray-600 rounded text-green-100"
                      aria-label={`Resume trigger ${t.name}`}
                    >
                      {actionId === t.id ? 'Resuming...' : 'Resume'}
                    </button>
                  )}
                  {t.status !== 'archived' && canControl(t) && (
                    <button
                      type="button"
                      onClick={() => handleLifecycle(t.id, 'archive')}
                      disabled={actionId === t.id}
                      className="ml-2 px-2 py-1 text-xs bg-gray-700 hover:bg-gray-600 disabled:bg-gray-600 rounded text-gray-300"
                      aria-label={`Archive trigger ${t.name}`}
                    >
                      {actionId === t.id ? 'Archiving...' : 'Archive'}
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
