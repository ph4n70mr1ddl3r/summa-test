# Changelog — Summa plan & spec suite

Version history for PLAN.md and the `specs/` suite. Newest first. Entries v2.1–v2.45
were extracted verbatim from PLAN.md's preamble in v2.46; from v2.46 on, history lives
here alone (PLAN.md carries only the current version stamp).

**Errata pass (v2.50)**: a full-suite review — PLAN.md and all 21 modules read against each
other for consistency, completeness, and unambiguity — closed six residues: DGV-014's
merge-access comparison is made total — `domain` vs `domain` joins the compared pairings
(two `domain`-access sides carry different participant sets), and a pair with no strictly
smaller side (each admitting members the other excludes, *or* the two evaluating equal)
refuses the default and demands a declared access — the equal-set outcome the
"strictly smaller wins" rule left undefined · the injection layer budgets gain their
parameter home — DRP-004's "soft, configurable" now names CFG-019 (org snapshot ~1k,
glossary ~2k, rules ~4k, goal slice ~1k), the residue class v2.48's parameter-citation
pass closed · DAT-120's `system` reservation names its second column — `audit_events.actor`
carries `member|'system'` per SPEC-16's own table, so "`asks.from` alone" was one short ·
SPEC-15's source line states §8.4 (Skills) as reference-only — the header skip 8.3 → 8.5
now explains itself in-module, where only TRACEABILITY's row did · two decision defaults
gained the citations PLAN already pointed at on its side (STG-001 → CFG-001; SUB-071 →
CFG-070, PLAN §8.7's §14.7 pointer). Lint green: 477 requirements, TRACEABILITY exact
both directions.

**Stack re-host (v2.49)**: the backend moves from Node 22 + TypeScript to **Java 25 LTS +
Spring Boot 4** (current major, fat-jar — one artifact) — a decided change landing as PLAN
amendment + spec delta per the change-control rule: ARC-005 and §3's stack bullet re-state
the spine — Java 25 LTS + Spring Boot daemon; React + Vite + Tailwind + shadcn console
(TypeScript) unchanged; SQLite (WAL) reached through sqlite-jdbc with sqlite-vec as a JVM
loadable extension; the playbook sandbox re-hosts `isolated-vm` → a sealed GraalJS polyglot
context (host access denied), the DSL unchanged and the child-process fallback kept; croner
→ Spring-scheduled cron triggers; MCP connectors via the official Java SDK; Tauri shell
unchanged · §3's "(unchanged from v1)" stack framing replaced with the re-host note, and
§8's "unchanged from v1" now names behavior over the new runtime (§8.6/SUB-060 carry the
sandbox clause) · DLV-040 re-aimed at the GraalJS sandbox (escape surface, stock-JDK vs
GraalVM JIT performance, child-process fallback); DLV-041 names the JVM extension load it
now validates · Phase 0's deliverable re-worded in both ladder tables (monorepo, Spring
Boot skeleton, sqlite-jdbc, TS-strict console) · "one binary" → "one process" wherever the
single-process shape is named (§2.2/PRN-002, VIS-007, NFR-021/§13.1) — a fat jar + JVM is
one process, not one binary, and the principle's meaning is the process shape. Lint green:
476 requirements, TRACEABILITY exact both directions.

**Errata pass (v2.48)**: a full-suite review — PLAN.md and all 21 modules read against each
other for consistency, completeness, and unambiguity — closed eight residues: `restricted`
gains the definition its four uses leaned on — DGV-002 names a restricted domain (access ≠
`public`), the term DGV-004's admin-read audit, CLC-052's rate limits, and §13's rows all
use — with §4.4 carrying the same parenthetical · the org-wide glossary gains a storage
home — the root `glossary.md`, git-backed alongside `goals/` because no domain row exists to
carry a `store` flag for org-scoped content (STG-001/002, §4.5's tree) · SPEC-16's
`workspaces` row gains `archived_at?` — the archive walk's terminal marker, DAT-010's
`deactivated_at` timestamp-is-the-whole-state pattern (CLC-040, §7) · `dna_proposals`'
ambiguous bare `at` splits into `created_at, reviewed_at?` — the filed date DWP-025
recomputes `review_by` from is now a named column (§7) · re-base and re-target, one
mechanism until now, gain their distinction — re-base re-issues the ended objective as a
new goal row, re-target swaps to a different goal (INT-051, §5.1) — and INT-071's
dependency re-base names its object (the edge re-pointed) · API-061's `gate` refusal code
gains the HTTP status its five siblings already had — 403 alongside eligibility, the status
coarse and `code` the fine grain · four parameter citations completed (SPW-001's TTL →
CFG-040; SPW-032's budget window → CFG-014; SUB-052's and DAT-101's dedupe window →
CFG-013). Lint green: 476 requirements, TRACEABILITY exact both directions.

**Errata pass (v2.47)**: a full-suite review — PLAN.md and all 21 modules read against each
other for consistency, completeness, and unambiguity — closed nine residues: the spawn
depth cap is one tunable, not two rules — SEC-011 and §10's checklist said "depth ≤ 2"
where §6.2/SPW-031 and §8.6/SUB-062 said default 2 — now CFG-018 (default 2), cited by
SPW-031 and SUB-062, with SEC-011 re-pointed at SPW-070 as the checklist's pointer to the
policy engine's home · the admin broadcast gained the representation DAT-120's keyed
member-reference rule left undefined: `asks.to` reserves the addressee `admins` — every
active admin, evaluated at render and respond time (ASK-055) — DAT-080 completing the
rule ASK-055's live-derived broadcast and ASK-101's aggregate admin ask presuppose ·
DRP-020's goal slice names domain-scoped `always` goals (the DNC-054 composition, §4.2's
own clause) instead of reading org-wide-only · INT-042's retrospective fallback drops
"lead is unset" — `lead` is required at write (INT-001, §7), non-active the only
fallback · two citations repointed to their REQ homes per the suite's cross-reference
convention (SPW-033 §6.4 → CLC-052; CLC-010 §5.1 → INT-041) · SUB-051's backlog "rate
limits" anchored to the runaway set (SPW-070) · SPEC-16's `dna_domains.status` regained
its `default 'active'` (§7 parity) · SPEC-21's Phase-6 deliverable says "agent
suspend/resume" (humans deactivate, they do not suspend) · TRACEABILITY's §13 row now
states where the unmapped risk rows' mitigations live (their cited modules). Lint green
under the new ID: 476 requirements, TRACEABILITY exact both directions.

