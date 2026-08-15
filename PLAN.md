# Coworker — Product & Engineering Plan

> A local-first "digital employee" platform: autonomous AI coworkers with roles, memory, skills,
> knowledge, permission boundaries, schedule/event/API automation, and multi-stage workflow
> orchestration. Role-universal from day one: a Coworker can be a software engineer, an executive
> secretary, an HR specialist, a finance analyst, or any repeatable enterprise role.
> ("Coworker" is a working title — see §10.)

---

## 1. Product vision

**Coworker** is a desktop-resident agent platform. A daemon runs 24/7 on the user's machine,
executing real work — files, documents, calendars, external systems — while a locally served web
console manages a staff of AI coworkers. The core thesis: an AI worker is only production-ready when
it stands on **five pillars — identity, memory, skills, division of labor, and permission
boundaries** — and when it is trusted with real responsibilities, not demos.

| Aspect | Design |
|---|---|
| Delivery | Desktop-resident service (macOS/Win/Linux) running 24/7; managed via a locally served web console |
| Core object | **Coworker** = digital employee with name, avatar, description, runtime, online status, hire date, work record |
| Identity | Each role defined by three files: `IDENTITY.md` (role & responsibilities), `STYLE.md` (work & communication style), `HANDBOOK.md` (rules, quality bars, boundaries) |
| Task modes | 1) **Conversation tasks** (chat with session continuity) 2) **Automations** (schedule / event / API triggers) 3) **Playbooks** (multi-stage, multi-coworker, parallel, human-in-the-loop workflows) |
| Memory | Personal memory (per-Coworker, all sessions) + project memory; auto-summarized after effective sessions; timeline + version rollback; editable/deletable |
| Skills | Marketplace of `SKILL.md`-based skills + custom upload; installed per-Coworker |
| Knowledge | Knowledge bases: sources (files/URLs) → compiled into atomic knowledge cards; bound to Coworkers; cited in answers (RAG) |
| Connectors | MCP services (manual config, JSON import, curated presets), per-Coworker; IM channels with session pairing |
| Groups | 2–8 Coworkers per group, one local Leader; shared tasks, per-member artifacts |
| Permissions | Per-Coworker file scope (directory allowlist), tool scope (tool allowlist), model policy, approval gates; read-only test run recommended; least privilege |
| Playbook engine | Script DSL: `phase`, `log`, `worker`, `parallel`, `pipeline`, `askUser`, `workflow` (sub-playbook), `action` (HTTP/local); auto-generated canvas; run history with statuses; triggers (schedule/API/Git/event) |
| Automation | Scheduled (timezone-aware, next-run visible), event (GitHub issues/PRs via webhook), API (POST URL + PAT bearer auth, `{{field}}` templating, `continuationKey` session reuse); run-now; pause/resume/copy |
| Task board | Cross-coworker tasks & to-dos: statuses, artifacts |
| Safety posture | Stopping a run never undoes completed side effects (stated plainly in UI); approval cards gate high-risk actions; secrets banned from memory/logs; PATs shown once |

---

## 2. Scope & guiding principles

**Principles**
1. **Local-first**: daemon + SQLite + files on the user's machine; no cloud dependency except LLM APIs.
2. **Files as source of truth** where users expect to edit (identity files, memory, skills are markdown); SQLite for indexes, runs, and timelines.
3. **Every capability is a guarded tool**: the agent runtime cannot touch anything not explicitly allowed (file/tool scopes enforced in code, not prompt).
4. **Provider-agnostic models**: OpenAI-compatible, Anthropic, and local (Ollama) behind one gateway interface; per-task model picker with an "Auto" router.
5. **Original identity**: all naming, copy, and visual design are created fresh for this product; nothing is copied from existing products.
6. **Role-agnostic core**: the runtime, guards, memory, playbooks, and triggers know nothing about engineering; every role difference is expressed as data (templates, skills, connectors, permissions).

**Role universality — a hard requirement.** Most agentic tools are built by and for developers; we
serve every department from day one, so any enterprise persona can be hired as a Coworker. Target
template catalog:

