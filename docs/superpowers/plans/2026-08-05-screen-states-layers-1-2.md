# Screen States — Layers 1 & 2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Land the state contract (`UiError`, the evaluation-order rule, a ratchet test) and prove it on the three About/Licenses screens, which get their first ViewModels and a full clean-architecture slice.

**Architecture:** Layer 1 adds no behaviour — it adds `UiError`, commits `NimazErrorState`, writes the convention test with today's violations seeded as an accepted backlog, and documents the rule. Layer 2 replaces `remember { mutableStateOf }` + `Libs.Builder()` in three composables with `OpenSourceLibrary` → `LibraryRepository` → `LicensesUseCases` → `LicensesViewModel`, so the `aboutlibraries` types stop at the data layer.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, kotlinx.coroutines, JUnit4 + Truth + MockK + `kotlinx-coroutines-test`, `com.mikepenz:aboutlibraries`.

## Global Constraints

- Spec: `docs/superpowers/specs/2026-08-05-screen-state-migration-design.md`. Read it before Task 1.
- Domain never imports `data`; presentation never imports entities/DAOs. (`CLAUDE.md` rule 1)
- ViewModels inject `XxxUseCases`, never repositories or DAOs. (rule 2)
- ViewModels expose `StateFlow<XxxUiState>` + a single `onEvent(event)`. No exposed `MutableStateFlow`. (rule 3)
- Repositories return domain models; map at the data layer. (rule 4)
- DI in `core/di`: `@Binds` interface→impl, `@Provides` for `XxxUseCases`, `@Singleton` in `SingletonComponent`. (rule 5)
- No hardcoded `Color(0xFF…)`; interactive UI comes from the design system. (rules 7, 8)
- User-facing error copy is a `@StringRes`. Exception text goes in `UiError.details`, never in `message`.
- Commits carry **no** `Co-Authored-By` trailer.
- Branch layout: `epic/screen-states` (trunk, cut from `origin/dev`) → `epic/ss-01-contract` → `epic/ss-02-about-licenses`, delivered with `gh stack`. `gh auth switch --hostname github.com --user arshad-shah` before any write; restore to `ShahA_hmh` afterwards.
- Verify before finishing any task: `./gradlew :app:compileDebugKotlin`, `./gradlew :app:testDebugUnitTest`, `python3 scripts/check_docs.py`.

## File Structure

**Layer 1**

| File | Responsibility |
|---|---|
| `presentation/components/atoms/NimazErrorState.kt` | the error component (already written, uncommitted) |
| `presentation/viewmodel/UiError.kt` | the error value every failing `UiState` carries |
| `test/…/presentation/viewmodel/UiErrorTest.kt` | pins the kind→tone/glyph mapping |
| `test/…/presentation/screens/ScreenStateConventionTest.kt` | the ratchet, with its accepted backlog |
| `docs/ARCHITECTURE.md` | §8 rule, §9 accepted pattern |
| `docs/CLEAN_ARCHITECTURE_CHECKLIST.md` | the new AP entry + detection commands |

**Layer 2**

| File | Responsibility |
|---|---|
| `domain/model/OpenSourceLibrary.kt` | the domain shape of a dependency's licence record |
| `domain/repository/LibraryRepository.kt` | the interface presentation depends on |
| `data/repository/LibraryRepositoryImpl.kt` | `Libs` → domain, on IO. The only file importing `aboutlibraries` |
| `domain/usecase/licenses/LicensesUseCases.kt` | `GetLibrariesUseCase`, `GetLibraryUseCase`, bundled |
| `core/di/LicensesModule.kt` | `@Binds` repository, `@Provides` use-case bundle |
| `presentation/viewmodel/about/LicensesUiState.kt` | list + detail states |
| `presentation/viewmodel/about/LicensesViewModel.kt` | owns both, `onEvent` |
| `presentation/screens/about/LicensesScreen.kt` | list, on the four-state rule |
| `presentation/screens/about/LicenseDetailScreen.kt` | detail, on the four-state rule |
| `core/navigation/NavGraph.kt` | passes `hiltViewModel()` into both |

---

# Layer 1 — `epic/ss-01-contract`

### Task 1: `UiError`

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/UiError.kt`
- Create: `app/src/test/java/com/arshadshah/nimaz/presentation/viewmodel/UiErrorTest.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `NimazErrorKind` from `presentation.components.atoms` (exists; enum with `internal val icon: ImageVector`, `internal val tone: NimazTone`).
- Produces: `UiError(@StringRes message: Int, kind: NimazErrorKind = GENERIC, details: String? = null)`. Every later task's `UiState` carries `error: UiError?`.

- [ ] **Step 1: Write the failing test**

`app/src/test/java/com/arshadshah/nimaz/presentation/viewmodel/UiErrorTest.kt`:

```kotlin
package com.arshadshah.nimaz.presentation.viewmodel

import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazErrorKind
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The contract a failing UiState carries. Two properties matter enough to pin:
 * the copy is a resource id (so it is translated), and the exception text is kept
 * apart from it (so it is never what the user reads).
 */
class UiErrorTest {

    @Test
    fun `defaults to a generic failure with no technical detail`() {
        val error = UiError(R.string.error_generic)

        assertThat(error.kind).isEqualTo(NimazErrorKind.GENERIC)
        assertThat(error.details).isNull()
    }

    @Test
    fun `an offline failure is a warning, not an alarm`() {
        // A dropped connection is expected and recoverable; rendering it in error-red
        // overstates it. The kind, not the call site, decides that.
        assertThat(NimazErrorKind.OFFLINE.tone).isEqualTo(NimazTone.WARNING)
        assertThat(NimazErrorKind.GENERIC.tone).isEqualTo(NimazTone.ERROR)
        assertThat(NimazErrorKind.NOT_FOUND.tone).isEqualTo(NimazTone.MUTED)
    }

    @Test
    fun `technical detail is carried separately from the readable message`() {
        val error = UiError(
            message = R.string.error_generic,
            kind = NimazErrorKind.SERVER,
            details = "java.net.SocketTimeoutException: timeout",
        )

        assertThat(error.message).isEqualTo(R.string.error_generic)
        assertThat(error.details).isEqualTo("java.net.SocketTimeoutException: timeout")
    }
}
```

