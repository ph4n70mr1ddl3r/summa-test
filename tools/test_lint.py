#!/usr/bin/env python3
"""Self-tests for lint_specs.py.

Runs the linter against the committed clean fixture corpus (tools/fixtures/clean)
and against one mutated copy of it per failure mode. Every check must stay green
on the clean corpus and fail loudly — with a specific, named error — when its
invariant is broken. Scenarios marked expect_ok exercise positive paths (e.g.
the intentional cross-listings exemption).

Each edit is (relative path, old, new): `old` is replaced by `new` once, and an
empty `old` creates the file with `new` as its content.

Run: python3 tools/test_lint.py
"""

from __future__ import annotations

import shutil
import subprocess
import sys
import tempfile
from dataclasses import dataclass
from pathlib import Path

TOOLS = Path(__file__).resolve().parent
CLEAN = TOOLS / "fixtures" / "clean"

CROSSLIST_SECTION = """
## Intentional cross-listings

| ID | Why |
|---|---|
"""


@dataclass
class Scenario:
    name: str
    edits: list[tuple[str, str, str]]
    expect_ok: bool = False
    fragment: str = ""


SCENARIOS = [
    # --- positive paths ---
    Scenario(
        "ok-crosslist-exemption",
        [
            (
                "specs/TRACEABILITY.md",
                "| §1 Vision | 01 | VIS-001 |",
                "| §1 Vision | 01 | VIS-001, PRN-002 |",
            ),
            (
                "specs/TRACEABILITY.md",
                "PRN-001…002 |\n",
                f"PRN-001…002 |\n{CROSSLIST_SECTION}| PRN-002 | fixture: named by both rows |\n",
            ),
        ],
        expect_ok=True,
    ),
    # --- checks 1+2: definitions and prefix homes ---
    Scenario(
        "duplicate-definition",
        [
            (
                "specs/02-principles.md",
                "- **PRN-001** —",
                "- **VIS-001** — duplicate.\n- **PRN-001** —",
            )
        ],
        fragment="duplicate definition VIS-001",
    ),
    Scenario(
        "prefix-two-homes",
        [
            (
                "specs/03-extra.md",
                "",
                "# SPEC-03 — Extra\n\n- **VIS-009** — stray definition, never cited.\n",
            )
        ],
        fragment="prefix VIS defined in multiple modules",
    ),
    # --- check 3: citation resolution ---
    Scenario(
        "dangling-citation",
        [("specs/01-vision.md", "cites PRN-001", "cites PRN-009")],
        fragment="dangling citations",
    ),
    Scenario(
        "reversed-range",
        [
            (
                "specs/01-vision.md",
                "per §1 (cites PRN-001).",
                "per §1 (cites PRN-001); never VIS-002…VIS-001.",
            )
        ],
        fragment="reversed range",
    ),
    Scenario(
        "cross-prefix-range",
        [
            (
                "specs/01-vision.md",
                "per §1 (cites PRN-001).",
                "per §1 (cites PRN-001); never VIS-001…PRN-002.",
            )
        ],
        fragment="cross-prefix range",
    ),
    # --- check 4: TRACEABILITY exact coverage ---
    Scenario(
        "trace-missing",
        [("specs/TRACEABILITY.md", "PRN-001…002", "PRN-001")],
        fragment="defined IDs never listed: PRN-002",
    ),
    Scenario(
        "trace-phantom",
        [("specs/TRACEABILITY.md", "PRN-001…002", "PRN-001…003")],
        fragment="listed IDs not defined: PRN-003",
    ),
    # --- check 5: TRACEABILITY per-row partition ---
    Scenario(
        "partition-wrong-module",
        [
            (
                "specs/TRACEABILITY.md",
                "| §2 Principles | 02 | PRN-001…002 |",
                "| §2 Principles | 02 | PRN-001…002, VIS-001 |",
            )
        ],
        fragment="homed in module 01, which the row's module column does not name",
    ),
    Scenario(
        "partition-multi-row",
        [
            (
                "specs/TRACEABILITY.md",
                "| §2 Principles | 02 | PRN-001…002 |",
                "| §2 Principles | 01, 02 | PRN-001…002, VIS-001 |",
            )
        ],
        fragment="VIS-001 is cited by 2 coverage rows",
    ),
    Scenario(
        "crosslist-stale-entry",
        [
            (
                "specs/TRACEABILITY.md",
                "PRN-001…002 |\n",
                f"PRN-001…002 |\n{CROSSLIST_SECTION}| PRN-001 | fixture: stale on purpose |\n",
            )
        ],
        fragment="stale Intentional cross-listings entry",
    ),
    # --- check 6: version pins ---
    Scenario(
        "version-pin-disagree",
        [("specs/README.md", "(v1.0)", "(v1.1)")],
        fragment="version pins disagree",
    ),
    Scenario(
        "version-pin-missing",
        [
            (
                "specs/README.md",
                "Normative requirements corpus derived from\n`PLAN.md` (v1.0).",
                "Normative requirements corpus derived from the plan.",
            )
        ],
        fragment="version pin not found in specs/README.md",
    ),
    # --- check 7: PLAN section references ---
    Scenario(
        "section-dangling",
        [("specs/01-vision.md", "per §1", "per §1 and §2.9")],
        fragment="dangling PLAN section references: §2.9",
    ),
    Scenario(
        "section-range-dangling",
        [("specs/01-vision.md", "per §1", "per §1–3")],
        fragment="dangling PLAN section references: §3",
    ),
    Scenario(
        "section-range-malformed",
        [("specs/01-vision.md", "per §1", "per §2.1–1.9")],
        fragment="malformed section range",
    ),
    # --- check 7, PLAN.md's own body and list-item addresses ---
    Scenario(
        "plan-section-dangling",
        [("PLAN.md", "Refusals are loud.", "Refusals are loud. See §1.5.")],
        fragment="PLAN.md: dangling PLAN section references: §1.5",
    ),
    Scenario(
        "plan-item-ref-ok",
        [
            (
                "PLAN.md",
                "Refusals are loud.\n",
                "Refusals are loud.\n\n## 3. Decisions\n\n1. First.\n",
            ),
            ("specs/01-vision.md", "per §1", "per §1 and §3.1"),
        ],
        expect_ok=True,
    ),
    Scenario(
        "plan-item-ref-dangling",
        [
            (
                "PLAN.md",
                "Refusals are loud.\n",
                "Refusals are loud.\n\n## 3. Decisions\n\n1. First.\n",
            ),
            ("specs/01-vision.md", "per §1", "per §1 and §3.2"),
        ],
        fragment="dangling PLAN section references: §3.2",
    ),
]


