# Traceability — PLAN.md ⇄ Specification Suite

Completeness proof for the SDD suite: every section of `PLAN.md` (v2.47) maps to requirements
in these specs, and every requirement traces back to a PLAN source. The coverage column lists
exact ID ranges generated from the defined requirements (no elided prefixes, no implied gaps).
Both directions — every defined ID listed, every listed ID defined — are verified by
`tools/lint_specs.py` in CI. Reading direction for implementation: PLAN section → spec module
→ REQ IDs → tasks/tests.

| PLAN.md section | Spec module(s) | Requirement coverage |
|---|---|---|
| §1 Product vision | 01 | VIS-001…007 |
| §2 Principles (incl. universal fallback) | 01, 19 | PRN-001…009; NFR-001 restates PRN-009 |
| §3 Architecture (topology, trust, leases, time, residency, stack) | 02 | ARC-001…005, ARC-010…016, ARC-020…024, ARC-030…032, ARC-040…043 |
| §4.1 Content model + item lifecycles (derived w/ §7, §4.2/§4.4/§9 closure semantics) | 03 | DNC-001…005, DNC-010…011, DNC-020…026, DNC-030…032, DNC-040…042, DNC-050…054, DNC-060…061 |
| §4.2 Read path (injection, retrieval, precedence, aliasing) | 04 | DRP-001…007, DRP-010…011, DRP-020…022, DRP-030…036, DRP-040 |
| §4.3 Write path (proposals, SLA, amendment, publish, sod) | 05 | DWP-001…003, DWP-010, DWP-020…025, DWP-030…033, DWP-040…042, DWP-050…051, DWP-060…064 |
| §4.4 Governance (domains, reader sets, topology ops, locks) | 06 | DGV-001…006, DGV-010…019, DGV-040…042, DGV-045…047, DGV-050…054 |
| §4.5 Storage (git/db-only, ingest, integrity, erasure) | 07 | STG-001…005, STG-010…012, STG-020…021, STG-030…036, STG-040 |
| §4.6 Knowledge vs. operational data | 04 | DRP-050…052 |
| §5 Org model (members, RBAC, board, groups, invariant, deputies) | 08 | ORG-001…002, ORG-020…022, ORG-025, ORG-030…033, ORG-040…043, ORG-050…051, ORG-060…061 |
| §5 Offboarding / demotion / last-admin guard | 09 | OFB-001…003, OFB-010…017, OFB-020…023, OFB-030…033 |
| §5.1 Initiatives (spine, transitions, pause/close, goals, dependencies) | 10 | INT-001…002, INT-010…011, INT-020…023, INT-030…033, INT-040…042, INT-050…052, INT-060…063, INT-070…071, INT-080…081 |
| §6.1 Spawn request & customRole | 11 | SPW-001…002, SPW-010…011 |
| §6.2 Policy engine (scopes, quotas, caps, gates, breaker, reaper) | 11 | SPW-020…023, SPW-030…036, SPW-040…048, SPW-060…064, SPW-070…071 |
| §6.3 Lineage (authority, retire/suspend/resume, re-role, fencing) | 12 | CLC-001…003, CLC-010, CLC-015, CLC-020…027, CLC-030…034, CLC-040 |
| §6.4 Personal assistants | 12 | CLC-050…053 |
| §6.5 Templates & catalog (versioning, upgrades, promotion) | 13 | TPL-001…004, TPL-010…011, TPL-020…023, TPL-030, TPL-040…046 |
| §7 Data model (all tables + inline invariants) | 16 (+03; workspaces archival walk: 12, CLC-040) | DAT-010…011, DAT-020…022, DAT-030, DAT-040, DAT-050, DAT-060…061, DAT-070, DAT-080…081, DAT-090…091, DAT-100…102, DAT-110, DAT-120…125 |
| §8.1 Agent runtime | 15 | SUB-001…005 |
| §8.2 Tools & MCP (staged writes, send-once, reconciliation) | 15 | SUB-010…011, SUB-020…022 |
| §8.3 Memory service (taint) | 15 | SUB-040…042 |
| §8.4 Skills | 15 (ref-only) | referenced by TPL-030, SUB-064 (uninstall-check reuse); no owned IDs |
| §8.5 Trigger engine (coalescing, idempotency) | 15 | SUB-050…052 |
| §8.6 Playbook engine (depth, pinning, retirement) | 15 | SUB-060…064 |
| §8.7 DNA engine (embedding switch, parity gate) | 15 | SUB-070…072 |
| §8.8 Groups & IM | 08, 15 | ORG-040…043; SUB-080 |
| §8.9 Console screens | 15 | SUB-090 |
| §8.10 Asks (tiers, expiry, withdrawal, quorum, chains, delegation, storms, digests) | 14 | ASK-001, ASK-010…012, ASK-015, ASK-030…033, ASK-040…044, ASK-050…058, ASK-060…061, ASK-090…095, ASK-100…101, ASK-110…111 |
| §8.11 Inter-agent communication | 15 | SUB-100…101 |
| §9 API surface | 17 | API-001…006, API-010, API-020…024, API-030…033, API-040…044, API-050…052, API-060…061 |
| §10 Security & governance checklist | 18 | SEC-001…005, SEC-009…012, SEC-020, SEC-030, SEC-040…042, SEC-050 |
| §11 Delivery plan (phases, spikes, acceptance, restore) | 21 | DLV-010…013, DLV-040…043, DLV-050…055 |
| §12 Testing & quality | 21, README, 19 | DLV-060…061; NFR-010…011 (determinism); README verification conventions |
| §13 Risks & mitigations | 19 | NFR-030…035; the table's remaining rows carry mitigations specified by their cited modules (SPW, DGV, ARC, SUB, CLC, DLV) |
| §13.1 Residual risk / accepted boundaries | 19 | NFR-001, NFR-020…022 |
| §14 Key open decisions 1–16 + named parameters | 20 | CFG-001, CFG-010…018, CFG-020, CFG-030, CFG-040, CFG-050, CFG-060, CFG-070, CFG-080, CFG-090, CFG-100, CFG-110, CFG-120, CFG-130, CFG-140, CFG-150, CFG-160 |

## Maintenance rule

When PLAN.md gains an amendment (an edge-case sweep or a new pass), the delta must land here
in the same change: new REQs in the host module, an updated coverage range above, and tests
citing the new IDs. A PLAN change without a spec delta is an incomplete change — and
`tools/lint_specs.py` (CI) fails the build if the ranges above drift from the definitions.
