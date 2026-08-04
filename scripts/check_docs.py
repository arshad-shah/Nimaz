#!/usr/bin/env python3
"""
Documentation drift checker.

The docs in `docs/` are the source of truth for how Nimaz is built — but a doc
is only worth reading if it is still true. This script reads the *code* and
asserts that the facts the docs claim about it are still the facts, so a route,
an announcement key, a Worker or a notification channel cannot be added without
the doc that owns it being updated in the same change.

Every check has a stable id (`NAV-01`, `SUB-04`, `DOC-02`, …) which is what the
docs and `docs/DOCUMENTATION.md` cite. Failures print the check id, what the
code says, what the doc says, and which file to edit.

Pure Python, no dependencies, no Android toolchain — it runs on any PR in a
couple of seconds. Same spirit as `scripts/check_tajweed_contrast.py`.

    python3 scripts/check_docs.py           # verify; exit 1 on any failure
    python3 scripts/check_docs.py --list    # list every check and what it guards
    python3 scripts/check_docs.py --only NAV   # run one family (NAV / SUB / DOC)
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "app/src/main/java/com/arshadshah/nimaz"
NAV_DIR = APP / "core/navigation"
DOCS = ROOT / "docs"

ARCHITECTURE = DOCS / "ARCHITECTURE.md"
NAVIGATION = DOCS / "NAVIGATION.md"
SUBSYSTEMS = DOCS / "SUBSYSTEMS.md"
DOCS_INDEX = DOCS / "README.md"

# Docs that live under docs/ but are deliberately exempt from the house style
# (DOC-01) and the index (DOC-02): historical planning artifacts and per-change
# design records, which are snapshots by definition and are never updated.
EXEMPT_DIRS = {"archive", "superpowers", "design", "quran"}

# The header block every current doc must carry. See docs/DOCUMENTATION.md §2.
REQUIRED_HEADER_LINES = ("> **Owns:**", "> **Update when:**", "> **Verified by:**")


# ──────────────────────────────────────────────────────────────────────────────
# Plumbing
# ──────────────────────────────────────────────────────────────────────────────

class Report:
    def __init__(self) -> None:
        self.failures: list[tuple[str, str]] = []
        self.passes: list[str] = []

    def ok(self, check: str, detail: str) -> None:
        self.passes.append(f"{check}  {detail}")

    def fail(self, check: str, detail: str) -> None:
        self.failures.append((check, detail))

    def expect_covered(
        self,
        check: str,
        *,
        expected: list[str],
        haystack: str,
        doc: Path,
        noun: str,
        fix: str,
    ) -> None:
        """Every item in `expected` must appear, backticked, in `haystack`."""
        missing = [item for item in expected if f"`{item}`" not in haystack]
        if missing:
            self.fail(
                check,
                f"{len(missing)} {noun} not documented in {doc.relative_to(ROOT)}: "
                + ", ".join(sorted(missing))
                + f"\n         → {fix}",
            )
        else:
            self.ok(check, f"{len(expected)} {noun} documented in {doc.relative_to(ROOT)}")


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def section(text: str, heading_contains: str, level: str = "## ") -> str:
    """Return the body of the first `## `-level section whose title contains the marker."""
    lines = text.splitlines()
    start = None
    for i, line in enumerate(lines):
        if line.startswith(level) and heading_contains.lower() in line.lower():
            start = i
            break
    if start is None:
        return ""
    for j in range(start + 1, len(lines)):
        if lines[j].startswith(level):
            return "\n".join(lines[start:j])
    return "\n".join(lines[start:])


def kotlin_block(text: str, signature: str) -> str:
    """Return the source of the function/`when` block starting at `signature`."""
    idx = text.find(signature)
    if idx < 0:
        return ""
    depth = 0
    started = False
    out = []
    for ch in text[idx:]:
        out.append(ch)
        if ch == "{":
            depth += 1
            started = True
        elif ch == "}":
            depth -= 1
            if started and depth == 0:
                break
    return "".join(out)


def slugify(heading: str) -> str:
    """GitHub's heading-anchor algorithm, close enough for link checking."""
    text = heading.strip().lstrip("#").strip().lower()
    text = text.replace("`", "")
    text = re.sub(r"[^\w\s-]", "", text, flags=re.UNICODE)
    return text.replace(" ", "-")


# ──────────────────────────────────────────────────────────────────────────────
# Code inventories — what the app actually contains
# ──────────────────────────────────────────────────────────────────────────────

