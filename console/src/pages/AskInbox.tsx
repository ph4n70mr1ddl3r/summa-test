import { useEffect, useState, useCallback, useRef } from 'react'
import { api, getUser } from '../services/api'
import type { Ask } from '../types'
import { tierColor, formatDate } from '../utils/formatting'
import { escapeHtml } from '../utils/escapeHtml'
import { ErrorBanner } from '../components/ErrorBanner'

function kindIcon(kind: string): string {
  switch (kind) {
    case 'approval': return '🔴'
    case 'question': return '?'
    case 'assignment': return '→'
    case 'spawn_request': return '+'
    case 'promotion': return '↑'
    default: return '•'
  }
}

export default function AskInbox() {
  const currentUser = getUser();
  const [asks, setAsks] = useState<Ask[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [respondingId, setRespondingId] = useState<string | null>(null)
  const [responseText, setResponseText] = useState('')
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [submitSuccess, setSubmitSuccess] = useState<string | null>(null)
  const [respondingForId, setRespondingForId] = useState<string | null>(null)
  const respondingIdRef = useRef<string | null>(null)
  respondingIdRef.current = respondingId

  const handleKeyDown = useCallback((e: KeyboardEvent) => {
    if (e.key === 'Escape' && respondingIdRef.current !== null) {
      setRespondingId(null)
      setResponseText('')
    }
  }, [])

  useEffect(() => {
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [handleKeyDown])

  const loadAsks = () => {
    setLoading(true)
    setError(null)
    let aborted = false
    api.asks.listByStatus('pending')
      .then((data) => { if (!aborted) { setAsks(data); setLoading(false) } })
      .catch((err) => { if (!aborted) { setError(err instanceof Error ? err.message : String(err)); setLoading(false) } })
    return () => { aborted = true }
  }

  useEffect(() => {
    const cancel = loadAsks()
    return cancel
  }, [])

  const handleRespond = async (id: string) => {
    setSubmitError(null)
    setSubmitSuccess(null)
    if (!responseText.trim()) {
      setSubmitError('Response cannot be empty')
      return
    }
    setRespondingForId(id)
    try {
      await api.asks.respond(id, responseText)
      setSubmitSuccess('Response recorded')
      setRespondingId(null)
      setResponseText('')
      await loadAsks()
    } catch (err) {
      setSubmitError(err instanceof Error ? err.message : String(err))
      await loadAsks()
    } finally {
      setRespondingForId(null)
    }
  }

  const handleWithdraw = async (id: string) => {
    try {
      await api.asks.withdraw(id)
      await loadAsks()
    } catch (err) {
      setSubmitError(err instanceof Error ? err.message : String(err))
      await loadAsks()
    }
  }

  if (loading) {
    return <div className="text-gray-400" role="status" aria-live="polite">Loading...</div>
  }

  if (error) {
    return <ErrorBanner message={error} onRetry={loadAsks} />
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">Ask Inbox</h2>
        <span className="text-sm text-gray-400">{asks.length} pending asks</span>
      </div>

      {submitError && (
        <div className="bg-red-900/30 border border-red-700 rounded-lg p-3 text-red-400 text-sm" role="alert">
          {submitError}
        </div>
      )}
      {submitSuccess && (
        <div className="bg-green-900/30 border border-green-700 rounded-lg p-3 text-green-400 text-sm" role="alert">
          {submitSuccess}
        </div>
      )}

      {asks.length === 0 ? (
        <div className="bg-gray-800 rounded-lg p-8 border border-gray-700 text-center" aria-live="polite">
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
                  <span className="text-xl" aria-label={`${ask.kind} ask`}>{kindIcon(ask.kind)}</span>
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-medium text-gray-200">{ask.kind.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())}</span>
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
                      From: <span className="text-gray-300">{ask.from}</span>
                      {` → `}To: <span className="text-gray-300">{ask.to}</span>
                    </p>
                  </div>
                </div>
                <div className="text-right shrink-0">
                  <p className="text-xs text-gray-500">Deadline</p>
                  <p className="text-sm text-gray-300">
                    {formatDate(ask.deadline, { dateOnly: false })}
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
                  type="button"
                  onClick={() => { setRespondingId(ask.id); setResponseText('') }}
                  className="px-3 py-1 bg-blue-700 hover:bg-blue-600 rounded text-sm text-blue-100"
                   aria-label={`Respond to ask from ${ask.from}`}
                >
                  Respond
                </button>
                <button
                  type="button"
                  onClick={() => handleWithdraw(ask.id)}
                  disabled={currentUser === null || currentUser.userId !== ask.from}
                  className="px-3 py-1 bg-gray-700 hover:bg-gray-600 disabled:bg-gray-800 disabled:text-gray-600 rounded text-sm text-gray-300"
                   aria-label={`Withdraw ask from ${ask.from}`}
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
                    aria-label="Response to ask"
                    className="w-full bg-gray-900 border border-gray-600 rounded px-3 py-2 text-gray-100 text-sm focus:border-blue-500 focus:outline-none"
                    rows={3}
                  />
                  <div className="flex gap-2">
                    <button
                      type="button"
                      onClick={() => handleRespond(ask.id)}
                      disabled={!responseText.trim() || respondingForId === ask.id}
                      className="px-3 py-1 bg-green-700 hover:bg-green-600 disabled:bg-gray-700 disabled:text-gray-500 rounded text-sm text-green-100"
                      aria-label="Submit response"
                    >
                      {respondingForId === ask.id ? 'Submitting...' : 'Submit'}
                    </button>
                    <button
                      type="button"
                      onClick={() => { setRespondingId(null); setResponseText('') }}
                      className="px-3 py-1 bg-gray-700 hover:bg-gray-600 rounded text-sm text-gray-300"
                      aria-label="Cancel response"
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
