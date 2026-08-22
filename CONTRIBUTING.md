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
python3 tools/test_lint.py
```

These checks verify:
- Every REQ ID is unique
- Every REQ ID is referenced in at least one spec file
- Required sections exist in each spec module
- Traceability matrix is consistent

### Adding a New Requirement

1. Determine the prefix (see `DEVELOPMENT.md` section on IDs).
2. Add the requirement to the appropriate spec module in `specs/`.
3. Run `python3 tools/lint_specs.py` to validate.
4. Implement the behavior in `backend/` or `console/`.
5. Add tests.
6. Update `specs/TRACEABILITY.md`.

### Adding a New Endpoint

1. Add model entity in `backend/src/main/java/com/summa/model/`
2. Add repository in `backend/src/main/java/com/summa/repository/`
3. Add service in `backend/src/main/java/com/summa/service/`
4. Add controller in `backend/src/main/java/com/summa/controller/`
5. Add test in `backend/src/test/java/com/summa/service/`
6. Update `specs/17-api-surface.md` with new REQ IDs
7. Update `specs/TRACEABILITY.md`
8. Run `python3 tools/lint_specs.py`

## Code Style

- **Backend**: Follows standard Java conventions. Use SLF4J for logging
  (`private static final Logger log = LoggerFactory.getLogger(...)`).
- **Console**: TypeScript strict mode. Tailwind CSS for styling.
- **specs**: One requirement per line, unique ID, testable language.

## Testing

```bash
# Backend tests
cd backend && mvn test

# Console tests
cd console && npm test

# Full suite
mvn test && npm test && python3 tools/lint_specs.py
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

## Reporting Issues

Report issues at https://github.com/summa-org/summa/issues with:
- The spec ID(s) affected
- Steps to reproduce
- Expected vs. actual behavior
