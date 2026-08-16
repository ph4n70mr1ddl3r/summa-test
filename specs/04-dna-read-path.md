# SPEC-04 — DNA Read Path

Source: PLAN.md §4.2, §4.6.

## Always-injected layers

- **DRP-001** — Every run's system prompt shall be assembled with four injected layers: the
  org snapshot, the glossary slice for the task's domain(s), all applicable rules for the
  workspace's domains, and the goal slice.
- **DRP-002** — The org snapshot shall render the live member set: a deactivated human or
  retired agent is absent (records live on in decisions and audit); and it shall carry
  state, not just membership — `active` renders available, `suspended` renders
  present-but-halted (never offered as a destination), `retiring` renders terminal-bound, and
  a `requested` hire is absent until activation publishes it. What a prompt presents as
  answerable matches what the write guards accept (ASK-061).
- **DRP-003** — Injection shall be deterministic per (reader access, domain set,
  linked-goal set, DNA version) so it is testable.
- **DRP-004** — Each layer carries a token budget (org snapshot ~1k, glossary ~2k, rules ~4k,
  goal slice ~1k — soft, configurable); overflow demotes items to retrieval (rules overflow
  into the searchable index) rather than truncating silently.
- **DRP-005** — Injected layers pass the same compartment access check as retrieval: a domain
  the run's member cannot read contributes no rules, glossary entries, or domain-scoped goals;
  injection never bypasses compartments.
- **DRP-006** — Binding a member to a workspace whose domains it cannot read shall be refused
  at spawn and on admin edit; a mid-life revocation that leaves a workspace with no readable
  domains shall refuse the next run's launch and raise an admin ask.
- **DRP-007** — Overflow demotion is ordered, not discretionary, so determinism survives an
  overflow: layers demote in reverse precedence — glossary first (terms stay resolvable
  through search and citation), then the goal slice ('linked' goals before 'always'), then
  rules last; within rules, narrative statements before enforcement-bearing
  (`machine_hint`-carrying) ones; ties break by item id ascending. The org snapshot degrades
  structurally instead: the routing spine — domains with their owners, groups with their
  Leaders — never demotes, and member rows beyond the budget demote to the org-facts
  directory (DNC-003), never a truncated roster.

## Applicable rules

- **DRP-010** — A rule applies when its domain intersects the workspace's `domain_ids` and its
  effective window covers the run; superseded rules drop out co-temporal with their superseder
  (DNC-023); `machine_hint` narrows matching where present.
- **DRP-011** — A domainless workspace (empty `domain_ids`) shall have a defined layer, not an
  error state: no rules (nothing intersects), the org-wide glossary slice, and org-wide
  'always' goals.

## Goal slice

- **DRP-020** — The slice contains active goals linked to the workspace through its
  initiatives, plus `always`-flagged goals the run can read — org-wide ones in every run,
  domain-scoped ones wherever their domain is readable (the composition rule, DNC-054) —
  each carrying statement, owner, deadline, status.
- **DRP-021** — Window semantics per DNC-051 apply to the slice; a goal past `effective_to`
  leaves the slice and the sponsor ask carries the outcome (INT-050): extend re-adds it under
  a new window; a terminal status ends it for good.
- **DRP-022** — An org-wide goal with no live initiative whose window ends routes the same ask
  to its owner — the admin when the owner is an agent or departed. No goal expires silently.

## Retrieval and citation

- **DRP-030** — Search and injection serve the living corpus — `active` items only.
- **DRP-031** — A retired item resolves by direct citation as read-only history (the page
  opens, provenance intact) without ever surfacing in search or injection; citation and search
  are different surfaces.
- **DRP-032** — Intra-domain glossary duplicates (term or alias duplicating a live entry of
  the same scope) are refused at propose and item write; org-wide entries share the null scope
  and the same refusal.
- **DRP-033** — Cross-domain alias collisions in a multi-domain workspace resolve
  deterministically: the primary domain's term wins, then org-wide entries, then all
  candidates render tagged with their domains — never a silent coin flip.
- **DRP-034** — A draft (owner-staged, cards and glossary) is visible to its owner alone.
- **DRP-035** — Decisions are always live in search within their domain's corpus; an archived
  domain's decisions leave search with the corpus and resolve by citation (DGV-041).
- **DRP-036** — Responses shall reference cards; the console and IM render citations that open
  the source card with its provenance.

## Precedence

- **DRP-040** — When layers disagree mid-run — applicable rule vs. goal slice vs. retrieved
  card — the run obeys rules (normative, windowed) over goals (aspirational) over retrieved
  knowledge (descriptive) and files a contradiction report rather than silently picking a side.

## Knowledge vs. operational data

- **DRP-050** — The DNA shall hold knowledge about systems of record (rules, definitions,
  decisions, how-tos), never a copy of their data; ERP/WMS/HRIS/CRM remain live systems of
  record read through scoped connectors at task time.
- **DRP-051** — When a run observes conflicting facts across systems (ERP says X, a DNA card
  says Y), it shall file a DNA proposal or contradiction report for the domain owner — a
  detection loop, never silent reconciliation.
- **DRP-052** — Answers depending on connector reads shall reference system, record, and
  timestamp, so freshness is visible in the answer.