**Review pass (v2.46)**: document architecture and tooling close the loop on v2.45's
review — PLAN.md history (v2.1–v2.45) moves to CHANGELOG.md, the plan gains a linked table of
contents, a version stamp, and a provenance note resolving the v1-baseline dependency (the
spec suite is the completion of record for the §7/§8/§9 deltas); §13.1's sweep recap yields
to a pointer (every entry preserved here) · spec delta: SPEC-16 gains the completion tables —
`groups` + `group_memberships`, `audit_events`, `pats`, `governance_settings`, `memory_items` —
and the member reference gets one keyed representation (DAT-120…125) · SPEC-17 gains the node
runtime surface (heartbeat, claims, work pull, run report — API-060) and the single refusal
envelope every 4xx answers in (API-061) · parameterization: storm-collapse window (default 1h,
CFG-015), per-source ask rate limit (default 60/h, CFG-016), org-wide concurrent agent cap
(default 100, CFG-017); ASK-061's "busy past SLA" and DGV-014's "most restrictive" given
computational definitions; INT-042 rewritten EARS-form; DLV-051's battery defined (versioned,
≥ 20 tasks, majority of scored metrics in ≥ 2 of 3 paired trials) · errata: fourteen
wrong-but-existing citations repointed to their normative homes — CLC-003 → TPL-040,
DGV-053 → STG-040, and the cross-reference audit's twelve more (SUB-051 coalescing ×3,
CLC-020 close check, ASK-061 lineage reassignment, CLC-051 mirrored scopes, ORG-022 rehire,
ARC-014 heartbeat ×2, DGV-041 archive reads, DLV-041 determinism spike, ASK-110 digests) ·
tooling: `tools/lint_specs.py` makes TRACEABILITY's "machine-checkable" claim
true — ID uniqueness, dangling citations, and coverage ranges verified in CI.

**Errata pass (v2.45)**: a full-suite consistency and completeness review — all 464
requirement IDs mechanically re-verified against TRACEABILITY (no dangling citations, no
duplicates, coverage exact) — closed the close-reading residue: §8.1's change count corrected
— the runtime's v2 changes run (a)–(d), not two · the suite's §4.6 attribution aligned —
module 04 is its home (DRP-050…052), so SPEC-07's header and the suite README no longer claim
it · two citations repointed to their normative homes (PRN-005 → SPW-030; DWP-032 → DWP-064) ·
SPW-034's claim-release enumeration completed with the requester's retraction, §6.2's list in
full · ASK-015's no-effect rule extended to withdrawn asks — a response racing the
originator's retraction is audit-only like one racing expiry (§8.10's rule) · TRACEABILITY's
§7 row now points to CLC-040's workspace-archival walk, and the suite README disambiguates
the lower-case namespace sense of "member".

**Rebrand (v2.44)**: the platform is **Summa**, and the AI members are **agents** — the term
the prose already leaned on ("a human or agent member") now the name itself: every former
"Coworker" in the plan and the spec suite becomes an agent, the schema with it (`coworkers`
table → `agents`, `/coworkers/:id/...` endpoints → `/agents/:id/...`), ephemeral workers
keeping their name, the spec suite's actors and module 12 retitled, and decision §14.8
resolved to the chosen brand. No requirement IDs change — CLC keeps its prefix (SPEC-12 is
the agent-lifecycle module), IDs being stable forever — and no semantics move: a rename, not
a redesign.

**Edge-case closure (v2.43)**: thirty-fifth sweep — staff-rekey, collapsed-retraction,
adoption-exclusivity, and cross-store seams closed inline: a pending owner-upgrade ask is
derived from the agent's `owner_human_id` — the staff-ownership post — and the §5
walks' re-owning re-keys it to the successor or admin custody inside the transfer, ids
stable and deadlines untouched, a response from the departing owner racing the transfer
refused at the eligibility door, the accept intersecting the re-derived owner's ceiling —
the §4.4 owner-derived re-key rule applied at the staff door, the upgrade decision landing
on the member who now owns the worker, never on the chain a departure walks (§5, §6.3, §7) ·
a retraction is originator-scoped, never communal: it resolves the retracting originator's
waiters alone, a collapsed canonical row surviving on the remaining originators with `from`
re-keyed and `collapsed_count` adjusted, only the last live originator's retraction closing
the ask per its expiry behavior — the answer communal, one decision for identical
questions; the retraction not, one change of heart per originator (§8.10, §7) · adoption
is one question at a time: a customRole hire with a live promotion ask refuses a second at
filing, the §6.4 assistant 1:1's pattern at the catalog door, so two accepts can never
race to pin one hire (§6.5, §9) · and a topology op whose sides differ in `store` runs
the store-change discipline — db-only content entering a git survivor demands the explicit
confirm, git entering a db-only survivor the one-commit sweep, either direction refusing
under either side's kind-`domain` hold, merge-into a held domain staying open exactly when
it moves no content across stores, a split result differing in `store` migrating its
mapped items the same way (§4.4, §4.5).

**Edge-case closure (v2.42)**: thirty-fourth sweep — proposer-mortality, ceiling-liveness,
catalog-naming, and erasure-prose seams closed inline: a retiring persistent agent's open
DNA proposals settle in the walk — withdrawn with an audit note, the §5 member-proposal
rule's agent twin (an agent never owns domains, so no transfer branch exists), the
folded-back learning available to the owner for re-proposal, and suspension leaving them
standing per the non-terminal rule — the review queue never waits on a departed proposer,
whatever member shape the proposer was (§6.3, §4.3) · the spawn approval's respond-time
assumptions gain their ceiling: `scopeCeiling` lands at activation as requested ∩ the
requester's live scopes — the §6.5 upgrade algebra at the spawn door — an empty intersection
archiving the request with pin and claims released, so child ⊆ parent binds the parent the
accept finds, never the snapshot the request filed (§6.2, §8.10, §7) · a template name keys
its lineage across live and retired rows: a new template reusing a fully retired name must
carry that name's class — class immutability spanning the lineage, not the live set — and a
role whose class genuinely changed takes a new name, the domain-name reuse rule's catalog
twin (§6.5, §7) · and the erasure annex covers operational prose: ask payloads, board-task
descriptions, and run artifacts naming the member are reported for the human
delete/rewrite/contest call like DNA prose — identity fields pseudonymize, prose reports
(§4.5).

**Edge-case closure (v2.41)**: thirty-third sweep — retraction, grant-mortality,
queue-clock, and ordered-degradation seams closed inline: the change of heart has the same
door every terminal has — a pending spawn request is retractable by its requester, the
approval ask's withdraw (its `from`) archiving the row with pin drained and claims
released, one settlement at every door (§6.2, §7, §8.10, §9) · a delegation's agent-named
grant dies with its grantee — the retire walk lapses the delegate edge, routing reverting
to the owner with a digest line, a post-named grant riding its post's re-pointing and
suspension keeping the non-active reassignment as its transient; window, supersession,
initiative close, and grantee retirement are a delegate edge's four ends (§6.3, §8.10) ·
the review SLA is bounded and monotonic under edit — `review_sla_days` ≥ 1 at every write
door, tightening recomputes standing `review_by` earlier and loosening never touches
standing clocks, §3's monotonic idiom at the queue door (§4.3, §7) · and injection
overflow is ordered, not discretionary — glossary, then goals ('linked' before 'always'),
then rules (narrative before enforcement-bearing), id-ascending ties, the org snapshot
degrading to its routing spine plus the org-facts directory rather than truncating (§4.2).

