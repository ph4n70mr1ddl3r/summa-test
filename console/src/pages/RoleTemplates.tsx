import { useEffect, useState } from 'react'
import { api, type RoleTemplate } from '../services/api'

export default function RoleTemplates() {
  const [templates, setTemplates] = useState<RoleTemplate[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.roleTemplates.list()
      .then((data) => { setTemplates(data); setLoading(false) })
      .catch((err) => { setError(err instanceof Error ? err.message : String(err)); setLoading(false) })
  }, [])

  if (loading) return <div className="text-gray-400">Loading...</div>
  if (error) return <div className="text-red-400">Error: {error}</div>

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">Role Templates</h2>
        <span className="text-sm text-gray-400">{templates.length} templates</span>
      </div>
      {templates.length === 0 ? (
        <div className="bg-gray-800 rounded-lg p-8 border border-gray-700 text-center">
          <p className="text-gray-400">No role templates.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {templates.map((t) => (
            <div key={t.id} className="bg-gray-800 rounded-lg p-4 border border-gray-700">
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-medium text-gray-200">{t.name}</p>
                  <p className="text-sm text-gray-400 mt-1">Class: {t.class} | Version: {t.version}</p>
                </div>
                <span className="text-xs px-2 py-1 rounded bg-gray-700 text-gray-300">{t.status}</span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
