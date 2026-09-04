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

| Component | Path (local) | Path (Docker/prod) | Description |
|-----------|--------------|---------------------|-------------|
| SQLite DB | `~/.summa/summa.db` | `/data/db/summa.db` | All runtime state (WAL mode) |
| DNA Git Repo | `~/.summa/dna` | `/data/dna` | Canonical DNA store (markdown) |
| Logs | stdout/stderr | stdout/stderr | Application logs (collect via `journald` / `docker compose logs`) |

> Override paths via `SUMMA_DB_PATH` and `SUMMA_DNA_REPO`. In containers, `~` is not expanded — use absolute paths.

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SUMMA_DB_PATH` | `~/.summa/summa.db` (local) · `/data/db/summa.db` (Docker/prod) | SQLite database path (`~` is not expanded in containers — use absolute paths) |
| `SUMMA_DNA_REPO` | `~/.summa/dna` (local) · `/data/dna` (Docker/prod) | DNA git repository path |
| `SUMMA_JWT_SECRET` | *(required)* | JWT signing secret (256+ bits; generate with `openssl rand -hex 32`) |
| `SUMMA_SPEND_CEILING` | `1000000` | Org spend ceiling |
| `SUMMA_CORS_ORIGINS` | *(empty = localhost + 127.0.0.1 only)* | Extra CORS origins, comma-separated (e.g. `https://app.example.com`) |
| `SPRING_PROFILES_ACTIVE` | `prod` (Dockerfile, docker-compose, start.sh) · `dev` (dev.sh, hot-reload) | Spring profile |
| `VITE_API_URL` | `/api` | API base URL baked into the console bundle at build time |
| `VITE_SUMMA_MODE` | `single-process` | Console mode badge: `single-process` or `multi-node` |
| `SUMMA_OIDC_ISSUER` | *(reserved)* | Keycloak issuer URI — **not yet wired**. Human auth today is email+password via `POST /api/auth/login`. See OIDC note below. |
| `SUMMA_OIDC_CLIENT_ID` | *(reserved)* | OIDC client ID — not yet wired |
| `SUMMA_OIDC_CLIENT_SECRET` | *(reserved)* | OIDC client secret — not yet wired |

> **OIDC note:** `SUMMA_OIDC_*` variables are reserved for a planned Keycloak integration. Human auth today is email + password via `POST /api/auth/login`. Keycloak OIDC was decided per PLAN §14.2 / SEC-001 but not yet implemented — these variables have no effect.

## API Endpoints

Abridged — the full surface with REQ IDs lives in `specs/17-api-surface.md`
(the controllers under `backend/src/main/java/com/summa/controller/` are authoritative).

### Auth & Bootstrap
- `POST /api/auth/login` — Email + password login (returns JWT)
- `POST /api/org/bootstrap` — First-run company + admin creation (public, first-run only; body: `{"name": "<string>", "email": "<email>", "password": "<8+ chars, upper+lower+digit>"}`). Always creates an `admin`; `rbac` is ignored.

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
- `GET /api/dna/search?q=...&domainId=<id>&limit=<n>` — FTS5 search (limit defaults to 20, max 100)

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
- `GET /api/agents/{id}` — Get agent
- `POST /api/agents/{id}/suspend` — Suspend agent
- `POST /api/agents/{id}/resume` — Resume agent
- `POST /api/agents/{id}/retire` — Retire agent
- `POST /api/agents/{id}/archive` — Archive agent
- `POST /api/agents/{id}/deny` — Deny agent spawn request
- `POST /api/agents/{id}/promote` — Promote agent
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
- `GET /api/governance/policies` — List policies
- `GET /api/governance/quotas` — List quotas
- `GET /api/governance/spend` — View spend
- `PUT /api/governance/policies` — Update policies
- `PUT /api/governance/quotas` — Update quotas
- `POST /api/governance/spend/overruns/{id}/ack` — Acknowledge spend overrun

### Admin
- `GET /api/health` — Health check
- `GET /api/info` — Version info
- `POST /api/admin/backup` — Create backup
- `POST /api/admin/backup/restore` — Restore from backup
- `POST /api/admin/secrets/scan` — Scan for leaked secrets

