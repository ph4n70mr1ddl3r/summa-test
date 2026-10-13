import { useEffect, useState } from 'react'
import { api } from '../services/api'
import type { DnaGlossary } from '../types'
import { formatDate, escapeHtml } from '../utils/formatting'
import { ErrorBanner } from '../components/ErrorBanner'

const GLOSSARY_STATUS_COLOR: Record<DnaGlossary['status'], string> = {
  draft: 'bg-yellow-900/50 text-yellow-400',
  active: 'bg-green-900/50 text-green-400',
  retired: 'bg-gray-600 text-gray-400',
}

function getGlossaryStatusColor(status: DnaGlossary['status']): string {
  return GLOSSARY_STATUS_COLOR[status] ?? 'bg-gray-700 text-gray-400'
}

export default function Glossary() {
  const [entries, setEntries] = useState<DnaGlossary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadEntries = () => {
    setLoading(true)
    setError(null)
    let aborted = false
    api.dna.glossary()
      .then((data) => { if (!aborted) { setEntries(Array.isArray(data) ? data : []); setLoading(false) } })
      .catch((err) => { if (!aborted) { setError(err instanceof Error ? err.message : String(err)); setLoading(false) } })
    return () => { aborted = true }
  }

  useEffect(() => {
    const cancel = loadEntries()
    return cancel
  }, [])

  if (loading) return <div className="text-gray-400" role="status" aria-live="polite">Loading...</div>
  if (error) return <ErrorBanner message={error} onRetry={loadEntries} onDismiss={() => setError(null)} />

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">DNA Glossary</h2>
        <span className="text-sm text-gray-400">{entries.length} entries</span>
      </div>
      {entries.length === 0 ? (
        <div className="bg-gray-800 rounded-lg p-8 border border-gray-700 text-center">
          <p className="text-gray-400">No glossary entries yet.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {entries.map((entry) => (
            <div key={entry.id} className="bg-gray-800 rounded-lg p-4 border border-gray-700">
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-medium text-gray-200">{escapeHtml(entry.term)}</p>
                  <p className="text-sm text-gray-400 mt-1">
                    {entry.domainId ? `Domain: ${escapeHtml(entry.domainId)}` : 'No domain'}
                    {entry.aliases ? ` · Aliases: ${escapeHtml(entry.aliases)}` : ''}
                  </p>
                </div>
                <span className={`text-xs px-2 py-1 rounded ${getGlossaryStatusColor(entry.status)}`} aria-label={`Status: ${entry.status}`}>
                  {entry.status}
                </span>
              </div>
              <pre className="mt-2 text-xs text-gray-400 bg-gray-900 rounded p-3 overflow-x-auto whitespace-pre-wrap">
                {escapeHtml(entry.definition)}
              </pre>
              <p className="text-xs text-gray-500 mt-2">
                Created {entry.createdAt != null ? formatDate(entry.createdAt, { dateOnly: true }) : '?'}
                {entry.updatedAt != null ? ` · Updated ${formatDate(entry.updatedAt, { dateOnly: true })}` : ''}
              </p>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
