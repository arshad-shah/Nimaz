"""Tests for scripts/check_docs.py — the doc-drift checker's own safety net.

The checker asserts that the docs still describe the code. Its own failure mode is
silence: a scan that finds nothing reports that everything it found is documented.
The migration to Gradle modules (#551) makes that failure reachable — code moves out
of `app/` and the scans, rooted there, come back smaller. These tests pin the two
properties that stop it: the scans span **every** module, and each one has a floor
below which it fails instead of passing.

Run: python3 -m pytest scripts/test_check_docs.py -v
"""

import importlib.util
import subprocess
import sys
import textwrap
from pathlib import Path

import pytest

SCRIPT = Path(__file__).resolve().parent / "check_docs.py"


def _load():
    """Import check_docs.py fresh, so one test's monkeypatching cannot leak."""
    spec = importlib.util.spec_from_file_location("check_docs_under_test", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


@pytest.fixture
def check_docs():
    return _load()


def _module_root(tmp_path: Path, gradle_module: str, lang: str = "java") -> Path:
    """Create `<gradle_module>/src/main/<lang>/com/arshadshah/nimaz` and return it."""
    root = tmp_path / gradle_module / "src/main" / lang / "com/arshadshah/nimaz"
    root.mkdir(parents=True, exist_ok=True)
    return root


def _write(root: Path, relative: str, text: str) -> Path:
    path = root / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(textwrap.dedent(text), encoding="utf-8")
    return path


def _rebase(module, tmp_path: Path) -> None:
    """Point the checker at a fixture tree instead of the real repo."""
    module.ROOT = tmp_path
    module.DOCS = tmp_path / "docs"
    module.DOCS.mkdir(parents=True, exist_ok=True)
    module.ARCHITECTURE = module.DOCS / "ARCHITECTURE.md"
    module.NAVIGATION = module.DOCS / "NAVIGATION.md"
    module.SUBSYSTEMS = module.DOCS / "SUBSYSTEMS.md"
    module.DOCS_INDEX = module.DOCS / "README.md"


# ──────────────────────────────────────────────────────────────────────────────
# source_roots — the scans must span every module
# ──────────────────────────────────────────────────────────────────────────────

def test_source_roots_finds_every_module(check_docs, tmp_path: Path):
    _rebase(check_docs, tmp_path)
    _module_root(tmp_path, "app")
    _module_root(tmp_path, "feature/widget")
    _module_root(tmp_path, "core/domain", lang="kotlin")

    roots = check_docs.source_roots()

    assert [str(r.relative_to(tmp_path)) for r in roots] == [
        "app/src/main/java/com/arshadshah/nimaz",
        "core/domain/src/main/kotlin/com/arshadshah/nimaz",
        "feature/widget/src/main/java/com/arshadshah/nimaz",
    ]


def test_source_roots_ignores_build_output(check_docs, tmp_path: Path):
    """A generated copy under build/ would double every count."""
    _rebase(check_docs, tmp_path)
    _module_root(tmp_path, "app")
    _module_root(tmp_path, "app/build/generated/source")

    roots = check_docs.source_roots()

    assert len(roots) == 1
    assert "build" not in roots[0].parts


def test_source_roots_refuses_to_return_an_empty_list(check_docs, tmp_path: Path):
    """No roots is a broken checkout, never a clean bill of health."""
    _rebase(check_docs, tmp_path)

    with pytest.raises(SystemExit, match="no Kotlin source roots"):
        check_docs.source_roots()


def test_workers_are_found_across_two_modules(check_docs, tmp_path: Path):
    """The exact shape of the migration: half the Workers move to :feature:widget."""
    _rebase(check_docs, tmp_path)
    app = _module_root(tmp_path, "app")
    widget = _module_root(tmp_path, "feature/widget")
    _write(app, "data/audio/AdhanDownloadWorker.kt", "class AdhanDownloadWorker\n")
    _write(widget, "widget/khatam/KhatamWidgetWorker.kt", "class KhatamWidgetWorker\n")

    assert check_docs.worker_classes() == ["AdhanDownloadWorker", "KhatamWidgetWorker"]


# ──────────────────────────────────────────────────────────────────────────────
# find_one — the single-file readers
# ──────────────────────────────────────────────────────────────────────────────

def test_find_one_returns_the_single_match(check_docs, tmp_path: Path):
    _rebase(check_docs, tmp_path)
    _module_root(tmp_path, "app")
    nav = _module_root(tmp_path, "core/navigation")
    expected = _write(nav, "core/navigation/Routes.kt", "object Routes\n")

    assert check_docs.find_one("core/navigation/Routes.kt") == expected


def test_find_one_fails_loudly_when_the_file_moved_away(check_docs, tmp_path: Path):
    _rebase(check_docs, tmp_path)
    _module_root(tmp_path, "app")

    with pytest.raises(SystemExit, match="could not find core/navigation/Routes.kt"):
        check_docs.find_one("core/navigation/Routes.kt")


def test_find_one_fails_loudly_when_two_modules_declare_it(check_docs, tmp_path: Path):
    """Reading whichever came first alphabetically would be a coin toss."""
    _rebase(check_docs, tmp_path)
    for module in ("app", "core/navigation"):
        _write(_module_root(tmp_path, module), "core/navigation/Routes.kt", "object Routes\n")

    with pytest.raises(SystemExit, match="matches 2 files"):
        check_docs.find_one("core/navigation/Routes.kt")


# ──────────────────────────────────────────────────────────────────────────────
# NAV-03 / NAV-04 — the nav graph after it is split into per-feature files
# ──────────────────────────────────────────────────────────────────────────────

def test_a_nav_graph_split_across_files_still_counts_every_destination(
    check_docs, tmp_path: Path
):
    _rebase(check_docs, tmp_path)
    app = _module_root(tmp_path, "app")
    feature = _module_root(tmp_path, "feature/prayer")
    _write(app, "core/navigation/NavGraph.kt", """\
        fun NavGraphBuilder.appGraph() {
            taggedComposable<Route.Home>(ScreenTags.Home) { }
        }
    """)
    _write(feature, "presentation/prayer/PrayerGraph.kt", """\
        fun NavGraphBuilder.prayerGraph() {
            taggedComposable<Route.PrayerTimes>(ScreenTags.PrayerTimes) { }
            taggedComposable<Route.PrayerTracker>(ScreenTags.PrayerTracker) { }
        }
    """)

    graph = check_docs.nav_graph_source()

    assert check_docs.wired_destination_count(graph) == 3


def test_a_bare_composable_in_a_feature_module_trips_nav_04(check_docs, tmp_path: Path):
    """The check that goes vacuous, not red, if the scan stays rooted at app/."""
    _rebase(check_docs, tmp_path)
    _write(_module_root(tmp_path, "app"), "core/navigation/NavGraph.kt", """\
        fun NavGraphBuilder.appGraph() {
            taggedComposable<Route.Home>(ScreenTags.Home) { }
        }
    """)
    _write(_module_root(tmp_path, "feature/qibla"), "presentation/qibla/QiblaGraph.kt", """\
        fun NavGraphBuilder.qiblaGraph() {
            composable<Route.Qibla> { }
        }
    """)

    graph = check_docs.nav_graph_source()

    assert check_docs.bare_composable_destinations(graph) == ["Qibla"]


# ──────────────────────────────────────────────────────────────────────────────
# expect_covered / expect_floor — an empty or shrunken scan must fail
# ──────────────────────────────────────────────────────────────────────────────

def test_an_empty_scan_fails_instead_of_reporting_ok_zero(check_docs, tmp_path: Path):
    """`ok 0 Workers documented` was the bug: nothing found, so nothing missing."""
    _rebase(check_docs, tmp_path)
    _module_root(tmp_path, "app")
    report = check_docs.Report()

    report.expect_covered(
        "SUB-02", expected=[], haystack="whatever", doc=check_docs.SUBSYSTEMS,
        noun="Workers", fix="…",
    )

    assert report.passes == []
    assert len(report.failures) == 1
    check, detail = report.failures[0]
    assert check == "SUB-02"
    assert "matched nothing" in detail


def test_expect_floor_fails_when_the_scan_shrinks(check_docs):
    report = check_docs.Report()

    report.expect_floor("SUB-02", 6, 7, noun="Workers")

    assert report.floors_met == []
    assert report.failures[0][0] == "SUB-02"
    assert "only 6 Workers found, floor is 7" in report.failures[0][1]


def test_expect_floor_allows_the_inventory_to_grow(check_docs):
    report = check_docs.Report()

    report.expect_floor("SUB-02", 9, 7, noun="Workers")

    assert report.failures == []
    assert report.floors_met == ["SUB-02 9 >= 7 Workers"]


def test_every_minimum_is_a_positive_integer(check_docs):
    """A floor of 0 is not a floor."""
    assert check_docs.MINIMUMS
    assert all(v > 0 for v in check_docs.MINIMUMS.values())


# ──────────────────────────────────────────────────────────────────────────────
# SUB-06 — the fourth DataStore, built with DataStoreFactory rather than named
# ──────────────────────────────────────────────────────────────────────────────

def test_a_hand_built_datastore_is_part_of_the_inventory(check_docs, tmp_path: Path):
    """`preferencesDataStore(name = …)` alone missed the Glance widget state store."""
    _rebase(check_docs, tmp_path)
    app = _module_root(tmp_path, "app")
    _write(app, "data/local/datastore/PreferencesDataStore.kt",
           'val x = preferencesDataStore(name = "nimaz_preferences")\n')
    _write(app, "widget/core/JsonGlanceStateDefinition.kt",
           "val store = DataStoreFactory.create(serializer = s) { file }\n")

    assert check_docs.preferences_datastore_names() == ["nimaz_preferences"]
    assert check_docs.datastore_names() == ["JsonGlanceStateDefinition", "nimaz_preferences"]


# ──────────────────────────────────────────────────────────────────────────────
# A whole post-migration tree, and the negative test this PR exists for
#
# These go through `check_sub(report)` rather than calling `expect_floor` directly.
# A test that builds its own Report and asserts on it proves the floor *function*
# works while saying nothing about whether it is still wired into the check — so a
# later PR could delete the `expect_floor(...)` call to make a refactor go green
# and the suite would stay 100% green. Running the real entry point is the only
# assertion that catches that.
# ──────────────────────────────────────────────────────────────────────────────

SCHEMA_VERSION = 25

# (gradle module, package path, class name) — deliberately split across two modules,
# which is what the tree looks like after :feature:widget exists.
WORKERS = [
    ("app", "data/audio/AdhanDownloadWorker.kt", "AdhanDownloadWorker"),
    ("feature/widget", "widget/nextprayer/NextPrayerWorker.kt", "NextPrayerWorker"),
    ("feature/widget", "widget/prayertimes/PrayerTimesWorker.kt", "PrayerTimesWorker"),
    ("feature/widget", "widget/prayertracker/PrayerTrackerWorker.kt", "PrayerTrackerWorker"),
    ("feature/widget", "widget/hijridate/HijriDateWorker.kt", "HijriDateWorker"),
    ("feature/widget", "widget/hijricalendar/HijriCalendarWorker.kt", "HijriCalendarWorker"),
    ("feature/widget", "widget/khatam/KhatamWorker.kt", "KhatamWorker"),
]

SERVICES = [
    ("app", "data/audio/AdhanPlaybackService.kt", "AdhanPlaybackService"),
    ("app", "data/audio/QuranPlaybackService.kt", "QuranPlaybackService"),
    ("app", "data/audio/DuaPlaybackService.kt", "DuaPlaybackService"),
    ("app", "data/announcement/NimazMessagingService.kt", "NimazMessagingService"),
]

WIDGET_PACKAGES = [
    ("nextprayer", "next_prayer_widget"),
    ("prayertimes", "prayer_times_widget"),
    ("prayertracker", "prayer_tracker_widget"),
    ("hijridate", "hijri_date_widget"),
    ("hijricalendar", "hijri_calendar_widget"),
    ("khatam", "khatam_widget"),
]

# The channel-id regex is `const val CHANNEL_ID[A-Z_]* = "…"`, so the constant
# suffixes have to be letters — digits would not match, and a fixture that quietly
# fails to trip a scan proves nothing.
CHANNELS = [f"channel_{chr(c)}" for c in range(ord("a"), ord("a") + 12)]
PREFERENCE_STORES = ["nimaz_preferences", "nimaz_announcements", "nimaz_ai_device"]
PAYLOAD_KEYS = ["title", "body", "route"]
ANNOUNCEMENT_TYPES = ["info", "celebration"]
CELEBRATION_EVENTS = ["eid_al_fitr", "eid_al_adha"]


def _build_tree(check_docs, tmp_path: Path) -> dict[str, Path]:
    """A two-module tree that satisfies every SUB check, and the doc describing it."""
    _rebase(check_docs, tmp_path)
    app = _module_root(tmp_path, "app")
    widget = _module_root(tmp_path, "feature/widget")
    written: dict[str, Path] = {}

    for gradle_module, relative, name in WORKERS:
        root = app if gradle_module == "app" else widget
        written[name] = _write(root, relative, f"class {name} : CoroutineWorker()\n")
    for _, relative, name in SERVICES:
        written[name] = _write(app, relative, f"class {name} : Service()\n")

    _write(app, "core/util/PrayerNotificationScheduler.kt", "object Channels {\n" + "".join(
        f'    const val CHANNEL_ID_{cid[-1].upper()} = "{cid}"\n' for cid in CHANNELS
    ) + "}\n")

    for store in PREFERENCE_STORES:
        _write(app, f"data/local/datastore/{store}.kt",
               f'private val Context.store by preferencesDataStore(name = "{store}")\n')

    _write(widget, "widget/core/JsonGlanceStateDefinition.kt",
           "abstract class JsonGlanceStateDefinition<T>(private val fileName: String) {\n"
           "    fun store() = DataStoreFactory.create(serializer = s) { file }\n}\n")
    for package_name, file_name in WIDGET_PACKAGES:
        written[file_name] = _write(
            widget, f"widget/{package_name}/StateDefinition.kt",
            f'object StateDefinition : JsonGlanceStateDefinition<S>(\n'
            f'    fileName = "{file_name}",\n)\n',
        )

    _write(app, "data/local/database/NimazDatabase.kt",
           f"const val NIMAZ_DATABASE_VERSION = {SCHEMA_VERSION}\n")
    _write(app, "data/announcement/AnnouncementPayloadMapper.kt", "object Keys {\n" + "".join(
        f'    const val KEY_{k.upper()} = "{k}"\n' for k in PAYLOAD_KEYS
    ) + "}\n")
    _write(app, "domain/model/Announcement.kt",
           "enum class AnnouncementType(val key: String) {\n"
           + "".join(f'    {k.upper()}("{k}"),\n' for k in ANNOUNCEMENT_TYPES)
           + "}\n\nenum class CelebrationEvent(val key: String) {\n"
           + "".join(f'    {k.upper()}("{k}"),\n' for k in CELEBRATION_EVENTS)
           + "}\n")

    documented = (
        [name for _, _, name in WORKERS]
        + [name for _, _, name in SERVICES]
        + [f"widget/{package_name}/" for package_name, _ in WIDGET_PACKAGES]
        + CHANNELS
        + PREFERENCE_STORES
        + ["JsonGlanceStateDefinition"]
        + [file_name for _, file_name in WIDGET_PACKAGES]
    )
    check_docs.SUBSYSTEMS.write_text(
        f"# Subsystems\n\n## 0. Inventory\n\n**Current schema version:** `{SCHEMA_VERSION}`\n\n"
        + "\n".join(f"- `{item}`" for item in documented)
        + "\n\n## 12. Engagement announcements (FCM)\n\n"
        + "\n".join(
            f"- `{key}`" for key in PAYLOAD_KEYS + ANNOUNCEMENT_TYPES + CELEBRATION_EVENTS
        )
        + "\n",
        encoding="utf-8",
    )
    return written


def _run_check_sub(check_docs):
    report = check_docs.Report()
    check_docs.check_sub(report)
    return report


def test_the_full_two_module_fixture_passes_every_sub_check(check_docs, tmp_path: Path):
    """The tree as it looks after :feature:widget exists — no check notices the move."""
    _build_tree(check_docs, tmp_path)

    report = _run_check_sub(check_docs)

    assert report.failures == [], report.failures
    assert len(report.passes) == 9  # SUB-01 … SUB-09


def test_the_fixture_meets_every_sub_floor_exactly(check_docs, tmp_path: Path):
    """If the fixture had slack, the negative tests below would prove nothing."""
    _build_tree(check_docs, tmp_path)

    report = _run_check_sub(check_docs)

    met = {line.split()[0]: line for line in report.floors_met}
    for check in ("SUB-02", "SUB-03", "SUB-04", "SUB-05", "SUB-06", "SUB-06-PREFS",
                  "SUB-06-GLANCE"):
        actual, _, floor = met[check].split()[1:4]
        assert actual == floor, met[check]


def test_removing_one_worker_turns_sub_02_red(check_docs, tmp_path: Path):
    """The PR's stated exit criterion, run through the real check_sub().

    Before this change, a Worker moving out of the scan's reach left SUB-02 green:
    it documented every Worker it could still see. It still does — the pass below is
    real — which is exactly why the floor has to exist, and why this asserts on the
    output of `check_sub` rather than on a Report it built itself.
    """
    written = _build_tree(check_docs, tmp_path)
    written["KhatamWorker"].unlink()  # the widget module the scan cannot reach

    report = _run_check_sub(check_docs)

    assert any("6 Workers documented" in line for line in report.passes)
    assert [check for check, _ in report.failures] == ["SUB-02"]
    assert "only 6 Workers found, floor is 7" in report.failures[0][1]


def test_removing_a_glance_state_file_turns_sub_06_red(check_docs, tmp_path: Path):
    """The six per-widget DataStore files are checked literals, not prose."""
    written = _build_tree(check_docs, tmp_path)
    written["khatam_widget"].unlink()

    report = _run_check_sub(check_docs)

    # SUB-04 stays green — the widget/khatam/ package is still there. Only the
    # DataStore file it declares is gone, which is precisely what SUB-06 now sees.
    failed = {check for check, _ in report.failures}
    assert failed == {"SUB-06", "SUB-06-GLANCE"}
    detail = dict(report.failures)["SUB-06-GLANCE"]
    assert "only 5 Glance widget state files found, floor is 6" in detail


def test_deleting_a_service_turns_sub_03_red(check_docs, tmp_path: Path):
    written = _build_tree(check_docs, tmp_path)
    written["QuranPlaybackService"].unlink()

    report = _run_check_sub(check_docs)

    assert [check for check, _ in report.failures] == ["SUB-03"]
    assert "only 3 Services found, floor is 4" in report.failures[0][1]


def test_an_empty_app_fails_every_sub_scan_rather_than_passing(check_docs, tmp_path: Path):
    """The end state of the migration if nothing here worked: app/ emptied out."""
    _build_tree(check_docs, tmp_path)
    for path in (tmp_path / "feature/widget").rglob("*Worker.kt"):
        path.unlink()
    (tmp_path / "app/src/main/java/com/arshadshah/nimaz"
     "/data/audio/AdhanDownloadWorker.kt").unlink()

    report = _run_check_sub(check_docs)

    details = dict(report.failures)
    assert "SUB-02" in details
    # Both guards fire: the floor, and the empty-expected-set rule.
    assert "matched nothing" in details["SUB-02"] or "only 0 Workers" in details["SUB-02"]
    assert not any("0 Workers documented" in line for line in report.passes)


# ──────────────────────────────────────────────────────────────────────────────
# Smoke — the real script against the real repo
# ──────────────────────────────────────────────────────────────────────────────

def test_the_real_repo_passes_every_check():
    result = subprocess.run(
        [sys.executable, str(SCRIPT)], capture_output=True, text=True
    )

    assert result.returncode == 0, result.stdout + result.stderr
    assert "All 23 documentation checks passed" in result.stdout


def test_every_floor_is_still_wired_into_a_check():
    """Pins the floor *count*, not just that floors exist.

    A floor only guards anything while its `expect_floor(...)` call is still in
    `check_nav`/`check_sub`. Deleting one is the cheapest possible way to make a
    refactor go green, and it leaves no trace in the check output — the run still
    says all 23 passed. Counting them is what makes that deletion fail here.
    """
    module = _load()
    result = subprocess.run(
        [sys.executable, str(SCRIPT)], capture_output=True, text=True
    )

    assert f"({len(module.MINIMUMS)} scan floors met)" in result.stdout, result.stdout


def test_no_check_reports_a_zero_count_against_the_real_repo():
    """A passing line that says `0` is the silent failure in written form."""
    result = subprocess.run(
        [sys.executable, str(SCRIPT)], capture_output=True, text=True
    )

    zeroes = [
        line for line in result.stdout.splitlines()
        if line.strip().startswith("ok") and " 0 " in line
    ]
    assert zeroes == []
