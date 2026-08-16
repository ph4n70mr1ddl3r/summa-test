# SPEC-19 — Non-Functional Requirements & Accepted Boundaries

Source: PLAN.md §2, §13, §13.1.

## Global contract

- **NFR-001** — **Universal fallback** (= PRN-009, restated as the top-level NFR): for the
  space no enumeration covers, no subsystem may fail silently or improvise a side effect —
  refuse the effect, write the audit, raise an ask. Handling every scenario does not mean
  predicting every scenario; it means no failure mode is silent. Enforced by the chaos +
  fault-injection suites (DLV-060).

## Determinism & testability

- **NFR-010** — Injection is deterministic per (reader access, domain set, linked-goal set,
  DNA version) (DRP-003); hybrid-ranking determinism (same query → same blend, across index
  rebuilds) is a Phase-0 spike gate (DLV-041).
- **NFR-011** — Time is monotonic in effect (ARC-031); no backward clock step un-expires
  anything or reverses a terminal transition.

## Accepted boundaries (explicit non-requirements)

These are stated so nobody expects them silently; each is a boundary of the trust or
deployment model, not a gap:

- **NFR-020** — **Malicious insider**: governance treats humans as the trust anchor — a
  domain owner publishing a poisoned rule gets agents obeying it until audit catches up;
  nothing sits above the owner short of admin. The sod knob raises the cost (DWP-050); the
  boundary itself is the trust model. Single-admin mode collapses review of own proposals to
  one click — the accepted degenerate case (CFG-130 keeps strictness separate).
- **NFR-021** — **Single control plane**: one control-plane instance is the design (a single
  logical instance over SQLite WAL — CFG-030's microservices packaging may split the plane
  into services, but exactly one service owns the store; no replication); its downtime is
  survived, not eliminated — runs queue, triggers
  coalesce (SUB-051), leases hold to their fence and pause-and-resync on reconnect
  (ARC-022), and recovery rides the restore runbook (DLV-055). Multi-instance HA is a
  redesign beyond this plan.
- **NFR-022** — **Deferred parameters, not deferred designs**: provider routing (CFG-150)
  and lease intervals (CFG-160) are decisions *over* designed mechanisms — the behavior
  exists; the tuning is organizational.

## Risk mitigations (selected, normative)

- **NFR-030** — Prompt injection via external content: taint-tracking for off-platform
  content; provenance + raw diffs in the review UI; spawns from tainted runs auto-gated
  (SPW-049); tainted context barred from external writes (SUB-042); tainted-origin asks lose
  digest pre-fills and tainted-run accepts are audit-only (ASK-043); taint survives
  publication as a provenance flag and propagates through memory until explicitly reviewed
  (DWP-023, SUB-041).
- **NFR-031** — DNA quality drift / gaming: human-owned review, provenance on every item,
  reviewer-agent contradiction reports, compartment isolation.
- **NFR-032** — Governance overhead vs. small-team speed: proportional governance (PRN-008);
  compartments optional at start.
- **NFR-033** — Operational-data sync temptation: the §4.6 hard line (DRP-050) — sync
  requests surface as proposals an owner must reject.
- **NFR-034** — Directive decay: initiatives first-class with escalation machinery (INT-060)
  and the goal slice keeping directives in every relevant prompt (DRP-020).
- **NFR-035** — Reorgs outpacing the model: topology changes are governed single-event ops
  with stable ids (DGV-010); offboarding walks every dependency (OFB-001); template upgrades
  rebase running staff in place (TPL-020).