- [ ] **Step 2: Run it and confirm it fails**

Run: `./gradlew :app:testDebugUnitTest --tests '*UiErrorTest*'`
Expected: FAIL — `Unresolved reference: UiError`.

- [ ] **Step 3: Write the implementation**

`app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/UiError.kt`:

```kotlin
package com.arshadshah.nimaz.presentation.viewmodel

import androidx.annotation.StringRes
import com.arshadshah.nimaz.presentation.components.atoms.NimazErrorKind

/**
 * A failure a screen can render — the value every failing `UiState` carries.
 *
 * [message] is a resource id and never a string, because the alternative is what the
 * app ships today: `"Failed to search locations: ${e.message}"` reaching a user's
 * screen in English, exception-shaped, in a build that otherwise translates
 * everything. The exception's own text belongs in [details], which
 * `NimazErrorState` keeps behind a "Show details" toggle — reachable for a bug
 * report, never the first thing read.
 *
 * The ViewModel picks the [kind] because it is the layer that knows *what* failed:
 * a network fetch, a missing row, a denied permission. The kind then fixes the glyph
 * and tone, so the same failure looks the same wherever it surfaces.
 */
data class UiError(
    @StringRes val message: Int,
    val kind: NimazErrorKind = NimazErrorKind.GENERIC,
    val details: String? = null,
)
```

- [ ] **Step 4: Confirm `R.string.error_generic` exists**

Run: `grep -n 'name="error_generic"' app/src/main/res/values/strings.xml`
Expected: one hit. If absent, add `<string name="error_generic">Something went wrong</string>` to `strings.xml`.

- [ ] **Step 5: Run the test again**

Run: `./gradlew :app:testDebugUnitTest --tests '*UiErrorTest*'`
Expected: PASS, 3 tests.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/components/atoms/NimazErrorState.kt \
        app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/UiError.kt \
        app/src/test/java/com/arshadshah/nimaz/presentation/viewmodel/UiErrorTest.kt
git commit -m "feat(design-system): an error state, and the value a UiState carries to fill it

NimazErrorState is the third of the three screen states, built to mirror
NimazLoadingState's FULLSCREEN/SECTION/INLINE variants so a screen can swap
loading for error without changing layout. UiError is what a failing UiState
holds: a @StringRes so the copy is translated, the exception text kept apart
in details where the component hides it behind a toggle."
```

### Task 2: The convention ratchet

**Files:**
- Create: `app/src/test/java/com/arshadshah/nimaz/presentation/screens/ScreenStateConventionTest.kt`

**Interfaces:**
- Consumes: nothing. Reads the source tree from the module root, exactly as `AnalyticsReachabilityTest` does (`File("src/main/java/…")` — relative to `app/`, which is the unit-test working directory).
- Produces: three `accepted` backlog sets that layers 2–6 empty. Later layers **delete entries**; they never add one.

- [ ] **Step 1: Write the test with empty backlogs**

`app/src/test/java/com/arshadshah/nimaz/presentation/screens/ScreenStateConventionTest.kt`:

```kotlin
package com.arshadshah.nimaz.presentation.screens

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * The three screen states are only consistent if nothing quietly re-rolls them.
 *
 * Source-scanning rather than reflection, for the same reason as
 * `AnalyticsReachabilityTest`: the questions here are facts about the source tree —
 * does a screen spin its own spinner, does anything read this error field, does this
 * failure path record anything for the user to see — and no test that merely *runs*
 * the code can answer them.
 *
 * Each backlog below is seeded with exactly the violations that existed when the
 * screen-state epic began, and shrinks as its layers land. Entries are only ever
 * removed. When a set empties it becomes a pure ratchet: the next regression fails
 * the PR that introduces it.
 */
class ScreenStateConventionTest {

    private val screensDir = File("src/main/java/com/arshadshah/nimaz/presentation/screens")
    private val viewModelDir = File("src/main/java/com/arshadshah/nimaz/presentation/viewmodel")

    /** Screens that still centre their own `CircularProgressIndicator`. Emptied by layer 5. */
    private val acceptedSpinners = setOf<String>(
        // SEEDED IN STEP 3
    )

    /** `UiState` files whose `error` field no screen reads. Emptied by layers 2-4 and 6. */
    private val acceptedUnreadErrors = setOf<String>(
        // SEEDED IN STEP 3
    )

    /** ViewModels with a `launchSafely` that records nothing for the user. Emptied by layers 3-4. */
    private val acceptedSilentFailures = setOf<String>(
        // SEEDED IN STEP 3
    )

    @Test
    fun `no screen rolls its own loading spinner`() {
        val offenders = screensDir.walkTopDown()
            .filter { it.extension == "kt" }
            .filter { file ->
                file.readText()
                    .lineSequence()
                    .filterNot { it.trimStart().startsWith("//") }
                    .any { "CircularProgressIndicator(" in it }
            }
            .map { it.name }
            .toSet()

        // Determinate LinearProgressIndicator is deliberately not checked: a bar that
        // reports how far along a known-length operation is (widget pin, sync, the
        // fasting day) is not a loading state, and NimazLoadingState cannot express it.
        assertThat(offenders - acceptedSpinners).isEmpty()
        assertThat(acceptedSpinners - offenders).isEmpty()
    }

    @Test
    fun `every UiState error field has a screen that reads it`() {
        val screenSource = screensDir.walkTopDown()
            .filter { it.extension == "kt" }
            .joinToString("\n") { it.readText() }

        val offenders = viewModelDir.walkTopDown()
            .filter { it.name.endsWith("UiState.kt") }
            .filter { "val error" in it.readText() || "val errorRes" in it.readText() }
            .filterNot { file ->
                // A feature reads its error if any screen mentions `.error` at all —
                // state.error, uiState.error, phase.error.
                val feature = file.name.removeSuffix("UiState.kt")
                screenSource.contains(".error", ignoreCase = false) &&
                    screenReadsFeatureError(feature, screensDir)
            }
            .map { it.name }
            .toSet()

        assertThat(offenders - acceptedUnreadErrors).isEmpty()
        assertThat(acceptedUnreadErrors - offenders).isEmpty()
    }

