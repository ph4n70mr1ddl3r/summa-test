import { describe, it, expect } from 'vitest'
import { formatDate, tierColor, spawnStatusColor, triggerStatusColor, triggerCriticalityColor, boardTaskStatusColor, roleTemplateStatusColor, dnaCardStatusColor, dnaGoalStatusColor, dnaRuleStatusColor, nodeStatusColor, groupStatusColor, rbacRoleColor, initiativeStatusColor, runStatusColor, agentStatusColor, askStatusColor } from './formatting'
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
  })

  it('formats date-only when requested', () => {
    const result = formatDate(1700000000, { dateOnly: true })
    expect(result).not.toBe('?')
  })
})

describe('tierColor', () => {
  it('returns red for critical', () => {
    expect(tierColor('critical' as AskTier)).toContain('red')
  })

  it('returns yellow for standard', () => {
    expect(tierColor('standard' as AskTier)).toContain('yellow')
  })

  it('returns gray for bulk', () => {
    expect(tierColor('bulk' as AskTier)).toContain('gray')
  })
})

describe('spawnStatusColor', () => {
  it('returns yellow for requested', () => {
    expect(spawnStatusColor('requested' as SpawnStatus)).toContain('yellow')
  })

  it('returns green for approved', () => {
    expect(spawnStatusColor('approved' as SpawnStatus)).toContain('green')
  })

  it('returns orange for halted', () => {
    expect(spawnStatusColor('halted' as SpawnStatus)).toContain('orange')
  })

  it('returns gray for expired', () => {
    expect(spawnStatusColor('expired' as SpawnStatus)).toContain('gray')
  })

  it('returns gray for archived', () => {
    expect(spawnStatusColor('archived' as SpawnStatus)).toContain('gray')
  })
})

describe('triggerCriticalityColor', () => {
  it('returns red for critical', () => {
    expect(triggerCriticalityColor('critical')).toContain('red')
  })

  it('returns yellow for standard', () => {
    expect(triggerCriticalityColor('standard')).toContain('yellow')
  })
})

describe('triggerStatusColor', () => {
  it('returns green for active', () => {
    expect(triggerStatusColor('active' as TriggerStatus)).toContain('green')
  })

  it('returns yellow for paused', () => {
    expect(triggerStatusColor('paused' as TriggerStatus)).toContain('yellow')
  })

  it('returns gray for archived', () => {
    expect(triggerStatusColor('archived' as TriggerStatus)).toContain('gray')
  })
})

describe('boardTaskStatusColor', () => {
  it('returns green for done', () => {
    expect(boardTaskStatusColor('done' as BoardTaskStatus)).toContain('green')
  })

  it('returns blue for in_progress', () => {
    expect(boardTaskStatusColor('in_progress' as BoardTaskStatus)).toContain('blue')
  })

  it('returns red for cancelled', () => {
    expect(boardTaskStatusColor('cancelled' as BoardTaskStatus)).toContain('red')
  })

  it('returns yellow for open', () => {
    expect(boardTaskStatusColor('open' as BoardTaskStatus)).toContain('yellow')
  })
})

describe('roleTemplateStatusColor', () => {
  it('returns green for active', () => {
    expect(roleTemplateStatusColor('active' as RoleTemplateStatus)).toContain('green')
  })

  it('returns yellow for draft', () => {
    expect(roleTemplateStatusColor('draft' as RoleTemplateStatus)).toContain('yellow')
  })

  it('returns gray for retired', () => {
    expect(roleTemplateStatusColor('retired' as RoleTemplateStatus)).toContain('gray')
  })
})

describe('dnaCardStatusColor', () => {
  it('returns green for active', () => {
    expect(dnaCardStatusColor('active' as DnaCardStatus)).toContain('green')
  })

  it('returns yellow for draft', () => {
    expect(dnaCardStatusColor('draft' as DnaCardStatus)).toContain('yellow')
  })

  it('returns gray for retired', () => {
    expect(dnaCardStatusColor('retired' as DnaCardStatus)).toContain('gray')
  })
})

