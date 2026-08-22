#!/usr/bin/env python3
"""Reproduce the numbers in docs/specs/multi-module-migration/BASELINE.md.

This is the driver, committed so the measurements can be re-run rather than
believed. `gradle-profiler` is not used — this is `--profile` plus a manual loop,
and BASELINE.md says so plainly.

    python3 scripts/measure_build_baseline.py results.json
    python3 scripts/measure_build_baseline.py results.json inc_leaf_screen
    python3 scripts/measure_build_baseline.py profile.json --profile

Re-running it after the split is the point: the protocol has to be identical at
every phase gate or the comparison means nothing. Read BASELINE.md §2 before
changing anything here — several of the choices below (`--no-build-cache` on the
clean build, a different edit on every single run, `--stop` before every profiled
run) exist to stop a number from being flattering rather than true.

Pre-warm ~/.gradle/caches/nimaz-data first, or one 54 MB authenticated download
lands inside whichever run happens to go first.
"""
import json
import os
import re
import shutil
import statistics
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
GRADLEW = str(ROOT / "gradlew")
SRC = ROOT / "app/src/main/java/com/arshadshah/nimaz"

# The four files this script edits are chosen to sit at different depths of the dependency graph,
# and #551 is moving them out of :app one milestone at a time — the domain model left in PR 5, the
# button and strings.xml in PR 10. Each is resolved against its possible homes rather than
# hardcoded, because a stale path here does not fail loudly: the edit would land on a file nobody
# compiles and the "incremental build" number would be measuring nothing. That is the same shape
# as the scan floors #553 added, applied to a measurement rather than a check.
def _resolve(*candidates):
    for candidate in candidates:
        if candidate.is_file():
            return candidate
    looked = "\n  ".join(str(c) for c in candidates)
    raise SystemExit(
        f"None of these exist:\n  {looked}\n"
        "A file this script edits has moved. Update its candidates — do NOT let the measurement "
        "run against a path that is not compiled, or the number is meaningless."
    )


LEAF = _resolve(SRC / "presentation/screens/settings/ZakatSettingsScreen.kt")
BUTTON = _resolve(
    ROOT / "core/ui/src/main/kotlin/com/arshadshah/nimaz/presentation/components/atoms/NimazButton.kt",
    SRC / "presentation/components/atoms/NimazButton.kt",
)
DOMAIN = _resolve(
    ROOT / "core/domain/src/main/kotlin/com/arshadshah/nimaz/domain/model/PrayerModels.kt",
    SRC / "domain/model/PrayerModels.kt",
)
STRINGS = _resolve(
    ROOT / "core/ui/src/main/res/values/strings.xml",
    ROOT / "app/src/main/res/values/strings.xml",
)

ORIGINALS = {}
# Monotonic across the whole session: two runs must never produce byte-identical
# sources, or the second would be served from the local build cache and the
# number would be a lie.
EDIT_SEQ = [0]


def restore_all():
    for path, text in ORIGINALS.items():
        path.write_text(text)


def _snapshot(path):
    if path not in ORIGINALS:
        ORIGINALS[path] = path.read_text()
    return ORIGINALS[path]


def whitespace_edit(path):
    """A whitespace-only edit, different on every call, reverted at the end."""
    text = _snapshot(path)
    EDIT_SEQ[0] += 1
    if path.suffix == ".xml":
        # Immediately after the <resources> tag: whitespace is valid XML there and
        # no string value changes.
        cut = text.index(">", text.index("<resources")) + 1
        path.write_text(text[:cut] + "\n" * EDIT_SEQ[0] + text[cut:])
    else:
        path.write_text(text.rstrip("\n") + "\n" * EDIT_SEQ[0])


def abi_edit(path):
    """An ABI-changing edit: a new public top-level declaration."""
    text = _snapshot(path)
    EDIT_SEQ[0] += 1
    n = EDIT_SEQ[0]
    path.write_text(f"{text.rstrip()}\n\nfun zakatBaselineProbe{n}(): Int = {n}\n")