def routes() -> list[str]:
    src = read(NAV_DIR / "Routes.kt")
    return sorted(set(re.findall(r"^\s+data (?:object|class) (\w+)", src, re.M)))


def nav_graph_source() -> str:
    return read(NAV_DIR / "NavGraph.kt")


def screen_tags() -> set[str]:
    return set(re.findall(r"const val (\w+)", read(NAV_DIR / "ScreenTags.kt")))


def announcement_static_keys() -> list[str]:
    body = kotlin_block(read(NAV_DIR / "AnnouncementRoutes.kt"), "private fun staticAnnouncementRoute")
    keys: list[str] = []
    for line in body.splitlines():
        if "->" not in line:
            continue
        keys += re.findall(r'"([^"]+)"', line.split("->")[0])
    return sorted(set(keys))


def announcement_routes_used() -> list[str]:
    src = read(NAV_DIR / "AnnouncementRoutes.kt")
    return sorted(set(re.findall(r"Route\.(\w+)", src)))


def help_deeplink_keys() -> list[str]:
    src = read(NAV_DIR / "HelpDeepLink.kt")
    return sorted(set(re.findall(r'"([^"]+)"\s*->', src)))


def database_version() -> int:
    src = read(APP / "data/local/database/NimazDatabase.kt")
    match = re.search(r"const val NIMAZ_DATABASE_VERSION = (\d+)", src)
    if not match:
        raise SystemExit("could not read NIMAZ_DATABASE_VERSION from NimazDatabase.kt")
    return int(match.group(1))


def worker_classes() -> list[str]:
    names: set[str] = set()
    for path in APP.rglob("*Worker.kt"):
        names |= set(re.findall(r"class (\w+Worker)", read(path)))
    return sorted(names)


def service_classes() -> list[str]:
    names: set[str] = set()
    for path in APP.rglob("*Service.kt"):
        names |= set(re.findall(r"^class (\w+Service)", read(path), re.M))
    return sorted(names)


def widget_packages() -> list[str]:
    return sorted(
        p.name for p in (APP / "widget").iterdir() if p.is_dir() and p.name != "core"
    )


def notification_channel_ids() -> list[str]:
    ids: set[str] = set()
    for path in APP.rglob("*.kt"):
        ids |= set(re.findall(r'const val CHANNEL_ID[A-Z_]* = "([^"]+)"', read(path)))
    return sorted(ids)


def datastore_names() -> list[str]:
    names: set[str] = set()
    for path in APP.rglob("*.kt"):
        names |= set(re.findall(r'preferencesDataStore\(\s*\n?\s*name = "([^"]+)"', read(path)))
    return sorted(names)


def announcement_payload_keys() -> list[str]:
    src = read(APP / "data/announcement/AnnouncementPayloadMapper.kt")
    return sorted(set(re.findall(r'const val KEY_\w+ = "([^"]+)"', src)))


def announcement_enum_keys(enum_name: str) -> list[str]:
    src = read(APP / "domain/model/Announcement.kt")
    body = kotlin_block(src, f"enum class {enum_name}")
    return sorted(set(re.findall(r'\w+\("([^"]+)"\)', body)))


def current_docs() -> list[Path]:
    return sorted(p for p in DOCS.glob("*.md") if p.name != "README.md")


# ──────────────────────────────────────────────────────────────────────────────
# NAV — docs/NAVIGATION.md owns the route graph
# ──────────────────────────────────────────────────────────────────────────────

CHECKS: dict[str, str] = {
    "NAV-01": "every Route in Routes.kt appears in the NAVIGATION.md route reference",
    "NAV-02": "every route named in NAVIGATION.md still exists in Routes.kt",
    "NAV-03": "the destination count claimed in NAVIGATION.md matches NavGraph.kt",
    "NAV-04": "every destination is wired with taggedComposable (never bare composable)",
    "NAV-05": "every Route has a matching ScreenTags entry",
    "NAV-06": "every static announcement route key is documented",
    "NAV-07": "every announcement key documented still exists in AnnouncementRoutes.kt",
    "NAV-08": "every Route reachable from an announcement key is in the grammar section",
    "NAV-09": "every help deep-link key is documented",
    "NAV-10": "every help deep-link key documented still exists in HelpDeepLink.kt",
    "SUB-01": "the schema version SUBSYSTEMS.md claims matches NIMAZ_DATABASE_VERSION",
    "SUB-02": "every Worker class is documented in SUBSYSTEMS.md",
    "SUB-03": "every Service class is documented in SUBSYSTEMS.md",
    "SUB-04": "every widget package is documented in the SUBSYSTEMS.md widget table",
    "SUB-05": "every notification channel id is documented in SUBSYSTEMS.md",
    "SUB-06": "every DataStore file name is documented in SUBSYSTEMS.md",
    "SUB-07": "every FCM announcement payload key is documented",
    "SUB-08": "every AnnouncementType key is documented",
    "SUB-09": "every CelebrationEvent key is documented",
    "DOC-01": "every current doc carries the standard header block",
    "DOC-02": "every current doc is listed in the docs/README.md index",
    "DOC-03": "every relative link between docs resolves (file and anchor)",
    "DOC-04": "every doc over 150 lines opens with a contents list",
}