    @Test
    fun `every launchSafely records the failure for the user`() {
        val offenders = viewModelDir.walkTopDown()
            .filter { it.name.endsWith("ViewModel.kt") }
            .filter { file ->
                val text = file.readText()
                val launches = Regex("launchSafely\\(").findAll(text).count()
                val handled = Regex("onFailure\\s*=").findAll(text).count()
                launches > handled
            }
            .map { it.name }
            .toSet()

        assertThat(offenders - acceptedSilentFailures).isEmpty()
        assertThat(acceptedSilentFailures - offenders).isEmpty()
    }

    /** True when some screen in the feature's package reads an error off its state. */
    private fun screenReadsFeatureError(feature: String, dir: File): Boolean =
        dir.walkTopDown()
            .filter { it.extension == "kt" }
            .any { it.readText().contains(Regex("""(state|uiState|phase)\.error""")) }
}
```

- [ ] **Step 2: Run it and watch it fail with the real inventory**

Run: `./gradlew :app:testDebugUnitTest --tests '*ScreenStateConventionTest*'`
Expected: FAIL, three times, each listing the file names to seed. Copy the reported sets verbatim — do **not** hand-write them from the spec, because the spec's counts were taken before this branch changed two files.

- [ ] **Step 3: Seed the three backlogs from that output**

Paste each reported set into its `setOf(…)`, one entry per line, alphabetically, each a bare file name (`"HadithReaderScreen.kt"`). Add a one-line comment above any entry whose reason is not obvious.

- [ ] **Step 4: Run again — green**

Run: `./gradlew :app:testDebugUnitTest --tests '*ScreenStateConventionTest*'`
Expected: PASS, 3 tests. The backlog now equals reality exactly; both directions of each assertion prove it.

- [ ] **Step 5: Commit**

```bash
git add app/src/test/java/com/arshadshah/nimaz/presentation/screens/ScreenStateConventionTest.kt
git commit -m "test(screens): a ratchet for the three screen states

Asks three questions no running test can: does a screen spin its own spinner,
does anything read this error field, does this failure path record anything a
user could see. Each backlog is seeded with exactly today's violations and only
ever shrinks; when one empties, the next regression fails its own PR."
```

### Task 3: Document the rule

**Files:**
- Modify: `docs/ARCHITECTURE.md` (§8 component bullets — beside the existing `NimazLoadingState` bullet added on this branch)
- Modify: `docs/CLEAN_ARCHITECTURE_CHECKLIST.md` (new AP entry)

- [ ] **Step 1: Add the evaluation-order rule to `ARCHITECTURE.md` §8**

Directly after the existing `NimazErrorState` bullet, add:

```markdown
    - **the four states are evaluated in one fixed order**, in every screen:
      `isLoading && empty` → `NimazLoadingState`; `error != null` → `NimazErrorState`;
      `empty` → `NimazEmptyState`; else content. Error **beats** empty, so a failed load
      can never be reported as "there is nothing here" (three Qur'an screens did exactly
      that); loading only wins when the screen is bare, so a failed *refresh* never blanks
      out content the user is reading — that is a `SECTION`/`INLINE` error or a
      `NimazBanner`. All three take the scaffold's `paddingValues`: they fill and centre,
      so omitting it centres them against the window and tucks them under the top bar.
      A failing `UiState` carries `error: UiError?` (`presentation/viewmodel/UiError.kt`),
      never a raw `String` — the copy is a `@StringRes` and the exception text goes in
      `details`. `ScreenStateConventionTest` holds the line.
```

- [ ] **Step 2: Add the checklist entry**

Append to `docs/CLEAN_ARCHITECTURE_CHECKLIST.md` a new `### AP-7.12 · Screen states improvised per screen` section with an unticked box, the three defects from spec §0, and the detection commands:

````markdown
### AP-7.12 · Screen states improvised per screen

- [ ] **25 hand-rolled spinners, 9 hand-rolled error blocks, 8 error fields nothing reads,
  and 24 `launchSafely` sites that record nothing for the user.** Tracked by
  `ScreenStateConventionTest`, whose three accepted backlogs shrink layer by layer;
  see `docs/superpowers/specs/2026-08-05-screen-state-migration-design.md`.

```bash
grep -rn --include='*.kt' 'CircularProgressIndicator(' app/src/main/java/com/arshadshah/nimaz/presentation/screens
grep -rln --include='*.kt' 'val error' app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel | sort
rg -n 'launchSafely\(' app/src/main/java --glob '*ViewModel.kt' -A3 | rg -v 'onFailure'
```
````

- [ ] **Step 3: Verify the docs still describe the code**

Run: `python3 scripts/check_docs.py`
Expected: `All 23 documentation checks passed.`

- [ ] **Step 4: Commit**

```bash
git add docs/ARCHITECTURE.md docs/CLEAN_ARCHITECTURE_CHECKLIST.md
git commit -m "docs(architecture): the order the four screen states are evaluated in

Error beats empty, and loading only wins on a bare screen. Both are stated as a
rule because both were violated: three Quran screens render a failed load as an
empty one, and a failed refresh elsewhere blanks out content the user is reading."
```

### Task 4: Open the stack

- [ ] **Step 1: Verify the layer is green end to end**

Run, in order:
```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
python3 scripts/check_docs.py
```
Expected: all three succeed. Do not proceed past a failure.

- [ ] **Step 2: Move the layer's commits onto its own branch**

The three commits above were made on `epic/screen-states`. Move them:

```bash
git branch epic/ss-01-contract
git reset --hard HEAD~3          # trunk keeps only the spec commit
git checkout epic/ss-01-contract
```

Confirm with `git log --oneline epic/screen-states` (one commit: the spec) and
`git log --oneline epic/ss-01-contract` (four commits).

- [ ] **Step 3: Initialise the stack**

```bash
gh auth switch --hostname github.com --user arshad-shah
gh stack init --base epic/screen-states epic/ss-01-contract
gh stack submit
gh stack view
```

- [ ] **Step 4: Restore the default account**

