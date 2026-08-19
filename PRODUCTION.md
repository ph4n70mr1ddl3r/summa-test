# Summa — Production Runbook

## Quick Start

```bash
# Single-process mode (development/small team)
./start.sh

# Dev mode (backend + console hot-reload)
./dev.sh
```

## Architecture

```
┌─────────────────────────────────────────────────────┐
│              Single-Process Mode                      │
│  Spring Boot 3.4 + SQLite (WAL) + FTS5              │
│  Port 8080 (API) / 3000 (console via proxy)         │
└─────────────────────────────────────────────────────┘
```

## Data Storage

| Component | Path | Description |
|-----------|------|-------------|
| SQLite DB | `~/.summa/summa.db` | All runtime state (WAL mode) |
| DNA Git Repo | `~/.summa/dna` | Canonical DNA store (markdown) |
| Logs | `target/logs/` | Application logs |

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SUMMA_DB_PATH` | `~/.summa/summa.db` | SQLite database path |
| `SUMMA_DNA_REPO` | `~/.summa/dna` | DNA git repository path |
| `SUMMA_JWT_SECRET` | *(required)* | JWT signing secret (256+ bits) |
| `SUMMA_SPEND_CEILING` | `1000000` | Org spend ceiling |
| `SUMMA_OIDC_ISSUER` | | Keycloak issuer URI |
| `SUMMA_OIDC_CLIENT_ID` | | OIDC client ID |
| `SUMMA_OIDC_CLIENT_SECRET` | | OIDC client secret |

## API Endpoints

### Auth & Bootstrap
- `POST /api/auth/login` — OIDC token exchange
- `POST /api/org/bootstrap` — First-run company + admin creation

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
docker-compose up -d
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

- [ ] Set `SUMMA_JWT_SECRET` to a 256-bit random value
- [ ] Configure OIDC/Keycloak for human auth
- [ ] Enable TLS behind reverse proxy
- [ ] Set protected branches on DNA repo
- [ ] Configure firewall for port 8080
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
