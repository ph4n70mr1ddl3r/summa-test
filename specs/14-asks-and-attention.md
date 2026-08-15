# SPEC-14 — Asks & Human Attention

Source: PLAN.md §8.10.

## Kinds, tiers, expiry

- **ASK-001** — Ask kinds: `approval | question | assignment | spawn_request`; fields per
  SPEC-16. Asks carry payload, deadline, SLA tier (`critical | standard | bulk`), escalation
  policy, expiry behavior (`deny | escalate | reassign`), optional initiative/workspace
  linkage, quorum count, responses ledger, collapsed count.
- **ASK-010** — SLA tiers: `critical` (blocks a customer-facing or money-moving run —
  interrupt-grade push, console + IM), `standard` (blocks a run — next digest), `bulk`
  (non-blocking — daily digest, batched). The headless approval policy maps onto them:
  `escalate_im` → critical, `queue_until_morning` → standard, `auto_deny` → expiry behavior
  `deny`.
- **ASK-011** — Expiry semantics are explicit per ask: `deny` (default for approvals and
  spawn requests — an expired approval is a no), `escalate` (default for questions),
  `reassign` (default for assignments); a run blocked on an expired ask never hangs. `deny`
  and `reassign` close the expired ask; `escalate` closes it and opens a linked successor.
- **ASK-012** — `deadline` derives from the tier unless set explicitly; an explicit deadline
  earlier than the ask's creation is refused at write.
- **ASK-015** — A quorum-1 ask closes on the first response received; later responses
  (member and deputy racing) are audit-only; a response to an expired ask is recorded with
  no effect — the successor ask, if any, carries the decision.

## Withdrawal and the system originator

- **ASK-030** — Withdrawal is the originator's side: `from` may retract a pending ask before
  it closes — collapsed waiters resolve with it, partial quorum accepts stay audit-only —
  and the retraction applies the ask's expiry behavior to whatever was waiting; the
  SPEC-09/12 walks' close-with-audit-note is this mechanism applied by the system.
- **ASK-031** — The system files asks as well as settling them: plane-originated asks —
  goal-window, stall, close-out, dependency, starvation, rebind, trip, upgrade, activation,
  contradiction, quarantine, parity, the storm aggregate, among others (the filing event is
  the rule, not the enumeration) — carry a reserved **system originator**: not a member row,
  never a target, never response-eligible, rendered 'System'.
- **ASK-032** — A system ask's withdrawal belongs to the system's named closures alone —
  the walks' audit-note settlement, the aggregate's recovery-or-ack close, expiry per
  behavior; a member's withdraw on a system ask is refused at the door, and the system
  retracts no member's ask: each side's retraction is its own.

## Respond door

- **ASK-040** — Eligibility is checked at the door: a response from outside the ask's
  eligible set — neither the addressee, nor the addressee's deputy, nor, for a quorum ask, a
  member of the evaluated pool or a pool member's deputy — is refused at the respond
  endpoint with the attempt audited.
- **ASK-041** — Responses re-validate before they bind: payload assumptions are recomputed
  at respond time — the diff still applies, the referenced DNA item is still live, the scope
  still holds — and a spawn approval names five assumptions: the initiative still accepts
  launches (INT-080), the workspace still accepts bindings and remains readable for the
  member it would publish, the requester is still `active`, and the spend halt is not
  holding. An accept racing a pause, close, workspace archival, requester suspension, or the
  breaker's trip is audit-only: the request archives with pin drained and claims released.
- **ASK-042** — A response against a superseded world is audit-only with a successor ask
  opened against current state (the machinery expiry uses).
- **ASK-043** — Provenance re-validates: an accept originating in a tainted run is
  audit-only — taint never becomes approval authority; the successor ask renders without a
  pre-fill while carrying the decision to an untainted reader.
- **ASK-044** — Event-side settlement: the event that terminally breaks a named assumption
  settles the ask at the event per its expiry behavior — a quorum ask whose rule went
  terminal mid-wait resolves the moment the premise dies (fail-safe), a domain's archive
  closes its owner-addressed asks with an audit note. Non-terminal states keep the deadline
  their resolver: a pool shrunk below N denies at expiry, not at the event, because a live
  pool can grow back — settle at the event only what the event ended.

## Quorum

- **ASK-050** — Rules may require N distinct approvals (`machine_hint.requires_approvals`):
  the ask carries `quorum_required` and closes answered once N distinct human members have
  accepted; a deny closes it denied immediately; expiry denies; a stale acceptance is
  audit-only and does not count.
- **ASK-051** — Quorum addressing: `to` = the pool's primary recipient — the rule's domain
  owner, or its delegate (who joins the pool) when a delegation routes the rule; the eligible
  pool is that owner plus every active admin, evaluated at respond time: a pool that grows
  mid-ask admits new acceptors; an acceptance that already counted stands — the ask closes
  the moment the Nth valid accept lands.
- **ASK-052** — When N > 1, only pool principals' accepts count: a deputy's accept is
  audit-only there (N approvals means N pool principals); toward quorum-1 a deputy may stand
  in for the one signature.
