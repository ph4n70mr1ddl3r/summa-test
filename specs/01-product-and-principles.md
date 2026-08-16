# SPEC-01 — Product & Principles

Source: PLAN.md §1, §2. Actors defined here are used uniformly across the suite (see README).

## Product requirements (VIS)

- **VIS-001** — The system shall be a self-hosted platform in which human employees and AI
  Coworkers operate as one organization with a shared member namespace, task board, and
  attention surface.
- **VIS-002** — The system shall maintain a governed **Company DNA** — knowledge cards, rules,
  decision records, SOPs, glossary, org facts, goals — as the single source of organizational
  coherence, changed only through the write paths of SPEC-05/06.
- **VIS-003** — The system shall record all work — chat tasks, automations, playbook runs — as
  runs with results, artifacts, and to-dos on a shared Task Board (ORG-030…033).
- **VIS-004** — The system shall carry directives from decision records and goals through
  **initiatives** to coordinated execution (SPEC-10).
- **VIS-005** — The system shall provide governed spawning: a human or agent member spawns
  Coworkers under policy, budget, and lineage constraints (SPEC-11).
- **VIS-006** — The system shall implement the improvement loop: every run's learning is
  classified (personal / project / DNA proposal, SUB-040), DNA changes are reviewed by owners,
  and the updated DNA guides subsequent runs.
- **VIS-007** — The system shall support a single-process deployment (control plane + one node
  in one binary, console at localhost) with no feature loss relative to the multi-node shape
  (ARC-001).

## Governing principles (PRN)

Each principle is a constraint on every other requirement; where a module relaxes one, the
module says so explicitly.

- **PRN-001** — DNA is the source of coherence: agents shall not silently fork DNA into private
  stores; durable knowledge lives only in the governed store (STG-001).
- **PRN-002** — One binary runs a small team; the same services split into control plane +
  nodes as the company grows. The split is a deployment change, not a rewrite (ARC-001…004).
- **PRN-003** — Every capability is a guarded tool: file scope, tool scope, egress guard, and
  audit are enforced in code, never in prompts (SEC-009, SUB-001).
- **PRN-004** — Agents are accountable to humans: every Coworker carries `owner_human_id`;
  ephemeral workers roll up to their spawner; every chain terminates at a human (ORG-050…051, SPW-046).
- **PRN-005** — Spawning is delegation, not reproduction: child scopes ⊆ parent scopes, budgets
  and TTLs bind, policy gates (SPW-010).
- **PRN-006** — Files for humans, database for machines: DNA, identity, memory are git-friendly
  markdown except where the privacy carve-out applies (STG-001…005); runs, audit, and indexes
  live in SQLite.
- **PRN-007** — Role-agnostic core: roles are data (templates, skills, connectors, scopes);
  one runtime serves all roles (TPL-001).
- **PRN-008** — Governance is proportional to blast radius: ephemeral workers get quotas;
  persistent hires and DNA changes get review (SPW-001, DWP-001).
- **PRN-009** — **Universal fallback**: when any subsystem meets a state its designers did not
  anticipate, it shall refuse the effect, write an audit entry, and raise an ask — no subsystem
  may fail silently or improvise a side effect. This is the contract that makes "handle every
  scenario" falsifiable rather than aspirational; the chaos/fault-injection suite (DLV-060)
  enforces it.

## Actor eligibility summary

The following guards recur across modules and are specified once:

- Viewer humans are never ask targets, assignees, deputies, sponsors, leads, goal owners,
  domain owners, group Leaders, proposers, or spawn requesters (ORG-020).
- Ephemeral workers are never leads, goal owners, named delegation agents, initiative
  originators, DNA proposers, or persistent-hire requesters (INT-001, INT-010, DNC-053,
  DWP-010, SPW-010, ASK-092).
- Non-active members (any status other than `active`) are refused every answerable post at
  write and reassign up the chain when targeted mid-life (ASK-060…061, ORG-021).
