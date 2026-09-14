# Contributing to Summa

## Development Workflow

Summa follows **Spec-Driven Development (SDD)**. Every behavior is defined in
`specs/` before implementation. The normative requirement IDs (e.g. `API-001`,
`DWP-003`) are machine-checked by the linter.

### Prerequisites

- Java 21+
- Node.js 22+
- Maven 3.9+
- Python 3.10+

### Spec Linting

Before committing, run the spec linter to validate structural integrity:

```bash
python3 tools/lint_specs.py
```

These checks verify:
- Every REQ ID is unique
- Every REQ ID is referenced in at least one spec file
- Required sections exist in each spec module
- Traceability matrix is consistent

You can also run the self-test suite with:

```bash
python3 tools/test_lint.py
```

### Adding a New Requirement

1. Determine the prefix (see `DEVELOPMENT.md` section on IDs).
2. Add the requirement to the appropriate spec module in `specs/`.
3. If the requirement touches the data model, update `specs/16-data-model.md` and `backend/src/main/resources/schema.sql`.
4. Run `python3 tools/lint_specs.py` to validate.
5. Implement the behavior in `backend/` or `console/`.
6. Add tests.
7. Update `specs/TRACEABILITY.md`.

### Adding a New Endpoint

See `DEVELOPMENT.md#adding-a-new-endpoint` for the full procedure.

## Code Style

- **Backend**: Follows standard Java conventions. Use SLF4J for logging
  (`private static final Logger log = LoggerFactory.getLogger(...)`).
- **Console**: TypeScript strict mode. Tailwind CSS for styling.
- **specs**: One requirement per line, unique ID, testable language.

## Testing

```bash
# Backend tests
cd backend && mvn test -Dspring.profiles.active=test

# Console tests
cd console && npm test

# Full suite from the repo root
npm test   # backend + specs + console
```

## Commit Messages

Use conventional commits:

- `feat:` — new feature or requirement
- `fix:` — bug fix
- `refactor:` — code restructuring without behavior change
- `docs:` — documentation update
- `spec:` — spec change
- `test:` — test addition or modification
- `chore:` — build, CI, or tooling change

## Pull Requests

- Each PR should address a single requirement or a small coherent set.
- Include spec ID(s) in the PR title or description.
- Run the full test suite before submitting.
- Ensure `python3 tools/lint_specs.py` passes.

## Security

Summa treats security as a first-class concern. All SEC-* requirements are normative and machine-checked.

### Before You Commit

- **Never commit secrets**: JWT secrets, passwords, API keys, or private keys must not appear in source code or tests. Use environment variables (`SUMMA_JWT_SECRET`, etc.).
- **Run the secrets scanner**: `curl -X POST http://localhost:8080/api/admin/secrets/scan -H 'Content-Type: application/json' -d '{"content": "<your text>"}'` before adding any sensitive strings.
- **Validate auth gates**: Every write endpoint must pass the write gate (`WriteGate`). Admin-only endpoints must explicitly check `RbacRole.ADMIN`.
- **Sanitize all user input**: FTS5 queries, file paths, and SQL parameters must be parameterized. Path traversal guards use `toRealPath()` before `startsWith()`.
- **Prefer `EntityNotFoundException` over `IllegalArgumentException`** for not-found cases — the global handler maps them to 404. Use `IllegalStateException` for gate/refusal logic (mapped to 403/409).

### Security Test Expectations

- New write endpoints need tests for: missing auth (401), insufficient role (403/409), and invalid input (422).
- Auth changes need tests for token expiry, alg:none rejection, and rate-limit behaviour.
- Path/traversal fixes need tests for symlink and `..` attack vectors.

### Reporting Vulnerabilities

Report security issues privately to https://github.com/summa-org/summa/security/advisories/new. Do not open a public issue.
