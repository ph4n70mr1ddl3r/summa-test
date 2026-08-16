# SPEC-11 — Spawning & Policy Engine

Source: PLAN.md §6.1, §6.2.

## Classes and request shape

- **SPW-001** — Two spawn classes: **persistent hire** (long-lived org member; full identity
  files; memory accrual; proposes via review; bounds: budget policy, `owner_human_id`) and
  **ephemeral worker** (bounded subtask delegation; minimal identity; read-only DNA; folds
  results back then dies; bounds: TTL default 24h, spend cap, task-scoped workspaces).
- **SPW-002** — Spawn request shape: `spawn({ from: templateId | customRole, class,
  purpose, workspaceBindings, scopeCeiling, budgetCap, ttl? })` — a guarded tool and console
  action.
- **SPW-010** — `customRole` is for persistent hires, proposed by humans or persistent
  agents behind an approval gate; an ephemeral requester is refused a persistent-hire
  request at write (template or customRole), its recommendation folding back to the spawner.
- **SPW-011** — Ephemeral workers must instantiate whitelisted subagent templates — no
  free-form ephemeral roles; ephemeral spawning is an agent/playbook capability only (a human
  wanting bounded delegation assigns a board task or instantiates a playbook).

## Template and parameter gates

- **SPW-020** — A spawn request may name an `active` template only — `draft` (authoring) and
  `retired` (history) are refused at request time; the status check claims the template row
  inside the spawn transaction so a retirement and a racing request see one winner.
- **SPW-021** — The gate is class-matched as well as status-matched: a persistent hire names
  a `persistent`-class template, an ephemeral worker a whitelisted `ephemeral-subagent` one.
- **SPW-022** — Parameters class-match the row: a `ttl` on a persistent-hire request is
  refused at write — a hire is never half-persistent, mortal by an unreviewed field.
- **SPW-023** — Spawn requests name the exact catalog row (console defaults to newest
  `active`): an approval publishes the version the requester saw (TPL-010).

## Scope and quotas

- **SPW-030** — Scope delegation: child's file/tool/connector scopes ⊆ parent's, enforced by
  the policy engine.
- **SPW-031** — Quotas: max concurrent ephemeral workers per spawner (CFG-040), global
  spawn depth (default 2), org-wide concurrent agents (default 100 active agents,
  persistent + ephemeral combined, CFG-017), per-spawn and org-wide spend caps metered by
  the spend ledger. Count caps are *claimed*, not checked: the engine increments atomically
  inside the spawn transaction — two spawners racing the last slot see one success and one
  refusal.
- **SPW-032** — Cap windows match worker class: an ephemeral's cap spans its lifetime; a
  persistent hire's is a periodic window (default monthly, admin-configurable) evaluating
  reserved + settled — a long-lived hire is neither bankrupted in week two nor free forever
  after one exhausted reserve.
- **SPW-033** — The money side reserves atomically: a spawn or run reserves against
  (reserved + settled) in the spend ledger, settles to actual cost at completion, releases on
  failure or reaping; two runs at 49% of a ceiling cannot both spend past it; §6.4
  rate/volume limits reserve reads identically.
- **SPW-034** — Claims are lifecycle-pinned to the request row: count-cap claims and budget
  reserves attach at request creation inside the spawn transaction, transfer to the live
  worker at activation, and release at every terminal a pending request has — denial,
  approval expiry, close-/archive-time settlements, the requester-state archive, and the
  requester's own retraction (SPW-047) — so an approval never publishes into an exhausted
  cap and cap space never leaks on a dead request.
- **SPW-035** — A settle may overshoot its reserve: the overrun settles in full, surfaces on
  the spend dashboard and the owner's digest, and further reserves against that cap are
  refused until an admin acknowledges through the overrun-ack endpoint — the refusal itself
  is the ask.
- **SPW-036** — Cap edits are claim-scoped, never retroactive: a tightened count cap leaves
  live workers to run out their natural terminal while new claims refuse the tightened
  value; nothing is force-reaped by a configuration act. The spend ceiling is the one edit
  that bites immediately — tightened below live reserved+settled it trips the breaker
  loudly (SPW-060).

## Approval gates

- **SPW-040** — Persistent hires route an approval ask to the owner of the domain the hire's
  primary workspace is bound to; a primary workspace with no bound domain, a workspaceless
  hire, and a domainless primary all route to an admin outright; a multi-domain workspace
  routes to the primary domain (first-bound, admin-editable). One deterministic hop, never an
  undefined gate.
