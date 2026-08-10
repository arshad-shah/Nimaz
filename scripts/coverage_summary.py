#!/usr/bin/env python3
"""Print a Markdown coverage summary from a JaCoCo XML report.

Deliberately non-failing. A missing or malformed report prints a note and exits 0,
because this runs on every pull request and must never be the reason one goes red:
the audit epic (#460) lands eleven PRs of new tests, and a coverage gate would have
blocked all of them until the last one merged.

Reported, not gated — the number is there so a reviewer can see it move.

Usage: coverage_summary.py <path-to-jacoco-xml>
"""

import sys
import xml.etree.ElementTree as ET  # trusted input: JaCoCo output from our own build
from pathlib import Path

# Ordered most-useful-first; a JaCoCo report may not carry all of them.
COUNTERS = [
    ("LINE", "Lines"),
    ("BRANCH", "Branches"),
    ("INSTRUCTION", "Instructions"),
]


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: coverage_summary.py <jacoco-xml>", file=sys.stderr)
        return 2

    path = Path(sys.argv[1])
    if not path.is_file():
        print("### Coverage\n\n_No coverage report was produced for this run._")
        return 0

    try:
        root = ET.parse(path).getroot()
    except ET.ParseError as exc:
        print(f"### Coverage\n\n_The coverage report could not be read: {exc}_")
        return 0

    totals = {
        counter.get("type"): (
            int(counter.get("missed", 0)),
            int(counter.get("covered", 0)),
        )
        for counter in root.findall("counter")
    }

    if not totals:
        # This is not a hypothetical. Before #464, all four jacoco tasks pointed at
        # `tmp/kotlin-classes/debug`, which AGP 9 does not write — so every report was
        # a 237-byte file with a sessioninfo and no classes. An empty table looks like
        # a formatting glitch; say plainly that nothing was measured.
        print(
            "### Coverage\n\n"
            "**The report contains no classes.** Coverage was not measured — this "
            "usually means the jacoco `classDirectories` no longer match where the "
            "build writes compiled classes. See `app/build.gradle.kts`."
        )
        return 0

    lines = ["### Coverage", "", "| Metric | Covered | Total | % |", "|---|---:|---:|---:|"]
    for key, label in COUNTERS:
        if key not in totals:
            continue
        missed, covered = totals[key]
        total = missed + covered
        pct = (covered / total * 100) if total else 0.0
        lines.append(f"| {label} | {covered} | {total} | {pct:.1f}% |")

    lines += ["", "_Reported, not gated — see #464._"]
    print("\n".join(lines))
    return 0


if __name__ == "__main__":
    sys.exit(main())
