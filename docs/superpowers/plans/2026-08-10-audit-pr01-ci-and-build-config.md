# Audit PR 1 — CI and build config: Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the CI lanes actually run, make the build faster, and put a coverage number on every PR — so the eleven PRs stacked above this one land on infrastructure that tells the truth.

**Architecture:** Five independent config changes, no production Kotlin touched. Two fix workflow triggers that silently never fire; one enables Gradle's parallel, build-cache and configuration-cache; one adds a jacoco report and a sticky PR comment; one deletes a duplicated version list that has already drifted. Nothing here changes app behaviour, so the verification for each task is "run the thing and read the output", not a unit test.

**Tech Stack:** GitHub Actions, Gradle 9 / AGP 9.2.1, JaCoCo, fastlane, Kotlin 2.3.21.

## Global Constraints

- Branch `epic/audit-01-ci`, cut from `epic/audit`. It is the bottom of the stack, so it targets `epic/audit`.
- Toolchain: JDK 21, `compileSdk 36`, `minSdk 29`. `sdk.dir` in `local.properties` or `ANDROID_HOME`.
- Gate before finishing, all four, per `CLAUDE.md`:
  ```bash
  ./gradlew :app:compileDebugKotlin
  ./gradlew :app:testDebugUnitTest
  ./gradlew :app:lintDebug          # SLOW (~10 min) and CI-blocking — do not skip
  python3 scripts/check_docs.py
  ```
- **Commit messages carry no `Co-Authored-By` trailer** — matching the `epic/viewmodel-cleanup` stack convention for this repo.
- Do not push to `dev`. Everything lands on `epic/audit` and reaches `dev` only after the internal-track build is validated.
- Issues closed by this PR: #461, #462, #463, #464, #465. Epic: #460.

---

### Task 1: Point the instrumented-test workflow at a branch that exists

**Files:**
- Modify: `.github/workflows/android_instrumented_tests.yml:15-17` (the `on:` block) and the trigger comment at `:5-6`

**Interfaces:**
- Consumes: nothing
- Produces: a `push` trigger on `dev` that actually fires. Task 4 of PR 12 (`AccessibilityChecks` on the instrumented lane) depends on this lane running at all.

- [ ] **Step 1: Confirm the trigger has never fired**

```bash
gh run list --workflow=android_instrumented_tests.yml --event=push --limit 20
git branch -r | grep -E 'origin/(main|dev|master)$'
```

Expected: the run list is empty for `push`, and `origin/main` does not exist. This is the evidence the audit's claim is real — record it in the PR body.

- [ ] **Step 2: Fix the trigger**

In `.github/workflows/android_instrumented_tests.yml`, change:

```yaml
on:
  push:
    branches: [ main ]
  pull_request:
```

to:

```yaml
on:
  push:
    branches: [ dev ]
  pull_request:
```

- [ ] **Step 3: Fix the comment that documents it**

The header comment at `:5-6` says:

```
#  - push to main           → the canonical lane.
```

Change `main` to `dev`. A comment describing a branch that does not exist is how this survived.

- [ ] **Step 4: Verify the workflow still parses**

Run: `python3 -c "import yaml,sys; yaml.safe_load(open('.github/workflows/android_instrumented_tests.yml')); print('ok')"`
Expected: `ok`

- [ ] **Step 5: Commit**

```bash
git add .github/workflows/android_instrumented_tests.yml
git commit -m "ci: run instrumented tests on dev, not a branch that does not exist

The workflow triggered on push to main. This repository's branches are dev and
master, so that trigger has never fired — only pull_request did anything.

Closes #461"
```

---

### Task 2: Let the epic branch reach the internal track

**Files:**
- Modify: `.github/workflows/internal_testing.yml:37-40` (the push `branches:` list)

**Interfaces:**
- Consumes: nothing
- Produces: the ability to ship `epic/audit` to the Play internal track with an empty `[deploy]` commit. **The whole epic's validation step depends on this.**

- [ ] **Step 1: Confirm the gap**

```bash
sed -n '36,41p' .github/workflows/internal_testing.yml
```

Expected output shows `claude/**`, `feature/**`, `fix/**` — and no `epic/**`. Without this, the marker commit on `epic/audit` produces no failure and no build: it just does nothing.

- [ ] **Step 2: Add the pattern**

```yaml
  push:
    branches:
      - 'claude/**'
      - 'feature/**'
      - 'fix/**'
      - 'epic/**'
```

