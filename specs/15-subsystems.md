# SPEC-15 — Subsystems

Source: PLAN.md §8.1–8.9, §8.11. §8.4 (Skills) is reference-only here — v1 behavior
carries over and its uninstall-dependency idiom is reused by TPL-030 and SUB-064 — so it
owns no SUB IDs (TRACEABILITY's §8.4 row).

## 8.1 Agent runtime (SUB-0xx)

- **SUB-001** — Core loop unchanged from v1 (prompt assembly → guarded loop → structured
  result) with the always-injected DNA layer preceding per-agent context (DRP-001).
- **SUB-002** — Scope enforcement, egress guard, write-lock, stop semantics, cost metering as
  in v1; every capability is a guarded tool enforced in code (PRN-003).
- **SUB-003** — Scope changes — revocations, role-change refreshes — take effect at the next
  run's prompt assembly; a long-running run re-checks its scopes before each external write,
  so a mid-run revocation gates the next side effect.
- **SUB-004** — Rules re-check at the same gate: rules carrying enforcement-bearing
  `machine_hint`s gate external writes exactly like scopes — a write the current applicable
  slice forbids is blocked and raises an ask; purely narrative rules stay advisory context.
- **SUB-005** — Provider degradation: the model gateway queues with backoff instead of
  failing fast; headless runs wait out a bounded outage (the bound is CFG-023); a sustained
  one raises a single critical admin ask (routing policy is CFG-150).

## 8.2 Tools, MCP, staged writes (SUB-0xx)

- **SUB-010** — Built-ins: `fs.*`, guarded `shell.exec`, `web.*`, `kb.search` → `dna.search`,
  `memory.write`, plus `spawn` as a guarded tool; egress guard unchanged.
- **SUB-011** — Connector tiers: tier 1 = email/calendar/docs; tier 2 = enterprise systems of
  record (ERP/WMS/HRIS/CRM) — read-only first, writes gated behind `critical`-tier asks;
  per-connector scoped credentials via PATs, never shared service accounts (the default
  tier-1 suite is CFG-050's decision).
- **SUB-020** — Write-capable tier-2 connectors implement staged writes — prepare → confirm
  → commit — every stage keyed by the idempotency key of its `external_writes` ledger row:
  playbook and node retries reuse the key and cannot duplicate side effects; a crashed or
  reaped run leaves a `prepared` row that reconciliation resolves — confirm or compensate
  per connector, or escalate to an admin ask where no compensation exists.
- **SUB-021** — Stage-less targets (an email send has no prepare) get send-once semantics:
  retry only on transport failure before the remote acknowledges; an ambiguous timeout after
  the wire degrades to an ask with the attempt audited — at-most-once delivery where
  idempotency cannot be engineered.
- **SUB-022** — Reconciliation is scheduled: a periodic pass walks `prepared` rows past the
  grace window (window and cadence both CFG-022) to a terminal state or an admin ask —
  a stranded write cannot wait forever on a reaper that has moved on.

## 8.3 Memory service (SUB-0xx)

- **SUB-040** — Three-tier classifier (personal / project / DNA proposal) with v1 machinery
  (dedupe, timeline, versions, secrets scanner) under it.
- **SUB-041** — Taint propagates through the tiers: memory written by a tainted run carries
  the flag, renders with its provenance when retrieved, is barred from digest pre-fills, and
  is cleared only by explicit review — the spawner's owner for personal memory, the domain
  owner for project memory, and the proposal review itself for proposal-tier rows (DWP-003)
  — never by the passage of time.
- **SUB-042** — A tainted memory item cannot be the sole support for an external write: pair
  it with an untainted source, or ask.

## 8.5 Trigger engine (SUB-0xx)

- **SUB-050** — Schedule/API/event triggers; every firing is a run of the same session
  worker; API triggers gain PAT scopes for external callers.
- **SUB-051** — Missed schedules neither replay nor vanish: firings elapsing during a
  agent suspension, an initiative pause (INT-033), the spend halt (SPW-064), or
  control-plane downtime coalesce into one catch-up run per trigger when the halt holding
  them lifts, carrying a missed-schedule summary (count, window) — per-trigger policy
  `replay|coalesce|skip`, default coalesce, with the runaway-protection rate limits
  (SPW-070) bounding a large backlog.
- **SUB-052** — Firings are idempotent at the boundary: every firing carries a deterministic
  key (schedule: trigger + scheduled time; webhook/API: event id or caller-supplied
  `Idempotency-Key`; event: source event id); the `trigger_firings` table refuses duplicates
  within a configurable window (default 7 days, sized for provider redelivery; CFG-013) and
  returns the original run — an outage never converts one event into two side effects.

## 8.6 Playbook engine (SUB-0xx)

- **SUB-060** — DSL unchanged from v1, the sandbox re-hosted as ARC-005's sealed GraalJS
  context (child-process fallback, DLV-040); `worker()` targets any member (human targets
  create an assignment ask; a viewer is refused at write); spawn-class playbooks (fan-out
  workers) build on ephemeral workers; SOPs instantiate as versioned playbooks with DNA
  pointer cards (DNC-002).
- **SUB-061** — Initiative playbooks: an SOP instantiated under an initiative becomes the
  cross-domain spine — nodes route asks into each domain's escalation chain and artifacts
  land on the initiative's board slice.
- **SUB-062** — Instantiation is bounded: depth cap (default 2, mirroring spawn depth,
  CFG-018) and cycle-checked at both authoring doors — direct or transitive
  self-instantiation refused at save and again at publish (a new version can close a cycle
  the saved graph left open); the runtime depth cap is the backstop.
- **SUB-063** — Versioned references are pinned: a run launches from the exact playbook
  version it was instantiated against; in-flight instantiations complete on their pin
  through a later publication or retirement.
- **SUB-064** — Retiring a playbook version refuses while live references hold it: triggers
  and schedules pointing at the version re-point or disable first (the skill-uninstall
  dependency check applied to playbooks); an SOP pointer card left citing a retired version
  does not block retirement — it rides the freshness pass and flags stale, never silently
  followed into a ghost.

## 8.7 DNA engine (SUB-0xx)

- **SUB-070** — Inherits v1 KB machinery (ingest → chunk → embed → cards → hybrid retrieval
  → citations) extended with domains, proposals, review queue, and glossary/rule/goal-slice
  injection.
- **SUB-071** — An embedding-model switch is a migration, not a reset: the index records its
  model, the new index builds alongside the old, a recall-parity sample gates the cutover,
  and the old index serves until the new one passes — search never blinks (the provider
  default itself is CFG-070).
- **SUB-072** — A repeatedly failing parity gate surfaces an admin ask carrying the deltas —
  roll forward, retune the sample, or stay on the old index — never an eternal silent shadow
  index.

## 8.8 Groups & IM (SUB-0xx)

- **SUB-080** — Unified human+agent teams; IM pairing routes to an agent whose asks
  escalate to the channel (the first channel is CFG-060's decision).

## 8.9 Console (SUB-0xx)

- **SUB-090** — v1 screens 1–9 plus: 10. Org & People (members, RBAC, lineage graph,
  retirement flows, custom-hire promotion action) · 11. DNA console (browse per domain,
  review queue with diffs and provenance, proposal history, glossary editor) · 12. Governance
  (policies, quotas, spend dashboard, spawn audit) · 13. Ask inbox (SLA indicators, batched
  digests, one-line accept/deny with diff links) · 14. Initiatives (goal-linked execution,
  burndown, spend vs. budget, delegated-authority grants).

## 8.11 Inter-agent communication (SUB-0xx)

- **SUB-100** — Agents exchange state, not chatter: agent→agent requests are asks with an
  agent target; shared context lives on the task board as tasks and artifacts; deliberate
  multi-agent fan-out is a playbook with `worker()` targets; disputes escalate to humans as
  DNA decision proposals — never agent-vs-agent argument loops.
- **SUB-101** — No free-form agent-to-agent chat channels exist; every cross-agent
  interaction is an auditable ledger entry (ask, task, or run artifact).
