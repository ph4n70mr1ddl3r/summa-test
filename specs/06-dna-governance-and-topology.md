# SPEC-06 — DNA Governance & Topology

Source: PLAN.md §4.4.

## Domains and reader sets

- **DGV-001** — DNA is partitioned into domains, each with a human owner and an access policy
  `public` | `domain` | `named`; attributes `store`, `sod`, `residency`, `review_sla_days`,
  `named_readers`, `status` per SPEC-16.
- **DGV-002** — Reader sets are defined, not ambient: `public` admits every member; `domain`
  admits the owner plus every member tied in through an active workspace binding (an agent
  via its workspaces' `domain_ids`, a human via workspace participation); `named` admits the
  owner plus the named list. A domain whose access policy is not `public` is **restricted** —
  the read-audit and rate-limit surface (DGV-004, CLC-052).
- **DGV-003** — Every reader-set input evaluates against live state: a deactivated human, a
  retired agent, or a dead `named_readers` entry contributes nothing; access re-evaluates
  with its inputs, and rehire's fresh row re-admits no one until named again. The SPEC-09
  walks scrub the lists as defense in depth.
- **DGV-004** — The owner always reads the domain they own; active admins hold governance
  reads of every domain — audited on restricted domains — because the escalation, sod, and
  custody paths hand them the content.
- **DGV-005** — Row-write authority is split: create/archive, structural attributes (`store`,
  `sod`, residency), and owner re-pointing are admin writes; the owner edits `access`,
  `named_readers`, and `review_sla_days`; every row-write is audited.
- **DGV-006** — The owner must hold role `owner` or `admin` at write; an RBAC demotion below
  that runs the SPEC-09 walk (transfer or admin custody, never an orphaned domain).

## Topology operations

- **DGV-010** — Reorgs — split, merge, rename, archive — are governed single auditable
  events: items move with ids stable (citations and supersession chains survive), access
  policies re-evaluate, workspace domain tags remap.
- **DGV-011** — An op declares its result: split names owner, access, `store`, `sod`,
  residency (and inherits the parent's `named_readers` list) for each resulting domain;
  undeclared attributes inherit from the parent — `review_sla_days` among them — the
  inherit-by-default rule merge states as persist-from-the-survivor (DGV-014), so a split
  result's queue SLA is never undefined.
- **DGV-012** — A split's declared mapping is total — every item, workspace binding, and open
  proposal names its result — or the op refuses at declare; the emptied parent archives inside
  the same event (division is dissolve-by-split).
- **DGV-013** — The declared mapping respects chain integrity: a supersession chain maps whole
  to one result; a mapping that would divide a chain is refused at declare.
- **DGV-014** — Merge declares the surviving domain's attributes: access defaults to the
  narrower of the pair, computed at declare time against live reader sets (DGV-002) —
  `public` is the widest; every other pairing — `domain` vs `domain` included — compares
  the two sides' evaluated member sets with the strictly smaller set winning, and a pair
  with no strictly smaller side (each admitting members the other excludes, or the two
  evaluating equal) refuses the default and demands a declared
  access; undeclared attributes persist from the survivor; the `named` list keeps the
  survivor's unless the op declares the union — a merge never silently widens access, and
  a narrowed list shows in the event's access re-evaluation.
- **DGV-015** — A `store` change migrates content inside the same audited event: git→db-only
  sweeps the files from the tree in one commit; db-only→git demands an explicit confirm
  (immutable history is published); both are refused while either side sits under a
  kind-`domain` legal hold; a standalone `store` flip through domain update runs the same
  migration, confirm, and hold refusal.
- **DGV-016** — Residency edits re-validate placements: a tightened constraint re-validates
  every bound workspace's placement — conforming leases stand, nonconforming ones rebind via
  ARC-012 or starve into ARC-011's ask; an attribute edit is never silently grandfathered.
- **DGV-017** — Legal holds freeze removal, not addition: archive, merge-away, and split of a
  held domain queue behind the hold's release; standalone store flips and cross-store
  topology ops refuse outright (DGV-015/054); rename and merge-into
  stay open — a cross-store merge-into being the exception DGV-054 names, the addition a
  hold permits never doubling as an unconfirmed publication into immutable history.
- **DGV-018** — The commit re-runs contradiction checks against post-op state inside the
  lock — items peaceful across two domains may collide in one, surfacing as review asks.
- **DGV-019** — Dissolution is the degenerate case: no bare delete exists — merge remaining
  items and bindings away, then archive the emptied domain.
- **DGV-054** — A topology op whose sides differ in `store` runs the store-change discipline
  (DGV-015) — content moving across stores is the flag's change whatever door it moves
  through: a merge of a db-only side into a git survivor demands the db-only→git explicit
  confirm (the op refusing until confirmed), a git side merging into a db-only survivor runs
  the one-commit sweep, and either direction refuses while either side sits under a
  kind-`domain` hold; a split result whose declared `store` differs from its parent migrates
  its mapped items by the same rules — confirm when the parent is db-only (content entering
  git), sweep when the parent is git (content leaving it) —
  held splits already queue behind release (DGV-017).

## Archive semantics

- **DGV-040** — Archive refuses a domain still holding live-set state: active items,
  owner-staged drafts, live workspace bindings, or open proposals. Terminal history
  (superseded/lapsed rules, terminal goals, retired cards/glossary) never blocks; decisions
  never block (lifecycle-free). A history-only domain archives directly.
- **DGV-041** — The archived row is read-only history: no injection, routing, or new
  bindings; nothing shredded; items resolve by citation (DRP-031); merge-away moves the whole
  corpus, history riding with the live set, ids stable.
- **DGV-042** — Open proposals travel with their domain: merge/split/rename remap in-review
  proposals to the resulting queues inside the audited event (payload `domain_id` rewritten,
  ids stable, `review_by` clocks running); archive counts them among refused holdings.

## Attention remapping

- **DGV-045** — Pending asks whose addressee was derived from a domain's owner (the
  persistent-hire spawn approval's gate hop, a quorum ask's primary recipient) re-key to the
  resulting owner inside topology ops — ask ids stable, deadlines untouched — and at every
  other owner-re-pointing door: the domain edit and the SPEC-09 walks.
- **DGV-046** — The spawn gate's hop keys on the hire workspace's primary domain: an admin
  edit of that binding (primary demoted, unbound, emptied to domainless) — or of the
  binding list that demotes or unbinds the primary workspace itself (SPW-040's first-bound
  rule) — re-keys a pending spawn approval to the gate the edited bindings derive (the new
  primary domain's owner, the re-pointed primary workspace's, or an admin once domainless),
  inside the audited edit, ids and deadlines stable.
- **DGV-047** — Archive, having no resulting owner to re-key onto, settles instead:
  owner-addressed asks pending against the archiving domain close with an audit note inside
  the event (ASK-044).

## Write-lock discipline

- **DGV-050** — Every domain has one writer door, serialized behind a domain-level write
  lock: topology ops queue behind in-flight proposals and each other; publishes and
  amendments serialize (DWP-031/040); external git ingest applies inside the same lock
  (STG-011).
- **DGV-051** — Writers spanning several domains — merges above all — acquire every affected
  domain's lock up front, in domain-id order; overlapping multi-domain ops serialize
  deadlock-free in one deterministic order.
- **DGV-052** — The same discipline binds every writer to a workspace's `domain_ids`: the
  admin binding edit (DGV-046), the workspace-archive walk (CLC-040), and a hand-merge
  touching several domains' trees acquire the affected locks up front, id-ordered; the second
  writer re-reads the list inside the lock it queued behind — no lost update either way.
- **DGV-053** — Prior states stay reconstructible from git history and audit; for db-only
  domains, topology ops write full manifests (item ids, from/to, access re-evaluations) to
  the audit log and trigger an export snapshot (STG-040).

## Key acceptance scenarios

```gherkin
Scenario: Overlapping merges serialize
  Given merge A-into-B and merge B-into-A filed concurrently
  When both acquire locks
  Then they serialize in domain-id order and one completes after the other

Scenario: Merge never widens access
  Given domain A 'public' merged into surviving domain B 'named'
  When the op declares no access override
  Then the result is 'named' with B's list
  And the event's access re-evaluation surfaces the narrowed reader set
```
