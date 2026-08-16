# SPEC-08 — Org Model & RBAC

Source: PLAN.md §5.

## Members and roles

- **ORG-001** — `humans` (identity, RBAC role) and `agents` (identity files, scopes) share
  one member namespace; the task board, asks, groups, and lineage reference members.
- **ORG-002** — Human RBAC roles: `admin` (everything), `owner` (one or more DNA domains +
  their agents), `member` (work, propose DNA, spawn within policy), `viewer` (read-only in
  full). Auth starts as local accounts; SSO/OIDC later (CFG-020).
- **ORG-020** — **Viewer total no-write surface**: a viewer is never an ask target, assignee,
  deputy, sponsor, lead, owner, group Leader, or originator — proposing or amending DNA,
  filing asks, creating board tasks or initiatives, and spawning are all refused at write;
  these are facets of one surface, not a checklist. Viewers receive the org-stall broadcast
  read-only (ASK-058).
- **ORG-021** — Mid-life role changes maintain the invariants through the SPEC-09 walks;
  write-time guards and the walks are one mechanism in two tenses.
- **ORG-022** — Rehire is a new member, never a resurrection: deactivation is terminal for
  identity; `decided_by` references, audit history, and spend attribution stay pinned to the
  departed identity (until STG-030 pseudonymizes the link); email addresses are not reused.

## Asks (org-level view; normative detail in SPEC-14)

- **ORG-025** — Approvals, questions, assignments, and spawn requests are all Asks routed to
  a member with payload, deadline, and escalation policy; humans answer in the console (later
  IM/email digests); agents answer via their session worker; v1 approvals map to asks of kind
  `approval`.

## Task board

- **ORG-030** — To-dos come from run results, playbook nodes, or any member with a write
  surface; a viewer reads the board, never writes it.
- **ORG-031** — Tasks are assignable to humans or agents, never viewers, and the assignee
  must be active at write; suspension freezes an assignee's tasks (resume re-arms them);
  retire/offboard walks return them (OFB-010, CLC-020).
- **ORG-032** — Tasks are groupable under initiatives (SPEC-10) and visible org-wide within
  access scopes.
- **ORG-033** — An exhausted assignment ask returns the task to the board pool (ASK-057) —
  the board is an assignment's fallback surface, never a hang.

## Groups

- **ORG-040** — Groups mix humans and agents; a local agent may act as Leader for
  execution routing. A group ends by a named act, never abandonment: archival — an admin
  act — leaves the row as read-only history (routing dead, no new members admitted), the
  name reusable among non-archived groups (DAT-122, DNC-060's idiom at the team axis); no
  bare delete exists.
- **ORG-041** — Group membership derives from live state: the offboard walk clears a departed
  human, the retire walk a retired agent — execution routing never addresses a dead
  identity.
- **ORG-042** — The Leader post is guarded at write: a viewer human or any non-active member
  is refused at set (routing addresses the Leader; the Leader must be answerable); an
  ephemeral worker is refused by the mortality pin.
- **ORG-043** — Leadership re-points on departure, retirement, or demotion inside the walks —
  a named successor, else the group's routing degrades to an admin ask; no routing surface
  outlives its holder un-asked.

## Accountability invariant

- **ORG-050** — Every agent row carries `owner_human_id`; spawned workers carry
  `spawned_by`; the chain must terminate at a human.
- **ORG-051** — Ownership is derived, not configured: a persistent hire's first owner is the
  gate's accepting human at activation (SPW-046); an ephemeral's is the first human up the
  `spawned_by` line, pinned at spawn; the SPEC-09/12 walks carry it from there.

## Deputies

- **ORG-060** — Every ask to a human carries the chain member → deputy → domain owner →
  admin (ASK-060); deputies are humans only.
- **ORG-061** — Refused at write: an agent deputy (standing approval authority for agents is
  what delegated rules exist for, ASK-090), a self-deputy, a viewer deputy, and deputy cycles
  (the walk's visited-set ends a mis-configured cycle at the hop, not the walk).
