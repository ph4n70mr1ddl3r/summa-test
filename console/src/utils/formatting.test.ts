import { describe, it, expect } from 'vitest'
import { formatDate, tierColor, spawnStatusColor, triggerStatusColor, triggerCriticalityColor, boardTaskStatusColor, roleTemplateStatusColor, dnaCardStatusColor, dnaGoalStatusColor, dnaRuleStatusColor, nodeStatusColor, groupStatusColor, rbacRoleColor, initiativeStatusColor, runStatusColor, agentStatusColor, askStatusColor, domainAccessColor, proposalStatusColor, proposalKindColor, countParticipants, truncateSnippet } from './formatting'
import type { AskTier, SpawnStatus, TriggerStatus, BoardTaskStatus, RoleTemplateStatus, DnaCardStatus, DnaGoalStatus, DnaRuleStatus, NodeStatus, GroupStatus, InitiativeStatus, RunStatus, AgentStatus, AskStatus } from '../services/api'
import type { RbacRole } from '../services/api'

describe('formatDate', () => {
  it('returns ? for null', () => {
    expect(formatDate(null)).toBe('?')
  })

  it('returns ? for undefined', () => {
    expect(formatDate(undefined)).toBe('?')
  })

  it('formats epoch seconds as local datetime', () => {
    const d = formatDate(1700000000)
    expect(d).not.toBe('?')
    expect(d.length).toBeGreaterThan(0)
    expect(d).toContain('UTC')
  })

  it('formats date-only when requested', () => {
    const result = formatDate(1700000000, { dateOnly: true })
    expect(result).not.toBe('?')
    expect(result).not.toContain('UTC')
  })
})

describe('tierColor', () => {
  it('returns exact classes for critical', () => {
    expect(tierColor('critical' as AskTier)).toBe('text-red-400 bg-red-900/30 border-red-700')
  })

  it('returns exact classes for standard', () => {
    expect(tierColor('standard' as AskTier)).toBe('text-yellow-400 bg-yellow-900/30 border-yellow-700')
  })

  it('returns exact classes for bulk', () => {
    expect(tierColor('bulk' as AskTier)).toBe('text-gray-400 bg-gray-800 border-gray-600')
  })
})

describe('spawnStatusColor', () => {
  it('returns exact classes for requested', () => {
    expect(spawnStatusColor('requested' as SpawnStatus)).toBe('bg-yellow-900/50 text-yellow-400')
  })

  it('returns exact classes for approved', () => {
    expect(spawnStatusColor('approved' as SpawnStatus)).toBe('bg-green-900/50 text-green-400')
  })

  it('returns exact classes for halted', () => {
    expect(spawnStatusColor('halted' as SpawnStatus)).toBe('bg-orange-900/50 text-orange-400')
  })

  it('returns exact classes for expired', () => {
    expect(spawnStatusColor('expired' as SpawnStatus)).toBe('bg-gray-600 text-gray-400')
  })

  it('returns exact classes for archived', () => {
    expect(spawnStatusColor('archived' as SpawnStatus)).toBe('bg-gray-600 text-gray-400')
  })
})

describe('triggerCriticalityColor', () => {
  it('returns exact classes for critical', () => {
    expect(triggerCriticalityColor('critical')).toBe('text-red-400 bg-red-900/30 border-red-700')
  })

  it('returns exact classes for standard', () => {
    expect(triggerCriticalityColor('standard')).toBe('text-yellow-400 bg-yellow-900/30 border-yellow-700')
  })
})

describe('triggerStatusColor', () => {
  it('returns exact classes for active', () => {
    expect(triggerStatusColor('active' as TriggerStatus)).toBe('bg-green-900/50 text-green-400')
  })

  it('returns exact classes for paused', () => {
    expect(triggerStatusColor('paused' as TriggerStatus)).toBe('bg-yellow-900/50 text-yellow-400')
  })

  it('returns exact classes for archived', () => {
    expect(triggerStatusColor('archived' as TriggerStatus)).toBe('bg-gray-600 text-gray-400')
  })
})

describe('boardTaskStatusColor', () => {
  it('returns exact classes for done', () => {
    expect(boardTaskStatusColor('done' as BoardTaskStatus)).toBe('bg-green-900/50 text-green-400')
  })

  it('returns exact classes for in_progress', () => {
    expect(boardTaskStatusColor('in_progress' as BoardTaskStatus)).toBe('bg-blue-900/50 text-blue-400')
  })

  it('returns exact classes for cancelled', () => {
    expect(boardTaskStatusColor('cancelled' as BoardTaskStatus)).toBe('bg-red-900/50 text-red-400')
  })

  it('returns exact classes for open', () => {
    expect(boardTaskStatusColor('open' as BoardTaskStatus)).toBe('bg-yellow-900/50 text-yellow-400')
  })
})

