import { useEffect, useState } from 'react'
import { api, getCurrentActor, type Ask } from '../services/api'
import { escapeHtml } from '../utils/escapeHtml'

export default function AskInbox() {
  const [asks, setAsks] = useState<Ask[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [respondingId, setRespondingId] = useState<string | null>(null)
  const [responseText, setResponseText] = useState('')
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [submitSuccess, setSubmitSuccess] = useState<string | null>(null)

  const loadAsks = () => {
    setLoading(true)
    setError(null)
    api.asks.listByStatus('pending')
      .then((data) => { setAsks(data); setLoading(false) })
      .catch((err) => { setError(err instanceof Error ? err.message : String(err)); setLoading(false) })
  }

  useEffect(() => {
    loadAsks()
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
      case 'promotion': return '\u2191'
      default: return '\u2022'
    }
  }

  const formatDeadline = (deadline: number) => {
    const d = new Date(deadline * 1000)
    return d.toLocaleString()
  }

  const handleRespond = async (id: string) => {
    setSubmitError(null)
    setSubmitSuccess(null)
    try {
      await api.asks.respond(id, responseText, getCurrentActor())
      setSubmitSuccess('Response recorded')
      setRespondingId(null)
      setResponseText('')
      loadAsks()
    } catch (err) {
      setSubmitError(err instanceof Error ? err.message : String(err))
    }
  }

  const handleWithdraw = async (id: string) => {
    try {
      await api.asks.withdraw(id, getCurrentActor())
      loadAsks()
    } catch (err) {
      setSubmitError(err instanceof Error ? err.message : String(err))
    }
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
          Failed to load asks: {escapeHtml(error)}
          <button
            onClick={loadAsks}
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

      {submitError && (
        <div className="bg-red-900/30 border border-red-700 rounded-lg p-3 text-red-400 text-sm" role="alert">
          {escapeHtml(submitError)}
        </div>
      )}
      {submitSuccess && (
        <div className="bg-green-900/30 border border-green-700 rounded-lg p-3 text-green-400 text-sm" role="alert">
          {escapeHtml(submitSuccess)}
        </div>
      )}

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
                          ×{ask.collapsedCount} collapsed
                        </span>
                      )}
                    </div>
                    <p className="text-sm text-gray-400 mt-1">
                      From: <span className="text-gray-300">{escapeHtml(ask.from)}</span>
                      {` → `}To: <span className="text-gray-300">{escapeHtml(ask.to)}</span>
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
                  {escapeHtml(ask.payload)}
                </pre>
              </details>
              <div className="mt-3 flex items-center gap-2">
                <button
                  onClick={() => { setRespondingId(ask.id); setResponseText('') }}
                  className="px-3 py-1 bg-blue-700 hover:bg-blue-600 rounded text-sm text-blue-100"
                >
                  Respond
                </button>
                <button
                  onClick={() => handleWithdraw(ask.id)}
                  className="px-3 py-1 bg-gray-700 hover:bg-gray-600 rounded text-sm text-gray-300"
                >
                  Withdraw
                </button>
              </div>
              {respondingId === ask.id && (
                <div className="mt-3 space-y-2">
                  <textarea
                    value={responseText}
                    onChange={(e) => setResponseText(e.target.value)}
                    placeholder="Type your response..."
                    className="w-full bg-gray-900 border border-gray-600 rounded px-3 py-2 text-gray-100 text-sm focus:border-blue-500 focus:outline-none"
                    rows={3}
                  />
                  <div className="flex gap-2">
                    <button
                      onClick={() => handleRespond(ask.id)}
                      className="px-3 py-1 bg-green-700 hover:bg-green-600 rounded text-sm text-green-100"
                    >
                      Submit
                    </button>
                    <button
                      onClick={() => { setRespondingId(null); setResponseText('') }}
                      className="px-3 py-1 bg-gray-700 hover:bg-gray-600 rounded text-sm text-gray-300"
                    >
                      Cancel
                    </button>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
        <h3 className="text-lg font-semibold text-yellow-300 mb-4">Ask Kinds</h3>
        <ul className="text-gray-400 space-y-1 text-sm">
          <li>• approval — requires human decision before proceeding</li>
          <li>• question — seeks information or guidance</li>
          <li>• assignment — delegates work to a member</li>
          <li>• spawn_request — gates agent spawning</li>
          <li>• promotion — elevates a custom hire to a templated role</li>
        </ul>
      </div>

      <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
        <h3 className="text-lg font-semibold text-yellow-300 mb-4">SLA Tiers</h3>
        <ul className="text-gray-400 space-y-1 text-sm">
          <li>• critical — blocks money-moving/critical runs (1h deadline)</li>
          <li>• standard — blocks regular runs (next digest)</li>
          <li>• bulk — non-blocking (24h deadline)</li>
        </ul>
      </div>
    </div>
  )
}