describe('dnaGoalStatusColor', () => {
  it('returns green for active', () => {
    expect(dnaGoalStatusColor('active' as DnaGoalStatus)).toContain('green')
  })

  it('returns blue for met', () => {
    expect(dnaGoalStatusColor('met' as DnaGoalStatus)).toContain('blue')
  })

  it('returns red for missed', () => {
    expect(dnaGoalStatusColor('missed' as DnaGoalStatus)).toContain('red')
  })

  it('returns gray for retired', () => {
    expect(dnaGoalStatusColor('retired' as DnaGoalStatus)).toContain('gray')
  })
})

describe('dnaRuleStatusColor', () => {
  it('returns green for active', () => {
    expect(dnaRuleStatusColor('active' as DnaRuleStatus)).toContain('green')
  })

  it('returns gray for superseded', () => {
    expect(dnaRuleStatusColor('superseded' as DnaRuleStatus)).toContain('gray')
  })

  it('returns yellow for lapsed', () => {
    expect(dnaRuleStatusColor('lapsed' as DnaRuleStatus)).toContain('yellow')
  })
})

describe('nodeStatusColor', () => {
  it('returns green for trusted', () => {
    expect(nodeStatusColor('trusted' as NodeStatus)).toContain('green')
  })

  it('returns red for revoked', () => {
    expect(nodeStatusColor('revoked' as NodeStatus)).toContain('red')
  })
})

describe('groupStatusColor', () => {
  it('returns green for active', () => {
    expect(groupStatusColor('active' as GroupStatus)).toContain('green')
  })

  it('returns gray for archived', () => {
    expect(groupStatusColor('archived' as GroupStatus)).toContain('gray')
  })
})

describe('rbacRoleColor', () => {
  it('returns red for admin', () => {
    expect(rbacRoleColor('admin' as RbacRole)).toContain('red')
  })

  it('returns yellow for owner', () => {
    expect(rbacRoleColor('owner' as RbacRole)).toContain('yellow')
  })

  it('returns gray for viewer', () => {
    expect(rbacRoleColor('viewer' as RbacRole)).toContain('gray')
  })
})

describe('initiativeStatusColor', () => {
  it('returns blue for proposed', () => {
    expect(initiativeStatusColor('proposed' as InitiativeStatus)).toContain('blue')
  })

  it('returns green for active', () => {
    expect(initiativeStatusColor('active' as InitiativeStatus)).toContain('green')
  })

  it('returns yellow for paused', () => {
    expect(initiativeStatusColor('paused' as InitiativeStatus)).toContain('yellow')
  })

  it('returns gray for closed', () => {
    expect(initiativeStatusColor('closed' as InitiativeStatus)).toContain('gray')
  })
})

describe('runStatusColor', () => {
  it('returns green for completed', () => {
    expect(runStatusColor('completed' as RunStatus)).toContain('green')
  })

  it('returns blue for running', () => {
    expect(runStatusColor('running' as RunStatus)).toContain('blue')
  })

  it('returns red for failed', () => {
    expect(runStatusColor('failed' as RunStatus)).toContain('red')
  })

  it('returns yellow for queued', () => {
    expect(runStatusColor('queued' as RunStatus)).toContain('yellow')
  })
})

describe('agentStatusColor', () => {
  it('returns green for active', () => {
    expect(agentStatusColor('active' as AgentStatus)).toContain('green')
  })

  it('returns blue for requested', () => {
    expect(agentStatusColor('requested' as AgentStatus)).toContain('blue')
  })

  it('returns yellow for suspended', () => {
    expect(agentStatusColor('suspended' as AgentStatus)).toContain('yellow')
  })

  it('returns orange for retiring', () => {
    expect(agentStatusColor('retiring' as AgentStatus)).toContain('orange')
  })

  it('returns gray for archived', () => {
    expect(agentStatusColor('archived' as AgentStatus)).toContain('gray')
  })
})

describe('askStatusColor', () => {
  it('returns yellow for pending', () => {
    expect(askStatusColor('pending' as AskStatus)).toContain('yellow')
  })

  it('returns green for answered', () => {
    expect(askStatusColor('answered' as AskStatus)).toContain('green')
  })

  it('returns red for expired', () => {
    expect(askStatusColor('expired' as AskStatus)).toContain('red')
  })

  it('returns gray for withdrawn', () => {
    expect(askStatusColor('withdrawn' as AskStatus)).toContain('gray')
  })
})
