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
>
> **Amendments (v2.3 — enterprise deployment shape)**: knowledge vs. operational data separation —
> systems of record stay live, never synced into the DNA (§4.6) · personal-assistant deployment
> shape with mirrored scopes (§6.4) · enterprise connector tier (§8.2) · inter-agent communication
> policy — state, not chatter (§8.11).
>
> **Amendments (v2.4 — directive-to-execution)**: goal slice promoted into the read path (§4.2) ·
> initiatives — the directive→work spine with sponsor, lead, deadline, dependency-checked close
> (§5.1, §7, §8.9) · cross-domain coordination via initiative playbooks (§8.6) · delegated approval
> authority as scoped, expiring DNA rules the ask router evaluates (§8.10) · business budgets
> display-only until post-v1 (§14.11).
>
> **Review pass (v2.5)**: goal slice wired through the runtime + DNA-engine read path (§8.1, §8.7) · proposal kinds cover goals; SOPs pinned as playbooks + pointer cards (§4.1, §7) · ask-digest grouping unified (§8.10, §11 P4) · schema gaps closed: ask→initiative link, domain `store` flag, human deputy, ephemeral status mapping, proposal withdraw (§7, §9) · new API: `/dna/goals`, `/initiatives`, governance reads (§9) · tier-2 connectors explicitly post-v1 (§11).
>
> **Org-change pass (v2.6)**: first-run bootstrap (§9, §11 P1) · domain split/merge/rename as governed topology ops (§4.4) · offboarding closed out — initiatives, board tasks, deputy refs, admin-custody fallback, last-admin guard (§5) · Coworker suspend + re-role = retire-and-respawn (§6.3) · role-template versioning with owner-approved upgrades (§6.5) · affinity-node loss → queue-or-rebind (§3) · initiative close lapses delegated rules (§8.10) · status enums pinned; viewers never ask targets (§5, §7) · deployment perimeter + proposal strictness parked as decisions (§14.12–13).

> **Consistency audit (v2.7)**: delegated rules get `effective_to` so "end by window" (§8.10) is representable (§7) · goal-slice "deadline" pinned to `dna_goals.effective_to` (§4.2, §7) · per-kind expiry defaults, the domain-owner escalation hop, and suspended ask targets specified (§8.10) · workspace rebind endpoint added (§9, §3) · digest ownership P4 (single-admin) vs P6 (per-human) disambiguated (§11) · single-admin "auto-approve" softened to one-click review, deferring to §14.13 (§13) · personal assistants retire — never re-own — on offboarding (§5) · viewer never-an-ask-target guard pinned in the schema (§7) · SLA-tier breach defaults parked as decision 14 (§14).
>
> **Consistency review (v2.8)**: workspace↔initiative binding added so the goal slice has a defined source (§4.2, §7) · asks carry `workspace_id`, keying the domain-owner escalation hop and digest grouping (§7, §8.10) · ephemeral→Coworker status mapping made 1:1 — `done` maps to `retiring` (§7) · domain `rename` joins split/merge as a governed endpoint (§9, §4.4) · retire/suspend/resume moved off `/spawn` onto the coworker they act on (§9) · stalled-initiative escalation specified — the §13 directive-decay row now has a mechanism (§5.1) · P3 goal slice scoped to org-wide goals until initiatives land in P4 (§11).
>
> **Edge-case pass (v2.9)**: escalation-chain exhaustion pinned — expire-per-behavior plus a critical org-stall broadcast (§8.10) · first-response-wins and expired-response = audit-only close the late/racing-answer seam (§8.10) · conflicting delegations resolve most-restrictive with a contradiction report (§8.10) · scope revocations re-checked before external writes (§8.1) · template retirement refuses live pins (§6.5) · spawner death retargets ephemeral fold-back to project memory (§6.3) · goal-window expiry under a live initiative raises a sponsor ask (§5.1) · goal-vs-goal contradictions join proposal-time checks (§4.4) · residual unhandled edge cases documented as §13.1; provider degradation and partitioned-node authority parked as decisions 15–16 (§14).
>
> **Edge-case audit (v2.10)**: second sweep — §13.1 re-ranked into severity tiers and extended · quorum approvals inexpressible (§4.1 vs §8.10) · external-write atomicity + trigger idempotency · erasure vs. append-only ledgers + data residency · db-only reconstructibility (§4.4 vs §4.5) · check-then-spend races (§6.2) · workspace-rebind fencing (§9) · restore reconciliation (8a) · mid-run rule staleness (§8.1) · ask storms (§8.10) · self-approval (§4.3) · clock/timezone semantics · proposal amendment (§7) · runtime precedence (§4.2) · taint decay (§8.3) · playbook recursion (§8.6) · git integrity (§4.5) · PAT lifecycle (§9, §10) · offboarding vs. authored proposals (§5) · embedding re-index (§14.7) · glossary alias collisions (§4.2, §7).
>
> **Edge-case pass (v2.11)**: third sweep — its findings closed inline: tainted-origin asks lose digest pre-fills; taint survives publication as provenance residue (§8.10, §4.3, §13) · deputies must be human; escalation walks carry a visited-set; multi-domain ask hops pinned to the primary domain (§8.10, §7) · DNA review queues get an SLA with admin escalation (§4.3, §7) · retire + offboarding walks extended to board tasks, owned goals, initiative posts, named-access lists (§5, §6.3) · trigger catch-up coalescing (§8.5) · paused-initiative semantics pinned (§5.1) · topology ops serialized behind a domain write lock (§4.4) · ask responses re-validate payload assumptions (§8.10) · count caps and bootstrap claimed atomically (§6.2, §9) · human DNA edits validate-or-quarantine (§4.5) · affinity starvation raises an ask; rebind is capability-checked (§3, §9) · rehire = new member, never resurrection (§5) · decision-vs-rule contradictions + provenance link re-validation (§4.4) · secrets-scanner override is an audited ask, not a silent wedge (§10).

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
  and to-dos on a shared Task Board any member can hold or assign. A directive becomes an
  **initiative** — a goal, a lead, and the tasks/playbooks/spawns that carry it to done (§5.1).
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
6. **Files for humans, database for machines**: DNA, identity, and memory are git-friendly markdown humans can read, edit, and review (sensitive domains excepted — §4.5 carve-out); runs, audit, and indexes live in SQLite.
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
   │ session workers      │   │ secretary + HR       │   │ automations, DNA jobs│
   │ near repos/tools     │   │ near email/calendar  │   │                      │
   └──────────────────────┘   └──────────────────────┘   └──────────────────────┘
