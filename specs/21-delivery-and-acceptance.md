# SPEC-21 — Delivery & Acceptance

Source: PLAN.md §11, §12.

## Phase ladder

| Phase | Deliverable | Est. (1 dev) |
|---|---|---|
| **0. Foundations** | Repo, CI, single-process skeleton — monorepo, Java 21 LTS + Spring Boot 3.4.1 (fat jar), sqlite-jdbc SQLite (WAL), React+Vite console shell (TS strict), REST+WS, 3-OS CI matrix, OCI images under rootless Podman + kind-on-Podman Kubernetes CI (CFG-030) | 1 wk |
| **1. MVP agent** | Chat with an agent doing real local work — model gateway, agent loop, guarded fs/shell/web tools, approval cards, audit, streaming chat UI, first-run bootstrap | 4–5 wks |
| **2. Identity, memory, skills, connectors** | agents feel like employees — role catalog, IDENTITY/STYLE/HANDBOOK, memory tiers 1–2, skills + market, MCP client + tier-1 connectors, workspace kinds, versioned role-template catalog | 3–4 wks |
| **3. Company DNA v1** | The coherence core — DNA store + domains/index, cards compilation, glossary + applicable-rules + goal-slice injection (org-wide goals first; linked goals wire up with initiatives in P4), proposals + owner review queue, citations | 3–4 wks |
| **4. Automation** | 24/7 operation — triggers, PATs, templating, headless Ask policy, shared task board, initiatives v0 (goal + lead + deadline + task grouping) | 2–3 wks |
| — | **v1 cut line**: Phases 0–4 + 8a are shippable v1 | — |
| **5. Playbooks** (v2 track) | DSL + sandbox, statuses, askUser → Asks, read-only canvas, versions, playbook triggers | 3–4 wks |
| **6. Multi-human org** (v2 track) | Server deployment, human accounts + RBAC, ask routing + per-human digests, shared board, node registration & affinity scheduling, delegated authority + initiative budgets, offboarding + last-admin guard, agent suspend/resume, template upgrades, domain split/merge | 3–4 wks |
| **7. Spawning** (v2 track) | Ephemeral workers (quota, TTL, fold-back), persistent hires (owner approval), policy engine, lineage graph, spend ledger + circuit-breaker | 2–3 wks |
| **8a. v1 hardening** | Backup/restore with reconciliation runbook, encrypted secrets, docs, security review | 1 wk |
| **8b. v1.1 polish** | Tauri shell, installers, telemetry (opt-in) | 1–2 wks |

- **DLV-010** — v1 (P0–P4 + 8a): ~14–18 weeks solo; full company OS (v2 track + 8b):
  ~23–31 weeks. Plan on 15–20% AI-assisted compression, not 40% — AI compresses boilerplate,
  not security-critical integration work.
- **DLV-011** — Milestones: working agent ~week 6 · DNA-coherent company ~week 13 · v1 ships
  ~week 14–18 · multi-human org ~week 20–26 · full spawning ~week 22–29.
- **DLV-012** — Sequencing is normative: DNA lands early (everything after it depends on
  shared context); spawning lands last (it needs governance + budgets + the org model); the
  v1 cut line stays at Phase 4 because playbooks, multi-human, and spawning add
  *multiplicative* complexity, not additive features.
- **DLV-013** — Initiatives land in two steps: v0 grouping in P4; budgets and delegated
  authority in P6. Tier-2 connectors start after v1 ships, one integration project per
  connector (CFG-090). The enterprise seams ride the same track: quorum asks, staged writes,
  and erasure/residency governance ship with the first write-capable connector; claim leases
  and fencing ship with P6 node registration.

## Phase-0 spikes (the ladder isn't committed until they pass)

- **DLV-040** — GraalJS playbook sandbox on the JVM: sealed-polyglot escape surface
  (host-access denial), stock-JDK vs GraalVM JIT performance, child-process fallback
  prototype.
- **DLV-041** — sqlite-vec (JVM loadable extension via sqlite-jdbc) + FTS5 hybrid-ranking
  determinism (NFR-010).
- **DLV-042** — Git-backed DNA store concurrency: concurrent publishes, direct edits vs.
  publish, index staleness, and which component holds the write lock in multi-node mode.
- **DLV-043** — Secrets API for the Tauri shell (stronghold / OS keyring).
- **DLV-044** — Container baseline for CFG-030's shape: rootless Podman builds of the
  per-service fat-jar images, a kind-on-Podman Kubernetes environment in CI, and proof that
  the microservices decomposition preserves the single-writer invariants — exactly one
  SQLite owner (NFR-021), one direct DNA writer (STG-020) — before the ladder commits.

## Acceptance criteria (a phase isn't done until its demo passes)

- **DLV-050** — **P1**: agent edits a repo file through a gated approval; audit trail
  complete end to end.
- **DLV-051** — **P3**: a fixed, version-controlled task battery (≥ 20 tasks spanning
  retrieval, rule compliance, and citation, committed to the repo at P3 start) run with and
  without DNA injection, three paired trials per arm; the demo passes when the with-DNA arm
  wins a majority of the battery's scored metrics — citation accuracy, rule compliance,
  task completion, each a rate over the battery — in at least two of the three trials:
  the product's core hypothesis, tested, not assumed.
- **DLV-052** — **P4**: headless trigger fires overnight; a blocked approval auto-denies at
  expiry; the morning digest renders correctly, grouped by initiative.
- **DLV-053** — **P6**: two humans + two nodes; heartbeat loss mid-run recovers with no
  orphaned work; a stale-epoch workspace claim is refused at the mediated boundary and
  reconciled on reconnect; a delegated-authority rule routes an approval to the initiative
  lead and expires cleanly; offboarding one human reassigns domains, asks, and initiatives;
  the last-admin guard refuses the final admin.
- **DLV-054** — **P7**: spawn storm trips the circuit-breaker; a depth-3 spawn is refused by
  policy, not prompt.
- **DLV-055** — **Restore runbook** (ships with 8a): restore DB + DNA git to point T, then
  reconcile — the audit log never rewinds (pre-restore segments re-append as a replay
  segment); nodes re-register and report runs executed after T; `external_writes` rows are
  rebuilt from node reports and connector-side idempotency-key queries where supported;
  erasure events in the replayed segment re-apply (a restore cannot resurrect erased data);
  conflicts become admin asks. Tested in CI as a chaos scenario.

## Test corpus (DLV-06x)

- **DLV-060** — The PLAN.md §12 corpus is the verification map for every requirement in this
  suite: unit, integration, E2E, injection suite, golden runs, and chaos-lite. The
  fault-injection probes assert the NFR-001 contract: an unanticipated state refuses the
  effect, writes audit, and raises an ask — never a silent failure.
- **DLV-061** — Test naming cites REQ IDs per the README convention; a requirement with no
  citing test is an open verification gap, tracked like missing code.