| Department | Example Coworkers | Key connectors (MCP) | Example skill packs |
|---|---|---|---|
| Engineering | Backend Engineer, QA Specialist, SRE | Git hosts, CI/CD, issue trackers | repo-analysis, test-review, incident-triage |
| Executive/Management | Executive Secretary, Chief-of-Staff Assistant | Email (Graph/Gmail), calendar, docs suite | morning-brief, meeting-minutes, travel-research, inbox-triage |
| HR | Onboarding Specialist, Recruiting Coordinator | ATS, email, docs | onboarding-checklist, jd-drafting |
| Finance | AP/AR Clerk, Expense Auditor | ERP/accounting, e-signature, email | expense-digest, invoice-match |
| Sales/Marketing | Sales Ops Analyst, Content Drafter | CRM, social schedulers | lead-qualify, content-calendar |
| Support | Support Agent (customer service) | Ticketing, IM channels | kb-answer, ticket-triage |
| Ops/Legal | Procurement Assistant, Contract Reviewer | Drive/SharePoint, e-signature | sop-checklist, contract-redline |

Design consequences (woven through this plan):
- **Workspaces, not repos**: a project may be a local folder, a cloud-drive mount, or purely a
  connector scope (mailbox, calendar, CRM queue) — see `projects.kind` in §4.
- **Deliverables are files *or* connector actions**: draft doc, calendar invite, sent email (behind
  an approval gate), CRM update — the run-result schema treats them uniformly.
- **External-facing actions are approval-gated by default**: an email to a customer costs more than
  a bad diff; the headless approval policy (§5.1) exists precisely for the secretary-at-2am case.
- **Knowledge base matters more for non-IT roles**: SOPs, policies, and glossaries are the main
  context for HR/finance/support Coworkers.

**Scope ladder** (each phase ships something usable)

- **MVP (Phases 0–1)**: one Coworker, chat console, real tool use on local files, permission guards, session history.
- **Core product (Phases 2–3)**: role templates + identity files, memory with timeline/rollback, skills + tier-1 connectors (email/calendar/docs), automated tasks (schedule + API triggers).
- **Full platform (Phases 4–6)**: Playbook engine + canvas, event/webhook triggers, knowledge base RAG, groups, one IM channel.
- **Production (Phase 7)**: packaging, security hardening, updater, docs.

**Explicit non-goals (v1)**: multi-tenant cloud, team sharing roles, mobile app, remote device fleet
(a single "device" = this machine), marketplace publishing workflow (local skill install only).

---

## 3. Recommended architecture

```
┌────────────────────────────  User's machine  ─────────────────────────────┐
│  Browser (or Tauri window)                                                │
│  ┌─────────────────────────── Web Console (React SPA) ─────────────────┐  │
│  │ Coworkers │ Chat │ Playbooks │ Automations │ KB │ IM │ Settings     │  │
│  └──────────────────────────────▲──────────────────────────────────────┘  │
│                 │ REST + WebSocket (localhost, token-auth)                 │
│  ┌──────────────┴─────────── Coworker Daemon (Node/TS) ─────────────────┐  │
│  │ API server │ WS hub │ Auth (PAT, console token)                      │  │
│  │ ── Orchestrator ────────────────────────────────────────────────────│  │
│  │  Agent runtime (session workers, streaming, approvals, cost meter)   │  │
│  │  Trigger engine (cron, webhooks, REST API triggers)                  │  │
│  │  Playbook engine (sandboxed DSL: worker/parallel/pipeline/askUser/…) │  │
│  │  Memory service (personal/project, timeline, versions)               │  │
│  │  RAG service (ingest → chunk → embed → cards → cited retrieval)      │  │
│  │ ── Guards ── File scope │ Tool scope │ Model policy │ Audit log      │  │
│  │ ── Integrations ── MCP client │ HTTP actions │ IM channel adapter    │  │
│  └──────────────┬───────────────────────────────────────────────────────┘  │
│                 │                                                          │
│  SQLite (coworker.db) + ~/.coworker/ (coworkers/, skills/, kb/, playbooks/)│
│  Model gateway ──► OpenAI-compatible / Anthropic / Ollama (local)          │
└────────────────────────────────────────────────────────────────────────────┘
```

### Stack decision (recommended, with alternatives)

