export default function DnaConsole() {
  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold">DNA Console</h2>
      
      <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
        <h3 className="text-lg font-semibold text-blue-300 mb-4">Domains</h3>
        <p className="text-gray-400 mb-4">
          DNA is partitioned into domains, each with a human owner and an access policy.
        </p>
        <div className="text-sm text-gray-500">
          <p>Endpoints: GET/POST /api/dna/domains</p>
        </div>
      </div>

      <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
        <h3 className="text-lg font-semibold text-blue-300 mb-4">Review Queue</h3>
        <p className="text-gray-400 mb-4">
          Domain owners review DNA proposals: diff view, provenance, and impact hints.
        </p>
        <div className="text-sm text-gray-500">
          <p>Endpoints: GET /api/dna/proposals, POST /api/dna/proposals/:id/review</p>
        </div>
      </div>

      <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
        <h3 className="text-lg font-semibold text-blue-300 mb-4">Content Model</h3>
        <ul className="text-gray-400 space-y-1 text-sm">
          <li>• Cards — atomic knowledge with provenance</li>
          <li>• Rules — normative statements with effective dates</li>
          <li>• Decisions — immutable decision records</li>
          <li>• Glossary — canonical terminology</li>
          <li>• Goals — company objectives and KPIs</li>
        </ul>
      </div>
    </div>
  )
}
