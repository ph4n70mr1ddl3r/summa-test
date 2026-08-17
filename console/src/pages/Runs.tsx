export default function Runs() {
  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold">Runs</h2>
      <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
        <h3 className="text-lg font-semibold text-blue-300 mb-4">Run Lifecycle</h3>
        <div className="flex items-center space-x-2 text-sm text-gray-400 mb-4">
          <span className="px-2 py-1 bg-gray-700 rounded">queued</span>
          <span>→</span>
          <span className="px-2 py-1 bg-blue-900 rounded">running</span>
          <span>→</span>
          <span className="px-2 py-1 bg-green-900 rounded">completed</span>
          <span className="ml-4">or</span>
          <span className="px-2 py-1 bg-red-900 rounded">failed</span>
          <span className="ml-4">or</span>
          <span className="px-2 py-1 bg-yellow-900 rounded">cancelled</span>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="bg-gray-700 rounded p-4">
            <h4 className="text-blue-300 font-semibold mb-2">API</h4>
            <code className="text-xs text-gray-400">
              GET /api/runs?agentId=&lt;id&gt;{'\n'}
              POST /api/runs{'\n'}
              POST /api/runs/:id/start{'\n'}
              POST /api/runs/:id/complete{'\n'}
              POST /api/runs/:id/fail{'\n'}
              POST /api/runs/:id/cancel
            </code>
          </div>
          <div className="bg-gray-700 rounded p-4">
            <h4 className="text-green-300 font-semibold mb-2">Metering</h4>
            <code className="text-xs text-gray-400">
              cost_tokens, cost_usd on complete{'\n'}
              Spent to spend_ledger{'\n'}
              Audit event on every transition
            </code>
          </div>
        </div>
      </div>

      <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
        <h3 className="text-lg font-semibold text-blue-300 mb-4">Triggers</h3>
        <p className="text-gray-400 text-sm">
          Schedule, API, and event triggers launch runs. Missed schedules coalesce on resume.
          Critical triggers keep firing during spend halt (critical floor).
        </p>
      </div>
    </div>
  )
}
