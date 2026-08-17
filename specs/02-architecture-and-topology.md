# SPEC-02 — Architecture & Topology

Source: PLAN.md §3.

## Deployment shapes

- **ARC-001** — The system shall run in single-process mode (control plane + one node, console
  at `localhost`) as the MVP path, and in control-plane + execution-node mode with no
  functional divergence between the shapes (the first-deployment default is CFG-030's
  decision).
- **ARC-002** — Nodes enroll via one-time token exchange, authenticate with a keypair identity
  on every connection, and are revocable from the console; every audit event shall record the
  executing node id where one exists (DAT-121 — plane-originated events carry none).
- **ARC-003** — Remote nodes are trusted compute, not enforcement boundaries: enforcement that
  must survive a hostile node (egress allowlisting, secret handling) shall route through the
  control plane / model gateway for remote nodes; single-process mode has no such exposure.
- **ARC-004** — 24/7 automations shall require an always-on node; workspace affinity on a
  sleeping machine is for interactive work only.
- **ARC-005** — The stack shall be: Java 25 LTS + Spring Boot 4 daemon (current major,
  fat-jar — one artifact per service, each shipped as an OCI image, ARC-006); React + Vite +
  Tailwind + shadcn console (TypeScript); SQLite
  (WAL) via sqlite-jdbc + sqlite-vec as a loadable extension (DLV-041 validates the JVM
  load) + FTS5; GraalJS playbook sandbox — a sealed polyglot context, host access denied
  (with the Phase-0 spike's child-process fallback, DLV-040); Spring-scheduled cron
  triggers; MCP connectors (official Java SDK); Tauri shell as Phase-8b polish.
- **ARC-006** — Every artifact shall ship as an OCI image, built and run rootless under
  Podman, with Kubernetes as the orchestration target (CFG-030, decided v2.58): the control
  plane decomposes into services along the seams this section already defines — plane
  API/console backend, model gateway, execution nodes, plus the deployment's Keycloak
  (CFG-020) as the human-IdP service — creating no second writer for any single-owner store
  (NFR-021's SQLite owner, STG-020's single direct DNA writer); single-process mode (ARC-001)
  remains deployable as one container. The decomposition is gated by the DLV-044 spike.

## Workspace affinity & placement

- **ARC-010** — Runs shall be scheduled to the node where the workspace's files/connectors
  live; affinity is a scheduling preference, not a marriage.
- **ARC-011** — When the affinity node goes offline, new runs shall queue until its heartbeat
  returns or an admin rebinds the workspace; a queue starved past the configurable window
  (default 24h, CFG-010) shall raise an admin ask.
- **ARC-012** — A workspace rebind shall validate that the target node advertises the
  workspace's required capabilities (files present, connectors authorized) and shall refuse a
  target that does not.
- **ARC-013** — The initial workspace–node placement shall run the same capability check as
  ARC-012: a workspace shall never be born attached to a node that cannot run it.
- **ARC-014** — Node capabilities are live advertisements re-stated on every heartbeat; a node
  whose advertisement no longer satisfies a bound workspace shall surface the same
  rebind-or-starvation ask as affinity loss — drift is a scheduling event, never a per-run
  failure.
- **ARC-015** — The console node surface shall edit `region` and descriptive metadata
  (the `name` row) only, never capabilities; a capability change reaches the plane as
  drift with its ask (ARC-014).
- **ARC-016** — Revoking a node shall (a) refuse its keypair at every connection thereafter,
  (b) surface a rebind ask for every workspace still bound to it, at revocation time, (c) halt
  its in-flight runs as suspension halts them — partial results fold back through the memory
  tiers, staged writes go to reconciliation (SUB-020), terminal with no resurrection — and
  (d) kill its claims with the row.

## Fenced leases

- **ARC-020** — A node holds a workspace under a renewable claim lease carrying an epoch;
  heartbeats renew it; an admin rebind revokes it and bumps the epoch.
- **ARC-021** — A node shall hold a live lease before claiming runs and before each external
  write.
- **ARC-022** — An expired lease (partition) shall force a pause-and-resync before the node
  touches anything the control plane mediates (model gateway, connectors), so a rebind never
  races a partitioned-but-alive node into dual-writer mode.
- **ARC-023** — A stale node's already-committed local writes shall be reconciled on reconnect
  (audit entry + contradiction report), never silently overwritten.
- **ARC-024** — The epoch fence shall refuse stale-node actions at the mediated boundary
  whatever the node still holds; the lease interval is tunable (CFG-160), the fence is not
  optional.

## Time authority

- **ARC-030** — The control plane is the time authority: deadlines, SLAs, windows, TTLs, and
  leases evaluate against its clock, never a node's.
- **ARC-031** — Time shall be monotonic in effect: expiries evaluate against a persisted
  high-water mark, so a backward clock step never un-expires an ask, window, lease, rule, or
  TTL, nor reverses a terminal transition; a forward step only makes watchers fire sooner.
- **ARC-032** — Per-human timezones and working hours define each recipient's morning for
  digests and `queue_until_morning`; a human with no calendar set shall fall back to the
  control plane's zone, 09:00–17:00 weekdays (ASK-110).

## Residency

- **ARC-040** — Nodes carry an admin-set `region` tag; domains may declare a residency
  constraint; scheduling (affinity and rebind) shall place work only on nodes satisfying it.
- **ARC-041** — A residency constraint no enrolled node satisfies shall starve affected work
  into the ARC-011 starvation ask, visible to the domain's owner in their digest — an
  impossible placement is a surfaced configuration error, never a silent queue.
- **ARC-042** — Editing a node's region shall re-validate every residency-constrained
  placement bound to it: conforming leases stand; nonconforming ones surface rebind-or-
  starvation (ARC-012/014). No placement is silently grandfathered.
- **ARC-043** — Setting or tightening a domain's residency constraint shall require an audited
  admin attestation that the control plane's own hosting (data at rest: git store, SQLite,
  db-only exports) satisfies it — a promise the deployment cannot keep is surfaced at
  declaration.

## Key acceptance scenarios

```gherkin
Scenario: Capability drift surfaces as scheduling state
  Given a workspace bound to a node whose heartbeat stops advertising a required capability
  When the next heartbeat lands
  Then the rebind-or-starvation ask is raised (not a per-run failure)
  And subsequent runs queue behind the starvation window

Scenario: Partition cannot produce dual writers
  Given a node partitioned past its lease expiry while the admin rebinds the workspace
  When the node reconnects and attempts a mediated action
  Then the stale epoch is refused at the boundary
  And its committed local writes are reconciled with an audit entry and contradiction report
```
