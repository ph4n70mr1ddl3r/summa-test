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
>
> **Edge-case closure (v2.12)**: fourth sweep — the §13.1 residue is designed, not deferred:
> N-of-M quorum asks make "two approvals" expressible (§4.1, §8.10, §7) · trigger idempotency keys
> + staged external writes on an `external_writes` ledger, reaper grace, retry-safe keys (§8.5,
> §8.2, §6.2) · erasure = pseudonymized ledgers + legal holds + node-region residency (§4.5, §3,
> §7) · db-only reconstructibility via topology audit manifests + scheduled exports (§4.5) ·
> reservation metering closes check-then-spend races (§6.2, §7) · workspace claims become
> epoch-fenced leases (§3) · restore gains a reconciliation runbook — audit replay + node
> re-registration (§11) · rules gate external writes like scopes (§8.1) · ask storms collapse +
> shed by rate limit (§8.10) · separation-of-duties knob per domain (§4.3) · initiatives grow
> `depends_on` (§5.1) · control-plane time authority + per-human calendars (§3, §7, §8.10) ·
> proposal amendment + publish-transaction contradiction re-checks (§4.3, §4.4) · runtime
> precedence pinned (§4.2) · taint propagates through memory, cleared only by review (§8.3) ·
> playbook depth cap + cycle detection (§8.6) · git integrity: signed refs + non-FF refusal
> (§4.5, §10) · PAT expiry/rotation/revocation (§9, §10) · breaker trips by criticality class
> (§6.2) · offboarding transfers or withdraws authored proposals (§5) · embedding switches cut
> over via dual index (§8.7) · glossary alias collisions resolve deterministically (§4.2) · §13.1
> restated as accepted boundaries + deferred parameters; new principle: degrade to an ask, never
> to silence (§2).
>
> **Edge-case closure (v2.13)**: fifth sweep — audit residue closed inline: viewer deputies
> refused at write, like agent and self deputies — a read-only member is never a standing answer
> hop (§7, §8.10) · quorum N validated against the eligible approver pool at proposal time, and
> a pool that shrinks below N denies at expiry with the shortfall named (§4.4, §8.10) · unset
> calendars get a defined fallback — control-plane zone, 09:00–17:00 weekdays (§8.10) ·
> initiative sponsors pinned human (§5.1) · offboarding terminates sessions and revokes PATs —
> deactivation is credential-death, not a disabled login flag (§5).
>
> **Edge-case closure (v2.14)**: sixth sweep — schema-text seams closed inline: human
> deactivation is representable (`deactivated_at`, §7) — the last-admin guard and chain-walk
> skips now have a data basis · goals gain optional domain scoping: sensitive objectives inherit
> compartment access; org-scoped items (goals, glossary) route proposals to the admin queue
> (§4.2, §4.3, §7) · injected layers pass the same access check as retrieval — an unreadable
> workspace degrades to an ask, never a silently empty prompt (§4.2) · quorum addressing pinned:
> `to` = the rule's domain owner, the pool = that owner + active admins, evaluated at respond
> time (§7, §8.10) · domain-kind legal holds freeze git history-rewrite remediation and db-only
> exports (§4.5, §7) · stranded prepared writes get a scheduled reconciliation pass (§8.2) ·
> initiative sponsor carried as pinned-human in the schema (§7) · admin lockout gets a
> server-local, audited reset flow (§10).
>
> **Edge-case closure (v2.15)**: seventh sweep — ownership and lifecycle seams closed inline:
> goal window-end semantics pinned — expired goals leave the slice, the sponsor ask carries the
> outcome, terminal goal transitions re-open it (§4.2, §5.1) · initiative transitions are owned —
> sponsor activation (ask-routed, deny at expiry), lead/sponsor pause-close, lifecycle endpoints
> added (§5.1, §9) · viewers are never task assignees, like ask targets (§5, §7) · erasure sweeps
> DNA provenance, not just ledgers; the storage tree gains domain-scoped goals and the db-only
> split (§4.5) · the last-admin guard is transactional — racing offboards cannot both land (§5) ·
> offboarding closes asks-from audit-only and gains its endpoint (§5, §9) · suspension halts the
> ephemeral subtree with fold-back, never mid-commit (§6.3) · stage-less sends are send-once — an
> ambiguous timeout is an ask, not a resend (§8.2) · ordinary expiring rules join delegations in
> `lapsed` semantics (§7) · the single control plane is named an accepted boundary (§13.1).
>
> **Edge-case closure (v2.16)**: eighth sweep — authority seams closed inline: the last-admin
> guard covers demotion, not just deactivation — an RBAC edit cannot leave the org headless (§5)
> · initiative sponsors and leads are ask-eligible at write — viewer and non-active members
> refused, like ask targets and assignees (§5.1, §7) · ask targets in any non-active state
> (requested, retiring) reassign up the chain (§8.10) · deputy accepts are audit-only toward
> multi-approval quorums; a delegated quorum rule addresses the delegate as primary recipient
> and pool member (§8.10) · topology ops declare their resulting domains — owner/access/store/sod/residency —
> with most-restrictive access on merge, hold-refused store migrations, and a post-op
> contradiction re-check inside the lock; dissolution is merge-away-then-archive, never bare
> delete (§4.4, §7, §9) · closing an initiative unbinds its workspaces — the goal slice
> re-derives at once (§5.1) · `depends_on` is acyclic — cycles refused at write (§5.1) · a
> settle that overshoots its reservation settles in full, surfaces, and gates further reserves
> until acknowledged (§6.2).
>
> **Edge-case closure (v2.17)**: ninth sweep — demotion and authority-flip seams closed inline:
> RBAC demotion is a walked transition, not a label flip — an edit that reduces a role runs the
> §5 dependency walk scoped to the new role's carrying capacity, inside the last-admin guard's
> transaction (§5) · goal owners are ask-eligible at write — viewer humans refused, agent owners
> keep the §4.2 admin-routing fallback (§7) · glossary proposals are expressible — `kind
> 'glossary'` joins the enum, closing the §4.2 admin-queue routing onto the schema (§7) ·
> item-level DNA CRUD is the publish path, not a side door — same domain write lock,
> contradiction re-check, sod routing, secrets scan (§9) · the secrets scanner covers ingested
> direct edits, not just proposals and memory (§4.5, §10) · the persistent-hire spawn gate
> falls back to admin for domainless primary workspaces and to the primary domain for
> multi-domain ones (§6.2) · budget-cap windows match worker class — per-worker lifetime for
> ephemeral, periodic window for persistent (§6.2) · initial workspace–node binding is
> capability-checked like rebind, and a residency constraint no node satisfies is surfaced
> through the same starvation ask, not a silent queue (§3).
>
> **Edge-case closure (v2.18)**: tenth sweep — representation and drain seams closed inline:
> the workspace–domain binding — the read path's applicability set, the spawn gate's hop, and
> the ask router's escalation hop all key on it — is schema-explicit: ordered `domain_ids`,
> first entry primary, unbinding the primary promotes the next, an empty list is domainless
> with the defined fallbacks, topology ops remap (§7) · archive refuses live workspace
> bindings; the merge step remaps them, ids stable (§4.4, §9) · cross-domain item moves are
> topology ops only — edit proposals and item-level CRUD refuse a `domain_id` change (§4.4) ·
> proposal amendments re-route with their payload: the reviewing queue matches the amended
> scope (§4.3) · the goal `inject` flag composes with scope — a domain-scoped 'always'
> injects wherever its domain is readable (§4.2, §7) · intra-domain glossary duplicates are
> refused at write; the resolution order stays cross-domain (§4.2) · initiative close drains:
> in-flight runs complete onto the closed slice, new work is refused, urgent stops go through
> suspend/retire (§5.1) · resume re-arms triggers and launches new runs but never resurrects
> a halted one — fold-back plus §8.2 reconciliation, no half-replayed side effect (§6.3) ·
> the TTL reaper returns open task assignments and re-routes in-flight asks, not just memory
> (§6.3) · the personal-assistant 1:1 is enforced by the policy engine, not implied (§6.4).
>
> **Edge-case closure (v2.19)**: eleventh sweep — authority-surface seams closed inline: the viewer
> role is a total no-write surface — never a target, assignee, or originator: propose, amend, ask,
> task, initiative, and spawn writes are all refused at write (§5) · demotion carries authored
> proposals with the authority — transferred for shed domains, withdrawn when the role can no
> longer propose, mirroring offboarding (§5) · the agent-target ask chain is pinned to the lineage
> owner, then the human chain — an ask to an agent never lacks a human next hop (§8.10) · pause
> retains its workspace bindings frozen — linked goals keep injecting while runs and spawns refuse;
> close still unbinds; a denied activation leaves the initiative `proposed` and inert (§5.1, §7) ·
> ephemeral-origin DNA proposals are refused at write — learning folds back, the spawner proposes
> (§7) · retire is a halt like suspend — fold-back + reconciliation, never mid-commit — and
> suspend/retire/resume authority is named: the owner human, an admin, or a bound-initiative
> sponsor, reconciling §5.1's sponsor stop with §6.3's emergency stop (§6.3, §9) · playbook
> criticality joins the schema, and a firing's class is the stricter of trigger and playbook tags
> (§7, §6.2) · ingest quarantine routes to the domain's owner — never an unowned inbox (§4.5) ·
> the domainless workspace has a defined read-path layer, and org-wide glossary duplicates are
> refused like intra-domain ones (§4.2) · board-task assignees must be active at write — suspend
> freezes assignments, retire/offboard walks return them (§7).

> **Edge-case closure (v2.20)**: twelfth sweep — lifecycle seams at the surfaces' edges closed
> inline: open proposals travel with their domain — merge/split/rename remap in-review proposals
> to the resulting queues inside the audited event with `review_by` clocks running, and archive
> counts them among the holdings it refuses (§4.4, §9) · coworker lifecycle acts are credential
> fences — PATs and sessions authenticate only while `active`, re-validated at every use; retire
> revokes outright, the §5 credential-death (§6.3, §10) · pause freezes execution, not
> deliberation — tasks may be filed and edited on the frozen slice as planning, runs and spawns
> still refuse (§5.1) · proposal revisions re-route by scope but never change `kind` — a queue
> is never handed a payload shape it wasn't routed to review (§4.3) · `closed` is terminal —
> revival is a new initiative referencing the old decision, never a rewrite (§5.1) · item-level
> DNA CRUD is create/update/retire, never delete — erasure stays the only shredding path
> (§9, §4.5).

> **Edge-case closure (v2.21)**: thirteenth sweep — corpus-state and post-hoc seams closed
> inline: glossary entries gain the status the "live entry" duplicate check always presupposed —
> retire is their item-CRUD surface, freeing terms and aliases as resolvable history (§7, §9) ·
> decisions are pinned immutable: create-only at every surface, reversal or amendment is a new
> record citing the old (§7, §9) · retrieval splits from citation — search and injection serve
> active items only, retired ones resolve read-only, drafts stage to their owner alone, and
> decisions, lifecycle-free by design, are always live (§4.2) · goal windows are two-sided —
> admission at `effective_from`, exit at `effective_to` (§4.2, §7) · the settle-overrun reserve
> gate gains its admin ack endpoint (§9, §6.2) · template retirement counts pending spawn
> requests as live pins, and an upgrade whose scope intersection comes back empty refuses to
> land (§6.5) · erasure sweeps memory attribution and reports free-text mentions for admin
> judgment — never silently kept, never silently rewritten (§4.5) · webhook redelivery after
> downtime dedupes on a defaulted 7-day window — an outage never converts one event into two
> side effects (§8.5) · a null `budget_cap` is defined — worker-uncapped, org ceilings still
> bind (§7) · digest grouping gains its ungrouped tail, so every ask has a place (§8.10).

> **Edge-case closure (v2.22)**: fourteenth sweep — access-model and residual-semantics seams
> closed inline: domain membership is defined — public/domain/named reader sets derive from
> workspace participation, the owner always reads what they own, and active admins hold audited
> governance reads everywhere the escalation, sod, and custody paths already hand them content
> (§4.4, §7) · a `met` goal fires the §5.1 sponsor ask like every other terminal transition,
> choices tracking the outcome — an initiative never executes on toward a goal that has already
> ended (§5.1) · sod's "second owner" was unrepresentable — publish routes to an active admin,
> the single-owner schema's one alternative publisher, and the single-admin collapse is that
> rule's degenerate case (§4.3) · a deadline passed with no open work still asks — a bulk-tier
> close-out ask to the sponsor, so finished initiatives never linger on their bindings (§5.1) ·
> a kind-`domain` hold refuses dissolution, archive, and store migration — rename and merge-into
> stay open — and holds gain their management endpoints (§4.4, §4.5, §9) · a tainted run's ask
> accepts are audit-only with an untainted successor — taint never becomes approval authority
> (§8.10, §13) · draft staging is pinned to the schemas that carry it — cards and glossary stage
> as drafts, rules and goals through future effective windows, decisions never (§4.2) · trigger
> and playbook criticality default `standard` (§7) · a retired Coworker's personal memory
> archives inert with it, never transferred (§6.3) · paths under a db-only domain's tree
> quarantine on ingest — one canon, not two (§4.5).

> **Edge-case closure (v2.23)**: fifteenth sweep — residual-surface seams closed inline: the
> `named` access policy's reader list gains the schema home its §5 walk always presupposed —
> `named_readers` member ids, ignored under the other policies, derived from live state so dead
> entries read nothing and rehire re-admits no one (§4.4, §7) · spawn requests gate on template
> status: `active` only — draft is authoring state, retired is history, refused at request time,
> the other half of §6.5's pin-drain (§6.2) · the sponsor's direction asks — stalled-work,
> close-out, goal-window, terminal-goal — escalate on expiry and stay pending in every digest,
> never a silent no; the activation ask's deny stays the deliberate exception (§5.1, §8.10) · an
> admin holds initiative pause/resume as emergency backstop — the §6.3 authority pattern applied
> to the initiative itself (§5.1) · card status defaults `active` like the glossary's — an
> owner's direct create is the publish path, draft an explicit stage (§7).

> **Edge-case closure (v2.24)**: sixteenth sweep — state-machine and gate-completion seams closed
> inline: the retire walk settles the retiree's asks — asks from it, pending spawn requests
> included, close with an audit note and drain their template pins, while asks to it ride §8.10's
> non-active reassignment; a terminal act leaves no waiters (§6.3, §6.5) · merge's undeclared
> attributes persist from the surviving domain, and the `named` reader list keeps the survivor's
> unless the op declares the union — never a silent widening; split's inherit-by-default hands
> results the parent's list (§4.4, §7) · a standalone `store` flip through domain update runs
> the same audited migration and hold refusal the merge path runs (§4.4, §4.5) · a residency
> edit re-validates bound workspaces' placements — rebind or starvation ask, never silently
> grandfathered (§4.4, §3) · the spawn gate is class-matched as well as status-matched (§6.2) ·
> a denied spawn request archives without activating — the status enum's missing denial
> transition (§7) · only `active` initiatives launch spawns, the twin of the paused-slice
> refusal (§5.1) · an exhausted assignment ask returns the task to the board pool (§8.10) · a
> TTL lapsing under suspension halts-then-reaps (§6.2, §6.3).

> **Edge-case closure (v2.25)**: seventeenth sweep — lifecycle-terminal and addressing seams
> closed inline: item-level retire on a rule is window truncation — `effective_to` pinned to
> now, the row lapsing at that boundary; the rules enum carries no `retired` because lapse is a
> rule's terminal (§9, §7) · frozen history is frozen at every surface — updates to
> superseded/lapsed rules, terminal goals, and retired cards/glossary entries are refused;
> correction and revival are new items citing or superseding the old, a predecessor stays
> superseded when its superseder lapses, and a draft discards by retiring (§9, §7) ·
> supersession is intra-domain — a cross-domain `supersedes_id` refused at propose and write;
> topology ops move chains whole (§4.4, §7) · publish re-validation covers the edit target's
> lifecycle — an edit proposal whose item retired mid-review refuses back to review (§4.3) ·
> the admin hop is a broadcast — every path routing to "an admin" addresses all active admins
> at once, first valid response wins, single-admin the degenerate case, and exhaustion is an
> unanswered broadcast (§8.10, §4.3, §6.2) · the sponsor pin is a write guard — agent sponsors
> refused at write (§5.1, §7) · the offboard/demotion goal walk clamps to active goals — a
> terminal goal's owner reference is pinned history, severable only by erasure (§5, §7) ·
> domain names unique among non-archived, role templates keyed unique on (class, name, version)
> (§7) · the §4.5 ingest sanity runs at propose, amend, and item write alike — one validation,
> every door (§9) · new board tasks join runs and spawns in the closed-slice refusal, while
> `proposed` and `paused` keep task-filing open as planning (§5.1).

> **Edge-case closure (v2.26)**: eighteenth sweep — single-writer, catalog, and
> attention-lifecycle seams closed inline: external git ingest joins the domain write lock —
> one writer door per domain, whichever side the write comes from (§4.5, §4.4) · a split's
> declared item mapping respects chain integrity — a supersession chain maps whole to one
> result, refused at declare (§4.4) · proposal amendment and publish serialize on the lock —
> racing amendments land as sequential revisions, publish binds the pre-lock latest (§4.3, §7)
> · template version selection is explicit — spawn requests name the exact catalog row,
> publication files the owner-upgrade asks, a denied upgrade leaves the pin standing on a
> still-active version (§6.5, §7) · a deadline-less initiative gets a stall clock anyway —
> the linked goal's window, else a sponsor staleness line (§5.1, §7) · node capabilities
> re-advertise on heartbeat — drift surfaces rebind-or-starvation, not per-run failures
> (§3, §7) · `decided_by` is cited provenance, not authority (§7) · asks gain originator
> withdrawal — resolving per the expiry behavior, the lifecycle walks'
> close-with-audit-note its system-applied form (§8.10, §7, §9).

> **Edge-case closure (v2.27)**: nineteenth sweep — config-surface, delegation-authority, and
> broadcast seams closed inline: the per-domain review SLA gets its schema home —
> `dna_domains.review_sla_days` drives `review_by`, topology results inherit it (§4.3, §7) ·
> governance policy/quota writes and node updates join the API surface, and a region edit
> re-validates residency-constrained placements — the node-side twin of §4.4's rule (§9, §3) ·
> a delegation naming an agent is the reviewed grant the agent-deputy refusal reserves this
> mechanism for — the agent's accept binds the asks its rule routes, is audit-only toward
> N>1, and resolves its recipient at ask creation (§8.10) · the org-stall broadcast is an
> alert, not an ask — viewers receive it read-only; the never-a-target guard governs waited-on
> answers (§8.10, §5) · owner-staged drafts ride the offboard/demotion walks — transferred with
> the domain and surfaced to the successor, never orphaned invisible (§5, §4.2) · the
> initiative stall clock's state coverage is pinned — `proposed` and `active` run it, pause
> suspends it, close stops it; inert is not invisible (§5.1).

> **Edge-case closure (v2.28)**: twentieth sweep — holder-mortality, lock-order, and linkage
> seams closed inline: ephemeral workers are refused at write the posts that outlive them —
> initiative lead, goal owner, named delegation agent — the agent-sponsor pin's twin: no
> mid-life walk is asked to re-point what a dying-by-schedule member should never have held,
> while the reap walk's task-and-ask returns stay the drain for what an ephemeral may
> legitimately hold (§5.1, §7, §8.10) · ephemeral-origin initiatives join ephemeral-origin DNA
> proposals in folding back to the spawner — a human or persistent Coworker opens the
> directive (§5.1) · topology ops touching several domains acquire every affected write lock
> up front, in domain-id order — overlapping merges serialize deadlock-free, "queue behind
> each other" gains its mechanism (§4.4) · a spawn-approval ask that expires is the denial's
> twin: `requested`→`archived`, template pin drained, the expiry the record (§6.2, §7) · the
> admin queue's org-scoped proposals derive `review_by` from the global default — no domain
> row governs them (§4.3) · the sponsor's terminal-goal answer moves the linkage with it:
> extend re-windows the same row, re-base/re-target swap `goal_ref` atomically with the
> answer, the goal slice re-deriving at once (§5.1).

> **Edge-case closure (v2.29)**: twenty-first sweep — split-completion, write-authority, and
> schedule seams closed inline: a split's declared mapping is total — every item, binding, and
> open proposal names its result or the op refuses at declare — and the emptied parent
> archives inside the same audited event, division pinned as dissolve-by-split; a held
> domain's split queues behind the hold's release with archive and merge-away (§4.4) ·
> domain-row writes get their authority home — create/archive and structural attributes
> (`store`, `sod`, residency) admin, compartmental attributes (access, `named_readers`,
> `review_sla_days`) owner — and template authorship joins them as an audited admin surface,
> adoption staying with the §6.5 owner asks (§9, §7, §6.5) · the reap walk settles asks in
> both directions — to-it re-routed up the chain, from-it closed with an audit note, the
> retire-walk settlement's ephemeral twin (§6.3) · human PATs and sessions authorize against
> live RBAC at every use — demotion narrows a standing credential at its next call, the
> credential-side twin of §6.3's status fence (§10) · residency's at-rest half is an audited
> admin attestation at set or tighten — the control plane's own hosting is declared, never
> silently assumed (§3) · a repeatedly failing recall-parity gate surfaces an admin ask with
> the deltas — never an eternal silent shadow index (§8.7) · quorum N is bounded below
> (`requires_approvals` ≥ 1) at every write door, the one-validation rule's newest bound
> (§8.10, §9) · a response racing an originator's withdrawal is audit-only, terminal like
> expiry (§8.10) · schedules elapsing under an initiative pause coalesce per §8.5 and play on
> resume, and closing re-points or disables the triggers and playbook schedules that launch
> under the initiative — pause defers timetables, close answers the door (§5.1).

> **Edge-case closure (v2.30)**: twenty-second sweep — revocation-mortality, goal-linkage,
> and close-drain seams closed inline: revoking a node with live workspace bindings surfaces
> each bound workspace's rebind ask at revocation time — a deliberate act is a visible
> configuration error, never a 24h silent queue a hopeless topology quietly endures — and
> in-flight runs on the node halt the way suspension halts them (fold-back + §8.2
> reconciliation, never mid-commit, terminal with no resurrection), the node's claims dying
> with the row: revocation is the fenced lease's terminal case (§3, §9) · the goal-end
> direction ask fires in every non-closed initiative state — under pause its escalation is
> suspended with the stall clock and plays on resume, under `proposed` it joins the activation
> ask on the sponsor's desk — activation itself re-validates the `goal_ref` it inherits: an
> accept against a dead goal is audit-only, the re-point successor ask carrying the decision;
> a new initiative's `goal_ref` names a live goal at write, and a re-point answer's target
> rides the same liveness check — an initiative is never born pointed at history, and the
> only way it comes to address a terminal row is the goal dying under it (§5.1, §8.10, §7) ·
> closing archives the initiative's pending spawn requests with their template pins drained —
> the retire walk's settlement applied at close — and a spawn-approval ask names the
> initiative among its respond-time assumptions: an accept racing a pause or close is
> audit-only, the request archiving, never a worker published into a slice that refuses
> launches (§5.1, §6.2, §8.10).

> **Edge-case closure (v2.31)**: twenty-third sweep — attention-remap, holder-racing, and
> workspace-mortality seams closed inline: pending asks travel with their topology — an ask
> whose `to` was derived from a domain's owner (the spawn-approval gate, a quorum ask's
> primary recipient) is re-keyed to the resulting owner inside the audited event, ask ids
> stable and deadlines untouched, the open-proposals rule extended to the attention surface,
> while hops and pools already evaluate against live state (§4.4, §6.2, §8.10) · upgrade
> asks settle with the Coworker they name — the retire walk closes an in-flight owner-upgrade
> ask unresolved with an audit note, a racing accept is audit-only with no successor, and
> suspension strands nothing: the rebase lands and resume re-arms (§6.3, §6.5, §8.10) · a
> template's class is immutable across its versions — a class-flipping version refused at
> publish; a role that changed class is a new template, the retire-and-respawn path (§6.5,
> §7) · workspace archival is a walked transition, never a bare delete — initiative bindings
> drop with the goal slice re-deriving, domain reader sets re-derive, the node claim dies
> with the row, new spawn bindings are refused, and workspace-keyed asks degrade to the
> domainless fallback (§7, §5.1, §3, §6.2, §8.10) · an initiative pause is a launch gate,
> not a mid-run kill — runs in flight complete onto the paused slice exactly as close's
> drain completes them (§5.1) · domain-owner re-pointing is an admin write, the §5 walks its
> system-applied form (§9, §7) · the time authority is monotonic in effect: a backward clock
> step never un-expires an ask, window, lease, or TTL, nor reverses a terminal transition
> (§3).

> **Edge-case closure (v2.32)**: twenty-fourth sweep — runtime-drain, re-key, and
> windowed-supersession seams closed inline: workspace archival drains the runtime that
> launches into it, not just the bindings — in-flight runs complete onto the archived slice as
> history, queued-but-unlaunched runs close with an audit note, workspace-bound triggers and
> playbook schedules re-point or disable, and project memory archives inert with it (§7, §5.1,
> §8.3) · pending asks re-key with every post they address, at every door the re-pointing has —
> sponsor-addressed asks follow the §5/§6.3 walks' sponsor re-pointing, and owner-derived asks
> re-key at the domain edit and the walk, not just the topology op (§4.4, §5, §6.2, §6.3) ·
> supersession takes effect at the superseder's window opening — a future-windowed successor
> is a scheduled replacement, never a normative gap (§4.2, §7) · an ephemeral worker is
> refused the persistent-hire request at write, its recommendation folding back to the
> spawner (§6.1, §6.2) · re-owning is scope-narrowing — transferred Coworkers re-derive
> against the new owner's ceiling, an empty intersection retiring (§5, §6.3, §6.5) · an
> approval gate may address its own originator — the owner hiring into their own domain — the
> ask the audit record (§6.2); and an ask deadline before its creation is refused at write
> (§8.10).

