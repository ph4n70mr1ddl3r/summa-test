import { useEffect, useState } from 'react'
import { api, loadWithFallback } from '../services/api'
import type { SpendSnapshot } from '../types'
import { ErrorBanner } from '../components/ErrorBanner'

export default function Governance() {
  const [policies, setPolicies] = useState<Record<string, unknown>>({})
  const [quotas, setQuotas] = useState<Record<string, unknown>>({})
  const [spend, setSpend] = useState<SpendSnapshot | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState<string | null>(null)
  const [editValues, setEditValues] = useState<Record<string, string>>({})

  const loadData = () => {
    setLoading(true)
    setError(null)
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
      if (aborted) return
      const policiesData = (data[0] != null && typeof data[0] === 'object' && !Array.isArray(data[0]))
        ? data[0] as Record<string, unknown> : null
      const quotasData = (data[1] != null && typeof data[1] === 'object' && !Array.isArray(data[1]))
        ? data[1] as Record<string, unknown> : null
      const spendData = (data[2] != null && typeof data[2] === 'object' && !Array.isArray(data[2]))
        ? data[2] as SpendSnapshot : null
      setPolicies(policiesData != null ? policiesData : {})
      setQuotas(quotasData != null ? quotasData : {})
      setSpend(spendData)
      setError(loadError)
      setLoading(false)
    })
    return () => { aborted = true }
  }

  useEffect(() => {
    const cancel = loadData()
    return cancel
  }, [])

  const handleSave = async (section: 'policies' | 'quotas', key: string, value: string) => {
    setSaving(`${section}:${key}`)
    setError(null)
    try {
      const numValue = Number(value)
      const body = { [key]: isNaN(numValue) ? value : numValue }
      const result = section === 'policies'
        ? await api.governance.updatePolicies(body)
        : await api.governance.updateQuotas(body)
      if (section === 'policies') setPolicies(result as Record<string, unknown>)
      else setQuotas(result as Record<string, unknown>)
      setEditValues(prev => { const next = { ...prev }; delete next[key]; return next })
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      setSaving(null)
    }
  }

  const startEdit = (section: 'policies' | 'quotas', key: string, currentValue: unknown) => {
    setEditValues(prev => ({ ...prev, [key]: String(currentValue ?? '') }))
  }

  const renderCell = (section: 'policies' | 'quotas', key: string, value: unknown) => {
    const isEditing = editValues[key] !== undefined
    const currentValue = isEditing ? editValues[key] : String(value ?? '')
    return (
      <div key={key} className="flex items-center justify-between text-sm gap-2">
        <span className="text-gray-400 flex-shrink-0 w-48 truncate">{key}</span>
        {isEditing ? (
          <div className="flex items-center gap-1 flex-1">
            <input
              type="text"
              value={currentValue}
              onChange={e => setEditValues(prev => ({ ...prev, [key]: e.target.value }))}
              className="flex-1 bg-gray-700 border border-gray-600 rounded px-2 py-0.5 text-gray-200 text-xs font-mono"
              aria-label={`Edit ${key}`}
              autoFocus
            />
            <button
              type="button"
              onClick={() => handleSave(section, key, currentValue)}
              disabled={saving === `${section}:${key}`}
              className="px-2 py-0.5 bg-green-700 hover:bg-green-600 disabled:bg-gray-600 rounded text-xs text-white"
              aria-label={`Save ${key}`}
            >
              {saving === `${section}:${key}` ? '…' : '✓'}
            </button>
            <button
              type="button"
              onClick={() => { const next = { ...editValues }; delete next[key]; setEditValues(next) }}
              className="px-2 py-0.5 bg-gray-700 hover:bg-gray-600 rounded text-xs text-gray-400"
              aria-label={`Cancel edit ${key}`}
            >
              ✗
            </button>
          </div>
        ) : (
          <button
            type="button"
            onClick={() => startEdit(section, key, value)}
            className="flex-1 text-right text-gray-200 font-mono text-xs hover:text-white truncate"
            aria-label={`Edit ${key}: ${currentValue}`}
          >
            {typeof value === 'object' && value !== null ? JSON.stringify(value) : String(value)}
          </button>
        )}
      </div>
    )
  }

  if (loading) return <div className="text-gray-400" role="status" aria-live="polite">Loading...</div>
  if (error) return <ErrorBanner message={error} onRetry={loadData} />

  const policyEntries = Object.entries(policies)
  const quotaEntries = Object.entries(quotas)

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold">Governance</h2>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
          <h3 className="text-lg font-semibold text-purple-300 mb-4">Policies ({policyEntries.length})</h3>
          {policyEntries.length === 0 ? (
            <p className="text-gray-500 text-sm">No policies configured.</p>
          ) : (
            <div className="space-y-1">
              {policyEntries.map(([k, v]) => renderCell('policies', k, v))}
            </div>
          )}
        </div>

        <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
          <h3 className="text-lg font-semibold text-purple-300 mb-4">Quotas ({quotaEntries.length})</h3>
          {quotaEntries.length === 0 ? (
            <p className="text-gray-500 text-sm">No quotas configured.</p>
          ) : (
            <div className="space-y-1">
              {quotaEntries.map(([k, v]) => renderCell('quotas', k, v))}
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