**Edge-case closure (v2.40)**: thirty-second sweep — timetable, originator, and
state-rendering seams closed inline: the spend halt is a timetable state, not only a launch
gate — schedules elapsing under the trip coalesce per the §8.5 machinery and play on the
trip ask's resolution, the critical floor launching critical-class firings throughout, total
exhaustion coalescing everything — pause's deferral rule extended to the money door, never a
silent drop nor a lift storm (§6.2, §8.5) · plane-filed asks gain their originator: a
reserved system principal — never a target, never response-eligible, rendered 'System' —
with withdrawal reserved to the mechanisms that name their closures, and each side's
retraction its own door; a compliance ask never files `from` the member it watches (§7,
§8.10) · the org snapshot carries state, not just membership: suspended renders
present-but-halted — the §8.10 non-active reassignment's rendering twin, never offered as a
destination — retiring renders terminal-bound, and a `requested` hire is absent until
activation publishes it, so what a prompt presents as answerable matches what the write
guards accept (§4.2) · erasure sweeps operational history: resolved asks — `from`/`to`
addressing and quorum response ledgers — and completed board-task assignments pseudonymize
with the audit and spend lines, event shape kept, identity link severed, pending state
pre-resolved by the §5 walk (§4.5).

**Edge-case closure (v2.39)**: thirty-first sweep — writer-order, catalog-state, and
live-rendering seams closed inline: the domain write lock's id-ordered discipline covers
every binding-surface writer — the admin `domain_ids` edit that re-keys the gate, the
workspace-archive walk that drops bindings and kills the claim, and a hand-merge touching
several domains' trees join topology remaps in up-front, id-ordered acquisition, so the
ordered binding list the gate hop, reader sets, and remaps key on has one writer at a time
and the second writer re-reads inside the lock — an edit racing a merge is a serialized
sequence, never a lost update (§4.4, §4.5, §7) · the custom-hire founding pin names its
state set — adoption at live activated states only (`active`, `suspended`); `requested`
would pin a row its own approval could yet archive, `retiring` and `archived` are
terminal-bound or terminal, each publishing unpinned with the reference as history, and a
hire activating after an unpinned publish stays unpinned (§6.5, §7, §9) · spawn parameters
class-match the template gate — a `ttl` on a persistent-hire request refused at write, a
hire never half-persistent, mortal by an unreviewed field (§6.2) · the admin broadcast
renders and admits responses against the live admin set — a mid-wait addition joins pending
broadcasts, a departure contributes nothing, a former admin's late response refused at the
eligibility door (§8.10) · and the org snapshot injects the live member set — departed
members leave the always-injected layer at the walk, their record living in decisions and
audit, no prompt carrying a departed member as present (§4.2).

**Edge-case closure (v2.38)**: thirtieth sweep — the custom-hire catalog seam closed inline: a
successful `customRole` hire is a candidate, not a dead end — its owner human, or an admin,
files a promotion ask snapshotting identity files and effective scopes at creation (the
proposal-payload pattern, §4.3) and addressing the admin broadcast (§8.10): catalog authorship
stays admin, adoption stays an owner ask · the accept publishes the row `active` with the
placement it names — a new template, or a new version of an existing one, the version path
filing upgrade asks to that template's pinned owners — and placement validates like every
catalog write: a name-version collision refuses the accept with the ask standing, and a class
flip refuses outright (§7's class immutability at its newest door — a custom hire is persistent
by construction, §6.1) · the accept pins the hire it promotes: a live hire — suspended included,
the §6.3 rebase rule, a pin being data, not execution — becomes the founding instance, later
versions' upgrade asks reaching it like every pin, while a hire retired before the accept
publishes unpinned, the founding reference audit and citation, the §5 terminal-clamp pattern at
the catalog door · the snapshot is the role, never the life — `default_scopes` stores effective
scopes as a ceiling (future spawns still child ⊆ spawner, §6.2; upgrades still new ∩ owner,
§6.5), personal memory never rides (§6.3), and the hire's own promotion-sense folds to its
owner as an ask (§6.1, §6.5, §7, §9, §12).

**Edge-case closure (v2.37)**: twenty-ninth sweep — spend-halt, record-mortality, and
answer-authority seams closed inline: the breaker's halt is a launch gate at every door —
runs in flight at the trip complete and settle onto the ledger, a settle overshoot tripping
the overrun gate as designed, and a spawn-approval accept landing under an active halt is
audit-only, the request archiving with pin and claims released, the pause-race rule at the
money door — while the critical floor carries critical-tagged firings only, never a hire
(§6.2, §8.10) · decisions never block archive — lifecycle-free records are history at
birth, they ride the archived row as citation history, leave search with their domain's
corpus, and merge-away moves them with it, ids stable (§4.2, §4.4, §7, §9) · an upgrade
accept re-validates its target version — a retirement that beat the accept is audit-only,
the pin standing, the next publication re-asking (§6.5, §7, §8.10) · a response from
outside the ask's eligible set — addressee, deputy, quorum pool — is refused at the door,
the attempt audited (§7, §8.10) · the review queue belongs to the domain, not the owner's
inbox — owner re-pointing at every door re-keys its rendering with `review_by` clocks
untouched (§4.3) · the storm-shed aggregate admin ask closes on rate recovery or
acknowledgment, the count preserved in audit (§8.10) · and node capabilities are
heartbeat-owned — the console node surface edits region and metadata, never the
advertisement (§3, §7, §9).

**Edge-case closure (v2.36)**: twenty-eighth sweep — assumption-settlement, gate-rekey, and
leader-guard seams closed inline: the event that terminally breaks a named ask assumption
settles the ask at the event — a quorum ask whose rule went terminal mid-wait resolves per
its expiry behavior with the successor machinery carrying the decision, and a domain's
archive closes its owner-addressed pending asks with an audit note, so no ask lingers
rendering answerable against a dead premise (§8.10, §4.4, §9) · the spawn gate's
creation-time hop never outlives the workspace binding it was read from — an admin edit of
a bound workspace's `domain_ids` re-keys its pending spawn approval to the gate the edited
binding derives, ids stable and deadlines untouched, the re-key's fourth door (§6.2, §7,
§4.4) · the group-Leader post gains its write guards — viewer and non-active members
refused at set, an ephemeral refused by the mortality pin, and the demotion walk re-pointing
a Leader the new role can no longer answer for (§5, §6.3) · and governance cap edits are
pinned claim-scoped, never retroactive — live claims run out, new claims refuse, and a
ceiling tightened below live spend trips the breaker loudly rather than contradicting its
own ledger (§6.2, §9).

**Edge-case closure (v2.35)**: twenty-seventh sweep — requester-liveness, holdings-scope,
and owner-derivation seams closed inline: a spawn approval names its requester's own state
among its respond-time assumptions — an accept racing the requester's suspension is
audit-only, the request archiving with its template pin drained and its cap claims
released, never a worker published under a halted subtree; retirement and offboarding
settle their requests in the walk, and the gate closes the non-terminal case the walks
leave standing (§8.10, §6.2, §6.3) · archive's holdings refusal is scoped to the live set
— active items, owner-staged drafts, live bindings, open proposals — while terminal
history never blocks: it stays with the archived row as the read-only record §7 always
named, a history-only domain archives directly, and merge moves the whole corpus with ids
stable, so §9's refusal and §7's archived row stop pulling against each other (§4.4, §7,
§9) · hire ownership gains its derivation: the persistent hire's `owner_human_id` is the
gate's accepting human at activation — a re-keyed gate landing on the re-keyed addressee —
and an ephemeral's is the first human up the `spawned_by` line, the §5 invariant derived
at spawn rather than merely checked (§6.2, §7) · a group's Leader post joins the walks —
re-pointed on departure or retirement, an unnamed successor degrading routing to an admin
ask — so no execution surface addresses a dead identity (§5, §6.3).