> **Edge-case closure (v2.33)**: twenty-fifth sweep — spawn-approval, dependency-liveness,
> and reference-mortality seams closed inline: workspace archival settles the pending spawn
> requests that bind to it — archived with their template pins drained, the initiative-close
> settlement on the workspace axis — and a spawn approval names its workspace among its
> respond-time assumptions: still binding-accepting, still readable for the member it would
> publish, an accept racing archival audit-only, the request archiving, never a worker
> published onto a row that refuses bindings (§7, §6.2, §8.10) · `depends_on` names live
> rows — an edge naming a closed initiative is refused at write, the `goal_ref` liveness rule
> on the graph axis, the only way an edge comes to address a terminal row being the upstream
> closing under it, exactly the case the close-ask exists for (§5.1, §7) · an activation
> accept re-validates the initiative's own state — an accept landing after a close that beat
> it is audit-only, terminal beats activation, the spawn-approval's pause/close rule at the
> activation door (§5.1, §8.10) · retiring a playbook version refuses while live references
> hold it — triggers and schedules re-point or disable first, the §8.4 uninstall check
> applied to playbooks, runs pinning the version they launched from, and SOP pointer cards
> riding the §4.4 freshness flags rather than dangling silently (§8.6) · workspace archival
> gets its endpoint and authority — admin, running the §7 walk (§9).

> **Edge-case closure (v2.34)**: twenty-sixth sweep — reader-set-liveness, spawn-claim, and
> chain-linearity seams closed inline: every reader-set input evaluates against live state —
> a `participants` entry or Coworker binding of a deactivated human or retired Coworker
> contributes nothing to `domain`-access reads, and the §5/§6.3 walks scrub the lists
> (participants removal, retiree bindings, group memberships) the way they scrub
> `named_readers` (§4.4, §5, §6.3, §7) · spawn-request claims are lifecycle-pinned —
> count-cap claims and budget reserves attach at request creation inside the spawn
> transaction, transfer at activation, and release at every terminal a pending request has
> (denial, approval expiry, close-/archive-time settlement), so an approval never publishes
> into an exhausted cap and cap space never leaks on a dead request; a workspaceless hire
> routes its approval to the admin gate like a domainless primary; and the spend breaker
> un-trips only through its trip ask's resolution — never by time (§6.2) · supersession
> chains are linear, not forks: a second live `supersedes_id` edge onto an already-superseded
> predecessor is refused at propose, amend, and item write — displacing a superseded rule
> means naming the chain's live head, so a predecessor's displacer is always exactly one
> rule (§4.4, §7, §9) · winding the company down is a deployment shutdown, never an
> offboarding — the last-admin guard's refusal is the org model staying honest about its
> human anchor, not a missing exit (§5).

> **Edge-case closure (v2.35)**: twenty-seventh sweep — requester-liveness, holdings-scope,
> and owner-derivation seams closed inline: a spawn approval names its requester's own state
> among its respond-time assumptions — an accept racing the requester's suspension is
> audit-only, the request archiving with its template pin drained and its cap claims
> released, never a worker published under a halted subtree; retirement and offboarding
> settle their requests in the walk, and the gate closes the non-terminal case the walks
> leave standing (§8.10, §6.2, §6.3) · archive's holdings refusal is scoped to the live set
> — active items, owner-staged drafts, live bindings, open proposals — while terminal
> history never blocks: it stays with the archived row as the read-only record §7 always
> named, a history-only domain archives directly, and merge moves the whole corpus with ids
> stable, so §9's refusal and §7's archived row stop pulling against each other (§4.4, §7,
> §9) · hire ownership gains its derivation: the persistent hire's `owner_human_id` is the
> gate's accepting human at activation — a re-keyed gate landing on the re-keyed addressee —
> and an ephemeral's is the first human up the `spawned_by` line, the §5 invariant derived
> at spawn rather than merely checked (§6.2, §7) · a group's Leader post joins the walks —
> re-pointed on departure or retirement, an unnamed successor degrading routing to an admin
> ask — so no execution surface addresses a dead identity (§5, §6.3).

> **Edge-case closure (v2.36)**: twenty-eighth sweep — assumption-settlement, gate-rekey, and
> leader-guard seams closed inline: the event that terminally breaks a named ask assumption
> settles the ask at the event — a quorum ask whose rule went terminal mid-wait resolves per
> its expiry behavior with the successor machinery carrying the decision, and a domain's
> archive closes its owner-addressed pending asks with an audit note, so no ask lingers
> rendering answerable against a dead premise (§8.10, §4.4, §9) · the spawn gate's
> creation-time hop never outlives the workspace binding it was read from — an admin edit of
> a bound workspace's `domain_ids` re-keys its pending spawn approval to the gate the edited
> binding derives, ids stable and deadlines untouched, the re-key's fourth door (§6.2, §7,
> §4.4) · the group-Leader post gains its write guards — viewer and non-active members
> refused at set, an ephemeral refused by the mortality pin, and the demotion walk re-pointing
> a Leader the new role can no longer answer for (§5, §6.3) · and governance cap edits are
> pinned claim-scoped, never retroactive — live claims run out, new claims refuse, and a
> ceiling tightened below live spend trips the breaker loudly rather than contradicting its
> own ledger (§6.2, §9).

> **Edge-case closure (v2.37)**: twenty-ninth sweep — spend-halt, record-mortality, and
> answer-authority seams closed inline: the breaker's halt is a launch gate at every door —
> runs in flight at the trip complete and settle onto the ledger, a settle overshoot tripping
> the overrun gate as designed, and a spawn-approval accept landing under an active halt is
> audit-only, the request archiving with pin and claims released, the pause-race rule at the
> money door — while the critical floor carries critical-tagged firings only, never a hire
> (§6.2, §8.10) · decisions never block archive — lifecycle-free records are history at
> birth, they ride the archived row as citation history, leave search with their domain's
> corpus, and merge-away moves them with it, ids stable (§4.2, §4.4, §7, §9) · an upgrade
> accept re-validates its target version — a retirement that beat the accept is audit-only,
> the pin standing, the next publication re-asking (§6.5, §7, §8.10) · a response from
> outside the ask's eligible set — addressee, deputy, quorum pool — is refused at the door,
> the attempt audited (§7, §8.10) · the review queue belongs to the domain, not the owner's
> inbox — owner re-pointing at every door re-keys its rendering with `review_by` clocks
> untouched (§4.3) · the storm-shed aggregate admin ask closes on rate recovery or
> acknowledgment, the count preserved in audit (§8.10) · and node capabilities are
> heartbeat-owned — the console node surface edits region and metadata, never the
> advertisement (§3, §7, §9).

> **Edge-case closure (v2.38)**: thirtieth sweep — the custom-hire catalog seam closed inline: a
> successful `customRole` hire is a candidate, not a dead end — its owner human, or an admin,
> files a promotion ask snapshotting identity files and effective scopes at creation (the
> proposal-payload pattern, §4.3) and addressing the admin broadcast (§8.10): catalog authorship
> stays admin, adoption stays an owner ask · the accept publishes the row `active` with the
> placement it names — a new template, or a new version of an existing one, the version path
> filing upgrade asks to that template's pinned owners — and placement validates like every
> catalog write: a name-version collision refuses the accept with the ask standing, and a class
> flip refuses outright (§7's class immutability at its newest door — a custom hire is persistent
> by construction, §6.1) · the accept pins the hire it promotes: a live hire — suspended included,
> the §6.3 rebase rule, a pin being data, not execution — becomes the founding instance, later
> versions' upgrade asks reaching it like every pin, while a hire retired before the accept
> publishes unpinned, the founding reference audit and citation, the §5 terminal-clamp pattern at
> the catalog door · the snapshot is the role, never the life — `default_scopes` stores effective
> scopes as a ceiling (future spawns still child ⊆ spawner, §6.2; upgrades still new ∩ owner,
> §6.5), personal memory never rides (§6.3), and the hire's own promotion-sense folds to its
> owner as an ask (§6.1, §6.5, §7, §9, §12).

> **Edge-case closure (v2.39)**: thirty-first sweep — writer-order, catalog-state, and
> live-rendering seams closed inline: the domain write lock's id-ordered discipline covers
> every binding-surface writer — the admin `domain_ids` edit that re-keys the gate, the
> workspace-archive walk that drops bindings and kills the claim, and a hand-merge touching
> several domains' trees join topology remaps in up-front, id-ordered acquisition, so the
> ordered binding list the gate hop, reader sets, and remaps key on has one writer at a time
> and the second writer re-reads inside the lock — an edit racing a merge is a serialized
> sequence, never a lost update (§4.4, §4.5, §7) · the custom-hire founding pin names its
> state set — adoption at live activated states only (`active`, `suspended`); `requested`
> would pin a row its own approval could yet archive, `retiring` and `archived` are
> terminal-bound or terminal, each publishing unpinned with the reference as history, and a
> hire activating after an unpinned publish stays unpinned (§6.5, §7, §9) · spawn parameters
> class-match the template gate — a `ttl` on a persistent-hire request refused at write, a
> hire never half-persistent, mortal by an unreviewed field (§6.2) · the admin broadcast
> renders and admits responses against the live admin set — a mid-wait addition joins pending
> broadcasts, a departure contributes nothing, a former admin's late response refused at the
> eligibility door (§8.10) · and the org snapshot injects the live member set — departed
> members leave the always-injected layer at the walk, their record living in decisions and
> audit, no prompt carrying a departed member as present (§4.2).
>
> **Edge-case closure (v2.40)**: thirty-second sweep — timetable, originator, and
> state-rendering seams closed inline: the spend halt is a timetable state, not only a launch
> gate — schedules elapsing under the trip coalesce per the §8.5 machinery and play on the
> trip ask's resolution, the critical floor launching critical-class firings throughout, total
> exhaustion coalescing everything — pause's deferral rule extended to the money door, never a
> silent drop nor a lift storm (§6.2, §8.5) · plane-filed asks gain their originator: a
> reserved system principal — never a target, never response-eligible, rendered 'System' —
> with withdrawal reserved to the mechanisms that name their closures, and each side's
> retraction its own door; a compliance ask never files `from` the member it watches (§7,
> §8.10) · the org snapshot carries state, not just membership: suspended renders
> present-but-halted — the §8.10 non-active reassignment's rendering twin, never offered as a
> destination — retiring renders terminal-bound, and a `requested` hire is absent until
> activation publishes it, so what a prompt presents as answerable matches what the write
> guards accept (§4.2) · erasure sweeps operational history: resolved asks — `from`/`to`
> addressing and quorum response ledgers — and completed board-task assignments pseudonymize
> with the audit and spend lines, event shape kept, identity link severed, pending state
> pre-resolved by the §5 walk (§4.5).
>
> **Edge-case closure (v2.41)**: thirty-third sweep — retraction, grant-mortality,
> queue-clock, and ordered-degradation seams closed inline: the change of heart has the same
> door every terminal has — a pending spawn request is retractable by its requester, the
> approval ask's withdraw (its `from`) archiving the row with pin drained and claims
> released, one settlement at every door (§6.2, §7, §8.10, §9) · a delegation's agent-named
> grant dies with its grantee — the retire walk lapses the delegate edge, routing reverting
> to the owner with a digest line, a post-named grant riding its post's re-pointing and
> suspension keeping the non-active reassignment as its transient; window, supersession,
> initiative close, and grantee retirement are a delegate edge's four ends (§6.3, §8.10) ·
> the review SLA is bounded and monotonic under edit — `review_sla_days` ≥ 1 at every write
> door, tightening recomputes standing `review_by` earlier and loosening never touches
> standing clocks, §3's monotonic idiom at the queue door (§4.3, §7) · and injection
> overflow is ordered, not discretionary — glossary, then goals ('linked' before 'always'),
> then rules (narrative before enforcement-bearing), id-ascending ties, the org snapshot
> degrading to its routing spine plus the org-facts directory rather than truncating (§4.2).
>
> **Edge-case closure (v2.42)**: thirty-fourth sweep — proposer-mortality, ceiling-liveness,
> catalog-naming, and erasure-prose seams closed inline: a retiring persistent Coworker's open
> DNA proposals settle in the walk — withdrawn with an audit note, the §5 member-proposal
> rule's agent twin (a Coworker never owns domains, so no transfer branch exists), the
> folded-back learning available to the owner for re-proposal, and suspension leaving them
> standing per the non-terminal rule — the review queue never waits on a departed proposer,
> whatever member shape the proposer was (§6.3, §4.3) · the spawn approval's respond-time
> assumptions gain their ceiling: `scopeCeiling` lands at activation as requested ∩ the
> requester's live scopes — the §6.5 upgrade algebra at the spawn door — an empty intersection
> archiving the request with pin and claims released, so child ⊆ parent binds the parent the
> accept finds, never the snapshot the request filed (§6.2, §8.10, §7) · a template name keys
> its lineage across live and retired rows: a new template reusing a fully retired name must
> carry that name's class — class immutability spanning the lineage, not the live set — and a
> role whose class genuinely changed takes a new name, the domain-name reuse rule's catalog
> twin (§6.5, §7) · and the erasure annex covers operational prose: ask payloads, board-task
> descriptions, and run artifacts naming the member are reported for the human
> delete/rewrite/contest call like DNA prose — identity fields pseudonymize, prose reports
> (§4.5).

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
  and to-dos on a shared Task Board any member can hold or assign (a viewer reads it — §5's
  read-only surface). A directive becomes an
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
9. **Degrade to an ask, never to silence**: when any subsystem meets a state its designers did not
   enumerate — malformed payload, racing update, broken invariant — the universal failure mode is:
   refuse the effect, write the audit, raise an ask. Handling every scenario does not mean
   predicting every scenario; it means no failure mode is silent (§12 enforces it).

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
  affinity node goes offline, new runs queue until its heartbeat returns or an admin rebinds the
  workspace to another node — a rebind that first validates the target node actually
  advertises the workspace's required capabilities (files present, connectors authorized — §7
  `nodes.capabilities`), and a queue starved past a configurable window (default 24h) raises an
  admin ask: starvation is surfaced, never silently endured. Revocation is not the silent twin
  of an outage: an offline node may heartbeat back, a revoked one never will — its keypair is
  refused at every connection — so revoking a node with live workspace bindings surfaces each
  bound workspace's rebind ask at revocation time (§9), the impossible-placement pattern applied
  to a deliberate act, never a 24h queue the topology itself made hopeless. In-flight runs on a
  revoked node halt the way suspension halts them — partial results fold back through the memory
  tiers, staged writes go to §8.2 reconciliation, never killed mid-commit, and the runs are
  terminal with no resurrection (§6.3) — and the node's claims die with the row: revocation is
  the fenced lease's terminal case, the epoch fence refusing at the mediated boundary whatever a
  revoked node still holds. Capability validation is a property
  of the bind, not of the failover: the initial workspace–node placement runs the same check, so
  a workspace is never born attached to a node that cannot run it. Capabilities are live
  advertisements, not enrollment facts: every heartbeat re-states them, and a node whose
  advertisement no longer satisfies a bound workspace — a repo moved on disk, a connector
  de-authorized — surfaces the same rebind-or-starvation ask the affinity-loss path raises,
  rather than failing run after run; drift is a scheduling event, not a per-run surprise. The
  advertisement is the node's own — heartbeat-owned, not console-editable: the admin surface
  edits region and metadata, never capabilities, because a console edit there would be a
  silent no-op the next heartbeat overwrites; a capability change reaches the plane as drift,
  with its ask — the one door that fact has.
- **Node trust model**: remote nodes are *trusted compute*, not enforcement boundaries — scope,
  egress, and audit code runs on the node, so a compromised node can bypass it. Nodes enroll via
  one-time tokens, authenticate with a keypair identity on every connection, are revocable from
  the console, and every audit event records the executing node id. Enforcement that must survive
  a hostile node (egress allowlisting, secret handling) routes through the control plane / model
  gateway for remote nodes; single-process mode has no such exposure. 24/7 automations require an
  always-on node — workspace affinity on a sleeping dev machine is for interactive work only.
- **Workspace claims are fenced leases**: a node holds a workspace under a renewable claim lease
  carrying an epoch; heartbeats renew it, an admin rebind revokes it and bumps the epoch. A node
  must hold a live lease before claiming runs and before each external write; an expired lease
  (partition) forces a pause-and-resync before the node touches anything the control plane
  mediates — model gateway, connectors — so rebind never races a partitioned-but-alive node into
  dual-writer mode. A stale node's already-committed local writes are reconciled on reconnect
  (audit entry + contradiction report), not silently overwritten. The lease interval is §14.16's
  tunable; the fence itself is not optional.
- **Time & residency**: the control plane is the time authority — deadlines, SLAs, windows, TTLs,
  and leases evaluate against its clock, never a node's. That authority is monotonic in
  effect: expiries evaluate against a persisted high-water mark, so a backward clock step —
  an NTP correction, a drifted host — never un-expires an ask, un-lapses a window, lease, or
  rule, or reverses a terminal transition; a forward step only makes the watchers fire
  sooner. Time is an input the plane bounds, never one it trusts nakedly. Per-human
  timezones and working hours
  (§7) define each recipient's morning for digests and `queue_until_morning` (§8.10). Nodes carry
  a `region` tag; domains may declare a residency constraint, and scheduling — affinity and
  rebind — places work only on nodes that satisfy it: EU data stays on EU nodes by construction,
  not convention. A residency constraint no enrolled node satisfies is surfaced, not stalled on:
  affected work starves into the same 24h starvation ask, and the domain's owner sees it in the
  digest — an impossible placement is a visible configuration error, never a silent queue.
  Region is admin-set, not self-reported, so its drift rides the edit rather than the
  heartbeat: editing a node's region re-validates every residency-constrained placement bound
  to it (§9) — conforming leases stand, nonconforming ones surface the same
  rebind-or-starvation ask — the node-side twin of the §4.4 domain-edit rule, never a silent
  grandfathering. Residency's at-rest half is declared, not assumed: data at rest — the git
  store, SQLite, the db-only exports — lives with the control plane, so an obligation that
  reaches at-rest placement pins the deployment itself. Setting or tightening a residency
  constraint takes an audited admin attestation that the control plane's own hosting satisfies
  it — the db-only→git confirm's pattern (§4.5) — so a placement promise the deployment cannot
  keep is surfaced at declaration, never discovered at audit time.
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
- **Always injected**: the org snapshot (who's who — the live member set: a deactivated human
  or retired Coworker leaves the snapshot at the walk, their record living on in decisions and
  audit, the §4.4 reader-set liveness rule applied to the injected layer's own source — no
  prompt carries a departed member as present — and membership is not the whole render: the
  snapshot carries state, so an `active` member renders available, a `suspended` Coworker
  renders present-but-halted — never offered as a destination, the §8.10 non-active
  reassignment's rendering twin — a `retiring` one terminal-bound, and a `requested` hire is
  absent until activation publishes it; what a prompt presents as answerable matches what the
  write guards accept, the liveness rule of the injected layer extended from membership to
  state), the glossary slice relevant to the task's
  domain, all *applicable rules* for the workspace's domains, and the **goal slice**: active goals
  linked to the workspace through its initiatives, plus goals flagged org-wide (inject 'always'; statement, owner,
  deadline, status) — the flag composes with scope rather than overriding it: a domain-scoped goal
  flagged 'always' injects into every run that can read its domain, 'linked' only where an
  initiative binds it, and the compartment check below gates both (§7). Goals inherit the rules' window
  semantics, two-sided: a goal not yet at `effective_from` has not entered the slice — a Q4
  objective drafted in Q3 stays out of prompts until its window opens — and a goal past
  `effective_to` leaves
  the slice at window end — a stale deadline is never injected forever — and the §5.1 sponsor ask
  carries the outcome: extend re-adds it under a new window, a terminal status (`met`/`missed`/
  `retired`, set through the §4.3 write path) ends it for good, and an org-wide goal with no live
  initiative routes the same ask to its owner — the admin, when the owner is an agent or departed
  (§8.10 chain) — so no goal expires silently. "Applicable" has defined
  semantics: a rule applies when its domain intersects the workspace's domains and its effective
  window (`effective_from`…`effective_to`) covers the run (superseded rules drop out of injection
  automatically — co-temporal with their superseder: a supersession takes effect at the
  superseder's `effective_from`, so publishing a successor with a future window schedules the
  replacement rather than opening a normative gap — the predecessor keeps injecting until its
  superseder's window opens, the chain edge recorded at publish and the slice obeying the
  windows, §7); `machine_hint`
  narrows matching where present. A domainless workspace (§7, empty `domain_ids`) still has a defined
  layer, not an error state: no rules (nothing intersects), the org-wide glossary slice, and org-wide
  `always` goals — the same determinism, one domain smaller. Each layer carries a token budget (org snapshot ~1k, glossary
  slice ~2k, rules ~4k, goal slice ~1k — soft limits, configurable); overflow demotes items to
  retrieval (rules overflow into the searchable DNA index) rather than truncating silently —
  and the demotion is ordered, not discretionary, so the determinism claim survives an
  overflow: layers demote in reverse precedence — glossary entries first (terms stay
  resolvable through search and citation), then the goal slice ('linked' goals before 'always'
  ones), then rules last, and within rules narrative statements before enforcement-bearing
  ones (`machine_hint`-carrying), ties broken by item id ascending — the normative,
  enforcement-bearing content is the last to leave the prompt, and the order is testable
  alongside the slice (§12). The org snapshot is the one layer that cannot demote its items to
  retrieval and stay coherent, so it degrades by structure instead: the routing spine —
  domains with their owners, groups with their Leaders — never demotes, and member rows beyond
  the budget demote to the org-facts record (§4.1, generated from the registry, read-only and
  retrievable), a large org rendering as its spine plus a directory rather than a truncated
  roster.
  Injection stays deterministic per (reader access, domain set, linked-goal set, DNA version)
  so it is testable (§12). Injected layers pass the same compartment access check as retrieval
  (§4.4): a domain the run's member cannot read contributes no rules, no glossary entries, no
  domain-scoped goals — injection never bypasses compartments. Binding a member to a workspace
  whose domains it cannot read is refused at spawn and on admin edit alike, and a mid-life
  revocation that leaves a workspace with no readable domains refuses the next run's launch and
  raises an admin ask — degrade to an ask (§2), never to a silently empty prompt.
