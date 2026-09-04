# Summa — Operating System for a Hybrid Human + AI Company
> Derived from `PLAN.md` (v2.61). The normative requirements are in `specs/`.

A self-hosted platform where human employees and AI agents work as one organization. A governed **Company DNA** — the shared knowledge, rules, decisions, and goals of the company — keeps every member coherent.

## Quick Start

### Prerequisites
- Java 21+
- Node.js 22+
- Maven 3.9+
- Python 3.10+ (spec lint tooling)

### Start Single-Process Mode (backend only)

```bash
# Build the backend JAR first (./start.sh runs the packaged JAR)
npm run build:backend
export SUMMA_JWT_SECRET=$(openssl rand -hex 32)
./start.sh
```

### Development Mode (backend + console)

```bash
./dev.sh
```

### Access
- Console: http://localhost:3000 (requires `./dev.sh` or `docker compose up -d`)
- API: http://localhost:8080/api
- Health: `GET /api/health` (no auth required)
- Bootstrap first admin: `POST /api/org/bootstrap` (no auth required, first-run only — always creates an `admin`)

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                    Control Plane                     │
│  Console UI │ Human auth + RBAC │ Org registry      │
│  DNA service (storage · retrieval · proposals)      │
│  Governance engine │ Task board + Asks             │
│  Trigger engine │ Playbook engine │ Scheduler       │
└──────────────────┬──────────────────────────────────┘
                   │ node protocol
    ┌──────────────┼──────────────┐
    ▼              ▼              ▼
 Node A (dev)   Node B (office)  Node C (server)
```

## Tech Stack

- **Backend**: Java 21 LTS + Spring Boot 3.4, SQLite (WAL)
- **Console**: React 19 + TypeScript + Vite + Tailwind CSS
- **Deployment**: OCI images, rootless Podman, Kubernetes-ready

## Spec Suite

The normative requirements are in `specs/`. Run the structural linter:

```bash
python3 tools/lint_specs.py
python3 tools/test_lint.py
```

## Phases

Status as of 2026-09-04; normative scope lives in `PLAN.md` and `specs/`
(see `specs/21-delivery-and-acceptance.md` for the full 0–8b phase table with entry/exit criteria).

| Phase | Deliverable | Status |
|-------|-------------|--------|
| 0. Foundations | Repo, CI, single-process skeleton | Complete |
| 1. MVP Agent | Chat with local work, model gateway | Complete |
| 2. Identity | Role catalog, memory tiers, skills | In Progress |
| 3. DNA v1 | Store, domains, proposals, review queue | In Progress |
| 4. Automation | Triggers, PATs, task board | In Progress |
| 5. Playbooks | DSL + sandbox | Pending |
| 6. Multi-human | RBAC, ask routing, node registration | Pending |
| 7. Spawning | Ephemeral workers, policy engine | Pending |
| 8a. Hardening | Security review, backup/restore drills | Pending |
| 8b. Delivery & Acceptance | Demos DLV-050…055, cut-over | Pending |

## License

Proprietary — see LICENSE for details.
