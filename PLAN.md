# Coworker — the operating system for a hybrid human + AI company

> A self-hosted platform where human employees and AI coworkers ("Coworkers") work as one
> organization. A governed **Company DNA** — the shared knowledge, rules, decisions, and goals of
> the company — keeps every member coherent. Any member, human or agent, can spawn new agents under
> explicit governance. Work compounds: every task leaves behind learning; learning improves the DNA;
> the DNA makes the next task better.
> ("Coworker" is a working title — see §14.)

**What changed in v2** (vs. the local-first single-operator plan): the company is the unit of
deployment, not the individual. Humans are first-class members alongside agents; the knowledge base
is promoted to a central, governed **Company DNA** with proposals and review; agents can spawn
agents (governed); topology becomes a **control plane + execution nodes**, with a single-process
mode so small teams start simple.
>
> **Review pass (v2.1)**: v1 cut line drawn at Phase 4 (§11) · Asks promoted to a designed
> subsystem (§8.10) · node trust model made explicit (§3, §10) · prompt injection added to risks
> (§13) · privacy carve-out for the git-backed DNA store (§4.5) · rule-applicability semantics +
> prompt budgets (§4.2) · Phase-0 spikes + per-phase acceptance criteria (§11).
>
> **Consistency pass (v2.2)**: milestone arithmetic corrected (§11) · headless approval policy
> mapped onto Ask tiers (§8.1) · ephemeral spawn vs. template allowlist disambiguated (§6.1) ·
> ask escalation/reassign semantics closed out (§7, §8.10) · node management API added (§9) ·
> human offboarding defined (§5).

---

## 1. Product vision

A company runs on shared context: what we know, how we work, what we decided, what we're trying to
achieve. Today that context lives in people's heads, chat scrolls, and stale wikis. **Coworker**
makes it explicit and operational:

- **Members** — humans and Coworkers in one org chart. Humans direct, approve, review, and own
  domains; Coworkers execute, learn, and propose.
- **Company DNA** — the single source of coherence: knowledge cards, rules, decision records,
  SOPs/playbooks, glossary, org facts, goals and metrics. Every member reads it; changes to it are
  governed.
- **Work ledger** — chat tasks, automations, and playbooks all create runs with results, artifacts,
  and to-dos on a shared Task Board any member can hold or assign.
- **Governed spawning** — delegation by hiring: a human or agent can spawn a new Coworker for a
  role or a task, inside policy, budget, and lineage constraints.

The core loop that makes the company improve itself:

```
   ┌──────────────────── work (chat · automation · playbook) ───────────────────┐
   │                                                                            │
 humans & Coworkers ── learning ──► memory service ── classifies ──┐            │
   ▲                                    personal / project         │            │
   │                                                    DNA proposals ▼            │
   └── guided by ── DNA (rules · glossary · cards · decisions) ◄── review ── owners ──┘
```

The five pillars survive from v1 — identity, memory, skills, division of labor, permission
boundaries — with a sixth raised to the top: **shared, governed context**.

---

## 2. Principles

1. **DNA is the source of coherence**: all durable company knowledge lives in one governed place; agents never silently fork it into private stores.
2. **Local-capable, company-first**: one binary runs everything for a small team; the same services split into control plane + nodes as the company grows.
3. **Every capability is a guarded tool**: file scope, tool scope, egress guard, and audit enforced in code, never in prompts.
4. **Agents are accountable to humans**: every Coworker has a human owner in its lineage chain; ephemeral workers roll up to their spawner, whose chain terminates at a human.
5. **Spawning is delegation, not reproduction**: a spawned Coworker's permissions are a subset of its spawner's; budgets and TTLs bound it; policy gates it.
6. **Files for humans, database for machines**: DNA, identity, and memory are git-friendly markdown humans can read, edit, and review; runs, audit, and indexes live in SQLite.
7. **Role-agnostic core**: roles are data (templates, skills, connectors, scopes) — engineer, secretary, HR, finance, all the same runtime.
8. **Governance proportional to blast radius**: ephemeral task workers need quotas, not paperwork; persistent hires and DNA changes need review.

---

## 3. Architecture

### Topology: control plane + execution nodes