# Below this, a reader can scroll. Above it, they need an index. See DOCUMENTATION.md §2.
CONTENTS_REQUIRED_LINES = 150


def check_nav(report: Report) -> None:
    nav_doc = read(NAVIGATION)
    reference = section(nav_doc, "Route reference")
    grammar = section(nav_doc, "Announcement route grammar")
    deeplinks = section(nav_doc, "Help deep-link grammar")
    all_routes = routes()

    # NAV-01 — no undocumented route.
    report.expect_covered(
        "NAV-01",
        expected=all_routes,
        haystack=reference,
        doc=NAVIGATION,
        noun="routes",
        fix="add a row to the matching table under '## Route reference'",
    )

    # NAV-02 — no route documented that no longer exists.
    documented = set()
    for line in reference.splitlines():
        if not line.startswith("| `"):
            continue
        cell = line.split("|")[1]
        documented |= set(re.findall(r"`(\w+)`", cell))
    phantom = sorted(documented - set(all_routes))
    if phantom:
        report.fail(
            "NAV-02",
            "documented but absent from Routes.kt: " + ", ".join(phantom)
            + "\n         → delete the row, or restore the route",
        )
    else:
        report.ok("NAV-02", f"{len(documented)} documented routes all exist")

    # NAV-03 — the destination count in prose.
    graph = nav_graph_source()
    wired = len(re.findall(r"\b(?:tagged)?[Cc]omposable<Route\.", graph))
    claimed = re.search(r"\((\d+) `composable<Route\.X>` destinations\)", nav_doc)
    if not claimed:
        report.fail("NAV-03", "NAVIGATION.md no longer states the destination count "
                              "(expected \"(N `composable<Route.X>` destinations)\")")
    elif int(claimed.group(1)) != wired:
        report.fail(
            "NAV-03",
            f"NAVIGATION.md claims {claimed.group(1)} destinations, NavGraph.kt wires {wired}"
            f"\n         → update the count in docs/NAVIGATION.md",
        )
    else:
        report.ok("NAV-03", f"{wired} destinations, count matches")

    # NAV-04 — every destination carries a test tag.
    untagged = re.findall(r"^\s*composable<Route\.(\w+)>", graph, re.M)
    if untagged:
        report.fail(
            "NAV-04",
            "wired with bare composable<> (no ScreenTag): " + ", ".join(sorted(set(untagged)))
            + "\n         → use taggedComposable<Route.X>(ScreenTags.X) in NavGraph.kt",
        )
    else:
        report.ok("NAV-04", "every destination uses taggedComposable")

    # NAV-05 — a tag exists for every route.
    tags = screen_tags()
    tagless = [r for r in all_routes if r not in tags]
    if tagless:
        report.fail(
            "NAV-05",
            "no ScreenTags entry: " + ", ".join(tagless)
            + "\n         → add `const val X = \"screen_x\"` to ScreenTags.kt",
        )
    else:
        report.ok("NAV-05", f"{len(all_routes)} routes all have a ScreenTags entry")

    # NAV-06 / NAV-07 — the announcement static allowlist, both directions.
    code_keys = announcement_static_keys()
    report.expect_covered(
        "NAV-06",
        expected=code_keys,
        haystack=grammar,
        doc=NAVIGATION,
        noun="announcement keys",
        fix="add the key to the static allowlist table under "
            "'## Announcement route grammar'",
    )
    # Only the first column of the *static allowlist* table counts as a claim that a
    # literal key exists. The parameterised table lists patterns, not keys, and prose
    # in the same section legitimately mentions other identifiers.
    allowlist = section(grammar, "Static allowlist", level="### ")
    doc_keys = {
        key
        for row in re.findall(r"^\| ((?:`[a-z0-9/\-]+`(?: / )?)+) \|", allowlist, re.M)
        for key in re.findall(r"`([a-z0-9/\-]+)`", row)
    }
    phantom_keys = sorted(doc_keys - set(code_keys))
    if phantom_keys:
        report.fail(
            "NAV-07",
            "documented announcement keys absent from AnnouncementRoutes.kt: "
            + ", ".join(phantom_keys)
            + "\n         → delete the row, or add the key to staticAnnouncementRoute",
        )
    else:
        report.ok("NAV-07", f"{len(doc_keys)} documented announcement keys all exist")

    # NAV-08 — every route an announcement can reach is named in the grammar section.
    report.expect_covered(
        "NAV-08",
        expected=announcement_routes_used(),
        haystack=grammar,
        doc=NAVIGATION,
        noun="announcement-reachable routes",
        fix="name the route in the static allowlist or the parameterised grammar table",
    )

    # NAV-09 / NAV-10 — help deep links, both directions.
    code_help = help_deeplink_keys()
    report.expect_covered(
        "NAV-09",
        expected=code_help,
        haystack=deeplinks,
        doc=NAVIGATION,
        noun="help deep-link keys",
        fix="add a row to the table under '## Help deep-link grammar'",
    )
    doc_help = set(re.findall(r"^\| `([a-z_]+)`", deeplinks, re.M))
    phantom_help = sorted(doc_help - set(code_help))
    if phantom_help:
        report.fail(
            "NAV-10",
            "documented help deep links absent from HelpDeepLink.kt: "
            + ", ".join(phantom_help),
        )
    else:
        report.ok("NAV-10", f"{len(doc_help)} documented help deep links all exist")