**Edge-case closure (v2.34)**: twenty-sixth sweep — reader-set-liveness, spawn-claim, and
chain-linearity seams closed inline: every reader-set input evaluates against live state —
a `participants` entry or agent binding of a deactivated human or retired agent
contributes nothing to `domain`-access reads, and the §5/§6.3 walks scrub the lists
(participants removal, retiree bindings, group memberships) the way they scrub
`named_readers` (§4.4, §5, §6.3, §7) · spawn-request claims are lifecycle-pinned —
count-cap claims and budget reserves attach at request creation inside the spawn
transaction, transfer at activation, and release at every terminal a pending request has
(denial, approval expiry, close-/archive-time settlement), so an approval never publishes
into an exhausted cap and cap space never leaks on a dead request; a workspaceless hire
routes its approval to the admin gate like a domainless primary; and the spend breaker
un-trips only through its trip ask's resolution — never by time (§6.2) · supersession
chains are linear, not forks: a second live `supersedes_id` edge onto an already-superseded
predecessor is refused at propose, amend, and item write — displacing a superseded rule
means naming the chain's live head, so a predecessor's displacer is always exactly one
rule (§4.4, §7, §9) · winding the company down is a deployment shutdown, never an
offboarding — the last-admin guard's refusal is the org model staying honest about its
human anchor, not a missing exit (§5).

**Edge-case closure (v2.33)**: twenty-fifth sweep — spawn-approval, dependency-liveness,
and reference-mortality seams closed inline: workspace archival settles the pending spawn
requests that bind to it — archived with their template pins drained, the initiative-close
settlement on the workspace axis — and a spawn approval names its workspace among its
respond-time assumptions: still binding-accepting, still readable for the member it would
publish, an accept racing archival audit-only, the request archiving, never a worker
published onto a row that refuses bindings (§7, §6.2, §8.10) · `depends_on` names live
rows — an edge naming a closed initiative is refused at write, the `goal_ref` liveness rule
on the graph axis, the only way an edge comes to address a terminal row being the upstream
closing under it, exactly the case the close-ask exists for (§5.1, §7) · an activation
accept re-validates the initiative's own state — an accept landing after a close that beat
it is audit-only, terminal beats activation, the spawn-approval's pause/close rule at the
activation door (§5.1, §8.10) · retiring a playbook version refuses while live references
hold it — triggers and schedules re-point or disable first, the §8.4 uninstall check
applied to playbooks, runs pinning the version they launched from, and SOP pointer cards
riding the §4.4 freshness flags rather than dangling silently (§8.6) · workspace archival
gets its endpoint and authority — admin, running the §7 walk (§9).

**Edge-case closure (v2.32)**: twenty-fourth sweep — runtime-drain, re-key, and
windowed-supersession seams closed inline: workspace archival drains the runtime that
launches into it, not just the bindings — in-flight runs complete onto the archived slice as
history, queued-but-unlaunched runs close with an audit note, workspace-bound triggers and
playbook schedules re-point or disable, and project memory archives inert with it (§7, §5.1,
§8.3) · pending asks re-key with every post they address, at every door the re-pointing has —
sponsor-addressed asks follow the §5/§6.3 walks' sponsor re-pointing, and owner-derived asks
re-key at the domain edit and the walk, not just the topology op (§4.4, §5, §6.2, §6.3) ·
supersession takes effect at the superseder's window opening — a future-windowed successor
is a scheduled replacement, never a normative gap (§4.2, §7) · an ephemeral worker is
refused the persistent-hire request at write, its recommendation folding back to the
spawner (§6.1, §6.2) · re-owning is scope-narrowing — transferred agents re-derive
against the new owner's ceiling, an empty intersection retiring (§5, §6.3, §6.5) · an
approval gate may address its own originator — the owner hiring into their own domain — the
ask the audit record (§6.2); and an ask deadline before its creation is refused at write
(§8.10).

**Edge-case closure (v2.31)**: twenty-third sweep — attention-remap, holder-racing, and
workspace-mortality seams closed inline: pending asks travel with their topology — an ask
whose `to` was derived from a domain's owner (the spawn-approval gate, a quorum ask's
primary recipient) is re-keyed to the resulting owner inside the audited event, ask ids
stable and deadlines untouched, the open-proposals rule extended to the attention surface,
while hops and pools already evaluate against live state (§4.4, §6.2, §8.10) · upgrade
asks settle with the agent they name — the retire walk closes an in-flight owner-upgrade
ask unresolved with an audit note, a racing accept is audit-only with no successor, and
suspension strands nothing: the rebase lands and resume re-arms (§6.3, §6.5, §8.10) · a
template's class is immutable across its versions — a class-flipping version refused at
publish; a role that changed class is a new template, the retire-and-respawn path (§6.5,
§7) · workspace archival is a walked transition, never a bare delete — initiative bindings
drop with the goal slice re-deriving, domain reader sets re-derive, the node claim dies
with the row, new spawn bindings are refused, and workspace-keyed asks degrade to the
domainless fallback (§7, §5.1, §3, §6.2, §8.10) · an initiative pause is a launch gate,
not a mid-run kill — runs in flight complete onto the paused slice exactly as close's
drain completes them (§5.1) · domain-owner re-pointing is an admin write, the §5 walks its
system-applied form (§9, §7) · the time authority is monotonic in effect: a backward clock
step never un-expires an ask, window, lease, or TTL, nor reverses a terminal transition
(§3).

**Edge-case closure (v2.30)**: twenty-second sweep — revocation-mortality, goal-linkage,
and close-drain seams closed inline: revoking a node with live workspace bindings surfaces
each bound workspace's rebind ask at revocation time — a deliberate act is a visible
configuration error, never a 24h silent queue a hopeless topology quietly endures — and
in-flight runs on the node halt the way suspension halts them (fold-back + §8.2
reconciliation, never mid-commit, terminal with no resurrection), the node's claims dying
with the row: revocation is the fenced lease's terminal case (§3, §9) · the goal-end
direction ask fires in every non-closed initiative state — under pause its escalation is
suspended with the stall clock and plays on resume, under `proposed` it joins the activation
ask on the sponsor's desk — activation itself re-validates the `goal_ref` it inherits: an
accept against a dead goal is audit-only, the re-point successor ask carrying the decision;
a new initiative's `goal_ref` names a live goal at write, and a re-point answer's target
rides the same liveness check — an initiative is never born pointed at history, and the
only way it comes to address a terminal row is the goal dying under it (§5.1, §8.10, §7) ·
closing archives the initiative's pending spawn requests with their template pins drained —
the retire walk's settlement applied at close — and a spawn-approval ask names the
initiative among its respond-time assumptions: an accept racing a pause or close is
audit-only, the request archiving, never a worker published into a slice that refuses
launches (§5.1, §6.2, §8.10).

