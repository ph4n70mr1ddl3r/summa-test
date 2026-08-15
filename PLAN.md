# Qwake — Plan to Build a QoderWake Clone

> A comprehensive build plan for a local-first "digital employee" platform: autonomous AI workers
> ("Wakers") with roles, memory, skills, knowledge, permissions, scheduled/event/API automation,
> and multi-stage workflow orchestration — modeled on [QoderWake](https://qoder.com/en/qoderwake).

---

## 1. What we are cloning (product analysis)

QoderWake is **not** a chatbot UI. It is a desktop-resident agent platform: a local daemon executes
real work on your machine (files, repos, external APIs) while a **Web Console** (served locally,
opened in a browser) manages everything. Key facts distilled from the official docs:

| Aspect | How QoderWake works |
|---|---|
| Delivery | Installed on macOS/Win/Linux (curl script on Linux); runs 24/7 as a local service; managed via local Web Console |
| Core object | **Waker** = digital employee with name, avatar, description, runtime device, online status, hire date, work record |
| Identity | Custom roles defined by three files: `IDENTITY.md` (responsibilities), `PERSONA.md` (work style), `BIBLE.md` (guidelines) |
| Task modes | 1) **Conversation tasks** (chat, session continuity) 2) **Automated tasks** (schedule / event / API triggers) 3) **WakerFlow** (multi-stage, multi-Waker, parallel, human-in-the-loop workflows) |
| Memory | Personal memory (per-Waker, all sessions) + Project memory (per-project); auto-summarized after sessions; timeline + version rollback; editable/deletable |
| Skills | Marketplace of `SKILL.md`-based skills (name/description frontmatter) + custom upload; installed per-Waker |
| Knowledge | KB with sources (files/URLs) → compiled into atomic **Knowledge Cards** (definition + references + links); bound to Wakers; cited in answers; RAG retrieval |
| Connectors | MCP services (manual config, JSON paste, system presets), per-Waker; DingTalk-style IM channels with **session pairing** |
| Groups | 2–8 Wakers per group, one local Leader; shared tasks, per-member artifacts |
| Permissions | Per-Waker **Tool Guard**, **File Guard** (directory allowlist), **Built-in Tools** toggles, **Model Security**; read-only test task recommended; least privilege |
| WakerFlow engine | Script DSL with `phase`, `log`, `worker`, `parallel`, `pipeline`, `askUser`, `workflow` (sub-flow), `action` (HTTP/local); canvas auto-generated from script; run history with statuses Queued/Running/Waiting-for-Input/Completed/Failed; triggers (schedule/API/GitHub/event) |
| Automation | Scheduled (cron-like, next-run + timezone), event (GitHub issues/PRs via webhook), API trigger (POST URL + PAT Bearer auth, `{{field}}` payload templating, `wakeSessionUniqueId` session reuse); run-now; pause/resume/copy |
| Task board | Cross-Waker task list: statuses, to-dos, artifacts |
| Safety posture | "Stopping a task does not undo external operations"; approval cards for high-risk actions; secrets banned from memory/logs; PAT shown once |

**Sources:** product page, official blog (five layers: identity, memory, skills, division of labor,
permissions), and docs.qoder.com pages: overview, manage-wakers, conversation-tasks, wakerflow,
automated-tasks, memory, skills-and-integrations, knowledge-base, task-board, settings, troubleshooting.

---

## 2. Scope & guiding principles

**Principles**
1. **Local-first**: daemon + SQLite + files on the user's machine; no cloud dependency except LLM APIs.
2. **Files as source of truth** where users expect to edit (identity files, memory, skills are markdown); SQLite for indexes, runs, and timelines.
3. **Every capability is a guarded tool**: the agent runtime cannot touch anything not explicitly allowed (File Guard / Tool Guard enforced in code, not prompt).
4. **Provider-agnostic models**: OpenAI-compatible, Anthropic, and local (Ollama) behind one gateway interface; per-task model picker with an "Auto" router.
5. **Clone the concepts, not the assets**: original name/branding ("Qwake" working title), original UI design, no copied trademarks, copy, or graphics.

**Scope ladder** (each phase ships something usable)

- **MVP (Phases 0–1)**: one Waker, chat console, real tool use on local files, permission guards, session history.
- **Core product (Phases 2–3)**: role templates + identity files, memory with timeline/rollback, skills, automated tasks (schedule + API triggers).
- **Full clone (Phases 4–6)**: WakerFlow engine + canvas, event/webhook triggers, knowledge base RAG, groups, one IM channel.
- **Production (Phase 7)**: packaging, security hardening, updater, docs.