- [ ] **Step 3: Verify the workflow still parses**

Run: `python3 -c "import yaml,sys; yaml.safe_load(open('.github/workflows/internal_testing.yml')); print('ok')"`
Expected: `ok`

- [ ] **Step 4: Commit**

```bash
git add .github/workflows/internal_testing.yml
git commit -m "ci: allow epic branches to ship to the internal track

The push filter listed claude/**, feature/** and fix/** only, so a
'chore: internal build [deploy]' commit on an epic branch silently did nothing.

Closes #462"
```

---

### Task 3: Turn on Gradle's parallel, build cache and configuration cache

**Files:**
- Modify: `gradle.properties:9` (heap) and `:10-13` (the commented-out parallel line)

**Interfaces:**
- Consumes: nothing
- Produces: faster builds for every task above. No API surface.

- [ ] **Step 1: Record the current cold-build time, so the change is measurable**

```bash
./gradlew --stop
time ./gradlew :app:compileDebugKotlin
```

Write the number in the PR body. Without a before, "it's faster" is an assertion.

- [ ] **Step 2: Replace the settings block**

Replace `gradle.properties` lines 7–13 (the jvmargs comment, the jvmargs line, and the three commented parallel lines) with:

```properties
# Specifies the JVM arguments used for the daemon process.
# 2048m was tight for a 965-file Kotlin module running KSP.
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
# Decoupled enough for parallel execution: this is effectively a single-module
# build (:app) plus buildSrc-style convention plugins.
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
```

- [ ] **Step 3: Verify the build still works from cold**

```bash
./gradlew --stop
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL, and a line reading `Configuration cache entry stored.`

- [ ] **Step 4: Verify the configuration cache is actually reused**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: `Reusing configuration cache.` If instead you get a list of configuration-cache *problems*, do not fight them here. Drop only `org.gradle.configuration-cache=true`, keep the other three, and open a follow-up issue on #460 naming the incompatible plugin. Parallel and build-cache are the bulk of the win and must not be blocked by it.

- [ ] **Step 5: Verify the test and lint lanes still pass under the new settings**

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

Expected: both BUILD SUCCESSFUL. Lint is slow (~10 min) and is the one most likely to object to the configuration cache — this is the step that catches it.

- [ ] **Step 6: Commit**

```bash
git add gradle.properties
git commit -m "build: enable parallel execution, build cache and configuration cache

Raises the daemon heap to 4g as well — 2g was tight for a 965-file Kotlin
module running KSP.

Closes #463"
```

---

### Task 4: Put a coverage number on every PR

**Files:**
- Modify: `app/build.gradle.kts:421-435` (the `jacocoTestReport` task — pin the XML output location)
- Modify: `.github/workflows/pr_checks.yml:58-71` (add the report and comment steps)
- Create: `scripts/coverage_summary.py`

**Interfaces:**
- Consumes: `jacocoTestReport`, which already exists and already `dependsOn("testDebugUnitTest")`.
- Produces: `scripts/coverage_summary.py <jacoco-xml-path>` printing a Markdown table to stdout and exiting 0. Nothing else consumes it.

**Why no threshold:** `jacocoAtomsCoverageVerification` already exists with a 90% rule scoped to `presentation.components.atoms`, deliberately kept out of the `check` graph "so it never blocks the existing CI lane". Leave that decision alone. This task makes coverage *visible*, not *blocking* — eleven PRs of new tests are about to land, and a hard global gate would fail every one of them until the last.

- [ ] **Step 1: Write the failing test for the summary script**

Create `scripts/test_coverage_summary.py`:

```python
import subprocess
import sys
import textwrap
from pathlib import Path

FIXTURE = textwrap.dedent("""\
    <?xml version="1.0" encoding="UTF-8"?>
    <report name="app">
      <counter type="INSTRUCTION" missed="300" covered="700"/>
      <counter type="BRANCH" missed="40" covered="60"/>
      <counter type="LINE" missed="25" covered="75"/>
    </report>
    """)


def test_summary_reports_line_and_branch_percentages(tmp_path: Path):
    xml = tmp_path / "jacocoTestReport.xml"
    xml.write_text(FIXTURE)

    out = subprocess.run(
        [sys.executable, "scripts/coverage_summary.py", str(xml)],
        capture_output=True, text=True, check=True,
    ).stdout

    assert "75.0%" in out          # lines: 75 covered of 100
    assert "60.0%" in out          # branches: 60 covered of 100
    assert "70.0%" in out          # instructions: 700 covered of 1000