```bash
gh auth switch --hostname github.com --user ShahA_hmh
```

---

# Layer 2 — `epic/ss-02-about-licenses`

Branch: `gh stack add epic/ss-02-about-licenses` (from `epic/ss-01-contract`).

### Task 5: The domain model and its mapper

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/domain/model/OpenSourceLibrary.kt`
- Create: `app/src/main/java/com/arshadshah/nimaz/domain/repository/LibraryRepository.kt`
- Create: `app/src/main/java/com/arshadshah/nimaz/data/repository/LibraryRepositoryImpl.kt`
- Create: `app/src/test/java/com/arshadshah/nimaz/data/repository/LibraryMappingTest.kt`

**Interfaces:**
- Produces:
  - `OpenSourceLibrary(id: Int, name: String, version: String?, author: String?, website: String?, licenses: List<LibraryLicense>)`
  - `LibraryLicense(name: String, url: String?, content: String?)`
  - `interface LibraryRepository { suspend fun getLibraries(): List<OpenSourceLibrary>; suspend fun getLibrary(id: Int): OpenSourceLibrary? }`
  - `internal fun Library.toDomain(): OpenSourceLibrary` in `LibraryRepositoryImpl.kt`
- Consumes: `com.mikepenz.aboutlibraries.entity.Library` — fields used are `name`, `artifactVersion`, `developers`, `website`, `licenses` (each with `name`, `url`, `licenseContent`), `uniqueId`.

- [ ] **Step 1: Write the failing mapper test**

`app/src/test/java/com/arshadshah/nimaz/data/repository/LibraryMappingTest.kt`:

```kotlin
package com.arshadshah.nimaz.data.repository

import com.google.common.truth.Truth.assertThat
import com.mikepenz.aboutlibraries.entity.Developer
import com.mikepenz.aboutlibraries.entity.License
import com.mikepenz.aboutlibraries.entity.Library
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Test

/**
 * The identity the licence detail route travels on.
 *
 * It used to be `library.hashCode()` — the hash of the whole object, so a version bump
 * or a re-ordered developer list silently changed the id of the same library. The
 * domain id is derived from `uniqueId` (the Maven coordinate) instead, which is what
 * actually identifies a dependency.
 */
class LibraryMappingTest {

    private fun library(
        uniqueId: String = "androidx.compose.ui:ui",
        version: String? = "1.7.0",
    ) = Library(
        uniqueId = uniqueId,
        artifactVersion = version,
        name = "Compose UI",
        description = null,
        website = "https://developer.android.com",
        developers = persistentSetOf(Developer("Google", null)).toList(),
        organization = null,
        scm = null,
        licenses = persistentSetOf(
            License("Apache-2.0", "https://apache.org/licenses/LICENSE-2.0", null, null, "Apache text"),
        ),
        funding = persistentSetOf(),
        tag = null,
    )

    @Test
    fun `identity comes from the coordinate, not the whole object`() {
        val a = library(version = "1.7.0").toDomain()
        val b = library(version = "1.8.0").toDomain()

        assertThat(a.id).isEqualTo(b.id)
        assertThat(a.id).isEqualTo("androidx.compose.ui:ui".hashCode())
    }

    @Test
    fun `the fields the screens render survive the mapping`() {
        val mapped = library().toDomain()

        assertThat(mapped.name).isEqualTo("Compose UI")
        assertThat(mapped.version).isEqualTo("1.7.0")
        assertThat(mapped.author).isEqualTo("Google")
        assertThat(mapped.website).isEqualTo("https://developer.android.com")
        assertThat(mapped.licenses).hasSize(1)
        assertThat(mapped.licenses.first().name).isEqualTo("Apache-2.0")
        assertThat(mapped.licenses.first().content).isEqualTo("Apache text")
    }
}
```

> **Before writing the implementation:** the `Library`/`License` constructors above are
> written against the version in `gradle/libs.versions.toml`. Confirm the parameter names
> with `./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep aboutlibraries`
> and the decompiled entity, and adjust the test's constructor call — not the assertions —
> if they differ.

- [ ] **Step 2: Run it and confirm it fails**

Run: `./gradlew :app:testDebugUnitTest --tests '*LibraryMappingTest*'`
Expected: FAIL — `Unresolved reference: toDomain`.

- [ ] **Step 3: Write the domain model**

`app/src/main/java/com/arshadshah/nimaz/domain/model/OpenSourceLibrary.kt`:

```kotlin
package com.arshadshah.nimaz.domain.model

/**
 * A dependency's licence record, as the About screens need it.
 *
 * [id] is derived from the Maven coordinate rather than the source object's hash, so a
 * version bump does not change which library a saved detail route points at.
 */
data class OpenSourceLibrary(
    val id: Int,
    val name: String,
    val version: String?,
    val author: String?,
    val website: String?,
    val licenses: List<LibraryLicense>,
)

data class LibraryLicense(
    val name: String,
    val url: String?,
    val content: String?,
)
```

- [ ] **Step 4: Write the repository interface**

`app/src/main/java/com/arshadshah/nimaz/domain/repository/LibraryRepository.kt`:

```kotlin
package com.arshadshah.nimaz.domain.repository

import com.arshadshah.nimaz.domain.model.OpenSourceLibrary

/** The open-source dependencies the app ships, and their licences. */
interface LibraryRepository {
    suspend fun getLibraries(): List<OpenSourceLibrary>
    suspend fun getLibrary(id: Int): OpenSourceLibrary?
}
```

- [ ] **Step 5: Write the implementation and the mapper**

`app/src/main/java/com/arshadshah/nimaz/data/repository/LibraryRepositoryImpl.kt`:

```kotlin
package com.arshadshah.nimaz.data.repository

import android.content.Context
import com.arshadshah.nimaz.domain.model.LibraryLicense
import com.arshadshah.nimaz.domain.model.OpenSourceLibrary
import com.arshadshah.nimaz.domain.repository.LibraryRepository
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.util.withContext
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext as onDispatcher
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only file in the app that imports `aboutlibraries`.
 *
 * Building `Libs` parses a generated asset, which is why both About screens used to do
 * it inside a `LaunchedEffect` on an IO dispatcher. It happens here instead, once, and
 * what leaves is a domain model.
 */