# ──────────────────────────────────────────────────────────────────────────────
# SUB — docs/SUBSYSTEMS.md owns the moving parts
# ──────────────────────────────────────────────────────────────────────────────

def check_sub(report: Report) -> None:
    sub_doc = read(SUBSYSTEMS)
    version = database_version()

    # SUB-01 — the one number that gates every migration.
    marker = re.search(r"\*\*Current schema version:\*\* `(\d+)`", sub_doc)
    if not marker:
        report.fail(
            "SUB-01",
            "SUBSYSTEMS.md no longer states the schema version "
            "(expected \"**Current schema version:** `N`\")",
        )
    elif int(marker.group(1)) != version:
        report.fail(
            "SUB-01",
            f"SUBSYSTEMS.md says schema version {marker.group(1)}, "
            f"NimazDatabase.kt says {version}"
            "\n         → update §5 and add the migration's row to the migration table",
        )
    else:
        report.ok("SUB-01", f"schema version {version} matches")

    report.expect_covered(
        "SUB-02", expected=worker_classes(), haystack=sub_doc, doc=SUBSYSTEMS,
        noun="Workers", fix="add it to the worker list in §3 (Background work)",
    )
    report.expect_covered(
        "SUB-03", expected=service_classes(), haystack=sub_doc, doc=SUBSYSTEMS,
        noun="Services", fix="document it in the owning section (§1 audio, §12 announcements, …)",
    )
    report.expect_covered(
        "SUB-04", expected=[f"widget/{p}/" for p in widget_packages()], haystack=sub_doc,
        doc=SUBSYSTEMS, noun="widget packages", fix="add a row to the widget table in §2",
    )
    report.expect_covered(
        "SUB-05", expected=notification_channel_ids(), haystack=sub_doc, doc=SUBSYSTEMS,
        noun="notification channels", fix="add a row to the channel table in §4",
    )
    report.expect_covered(
        "SUB-06", expected=datastore_names(), haystack=sub_doc, doc=SUBSYSTEMS,
        noun="DataStore files", fix="add a row to the DataStore table in §6",
    )
    announcements = section(sub_doc, "Engagement announcements")
    report.expect_covered(
        "SUB-07", expected=announcement_payload_keys(), haystack=announcements,
        doc=SUBSYSTEMS, noun="FCM payload keys", fix="add a row to the payload contract table in §12",
    )
    report.expect_covered(
        "SUB-08", expected=announcement_enum_keys("AnnouncementType"), haystack=announcements,
        doc=SUBSYSTEMS, noun="announcement types", fix="document the type in §12",
    )
    report.expect_covered(
        "SUB-09", expected=announcement_enum_keys("CelebrationEvent"), haystack=announcements,
        doc=SUBSYSTEMS, noun="celebration events", fix="document the event key in §12",
    )


# ──────────────────────────────────────────────────────────────────────────────
# DOC — the docs' own house style
# ──────────────────────────────────────────────────────────────────────────────