describe('roleTemplateStatusColor', () => {
  it('returns exact classes for active', () => {
    expect(roleTemplateStatusColor('active' as RoleTemplateStatus)).toBe('bg-green-900/50 text-green-400')
  })

  it('returns exact classes for draft', () => {
    expect(roleTemplateStatusColor('draft' as RoleTemplateStatus)).toBe('bg-yellow-900/50 text-yellow-400')
  })

  it('returns exact classes for retired', () => {
    expect(roleTemplateStatusColor('retired' as RoleTemplateStatus)).toBe('bg-gray-600 text-gray-400')
  })
})

describe('dnaCardStatusColor', () => {
  it('returns exact classes for active', () => {
    expect(dnaCardStatusColor('active' as DnaCardStatus)).toBe('bg-green-900/50 text-green-400')
  })

  it('returns exact classes for draft', () => {
    expect(dnaCardStatusColor('draft' as DnaCardStatus)).toBe('bg-yellow-900/50 text-yellow-400')
  })

  it('returns exact classes for retired', () => {
    expect(dnaCardStatusColor('retired' as DnaCardStatus)).toBe('bg-gray-600 text-gray-400')
  })
})

describe('dnaGoalStatusColor', () => {
  it('returns exact classes for active', () => {
    expect(dnaGoalStatusColor('active' as DnaGoalStatus)).toBe('bg-green-900/50 text-green-400')
  })

  it('returns exact classes for met', () => {
    expect(dnaGoalStatusColor('met' as DnaGoalStatus)).toBe('bg-blue-900/50 text-blue-400')
  })

  it('returns exact classes for missed', () => {
    expect(dnaGoalStatusColor('missed' as DnaGoalStatus)).toBe('bg-red-900/50 text-red-400')
  })

  it('returns exact classes for retired', () => {
    expect(dnaGoalStatusColor('retired' as DnaGoalStatus)).toBe('bg-gray-600 text-gray-400')
  })
})

describe('dnaRuleStatusColor', () => {
  it('returns exact classes for active', () => {
    expect(dnaRuleStatusColor('active' as DnaRuleStatus)).toBe('bg-green-900/50 text-green-400')
  })

  it('returns exact classes for superseded', () => {
    expect(dnaRuleStatusColor('superseded' as DnaRuleStatus)).toBe('bg-gray-600 text-gray-400')
  })

  it('returns exact classes for lapsed', () => {
    expect(dnaRuleStatusColor('lapsed' as DnaRuleStatus)).toBe('bg-yellow-900/50 text-yellow-400')
  })
})

describe('nodeStatusColor', () => {
  it('returns exact classes for trusted', () => {
    expect(nodeStatusColor('trusted' as NodeStatus)).toBe('bg-green-900/50 text-green-400')
  })

  it('returns exact classes for revoked', () => {
    expect(nodeStatusColor('revoked' as NodeStatus)).toBe('bg-red-900/50 text-red-400')
  })
})

describe('groupStatusColor', () => {
  it('returns exact classes for active', () => {
    expect(groupStatusColor('active' as GroupStatus)).toBe('bg-green-900/50 text-green-400')
  })

  it('returns exact classes for archived', () => {
    expect(groupStatusColor('archived' as GroupStatus)).toBe('bg-gray-600 text-gray-400')
  })
})

describe('rbacRoleColor', () => {
  it('returns exact classes for admin', () => {
    expect(rbacRoleColor('admin' as RbacRole)).toBe('bg-red-900/50 text-red-400')
  })

  it('returns exact classes for owner', () => {
    expect(rbacRoleColor('owner' as RbacRole)).toBe('bg-yellow-900/50 text-yellow-400')
  })

  it('returns exact classes for viewer', () => {
    expect(rbacRoleColor('viewer' as RbacRole)).toBe('bg-gray-600 text-gray-400')
  })

  it('returns exact classes for member', () => {
    expect(rbacRoleColor('member' as RbacRole)).toBe('bg-blue-900/50 text-blue-400')
  })
})

describe('initiativeStatusColor', () => {
  it('returns exact classes for proposed', () => {
    expect(initiativeStatusColor('proposed' as InitiativeStatus)).toBe('bg-blue-900/50 text-blue-400')
  })

  it('returns exact classes for active', () => {
    expect(initiativeStatusColor('active' as InitiativeStatus)).toBe('bg-green-900/50 text-green-400')
  })

  it('returns exact classes for paused', () => {
    expect(initiativeStatusColor('paused' as InitiativeStatus)).toBe('bg-yellow-900/50 text-yellow-400')
  })

  it('returns exact classes for closed', () => {
    expect(initiativeStatusColor('closed' as InitiativeStatus)).toBe('bg-gray-600 text-gray-400')
  })
})

describe('runStatusColor', () => {
  it('returns exact classes for completed', () => {
    expect(runStatusColor('completed' as RunStatus)).toBe('bg-green-900/50 text-green-400')
  })

  it('returns exact classes for running', () => {
    expect(runStatusColor('running' as RunStatus)).toBe('bg-blue-900/50 text-blue-400')
  })

  it('returns exact classes for failed', () => {
    expect(runStatusColor('failed' as RunStatus)).toBe('bg-red-900/50 text-red-400')
  })

  it('returns exact classes for queued', () => {
    expect(runStatusColor('queued' as RunStatus)).toBe('bg-yellow-900/50 text-yellow-400')
  })

  it('returns exact classes for cancelled', () => {
    expect(runStatusColor('cancelled' as RunStatus)).toBe('bg-gray-600 text-gray-400')
  })

  it('returns exact classes for suspended', () => {
    expect(runStatusColor('suspended' as RunStatus)).toBe('bg-orange-900/50 text-orange-400')
  })
})