- **Retrieved on demand**: cards, decisions, and goals via hybrid search (BM25 + vector over the
  card index) — same retrieval machinery as v1's KB, now pointed at DNA. Search and injection
  serve the living corpus — `active` items only; a retired item resolves by direct citation as
  read-only history (the page opens, provenance intact) without ever surfacing in search or
  injection, a draft (owner-staged through item CRUD, §9) is visible to its owner alone (and a departing
  owner's drafts transfer with the domain, §5 — never orphaned invisible) — staging
  lives where the schema carries it: cards and glossary hold `draft` status, rules and goals
  stage through a future `effective_from` window instead, and decisions — lifecycle-free by
  design, immutable records (§7) — are always live: that is the item axis — no retired state
  exists for a decision — and it composes with the domain axis rather than overriding it, an
  archived domain's decisions leaving search with their domain's corpus and resolving by
  citation like the rest of its history (§4.4) — the phrase names a record's lifecycle, never
  an exemption from its domain's. Citation and
  search are different surfaces: the record stays navigable without haunting the prompt.
- **Cited in answers**: responses reference cards; the console (and IM) renders citations that open
  the source card with its provenance.
- **Precedence is fixed**: when layers disagree mid-run — an applicable rule vs. the goal slice
  vs. a retrieved card — the run obeys rules (normative, windowed) over goals (aspirational) over
  retrieved knowledge (descriptive), and files a contradiction report (§4.4) instead of silently
  picking a side. Glossary alias collisions in a multi-domain workspace resolve deterministically:
  the primary domain's term (§8.10) wins, then org-wide entries, then all candidates render tagged
  with their domains — never a silent coin flip. Within one domain the ambiguity is refused
  rather than resolved: a term or alias duplicating a live entry of the same domain is rejected
  at propose and at item write (§9; a live entry is a non-retired glossary row, §7 — draft and
  active hold their terms, retirement is what frees one for reuse) — the resolution order
  exists for cross-domain overlap, not intra-domain sloppiness. Org-wide entries share the null scope and are held to the same refusal:
  two org-wide definitions of "ARR" is sloppiness, not overlap the order gets to arbitrate.

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
(`review_by`, default 7 days, per-domain configurable through `dna_domains.review_sla_days`, §7); a breach escalates to the admin and a
stale queue surfaces in the owner's digest — the §1 learning loop must not starve on an ignored
inbox. The SLA itself is bounded and monotonic under edit: `review_sla_days` is ≥ 1 day at
every write door (the one-validation rule's newest bound — an SLA of zero is an
always-breaching queue, not a cadence), and an edit re-derives standing clocks in one
direction only — tightening recomputes each open proposal's `review_by` from its filed date
under the new SLA and applies it only where it lands earlier, while loosening leaves
standing clocks untouched and governs proposals filed after it: urgency moves forward,
never back, §3's monotonic idiom at the queue door, so an owner tightening a chronically
late queue sees the breach surface now, not after the old clock runs out. The queue belongs to the domain, not to the owner's inbox: it renders to whoever holds
`owner_human_id`, and owner re-pointing at any of its doors — a topology op (§4.4), the §9
domain edit, the §5 walks' transfer — re-keys the rendering to the new owner with `review_by`
clocks untouched, the §4.4 remap rule applied at the ownership door: a transferred domain's
open proposals surface in the successor's queue at once, never waiting on an owner of record
the row no longer names. Org-scoped items — org-wide goals and org-wide glossary entries (`domain_id` null, §7) —
have no domain owner of their own; their proposals route to the admin's review queue, the same
owner-of-last-resort pattern §5 applies to unowned domains, and their `review_by` derives from
the global default — no domain row governs the admin queue, so the 7-day default is its SLA.
Taint survives publication as
provenance residue: an item accepted from a tainted run keeps its flag, renders with an
indicator wherever cited, and heads the §4.4 scheduled quality
reviews — the owner's accept is informed consent, not a laundering step. Humans of any role but
viewer (§5's no-write surface) can also propose directly, and can edit in their own tools —
the store is git-backed markdown, so a PR workflow is possible for teams that want it. Proposals
are amendable in review: the proposer — or the reviewing owner, as suggested changes — files a new
revision (`revision`, §7); reviewers see latest-plus-history, publish binds the latest, and
withdraw-and-refile stops being the only edit path. Amendment and publish serialize behind the
same domain write lock (§4.4): racing amendments — proposer and reviewing owner filing
concurrently — land as sequential revisions on the atomically incrementing counter, never a lost
update, and a publish binds the latest revision that preceded it into the lock, never a moving
target. An amendment re-routes with its payload: a
revision that moves an item between org-wide and domain-scoped re-routes the review to the queue
governing the amended scope — org-wide lands in the admin queue, domain-scoped in the owner's —
so the reviewing authority always matches what would publish (cross-domain moves are refused
outright, §4.4). Revisions never change `kind`: scope re-routes, kind does not — a card
revision that should be a rule is a new proposal, so a review queue is never handed a payload
shape it was not routed to review. Racing publishes cannot land contradictions:
publish runs inside the domain write lock (§4.4) and re-runs contradiction checks against current
state at commit — the second of two sequenced contradictory publishes is refused back to review,
not half-silently merged. The re-check covers the edit target's lifecycle as well: an edit
proposal whose item has retired — or otherwise left the live set (§7) — mid-review refuses back
to review instead of editing frozen history; §8.10's respond-time re-validation, applied at the
publish gate. Separation of duties is a per-domain knob (`sod`, default `off`): when
on, the proposer cannot be the publisher — an owner's own proposal routes publish to an active
admin, the one alternative publisher the single-owner schema names (`dna_domains.owner_human_id`,
§7 — there is no second owner to route to); in a single-admin org that admin is the proposer
themself, which is exactly the one-click collapse §13 accepts for single-admin mode, and §14.13
keeps governing strictness separately.

### 4.4 Governance

- **Domains & compartments**: DNA is partitioned into domains (Engineering, Finance, HR…) each with
  a human owner and an access policy (`public` | `members of domain` | `named members`). The
  reader set is defined, not ambient: `public` admits every member; `members of domain` admits
  the owner plus every member tied in through an active workspace binding — a Coworker through
  its workspaces' `domain_ids`, a human through workspace participation (`participants`, §7) —
  and `named` admits the owner plus the named list — a list with a schema home (`named_readers`,
  §7: member ids, ignored unless access is `named`) that derives from live state like every other
  reader set: a deactivated human or retired Coworker on it contributes nothing — access
  re-evaluates with its inputs, and rehire's fresh row re-admits no one until named again.
  Liveness is a property of every input, not just the named list: a `participants` entry or a
  Coworker workspace binding whose member is deactivated — or whose Coworker is retired —
  contributes nothing to a `domain`-access reader set, the same live-state derivation
  `named_readers` gets (§7), and the §5/§6.3 walks scrub the lists anyway, so a departing
  member loses compartment reads at the walk and again at the derive — defense in depth, not
  two different rules. The
  owner always reads the domain they own —
  review is ownership's job, and §5's admin custody reads through the ownership it holds — and
  active admins read every domain: the §4.3 SLA escalation, sod routing, and §5 custody paths
  all hand admins domain content, so the role carries governance reads — audited on restricted
  domains like any other read (§13) — rather than a second, smaller map of what an admin may
  happen to see. Access re-evaluates with its inputs: a topology remap or a workspace unbind
  re-derives the reader set, and §4.2's no-readable-domains rule — next run refused, admin
  asked — is the same rule seen from the domain side. Retrieval
  respects the reader's access — the HR intern's Coworker never sees salary cards.
- **Provenance**: every card/rule/decision records where it came from; uncited claims are flagged
  during review.
- **Freshness**: review cadence and stale flags per item; scheduled DNA quality checks (a reviewer
  agent drafts a report; humans decide) re-validate provenance refs too — moved documents and
  rotated systems flag the card stale instead of letting citations rot silently.
- **Conflicts**: new rules supersede old ones explicitly (chains, not forks: a predecessor
  carries at most one live displacer — a second `supersedes_id` edge onto an already-superseded
  row is refused at every write door, §7, and the way to replace a superseded rule is to name
  the chain's live head — so the displacer whose window ends a predecessor's injection is
  always exactly one rule, never an unresolved pair); the review UI shows
  contradictions — rule-vs-rule, goal-vs-goal, decision-vs-rule, and quorum-vs-pool shortfalls
  (§8.10) — detected at proposal time and
  re-checked at publish, inside the domain write lock, so racing publishes cannot land
  contradictions sequentially (§4.3).
- **Topology changes**: reorgs split, merge, rename, and archive domains — a governed operation,
  not a hand-run migration: items move with ids stable (citations and supersession chains survive),
  access policies re-evaluate against the new topology, workspace domain tags remap, and the move
  is a single auditable event. The declared item mapping respects chain integrity: a supersession
  chain — intra-domain by the §7 write guard — maps whole to one result, and a split whose
  declared mapping would divide a chain across its results is refused at declare, the topology
  twin of that guard: a reorg never strands a chain across domains any more than a write does. An
  op declares its result, not just its inputs: split names owner,
  access, `store`, `sod`, and residency for each resulting domain (inherit-by-default — each
  result starts from the parent's `named_readers` list too, §7, there to be edited, not
  re-derived), a split's declared mapping is total or it refuses at declare — every item,
  workspace binding, and open proposal names its result, the archive rule's holdings
  enumerated up front — and the emptied parent archives inside the same audited event:
  division is dissolve-by-split, merge-away-then-archive's algebra with N receivers, the
  results new rows while items keep the stable ids citations depend on. Merge
  declares the surviving domain's attributes — access defaults to the most restrictive of the
  merged pair, undeclared attributes persist from the surviving domain, and the `named` reader
  list keeps the same floor: the survivor's list stands unless the op declares the union, so a
  merge never silently widens access — and a narrowed list shows in the event's access
  re-evaluation, never a quiet lockout. A `store` change migrates content inside the same audited
  event (git→db-only
  sweeps the files from the tree in one commit; db-only→git demands an explicit confirm, because
  the merge publishes immutable history) and is refused outright while either side sits under a
  kind-`domain` legal hold (§4.5) — and the migration is a property of the flag, not of the op
  flipping it: a standalone `store` change through domain update (§9) runs the same one-commit
  sweep, the same explicit confirm, and the same hold refusal. Residency edits carry the same
  visibility: a tightened constraint re-validates every bound workspace's placement — conforming
  leases stand, nonconforming ones rebind through the §3 capability-and-region check or starve
  into the same §3 starvation ask — an attribute edit is never silently grandfathered onto
  placements the old value allowed. The hold's refusal reaches the dissolving ops too: archive and merge-away of a held domain queue
  behind the hold's release, while rename stays available — ids, content, and access unchanged,
  the event audited — and merge-into a held domain stays open, a hold freezing removal, not
  addition. Split queues with the refusing side: dividing a held domain is removal in bulk —
  every item leaves it — so a split waits behind the hold's release too, and no topology op
  empties a domain while it is held. The commit re-runs contradiction checks against the post-op
  state inside the lock — items that coexist peacefully across two domains may collide in one,
  and the collision surfaces as review asks, never as silent coexistence. Dissolution is the
  degenerate case, not a missing feature: there is no bare delete — merge the domain's remaining
  items and workspace bindings away, then archive the emptied domain — archive refuses a domain
  still holding any of them (§9): the merge remaps bound workspaces to the survivor with ids stable,
  and an admin who wants a workspace domainless unbinds it first, so nothing silently re-points.
  And "holding" is scoped to the live set, so the §7 archived row is a real state, not dead text:
  an active item, an owner-staged draft (§4.2), a live workspace binding, or an open proposal
  blocks the archive, while terminal history — superseded and lapsed rules, terminal goals,
  retired cards and glossary entries — never does, and decisions never do either: a
  lifecycle-free record (§7) is history the moment it is written, not live state — "always
  live" (§4.2) names its search lifecycle inside the corpus, never an exemption from its
  domain's. History stays with the archived row as the
  read-only record §7 names, its items — decisions included — resolving by citation exactly as
  retired ones do (§4.2),
  nothing shredded. A domain holding nothing live archives directly — decisions and terminal
  rows alone are that domain — dissolution without a
  merge receiver — while merge-away moves the whole corpus, history riding with the live set to
  the survivor, decisions among it, ids stable, one audited event; reconstructibility never
  depended on emptiness, only on the refusal to shred.
  Open proposals travel with their domain rather than reviewing into a ghost: merge, split, and
  rename remap in-review proposals to the resulting domains' queues — payload `domain_id`
  rewritten, proposal ids stable, a split's proposals following the item mapping the op declares —
  inside the same audited event, their `review_by` clocks still running (a re-route changes the
  reviewer, not the SLA), and archive counts open proposals among the holdings it refuses exactly
  as it refuses live items and live bindings
  (§7 `status 'archived'`: read-only history; no
  injection, routing, or new bindings; nothing shredded, so reconstructibility survives).
  Pending asks travel with their domain too, not just proposals: an ask whose `to` was
  derived from a domain's owner — a persistent-hire spawn approval's gate hop (§6.2), a
  quorum ask's primary recipient (§8.10) — is re-keyed to the resulting domain's owner
  inside the same audited event, ask ids stable and deadlines untouched (a re-key changes
  the addressee, not the SLA), the open-proposals rule extended to the attention surface;
  escalation hops and quorum pools already evaluate against live state (§8.10), so
  creation-time addressing is the only piece a remap could leave stale, and the remap
  carries it — and the re-key binds to owner re-pointing itself, not to this op alone:
  re-pointing has three doors (this topology op, the §9 domain edit, the §5 walks' transfer),
  and a pending approval or quorum ask never outlives the act at any of them. The gate hop
  carries a derivation of its own beyond the owner, so a fourth door joins the three: the §6.2
  approval keys on the hire workspace's primary domain, and an admin edit of that binding (§7
  — primary demoted, unbound, or emptied to domainless) re-keys a pending spawn approval to
  the gate the edited binding derives — the new primary's owner or an admin once domainless —
  inside the audited edit, ids stable and deadlines untouched: creation-time addressing never
  outlives the row it was read from, on the binding surface any more than on the owner
  surface. And archive, which has no resulting owner to re-key onto, settles instead:
  owner-addressed asks pending against the archiving domain close with an audit note inside
  the event — §8.10's event-side settlement at the dissolution door — so the attention a
  domain held dies with its routing, never left addressing an owner of record that routing no
  longer derives.
Topology ops serialize behind a domain-level write lock (§4.5):
  split/merge/rename/archive queue behind in-flight proposals and each other — the stable-id
  guarantees assume no concurrent topology mutation, so the system enforces the assumption rather
  than hoping. Ops spanning several domains — merge above all — acquire every affected domain's
  lock up front, in domain-id order: two overlapping merges (merge A-into-B racing merge
  B-into-A) serialize in one deterministic order instead of deadlocking on half-acquired lock
  sets, and "queue behind each other" has its mechanism, not just its promise. The same
  discipline binds every writer to a workspace's `domain_ids`, not only the topology ops that
  remap them: the admin binding edit that re-keys the §6.2 gate (§7) and the workspace-archive
  walk that drops bindings and kills the claim (§7) acquire every affected domain's lock up
  front, in id order, before touching the list — and a hand-merge or PR touching several
  domains' trees in one commit acquires id-ordered the same way. The ordered binding is state
  the gate hop, the reader sets, and every remap key on; an unserialized edit racing a merge is
  a lost update either way it lands — the merge overwriting a deliberate unbind with a remap
  read from stale state, or the edit resurrecting a domain id the merge dissolved — so the
  binding rows get the one-writer door the content already has, and the second writer re-reads
  the list inside the lock it queued behind. Prior states
  stay reconstructible from git history and audit. Item edits do not
  smuggle topology: an edit proposal — or an item-level write (§9) — that would change an item's
  `domain_id` is refused at propose/write time. Cross-domain moves are topology ops: they carry
  id-stable remapping, attribute declaration, and post-op contradiction re-checks that an edit
  field would bypass.

### 4.5 Storage

```
~/.coworker/dna/            (or a company git repo — the canonical store)
  domains/<domain>/cards/*.md, rules/*.md, decisions/*.md, glossary.md, goals/*.md
  goals/<quarter>.md          (org-wide — domain_id null)
```
Markdown + frontmatter (id, version, effective dates, provenance, access); the control plane
maintains the SQLite/FTS/vector index over it. Humans can read and edit their company's brain with
any editor; git history *is* the DNA timeline. The tree holds `store: 'git'` domains only: a
db-only domain's whole content set — cards, rules, decisions, glossary, goals included — lives in
SQLite per the carve-out below; domain-scoped goals file under their domain and follow its `store`
flag, org-wide goals under `goals/` are git-backed. Frontmatter carries a `schema_version`: product
upgrades run in-place content migrations (post-backup) — an old store is never stranded. Direct
human edits are welcomed, not trusted: the control plane validates every ingested change
(frontmatter schema, unique ids, effective-window sanity, and a secrets scan — §10's scanner
guards the ingest door too, so a pasted credential cannot enter the canonical store through a
hand-merge) and quarantines invalid files to a
  review queue with the parse error attached — routed to the affected domain's owner (the admin
  for org-wide files), so a quarantine is never an unowned inbox — a bad hand-merge degrades to an ask, never to a
  silently corrupted index. And ingest serializes like every other writer: a valid hand-merge or
  PR landing applies inside the same domain write lock publishes and topology ops queue behind
  (§4.4) — the domain has one writer door, whichever side the write comes from, so an external
  edit cannot interleave with a publish or an item move and land as half-applied state — and a
  commit touching several domains' trees is a writer spanning domains like a merge: it acquires
  every affected lock up front, in id order, before applying a line. Paths
  under a db-only domain's tree are invalid by the same rule:
  the canonical copy lives in SQLite (the carve-out below), so a file that appears there — a
  stray hand-merge, a migration leftover — quarantines to the domain's owner rather than forking
  a second canon.

**Git integrity**: the control plane is the DNA repo's only direct writer — it signs commits and
refs with a deployment key and refuses non-fast-forward updates it did not perform; divergence
quarantines like any invalid ingest. Teams adopting the PR workflow get protected-branch
prerequisites (no force-push, no direct push, review through PRs) as deployment requirements,
verified at startup — the history that citations and supersession chains depend on is guarded,
not assumed.

**Privacy carve-out**: git history is effectively immutable, which collides with deletion
obligations (GDPR-style erasure, offboarded-employee data, HR/Finance records). Domains may
declare `store: 'db-only'` — HR and Finance default to it: content lives in SQLite with
export-on-demand and never enters the git store; the git timeline is reserved for non-sensitive
domains. If sensitive material lands in git by mistake, remediation is a documented history
rewrite (rotate the repo, notify domain owners) — decided here, not improvised under a deadline.

Erasure of a *person* is pseudonymization, not shredding: the append-only ledgers (audit, spend)
keep the event shape — what happened, when, with what effect — while the member reference is
replaced by a one-way pseudonym, severing identity without amputating the trail. The sweep covers
every live-state reference, not just ledgers: DNA provenance frontmatter and proposal attribution
rewrite to the pseudonym as a normal signed commit, and memory attribution joins the sweep —
personal and project memory (§8.3) re-point to the pseudonym the same way, so a departed
member's lessons survive as lessons while the identity link does not. Operational history
joins the sweep on the ledger's terms: resolved ask rows — their `from`/`to` addressing and
the responses ledger behind any quorum — and completed board-task assignments pseudonymize
with the audit and spend lines, the event shape (what was asked, what answered, what was
assigned, when) kept while the identity link severs; pending state needs no rule of its own,
the §5 walk having resolved it before an erasure request can run. Free-text mentions —
prose that names the member inside a card body, a memory item, a decision's context, or the
operational record's own prose: ask payloads, board-task descriptions, and run artifacts —
are reported, never rewritten: the sweep files an erasure annex to the admin listing each
mention with its owner — the initiative sponsor where the surface is initiative-tagged, else
the admin, operational records having no domain owner — and the per-mention call (delete,
rewrite under owner review, or contest as outside the demand) is a human one — never
silently kept, never silently rewritten; the identity fields around the prose pseudonymize
on the ledger's terms (above) while the prose itself reports. Git history
retains the pre-pseudonym commits
under the immutable-history boundary, and an erasure demand that exceeds pseudonymization takes
the documented history-rewrite remediation below. Legal holds
(`data_holds`, §7) freeze erasure for covered subjects until an admin releases them, audited; an
erasure request against a member with live dependencies is refused until the §5 offboarding walk
has run. Topology history for db-only domains rests on the audit log, not git: split/merge/rename/archive
on a db-only domain writes a full manifest — item ids, from/to domain, access re-evaluations — to
the audit log and triggers an export snapshot, with scheduled exports backing the history the git
timeline never held (§4.4's reconstructibility promise, restated per store kind).

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
  propose DNA, spawn within policy), `viewer` (read-only in full — never an ask target, never an assignee, and
  never an originator: proposing or amending DNA, filing asks, creating board tasks or initiatives, and spawning
  are all refused at write; the deputy, target, assignee, sponsor, lead, owner, group-Leader, and proposer guards in this plan
  are facets of one total no-write surface, not a checklist to dodge one item at a time). Auth starts as local accounts; SSO/OIDC
  later.
- **Asks — the universal interrupt**: approvals, questions, assignments, and spawn requests are all
  *Asks*: routed to a member (human or agent) with payload, deadline, and escalation policy —
  SLA tiers, expiry semantics, and escalation chains are a designed subsystem (§8.10), not just a
  routing table.
  Humans answer in the console (later IM/email digests); agents answer via their session worker.
  Approvals from v1 become Asks of kind `approval`.
- **Shared Task Board**: to-dos come from run results, playbook nodes, or any member with a write
  surface (a viewer reads the board, never writes it — §5's role bullet); assignable to
  humans or Coworkers — never viewers: the never-an-ask-target guard extends to assignments, a
  read-only member is not a worker, refused at write (§7) — groupable under initiatives (§5.1);
  visible org-wide within access scopes.
- **Groups/teams** mix humans and Coworkers (v1 kept agent-only groups; v2 unifies — a local
  Coworker still acts as Leader for execution routing). Membership derives from live state
  like every reader set: the offboard walk clears a departed human, the retire walk a retired
  Coworker — execution routing never addresses a dead identity. Leadership derives the same
  way: a group whose Leader departs or retires re-points the post inside the walk — a named
  successor, else the group's routing degrades to an admin ask (§2) rather than addressing a
  dead identity — the post is a routing surface, and no routing surface outlives its holder
  un-asked. The post is guarded at write like every routing surface it kinships with: a
  viewer human — or any non-active member — is refused the Leader post at set, routing
  addresses the Leader and the Leader must be answerable, the never-an-ask-target guard's
  facet; an ephemeral Coworker is refused by the mortality pin (§5.1), the lead/goal-owner/
  delegate guard's twin — a member dying by schedule must not hold a post built to outlive
  it, the reap walk's re-point (§6.3) the drain for what slipped past, never the design. And
  the demotion walk carries the post with the authority: a human demoted to viewer sheds the
  Leader post the way they shed deputy references and led initiatives — re-pointed inside the
  walk, a named successor else the admin-ask degradation — never left addressing a member who
  can no longer answer.
- **Accountability invariant**: every Coworker row carries `owner_human_id`; spawned workers carry
  `spawned_by`; the chain must terminate at a human. Enforced at spawn time — and derived there,
  not configured: §6.2 names the row's first owner (the gate's accepting human for a persistent
  hire, the first human up the chain for an ephemeral), and the §5/§6.3 walks carry it from there.
- **Offboarding**: deactivating a human runs the §6.3 dependency check across everything they
  touch: owned DNA domains (to a named successor, else **admin custody** — never orphaned,
  and carrying their pending owner-addressed asks with them: the §4.4 re-key binds to
  re-pointing at every door, this walk included — a spawn approval or quorum ask re-keys to
  the successor or custody inside the transfer, ids stable, deadlines untouched), open
  asks (to the member: reassigned up the chain; from the member: closed with an audit note — a
  departed member's pending spawn requests no longer gate anything) and board-task assignments
  (reassigned or returned to the pool), dependent Coworkers
  (re-owned or retired — re-owning narrows, never widens: a transferred Coworker's scopes
  re-derive as current ∩ the new owner's live ceiling at the walk, the §6.5 upgrade algebra
  applied to custody, and a transfer whose intersection comes back empty retires the worker —
  custody is never a scope widening; personal assistants are always retired: mirrored scopes die with the
  member, §6.4), sponsored/led initiatives (reassigned or closed — with their pending
  sponsor-addressed asks re-keying to the re-pointed sponsor inside the same walk, ids stable
  and deadlines untouched: the §4.4 remap rule extended from domain-owner derivations to post
  derivations, so activation, direction, and dependency decisions land on the member who now
  holds the post, never on the departed member's deputy), owned goals
  (`dna_goals.owner` — re-owned via the successor or admin custody, else retired; the walk clamps
  to active goals: a terminal one is frozen history (§7) whose owner reference stays pinned to
  the departed identity — severable only by §4.5 erasure, never rewritten by the walk), membership in
  `named` domain access lists (removed; policies re-evaluated), workspace participation and
  group memberships (`participants` entries removed, groups cleared — the named-list scrub's
  twin, §4.4: reader sets and execution routing re-derive), and deputy references (cleared in
  both directions — anyone deputizing the departing member re-points or clears), sessions
  terminated and PATs revoked (deactivation is credential-death, not a disabled login flag), and
  pending DNA proposals they authored (transferred to the successor for owned domains, auto-withdrawn with an
  audit note for member proposals — the review queue never waits on a departed
  proposer) — and owner-staged drafts (§4.2), the other authored-but-unpublished state: they
  transfer with the domain to the successor or admin custody and surface in the inheritor's
  DNA console as staged items awaiting a decision, never orphaning as content invisible to
  every living member. Inactive members
  are skipped when walking ask chains. Guard: the last active admin
  cannot be deactivated — evaluated inside the offboarding transaction, so two racing
  deactivations of the last two admins see one success and one refusal (§9's bootstrap
  atomicity pattern); the org never goes headless by accident or by race. The guard's refusal
  is a boundary statement, not a missing exit: winding the company down is never an
  offboarding — an org with zero humans has no accountability anchor (§2), so dissolution is
  a deployment shutdown (export, halt — §11's backup artifacts), an operational act with no
  in-product endpoint, and the guard refusing the last offboarding is the org model staying
  honest about that. Demotion joins
deactivation under the guard: an RBAC role change that would leave zero active admins is refused
by the same transactional check — headless-by-self-demotion is the same accident through a
different door. Audit history is retained;
  personal data falls under the §4.5 deletion carve-out. Rehire is a new member, never a
  resurrection: deactivation is terminal for identity, so a returning employee gets a fresh row —
`decided_by` references, audit history, and spend attribution stay pinned to the departed
identity (until a §4.5 erasure request pseudonymizes the reference — the events are durable, the
identity link is not), and email addresses are not reused.
- **Demotion is a walked transition, not a label flip**: an RBAC edit that reduces a human's
  role runs the same dependency walk as offboarding, scoped to what the new role can no longer
  carry, inside the last-admin guard's transaction — a demotion cannot half-land, and it cannot
  leave holdings the role does not support. To `viewer`: open asks to the member reassign up the
  chain (asks from the member close with an audit note, as offboarding does), board-task
  assignments return to the pool or reassign, deputy references clear in both directions, group Leader posts re-point (the routing
  surface's answer-eligibility shed, the groups bullet's demotion clause), owned
  goals re-own via successor or admin custody or retire — active goals, the same clamp as
  offboarding: terminal ones are frozen history (§7) — sponsored/led initiatives re-point,
  and owned authority the role no longer carries transfers the way offboarding transfers it —
  owned domains to a successor or admin custody, owned Coworkers re-owned or retired, with
  personal assistants always retiring: a never-ask-target cannot own staff, and viewer-mirrored
  scopes (§6.4) would make the assistant read-only anyway. The re-key rules ride identically —
  sponsor-addressed asks re-point with the posts, owner-addressed asks with the domains,
  re-owned staff narrow to the new owner's ceiling: the walk is one mechanism with two doors,
  and its attention and scope effects are the same through either. `owner` → `member` sheds domain
  ownership by the same rule. Authored proposals travel with the authority, exactly as at
  offboarding: transferred to the successor for shed domains, withdrawn with an audit note when
  the new role can no longer propose (to viewer) — the review queue never waits on a proposer who
  can no longer amend, and the member level keeps its amendment rights (§4.3). Staged drafts
  ride the shed domains the same way — transferred with them, surfaced to the successor, the
  clause offboarding pins (§4.2). The §7 write-time guards refuse viewer deputies, assignees,
  sponsors, and leads at set; the demotion walk is what keeps those invariants true mid-life,
  not merely at write — the guard and the walk are one mechanism in two tenses.

### 5.1 Initiatives — from directive to coordinated execution

A CEO-level directive ("let's open the Austin store") must not die in a chat scroll. Its path:

1. **DNA first**: the directive lands as a decision record and (usually) a goal through the normal
   write path (§4.3) — the *what* and *why* stay governed.
2. **An initiative opens**: the execution spine linking goal → work. It carries a **sponsor** (the
   authority behind the directive — pinned human, like every accountability chain §2), a **lead**
   (accountable member, human by default), a deadline,
   status (`proposed` → `active` → `paused`/`closed`), and an optional business budget (§14.11).
3. **Decomposition is work, not talk**: the lead creates board tasks (human or Coworker assignees),
   instantiates the relevant SOP as a playbook, requests spawns — all tagged with the initiative
   and visible on its slice of the shared board.
4. **Cross-domain coordination runs through the playbook spine**: an initiative playbook's nodes
   route asks into each domain's own chain (§8.6, §8.10) — Finance asks to Finance, Legal to Legal
   — so coordination is auditable state, not another chat channel (§8.11).
5. **Progress is state, not narration**: the initiative view is goal + ask burndown + task/playbook
   status + spend. A stalled initiative — deadline passed with open work — raises an ask to its
   sponsor (then admin), reusing the §8.10 escalation machinery. The stall clock is defined even
   without a deadline: an initiative opened with none keys staleness to its linked goal's window
   when there is one, and falls to a bulk-tier staleness line in its sponsor's digest after a
   configurable window (default 30 days) when there is not — directive decay (§13) has no dead
   zone. The clock's state coverage is pinned with it: it runs while `active` and while
   `proposed` — a directive that never won its activation is decay in its purest form, and the
   activation ask's deny (below) ends the wait, not the watch: the bulk line keeps rendering in
   the sponsor's digest, inert is not invisible — `paused` suspends it exactly as it suspends
   the stalled-work escalation, resume restarting it, and `closed` stops it for good. A deadline passed with *no*
   open work is not silent either: a bulk-tier close-out ask goes to the sponsor — close, or
   extend the deadline if more is coming — so a finished initiative cannot linger on holding
   workspace bindings and injecting goals nobody is driving. The same ask fires when the
   linked goal's window (`effective_to`, §4.2) ends while the initiative is live —
   extend, re-target, or close is a human call, not a silent drop from the slice — and any
   terminal transition of the linked goal (`met`, `missed`, `retired`) fires the same ask, not
   just a window elapsing — the choices track the outcome: window-end offers extend/re-target/
   close, `met` offers close or re-target (a met goal needs no extension), `missed` and
   `retired` offer re-base/re-target/close — so an initiative never keeps executing
   toward a goal that has already ended. The ask's state coverage is every non-closed state,
   not `active` alone: a goal ending under pause raises it with the escalation suspended
   alongside the stall clock — playing on resume, the pause-defers-timetables rule applied to
   attention — and a goal ending while `proposed` joins the activation ask on the sponsor's
   desk, one desk, two questions. Activation re-validates the `goal_ref` it inherits like
	  every respond-time assumption (§8.10): a sponsor accepting activation against a goal that
	  died mid-wait is audit-only, the re-point successor ask carrying the decision. The ask
	  re-validates the initiative's own state at the same door: a close may land from
	  `proposed` — the denied-initiative rule names it, a sponsor or lead shutting down a
	  directive that never won its authority — and an activation accept arriving after that
	  close is audit-only, the row stays closed: terminal beats activation, the spawn
	  approval's pause/close rule (§8.10) applied at the activation door, so a closed
	  initiative is never resurrected by an answer that raced its own funeral. And the
   linkage is guarded at birth: a new initiative's `goal_ref` names a live goal at write
   (§7) — an initiative is never born pointed at history; the only way it comes to address a
   terminal row is the goal dying under it, which is exactly the case the ask exists for. The answered choice moves the linkage with it,
   atomically with the answer: extend re-windows the same row, re-base and re-target swap
   `goal_ref` onto the new goal, and the goal slice re-derives at once (§7) — the re-point is
   part of the ask's effect, never a manual afterthought that leaves the spine addressing a
   terminal row — and the swap's target rides the same respond-time liveness check (§8.10):
   a re-point onto a goal that itself died while the ask waited is audit-only, the successor
   ask carrying a live choice; an unanswered ask escalates rather than leaving the linkage dangling (the
   direction-ask rule, below). **Pause is explicit
  and total**: a paused initiative suspends its stalled-work escalation and freezes its board
 slice (no new runs or spawns launch under it — filing and editing board tasks on the frozen
 slice stays open as planning: pause stops execution, not deliberation, with §7's assignee
  freeze the per-member axis and this the per-initiative one; schedules elapsing under the
  pause coalesce per §8.5 and play their catch-up run on resume — pause defers timetables,
  never drops them). The freeze is a launch gate, not a mid-run kill: runs already in
  flight when the pause lands complete onto the paused slice — the same drain close runs,
  below — and stopping work mid-flight stays suspend/retire's job (§6.3). Pause — unlike
  close — does *not* lapse its delegated
 rules (§8.10) or drop its workspace bindings: the binding list keeps the paused initiative, linked
 goals keep injecting (context, not execution), and resume unfreezes in place — close, not pause, is
 the transition that unbinds (§7). Pause freezes execution, not authority; the delegation's own window (§4.2
  `effective_to`) stays the bound, and pausing past the deadline still raises the sponsor ask —
  pause is a state, not a way to outlive a deadline silently. Closing runs the same dependency
   check as retiring a Coworker (§6.3) over the initiative's durable state — open asks
   and tasks resolved or reassigned, pending spawn requests archived with their template pins
   drained (the retire walk's spawn-request settlement applied at close, §6.3, §6.5: an
   approval landing after close would publish a worker into a slice that refuses launches,
   and a terminal act leaves no waiters), and the triggers and playbook schedules whose runs launch
   under it re-pointed to a successor initiative or disabled with an audit note, the
   retire-walk's automation resolution applied to the spine, so nothing keeps knocking on a
   closed door — while its in-flight execution drains, never truncates:
   runs already launched complete (staged external writes are never killed mid-commit, §8.2)
   with artifacts landing on the closed slice as history, new runs and spawns under the
   initiative are refused, and its ephemeral workers finish their bounded task and fold back;
   a sponsor who needs work stopped mid-flight suspends or retires the specific Coworkers
   (§6.3) rather than closing under them. The retrospective files DNA proposals — the §1 loop
   closes.
   Initiatives may declare dependencies (`depends_on`, §7): closing an upstream initiative with
   active dependents raises an ask to each dependent's sponsor — proceed, re-base, or pause — a
   coordination signal, not a hard block; the humans who own the downstream calls make them. The
   graph stays a DAG: dependency cycles are refused at write — the §8.10 deputy-cycle guard
   applied to initiatives; a malformed web of directives is rejected at the door, not discovered
   mid-close. Edges name live rows, the `goal_ref` rule on the graph axis: a dependency
   declared on a closed initiative is refused at write — the upstream's close signal already
   fired without this dependent, and an edge born pointing at history is a fossil, not a
   coordination signal. The only way an edge comes to address a terminal row is the upstream
   closing under a dependent that declared it live, which is exactly the case the close-ask
   exists for.

**Transitions are owned, not ambient**: `proposed` → `active` is the sponsor's acceptance — an
initiative opened by anyone other than its sponsor routes an activation ask to the sponsor
(expiry `deny`: a directive that never won its authority never gets execution — the denied
initiative stays `proposed`, inert — no bindings, no runs, no escalations — until its sponsor or
lead closes it; org state is never silently evaporated), a sponsor's own
opens active — initiatives bind workspaces at activation, pause retains the binding frozen
(above), close drops it (§7) — and only `active` initiatives launch runs — and spawns: a spawn
filed under a non-active initiative is refused at request (§6.2), the spawn twin of the
paused-slice refusal — `proposed` has no bindings to offer a new workspace (§7), `paused`
freezes execution, `closed` refuses new work — and board tasks join runs and spawns in that
refusal: filing stays open on `proposed` and `paused` slices as planning (the pause rule,
generalized), never on a closed one — a task that still needs filing belongs to a successor
initiative, so the closed slice is history the day it closes. Pause and
resume belong to the lead or the sponsor — an admin holds both as emergency backstop, the §6.3
authority pattern applied to the initiative itself, so the org's halt authority never lacks a
hand on the switch — and close belongs to either and always runs the §6.3
dependency check (§9) — and unbinds: workspaces drop the closed initiative from their binding
list (§7), the goal slice re-derives at once, and no workspace keeps reading a closed spine.
The sponsor's other direction asks — stalled-work escalation, the close-out ask, goal-window and
terminal-goal asks — carry the opposite expiry from activation: they are questions (§8.10), so an
unanswered one escalates sponsor → admin and stays pending in every digest rather than dying as a
silent no — an unanswered question may never decide an initiative's fate by disappearing, which
is precisely what the activation ask's `deny` reserves for itself: a directive that never won its
authority must never execute.
`Closed` is terminal — initiatives never reopen: a revived directive opens a new initiative
referencing the old one's decision (`decision_ref`), so burndown and history survive as
themselves instead of being rewritten.
Both posts are ask-eligible at write — a viewer human, or any non-active member in either role,
is refused the way an ask target or assignee is (§5): a post that routes asks cannot be held by
a member who can never answer one, and mid-life departures re-point the posts via the §5/§6.3
walks, so eligibility is maintained, not merely checked once. The sponsor's pin is a guard, not
an aspiration: an agent sponsor is refused at the same write — the post is human by schema and
by check (§7), so no mid-life walk is ever asked to re-point a post that should never have
existed. Mortality joins eligibility as a write guard: an ephemeral worker is refused as lead —
and as goal owner (§7) and as a named delegation agent (§8.10) — the agent-sponsor pin's logic
applied to TTL: a member dying by schedule must not hold a post built to outlive it, and the
§6.3 reap walk — which returns tasks and re-routes asks, the holdings an ephemeral may
legitimately leave — is never asked to re-point what the write gate should have refused. The
origin matches the holder: ephemeral-origin initiatives are refused at write, the
ephemeral-origin DNA-proposal rule (§7) generalized to directives — a bounded worker's
directive-deserving output folds back to its spawner, and a human or persistent Coworker opens
the initiative.

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

`customRole` is for persistent hires (proposed by humans or persistent Coworkers behind an
approval gate — the requester gate is class-matched like the §6.2 template gate: an ephemeral
worker is refused a persistent-hire request at write, template or customRole, its
recommendation folding back to the spawner, the §5.1/§7 ephemeral-origin rule applied to the
hire surface; a dying-by-schedule requester would otherwise leave the approval ask for the
reap walk to close, and a write gate beats a walk);
ephemeral workers must instantiate whitelisted subagent templates (§6.2) — no free-form
ephemeral roles. Ephemeral spawning is an agent/playbook capability only; a human wanting bounded
delegation assigns a board task or instantiates a playbook. And a custom hire is one-off by
request, not by fate: the role definition lives in the hire, and one that worked promotes into
the reusable catalog through the owner-asked path (§6.5) — the catalog grows from what worked,
not only from what was pre-authored.

### 6.2 Policy engine (hard-coded, not prompt-enforced)

- **Scope delegation**: child's file/tool/connector scopes ⊆ parent's. A secretary cannot spawn
  anything with repo write access she doesn't have.
- **Allowlists**: which templates each member class may spawn; ephemeral workers restricted to
  whitelisted "subagent" templates — and the allowlist spans the catalog's live surface only: a
  spawn request may name an `active` template (§7), never a `draft` (authoring state) or
  `retired` (history) one, refused at request time — and class-matched as well as
  status-matched: a persistent hire names a `persistent`-class template, an ephemeral worker a
  whitelisted `ephemeral-subagent` one (§7), the wrong class refused by the same transactional
  claim. That gate is the other half of §6.5's
  pin-drain — retirement lands once pins and pending requests drain, and no new request can slip
  in behind it: the status check claims the template row inside the spawn transaction, the same
  atomic-claim pattern the count caps use, so a retirement and a racing request see one winner.
  Parameters class-match like the row: a `ttl` on a persistent-hire request is refused at write —
  the class's bounds are budget policy and an owner (the §6 class table), and a TTL-mortal org
  member is the ephemeral shape — the class-matched gate extended from the catalog row to the
  request's own fields, so a hire cannot arrive half-persistent, mortal by a field nobody
  reviews.
- **Quotas & caps**: max concurrent ephemeral workers per spawner, global spawn depth (default 2),
  org-wide concurrent Coworkers, per-spawn and org-wide spend caps metered by the spend ledger.
  Cap windows match worker class: an ephemeral worker's cap spans its lifetime, a persistent
  hire's cap is a periodic window (default monthly, admin-configurable) inside which reserved +
  settled evaluate — a long-lived hire is neither bankrupted in week two by a lifetime cap nor
  free forever after one exhausted reserve.
  Count caps are *claimed*, not checked: the policy engine increments atomically inside the spawn
  transaction, so two spawners racing the last concurrent slot see one success and one refusal —
  no check-then-act window. The money side reserves the same way: a spawn or run reserves its
  budget atomically against (reserved + settled) in the spend ledger (`kind
  'reserve'|'settle'|'release'`, §7), settles to actual cost at completion, and releases on
  failure or reaping — two runs sitting at 49% of a ceiling cannot both spend past it, and §6.4
  rate/volume limits reserve reads identically. Claims are lifecycle-pinned to the request
  row, not just the moment of claim: a count-cap claim or budget reserve attaches at request
  creation — inside the spawn transaction, where the racing-claim guarantee lives — transfers
  to the live worker at activation, and releases at every terminal a pending request has:
  denial, approval expiry, the close-/archive-time settlements that drain template pins
  (§5.1, §7), the requester-state archive the approval gate names (below), and the
  requester's own retraction (below) — the pin-drain's budget twin, every terminal covered. An approval can never publish into an exhausted
  cap (the accept-time re-validation family, §8.10), and cap space never leaks on a request
  that died waiting. A settle may overshoot its reserve — the final
  provider call lands after the meter — and the overrun is handled, not rolled forward: it
  settles in full, surfaces on the spend dashboard and the owner's digest, and further reserves
  against that cap are refused until an admin acknowledges through the §9 overrun-ack endpoint —
  the refusal itself is the ask (§2). And cap edits are claim-scoped, never retroactive:
  caps gate claims, not existence — a tightened count cap leaves live workers to run out their
  natural terminal on the claims they hold while new claims refuse the tightened value, and
  nothing is force-reaped or stranded by a configuration act. The spend ceiling is the one
  edit that bites immediately, and it bites as designed: a ceiling tightened below live
  reserved+settled is the breaker's trip condition — the halt lands, the trip ask raises
  below, and the edit is a loud act, never a silent contradiction between a cap row and its
  ledger.
- **Approval gates**: persistent hires → Ask to the owner of the domain the hire's primary
  workspace is bound to (or an admin) — a primary workspace with no bound domain routes the ask
  to an admin outright, as does a hire with no workspace binding at all: no primary workspace
  is no primary domain, the same deterministic hop — and a multi-domain one routes to the
  primary domain (first-bound,
  admin-editable, §8.10): one deterministic hop, never an undefined gate — and a gate may
  address its own originator: the domain owner hiring into their own domain accepts in one
  click, the ask itself the audit record of the self-approval (sod governs DNA publish, §4.3,
  not hire, and the quota, depth, and budget gates still bind); the hop's addressee rides
  owner re-pointing wherever it happens — topology op, §9 domain edit, or the §5 walk — a
  pending approval re-keying to the resulting owner with its deadline untouched, and it rides
  the binding too: an admin edit of the hire workspace's `domain_ids` (§7 — primary demoted,
  unbound, or emptied to domainless) re-keys a pending approval to the gate the edited binding
  derives, the new primary domain's owner or an admin once domainless, inside the audited edit
  with ids stable and deadlines untouched — the creation-time hop never outliving the binding
  row it was read from (§4.4);
  agent-spawned
  ephemeral workers exceeding quota → Ask to the spawner's owner human. An approval ask that
  expires is the denial's twin — deny is the spawn request's expiry default (§8.10): the
  request transitions `requested`→`archived` (§7) and drains its template pin, the expiry the
  record exactly as the deny is (§6.5). The requester's own state is an assumption of the same
  rank: an accept landing while the requesting member sits in any non-active state is
  audit-only, and the request archives with its template pin drained and its cap claims
  released (§8.10) — retirement and offboarding already settle their requests inside the walk
  (§6.3, §5), and suspension, which resolves no dependents, is covered at the door: never a
  worker published under a halted subtree, and the suspended requester re-requests on resume.
  The ceiling the request names is an assumption of the same rank: `scopeCeiling` lands at
  activation as requested ∩ the requester's live scopes — the §6.5 upgrade algebra at the
  spawn door, a requester narrowed between filing and accept narrowing the child with the
  answer — and an intersection that comes back empty archives the request with its template
  pin drained and its cap claims released, the empty-intersection refusal's spawn twin:
  child ⊆ parent binds the parent the accept finds, never the snapshot the request filed,
  so a demoted or de-scoped requester cannot publish a child above the ceiling they now hold.
  And the change of heart has a door of its own: the approval ask is the requester's — `from`
  the requester, §8.10 — so its withdraw is the request's retraction: the ask resolves per
  the withdrawal algebra (a withdrawn approval is a no), and the request archives with its
  template pin drained and its cap claims released — denial, expiry, the walks'
  settlements, the requester-state archive, and now the requester's own retract: one
  settlement, every terminal, a pending hire never outliving the live intent that filed it.
  Approval is adoption, and ownership follows it: the hire's `owner_human_id` at activation is
  the gate's accepting human — the domain owner or admin whose answer published it, the
  self-addressed gate collapsing requester and owner into the one click — so a member hiring
  into another's domain staffs that domain, and the human who accepted the hire onto the books
  owns it; a re-keyed gate ask lands the same way, whoever the re-key addressed when the
  accept arrived owning the hire (§4.4), and the §5/§6.3 walks re-point from there — the
  derivation names the first owner, never a permanent one. Ephemeral workers, ungated, roll to
  the chain: their `owner_human_id` is the first human up the `spawned_by` line — the spawner
  themself when a human spawns, the spawner's owner when an agent does (§2) — pinned at
  spawn, so `spawned_by` carries lineage while `owner_human_id` carries accountability.
- **Runaway protection**: depth cap, rate limits, TTL reaper, budget circuit-breaker (org spend
  ceiling halts all spawns and automations with a loud Ask to admins). The breaker trips by
  class: triggers and playbooks carry a `criticality` tag (§7 — a firing's class is the stricter
  of its trigger's and playbook's tags), and a ceiling breach halts
  `standard`-class work first while a small critical floor (default 5%) keeps money-moving and
  customer-facing automations alive — total exhaustion still halts everything, loudly. And it
  un-trips only through that ask: the trip ask's accept lifts the halt, a deny holds it while
  ceilings are re-tuned — spend does not decay with time, so the breaker never releases
  itself, and an unacknowledged halt stays visible instead of expiring into silence (§2's
  contract on the money surface). The halt is a launch gate at every door the money surface
  has, not only the scheduler's: runs already in flight when the trip lands complete and
  settle onto the ledger — a settle overshooting under the halt trips the overrun ack gate
  exactly as designed (above) — and staged external writes are never killed mid-commit
  (§8.2); the spawn gate joins them: a spawn-approval accept landing while the halt holds is
  audit-only, the request archiving with its template pin drained and its cap claims released
  — the §8.10 pause-race rule at the money door, publishing a worker being a launch the halt
  refuses — and the requester re-requests once the trip ask resolves (lift or re-tuned
  ceilings, above). The critical floor scopes with this rather than around it: criticality
  tags live on triggers and playbooks (§7), so the floor keeps critical-class firings —
  in-flight and newly fired — reserving within its headroom through a partial breach, but a
  spawn approval carries no criticality tag and never rides the floor; no accept publishes a
  worker past a halt, and total exhaustion halts everything anyway. And the halt is a timetable
  state as well, not only a launch gate: schedules elapsing while it holds are pause's rule at
  the money door — firings coalesce per the §8.5 machinery, one catch-up run per trigger with
  its missed-schedule summary, §6.2 rate limits bounding the backlog — and play when the trip
  ask resolves, so a standard-class trigger neither dies silently under the halt (§2) nor
  storms on the lift; the critical floor keeps critical-class firings launching in its headroom
  throughout, and a total-exhaustion halt coalesces everything, playing on lift — the money
  door defers timetables exactly as an initiative pause (§5.1) and a suspension (§8.5) do,
  never dropping them. The TTL
  reaper never kills between prepare and commit of an external write: it grants a grace window
  and leaves a reconcilable `external_writes` row instead (§8.2). A TTL lapsing while its worker
  is suspended halts-then-reaps — fold-back and §8.2 reconciliation first, archive after (§6.3):
  suspension defers the reaper's trigger, never its semantics.

### 6.3 Lineage

Lifecycle acts carry named authority: suspend, retire, and resume belong to the Coworker's owner
human, an admin, or the sponsor of an initiative the Coworker is bound to — §5.1's "a sponsor who
needs work stopped mid-flight suspends or retires" routes through this authority, not around it,
and an owner's staff stop at the owner's hand.

`spawned_by` chains render as an org graph in the console: who created whom, why (purpose), spend,
and current status. Retiring a persistent Coworker is a halt, not a drain: in-flight runs stop
exactly as under suspension — partial results fold back through the memory tiers, staged writes go
to §8.2 reconciliation, never killed mid-commit — before dependents resolve. Retiring requires
resolving its dependents (automations,
playbooks, paired IM sessions, live spawned workers — a dying spawner's ephemeral children fold
back into the workspace's project memory, not the departed personal one — plus board-task
assignments returned to the pool or reassigned, the retiree's workspace bindings and group
memberships dropped and group Leader posts re-pointed or degraded to an admin ask
(reader sets and execution routing re-deriving — the §5 participants
scrub's retiree twin, §4.4), owned goals re-owned or retired (narrowing to
the new owner's ceiling, the §5 rule — an empty intersection retiring), delegation grants
naming the retiree as their agent approver resolved with it — the `machine_hint`'s delegate
edge lapses inside the walk, the rule's normative content standing while its routing
reverts to the domain owner, a digest line noting the grant that died with its grantee
(§8.10: window, supersession, initiative close, and grantee retirement are a delegate
edge's four ends; a post-named grant — "by the lead" — rides the post's re-pointing
instead, and suspension keeps the non-active reassignment as its transient), and initiative
lead/sponsor posts reassigned or closed via §5.1 — their pending sponsor-addressed asks
re-keying to the re-pointed sponsor inside the walk, the offboard rule's post-derivation
twin — and the retiree's own pending asks closed with
an audit note — pending spawn requests included, draining the template pins they hold (§6.5):
their originating runs are halted and folded, so an answer would have no consumer, and a
terminal act must not leave state waiting on a member who will never respond) — and authored
proposals join the settlement: a persistent Coworker's open DNA proposals withdraw with an
audit note inside the walk, the §5 member-proposal rule's agent twin (a Coworker never owns
domains, so there is no transfer branch), the folded-back learning staying available to its
owner for a fresh proposal, and the review queue never waiting on a departed proposer
whatever member shape the proposer was — while suspension leaves them standing, the
non-terminal rule: the reviewing owner may still publish, amendment alone waits on the
proposer. The whole requirement is the same dependency check as deleting a skill, applied to
staff; the §5 offboarding walk is its superset for humans. Upgrade asks about the retiree
settle with it: an owner-upgrade ask
pinned to this Coworker (§6.5) closes unresolved with an audit note inside the walk — the
pin dies with the row — and a response racing the retirement is audit-only with no
successor ask, the respond-time re-validation's terminal case (§8.10). Suspension strands
no upgrade either way: an accept landing on a suspended Coworker is a data rebase — files
and scopes, not execution — and resume re-arms under the rebased template, the halt
freezing execution without freezing identity. Asks *to* the
retiree need no walk entry of their own: §8.10's non-active target rule (retiring included)
already reassigns them up the lineage chain, exactly as under suspension. The retiree's
personal memory archives with it — inert history under the archived identity, never injected,
never transferable to a respawn (re-role's lessons-go-to-DNA is the only bridge); a fresh hire
starts a fresh memory. The ephemeral
analogue runs at reap: the TTL reaper's fold-back returns the dying worker's open board-task
assignments to the pool, re-routes asks *to* it up the chain, and closes asks *from* it with an
audit note — the retire-walk settlement's ephemeral twin: a folded-back worker's pending
approvals have no consumer, and a terminal act leaves no waiters (§8.10) — memory is not the
only state a worker holds.

Two state changes short of retirement: **suspend** — an admin's emergency stop that halts triggers
and runs without resolving dependents (in-flight asks re-route up the chain); the halt covers the
subtree — live ephemeral descendants stop and fold back into the workspace's project memory
exactly as on spawner death, staged writes left to §8.2 reconciliation rather than killed
mid-commit — and the halt gates its own publishing: a spawn request pending *from* the
suspended worker is not a dependent to resolve but a launch the halt refuses, an approval
landing during the suspension audit-only and the request archiving (§8.10), so suspension
never publishes into its own halted subtree. **Resume** re-arms triggers (missed schedules
coalesce, §8.5) and launches new
runs, but never resurrects a halted one — a run suspended mid-flight is terminal: partial
results fold back through the memory tiers, interrupted work re-enters as new runs or board
tasks, and staged writes resolve through §8.2 reconciliation, so resume cannot half-replay a
side effect. Lifecycle acts are credential fences: a Coworker's PATs and sessions authenticate
only while its status is `active` — auth re-validates status at every use, so a suspended
worker's PAT cannot fire an API trigger and a retiring one's credentials die with the halt
(retire revokes PATs and terminates sessions outright, the same credential-death §5 gives
human offboarding); resume re-arms what suspension made inert, and suspension made it inert —
never deleted — so nothing re-authenticates from a stale grant. The second state change is **re-role** —
re-tasking a Coworker to a different role is retire-and-respawn (identities are role-shaped;
project memory stays with the workspace, lessons go to DNA), never an in-place IDENTITY rewrite.
In-place evolution of the *same* role is the template upgrade path (§6.5).

### 6.4 Personal assistants (deployment shape)

One persistent assistant per human employee is a *deployment* of the existing model, not a new
architecture:

- **Template**: a persistent-hire template (`personal-assistant`) bound 1:1 to a human —
  `owner_human_id` = the assisted employee. The 1:1 is enforced, not implied: the policy
  engine refuses a second `personal-assistant` spawn for a human with a live assistant —
  retirement closes the deployment, a fresh spawn reopens it. The assistant serves the employee but is accountable
  to the company: DNA proposals route to domain owners; compartment access is never widened to
  please the human.
- **Scope mirroring**: the assistant's scopes (DNA compartments, connector scopes, tool access)
  are derived from the human's RBAC role at spawn, **refreshed on role change, revoked on
  offboarding** (§5) — with one carve-out: a demotion to viewer retires the assistant rather
  than refreshing it, per the §5 demotion walk (mirrored viewer scopes are read-only, and a
  never-ask-target owns no staff). The scope-delegation invariant is reused with the employee's
  role as the ceiling: assistant ⊆ employee, everywhere.
- **Mirrored access ≠ mirrored behavior**: a human rarely opens 10,000 HR records; an assistant
  might bulk-read them. Restricted-domain reads carry **rate/volume limits** in addition to
  permission checks, and every read of a restricted domain is audited (§13).
- **Identity separation**: the assistant acts under its own member identity (own PAT, own audit
  trail, own spend-ledger line), never the employee's credentials — actions stay attributable.

### 6.5 Role & template evolution

Roles change as the company does; running staff must track the change without a respawn stampede.
Templates are versioned (§7 `role_templates`); every persistent Coworker pins the version it was
spawned from. A template's class is its identity across those versions: a version declaring
a class different from its name's predecessors is refused at publish — the (class, name,
version) key names rows, but the name keys the lineage — so a persistent Coworker's
upgrade ask can never point at an ephemeral-class row; a role that has genuinely changed
class is a new template and the retire-and-respawn path (§6.3), not a version bump. And the
name carries that lineage past retirement: a new template may reuse a fully retired name,
but only in that name's class — the class pin spans the name's live and retired rows alike,
so a catalog name never changes shape over time (§7), the domain-name reuse rule (§4.4)
applied to the catalog.
The catalog's write surface is admin-governed and audited — authoring,
publishing, and retiring templates are infrastructure acts; the owner asks below govern
adoption of what lands, never authorship of it. Version selection is explicit at every door:
a spawn request names the exact
catalog row — the console defaults to the newest `active` version — so an approval publishes
into the version the requester saw, never whichever row appeared or retired in between.
Publishing a new `active` version files the upgrade ask to each pinned Coworker's owner — the
company-wide bump-plus-queue below, made literal — and nothing auto-applies: a denied or expired
upgrade leaves the Coworker pinned to its current version, which remains a legitimate `active`
row, because publication supersedes but never retires — retirement stays the explicit, pin-gated
act (below), and the next bump re-asks. The accept re-validates what it would apply: an
upgrade accept re-checks its target version's status at the door — a new version holds no pins
of its own, so it can retire while its upgrade asks wait, and a retirement that beat the
accept leaves it audit-only, the pin standing on its still-legitimate row, the next
publication re-asking — the §8.10 respond-time family applied to the catalog, so no answer
rebases a Coworker onto a version the catalog has buried. An **upgrade** is proposal-shaped: the diff — IDENTITY/HANDBOOK changes, scope
deltas — goes to the Coworker's owner as an Ask; on accept, files rebase and scopes re-derive as
new-template ∩ owner's-current-scopes, never widening — and an intersection that comes back
empty refuses to land: the upgrade closes unresolved with the empty re-derivation surfaced in
the ask, because a scope-less Coworker is not an upgraded hire (retire-and-respawn is the path
when the role has genuinely moved past what the owner can carry). Ephemeral subagent templates
upgrade in place — workers are short-lived, so new spawns simply get the new version. Retiring a
template with live pins is refused, and pins count pending spawn requests as well as running
Coworkers: a request awaiting approval references its template exactly as a live worker does,
and retiring underneath it would let the approval publish into a ghost — upgrade or
retire-and-respawn the pinned Coworkers and resolve or reject the pending requests first (the
§8.4 skill-uninstall dependency check, applied to templates). A company-wide role
overhaul is one template bump plus a queue of owner asks, not a rehire.

**Custom hires promote into the catalog.** A one-off role that worked is a candidate, not a dead
end, and the connection is adoption-shaped: the hire's owner human — or an admin — files a
promotion ask (the §9 endpoint snapshots identity files and effective scopes at creation; the
payload is the proposal's, §4.3's pattern, never a live view), addressed to the admin broadcast
(§8.10): catalog authorship stays an admin surface, and the owner's ask is the adoption door. The
accept publishes the row `active` with the placement it names — a new template, or a new version
of an existing one, the version path filing upgrade asks to that template's pinned Coworkers'
owners exactly as a hand-authored publication does — and the placement validates like every
catalog write: a name colliding at the named version refuses the accept with the ask standing
(a bad answer, not a dead ask), and a class flip refuses outright — §7's class immutability at
its newest door; a custom hire is persistent by construction (§6.1), so promotion never lands an
ephemeral row. The accept pins the hire it promotes, and the adoption names its state set
exactly: a hire in a live, activated state at accept — `active`, or `suspended` (the §6.3
rebase rule; a pin is data, not execution) — becomes the template's founding instance,
identity continuity exact (the body is its own) and later versions' upgrade asks reaching it like
every pinned Coworker, while the pin itself re-derives nothing — the hire's live scopes stand,
already ∩ its owner (§6.2); the upgrade algebra first applies at the next version's accept.
Every other state publishes its template unpinned: a hire still `requested` would pin a row
its own approval could yet archive — denial and expiry are terminal for a pending request
(§7), and a founding pin must not ride a row one expiry away from history — while `retiring`
and `archived` are terminal-bound or terminal already; in each the founding reference rides the
audit event and the body's citation as history — the §5 terminal-clamp pattern at the catalog
door, the record surviving the row that made it. A hire that activates after an unpinned
publish stays unpinned — the founding moment passed it by, and a later version's upgrade path
is the only rebase it can ride. The snapshot is the role, never the life:
identity files and effective scopes, the latter stored as the template's `default_scopes` — a
ceiling, not a grant: every future spawn still derives child ⊆ spawner (§6.2), every upgrade
still re-derives new ∩ owner — and never personal memory (§6.3: a fresh hire starts fresh; a
founding instance's lessons are its own). The hire's own sense that it has become a role routes
to its owner as an ask, the fold-back shape — self-promotion is an adoption question, not a
catalog door. A denied or expired promotion ask is record only, and nothing about the hire
changes while its promotion pends: the ask is adoption, not alteration.

---

## 7. Data model (v2 delta)

New/changed tables (v1 session/run/message/skill/connector tables carry over):

> **Self-containedness**: this section, §8, and §9 are deltas against a v1 design doc that is not
> in this repo. Before Phase 0 starts, inline or link the carried-over v1 specs here. If any v1
> deployment exists, add a migration section: v1 `approvals` rows → `asks` of kind `approval`;
> per-Coworker KBs → DNA domain imports.

```
humans         (id, name, email, rbac 'admin'|'owner'|'member'|'viewer', auth json,
                 deputy_member_id?, timezone?, working_hours json?, created_at, deactivated_at?)
                 -- deactivated_at: offboarding's terminal marker (§5) — rehire is a new row,
                 -- so a timestamp is the whole state; ask-chain walks skip deactivated members
                 -- and the last-admin guard counts admins with deactivated_at IS NULL (§5, §8.10)
                 -- deputy: first hop of the §8.10 chain;
                 -- must reference a humans row that is neither the member nor a viewer —
                 -- agent, self, and viewer deputies refused at write (§8.10)
                 -- viewers are read-only and never valid ask targets (§5)
                 -- timezone/working_hours: per-human calendar — digests and queue_until_morning
                 -- compute against it (§8.10, §3)
coworkers      + owner_human_id, class 'persistent'|'ephemeral', spawned_by member?, ttl_at,
                 budget_cap, lineage_depth, template_id?, template_version?,
                 status 'requested'|'active'|'suspended'|'retiring'|'archived'
                 -- owner_human_id derivation (§6.2): a persistent hire's owner is the gate's
                 -- accepting human at activation (the self-addressed collapse included, a
                 -- re-keyed gate landing on the re-keyed addressee); an ephemeral's is the
                 -- first human up the spawned_by line — the §5 invariant derived at spawn
                 -- budget_cap window: per-worker lifetime when ephemeral, periodic (default
                 -- monthly) when persistent — §6.2; null = worker-uncapped: no per-worker
                 -- ceiling, but org-wide caps and the §6.2 breaker still bind — a cap is a
                 -- bound, not a prerequisite
                 -- ephemeral lifecycle maps 1:1: spawned→requested, running→active, done→retiring,
                 -- reaped→archived (done = fold-back pending, the ephemeral analogue of retiring)
                 -- suspended = emergency stop, halts triggers/runs without resolving dependents (§6.3)
                 -- a denied spawn request transitions requested→archived without ever
                 -- activating: denial is terminal for a `requested` row — the ask's deny is
                 -- the record — and archiving it drains the template pin it held (§6.2, §6.5);
                 -- an expired approval ask is the same denial (deny is the spawn request's
                 -- expiry default, §8.10): the row archives, the pin drains, the expiry the
                 -- record (§6.2); and the requester's withdraw on the approval ask is the
                 -- same terminal — the retraction archives the row, drains the pin, and
                 -- releases the claims (§6.2)
                 -- template_id null marks a customRole hire (§6.1): promotion (§6.5) is its
                 -- catalog door — the accepting admin's publish pins the hire as the new
                 -- row's founding instance when it sits in a live, activated state (active,
                 -- or suspended — the §6.3 rebase rule), the pin re-deriving nothing;
                 -- requested, retiring, or already archived at the accept, the template
                 -- publishes unpinned, the founding reference audit and citation
role_templates (id, name, version, class 'persistent'|'ephemeral-subagent', body json
                 (identity/style/handbook), default_scopes json, status 'draft'|'active'|'retired')
                 -- versioned catalog; persistent Coworkers pin (template_id, template_version) (§6.5)
                 -- (class, name, version) unique — the catalog's deterministic key: a new
                 -- version is a new row, never an in-place rewrite of one a Coworker pins;
                 -- class is stable across a name's versions — a class-flipping version is
                 -- refused at publish (§6.5), so a pinned lineage never changes shape
                 -- under the Coworkers holding it — and the name keys the lineage past
                 -- retirement: a new template reusing a fully retired name must carry that
                 -- name's class, class immutability spanning the lineage's live and retired
                 -- rows alike, so a catalog name never changes shape over time; a role whose
                 -- class genuinely changed takes a new name (§6.5), the domain-name reuse
                 -- rule's catalog twin (§4.4)
                 -- spawn requests name the exact row (newest active the console default, §6.5);
                 -- publishing a new active version files owner-upgrade asks to pinned Coworkers —
                 -- publication supersedes, never retires: a denied upgrade's pin stands on a
                 -- still-active row until explicitly retired (§6.5); an upgrade accept
                 -- re-validates the target row's status — a version retired mid-wait makes
                 -- the accept audit-only, the pin standing (§6.5, §8.10);
                 -- catalog writes — create, publish, retire — are admin, audited (§9):
                 -- authorship is infrastructure; the owner asks govern adoption, not authoring
nodes          (id, name, kind 'local'|'remote', capabilities json, region?, claim json?,
                 last_heartbeat, pubkey, enrolled_at, revoked_at?, status 'trusted'|'revoked')
                 -- region gates residency-constrained scheduling (§3, §4.5);
                 -- claim: epoch-fenced workspace leases (§3)
                 -- capabilities re-advertise on every heartbeat: drift against a bound
                 -- workspace surfaces the §3 rebind-or-starvation ask, not per-run failures
                 -- the advertisement is heartbeat-owned: console edits touch region and
                 -- metadata only, never capabilities (§3, §9)
dna_domains    (id, name, owner_human_id, access 'public'|'domain'|'named',
                 named_readers json, store 'git'|'db-only', sod 'off'|'reviewer-distinct',
                 review_sla_days int default 7,
                 residency?, status 'active'|'archived' default 'active')
                 -- db-only: the §4.5 privacy carve-out; sod: proposer ≠ publisher when on (§4.3);
                 -- review_sla_days: the per-domain queue SLA's schema home (§4.3) — review_by
                 -- derives from it at propose, and topology results inherit or persist it like
                 -- every other domain attribute (§4.4); bounded ≥ 1 at every write door, and
                 -- an edit re-derives standing clocks monotonically — tightening earlier,
                 -- never loosening (§4.3);
                 -- row-write authority is split (§9): create/archive, the structural
                 -- attributes — store, sod, residency — and owner re-pointing are admin
                 -- writes; the owner edits access, named_readers, and review_sla_days;
                 -- every row-write audited; owner re-pointing is the §5 walks' voluntary
                 -- form — the walk transfers on departure, the edit re-points in place,
                 -- one authority behind both;
                 -- residency constrains node placement (§3);
                 -- name unique among non-archived domains — review queues, digests, and
                 -- routing keys never alias (an archived name is history and may be reused);
                 -- access reader sets defined (§4.4): public = every member; domain = owner +
                 -- participants of workspaces bound to it; named = owner + the named list;
                 -- the owner always reads their own domain, active admins read all (audited, §13);
                 -- named_readers: the member ids behind access 'named' — ignored under the other
                 -- policies, evaluated against live state (deactivated/retired entries contribute
                 -- nothing), and removed outright by the §5 walk (§4.4)
                 -- owner must hold role 'owner' or 'admin' at write — an RBAC demotion below
                 -- that runs the §5 walk (transfer or admin custody, never an orphaned domain);
                 -- archived: read-only history — no injection, routing, or new bindings;
                 -- the holdings that block archive are the live set only (§4.4): terminal
                 -- history stays with the row, resolvable by citation (§4.2), decisions
                 -- included — lifecycle-free records are history at birth, never live-set
                 -- members (§4.2) — and a history-only domain archives directly;
                 -- dissolution = merge-away then archive, never bare delete (§4.4) — merge
                 -- moves the whole corpus, history included, ids stable
dna_cards      (id, domain_id, title, definition_md, refs json, provenance json, version,
                 status 'draft'|'active'|'retired' default 'active')
                 -- default active mirrors the glossary (§7): an owner's direct create is the
                 -- publish path (§9); draft is an explicit owner-staged phase (§4.2)
                 -- retirement is terminal: revival is a new card, and a draft discards by
                 -- retiring — one lifecycle, no un-retire (§9)
dna_rules      (id, domain_id, statement_md, machine_hint json?, effective_from, effective_to?,
                 supersedes_id, status 'active'|'superseded'|'lapsed')
                 -- effective_to bounds delegation windows (§8.10); lapsed: the window ended —
                 -- ordinary expiring rules transition lapsed at effective_to exactly like
                 -- delegations, dropping out of injection and routing; initiative close lapses
                 -- its scoped rules the same way (§8.10)
                 -- item-level retire (§9) is window truncation: effective_to pinned to now,
                 -- the row lapsing at that boundary — the enum carries no 'retired' because
                 -- lapse is a rule's terminal; superseded and lapsed rows are frozen history —
                 -- updates refused, revival a new rule, and a predecessor stays superseded when
                 -- its superseder lapses (chains are explicit; nothing flips back silently);
                 -- the displacement edge is the superseder's effective_from: a predecessor
                 -- keeps injecting until its superseder's window opens (§4.2), so a
                 -- future-windowed successor is a scheduled replacement, never a normative
                 -- gap — status records the chain edge at publish; the slice obeys the windows;
                 -- supersedes_id is intra-domain, refused cross-domain at propose and write —
                 -- topology ops move chains whole, so a chain never straddles domains (§4.4);
                 -- and a chain is linear, never a fork: a predecessor holds at most one live
                 -- displacer — a second live supersedes edge onto an already-superseded row is
                 -- refused at propose, amend, and item write (§9's one-validation rule), the
                 -- way to displace a superseded rule being to name the chain's live head, so
                 -- the superseder whose window displaces a predecessor is always one rule (§4.4)
dna_decisions  (id, domain_id, context_md, outcome_md, decided_by member, decided_at)
                 -- immutable and lifecycle-free: create-only at every surface (proposal publish
                 -- and item CRUD, §9) — no update, retire, or delete exists for them, and they
                 -- are always live in search (§4.2); reversal or amendment is a new decision
                 -- record citing the old through refs — the decision analogue of §4.4's
                 -- supersession chains
                 -- decided_by is cited provenance, not authority: any member — viewer, agent,
                 -- since-departed — may be recorded as the decider of record (the field
                 -- documents the world); review at publish is the authority, and no
                 -- ask-eligibility guard applies to the field
                 -- archive interplay (§4.4): a decision never blocks its domain's archive —
                 -- lifecycle-free means no live-set membership — an archived domain's
                 -- decisions leave search with its corpus, resolving by citation like the
                 -- rest of its history, and merge-away moves them with the corpus, ids stable
dna_glossary   (id, domain_id?, term, definition, aliases json,
                 status 'draft'|'active'|'retired' default 'active')
                 -- the "live entry" of the §4.2 duplicate check is any non-retired row of the
                 -- same scope — draft and active both hold their terms; retire (item CRUD, §9 —
                 -- never delete) is what frees a term or alias for a new live entry, and the
                 -- retired entry stays resolvable as read-only history (§4.2)
                 -- retirement stays terminal for the same reason it frees terms: un-retiring
                 -- would collide with a re-claimed term or alias — revival is a new entry (§9)
dna_goals      (id, domain_id?, quarter?, statement_md, owner member, status 'active'|'met'|'missed'|'retired',
                inject 'always'|'linked', effective_from, effective_to?)  -- goal-slice source (§4.2)
                -- the slice's 'deadline' (§4.2) is effective_to, and the window is two-sided:
                -- admission at effective_from, exit at effective_to (§4.2);
                -- owner: any member but an ephemeral worker — a viewer human is refused at
                 -- write (the §5 ask-eligibility guard: goal expiry asks route to the owner,
                 -- §4.2, so an owner must be answerable), an ephemeral by the §5.1 mortality
                 -- guard (a TTL-mortal member is not a durable owner); an agent owner keeps
                 -- the §4.2 admin-routing fallback, and demotion walks ownership like every
                 -- other holding (§5);
                -- domain_id null = org-wide: member-public by definition, and its proposals
                -- route to the admin review queue (§4.3); a domain-scoped goal inherits its
                -- domain's access policy — it injects only where that domain is readable (§4.2),
                -- so a sensitive objective (unannounced restructuring, pre-earnings targets)
                -- is compartmented like any other DNA content; the inject flag composes with
                -- that scope: 'always' reaches every run that can read the domain, 'linked'
                -- only initiative-bound workspaces (§4.2)
                -- terminal statuses are immutable: post-terminal updates refused at every
                -- surface (§9) — re-base and re-target create a new goal row; the §5 walks
                -- clamp to active goals, a terminal owner reference staying pinned to the
                -- departed identity — severable only by §4.5 erasure, never by a walk
dna_proposals  (id, kind 'card'|'rule'|'decision'|'goal'|'glossary'|'edit', payload json, revision int
                 default 1, proposed_by member,
                 provenance json, status 'open'|'published'|'rejected'|'withdrawn', reviewed_by?, at,
                 review_by?)  -- review_by: queue SLA deadline; breach escalates to admin (§4.3);
                 -- revision: amendable in review — history retained, publish binds latest (§4.3);
                 -- amendment and publish serialize behind the domain write lock, racing
                 -- amendments landing as sequential revisions (§4.3);
                 -- proposed_by must hold a write surface: humans and persistent Coworkers only —
                 -- an ephemeral worker is refused at propose (§6); its learning folds back, and
                 -- the spawner or a human proposes from it. Viewers are refused the same way (§5)
asks           (id, kind 'approval'|'question'|'assignment'|'spawn_request', from member, to member,
                 payload json, initiative_id?, workspace_id?, status 'pending'|'answered'|'expired'|'withdrawn', deadline, created_at,
                 sla_tier 'critical'|'standard'|'bulk', escalation json,
                 expiry_behavior 'deny'|'escalate'|'reassign', responded_at?, quorum_required int
                 default 1, responses json, collapsed_count int default 1)
                 -- supersedes approvals;
                 -- withdrawn: the originator's retract of a pending ask — collapsed waiters
                 -- resolve with it, partial quorum accepts stay audit-only, and whatever waited
                 -- resolves per the expiry behavior (a withdrawn approval is a no); the §5/§6.3
                 -- walks' close-with-audit-note is this mechanism system-applied (§8.10);
                 -- quorum: N distinct human accepts close it answered, deny wins immediately (§8.10);
                 -- quorum addressing: to = the rule's domain owner (primary recipient); the
                 -- eligible pool — that owner + active admins — evaluates at respond time (§8.10)
                 -- responses: the accept ledger behind N-of-M; collapsed_count: identical asks
                 -- folded into one canonical row — its answer resolves every waiter (§8.10);
                 -- escalate/reassign close the expired ask and open a linked successor ask (§8.10);
                 -- workspace_id keys the domain-owner escalation hop and digest grouping (§8.10);
                 -- respond re-validates payload assumptions — answers against a superseded
                 -- world are audit-only, a successor ask carries the decision (§8.10)
                 -- respond-door eligibility: a response from outside the ask's eligible set —
                 -- addressee, deputy, quorum pool — is refused at the door, the attempt
                 -- audited (§8.10)
                 -- event-side settlement: the event that terminally breaks a named
                 -- assumption resolves the ask at the event per its expiry behavior — a
                 -- quorum ask whose rule went terminal mid-wait, a domain's archive closing
                 -- its owner-addressed asks with an audit note — partial accepts staying
                 -- audit-only (§8.10, §4.4)
                 -- the terminal admin hop is a broadcast: every active admin addressed at once,
                 -- first valid response wins — a single-admin org the degenerate case (§8.10)
                 -- from: member-filed asks name their filer; plane-filed asks — goal-window,
                 -- stall, close-out, dependency, starvation, rebind, trip, upgrade,
                 -- activation, contradiction, quarantine, parity, storm-aggregate among
                 -- them, the filing event the rule, not the enumeration — carry the
                 -- reserved system originator: not a member row, never a target, never
                 -- response-eligible, rendered 'System'
                 -- (§8.10); withdrawal of a system ask belongs to the system's named closures
                 -- alone — the walks' audit-note settlement, the aggregate's recovery-or-ack
                 -- close, expiry per behavior — and the system withdraws no member's ask:
                 -- each side's retract is its own door
initiatives    (id, title, goal_ref?, decision_ref?, sponsor member, lead member,
                 status 'proposed'|'active'|'paused'|'closed', business_budget json?, deadline?,
                 closed_at?, depends_on json?)
                 -- sponsor: pinned human — an agent sponsor refused at the same write (§5.1);
                 -- lead: any member but an ephemeral worker — the §5.1 mortality guard, with
                 -- the eligibility refusals: viewer and non-active members refused (§5.1);
                 -- transition authority (§5.1): sponsor activation, lead/sponsor pause-resume-close,
                 -- admin backstop on pause/resume
                 -- goal_ref: a live goal at write — an initiative is never born pointed at
                 -- history (§5.1); the goal-end direction ask fires in every non-closed state,
                 -- and activation re-validates the reference: an accept against a goal that
                 -- died mid-wait is audit-only, the re-point successor ask carrying the
                 -- decision (§5.1, §8.10)
                 -- deadline optional: stall detection without one keys to the linked goal's
                 -- window, else the sponsor's staleness digest line (§5.1);
                 -- depends_on: cross-initiative DAG — acyclic, enforced at write, and its
                 -- edges name non-closed rows: a dependency on a closed initiative is
                 -- refused at write, the goal_ref liveness rule on the graph axis (§5.1);
                 -- closing an upstream with live dependents asks each sponsor — signal,
                 -- not block (§5.1)
board_tasks    + assignee_member_id?, initiative_id?  (runs carry initiative_id? the
                 same way — burndown, per-initiative digests)
                 -- assignee: any member but a viewer — and active at write, the sponsor/lead
                 -- guard (§5.1) extended to assignments; suspension freezes an assignee's tasks
                 -- (resume re-arms them), retire/offboard walks return them (§5, §6.3)
workspaces     + initiative_ids json?, domain_ids json?, node_id?, claim_epoch int default 0,
                 lease_expires_at?, participants json
                 -- participants: the member ids on this workspace's collaboration surface —
                 -- §4.4 'domain' DNA access derives its human reader set from the binding
                 -- through this list (a Coworker's reads derive from its workspaces directly);
                 -- the list evaluates against live state like named_readers (§4.4) — a
                 -- deactivated human or retired Coworker contributes nothing — and the walks
                 -- scrub it: §5 removes a departing human's entries, §6.3 drops a retiree's
                 -- bindings, defense in depth at both surfaces
                 -- initiatives bound here from activation (bound at spawn under an initiative,
                 -- admin-editable; pause retains the binding frozen and close drops it, §5.1);
                 -- the source of the §4.2 goal slice;
                 -- domain_ids: the ordered domain binding — the read path's applicability set
                 -- (§4.2), the spawn gate's hop (§6.2), and the ask router's escalation hop
                 -- (§8.10) all key on it; the first entry is the primary domain (first-bound,
                 -- admin-editable, §8.10); unbinding the primary promotes the next entry, an
                 -- empty list is a domainless workspace with the defined fallbacks (§6.2,
                  -- §8.10), and topology ops remap the list with ids stable (§4.4);
                  -- a pending §6.2 spawn approval keyed on this binding re-keys at the edit —
                  -- the gate the new primary derives, an admin once domainless — ids and
                  -- deadlines stable, creation-time addressing never stale (§4.4);
                  -- and binding writes serialize behind the affected domains' write locks
                  -- (§4.4): the admin edit and the archive walk join topology remaps and
                  -- multi-domain ingests in up-front, id-ordered acquisition — one writer
                  -- door for the binding rows, as for the content
                 -- node/epoch/lease: affinity placement + the fenced claim (§3)
                 -- lifecycle: workspaces archive, never bare-delete — runs and artifacts
                 -- are history; archival is a walked transition (§3, §4.4, §5.1, §8.10):
                 -- initiative bindings drop (the goal slice re-derives), domain reader
                 -- sets re-derive, the node claim dies with the row (the lease's terminal
                 -- case), new spawn bindings are refused (the domain-archive rule), and
                 -- workspace-keyed asks degrade to the domainless fallback — hop skipped,
                 -- digest tail — so no pending ask routes through a workspace gone from
                 -- live state; pending spawn requests binding to it archive with their
                 -- template pins drained — the initiative-close settlement on the workspace
                 -- axis (§5.1, §6.5): an approval landing after archival has no live row to
                 -- publish into, and a terminal act leaves no waiters; and the runtime that
                 -- launches into the workspace drains with
                 -- it: in-flight runs complete onto the archived slice as history — close's
                 -- drain (§5.1); a walk is a walk, never a kill — queued-but-unlaunched
                 -- runs close with an audit note (nothing half-starts on a dead surface),
                 -- the triggers and playbook schedules bound to the workspace re-point to a
                 -- successor workspace or disable with an audit note (§5.1's close-time
                 -- automation rule on the workspace axis), new runs join spawns in refusing
                 -- the archived row, and project memory (§8.3) archives inert with it — the
                 -- retire rule applied to the workspace's own tier: never transferred, never
                 -- injected
triggers       + criticality 'standard'|'critical' default 'standard'  -- §6.2 breaker trip order
playbooks      + criticality 'standard'|'critical' default 'standard'  -- with triggers (§6.2
                 -- breaker): a firing's class is the stricter of its trigger's and playbook's tags
spend_ledger   (id, member_id, run_id?, spawn_id?, kind 'reserve'|'settle'|'release',
                 tokens_in/out, cost, pricing_version, at)
                 -- reservation metering: caps evaluate reserved + settled; releases return
                 -- budget on failure or reap (§6.2)
trigger_firings (id, trigger_id, idempotency_key, fired_at, run_id?)
                 -- unique (trigger_id, idempotency_key) within the dedupe window
                 -- (default 7d — sized to cover provider redelivery after downtime, §8.5):
                 -- replays return the original run
external_writes (id, run_id, connector, op, idempotency_key, status 'prepared'|'committed'|
                 'compensated'|'failed', prepared_at, resolved_at?)
                 -- staged writes: prepare→confirm→commit (§8.2); stranded 'prepared' rows are
                 -- reconciled — confirm, compensate, or escalate; the reaper leaves these, not
                 -- half-posted side effects (§6.2)
data_holds     (id, kind 'member'|'domain', subject_id, reason_md, created_by, released_at?)
                 -- created/released through the §9 admin endpoints, audited;
                 -- legal hold freezes §4.5 erasure until admin release, audited;
                 -- kind 'domain' freezes the §4.5 history-rewrite remediation and db-only
                 -- export/deletion for that domain: sensitive material found in git cannot be
                 -- scrubbed out from under litigation
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
  revocation gates the next side effect rather than lingering to the run's end. Rules re-check at
  the same gate: rules carrying enforcement-bearing `machine_hint`s gate external writes exactly
  like scopes — a write the current applicable slice forbids is blocked and raises an ask, while
  purely narrative rules stay advisory context (§4.2 precedence). (d) Provider degradation: the
  model gateway queues with backoff instead of failing fast; headless runs wait out a bounded
  outage, and a sustained one raises a single critical admin ask — routing policy stays decision
  15 (§14), but no 24/7 run dies on a vendor blip.
- **8.2 Tools & MCP** — built-ins (`fs.*`, guarded `shell.exec`, `web.*`, `kb.search` → `dna.search`,
  `memory.write`) plus **`spawn`** as a guarded tool. Egress guard unchanged. Connector tiers:
  tier 1 = email/calendar/docs; **tier 2 = enterprise systems of record** (ERP/WMS/HRIS/CRM) —
  read-only first, writes gated behind `critical`-tier Asks (§8.10); per-connector scoped
  credentials via PATs, never shared service accounts; §4.6 governs what may enter the DNA.
  Write-capable tier-2 connectors implement staged writes — prepare → confirm → commit — every
  stage keyed by the idempotency key of its `external_writes` ledger row (§7): playbook retries
  and node retries reuse the key and cannot duplicate side effects, and a crashed or reaped run
  leaves a `prepared` row that reconciliation resolves — confirm or compensate per connector, or
  escalate to an admin ask where no compensation exists. Stage-less targets — an email send has
  no prepare — get send-once semantics: retry only on transport failure before the remote
  acknowledges; an ambiguous timeout after the wire degrades to an ask with the attempt audited,
  never a blind resend — at-most-once delivery where idempotency cannot be engineered. Reconciliation is scheduled, not
  improvised: a periodic pass — the steady-state twin of the §11 restore runbook — walks
  `prepared` rows past the grace window to a terminal state or an admin ask, so a stranded write
  cannot wait forever on a reaper that has already moved on.
- **8.3 Memory service** — now three-tier classifier (personal / project / DNA proposal) with the
  v1 machinery (dedupe, timeline, versions, secrets scanner) under it. Taint propagates through
  the tiers: memory written by a tainted run (§13) carries the flag, renders with its provenance
  when retrieved, is barred from digest pre-fills like tainted asks (§8.10), and is cleared only
  by explicit review — the spawner's owner for personal memory, the domain owner for project
  memory — never by the passage of time. A tainted memory item cannot be the sole support for an
  external write: pair it with an untainted source, or ask.
- **8.4 Skills** — unchanged; domain-organized packs; uninstall dependency checks.
- **8.5 Trigger engine** — schedule/API/event triggers unchanged; every firing is a run of the same
  session worker; API triggers gain PAT scopes for external callers. Missed schedules neither
  replay nor vanish: firings elapsing during a Coworker suspension, an initiative pause (§5.1),
  the §6.2 spend halt, or control-plane downtime coalesce into one catch-up run per trigger when
  the halt holding them lifts (resume, un-pause, or the trip ask's resolution), carrying a
  missed-schedule summary (count, window) — per-trigger policy `replay|coalesce|skip`, default coalesce, with §6.2 rate limits
  bounding a large backlog. Firings are idempotent at the boundary: every firing carries a
  deterministic key — schedule: trigger + scheduled time; webhook/API: event id or
  caller-supplied `Idempotency-Key`; event: source event id — and the `trigger_firings` table
  (§7) refuses duplicates within a configurable window (default 7 days), returning the original
  run — a default sized for redelivery, not just instant replay: webhook providers that back up
  during a control-plane outage redeliver on recovery, and the late copies meet the same dedupe
  as the immediate ones — an outage never converts one event into two side effects. A replayed
  webhook is one run, not two invoices.
- **8.6 Playbook engine** — DSL and sandbox unchanged; `worker()` targets any member (human targets
  create an assignment Ask; a viewer is refused at write like every ask target, §5); spawn-class playbooks (fan-out workers) built on §6 ephemeral workers
  · **initiative playbooks** (§5.1): an SOP instantiated under an initiative becomes the
  cross-domain spine — nodes route asks into each domain's escalation chain (§8.10) and artifacts
  land on the initiative's board slice. Instantiation is bounded like spawning: playbooks carry
  an instantiation depth cap (default 2, mirroring §6.2) and are cycle-checked at publish —
  direct or transitive self-instantiation is refused at save, and the runtime depth cap is the
  backstop; an orchestration loop cannot starve sandbox quotas underneath the spawn policy.
  Versioned references are pinned, not dangling: a run launches from the exact playbook
  version it was instantiated against, so in-flight instantiations complete on their pin
  through a later publication or retirement — the template-row rule's shape (§6.5) — and
  retiring a version refuses while live references hold it: triggers and schedules pointing
  at the version re-point or disable first, the §8.4 skill-uninstall dependency check applied
  to playbooks. An SOP pointer card left citing a retired version does not block retirement —
  it rides the §4.4 freshness pass and flags stale, a dangling reference surfaced to its
  owner, never silently followed into a ghost.
- **8.7 DNA engine** — inherits v1 KB machinery (ingest → chunk → embed → cards → hybrid retrieval →
  citations) extended with domains, proposals, review queue, and glossary/rule/goal-slice
  injection. An embedding-model switch (§14.7) is a migration, not a reset: the index records its
  model, the new index builds alongside the old, a recall-parity sample gates the cutover, and
  the old index serves until the new one passes — search never blinks. A gate that keeps
  failing is a decision, not a limbo: the stall surfaces as an admin ask carrying the parity
  deltas — roll forward, retune the sample, or stay on the old index — so a failed migration
  never becomes an eternal, silent shadow.
- **8.8 Groups & IM** — unified human+agent teams; IM pairing routes to a Coworker whose asks
  escalate to the channel.
- **8.9 Console screens** — v1 screens 1–9, plus five new: **10. Org & People** (members, RBAC, lineage graph,
  retirement flows, the custom-hire promotion action §6.5) · **11. DNA console** (browse cards/rules/decisions per domain, review queue
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
never hangs indefinitely. Withdrawal is the originator's side of the same coin: `from` may
retract a pending ask before it closes — collapsed waiters resolve with it, partial quorum
accepts stay audit-only — and the retraction applies the ask's expiry behavior to whatever was
waiting (a withdrawn approval is a no), so the §5/§6.3 walks' close-with-audit-note is this
mechanism applied by the system, not a parallel one. The system files asks as well as
settling them: the plane-originated asks the design leans on — goal-window, stall, close-out,
dependency, starvation, rebind, trip, upgrade, activation, contradiction, quarantine,
parity, the storm aggregate, among others, the filing event the rule rather than the
enumeration — carry a reserved system originator rather than borrowing a member's identity (a compliance ask filed
`from` the owner it watches would hand them the retract). The system originator is never a
target and never response-eligible, renders as 'System' in every digest, and its withdrawal is
only ever a mechanism that names its own closure — the walks' audit-note settlement, the
aggregate's recovery-or-ack, expiry per behavior — while a member's withdraw on a system ask is
refused at the door and the system retracts no member's ask: each side's retraction is its own.
A quorum-1 ask (the default) closes on the first response received — later
responses (member and deputy racing) are audit-only; a response to an expired ask is recorded but has no
  effect: the successor ask, if any, carries the decision — and a withdrawn ask is terminal the
  same way, a response racing the originator's retraction audit-only like one racing expiry.
  Eligibility is checked at the door itself: a response tendered by a member with no standing
  on the ask — neither the addressee, nor the addressee's deputy, nor, for a quorum ask, a
  member of the evaluated pool or a pool member's deputy — is refused at the respond endpoint
  with the attempt audited, never recorded as an answer; who may answer is part of the ask's
  contract, checked like what it answers.
  Responses re-validate before they bind:
  at respond time the ask's payload assumptions are recomputed — the diff still applies, the
  referenced DNA item is still live, the scope still holds — and a spawn approval names five
  more assumptions of its own: the initiative it files under still accepts launches (§5.1),
  the workspace it binds still accepts bindings and remains readable for the member it
  would publish — §4.2's spawn-time refusal, re-checked at the door the approval finally
  answers — and the requester itself is still `active`: an approval publishes a worker into
  its requester's live context, and lineage, fold-back, and quota all key on a spawner that
  can still receive them — and the ceiling it names still derives: `scopeCeiling` lands at
  activation as requested ∩ the requester's live scopes, an empty intersection archiving the
  request with pin and claims released (§6.2), the child never published above a narrowed
  parent — and the spend halt is not holding: a §6.2 trip refuses launches,
  and publishing a worker is one. So an accept racing a pause, a close, a workspace archival,
  the requester's own suspension, a narrowing that emptied the ceiling, or the breaker's trip
  is audit-only and the request archives
  with its template pin
  drained and its cap claims released, never a worker published into a frozen slice, onto an
  archived row, under a halted subtree, or past a tripped ceiling (§7's walk settles the pending request at archival;
  this is its racing half). Retirement and offboarding settle the request inside their walks
  (§6.3, §5); suspension — which resolves no dependents — is the case the walks deliberately
  leave standing, so the gate closes it at the door: the suspended spawner re-requests on
  resume, for resume never resurrects what the halt archived (§6.3). And a response against
  a superseded
world is audit-only like a late response, with a successor ask opened against current state (the
same machinery expiry uses). Settlement has an event-side twin: the respond door is not the
  only place a broken assumption is discovered — the event that terminally breaks one settles
  the ask at the event, the walks' leave-no-waiters contract applied to attention. A quorum
  ask whose rule went terminal mid-wait — superseded, lapsed, or close-lapsed with its
  initiative — resolves per its expiry behavior the moment the premise dies (an approval
  denies, fail-safe), partial accepts staying audit-only, the withdrawal algebra (§7), and
  the successor-ask machinery carries any decision the action still needs against current
  rules, the live slice re-derived: a pending ask is attention owed on a live question, and a
  question whose premise has died must not linger in every digest as answerable. Non-terminal
  states keep the deadline their resolver — suspension's request rule (§6.2) stands exactly
  as written, and a pool shrunk below N (below) denies at expiry rather than at the event
  precisely because a live pool can grow back; a terminal premise cannot, and that is the
  line: settle at the event only what the event ended. A domain's archive settles the same
  way — no resulting owner exists to re-key onto, so owner-addressed asks pending against it
  close with an audit note inside the event (§4.4). Provenance re-validates with them: an accept originating in a
tainted run (§13) is audit-only the same way — taint never becomes approval authority — and the
successor ask renders without a pre-fill (below) while it carries the decision to an untainted
reader. **Quorum asks**: rules may require N distinct approvals
(`machine_hint.requires_approvals` — §4.1's flagship "invoices > $10k require two approvals"
becomes expressible): the ask carries `quorum_required` (§7) and closes answered once N
distinct human members have accepted. Addressing is precise, not ambient: `to` names the pool's
primary recipient — the rule's domain owner, or its delegate (who joins the pool) when a
delegation routes the rule — and the eligible pool is that owner plus every
active admin (`deactivated_at` IS NULL, §7), evaluated at respond time: a pool that grows
mid-ask admits new acceptors, an acceptance's eligibility re-validates like every other
respond-time check, and an acceptance that already counted stands — the ask closes the moment
the Nth valid accept lands, so a later offboarding cannot reopen a closed decision. When N > 1,
only pool members' accepts count: a deputy's accept is audit-only there — for multi-approval
quorums the pool's own redundancy (owner plus admins) is the absence mechanism, and N approvals
means N pool principals, never one principal answering through two doors; a quorum-1 ask keeps
first-response-wins above, where a deputy may stand in for the one signature. A deny
still closes it denied immediately, expiry still denies, and a stale acceptance is audit-only
and does not count toward N. SLA breach escalates to the admin, who may contribute one of the
required approvals. N is bounded below and pool-checked at every write door:
`requires_approvals` must be ≥ 1 — a quorum of zero is the absence of an ask, not a rule —
and N is validated
against the pool it demands: a rule whose `requires_approvals` exceeds the eligible approvers
flags at proposal time like any contradiction (§4.4), and a pool that later shrinks below N
(offboarding) leaves the ask unanswerable — it denies at expiry, fail-safe, with the breach
escalation naming the shortfall: an impossible quorum degrades to a visible no, never a hang.
**Escalation chains**: every ask to a human carries member → deputy (set per member in the org
  registry; humans only — an agent deputy is refused at write, because standing approval authority
  for agents is exactly what the reviewed, windowed delegated rules below exist for; self-deputy,
  deputy cycles, and viewer deputies are refused the same way — a read-only member is never a
  standing answer hop) → domain owner (of the domain the ask's workspace
  belongs to; asks with no domain skip the hop; multi-domain workspaces hop to the primary domain
  — first-bound, admin-editable: one deterministic hop, not a fan-out to every owner) → admin,
  walked on SLA breach (inactive members are skipped; the walk carries a visited-set, so a
  mis-configured cycle ends the hop, not the walk — the §5 last-admin guard and the exhaustion
  broadcast remain the backstops). The admin hop is a broadcast, not a pick: every path that
  routes to "an admin" — this terminal hop, the §4.3 review-SLA escalation and sod publish
  routing, the §6.2 spawn gate's fallback — addresses all active admins at once and the first
  valid response wins (sod publish resolves inside the domain write lock, so racing admins see
  one winner). The broadcast's recipient set is itself live-derived, the quorum pool's rule:
  it renders and admits responses against the current active-admin set, so an admin added
  mid-wait joins pending broadcasts, and one departed mid-wait contributes nothing — the §5
  walk owes no reassignment for an ask no single member holds, the last-admin guard keeps the
  set from emptying, and a former admin's late response is refused at the eligibility door
  like any out-of-set answer (§7). A single-admin org is the one-recipient degenerate case,
  and the broadcast is not ambient authority — a member-addressed ask stays member-addressed
  until its own chain escalates. `deadline` derives from the tier unless set
  explicitly — and an explicit deadline earlier than the ask's creation is refused at write:
  a past deadline is a contradiction, not a tier, and never an instantly-expired ask (the §9
  window-sanity rule's attention-side twin). Chain exhaustion — the admin broadcast finds no active recipient, or breaches — expires the ask
  per its expiry behavior (an unanswered approval is a no, never a hang; an exhausted assignment
  returns the task to the board pool with a digest line — the board is an assignment's fallback
  surface, never a hang either) and broadcasts a
  critical-tier org-stall alert to every active human: the §5 last-admin guard keeps an admin
  from being *deactivated*, not from being *absent*; the broadcast is the backstop. The
  broadcast is an alert, not an ask: it renders to every active human — viewers included,
  read-only — because the never-an-ask-target guard (§5) governs members the org waits on for
  an answer and an awareness blast waits on no one; the ask-shaped exhaustion record behind it
  addresses answerable members only. **Batching**: the digest composer groups by initiative, then workspace, then an ungrouped tail — an ask carrying neither link (org-level admin asks, member-direct questions) still renders there, so no ask falls out of every digest — and pre-fills recommended
  answers — recommendations compute only from re-validated, untainted payloads: an ask originating
  in a tainted run (§13) renders without a pre-fill, so one-click accept is a convenience for
  trusted provenance, not an injection surface; approvals render as one-line accept/deny with diff links — reviewers see raw diffs,
  never agent-authored summaries alone. Storms collapse: identical pending asks — same kind,
  target set, payload hash — attach to one canonical ask as a `collapsed_count` within a window,
  the digest renders "37 identical escalations" as one line, and the canonical ask's answer
  resolves every collapsed waiter; a per-source ask-creation rate limit (per run, trigger,
  Coworker) sheds overflow into a single aggregate admin ask — the attention-side twin of the
  §6.2 circuit-breaker — and the aggregate has a lifecycle, not a permanent residence: it
  closes resolved when its source's creation rate falls back under the limit for a full window
  or an admin acknowledges it, the shed count preserved in the audit — the line that surfaces
  a storm never outlives the storm it surfaced. Digests compute per recipient: each human's timezone and working hours
  (§7) define their morning — `queue_until_morning` means the recipient's, not the server's (§3
  time authority). An unset calendar is still a calendar: humans with no timezone or working
  hours fall back to the control plane's zone and 09:00–17:00 weekdays — the digest always has
  a definite morning to compute. **Agent targets**: an ask routed to a Coworker queues into
  its next run (or wakes a session worker); if the target is anything but `active` — requested,
  retiring, suspended, or archived, the ephemeral states included via the §7 mapping — or is
  busy past SLA, the ask reassigns up the chain (§6.3 suspend re-routing included). The agent
  chain is the lineage chain: the first hop is the Coworker's `owner_human_id` (§2 — the chain
  always terminates at a human, so the hop is never undefined), and from there it continues down
  the human chain — deputy → domain owner → admin — with the same visited-set; an ask to an
  agent never lacks a human next hop. **Delegated authority** — a directive can push authority,
  not just work: the sponsor proposes a DNA rule scoped by `machine_hint` (initiative, ceiling,
  window) — "initiative X: store invoices ≤ $25k need one approval, by the lead, until
  2026-12-31" — reviewed like any rule. The ask router evaluates applicable rules, delegations
  included, when choosing approvers, so a static approval matrix doesn't route six months of store
  invoices through the same two people. When several delegated rules match one ask, the most
  restrictive ceiling wins and a contradiction report goes to the sponsoring owners. A
delegation may name an agent — persistent only: an ephemeral worker is refused at propose, the
§5.1 mortality guard (a reviewed grant never runs to a dying-by-schedule identity) — and
"by the lead" where the lead is one is precisely the reviewed,
windowed grant the agent-deputy refusal above reserves this mechanism for: the named agent is
the routed ask's primary recipient, answering through its session worker like any agent
target, and its accept binds the asks its rule routes — the rule's review is the authority,
and the accept is a run output carrying §13's taint rules like any other. Toward
multi-approval quorums the human-principals rule stands: an agent's accept is audit-only
there, exactly like a deputy's — a single signature may be delegated to a reviewed agent; a
quorum may not. The named recipient resolves at ask-creation time — the grant runs to the
post's current holder, and a non-active delegate reassigns by the standing chain rules,
never to a departed identity. Delegations end by window, supersession, or initiative
  close — rule semantics, not bespoke state: closing an initiative lapses every rule whose
  `machine_hint` scopes it to that initiative (status → `lapsed`, dropped from injection and routing).
  The grantee's own retirement is the fourth end: the §6.3 walk lapses an agent-named
  delegate edge with its grantee — the rule stands, routing reverting to its owner, the
  digest line the notice — while a post-named grant rides the post's re-pointing and
  suspension remains the transient the non-active reassignment covers. A reviewed grant
  never runs to a departed identity, exactly as no routing surface does.
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
POST /auth/pats · POST /auth/pats/:id/revoke  (PAT lifecycle: scoped create, expiry + rotation
               + last-used stamps, §10)
POST /org/bootstrap (first-run: create company + first admin; refused once any human exists —
               a transactional singleton guard, not check-then-act)
CRUD /org/humans · /org/members · GET /org/lineage
POST /org/humans/:id/erasure (admin; audited; honors data_holds — §4.5)
POST /org/humans/:id/offboard (admin; runs the §5 dependency walk; transactional last-admin guard)
POST /nodes/enroll (one-time token exchange) · GET /nodes · POST /nodes/:id/revoke
               (revocation surfaces the §3 rebind ask for every workspace bound to the node —
               a deliberate act is never a silent queue; in-flight runs halt with fold-back
               and §8.2 reconciliation; the node's claims die with the row)
               · PUT /nodes/:id (admin; region and metadata — capabilities are heartbeat-owned
               advertisements, not console-editable, §3; a region edit re-validates every
               residency-constrained placement bound to the node — §3, the node-side twin of
               §4.4's domain-edit rule)
CRUD /dna/domains · /dna/cards|rules|decisions|glossary|goals
               (domain-row authority, §7: create/archive, structural attributes — `store`,
               `sod`, residency — and owner re-pointing are admin writes; the owner edits
               access policy, `named_readers`, and `review_sla_days`; every row-write
               audited)
               (item-level CRUD is the publish path, not a side door around §4.3: every write
               lands inside the domain write lock with the §4.4 publish-time contradiction
               re-check, §4.3 sod routing, and the §10 secrets scan — an owner's direct write
               gets every guarantee a reviewed proposal's publish gets; and the surface is
               create / update / retire, never delete: citations, supersession chains, and
               provenance are the point, and §4.5 erasure is the only shredding path — with
               decisions the immutable exception: create-only, no update or retire, reversal
               or amendment a new record citing the old, §7; updates land on live states only —
               an owner's draft or an active item — while superseded/lapsed rules, terminal
               goals, and retired cards/glossary entries are frozen history (§7): correction
               and revival are new items citing or superseding the old, a rule's retire maps
               to window truncation (§7), and a draft discards by retiring; and the §4.5
               ingest sanity — window ordering, unique ids, machine-hint bounds (quorum N ≥ 1,
               §8.10) — is the same validation at every
               door: propose, amend, and item write run one check, so no door is softer than
               the git door)
POST /dna/proposals  POST /dna/proposals/:id/review (publish|reject) · POST /dna/proposals/:id/withdraw
               · POST /dna/proposals/:id/amend (revision during review, §4.3)  GET /dna/review-queue
POST /dna/domains/:id/split|merge|rename|archive (governed topology ops, §4.4; archive refuses
               a domain still holding live-set items — active rows and owner-staged drafts —
               live workspace bindings, or open proposals; terminal history never blocks —
               decisions included, lifecycle-free records (§7, §4.4) — staying with the
               archived row as read-only record (§7) — merge away first, or
               archive directly once only history remains; owner-addressed pending asks
               settle inside the event instead of blocking — closed with an audit note,
               attention dying with the domain's routing, §8.10)
CRUD /role-templates (versioned catalog, §6.5; create/publish/retire are admin writes,
               audited — authorship is infrastructure, adoption rides the §6.5 owner asks)
POST /spawn          GET /spawn/:id   (spawn requests; approval + spawn-storm monitoring; the
               requester's retraction rides the approval ask's withdraw endpoint, §6.2, §8.10)
POST /coworkers/:id/retire · /suspend · /resume   (lifecycle acts on the coworker, §6.3 — not the
               spawn request; authority: the Coworker's owner human, an admin, or a
               bound-initiative sponsor)
POST /coworkers/:id/promote  (catalog act, §6.5 — files the promotion ask for a customRole hire;
               authority: the hire's owner human or an admin; the ask snapshots identity files
               and effective scopes at creation, the accept names the placement — a new template
               or a new version of an existing one — and publishes the row active, pinning the
               hire as the founding instance only in a live activated state: active or
               suspended; requested/retiring/archived publish unpinned, the reference history)
CRUD /asks  ·  POST /asks/:id/respond  ·  POST /asks/:id/withdraw (originator retract, §8.10)  ·  WS: ask.requested, ask.answered
CRUD /initiatives · POST /initiatives/:id/activate|pause|resume|close
               (transition authority §5.1; close runs the §6.3 dependency check)
CRUD /board-tasks (assign to any ask-eligible member — viewer and non-active refused at write, §7)
POST /workspaces/:id/rebind (admin affinity failover; refuses a target node lacking the
               workspace's required capabilities, §3)
POST /workspaces/:id/archive (admin; runs the §7 walked transition — initiative bindings
               dropped with the goal slice re-derived, reader sets re-derived, node claim
               killed, pending spawn requests archived with their template pins drained,
               runtime drained, project memory inert — the endpoint and authority the walk
               presupposes)
GET /governance/policies|quotas|spend  (console screens 12 & 14)
               · PUT /governance/policies|quotas  (admin; audited — the org-global tunables'
               write surface; per-object settings ride their own CRUD — §4.3's SLA on the
               domain, §8.5's catch-up policy on the trigger — and §14's deferred parameters
               land here when decided; cap edits are claim-scoped, never retroactive over
               live claims — a spend ceiling tightened below live reserved+settled trips the
               §6.2 breaker loudly rather than contradicting its own ledger (§6.2))
POST /governance/spend/overruns/:id/ack (admin; lifts the §6.2 reserve gate an acknowledged
               settle overrun raised — :id is the overshot settle's spend-ledger row)
POST /governance/holds · POST /governance/holds/:id/release  (admin; audited — data_holds
               lifecycle; erasure (§4.5) and the hold-refused topology ops (§4.4) check it)
(v1 endpoints for coworkers, sessions, messages, workspaces, automated-tasks, triggers, playbooks, runs carry over)
```

---

## 10. Security & governance checklist

- Human authn (local accounts → SSO later) + RBAC; PATs hashed, shown once, scoped — and mortal:
  expiry (default 90d), rotation (create-replacement + revoke-old in one flow), a revoke
  endpoint (§9), and last-used stamps for compromise detection. They also authorize against
  live authority: a PAT's — or session's — effective scopes are the grant intersected with the
  principal's current RBAC, re-evaluated at every use, so a demotion narrows a standing
  credential at its next call instead of letting it outlive the role that authorized it — the
  credential-side twin of §6.3's status fence. Coworker credentials are
  status-fenced on top of mortal: they authenticate only while the Coworker is `active`,
  re-validated at every use (§6.3).
- **Admin lockout is recoverable by design**: a single-admin self-hosted org whose admin loses
  their credentials is not a bricked org — a server-local CLI reset flow (run on the host;
  physical/filesystem access is the recovery root of trust for self-hosted, mirroring §4.5's
  git-integrity stance) restores access, and every reset writes an audit entry. Degrade to a
  documented recovery, never to silence (§2).
- Agent scopes enforced in code (file scope realpath checks, tool allowlists, egress CIDR guard);
  every call audited; append-only audit log.
- **Scope delegation invariant** at spawn: child ⊆ parent, enforced by the policy engine.
- **DNA write policy**: agents propose, owners publish; compartment access enforced on retrieval;
  secrets scanner over all proposals, memory, and ingested direct edits (§4.5) — scanner hits
  quarantine to the owner with an
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
- DNA repo integrity: the control plane is the only direct writer, commits and refs are signed,
  non-fast-forward updates are refused, and PR workflows require protected branches (§4.5).
- Erasure & residency: erasure requests pseudonymize the append-only ledgers under legal-hold
  guards (§4.5); node regions + domain residency constraints govern placement (§3).

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
| **8a. v1 hardening** | v1 production-ready | Backup/restore with a reconciliation runbook (audit replay + node re-registration), encrypted secrets, docs, security review | 1 wk |
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
names the first system — an integration project per connector, not a phase. The enterprise seams
ride the same track: quorum asks, staged writes, and erasure/residency governance ship with the
first write-capable connector (§8.10, §8.2, §4.5); claim leases and fencing ship with Phase 6
node registration (§3).

**Restore runbook** (ships with Phase 8a): restore DB + DNA git to point T, then reconcile — the
audit log never rewinds (pre-restore segments are re-appended as a replay segment, so the
append-only property survives the restore); nodes re-register and report runs executed after T;
`external_writes` rows are rebuilt from node reports and connector-side idempotency-key queries
where supported; erasure events in the replayed segment re-apply, so a restore cannot resurrect
erased data; conflicts become admin asks. Tested in CI as a chaos scenario (§12).

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
  stale-epoch workspace claim is refused at the mediated boundary and reconciled on reconnect; a
  delegated-authority rule routes an approval to the initiative lead and expires cleanly;
  offboarding one human reassigns domains, asks, and initiatives, and the last-admin guard
  refuses the final admin.
- **P7**: spawn storm trips the circuit-breaker; a depth-3 spawn is refused by policy, not prompt.

---

## 12. Testing & quality

- **Unit**: scope delegation algebra (child ⊆ parent), spawn policy engine (quotas/depth/TTL),
  DNA proposal workflow states, goal-slice injection determinism, delegated-authority evaluation
  in ask routing, egress/path guards, scheduler math, memory 3-tier classifier, offboarding dependency walk (last-admin guard incl.
  racing offboards and self-demotion refusal, initiative reassignment, deputy clearing, session/PAT revocation, asks-from
  closure), domain split/merge id-and-chain invariants, merge attribute resolution
  (most-restrictive access, declared store, hold-refused migration) + post-op contradiction
  re-check + archive-refuses-items,
  template-upgrade scope re-derivation, escalation-walk visited-set (deputy cycles), deputy
  guard (agent, self, viewer, and cycle refusals), trigger catch-up coalescing, atomic quota claims under racing spawners, DNA
  store ingestion quarantine, topology-op write-lock serialization, ask respond-time
  re-validation, quorum accumulation (N distinct accepts, deny-wins, stale accepts don't count,
  pool-shortfall denial, pool-eligibility at respond time, deputy accepts audit-only toward N>1),
  spend reservation under racing runs, settle-overrun gating, separation-of-duties refusal, playbook cycle detection,
  alias-collision resolution order, injected-layer compartment filtering (rules/glossary/goals),
  org-scoped proposal routing to the admin queue, PAT expiry enforcement, erasure
  pseudonymization + hold blocking (incl. the DNA provenance sweep), initiative transition
  authority (sponsor activation ask, lead/sponsor pause-close, admin pause backstop,
  viewer/non-active sponsor-lead refusal, close-time workspace unbinding, depends_on cycle
  refusal), goal window-end slice drop and
  terminal-status exit, viewer assignee refusal,
  RBAC demotion walk (scoped eligibility shedding: asks reassigned and closed, assignments
  returned, deputies cleared both directions, goals and initiative posts re-pointed, domains and
  Coworkers transferred, personal assistants retired — transactional with the last-admin guard),
  write-time owner guards (viewer goal owner, non-owner/admin domain owner),
  glossary proposal routing (org-wide → admin queue), item-CRUD publish-path guarantees
  (lock serialization, sod routing, contradiction re-check), persistent-cap window rollover,
  domainless/multi-domain spawn-gate routing, workspace-domain binding semantics (ordered
  list, primary promotion on unbind, topology remap, archive refusing live bindings),
  cross-domain item-edit refusal at propose and write, amendment re-routing between
  org-wide and domain scope, domain-scoped goal inject-flag composition, intra-domain
  alias duplicate refusal, ephemeral-reap task and ask returns, personal-assistant 1:1
  spawn refusal, initiative close-drain ordering (durable state resolved, in-flight
  completes, new work refused), viewer no-write surface (propose, amend, ask, task, initiative,
  and spawn refusals at write), demotion transfer/withdrawal of authored proposals, agent-ask
  chain rooted at the lineage owner, ephemeral-origin proposal refusal, active-assignee guard
  (suspend freezes, retire/offboard walks return), pause-retained workspace binding (linked
  goals inject, runs and spawns refuse, close drops), denied-activation inertness, coworker
  lifecycle authority (owner human, admin, bound-initiative sponsor), firing-criticality
  composition (stricter of trigger and playbook), topology-op proposal remap (queues
  re-pointed inside the event, `review_by` clocks kept, archive counting open proposals),
  coworker credential fences (non-`active` PAT refused at auth, retire revocation,
  resume re-arm), paused-slice planning writes (task file/edit allowed, run/spawn
  refusal unchanged), amendment kind immutability, initiative reopen refusal (closed
  terminal, revive-as-new), item-CRUD delete refusal (retire only), glossary lifecycle (retire
  frees a term or alias for reuse, retired entries resolve read-only, duplicates refused only
  against non-retired entries), decision immutability (update/retire refused at every surface,
  reversal = new record citing the old), active-only search and injection (retired items absent
  from both yet resolvable by citation, drafts owner-visible alone), goal two-sided windows
  (admission at effective_from), settle-overrun ack lifting the reserve gate, template
  retirement counting pending spawn requests as pins, upgrade empty-intersection refusal,
  null budget_cap (uncapped worker, org ceilings still enforced), digest ungrouped tail
  (neither-link asks render), domain-access reader-set derivation (public/domain/named,
  owner-always-reads, admin governance reads audited, re-evaluation on unbind/remap), met-goal
  sponsor ask with outcome-tracked choices, sod publish routing to an active admin (single-owner
  schema), close-out ask on a deadline passed with no open work, retire archiving personal
  memory inert, db-only tree-path quarantine on ingest, hold-refused dissolution/archive/
  store-migration with rename and merge-into open, tainted-run accepts audit-only with an
  untainted successor ask, draft staging confined to cards and glossary, trigger/playbook
  criticality defaults, named-reader derivation (list-backed `named` access, ignored under the
  other policies, dead entries contributing nothing, §5 walk removal), spawn refusal of `draft`
  and `retired` templates, sponsor direction asks escalating on expiry — never denying — with
  the activation deny as the stated exception, retire-walk ask settlement (from-it closed with
  an audit note, spawn requests drained with their template pins, to-it re-routed via the
  non-active rule), merge undeclared-attribute persistence with the named-list floor (union
  only by declaration, split inheriting the parent's list), standalone store-flip migration and
  hold refusal, residency-edit placement re-validation, class-matched spawn-gate refusal,
  denied-request `requested`→`archived` transition, spawn-under-non-active-initiative refusal,
  assignment-exhaustion pool return, suspended-TTL halt-then-reap, rule item-retire
  truncation (effective_to pinned to now, lapsed transition, a predecessor staying
  superseded), frozen-history update refusal (superseded and lapsed rules, terminal
  goals, retired cards and glossary — draft discard riding retire), cross-domain
  supersedes_id refusal at propose and write, edit-proposal publish refused onto a
  retired target, admin-hop broadcast addressing (first valid response wins,
  member-addressed asks unpreempted pre-escalation, an unanswered broadcast as
  exhaustion), agent-sponsor write refusal, the offboard/demotion goal-walk terminal
  clamp, domain-name uniqueness among non-archived and template (class, name, version)
  key uniqueness, window-sanity validation identical at the propose, amend, and
  item-write doors, closed-slice task-filing refusal with proposed/paused planning
  open, git-ingest serialization behind the domain write lock (a valid hand-merge racing a
  publish or topology op queues, never interleaves), split-mapping chain-integrity refusal (a
  supersession chain divided across results refused at declare), proposal-amendment
  serialization (racing amendments land as sequential revisions, publish binds the pre-lock
  latest), spawn-request exact-row version pinning (newest-active console default, approval
  publishing the row the requester saw), publication-filed owner-upgrade asks, denied-upgrade
  pins standing on still-active versions, deadline-less initiative staleness (goal-window
  fallback, sponsor digest line at the 30-day default), heartbeat capability-drift surfacing
  (rebind-or-starvation, not per-run failure), decided_by provenance semantics (no eligibility
  guard on the field), ask withdrawal (originator retract applying the expiry behavior,
  collapsed waiters resolved, lifecycle walk closures as withdrawals), review-SLA derivation
  (review_by from dna_domains.review_sla_days, default 7d), governance policy/quota and
  node-update writes (admin-only, audited), delegated agent approvers (accept binds the routed
  quorum-1 ask, audit-only toward N>1, creation-time recipient resolution), org-stall
  broadcast addressing (viewers read-only, no waited-on response), staged-draft transfer on
  the offboard/demotion walks, stall-clock state coverage (proposed renders the line, pause
  suspends, close stops), ephemeral-holder write refusals (initiative lead, goal owner, named
  delegation agent, initiative origin — directive output folded back to the spawner),
  multi-domain lock ordering (overlapping merges acquire affected locks id-ordered up front
  and serialize without deadlock), spawn-approval expiry archiving (requested→archived with
  the template pin drained), org-scoped review_by derivation from the global default,
  sponsor-answer goal_ref re-pointing (extend re-windows the row, re-base/re-target swap it,
  the goal slice re-deriving), split totality and parent archival (an unmapped holding refused
  at declare, the emptied parent archived inside the event), held-domain split queueing behind
  release, domain-row write authority (structural attributes admin, compartmental attributes
  owner), template-catalog authorship as an admin surface, reap-walk ask settlement (to-it
  re-routed, from-it closed with an audit note), PAT live-authority intersection under
  demotion, residency at-rest attestation on set or tighten, quorum lower bound
  (`requires_approvals` ≥ 1) at every write door, withdrawn-racer responses audit-only,
  close-time automation re-pointing or disabling, node revocation with live bindings
  (rebind ask surfaced at revocation time, in-flight halt with fold-back, claims dying with
  the row), goal-end direction asks beyond `active` (pause suspending the escalation with the
  stall clock, proposed joining the activation ask, activation re-validating the goal_ref with
  an audit-only accept against a dead goal), goal_ref live-at-write (an initiative never born
  pointed at history, a re-point target riding the same liveness check), close-time
  spawn-request archival with template-pin drain, spawn-approval respond-time initiative
  re-validation (an accept racing pause or close audit-only, the request archiving),
  topology remap of owner-addressed asks (spawn-approval and quorum primary recipients
  re-keyed to the resulting owner, ids and deadlines stable), retire-walk upgrade-ask
  settlement (closed unresolved with an audit note, a racing accept audit-only with no
  successor, an accept on a suspended Coworker rebasing inertly), template class
  immutability (a class-flipping version refused at publish), workspace archival walk
  (initiative bindings dropped with the goal slice re-derived, reader sets re-derived,
  node claim death, spawn-binding refusal, workspace-keyed asks degraded to the
  domainless fallback), pause drain of in-flight runs (completing onto the paused slice
  while new launches still refuse), domain-owner re-pointing as an admin write, and
  monotonic expiry under a backward clock step (no ask, window, lease, or TTL
  un-expires, no terminal transition reverses), workspace-archive runtime drain (in-flight
  completion onto the archived slice, queued-launch closure with an audit note, trigger
  re-point-or-disable, project memory archiving inert), sponsor-addressed ask re-keying on
  the walks' sponsor re-pointing plus owner-addressed re-keying at every owner-re-point door
  (domain edit and §5 walk, not just topology ops), future-window supersession (a predecessor
  injecting until its superseder's effective_from opens), ephemeral-origin persistent-hire
  request refusal with spawner fold-back, re-own scope narrowing (current ∩ new-owner
  ceiling, empty-intersection retirement), the self-addressed approval gate (owner hiring
  into their own domain — one-click accept, quota/depth/budget still binding), ask-deadline
  sanity (a deadline before created_at refused at write), workspace-archive spawn-request
  settlement (pending requests binding to the workspace archived with their template pins
  drained, an accept racing archival audit-only — the respond-time workspace assumption:
  binding-accepting and member-readable, the §4.2 spawn-time refusal re-checked at the
  door), dependency-edge liveness (depends_on naming a closed initiative refused at write),
  activation-accept racing close (audit-only, the row staying closed — terminal beats
  activation), playbook-version retirement refusing live trigger and schedule references
  (runs pinning their launched version, SOP pointer cards flagging stale through the
  freshness pass instead of blocking), workspace-archive endpoint authority (admin,
  running the full §7 walk), reader-set input liveness (a deactivated participant or a
  retired Coworker's binding contributing nothing to domain-access reads, the walks
  scrubbing participants entries and group memberships), spawn-claim lifecycle (count-cap
  claims and budget reserves attaching at request creation, riding the pending row,
  releasing at denial, expiry, and archive-time settlement — an approval never publishing
  into an exhausted cap), the workspaceless hire gate (admin-routed like a domainless
  primary), supersession fork refusal (a second live edge onto a superseded predecessor
  refused at propose, amend, and item write, a head-naming successor landing), breaker
  un-trip only through the trip ask's resolution (no time-based release), org wind-down
  outside the offboarding guard (a deployment shutdown, never a headless org), spawn-approval
  requester-state re-validation (an accept racing the requester's suspension audit-only, the
  request archiving with pin and claims released — never a worker published under a halted
  subtree, re-request on resume), archive-holdings scope (live items, owner-staged drafts,
  live bindings, and open proposals blocking; terminal history never blocking, staying with
  the archived row as read-only record; a history-only domain archiving directly; merge
  moving the whole corpus with ids stable), hire-ownership derivation (the gate's accepting
  human owning the hire at activation, a re-keyed gate landing on the re-keyed addressee,
  ephemeral owners resolving to the first human up the spawned_by line), and
  group-leadership re-pointing on the walks (a departed Leader re-pointed, an unnamed
  successor degrading routing to an admin ask), event-side ask settlement (a quorum ask
  resolving per its expiry behavior the moment its rule goes terminal mid-wait, the
  successor ask carrying the decision; archive closing owner-addressed asks with an audit
  note), spawn-gate re-key on workspace-binding edits (a pending approval re-keyed inside the
  audited edit to the gate the edited `domain_ids` derives — the new primary's owner or the
  admin fallback — ids and deadlines stable), group-Leader write guards (viewer and
  non-active members refused at set, the ephemeral mortality pin) with demotion re-pointing,
  and cap-edit non-retroactivity (a tightened count cap refusing new claims while live
  claims run out, a spend ceiling tightened below live reserved+settled tripping the breaker
  with its ask), spend-halt launch-gating (runs in flight at a trip completing and settling,
  a spawn-approval accept under an active halt audit-only with the request archived and its
  claims released, the critical floor carrying critical-tagged firings only, never a hire),
  decision-archive interplay (a decision never blocking archive, an archived domain's
  decisions leaving search and resolving by citation, merge moving them with the corpus),
  upgrade-accept target re-validation (a version retired mid-wait leaving the accept
  audit-only, the pin standing), respond-door eligibility (an out-of-set response refused at
  the door with the attempt audited), review-queue re-keying on owner re-pointing (the queue
  rendering to the new owner at every door, `review_by` clocks untouched), the storm
  aggregate's close condition (rate recovery for a full window or admin acknowledgment, the
  count preserved in audit), and heartbeat-owned node capabilities (console node edits
  refusing capabilities, region and metadata only), custom-hire catalog promotion (the
  owner/admin ask snapshotting identity files and effective scopes with personal memory never
  carried; the accept publishing `active` with its named placement; name-version collision
  refusing the accept with the ask standing; class-flip refusal at the promotion door;
  founding-pin adoption at live activated states only — active, suspended — with requested,
  retiring, and archived publishing unpinned, the reference as history, and a hire activating
  after an unpinned publish staying unpinned; version-bump promotion filing upgrade asks to
  the pinned owners; `default_scopes` stored as a ceiling — future spawns still child ⊆ spawner; the
  founding pin re-deriving nothing, the upgrade algebra first applying at the next version's
  accept), binding-surface write serialization (an admin `domain_ids` edit and the
  workspace-archive walk acquiring every affected domain's lock id-ordered against a racing
  merge, the second writer re-reading the list inside the lock — no lost update either
  direction: no deliberate unbind overwritten by a stale remap, no dissolved id resurrected),
  multi-domain ingest lock ordering (one commit touching several domains' trees acquiring
  id-ordered before applying a line), class-matched spawn parameters (a `ttl` on a
  persistent-hire request refused at write), admin-broadcast live-set rendering (a mid-wait
  admin addition joining pending broadcasts, a departed admin contributing nothing, a former
  admin's late response refused at the eligibility door), org-snapshot liveness (a
  deactivated human and a retired Coworker absent from the injected layer, their records
  resolvable through decisions and audit), spend-halt timetable coalescing (an elapsing
  trigger deferring under the trip as one catch-up run per trigger, playing on the trip
  ask's resolution, critical-class launching in the floor's headroom throughout, total
  exhaustion coalescing everything), the system originator (plane-filed asks carrying the
  reserved principal, refused as a target and as a responder, the walks' closures its only
  withdrawal, a member's withdraw attempt on a system ask refused at the door, the system
  retracting no member's ask), org-snapshot state rendering (a suspended Coworker
  present-but-halted, a retiring one terminal-bound, a requested hire absent until
  activation publishes it), spawn-request retraction (the requester's withdraw on the
  approval ask archiving the request with its template pin drained and cap claims released —
  the denial/expiry settlement at the originator's own door), delegation-grant mortality
  (the retire walk lapsing an agent-named delegate edge with routing reverting to the owner,
  a post-named grant riding its post's re-pointing, suspension keeping the non-active
  reassignment as its transient), review-SLA edit monotonicity (tightening recomputing
  standing review_by earlier from the filed date, loosening never touching standing clocks,
  the ≥1-day bound refused at every write door), injection overflow demotion order
  (glossary before goals, 'linked' before 'always', narrative before enforcement-bearing,
  id-ascending ties — the org snapshot degrading to spine-plus-org-facts rather than
  truncating), and erasure's operational-history sweep (resolved asks —
  `from`/`to` and quorum response ledgers — and completed assignments pseudonymized with
  the event shape kept, pending state resolved by the prerequisite walk), retire-walk
  proposal settlement (a persistent Coworker's open DNA proposals withdrawn with an audit
  note inside the retirement, the folded-back learning available to the owner, suspension
  leaving them standing — publish open, amendment waiting), spawn-approval ceiling
  re-derivation (requested ∩ the requester's live scopes at the accept, a narrowing landing
  the child at the narrower ceiling, an empty intersection archiving with pin and claims
  released), template-name lineage reuse (a fully retired name reusable same-class only, a
  class-flipping reuse refused at create), and erasure's operational-prose annex (ask
  payloads, task descriptions, and run artifacts reported with the accountable human and the
  human call, identity fields pseudonymizing).
- **Integration**: agent loop against scripted mock models; DNA injection determinism (same domain →
  same rules in prompt); multi-node run scheduling and heartbeat loss; spawn storm → circuit-breaker; affinity node
  offline → runs queue, starvation ask at window, capability-less rebind refused; review-queue
  SLA breach → admin escalation; tainted-origin ask renders in the digest without a pre-fill;
  duplicate webhook → one run; staged-write crash → reconciliation row; rebind during partition
  → stale-epoch refusal + reconnect reconciliation; provider outage → queued run + a single
  admin ask; ask storm → collapsed digest + rate-limit shed; restore replay re-applies erasure;
  scope revocation emptying a workspace's readable domains → refused run + admin ask; stranded
  prepared write past grace → scheduled reconciliation resolves or escalates; suspend halts the
  ephemeral subtree with fold-back; ambiguous send timeout → ask, no resend; goal window end
  drops the slice and raises the sponsor ask; a secret pasted into a direct git edit
  quarantines on ingest; a workspace's initial bind to a capability-less node is refused; a
  residency constraint no node satisfies surfaces the starvation ask; suspend → resume leaves
  the halted run terminal (fold-back + reconciliation, no half-replayed side effect) while
  missed triggers coalesce; an in-flight run at initiative close drains onto the closed slice
  while new runs under it are refused; pause keeps the binding frozen — linked goals still
  inject while new runs and spawns are refused, and resume unfreezes in place; retire halts an
  in-flight run with fold-back and reconciliation, never mid-commit; a quarantined direct edit
  lands in the affected domain owner's queue; a suspended Coworker's PAT firing an API trigger
  is refused at auth, not at the run; a domain merge re-queues its open proposals to the
  survivor's owner with the SLA clock untouched; a webhook redelivered after control-plane
  downtime lands one run inside the 7-day dedupe window; an erasure sweep files its free-text
  mention annex instead of rewriting prose; a goal drafted before its window stays out of
  prompts until effective_from opens it; an admin's read of a restricted domain lands in the
  audit log; a merge-away under a kind-'domain' hold is refused until release; a `met` goal
  raises the sponsor's close-or-re-target ask while runs under the initiative wind down; a
  domain residency tightened under live placements rebinds conforming workspaces and surfaces
  the starvation ask for the rest; a standalone `store` flip sweeps the tree in one audited
  commit and is refused under a kind-'domain' hold; a retiring Coworker's pending spawn request
  closes with an audit note and releases its template pin; a valid hand-merge landing
  mid-topology-op serializes behind the domain lock; a split whose declared mapping divides a
  supersession chain is refused at declare; a node whose heartbeat drops a workspace-required
  capability surfaces the rebind ask; a withdrawn approval resolves its waiting run as a no; a
  deadline-less, goal-less initiative surfaces the sponsor staleness line; an admin's
  node-region edit under residency-constrained placements surfaces rebind-or-starvation for
  the nonconforming ones; a delegation naming an agent lead routes its approval through the
  agent's session worker and closes on the agent's accept; an expired spawn-approval ask
  archives its request and drains the template pin; merge A-into-B racing merge B-into-A
  serializes behind id-ordered locks, one completing after the other; a re-base answer
  re-points the initiative's goal_ref and the workspace goal slice re-derives on the next
  run; a split whose mapping leaves a holding unmapped is refused at declare, and a completed
  split archives its emptied parent inside the event; a split of a held domain queues behind
  the hold's release; a demoted human's standing PAT narrows to the new role at its next
  call; a repeatedly failing recall-parity gate surfaces its admin ask instead of serving
  silently; firings elapsing under an initiative pause coalesce and play on resume; closing
  an initiative disables or re-points the triggers whose runs launched under it, with an
  audit note; a reaped ephemeral's pending ask from it closes with an audit note while asks
  to it re-route up the chain; revoking a node with bound workspaces surfaces their rebind
  asks at revocation time and halts in-flight runs with fold-back and reconciliation; a goal
  going terminal under a paused initiative raises the sponsor ask suspended with the stall
  clock and playing on resume, and a sponsor's activation accept against a goal that died
  mid-wait is audit-only with the re-point successor ask carrying the decision; a spawn
  approval racing its initiative's close is audit-only, the request archiving with its
  template pin drained; a merge re-keys a pending spawn-approval ask to the surviving
  domain's owner with its deadline untouched; an owner-upgrade ask racing its Coworker's
  retirement closes unresolved while a racing accept stays audit-only; archiving a
  workspace drops its initiative bindings, kills its node claim, and degrades its keyed
  asks to the domainless fallback; runs in flight at an initiative pause complete onto
  the paused slice while new launches refuse; a backward clock step leaves every expiry
  standing; archiving a workspace with an in-flight run completes it onto the archived slice
  while its bound trigger disables with an audit note and its queued launch closes; a
  departing sponsor's pending activation ask re-keys to the re-pointed sponsor with its
  deadline untouched; a superseding rule published with a future window leaves its
  predecessor injecting until the window opens; an ephemeral worker's persistent-hire
  request is refused at write and folds back to its spawner; a re-owned Coworker's scopes
  narrow to the new owner's ceiling, an empty intersection retiring it; a domain owner's
  self-addressed hire approval closes on their own accept; an ask filed with a past deadline
  is refused at creation; archiving a workspace archives a pending spawn request bound to it
  and drains its template pin, while a racing accept stays audit-only; an initiative naming
  a closed dependency is refused at creation; a sponsor's activation accept landing after
  the lead closed the proposed initiative is audit-only and the row stays closed; retiring a
  playbook version with a live trigger is refused until the trigger re-points, an in-flight
  run completing on its pinned version meanwhile; offboarding a human removes their
  participants entries and group memberships with domain reader sets re-derived, and
  retiring a Coworker drops its workspace bindings; a denied or expired spawn request
  releases its quota claim and budget reserve for the next spawner; a proposal naming an
  already-superseded predecessor is refused while one naming the chain's live head
  publishes; the spend breaker's trip ask resolves the halt — an accept lifting it, a deny
  holding it — and the halt never lifts by itself; a spawn approval landing while its
  requester sits suspended is audit-only, the request archiving with its template pin
  drained and quota claim released; a domain holding only terminal history archives
  directly, its citations resolving read-only; a hire published on a domain owner's accept
  carries the accepter as its owner_human_id; and a group led by a retiring Coworker
  re-points its Leader inside the walk, an unnamed successor degrading routing to an admin
  ask; a quorum ask whose rule is superseded mid-wait closes at the event, its successor ask
  carrying the decision against current rules; archiving a domain closes its owner-addressed
  pending ask with an audit note inside the event; an admin unbinding a pending hire's
  primary domain re-keys its approval ask to the gate the new binding derives; and tightening
  the ephemeral quota under live workers refuses new claims while the live ones run out,
  while tightening the spend ceiling below live spend trips the breaker and raises its trip
  ask; a breaker trip landing mid-run lets the in-flight run complete and settle while new
  launches and a racing spawn-approval accept refuse until the trip ask resolves; a domain
  holding only decisions and terminal history archives directly, its decisions resolving by
  citation, and a merge moves decisions with the corpus; an upgrade accept arriving after its
  target version retired stays audit-only with the pin standing; a response from a member with
  no standing on the ask is refused at the door and audited; re-pointing a domain's owner
  mid-review moves its open proposals into the successor's queue with the SLA clock untouched;
  a storm's aggregate admin ask closes resolved when its source's rate falls back under the
  limit; and a console node edit attempting capabilities is refused, the advertisement
  re-stating on heartbeat; a promoted custom hire pinned at its accept receives the next
  version's upgrade ask like any pinned Coworker, its personal memory staying its own; an
  admin binding edit racing a domain merge serializes behind id-ordered domain locks, the
  merge remapping the list it re-read inside the lock; a hand-merge touching two domains'
  trees acquires both locks id-ordered before applying a line; a promotion accept landing on
  a still-`requested` custom hire publishes its template unpinned with the reference riding
  the audit event, and the hire's later activation leaves it unpinned; a spawn request
  carrying a `ttl` on a persistent class is refused at write; an admin added mid-wait sees
  the pending admin-broadcast asks in their inbox and may answer them, while a departed
  admin's late response is refused at the door; a departed human is absent from the org
  snapshot of every subsequently assembled prompt; a trigger elapsing under a tripped spend
  breaker plays its coalesced catch-up run when the trip ask's accept lifts the halt, while
  a critical-tagged trigger keeps firing in the floor's headroom throughout; a plane-filed
  goal-window ask renders 'System' as its originator and a member's withdraw attempt on it
  is refused at the door; a suspended Coworker renders present-but-halted in every org
  snapshot assembled while the suspension holds, and a requested hire renders nowhere until
  activation; a requester withdrawing their pending spawn-approval ask archives the request
  and releases its claims for the next spawner; retiring an agent named as a delegation's
  approver lapses the grant's routing edge and the next ask the rule routes addresses the
  owner; tightening a domain's review SLA moves its open proposals' review_by earlier while
  loosening leaves standing clocks untouched; a rules layer overflowing its token budget
  demotes narrative rules to the searchable index first while enforcement-bearing rules stay
  injected, and an org snapshot past its budget renders the routing spine with members
  demoted to the org-facts directory; and erasing a departed member pseudonymizes their
  resolved asks and completed
  assignments while the walks have already resolved the pending ones; retiring a Coworker
  mid-review withdraws its open proposals with an audit note and the reviewing owner's queue
  moves on, a suspended retiree-to-be's proposals still publishable meanwhile; a spawn
  approval landing after its requester's scopes were narrowed publishes the child at the
  re-derived ceiling, while one landing on a narrowed-to-empty ceiling archives the request
  with its claims released; creating a template on a fully retired name in the other class is
  refused at the door while a same-class reuse lands as a new version row; and erasing a
  departed member lists their name inside ask payloads and task descriptions in the annex —
  sponsor- or admin-owned per surface — while the addressing pseudonymizes.
- **E2E**: hire → chat → gated write approval → DNA proposal → review → next run uses the new rule;
  and directive → decision + goal → initiative → playbook fan-out → dependency-checked close →
  retrospective proposal.
- **Injection suite**: a tainted external document yields a DNA proposal that carries its taint
  flag; the reviewer sees the raw diff (never an agent summary alone); a tainted run cannot spawn
  ungated.
- **Golden runs**: "morning brief", "issue triage → fix → ask", and a "spawn ephemeral researcher →
  fold back report" flow replayed in CI with fake models.
- **Chaos-lite**: kill node mid-run; kill control plane with live nodes; restart → resumes cleanly,
  audit intact, no orphan spawns (reaper); fault-injection probes assert the §2 contract — an
  unanticipated state refuses the effect, writes audit, and raises an ask; never a silent
  failure.

## 13. Risks & mitigations

| Risk | Mitigation |
|---|---|
| DNA quality drift / gaming (agents proposing self-serving rules) | Human-owned review, provenance on every item, reviewer-agent contradiction reports, compartment isolation |
| Prompt injection via external content (email, web, ingested docs steering proposals, spawns, writes, ask answers) | Taint-tracking for off-platform content; provenance + raw diffs in the review UI; spawns from tainted runs auto-gated; tainted context barred from external writes; tainted-origin asks lose digest pre-fills and tainted-run accepts are audit-only; taint survives publication as a provenance flag and propagates through memory until explicitly reviewed (§8.10, §4.3, §8.3) |
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

### 13.1 Residual risk — accepted boundaries, deferred parameters

Five sweeps (v2.9–v2.13) closed the enumerated edge-case space inline; what v2.10 ranked and
v2.11 triaged, v2.12 designed, v2.13 audited and closed — and v2.14 closed the seams between
prose and schema that the fifth sweep's audit still left open; v2.15's seventh sweep closed the
remaining ownership seams — goal window-end semantics (§4.2), initiative transition authority
(§5.1), viewer assignees (§5), erasure's provenance sweep (§4.5), the transactional last-admin
guard (§5), suspended subtrees (§6.3), send-once delivery (§8.2), and the offboard endpoint
(§9); v2.16's eighth sweep closed the authority seams around them — last-admin demotion (§5),
ask-eligible initiative posts (§5.1), transient ask targets and deputy-vs-quorum counting
(§8.10), topology-op result declaration with dissolution-by-archive (§4.4), close-time
workspace unbinding (§5.1), initiative-DAG cycles (§5.1), and settle overruns (§6.2); v2.17's
ninth sweep closed the demotion and authority-flip seams beneath them — the RBAC demotion walk
(§5), ask-eligible goal owners and the domain-owner role guard (§7), glossary proposal kinds
(§7), the item-CRUD publish path (§9), secrets scanning on ingested edits (§4.5, §10),
spawn-gate routing fallbacks and cap windows (§6.2), initial-bind capability checks and
residency starvation surfacing (§3); v2.18's tenth sweep closed the representation and drain
seams beneath those — the schema-explicit workspace–domain binding with primary promotion and
topology remap (§7), archive refusing live workspace bindings (§4.4), cross-domain item moves
refused as edits (§4.4), amendment re-routing with the payload's scope (§4.3), goal inject-flag
composition (§4.2, §7), intra-domain alias duplicate refusal (§4.2), initiative close drain
semantics (§5.1), resume-without-resurrection (§6.3), ephemeral reap returning held state
(§6.3), and the policy-enforced assistant 1:1 (§6.4); v2.19's eleventh sweep closed the
authority-surface seams beneath those — the viewer's total no-write surface (§5), demotion
carrying authored proposals (§5), the agent-target ask chain pinned to the lineage owner (§8.10),
pause's retained workspace binding and inert denied-activation initiatives (§5.1, §7),
ephemeral-origin proposal refusal (§7), retire's halt semantics and named lifecycle authority
(§6.3, §9), playbook criticality in the schema (§7), quarantine routing (§4.5), the domainless
read-path layer (§4.2), org-wide glossary duplicate refusal (§4.2), and the active-assignee
guard (§7); v2.20's twelfth sweep closed the lifecycle seams at the surfaces' edges — open
proposals across topology ops (§4.4), credential fences on coworker suspend/retire (§6.3, §10),
paused-slice planning vs. execution (§5.1), amendment kind immutability (§4.3), terminal close
with revive-as-new (§5.1), and retire-not-delete item CRUD (§9); v2.21's thirteenth sweep
closed the corpus-state and post-hoc seams beneath those — glossary lifecycle with
retirement-as-freeing (§7, §9), decision immutability (§7, §9), the retrieval/citation split
with active-only search and injection (§4.2), two-sided goal windows (§4.2, §7), the
settle-overrun ack endpoint (§6.2, §9), template pins counting pending spawn requests plus
empty-intersection upgrade refusal (§6.5), erasure sweeping memory attribution with a
free-text mention annex (§4.5), redelivery-proof webhook dedupe (§8.5), null budget_cap
semantics (§7), and the digest's ungrouped tail (§8.10); v2.22's fourteenth sweep closed the
access-model and residual-semantics seams beneath those — defined domain-membership reader sets
with owner and audited admin reads (§4.4, §7), the `met`-goal sponsor ask (§5.1), sod's
single-owner admin routing (§4.3), criticality defaults (§7), draft-staging applicability
(§4.2), the finished-initiative close-out ask (§5.1), inert personal memory on retire (§6.3),
db-only tree-path quarantine (§4.5), hold-frozen dissolution with hold management endpoints
(§4.4, §9), and audit-only tainted accepts (§8.10); v2.23's fifteenth sweep closed the
residual-surface seams beneath those — the named-list schema home with live-state derivation
(§4.4, §7), the template-status spawn gate (§6.2), escalate-not-deny sponsor direction asks with
the admin pause backstop (§5.1), and the card default aligned to the glossary's (§7); v2.24's
sixteenth sweep closed the state-machine and gate-completion seams beneath those — retire-walk
ask settlement with pin drain (§6.3, §6.5), merge's undeclared-attribute algebra with the
named-list floor (§4.4, §7), the standalone store flip carrying the merge path's migration and
hold refusal (§4.4, §4.5), residency-edit placement re-validation (§4.4, §3), the class-matched
spawn gate (§6.2), the denied-request archive transition (§7), the active-only spawn gate
(§5.1), assignment-exhaustion pool return (§8.10), and suspension-deferred TTL reaping
(§6.2, §6.3); v2.25's seventeenth sweep closed the lifecycle-terminal and addressing seams
beneath those — rule retirement as window truncation over frozen superseded/lapsed history
(§7, §9), terminal-goal immutability with the walks clamped to active goals (§7, §5),
retirement-as-terminal for cards and glossary (§7), intra-domain supersession (§4.4, §7),
the publish gate's edit-target lifecycle re-check (§4.3), the broadcast admin hop (§8.10,
§4.3, §6.2), the write-enforced sponsor pin (§5.1, §7), domain-name and template-key
uniqueness (§7), one window-sanity validation behind every write door (§9), and the
closed-slice task-filing refusal (§5.1); v2.26's eighteenth sweep closed the single-writer,
catalog, and attention-lifecycle seams beneath those — write-locked external ingest (§4.5,
§4.4), split-mapping chain integrity (§4.4), amendment/publish serialization on the lock
(§4.3, §7), explicit template-version selection with publication-filed upgrade asks and
denial leaving the pin standing (§6.5, §7), the deadline-less initiative stall clock (§5.1),
heartbeat capability-drift surfacing (§3), `decided_by` as cited provenance (§7), and
originator ask withdrawal unifying the walks' closures (§8.10, §7, §9); v2.27's nineteenth
sweep closed the config-surface, delegation-authority, and broadcast seams beneath those —
the review SLA's schema home (§4.3, §7), governance/node write surfaces with region-edit
re-validation (§9, §3), agent-named delegations' accept semantics (§8.10), the org-stall
alert distinguished from an ask against the viewer guard (§8.10, §5), staged drafts riding
the walks (§5, §4.2), and the stall clock's state coverage (§5.1); v2.28's twentieth sweep
closed the holder-mortality, lock-order, and linkage seams beneath those — ephemeral holders
refused the posts that outlive them (lead, goal owner, named delegate) with ephemeral-origin
initiatives folded back to the spawner (§5.1, §7, §8.10), id-ordered multi-domain lock
acquisition for overlapping topology ops (§4.4), spawn-approval expiry as denial-with-drain
(§6.2, §7), the admin queue's default review SLA (§4.3), and sponsor-answer goal_ref
re-pointing (§5.1); v2.29's twenty-first sweep closed the split-completion, write-authority,
and schedule seams beneath those — total split mappings with the emptied parent archived
in-event and held-domain splits queued behind release (§4.4), domain-row and template-catalog
write authority pinned — structural admin, compartmental owner, authorship admin (§9, §7,
§6.5), the reap walk's both-direction ask settlement (§6.3), live-RBAC credential narrowing
on demotion (§10), residency's at-rest admin attestation (§3), parity-stall surfacing (§8.7),
the quorum lower bound at every write door (§8.10, §9), withdrawn-racer audit-only responses
(§8.10), and pause-coalesced schedules plus close-time automation resolution (§5.1); v2.30's
twenty-second sweep closed the revocation-mortality, goal-linkage, and close-drain seams
beneath those — node revocation with live bindings surfacing rebind asks at revocation time,
halting in-flight runs with fold-back and reconciliation, and killing claims with the row
(§3, §9), the goal-end direction ask extended to every non-closed initiative state with
activation re-validating the goal_ref and the linkage guarded live-at-write (§5.1, §8.10,
§7), and close-time spawn-request archival with the initiative named among spawn-approval
respond-time assumptions (§5.1, §6.2, §8.10); v2.31's twenty-third sweep closed the
attention-remap, holder-racing, and workspace-mortality seams beneath those — topology
remaps re-keying owner-addressed pending asks with ids and deadlines stable (§4.4, §8.10),
upgrade asks settling with the Coworker they name — closed unresolved at retirement,
rebased inertly under suspension (§6.3, §6.5) — template class immutability across a
name's versions (§6.5, §7), workspace archival as a walked transition degrading keyed
asks to the domainless fallback (§7, §3), pause as a launch gate that drains in-flight
runs (§5.1), domain-owner re-pointing as an admin write (§9, §7), and monotonic expiry
evaluation against backward clock steps (§3); v2.32's twenty-fourth sweep closed the
runtime-drain, re-key, and windowed-supersession seams beneath those — workspace archival
draining the runtime that launches into it (in-flight completion as history, queued-launch
closure, trigger re-point-or-disable, inert project memory, §7, §5.1), pending asks re-keying
with every post they address at every door the re-pointing has (sponsor posts on the §5/§6.3
walks, owner re-pointing across topology op, domain edit, and walk, §4.4, §6.2), the
supersession displacement edge pinned to the superseder's window opening — a future-windowed
successor a scheduled replacement, never a normative gap (§4.2, §7), ephemeral-origin
persistent-hire requests refused at write with spawner fold-back (§6.1, §6.2), re-owning
pinned as scope-narrowing with empty-intersection retirement (§5, §6.3), the self-addressed
approval gate named as the audited one-click it is (§6.2), and ask deadlines sanity-checked
at creation (§8.10); v2.33's twenty-fifth sweep closed the spawn-approval,
dependency-liveness, and reference-mortality seams beneath those — workspace archival
settling the pending spawn requests that bind to it with their template pins drained, and
the spawn approval's respond-time assumptions extended to its workspace — still
binding-accepting, still readable for the member it would publish, an accept racing
archival audit-only (§7, §6.2, §8.10), dependency edges naming live initiatives with a
closed-row dependency refused at write (§5.1, §7), the activation accept re-validating the
initiative's own state against a racing close — terminal beats activation (§5.1, §8.10),
playbook-version retirement refusing live trigger references with runs pinning their
launched version and SOP pointer cards riding the freshness flags (§8.6, §4.4), and the
workspace-archive endpoint with admin authority naming the walk's door (§9); v2.34's
twenty-sixth sweep closed the reader-set-liveness, spawn-claim, and chain-linearity seams
beneath those — participants and Coworker-binding inputs evaluated against live state with
the walks scrubbing participants entries, retiree bindings, and group memberships (§4.4, §5,
§6.3, §7), spawn-request quota claims and budget reserves pinned to the request row's
lifecycle with the workspaceless approval gate and the breaker's ask-borne lift named
(§6.2), supersession chains pinned linear with forks refused at every write door (§4.4, §7,
§9), and org wind-down named a deployment shutdown outside the last-admin guard's accident
scope (§5); v2.35's twenty-seventh sweep closed the requester-liveness, holdings-scope, and
owner-derivation seams beneath those — spawn approvals re-validating the requester's own
state, the suspension-raced accept archiving at the door (§8.10, §6.2, §6.3), archive's
holdings scoped to the live set with terminal history staying on as the archived row's
read-only record and merge moving the whole corpus (§4.4, §7, §9), hire ownership derived
from the gate's accepter and ephemeral ownership from the chain's first human (§6.2, §7),
and group leadership re-pointed by the walks (§5, §6.3); v2.36's twenty-eighth sweep closed
the assumption-settlement, gate-rekey, and leader-guard seams beneath those — events that
terminally break a named ask assumption settling the ask at the event, a quorum ask
resolving per its expiry behavior when its rule dies and a domain's archive closing its
owner-addressed attention with an audit note (§8.10, §4.4, §9), the spawn gate's pending hop
re-keyed at workspace-binding edits, the re-key's fourth door (§6.2, §7, §4.4), the
group-Leader post's write guards and demotion re-pointing (§5, §6.3), and claim-scoped,
non-retroactive cap edits with a loud breaker trip on a tightened ceiling (§6.2, §9); v2.37's
twenty-ninth sweep closed the spend-halt, record-mortality, and answer-authority seams beneath
those — the breaker's halt pinned as a launch gate at every door, in-flight runs draining to
their settles, a spawn-approval accept racing the trip archiving at the door, and the critical
floor carrying critical-tagged firings only, never a hire (§6.2, §8.10), decisions pinned
lifecycle-free history that never blocks archive and leaves search with their domain
(§4.2, §4.4, §7, §9), the upgrade accept re-validating its target version's status (§6.5, §7,
§8.10), respond-door eligibility refused outside the ask's eligible set with the attempt
audited (§7, §8.10), the review queue re-keying with owner re-pointing at every door, clocks
untouched (§4.3), the storm-shed aggregate gaining its close condition (§8.10), and node
capabilities pinned heartbeat-owned against console edits (§3, §7, §9); v2.38's thirtieth sweep
closed the custom-hire catalog seam beneath those — `customRole` hires promoting into the
versioned catalog through an owner-filed, admin-published ask: creation-time snapshot,
accept-named placement with collision and class-flip refusals, founding-pin adoption with the
retired-at-accept unpinned publish, and scope-ceiling/memory hygiene (§6.1, §6.5, §7, §9, §12);
v2.39's thirty-first sweep closed the writer-order, catalog-state, and live-rendering seams
beneath those — binding-surface writers (the admin `domain_ids` edit, the workspace-archive
walk) and multi-domain hand-merges joining the id-ordered domain write lock (§4.4, §4.5, §7),
the founding pin's state coverage at the promotion door (§6.5, §7, §9), class-matched spawn
parameters with the persistent-class `ttl` refused (§6.2), the admin broadcast's live-set
rendering with door-refused late responses (§8.10), and the org snapshot's live member set
(§4.2); v2.40's thirty-second sweep closed the timetable, originator, and state-rendering
seams beneath those — the spend halt's coalesced timetables playing on the trip ask's
resolution, the critical floor carrying critical-class throughout (§6.2, §8.5), the reserved
system originator for plane-filed asks with retraction kept to the system's named closures
(§7, §8.10), the org snapshot's state axis — suspended rendered present-but-halted, requested
absent until activation (§4.2) — and erasure extended to resolved asks and completed
assignments on the ledger's terms (§4.5); v2.41's thirty-third sweep closed the retraction,
grant-mortality, queue-clock, and ordered-degradation seams beneath those — the requester's
withdraw on a pending spawn approval as the request's own retraction terminal (§6.2, §7,
§8.10, §9), agent-named delegation grants lapsing with their grantee inside the retire walk
while post-named grants ride re-pointing (§6.3, §8.10), review-SLA edits re-deriving
standing clocks monotonically with the ≥1 bound at every door (§4.3, §7), and the injection
overflow's ordered demotion with the org snapshot degrading to spine-plus-org-facts (§4.2);
v2.42's thirty-fourth sweep closed the proposer-mortality, ceiling-liveness, catalog-naming,
and erasure-prose seams beneath those — the retire walk withdrawing a persistent Coworker's
open DNA proposals with an audit note, the §5 member-proposal rule's agent twin (§6.3, §4.3),
the spawn approval's ceiling re-derived against the requester's live scopes at the door,
landing requested ∩ current with an empty intersection archiving (§6.2, §8.10), template
names keying their lineage across retired rows with class-pinned reuse (§6.5, §7), and the
erasure annex extended to operational prose — ask payloads, task descriptions, run artifacts
(§4.5).
The former
residue — quorum approvals, external-write atomicity,
trigger idempotency, erasure vs. append-only ledgers, db-only reconstructibility,
check-then-spend races, rebind dual-writers, restore reconciliation, mid-run rule staleness,
ask storms, self-approval, cross-initiative dependencies, clock/calendar semantics, proposal
amendment, runtime precedence, taint decay, playbook recursion, git integrity, PAT lifecycle,
breaker collateral, authored proposals, embedding re-index, alias collisions — now has working
mechanisms in the sections the sweep changelogs cite. What remains is stated, not hidden:

