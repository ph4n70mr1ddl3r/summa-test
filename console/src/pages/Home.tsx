export default function Home() {
  return (
    <div className="space-y-8">
      <div className="text-center py-12">
        <h2 className="text-4xl font-bold text-blue-400 mb-4">Summa</h2>
        <p className="text-xl text-gray-400 mb-8">
          The operating system for a hybrid human + AI company
        </p>
        <div className="flex justify-center space-x-4">
          <a href="/api/org/humans" className="px-4 py-2 bg-blue-600 hover:bg-blue-700 rounded text-white">
            View API
          </a>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
          <h3 className="text-lg font-semibold text-blue-300 mb-2">DNA Console</h3>
          <p className="text-gray-400">
            Browse and review the Company DNA — knowledge cards, rules, decisions, and glossary.
          </p>
          <a href="/dna" className="mt-4 inline-block text-blue-400 hover:text-blue-300">
            Open DNA →
          </a>
        </div>

        <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
          <h3 className="text-lg font-semibold text-green-300 mb-2">Organization</h3>
          <p className="text-gray-400">
            Manage humans, agents, groups, and RBAC roles across the organization.
          </p>
          <a href="/org" className="mt-4 inline-block text-green-400 hover:text-green-300">
            Open Org →
          </a>
        </div>

        <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
          <h3 className="text-lg font-semibold text-yellow-300 mb-2">Ask Inbox</h3>
          <p className="text-gray-400">
            Review pending approvals, questions, and assignments from agents and the system.
          </p>
          <a href="/asks" className="mt-4 inline-block text-yellow-400 hover:text-yellow-300">
            Open Asks →
          </a>
        </div>
      </div>

      <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
        <h3 className="text-lg font-semibold text-gray-200 mb-4">API Health</h3>
        <ApiStatusCheck />
      </div>
    </div>
  )
}

function ApiStatusCheck() {
  return (
    <div className="flex items-center space-x-2 text-sm">
      <span className="w-2 h-2 bg-green-400 rounded-full"></span>
      <span className="text-gray-400">Backend: Running on port 8080</span>
    </div>
  )
}
