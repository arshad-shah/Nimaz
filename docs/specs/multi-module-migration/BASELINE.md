# Nimaz — pre-migration build baseline

> **Owns:** the build-time measurements the multi-module split (#551) is judged against, and the
> protocol they were taken under.
> **Update when:** a phase gate is reached — re-measure with the *same* protocol and add a
> column. Never edit a recorded number in place; append.
> **Related:** [`SPEC.md` §6.5](SPEC.md#65-tier-5--did-it-actually-pay-off) for the expectations
> these numbers test, [`EPIC.md`](EPIC.md) for the PR stack.

A number without a protocol is folklore. This document records both, so that a "before" and an
"after" taken months apart are actually comparable.

## Contents

- [1. Header — what was measured, where](#1-header--what-was-measured-where)
- [2. Protocol](#2-protocol)
- [3. Results](#3-results)
- [4. What is expected *not* to improve](#4-what-is-expected-not-to-improve)
- [5. The stopping rule, made unambiguous](#5-the-stopping-rule-made-unambiguous)
- [6. Honest limitations](#6-honest-limitations)

## 1. Header — what was measured, where

| | |
|---|---|
| Tree | The commit that introduced this file, on `mm/02-baseline-metrics` (PR 3 of the stack), parented on `9205bd6d` — before any module exists. It is not quoted by SHA because the SHA changes with every amend of the very commit that carries the numbers. Both columns were measured on that tree; the *before* column differs from the *after* column only by `--no-configuration-cache`. |
| Gradle | 9.5.1 |
| JDK | 21.0.10, Amazon Corretto (`21.0.10+7-LTS`) |
| AGP / compileSdk | AGP 9, compileSdk 37, minSdk 29 |
| Machine | Apple M2 Pro, 12 cores, 16 GB, macOS 26.5.1 |
| Daemon JVM | `-Xmx4096m` (`gradle.properties`) |
| Modules | `:app`, `:baselineprofile`, plus the `build-logic` included build |

## 2. Protocol

1. **The content-artifact cache is pre-warmed.** `~/.gradle/caches/nimaz-data` already holds the
   artifact pinned by `data.lock.json`, so `fetchNimazData` is a local sha256 verification and a
   copy. Without this, one cold 54 MB authenticated download lands inside whichever run happens
   to go first and contaminates every "clean" number — and offline CI could not reproduce it
   anyway.
2. **Per scenario:** `./gradlew --stop`, then **one discarded warm-up run**, then the measured
   runs. Stopping the daemon per scenario, not per run, is deliberate: a developer's second
   build of the day is the case worth optimising, not their first.
3. **Runs:** 5 measured runs for the incremental scenarios. **3** for `clean assembleDebug` and
   for `testDebugUnitTest`, because each takes minutes and the variance turned out to be under
   2s across the three. The count is stated per row in §3.1 — no row hides how many runs it is
   built from.
4. **Median, min and max** are recorded. A single number hides the run where Spotlight woke up.
5. **Every scenario is run twice over:** once with `--no-configuration-cache` (the *before*
   column) and once with the configuration cache on (the *after* column). Both columns come from
   the same commit and the same machine on the same day.
6. **Timing is wall clock around the `./gradlew` process**, not Gradle's own
   "BUILD SUCCESSFUL in …". It therefore includes launcher start-up and daemon connection —
   which is what a developer actually waits for, and is also where part of the
   configuration-cache win shows up.
7. **Incremental edits are whitespace-only and never repeated.** Each run appends a *different*
   number of blank lines, so no two runs produce byte-identical sources. Repeating an edit would
   let the local build cache serve the compile task and the second column would be a lie.
   (`strings.xml` gets its blank lines immediately after the `<resources>` tag, where whitespace
   is valid XML and no string value changes.) One **supplementary** scenario breaks this rule on
   purpose and says so in its row: an *ABI-changing* edit to the same leaf screen — a new public
   top-level declaration — because whitespace is ABI-neutral and therefore the friendliest
   possible case for a single-module build. The two bracket the real answer. Sources are restored
   after every scenario; the working tree ends clean.
8. **`clean assembleDebug` runs with `--no-build-cache`.** With the build cache on, "clean" is
   not cold — it is a cache restore, and the number moves with whatever happened to be in the
   cache. `--no-build-cache` gives a definition of "cold" that reproduces.
9. **`testDebugUnitTest` runs with `--rerun`**, which forces the test task itself to re-execute
   without re-running everything upstream.
10. **Configuration time** comes from the `--profile` HTML report's "Configuring Projects" line,
    on a separate, otherwise fully up-to-date `:app:assembleDebug`, with `./gradlew --stop`
    before **every** run. That last detail matters: measured on a warm daemon, a
    `--no-configuration-cache` build reports ~0.17s of configuration rather than ~3s, because the
    daemon still holds the compiled and class-loaded build scripts. That is a real effect and a
    real saving, but it is not the configuration phase's cost, and reporting it as such would
    make the before column look 20x better than it is.

The driver is committed as **[`scripts/measure_build_baseline.py`](../../../scripts/measure_build_baseline.py)**,
so these numbers can be re-run rather than believed:

```bash
python3 scripts/measure_build_baseline.py results.json                  # every scenario
python3 scripts/measure_build_baseline.py results.json inc_leaf_screen  # just one
python3 scripts/measure_build_baseline.py profile.json --profile        # §3.2
```

Scenario ids in that script match the row labels in §3.1 exactly. **`gradle-profiler` is not
installed on this machine and was not used.** This is `--profile` plus a manual loop, and no more
rigour than that should be read into these figures.

### Files touched by the incremental scenarios

| Scenario | File |
|---|---|
| leaf screen | `presentation/screens/settings/ZakatSettingsScreen.kt` (371 lines; referenced only by `NavGraph.kt` and `AdaptiveSettingsScreen.kt`) |
| design-system hub | `presentation/components/atoms/NimazButton.kt` (302 lines) |
| domain model | `domain/model/PrayerModels.kt` (321 lines) |
| resources | `res/values/strings.xml` (2,362 lines) |

## 3. Results

### 3.1 Build scenarios

Wall clock in seconds, **median (min–max)**. "Before" is `--no-configuration-cache`; "after" is
the configuration cache on, which is what `gradle.properties` now sets.

Every row is labelled with the scenario id that reproduces it — pass it to
`scripts/measure_build_baseline.py`, and cite it by that id, never by position in the table.

| Row (scenario id) | What it does | Runs | Before (cc off) | After (cc on) | Δ |
|---|---|---|---|---|---|
| **`clean_assembleDebug`** | `:app:clean`, then `:app:assembleDebug --no-build-cache` | 3 | **69.4** (68.5–70.6) | **70.1** (65.4–78.0) | none — see below |
| **`inc_leaf_screen`** | whitespace edit to the leaf screen | 5 | **6.7** (6.6–6.8) | **5.7** (5.7–6.9) | −15% |
| **`inc_leaf_screen_abi`** | *supplementary* — ABI-changing edit to the same leaf screen | 5 | **11.0** (10.9–14.9) | **9.1** (8.8–10.2) | −17% |
| **`inc_nimaz_button`** | whitespace edit to `NimazButton.kt` | 5 | **11.9** (10.4–38.8) | **8.9** (8.6–9.9) | −25% |
| **`inc_domain_model`** | whitespace edit to a domain model | 5 | **6.2** (5.8–18.9) | **5.4** (5.0–6.1) | −13% |
| **`inc_strings_xml`** | whitespace edit to `strings.xml` | 5 | **1.8** (1.7–3.3) | **1.1** (1.0–1.2) | −39% |
| **`testDebugUnitTest`** | `:app:testDebugUnitTest --rerun` | 3 | **50.7** (50.0–52.3) | **49.6** (49.0–51.2) | −2% (noise) |

Two things the min/max columns are earning their place by showing.

**`clean_assembleDebug` shows no configuration-cache benefit at all**, and the cache-on median is
nominally *worse*. It is not worse; the two ranges overlap almost completely (65.4–78.0 against
68.5–70.6). Roughly 4s of saved configuration inside a ~70s build is simply below this
measurement's noise floor. Recorded as "none" rather than as a number, because a −1% or +1% here
would be invented precision.

**Three `cc off` rows have a first-run outlier** (38.8s, 18.9s, 3.3s against medians of 11.9,
6.2 and 1.8). Each is run 1 of its set — the first *measured* run after `--stop` and the
discarded warm-up. Without the configuration cache the daemon re-runs the full configuration
phase every build, and the first one after a restart pays for JIT that later runs do not. The
cache-on column has no equivalent outlier, which is the same effect seen from the other side.

### 3.2 Configuration time (`--profile`)

Measured on an otherwise fully up-to-date `:app:assembleDebug`, with `./gradlew --stop` before
**every** run so that daemon warmth cannot flatter either column. 3 runs each; median.

| `--profile` line | Before (cc off) | After (cc on, entry reused) |
|---|---|---|
| Settings and buildSrc | 1.279s (1.240–1.300) | — (phase skipped) |
| **Configuring Projects** | **2.923s** (2.886–3.997) | **0s** (phase skipped) |
| Total build time | 8.026s (8.006–9.434) | 4.078s (4.009–4.135) |

This is the row to watch as modules are added: configuration cost is paid **per project**, so it
is the one number the split can make meaningfully worse. With the cache reused it is currently
zero, which is the whole reason for enabling it before the split rather than after.

### 3.3 Reading these numbers

**The configuration cache is worth ~1–3s on every incremental build and ~4s on an up-to-date
one**, and none of that has anything to do with modularisation. That is why §5 reads the stopping
rule off the *cache-on* column: the split has to earn its 40% on top of this, not out of it.

**The leaf-screen number has very little headroom.** A whitespace edit to `strings.xml` that
changes nothing downstream still costs **1.1s** — the floor of running `:app:assembleDebug` at
all. So of the leaf screen's 5.7s, about 4.6s is real compile, dex and package work, and a 40%
improvement means finding 2.3s inside that 4.6s. That is a demanding target, and §6 records a
measurement-stability problem that makes it harder still to judge. Read §5 and §6 together before
using this row to decide anything.

**The gap between an ABI-neutral and an ABI-changing edit is where the split's win has to come
from.** The supplementary ABI-changing edit — a new public top-level declaration in the same leaf
screen, which invalidates every downstream compilation unit in `:app` — costs **9.1s** against
the whitespace edit's **5.7s**. That 3.4s is what a module boundary can plausibly take back,
because after the split only the screen's own feature module is downstream of it. It also means
the two rows must be reported together: a split that halves `inc_leaf_screen_abi` and leaves
`inc_leaf_screen` alone has done real work that the stopping rule, read literally, would score as
a failure.

## 4. What is expected *not* to improve

Recorded **now**, before any module exists, so that a disappointing result cannot be explained
away afterwards.

| Measurement | Expectation after the split | Why |
|---|---|---|
| Clean `:app:assembleDebug` | Roughly flat, possibly slightly worse | More projects to configure and more module boundaries to cross; nothing is removed |
| Incremental — leaf screen | **Should improve substantially. This is the number the epic is judged on.** | A leaf screen ends up in a feature module that nothing depends on, so only that module recompiles |
| Incremental — `NimazButton.kt` | **Will not improve.** Shared hub by design | It lands in `:core:ui`, which every feature module depends on |
| Incremental — domain model | Will not improve much | `:core:domain` is at the bottom of the graph; a change there is felt everywhere |
| Incremental — `strings.xml` | **Will not improve.** Deliberately unsplit | 1,910 strings across six locales stay whole in `:core:ui` (SPEC §7) |
| `testDebugUnitTest` | Should improve, via parallel execution across modules | Independent module test tasks can run concurrently |
| Configuration time | Watch it — more modules means more configuration | The configuration cache is what keeps this from getting worse |

`:core:domain:test` alone — SPEC §6.5's remaining row — is **deliberately absent here**. There is
no `:core:domain` yet, so there is nothing to measure. It gets its first value at the PR 5 gate,
and its expectation ("should drop to seconds") stands unchanged.

## 5. The stopping rule, made unambiguous

> If incremental rebuild after touching a **leaf screen** has not improved by at least **40%**
> once Milestone 5 is half done, stop and reassess.

For the avoidance of any later argument about which number that means:

- The measurement is the row labelled **`inc_leaf_screen`** in §3.1 — **5.7s** — from the
  **configuration cache on** column, **median** of 5 runs, same protocol, same machine. Reproduce
  it with `python3 scripts/measure_build_baseline.py results.json inc_leaf_screen`.
- **Measure both sides in the same session.** §6 records this row moving by 37% between two runs
  of the identical protocol on effectively the same tree, hours apart. A "before" taken today and
  an "after" taken in three months is therefore not a valid comparison at the 40% threshold. At
  every phase gate, check out this baseline commit into a `git worktree`, measure it and the gate
  commit **back to back in one sitting**, and compare those two. It costs ten minutes and it is
  the difference between a decision and a coin toss.
- The threshold is against the **configuration-cache-on** figure recorded in §3, not against the
  `--no-configuration-cache` one. Enabling the cache is this PR's win, not the split's, and
  crediting it to the split would be double counting.
- "Milestone 5 half done" means PRs 13–16 of the stack merged (four of the eight feature
  modules).
- The **whitespace** row is the one the rule is read from, because that is what SPEC §6.5 says.
  Report the ABI-changing row alongside it at every gate. If the split helps the ABI-changing
  edit substantially and the whitespace edit not at all, that is a result worth arguing about
  rather than a failure — but it has to be argued *then*, in the open, not by quietly switching
  which row the 40% is measured on.

## 6. Honest limitations

- **The `inc_leaf_screen` row is not stable across sessions, and it is the row the stopping rule
  is read from.** It was measured twice under the identical protocol on this machine, on trees
  differing only by comment and doc edits and by `project.findProperty` → `providers.gradleProperty`
  in three places — none of which touches compilation. The cache-on median came out **9.0s** the
  first time and **5.7s** the second, hours apart: a 37% swing, which is almost exactly the size
  of the 40% threshold the epic's stopping rule uses. The other six rows agreed between the two
  sessions to within a few percent (`inc_leaf_screen_abi` 10.4 → 9.1, `inc_nimaz_button` 9.1 →
  8.9, `inc_domain_model` 5.3 → 5.4, `inc_strings_xml` 1.0 → 1.1, `testDebugUnitTest` 50.5 →
  49.6), so this is specific to that one scenario rather than general machine drift, and the cause
  was not identified — most likely accumulated Kotlin incremental-compilation or build-cache state
  that `:app:clean` does not reset. **The figure in §3.1 is the second, lower one**, because it is
  the one the committed driver reproduces on the committed tree. The consequence is in §5: at a
  phase gate, measure the baseline commit and the gate commit in the *same session*, and compare
  those. Do not compare a fresh number against the one printed here.
- **SPEC §6.5's "`:app:compileDebugKotlin` = 3m 44s cold" has no recorded protocol** — no
  branch, no daemon state, no `clean` vs `--rerun-tasks`, and no statement of whether the
  content-artifact cache was warm. It is not invented, but it is not reproducible as stated,
  which is precisely the gap this document exists to close. **It should not be compared against
  anything here.** Treat §3 as the first real baseline.
- **One machine, one sitting.** These are not CI numbers. CI is a colder machine with a colder
  Gradle cache and a cold `nimaz-data` fetch; the ratios should carry over, the absolute values
  will not.
- **`gradle-profiler` was not used** (see §2).
- **The `strings.xml` row under-measures a real string change.** A whitespace-only edit inside
  `<resources>` is normalised away by AAPT2, so the resource merge re-runs and produces
  *identical* output, and everything downstream stays up to date. 1.0s is therefore the cost of
  invalidating the resource merge, not the cost of editing a string. It is still a valid
  before/after pair — the after measurement will be taken exactly the same way — but do not read
  it as "changing a string costs a second".
- **The `testDebugUnitTest` row is `--rerun` on the test task only**, on an already-compiled
  tree and a warm daemon: ~50s. The **3m 17s** recorded on the epic base `7950101` is a different
  measurement under an unrecorded protocol — almost certainly including compilation. The two are
  **not comparable**, and neither is wrong; only the 50s figure has a protocol attached, so only
  it is a baseline. Compare future runs against 50s and against nothing else.
- **`:app:testDebugUnitTest` = 3m 17s** was measured on the epic base `7950101` before this
  branch existed, under an unrecorded protocol. It is kept here only as a sanity check against
  §3's figure, not as a baseline in its own right.