- **ASK-053** — N is bounded below (`requires_approvals` ≥ 1) and pool-checked at every
  write door: N exceeding the eligible approver pool flags at proposal time like any
  contradiction; a pool that later shrinks below N leaves the ask unanswerable — it denies
  at expiry, fail-safe, with the breach escalation naming the shortfall.
- **ASK-054** — SLA breach escalates to the admin, who may contribute one of the required
  approvals.

## Escalation chains

- **ASK-060** — Every ask to a human carries member → deputy → domain owner (of the domain
  the ask's workspace belongs to; no domain skips the hop; multi-domain workspaces hop to
  the primary domain) → admin, walked on SLA breach; inactive members are skipped; the walk
  carries a visited-set so a mis-configured cycle ends the hop, not the walk.
- **ASK-061** — Agent targets: an ask routed to a Coworker queues into its next run (or
  wakes a session worker); if the target is anything but `active`, or is busy past SLA, the
  ask reassigns up the chain; the agent chain is the lineage chain — first hop the Coworker's
  `owner_human_id`, then the human chain with the same visited-set; an ask to an agent never
  lacks a human next hop.
- **ASK-055** — The admin hop is a broadcast: every path routing to "an admin" — the
  terminal hop, review-SLA escalation, sod publish routing, the spawn gate's fallback —
  addresses all active admins at once and the first valid response wins; the broadcast
  renders and admits responses against the live admin set: an admin added mid-wait joins
  pending broadcasts, a departed one contributes nothing, and a former admin's late response
  is refused at the eligibility door. A single-admin org is the degenerate case.
- **ASK-056** — The broadcast is not ambient authority: a member-addressed ask stays
  member-addressed until its own chain escalates.
- **ASK-057** — Chain exhaustion — the broadcast finds no active recipient or breaches —
  expires the ask per its expiry behavior (an unanswered approval is a no; an exhausted
  assignment returns the task to the board pool with a digest line) and broadcasts a
  critical-tier org-stall alert to every active human.
- **ASK-058** — The org-stall broadcast is an alert, not an ask: it renders to every active
  human — viewers included, read-only — because the never-a-target guard governs members the
  org waits on for an answer and an awareness blast waits on no one.

## Delegated authority

- **ASK-090** — A directive can push authority, not just work: the sponsor proposes a DNA
  rule scoped by `machine_hint` (initiative, ceiling, window), reviewed like any rule; the
  ask router evaluates applicable rules — delegations included — when choosing approvers.
- **ASK-091** — When several delegated rules match one ask, the most restrictive ceiling
  wins and a contradiction report goes to the sponsoring owners.
- **ASK-092** — A delegation may name an agent — persistent only (an ephemeral is refused at
  propose): the named agent is the routed ask's primary recipient, answering through its
  session worker; its accept binds the asks its rule routes (the rule's review is the
  authority; the accept is a run output carrying taint rules); toward N > 1 the agent's
  accept is audit-only, exactly like a deputy's.
- **ASK-093** — The named recipient resolves at ask-creation time — the grant runs to the
  post's current holder; a non-active delegate reassigns by the standing chain rules, never
  to a departed identity.
- **ASK-094** — Delegations end by window, supersession, or initiative close (rule
  semantics): closing an initiative lapses every rule scoped to it — status `lapsed`, dropped
  from injection and routing.
- **ASK-095** — The grantee's own retirement is a delegate edge's fourth end: the retire
  walk lapses an agent-named grant with its grantee (CLC-025) — the rule stands, routing
  reverting to its owner, the digest line the notice — while a post-named grant rides the
  post's re-pointing and suspension remains the transient the non-active reassignment
  covers. A reviewed grant never runs to a departed identity, exactly as no routing surface
  does.

## Storms and digests

- **ASK-100** — Storms collapse: identical pending asks — same kind, target set, payload
  hash — attach to one canonical ask as a `collapsed_count` within a window; the digest
  renders "37 identical escalations" as one line; the canonical ask's answer resolves every
  collapsed waiter.
- **ASK-101** — A per-source ask-creation rate limit (per run, trigger, Coworker) sheds
  overflow into a single aggregate admin ask; the aggregate has a lifecycle: it closes
  resolved when its source's rate falls back under the limit for a full window or an admin
  acknowledges it, the shed count preserved in audit.
- **ASK-110** — Digests compute per recipient: each human's timezone and working hours define
  their morning (unset calendars fall back per ARC-032); the composer groups by initiative,
  then workspace, then an ungrouped tail — every ask renders somewhere.
- **ASK-111** — Pre-filled recommendations compute only from re-validated, untainted
  payloads: a tainted-origin ask renders without a pre-fill; approvals render as one-line
  accept/deny with diff links — reviewers see raw diffs, never agent-authored summaries
  alone.

## Key acceptance scenarios

```gherkin
Scenario: An impossible quorum degrades to a visible no
  Given a rule requiring 3 approvals and an eligible pool of 2
  When the ask expires
  Then it closes denied with the breach escalation naming the shortfall
  And no hang remains

Scenario: Taint never becomes approval authority
  Given an approval ask originating in a tainted run
  When the addressee accepts
  Then the accept is audit-only
  And an untainted successor ask carries the decision without a pre-fill
```