```

- **Single-process mode**: control plane + one node in one process, console at `localhost` — this is
  the MVP path; nothing is lost when scaling out later.
- **Workspace affinity**: runs are scheduled to the node where the workspace's files/connectors
  live (an engineer Coworker runs on the machine with the repo; the secretary's mailbox connector
  lives wherever it was authorized). Affinity is a scheduling preference, not a marriage: when the
  affinity node is offline or revoked, new runs queue until its heartbeat returns or an admin
  rebinds the workspace to another node — a rebind that first validates the target node actually
  advertises the workspace's required capabilities (files present, connectors authorized — §7
  `nodes.capabilities`), and a queue starved past a configurable window (default 24h) raises an
  admin ask: starvation is surfaced, never silently endured.
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
| **SOPs** | Process definitions — versioned playbooks plus a pointer card (note below the table) | "Onboarding checklist → onboarding playbook" |
| **Glossary** | Canonical terminology, mapped to aliases | "ARR = annual recurring revenue (not 'annual revenue')" |
| **Org facts** | Who exists (humans + Coworkers), teams, domain ownership | generated from the org registry, read-only |
| **Goals & metrics** | Company/quarterly objectives and KPIs; work links to them through initiatives (§5.1) | "Q3: cut support first-response to < 2h" |

**SOP representation** (pinned — §4.5 and §7 deliberately carry no `dna_sops`): the executable
process is a versioned playbook (§8.6); the DNA holds a card carrying the narrative and a reference
to the playbook id — never a second executable copy. Proposing an SOP means proposing the pointer
card; playbook changes ride the playbook engine's own versioning.

### 4.2 Read path — how coherence actually happens

Every run's system prompt is assembled with:
- **Always injected**: the org snapshot (who's who), the glossary slice relevant to the task's
  domain, all *applicable rules* for the workspace's domains, and the **goal slice**: active goals
  linked to the workspace through its initiatives, plus goals flagged org-wide (inject 'always'; statement, owner,
  deadline, status). "Applicable" has defined
  semantics: a rule applies when its domain intersects the workspace's domains and its effective
  window (`effective_from`…`effective_to`) covers the run (superseded rules drop out of injection automatically); `machine_hint`
  narrows matching where present. Each layer carries a token budget (org snapshot ~1k, glossary
  slice ~2k, rules ~4k, goal slice ~1k — soft limits, configurable); overflow demotes items to
  retrieval (rules overflow into the searchable DNA index) rather than truncating silently.
  Injection stays deterministic per (domain set, linked-goal set, DNA version) so it is testable
  (§12).
- **Retrieved on demand**: cards, decisions, and goals via hybrid search (BM25 + vector over the
  card index) — same retrieval machinery as v1's KB, now pointed at DNA.
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
reject leaves an audit trail. The queue has a cadence of its own: proposals carry a review SLA
(`review_by`, default 7 days, per-domain configurable); a breach escalates to the admin and a
stale queue surfaces in the owner's digest — the §1 learning loop must not starve on an ignored
inbox. Taint survives publication as provenance residue: an item accepted from a tainted run
keeps its flag, renders with an indicator wherever cited, and heads the §4.4 scheduled quality
reviews — the owner's accept is informed consent, not a laundering step. Humans can also propose directly, and can edit in their own tools —
the store is git-backed markdown, so a PR workflow is possible for teams that want it.

### 4.4 Governance

- **Domains & compartments**: DNA is partitioned into domains (Engineering, Finance, HR…) each with
  a human owner and an access policy (`public` | `members of domain` | `named members`). Retrieval
  respects the reader's access — the HR intern's Coworker never sees salary cards.
- **Provenance**: every card/rule/decision records where it came from; uncited claims are flagged
  during review.
- **Freshness**: review cadence and stale flags per item; scheduled DNA quality checks (a reviewer
  agent drafts a report; humans decide) re-validate provenance refs too — moved documents and
  rotated systems flag the card stale instead of letting citations rot silently.
- **Conflicts**: new rules supersede old ones explicitly (chains retained); the review UI shows
  contradictions — rule-vs-rule, goal-vs-goal, and decision-vs-rule — detected at proposal time.
- **Topology changes**: reorgs split, merge, and rename domains — a governed operation, not a
  hand-run migration: items move with ids stable (citations and supersession chains survive),
  access policies re-evaluate against the new topology, workspace domain tags remap, and the move
  is a single auditable event. Topology ops serialize behind a domain-level write lock (§4.5):
  split/merge/rename queue behind in-flight proposals and each other — the stable-id guarantees
  assume no concurrent topology mutation, so the system enforces the assumption rather than
  hoping. Prior states stay reconstructible from git history and audit.

### 4.5 Storage

```
~/.coworker/dna/            (or a company git repo — the canonical store)
  domains/<domain>/cards/*.md, rules/*.md, decisions/*.md, glossary.md
  goals/<quarter>.md
```
Markdown + frontmatter (id, version, effective dates, provenance, access); the control plane
maintains the SQLite/FTS/vector index over it. Humans can read and edit their company's brain with
any editor; git history *is* the DNA timeline. Frontmatter carries a `schema_version`: product
upgrades run in-place content migrations (post-backup) — an old store is never stranded. Direct
human edits are welcomed, not trusted: the control plane validates every ingested change
(frontmatter schema, unique ids, effective-window sanity) and quarantines invalid files to a
review queue with the parse error attached — a bad hand-merge degrades to an ask, never to a
silently corrupted index.

**Privacy carve-out**: git history is effectively immutable, which collides with deletion
obligations (GDPR-style erasure, offboarded-employee data, HR/Finance records). Domains may
declare `store: 'db-only'` — HR and Finance default to it: content lives in SQLite with
export-on-demand and never enters the git store; the git timeline is reserved for non-sensitive
domains. If sensitive material lands in git by mistake, remediation is a documented history
rewrite (rotate the repo, notify domain owners) — decided here, not improvised under a deadline.

### 4.6 Knowledge vs. operational data (systems of record)

The DNA holds *knowledge about* the company's systems — rules, definitions, decisions, how-tos —
never a *copy of their data*. ERP, WMS, HRIS, CRM remain live systems of record:

- **No bulk sync of operational data into the DNA.** A synced copy goes stale the moment the system
  of record changes; two copies of a fact are two ways to be inconsistent. Operational facts
  (order status, stock levels, an employee's record) are read live through scoped connectors at
  task time.
- **DNA cards carry the interpretive layer**: "refund window is 30 days (ref: policy §4)", "WMS
  location codes are zone-prefixed", "PO approval matrix by amount" — the context that makes raw
  rows meaningful, with provenance pointing at the source system or document.
- **Inconsistencies are signal, not noise**: when a run observes conflicting facts across systems
  (ERP says X, a DNA card says Y), it files a DNA proposal or contradiction report for the domain
  owner — a detection loop, never silent reconciliation.
- **Live lookups are cited like cards**: answers depending on connector reads reference system,
  record, and timestamp, so freshness is visible in the answer.

---

## 5. The org model: humans + Coworkers as members

- **Members**: `humans` (identity, RBAC role) and `coworkers` (identity files, scopes) share one
  member namespace — the task board, asks, groups, and lineage all reference members.
- **Human RBAC**: `admin` (everything), `owner` (one or more DNA domains + their Coworkers), `member` (work,
  propose DNA, spawn within policy), `viewer` (read-only — never an ask target). Auth starts as local accounts; SSO/OIDC
  later.
- **Asks — the universal interrupt**: approvals, questions, assignments, and spawn requests are all
  *Asks*: routed to a member (human or agent) with payload, deadline, and escalation policy —
  SLA tiers, expiry semantics, and escalation chains are a designed subsystem (§8.10), not just a
  routing table.
  Humans answer in the console (later IM/email digests); agents answer via their session worker.
  Approvals from v1 become Asks of kind `approval`.
- **Shared Task Board**: to-dos come from run results, playbook nodes, or any member; assignable to
  humans or Coworkers, groupable under initiatives (§5.1); visible org-wide within access scopes.
- **Groups/teams** mix humans and Coworkers (v1 kept agent-only groups; v2 unifies — a local
  Coworker still acts as Leader for execution routing).
- **Accountability invariant**: every Coworker row carries `owner_human_id`; spawned workers carry
  `spawned_by`; the chain must terminate at a human. Enforced at spawn time.
- **Offboarding**: deactivating a human runs the §6.3 dependency check across everything they
  touch: owned DNA domains (to a named successor, else **admin custody** — never orphaned), open
  asks and board-task assignments (reassigned or returned to the pool), dependent Coworkers
  (re-owned or retired — personal assistants are always retired: mirrored scopes die with the
  member, §6.4), sponsored/led initiatives (reassigned or closed), owned goals
  (`dna_goals.owner` — re-owned via the successor or admin custody, else retired), membership in
  `named` domain access lists (removed; policies re-evaluated), and deputy references (cleared in
  both directions — anyone deputizing the departing member re-points or clears). Inactive members
  are skipped when walking ask chains. Guard: the last active admin
  cannot be deactivated — the org never goes headless by accident. Audit history is retained;
  personal data falls under the §4.5 deletion carve-out. Rehire is a new member, never a
  resurrection: deactivation is terminal for identity, so a returning employee gets a fresh row —
  `decided_by` references, audit history, and spend attribution stay pinned to the departed
  identity, and email addresses are not reused.

### 5.1 Initiatives — from directive to coordinated execution

A CEO-level directive ("let's open the Austin store") must not die in a chat scroll. Its path:

1. **DNA first**: the directive lands as a decision record and (usually) a goal through the normal
   write path (§4.3) — the *what* and *why* stay governed.
2. **An initiative opens**: the execution spine linking goal → work. It carries a **sponsor** (the
   authority behind the directive), a **lead** (accountable member, human by default), a deadline,
   status (`proposed` → `active` → `paused`/`closed`), and an optional business budget (§14.11).
3. **Decomposition is work, not talk**: the lead creates board tasks (human or Coworker assignees),
   instantiates the relevant SOP as a playbook, requests spawns — all tagged with the initiative
   and visible on its slice of the shared board.
4. **Cross-domain coordination runs through the playbook spine**: an initiative playbook's nodes
   route asks into each domain's own chain (§8.6, §8.10) — Finance asks to Finance, Legal to Legal
   — so coordination is auditable state, not another chat channel (§8.11).
5. **Progress is state, not narration**: the initiative view is goal + ask burndown + task/playbook
   status + spend. A stalled initiative — deadline passed with open work — raises an ask to its
   sponsor (then admin), reusing the §8.10 escalation machinery. The same ask fires when the
   linked goal's window (`effective_to`, §4.2) ends while the initiative is still active —
   extend, re-target, or close is a human call, not a silent drop from the slice. **Pause is explicit
  and total**: a paused initiative suspends its stalled-work escalation and freezes its board
  slice (no new runs launch under it), but — unlike close — does *not* lapse its delegated rules
  (§8.10): pause freezes execution, not authority; the delegation's own window (§4.2
  `effective_to`) stays the bound, and pausing past the deadline still raises the sponsor ask —
  pause is a state, not a way to outlive a deadline silently. Closing runs the same dependency
   check as retiring a Coworker (§6.3): open asks
   and tasks resolved or reassigned; the retrospective files DNA proposals — the §1 loop closes.

An initiative is an org entity (visible, accountable); **project memory** (§8.3) stays the
automatic per-workspace memory tier — one is governance, the other learning. **v0 shape**
(Phase 4): goal + lead + deadline + task grouping. Business budgets and delegated authority
(§8.10) land with the multi-human org (Phase 6) — delegating authority presumes more than one
human to delegate to.

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
ephemeral roles. Ephemeral spawning is an agent/playbook capability only; a human wanting bounded
delegation assigns a board task or instantiates a playbook.

### 6.2 Policy engine (hard-coded, not prompt-enforced)

- **Scope delegation**: child's file/tool/connector scopes ⊆ parent's. A secretary cannot spawn
  anything with repo write access she doesn't have.
- **Allowlists**: which templates each member class may spawn; ephemeral workers restricted to
  whitelisted "subagent" templates.
- **Quotas & caps**: max concurrent ephemeral workers per spawner, global spawn depth (default 2),
  org-wide concurrent Coworkers, per-spawn and org-wide spend caps metered by the spend ledger.
  Count caps are *claimed*, not checked: the policy engine increments atomically inside the spawn
  transaction, so two spawners racing the last concurrent slot see one success and one refusal —
  no check-then-act window (the money-side reservation design stays open, §13.1).
- **Approval gates**: persistent hires → Ask to the owner of the domain the hire's primary
  workspace is bound to (or an admin); agent-spawned
  ephemeral workers exceeding quota → Ask to the spawner's owner human.
- **Runaway protection**: depth cap, rate limits, TTL reaper, budget circuit-breaker (org spend
  ceiling halts all spawns and automations with a loud Ask to admins).

### 6.3 Lineage

`spawned_by` chains render as an org graph in the console: who created whom, why (purpose), spend,
and current status. Retiring a persistent Coworker requires resolving its dependents (automations,
playbooks, paired IM sessions, live spawned workers — a dying spawner's ephemeral children fold
back into the workspace's project memory, not the departed personal one — plus board-task
assignments returned to the pool or reassigned, owned goals re-owned or retired, and initiative
lead/sponsor posts reassigned or closed via §5.1) — the same dependency check as deleting a
skill, applied to staff; the §5 offboarding walk is its superset for humans.

Two state changes short of retirement: **suspend** — an admin's emergency stop that halts triggers
and runs without resolving dependents (in-flight asks re-route up the chain) — and **re-role** —
re-tasking a Coworker to a different role is retire-and-respawn (identities are role-shaped;
project memory stays with the workspace, lessons go to DNA), never an in-place IDENTITY rewrite.
In-place evolution of the *same* role is the template upgrade path (§6.5).

### 6.4 Personal assistants (deployment shape)

One persistent assistant per human employee is a *deployment* of the existing model, not a new
architecture:

- **Template**: a persistent-hire template (`personal-assistant`) bound 1:1 to a human —
  `owner_human_id` = the assisted employee. The assistant serves the employee but is accountable
  to the company: DNA proposals route to domain owners; compartment access is never widened to
  please the human.
- **Scope mirroring**: the assistant's scopes (DNA compartments, connector scopes, tool access)
  are derived from the human's RBAC role at spawn, **refreshed on role change, revoked on
  offboarding** (§5). The scope-delegation invariant is reused with the employee's role as the
  ceiling: assistant ⊆ employee, everywhere.
- **Mirrored access ≠ mirrored behavior**: a human rarely opens 10,000 HR records; an assistant
  might bulk-read them. Restricted-domain reads carry **rate/volume limits** in addition to
  permission checks, and every read of a restricted domain is audited (§13).
- **Identity separation**: the assistant acts under its own member identity (own PAT, own audit
  trail, own spend-ledger line), never the employee's credentials — actions stay attributable.

### 6.5 Role & template evolution

Roles change as the company does; running staff must track the change without a respawn stampede.
Templates are versioned (§7 `role_templates`); every persistent Coworker pins the version it was
spawned from. An **upgrade** is proposal-shaped: the diff — IDENTITY/HANDBOOK changes, scope
deltas — goes to the Coworker's owner as an Ask; on accept, files rebase and scopes re-derive as
new-template ∩ owner's-current-scopes, never widening. Ephemeral subagent templates upgrade in
place — workers are short-lived, so new spawns simply get the new version. Retiring a template
with live pins is refused — upgrade or retire-and-respawn the pinned Coworkers first (the §8.4
skill-uninstall dependency check, applied to templates). A company-wide role
overhaul is one template bump plus a queue of owner asks, not a rehire.

---

## 7. Data model (v2 delta)

New/changed tables (v1 session/run/message/skill/connector tables carry over):

> **Self-containedness**: this section, §8, and §9 are deltas against a v1 design doc that is not
> in this repo. Before Phase 0 starts, inline or link the carried-over v1 specs here. If any v1
> deployment exists, add a migration section: v1 `approvals` rows → `asks` of kind `approval`;
> per-Coworker KBs → DNA domain imports.

```
humans         (id, name, email, rbac 'admin'|'owner'|'member'|'viewer', auth json,
                 deputy_member_id?, created_at)  -- deputy: first hop of the §8.10 chain;
                 -- must reference a humans row — agent and self deputies refused at write (§8.10)
                 -- viewers are read-only and never valid ask targets (§5)
coworkers      + owner_human_id, class 'persistent'|'ephemeral', spawned_by member?, ttl_at,
                 budget_cap, lineage_depth, template_id?, template_version?,
                 status 'requested'|'active'|'suspended'|'retiring'|'archived'
                 -- ephemeral lifecycle maps 1:1: spawned→requested, running→active, done→retiring,
                 -- reaped→archived (done = fold-back pending, the ephemeral analogue of retiring)
                 -- suspended = emergency stop, halts triggers/runs without resolving dependents (§6.3)
role_templates (id, name, version, class 'persistent'|'ephemeral-subagent', body json
                 (identity/style/handbook), default_scopes json, status 'draft'|'active'|'retired')
                 -- versioned catalog; persistent Coworkers pin (template_id, template_version) (§6.5)
nodes          (id, name, kind 'local'|'remote', capabilities json, last_heartbeat,
                 pubkey, enrolled_at, revoked_at?, status 'trusted'|'revoked')
dna_domains    (id, name, owner_human_id, access 'public'|'domain'|'named',
                 store 'git'|'db-only')  -- db-only: the §4.5 privacy carve-out
dna_cards      (id, domain_id, title, definition_md, refs json, provenance json, version,
                 status 'draft'|'active'|'retired')
dna_rules      (id, domain_id, statement_md, machine_hint json?, effective_from, effective_to?,
                 supersedes_id, status 'active'|'superseded'|'lapsed')
                 -- effective_to bounds delegation windows (§8.10); lapsed: delegation ended (§8.10)
dna_decisions  (id, domain_id, context_md, outcome_md, decided_by member, decided_at)
dna_glossary   (id, domain_id?, term, definition, aliases json)
dna_goals      (id, quarter?, statement_md, owner member, status 'active'|'met'|'missed'|'retired',
                inject 'always'|'linked', effective_from, effective_to?)  -- goal-slice source (§4.2)
                -- the slice's 'deadline' (§4.2) is effective_to
dna_proposals  (id, kind 'card'|'rule'|'decision'|'goal'|'edit', payload json, proposed_by member,
                 provenance json, status 'open'|'published'|'rejected'|'withdrawn', reviewed_by?, at,
                 review_by?)  -- review_by: queue SLA deadline; breach escalates to admin (§4.3)
asks           (id, kind 'approval'|'question'|'assignment'|'spawn_request', from member, to member,
                 payload json, initiative_id?, workspace_id?, status 'pending'|'answered'|'expired', deadline, created_at,
                 sla_tier 'critical'|'standard'|'bulk', escalation json,
                 expiry_behavior 'deny'|'escalate'|'reassign', responded_at?)  -- supersedes approvals;
                 escalate/reassign close the expired ask and open a linked successor ask (§8.10);
                 workspace_id keys the domain-owner escalation hop and digest grouping (§8.10);
                 -- respond re-validates payload assumptions — answers against a superseded
                 -- world are audit-only, a successor ask carries the decision (§8.10)
initiatives    (id, title, goal_ref?, decision_ref?, sponsor member, lead member,
                 status 'proposed'|'active'|'paused'|'closed', business_budget json?, deadline?,
                 closed_at?)
board_tasks    + assignee_member_id?, initiative_id?  (runs carry initiative_id? the
                 same way — burndown, per-initiative digests)
workspaces     + initiative_ids json?  -- active initiatives bound here (bound at spawn under an
                 initiative, admin-editable); the source of the §4.2 goal slice
spend_ledger   (id, member_id, run_id?, spawn_id?, tokens_in/out, cost, pricing_version, at)
```

v1's per-Coworker knowledge bases are subsumed: a "KB" is now a DNA domain import (sources are
ingested and compiled into cards inside a domain), plus retained per-project reference folders.

---

## 8. Subsystem designs (carried from v1, updated)

- **8.1 Agent runtime** — unchanged core (prompt assembly → guarded loop → structured result), with
  two v2 changes: (a) the always-injected DNA layer (org snapshot, glossary slice, applicable
  rules, goal slice) precedes per-Coworker context; (b) headless approval policy now routes into **Asks** —
  `auto_deny` (default) | `queue_until_morning` (Task Board digest) | `escalate_im`. These are
  Ask tiers in disguise (§8.10): `escalate_im` → `critical`, `queue_until_morning` → `standard`
  (next digest), `auto_deny` → expiry behavior `deny`; the configuration surface is the ask
  policy, not a separate one. Scope
  enforcement, egress guard, write-lock, stop semantics, cost metering as in v1. (c) Scope
  changes — revocations, §6.4 role-change refreshes — take effect at the next run's prompt
  assembly; a long-running run re-checks its scopes before each external write, so a mid-run
  revocation gates the next side effect rather than lingering to the run's end.
- **8.2 Tools & MCP** — built-ins (`fs.*`, guarded `shell.exec`, `web.*`, `kb.search` → `dna.search`,
  `memory.write`) plus **`spawn`** as a guarded tool. Egress guard unchanged. Connector tiers:
  tier 1 = email/calendar/docs; **tier 2 = enterprise systems of record** (ERP/WMS/HRIS/CRM) —
  read-only first, writes gated behind `critical`-tier Asks (§8.10); per-connector scoped
  credentials via PATs, never shared service accounts; §4.6 governs what may enter the DNA.
- **8.3 Memory service** — now three-tier classifier (personal / project / DNA proposal) with the
  v1 machinery (dedupe, timeline, versions, secrets scanner) under it.
- **8.4 Skills** — unchanged; domain-organized packs; uninstall dependency checks.
- **8.5 Trigger engine** — schedule/API/event triggers unchanged; every firing is a run of the same
  session worker; API triggers gain PAT scopes for external callers. Missed schedules neither
  replay nor vanish: firings elapsing during a Coworker suspension or control-plane downtime
  coalesce into one catch-up run per trigger on resume, carrying a missed-schedule summary (count,
  window) — per-trigger policy `replay|coalesce|skip`, default coalesce, with §6.2 rate limits
  bounding a large backlog.
- **8.6 Playbook engine** — DSL and sandbox unchanged; `worker()` targets any member (human targets
  create an assignment Ask); spawn-class playbooks (fan-out workers) built on §6 ephemeral workers
  · **initiative playbooks** (§5.1): an SOP instantiated under an initiative becomes the
  cross-domain spine — nodes route asks into each domain's escalation chain (§8.10) and artifacts
  land on the initiative's board slice.
- **8.7 DNA engine** — inherits v1 KB machinery (ingest → chunk → embed → cards → hybrid retrieval →
  citations) extended with domains, proposals, review queue, and glossary/rule/goal-slice injection.
- **8.8 Groups & IM** — unified human+agent teams; IM pairing routes to a Coworker whose asks
  escalate to the channel.
- **8.9 Console screens** — v1 screens 1–9, plus five new: **10. Org & People** (members, RBAC, lineage graph,
  retirement flows) · **11. DNA console** (browse cards/rules/decisions per domain, review queue
  with diffs and provenance, proposal history, glossary editor) · **12. Governance** (policies,
  quotas, spend dashboard, spawn audit) · **13. Ask inbox** (SLA indicators, batched digests,
  one-line accept/deny with diff links) · **14. Initiatives** (goal-linked execution: status, ask
  burndown, task/playbook progress, spend vs. budget, delegated-authority grants).
- **8.10 Asks — the human-attention subsystem** — system throughput is bounded by ask-response
  latency, so asks are engineered, not merely routed. **SLA tiers**: `critical` (blocks a
  customer-facing or money-moving run — interrupt-grade push, console + IM), `standard` (blocks a
  run — next digest), `bulk` (non-blocking — daily digest, batched). **Expiry semantics** are
  explicit per ask: `deny` (default for approvals and spawn requests — an expired approval is a
  no), `escalate` (route up the chain — default for questions), `reassign` (fall back to a named
  deputy — default for assignments); a run blocked on an expired ask
  never hangs indefinitely. An ask closes on the first response received — later responses
  (member and deputy racing) are audit-only; a response to an expired ask is recorded but has no
  effect: the successor ask, if any, carries the decision. Responses re-validate before they bind:
  at respond time the ask's payload assumptions are recomputed — the diff still applies, the
  referenced DNA item is still live, the scope still holds — and a response against a superseded
  world is audit-only like a late response, with a successor ask opened against current state (the
  same machinery expiry uses). **Escalation chains**: every ask to a human carries member → deputy (set per member in the org
  registry; humans only — an agent deputy is refused at write, because standing approval authority
  for agents is exactly what the reviewed, windowed delegated rules below exist for; self-deputy
  and deputy cycles are refused the same way) → domain owner (of the domain the ask's workspace
  belongs to; asks with no domain skip the hop; multi-domain workspaces hop to the primary domain
  — first-bound, admin-editable: one deterministic hop, not a fan-out to every owner) → admin,
  walked on SLA breach (inactive members are skipped; the walk carries a visited-set, so a
  mis-configured cycle ends the hop, not the walk — the §5 last-admin guard and the exhaustion
  broadcast remain the backstops); `deadline` derives from the tier unless set
  explicitly. Chain exhaustion — the terminal admin is inactive or breaches — expires the ask
  per its expiry behavior (an unanswered approval is a no, never a hang) and broadcasts a
  critical-tier org-stall alert to every active human: the §5 last-admin guard keeps an admin
  from being *deactivated*, not from being *absent*; the broadcast is the backstop. **Batching**: the digest composer groups by initiative, then workspace, and pre-fills recommended
  answers — recommendations compute only from re-validated, untainted payloads: an ask originating
  in a tainted run (§13) renders without a pre-fill, so one-click accept is a convenience for
  trusted provenance, not an injection surface; approvals render as one-line accept/deny with diff links — reviewers see raw diffs,
  never agent-authored summaries alone. **Agent targets**: an ask routed to a Coworker queues into
  its next run (or wakes a session worker); if the target is ephemeral, suspended, archived, or
  busy past SLA, the ask reassigns up the chain (§6.3 suspend re-routing included). **Delegated authority** — a directive can push authority,
  not just work: the sponsor proposes a DNA rule scoped by `machine_hint` (initiative, ceiling,
  window) — "initiative X: store invoices ≤ $25k need one approval, by the lead, until
  2026-12-31" — reviewed like any rule. The ask router evaluates applicable rules, delegations
  included, when choosing approvers, so a static approval matrix doesn't route six months of store
  invoices through the same two people. When several delegated rules match one ask, the most
  restrictive ceiling wins and a contradiction report goes to the sponsoring owners. Delegations end by window, supersession, or initiative
  close — rule semantics, not bespoke state: closing an initiative lapses every rule whose
  `machine_hint` scopes it to that initiative (status → `lapsed`, dropped from injection and routing).
- **8.11 Inter-agent communication** — agents exchange **state, not chatter**. Agent→agent requests
  are Asks with an agent target (§8.10); shared context lives on the task board as tasks and
  artifacts, not repeated in-context explanation; deliberate multi-agent fan-out is a playbook
  with `worker()` targets (§8.6); disputes between agents escalate to humans as DNA decision
  proposals — never agent-vs-agent argument loops. No free-form agent-to-agent chat channels; every
  cross-agent interaction is an auditable ledger entry (ask, task, or run artifact). Rationale:
  unbounded agent conversation amplifies shared errors, burns tokens, and resists audit.

---

## 9. API surface (delta)

```
POST /auth/login (human sessions; PATs for agents/services)
POST /org/bootstrap (first-run: create company + first admin; refused once any human exists —
               a transactional singleton guard, not check-then-act)
CRUD /org/humans · /org/members · GET /org/lineage
POST /nodes/enroll (one-time token exchange) · GET /nodes · POST /nodes/:id/revoke
CRUD /dna/domains · /dna/cards|rules|decisions|glossary|goals
POST /dna/proposals  POST /dna/proposals/:id/review (publish|reject) · POST /dna/proposals/:id/withdraw  GET /dna/review-queue
POST /dna/domains/:id/split|merge|rename (governed topology ops, §4.4)
CRUD /role-templates (versioned catalog, §6.5)
POST /spawn          GET /spawn/:id   (spawn requests; approval + spawn-storm monitoring)
POST /coworkers/:id/retire · /suspend · /resume   (lifecycle acts on the coworker, §6.3 — not the spawn request)
CRUD /asks  ·  POST /asks/:id/respond  ·  WS: ask.requested, ask.answered
CRUD /initiatives · POST /initiatives/:id/close (runs the §6.3 dependency check)
CRUD /board-tasks (assign to any member)
POST /workspaces/:id/rebind (admin affinity failover; refuses a target node lacking the
               workspace's required capabilities, §3)
GET /governance/policies|quotas|spend  (console screens 12 & 14)
(v1 endpoints for coworkers, sessions, messages, workspaces, automated-tasks, triggers, playbooks, runs carry over)
```

---

## 10. Security & governance checklist

- Human authn (local accounts → SSO later) + RBAC; PATs hashed, shown once, scoped.
- Agent scopes enforced in code (file scope realpath checks, tool allowlists, egress CIDR guard);
  every call audited; append-only audit log.
- **Scope delegation invariant** at spawn: child ⊆ parent, enforced by the policy engine.
- **DNA write policy**: agents propose, owners publish; compartment access enforced on retrieval;
  secrets scanner over all proposals and memory — scanner hits quarantine to the owner with an
  audited admin override, so a false positive is a visible ask, never a silent wedge in the write
  path.
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
| **1. MVP agent** | Chat with a Coworker doing real local work | Model gateway, agent loop, guarded fs/shell/web tools, approval cards, audit, streaming chat UI, first-run bootstrap (company + seed admin) | 4–5 wks |
| **2. Identity, memory, skills, connectors** | Coworkers feel like employees | Role catalog across departments, IDENTITY/STYLE/HANDBOOK, memory tiers 1–2 (personal/project), skills + market, MCP client + tier-1 connectors, workspace kinds, versioned role-template catalog (§6.5) | 3–4 wks |
| **3. Company DNA v1** | The coherence core | DNA store (git-backed markdown) + domains/index, cards compilation from sources, glossary + applicable-rules + goal-slice injection into every prompt (org-wide goals first; linked goals wire up with initiatives in P4), proposals + owner review queue, citations in answers | 3–4 wks |
| **4. Automation** | 24/7 operation | Schedule/API/event triggers, PATs, `{{field}}` templating, headless Ask policy, shared task board, initiatives v0 (goal + lead + deadline + task grouping) | 2–3 wks |
| — | **v1 cut line** | Phases 0–4 + 8a are shippable v1: a DNA-coherent, automated company run by one admin + Coworkers | — |
| **5. Playbooks** *(v2 track)* | Multi-stage orchestration | DSL + sandbox, statuses, askUser → Asks, read-only canvas, versions, playbook triggers | 3–4 wks |
| **6. Multi-human org** *(v2 track)* | A company, not a person | Server deployment, human accounts + RBAC, asks routing + per-human digests (P4 shipped the single-admin digest), shared board, node registration & workspace-affinity scheduling, delegated authority + initiative budgets, offboarding flows + last-admin guard, Coworker suspend/resume, template upgrades, domain split/merge (§4.4) | 3–4 wks |
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
proves the core loop (work → learning → DNA → better work) end to end. Initiatives land in two
steps — v0 grouping in Phase 4 (a lone admin still needs directives turned into work); budgets and
delegated authority in Phase 6 (delegation presumes more than one human). Tier-2 enterprise
connectors (§8.2) are deliberately absent from the ladder: they start after v1 ships, once §14.9
names the first system — an integration project per connector, not a phase.

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
  digest renders correctly, grouped by initiative.
- **P6**: two humans + two nodes; heartbeat loss mid-run recovers with no orphaned work; a
  delegated-authority rule routes an approval to the initiative lead and expires cleanly;
  offboarding one human reassigns domains, asks, and initiatives, and the last-admin guard
  refuses the final admin.
- **P7**: spawn storm trips the circuit-breaker; a depth-3 spawn is refused by policy, not prompt.

---

## 12. Testing & quality

- **Unit**: scope delegation algebra (child ⊆ parent), spawn policy engine (quotas/depth/TTL),
  DNA proposal workflow states, goal-slice injection determinism, delegated-authority evaluation
  in ask routing, egress/path guards, scheduler math, memory 3-tier classifier, offboarding dependency walk (last-admin guard,
  initiative reassignment, deputy clearing), domain split/merge id-and-chain invariants,
  template-upgrade scope re-derivation, escalation-walk visited-set (deputy cycles), deputy
  humans-only guard, trigger catch-up coalescing, atomic quota claims under racing spawners, DNA
  store ingestion quarantine, topology-op write-lock serialization, ask respond-time
  re-validation.
- **Integration**: agent loop against scripted mock models; DNA injection determinism (same domain →
  same rules in prompt); multi-node run scheduling and heartbeat loss; spawn storm → circuit-breaker; affinity node
  offline → runs queue, starvation ask at window, capability-less rebind refused; review-queue
  SLA breach → admin escalation; tainted-origin ask renders in the digest without a pre-fill.
- **E2E**: hire → chat → gated write approval → DNA proposal → review → next run uses the new rule;
  and directive → decision + goal → initiative → playbook fan-out → dependency-checked close →
  retrospective proposal.
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
| Prompt injection via external content (email, web, ingested docs steering proposals, spawns, writes, ask answers) | Taint-tracking for off-platform content; provenance + raw diffs in the review UI; spawns from tainted runs auto-gated; tainted context barred from external writes; tainted-origin asks lose digest pre-fills, and taint survives publication as a provenance flag (§8.10, §4.3) |
| Spawn runaway / cost explosion | Depth cap, quotas, TTL reaper, spend circuit-breaker, approval gates on persistent hires |
| Governance overhead kills small-team speed | Proportional governance: single-admin mode collapses review of own proposals to one click; compartments optional at start; auto-publish itself stays behind the §14.13 decision |
| Privacy leakage across departments | DNA compartments enforced at retrieval; access scopes on domains; audit on every read of restricted domains |
| Multi-human/multi-node complexity landing too early | Single-process mode is the default until Phase 6; the split is a deployment change, not a rewrite |
| Agent reliability unattended | Conservative scopes, Ask gates before external writes, run-now dry tests, explicit success criteria |
| Native-module fragility across OSes | 3-OS CI from Phase 0; prebuilt binaries; child-process fallback for the playbook sandbox |
| Scope creep | Phase ladder above; DNA and spawning are the only new pillars — resist others until v1 ships |
| Operational-data sync temptation (copying ERP/WMS/HR data into the DNA) | §4.6 hard line: knowledge only, live lookups via connectors; sync requests surface as proposals an owner must reject |
| Agent-to-agent chatter (error amplification, unauditable loops, token burn) | §8.11: communication only via asks / board / playbooks; no free-form agent channels; disputes escalate to humans |
| Assistant scope drift after role change; bulk reads of restricted data | Scope mirroring refreshed on role change and revoked on offboarding (§6.4); rate/volume limits + audit on restricted-domain reads |
| Directive decay (decisions published, never decomposed; initiatives go stale) | §5.1: initiatives are first-class — lead, deadline, status; the goal slice keeps directives in every relevant prompt (§4.2); stalled initiatives escalate like asks (§8.10); close runs the dependency check (§6.3) |
| Reorgs outpace the model (domain splits, role overhauls, departures mid-initiative) | Topology changes are governed single-event ops with stable ids (§4.4); offboarding walks every dependency with admin-custody fallback and a last-admin guard (§5); template upgrades rebase running staff in place (§6.5) |

### 13.1 Known unhandled edge cases (documented, not designed)

Three audit passes: v2.9 closed the cheap seams inline (§4.4, §5.1, §6.3, §6.5, §8.1, §8.10);
v2.10 ran a second sweep and re-ranked the residue by severity; v2.11 ran a third sweep and
closed its findings inline (deputy governance, escalation cycles, review-queue SLA, retire and
offboarding walk completeness, trigger catch-up, paused-initiative semantics, topology-op
serialization, stale ask payloads, atomic count caps, store-ingest quarantine, taint residue,
affinity starvation, rebind capability checks, rehire semantics, named-access staleness,
decision-vs-rule contradictions, provenance rot, scanner override). The residue below still
needs real design, not a sentence:

**Blocks correctness or the first enterprise deployment**

- **Multi-approver quorum rules are inexpressible.** The plan's own flagship rule — "Invoices >
  $10k require two approvals" (§4.1) — cannot run: asks close on the first response (§8.10) and
  delegated authority selects *the* approver; no countersign or N-of-M semantics exist anywhere
  in the rule or ask model. Must land with the tier-2 write gates (§8.2, §14.11), before
  money-moving writes become routine.
- **External writes without atomicity.** No transactional boundary spans external systems: the
  TTL reaper can kill an ephemeral worker mid-write (§6.2) — half-posted invoice, partial commit
  — and a playbook retrying a failed node duplicates side effects (§8.6). Needs staged writes
  (prepare → confirm → commit) or per-connector compensations before write-capable ERP/WMS
  integration (§14.9).
- **Trigger idempotency.** A replayed webhook or duplicated API trigger (§8.5) fires duplicate
  runs and duplicate external writes; no idempotency key exists anywhere in the trigger, ask,
  or run model. Headless 24/7 operation (§11 P4) needs dedupe at the trigger boundary.
- **Erasure vs. provenance — and the append-only ledgers.** The §4.5 carve-out deletes content,
  but provenance *is* member references; the audit log and spend ledger carry member references
  too and are append-only by design (§10); and a multi-node org can place EU employee data on a
  US node — residency is unmodelled (§3). Legal hold remains unaddressed. Needs a
  data-governance pass before the first enterprise deployment.
- **db-only domains vs. reconstructibility.** §4.4 promises prior states survive topology
  changes via git history + audit; §4.5 exempts HR/Finance from git, so their topology history
  rests on audit alone and the history-rewrite remediation doesn't apply to them. The
  topology-op guarantees need restating per store kind.

**High — correctness under concurrency and failure**

- **Check-then-spend races.** Spend caps (§6.2) and §6.4 rate/volume limits are evaluated per
  run: two parallel runs at 49% of a ceiling both pass, then both spend past it. Needs
  reservation-style metering (reserve at write-planning, settle at completion) in the spend
  ledger.
- **Workspace-rebind dual-writer.** An admin rebind (§9, §3) while a partitioned-but-alive node
  still executes a claimed run puts two writers on one repo — a fencing problem, distinct from
  §14.16's staleness question; needs epoch/lease fencing on workspace claims.
- **Partitioned-node authority.** A node cut off from the control plane keeps executing under
  cached DNA version and scopes for as long as it runs (§3 trust model); max staleness, lease
  expiry, and post-reconnect audit reconciliation are undesigned — parked as §14.16.
- **Restore vs. live state.** A point-in-time restore (Phase 8a) against nodes and an
  append-only audit holding newer events needs rewind reconciliation — the inverse of §14.16's
  reconnect problem, equally undesigned.
- **Rules are not re-checked mid-run.** §8.1(c) re-checks scopes before each external write but
  not rules — a run started under the old regime can complete a write the new rule forbids.
  Whether rules gate writes like scopes or stay advisory context is a semantics call, not a
  sentence.
- **Ask storms.** The circuit-breaker (§6.2) caps spend, not attention: a mass incident fans
  out N failed runs, each raising an escalation ask (§8.10). Digests batch but nothing collapses
  identical asks or sheds load — the attention-side twin of circuit-breaker collateral (below).
- **Self-approval in multi-human orgs.** A domain owner proposing and publishing their own rule
  is structurally unchecked (§4.3–§4.4) — no malice required, unlike the malicious-insider
  boundary below. Needs a separation-of-duties knob per domain; §14.13 governs strictness, not
  self-review.
- **Model-provider outage.** The gateway fronts multiple providers (§3) but has no degradation
  strategy — queueing, fallback routing, degraded-mode behavior for a 24/7 org (§11 P4) are
  undesigned — parked as §14.15.
- **Cross-initiative dependencies.** Close checks are self-contained (§5.1); initiative DAGs
  ("Austin depends on ERP migration") are unmodelled — closing an upstream initiative can strand
  a downstream one. Needs a dependency representation before multi-initiative orgs.

**Medium — real seams, bounded blast radius today**

- **Clock skew and calendar semantics.** TTL reaping, SLA deadlines, effective windows, and
  delegation expiry (§4.2, §6.2, §8.10) assume synchronized clocks — a skewed node can act
  under an expired delegation; `queue_until_morning` (§8.1) has no timezone semantics for a
  distributed org — whose morning? Needs a time authority plus per-human calendars.
- **Proposal amendment.** Only `withdraw` exists (§7): an edit during review forces
  withdraw-and-refile, and racing proposals can publish contradictory versions sequentially —
  contradiction detection (§4.4) runs pairwise at publish, not re-run on concurrent publishes.
- **Runtime precedence conflicts.** Contradiction detection is proposal-time only (§4.4): an
  injected rule vs. a retrieved card vs. the goal slice (§4.2) can disagree mid-run with no
  defined precedence. Stating an order is cheap; choosing the right one is a design call.
- **Taint decay.** Taint flags gate the tainted run and its proposals (§13), but tainted content
  that lands in project memory (§8.3) and is retrieved weeks later by an untainted run has no
  propagation or decay semantics.
- **Playbook recursion.** The depth cap bounds agent spawns (§6.2), not playbook→playbook
  instantiation loops (§8.6) — a self-instantiating playbook starves sandbox quotas without
  ever tripping spawn policy.
- **Git integrity guard.** "Git history *is* the DNA timeline" (§4.5), yet nothing detects or
  refuses an accidental force-push/rebase that destroys citations and supersession chains —
  needs signed refs or commit signing alongside the §10 checklist.
- **PAT lifecycle.** PATs are hashed, shown once, and scoped (§10), but have no revocation
  endpoint (§9), expiry, or rotation flow — compromise detection is undesigned.
- **Circuit-breaker collateral.** The org-wide spend ceiling halts *all* spawns and automations
  (§6.2), critical runs included; there are no per-run criticality classes or carve-outs.
  Revisit with §14.11 (budgets) — until then a noisy consumer can stop the org, deliberately.

**Low — real but bounded**

- **Offboarding vs. authored proposals.** The §5 dependency walk reassigns owned domains, asks,
  and initiatives but leaves the departing member's pending DNA proposals in queue under a
  stale proposer — they should auto-withdraw or transfer with the successor.
- **Embedding-model switch.** Switching embedding models (§14.7 local fallback) invalidates the
  entire vector index; the Phase-0 spike tests per-model determinism, not re-index migration.
- **Glossary alias collisions.** Aliases are per-domain (`dna_glossary.domain_id?`, §7), but a
  multi-domain workspace injects several glossary slices (§4.2) — colliding aliases have no
  disambiguation rule.
- **Malicious insider.** Governance assumes humans are the trust anchor: a domain owner
  publishing a poisoned rule gets every agent obeying it; audit is after-the-fact, and nothing
  sits above the owner short of admin. Accepted boundary of the trust model, stated so nobody
  is surprised.

## 14. Key open decisions

1. **DNA canonical store**: plain git repo vs. DB-with-export (default: git-backed markdown).
2. **Human auth v1**: local accounts (default) vs. OIDC-only for companies with SSO.
3. **First deployment shape**: single-process on an office machine (default) vs. containerized server from day one.
4. **Ephemeral worker default TTL & quota**: 24h / 3 concurrent (default) — tune with use.
5. **Tier-1 business suite**: Microsoft 365/Graph (default) vs Google Workspace.
6. **First IM channel**: Slack (default) vs Discord vs Telegram.
7. **Embeddings**: API (default) with local fallback.
8. **Name/branding**: working title pending trademark + domain search.
9. **Tier-2 connector priority**: which enterprise system first (ERP vs HRIS vs CRM) — decide when the first company deployment names its pain; not before v1 ships.
10. **Personal-assistant rollout**: opt-in per employee (default) vs org-wide mandate.
11. **Business budgets**: display-only field on initiatives (default) vs enforcement tied into the §8.2 tier-2 write gates — revisit with the first write-capable ERP/WMS connector.
12. **Deployment perimeter**: one deployment per company (default) — M&A-style consolidation of two deployments is a migration project, not a runtime feature.
13. **Per-domain proposal strictness**: every proposal reviewed (default) vs opt-in auto-publish for low-blast-radius domains (audited, retro-reviewable) — revisit when proposal volume drowns owners.
14. **Ask SLA tier defaults**: how long each tier runs before breach-and-escalate (e.g. `critical` 1h, `standard` to next digest, `bulk` 24h) — defaults tuned with the first real org; ask deadlines derive from these unless set per ask (§8.10).
15. **Model-provider degradation**: single provider (default) with manual fallback vs. automatic multi-provider routing and queueing for headless runs — design before the first 24/7 deployment leans on one vendor's uptime (§13.1).
16. **Partitioned-node authority**: how long a node may act on cached scopes/DNA without a control-plane heartbeat (lease vs. unlimited trust), and what reconnect reconciliation owes the audit log — decide with Phase 6 node registration (§13.1).
