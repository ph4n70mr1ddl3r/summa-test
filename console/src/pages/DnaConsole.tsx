import { useEffect, useState } from 'react'
import { NavLink } from 'react-router-dom'
import { api, type DnaDomain, type DnaCard, type DnaGoal, type DnaProposal } from '../services/api'

export default function DNAConsole() {
  const [domains, setDomains] = useState<DnaDomain[]>([])
  const [cards, setCards] = useState<DnaCard[]>([])
  const [goals, setGoals] = useState<DnaGoal[]>([])
  const [proposals, setProposals] = useState<DnaProposal[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let aborted = false
    Promise.all([
      api.dna.domains(),
      api.dna.cards(),
      api.dna.goals(),
      api.dna.proposals('open'),
    ]).then(([d, c, g, p]) => {
      if (aborted) return
      setDomains(d)
      setCards(c)
      setGoals(g)
      setProposals(p)
      setLoading(false)
    }).catch((e) => {
      if (aborted) return
      setError('Failed to load DNA data: ' + (e?.message || String(e)))
      setLoading(false)
    })
    return () => { aborted = true }
  }, [])

  if (loading) return <div className="text-gray-400">Loading...</div>
  if (error) return <div className="text-red-400">Error: {error}</div>

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold">DNA Console</h2>

      <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-blue-300">Domains ({domains.length})</h3>
          <NavLink to="/dna" className="text-sm text-blue-400 hover:text-blue-300">Manage →</NavLink>
        </div>
        {domains.length === 0 ? (
          <p className="text-gray-500 text-sm">No domains configured.</p>
        ) : (
          <div className="space-y-2">
            {domains.map((d) => (
              <div key={d.id} className="flex items-center justify-between bg-gray-700 rounded px-3 py-2">
                <span className="text-gray-200 text-sm">{d.name}</span>
                <span className={`text-xs px-2 py-0.5 rounded ${d.access === 'public' ? 'bg-green-900/50 text-green-400' : 'bg-yellow-900/50 text-yellow-400'}`}>
                  {d.access}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-blue-300">Review Queue ({proposals.length} open)</h3>
          <NavLink to="/dna" className="text-sm text-blue-400 hover:text-blue-300">Review →</NavLink>
        </div>
        {proposals.length === 0 ? (
          <p className="text-gray-500 text-sm">No open proposals.</p>
        ) : (
          <div className="space-y-2">
            {proposals.slice(0, 5).map((p) => (
              <div key={p.id} className="bg-gray-700 rounded px-3 py-2 text-sm text-gray-300">
                <span className="font-medium">{p.kind}</span>
                <span className="text-gray-500 ml-2">{p.proposedBy}</span>
                <span className={`ml-auto text-xs px-2 py-0.5 rounded ${p.status === 'open' ? 'bg-blue-900/50 text-blue-400' : 'bg-gray-600 text-gray-300'}`}>
                  {p.status}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-gray-800 rounded-lg p-4 border border-gray-700">
          <p className="text-2xl font-bold text-blue-400">{cards.length}</p>
          <p className="text-sm text-gray-400">Knowledge Cards</p>
        </div>
        <div className="bg-gray-800 rounded-lg p-4 border border-gray-700">
          <p className="text-2xl font-bold text-green-400">{goals.length}</p>
          <p className="text-sm text-gray-400">Active Goals</p>
        </div>
        <div className="bg-gray-800 rounded-lg p-4 border border-gray-700">
          <p className="text-2xl font-bold text-yellow-400">{domains.length}</p>
          <p className="text-sm text-gray-400">Domains</p>
        </div>
      </div>
    </div>
  )
}