```
   Humans' browsers ──────┐                    ┌── Model gateway ──► OpenAI-compat / Anthropic / Ollama
   (console: work, DNA,   │                    │
    org, approvals)       ▼                    │
┌───────────────────────────────────────────────────────────────────────────┐
│                       Control plane (self-hosted server)                  │
│  Console UI  │  Human auth + RBAC  │  Org registry (members, lineage)     │
│  DNA service (storage · retrieval · proposals · review · compartments)    │
│  Governance engine (policies, budgets, spend ledger)                      │
│  Task board + Asks (approvals/questions/assignments routed to members)    │
│  Trigger engine │ Playbook engine │ Scheduler │ Audit │ REST + WS hub     │
└──────────────┬────────────────────────────────────────────────────────────┘
               │ node protocol (register, heartbeat, claim runs, stream events)
   ┌───────────┴──────────┐   ┌──────────────────────┐   ┌──────────────────────┐
   │ Node A (dev machine) │   │ Node B (office box)  │   │ Node C (server-local)│
   │ session workers      │   │ secretary + HR       │   │ automations, KB jobs │
   │ near repos/tools     │   │ near email/calendar  │   │                      │
   └──────────────────────┘   └──────────────────────┘   └──────────────────────┘
```

- **Single-process mode**: control plane + one node in one process, console at `localhost` — this is
  the MVP path; nothing is lost when scaling out later.
- **Workspace affinity**: runs are scheduled to the node where the workspace's files/connectors
  live (an engineer Coworker runs on the machine with the repo; the secretary's mailbox connector
  lives wherever it was authorized).
- **Node trust model**: remote nodes are *trusted compute*, not enforcement boundaries — scope,
  egress, and audit code runs on the node, so a compromised node can bypass it. Nodes enroll via
  one-time tokens, authenticate with a keypair identity on every connection, are revocable from
  the console, and every audit event records the executing node id. Enforcement that must survive
  a hostile node (egress allowlisting, secret handling) routes through the control plane / model
  gateway for remote nodes; single-process mode has no such exposure. 24/7 automations require an
  always-on node — workspace affinity on a sleeping dev machine is for interactive work only.
- **Stack** (unchanged from v1): Node 22 + TS daemon, React + Vite + Tailwind + shadcn console,
  SQLite (WAL) + sqlite-vec + FTS5, `isolated-vm` playbook sandbox, croner triggers, MCP connectors,
  Tauri shell as Phase-8b polish.

---

## 4. The Company DNA

The DNA is what makes fifty agents and ten humans feel like one company. It is *not* a dump of
documents; it is curated, structured, versioned context.

### 4.1 Content model

| Type | What it holds | Example |
|---|---|---|
| **Cards** | Atomic knowledge: definitions, how-tos, facts, with provenance | "Our refund policy has a 30-day window (ref: policy.doc §4)" |
| **Rules** | Normative statements with effective dates; supersession chains; optional machine hints | "Invoices > $10k require two approvals" |
| **Decisions** | Decision records: context, options chosen, outcome, owner, date | "We chose Postgres over Mongo for billing (2026-05-12, Alice)" |
| **SOPs** | Process definitions; the durable ones are executable playbooks | "Onboarding checklist → onboarding playbook" |
| **Glossary** | Canonical terminology, mapped to aliases | "ARR = annual recurring revenue (not 'annual revenue')" |
| **Org facts** | Who exists (humans + Coworkers), teams, domain ownership | generated from the org registry, read-only |
| **Goals & metrics** | Company/quarterly objectives and KPIs; work links to them | "Q3: cut support first-response to < 2h" |

### 4.2 Read path — how coherence actually happens

