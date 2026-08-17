# SPEC-20 — Configuration & Open Decisions

Source: PLAN.md §14. Each decision is a named parameter with a default and a decision
trigger; the mechanism it tunes is already designed (NFR-022). Defaults live behind the
`PUT /governance/policies|quotas` surface (API-050) unless they ride their own object.

- **CFG-001** — DNA canonical store: git-backed markdown — decided (v2.57): the erasure
  collision is contained by the db-only carve-out (STG-002…003, STG-030…033, STG-005),
  human legibility is load-bearing (PRN-006), and git concurrency is what the DLV-042
  spike gates before the ladder commits. DB-with-export remains the per-domain carve-out,
  not the default.
- **CFG-002** — Human auth v1: the deployment's own Keycloak over OIDC — Summa stores no
  human credentials (SEC-001); `humans.auth` carries the Keycloak subject link, never
  credential material — "local accounts" are Keycloak realm accounts (SEC-002).
- **CFG-003** — SQLite as single-process default: WAL mode with FTS5, chosen for the
  MVP path where a single owner holds both control plane and node (ARC-001); remote-node
  deployments layer on a replicated store (CFG-030).
- **CFG-004** — Audit log retention: append-only, retained indefinitely in SQLite; export
  to cold storage is admin-configured (STG-030).
- **CFG-005** — Plan file encoding: UTF-8 markdown with ASCII requirement IDs; no binary
  assets in the DNA store (STG-001).
- **CFG-006** — Console dev server: Vite on port 3000, proxying /api to :8080; production
  serves the built SPA from the same Spring Boot process under /api (no separate CORS).
- **CFG-007** — Node heartbeat cadence: 30 s interval, 90 s timeout — the fence lease
  interval (ARC-020) is a function of these, see CFG-160 for tunability.
- **CFG-008** — FTS5 ngram tokenizer: default for DNA search (DRP-030); configured at
  database creation time via SchemaInitializer.
- **CFG-009** — GraalJS sandbox isolation: sealed polyglot context, no host access;
  child-process fallback when GraalVM is unavailable (DLV-040).
- **CFG-020** — Human auth v1: the deployment's own Keycloak over OIDC — decided (v2.58):
  Summa stores no human credentials (SEC-001) — `humans.auth` carries the Keycloak subject
  link, never credential material — "local accounts" are Keycloak realm accounts, and company
  SSO is Keycloak brokering the company's IdP: the same OIDC surface to Summa either way,
  which is why the original local-vs-SSO either/or collapsed. RBAC, PATs, and sessions stay
  Summa's own (SEC-004/005); lockout recovery rides Keycloak's realm-admin paths (SEC-002).
- **CFG-030** — First deployment shape: microservices on Kubernetes under rootless Podman,
  containerized from day one — decided (v2.58): every artifact ships as an OCI image
  (ARC-006), the decomposition follows §3's existing seams (plane API/console backend, model
  gateway, execution nodes, plus the Keycloak service CFG-020 adds) without creating a second
  writer for any single-owner store, and single-process mode (ARC-001) survives as one
  container for the small-team case. The packaging is not an HA license — NFR-021's
  single-instance boundary and the single-writer invariants (STG-020) stand — and the
  DLV-044 spike gates the decomposition before the ladder commits.
- **CFG-040** — Ephemeral default TTL & quota: 24h / 3 concurrent per spawner (default) —
  tune with use.
- **CFG-050** — Tier-1 business suite: Microsoft 365/Graph (default) vs. Google Workspace.
- **CFG-060** — First IM channel: Slack (default) vs. Discord vs. Telegram.
- **CFG-070** — Embeddings: API (default) with local fallback.
- **CFG-080** — Name/branding: **Summa** — decided (PLAN v2.44); formal trademark + domain
  confirmation pending. The AI members are *agents*; the former working title was "Coworker".
- **CFG-090** — Tier-2 connector priority: which enterprise system first (ERP vs. HRIS vs.
  CRM) — decide when the first company deployment names its pain; not before v1 ships.
- **CFG-100** — Personal-assistant rollout: opt-in per employee (default) vs. org-wide
  mandate.