**Edge-case closure (v2.29)**: twenty-first sweep — split-completion, write-authority, and
schedule seams closed inline: a split's declared mapping is total — every item, binding, and
open proposal names its result or the op refuses at declare — and the emptied parent
archives inside the same audited event, division pinned as dissolve-by-split; a held
domain's split queues behind the hold's release with archive and merge-away (§4.4) ·
domain-row writes get their authority home — create/archive and structural attributes
(`store`, `sod`, residency) admin, compartmental attributes (access, `named_readers`,
`review_sla_days`) owner — and template authorship joins them as an audited admin surface,
adoption staying with the §6.5 owner asks (§9, §7, §6.5) · the reap walk settles asks in
both directions — to-it re-routed up the chain, from-it closed with an audit note, the
retire-walk settlement's ephemeral twin (§6.3) · human PATs and sessions authorize against
live RBAC at every use — demotion narrows a standing credential at its next call, the
credential-side twin of §6.3's status fence (§10) · residency's at-rest half is an audited
admin attestation at set or tighten — the control plane's own hosting is declared, never
silently assumed (§3) · a repeatedly failing recall-parity gate surfaces an admin ask with
the deltas — never an eternal silent shadow index (§8.7) · quorum N is bounded below
(`requires_approvals` ≥ 1) at every write door, the one-validation rule's newest bound
(§8.10, §9) · a response racing an originator's withdrawal is audit-only, terminal like
expiry (§8.10) · schedules elapsing under an initiative pause coalesce per §8.5 and play on
resume, and closing re-points or disables the triggers and playbook schedules that launch
under the initiative — pause defers timetables, close answers the door (§5.1).

**Edge-case closure (v2.28)**: twentieth sweep — holder-mortality, lock-order, and linkage
seams closed inline: ephemeral workers are refused at write the posts that outlive them —
initiative lead, goal owner, named delegation agent — the agent-sponsor pin's twin: no
mid-life walk is asked to re-point what a dying-by-schedule member should never have held,
while the reap walk's task-and-ask returns stay the drain for what an ephemeral may
legitimately hold (§5.1, §7, §8.10) · ephemeral-origin initiatives join ephemeral-origin DNA
proposals in folding back to the spawner — a human or persistent agent opens the
directive (§5.1) · topology ops touching several domains acquire every affected write lock
up front, in domain-id order — overlapping merges serialize deadlock-free, "queue behind
each other" gains its mechanism (§4.4) · a spawn-approval ask that expires is the denial's
twin: `requested`→`archived`, template pin drained, the expiry the record (§6.2, §7) · the
admin queue's org-scoped proposals derive `review_by` from the global default — no domain
row governs them (§4.3) · the sponsor's terminal-goal answer moves the linkage with it:
extend re-windows the same row, re-base/re-target swap `goal_ref` atomically with the
answer, the goal slice re-deriving at once (§5.1).

**Edge-case closure (v2.27)**: nineteenth sweep — config-surface, delegation-authority, and
broadcast seams closed inline: the per-domain review SLA gets its schema home —
`dna_domains.review_sla_days` drives `review_by`, topology results inherit it (§4.3, §7) ·
governance policy/quota writes and node updates join the API surface, and a region edit
re-validates residency-constrained placements — the node-side twin of §4.4's rule (§9, §3) ·
a delegation naming an agent is the reviewed grant the agent-deputy refusal reserves this
mechanism for — the agent's accept binds the asks its rule routes, is audit-only toward
N>1, and resolves its recipient at ask creation (§8.10) · the org-stall broadcast is an
alert, not an ask — viewers receive it read-only; the never-a-target guard governs waited-on
answers (§8.10, §5) · owner-staged drafts ride the offboard/demotion walks — transferred with
the domain and surfaced to the successor, never orphaned invisible (§5, §4.2) · the
initiative stall clock's state coverage is pinned — `proposed` and `active` run it, pause
suspends it, close stops it; inert is not invisible (§5.1).

**Edge-case closure (v2.26)**: eighteenth sweep — single-writer, catalog, and
attention-lifecycle seams closed inline: external git ingest joins the domain write lock —
one writer door per domain, whichever side the write comes from (§4.5, §4.4) · a split's
declared item mapping respects chain integrity — a supersession chain maps whole to one
result, refused at declare (§4.4) · proposal amendment and publish serialize on the lock —
racing amendments land as sequential revisions, publish binds the pre-lock latest (§4.3, §7)
· template version selection is explicit — spawn requests name the exact catalog row,
publication files the owner-upgrade asks, a denied upgrade leaves the pin standing on a
still-active version (§6.5, §7) · a deadline-less initiative gets a stall clock anyway —
the linked goal's window, else a sponsor staleness line (§5.1, §7) · node capabilities
re-advertise on heartbeat — drift surfaces rebind-or-starvation, not per-run failures
(§3, §7) · `decided_by` is cited provenance, not authority (§7) · asks gain originator
withdrawal — resolving per the expiry behavior, the lifecycle walks'
close-with-audit-note its system-applied form (§8.10, §7, §9).

**Edge-case closure (v2.25)**: seventeenth sweep — lifecycle-terminal and addressing seams
closed inline: item-level retire on a rule is window truncation — `effective_to` pinned to
now, the row lapsing at that boundary; the rules enum carries no `retired` because lapse is a
rule's terminal (§9, §7) · frozen history is frozen at every surface — updates to
superseded/lapsed rules, terminal goals, and retired cards/glossary entries are refused;
correction and revival are new items citing or superseding the old, a predecessor stays
superseded when its superseder lapses, and a draft discards by retiring (§9, §7) ·
supersession is intra-domain — a cross-domain `supersedes_id` refused at propose and write;
topology ops move chains whole (§4.4, §7) · publish re-validation covers the edit target's
lifecycle — an edit proposal whose item retired mid-review refuses back to review (§4.3) ·
the admin hop is a broadcast — every path routing to "an admin" addresses all active admins
at once, first valid response wins, single-admin the degenerate case, and exhaustion is an
unanswered broadcast (§8.10, §4.3, §6.2) · the sponsor pin is a write guard — agent sponsors
refused at write (§5.1, §7) · the offboard/demotion goal walk clamps to active goals — a
terminal goal's owner reference is pinned history, severable only by erasure (§5, §7) ·
domain names unique among non-archived, role templates keyed unique on (class, name, version)
(§7) · the §4.5 ingest sanity runs at propose, amend, and item write alike — one validation,
every door (§9) · new board tasks join runs and spawns in the closed-slice refusal, while
`proposed` and `paused` keep task-filing open as planning (§5.1).