Every run's system prompt is assembled with:
- **Always injected**: the org snapshot (who's who), the glossary slice relevant to the task's
  domain, and all *applicable rules* for the workspace's domains. "Applicable" has defined
  semantics: a rule applies when its domain intersects the workspace's domains and its effective
  window covers the run (superseded rules drop out of injection automatically); `machine_hint`
  narrows matching where present. Each layer carries a token budget (org snapshot ~1k, glossary
  slice ~2k, rules ~4k — soft limits, configurable); overflow demotes items to retrieval (rules
  overflow into the searchable DNA index) rather than truncating silently. Injection stays
  deterministic per (domain set, DNA version) so it is testable (§12).
- **Retrieved on demand**: cards and decisions via hybrid search (BM25 + vector over the card
  index) — same retrieval machinery as v1's KB, now pointed at DNA.
- **Cited in answers**: responses reference cards; the console (and IM) renders citations that open
  the source card with its provenance.

### 4.3 Write path — learning without corruption

The memory service classifies what a run learned into three tiers:

| Tier | Scope | Write path |
|---|---|---|
| Personal memory | One Coworker | automatic (dedupe + timeline + versions, as v1) |
| Project memory | One workspace/project | automatic, same machinery |
| **DNA proposal** | The company | **always proposed, never auto-published** |

A DNA proposal carries the change (new card / rule / decision / edit), its provenance (source
session, docs, or observation), and the proposing member. Domain owners review from a queue in the
DNA console (diff view, provenance, impact hints); publish creates a version and an effective date;
reject leaves an audit trail. Humans can also propose directly, and can edit in their own tools —
the store is git-backed markdown, so a PR workflow is possible for teams that want it.

### 4.4 Governance

- **Domains & compartments**: DNA is partitioned into domains (Engineering, Finance, HR…) each with
  a human owner and an access policy (`public` | `members of domain` | `named members`). Retrieval
  respects the reader's access — the HR intern's Coworker never sees salary cards.
- **Provenance**: every card/rule/decision records where it came from; uncited claims are flagged
  during review.
- **Freshness**: review cadence and stale flags per item; scheduled DNA quality checks (a reviewer
  agent drafts a report; humans decide).
- **Conflicts**: new rules supersede old ones explicitly (chains retained); the review UI shows
  contradictions detected at proposal time.

### 4.5 Storage

```
~/.coworker/dna/            (or a company git repo — the canonical store)
  domains/<domain>/cards/*.md, rules/*.md, decisions/*.md, glossary.md
  goals/<quarter>.md
```
Markdown + frontmatter (id, version, effective dates, provenance, access); the control plane
maintains the SQLite/FTS/vector index over it. Humans can read and edit their company's brain with
any editor; git history *is* the DNA timeline.

**Privacy carve-out**: git history is effectively immutable, which collides with deletion
obligations (GDPR-style erasure, offboarded-employee data, HR/Finance records). Domains may
declare `store: 'db-only'` — HR and Finance default to it: content lives in SQLite with
export-on-demand and never enters the git store; the git timeline is reserved for non-sensitive
domains. If sensitive material lands in git by mistake, remediation is a documented history
rewrite (rotate the repo, notify domain owners) — decided here, not improvised under a deadline.

---

## 5. The org model: humans + Coworkers as members

- **Members**: `humans` (identity, RBAC role) and `coworkers` (identity files, scopes) share one
  member namespace — the task board, asks, groups, and lineage all reference members.
- **Human RBAC**: `admin` (everything), `owner` (a DNA domain + its Coworkers), `member` (work,
  propose DNA, spawn within policy), `viewer` (read-only). Auth starts as local accounts; SSO/OIDC
  later.
- **Asks — the universal interrupt**: approvals, questions, assignments, and spawn requests are all
  *Asks*: routed to a member (human or agent) with payload, deadline, and escalation policy —
  SLA tiers, expiry semantics, and escalation chains are a designed subsystem (§8.10), not just a
  routing table.
  Humans answer in the console (later IM/email digests); agents answer via their session worker.
  Approvals from v1 become Asks of kind `approval`.
- **Shared Task Board**: to-dos come from run results, playbook nodes, or any member; assignable to
  humans or Coworkers; visible org-wide within access scopes.
- **Groups/teams** mix humans and Coworkers (v1 kept agent-only groups; v2 unifies — a local
  Coworker still acts as Leader for execution routing).
- **Accountability invariant**: every Coworker row carries `owner_human_id`; spawned workers carry
  `spawned_by`; the chain must terminate at a human. Enforced at spawn time.
- **Offboarding**: deactivating a human reassigns their owned DNA domains, open asks, and
  dependent Coworkers via the same dependency check as retiring a Coworker (§6.3); audit history
  is retained, and their personal data is subject to the §4.5 deletion carve-out.

---

## 6. Spawning & lifecycle

Spawning is how the org flexes: a manager hires a specialist; an agent spinning up subtask workers.
One mechanism, two classes:

| | **Persistent hire** | **Ephemeral worker** |
|---|---|---|
| Purpose | Long-lived role (new org member) | Bounded subtask delegation |
| Created by | Humans (console), agents via spawn tool (usually approval-gated) | Agents freely within quota |
| Identity | Full IDENTITY/STYLE/HANDBOOK + memory accrual | Minimal: purpose, prompt, sandbox profile |
| DNA access | Read per compartments; proposes via review | Read-only; **cannot propose DNA changes** |
| Memory | Own personal memory | Folds results back into spawner's session/project memory, then dies |
| Bounds | Budget policy, owner_human_id | TTL (default 24h), spend cap, task-scoped workspaces |
| Lifecycle | requested → active → retiring → archived | spawned → running → done → reaped |

### 6.1 The spawn request (a guarded tool + console action)

```
spawn({ from: templateId | customRole, class: 'persistent'|'ephemeral',
        purpose, workspaceBindings, scopeCeiling: inherited-subset,
        budgetCap, ttl? })
```

`customRole` is for persistent hires (proposed by humans, or by agents behind an approval gate);
ephemeral workers must instantiate whitelisted subagent templates (§6.2) — no free-form
ephemeral roles.

### 6.2 Policy engine (hard-coded, not prompt-enforced)

- **Scope delegation**: child's file/tool/connector scopes ⊆ parent's. A secretary cannot spawn
  anything with repo write access she doesn't have.
- **Allowlists**: which templates each member class may spawn; ephemeral workers restricted to
  whitelisted "subagent" templates.
- **Quotas & caps**: max concurrent ephemeral workers per spawner, global spawn depth (default 2),
  org-wide concurrent Coworkers, per-spawn and org-wide spend caps metered by the spend ledger.
- **Approval gates**: persistent hires → Ask to the domain owner (or admin); agent-spawned
  ephemeral workers exceeding quota → Ask to the spawner's owner human.
- **Runaway protection**: depth cap, rate limits, TTL reaper, budget circuit-breaker (org spend
  ceiling halts all spawns and automations with a loud Ask to admins).

### 6.3 Lineage

`spawned_by` chains render as an org graph in the console: who created whom, why (purpose), spend,
and current status. Retiring a persistent Coworker requires resolving its dependents (automations,
playbooks, paired IM sessions) — the same dependency check as deleting a skill, applied to staff.

---

## 7. Data model (v2 delta)

New/changed tables (v1 session/run/message/skill/connector tables carry over):

> **Self-containedness**: this section, §8, and §9 are deltas against a v1 design doc that is not
> in this repo. Before Phase 0 starts, inline or link the carried-over v1 specs here. If any v1
> deployment exists, add a migration section: v1 `approvals` rows → `asks` of kind `approval`;
> per-Coworker KBs → DNA domain imports.

```
humans         (id, name, email, rbac 'admin'|'owner'|'member'|'viewer', auth json, created_at)
coworkers      + owner_human_id, class 'persistent'|'ephemeral', spawned_by member?, ttl_at,
                 budget_cap, lineage_depth, status 'requested'|'active'|'retiring'|'archived'
nodes          (id, name, kind 'local'|'remote', capabilities json, last_heartbeat,
                 pubkey, enrolled_at, revoked_at?, status 'trusted'|'revoked')
dna_domains    (id, name, owner_human_id, access 'public'|'domain'|'named')
dna_cards      (id, domain_id, title, definition_md, refs json, provenance json, version, status)
dna_rules      (id, domain_id, statement_md, machine_hint json?, effective_from, supersedes_id, status)
dna_decisions  (id, domain_id, context_md, outcome_md, decided_by member, decided_at)
dna_glossary   (id, domain_id?, term, definition, aliases json)
dna_proposals  (id, kind 'card'|'rule'|'decision'|'edit', payload json, proposed_by member,
                 provenance json, status 'open'|'published'|'rejected'|'withdrawn', reviewed_by?, at)
asks           (id, kind 'approval'|'question'|'assignment'|'spawn_request', from member, to member,
                 payload json, status 'pending'|'answered'|'expired', deadline, created_at,
                 sla_tier 'critical'|'standard'|'bulk', escalation json,
                 expiry_behavior 'deny'|'escalate'|'reassign', responded_at?)  -- supersedes approvals;
                 escalate/reassign close the expired ask and open a linked successor ask (§8.10)
board_tasks    + assignee_member_id?  (assignee references the unified member namespace)
spend_ledger   (id, member_id, run_id?, spawn_id?, tokens_in/out, cost, pricing_version, at)
```

v1's per-Coworker knowledge bases are subsumed: a "KB" is now a DNA domain import (sources are
ingested and compiled into cards inside a domain), plus retained per-project reference folders.