### Auth
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
- `POST /api/workspaces/{id}/rebind` — Rebind workspace to node (body: `{"targetNodeId": "<node-id>"}`)
- `POST /api/workspaces/{id}/archive` — Archive workspace

## Deployment

### Single Container (API only, no console)
```bash
docker run -d \
  -p 8080:8080 \
  -v summa-data:/data \
  -e SUMMA_JWT_SECRET=<your-secret> \
  summa:latest
```
> This image serves the API on `:8080` only. The console is available via `./dev.sh` (:3000) or the `docker compose up -d` console service.

### Docker Compose
```bash
# Either: set in your environment
export SUMMA_JWT_SECRET=$(openssl rand -hex 32)
# Or: copy .env.example and fill in the secret
cp .env.example .env
# (then edit .env to set SUMMA_JWT_SECRET)

docker compose up -d --build
```
> `docker-compose.yml` uses `${SUMMA_JWT_SECRET:?...}` which fails fast if the variable is unset. Compose auto-loads `.env` from the current directory.

### OCI Images (Podman)
```bash
podman build -t summa .
podman run -d \
  -p 8080:8080 \
  -v summa-data:/data \
  -e SUMMA_JWT_SECRET=$(openssl rand -hex 32) \
  summa
```

## Backup & Restore

```bash
# Create backup
curl -X POST http://localhost:8080/api/admin/backup \
  -H 'Content-Type: application/json' \
  -d '{"backupDir": "/tmp"}'

# Restore
curl -X POST http://localhost:8080/api/admin/backup/restore \
  -H 'Content-Type: application/json' \
  -d '{"backupPath": "/tmp/summa-backup-2026-01-01T00-00-00Z.zip"}'
```
> Backups must reside under the JVM's `java.io.tmpdir` (typically `/tmp`). Paths outside this root are rejected with 400.

## Security Checklist

- [ ] Set `SUMMA_JWT_SECRET` to a 256-bit random value (`openssl rand -hex 32`)
- [ ] Enable TLS behind reverse proxy
- [ ] Set protected branches on DNA repo
- [ ] Configure firewall for ports 8080 (API, always) and 3000 (console, dev.sh / compose only)
- [ ] Rotate JWT secret annually
- [ ] Back up database and DNA repo daily

## Monitoring

- Health: `GET /api/health`
- Info: `GET /api/info`
- Audit: `GET /api/org/audit?limit=100`
- Spend: `GET /api/governance/spend`

## Troubleshooting

### Database locked
> Requires `sqlite3` CLI (`apt install sqlite3`).
```bash
# Check WAL mode
sqlite3 ~/.summa/summa.db "PRAGMA journal_mode;"
# Should return: WAL
```

### DNA repo divergence
```bash
# Backup first — divergence quarantines per PLAN §4.5; prefer console review queue over force-push
cp -a ~/.summa/dna /tmp/dna-backup-$(date +%F)
cd ~/.summa/dna
git status
git log --oneline -5
# Only after confirming plane state: do NOT run git reset --hard unless you have reviewed the divergence
```

### Last admin guard
Cannot offboard the last active admin. Create a second admin first via bootstrap or by promoting an existing human:
```bash
# Option A: promote an existing human to admin
curl -X PUT http://localhost:8080/api/org/humans/<id>/rbac \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <jwt>' \
  -d '{"rbac":"admin"}'

# Option B: re-bootstrap (only works if org was never bootstrapped)
curl -X POST http://localhost:8080/api/org/bootstrap \
  -H 'Content-Type: application/json' \
  -d '{"name":"Admin","email":"admin@example.com","password":"ChangeMe123"}'
```
> `password` must be ≥8 characters with at least one uppercase, one lowercase, and one digit.

## Spec Compliance

All requirements trace to `specs/` with IDs like `API-001`, `DWP-001`, etc.
Lint: `python3 tools/lint_specs.py`
Self-tests: `python3 tools/test_lint.py`