@Singleton
class LibraryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : LibraryRepository {

    override suspend fun getLibraries(): List<OpenSourceLibrary> = onDispatcher(Dispatchers.IO) {
        Libs.Builder().withContext(context).build()
            .libraries
            .map { it.toDomain() }
            .sortedBy { it.name.lowercase() }
    }

    override suspend fun getLibrary(id: Int): OpenSourceLibrary? =
        getLibraries().firstOrNull { it.id == id }
}

internal fun Library.toDomain(): OpenSourceLibrary = OpenSourceLibrary(
    id = uniqueId.hashCode(),
    name = name,
    version = artifactVersion,
    author = developers.firstOrNull()?.name,
    website = website,
    licenses = licenses.map { LibraryLicense(it.name, it.url, it.licenseContent) },
)
```

No caching: `getLibrary` re-reads the list, which is a bundled asset parse behind a
`suspend` on IO, called at most twice per visit to the About area. A `by lazy` over a
suspend-produced list cannot be written correctly, and a `Mutex`-guarded cache is more
machinery than two screens justify.

- [ ] **Step 6: Run the test**

Run: `./gradlew :app:testDebugUnitTest --tests '*LibraryMappingTest*'`
Expected: PASS, 2 tests.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/domain/model/OpenSourceLibrary.kt \
        app/src/main/java/com/arshadshah/nimaz/domain/repository/LibraryRepository.kt \
        app/src/main/java/com/arshadshah/nimaz/data/repository/LibraryRepositoryImpl.kt \
        app/src/test/java/com/arshadshah/nimaz/data/repository/LibraryMappingTest.kt
git commit -m "feat(about): licences as a domain model, behind a repository

Two composables were building Libs from a LaunchedEffect and holding the
aboutlibraries entity in remember. The parse moves to the data layer, and the
detail route's identity moves from the object's hash to the Maven coordinate,
so a version bump no longer renames the library it points at."
```

### Task 6: Use cases and DI

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/domain/usecase/licenses/LicensesUseCases.kt`
- Create: `app/src/main/java/com/arshadshah/nimaz/core/di/LicensesModule.kt`

**Interfaces:**
- Produces: `LicensesUseCases(getLibraries: GetLibrariesUseCase, getLibrary: GetLibraryUseCase)`, each with `suspend operator fun invoke(...)`. `LicensesViewModel` in Task 7 injects this and nothing else.

- [ ] **Step 1: Write the use cases**

```kotlin
package com.arshadshah.nimaz.domain.usecase.licenses

import com.arshadshah.nimaz.domain.model.OpenSourceLibrary
import com.arshadshah.nimaz.domain.repository.LibraryRepository

data class LicensesUseCases(
    val getLibraries: GetLibrariesUseCase,
    val getLibrary: GetLibraryUseCase,
)

class GetLibrariesUseCase(private val repo: LibraryRepository) {
    suspend operator fun invoke(): List<OpenSourceLibrary> = repo.getLibraries()
}

class GetLibraryUseCase(private val repo: LibraryRepository) {
    suspend operator fun invoke(id: Int): OpenSourceLibrary? = repo.getLibrary(id)
}
```

- [ ] **Step 2: Write the module**

```kotlin
package com.arshadshah.nimaz.core.di

import com.arshadshah.nimaz.data.repository.LibraryRepositoryImpl
import com.arshadshah.nimaz.domain.repository.LibraryRepository
import com.arshadshah.nimaz.domain.usecase.licenses.GetLibrariesUseCase
import com.arshadshah.nimaz.domain.usecase.licenses.GetLibraryUseCase
import com.arshadshah.nimaz.domain.usecase.licenses.LicensesUseCases
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LicensesModule {

    @Binds
    @Singleton
    abstract fun bindLibraryRepository(impl: LibraryRepositoryImpl): LibraryRepository

    companion object {
        @Provides
        @Singleton
        fun provideLicensesUseCases(repo: LibraryRepository): LicensesUseCases = LicensesUseCases(
            getLibraries = GetLibrariesUseCase(repo),
            getLibrary = GetLibraryUseCase(repo),
        )
    }
}
```

- [ ] **Step 3: Let KSP validate the graph**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. A Hilt error here means the binding is wrong — read the KSP message, do not proceed.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/domain/usecase/licenses/LicensesUseCases.kt \
        app/src/main/java/com/arshadshah/nimaz/core/di/LicensesModule.kt
git commit -m "feat(about): licences use cases and their DI module"
```