---

## 8. Subsystem designs (carried from v1, updated)

- **8.1 Agent runtime** — unchanged core (prompt assembly → guarded loop → structured result), with
  two v2 changes: (a) the always-injected DNA layer (org snapshot, glossary slice, applicable
  rules) precedes per-Coworker context; (b) headless approval policy now routes into **Asks** —
  `auto_deny` (default) | `queue_until_morning` (Task Board digest) | `escalate_im`. These are
  Ask tiers in disguise (§8.10): `escalate_im` → `critical`, `queue_until_morning` → `standard`
  (next digest), `auto_deny` → expiry behavior `deny`; the configuration surface is the ask
  policy, not a separate one. Scope
  enforcement, egress guard, write-lock, stop semantics, cost metering as in v1.
- **8.2 Tools & MCP** — built-ins (`fs.*`, guarded `shell.exec`, `web.*`, `kb.search` → `dna.search`,
  `memory.write`) plus **`spawn`** as a guarded tool. Egress guard unchanged. Connector tiers
  unchanged (tier 1 = email/calendar/docs).
- **8.3 Memory service** — now three-tier classifier (personal / project / DNA proposal) with the
  v1 machinery (dedupe, timeline, versions, secrets scanner) under it.
- **8.4 Skills** — unchanged; domain-organized packs; uninstall dependency checks.
- **8.5 Trigger engine** — schedule/API/event triggers unchanged; every firing is a run of the same
  session worker; API triggers gain PAT scopes for external callers.