**Explicit non-goals (v1)**: multi-tenant cloud, team sharing roles, mobile app, remote devices fleet
(a single "device" = this machine), marketplace publishing workflow (local skill install only).

---

## 3. Recommended architecture

```
┌────────────────────────────  User's machine  ─────────────────────────────┐
│                                                                            │
│  Browser (or Tauri window)                                                 │
│  ┌─────────────────────────── Web Console (React SPA) ─────────────────┐   │
│  │ Waker Mgmt │ Chat │ WakerFlow canvas │ Auto Tasks │ Task Board │     │   │
│  │ Knowledge Base │ IM │ Settings │ Approvals                          │   │
│  └──────────────▲──────────────────────────────────────────────────────┘   │
│                 │ REST + WebSocket (localhost, token-auth)                 │
│  ┌──────────────┴─────────── Qwake Daemon (Node/TS) ────────────────────┐  │
│  │ API server │ WS hub │ Auth (PAT, console token)                     │  │
│  │ ── Orchestrator ────────────────────────────────────────────────────│  │
│  │  Agent runtime (session workers, streaming, approvals, cost meter)  │  │
│  │  Trigger engine (cron, webhooks, REST API triggers)                 │  │
│  │  Flow engine (sandboxed DSL: worker/parallel/pipeline/askUser/…)    │  │
│  │  Memory service (personal/project, timeline, versions)              │  │
│  │  RAG service (ingest → chunk → embed → cards → cited retrieval)     │  │
│  │ ── Guards ── File Guard │ Tool Guard │ Model policy │ Audit log     │  │
│  │ ── Integrations ── MCP client │ HTTP actions │ IM channel adapter   │  │
│  └──────────────┬──────────────────────────────────────────────────────┘  │
│                 │                                                          │
│  SQLite (qwake.db) + ~/.qwake/ (wakers/, skills/, projects/, kb/, flows/) │
│  Model gateway ──► OpenAI-compatible / Anthropic / Ollama (local)         │
└────────────────────────────────────────────────────────────────────────────┘
```

### Stack decision (recommended, with alternatives)