- **CFG-110** — Business budgets: display-only field on initiatives (default) vs.
  enforcement tied into tier-2 write gates — revisit with the first write-capable
  ERP/WMS connector.
- **CFG-120** — Name/branding: **Summa** — decided (PLAN v2.44); formal trademark + domain
  confirmation pending. The AI members are *agents*; the former working title was "Coworker".
  The deployment perimeter is CFG-020's Keycloak choice and CFG-030's shape decision.
- **CFG-130** — Per-domain proposal strictness: every proposal reviewed (default) vs.
  opt-in auto-publish for low-blast-radius domains (audited, retro-reviewable) — revisit
  when proposal volume drowns owners.
- **CFG-140** — Ask SLA tier defaults: how long each tier runs before breach-and-escalate
  (defaults: `critical` 1h, `standard` to next digest, `bulk` 24h) — tuned with the first
  real org; ask deadlines derive from these unless set per ask (ASK-012).
- **CFG-150** — Model-provider degradation: single provider (default) with manual fallback
  vs. automatic multi-provider routing — decide before the first 24/7 deployment leans on one
  vendor's uptime (SUB-005).
- **CFG-160** — Partitioned-node authority: how long a node may act on cached scopes/DNA
  without a heartbeat — the fenced-lease mechanism is designed (ARC-020…024); the lease
  interval and reconciliation depth are the tunables — decide with Phase 6 node registration.
- **CFG-170** — OKF interchange profile: the Open Knowledge Format (v0.2) is the DNA
  store's exchange profile — export target and ingest source (STG-050…052) — never the
  canonical schema — decided (v2.57); revisit only on an OKF major-version break.

## Additional named parameters (from the body of the plan)

- **CFG-010** — Affinity starvation window: default 24h before the admin ask (ARC-011).
- **CFG-011** — Deadline-less initiative staleness line: default 30 days in the sponsor's
  digest (INT-061).
- **CFG-012** — Critical floor under a partial spend breach: default 5% (SPW-060).
- **CFG-013** — Trigger dedupe window: default 7 days, sized for webhook redelivery
  (SUB-052).
- **CFG-014** — Persistent-hire budget window: default monthly, admin-configurable
  (SPW-032).
- **CFG-015** — Storm-collapse window: identical pending asks collapse into one canonical
  ask within this window (default 1h) (ASK-100).
- **CFG-016** — Per-source ask-creation rate limit: default 60 asks/hour per run, trigger,
  or agent; the storm aggregate closes after one full window back under the limit
  (ASK-101).
- **CFG-017** — Org-wide concurrent agent cap: default 100 active agents (persistent +
  ephemeral combined) — a runaway-spawn backstop sized to trip well before resource
  exhaustion (SPW-031).
- **CFG-018** — Spawn depth cap: default 2 — the global lineage depth SPW-031 enforces
  and the playbook instantiation depth mirrors (SUB-062).
- **CFG-019** — Injection layer token budgets: org snapshot ~1k, glossary ~2k, rules ~4k,
  goal slice ~1k (defaults) — soft budgets; overflow demotes per the DRP-007 order rather
  than truncating (DRP-004).
- **CFG-021** — PAT default lifetime: default 90 days — the expiry SEC-004 names when a
  create sets none; per-token expiry, rotation, and revocation stay row-level (DAT-124).
- **CFG-022** — External-write grace window & reconciliation cadence: both default 5 minutes
  — how long the TTL reaper lets a worker run between prepare and commit before halting it
  (SPW-071), and the age at which the scheduled pass walks a stranded `prepared` row
  (SUB-022).
- **CFG-023** — Sustained-outage ask threshold: default 15 minutes — how long provider
  degradation queues with backoff before the single critical admin ask fires (SUB-005);
  the routing policy around it is CFG-150's decision.
- **CFG-024** — Org-scoped review SLA: default 7 days — the `review_by` source for
  org-scoped proposals when no domain row governs the admin queue (DWP-022); PLAN §4.3's
  global default, given its parameter home (DAT-123's key space).
