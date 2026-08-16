# SPEC-17 — API Surface

Source: PLAN.md §9 (delta over v1 endpoints for agents, sessions, messages, memory,
workspaces, automated-tasks, triggers, playbooks, runs). General contract: every refusal on
this surface is the PRN-009 form — refuse, audit, ask where a human decision is needed.

## Auth & org

- **API-001** — `POST /auth/login` (human sessions); PATs for agents/services.
- **API-002** — `POST /auth/pats` · `POST /auth/pats/:id/revoke` — scoped create, expiry +
  rotation + last-used stamps (SEC-004).
- **API-003** — `POST /org/bootstrap` — first-run create company + first admin; refused once
  any human exists (transactional singleton guard, not check-then-act).
- **API-004** — `CRUD /org/humans` · `/org/members` · `GET /org/lineage`.
- **API-005** — `POST /org/humans/:id/erasure` — admin, audited, honors `data_holds`
  (STG-030…034).
- **API-006** — `POST /org/humans/:id/offboard` — admin; runs the OFB-001 walk; transactional
  last-admin guard (OFB-020).
- **API-007** — `CRUD /org/groups` · `POST /org/groups/:id/archive` — the groups write
  surface: archival is ORG-040's admin door (read-only history, DAT-122); membership
  add/remove derives from live state per ORG-041; the Leader set rides the ORG-042 write
  guards; every write audited.

## Nodes

- **API-010** — `POST /nodes/enroll` (one-time token exchange) · `GET /nodes` ·
  `POST /nodes/:id/revoke` — revocation surfaces the rebind ask for every bound workspace at
  revocation time, halts in-flight runs with fold-back and reconciliation, kills claims
  (ARC-016) · `PUT /nodes/:id` — admin; `region` and metadata (the `name` row) only, never
  capabilities (ARC-015); a region edit re-validates every residency-constrained placement
  bound to the node (ARC-042).
- **API-060** — Node runtime surface (node-authenticated via its enrolled keypair, SEC-012):
  `POST /nodes/:id/heartbeat` writes `last_heartbeat` and capability state (ARC-014) ·
  `POST /nodes/:id/claims` acquires or renews a workspace claim as an epoch-fenced lease
  (ARC-020; a stale epoch is refused at this mediated boundary per ARC-024, demoed at
  DLV-053) ·
  `POST /nodes/:id/work/pull` fetches queued runs for workspaces the node holds a live
  claim on · `POST /nodes/:id/runs/:runId/report` lands results, artifacts, and spend
  ledger lines. Every endpoint refuses a revoked node (ARC-016); refusals take the PRN-009
  form like any other.

## DNA

- **API-020** — `CRUD /dna/domains` — row-write authority per DGV-005: create/archive,
  structural attributes (`store`, `sod`, residency), owner re-pointing = admin; `access`,
  `named_readers`, `review_sla_days` = owner; every row-write audited.
- **API-021** — `CRUD /dna/cards|rules|decisions|glossary|goals` — the item-level publish
  path per DWP-060…064: lock-serialized, contradiction re-check, sod routing, secrets scan,
  create/update/retire only (never delete; decisions create-only).
- **API-022** — `POST /dna/proposals` · `POST /dna/proposals/:id/review` (publish|reject) ·
  `POST /dna/proposals/:id/withdraw` · `POST /dna/proposals/:id/amend` (revision during
  review) · `GET /dna/review-queue`.
- **API-023** — `POST /dna/domains/:id/split|merge|rename|archive` — governed topology ops
  per SPEC-06; archive refuses live-set holdings (DGV-040); owner-addressed pending asks
  settle inside the event (DGV-047).
- **API-024** — `GET /dna/goals` (goal-slice reads), governance reads.

## Catalog & spawning

- **API-030** — `CRUD /role-templates` — versioned catalog; create/publish/retire are admin
  writes, audited (TPL-004).
- **API-031** — `POST /spawn` · `GET /spawn/:id` — spawn requests with approval and
  spawn-storm monitoring (SPEC-11); the requester's retraction rides the approval ask's
  withdraw endpoint (API-040, SPW-047).
- **API-032** — `POST /agents/:id/retire · /suspend · /resume` — lifecycle acts on the
  agent (not the spawn request); authority per CLC-010.
- **API-033** — `POST /agents/:id/promote` — files the promotion ask for a customRole
  hire; authority: the hire's owner human or an admin; the ask snapshots identity files and
  effective scopes at creation; the accept names the placement and publishes per
  TPL-040…044; one live promotion ask per hire — a second is refused at filing (TPL-046).

## Work, initiatives, workspaces

- **API-040** — `CRUD /asks` · `POST /asks/:id/respond` · `POST /asks/:id/withdraw`
  (originator retract; the ASK-030/032 door rules apply) · `WS: ask.requested, ask.answered`.
- **API-041** — `CRUD /initiatives` · `POST /initiatives/:id/activate|pause|resume|close` —
  transition authority per INT-020…023; close runs the dependency check.
- **API-042** — `CRUD /board-tasks` — assign to any ask-eligible member (viewer and
  non-active refused at write, ORG-031).
- **API-043** — `POST /workspaces/:id/rebind` — admin affinity failover; refuses a target
  node lacking required capabilities (ARC-012).
- **API-044** — `POST /workspaces/:id/archive` — admin; runs the CLC-040 walked transition.

## Memory

- **API-045** — `POST /memory/:id/review` — the taint-clearance door (SUB-041): the
  spawner's owner for personal-tier rows, the domain owner for project-tier rows —
  DAT-125's `reviewed_by/at`; proposal-tier rows clear through the review itself (DWP-003).

## Governance

- **API-050** — `GET /governance/policies|quotas|spend` (console screens 12 & 14) ·
  `PUT /governance/policies|quotas` — admin, audited; per-object settings ride their own
  CRUD; cap edits are claim-scoped (SPW-036).
- **API-051** — `POST /governance/spend/overruns/:id/ack` — admin; lifts the SPW-035 reserve
  gate (`:id` = the overshot settle's spend-ledger row).
- **API-052** — `POST /governance/holds` · `POST /governance/holds/:id/release` — admin,
  audited; `data_holds` lifecycle (DAT-110).

## Refusal envelope

- **API-061** — One refusal envelope on the whole surface: every refused write or action
  responds 4xx with the machine-parseable body `{code, message, audit_event_id, ask_id?}`.
  `code` is a stable enum — `validation | eligibility | not_found | conflict |
  rate_limited | gate` — mapped per family: 422 validation, 403 eligibility (the write-door
  guards), 404 not_found, 409 conflict (lock serialization, racing transitions, stale
  epoch), 429 rate_limited (ASK-101), with `gate` — guard refusals that carry policy
  context (quota, depth, spend halt) — mapping to 403 alongside eligibility: the status is
  coarse, `code` the fine grain. `audit_event_id` links the row the refusal wrote and
  `ask_id` the ask a PRN-009/NFR-001 refusal raised — a refusal without an audit row is a
  bug, not a response.
