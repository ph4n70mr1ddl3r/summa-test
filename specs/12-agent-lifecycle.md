# SPEC-12 — Agent Lifecycle

Source: PLAN.md §6.3, §6.4. The member type this module governs is the *agent* (formerly
"Coworker", renamed PLAN v2.44); the CLC prefix is retained — requirement IDs are stable
forever.

## Status machine

- **CLC-001** — Persistent status enum: `requested | active | suspended | retiring |
  archived`; ephemeral lifecycle maps 1:1: spawned→requested, running→active, done→retiring
  (fold-back pending), reaped→archived.
- **CLC-002** — Denial is terminal for a `requested` row: a denied or expired spawn request
  transitions `requested`→`archived` without ever activating, draining its template pin
  (SPW-044).
- **CLC-003** — `template_id` null marks a customRole hire; promotion (TPL-040) is its
  catalog door.

## Lifecycle act authority

- **CLC-010** — Suspend, retire, and resume belong to the agent's owner human, an admin,
  or the sponsor of an initiative the agent is bound to — the sponsor's mid-flight stop
  (INT-041) routes through this authority, not around it.
- **CLC-015** — Retiring a persistent agent is a halt, not a drain: in-flight runs stop
  exactly as under suspension — partial results fold back through the memory tiers, staged
  writes go to reconciliation, never killed mid-commit — before dependents resolve.

## Retire walk

