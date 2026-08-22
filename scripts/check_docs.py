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
import os
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DOCS = ROOT / "docs"

# Directories that are never source: build output, VCS, tooling caches. Pruned
# rather than filtered, so the walk below stays fast on a repo with 20 modules
# each carrying a build/ tree.
PRUNED_DIRS = {"build", ".git", ".gradle", ".idea", "node_modules", ".kotlin", "generated"}

# Where Kotlin/Java source lives inside a Gradle module.
SOURCE_SET_SUFFIXES = (
    Path("src/main/java/com/arshadshah/nimaz"),
    Path("src/main/kotlin/com/arshadshah/nimaz"),
)

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
        # Floors are not checks in their own right — they are a precondition of the
        # check they belong to — so they are counted separately and never inflate
        # the "All N documentation checks passed" total.
        self.floors_met: list[str] = []

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
        # An empty expected-set is never a pass. "ok 0 Workers documented" is the
        # exact shape of the silent failure this script exists to prevent: the scan
        # found nothing, so every item it found was trivially documented.
        if not expected:
            self.fail(
                check,
                f"the {noun} scan matched nothing — 0 found across "
                f"{len(source_roots())} module source root(s)."
                "\n         → this is a broken scan, not a clean result: the code it looks "
                "for moved, was renamed, or lives outside every source root",
            )
            return
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

    def expect_floor(self, check: str, actual: int, floor: int, *, noun: str) -> None:
        """`actual` must be at least `floor`, or the scan has shrunk and lies.

        See MINIMUMS for why these numbers exist and what it takes to lower one.
        """
        if actual < floor:
            self.fail(
                check,
                f"only {actual} {noun} found, floor is {floor} — the scan shrank."
                "\n         → code moved out of every source root, or a pattern stopped "
                "matching. Fix the scan. Lowering the floor in MINIMUMS is only correct "
                "when the thing was genuinely deleted, and needs saying so in the commit "
                "message (see docs/DOCUMENTATION.md §4).",
            )
        else:
            self.floors_met.append(f"{check} {actual} >= {floor} {noun}")


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def source_roots() -> list[Path]:
    """Every `…/src/main/{java,kotlin}/com/arshadshah/nimaz` directory in the repo.

    Nimaz is being split into Gradle modules (#551). Before that split every scan
    in this file was rooted at the single `app/…` path, so the moment a Worker or
    a widget moved into `feature/…` the scan simply found fewer of them and the
    "everything is documented" checks passed *because there was less to find*.
    Rooting at every module makes a move invisible to the checks, which is the
    point: the inventory is a property of the app, not of one module.

    Walks the tree once with build/VCS directories pruned. Raises if the result is
    empty — a scan with no roots is a broken checkout, never a clean bill of health.
    """
    roots: list[Path] = []
    for dirpath, dirnames, _ in os.walk(ROOT):
        dirnames[:] = [d for d in dirnames if d not in PRUNED_DIRS]
        here = Path(dirpath)
        for suffix in SOURCE_SET_SUFFIXES:
            candidate = here / suffix
            if candidate.is_dir():
                roots.append(candidate)
        # Do not descend into a source set we just matched: the package tree below
        # it cannot contain another module.
        if any((here / suffix).is_dir() for suffix in SOURCE_SET_SUFFIXES):
            dirnames[:] = [d for d in dirnames if d != "src"]
    if not roots:
        raise SystemExit(
            "no Kotlin source roots found under "
            f"{ROOT} — expected at least one */src/main/{{java,kotlin}}/com/arshadshah/nimaz"
        )
    return sorted(set(roots))


def source_files(pattern: str = "*.kt") -> list[Path]:
    """Every source file matching `pattern` across every module."""
    return sorted({path for root in source_roots() for path in root.rglob(pattern)})


def find_one(relative: str) -> Path:
    """The single file at `relative` (a package path or glob) across all modules.

    Fails loudly on 0 matches (the file moved out of the package tree, or was
    renamed) and on more than 1 (two modules both declare it — an ambiguity the
    caller must resolve, not something to silently pick a winner for).
    """
    matches = sorted({p for root in source_roots() for p in root.glob(relative) if p.is_file()})
    if not matches:
        raise SystemExit(
            f"could not find {relative} in any module source root "
            f"({', '.join(str(r.relative_to(ROOT)) for r in source_roots())})"
        )
    if len(matches) > 1:
        raise SystemExit(
            f"{relative} matches {len(matches)} files, expected exactly one: "
            + ", ".join(str(m.relative_to(ROOT)) for m in matches)
        )
    return matches[0]


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
    src = read(find_one("core/navigation/Routes.kt"))
    return sorted(set(re.findall(r"^\s+data (?:object|class) (\w+)", src, re.M)))


