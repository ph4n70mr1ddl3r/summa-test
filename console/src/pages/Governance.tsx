import { useEffect, useState, useCallback } from 'react'
import { api, loadWithFallback } from '../services/api'
import type { SpendSnapshot } from '../types'
import { ErrorBanner } from '../components/ErrorBanner'
import { hasAnyData } from '../utils/loadState'
import { escapeHtml } from '../utils/formatting'

export default function Governance() {
  const [policies, setPolicies] = useState<Record<string, unknown>>({})
  const [quotas, setQuotas] = useState<Record<string, unknown>>({})
  const [spend, setSpend] = useState<SpendSnapshot | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [hasData, setHasData] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)
  const [saving, setSaving] = useState<Record<string, boolean>>({})
  const [editValues, setEditValues] = useState<Record<string, string>>({})

  const loadData = useCallback(() => {
    setLoading(true)
    setError(null)
    setEditValues({})
    let aborted = false
    loadWithFallback(
      () => Promise.all([
        api.governance.policies(),
        api.governance.quotas(),
        api.governance.spend(),
      ]),
      () => Promise.all([
        api.governance.policies().catch(() => null),
        api.governance.quotas().catch(() => null),
        api.governance.spend().catch(() => null),
      ]),
    ).then(({ data, error: loadError }) => {
      if (aborted) { setLoading(false); return }
      const policiesData = (data[0] != null && typeof data[0] === 'object' && !Array.isArray(data[0]))
        ? data[0] as Record<string, unknown> : null
      const quotasData = (data[1] != null && typeof data[1] === 'object' && !Array.isArray(data[1]))
        ? data[1] as Record<string, unknown> : null
      const spendData = (data[2] != null && typeof data[2] === 'object' && !Array.isArray(data[2]))
        ? data[2] as SpendSnapshot : null
      setPolicies(policiesData != null ? policiesData : {})
      setQuotas(quotasData != null ? quotasData : {})
      setSpend(spendData)
      // A partial outage must not blank the page — the panels that did load
      // stay on screen with the banner above them.
      setHasData(hasAnyData(data))
      setError(loadError)
      setLoading(false)
    })
    return () => { aborted = true }
  }, [])

  useEffect(() => {
    const cancel = loadData()
    return cancel
  }, [loadData])

  const handleSave = useCallback(async (section: 'policies' | 'quotas', key: string, currentValue: string) => {
    const saveKey = `${section}:${key}`
    setSaving(prev => ({ ...prev, [saveKey]: true }))
    setActionError(null)
    try {
      const trimmed = currentValue.trim()
      if (trimmed === '') {
        // Clearing a value: remove from server state and local cache
        const deleteBody = { [key]: null }
        if (section === 'policies') await api.governance.updatePolicies(deleteBody)
        else await api.governance.updateQuotas(deleteBody)
        if (section === 'policies') setPolicies(prev => { const next = { ...prev }; delete next[key]; return next })
        else setQuotas(prev => { const next = { ...prev }; delete next[key]; return next })
        setEditValues(prev => { const next = { ...prev }; delete next[key]; return next })
        return
      }
      // Boolean round-trip: a stored boolean rendered as the text "true"/"false"
      // must be sent back as a JSON boolean — the backend validateNumberOrBoolean
      // only accepts Boolean | Number | numeric string, so a bare "true" string
      // would fail validation ("must be a number") and corrupt the setting.
      const isBoolString = trimmed === 'true' || trimmed === 'false'
      const numericValue = Number(trimmed)
      let savedValue: number | boolean | string
      let body: Record<string, unknown>
      if (isBoolString) {
        savedValue = trimmed === 'true'
        body = { [key]: savedValue }
      } else if (Number.isNaN(numericValue)) {
        savedValue = trimmed
        body = { [key]: trimmed }
      } else {
        savedValue = numericValue
        body = { [key]: numericValue }
      }
      if (section === 'policies') await api.governance.updatePolicies(body)
      else await api.governance.updateQuotas(body)
      if (section === 'policies') setPolicies(prev => ({ ...prev, [key]: savedValue }))
      else setQuotas(prev => ({ ...prev, [key]: savedValue }))
      // Use functional update to read the latest editValues without stale closure
      setEditValues(prev => { const next = { ...prev }; delete next[key]; return next })
    } catch (err) {
      setActionError(err instanceof Error ? err.message : String(err))
      try { await loadData(); setError(null) } catch { /* keep original action error */ }
    } finally {
      setSaving(prev => { const next = { ...prev }; delete next[saveKey]; return next })
    }
  }, [loadData])

  const startEdit = useCallback((key: string, currentValue: unknown) => {
    const displayValue = typeof currentValue === 'object' && currentValue !== null
      ? JSON.stringify(currentValue)
      : String(currentValue ?? '')
    setEditValues(prev => ({ ...prev, [key]: displayValue }))
  }, [])

  const renderCell = useCallback((section: 'policies' | 'quotas', key: string, value: unknown, currentEditValue?: string) => {
    const isEditing = currentEditValue !== undefined
    const currentValue = isEditing ? currentEditValue : String(value ?? '')
    const saveKey = `${section}:${key}`
    const sv = saving[saveKey] ?? false
    return (
      <div key={key} className="flex items-center justify-between text-sm gap-2">
        <span className="text-gray-400 flex-shrink-0 w-48 truncate">{escapeHtml(key)}</span>
        {isEditing ? (
          <div className="flex items-center gap-1 flex-1">
            <input
              type="text"
              value={currentValue}
              onChange={(e) => setEditValues(prev => ({ ...prev, [key]: e.target.value }))}
              className="flex-1 bg-gray-700 border border-gray-600 rounded px-2 py-0.5 text-gray-200 text-xs font-mono"
              aria-label={`Edit ${key}`}
            />
              <button
              type="button"
              onClick={(e) => { e.stopPropagation(); handleSave(section, key, currentEditValue) }}
              disabled={sv}
              className="px-2 py-0.5 bg-green-700 hover:bg-green-600 disabled:bg-gray-600 rounded text-xs text-white"
              aria-label={`Save ${key}`}
            >
              {sv ? 'Saving...' : '✓'}
            </button>
            <button
              type="button"
              onClick={() => setEditValues(prev => { const next = { ...prev }; delete next[key]; return next })}
              className="px-2 py-0.5 bg-gray-700 hover:bg-gray-600 rounded text-xs text-gray-400"
              aria-label={`Cancel edit ${key}`}
            >
              ×
            </button>
          </div>
        ) : (
          <button
            type="button"
            onClick={() => startEdit(key, value)}
             className="flex-1 text-right text-gray-200 font-mono text-xs hover:text-white truncate"
             aria-label={`Edit ${key}`}
          >
            {typeof value === 'object' && value !== null ? escapeHtml(JSON.stringify(value)) : escapeHtml(String(value))}
          </button>
        )}
      </div>
    )
  }, [saving, startEdit, handleSave])

  if (loading) return <div className="text-gray-400" role="status" aria-live="polite">Loading...</div>
  if (error && !hasData) return <ErrorBanner message={error} onRetry={loadData} onDismiss={() => setError(null)} />

  const policyEntries = Object.entries(policies)
  const quotaEntries = Object.entries(quotas)

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold">Governance</h2>

      {error && (
        <ErrorBanner message={error} onRetry={loadData} onDismiss={() => setError(null)} />
      )}

      {actionError && (
        <div className="rounded-lg p-3 text-sm bg-red-900/30 border border-red-700 text-red-400" role="alert">
          {actionError}
          <button type="button" onClick={() => setActionError(null)} className="ml-2 text-red-300 hover:text-white" aria-label="Dismiss governance error">×</button>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
          <h3 className="text-lg font-semibold text-purple-300 mb-4">Policies ({policyEntries.length})</h3>
          {policyEntries.length === 0 ? (
            <p className="text-gray-500 text-sm">No policies configured.</p>
          ) : (
            <div className="space-y-1">
              {policyEntries.map(([k, v]) => renderCell('policies', k, v, editValues[k]))}
            </div>
          )}
        </div>

        <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
          <h3 className="text-lg font-semibold text-purple-300 mb-4">Quotas ({quotaEntries.length})</h3>
          {quotaEntries.length === 0 ? (
            <p className="text-gray-500 text-sm">No quotas configured.</p>
          ) : (
            <div className="space-y-1">
              {quotaEntries.map(([k, v]) => renderCell('quotas', k, v, editValues[k]))}
            </div>
          )}
        </div>
      </div>

      {spend && (
        <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
          <h3 className="text-lg font-semibold text-purple-300 mb-4">Spend</h3>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div>
              <p className="text-2xl font-bold text-green-400">{spend.reserved}</p>
              <p className="text-xs text-gray-500">Reserved</p>
            </div>
            <div>
              <p className="text-2xl font-bold text-blue-400">{spend.settled}</p>
              <p className="text-xs text-gray-500">Settled</p>
            </div>
            <div>
              <p className="text-2xl font-bold text-yellow-400">{spend.ceiling}</p>
              <p className="text-xs text-gray-500">Ceiling</p>
            </div>
            <div>
              <p className={`text-2xl font-bold ${spend.halted ? 'text-red-400' : 'text-gray-300'}`}>
                {spend.utilization}
              </p>
              <p className="text-xs text-gray-500">
                {spend.halted ? 'HALTED' : 'Utilization'}
              </p>
            </div>
          </div>
        </div>
      )}

    </div>
  )
}
