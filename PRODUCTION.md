# Summa — Production Runbook

## Quick Start

```bash
# Single-process mode (development/small team).
# The script runs the packaged JAR, so build it first:
npm run build:backend
export SUMMA_JWT_SECRET=$(openssl rand -hex 32)
./start.sh

# Dev mode (backend + console hot-reload)
./dev.sh
```

## Architecture

```
Single-Process Mode (./start.sh):
  ┌─────────────────────────────────────────────────────┐
  │  Spring Boot 3.4 + SQLite (WAL) + FTS5              │
  │  Port 8080 (API only; context-path /api)            │
  └─────────────────────────────────────────────────────┘

Dev Mode (./dev.sh): API on :8080 + Console on :3000 (separate processes)

Docker Compose: API container + nginx-proxied console on :3000
Kubernetes (prod): decomposed services
```

- **Single-process** (`./start.sh`): API on `:8080` only. No console.
- **Dev mode** (`./dev.sh`): API on `:8080` + console on `:3000` (separate processes).
- **Docker Compose**: API container + nginx-proxied console on `:3000` (console proxies `/api` to the API container).

## Data Storage

| Component | Path | Description |
|-----------|------|-------------|
| SQLite DB | `~/.summa/summa.db` | All runtime state (WAL mode) |
| DNA Git Repo | `~/.summa/dna` | Canonical DNA store (markdown) |
| Logs | stdout/stderr | Application logs (collect via `journald` / `docker compose logs`) |

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SUMMA_DB_PATH` | `~/.summa/summa.db` | SQLite database path |
| `SUMMA_DNA_REPO` | `~/.summa/dna` | DNA git repository path |
| `SUMMA_JWT_SECRET` | *(required)* | JWT signing secret (256+ bits; generate with `openssl rand -hex 32`) |
| `SUMMA_SPEND_CEILING` | `1000000` | Org spend ceiling |
| `SUMMA_CORS_ORIGINS` | *(localhost only)* | Extra CORS origins, comma-separated (e.g. `https://app.example.com`) |
| `SPRING_PROFILES_ACTIVE` | `prod` in Docker | Spring profile (`dev` for local hot-reload) |
| `VITE_API_URL` | `/api` | API base URL baked into the console bundle at build time |
| `VITE_SUMMA_MODE` | `single-process` | Console mode badge: `single-process` or `multi-node` |
| `SUMMA_OIDC_ISSUER` | *(planned)* | Keycloak issuer URI (not yet wired — see note below) |
| `SUMMA_OIDC_CLIENT_ID` | *(planned)* | OIDC client ID (not yet wired) |
| `SUMMA_OIDC_CLIENT_SECRET` | *(planned)* | OIDC client secret (not yet wired) |

> **OIDC note:** `SUMMA_OIDC_*` is reserved for a planned Keycloak integration.
> Human auth today is email + password via `POST /api/auth/login`.

## API Endpoints

Abridged — the full surface with REQ IDs lives in `specs/17-api-surface.md`
(the controllers under `backend/src/main/java/com/summa/controller/` are authoritative).

### Auth & Bootstrap
- `POST /api/auth/login` — Email + password login (returns JWT)
- `POST /api/org/bootstrap` — First-run company + admin creation (public, first-run only)

### Organization
- `GET /api/org/humans` — List humans
- `POST /api/org/humans/{id}/offboard` — Offboard human
- `GET /api/org/audit` — Audit log

### DNA
- `GET /api/dna/domains` — List domains
- `GET /api/dna/cards` — List cards
- `GET /api/dna/rules` — List rules
- `GET /api/dna/decisions` — List decisions
- `GET /api/dna/glossary` — List glossary entries
- `GET /api/dna/goals` — List goals
- `GET /api/dna/proposals` — Review queue
- `GET /api/dna/search?q=...` — FTS5 search

### Asks
- `GET /api/asks` — List asks
- `POST /api/asks` — Create ask
- `POST /api/asks/{id}/respond` — Respond to ask

### Initiatives
- `GET /api/initiatives` — List initiatives
- `POST /api/initiatives` — Create initiative
- `POST /api/initiatives/{id}/activate|pause|resume|close`

### Board Tasks
- `GET /api/board-tasks` — List tasks
- `POST /api/board-tasks` — Create task
- `POST /api/board-tasks/{id}/assign|complete|unassign`

### Agents
- `GET /api/agents` — List agents
- `POST /api/agents/{id}/suspend|resume|retire|archive`
- `GET /api/agents/{id}/lineage` — Lineage graph

### Spawn
- `GET /api/spawn` — List spawn requests
- `POST /api/spawn` — Create spawn request
- `POST /api/spawn/{id}/approve|deny`

### Runs
- `GET /api/runs` — List runs
- `POST /api/runs` — Create run
- `POST /api/runs/{id}/start|complete|fail|cancel`

### Triggers
- `GET /api/triggers` — List triggers
- `POST /api/triggers` — Create trigger
- `POST /api/triggers/{id}/pause|resume|archive`

