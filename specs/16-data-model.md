# SPEC-16 — Data Model

Source: PLAN.md §7. This module is the representational ground truth; state names and field
names used across the suite are defined here. v1 tables (session/run/message/skill/connector)
carry over and are out of scope here.

> **Self-containedness debt** (from PLAN §7): before Phase 0, inline or link the carried-over
> v1 specs; if any v1 deployment exists, add migrations (v1 `approvals` → asks of kind
> `approval`; per-agent KBs → DNA domain imports).

## Tables

```
humans         (id, name, email, rbac 'admin'|'owner'|'member'|'viewer', auth json,
                deputy_member_id?, timezone?, working_hours json?, created_at, deactivated_at?)
agents         + owner_human_id, class 'persistent'|'ephemeral', spawned_by member?, ttl_at,
                budget_cap, lineage_depth, template_id?, template_version?,
                status 'requested'|'active'|'suspended'|'retiring'|'archived'
role_templates (id, name, version, class 'persistent'|'ephemeral-subagent', body json,
                default_scopes json, status 'draft'|'active'|'retired')
nodes          (id, name, kind 'local'|'remote', capabilities json, region?, claim json?,
                last_heartbeat, pubkey, enrolled_at, revoked_at?, status 'trusted'|'revoked')
dna_domains    (id, name, owner_human_id, access 'public'|'domain'|'named',
                named_readers json, store 'git'|'db-only', sod 'off'|'reviewer-distinct',
                review_sla_days int default 7, residency?, status 'active'|'archived'
                default 'active')
dna_cards      (id, domain_id, title, definition_md, refs json, provenance json, version,
                status 'draft'|'active'|'retired' default 'active')
dna_rules      (id, domain_id, statement_md, machine_hint json?, effective_from, effective_to?,
                supersedes_id, status 'active'|'superseded'|'lapsed')
dna_decisions  (id, domain_id, context_md, outcome_md, decided_by member, decided_at)
dna_glossary   (id, domain_id?, term, definition, aliases json,
                status 'draft'|'active'|'retired' default 'active')
dna_goals      (id, domain_id?, quarter?, statement_md, owner member,
                status 'active'|'met'|'missed'|'retired', inject 'always'|'linked',
                effective_from, effective_to?)
dna_proposals  (id, kind 'card'|'rule'|'decision'|'goal'|'glossary'|'edit', payload json,
                revision int default 1, proposed_by member, provenance json,
                status 'open'|'published'|'rejected'|'withdrawn', reviewed_by?, created_at,
                reviewed_at?, review_by?)
asks           (id, kind 'approval'|'question'|'assignment'|'spawn_request', from member,
                to member, payload json, initiative_id?, workspace_id?,
                status 'pending'|'answered'|'expired'|'withdrawn', deadline, created_at,
                sla_tier 'critical'|'standard'|'bulk', escalation json,
                expiry_behavior 'deny'|'escalate'|'reassign', responded_at?,
                quorum_required int default 1, responses json, collapsed_count int default 1)
initiatives    (id, title, goal_ref?, decision_ref?, sponsor member, lead member,
                status 'proposed'|'active'|'paused'|'closed', business_budget json?,
                deadline?, closed_at?, depends_on json?)
board_tasks    + assignee_member_id?, initiative_id?   (runs carry initiative_id? likewise)
workspaces     + initiative_ids json?, domain_ids json?, node_id?, claim_epoch int default 0,
                lease_expires_at?, participants json, archived_at?
triggers       + criticality 'standard'|'critical' default 'standard'
playbooks      + criticality 'standard'|'critical' default 'standard'
spend_ledger   (id, member_id, run_id?, spawn_id?, kind 'reserve'|'settle'|'release',
                tokens_in/out, cost, pricing_version, at)
trigger_firings (id, trigger_id, idempotency_key, fired_at, run_id?)
external_writes (id, run_id, connector, op, idempotency_key,
                status 'prepared'|'committed'|'compensated'|'failed', prepared_at, resolved_at?)
data_holds     (id, kind 'member'|'domain', subject_id, reason_md, created_by, released_at?)
groups         (id, name, leader_member_id?, status 'active'|'archived', created_at)
group_memberships (group_id, member_id, added_by member, added_at, removed_at?)
audit_events   (id, at, actor member|'system', action, object_type, object_id, detail json,
                node_id?, origin 'live'|'replay' default 'live')
pats           (id, member_id, name, token_hash, scopes json, created_at, expires_at,
                revoked_at?, last_used_at?)
governance_settings (key, value json, edited_by member, edited_at)
memory_items   (id, tier 'personal'|'project'|'proposal', member_id?, workspace_id?,
                content_md, provenance json, tainted bool default false, created_at,
                reviewed_by?, reviewed_at?)
```

## Representational invariants (DAT)

- **DAT-010** — `humans.deactivated_at` is offboarding's whole terminal marker: ask-chain
  walks skip deactivated members; the last-admin guard counts admins with
  `deactivated_at IS NULL`; rehire is a new row (ORG-022).
- **DAT-011** — `humans.deputy_member_id` must reference a humans row that is neither the
  member nor a viewer; agent, self, and viewer deputies are refused at write (ORG-060/061).
- **DAT-020** — `agents.owner_human_id` is derived at spawn per ORG-051 and carried by the
  walks; the chain terminates at a human (PRN-004).
- **DAT-021** — `budget_cap` null = worker-uncapped (org-wide caps and the breaker still
  bind); the window matches class per SPW-032.
