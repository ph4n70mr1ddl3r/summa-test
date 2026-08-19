import { useEffect, useState } from 'react'
import { api, type Ask } from '../services/api'

export default function AskInbox() {
  const [asks, setAsks] = useState<Ask[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.asks.listByStatus('pending')
      .then((data) => {
        setAsks(data)
        setLoading(false)
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : String(err))
        setLoading(false)
      })
  }, [])

  const tierColor = (tier: string) => {
    switch (tier) {
      case 'critical': return 'text-red-400 bg-red-900/30 border-red-700'
      case 'standard': return 'text-yellow-400 bg-yellow-900/30 border-yellow-700'
      case 'bulk': return 'text-gray-400 bg-gray-800 border-gray-600'
      default: return 'text-gray-400 bg-gray-800 border-gray-600'
    }
  }

  const kindIcon = (kind: string) => {
    switch (kind) {
      case 'approval': return '\u23FA'
      case 'question': return '?'
      case 'assignment': return '\u2192'
      case 'spawn_request': return '+'
      default: return '\u2022'
    }
  }

  const formatDeadline = (deadline: string | number) => {
    const epochSeconds = typeof deadline === 'string' ? parseInt(deadline, 10) : deadline
    if (isNaN(epochSeconds)) return '\u2014'
    const d = new Date(epochSeconds * 1000)
    return d.toLocaleString()
  }

  if (loading) {
    return (
      <div className="space-y-6">
        <h2 className="text-2xl font-bold">Ask Inbox</h2>
        <div className="text-gray-400">Loading...</div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="space-y-6">
        <h2 className="text-2xl font-bold">Ask Inbox</h2>
        <div className="bg-red-900/30 border border-red-700 rounded-lg p-4 text-red-400">
          Failed to load asks: {error}
          <button
            onClick={() => window.location.reload()}
            className="ml-4 px-3 py-1 bg-red-700 hover:bg-red-600 rounded text-sm text-red-100"
          >
            Retry
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">Ask Inbox</h2>
        <span className="text-sm text-gray-400">{asks.length} pending asks</span>
      </div>

      {asks.length === 0 ? (
        <div className="bg-gray-800 rounded-lg p-8 border border-gray-700 text-center">
          <p className="text-gray-400">No pending asks.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {asks.map((ask) => (
            <div
              key={ask.id}
              className="bg-gray-800 rounded-lg p-4 border border-gray-700 hover:border-gray-600 transition-colors"
            >
              <div className="flex items-start justify-between gap-4">
                <div className="flex items-center gap-3">
                  <span className="text-xl">{kindIcon(ask.kind)}</span>
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-medium text-gray-200 capitalize">{ask.kind}</span>
                      <span className={`text-xs px-2 py-0.5 rounded border ${tierColor(ask.slaTier)}`}>
                        {ask.slaTier}
                      </span>
                      {ask.collapsedCount != null && ask.collapsedCount > 1 && (
                        <span className="text-xs text-gray-500">
                          \u00d7{ask.collapsedCount} collapsed
                        </span>
                      )}
                    </div>
                    <p className="text-sm text-gray-400 mt-1">
                      From: <span className="text-gray-300">{ask.from}</span>
                      {' \u2192 '}To: <span className="text-gray-300">{ask.to}</span>
                    </p>
                  </div>
                </div>
                <div className="text-right shrink-0">
                  <p className="text-xs text-gray-500">Deadline</p>
                  <p className="text-sm text-gray-300">
                    {formatDeadline(ask.deadline)}
                  </p>
                  {ask.quorumRequired && ask.quorumRequired > 1 && (
                    <p className="text-xs text-gray-500 mt-1">
                      Quorum: {ask.quorumRequired}
                    </p>
                  )}
                </div>
              </div>
              <details className="mt-3">
                <summary className="text-sm text-blue-400 cursor-pointer hover:text-blue-300">
                  View payload
                </summary>
                <pre className="mt-2 text-xs text-gray-400 bg-gray-900 rounded p-3 overflow-x-auto">
                  {ask.payload}
                </pre>
              </details>
            </div>
          ))}
        </div>
      )}

      <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
        <h3 className="text-lg font-semibold text-yellow-300 mb-4">Ask Kinds</h3>
        <ul className="text-gray-400 space-y-1 text-sm">
          <li>\u2022 approval — requires human decision before proceeding</li>
          <li>\u2022 question — seeks information or guidance</li>
          <li>\u2022 assignment — delegates work to a member</li>
          <li>\u2022 spawn_request — gates agent spawning</li>
        </ul>
      </div>

      <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
        <h3 className="text-lg font-semibold text-yellow-300 mb-4">SLA Tiers</h3>
        <ul className="text-gray-400 space-y-1 text-sm">
          <li>\u2022 critical — blocks money-moving/critical runs (1h deadline)</li>
          <li>\u2022 standard — blocks regular runs (next digest)</li>
          <li>\u2022 bulk — non-blocking (24h deadline)</li>
        </ul>
      </div>
    </div>
  )
}