def run_lint(root: Path) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [sys.executable, str(TOOLS / "lint_specs.py"), str(root)],
        capture_output=True,
        text=True,
    )


def apply(root: Path, sc: Scenario) -> None:
    for rel, old, new in sc.edits:
        path = root / rel
        if old == "":
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(new, encoding="utf-8")
            continue
        text = path.read_text(encoding="utf-8")
        assert old in text, f"{sc.name}: pattern {old!r} not found in {rel}"
        path.write_text(text.replace(old, new, 1), encoding="utf-8")


def main() -> int:
    failures: list[str] = []

    result = run_lint(CLEAN)
    if result.returncode != 0 or "lint: OK" not in result.stdout:
        failures.append(f"clean corpus: exit {result.returncode}: {result.stderr.strip()}")
    print(f"clean corpus: {'ok' if not failures else 'FAIL'}")

    for sc in SCENARIOS:
        with tempfile.TemporaryDirectory() as td:
            root = Path(td) / "corpus"
            shutil.copytree(CLEAN, root)
            apply(root, sc)
            result = run_lint(root)
            if sc.expect_ok:
                if result.returncode != 0 or "lint: OK" not in result.stdout:
                    failures.append(
                        f"{sc.name}: expected OK, got exit {result.returncode}: "
                        f"{result.stderr.strip()}"
                    )
            elif result.returncode == 0:
                failures.append(f"{sc.name}: expected failure, got exit 0")
            elif sc.fragment not in result.stderr:
                failures.append(
                    f"{sc.name}: expected {sc.fragment!r} in stderr, "
                    f"got: {result.stderr.strip()}"
                )
        print(f"{sc.name}: {'FAIL' if failures and failures[-1].startswith(sc.name) else 'ok'}")

    if failures:
        for f in failures:
            print(f"fail: {f}", file=sys.stderr)
        print(f"{len(failures)} failing scenario(s)", file=sys.stderr)
        return 1
    print(f"self-tests: OK ({len(SCENARIOS)} scenarios + clean corpus)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
