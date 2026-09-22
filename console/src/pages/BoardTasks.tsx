import { useEffect, useState } from 'react'
import { api } from '../services/api'
import type { BoardTask } from '../types'
import { boardTaskStatusColor } from '../utils/formatting'
import { ErrorBanner } from '../components/ErrorBanner'

export default function BoardTasks() {
  const [tasks, setTasks] = useState<BoardTask[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadTasks = () => {
    setLoading(true)
    setError(null)
    let aborted = false
    api.boardTasks.list()
      .then((data) => { if (!aborted) { setTasks(data); setLoading(false) } })
      .catch((err) => { if (!aborted) { setError(err instanceof Error ? err.message : String(err)); setLoading(false) } })
    return () => { aborted = true }
  }

  useEffect(() => {
    const cancel = loadTasks()
    return cancel
  }, [])

  if (loading) return <div className="text-gray-400" role="status" aria-live="polite">Loading...</div>
  if (error) return <ErrorBanner message={error} onRetry={loadTasks} />

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">Board Tasks</h2>
        <span className="text-sm text-gray-400">{tasks.length} tasks</span>
      </div>
      {tasks.length === 0 ? (
        <div className="bg-gray-800 rounded-lg p-8 border border-gray-700 text-center">
          <p className="text-gray-400">No board tasks.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {tasks.map((task) => (
            <div key={task.id} className="bg-gray-800 rounded-lg p-4 border border-gray-700">
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-medium text-gray-200">{task.title}</p>
                  {task.description && (
                    <p className="text-sm text-gray-400 mt-1">{task.description}</p>
                  )}
                  <p className="text-sm text-gray-400 mt-1">Priority: {task.priority} | Assignee: {task.assigneeMemberId ?? 'unassigned'}</p>
                </div>
                <span className={`text-xs px-2 py-1 rounded ${boardTaskStatusColor(task.status)}`} aria-label={`Status: ${task.status}`}>{task.status}</span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
