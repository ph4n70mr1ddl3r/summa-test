export default function DNACards() {
  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold">DNA Cards</h2>
      <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
        <p className="text-gray-400 mb-4">
          Atomic knowledge cards with provenance. Create, edit, retire — never delete.
        </p>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="bg-gray-700 rounded p-4">
            <h4 className="text-blue-300 font-semibold mb-2">API</h4>
            <code className="text-xs text-gray-400">
              GET /api/dna/cards?domainId=&lt;id&gt;{'\n'}
              POST /api/dna/cards{'\n'}
              PATCH /api/dna/cards/:id{'\n'}
              POST /api/dna/cards/:id/retire
            </code>
          </div>
          <div className="bg-gray-700 rounded p-4">
            <h4 className="text-green-300 font-semibold mb-2">States</h4>
            <code className="text-xs text-gray-400">
              draft → active (publish){'\n'}
              active → retired (terminal)
            </code>
          </div>
        </div>
      </div>
    </div>
  )
}
