import type {
  AgentStatus,
  AskStatus,
  AskTier,
  SpawnStatus,
  InitiativeStatus,
  RunStatus,
  TriggerStatus,
  BoardTaskStatus,
  RoleTemplateStatus,
  DnaCardStatus,
  DnaGoalStatus,
  DnaRuleStatus,
  NodeStatus,
  GroupStatus,
} from '../services/api'

export function formatDate(epochSeconds: number | undefined | null, options?: { dateOnly?: boolean }): string {
  if (epochSeconds === null || epochSeconds === undefined) return '?'
  const fmt = new Intl.DateTimeFormat(undefined, {
    timeZone: 'UTC',
    year: 'numeric', month: '2-digit', day: '2-digit',
    ...(options?.dateOnly ? {} : { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
  })
  const base = fmt.format(new Date(epochSeconds * 1000))
  return options?.dateOnly ? base : `${base} UTC`
}

export function agentStatusColor(status: AgentStatus): string {
  switch (status) {
    case 'active': return 'bg-green-900/50 text-green-400'
    case 'requested': return 'bg-blue-900/50 text-blue-400'
    case 'suspended': return 'bg-yellow-900/50 text-yellow-400'
    case 'retiring': return 'bg-orange-900/50 text-orange-400'
    case 'archived': return 'bg-gray-600 text-gray-400'
    default: return 'bg-gray-700 text-gray-300'
  }
}

export function askStatusColor(status: AskStatus): string {
  switch (status) {
    case 'pending': return 'bg-yellow-900/50 text-yellow-400'
    case 'answered': return 'bg-green-900/50 text-green-400'
    case 'expired': return 'bg-red-900/50 text-red-400'
    case 'withdrawn': return 'bg-gray-600 text-gray-400'
    default: return 'bg-gray-700 text-gray-300'
  }
}

export function tierColor(tier: AskTier): string {
  switch (tier) {
    case 'critical': return 'text-red-400 bg-red-900/30 border-red-700'
    case 'standard': return 'text-yellow-400 bg-yellow-900/30 border-yellow-700'
    case 'bulk': return 'text-gray-400 bg-gray-800 border-gray-600'
    default: return 'text-gray-400 bg-gray-800 border-gray-600'
  }
}

export function spawnStatusColor(status: SpawnStatus): string {
  switch (status) {
    case 'requested': return 'bg-yellow-900/50 text-yellow-400'
    case 'approved': return 'bg-green-900/50 text-green-400'
    case 'denied': return 'bg-red-900/50 text-red-400'
    default: return 'bg-gray-700 text-gray-300'
  }
}

export function initiativeStatusColor(status: InitiativeStatus): string {
  switch (status) {
    case 'proposed': return 'bg-blue-900/50 text-blue-400'
    case 'active': return 'bg-green-900/50 text-green-400'
    case 'paused': return 'bg-yellow-900/50 text-yellow-400'
    case 'closed': return 'bg-gray-600 text-gray-400'
    default: return 'bg-gray-700 text-gray-300'
  }
}

export function runStatusColor(status: RunStatus): string {
  switch (status) {
    case 'completed': return 'bg-green-900/50 text-green-400'
    case 'running': return 'bg-blue-900/50 text-blue-400'
    case 'failed': return 'bg-red-900/50 text-red-400'
    case 'queued': return 'bg-yellow-900/50 text-yellow-400'
    case 'cancelled': return 'bg-gray-600 text-gray-400'
    case 'suspended': return 'bg-orange-900/50 text-orange-400'
    default: return 'bg-gray-700 text-gray-300'
  }
}

export function triggerStatusColor(status: TriggerStatus): string {
  switch (status) {
    case 'active': return 'bg-green-900/50 text-green-400'
    case 'paused': return 'bg-yellow-900/50 text-yellow-400'
    case 'archived': return 'bg-gray-600 text-gray-400'
    default: return 'bg-gray-700 text-gray-300'
  }
}

export function boardTaskStatusColor(status: BoardTaskStatus): string {
  switch (status) {
    case 'done': return 'bg-green-900/50 text-green-400'
    case 'in_progress': return 'bg-blue-900/50 text-blue-400'
    case 'cancelled': return 'bg-red-900/50 text-red-400'
    case 'open': return 'bg-yellow-900/50 text-yellow-400'
    default: return 'bg-gray-700 text-gray-300'
  }
}

export function roleTemplateStatusColor(status: RoleTemplateStatus): string {
  switch (status) {
    case 'active': return 'bg-green-900/50 text-green-400'
    case 'draft': return 'bg-yellow-900/50 text-yellow-400'
    case 'retired': return 'bg-gray-600 text-gray-400'
    default: return 'bg-gray-700 text-gray-300'
  }
}

export function dnaCardStatusColor(status: DnaCardStatus): string {
  switch (status) {
    case 'active': return 'bg-green-900/50 text-green-400'
    case 'draft': return 'bg-yellow-900/50 text-yellow-400'
    case 'retired': return 'bg-gray-700 text-gray-300'
    default: return 'bg-gray-700 text-gray-300'
  }
}

export function dnaGoalStatusColor(status: DnaGoalStatus): string {
  switch (status) {
    case 'active': return 'bg-green-900/50 text-green-400'
    case 'met': return 'bg-blue-900/50 text-blue-400'
    case 'missed': return 'bg-red-900/50 text-red-400'
    case 'retired': return 'bg-gray-700 text-gray-300'
    default: return 'bg-gray-700 text-gray-300'
  }
}

export function dnaRuleStatusColor(status: DnaRuleStatus): string {
  switch (status) {
    case 'active': return 'bg-green-900/50 text-green-400'
    case 'superseded': return 'bg-gray-700 text-gray-300'
    case 'lapsed': return 'bg-yellow-900/50 text-yellow-400'
    default: return 'bg-gray-700 text-gray-300'
  }
}

export function nodeStatusColor(status: NodeStatus): string {
  switch (status) {
    case 'trusted': return 'bg-green-900/50 text-green-400'
    case 'revoked': return 'bg-red-900/50 text-red-400'
    default: return 'bg-gray-700 text-gray-300'
  }
}

export function groupStatusColor(status: GroupStatus): string {
  switch (status) {
    case 'active': return 'bg-green-900/50 text-green-400'
    case 'archived': return 'bg-gray-700 text-gray-300'
    default: return 'bg-gray-700 text-gray-300'
  }
}

export type RbacRole = 'admin' | 'owner' | 'member' | 'viewer'
export function rbacRoleColor(role: RbacRole): string {
  switch (role) {
    case 'admin': return 'bg-red-900/50 text-red-400'
    case 'owner': return 'bg-yellow-900/50 text-yellow-400'
    case 'viewer': return 'bg-gray-600 text-gray-400'
    default: return 'bg-gray-700 text-gray-300'
  }
}

export function countParticipants(participantsJson: string | null | undefined): number {
  try {
    const p = JSON.parse(participantsJson ?? '[]')
    return Array.isArray(p) ? p.length : 0
  } catch {
    return 0
  }
}

export function truncateSnippet(text: string, maxLen: number): string {
  if (maxLen <= 0) return ''
  return text.length > maxLen ? text.slice(0, maxLen) + '...' : text
}