### Task 7: `LicensesViewModel`

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/about/LicensesUiState.kt`
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/about/LicensesViewModel.kt`
- Create: `app/src/test/java/com/arshadshah/nimaz/presentation/viewmodel/about/LicensesViewModelTest.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `LicensesUseCases` (Task 6), `UiError` (Task 1), `Telemetry` + `launchSafely` from `core.monitoring`.
- Produces:
  - `LicensesListUiState(libraries: List<OpenSourceLibrary> = emptyList(), isLoading: Boolean = true, error: UiError? = null)`
  - `LicenseDetailUiState(library: OpenSourceLibrary? = null, isLoading: Boolean = true, error: UiError? = null)`
  - `LicensesViewModel.listState`, `.detailState`, `.onEvent(LicensesEvent)`
  - `sealed interface LicensesEvent { data object LoadLibraries; data class LoadLibrary(val id: Int); data object Retry; data object DismissError }`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.arshadshah.nimaz.presentation.viewmodel.about

import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.OpenSourceLibrary
import com.arshadshah.nimaz.domain.usecase.licenses.LicensesUseCases
import com.arshadshah.nimaz.presentation.components.atoms.NimazErrorKind
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * The three screens this ViewModel serves had no ViewModel at all: a `LaunchedEffect`
 * built `Libs` and a `remember { mutableStateOf }` held the result, so a parse failure
 * left `isLoading` true forever and nothing was testable. These are the cases that
 * could not previously be written.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LicensesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val telemetry = RecordingTelemetry()
    private lateinit var useCases: LicensesUseCases

    private val compose = OpenSourceLibrary(
        id = 1, name = "Compose UI", version = "1.7.0",
        author = "Google", website = null, licenses = emptyList(),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        useCases = mockk(relaxed = true)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `a loaded list clears loading and carries no error`() = runTest(dispatcher) {
        coEvery { useCases.getLibraries() } returns listOf(compose)

        val viewModel = LicensesViewModel(useCases, telemetry)
        viewModel.onEvent(LicensesEvent.LoadLibraries)
        advanceUntilIdle()

        assertThat(viewModel.listState.value.libraries).containsExactly(compose)
        assertThat(viewModel.listState.value.isLoading).isFalse()
        assertThat(viewModel.listState.value.error).isNull()
    }

    @Test
    fun `a failed load stops loading and says so`() = runTest(dispatcher) {
        coEvery { useCases.getLibraries() } throws IllegalStateException("asset missing")

        val viewModel = LicensesViewModel(useCases, telemetry)
        viewModel.onEvent(LicensesEvent.LoadLibraries)
        advanceUntilIdle()

        val state = viewModel.listState.value
        // The defect this pins: the old screen left the spinner running forever.
        assertThat(state.isLoading).isFalse()
        assertThat(state.error?.message).isEqualTo(R.string.licenses_load_failed)
        assertThat(state.error?.details).isEqualTo("asset missing")
        assertThat(telemetry.failures).hasSize(1)
    }

    @Test
    fun `retry clears the error and asks again`() = runTest(dispatcher) {
        coEvery { useCases.getLibraries() } throws IllegalStateException("asset missing")
        val viewModel = LicensesViewModel(useCases, telemetry)
        viewModel.onEvent(LicensesEvent.LoadLibraries)
        advanceUntilIdle()

        coEvery { useCases.getLibraries() } returns listOf(compose)
        viewModel.onEvent(LicensesEvent.Retry)
        advanceUntilIdle()

        assertThat(viewModel.listState.value.error).isNull()
        assertThat(viewModel.listState.value.libraries).containsExactly(compose)
    }

    @Test
    fun `a library the id does not match is not found, not a crash`() = runTest(dispatcher) {
        coEvery { useCases.getLibrary(any()) } returns null

        val viewModel = LicensesViewModel(useCases, telemetry)
        viewModel.onEvent(LicensesEvent.LoadLibrary(404))
        advanceUntilIdle()

        val state = viewModel.detailState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.library).isNull()
        assertThat(state.error?.kind).isEqualTo(NimazErrorKind.NOT_FOUND)
    }
}
```

- [ ] **Step 2: Confirm `RecordingTelemetry` exposes `failures`**

Run: `grep -n 'class RecordingTelemetry' -A 20 app/src/test/java/com/arshadshah/nimaz/core/monitoring/RecordingTelemetry.kt`
Adjust the assertion in the last-but-one test to whatever the recorder actually exposes.

- [ ] **Step 3: Run the test and confirm it fails**

Run: `./gradlew :app:testDebugUnitTest --tests '*LicensesViewModelTest*'`
Expected: FAIL — `Unresolved reference: LicensesViewModel`.

- [ ] **Step 4: Add the strings**

In `app/src/main/res/values/strings.xml`:

```xml
<string name="licenses_load_failed">The licence list couldn\'t be loaded</string>
<string name="licenses_load_failed_body">This list is built from the app\'s own bundled data, so retrying usually works.</string>
<string name="license_detail_not_found_body">This library isn\'t in the bundled licence list. It may have been removed in a newer version of the app.</string>
```

- [ ] **Step 5: Write the state**

```kotlin
package com.arshadshah.nimaz.presentation.viewmodel.about

import com.arshadshah.nimaz.domain.model.OpenSourceLibrary
import com.arshadshah.nimaz.presentation.viewmodel.UiError

data class LicensesListUiState(
    val libraries: List<OpenSourceLibrary> = emptyList(),
    val isLoading: Boolean = true,
    val error: UiError? = null,
)

data class LicenseDetailUiState(
    val library: OpenSourceLibrary? = null,
    val isLoading: Boolean = true,
    val error: UiError? = null,
)

sealed interface LicensesEvent {
    data object LoadLibraries : LicensesEvent
    data class LoadLibrary(val id: Int) : LicensesEvent
    data object Retry : LicensesEvent
    data object DismissError : LicensesEvent
}
```

- [ ] **Step 6: Write the ViewModel**

```kotlin
package com.arshadshah.nimaz.presentation.viewmodel.about

import androidx.lifecycle.ViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.launchSafely
import com.arshadshah.nimaz.domain.usecase.licenses.LicensesUseCases
import com.arshadshah.nimaz.presentation.components.atoms.NimazErrorKind
import com.arshadshah.nimaz.presentation.viewmodel.UiError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Two states for one feature — the list and the detail — which is the house pattern for
 * a list/detail pair (see `AsmaUlHusnaViewModel`), not a state that should be merged.
 */
@HiltViewModel
class LicensesViewModel @Inject constructor(
    private val useCases: LicensesUseCases,
    private val telemetry: Telemetry,
) : ViewModel() {

    private val _listState = MutableStateFlow(LicensesListUiState())
    val listState: StateFlow<LicensesListUiState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(LicenseDetailUiState())
    val detailState: StateFlow<LicenseDetailUiState> = _detailState.asStateFlow()

    fun onEvent(event: LicensesEvent) {
        when (event) {
            LicensesEvent.LoadLibraries -> loadLibraries()
            is LicensesEvent.LoadLibrary -> loadLibrary(event.id)
            // Retry serves the list only. The detail screen's failure is NOT_FOUND —
            // a library that is not in the bundled list will not be there next time
            // either, so offering "try again" there would be a lie.
            LicensesEvent.Retry -> loadLibraries()
            LicensesEvent.DismissError -> {
                _listState.update { it.copy(error = null) }
                _detailState.update { it.copy(error = null) }
            }
        }
    }

    private fun loadLibraries() {
        _listState.update { it.copy(isLoading = true, error = null) }
        launchSafely(
            telemetry, DOMAIN, "load_libraries",
            onFailure = { throwable ->
                _listState.update {
                    it.copy(
                        isLoading = false,
                        error = UiError(
                            message = R.string.licenses_load_failed,
                            kind = NimazErrorKind.GENERIC,
                            details = throwable.message,
                        ),
                    )
                }
            },
        ) {
            val libraries = useCases.getLibraries()
            _listState.update { it.copy(libraries = libraries, isLoading = false) }
        }
    }

    private fun loadLibrary(id: Int) {
        _detailState.update { it.copy(isLoading = true, error = null) }
        launchSafely(
            telemetry, DOMAIN, "load_library",
            onFailure = { throwable ->
                _detailState.update {
                    it.copy(
                        isLoading = false,
                        error = UiError(R.string.licenses_load_failed, NimazErrorKind.GENERIC, throwable.message),
                    )
                }
            },
        ) {
            val library = useCases.getLibrary(id)
            _detailState.update {
                it.copy(
                    library = library,
                    isLoading = false,
                    // A missing id is a real answer, not a thrown failure — the old screen
                    // reported it by leaving `library` null and setting a red string.
                    error = if (library == null) {
                        UiError(R.string.license_detail_library_not_found, NimazErrorKind.NOT_FOUND)
                    } else null,
                )
            }
        }
    }

    private companion object {
        const val DOMAIN = "about"
    }
}
```

