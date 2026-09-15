export function formatDate(epochSeconds: number | undefined | null, options?: { dateOnly?: boolean }): string {
  if (epochSeconds === null || epochSeconds === undefined) return '?'
  const d = new Date(epochSeconds * 1000)
  if (options?.dateOnly) {
    return d.toLocaleDateString()
  }
  return d.toLocaleString()
}

export function tierColor(tier: string): string {
  switch (tier) {
    case 'critical': return 'text-red-400 bg-red-900/30 border-red-700'
    case 'standard': return 'text-yellow-400 bg-yellow-900/30 border-yellow-700'
    case 'bulk': return 'text-gray-400 bg-gray-800 border-gray-600'
    default: return 'text-gray-400 bg-gray-800 border-gray-600'
  }
}

export function spawnStatusColor(status: string): string {
  switch (status) {
    case 'requested': return 'bg-yellow-900/50 text-yellow-400'
    case 'approved': return 'bg-green-900/50 text-green-400'
    case 'denied': return 'bg-red-900/50 text-red-400'
    default: return 'bg-gray-700 text-gray-300'
  }
}