def test_summary_handles_a_missing_report(tmp_path: Path):
    result = subprocess.run(
        [sys.executable, "scripts/coverage_summary.py", str(tmp_path / "nope.xml")],
        capture_output=True, text=True,
    )
    assert result.returncode == 0
    assert "no coverage report" in result.stdout.lower()
```

- [ ] **Step 2: Run it to make sure it fails**

Run: `python3 -m pytest scripts/test_coverage_summary.py -v`
Expected: FAIL — `can't open file 'scripts/coverage_summary.py'`. (If `pytest` is unavailable, `pip install pytest` into a venv; this script is repo tooling, not app code, and `scripts/check_docs.py` is likewise plain Python.)

- [ ] **Step 3: Write the script**

Create `scripts/coverage_summary.py`:

```python
#!/usr/bin/env python3
"""Print a Markdown coverage summary from a JaCoCo XML report.

Deliberately non-failing: a missing report prints a note and exits 0. This runs
on every PR and must never be the reason a PR goes red — the audit epic (#460)
lands eleven PRs of new tests, and a coverage gate would block all of them until
the last one merged.
"""
import sys
import xml.etree.ElementTree as ET  # trusted input: JaCoCo output from our own build step
from pathlib import Path

COUNTERS = [("LINE", "Lines"), ("BRANCH", "Branches"), ("INSTRUCTION", "Instructions")]


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: coverage_summary.py <jacoco-xml>", file=sys.stderr)
        return 2

    path = Path(sys.argv[1])
    if not path.is_file():
        print("### Coverage\n\n_No coverage report was produced for this run._")
        return 0

    root = ET.parse(path).getroot()
    totals = {
        c.get("type"): (int(c.get("missed", 0)), int(c.get("covered", 0)))
        for c in root.findall("counter")
    }

    print("### Coverage")
    print()
    print("| Metric | Covered | Total | % |")
    print("|---|---:|---:|---:|")
    for key, label in COUNTERS:
        if key not in totals:
            continue
        missed, covered = totals[key]
        total = missed + covered
        pct = (covered / total * 100) if total else 0.0
        print(f"| {label} | {covered} | {total} | {pct:.1f}% |")
    print()
    print("_Reported, not gated — see #464._")
    return 0


if __name__ == "__main__":
    sys.exit(main())
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `python3 -m pytest scripts/test_coverage_summary.py -v`
Expected: 2 passed.

- [ ] **Step 5: Pin the JaCoCo XML output location**

The default XML destination depends on the task name and is easy to break. In `app/build.gradle.kts`, inside `tasks.register<JacocoReport>("jacocoTestReport")`, change the `reports` block to:

```kotlin
    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(false)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/jacocoTestReport.xml"))
    }
```

- [ ] **Step 6: Verify the report is produced where the script expects it**

```bash
./gradlew :app:jacocoTestReport
python3 scripts/coverage_summary.py app/build/reports/jacoco/jacocoTestReport.xml
```

Expected: a Markdown table with three rows and non-zero percentages. Record the current numbers in the PR body — they are the baseline the rest of the epic moves.

- [ ] **Step 7: Wire it into the PR lane**

In `.github/workflows/pr_checks.yml`, immediately after the `Run Tests and Lint` step (`:58-59`), insert:

```yaml
      # Coverage is reported, never gated. jacocoTestReport already depends on
      # testDebugUnitTest, which the step above has run, so this is close to free.
      - name: Coverage report
        if: always()
        run: ./gradlew :app:jacocoTestReport

      - name: Summarise coverage
        if: always()
        run: |
          python3 scripts/coverage_summary.py \
            app/build/reports/jacoco/jacocoTestReport.xml > coverage.md
          cat coverage.md >> "$GITHUB_STEP_SUMMARY"

      - name: Comment coverage on the PR
        if: always()
        uses: marocchino/sticky-pull-request-comment@v2
        with:
          header: coverage
          path: coverage.md
```

`if: always()` on all three matters: when a test fails you most want to see what coverage did.

- [ ] **Step 8: Grant the workflow permission to comment**

`pr_checks.yml` declares no `permissions:` block, so it inherits the repository default, which may be read-only. Add one at job level, immediately above `runs-on` in the `check` job:

```yaml
    permissions:
      contents: read
      pull-requests: write
