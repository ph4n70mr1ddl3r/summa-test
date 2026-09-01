#!/usr/bin/env python3
"""Structural linter for the Summa SDD spec suite.

Checks (errors exit 1):
  1. every requirement ID (PREFIX-NNN) is defined exactly once across the suite;
  2. every prefix has exactly one home module file;
  3. every REQ-ID citation in specs/*.md and PLAN.md resolves to a definition
     (ranges like `ARC-020…024` and `ARC-020…ARC-024` are expanded; cross-prefix
     and reversed ranges are errors);
  4. TRACEABILITY.md coverage is exact in both directions: every defined ID is
     listed, every listed ID is defined;
  5. TRACEABILITY.md coverage is partitioned per row: every ID a coverage row
     cites is homed in a module that row's module column names, and no defined
     ID is cited by more than one coverage row — the only exemptions being the
     IDs declared in TRACEABILITY.md's own "Intentional cross-listings" table,
     each of which must actually appear in two or more rows (no stale entries);
  6. the three version pins agree: PLAN.md's `*Version vX.Y*` line, README's
     "derived from `PLAN.md` (vX.Y)", and TRACEABILITY.md's header;
  7. every `§N[.N]` section reference in specs/*.md and in PLAN.md's own body —
     ranges like `§8.1–8.9` expanded — resolves to a numbered heading of
     PLAN.md or, for a section whose body is a numbered list carrying no
     numbered subsections of its own, to one of its items (`§2.9` = principle
     nine, `§14.3` = decision three).

Warnings (do not fail): definitions whose text carries no RFC-2119-style verb.

CHANGELOG.md is not linted — it is a verbatim historical record.

Run: python3 tools/lint_specs.py [ROOT]  — ROOT defaults to this repository; the
fixture-based self-tests (tools/test_lint.py) invoke it against other roots.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

DEFAULT_ROOT = Path(__file__).resolve().parent.parent

DEF_RE = re.compile(r"^- \*\*([A-Z]{3})-(\d{3})\*\*")
TOKEN_RE = re.compile(r"\b([A-Z]{3})-(\d{3})\b")
RANGE_RE = re.compile(r"\b([A-Z]{3})-(\d{3})\s*(?:…|\.\.)\s*(?:([A-Z]{3})-)?(\d{3})\b")
KEYWORD_RE = re.compile(
    r"\b(shall|should|may|must|never|refus[ea]?|forbid|denied?|carries|is|are|derives|evaluates)\b",
    re.IGNORECASE,
)
PLAN_SEC_RE = re.compile(r"^#{2,4}\s+(\d+(?:\.\d+)*)\.?\s", re.M)
SEC_TOKEN_RE = re.compile(r"§(\d+(?:\.\d+)?)")
SEC_RANGE_RE = re.compile(r"§(\d+(?:\.\d+)?)\s*[–—-]\s*(?:§)?(\d+(?:\.\d+)?)")
VERSION_PINS = (
    ("PLAN.md", r"(?m)^\*Version v(\d+\.\d+)"),
    ("specs/README.md", r"derived from\s*`PLAN\.md`\s*\(v(\d+\.\d+)\)"),
    ("specs/TRACEABILITY.md", r"`PLAN\.md` \(v(\d+\.\d+)\)"),
)


def expand(prefix: str, lo: int, hi: int) -> list[str]:
    if hi < lo:
        return []
    if hi - lo > 999:
        raise ValueError(f"range {prefix}-{lo:03d}…{prefix}-{hi:03d} spans {hi - lo + 1} IDs (max 1000)")
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
        try:
            ids.update(expand(prefix, lo, hi))
        except ValueError as ve:
            errors.append(f"{where}: {ve}")
    return ids


def ids_in(text: str, where: str = "<memory>", errors: list[str] | None = None) -> set[str]:
    """Every REQ ID mentioned in `text`, ranges expanded."""
    buf: list[str] = []
    errors = errors if errors is not None else buf
    ids = scan_ranges(text, where, errors)
    for m in TOKEN_RE.finditer(text):
        ids.add(f"{m.group(1)}-{m.group(2)}")
    return ids


def expand_sections(lo: str, hi: str) -> list[str] | None:
    """Expand `§8.1–8.9`-style ranges; None when the pair is not expandable."""
    lo_parts, hi_parts = lo.split("."), hi.split(".")
    if len(lo_parts) != len(hi_parts):
        return None
    if len(lo_parts) == 1:
        nums = range(int(lo), int(hi) + 1)
        return [] if hi < lo else [str(n) for n in nums]
    if lo_parts[:-1] != hi_parts[:-1]:
        return None
    nums = range(int(lo_parts[1]), int(hi_parts[1]) + 1)
    return [] if hi < lo else [f"{lo_parts[0]}.{n}" for n in nums]


def sections_in(text: str, where: str, errors: list[str]) -> set[str]:
    """Every `§N.N` section cited in `text`, `§A.B–A.C` ranges expanded."""
    secs: set[str] = set()
    for m in SEC_RANGE_RE.finditer(text):
        expanded = expand_sections(m.group(1), m.group(2))
        if expanded is None or not expanded:
            errors.append(f"{where}: malformed section range {m.group(0)!r}")
            continue
        secs.update(expanded)
    for m in SEC_TOKEN_RE.finditer(text):
        secs.add(m.group(1))
    return secs


def module_number(path: Path) -> int:
    return int(re.match(r"(\d+)-", path.name).group(1))


def cross_listed_ids(trace: str, errors: list[str]) -> set[str]:
    """IDs declared in TRACEABILITY.md's `## Intentional cross-listings` table.

    Only the table's ID column counts — the reason column is prose and may cite
    other IDs without granting them exemptions.
    """
    m = re.search(r"^## Intentional cross-listings\s*$", trace, re.M)
    if not m:
        return set()
    rest = trace[m.end() :]
    nxt = re.search(r"^## ", rest, re.M)
    section = rest if nxt is None else rest[: nxt.start()]
    ids: set[str] = set()
    for line in section.split("\n"):
        if line.startswith("|"):
            ids |= ids_in(line.split("|")[1], "TRACEABILITY.md", errors)
    return ids


def check_version_pins(root: Path, errors: list[str]) -> None:
    pins: dict[str, str] = {}
    for rel, pattern in VERSION_PINS:
        m = re.search(pattern, (root / rel).read_text(encoding="utf-8"))
        if m:
            pins[rel] = m.group(1)
        else:
            errors.append(f"version pin not found in {rel}")
    if len(set(pins.values())) > 1:
        detail = ", ".join(f"{k} v{v}" for k, v in sorted(pins.items()))
        errors.append(f"version pins disagree: {detail}")


def plan_sections(root: Path) -> set[str]:
    """Valid § targets in PLAN.md: numbered headings, plus the items of a
    top-level section's numbered list when the section carries no numbered
    `### N.M` subsections of its own — §2's principles and §14's decisions
    are addressable as §2.9 / §14.3 without being headings. Fenced code
    blocks are stripped first so schema lines can't pose as list items.
    """
    text = (root / "PLAN.md").read_text(encoding="utf-8")
    secs: set[str] = set(PLAN_SEC_RE.findall(text))
    for m in re.finditer(r"^## (\d+)\.?[ \t].*?(?=^## \d+\.?[ \t]|\Z)", text, re.M | re.S):
        num, body = m.group(1), re.sub(r"```.*?```", "", m.group(0), flags=re.S)
        if re.search(rf"^### {num}\.\d+", body, re.M):
            continue
        secs.update(f"{num}.{i}" for i in re.findall(r"^(\d+)\.[ \t]", body, re.M))
    return secs


def check_section_refs(root: Path, specs: Path, errors: list[str]) -> None:
    plan_secs = plan_sections(root)

    def sec_key(s: str) -> tuple[int, ...]:
        return tuple(int(p) for p in s.split("."))

    for path in sorted(specs.glob("*.md")) + [root / "PLAN.md"]:
        text = path.read_text(encoding="utf-8")
        dangling = sections_in(text, path.name, errors) - plan_secs
        if dangling:
            listed = ", ".join(f"§{s}" for s in sorted(dangling, key=sec_key))
            errors.append(f"{path.name}: dangling PLAN section references: {listed}")


def check_traceability_partition(
    trace: str, defined: dict[str, Path], errors: list[str]
) -> set[str]:
    """Per-row partition of the coverage table; returns the cross-listed IDs."""
    cross = cross_listed_ids(trace, errors)
    row_ids: dict[str, list[str]] = {}
    for row in (l for l in trace.split("\n") if l.startswith("| §")):
        cells = [c.strip() for c in row.split("|")]
        label, mods = cells[1], {int(n) for n in re.findall(r"\b(\d{2})\b", cells[2])}
        for rid in sorted(ids_in(row) & set(defined)):
            row_ids.setdefault(rid, []).append(label)
            home = module_number(defined[rid])
            if rid not in cross and home not in mods:
                errors.append(
                    f"TRACEABILITY.md: row {label[:40]!r} cites {rid}, homed in "
                    f"module {home:02d}, which the row's module column does not name"
                )
    for rid in sorted(defined):
        n_rows = len(row_ids.get(rid, []))
        if n_rows > 1 and rid not in cross:
            errors.append(
                f"TRACEABILITY.md: {rid} is cited by {n_rows} coverage rows but is not "
                "in the Intentional cross-listings table"
            )
        if rid in cross and n_rows < 2:
            errors.append(
                f"TRACEABILITY.md: cross-listed {rid} appears in only {n_rows} "
                "coverage row(s) — stale Intentional cross-listings entry"
            )
    return cross


def main(argv: list[str] | None = None) -> int:
    args = sys.argv[1:] if argv is None else argv
    root = Path(args[0]).resolve() if args else DEFAULT_ROOT
    specs = root / "specs"
    errors: list[str] = []
    warnings: list[str] = []

    # --- 1+2. definitions and prefix homes ---
    defined: dict[str, Path] = {}
    prefix_home: dict[str, set[Path]] = {}
    def_text: dict[str, list[str]] = {}

    modules = sorted(specs.glob("[0-9][0-9]-*.md"))
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
    check_files = sorted(specs.glob("*.md")) + [root / "PLAN.md"]
    for path in check_files:
        text = path.read_text(encoding="utf-8")
        covered = ids_in(text, path.name, errors) - set(defined)
        if covered:
            errors.append(
                f"{path.name}: dangling citations {', '.join(sorted(covered))}"
            )

    # --- 4+5. TRACEABILITY exact coverage, both directions, partitioned per row ---
    trace = (specs / "TRACEABILITY.md").read_text(encoding="utf-8")
    listed = ids_in(trace, "TRACEABILITY.md", errors)
    missing = sorted(set(defined) - listed)
    phantom = sorted(listed - set(defined))
    if missing:
        errors.append(f"TRACEABILITY.md: defined IDs never listed: {', '.join(missing)}")
    if phantom:
        errors.append(f"TRACEABILITY.md: listed IDs not defined: {', '.join(phantom)}")
    cross = check_traceability_partition(trace, defined, errors)

    # --- 6. version-pin agreement (PLAN, README, TRACEABILITY) ---
    check_version_pins(root, errors)

    # --- 7. PLAN § references resolve to numbered PLAN headings ---
    check_section_refs(root, specs, errors)

    # --- warnings: keyword-less requirement text ---
    for rid, text_lines in sorted(def_text.items()):
        body = " ".join(text_lines)
        if not KEYWORD_RE.search(body):
            warnings.append(f"{rid} ({defined[rid].name}): no RFC-2119-style verb found")

    # --- report ---
    print(f"{len(defined)} requirement IDs across {len(modules)} modules")
    print(f"prefix homes: {len(prefix_home)}")
    print(f"intentional cross-listings: {len(cross)}")
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
