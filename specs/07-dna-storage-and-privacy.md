# SPEC-07 — DNA Storage & Privacy

Source: PLAN.md §4.5, §4.6.

## Stores

- **STG-001** — The canonical DNA store is git-backed markdown with frontmatter (id,
  version, effective dates, provenance, access): `domains/<domain>/{cards,rules,decisions,
  goals}/*.md`, `glossary.md`; org-wide goals under `goals/`. The control plane maintains the
  SQLite/FTS/vector index over it. Humans can read and edit their company's brain with any
  editor; git history *is* the DNA timeline.
- **STG-002** — The tree holds `store: 'git'` domains only; a db-only domain's whole content
  set lives in SQLite per the privacy carve-out; domain-scoped goals follow their domain's
  `store` flag; org-wide goals are git-backed.
- **STG-003** — Domains may declare `store: 'db-only'` (HR and Finance default to it):
  content lives in SQLite with export-on-demand and never enters the git store.
- **STG-004** — Frontmatter carries a `schema_version`; product upgrades run in-place content
  migrations post-backup — an old store is never stranded.
- **STG-005** — If sensitive material lands in git by mistake, remediation is the documented
  history-rewrite procedure (rotate the repo, notify domain owners) — decided here, not
  improvised under deadline.

## Ingest

- **STG-010** — Direct human edits are welcomed, not trusted: the control plane validates
  every ingested change (frontmatter schema, unique ids, effective-window sanity, secrets
  scan) and quarantines invalid files to a review queue with the parse error attached, routed
  to the affected domain's owner (the admin for org-wide files) — a bad hand-merge degrades
  to an ask, never a silently corrupted index.
- **STG-011** — Ingest serializes like every other writer: a valid hand-merge or PR applies
  inside the same domain write lock (DGV-050); a commit touching several domains' trees
  acquires every affected lock up front, id-ordered (DGV-051).
- **STG-012** — Paths under a db-only domain's tree are invalid: a file appearing there
  quarantines to the domain's owner rather than forking a second canon.

## Git integrity

- **STG-020** — The control plane is the DNA repo's only direct writer: it signs commits and
  refs with a deployment key and refuses non-fast-forward updates it did not perform;
  divergence quarantines like any invalid ingest.
- **STG-021** — Teams adopting the PR workflow get protected-branch prerequisites (no
  force-push, no direct push, review through PRs) as deployment requirements, verified at
  startup.

## Erasure

- **STG-030** — Erasure of a person is pseudonymization, not shredding: append-only ledgers
  (audit, spend) keep the event shape while the member reference becomes a one-way pseudonym.
- **STG-031** — The sweep covers: DNA provenance frontmatter and proposal attribution
  (rewritten as a normal signed commit); memory attribution — personal and project memory
  re-point to the pseudonym; and operational history — resolved ask rows (`from`/`to` and the
  quorum responses ledger) and completed board-task assignments pseudonymize with the
  ledgers, event shape kept, identity link severed; pending state is pre-resolved by the
  SPEC-09 walk before erasure can run.
- **STG-032** — Free-text mentions (prose naming the member in a card body, memory item, or
  decision context) are reported, never rewritten: the sweep files an erasure annex to the
  admin listing each mention with its owner; the per-mention call (delete, rewrite under
  owner review, contest) is human.
- **STG-033** — Git history retains the pre-pseudonym commits under the immutable-history
  boundary; a demand exceeding pseudonymization takes the STG-005 history-rewrite
  remediation.
- **STG-034** — Legal holds (`data_holds`, kind `member`) freeze erasure for covered subjects
  until an admin releases them, audited; an erasure request against a member with live
  dependencies is refused until the SPEC-09 offboarding walk has run.
- **STG-035** — Erasure is the only shredding path in the product (DWP-061).
- **STG-036** — The annex covers operational prose: ask payloads, board-task descriptions,
  and run artifacts naming the member are reported for the human delete/rewrite/contest call
  like DNA prose, each listed with the surface's accountable human — the initiative sponsor
  where the surface is initiative-tagged, else the admin (operational records have no domain
  owner). Identity fields pseudonymize on the ledger's terms (STG-031); the prose reports.

## Reconstructibility (db-only)

- **STG-040** — Topology history for db-only domains rests on the audit log, not git:
  split/merge/rename/archive writes a full manifest and triggers an export snapshot;
  scheduled exports back the history the git timeline never held.

## Key acceptance scenarios

```gherkin
Scenario: A pasted credential cannot enter the canonical store
  Given a hand-merged commit whose frontmatter contains a live credential
  When the control plane ingests the commit
  Then the file quarantines to the domain owner's review queue with the scanner finding attached
  And the index is not corrupted

Scenario: Erasure keeps lessons, severs identity
  Given a departed member with authored cards, memory items, resolved asks, and completed assignments
  When an admin runs erasure (no legal hold)
  Then ledgers, provenance, memory attribution, ask rows, and assignments pseudonymize
  And an annex lists every free-text mention for human judgment
```
