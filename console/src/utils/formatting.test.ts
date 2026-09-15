import { describe, it, expect } from 'vitest'
import { formatDate, tierColor, spawnStatusColor, triggerStatusColor, boardTaskStatusColor, roleTemplateStatusColor, dnaCardStatusColor, dnaGoalStatusColor, dnaRuleStatusColor, nodeStatusColor, groupStatusColor, rbacRoleColor, initiativeStatusColor, runStatusColor, agentStatusColor, askStatusColor } from './formatting'

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
    expect(tierColor('critical')).toContain('red')
  })

  it('returns yellow for standard', () => {
    expect(tierColor('standard')).toContain('yellow')
  })

  it('returns gray for bulk', () => {
    expect(tierColor('bulk')).toContain('gray')
  })

  it('defaults to gray', () => {
    expect(tierColor('unknown')).toContain('gray')
  })
})

describe('spawnStatusColor', () => {
  it('returns yellow for requested', () => {
    expect(spawnStatusColor('requested')).toContain('yellow')
  })

  it('returns green for approved', () => {
    expect(spawnStatusColor('approved')).toContain('green')
  })

  it('returns red for denied', () => {
    expect(spawnStatusColor('denied')).toContain('red')
  })

  it('defaults to gray', () => {
    expect(spawnStatusColor('unknown')).toContain('gray')
  })
})

describe('triggerStatusColor', () => {
  it('returns green for active', () => {
    expect(triggerStatusColor('active')).toContain('green')
  })

  it('returns yellow for paused', () => {
    expect(triggerStatusColor('paused')).toContain('yellow')
  })

  it('returns gray for archived', () => {
    expect(triggerStatusColor('archived')).toContain('gray')
  })

  it('defaults to gray', () => {
    expect(triggerStatusColor('unknown')).toContain('gray')
  })
})

describe('boardTaskStatusColor', () => {
  it('returns green for done', () => {
    expect(boardTaskStatusColor('done')).toContain('green')
  })

  it('returns blue for in_progress', () => {
    expect(boardTaskStatusColor('in_progress')).toContain('blue')
  })

  it('returns red for cancelled', () => {
    expect(boardTaskStatusColor('cancelled')).toContain('red')
  })

  it('returns yellow for open', () => {
    expect(boardTaskStatusColor('open')).toContain('yellow')
  })

  it('defaults to gray', () => {
    expect(boardTaskStatusColor('unknown')).toContain('gray')
  })
})

describe('roleTemplateStatusColor', () => {
  it('returns green for active', () => {
    expect(roleTemplateStatusColor('active')).toContain('green')
  })

  it('returns yellow for draft', () => {
    expect(roleTemplateStatusColor('draft')).toContain('yellow')
  })

  it('returns gray for retired', () => {
    expect(roleTemplateStatusColor('retired')).toContain('gray')
  })

  it('defaults to gray', () => {
    expect(roleTemplateStatusColor('unknown')).toContain('gray')
  })
})

describe('dnaCardStatusColor', () => {
  it('returns green for active', () => {
    expect(dnaCardStatusColor('active')).toContain('green')
  })

  it('returns yellow for draft', () => {
    expect(dnaCardStatusColor('draft')).toContain('yellow')
  })

  it('returns gray for retired', () => {
    expect(dnaCardStatusColor('retired')).toContain('gray')
  })

  it('defaults to gray', () => {
    expect(dnaCardStatusColor('unknown')).toContain('gray')
  })
})

describe('dnaGoalStatusColor', () => {
  it('returns green for active', () => {
    expect(dnaGoalStatusColor('active')).toContain('green')
  })

  it('returns blue for met', () => {
    expect(dnaGoalStatusColor('met')).toContain('blue')
  })

  it('returns red for missed', () => {
    expect(dnaGoalStatusColor('missed')).toContain('red')
  })

  it('returns gray for retired', () => {
    expect(dnaGoalStatusColor('retired')).toContain('gray')
  })

  it('defaults to gray', () => {
    expect(dnaGoalStatusColor('unknown')).toContain('gray')
  })
})