| Layer | Recommendation | Why | Alternatives |
|---|---|---|---|
| Daemon/runtime | **Node.js 22 + TypeScript** | First-class MCP SDK & AI SDKs; one language across daemon + playbook DSL; fast iteration | Python (strong agent ecosystem, but two-language stack), Go/Rust (perf, slower to build) |
| Console UI | **React 18 + Vite + Tailwind + shadcn/ui** | Fast, dense admin-style UI; component ecosystem | Next.js (unneeded — served locally by daemon) |
| Desktop shell (later) | **Tauri 2** wrapping the local console | Small binary, auto-update, tray | Electron (quicker but heavy); Phase 7 — v1 is daemon + browser tab; the desktop shell is polish, not a dependency |
| Storage | **SQLite** (better-sqlite3 + Drizzle ORM) + **sqlite-vec** for vectors | Single-file, embedded, vector search without a server | LanceDB, Postgres+pgvector (if cloud later) |
| Agent loop | Custom thin loop (plan → act → observe → verify) using provider SDKs | Full control over guards, streaming, approvals, memory injection | LangGraph (opinionated; adds abstraction cost) |
| Playbook sandbox | `isolated-vm` executing TS-like DSL with injected async host functions | Matches the script model; no `eval` of raw user code in-process | JSON graph interpreter (safer, less expressive) |
| Realtime | WebSocket (Socket.IO or ws) | Streaming tokens, approval cards, run status | SSE (simpler, one-way) |
| Scheduler | `croner` + persisted next-run in SQLite | Survives restarts, timezone-aware | node-cron (in-memory only — not enough) |
| Ingestion/RAG | `unpdf`/mammoth/xlsx for docs, local or API embeddings, hybrid BM25+vector | Runs locally, cheap | Hosted RAG services |

---

## 4. Data model (SQLite, first cut)

```
coworkers       (id, name, avatar, description, role_template_id, status, created_at)
role_templates  (id, name, description, avatar, identity_md, style_md, handbook_md, default_skills json)
projects        (id, name, description, kind 'local_folder'|'cloud_mount'|'connector_scope', path_or_ref, scope 'public'|'private')
coworker_projects (coworker_id, project_id)                -- single source of binding (no coworker_id on projects)
permissions     (coworker_id, file_scopes json, tool_scopes json, builtin_tools json, model_policy json,
                 approval_policy json)                      -- headless approval behavior, see §5.1
groups          (id, name, leader_coworker_id, created_at)
group_members   (group_id, coworker_id, added_at)           -- add-only, 2–8 members; leader must be local
board_tasks     (id, coworker_id?, group_id?, session_id?, source 'agent'|'user', title, detail,
                 status 'todo'|'doing'|'blocked'|'done', assignee_coworker_id?, created_at, updated_at)
sessions        (id, coworker_id?, group_id?, project_id, type 'chat'|'automated'|'playbook'|'group', title, status, created_at)
messages        (id, session_id, role, content, attachments json, model, tokens_in/out, cost, created_at)
approvals       (id, session_id, tool_call json, status 'pending'|'approved'|'denied', decided_at)
runs            (id, session_id, trigger_type, status, started_at, finished_at, result_summary, error)
automated_tasks (id, coworker_id, name, description, project_id, model, enabled, max_runs, deadline)
triggers        (id, task_id, type 'schedule'|'event'|'api', config json, webhook_token, next_run_at, last_run_at)
skills          (id, name, description, source 'market'|'custom', path, installed_at)  + coworker_skills join
memory_items    (id, coworker_id, project_id?, category, content, source 'auto'|'manual', supersedes_id, created_at)
memory_events   (id, coworker_id, project_id?, kind 'add'|'edit'|'delete'|'rollback', payload json, at)   -- timeline
memory_versions (id, coworker_id, project_id?, scope 'personal'|'project', version, snapshot json, created_at)
playbooks       (id, name, description, script, canvas_layout json, version, created_at)
playbook_runs   (id, playbook_id, args json, status, current_node, waiting_approval_id?, result json, logs json, started_at, ended_at)
playbook_triggers (id, playbook_id, type, config json)
knowledge_bases (id, name, owner_email?)         kb_bindings (coworker_id, kb_id)
kb_sources      (id, kb_id, kind 'file'|'url', path/url, content_hash, imported_at)
kb_cards        (id, source_id, title, definition_md, refs json, links json)
kb_chunks       (id, source_id, card_id?, text, embedding blob)   -- sqlite-vec index + FTS5 table over text (hybrid BM25/vector)
im_channels     (id, kind, config json, status)  im_pairings (id, channel_id, session_key, coworker_id, model, project_id, status)
api_tokens      (id, label, hash, scopes, created_at, last_used_at)   -- PATs, shown once
audit_log       (id, coworker_id, session_id, tool, args json, verdict 'allow'|'deny'|'confirm', at)
-- SQLite in WAL mode; model/provider config lives in ~/.coworker/config.toml (not the DB)
```