**Edge-case closure (v2.24)**: sixteenth sweep — state-machine and gate-completion seams closed
inline: the retire walk settles the retiree's asks — asks from it, pending spawn requests
included, close with an audit note and drain their template pins, while asks to it ride §8.10's
non-active reassignment; a terminal act leaves no waiters (§6.3, §6.5) · merge's undeclared
attributes persist from the surviving domain, and the `named` reader list keeps the survivor's
unless the op declares the union — never a silent widening; split's inherit-by-default hands
results the parent's list (§4.4, §7) · a standalone `store` flip through domain update runs
the same audited migration and hold refusal the merge path runs (§4.4, §4.5) · a residency
edit re-validates bound workspaces' placements — rebind or starvation ask, never silently
grandfathered (§4.4, §3) · the spawn gate is class-matched as well as status-matched (§6.2) ·
a denied spawn request archives without activating — the status enum's missing denial
transition (§7) · only `active` initiatives launch spawns, the twin of the paused-slice
refusal (§5.1) · an exhausted assignment ask returns the task to the board pool (§8.10) · a
TTL lapsing under suspension halts-then-reaps (§6.2, §6.3).

**Edge-case closure (v2.23)**: fifteenth sweep — residual-surface seams closed inline: the
`named` access policy's reader list gains the schema home its §5 walk always presupposed —
`named_readers` member ids, ignored under the other policies, derived from live state so dead
entries read nothing and rehire re-admits no one (§4.4, §7) · spawn requests gate on template
status: `active` only — draft is authoring state, retired is history, refused at request time,
the other half of §6.5's pin-drain (§6.2) · the sponsor's direction asks — stalled-work,
close-out, goal-window, terminal-goal — escalate on expiry and stay pending in every digest,
never a silent no; the activation ask's deny stays the deliberate exception (§5.1, §8.10) · an
admin holds initiative pause/resume as emergency backstop — the §6.3 authority pattern applied
to the initiative itself (§5.1) · card status defaults `active` like the glossary's — an
owner's direct create is the publish path, draft an explicit stage (§7).

**Edge-case closure (v2.22)**: fourteenth sweep — access-model and residual-semantics seams
closed inline: domain membership is defined — public/domain/named reader sets derive from
workspace participation, the owner always reads what they own, and active admins hold audited
governance reads everywhere the escalation, sod, and custody paths already hand them content
(§4.4, §7) · a `met` goal fires the §5.1 sponsor ask like every other terminal transition,
choices tracking the outcome — an initiative never executes on toward a goal that has already
ended (§5.1) · sod's "second owner" was unrepresentable — publish routes to an active admin,
the single-owner schema's one alternative publisher, and the single-admin collapse is that
rule's degenerate case (§4.3) · a deadline passed with no open work still asks — a bulk-tier
close-out ask to the sponsor, so finished initiatives never linger on their bindings (§5.1) ·
a kind-`domain` hold refuses dissolution, archive, and store migration — rename and merge-into
stay open — and holds gain their management endpoints (§4.4, §4.5, §9) · a tainted run's ask
accepts are audit-only with an untainted successor — taint never becomes approval authority
(§8.10, §13) · draft staging is pinned to the schemas that carry it — cards and glossary stage
as drafts, rules and goals through future effective windows, decisions never (§4.2) · trigger
and playbook criticality default `standard` (§7) · a retired agent's personal memory
archives inert with it, never transferred (§6.3) · paths under a db-only domain's tree
quarantine on ingest — one canon, not two (§4.5).

**Edge-case closure (v2.21)**: thirteenth sweep — corpus-state and post-hoc seams closed
inline: glossary entries gain the status the "live entry" duplicate check always presupposed —
retire is their item-CRUD surface, freeing terms and aliases as resolvable history (§7, §9) ·
decisions are pinned immutable: create-only at every surface, reversal or amendment is a new
record citing the old (§7, §9) · retrieval splits from citation — search and injection serve
active items only, retired ones resolve read-only, drafts stage to their owner alone, and
decisions, lifecycle-free by design, are always live (§4.2) · goal windows are two-sided —
admission at `effective_from`, exit at `effective_to` (§4.2, §7) · the settle-overrun reserve
gate gains its admin ack endpoint (§9, §6.2) · template retirement counts pending spawn
requests as live pins, and an upgrade whose scope intersection comes back empty refuses to
land (§6.5) · erasure sweeps memory attribution and reports free-text mentions for admin
judgment — never silently kept, never silently rewritten (§4.5) · webhook redelivery after
downtime dedupes on a defaulted 7-day window — an outage never converts one event into two
side effects (§8.5) · a null `budget_cap` is defined — worker-uncapped, org ceilings still
bind (§7) · digest grouping gains its ungrouped tail, so every ask has a place (§8.10).

**Edge-case closure (v2.20)**: twelfth sweep — lifecycle seams at the surfaces' edges closed
inline: open proposals travel with their domain — merge/split/rename remap in-review proposals
to the resulting queues inside the audited event with `review_by` clocks running, and archive
counts them among the holdings it refuses (§4.4, §9) · agent lifecycle acts are credential
fences — PATs and sessions authenticate only while `active`, re-validated at every use; retire
revokes outright, the §5 credential-death (§6.3, §10) · pause freezes execution, not
deliberation — tasks may be filed and edited on the frozen slice as planning, runs and spawns
still refuse (§5.1) · proposal revisions re-route by scope but never change `kind` — a queue
is never handed a payload shape it wasn't routed to review (§4.3) · `closed` is terminal —
revival is a new initiative referencing the old decision, never a rewrite (§5.1) · item-level
DNA CRUD is create/update/retire, never delete — erasure stays the only shredding path
(§9, §4.5).

**Edge-case closure (v2.19)**: eleventh sweep — authority-surface seams closed inline: the viewer
role is a total no-write surface — never a target, assignee, or originator: propose, amend, ask,
task, initiative, and spawn writes are all refused at write (§5) · demotion carries authored
proposals with the authority — transferred for shed domains, withdrawn when the role can no
longer propose, mirroring offboarding (§5) · the agent-target ask chain is pinned to the lineage
owner, then the human chain — an ask to an agent never lacks a human next hop (§8.10) · pause
retains its workspace bindings frozen — linked goals keep injecting while runs and spawns refuse;
close still unbinds; a denied activation leaves the initiative `proposed` and inert (§5.1, §7) ·
ephemeral-origin DNA proposals are refused at write — learning folds back, the spawner proposes
(§7) · retire is a halt like suspend — fold-back + reconciliation, never mid-commit — and
suspend/retire/resume authority is named: the owner human, an admin, or a bound-initiative
sponsor, reconciling §5.1's sponsor stop with §6.3's emergency stop (§6.3, §9) · playbook
criticality joins the schema, and a firing's class is the stricter of trigger and playbook tags
(§7, §6.2) · ingest quarantine routes to the domain's owner — never an unowned inbox (§4.5) ·
the domainless workspace has a defined read-path layer, and org-wide glossary duplicates are
refused like intra-domain ones (§4.2) · board-task assignees must be active at write — suspend
freezes assignments, retire/offboard walks return them (§7).