- **SPW-041** — A gate may address its own originator: the domain owner hiring into their own
  domain accepts in one click, the ask itself the audit record (sod governs DNA publish, not
  hire; quota, depth, and budget gates still bind).
- **SPW-042** — The hop's addressee rides owner re-pointing and workspace-binding edits
  wherever they happen (DGV-045/046) — creation-time addressing never outlives the row it
  was read from.
- **SPW-043** — Agent-spawned ephemeral workers exceeding quota route an ask to the spawner's
  owner human.
- **SPW-044** — An approval ask that expires is the denial's twin (`deny` is the spawn
  request's expiry default): the request transitions `requested`→`archived` and drains its
  template pin, the expiry the record exactly as the deny is.
- **SPW-045** — The requester's own state is a respond-time assumption: an accept landing
  while the requester sits in any non-active state is audit-only and the request archives
  with pin drained and claims released; retirement and offboarding settle their requests
  inside their walks; suspension is covered at the door — never a worker published under a
  halted subtree; the suspended requester re-requests on resume.
- **SPW-046** — Ownership follows approval: the persistent hire's `owner_human_id` at
  activation is the gate's accepting human (the self-addressed collapse included; a re-keyed
  gate lands on the re-keyed addressee); the walks re-point from there. Ephemeral workers,
  ungated, roll to the chain: the first human up the `spawned_by` line, pinned at spawn.
- **SPW-047** — The requester's withdraw on a pending spawn-approval ask is the request's
  own retraction: the ask resolves per the withdrawal algebra (ASK-030 — a withdrawn
  approval is a no), and the request archives with its template pin drained and cap claims
  released — denial, expiry, the walks' settlements, the requester-state archive, and the
  retract: one settlement, every terminal; a pending hire never outlives the live intent
  that filed it.
- **SPW-048** — The requested `scopeCeiling` is a respond-time assumption of the same rank:
  it lands at activation as requested ∩ the requester's live scopes (the upgrade algebra,
  TPL-020, at the spawn door), and an empty intersection archives the request with its
  template pin drained and cap claims released. Child ⊆ parent binds the parent the accept
  finds, never the snapshot the request filed — a demoted or de-scoped requester cannot
  publish a child above the ceiling they now hold (ASK-041's re-validation family).

## Spend circuit-breaker

- **SPW-060** — The org spend ceiling halts all spawns and automations with a loud ask to
  admins; the breaker trips by class: triggers and playbooks carry criticality (a firing's
  class is the stricter of its trigger's and playbook's tags), a breach halts
  `standard`-class work first while a small critical floor (default 5%, CFG-012) keeps
  money-moving and customer-facing automations alive; total exhaustion halts everything.
- **SPW-061** — The breaker un-trips only through its trip ask's resolution — an accept
  lifts the halt, a deny holds it while ceilings are re-tuned; spend does not decay with
  time, so the breaker never releases itself and an unacknowledged halt stays visible.
- **SPW-062** — The halt is a launch gate at every door: runs in flight at the trip complete
  and settle onto the ledger (an overshoot trips the overrun gate as designed); staged
  external writes are never killed mid-commit; a spawn-approval accept landing under an
  active halt is audit-only, the request archiving with pin and claims released.
- **SPW-063** — The critical floor carries critical-tagged firings only, never a hire: spawn
  approvals carry no criticality tag and never ride the floor.
- **SPW-064** — The halt is a timetable state as well: schedules elapsing while it holds
  coalesce per the SUB-051 machinery and play on the trip ask's resolution — the critical
  floor launching critical-class throughout, total exhaustion coalescing everything — never
  a silent drop nor a lift storm.

## Runaway protection

- **SPW-070** — Depth cap, rate limits, TTL reaper, budget circuit-breaker, approval gates
  on persistent hires; ephemeral workers get connector-sandboxed, task-scoped workspaces
  only.
- **SPW-071** — The TTL reaper never kills between prepare and commit of an external write:
  it grants a grace window and leaves a reconcilable `external_writes` row (SUB-020). A TTL
  lapsing while its worker is suspended halts-then-reaps: fold-back and reconciliation
  first, archive after — suspension defers the reaper's trigger, never its semantics.

## Key acceptance scenarios

```gherkin
Scenario: The last concurrent slot has one winner
  Given one ephemeral slot remaining under the spawner's quota
  When two spawn requests race
  Then exactly one transaction claims the slot and the other refuses
  And the refusal is surfaced to the losing spawner

Scenario: No worker published under a halt
  Given the breaker tripped and unresolved
  When a spawn-approval accept arrives
  Then the accept is audit-only, the request archives, its pin drains, claims release
  And the requester re-requests after the trip ask resolves
```