- **CLC-020** — Retiring requires resolving dependents: automations, playbooks, paired IM
  sessions, live spawned workers (a dying spawner's ephemeral children fold back into the
  workspace's project memory, not the departed personal one); board-task assignments
  returned to the pool or reassigned; workspace bindings and group memberships dropped with
  Leader posts re-pointed or degraded to an admin ask; owned goals re-owned or retired
  (the OFB-013 rule: successor or admin custody, else retire — the active-goal clamp);
  initiative lead/sponsor
  posts reassigned or closed with their pending sponsor-addressed asks re-keying inside the
  walk; the retiree's own pending asks closed with an audit note — pending spawn requests
  included, draining the template pins (a terminal act leaves no waiters).
- **CLC-021** — Asks *to* the retiree need no walk entry: the non-active target rule
  (ASK-061) reassigns them up the lineage chain.
- **CLC-022** — Upgrade asks settle with the agent they name: an owner-upgrade ask closes
  unresolved with an audit note inside the walk; a response racing the retirement is
  audit-only with no successor ask.
- **CLC-023** — The retiree's personal memory archives with it: inert history under the
  archived identity, never injected, never transferable to a respawn; a fresh hire starts
  fresh memory (re-role's lessons-go-to-DNA is the only bridge).
- **CLC-024** — The ephemeral analogue runs at reap: the TTL reaper's fold-back returns open
  board-task assignments to the pool, re-routes asks *to* it up the chain, and closes asks
  *from* it with an audit note.
- **CLC-025** — Delegation grants naming the retiree as their agent approver resolve with
  it: the `machine_hint`'s delegate edge lapses inside the walk — the rule's normative
  content stands, its routing reverting to the domain owner, a digest line noting the grant
  that died with its grantee (ASK-095). A post-named grant ("by the lead") rides the post's
  re-pointing instead, and suspension keeps the non-active reassignment as its transient.
- **CLC-026** — Authored proposals ride the retire walk: a persistent agent's open DNA
  proposals withdraw with an audit note inside the retirement — the SPEC-09 member-proposal
  rule's agent twin (an agent never owns domains, so there is no transfer branch), the
  folded-back learning staying available to its owner for a fresh proposal, and the review
  queue never waiting on a departed proposer whatever member shape the proposer was.
  Suspension leaves them standing (non-terminal): the reviewing owner may still publish;
  amendment alone waits on the proposer.
- **CLC-027** — Re-owning is the upgrade ask's re-key door: a pending owner-upgrade ask is
  derived from the row's `owner_human_id` (the staff-ownership post), so the OFB-011
  re-owning re-keys it to the successor or admin custody inside the transfer — ids stable,
  deadlines untouched, DGV-045's owner-derived rule applied at the staff door; the generic
  asks-to-the-departing-member reassignment (OFB-003) covers only what no live derivation
  addresses. A response from the departing owner racing the transfer is refused at the
  eligibility door (ASK-040), audit-only; the accept evaluates the upgrade algebra against
  the owner it finds — new-template ∩ the re-derived owner's current scopes — so an upgrade
  never widens past the member the walk just handed the staff to (TPL-020).

## Suspend and resume

- **CLC-030** — Suspend is an emergency stop that halts triggers and runs without resolving
  dependents; in-flight asks re-route up the chain; the halt covers the subtree — live
  ephemeral descendants stop and fold back exactly as on spawner death.
- **CLC-031** — The halt gates its own publishing: a spawn request pending *from* the
  suspended worker is a launch the halt refuses — an approval landing during suspension is
  audit-only and the request archives.
- **CLC-032** — Resume re-arms triggers (missed schedules coalesce, SUB-051) and launches
  new runs, but never resurrects a halted one — a run suspended mid-flight is terminal:
  partial results fold back, interrupted work re-enters as new runs or board tasks, staged
  writes resolve through reconciliation — no half-replayed side effect.
- **CLC-033** — Lifecycle acts are credential fences: an agent's PATs and sessions
  authenticate only while its status is `active` — auth re-validates status at every use;
  retire revokes PATs and terminates sessions outright (credential-death, as human
  offboarding); resume re-arms what suspension made inert, never deleted.
- **CLC-034** — Re-role is retire-and-respawn, never an in-place IDENTITY rewrite
  (identities are role-shaped; project memory stays with the workspace, lessons go to DNA);
  in-place evolution of the same role is the template upgrade path (TPL-020).

## Workspace archival

- **CLC-040** — Workspaces archive, never bare-delete: runs and artifacts are history —
  `workspaces.archived_at` the terminal marker (DAT-010's `deactivated_at` pattern: a
  timestamp is the whole state). Archival is a walked transition: initiative bindings drop
  (goal slice re-derives), domain
  reader sets re-derive, the node claim dies with the row (the lease's terminal case), new
  spawn bindings are refused, workspace-keyed asks degrade to the domainless fallback (hop
  skipped, digest tail), pending spawn requests binding to it archive with their pins
  drained, in-flight runs complete onto the archived slice as history, queued-but-unlaunched
  runs close with an audit note, bound triggers and playbook schedules re-point or disable,
  and project memory archives inert — never transferred, never injected. Authority: admin
  (API-044).

## Personal assistants

- **CLC-050** — One persistent assistant per human employee as a deployment of the existing
  model: a `personal-assistant` persistent template bound 1:1 (`owner_human_id` = the
  assisted employee); the 1:1 is policy-enforced — a second spawn for a human with a live
  assistant is refused; retirement closes the deployment, a fresh spawn reopens it.
- **CLC-051** — Scope mirroring: assistant scopes derive from the human's RBAC role at
  spawn, refreshed on role change, revoked on offboarding; a demotion to viewer retires the
  assistant (mirrored viewer scopes are read-only and a never-ask-target owns no staff);
  the delegation invariant holds with the employee's role as ceiling.
- **CLC-052** — Mirrored access ≠ mirrored behavior: restricted-domain reads (DGV-002's
  *restricted*: access ≠ `public`) carry rate/volume limits in addition to permission
  checks, and every read of a restricted domain is audited.
- **CLC-053** — Identity separation: the assistant acts under its own member identity (own
  PAT, audit trail, spend-ledger line), never the employee's credentials.

## Key acceptance scenarios

```gherkin
Scenario: Resume never resurrects a halted run
  Given a run halted mid-flight by suspension with a staged external write
  When the owner resumes the agent
  Then the halted run stays terminal with results folded back
  And the staged write resolves through reconciliation
  And missed trigger schedules coalesce into one catch-up run

Scenario: Terminal act leaves no waiters
  Given an agent with a pending spawn request and pending asks it filed
  When it retires
  Then the spawn request closes with an audit note and drains its pin
  And asks to it re-route up the chain

Scenario: Re-owning carries the upgrade decision
  Given a pending owner-upgrade ask addressed to an agent's departing owner
  When the offboard walk re-owns the agent to a successor
  Then the ask re-keys to the successor with its deadline untouched
  And the departed owner's racing response is refused at the eligibility door
  And the successor's accept rebases at the successor's own ceiling
```
