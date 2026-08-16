# SPEC-18 — Security & Governance

Source: PLAN.md §10.

## Authentication & credentials

- **SEC-001** — Human authn via local accounts (SSO/OIDC later, CFG-020) + RBAC; agent authn
  via status-fenced PATs and sessions.
- **SEC-002** — Admin lockout is recoverable by design: a server-local CLI reset flow (run on
  the host; physical/filesystem access is the recovery root of trust for self-hosted)
  restores access, and every reset writes an audit entry — degrade to a documented recovery,
  never to silence.
- **SEC-003** — Agent credentials are status-fenced on top of mortal: they authenticate
  only while the agent is `active`, re-validated at every use (CLC-033).
- **SEC-004** — PATs are hashed, shown once, scoped — and mortal: expiry (default 90d,
  CFG-021), rotation (create-replacement + revoke-old in one flow), a revoke endpoint, and
  last-used stamps for compromise detection.
- **SEC-005** — Human PATs and sessions authorize against live authority: effective scopes
  are the grant intersected with the principal's current RBAC, re-evaluated at every use —
  a demotion narrows a standing credential at its next call.

## Enforcement in code

- **SEC-009** — Agent scopes enforced in code (file scope realpath checks, tool allowlists,
  egress CIDR guard); every call audited; append-only audit log.
- **SEC-010** — The scope delegation invariant at spawn: child ⊆ parent, enforced by the
  policy engine (SPW-030).
- **SEC-011** — Spawn safety per the SPEC-11 policy engine (SPW-070): quotas, depth cap
  (default 2, CFG-018), TTL reaper, spend circuit-breaker, approval gates on persistent
  hires; ephemeral workers get connector-sandboxed, task-scoped workspaces only.
- **SEC-012** — Node trust: enrollment via one-time tokens + keypair identity, revocation
  from the console, node id stamped on every audit event where one exists (DAT-121);
  for remote nodes, egress
  allowlisting and secret handling route through the control plane / gateway; the console
  surfaces each node's trust level explicitly (ARC-002/003).

## DNA write policy & secrets

- **SEC-020** — Agents propose, owners publish; compartment access enforced on retrieval and
  injection (DRP-005, DGV-002).
- **SEC-030** — The secrets scanner covers all proposals, item-level DNA writes (DWP-060),
  memory, and ingested direct edits
  (STG-010); scanner hits quarantine to the owner with an audited admin override — a false
  positive is a visible ask, never a silent wedge in the write path.

## Secrets storage & transport

- **SEC-040** — Secrets in OS-encrypted storage (OS keyring / Tauri stronghold; exact
  mechanism is the Phase-0 spike DLV-043); redaction before any egress to providers covers
  secrets *and* PII.
- **SEC-041** — Webhooks are signature-verified; the console is served over localhost or TLS
  behind the company's reverse proxy in server mode.
- **SEC-042** — DNA repo integrity per STG-020/021: single direct writer, signed commits and
  refs, non-fast-forward refusal, protected-branch prerequisites verified at startup.

## Erasure & residency

- **SEC-050** — Erasure requests pseudonymize the append-only ledgers under legal-hold guards
  (STG-030…034); node regions + domain residency constraints govern placement (ARC-040…043).
