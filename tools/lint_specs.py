#!/usr/bin/env python3
"""Structural linter for the Summa SDD spec suite.

Checks (errors exit 1):
  1. every requirement ID (PREFIX-NNN) is defined exactly once across the suite;
  2. every prefix has exactly one home module file;
  3. every REQ-ID citation in specs/*.md and PLAN.md resolves to a definition
     (ranges like `ARC-020…024` and `ARC-020…ARC-024` are expanded; cross-prefix
     and reversed ranges are errors);
  4. TRACEABILITY.md coverage is exact in both directions: every defined ID is
     listed, every listed ID is defined.

Warnings (do not fail): definitions whose text carries no RFC-2119-style verb.

CHANGELOG.md is not linted — it is a verbatim historical record.

Run: python3 tools/lint_specs.py
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SPECS = ROOT / "specs"

DEF_RE = re.compile(r"^- \*\*([A-Z]{3})-(\d{3})\*\*")
TOKEN_RE = re.compile(r"\b([A-Z]{3})-(\d{3})\b")
RANGE_RE = re.compile(r"\b([A-Z]{3})-(\d{3})\s*(?:…|\.\.)\s*(?:([A-Z]{3})-)?(\d{3})\b")
KEYWORD_RE = re.compile(
    r"\b(shall|should|may|must|never|refus|forbid|denied?|carries|is|are|derives|evaluates)\b",
    re.IGNORECASE,
)


def expand(prefix: str, lo: int, hi: int) -> list[str]:
    if hi < lo or hi - lo > 999:
        return []
    return [f"{prefix}-{i:03d}" for i in range(lo, hi + 1)]


def scan_ranges(text: str, where: str, errors: list[str]) -> set[str]:
    """Expand every `PFX-001…003` / `PFX-001…PFX-003` range in `text`.

    A repeated end prefix must match the start prefix, and a reversed range is
    a typo — both are errors, not silently skipped scans.
    """
    ids: set[str] = set()
    for m in RANGE_RE.finditer(text):
        prefix, lo = m.group(1), int(m.group(2))
        end_prefix, hi = m.group(3), int(m.group(4))
        if end_prefix and end_prefix != prefix:
            errors.append(f"{where}: cross-prefix range {m.group(0)!r}")
            continue
        if hi < lo:
            errors.append(f"{where}: reversed range {m.group(0)!r}")
            continue
        ids.update(expand(prefix, lo, hi))
    return ids


def module_number(path: Path) -> int:
    return int(re.match(r"(\d+)-", path.name).group(1))


def main() -> int:
    errors: list[str] = []
    warnings: list[str] = []

    # --- 1+2. definitions and prefix homes ---
    defined: dict[str, Path] = {}
    prefix_home: dict[str, set[Path]] = {}
    def_text: dict[str, list[str]] = {}

    modules = sorted(SPECS.glob("[0-9][0-9]-*.md"))
    for path in modules:
        lines = path.read_text(encoding="utf-8").split("\n")
        current: list[str] | None = None
        for line in lines:
            m = DEF_RE.match(line)
            if m:
                rid = f"{m.group(1)}-{m.group(2)}"
                if rid in defined:
                    errors.append(
                        f"duplicate definition {rid}: {defined[rid].name} and {path.name}"
                    )
                defined[rid] = path
                prefix_home.setdefault(m.group(1), set()).add(path)
                current = def_text.setdefault(rid, [])
                current.append(line)
            elif current is not None:
                if line.startswith(("- ", "#", "```")) or (
                    not line.strip() and current and current[-1] == ""
                ):
                    current = None
                else:
                    current.append(line)
    for prefix, homes in sorted(prefix_home.items()):
        if len(homes) > 1:
            errors.append(
                f"prefix {prefix} defined in multiple modules: "
                + ", ".join(sorted(p.name for p in homes))
            )

    # --- 3. citation resolution across specs/ and PLAN.md ---
    check_files = sorted(SPECS.glob("*.md")) + [ROOT / "PLAN.md"]
    for path in check_files:
        text = path.read_text(encoding="utf-8")
        covered: set[str] = scan_ranges(text, path.name, errors)
        for m in TOKEN_RE.finditer(text):
            covered.add(f"{m.group(1)}-{m.group(2)}")
        covered -= set(defined)
        if covered:
            errors.append(
                f"{path.name}: dangling citations {', '.join(sorted(covered))}"
            )

    # --- 4. TRACEABILITY exact coverage, both directions ---
    trace = (SPECS / "TRACEABILITY.md").read_text(encoding="utf-8")
    listed: set[str] = scan_ranges(trace, "TRACEABILITY.md", errors)
    for m in TOKEN_RE.finditer(trace):
        listed.add(f"{m.group(1)}-{m.group(2)}")

    # rows cite module names like "15 (ref-only)" and ids in prose; the token scan
    # already collected every id-shaped token in the table, which is what we need
    missing = sorted(set(defined) - listed)
    phantom = sorted(listed - set(defined))
    if missing:
        errors.append(f"TRACEABILITY.md: defined IDs never listed: {', '.join(missing)}")
    if phantom:
        errors.append(f"TRACEABILITY.md: listed IDs not defined: {', '.join(phantom)}")

    # --- warnings: keyword-less requirement text ---
    for rid, text_lines in sorted(def_text.items()):
        body = " ".join(text_lines)
        if not KEYWORD_RE.search(body):
            warnings.append(f"{rid} ({defined[rid].name}): no RFC-2119-style verb found")

    # --- report ---
    print(f"{len(defined)} requirement IDs across {len(modules)} modules")
    print(f"prefix homes: {len(prefix_home)}")
    if len(warnings):
        print(
            f"{len(warnings)} requirements lack an explicit RFC-2119 keyword "
            "(advisory — the suite's prevailing style is indicative mood; README keywords)"
        )
    if errors:
        for e in errors:
            print(f"error: {e}", file=sys.stderr)
        print(f"{len(errors)} errors", file=sys.stderr)
        return 1
    print("lint: OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