- **DAT-022** — Ephemeral status mapping is 1:1 (CLC-001); `template_id` null marks a
  customRole hire (CLC-003).
- **DAT-030** — `role_templates` unique on (class, name, version); class stable across a
  name's versions — live and retired rows alike, a reused name carrying its class (TPL-002/
  003/045); catalog writes are admin, audited (TPL-004).
- **DAT-040** — `nodes.capabilities` is heartbeat-owned (ARC-014); `claim` carries the
  epoch-fenced lease (ARC-020); `status 'revoked'` is terminal with the keypair refused
  everywhere (ARC-016).
- **DAT-050** — `dna_domains`: row-write authority split per DGV-005; name unique among
  non-archived (DNC-060); `named_readers` evaluated against live state (DGV-003); owner must
  hold role owner/admin at write (DGV-006); archived = read-only history with the holdings
  rule of DGV-040.
- **DAT-060** — Item lifecycles per SPEC-03 (cards/glossary draft|active|retired; rules
  active|superseded|lapsed with window truncation; decisions lifecycle-free; goals terminal-
  immutable). Frozen-history updates are refused at every surface (DWP-062).
- **DAT-061** — `dna_rules.supersedes_id` intra-domain, chains linear (DNC-024/025);
  displacement effective at the superseder's `effective_from` (DNC-023).
- **DAT-070** — `dna_proposals.proposed_by` must hold a write surface (DWP-010); revisions
  serialize on the domain lock (DWP-031); kind is revision-immutable (DWP-033).
- **DAT-080** — `asks`: quorum addressing per ASK-051; responses ledger behind N-of-M;
  `collapsed_count` folds identical asks (ASK-100); `workspace_id` keys the domain-owner
  escalation hop and digest grouping; the system originator (ASK-031) is a reserved
  non-member value of `from`; `to` reserves the broadcast addressee `admins` — every
  active admin, evaluated at render and respond time (ASK-055) — carried by every ask
  routed to 'an admin' at creation or on escalation, never a single admin's id.
- **DAT-081** — `initiatives.sponsor` pinned human (INT-001); `goal_ref` live at write
  (INT-011); `depends_on` acyclic with live edges (INT-070).
- **DAT-090** — `workspaces.domain_ids` is the ordered binding — first entry primary
  (admin-editable), unbinding the primary promotes the next, empty = domainless with defined
  fallbacks; topology ops remap with ids stable; binding writes serialize behind the affected
  domains' write locks (DGV-052); a pending spawn approval keyed on the binding re-keys at
  the edit (DGV-046).
- **DAT-091** — `workspaces.participants` evaluates against live state and the walks scrub it
  (DGV-003, OFB-014).
- **DAT-100** — `spend_ledger` kinds reserve|settle|release meter caps as reserved + settled
  (SPW-033/034).
- **DAT-101** — `trigger_firings` unique on (trigger_id, idempotency_key) within the dedupe
  window (default 7d, CFG-013); replays return the original run (SUB-052).
- **DAT-102** — `external_writes` staged lifecycle per SUB-020/022.
- **DAT-110** — `data_holds`: kind `member` freezes erasure; kind `domain` freezes
  history-rewrite remediation and db-only export/deletion, and the hold-refused topology ops
  check it (DGV-017, STG-034); created/released through the admin endpoints, audited.

## Completion tables (v2.46)

- **DAT-120** — Member references are one keyed union: every member-typed column
  (`asks.from/to`, `initiatives.sponsor/lead`, `board_tasks.assignee_member_id`,
  `groups.leader_member_id`, `pats.member_id`, `group_memberships.member_id`, among others)
  carries `h:<humans.id>` or `a:<agents.id>` — never a bare integer — and resolves against
  the live row of its keyed kind (ORG-001's shared namespace, given a representation). The
  reserved `system` originator is the one non-member value a member-typed column may carry,
  `asks.from` alone (DAT-080); `asks.to` carries the suite's one reserved addressee —
  `admins`, the live-evaluated admin broadcast (DAT-080).
- **DAT-121** — `audit_events` is append-only: rows are inserted, never updated or deleted;
  every refusal, write door, credential use, and admin act lands one row (SEC-009,
  NFR-001); `node_id` stamps the acting node where one exists (SEC-012); restore re-appends
  pre-restore segments with `origin 'replay'` (DLV-055); erasure pseudonymizes `actor`
  without deleting the row (STG-030).
- **DAT-122** — `groups` is unique on name among non-archived; `group_memberships` evaluate
  against live state with the walks writing the removals (ORG-041); the Leader-post write
  guard is ORG-042's.
- **DAT-123** — `governance_settings` is the single persistence home for the policy and
  quota values behind API-050 — one row per named parameter, the CFG catalog its key space;
  edits are admin, audited; a fresh deployment ships the documented defaults as its initial
  key set.
- **DAT-124** — `pats` stores `token_hash` only, never plaintext (SEC-004); status-fencing
  and live-authority intersection evaluate at use, never on the row (SEC-003/005); expiry,
  rotation, revocation, and last-used are row-writes on the one live row per token.
- **DAT-125** — `memory_items`: tier per the SUB-040 classifier; `tainted` propagates per
  SUB-041 and clears only through `reviewed_by/at` — never by passage of time; personal-tier
  rows archive inert with their member (CLC-023) — the member's archived status is the
  inertness, no row rewrite needed.
