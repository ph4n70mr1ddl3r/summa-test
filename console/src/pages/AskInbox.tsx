export default function AskInbox() {
  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold">Ask Inbox</h2>
      
      <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
        <h3 className="text-lg font-semibold text-yellow-300 mb-4">Ask Kinds</h3>
        <ul className="text-gray-400 space-y-1 text-sm">
          <li>• approval — requires human decision before proceeding</li>
          <li>• question — seeks information or guidance</li>
          <li>• assignment — delegates work to a member</li>
          <li>• spawn_request — gates agent spawning</li>
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

      <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
        <h3 className="text-lg font-semibold text-yellow-300 mb-4">Escalation</h3>
        <p className="text-gray-400 text-sm">
          member → deputy → domain owner → admin broadcast. Each hop evaluated against live state.
        </p>
      </div>

      <div className="text-sm text-gray-500">
        <p>Endpoints: CRUD /api/asks</p>
      </div>
    </div>
  )
}