On-disk layout (user-editable, git-friendly):

```
~/.coworker/
  coworker.db
  coworkers/<id>/identity.md|style.md|handbook.md
  skills/<skill>/SKILL.md (+ resources)
  playbooks/<id>/playbook.ts (script) + canvas.json
  kb/<id>/sources/…
  backups/            -- memory snapshots, db backups
```

---

## 5. Subsystem designs

### 5.1 Agent runtime (the heart)

- **Session worker** = one execution of a task (chat turn, automated run, or playbook `worker()` node).
- **Prompt assembly**: system prompt = identity.md + style.md + handbook.md + relevant memory
  (personal + project) + installed skills' summaries + bound-KB retrieval context + tool manifest +
  permission constraints. Local/cloud-folder workspaces get a tree summary on first use;
  connector-scope workspaces get a connector manifest instead.
- **Loop**: model → tool calls → scope checks → execute (or emit approval card and park) → observe →
  repeat → final answer + structured result (deliverables list, files/objects touched).
- **Scopes (enforced in code)**:
  - *File scope*: every path resolved (realpath, no symlink escape) against per-Coworker allowlist; reads outside allowlist denied; writes additionally require the directory to be write-enabled.
  - *Tool scope*: allowlist of tool classes (fs, shell, web.fetch, web.search, mcp.<name>.*); shell requires explicit enable + confirmation for non-allowlisted commands.
  - *Model policy*: max cost/run, model whitelist, redacted-regex list scrubbed before egress.
  - *Audit*: every call logged with verdict.
- **Approvals**: high-risk ops pause the run, push an approval card over WS to console (and later IM); resume/deny/timeout policy per Coworker.
- **Headless approval policy** (automations & playbook runs — nobody is watching at 2am): per-Coworker
  `approval_policy` = `auto_deny` (default) | `queue_until_morning` (parked approvals surface on the
  Task Board at next login) | `escalate_im` (approval card pushed to the paired IM channel). Gated
  actions are never silently allowed in headless runs, and external-facing connector actions (send
  email, CRM write, calendar invite to outsiders) stay gated regardless of mode — a secretary
  Coworker must not send a 2am email on its own.
- **Structured results → Task Board**: every run returns a typed result (deliverables, files/objects
  touched, open to-dos); to-dos land in `board_tasks` automatically, and users can create their own.
- **Write concurrency**: one writer per project at a time — a project-level lock serializes mutating
  runs (groups and `parallel()` playbook nodes included); readers are unaffected.
- **Stop semantics**: stop kills the loop but never claims to undo completed side effects; the UI
  states this plainly.
- **Cost metering & "Auto" router**: token/cost per run; Auto = cheap model for planning/simple,
  strong model for code-heavy steps (start with a static heuristic router).

### 5.2 Tools & MCP

Built-ins v1: `fs.read/write/patch/list/search`, `shell.exec` (guarded), `web.fetch`, `web.search`,
`kb.search`, `memory.write` (goes through memory service, never raw file writes).
**Egress guard** (applies to `web.fetch`, playbook `action(http)`, and any connector making raw HTTP
calls): private/link-local CIDRs (127/8, 10/8, 172.16/12, 192.168/16, 169.254/16, ::1, fd00::/8),
`file://`, and cloud metadata endpoints blocked by default; per-Coworker URL allowlist/denylist on top.
MCP client via `@modelcontextprotocol/sdk`: per-Coworker connector registry (manual config, JSON paste,
curated presets later), tool discovery, health check ("Detect → Connected"), per-tool enable flags.
**Connector catalog roadmap** (what makes non-IT roles real): tier 1 = email + calendar + docs suite
(Microsoft Graph or Google Workspace — see §10); tier 2 = Git hosts, issue trackers, ticketing;
tier 3 = CRM, ERP/accounting, e-signature, IM. Each connector ships as an MCP server + companion
skill pack that teaches the Coworker the tool's conventions.

### 5.3 Memory service

- Two scopes (personal/project). After each effective session: LLM summarize → propose additions →
  merge (dedupe, supersede) → write items + timeline event + version snapshot.
- UI: overview, category cards, full-memory editor with conflict detection, timeline (time, source,
  scope, category), version diff + rollback.
