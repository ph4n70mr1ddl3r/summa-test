# Summa — SDD Specification Suite

This directory is the **normative requirements corpus** for the Summa platform, derived from
`PLAN.md` (v2.58). `PLAN.md` is the design rationale — the "why", with history;
these specs are the implementation contract — the "what", discrete and testable. When the two
disagree, fix one of them in the same change; never implement against a discrepancy silently.

## How to use this suite (SDD workflow)

1. **Pick a slice.** Choose an implementation cycle from `21-delivery-and-acceptance.md`
   (phases are ordered and gate each other) and the capability modules it touches.
2. **Extract tasks.** Every requirement with an ID in scope becomes one or more tasks in the
   cycle's task list; each task cites the REQ IDs it satisfies.
3. **Implement against the IDs.** Code, schema, and endpoints are written to satisfy named
   requirements, not prose recollection.
4. **Verify by ID.** A cycle is done when every in-scope requirement has passing evidence —
   a unit, integration, E2E, or chaos test whose name cites the REQ ID (see "Verification
   conventions" below). No evidence, no done.
5. **Change control.** A design change lands as: PLAN.md amendment → spec delta (new REQs,
   never silent edits of existing ones — supersede instead) → test updates → code. Requirement
   IDs are stable forever once issued and are never reused.

## Conventions

- **Keywords** follow RFC 2119: *shall* (mandatory), *should* (default, tunable), *may*
  (optional). "The system shall not" is an absolute prohibition. "Shall refuse" always means:
  reject the write/action, record an audit entry, and where a human decision is needed raise an
  ask — never a silent failure (principle PRN-009).
- **Requirement IDs** are `<PREFIX>-<NNN>`, unique across the suite, one prefix per module
  (module 01 carries the two prefixes of its source sections, VIS and PRN) — e.g. `ASK-041`.
  Cross-file references always cite the ID, never paraphrase.
- **Single home rule**: every behavior has exactly one normative home module. Other modules
   reference it by ID. If two modules seem to state one rule, the more specific one is
   authoritative and the other is a pointer.
- **Actors** (used uniformly):
  - *Admin* — human with role `admin`; holds governance surfaces and the terminal escalation hop.
  - *Owner* — human holding `dna_domains.owner_human_id` for ≥1 domain.
  - *Member* — human with role `member` or above.
  - *Viewer* — human with role `viewer`; a total read-only surface (ORG-020).
  - *Agent* — persistent AI member (the type formerly named "Coworker"; SPEC-12 keeps the CLC
    prefix for ID stability); *ephemeral worker* — TTL-bounded agent.
  - *Control plane* — the self-hosted server; *Node* — enrolled execution host.
  - *System originator* — the reserved non-member principal that files plane-originated asks
    (ASK-031…032).
- Lower-case *member* in requirement prose (e.g. "any member but an ephemeral worker") means a
  row of the shared member namespace — human or agent, any role (ORG-001); the capitalized
  *Member* actor above is the RBAC role. Requirements name excluded classes explicitly.
- **State names** (`active`, `suspended`, `requested`, …) are the enum values of
  `16-data-model.md`; the data model is the representational ground truth.
- **Testability**: every requirement is written to be falsifiable by a test. Event-driven
  requirements use EARS form: "When \<trigger\>, the system shall \<response\>."

## Module index

| File | Prefix | Domain | PLAN.md source |
|---|---|---|---|
| `01-product-and-principles.md` | VIS/PRN | Vision, actors, governing principles | §1, §2 |
| `02-architecture-and-topology.md` | ARC | Deployment, nodes, leases, time, residency | §3 |
| `03-dna-content-and-lifecycles.md` | DNC | Content model, item lifecycles | §4.1, §7 |
| `04-dna-read-path.md` | DRP | Injection, retrieval, citation, precedence | §4.2, §4.6 |
| `05-dna-write-path.md` | DWP | Proposals, review, amendment, publish | §4.3 |
| `06-dna-governance-and-topology.md` | DGV | Domains, reader sets, topology ops, locks | §4.4 |
| `07-dna-storage-and-privacy.md` | STG | Git/db-only stores, ingest, erasure | §4.5 |
| `08-org-model-and-rbac.md` | ORG | Members, roles, board, groups, deputies | §5 |
| `09-offboarding-and-demotion.md` | OFB | Departure walks, last-admin guard | §5 |
| `10-initiatives.md` | INT | Initiative spine, transitions, dependencies | §5.1 |
| `11-spawning-and-policy.md` | SPW | Spawn requests, gates, caps, breaker | §6.1, §6.2 |
| `12-agent-lifecycle.md` | CLC | Suspend/retire/resume/reap, assistants, workspace archival | §6.3, §6.4, §7 (workspaces) |
| `13-templates-and-catalog.md` | TPL | Versioned catalog, upgrades, promotion | §6.5 |
| `14-asks-and-attention.md` | ASK | Tiers, expiry, quorum, escalation, digests | §8.10 |
| `15-subsystems.md` | SUB | Runtime, tools, memory, triggers, playbooks | §8.1–8.9, §8.11 |
| `16-data-model.md` | DAT | Tables, enums, invariants | §7 |
| `17-api-surface.md` | API | Endpoints, authority, refusals | §9 |
| `18-security.md` | SEC | Authn, PATs, scopes, secrets, audit | §10 |
| `19-nfr-and-boundaries.md` | NFR | Determinism, boundaries, universal fallback | §2, §13, §13.1 |
| `20-configuration-and-decisions.md` | CFG | Tunables and the 17 key decisions | §14 |
| `21-delivery-and-acceptance.md` | DLV | Phases, spikes, acceptance demos | §11, §12 |

`TRACEABILITY.md` maps every PLAN.md section to the requirements that specify it and is the
completeness proof for this suite.

## Structural lint

The suite's structural invariants are machine-checked by `tools/lint_specs.py`, run in CI on
every push: ID uniqueness, one home module per prefix, no dangling citations (ranges
expanded), no dangling PLAN `§` references (ranges expanded), agreement of the three version
pins (PLAN's version stamp, this README's header, TRACEABILITY's header), and TRACEABILITY
exact in both directions — plus per-row partition: every coverage row cites only IDs homed in
the modules it names, and no ID belongs to two rows outside the "Intentional cross-listings"
table in TRACEABILITY.md. A spec change that breaks any of them fails the build; the
maintenance rule below is enforced, not aspirational. The linter itself is covered by a
fixture-based self-test (`tools/test_lint.py`, also run in CI): every check must fail loudly
when its invariant is broken.

## Verification conventions

`PLAN.md` §12 enumerates the test corpus; these specs are its organizing key. Conventions:

- Unit tests assert single-requirement behavior and are named `test_<prefix>_<nnn>_<slug>`.
- Integration tests may cover a requirement chain and cite every REQ they exercise.
- `19-nfr-and-boundaries.md` NFR-001 (the universal fallback) is additionally enforced by the
  §12 fault-injection probes: an unanticipated state must refuse the effect, write audit, and
  raise an ask.
