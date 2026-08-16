# SPEC-05 — DNA Write Path

Source: PLAN.md §4.3.

## Tiers and proposal basics

- **DWP-001** — The memory service classifies run learning into three tiers: personal memory
  (one agent, automatic), project memory (one workspace, automatic), and DNA proposals
  (company-wide, always proposed, never auto-published).
- **DWP-002** — A DNA proposal carries the change (new card / rule / decision / goal /
  glossary / edit), its provenance (source session, docs, or observation), and the proposing
  member; proposal kinds are exactly `card|rule|decision|goal|glossary|edit`.
- **DWP-003** — Domain owners review from a queue in the DNA console (diff view, provenance,
  impact hints); publish creates a version and an effective date; reject leaves an audit trail.
- **DWP-010** — `proposed_by` must hold a write surface: humans of any role but viewer and
  persistent agents only — an ephemeral worker's propose is refused at write; its learning
  folds back and the spawner or a human proposes from it.

## Review queue and SLA

- **DWP-020** — Proposals carry `review_by` derived from the domain's `review_sla_days`
  (default 7); a breach escalates to the admin and a stale queue surfaces in the owner's
  digest.
- **DWP-021** — The queue belongs to the domain, not the owner's inbox: it renders to whoever
  holds `owner_human_id`, and owner re-pointing at any door (topology op, domain edit, the
  SPEC-09 walks) re-keys the rendering with `review_by` clocks untouched.
- **DWP-022** — Org-scoped items (org-wide goals, org-wide glossary; `domain_id` null) route
  to the admin review queue; their `review_by` derives from the global default — no domain row
  governs them.
- **DWP-023** — Taint survives publication as provenance residue: an item accepted from a
  tainted run keeps its flag, renders with an indicator wherever cited, and heads the scheduled
  quality reviews — the owner's accept is informed consent, not a laundering step.
- **DWP-024** — Humans of any role but viewer may propose directly and may edit in their own
  tools (the store is git-backed; a PR workflow is possible for teams that want it, STG-010).
- **DWP-025** — The SLA is bounded and monotonic under edit: `review_sla_days` ≥ 1 day at
  every write door (an SLA of zero is an always-breaching queue, not a cadence), and an edit
  re-derives standing clocks in one direction only — tightening recomputes each open
  proposal's `review_by` from its filed date under the new SLA and applies it only where it
  lands earlier; loosening leaves standing clocks untouched and governs proposals filed
  after it. Urgency moves forward, never back (ARC-031's monotonic idiom at the queue
  door).

## Amendment

- **DWP-030** — Proposals are amendable in review: the proposer — or the reviewing owner, as
  suggested changes — files a new revision; reviewers see latest-plus-history; publish binds
  the latest.
- **DWP-031** — Amendment and publish serialize behind the domain write lock (DGV-050):
  racing amendments land as sequential revisions on the atomically incrementing counter; a
  publish binds the latest revision that preceded it into the lock.
- **DWP-032** — An amendment re-routes with its payload: a revision moving an item between
  org-wide and domain-scoped re-routes review to the queue governing the amended scope;
  cross-domain moves are refused outright (DWP-064).
- **DWP-033** — Revisions never change `kind`: scope re-routes, kind does not; a card revision
  that should be a rule is a new proposal.

## Publish

- **DWP-040** — Publish runs inside the domain write lock and re-runs contradiction checks
  against current state at commit — the second of two sequenced contradictory publishes is
  refused back to review, never half-silently merged.
- **DWP-041** — The re-check covers the edit target's lifecycle: an edit proposal whose item
  retired — or otherwise left the live set — mid-review refuses back to review instead of
  editing frozen history.
- **DWP-042** — Contradiction detection covers rule-vs-rule, goal-vs-goal, decision-vs-rule,
  and quorum-vs-pool shortfalls (ASK-053), at proposal time and re-checked at publish.
- **DWP-050** — Separation of duties is a per-domain knob (`sod`, default off): when on, the
  proposer cannot be the publisher — an owner's own proposal routes publish to the
  admin broadcast (ASK-055); in a single-admin org that collapse to one click is accepted
  (NFR-020), with strictness governed separately (CFG-130).
- **DWP-051** — Withdrawal: the proposer may withdraw a pending proposal; the SPEC-09 walks
  transfer or auto-withdraw authored proposals on departure so the queue never waits on a
  proposer who cannot amend.

## Item-level CRUD

- **DWP-060** — Item-level DNA CRUD is the publish path, not a side door: every write lands
  inside the domain write lock with the publish-time contradiction re-check, sod routing, and
  the secrets scan (SEC-030) — an owner's direct write gets every guarantee a reviewed
  proposal's publish gets.
- **DWP-061** — The item surface is create / update / retire, never delete; erasure (STG-030)
  is the only shredding path; decisions are the immutable exception (DNC-030).
- **DWP-062** — Updates land on live states only (owner's draft or active item); superseded/
  lapsed rules, terminal goals, and retired cards/glossary are frozen history (DNC-011/022/
  052); a rule's retire maps to window truncation (DNC-021); a draft discards by retiring.
- **DWP-063** — One validation at every door: propose, amend, and item write run the same
  ingest sanity — window ordering, unique ids, machine-hint bounds (quorum N ≥ 1) — so no
  door is softer than the git door (STG-010).
- **DWP-064** — Item edits shall not smuggle topology: a proposal or item write changing an
  item's `domain_id` is refused; cross-domain moves are topology ops (DGV-010).

## Key acceptance scenarios

```gherkin
Scenario: Racing contradictory publishes
  Given two proposals in review whose payloads contradict on current state
  When both publishes land in sequence
  Then the first publishes inside the lock
  And the second re-runs contradiction checks, is refused, and returns to review

Scenario: Amendment re-routes by scope
  Given a domain-scoped card proposal under owner review
  When the proposer amends it to org-wide scope
  Then the proposal re-routes to the admin queue with its review_by clock still running
```
