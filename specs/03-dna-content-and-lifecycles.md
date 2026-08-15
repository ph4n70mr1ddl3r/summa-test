# SPEC-03 — DNA Content & Lifecycles

Source: PLAN.md §4.1, §7 (item tables). Data representation: SPEC-16.

## Content model

- **DNC-001** — The DNA shall contain exactly these item kinds: cards, rules, decisions,
  glossary entries, goals, org facts, and SOP pointer cards.
- **DNC-002** — SOPs shall be represented as a versioned playbook (SUB-060) plus a DNA pointer
  card carrying the narrative and the playbook id; the DNA shall never hold a second executable
  copy of a process.
- **DNC-003** — Org facts (who exists, teams, domain ownership) shall be generated read-only
  from the org registry and shall not be directly writable.
- **DNC-004** — Every card, rule, and decision shall record provenance; uncited claims shall be
  flagged during review (DWP-050).
- **DNC-005** — Items carry freshness metadata: review cadence and stale flags; scheduled DNA
  quality checks re-validate provenance refs — moved documents and rotated systems flag the
  card stale rather than letting citations rot silently.

## Cards

- **DNC-010** — Card status enum: `draft` | `active` | `retired`, default `active`: an owner's
  direct create is the publish path; draft is an explicit owner-staged phase.
- **DNC-011** — Card retirement shall be terminal: revival is a new card citing the old; a
  draft discards by retiring. Un-retire does not exist.

## Rules

- **DNC-020** — Rule status enum: `active` | `superseded` | `lapsed`; there is no `retired`.
- **DNC-021** — Item-level retire on a rule is window truncation: `effective_to` pinned to
  now, the row lapsing at that boundary.
- **DNC-022** — Superseded and lapsed rules are frozen history: updates refused; correction is
  a new rule citing or superseding the old; a predecessor stays superseded when its superseder
  lapses (nothing flips back silently).
- **DNC-023** — The displacement edge is the superseder's `effective_from`: a predecessor keeps
  injecting until its superseder's window opens; a future-windowed successor is a scheduled
  replacement, never a normative gap.
- **DNC-024** — `supersedes_id` is intra-domain; cross-domain edges are refused at propose and
  item write (topology ops move chains whole, DGV-013).
- **DNC-025** — Supersession chains are linear, not forks: a second live `supersedes_id` edge
  onto an already-superseded row is refused at propose, amend, and item write; displacing a
  superseded rule means naming the chain's live head.
- **DNC-026** — Ordinary expiring rules transition `lapsed` at `effective_to`, dropping out of
  injection and routing; initiative close lapses its scoped rules identically (ASK-090).

## Decisions

- **DNC-030** — Decisions are immutable and lifecycle-free: create-only at every surface
  (proposal publish and item CRUD); no update, retire, or delete exists for them; reversal or
  amendment is a new decision record citing the old through `refs`.
- **DNC-031** — `decided_by` is cited provenance, not authority: any member — viewer, agent,
  since-departed — may be recorded as decider of record; no ask-eligibility guard applies to
  the field.
- **DNC-032** — Decisions are history at birth: they never block their domain's archive
  (DGV-040), leave search with their domain's corpus when it archives, resolve by citation,
  and move with the corpus on merge, ids stable.

## Glossary

- **DNC-040** — Glossary status enum: `draft` | `active` | `retired`, default `active`.
- **DNC-041** — The "live entry" of the duplicate check (DRP-032) is any non-retired row of
  the same scope — draft and active both hold their terms.
- **DNC-042** — Retirement (item CRUD, never delete) is what frees a term or alias for reuse;
  the retired entry stays resolvable as read-only history; retirement stays terminal because
  un-retiring could collide with a re-claimed term.

## Goals

- **DNC-050** — Goal status enum: `active` | `met` | `missed` | `retired`; `inject` flag
  `always` | `linked`; optional `domain_id` (null = org-wide).
- **DNC-051** — Goal windows are two-sided: admission at `effective_from`, exit at
  `effective_to`; a goal not yet at `effective_from` has not entered the slice; one past
  `effective_to` leaves it.
- **DNC-052** — Terminal statuses are immutable: post-terminal updates are refused at every
  surface; re-base and re-target create a new goal row.
- **DNC-053** — Goal owner: any member but an ephemeral worker; a viewer human is refused at
  write; an agent owner keeps the admin-routing fallback (DRP-022); walks clamp to active
  goals — a terminal goal's owner reference is pinned history, severable only by erasure
  (STG-030).
- **DNC-054** — A domain-scoped goal inherits its domain's access policy; the `inject` flag
  composes with scope: 'always' reaches every run that can read the domain, 'linked' only
  initiative-bound workspaces.

## Uniqueness invariants

- **DNC-060** — Domain names shall be unique among non-archived domains (an archived name is
  reusable history).
- **DNC-061** — Role templates are keyed unique on (class, name, version) (TPL-002).

## Key acceptance scenarios

```gherkin
Scenario: Forked supersession refused
  Given rule R1 superseded by R2 (live head)
  When a proposer files R3 with supersedes_id = R1
  Then the write is refused at propose naming the chain's live head

Scenario: Scheduled replacement leaves no gap
  Given R1 active and R2 published with supersedes_id = R1 and effective_from in the future
  When runs execute before that window opens
  Then R1 still injects
  And when the window opens, R2 displaces R1 without a co-temporal pair
```
