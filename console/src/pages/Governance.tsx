export default function Governance() {
  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold">Governance</h2>
      
      <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
        <h3 className="text-lg font-semibold text-purple-300 mb-4">Policies</h3>
        <p className="text-gray-400 text-sm mb-2">
          Organization-wide tunables: spawn quotas, ask deadlines, DNA review SLAs, spend ceilings.
        </p>
        <div className="text-xs text-gray-500">
          <p>Endpoints: GET/PUT /api/governance/policies</p>
        </div>
      </div>

      <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
        <h3 className="text-lg font-semibold text-purple-300 mb-4">Quotas</h3>
        <p className="text-gray-400 text-sm mb-2">
          Spawn limits: max concurrent ephemerals per spawner, org-wide agent cap, depth cap.
        </p>
        <div className="text-xs text-gray-500">
          <p>Endpoints: GET/PUT /api/governance/quotas</p>
        </div>
      </div>

      <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
        <h3 className="text-lg font-semibold text-purple-300 mb-4">Spend</h3>
        <p className="text-gray-400 text-sm mb-2">
          Org-wide spend ceiling and critical floor. Breaker trips on threshold breach.
        </p>
        <div className="text-xs text-gray-500">
          <p>Endpoints: GET /api/governance/spend</p>
        </div>
      </div>

      <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
        <h3 className="text-lg font-semibold text-purple-300 mb-4">Data Holds</h3>
        <p className="text-gray-400 text-sm mb-2">
          Freezes erasure for covered subjects until admin releases them.
        </p>
        <div className="text-xs text-gray-500">
          <p>Endpoints: POST /api/governance/holds, POST /api/governance/holds/:id/release</p>
        </div>
      </div>
    </div>
  )
}
