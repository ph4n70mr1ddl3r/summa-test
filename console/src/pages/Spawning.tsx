export default function Spawning() {
  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold">Spawning</h2>
      <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
        <h3 className="text-lg font-semibold text-purple-300 mb-4">Spawn Requests</h3>
        <p className="text-gray-400 mb-4">
          Two classes: persistent hires (approval-gated) and ephemeral workers (quotas + TTL).
        </p>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="bg-gray-700 rounded p-4">
            <h4 className="text-blue-300 font-semibold mb-2">API</h4>
            <code className="text-xs text-gray-400">
              GET /api/spawn{'\n'}
              POST /api/spawn{'\n'}
              POST /api/spawn/:id/approve{'\n'}
              POST /api/spawn/:id/deny
            </code>
          </div>
          <div className="bg-gray-700 rounded p-4">
            <h4 className="text-green-300 font-semibold mb-2">Gates</h4>
            <code className="text-xs text-gray-400">
              Spend circuit-breaker{'\n'}
              Quota caps (CFG-040/017/018){'\n'}
              Depth limit (default 2){'\n'}
              Template class match
            </code>
          </div>
        </div>
      </div>

      <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
        <h3 className="text-lg font-semibold text-purple-300 mb-4">Spend Circuit-Breaker</h3>
        <p className="text-gray-400 text-sm">
          When spend exceeds ceiling, all non-critical spawns halt. Critical floor (5%) keeps
          money-moving automations alive. Breaker un-trips only through admin ask resolution.
        </p>
      </div>

      <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
        <h3 className="text-lg font-semibold text-purple-300 mb-4">Lineage</h3>
        <p className="text-gray-400 text-sm">
          Every agent carries owner_human_id + spawned_by. Ephemeral workers fold back to spawner.
          Persistent hires roll up to first human up the chain. Depth cap (default 2) prevents runaway spawning.
        </p>
      </div>
    </div>
  )
}
