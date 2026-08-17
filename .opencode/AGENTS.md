# Agent Configuration

## Skills

### implement-project
Used when implementing a new project or feature from spec documents.

### run-tests
Run tests and verify build passes.

### lint-specs
Run spec linter to verify requirements compliance.

## Permissions

### File Access
- Read: all files in workspace
- Write: backend/, console/, docs/
- Restricted: .git/, node_modules/, target/

### Tool Access
- bash: allowed for build, test, lint commands
- read: allowed for all files
- write: allowed for source files only
- webfetch: allowed for documentation lookups

### Execution Rules
1. Always run lint before committing
2. Run relevant tests after changes
3. Verify spec compliance with lint_specs.py
4. Never commit secrets or credentials
5. Build must succeed before marking phase complete

## Workflow

### New Phase Implementation
1. Read relevant spec modules
2. Implement backend services/controllers/models
3. Add corresponding tests
4. Verify compilation and test passage
5. Update console UI if needed
6. Commit with descriptive message

### Verification Checklist
- [ ] Backend compiles (`mvn compile`)
- [ ] Unit tests pass (`mvn test`)
- [ ] Console builds (`npm run build`)
- [ ] Spec lint green (`python3 tools/lint_specs.py`)
- [ ] Linter self-tests pass (`python3 tools/test_lint.py`)