- Hard rules: secrets scanner blocks tokens/passwords; stale-item cleanup prompts.

### 5.4 Skills

`SKILL.md` with name/description frontmatter (+ optional scripts/resources). Loader injects skill
summaries into prompt; full skill content lazy-loaded when the model invokes `skill.read`. Local
"marketplace" = a git-synced folder of skill packs; custom upload validates structure; uninstall
checks dependents (automated tasks/playbooks) first. Skill packs are domain-organized (see the §2
catalog) so a secretary Coworker installs meeting-minutes and travel-research, not test-review.

### 5.5 Trigger engine (Automations)

- **Schedule**: croner + persisted `next_run_at`; timezone-aware; max_runs/deadline; "run now".
- **API**: `POST /api/v1/triggers/<token>` with PAT Bearer; `{{field}}` templating into the task
  description; optional `continuationKey` to continue a business session (documented as ≠ idempotency).
- **Event**: webhook receiver for GitHub (issues/PRs/comments) with per-trigger secret + filters
  (repo/branch/type); "test check" button; event activity log.
- Executor reuses the same session-worker as chat (single code path), with a fresh session per run
  unless session reuse requested; failures surface in run history with the standard triage checklist.
- Headless runs follow the Coworker's approval policy (§5.1) — automations are headless by design,
  and that is only safe because gated actions deny/park instead of deadlocking.
- Cross-domain examples, same machinery: "Main repo daily risk inspection" (engineer) and "Morning
  brief: today's calendar, unread VIP email, open to-dos" at 07:00 (secretary).

### 5.6 Playbook engine

- DSL (TypeScript-subset, executed in isolated-vm with injected async host functions):

```ts
phase("Triage");
const issues = await worker("triage", "Classify open issues from {{repo}}", { project: "main" });
phase("Fix");
const results = await parallel(issues, (i) => worker("fixer", `Fix: ${i.title}`, { input: i }));
phase("Review");
const ok = await askUser("Approve PRs?", { options: results.map(r => r.prUrl) });
return { fixed: results.length, approved: ok };
```

- Host functions: `phase/log/worker/parallel/pipeline/askUser/workflow/action(http|local)`.
- Creation flow: describe goal → generate canvas + script → edit either → manual run → add trigger.
  Canvas = React Flow graph rendered from an AST/instrumentation trace of the script
  (Phase/Worker/Parallel/AskUser/Action nodes); keep script authoritative.
- Run states: Queued/Running/Waiting-for-Input/Completed/Failed/Terminated; per-node logs
  (`+2m33s` relative timestamps), final return value surfaced explicitly; retry reuses completed
  nodes; versions immutable.
- Delivery note: the canvas starts read-only in Phase 4 (a generated view of the script); interactive
  canvas editing lands as Phase 4.5 only if needed — the script stays authoritative either way.

### 5.7 Knowledge base (RAG)

- Ingest files (PDF/Word/Excel/PPT/MD/TXT/CSV/HTML/images-OCR-later) + URLs (dedupe by hash).
- Pipeline: parse → chunk → embed (local or API) → store in sqlite-vec; **card compilation** = LLM
  pass distilling atomic cards (definition, references, links) — cards are the retrieval and citation
  unit; hybrid search (BM25 + vector); answers cite cards, console opens the card in a right panel.
- Scheduled maintenance jobs: recompile, source refresh, knowledge quality check.
- Non-IT reliance: for HR/finance/support Coworkers the KB (SOPs, policies, glossaries) is the primary
  context — retrieval quality is their core dependency, which is why KB is Phase 5, not later.

### 5.8 Groups & IM (Phase 6)

- Groups: 2–8 Coworkers, local Leader receives unaddressed messages, `@member` routing, shared task
  view (progress, assignments, per-member artifacts); members add-only (rebuild to change) — v1
  keeps the same constraint for simplicity. Example group: secretary Coworker (Leader) + analyst +
  writer producing a weekly business review.
- IM: one channel to start (Slack or Discord — pick by your preference). Bot adapter → pairing
  requests (verify initiator → assign Coworker/model/directory → Allow) → paired sessions route
  inbound messages to the Coworker's session worker; replies and approval cards go back to the channel.

### 5.9 Console UI (screens)

