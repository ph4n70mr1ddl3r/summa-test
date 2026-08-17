export default function OrgView() {
  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold">Organization</h2>
      
      <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
        <h3 className="text-lg font-semibold text-green-300 mb-4">Members</h3>
        <p className="text-gray-400 mb-4">
          Humans and agents share one member namespace. RBAC roles: admin, owner, member, viewer.
        </p>
        <div className="text-sm text-gray-500">
          <p>Endpoints: GET /api/org/humans</p>
        </div>
      </div>

      <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
        <h3 className="text-lg font-semibold text-green-300 mb-4">Groups</h3>
        <p className="text-gray-400 mb-4">
          Mix humans and agents; archival is the terminal act — no bare delete.
        </p>
        <div className="text-sm text-gray-500">
          <p>Endpoints: CRUD /api/org/groups</p>
        </div>
      </div>

      <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
        <h3 className="text-lg font-semibold text-green-300 mb-4">Nodes</h3>
        <p className="text-gray-400 mb-4">
          Enroll execution nodes via one-time token exchange; authenticate with keypair identity.
        </p>
        <div className="text-sm text-gray-500">
          <p>Endpoints: POST /api/nodes/enroll, POST /api/nodes/:id/heartbeat</p>
        </div>
      </div>

      <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
        <h3 className="text-lg font-semibold text-green-300 mb-4">Role Templates</h3>
        <p className="text-gray-400 mb-4">
          Versioned catalog; create/publish/retire are admin writes.
        </p>
        <div className="text-sm text-gray-500">
          <p>Endpoints: CRUD /api/role-templates</p>
        </div>
      </div>
    </div>
  )
}