def strip_kotlin_comments(src: str) -> str:
    """Kotlin source with `//` and `/* */` comments blanked out.

    NAV-03 and NAV-04 count occurrences of `taggedComposable<Route.X>` in source. Without this,
    a file that merely *documents* the pattern is counted as wiring a destination — which is
    exactly what happened when `taggedComposable` was extracted into its own file in PR 11 of
    #551 and its KDoc quoted the very call shape the checks look for. NAV-03 went red claiming
    95 destinations against 94 documented, and the tempting "fix" is to edit the documented
    number, which would then be wrong.

    The same blanking helps NAV-04 in the other direction, though less dramatically: a
    *block*-commented `composable<Route.X>` sitting at the start of a line used to be reported as
    an untagged destination and no longer is. A `//`-commented one never was, because NAV-04's
    regex anchors on `composable` following only whitespace.

    Line structure is preserved (comments become blank, newlines survive) because NAV-04 anchors
    its regex on the start of a line.
    """
    out = []
    i, n = 0, len(src)
    in_line, in_block, in_string = False, False, False
    while i < n:
        ch = src[i]
        nxt = src[i + 1] if i + 1 < n else ""
        if in_line:
            if ch == "\n":
                in_line = False
                out.append(ch)
            else:
                out.append(" ")
        elif in_block:
            if ch == "*" and nxt == "/":
                in_block = False
                out.append("  ")
                i += 2
                continue
            out.append("\n" if ch == "\n" else " ")
        elif in_string:
            out.append(ch)
            if ch == "\\":
                if i + 1 < n:
                    out.append(nxt)
                    i += 2
                    continue
            elif ch == '"':
                in_string = False
        elif ch == "/" and nxt == "/":
            in_line = True
            out.append("  ")
            i += 2
            continue
        elif ch == "/" and nxt == "*":
            in_block = True
            out.append("  ")
            i += 2
            continue
        else:
            if ch == '"':
                in_string = True
            out.append(ch)
        i += 1
    return "".join(out)


def nav_graph_source() -> str:
    """Every file that wires a destination, concatenated.

    This used to read the single file `core/navigation/NavGraph.kt`. The migration
    dissolves that file into per-feature `NavGraphBuilder` extensions living beside
    their screens, at which point NAV-03 (the destination count) would go red — loud,
    and tempting to "fix" by editing the documented number — while NAV-04, the
    bare-`composable` detector, would go **silently vacuous**: nothing to scan means
    nothing untagged to find. Scanning every file that mentions a destination keeps
    both checks meaningful wherever the wiring lives.

    Joined with newlines because NAV-04 anchors on the start of a line.
    """
    sources = [
        text
        for text in (strip_kotlin_comments(read(path)) for path in source_files("*.kt"))
        if "taggedComposable<Route." in text or "composable<Route." in text
    ]
    return "\n".join(sources)


def wired_destination_count(graph: str) -> int:
    """How many destinations the nav graph wires, tagged or not."""
    return len(re.findall(r"\b(?:tagged)?[Cc]omposable<Route\.", graph))


def bare_composable_destinations(graph: str) -> list[str]:
    """Destinations wired with a bare `composable<Route.X>` — no ScreenTag, untestable."""
    return sorted(set(re.findall(r"^\s*composable<Route\.(\w+)>", graph, re.M)))


def screen_tags() -> set[str]:
    return set(re.findall(r"const val (\w+)", read(find_one("core/navigation/ScreenTags.kt"))))


def announcement_static_keys() -> list[str]:
    body = kotlin_block(
        read(find_one("core/navigation/AnnouncementRoutes.kt")),
        "private fun staticAnnouncementRoute",
    )
    keys: list[str] = []
    for line in body.splitlines():
        if "->" not in line:
            continue
        keys += re.findall(r'"([^"]+)"', line.split("->")[0])
    return sorted(set(keys))


def announcement_routes_used() -> list[str]:
    src = read(find_one("core/navigation/AnnouncementRoutes.kt"))
    return sorted(set(re.findall(r"Route\.(\w+)", src)))


def help_deeplink_keys() -> list[str]:
    src = read(find_one("core/navigation/HelpDeepLink.kt"))
    return sorted(set(re.findall(r'"([^"]+)"\s*->', src)))