def check_doc(report: Report) -> None:
    docs = current_docs()

    # DOC-01 — the header block that says who owns what and when to update it.
    missing_header = []
    for path in docs:
        head = "\n".join(read(path).splitlines()[:15])
        absent = [line for line in REQUIRED_HEADER_LINES if line not in head]
        if absent:
            missing_header.append(f"{path.name} (missing {', '.join(absent)})")
    if missing_header:
        report.fail(
            "DOC-01",
            "docs without the standard header block:\n           "
            + "\n           ".join(missing_header)
            + "\n         → see docs/DOCUMENTATION.md §2 for the block to copy",
        )
    else:
        report.ok("DOC-01", f"{len(docs)} docs carry the standard header")

    # DOC-02 — the index is complete.
    index = read(DOCS_INDEX) if DOCS_INDEX.exists() else ""
    unindexed = [p.name for p in docs if f"]({p.name})" not in index]
    if unindexed:
        report.fail(
            "DOC-02",
            "not listed in docs/README.md: " + ", ".join(unindexed)
            + "\n         → add a row to the index table",
        )
    else:
        report.ok("DOC-02", f"{len(docs)} docs all listed in the index")

    # DOC-03 — no broken cross-links.
    broken: list[str] = []
    md_files = [p for p in DOCS.rglob("*.md") if not any(d in p.parts for d in EXEMPT_DIRS)]
    md_files.append(ROOT / "CLAUDE.md")
    md_files.append(ROOT / "README.md")
    for path in md_files:
        text = read(path)
        own_headings = {slugify(line) for line in text.splitlines() if line.startswith("#")}
        # Same-file anchors: contents lists and cross-section references.
        for anchor in re.findall(r"\]\(#([^)\s]+)\)", text):
            if anchor not in own_headings:
                broken.append(f"{path.relative_to(ROOT)} → #{anchor} (no such heading)")
        for target in re.findall(r"\]\(([^)\s]+\.md(?:#[^)\s]+)?)\)", text):
            file_part, _, anchor = target.partition("#")
            resolved = (path.parent / file_part).resolve()
            if not resolved.exists():
                broken.append(f"{path.relative_to(ROOT)} → {target} (no such file)")
                continue
            if anchor:
                headings = {
                    slugify(line)
                    for line in read(resolved).splitlines()
                    if line.startswith("#")
                }
                if anchor not in headings:
                    broken.append(f"{path.relative_to(ROOT)} → {target} (no such heading)")
    if broken:
        report.fail("DOC-03", "broken links:\n           " + "\n           ".join(broken))
    else:
        report.ok("DOC-03", f"all cross-links in {len(md_files)} files resolve")

    # DOC-04 — long docs are navigable.
    long_docs = [(p, len(read(p).splitlines())) for p in docs]
    no_contents = [
        f"{p.name} ({n} lines)"
        for p, n in long_docs
        if n > CONTENTS_REQUIRED_LINES
        and not re.search(r"^## (Contents|Table of contents)", read(p), re.M)
    ]
    if no_contents:
        report.fail(
            "DOC-04",
            "over " + str(CONTENTS_REQUIRED_LINES) + " lines with no contents list: "
            + ", ".join(no_contents)
            + "\n         → add a '## Contents' list with working anchors",
        )
    else:
        report.ok(
            "DOC-04",
            f"{sum(1 for _, n in long_docs if n > CONTENTS_REQUIRED_LINES)} long docs "
            "have a contents list",
        )


# ──────────────────────────────────────────────────────────────────────────────

FAMILIES = {"NAV": check_nav, "SUB": check_sub, "DOC": check_doc}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--list", action="store_true", help="list every check and exit")
    parser.add_argument("--only", choices=sorted(FAMILIES), help="run one family of checks")
    parser.add_argument("-q", "--quiet", action="store_true", help="print failures only")
    args = parser.parse_args()

    if args.list:
        for check, description in CHECKS.items():
            print(f"{check}  {description}")
        return 0

    report = Report()
    for family, fn in FAMILIES.items():
        if args.only and args.only != family:
            continue
        fn(report)

    if not args.quiet:
        for line in report.passes:
            print(f"  ok   {line}")
    for check, detail in report.failures:
        print(f"  FAIL {check}  {detail}")

    print()
    if report.failures:
        print(f"{len(report.failures)} documentation check(s) failed, "
              f"{len(report.passes)} passed.")
        print("The docs have drifted from the code. Update the doc named in each failure —")
        print("see docs/DOCUMENTATION.md for which doc owns what.")
        return 1
    print(f"All {len(report.passes)} documentation checks passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
