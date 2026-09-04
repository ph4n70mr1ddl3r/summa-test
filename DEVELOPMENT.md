# Summa Development Guide

## Project Structure

```
summa/
├── backend/           # Java 21 LTS + Spring Boot 3.4
│   ├── src/main/java/com/summa/
│   │   ├── controller/    # REST endpoints (API-001..061)
│   │   ├── service/       # Business logic
│   │   ├── repository/    # JPA data access
│   │   ├── model/         # JPA entities (DAT-010)
│   │   ├── enums/         # Type enums
│   │   ├── security/      # Auth filters
│   │   ├── config/        # App configuration (incl. SchemaInitializer, WebConfig)
│   │   ├── exception/     # Exception handlers
│   │   └── util/          # Shared utilities
│   ├── src/main/resources/  # application*.yml, schema.sql (applied by SchemaInitializer)
│   └── src/test/java/     # Unit tests
├── console/           # React 19 + TypeScript + Vite
│   ├── src/
│   │   ├── pages/       # Route components
│   │   ├── services/    # API client
│   │   └── components/  # Reusable UI (placeholder)
│   ├── public/
│   ├── nginx.conf     # Compose reverse proxy (/api → backend)
│   └── vite/vitest/eslint configs
├── specs/             # Normative requirements (see specs/README.md, specs/TRACEABILITY.md)
├── tools/             # Lint tooling + fixtures (tools/fixtures/)
├── .github/workflows/ # CI (backend tests, console build+lint, spec lint)
├── Dockerfile         # Multi-stage backend image (builds JAR from source)
├── Dockerfile.console # Console build (repo-root context) + nginx
├── docker-compose.yml
└── start.sh / dev.sh
```

> Abbreviated — `find backend console specs tools .github -maxdepth 3` shows the full tree.

## Running Locally

```bash
# Start everything
./dev.sh

# Or just backend
cd backend && mvn spring-boot:run

# Or just console
cd console && npm run dev
```

## Key Architectural Decisions

1. **Single-process mode first**: Everything runs in one JVM process. Scale by adding node containers later.
2. **SQLite WAL**: All state in one SQLite file with write-ahead logging for concurrency.
3. **Git-backed DNA**: Markdown files in `~/.summa/dna/` are the canonical store; SQLite indexes them.
4. **Audit-first**: Every write is audited. Refusals write audit events and raise asks.
5. **PRN-009 Universal Fallback**: No silent failures — refuse, audit, ask.

## Testing

```bash
# Full suite from the repo root (orchestrated by root package.json)
npm test            # backend (mvn) + spec self-tests + console (vitest)

# Targeted runs
npm run test:backend   # == cd backend && mvn test
npm run test:console   # == cd console && npm test
npm run test:specs     # == python3 tools/test_lint.py

# Spec lint
python3 tools/lint_specs.py
python3 tools/test_lint.py
```

## Spec Traceability

Every requirement has a unique ID: `PREFIX-NNN`.
- `API-*` = API surface
- `DGV-*` = DNA governance
- `DWP-*` = DNA write path
- `DRP-*` = DNA read path
- `SPW-*` = Spawning
- `ASK-*` = Asks
- `CLC-*` = Agent lifecycle
- `ORG-*` = Org model
- `NFR-*` = Non-functional
- `SEC-*` = Security
- `STG-*` = Storage
- `TPL-*` = Templates
- `INT-*` = Initiatives
- `SUB-*` = Subsystems
- `DAT-*` = Data model
- `DNC-*` = DNA content
- `ARC-*` = Architecture
- `VIS-*` = Vision
- `PRN-*` = Principles
- `CFG-*` = Configuration
- `DLV-*` = Delivery
- `OFB-*` = Offboarding

Find IDs in `specs/` module files. Every behavior is testable against these IDs.

## Adding a New Endpoint

1. Add model entity in `backend/src/main/java/com/summa/model/`
2. Add repository in `backend/src/main/java/com/summa/repository/`
3. Add service in `backend/src/main/java/com/summa/service/`
4. Add controller in `backend/src/main/java/com/summa/controller/`
5. Add test in `backend/src/test/java/com/summa/service/`
6. Update `specs/17-api-surface.md` with new REQ IDs
7. Update `specs/TRACEABILITY.md`
8. Run `python3 tools/lint_specs.py`

## Deployment

See `PRODUCTION.md` for full deployment guide.

## Status

- Phase 0 (Foundations): Complete
- Phase 1 (MVP Agent): Complete
- Phase 2 (Identity/Memory): In Progress
- Phase 3 (DNA v1): In Progress
- Phase 4 (Automation): In Progress
- Phase 5-7: Pending