def database_version() -> int:
    src = read(find_one("data/local/database/NimazDatabase.kt"))
    match = re.search(r"const val NIMAZ_DATABASE_VERSION = (\d+)", src)
    if not match:
        raise SystemExit("could not read NIMAZ_DATABASE_VERSION from NimazDatabase.kt")
    return int(match.group(1))


def worker_classes() -> list[str]:
    names: set[str] = set()
    for path in source_files("*Worker.kt"):
        names |= set(re.findall(r"class (\w+Worker)", read(path)))
    return sorted(names)


def service_classes() -> list[str]:
    names: set[str] = set()
    for path in source_files("*Service.kt"):
        names |= set(re.findall(r"^class (\w+Service)", read(path), re.M))
    return sorted(names)


def widget_packages() -> list[str]:
    return sorted(
        {
            child.name
            for root in source_roots()
            if (root / "widget").is_dir()
            for child in (root / "widget").iterdir()
            if child.is_dir() and child.name != "core"
        }
    )


def notification_channel_ids() -> list[str]:
    ids: set[str] = set()
    for path in source_files("*.kt"):
        ids |= set(re.findall(r'const val CHANNEL_ID[A-Z_]* = "([^"]+)"', read(path)))
    return sorted(ids)


def preferences_datastore_names() -> list[str]:
    """The `preferencesDataStore(name = "…")` files — one named file each."""
    names: set[str] = set()
    for path in source_files("*.kt"):
        names |= set(re.findall(r'preferencesDataStore\(\s*\n?\s*name = "([^"]+)"', read(path)))
    return sorted(names)


def typed_datastore_owners() -> list[str]:
    """Files that build a DataStore by hand with `DataStoreFactory.create`.

    These have no literal file name to capture — `JsonGlanceStateDefinition` takes
    its file name as a constructor parameter and creates one store per widget — so
    the *owner* is what gets documented. Without this the SUB-06 claim that "every
    DataStore file is documented" was checking three files out of four.
    """
    return sorted(
        {path.stem for path in source_files("*.kt") if "DataStoreFactory.create" in read(path)}
    )


def glance_state_file_names() -> list[str]:
    """The DataStore file name each widget's Glance state definition writes to.

    `JsonGlanceStateDefinition` takes its file name as a constructor argument, so
    naming the owner alone leaves the six files on disk as prose no check reads.
    Scoped to files that subclass it: `fileName = "…"` on its own also matches the
    bundled adhan audio, which is not a DataStore.
    """
    names: set[str] = set()
    for path in source_files("*.kt"):
        src = read(path)
        if "JsonGlanceStateDefinition<" not in src:
            continue
        names |= set(re.findall(r'fileName = "([^"]+)"', src))
    return sorted(names)


def datastore_names() -> list[str]:
    return sorted(
        set(preferences_datastore_names())
        | set(typed_datastore_owners())
        | set(glance_state_file_names())
    )


def announcement_payload_keys() -> list[str]:
    src = read(find_one("data/announcement/AnnouncementPayloadMapper.kt"))
    return sorted(set(re.findall(r'const val KEY_\w+ = "([^"]+)"', src)))


def announcement_enum_keys(enum_name: str) -> list[str]:
    src = read(find_one("domain/model/Announcement.kt"))
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

# ── Scan floors ───────────────────────────────────────────────────────────────
# A "documented everything I found" check is only as good as what it found. If a
# scan silently returns fewer items — because the code moved into a module the
# scan does not reach, or a pattern stopped matching — the check passes because
# there is less to find. Each number below is the count measured in the tree at
# the time it was added; the scan may only ever grow past it.
#
# Lowering one is a claim that the thing was genuinely deleted. Say so in the
# commit message. See docs/DOCUMENTATION.md §4.
MINIMUMS: dict[str, int] = {
    "NAV-01": 94,   # Routes in Routes.kt
    "NAV-03": 94,   # destinations wired across every nav-graph file
    "NAV-05": 104,  # ScreenTags entries — more than the 94 Routes on purpose: some
                    #                       screens are tabs inside a parent, so they
                    #                       carry a tag without owning a Route
    "NAV-06": 57,   # static announcement route keys
    "NAV-08": 67,   # Routes reachable from an announcement key
    "NAV-09": 22,   # help deep-link keys
    "SUB-02": 7,    # Worker classes
    "SUB-03": 4,    # Service classes
    "SUB-04": 6,    # widget packages
    "SUB-05": 12,   # notification channel ids
    "SUB-06": 10,   # DataStore files: 3 preferencesDataStore + the Glance state
                    #                  definition and the 6 widget files it writes
    "SUB-06-PREFS": 3,   # of which, `preferencesDataStore(name = …)` files
    "SUB-06-GLANCE": 6,  # of which, per-widget Glance state files
}