def gradle(args, cc, capture_profile=False):
    cmd = [GRADLEW, "--console=plain"] + list(args)
    if not cc:
        cmd.append("--no-configuration-cache")
    if capture_profile:
        cmd.append("--profile")
    started = time.monotonic()
    proc = subprocess.run(cmd, cwd=ROOT, capture_output=True, text=True)
    elapsed = time.monotonic() - started
    if proc.returncode != 0:
        sys.stderr.write("FAILED: " + " ".join(cmd) + "\n" + proc.stdout[-4000:])
        raise SystemExit(1)
    return elapsed


def stop_daemon():
    subprocess.run([GRADLEW, "--stop"], cwd=ROOT, capture_output=True)


SCENARIOS = {
    #                     runs  setup                      gradle args
    "clean_assembleDebug": (3, lambda cc: gradle([":app:clean"], cc),
                            [":app:assembleDebug", "--no-build-cache"]),
    "inc_leaf_screen": (5, lambda cc: whitespace_edit(LEAF), [":app:assembleDebug"]),
    "inc_leaf_screen_abi": (5, lambda cc: abi_edit(LEAF), [":app:assembleDebug"]),
    "inc_nimaz_button": (5, lambda cc: whitespace_edit(BUTTON), [":app:assembleDebug"]),
    "inc_domain_model": (5, lambda cc: whitespace_edit(DOMAIN), [":app:assembleDebug"]),
    "inc_strings_xml": (5, lambda cc: whitespace_edit(STRINGS), [":app:assembleDebug"]),
    "testDebugUnitTest": (3, lambda cc: None, [":app:testDebugUnitTest", "--rerun"]),
}


def summarise(times):
    return {
        "runs": len(times),
        "times": times,
        "median": round(statistics.median(times), 1),
        "min": min(times),
        "max": max(times),
    }


def run_scenarios(out, names):
    results = json.loads(out.read_text()) if out.exists() else {}
    try:
        for name in names:
            runs, setup, args = SCENARIOS[name]
            for cc in (False, True):
                key = f"{name}::{'cc' if cc else 'nocc'}"
                if key in results:
                    print("skip", key, flush=True)
                    continue
                stop_daemon()
                times = []
                for i in range(runs + 1):  # run 0 is the discarded warm-up
                    setup(cc)
                    elapsed = gradle(args, cc)
                    tag = " (warm-up, discarded)" if i == 0 else ""
                    print(f"{key} run{i}{tag}: {elapsed:.1f}s", flush=True)
                    if i:
                        times.append(round(elapsed, 1))
                results[key] = summarise(times)
                out.write_text(json.dumps(results, indent=2))
                restore_all()
    finally:
        restore_all()
    return results


PROFILE_ROWS = [
    "Total Build Time", "Startup", "Settings and buildSrc",
    "Loading Projects", "Configuring Projects", "Task Execution",
]


def read_profile(path):
    html = path.read_text()
    out = {}
    for row in PROFILE_ROWS:
        m = re.search(re.escape(row) + r"</td>\s*<td class=\"numeric\">([^<]+)</td>", html)
        out[row] = m.group(1) if m else None
    return out


def run_profile(out, runs=3):
    """Configuration time, on an otherwise fully up-to-date :app:assembleDebug.

    `--stop` before EVERY run, not just before the set: on a warm daemon a
    --no-configuration-cache build reports ~0.17s of configuration rather than
    ~3s, because the daemon still holds the compiled, class-loaded scripts. That
    is a real saving but it is not the configuration phase's cost, and reporting
    it as such makes the before column look 20x better than it is.
    """
    reports = ROOT / "build/reports/profile"
    shutil.rmtree(reports, ignore_errors=True)
    results = {}
    for cc in (False, True):
        if cc:
            gradle([":app:assembleDebug"], cc=True)  # prime the cache entry
        rows = []
        for _ in range(runs):
            stop_daemon()
            gradle([":app:assembleDebug"], cc, capture_profile=True)
            newest = max(reports.glob("profile-*.html"), key=os.path.getmtime)
            rows.append(read_profile(newest))
        results["cc" if cc else "nocc"] = rows
    out.write_text(json.dumps(results, indent=2))
    return results


def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    out = Path(sys.argv[1])
    rest = sys.argv[2:]
    if "--profile" in rest:
        print(json.dumps(run_profile(out), indent=2))
    else:
        names = rest[0].split(",") if rest else list(SCENARIOS)
        print(json.dumps(run_scenarios(out, names), indent=2))


if __name__ == "__main__":
    main()
