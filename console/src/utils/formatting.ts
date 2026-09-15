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

export function initiativeStatusColor(status: string): string {
  switch (status) {
    case 'proposed': return 'bg-blue-900/50 text-blue-400'
    case 'active': return 'bg-green-900/50 text-green-400'
    case 'paused': return 'bg-yellow-900/50 text-yellow-400'
    case 'closed': return 'bg-gray-600 text-gray-400'
    default: return 'bg-gray-700 text-gray-300'
  }
}

export function runStatusColor(status: string): string {
  switch (status) {
    case 'completed': return 'bg-green-900/50 text-green-400'
    case 'running': return 'bg-blue-900/50 text-blue-400'
    case 'failed': return 'bg-red-900/50 text-red-400'
    case 'queued': return 'bg-yellow-900/50 text-yellow-400'
    default: return 'bg-gray-700 text-gray-300'
  }
}

export function triggerStatusColor(status: string): string {
  switch (status) {
    case 'active': return 'bg-green-900/50 text-green-400'
    case 'paused': return 'bg-yellow-900/50 text-yellow-400'
    case 'archived': return 'bg-gray-600 text-gray-400'
    default: return 'bg-gray-700 text-gray-300'
  }
}

export function boardTaskStatusColor(status: string): string {
  switch (status) {
    case 'done': return 'bg-green-900/50 text-green-400'
    case 'in_progress': return 'bg-blue-900/50 text-blue-400'
    case 'cancelled': return 'bg-red-900/50 text-red-400'
    case 'open': return 'bg-yellow-900/50 text-yellow-400'
    default: return 'bg-gray-700 text-gray-300'
  }
}

export function roleTemplateStatusColor(status: string): string {
  switch (status) {
    case 'active': return 'bg-green-900/50 text-green-400'
    case 'draft': return 'bg-yellow-900/50 text-yellow-400'
    case 'retired': return 'bg-gray-600 text-gray-400'
    default: return 'bg-gray-700 text-gray-300'
  }
}

export function dnaCardStatusColor(status: string): string {
  switch (status) {
    case 'active': return 'bg-green-900/50 text-green-400'
    case 'draft': return 'bg-yellow-900/50 text-yellow-400'
    case 'retired': return 'bg-gray-700 text-gray-300'
    default: return 'bg-gray-700 text-gray-300'
  }
}

export function dnaGoalStatusColor(status: string): string {
  switch (status) {
    case 'active': return 'bg-green-900/50 text-green-400'
    case 'met': return 'bg-blue-900/50 text-blue-400'
    case 'missed': return 'bg-red-900/50 text-red-400'
    case 'retired': return 'bg-gray-700 text-gray-300'
    default: return 'bg-gray-700 text-gray-300'
  }
}

export function dnaRuleStatusColor(status: string): string {
  switch (status) {
    case 'active': return 'bg-green-900/50 text-green-400'
    case 'superseded': return 'bg-gray-700 text-gray-300'
    case 'lapsed': return 'bg-yellow-900/50 text-yellow-400'
    default: return 'bg-gray-700 text-gray-300'
  }
}

export function nodeStatusColor(status: string): string {
  switch (status) {
    case 'trusted': return 'bg-green-900/50 text-green-400'
    case 'revoked': return 'bg-red-900/50 text-red-400'
    default: return 'bg-gray-700 text-gray-300'
  }
}

export function groupStatusColor(status: string): string {
  switch (status) {
    case 'active': return 'bg-green-900/50 text-green-400'
    case 'archived': return 'bg-gray-600 text-gray-400'
    default: return 'bg-gray-700 text-gray-300'
  }
}

export function rbacRoleColor(role: string): string {
  switch (role) {
    case 'admin': return 'bg-red-900/50 text-red-400'
    case 'owner': return 'bg-yellow-900/50 text-yellow-400'
    case 'viewer': return 'bg-gray-600 text-gray-400'
    default: return 'bg-blue-900/50 text-blue-400'
  }
}