### Governance
- `GET /api/governance/policies|quotas|spend`
- `PUT /api/governance/policies|quotas`

### Admin
- `GET /api/health` — Health check
- `GET /api/info` — Version info
- `POST /api/admin/backup` — Create backup
- `POST /api/admin/backup/restore` — Restore from backup
- `POST /api/admin/secrets/scan` — Scan for leaked secrets

### Auth
- `POST /api/auth/login` — Email + password login (returns JWT)
- `PUT /api/auth/change-password` — Change password
- `GET /api/auth/pats` — List PATs
- `POST /api/auth/pats` — Create PAT
- `POST /api/auth/pats/{id}/revoke` — Revoke PAT

### Data Holds
- `GET /api/governance/holds` — List active holds
- `POST /api/governance/holds` — Place a data hold
- `POST /api/governance/holds/{id}/release` — Release a hold

### Groups
- `GET /api/org/groups` — List groups
- `GET /api/org/groups/{id}` — Get group
- `POST /api/org/groups` — Create group
- `POST /api/org/groups/{id}/archive` — Archive group
- `PUT /api/org/groups/{id}/leader` — Change group leader

### Memory
- `GET /api/memory` — List memory items (filter: memberId, workspaceId, tainted)
- `GET /api/memory/{id}` — Get memory item
- `POST /api/memory` — Create memory item
- `POST /api/memory/{id}/review` — Review (cleared) memory item

### Nodes
- `GET /api/nodes` — List nodes
- `GET /api/nodes/{id}` — Get node
- `POST /api/nodes/enroll` — Enroll a new node
- `POST /api/nodes/{id}/heartbeat` — Node heartbeat
- `POST /api/nodes/{id}/claims` — Claim workspace (lease)
- `POST /api/nodes/{id}/work/pull` — Pull queued work
- `POST /api/nodes/{id}/runs/{runId}/report` — Report run results
- `POST /api/nodes/{id}/revoke` — Revoke node
- `PUT /api/nodes/{id}` — Update node metadata

### Role Templates
- `GET /api/role-templates` — List templates
- `GET /api/role-templates/{id}` — Get template
- `POST /api/role-templates` — Create template
- `POST /api/role-templates/{id}/publish` — Publish template
- `POST /api/role-templates/{id}/retire` — Retire template

### Workspaces
- `GET /api/workspaces` — List workspaces
- `GET /api/workspaces/{id}` — Get workspace
- `POST /api/workspaces` — Create workspace
- `POST /api/workspaces/{id}/rebind` — Rebind workspace to member
- `POST /api/workspaces/{id}/archive` — Archive workspace

## Deployment

### Single Container (recommended)
```bash
docker run -d \
  -p 8080:8080 \
  -v summa-data:/data \
  -e SUMMA_JWT_SECRET=<your-secret> \
  -e SUMMA_OIDC_ISSUER=https://keycloak.example.com/realms/summa \
  summa:latest
```

### Docker Compose
```bash
export SUMMA_JWT_SECRET=$(openssl rand -hex 32)
docker compose up -d --build
```

### OCI Images (Podman)
```bash
podman build -t summa .
podman run -d -p 8080:8080 summa
```

## Backup & Restore

```bash
# Create backup
curl -X POST http://localhost:8080/api/admin/backup \
  -H 'Content-Type: application/json' \
  -d '{"backupDir": "/backups"}'

# Restore
curl -X POST http://localhost:8080/api/admin/backup/restore \
  -H 'Content-Type: application/json' \
  -d '{"backupPath": "/backups/summa-backup-2026-01-01T00-00-00Z.zip"}'
```

## Security Checklist

- [ ] Set `SUMMA_JWT_SECRET` to a 256-bit random value (`openssl rand -hex 32`)
- [ ] Enable TLS behind reverse proxy
- [ ] Set protected branches on DNA repo
- [ ] Configure firewall for ports 8080 (API) and 3000 (console, compose only)
- [ ] Rotate JWT secret annually
- [ ] Back up database and DNA repo daily

## Monitoring

- Health: `GET /api/health`
- Info: `GET /api/info`
- Audit: `GET /api/org/audit?limit=100`
- Spend: `GET /api/governance/spend`

## Troubleshooting

### Database locked
```bash
# Check WAL mode
sqlite3 ~/.summa/summa.db "PRAGMA journal_mode;"
# Should return: WAL
```

### DNA repo divergence
```bash
cd ~/.summa/dna
git log --oneline -5
# If diverged: git reset --hard origin/main
```

### Last admin guard
Cannot offboard the last active admin. Create a second admin first:
```bash
curl -X POST http://localhost:8080/api/org/humans \
  -H 'Content-Type: application/json' \
  -d '{"name":"New Admin","email":"admin2@example.com","rbac":"admin"}'
```

## Spec Compliance

All requirements trace to `specs/` with IDs like `API-001`, `DWP-001`, etc.
Lint: `python3 tools/lint_specs.py`
Self-tests: `python3 tools/test_lint.py`