1. **Coworker Management** — cards (name, desc, runtime, online dot, local tag), filters, actions
   (chat / new automation / delete — sharing is post-v1), New Coworker (Hire) wizard
   (template → identity → device) with the template picker organized by department (§2 catalog).
2. **Chat** — left rail (Coworkers/groups/history), top bar (executor + entry points), message stream
   (execution process, artifacts, approval/question cards), composer (workspace picker, `+`
   attachments, `@` file refs, model selector incl. Auto).
3. **Coworker detail** — left config menu: Home, Projects, Automations, Chat history, Playbooks,
   Memory, Skills, Knowledge, Connectors, IM, Permissions (support-agent records only if the
   Phase 6+ customer-service mode ships).
4. **Playbooks** — list page; editor (canvas + script + versions tabs, run button, triggers);
   run history (status, per-phase/node logs, waiting-input prompt, final return).
5. **Automations** — task cards (More: pause/resume/edit/copy/delete/run-now), detail (trigger
   config, next run, event activity, run history, API trigger docs with copy-URL + body schema).
6. **Task Board** — cross-coworker tasks/to-dos/statuses/artifacts; items come from agent-run result
   schemas plus user creation (source of truth: `board_tasks`, §4).
7. **Knowledge Base** — sources tree + cards tree, compile button + progress, retrieval QA box,
   bindings, scheduled jobs.
8. **IM** — channel config, pending pairing queue, paired sessions.
9. **Settings** — models & keys (OS-encrypted storage via Tauri `safeStorage`; config file at
   `~/.coworker/config.toml`), devices, diagnostics, updates, PATs.

---

## 6. API surface (daemon, localhost + token)

```
POST /api/v1/coworkers         GET/PATCH/DELETE /api/v1/coworkers/:id
GET  /api/v1/coworkers/:id/memory   + memory items CRUD, timeline, rollback
POST /api/v1/sessions          (type, coworker, project) → id
POST /api/v1/sessions/:id/messages        (streaming over WS channel session:<id>)
POST /api/v1/approvals/:id     (approve|deny)
CRUD /api/v1/automated-tasks   + POST …/:id/run  + POST …/:id/pause|resume
POST /api/v1/triggers/api/:token          (PAT; API trigger ingress)
POST /api/v1/webhooks/github/:triggerId   (event trigger ingress, signature-verified)
CRUD /api/v1/playbooks         + POST /api/v1/playbooks/:id/run  + runs endpoints
CRUD /api/v1/kb, /api/v1/kb/:id/sources, POST …/compile, POST …/retrieve
WS   /ws  → events: token.stream, run.status, approval.requested, …
```

---

## 7. Delivery plan

| Phase | Deliverable | Key work | Est. (1 dev) |
|---|---|---|---|
| **0. Foundations** | Repo, CI, daemon skeleton serving console | Monorepo (pnpm), TS strict, Drizzle+SQLite (WAL), config, logging, REST+WS bootstrap, console shell with nav, CI matrix for Windows/macOS/Linux native builds | 1 wk |
| **1. MVP agent** | Chat with a Coworker doing real local work | Model gateway (OpenAI-compat/Anthropic/Ollama), agent loop, built-in fs/shell/web tools + egress guard, file/tool scopes, approval cards, audit log, session persistence, streaming chat UI, workspace picker, stop button | 4–5 wks |
| **2. Identity, memory, skills, connectors** | Coworkers feel like employees in any department | Role template catalog seeded across departments (§2), IDENTITY/STYLE/HANDBOOK files, New Coworker wizard, memory service (auto-summary, timeline, versions, editor), skill loader + local market + upload + starter skill packs, MCP client + tier-1 connector pack (email/calendar/docs), workspaces incl. cloud mounts & connector scopes | 3–4 wks |
| **3. Automation** | 24/7 operation | Schedule triggers (croner + persistence, run-now), API triggers + PATs + `{{field}}` templating, run history UI, GitHub webhook events, headless approval policy, task board (agent-emitted + user to-dos) | 2–3 wks |
| **4. Playbooks** | Multi-stage orchestration | DSL + isolated-vm host functions, playbook runner w/ statuses, askUser approval cards, read-only script-generated canvas (interactive editor as Phase 4.5 if needed), versions, playbook triggers | 3–4 wks |
| **5. Knowledge base** | Cited answers from your docs | Ingestion pipeline, chunk+embed+sqlite-vec, card compilation, retrieval tool + citations panel, KB bindings, scheduled maintenance | 2–3 wks |
| **6. Groups + IM** | Team of employees | Group sessions (Leader routing, @mentions, artifacts), one IM channel with pairing flow | 2–4 wks |
| **7. Production** | Shippable v1 | Tauri shell + tray + updater, encrypted secret storage, backup/restore, telemetry (opt-in), docs, installer scripts (curl/npx), security review | 2 wks |

