import { useEffect, useState } from 'react'
import { api, type DnaCard } from '../services/api'
import { escapeHtml } from '../utils/escapeHtml'

export default function DNACards() {
  const [cards, setCards] = useState<DnaCard[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let aborted = false
    api.dna.cards()
      .then((data) => { if (!aborted) { setCards(data); setLoading(false) } })
      .catch((err) => { if (!aborted) { setError(err instanceof Error ? err.message : String(err)); setLoading(false) } })
    return () => { aborted = true }
  }, [])

  if (loading) return <div className="text-gray-400">Loading...</div>
  if (error) return <div className="text-red-400">Error: {error}</div>

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">DNA Cards</h2>
        <span className="text-sm text-gray-400">{cards.length} cards</span>
      </div>
      {cards.length === 0 ? (
        <div className="bg-gray-800 rounded-lg p-8 border border-gray-700 text-center">
          <p className="text-gray-400">No knowledge cards yet. Create your first card to get started.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {cards.map((card) => (
            <div key={card.id} className="bg-gray-800 rounded-lg p-4 border border-gray-700">
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-medium text-gray-200">{escapeHtml(card.title)}</p>
                  <p className="text-sm text-gray-400 mt-1">Domain: {escapeHtml(card.domainId)}</p>
                </div>
                <span className={`text-xs px-2 py-1 rounded ${
                  card.status === 'active' ? 'bg-green-900/50 text-green-400' :
                  card.status === 'draft' ? 'bg-yellow-900/50 text-yellow-400' :
                  'bg-gray-700 text-gray-300'
                }`}>
                  {card.status}
                </span>
              </div>
              <pre className="mt-2 text-xs text-gray-400 bg-gray-900 rounded p-3 overflow-x-auto whitespace-pre-wrap">
                {(() => {
                const snippet = escapeHtml(card.definitionMd)
                return <>{snippet.slice(0, 200)}{snippet.length > 200 ? '...' : ''}</>
              })()}
              </pre>
              <p className="text-xs text-gray-500 mt-2">v{card.version} · Created {card.createdAt ?? '?'}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
