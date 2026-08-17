export default function DNARules() {
  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold">DNA Rules</h2>
      <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
        <p className="text-gray-400 mb-4">
          Normative statements with effective dates. Supersession chains, not forks.
        </p>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="bg-gray-700 rounded p-4">
            <h4 className="text-blue-300 font-semibold mb-2">API</h4>
            <code className="text-xs text-gray-400">
              GET /api/dna/rules?domainId=&lt;id&gt;{'\n'}
              POST /api/dna/rules{'\n'}
              PATCH /api/dna/rules/:id{'\n'}
              POST /api/dna/rules/:id/supersede/:supersedesId
            </code>
          </div>
          <div className="bg-gray-700 rounded p-4">
            <h4 className="text-green-300 font-semibold mb-2">States</h4>
            <code className="text-xs text-gray-400">
              active → superseded (by successor){'\n'}
              active → lapsed (window end)
            </code>
          </div>
        </div>
      </div>
    </div>
  )
}
