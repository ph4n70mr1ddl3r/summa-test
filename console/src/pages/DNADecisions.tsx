export default function DNADecisions() {
  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold">DNA Decisions</h2>
      <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
        <p className="text-gray-400 mb-4">
          Immutable decision records. Create-only — reversal is a new record citing the old.
        </p>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="bg-gray-700 rounded p-4">
            <h4 className="text-blue-300 font-semibold mb-2">API</h4>
            <code className="text-xs text-gray-400">
              GET /api/dna/decisions?domainId=&lt;id&gt;{'\n'}
              POST /api/dna/decisions
            </code>
          </div>
          <div className="bg-gray-700 rounded p-4">
            <h4 className="text-green-300 font-semibold mb-2">Properties</h4>
            <code className="text-xs text-gray-400">
              Lifecycle-free (immutable){'\n'}
              Decided-by cited as provenance{'\n'}
              Never block domain archive
            </code>
          </div>
        </div>
      </div>
    </div>
  )
}