- **Malicious insider — accepted boundary.** Governance treats humans as the trust anchor: a
  domain owner publishing a poisoned rule gets agents obeying it until audit catches up; nothing
  sits above the owner short of admin. The §4.3 separation-of-duties knob raises the cost; the
  boundary itself is the trust model, stated so nobody is surprised.
- **Single control plane — accepted boundary.** One control-plane instance is the design (§3's
  stack: one binary, SQLite WAL); its downtime is survived, not eliminated — runs queue, triggers
  coalesce (§8.5), leases hold to their fence and pause-and-resync on reconnect (§3), and
  recovery rides the §11 restore runbook. Multi-instance HA is a redesign beyond this plan,
  stated so nobody expects it silently.
- **Deferred parameters, not deferred designs.** Provider routing (§14.15) and lease intervals
  (§14.16) are decisions *over* designed mechanisms — §8.1(d)'s queue-and-ask, §3's fenced
  leases: the behavior exists; the tuning is organizational and lands with Phase 6 and the first
  24/7 rollout respectively.
- **The universal fallback.** For the space no enumeration covers, §2's ninth principle is the
  contract: refuse the effect, write the audit, raise an ask — no subsystem may fail silently or
  improvise a side effect. Handling every scenario does not mean predicting every scenario; it
  means no failure mode is silent, and §12's chaos + fault-injection suites exist to enforce it.

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
15. **Model-provider degradation**: single provider (default) with manual fallback vs. automatic multi-provider routing — queueing, bounded wait, and the single-outage critical ask are designed (§8.1(d)); the decision is the routing policy, to be made before the first 24/7 deployment leans on one vendor's uptime.
16. **Partitioned-node authority**: how long a node may act on cached scopes/DNA without a control-plane heartbeat — the fenced-lease mechanism (epoch claims, pause-and-resync, reconnect reconciliation) is designed (§3); the lease interval and reconciliation depth are the tunables — decide with Phase 6 node registration.