**Edge-case closure (v2.18)**: tenth sweep — representation and drain seams closed inline:
the workspace–domain binding — the read path's applicability set, the spawn gate's hop, and
the ask router's escalation hop all key on it — is schema-explicit: ordered `domain_ids`,
first entry primary, unbinding the primary promotes the next, an empty list is domainless
with the defined fallbacks, topology ops remap (§7) · archive refuses live workspace
bindings; the merge step remaps them, ids stable (§4.4, §9) · cross-domain item moves are
topology ops only — edit proposals and item-level CRUD refuse a `domain_id` change (§4.4) ·
proposal amendments re-route with their payload: the reviewing queue matches the amended
scope (§4.3) · the goal `inject` flag composes with scope — a domain-scoped 'always'
injects wherever its domain is readable (§4.2, §7) · intra-domain glossary duplicates are
refused at write; the resolution order stays cross-domain (§4.2) · initiative close drains:
in-flight runs complete onto the closed slice, new work is refused, urgent stops go through
suspend/retire (§5.1) · resume re-arms triggers and launches new runs but never resurrects
a halted one — fold-back plus §8.2 reconciliation, no half-replayed side effect (§6.3) ·
the TTL reaper returns open task assignments and re-routes in-flight asks, not just memory
(§6.3) · the personal-assistant 1:1 is enforced by the policy engine, not implied (§6.4).

**Edge-case closure (v2.17)**: ninth sweep — demotion and authority-flip seams closed inline:
RBAC demotion is a walked transition, not a label flip — an edit that reduces a role runs the
§5 dependency walk scoped to the new role's carrying capacity, inside the last-admin guard's
transaction (§5) · goal owners are ask-eligible at write — viewer humans refused, agent owners
keep the §4.2 admin-routing fallback (§7) · glossary proposals are expressible — `kind
'glossary'` joins the enum, closing the §4.2 admin-queue routing onto the schema (§7) ·
item-level DNA CRUD is the publish path, not a side door — same domain write lock,
contradiction re-check, sod routing, secrets scan (§9) · the secrets scanner covers ingested
direct edits, not just proposals and memory (§4.5, §10) · the persistent-hire spawn gate
falls back to admin for domainless primary workspaces and to the primary domain for
multi-domain ones (§6.2) · budget-cap windows match worker class — per-worker lifetime for
ephemeral, periodic window for persistent (§6.2) · initial workspace–node binding is
capability-checked like rebind, and a residency constraint no node satisfies is surfaced
through the same starvation ask, not a silent queue (§3).

**Edge-case closure (v2.16)**: eighth sweep — authority seams closed inline: the last-admin
guard covers demotion, not just deactivation — an RBAC edit cannot leave the org headless (§5)
· initiative sponsors and leads are ask-eligible at write — viewer and non-active members
refused, like ask targets and assignees (§5.1, §7) · ask targets in any non-active state
(requested, retiring) reassign up the chain (§8.10) · deputy accepts are audit-only toward
multi-approval quorums; a delegated quorum rule addresses the delegate as primary recipient
and pool member (§8.10) · topology ops declare their resulting domains — owner/access/store/sod/residency —
with most-restrictive access on merge, hold-refused store migrations, and a post-op
contradiction re-check inside the lock; dissolution is merge-away-then-archive, never bare
delete (§4.4, §7, §9) · closing an initiative unbinds its workspaces — the goal slice
re-derives at once (§5.1) · `depends_on` is acyclic — cycles refused at write (§5.1) · a
settle that overshoots its reservation settles in full, surfaces, and gates further reserves
until acknowledged (§6.2).

**Edge-case closure (v2.15)**: seventh sweep — ownership and lifecycle seams closed inline:
goal window-end semantics pinned — expired goals leave the slice, the sponsor ask carries the
outcome, terminal goal transitions re-open it (§4.2, §5.1) · initiative transitions are owned —
sponsor activation (ask-routed, deny at expiry), lead/sponsor pause-close, lifecycle endpoints
added (§5.1, §9) · viewers are never task assignees, like ask targets (§5, §7) · erasure sweeps
DNA provenance, not just ledgers; the storage tree gains domain-scoped goals and the db-only
split (§4.5) · the last-admin guard is transactional — racing offboards cannot both land (§5) ·
offboarding closes asks-from audit-only and gains its endpoint (§5, §9) · suspension halts the
ephemeral subtree with fold-back, never mid-commit (§6.3) · stage-less sends are send-once — an
ambiguous timeout is an ask, not a resend (§8.2) · ordinary expiring rules join delegations in
`lapsed` semantics (§7) · the single control plane is named an accepted boundary (§13.1).

**Edge-case closure (v2.14)**: sixth sweep — schema-text seams closed inline: human
deactivation is representable (`deactivated_at`, §7) — the last-admin guard and chain-walk
skips now have a data basis · goals gain optional domain scoping: sensitive objectives inherit
compartment access; org-scoped items (goals, glossary) route proposals to the admin queue
(§4.2, §4.3, §7) · injected layers pass the same access check as retrieval — an unreadable
workspace degrades to an ask, never a silently empty prompt (§4.2) · quorum addressing pinned:
`to` = the rule's domain owner, the pool = that owner + active admins, evaluated at respond
time (§7, §8.10) · domain-kind legal holds freeze git history-rewrite remediation and db-only
exports (§4.5, §7) · stranded prepared writes get a scheduled reconciliation pass (§8.2) ·
initiative sponsor carried as pinned-human in the schema (§7) · admin lockout gets a
server-local, audited reset flow (§10).

**Edge-case closure (v2.13)**: fifth sweep — audit residue closed inline: viewer deputies
refused at write, like agent and self deputies — a read-only member is never a standing answer
hop (§7, §8.10) · quorum N validated against the eligible approver pool at proposal time, and
a pool that shrinks below N denies at expiry with the shortfall named (§4.4, §8.10) · unset
calendars get a defined fallback — control-plane zone, 09:00–17:00 weekdays (§8.10) ·
initiative sponsors pinned human (§5.1) · offboarding terminates sessions and revokes PATs —
deactivation is credential-death, not a disabled login flag (§5).

**Edge-case closure (v2.12)**: fourth sweep — the §13.1 residue is designed, not deferred:
N-of-M quorum asks make "two approvals" expressible (§4.1, §8.10, §7) · trigger idempotency keys
+ staged external writes on an `external_writes` ledger, reaper grace, retry-safe keys (§8.5,
§8.2, §6.2) · erasure = pseudonymized ledgers + legal holds + node-region residency (§4.5, §3,
§7) · db-only reconstructibility via topology audit manifests + scheduled exports (§4.5) ·
reservation metering closes check-then-spend races (§6.2, §7) · workspace claims become
epoch-fenced leases (§3) · restore gains a reconciliation runbook — audit replay + node
re-registration (§11) · rules gate external writes like scopes (§8.1) · ask storms collapse +
shed by rate limit (§8.10) · separation-of-duties knob per domain (§4.3) · initiatives grow
`depends_on` (§5.1) · control-plane time authority + per-human calendars (§3, §7, §8.10) ·
proposal amendment + publish-transaction contradiction re-checks (§4.3, §4.4) · runtime
precedence pinned (§4.2) · taint propagates through memory, cleared only by review (§8.3) ·
playbook depth cap + cycle detection (§8.6) · git integrity: signed refs + non-FF refusal
(§4.5, §10) · PAT expiry/rotation/revocation (§9, §10) · breaker trips by criticality class
(§6.2) · offboarding transfers or withdraws authored proposals (§5) · embedding switches cut
over via dual index (§8.7) · glossary alias collisions resolve deterministically (§4.2) · §13.1
restated as accepted boundaries + deferred parameters; new principle: degrade to an ask, never
to silence (§2).