- **8.6 Playbook engine** — DSL and sandbox unchanged; `worker()` targets any member (human targets
  create an assignment Ask); spawn-class playbooks (fan-out workers) built on §6 ephemeral workers.
- **8.7 DNA engine** — inherits v1 KB machinery (ingest → chunk → embed → cards → hybrid retrieval →
  citations) extended with domains, proposals, review queue, and glossary/rule injection.
- **8.8 Groups & IM** — unified human+agent teams; IM pairing routes to a Coworker whose asks
  escalate to the channel.
- **8.9 Console screens** — v1 screens 1–9, plus four new: **10. Org & People** (members, RBAC, lineage graph,
  retirement flows) · **11. DNA console** (browse cards/rules/decisions per domain, review queue
  with diffs and provenance, proposal history, glossary editor) · **12. Governance** (policies,
  quotas, spend dashboard, spawn audit) · **13. Ask inbox** (SLA indicators, batched digests,
  one-line accept/deny with diff links).
- **8.10 Asks — the human-attention subsystem** — system throughput is bounded by ask-response
  latency, so asks are engineered, not merely routed. **SLA tiers**: `critical` (blocks a
  customer-facing or money-moving run — interrupt-grade push, console + IM), `standard` (blocks a
  run — next digest), `bulk` (non-blocking — daily digest, batched). **Expiry semantics** are
  explicit per ask: `deny` (default for approvals — an expired approval is a no), `escalate`
  (route up the chain), `reassign` (fall back to a named deputy); a run blocked on an expired ask
  never hangs indefinitely. **Escalation chains**: every ask to a human carries member → deputy →
  domain owner → admin, walked on SLA breach; `deadline` derives from the tier unless set
  explicitly. **Batching**: the digest composer groups by workspace and pre-fills recommended
  answers; approvals render as one-line accept/deny with diff links — reviewers see raw diffs,
  never agent-authored summaries alone. **Agent targets**: an ask routed to a Coworker queues into
  its next run (or wakes a session worker); if the target is ephemeral, archived, or busy past
  SLA, the ask reassigns up the chain.

---

## 9. API surface (delta)

```
POST /auth/login (human sessions; PATs for agents/services)
CRUD /org/humans · /org/members · GET /org/lineage
POST /nodes/enroll (one-time token exchange) · GET /nodes · POST /nodes/:id/revoke
CRUD /dna/domains · /dna/cards|rules|decisions|glossary
POST /dna/proposals  POST /dna/proposals/:id/review (publish|reject)  GET /dna/review-queue
POST /spawn          GET /spawn/:id  POST /spawn/:id/retire
CRUD /asks  ·  POST /asks/:id/respond  ·  WS: ask.requested, ask.answered
CRUD /board-tasks (assign to any member)
(v1 endpoints for coworkers, sessions, messages, automated-tasks, triggers, playbooks, runs carry over)
```