describe('dnaRuleStatusColor', () => {
  it('returns green for active', () => {
    expect(dnaRuleStatusColor('active')).toContain('green')
  })

  it('returns gray for superseded', () => {
    expect(dnaRuleStatusColor('superseded')).toContain('gray')
  })

  it('returns yellow for lapsed', () => {
    expect(dnaRuleStatusColor('lapsed')).toContain('yellow')
  })

  it('defaults to gray', () => {
    expect(dnaRuleStatusColor('unknown')).toContain('gray')
  })
})

describe('nodeStatusColor', () => {
  it('returns green for trusted', () => {
    expect(nodeStatusColor('trusted')).toContain('green')
  })

  it('returns red for revoked', () => {
    expect(nodeStatusColor('revoked')).toContain('red')
  })

  it('defaults to gray', () => {
    expect(nodeStatusColor('unknown')).toContain('gray')
  })
})

describe('groupStatusColor', () => {
  it('returns green for active', () => {
    expect(groupStatusColor('active')).toContain('green')
  })

  it('returns gray for archived', () => {
    expect(groupStatusColor('archived')).toContain('gray')
  })

  it('defaults to gray', () => {
    expect(groupStatusColor('unknown')).toContain('gray')
  })
})

describe('rbacRoleColor', () => {
  it('returns red for admin', () => {
    expect(rbacRoleColor('admin')).toContain('red')
  })

  it('returns yellow for owner', () => {
    expect(rbacRoleColor('owner')).toContain('yellow')
  })

  it('returns gray for viewer', () => {
    expect(rbacRoleColor('viewer')).toContain('gray')
  })

  it('returns blue for unknown', () => {
    expect(rbacRoleColor('unknown')).toContain('blue')
  })
})

describe('initiativeStatusColor', () => {
  it('returns blue for proposed', () => {
    expect(initiativeStatusColor('proposed')).toContain('blue')
  })

  it('returns green for active', () => {
    expect(initiativeStatusColor('active')).toContain('green')
  })

  it('returns yellow for paused', () => {
    expect(initiativeStatusColor('paused')).toContain('yellow')
  })

  it('returns gray for closed', () => {
    expect(initiativeStatusColor('closed')).toContain('gray')
  })

  it('defaults to gray', () => {
    expect(initiativeStatusColor('unknown')).toContain('gray')
  })
})

describe('runStatusColor', () => {
  it('returns green for completed', () => {
    expect(runStatusColor('completed')).toContain('green')
  })

  it('returns blue for running', () => {
    expect(runStatusColor('running')).toContain('blue')
  })

  it('returns red for failed', () => {
    expect(runStatusColor('failed')).toContain('red')
  })

  it('returns yellow for queued', () => {
    expect(runStatusColor('queued')).toContain('yellow')
  })

  it('defaults to gray', () => {
    expect(runStatusColor('unknown')).toContain('gray')
  })
})

describe('agentStatusColor', () => {
  it('returns green for active', () => {
    expect(agentStatusColor('active')).toContain('green')
  })

  it('returns blue for requested', () => {
    expect(agentStatusColor('requested')).toContain('blue')
  })

  it('returns yellow for suspended', () => {
    expect(agentStatusColor('suspended')).toContain('yellow')
  })

  it('returns orange for retiring', () => {
    expect(agentStatusColor('retiring')).toContain('orange')
  })

  it('returns gray for archived', () => {
    expect(agentStatusColor('archived')).toContain('gray')
  })

  it('defaults to gray', () => {
    expect(agentStatusColor('unknown')).toContain('gray')
  })
})

describe('askStatusColor', () => {
  it('returns yellow for pending', () => {
    expect(askStatusColor('pending')).toContain('yellow')
  })

  it('returns green for answered', () => {
    expect(askStatusColor('answered')).toContain('green')
  })

  it('returns red for expired', () => {
    expect(askStatusColor('expired')).toContain('red')
  })

  it('returns gray for withdrawn', () => {
    expect(askStatusColor('withdrawn')).toContain('gray')
  })

  it('defaults to gray', () => {
    expect(askStatusColor('unknown')).toContain('gray')
  })
})
