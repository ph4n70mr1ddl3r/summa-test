# SPEC-09 — Offboarding & Demotion

Source: PLAN.md §5 (offboarding, demotion, last-admin guard).

## Offboarding walk

- **OFB-001** — Deactivating a human runs the full dependency walk across everything they
  touch; every disposition below lands inside the walk.
- **OFB-002** — Owned DNA domains transfer to a named successor, else **admin custody** —
  never orphaned; their pending owner-addressed asks re-key to the successor or custody with
  ids stable and deadlines untouched (DGV-045); the review queue re-renders to the inheritor
  (DWP-021); owner-staged drafts transfer with the domain and surface in the inheritor's
  console, never orphaned invisible.
- **OFB-003** — Open asks *to* the member reassign up the chain; asks *from* the member close
  with an audit note (a departed member's pending spawn requests no longer gate anything).
- **OFB-010** — Board-task assignments are reassigned or returned to the pool.
- **OFB-011** — Dependent Coworkers are re-owned or retired. Re-owning narrows, never widens:
  scopes re-derive as current ∩ the new owner's live ceiling; an empty intersection retires
  the worker. Personal assistants are always retired (mirrored scopes die with the member,
  CLC-050).
- **OFB-012** — Sponsored/led initiatives are reassigned or closed, with their pending
  sponsor-addressed asks re-keying to the re-pointed sponsor inside the walk, ids stable,
  deadlines untouched.
- **OFB-013** — Owned goals (`dna_goals.owner`) re-own via successor or admin custody, else
  retire — clamped to active goals: a terminal goal's owner reference is pinned history,
  severable only by erasure (STG-030).
- **OFB-014** — Membership in `named` domain access lists is removed (policies re-evaluated);
  workspace participation entries and group memberships are cleared (reader sets and routing
  re-derive); deputy references clear in both directions.
- **OFB-015** — Sessions are terminated and PATs revoked: deactivation is credential-death,
  not a disabled login flag.
- **OFB-016** — Pending DNA proposals they authored transfer to the successor for owned
  domains and are auto-withdrawn with an audit note for member proposals — the review queue
  never waits on a departed proposer.
- **OFB-017** — Inactive members are skipped when walking ask chains.

## Last-admin guard

- **OFB-020** — The last active admin cannot be deactivated — evaluated inside the
  offboarding transaction, so two racing deactivations of the last two admins see one success
  and one refusal (the bootstrap atomicity pattern).
- **OFB-021** — Demotion joins deactivation under the guard: an RBAC role change that would
  leave zero active admins is refused by the same transactional check.
- **OFB-022** — Org wind-down is never an offboarding: an org with zero humans has no
  accountability anchor, so dissolution is a deployment shutdown (export, halt)
  with no in-product endpoint; the guard's refusal is the org model staying honest about its
  human anchor.
- **OFB-023** — Audit history is retained; personal data falls under the STG-030 carve-out.

## Demotion walk

- **OFB-030** — An RBAC edit that reduces a human's role runs the same dependency walk as
  offboarding, scoped to what the new role can no longer carry, inside the last-admin
  guard's transaction — a demotion cannot half-land.
- **OFB-031** — To `viewer`: asks to the member reassign up the chain; asks from the member
  close with an audit note; assignments return to the pool or reassign; deputy references
  clear in both directions; group Leader posts re-point; owned goals re-own or retire (the
  active-goal clamp applies); sponsored/led initiatives re-point; shed authority transfers as
  at offboarding (domains, Coworkers — with assistants always retiring: a never-ask-target
  owns no staff).
- **OFB-032** — Authored proposals travel with the authority: transferred to the successor
  for shed domains, withdrawn with an audit note when the new role can no longer propose (to
  viewer); the member level keeps its amendment rights. Staged drafts ride the shed domains.
- **OFB-033** — `owner` → `member` sheds domain ownership by the same rule; the re-key rules
  ride identically — sponsor-addressed asks with the posts, owner-addressed asks with the
  domains, re-owned staff narrowing to the new ceiling.

## Key acceptance scenarios

```gherkin
Scenario: Racing offboards cannot behead the org
  Given exactly two active admins A and B
  When deactivations of A and B are submitted concurrently
  Then one succeeds and one is refused by the transactional guard
  And the org retains exactly one active admin

Scenario: Custody never widens scopes
  Given a departing owner's Coworker with scopes S, successor ceiling C
  When the walk re-owns it to the successor
  Then its scopes become S ∩ C
  And if S ∩ C = ∅ the Coworker retires
```