describe('agentStatusColor', () => {
  it('returns exact classes for active', () => {
    expect(agentStatusColor('active' as AgentStatus)).toBe('bg-green-900/50 text-green-400')
  })

  it('returns exact classes for requested', () => {
    expect(agentStatusColor('requested' as AgentStatus)).toBe('bg-blue-900/50 text-blue-400')
  })

  it('returns exact classes for suspended', () => {
    expect(agentStatusColor('suspended' as AgentStatus)).toBe('bg-yellow-900/50 text-yellow-400')
  })

  it('returns exact classes for retiring', () => {
    expect(agentStatusColor('retiring' as AgentStatus)).toBe('bg-orange-900/50 text-orange-400')
  })

  it('returns exact classes for archived', () => {
    expect(agentStatusColor('archived' as AgentStatus)).toBe('bg-gray-600 text-gray-400')
  })
})

describe('askStatusColor', () => {
  it('returns exact classes for pending', () => {
    expect(askStatusColor('pending' as AskStatus)).toBe('bg-yellow-900/50 text-yellow-400')
  })

  it('returns exact classes for answered', () => {
    expect(askStatusColor('answered' as AskStatus)).toBe('bg-green-900/50 text-green-400')
  })

  it('returns exact classes for expired', () => {
    expect(askStatusColor('expired' as AskStatus)).toBe('bg-red-900/50 text-red-400')
  })

  it('returns exact classes for withdrawn', () => {
    expect(askStatusColor('withdrawn' as AskStatus)).toBe('bg-gray-600 text-gray-400')
  })
})

describe('domainAccessColor', () => {
  it('returns exact classes for public', () => {
    expect(domainAccessColor('public')).toBe('bg-green-900/50 text-green-400')
  })

  it('returns exact classes for domain', () => {
    expect(domainAccessColor('domain')).toBe('bg-blue-900/50 text-blue-400')
  })

  it('returns exact classes for named', () => {
    expect(domainAccessColor('named')).toBe('bg-yellow-900/50 text-yellow-400')
  })
})

describe('proposalStatusColor', () => {
  it('returns exact classes for open', () => {
    expect(proposalStatusColor('open')).toBe('bg-blue-900/50 text-blue-400')
  })

  it('returns exact classes for published', () => {
    expect(proposalStatusColor('published')).toBe('bg-green-900/50 text-green-400')
  })

  it('returns exact classes for rejected', () => {
    expect(proposalStatusColor('rejected')).toBe('bg-red-900/50 text-red-400')
  })

  it('returns exact classes for withdrawn', () => {
    expect(proposalStatusColor('withdrawn')).toBe('bg-gray-600 text-gray-400')
  })
})

describe('proposalKindColor', () => {
  it('returns exact classes for card', () => {
    expect(proposalKindColor('card')).toBe('bg-blue-900/50 text-blue-400')
  })

  it('returns exact classes for rule', () => {
    expect(proposalKindColor('rule')).toBe('bg-yellow-900/50 text-yellow-400')
  })

  it('returns exact classes for decision', () => {
    expect(proposalKindColor('decision')).toBe('bg-green-900/50 text-green-400')
  })

  it('returns exact classes for goal', () => {
    expect(proposalKindColor('goal')).toBe('bg-purple-900/50 text-purple-400')
  })

  it('returns exact classes for glossary', () => {
    expect(proposalKindColor('glossary')).toBe('bg-pink-900/50 text-pink-400')
  })

  it('returns exact classes for edit', () => {
    expect(proposalKindColor('edit')).toBe('bg-gray-700 text-gray-300')
  })
})

describe('countParticipants', () => {
  it('returns 0 for null', () => {
    expect(countParticipants(null)).toBe(0)
  })

  it('returns 0 for undefined', () => {
    expect(countParticipants(undefined)).toBe(0)
  })

  it('returns correct count for valid JSON array', () => {
    expect(countParticipants('["a","b","c"]')).toBe(3)
  })

  it('returns 0 for invalid JSON', () => {
    expect(countParticipants('not-json')).toBe(0)
  })

  it('returns 0 for non-array JSON', () => {
    expect(countParticipants('{"key":"value"}')).toBe(0)
  })
})

describe('truncateSnippet', () => {
  it('returns empty string when maxLen <= 0', () => {
    expect(truncateSnippet('hello', 0)).toBe('')
    expect(truncateSnippet('hello', -1)).toBe('')
  })

  it('returns original text when within limit', () => {
    expect(truncateSnippet('hello', 10)).toBe('hello')
  })

  it('truncates and appends ellipsis when over limit', () => {
    expect(truncateSnippet('hello world', 5)).toBe('hello...')
  })
})