| Layer | Recommendation | Why | Alternatives |
|---|---|---|---|
| Daemon/runtime | **Node.js 22 + TypeScript** | First-class MCP SDK & AI SDKs; one language across daemon + flow DSL; fast iteration | Python (strong agent ecosystem, but two-language stack), Go/Rust (perf, slower to build) |
| Console UI | **React 18 + Vite + Tailwind + shadcn/ui** | Fast, dense admin-style UI; component ecosystem | Next.js (unneeded — served locally by daemon) |
| Desktop shell (later) | **Tauri 2** wrapping the local console | Small binary, auto-update, tray | Electron (quicker but heavy); Phase 7 — v1 is daemon + browser tab (this matches QoderWake's own delivery model) |
| Storage | **SQLite** (better-sqlite3 + Drizzle ORM) + **sqlite-vec** for vectors | Single-file, embedded, vector search without a server | LanceDB, Postgres+pgvector (if cloud later) |
| Agent loop | Custom thin loop (plan → act → observe → verify) using provider SDKs | Full control over guards, streaming, approvals, memory injection | LangGraph (opinionated; adds abstraction cost) |
| Flow sandbox | `isolated-vm` executing TS-like DSL with injected async host functions | Matches Qoder's script model; no `eval` of raw user code in-process | JSON graph interpreter (safer, less expressive) |
| Realtime | WebSocket (Socket.IO or ws) | Streaming tokens, approval cards, run status | SSE (simpler, one-way) |
| Scheduler | `croner` + persisted next-run in SQLite | Survives restarts, timezone-aware | node-cron (in-memory only — not enough) |
| Ingestion/RAG | `unpdf`/mammoth/xlsx for docs, local or API embeddings, hybrid BM25+vector | Runs locally, cheap | Hosted RAG services |

---

## 4. Data model (SQLite, first cut)

```
wakers          (id, name, avatar, description, role_template_id, status, created_at)
role_templates  (id, name, description, avatar, identity_md, persona_md, bible_md, default_skills json)
projects        (id, name, description, abs_path, scope 'public'|'private', waker_id?)
waker_projects  (waker_id, project_id)
permissions     (waker_id, file_guards json, tool_guards json, builtin_tools json, model_policy json)
sessions        (id, waker_id, project_id, type 'chat'|'automated'|'flow'|'group', title, status, created_at)
messages        (id, session_id, role, content, attachments json, model, tokens_in/out, cost, created_at)
approvals       (id, session_id, tool_call json, status 'pending'|'approved'|'denied', decided_at)
runs            (id, session_id, trigger_type, status, started_at, finished_at, result_summary, error)
automated_tasks (id, waker_id, name, description, project_id, model, enabled, max_runs, deadline)
triggers        (id, task_id, type 'schedule'|'event'|'api', config json, webhook_token, next_run_at, last_run_at)
skills          (id, name, description, source 'market'|'custom', path, installed_at)  + waker_skills join
memory_items    (id, waker_id, project_id?, category, content, source 'auto'|'manual', supersedes_id, created_at)
memory_events   (id, waker_id, project_id?, kind 'add'|'edit'|'delete'|'rollback', payload json, at)   -- timeline
memory_versions (id, waker_id, scope, version, snapshot json, created_at)
flows           (id, name, description, script, canvas_layout json, version, created_at)
flow_runs       (id, flow_id, args json, status, current_node, waiting_approval_id?, result json, logs json, started_at, ended_at)
flow_triggers   (id, flow_id, type, config json)
knowledge_bases (id, name, owner_email?)         kb_bindings (waker_id, kb_id)
kb_sources      (id, kb_id, kind 'file'|'url', path/url, content_hash, imported_at)
kb_cards        (id, source_id, title, definition_md, refs json, links json)
kb_chunks       (id, source_id, card_id?, text, embedding blob)   -- sqlite-vec index
im_channels     (id, kind, config json, status)  im_pairings (id, channel_id, session_key, waker_id, model, project_id, status)
api_tokens      (id, label, hash, scopes, created_at, last_used_at)   -- PATs, shown once
audit_log       (id, waker_id, session_id, tool, args json, verdict 'allow'|'deny'|'confirm', at)
```

On-disk layout (user-editable, git-friendly):

```
~/.qwake/
  qwake.db
  wakers/<id>/identity.md|persona.md|bible.md
  skills/<skill>/SKILL.md (+ resources)
  flows/<id>/flow.ts (script) + canvas.json
  kb/<id>/sources/… 
  backups/            -- memory snapshots, db backups
```

---

## 5. Subsystem designs

### 5.1 Agent runtime (the heart)

- **Session worker** = one execution of a task (chat turn, automated run, or flow `worker()` node).
- **Prompt assembly**: system prompt = identity.md + persona.md + bible.md + relevant memory
  (personal + project) + installed skills' summaries + bound-KB retrieval context + tool manifest +
  permission constraints. Working directory tree summary injected on first use.
- **Loop**: model → tool calls → Guard checks → execute (or emit approval card and park) → observe →
  repeat → final answer + structured result (deliverables list, files touched).
- **Guards (enforced in code)**:
  - *File Guard*: every path resolved (realpath, no symlink escape) against per-Waker allowlist; reads outside allowlist denied; writes additionally require the directory to be write-enabled.
  - *Tool Guard*: allowlist of tool classes (fs, shell, web.fetch, web.search, mcp.<name>.*); shell requires explicit enable + confirmation for non-allowlisted commands.
  - *Model policy*: max cost/run, model whitelist, redacted-regex list scrubbed before egress.
  - *Audit*: every call logged with verdict.
- **Approvals**: high-risk ops pause the run, push an approval card over WS to console (and later IM); resume/deny/timeout policy per Waker.
- **Stop semantics**: match Qoder's honesty — stop kills the loop but **never claims to undo** completed side effects; UI states this.
- **Cost metering & "Auto" router**: token/cost per run; Auto = cheap model for planning/simple, strong model for code-heavy steps (start with a static heuristic router).

### 5.2 Tools & MCP

Built-ins v1: `fs.read/write/patch/list/search`, `shell.exec` (guarded), `web.fetch`, `web.search`,
`kb.search`, `memory.write` (goes through memory service, never raw file writes).
MCP client via `@modelcontextprotocol/sdk`: per-Waker connector registry (manual config, JSON paste,
future: system presets), tool discovery, health check ("Detect → Connected"), per-tool enable flags.

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
checks dependents (automated tasks/flows) first.

### 5.5 Trigger engine (Automated Tasks)

- **Schedule**: croner + persisted `next_run_at`; timezone-aware; max_runs/deadline; "run now".
- **API**: `POST /api/v1/triggers/<token>` with PAT Bearer; `{{field}}` templating into the task
  description; optional `sessionUniqueId` to continue a business session (documented as ≠ idempotency).
- **Event**: webhook receiver for GitHub (issues/PRs/comments) with per-trigger secret + filters
  (repo/branch/type); "test check" button; event activity log.
- Executor reuses the same session-worker as chat (single code path), with a fresh session per run
  unless session reuse requested; failures surface in run history with the standard triage checklist.

### 5.6 WakerFlow engine

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
- Creation flow mirrors Qoder: describe goal → generate canvas + script → edit either → manual run →
  add trigger. Canvas = React Flow graph rendered from an AST/instrumentation trace of the script
  (Phase/Worker/Parallel/AskUser/Action nodes); keep script authoritative.
- Run states: Queued/Running/Waiting-for-Input/Completed/Failed/Terminated; per-node logs
  (`+2m33s` relative timestamps), final return value surfaced explicitly; retry reuses completed
  nodes; versions immutable.

### 5.7 Knowledge base (RAG)

- Ingest files (PDF/Word/Excel/PPT/MD/TXT/CSV/HTML/images-OCR-later) + URLs (dedupe by hash).
- Pipeline: parse → chunk → embed (local or API) → store in sqlite-vec; **card compilation** = LLM
  pass distilling atomic cards (definition, references, links) — cards are the retrieval and citation
  unit; hybrid search (BM25 + vector); answers cite cards, console opens the card in a right panel.
- Scheduled maintenance jobs: recompile, source refresh, knowledge quality check.

### 5.8 Groups & IM (Phase 6)

- Groups: 2–8 Wakers, local Leader receives unaddressed messages, `@member` routing, shared task
  view (progress, assignments, per-member artifacts); members add-only (rebuild to change) — clone
  the same constraint for v1 simplicity.
- IM: one channel to start (Slack or Discord — pick by your preference; Qoder uses DingTalk).
  Bot adapter → pairing requests (verify initiator → assign Waker/model/directory → Allow) → paired
  sessions route inbound messages to the Waker's session worker; replies and approval cards go back
  to the channel.

### 5.9 Console UI (screens)

1. **Waker Management** — cards (name, desc, runtime, online dot, local tag), filters, actions
   (chat / new automated task / share / delete), New Waker wizard (template → identity → device).
2. **Chat** — left rail (Wakers/groups/history), top bar (executor + entry points), message stream
   (execution process, artifacts, approval/question cards), composer (working-dir picker, `+`
   attachments, `@` file refs, model selector incl. Auto).
3. **Waker detail** — left config menu: Home, Projects, Automated tasks, Chat tasks, Workflows,
   Memory, Skills, Knowledge, Connectors, IM, Permissions, Q&A records (if specialist).
4. **WakerFlow** — list page; editor (canvas + script + versions tabs, run button, triggers);
   run history (status, per-phase/node logs, waiting-input prompt, final return).
5. **Automated Tasks** — task cards (More: pause/resume/edit/copy/delete/run-now), detail (trigger
   config, next run, event activity, run history, API trigger docs with copy-URL + body schema).
6. **Task Board** — cross-Waker tasks/to-dos/statuses/artifacts.
7. **Knowledge Base** — sources tree + cards tree, compile button + progress, retrieval QA box,
   bindings, scheduled jobs.
8. **IM** — channel config, pending pairing queue, paired sessions.
9. **Settings** — models & keys (stored in OS keychain), devices, diagnostics, updates, PATs.

---

## 6. API surface (daemon, localhost + token)

```
POST /api/v1/wakers            GET/PATCH/DELETE /api/v1/wakers/:id
GET  /api/v1/wakers/:id/memory | memory items CRUD, timeline, rollback
POST /api/v1/sessions          (type, waker, project) → id
POST /api/v1/sessions/:id/messages        (streaming over WS channel session:<id>)
POST /api/v1/approvals/:id     (approve|deny)
CRUD /api/v1/automated-tasks   + POST …/:id/run  + POST …/:id/pause|resume
POST /api/v1/triggers/api/:token          (PAT; API trigger ingress)
POST /api/v1/webhooks/github/:triggerId   (event trigger ingress, signature-verified)
CRUD /api/v1/flows             + POST /api/v1/flows/:id/run  + runs endpoints
CRUD /api/v1/kb, /api/v1/kb/:id/sources, POST …/compile, POST …/retrieve
WS   /ws  → events: token.stream, run.status, approval.requested, …
```

---

## 7. Delivery plan

| Phase | Deliverable | Key work | Est. (1 dev) |
|---|---|---|---|
| **0. Foundations** | Repo, CI, daemon skeleton serving console | Monorepo (pnpm), TS strict, Drizzle+SQLite, config, logging, REST+WS bootstrap, console shell with nav | 1 wk |
| **1. MVP agent** | Chat with a Waker doing real local work | Model gateway (OpenAI-compat/Anthropic/Ollama), agent loop, built-in fs/shell/web tools, File+Tool Guards, audit log, session persistence, streaming chat UI, working-dir picker, stop button | 3–4 wks |
| **2. Identity, memory, skills** | Wakers feel like employees | Role templates + IDENTITY/PERSONA/BIBLE files, New Waker wizard, memory service (auto-summary, timeline, versions, editor), skill loader + local market + upload, projects (public/private binding) | 2–3 wks |
| **3. Automation** | 24/7 operation | Schedule triggers (croner + persistence, run-now), API triggers + PATs + `{{field}}` templating, run history UI, GitHub webhook events, task board | 2–3 wks |
| **4. WakerFlow** | Multi-stage orchestration | DSL + isolated-vm host functions, flow runner w/ statuses, askUser approval cards, canvas gen + React Flow editor, versions, flow triggers | 3–4 wks |
| **5. Knowledge base** | Cited answers from your docs | Ingestion pipeline, chunk+embed+sqlite-vec, card compilation, retrieval tool + citations panel, KB bindings, scheduled maintenance | 2–3 wks |
| **6. Groups + IM** | Team of employees | Group sessions (Leader routing, @mentions, artifacts), one IM channel with pairing flow | 2–4 wks |
| **7. Production** | Shippable v1 | Tauri shell + tray + updater, keychain secrets, backup/restore, telemetry (opt-in), docs, installer scripts (curl/npx), security review | 2 wks |

**Total: ~17–24 weeks solo** (compress ~40% with AI-assisted development; MVP demo-able in week 5).
Suggested order note: Phase 5 can swap with 4 if RAG matters more to you than flows; Phase 6 can be
cut entirely for v1 without breaking the story.

---

## 8. Testing & quality

- **Unit**: guards (path-escape, symlink tricks), scheduler math (timezones, DST), `{{field}}`
  templating, memory merge/dedupe.
- **Integration**: agent loop against a mock model with scripted tool calls; trigger ingress
  (signature verification, PAT auth); flow engine (parallel fan-out, askUser park/resume).
- **E2E**: Playwright over the console — create Waker → chat → approve a guarded write → verify file.
- **Golden flows**: recorded "daily report" and "issue triage → fix → ask user" runs replayed in CI
  with fake models to catch regressions.
- **Chaos-lite**: kill daemon mid-run → restart → runs resume or fail cleanly with audit intact.

## 9. Security checklist (build-in, not bolt-on)

- Console bound to 127.0.0.1 with per-install bearer token; PATs hashed + shown once; scopes.
- Secrets in OS keychain (fallback: encrypted file with machine key); secret scanner on memory,
  logs, and KB; regex redaction before any egress to model providers.
- Webhooks signature-verified; MCP connectors run with least privileges; tool allowlists default-deny.
- Audit log immutable (append-only table); "dangerous action" confirmations (delete Waker, unbind KB)
  check dependents, mirroring Qoder's UX.
- Sandboxing: isolated-vm for flow scripts; shell tool behind explicit opt-in + command confirmations.

## 10. Key open decisions (defaults chosen, flag if you disagree)

1. **Stack**: Node/TS daemon + React console + SQLite — *default yes*; alternative Python daemon.
2. **Desktop shell**: browser tab first, Tauri wrap in Phase 7 — alternative Electron from day 1.
3. **First IM channel**: Slack (default) vs Discord vs Telegram.
4. **Embeddings**: local (bge/e5 via Ollama) vs API (OpenAI/voyage) — default: API with local fallback.
5. **Name/branding**: "Qwake" placeholder — pick before any public release; keep all copy original.

## 11. Risks & mitigations

| Risk | Mitigation |
|---|---|
| Agent reliability for unattended runs | Conservative guards, approval gates before external writes, run-now dry tests, explicit success criteria in task descriptions (Qoder's own guidance) |
| Cost runaway on 24/7 automation | Per-run cost caps, model policy per Waker, spend dashboard, Auto-router |
| Flow DSL complexity | Script stays authoritative; canvas is a view; version everything; retry reuses completed nodes |
| Memory poisoning / stale rules | Timeline + rollback, manual review prompts, secret scanner, scope discipline (personal vs project) |
| Scope creep toward full Qoder parity | Scope ladder above; every phase ships; non-goals enforced until v1 done |
| Trademark/IP issues | Original name, copy, and visuals; clone concepts and interaction patterns only |

---

*Research sources: qoder.com/en/qoderwake (product page), qoder.com/blog/qoderwake (five-layer architecture), docs.qoder.com — overview, manage-wakers, conversation-tasks, wakerflow, automated-tasks, memory, skills-and-integrations, knowledge-base (fetched 2026-08-15).*
