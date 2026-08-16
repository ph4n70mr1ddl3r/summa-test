# SPEC-13 — Templates & Catalog

Source: PLAN.md §6.5.

## Catalog structure

- **TPL-001** — Roles are data: templates carry identity/style/handbook (`body`), scopes
  (`default_scopes`), class, and status; one runtime serves all roles.
- **TPL-002** — `role_templates` are keyed unique on (class, name, version); a new version is
  a new row, never an in-place rewrite of one an agent pins; every persistent agent pins
  the version it was spawned from.
- **TPL-003** — A template's class is immutable across its name's versions: a class-flipping
  version is refused at publish — a role that changed class is a new template and the
  retire-and-respawn path.
- **TPL-004** — Catalog writes — create, publish, retire — are admin-governed and audited:
  authorship is infrastructure; owner asks govern adoption, never authoring.
- **TPL-045** — A template name keys its lineage across live and retired rows: a new
  template reusing a fully retired name shall carry that name's class — class immutability
  spans the lineage, not the live set, so a catalog name never changes shape over time — and
  a role whose class genuinely changed takes a new name; the domain-name reuse rule
  (DAT-050/DNC-060) applied to the catalog.

## Version selection and publication

- **TPL-010** — Spawn requests name the exact catalog row; the console defaults to the newest
  `active` version — an approval publishes the version the requester saw, never whichever row
  appeared or retired in between.
- **TPL-011** — Publishing a new `active` version files an upgrade ask to each pinned
  agent's owner; nothing auto-applies; publication supersedes but never retires — a denied
  or expired upgrade leaves the pin on its still-legitimate `active` row, and the next bump
  re-asks.

## Upgrades

- **TPL-020** — An upgrade is proposal-shaped: the diff (IDENTITY/HANDBOOK changes, scope
  deltas) goes to the agent's owner as an ask; on accept, files rebase and scopes
  re-derive as new-template ∩ owner's-current-scopes, never widening; an empty intersection
  refuses to land — the upgrade closes unresolved with the empty re-derivation surfaced
  (retire-and-respawn is the path when the role has genuinely moved past the owner).
- **TPL-021** — The accept re-validates its target version's status at the door: a version
  retired mid-wait leaves the accept audit-only, the pin standing, the next publication
  re-asking.
- **TPL-022** — Suspension strands no upgrade: an accept landing on a suspended agent is
  a data rebase (files and scopes, not execution) and resume re-arms under the rebased
  template.
- **TPL-023** — Ephemeral subagent templates upgrade in place — workers are short-lived, so
  new spawns get the new version.

## Retirement

- **TPL-030** — Retiring a template with live pins is refused; pins count pending spawn
  requests as well as running agents — a request awaiting approval references its template
  exactly as a live worker does. Upgrade or retire-and-respawn the pinned agents and
  resolve the pending requests first (the skill-uninstall dependency check, applied to
  templates).

## Custom-hire promotion

- **TPL-040** — A successful `customRole` hire is a candidate, not a dead end: its owner
  human — or an admin — files a promotion ask snapshotting identity files and effective
  scopes at creation (the proposal-payload pattern: the payload is the proposal's, never a
  live view), addressed to the admin broadcast.
- **TPL-041** — The accept publishes the row `active` with the placement it names — a new
  template, or a new version of an existing one (the version path filing upgrade asks to that
  template's pinned owners exactly as a hand-authored publication); placement validates like
  every catalog write: a name-version collision refuses the accept with the ask standing; a
  class flip refuses outright.
- **TPL-042** — The accept pins the hire it promotes, and adoption names its state set: a
  hire in a live activated state at accept — `active` or `suspended` (a pin is data, not
  execution) — becomes the founding instance; `requested`, `retiring`, and `archived` publish
  unpinned, the founding reference riding the audit event as history; a hire activating
  after an unpinned publish stays unpinned — the later version's upgrade path is its only
  rebase.
- **TPL-043** — The snapshot is the role, never the life: `default_scopes` stores effective
  scopes as a ceiling (future spawns still child ⊆ spawner; upgrades still new ∩ owner);
  personal memory never rides; the founding pin re-derives nothing (the hire's live scopes
  stand; the upgrade algebra first applies at the next version's accept).
- **TPL-044** — The hire's own sense that it has become a role routes to its owner as an ask
  (the fold-back shape) — self-promotion is an adoption question, not a catalog door; a
  denied or expired promotion ask is record only: the ask is adoption, not alteration.
- **TPL-046** — Adoption is one question at a time: a hire with a live promotion ask refuses
  a second at filing — the CLC-050 assistant-1:1 pattern at the catalog door — so two
  accepts can never race to pin one hire (TPL-042 names the founding instance one row's to
  be) and a placement decision never lands twice; the ask's own terminals — deny, expiry,
  accept — clear the way for the next.

## Key acceptance scenarios

```gherkin
Scenario: A custom hire that worked joins the catalog
  Given an active customRole hire whose owner files a promotion ask
  When an admin accepts naming a new template placement
  Then the row publishes active, the hire becomes its founding pin
  And default_scopes store the hire's effective scopes as a ceiling
  And the hire's personal memory stays its own

Scenario: Retirement waits for pins
  Given a template with two running agents and one pending spawn request
  When an admin retires it
  Then the retirement is refused until the pins drain
```