**Edge-case pass (v2.11)**: third sweep — its findings closed inline: tainted-origin asks lose digest pre-fills; taint survives publication as provenance residue (§8.10, §4.3, §13) · deputies must be human; escalation walks carry a visited-set; multi-domain ask hops pinned to the primary domain (§8.10, §7) · DNA review queues get an SLA with admin escalation (§4.3, §7) · retire + offboarding walks extended to board tasks, owned goals, initiative posts, named-access lists (§5, §6.3) · trigger catch-up coalescing (§8.5) · paused-initiative semantics pinned (§5.1) · topology ops serialized behind a domain write lock (§4.4) · ask responses re-validate payload assumptions (§8.10) · count caps and bootstrap claimed atomically (§6.2, §9) · human DNA edits validate-or-quarantine (§4.5) · affinity starvation raises an ask; rebind is capability-checked (§3, §9) · rehire = new member, never resurrection (§5) · decision-vs-rule contradictions + provenance link re-validation (§4.4) · secrets-scanner override is an audited ask, not a silent wedge (§10).

**Edge-case audit (v2.10)**: second sweep — §13.1 re-ranked into severity tiers and extended · quorum approvals inexpressible (§4.1 vs §8.10) · external-write atomicity + trigger idempotency · erasure vs. append-only ledgers + data residency · db-only reconstructibility (§4.4 vs §4.5) · check-then-spend races (§6.2) · workspace-rebind fencing (§9) · restore reconciliation (8a) · mid-run rule staleness (§8.1) · ask storms (§8.10) · self-approval (§4.3) · clock/timezone semantics · proposal amendment (§7) · runtime precedence (§4.2) · taint decay (§8.3) · playbook recursion (§8.6) · git integrity (§4.5) · PAT lifecycle (§9, §10) · offboarding vs. authored proposals (§5) · embedding re-index (§14.7) · glossary alias collisions (§4.2, §7).

**Edge-case pass (v2.9)**: escalation-chain exhaustion pinned — expire-per-behavior plus a critical org-stall broadcast (§8.10) · first-response-wins and expired-response = audit-only close the late/racing-answer seam (§8.10) · conflicting delegations resolve most-restrictive with a contradiction report (§8.10) · scope revocations re-checked before external writes (§8.1) · template retirement refuses live pins (§6.5) · spawner death retargets ephemeral fold-back to project memory (§6.3) · goal-window expiry under a live initiative raises a sponsor ask (§5.1) · goal-vs-goal contradictions join proposal-time checks (§4.4) · residual unhandled edge cases documented as §13.1; provider degradation and partitioned-node authority parked as decisions 15–16 (§14).

**Consistency review (v2.8)**: workspace↔initiative binding added so the goal slice has a defined source (§4.2, §7) · asks carry `workspace_id`, keying the domain-owner escalation hop and digest grouping (§7, §8.10) · ephemeral→agent status mapping made 1:1 — `done` maps to `retiring` (§7) · domain `rename` joins split/merge as a governed endpoint (§9, §4.4) · retire/suspend/resume moved off `/spawn` onto the agent they act on (§9) · stalled-initiative escalation specified — the §13 directive-decay row now has a mechanism (§5.1) · P3 goal slice scoped to org-wide goals until initiatives land in P4 (§11).

**Consistency audit (v2.7)**: delegated rules get `effective_to` so "end by window" (§8.10) is representable (§7) · goal-slice "deadline" pinned to `dna_goals.effective_to` (§4.2, §7) · per-kind expiry defaults, the domain-owner escalation hop, and suspended ask targets specified (§8.10) · workspace rebind endpoint added (§9, §3) · digest ownership P4 (single-admin) vs P6 (per-human) disambiguated (§11) · single-admin "auto-approve" softened to one-click review, deferring to §14.13 (§13) · personal assistants retire — never re-own — on offboarding (§5) · viewer never-an-ask-target guard pinned in the schema (§7) · SLA-tier breach defaults parked as decision 14 (§14).

**Org-change pass (v2.6)**: first-run bootstrap (§9, §11 P1) · domain split/merge/rename as governed topology ops (§4.4) · offboarding closed out — initiatives, board tasks, deputy refs, admin-custody fallback, last-admin guard (§5) · agent suspend + re-role = retire-and-respawn (§6.3) · role-template versioning with owner-approved upgrades (§6.5) · affinity-node loss → queue-or-rebind (§3) · initiative close lapses delegated rules (§8.10) · status enums pinned; viewers never ask targets (§5, §7) · deployment perimeter + proposal strictness parked as decisions (§14.12–13).

**Review pass (v2.5)**: goal slice wired through the runtime + DNA-engine read path (§8.1, §8.7) · proposal kinds cover goals; SOPs pinned as playbooks + pointer cards (§4.1, §7) · ask-digest grouping unified (§8.10, §11 P4) · schema gaps closed: ask→initiative link, domain `store` flag, human deputy, ephemeral status mapping, proposal withdraw (§7, §9) · new API: `/dna/goals`, `/initiatives`, governance reads (§9) · tier-2 connectors explicitly post-v1 (§11).

**Amendments (v2.4 — directive-to-execution)**: goal slice promoted into the read path (§4.2) ·
initiatives — the directive→work spine with sponsor, lead, deadline, dependency-checked close
(§5.1, §7, §8.9) · cross-domain coordination via initiative playbooks (§8.6) · delegated approval
authority as scoped, expiring DNA rules the ask router evaluates (§8.10) · business budgets
display-only until post-v1 (§14.11).

**Amendments (v2.3 — enterprise deployment shape)**: knowledge vs. operational data separation —
systems of record stay live, never synced into the DNA (§4.6) · personal-assistant deployment
shape with mirrored scopes (§6.4) · enterprise connector tier (§8.2) · inter-agent communication
policy — state, not chatter (§8.11).

**Consistency pass (v2.2)**: milestone arithmetic corrected (§11) · headless approval policy
mapped onto Ask tiers (§8.1) · ephemeral spawn vs. template allowlist disambiguated (§6.1) ·
ask escalation/reassign semantics closed out (§7, §8.10) · node management API added (§9) ·
human offboarding defined (§5).

**Review pass (v2.1)**: v1 cut line drawn at Phase 4 (§11) · Asks promoted to a designed
subsystem (§8.10) · node trust model made explicit (§3, §10) · prompt injection added to risks
(§13) · privacy carve-out for the git-backed DNA store (§4.5) · rule-applicability semantics +
prompt budgets (§4.2) · Phase-0 spikes + per-phase acceptance criteria (§11).