def check_nav(report: Report) -> None:
    nav_doc = read(NAVIGATION)
    reference = section(nav_doc, "Route reference")
    grammar = section(nav_doc, "Announcement route grammar")
    deeplinks = section(nav_doc, "Help deep-link grammar")
    all_routes = routes()

    report.expect_floor("NAV-01", len(all_routes), MINIMUMS["NAV-01"], noun="routes")

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
    wired = wired_destination_count(graph)
    report.expect_floor("NAV-03", wired, MINIMUMS["NAV-03"], noun="wired destinations")
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
    untagged = bare_composable_destinations(graph)
    if untagged:
        report.fail(
            "NAV-04",
            "wired with bare composable<> (no ScreenTag): " + ", ".join(untagged)
            + "\n         → use taggedComposable<Route.X>(ScreenTags.X) where it is wired",
        )
    else:
        report.ok("NAV-04", "every destination uses taggedComposable")

    # NAV-05 — a tag exists for every route.
    tags = screen_tags()
    report.expect_floor("NAV-05", len(tags), MINIMUMS["NAV-05"], noun="ScreenTags entries")
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
    report.expect_floor("NAV-06", len(code_keys), MINIMUMS["NAV-06"], noun="announcement keys")
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
    reachable = announcement_routes_used()
    report.expect_floor(
        "NAV-08", len(reachable), MINIMUMS["NAV-08"], noun="announcement-reachable routes"
    )
    report.expect_covered(
        "NAV-08",
        expected=reachable,
        haystack=grammar,
        doc=NAVIGATION,
        noun="announcement-reachable routes",
        fix="name the route in the static allowlist or the parameterised grammar table",
    )

    # NAV-09 / NAV-10 — help deep links, both directions.
    code_help = help_deeplink_keys()
    report.expect_floor("NAV-09", len(code_help), MINIMUMS["NAV-09"], noun="help deep-link keys")
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

    workers = worker_classes()
    report.expect_floor("SUB-02", len(workers), MINIMUMS["SUB-02"], noun="Workers")
    report.expect_covered(
        "SUB-02", expected=workers, haystack=sub_doc, doc=SUBSYSTEMS,
        noun="Workers", fix="add it to the worker list in §3 (Background work)",
    )
    services = service_classes()
    report.expect_floor("SUB-03", len(services), MINIMUMS["SUB-03"], noun="Services")
    report.expect_covered(
        "SUB-03", expected=services, haystack=sub_doc, doc=SUBSYSTEMS,
        noun="Services", fix="document it in the owning section (§1 audio, §12 announcements, …)",
    )
    widgets = widget_packages()
    report.expect_floor("SUB-04", len(widgets), MINIMUMS["SUB-04"], noun="widget packages")
    report.expect_covered(
        "SUB-04", expected=[f"widget/{p}/" for p in widgets], haystack=sub_doc,
        doc=SUBSYSTEMS, noun="widget packages", fix="add a row to the widget table in §2",
    )
    channels = notification_channel_ids()
    report.expect_floor("SUB-05", len(channels), MINIMUMS["SUB-05"], noun="notification channels")
    report.expect_covered(
        "SUB-05", expected=channels, haystack=sub_doc, doc=SUBSYSTEMS,
        noun="notification channels", fix="add a row to the channel table in §4",
    )
    datastores = datastore_names()
    report.expect_floor("SUB-06", len(datastores), MINIMUMS["SUB-06"], noun="DataStore files")
    report.expect_floor(
        "SUB-06-PREFS", len(preferences_datastore_names()), MINIMUMS["SUB-06-PREFS"],
        noun="preferencesDataStore files",
    )
    report.expect_floor(
        "SUB-06-GLANCE", len(glance_state_file_names()), MINIMUMS["SUB-06-GLANCE"],
        noun="Glance widget state files",
    )
    report.expect_covered(
        "SUB-06", expected=datastores, haystack=sub_doc, doc=SUBSYSTEMS,
        noun="DataStore files", fix="add a row to the DataStore table in §0.5",
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
    print(
        f"All {len(report.passes)} documentation checks passed"
        f" ({len(report.floors_met)} scan floors met)."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