- [ ] **Step 7: Run the tests**

Run: `./gradlew :app:testDebugUnitTest --tests '*LicensesViewModelTest*'`
Expected: PASS, 4 tests.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/about/ \
        app/src/test/java/com/arshadshah/nimaz/presentation/viewmodel/about/ \
        app/src/main/res/values/strings.xml
git commit -m "feat(about): a ViewModel for the licence screens

The list and detail states are separate because the screens are, and a failed
parse now ends with isLoading false and a UiError rather than a spinner that
never stops. Four cases that could not be written before this existed."
```

### Task 8: `LicensesScreen` on the rule

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/about/LicensesScreen.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/core/navigation/NavGraph.kt:1145-1152`
- Modify: `app/src/test/java/com/arshadshah/nimaz/presentation/screens/ScreenStateConventionTest.kt`

- [ ] **Step 1: Replace the screen's state ownership**

Delete the `LocalContext`, `remember`, `LaunchedEffect` and `Libs` imports and their
bodies (`LicensesScreen.kt:53-63`). The signature becomes:

```kotlin
@Composable
fun LicensesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    viewModel: LicensesViewModel = hiltViewModel(),
) {
    val state by viewModel.listState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onEvent(LicensesEvent.LoadLibraries) }
```

- [ ] **Step 2: Apply the four-state rule to the body**

Inside `NimazScreenScaffold`'s content lambda, replacing the current `if (isLoading)`:

```kotlin
    ) { paddingValues ->
        // Bound to a local so the null check smart-casts — `state` is a delegated
        // property, so `state.error` does not, and `!!` at every use is not the answer.
        val error = state.error
        when {
            state.isLoading && state.libraries.isEmpty() ->
                NimazLoadingState(modifier = Modifier.padding(paddingValues))

            error != null -> NimazErrorState(
                title = stringResource(R.string.licenses_load_failed),
                message = stringResource(R.string.licenses_load_failed_body),
                kind = error.kind,
                details = error.details,
                primaryAction = NimazErrorDefaults.retry(
                    onRetry = { viewModel.onEvent(LicensesEvent.Retry) },
                    label = stringResource(R.string.try_again),
                ),
                modifier = Modifier.padding(paddingValues),
            )

            else -> LazyColumn(
                // …unchanged body, iterating state.libraries…
            )
        }
    }
```

Note the row's click becomes `onClick = { onNavigateToDetail(library.id) }` — the domain
model's stable id, not `library.hashCode()`.

- [ ] **Step 3: Pass the ViewModel in `NavGraph`**

`taggedComposable<Route.Licenses>` needs no change if `hiltViewModel()` is the default
argument. Confirm the file still compiles and that `LicensesScreen(` is called with only
its two navigation lambdas.

- [ ] **Step 4: Remove `LicensesScreen.kt` from the spinner backlog**

In `ScreenStateConventionTest.acceptedSpinners`, delete the `"LicensesScreen.kt"` entry.

- [ ] **Step 5: Verify**

Run:
```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest --tests '*ScreenStateConventionTest*'
```
Expected: both pass. If the second fails saying `acceptedSpinners - offenders` is not
empty, the entry was removed but the file still has a spinner — find it.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/screens/about/LicensesScreen.kt \
        app/src/test/java/com/arshadshah/nimaz/presentation/screens/ScreenStateConventionTest.kt
git commit -m "refactor(about): the licence list asks a ViewModel, and can fail out loud"
```

### Task 9: `LicenseDetailScreen` on the rule

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/about/LicenseDetailScreen.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/core/navigation/NavGraph.kt:1154-1160`
- Modify: `app/src/test/java/com/arshadshah/nimaz/presentation/screens/ScreenStateConventionTest.kt`

- [ ] **Step 1: Replace state ownership**

```kotlin
@Composable
fun LicenseDetailScreen(
    libraryId: Int,
    onNavigateBack: () -> Unit,
    viewModel: LicensesViewModel = hiltViewModel(),
) {
    val state by viewModel.detailState.collectAsStateWithLifecycle()

    LaunchedEffect(libraryId) { viewModel.onEvent(LicensesEvent.LoadLibrary(libraryId)) }
```

The parameter is renamed from `libraryHashCode` to `libraryId`; `Route.LicenseDetail`
keeps its `libraryHashCode: Int` field name for now (renaming it is a `NAVIGATION.md`
change this layer does not need), so `NavGraph` passes
`libraryId = args.libraryHashCode`.

- [ ] **Step 2: Apply the four-state rule**

```kotlin
    ) { paddingValues ->
        val error = state.error
        val library = state.library
        when {
            state.isLoading -> NimazLoadingState(modifier = Modifier.padding(paddingValues))

            error != null -> NimazErrorState(
                title = stringResource(error.message),
                message = stringResource(R.string.license_detail_not_found_body),
                kind = error.kind,
                details = error.details,
                secondaryAction = NimazErrorAction(
                    label = stringResource(R.string.back),
                    onClick = onNavigateBack,
                ),
                modifier = Modifier.padding(paddingValues),
            )

            library != null -> LibraryDetailContent(
                library = library,
                modifier = Modifier.padding(paddingValues),
            )
        }
    }
```