```

- [ ] **Step 9: Verify both workflows still parse**

```bash
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/pr_checks.yml')); print('ok')"
```
Expected: `ok`

- [ ] **Step 10: Commit**

```bash
git add scripts/coverage_summary.py scripts/test_coverage_summary.py \
        app/build.gradle.kts .github/workflows/pr_checks.yml
git commit -m "ci: report test coverage on every pull request

Four jacoco tasks and a 90% rule already existed, but the rule is scoped to the
atoms package and kept out of the check graph, and the PR lane runs only
'fastlane android test' — so no coverage number ever reached a PR.

Reported as a sticky comment, not gated: eleven PRs of new tests are about to
land and a global threshold would fail all of them until the last.

Closes #464"
```

---

### Task 5: Delete the README version list that has already drifted

**Files:**
- Modify: `README.md:42-56` (the `## Tech Stack` section)

**Interfaces:**
- Consumes: nothing
- Produces: nothing

- [ ] **Step 1: Confirm the drift, so the commit message states facts**

```bash
grep -nE "agp|kotlin|composeBom|media3|workManager" gradle/libs.versions.toml | head
```

Expected: the real values, against the README's claims of AGP 8.12.0, Kotlin 2.3.0, Compose BOM 2026.01.00, Media3 1.9.0, WorkManager 2.11.0. The audit measured the actual values as 9.2.1, 2.3.21, 2026.05.01, 1.10.1, 2.11.2 — confirm rather than assume, since `dev` has moved since.

- [ ] **Step 2: Replace the section**

Replace `README.md` lines 42–56 (from `## Tech Stack` through the `- \`gradle/libs.versions.toml\`` bullet) with:

```markdown
## Tech Stack

Kotlin and Jetpack Compose, Clean Architecture with MVVM + UDF, Hilt, Room,
DataStore, Media3, WorkManager and Glance.

**Versions are not duplicated here.** The version catalog is the single source of
truth — see [`gradle/libs.versions.toml`](gradle/libs.versions.toml). A hand-maintained
list in this file drifted silently for five dependencies before it was caught, and
`scripts/check_docs.py` does not cover the README.
```

- [ ] **Step 3: Verify the docs checker still passes**

Run: `python3 scripts/check_docs.py`
Expected: `All 23 documentation checks passed.` — in particular DOC-03, which resolves cross-links, must still pass with the new relative link to the catalog.

- [ ] **Step 4: Commit**

```bash
git add README.md
git commit -m "docs: stop duplicating dependency versions in the README

Five of them had drifted: AGP, Kotlin, the Compose BOM, Media3 and WorkManager.
check_docs.py enforces 23 obligations and none of them cover the README, so a
list here cannot be kept honest. Point at the version catalog instead.

Closes #465"
```

---

### Task 6: Open the PR at the bottom of the stack

**Files:** none

- [ ] **Step 1: Run the full gate one more time on the finished branch**

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
python3 scripts/check_docs.py
```

All four must pass. Do not open the PR on a partial run — `lintDebug` is CI-blocking and takes ~10 minutes, and it is the one that catches what the others cannot.

- [ ] **Step 2: Initialise the stack and submit**

```bash
gh stack init epic/audit-01-ci
gh stack submit
```

The PR targets `epic/audit`. Verify with `gh pr view --json baseRefName -q .baseRefName` — expected `epic/audit`, **not** `dev`.

- [ ] **Step 3: Fill in the PR body with the measurements taken along the way**

The body must carry: the empty `gh run list` output from Task 1 Step 1, the before/after build times from Task 3, and the baseline coverage table from Task 4 Step 6. Every claim this PR makes is measurable, so measure it.

- [ ] **Step 4: Confirm the coverage comment actually appeared**

Once CI finishes: `gh pr view --comments | grep -A8 "Coverage"`
Expected: the sticky comment with the table. If the step ran but no comment appeared, the job-level `permissions:` block from Task 4 Step 8 is missing or wrong.

---

## Notes for the next PR in the stack

PR 2 (`epic/audit-02-baseline-profile`) branches from `epic/audit-01-ci`, not from `epic/audit`. Add it with `gh stack add`, not `git checkout -b` off the integration branch — the stack tool tracks the parent relationship and `gh stack submit` retargets the PRs when anything below is rebased.