---

## 10. Security & governance checklist

- Human authn (local accounts → SSO later) + RBAC; PATs hashed, shown once, scoped.
- Agent scopes enforced in code (file scope realpath checks, tool allowlists, egress CIDR guard);
  every call audited; append-only audit log.
- **Scope delegation invariant** at spawn: child ⊆ parent, enforced by the policy engine.
- **DNA write policy**: agents propose, owners publish; compartment access enforced on retrieval;
  secrets scanner over all proposals and memory.
- **Spawn safety**: quotas, depth ≤ 2, TTL reaper, spend circuit-breaker, approval gates on
  persistent hires; ephemeral workers get connector-sandboxed, task-scoped workspaces only.
- **Node trust**: enrollment via one-time tokens + keypair identity, revocation from the console,
  node id stamped on every audit event; for remote nodes, egress allowlisting and secret handling
  route through the control plane / gateway rather than node-local code. The console surfaces each
  node's trust level explicitly — nodes are trusted compute, and admins should see that stated.
- Secrets in OS-encrypted storage (OS keyring / Tauri stronghold — note `safeStorage` is
  Electron's API; the exact mechanism is a Phase-0 spike); redaction before any egress to
  providers covers secrets *and* PII.
- Webhooks signature-verified; console served over localhost or TLS behind the company's reverse
  proxy in server mode.

---

## 11. Delivery plan (rephased)

| Phase | Deliverable | Key work | Est. (1 dev) |
|---|---|---|---|
| **0. Foundations** | Repo, CI, single-process skeleton | Monorepo, TS strict, Drizzle+SQLite (WAL), REST+WS, console shell, 3-OS CI matrix | 1 wk |
| **1. MVP agent** | Chat with a Coworker doing real local work | Model gateway, agent loop, guarded fs/shell/web tools, approval cards, audit, streaming chat UI | 4–5 wks |
| **2. Identity, memory, skills, connectors** | Coworkers feel like employees | Role catalog across departments, IDENTITY/STYLE/HANDBOOK, memory tiers 1–2 (personal/project), skills + market, MCP client + tier-1 connectors, workspace kinds | 3–4 wks |
| **3. Company DNA v1** | The coherence core | DNA store (git-backed markdown) + domains/index, cards compilation from sources, glossary + applicable-rules injection into every prompt, proposals + owner review queue, citations in answers | 3–4 wks |
| **4. Automation** | 24/7 operation | Schedule/API/event triggers, PATs, `{{field}}` templating, headless Ask policy, shared task board | 2–3 wks |
| — | **v1 cut line** | Phases 0–4 + 8a are shippable v1: a DNA-coherent, automated company run by one admin + Coworkers | — |
| **5. Playbooks** *(v2 track)* | Multi-stage orchestration | DSL + sandbox, statuses, askUser → Asks, read-only canvas, versions, playbook triggers | 3–4 wks |
| **6. Multi-human org** *(v2 track)* | A company, not a person | Server deployment, human accounts + RBAC, asks routing + digests, shared board, node registration & workspace-affinity scheduling | 3–4 wks |
| **7. Spawning** *(v2 track)* | The org flexes | Ephemeral workers (quota, TTL, fold-back memory), then persistent hires (owner approval), policy engine, lineage graph, spend ledger + circuit-breaker | 2–3 wks |
| **8a. v1 hardening** | v1 production-ready | Backup/restore, encrypted secrets, docs, security review | 1 wk |
| **8b. v1.1 polish** | Distribution | Tauri shell, installers, telemetry (opt-in) | 1–2 wks |

**v1 (Phases 0–4 + 8a): ~14–18 weeks solo.** Full company OS (v2 track + 8b): ~23–31 weeks total.
(AI assistance reliably compresses boilerplate, not security-critical integration work — plan on
15–20%, not 40%.) Milestones: working agent week ~6 · DNA-coherent company ~week 13 · **v1 ships
~week 14–18** · multi-human org ~week 20–26 · full spawning ~week 22–29.

Sequencing rationale: DNA lands early (Phase 3) because everything after it (multi-human, spawning)
depends on shared context; spawning lands last because it needs governance + budgets + the org model
to be safe. The v1 cut line stays at Phase 4 because playbooks, multi-human, and spawning add
*multiplicative* complexity (governance surface × trust surface), not additive features — v1 first
proves the core loop (work → learning → DNA → better work) end to end.

**Phase-0 spikes** (timeboxed; the ladder isn't committed until they pass):
- `isolated-vm` on Node 22: maintenance status, compatibility, child-process fallback prototype.
- sqlite-vec + FTS5 hybrid-ranking determinism: same query → same blend, across index rebuilds.
- Git-backed DNA store concurrency: concurrent proposal publishes, direct edits vs. publish,
  index staleness, and which component holds the write lock in multi-node mode.
- Secrets API for the Tauri shell (stronghold / OS keyring — `safeStorage` is Electron's).

**Acceptance criteria** (a phase isn't done until its demo passes):
- **P1**: agent edits a repo file through a gated approval; audit trail complete end to end.
- **P3**: fixed task battery run with/without DNA injection shows measurable improvement
  (citation accuracy, rule compliance) — the product's core hypothesis, tested here, not assumed.
- **P4**: headless trigger fires overnight; a blocked approval auto-denies at expiry; the morning
  digest renders correctly.
- **P6**: two humans + two nodes; heartbeat loss mid-run recovers with no orphaned work.
- **P7**: spawn storm trips the circuit-breaker; a depth-3 spawn is refused by policy, not prompt.

---

## 12. Testing & quality

- **Unit**: scope delegation algebra (child ⊆ parent), spawn policy engine (quotas/depth/TTL),
  DNA proposal workflow states, egress/path guards, scheduler math, memory 3-tier classifier.
- **Integration**: agent loop against scripted mock models; DNA injection determinism (same domain →
  same rules in prompt); multi-node run scheduling and heartbeat loss; spawn storm → circuit-breaker.
- **E2E**: hire → chat → gated write approval → DNA proposal → review → next run uses the new rule.
- **Injection suite**: a tainted external document yields a DNA proposal that carries its taint
  flag; the reviewer sees the raw diff (never an agent summary alone); a tainted run cannot spawn
  ungated.
- **Golden runs**: "morning brief", "issue triage → fix → ask", and a "spawn ephemeral researcher →
  fold back report" flow replayed in CI with fake models.
- **Chaos-lite**: kill node mid-run; kill control plane with live nodes; restart → resumes cleanly,
  audit intact, no orphan spawns (reaper).

## 13. Risks & mitigations

| Risk | Mitigation |
|---|---|
| DNA quality drift / gaming (agents proposing self-serving rules) | Human-owned review, provenance on every item, reviewer-agent contradiction reports, compartment isolation |
| Prompt injection via external content (email, web, ingested docs steering proposals, spawns, writes) | Taint-tracking for off-platform content; provenance + raw diffs in the review UI; spawns from tainted runs auto-gated; tainted context barred from external writes |
| Spawn runaway / cost explosion | Depth cap, quotas, TTL reaper, spend circuit-breaker, approval gates on persistent hires |
| Governance overhead kills small-team speed | Proportional governance: single-admin mode auto-approves own proposals; compartments optional at start |
| Privacy leakage across departments | DNA compartments enforced at retrieval; access scopes on domains; audit on every read of restricted domains |
| Multi-human/multi-node complexity landing too early | Single-process mode is the default until Phase 6; the split is a deployment change, not a rewrite |
| Agent reliability unattended | Conservative scopes, Ask gates before external writes, run-now dry tests, explicit success criteria |
| Native-module fragility across OSes | 3-OS CI from Phase 0; prebuilt binaries; child-process fallback for the playbook sandbox |
| Scope creep | Phase ladder above; DNA and spawning are the only new pillars — resist others until v1 ships |

## 14. Key open decisions

1. **DNA canonical store**: plain git repo vs. DB-with-export (default: git-backed markdown).
2. **Human auth v1**: local accounts (default) vs. OIDC-only for companies with SSO.
3. **First deployment shape**: single-process on an office machine (default) vs. containerized server from day one.
4. **Ephemeral worker default TTL & quota**: 24h / 3 concurrent (default) — tune with use.
5. **Tier-1 business suite**: Microsoft 365/Graph (default) vs Google Workspace.
6. **First IM channel**: Slack (default) vs Discord vs Telegram.
7. **Embeddings**: API (default) with local fallback.
8. **Name/branding**: working title pending trademark + domain search.