`LibraryDetailContent`'s parameter type changes from `Library` to `OpenSourceLibrary`;
inside it, `library.developers.firstOrNull()?.name` becomes `library.author`,
`library.licenses.forEach { license -> license.licenseContent }` becomes
`license.content`, and `library.artifactVersion` becomes `library.version`.

- [ ] **Step 3: Confirm `R.string.back` exists**

Run: `grep -n 'name="back"' app/src/main/res/values/strings.xml`
If absent, use `R.string.close`, or add `<string name="back">Back</string>`.

- [ ] **Step 4: Remove `LicenseDetailScreen.kt` from both backlogs**

Delete its entries from `acceptedSpinners` (and from `acceptedUnreadErrors` if the
seeding put it there).

- [ ] **Step 5: Verify the whole layer**

Run:
```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
python3 scripts/check_docs.py
```
Expected: all pass.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/screens/about/LicenseDetailScreen.kt \
        app/src/main/java/com/arshadshah/nimaz/core/navigation/NavGraph.kt \
        app/src/test/java/com/arshadshah/nimaz/presentation/screens/ScreenStateConventionTest.kt
git commit -m "refactor(about): the licence detail asks a ViewModel, and says when a library is missing"
```

### Task 10: `AboutScreen` and the update-state mapper

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/about/UpdatePrompt.kt`
- Create: `app/src/test/java/com/arshadshah/nimaz/presentation/viewmodel/about/UpdatePromptTest.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/about/AboutScreen.kt:84-97,340-413`
- Modify: `docs/ARCHITECTURE.md` §9

**Interfaces:**
- Produces: `fun updatePrompt(state: UpdateState): UpdatePrompt` and
  `data class UpdatePrompt(@StringRes val label: Int, val icon: ImageVector, val isActionable: Boolean, val isError: Boolean)`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.arshadshah.nimaz.presentation.viewmodel.about

import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.update.UpdateState
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The About screen decided all of this inside a composable, in an inline `when` that
 * also performed the click. Pulling the decision out is what makes it assertable —
 * and what stops "check for updates" and "restart to install" sharing a code path.
 */
class UpdatePromptTest {

    @Test
    fun `a failed check reads as an error and is still actionable`() {
        val prompt = updatePrompt(UpdateState.Error)

        assertThat(prompt.label).isEqualTo(R.string.update_check_failed)
        assertThat(prompt.isError).isTrue()
        // Retrying the check is the whole point of tapping it.
        assertThat(prompt.isActionable).isTrue()
    }

    @Test
    fun `an in-flight download is not tappable`() {
        assertThat(updatePrompt(UpdateState.Checking).isActionable).isFalse()
    }
}
```

Adjust `UpdateState`'s member names to the real sealed hierarchy —
`grep -n 'sealed .* UpdateState' -A 15 app/src/main/java/com/arshadshah/nimaz/core/update/*.kt`.

- [ ] **Step 2: Run it and confirm it fails**

Run: `./gradlew :app:testDebugUnitTest --tests '*UpdatePromptTest*'`
Expected: FAIL — `Unresolved reference: updatePrompt`.

- [ ] **Step 3: Move the `when` out of the composable**

Write `UpdatePrompt.kt` holding the mapping currently inlined at `AboutScreen.kt:340-413`
(label, icon, `isError`, `isActionable`), then make `AboutScreen` read
`val prompt = updatePrompt(updateState)` and render from it.

- [ ] **Step 4: Run the tests**

Run: `./gradlew :app:testDebugUnitTest --tests '*UpdatePromptTest*'`
Expected: PASS, 2 tests.

- [ ] **Step 5: Register the accepted pattern**

Add to `docs/ARCHITECTURE.md` §9's *Accepted patterns* list:

```markdown
> - **`LocalInAppUpdateManager` stays a CompositionLocal** — the Play in-app update flow
>   needs an `Activity` to start, so routing it through a ViewModel would put an Activity
>   reference in one, which is worse than the coupling it removes. The *decision* the
>   About screen makes from `UpdateState` is not exempt: it lives in `updatePrompt()`
>   (`presentation/viewmodel/about/UpdatePrompt.kt`) and is unit-tested. Only the manager
>   handle itself is reached through the composition.
```

- [ ] **Step 6: Verify and commit**

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
python3 scripts/check_docs.py
git add app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/about/UpdatePrompt.kt \
        app/src/test/java/com/arshadshah/nimaz/presentation/viewmodel/about/UpdatePromptTest.kt \
        app/src/main/java/com/arshadshah/nimaz/presentation/screens/about/AboutScreen.kt \
        docs/ARCHITECTURE.md
git commit -m "refactor(about): the update decision leaves the composable

Which label, which icon, whether a tap does anything — all of it was an inline
when inside AboutScreen that also performed the click, so none of it could be
asserted. The manager handle stays a CompositionLocal: the Play flow needs an
Activity, and a ViewModel is the wrong place to hold one."
```

- [ ] **Step 7: Submit the layer**

```bash
gh auth switch --hostname github.com --user arshad-shah
gh stack submit
gh stack view
gh auth switch --hostname github.com --user ShahA_hmh
```

---

## Layers 3–6

Each gets its own plan document, written when its layer starts, so the per-screen code in
it is written against the tree as it actually is by then:

- `docs/superpowers/plans/…-screen-states-layer-3-silent-failures.md` — Hadith ×3, Help ×3,
  Bookmarks, Tafseer, SurahThematic ×3, Search, Home, Zakat
- `…-layer-4-handrolled-errors.md` — Sync, Qibla, Dua ×4, Calendar, NightWorship
- `…-layer-5-loading-empty.md` — the remaining 23 `CircularProgressIndicator` sites
- `…-layer-6-vestigial.md` — vestigial fields, empty backlogs, checklist ticked

Every one of them ends the same way: entries deleted from `ScreenStateConventionTest`'s
backlogs, never added.