**Total: ~19–26 weeks solo** (compress ~40% with AI-assisted development; MVP demo-able in week 6).
Suggested order note: Phase 5 can swap with 4 if RAG matters more to you than playbooks; Phase 6
can be cut entirely for v1 without breaking the story — but keep the tier-1 connector pack in
Phase 2: it is what makes the platform "any employee" rather than "another coding agent".

---

## 8. Testing & quality

- **Unit**: scopes (path-escape, symlink tricks, egress CIDR/allowlist, headless approval policy),
  scheduler math (timezones, DST), `{{field}}` templating, memory merge/dedupe, project write-lock.
- **Integration**: agent loop against a mock model with scripted tool calls; trigger ingress
  (signature verification, PAT auth); playbook engine (parallel fan-out, askUser park/resume).
- **E2E**: Playwright over the console — create Coworker → chat → approve a scoped write → verify file.
- **Golden runs**: recorded "daily report" and "issue triage → fix → ask user" playbooks replayed in
  CI with fake models to catch regressions.
- **Chaos-lite**: kill daemon mid-run → restart → runs resume or fail cleanly with audit intact.

## 9. Security checklist (build-in, not bolt-on)

- Console bound to 127.0.0.1 with per-install bearer token; PATs hashed + shown once; scopes.
- Secrets in OS-encrypted storage (Tauri `safeStorage`; the Node `keytar` module is archived — do
  not build on it); secret scanner on memory, logs, and KB; regex redaction before any egress to
  model providers.
- Webhooks signature-verified; MCP connectors run with least privileges; tool allowlists default-deny.
- Egress guard on every raw-HTTP capability (§5.2): private CIDRs, `file://`, and cloud metadata
  endpoints blocked by default — an agent with filesystem access must not be usable as an SSRF pivot.
- Audit log immutable (append-only table); destructive-action confirmations (delete Coworker, unbind
  KB) check dependents first.
- Sandboxing: isolated-vm for playbook scripts; shell tool behind explicit opt-in + command confirmations.

## 10. Key open decisions (defaults chosen, flag if you disagree)

1. **Stack**: Node/TS daemon + React console + SQLite — *default yes*; alternative Python daemon.
2. **Desktop shell**: browser tab first, Tauri wrap in Phase 7 — alternative Electron from day 1.
3. **First IM channel**: Slack (default) vs Discord vs Telegram.
4. **Tier-1 business suite**: Microsoft 365/Graph (default for enterprise) vs Google Workspace —
   determines email/calendar/docs connectors and with them the secretary persona.
5. **Embeddings**: local (bge/e5 via Ollama) vs API (OpenAI/voyage) — default: API with local fallback.
6. **Name/branding**: "Coworker" is a working title — run a trademark + domain search and pick the
   final name before any public release; keep all copy original.

## 11. Risks & mitigations

| Risk | Mitigation |
|---|---|
| Agent reliability for unattended runs | Conservative scopes, approval gates before external writes, run-now dry tests, explicit success criteria in task descriptions |
| Cost runaway on 24/7 automation | Per-run cost caps, model policy per Coworker, spend dashboard, Auto-router |
| Playbook DSL complexity | Script stays authoritative; canvas is a view; version everything; retry reuses completed nodes |
| Native-module fragility (`isolated-vm`, `better-sqlite3`) across 3 OSes and inside a Tauri bundle | CI builds all platforms from Phase 0; prebuilt binaries; fallback: run playbook scripts in a sandboxed child process instead of an in-process VM |
| Non-IT roles over-promise (a Coworker that "books travel" needs many connectors) | Tiered connector roadmap (§5.2); secretary persona ships only with tier-1 connectors; skill packs absorb the long tail |
| Memory poisoning / stale rules | Timeline + rollback, manual review prompts, secret scanner, scope discipline (personal vs project) |
| Scope creep | Scope ladder above; every phase ships; non-goals enforced until v1 done |
| Name collision with existing products | Working title is a placeholder; final name chosen via trademark + domain search before release (§10) |
