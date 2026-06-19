# Qaida Reader UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the child-facing Qaida Reader UI (course map, lesson reader, letter explorer, celebration) binding to the existing `QaidaReaderViewModel`.

**Architecture:** Atoms/molecules/organisms in `presentation/components/*` (flat, `Qaida`-prefixed) compose into three screens under `presentation/screens/qaida/`. Each screen obtains the existing `QaidaReaderViewModel` via `hiltViewModel()` and observes Room-backed `StateFlow`s. The course map's winding trail is a custom `Layout` + `Canvas`. Three flat `@Serializable` routes; entry via the More menu.

**Tech Stack:** Kotlin, Jetpack Compose, Material3, Hilt, kotlinx.serialization navigation, Robolectric + Compose UI test (`createComponentComposeRule`, `setThemedContent`).

## Global Constraints

- Coverage gate: every new file in `presentation.components.atoms`/`.molecules`/`.organisms` needs **≥90% instruction coverage** (JaCoCo). Put Qaida components **flat** in those packages with a `Qaida` prefix — NOT in a subpackage (subpackages escape the `PACKAGE`-element rule).
- Screens live in `presentation/screens/qaida/`; screens are NOT under the coverage gate.
- Tests: `@RunWith(RobolectricTestRunner::class)`, `@get:Rule val composeRule = createComponentComposeRule()`, render via `composeRule.setThemedContent { … }`. Test files go in `app/src/testDebug/java/com/arshadshah/nimaz/...` mirroring the source package.
- Arabic text uses the existing `ArabicText` atom (`com.arshadshah.nimaz.presentation.components.atoms.ArabicText`) + `AmiriFontFamily`; never raw `Text` for Arabic glyphs.
- Brand colors via `MaterialTheme.colorScheme` where possible: primary = teal, secondary = gold. Parchment surface is Qaida-specific — define once (Task 2) and reuse.
- All interactive elements ≥ 56dp touch target; meaningful `contentDescription`s; respect existing `LocalHapticState`/animations settings (use them if present, else no-op).
- Do NOT modify the data/audio/progress layers or `QaidaReaderViewModel` unless a task explicitly says so.
- Commit after every task with a `feat(qaida):` / `test(qaida):` message.

### Reference: existing types this plan consumes (do not redefine)

```kotlin
// domain/model/QaidaModels.kt
data class QaidaLessonState(val lesson: QaidaLesson, val status: LessonStatus, val stars: Int,
    val completedCells: Int, val totalCells: Int, val completionFraction: Float, val lastCellId: Int?)
data class QaidaCourseProgress(val lessons: List<QaidaLessonState>, val completedLessons: Int,
    val totalLessons: Int, val totalStars: Int, val maxStars: Int, val totalCellsHeard: Int,
    val overallFraction: Float, val nextLessonId: Int?)
data class QaidaLesson(val id: Int, val lessonNumber: Int, val titleEnglish: String,
    val titleArabic: String, val titleTransliteration: String, val description: String,
    val conceptTags: List<String>, val icon: String, val displayOrder: Int)
data class QaidaCell(val id: Int, val lineId: Int, val lessonId: Int, val position: Int,
    val textArabic: String, val transliteration: String, val tokenType: TokenType,
    val audioKey: String, val audioPath: String, val highlightGroup: String?,
    val letterId: Int?, val notes: String?)
data class QaidaLine(val id: Int, val lessonId: Int, val lineNumber: Int, val lineType: LineType,
    val instructionEnglish: String?, val instructionArabic: String?, val displayOrder: Int)
data class QaidaLineContent(val line: QaidaLine, val cells: List<QaidaCell>)
data class QaidaLessonContent(val lesson: QaidaLesson, val lines: List<QaidaLineContent>)
data class QaidaLetter(val id: Int, val letterArabic: String, val nameArabic: String,
    val nameTransliteration: String, val isolatedForm: String, val initialForm: String?,
    val medialForm: String?, val finalForm: String?, val isConnecting: Boolean,
    val makhrajArea: MakhrajArea, val makhrajDetail: String, val phoneticHint: String?,
    val audioKey: String, val audioPath: String, val displayOrder: Int)
data class QaidaLessonState // (above)
enum class LessonStatus { LOCKED, UNLOCKED, IN_PROGRESS, COMPLETED }
enum class MakhrajArea { JAWF, HALQ, LISAN, SHAFATAIN, KHAYSHUM }

// data/audio/QaidaAudioManager.kt
data class QaidaAudioState(val currentKey: String?, val isPlaying: Boolean, val isLoading: Boolean, val error: String?)

// presentation/viewmodel/QaidaReaderViewModel.kt  (@HiltViewModel)
val selectedLessonId: StateFlow<Int?>
val courseProgress: StateFlow<QaidaCourseProgress?>
val letters: StateFlow<List<QaidaLetter>>
val lessonContent: StateFlow<QaidaLessonContent?>
val lessonProgress: StateFlow<QaidaLessonState?>
val audioState: StateFlow<QaidaAudioState>
val playingCell: StateFlow<QaidaCell?>
fun selectLesson(lessonId: Int); fun onCellTapped(cell: QaidaCell); fun playLine(lineId: Int)
fun nextLesson(); fun previousLesson(); fun resume()

// presentation/components/atoms/ArabicText.kt
@Composable fun ArabicText(text: String, modifier: Modifier = Modifier,
    size: ArabicTextSize = ArabicTextSize.MEDIUM, color: Color = …, fontWeight: FontWeight = …,
    textAlign: TextAlign = TextAlign.Center, maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip, style: TextStyle? = null)
enum class ArabicTextSize { SMALL, MEDIUM, LARGE, EXTRA_LARGE, QURAN }
```

> **Execution note:** Some exact names in the surrounding code (e.g. the `Route` sealed interface members, `NimazMenuItem` parameter names, `createComponentComposeRule`/`setThemedContent` import paths, haptic CompositionLocal name) must be confirmed by opening the referenced file before writing each task's code. Each navigation/entry task lists the file to open first. Component tasks are self-contained and do not depend on those.

---

## File Structure

**Atoms** (`app/src/main/java/com/arshadshah/nimaz/presentation/components/atoms/`):
- `QaidaTheme.kt` — Qaida color tokens (parchment, illuminated border) + `QaidaMedallionState` enum.
- `QaidaStarRow.kt` — row of filled/empty stars.
- `QaidaMedallion.kt` — circular lesson node (done/current/locked).
- `HarakatArabicText.kt` — Arabic with harakat tinted per `highlightGroup`.

**Molecules** (`…/components/molecules/`):
- `QaidaCellTile.kt`, `QaidaPlayLineButton.kt`, `QaidaLineProgressDots.kt`,
  `QaidaCourseHeader.kt`, `QaidaLetterTile.kt`, `QaidaLetterForms.kt`, `QaidaMakhrajHelper.kt`.

**Organisms** (`…/components/organisms/`):
- `QaidaCoursePath.kt`, `QaidaLessonLines.kt`, `QaidaLetterBoard.kt`,
  `QaidaLetterDetailSheet.kt`, `QaidaCelebrationOverlay.kt`.

**Screens** (`…/screens/qaida/`):
- `QaidaHomeScreen.kt`, `QaidaReaderScreen.kt`, `QaidaLettersScreen.kt`.

**Navigation** (modify): `core/navigation/Routes.kt`, `core/navigation/NavGraph.kt`,
`presentation/screens/more/MoreMenuScreen.kt`.

---

## Task 1: Navigation scaffolding + entry point

Make the three Qaida screens reachable with placeholder bodies, so navigation is testable before any UI exists.

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/core/navigation/Routes.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/core/navigation/NavGraph.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/more/MoreMenuScreen.kt`
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/qaida/QaidaHomeScreen.kt`
- Create: `…/screens/qaida/QaidaReaderScreen.kt`
- Create: `…/screens/qaida/QaidaLettersScreen.kt`

**Interfaces:**
- Produces: `Route.QaidaHome`, `Route.QaidaReader(lessonId: Int)`, `Route.QaidaLetters`;
  composables `QaidaHomeScreen(onNavigateBack, onOpenLesson: (Int)->Unit, onOpenLetters: ()->Unit)`,
  `QaidaReaderScreen(lessonId: Int, onNavigateBack: ()->Unit)`,
  `QaidaLettersScreen(onNavigateBack: ()->Unit)`.

- [ ] **Step 1: Open the files to confirm conventions.** Read `Routes.kt` (sealed interface name + `@Serializable` import), `NavGraph.kt` (one `composable<Route.X>` block + how callbacks navigate), and `MoreMenuScreen.kt` (the exact `NimazMenuItem` signature + how callbacks are threaded in). Note the real names; the snippets below assume `sealed interface Route` and `NimazMenuItem(title, subtitle, icon, onClick)`.

- [ ] **Step 2: Add routes.** In `Routes.kt`, alongside the existing routes:

```kotlin
@Serializable data object QaidaHome : Route
@Serializable data class QaidaReader(val lessonId: Int) : Route
@Serializable data object QaidaLetters : Route
```

- [ ] **Step 3: Add placeholder screens.** Create the three screen files. Each is a `Scaffold` with a top app bar and a centered placeholder so navigation is verifiable:

```kotlin
package com.arshadshah.nimaz.presentation.screens.qaida

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QaidaHomeScreen(
    onNavigateBack: () -> Unit,
    onOpenLesson: (Int) -> Unit,
    onOpenLetters: () -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Qaida") }) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text("Qaida course map")
        }
    }
}
```

Create `QaidaReaderScreen(lessonId: Int, onNavigateBack: () -> Unit)` and
`QaidaLettersScreen(onNavigateBack: () -> Unit)` the same way (placeholder text
`"Lesson $lessonId"` and `"Letters"` respectively).

- [ ] **Step 4: Register routes in `NavGraph.kt`** (mirror the existing `composable<Route.QuranReader>` style):

```kotlin
composable<Route.QaidaHome> {
    QaidaHomeScreen(
        onNavigateBack = { navController.popBackStack() },
        onOpenLesson = { lessonId -> navController.navigate(Route.QaidaReader(lessonId)) },
        onOpenLetters = { navController.navigate(Route.QaidaLetters) },
    )
}
composable<Route.QaidaReader> { backStackEntry ->
    val args = backStackEntry.toRoute<Route.QaidaReader>()
    QaidaReaderScreen(lessonId = args.lessonId, onNavigateBack = { navController.popBackStack() })
}
composable<Route.QaidaLetters> {
    QaidaLettersScreen(onNavigateBack = { navController.popBackStack() })
}
```

- [ ] **Step 5: Add the More-menu entry.** In `MoreMenuScreen.kt`, add a `NimazMenuItem` (use a books/school icon, e.g. `Icons.Default.MenuBook`) wired to a new `onNavigateToQaida: () -> Unit` parameter; thread that callback from where `MoreMenuScreen` is hosted in `NavGraph.kt` to `navController.navigate(Route.QaidaHome)`.

- [ ] **Step 6: Build to verify it compiles.**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Manually verify navigation** (emulator or note for reviewer): More → Qaida → placeholder map; tapping through reaches reader/letters placeholders; back works.

- [ ] **Step 8: Commit.**

```bash
git add app/src/main/java/com/arshadshah/nimaz/core/navigation app/src/main/java/com/arshadshah/nimaz/presentation/screens/qaida app/src/main/java/com/arshadshah/nimaz/presentation/screens/more/MoreMenuScreen.kt
git commit -m "feat(qaida): navigation scaffolding + More-menu entry (#178)"
```

---

## Task 2: `QaidaTheme` tokens (atom)

Shared Qaida colors + the medallion state enum, reused by every component.

**Files:**
- Create: `…/components/atoms/QaidaTheme.kt`
- Test: `app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/atoms/QaidaThemeTest.kt`

**Interfaces:**
- Produces: `object QaidaColors { val Parchment: Color; val ParchmentDark: Color; val Ink: Color; val Gold: Color; val Teal: Color; val LockedFill: Color }`; `enum class QaidaMedallionState { DONE, CURRENT, LOCKED }`; `@Composable fun qaidaParchmentBrush(): Brush`.

- [ ] **Step 1: Write the failing test.**

```kotlin
package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class QaidaThemeTest {
    @Test fun `parchment palette has expected anchor colors`() {
        assertEquals(Color(0xFFFBF5E6), QaidaColors.Parchment)
        assertEquals(Color(0xFFF4E9CE), QaidaColors.ParchmentDark)
        assertEquals(Color(0xFFEAB308), QaidaColors.Gold)
    }
    @Test fun `medallion has three states`() {
        assertEquals(3, QaidaMedallionState.entries.size)
    }
}
```

- [ ] **Step 2: Run to verify it fails.** Run: `./gradlew :app:testDebugUnitTest --tests "*QaidaThemeTest*"` → FAIL (unresolved `QaidaColors`).

- [ ] **Step 3: Implement.**

```kotlin
package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object QaidaColors {
    val Parchment = Color(0xFFFBF5E6)
    val ParchmentDark = Color(0xFFF4E9CE)
    val Ink = Color(0xFF3A3327)
    val Gold = Color(0xFFEAB308)
    val Teal = Color(0xFF14B8A6)
    val TealDeep = Color(0xFF0D9488)
    val LockedFill = Color(0xFFE2D6BB)
    val LockedInk = Color(0xFFA89A78)
}

enum class QaidaMedallionState { DONE, CURRENT, LOCKED }

@Composable
fun qaidaParchmentBrush(): Brush =
    Brush.verticalGradient(listOf(QaidaColors.Parchment, QaidaColors.ParchmentDark))
```

- [ ] **Step 4: Run to verify it passes.** Run the same command → PASS.

- [ ] **Step 5: Commit.**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/components/atoms/QaidaTheme.kt app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/atoms/QaidaThemeTest.kt
git commit -m "feat(qaida): parchment/gold theme tokens + medallion state enum (#178)"
```

---

## Task 3: `QaidaStarRow` (atom)

**Files:**
- Create: `…/components/atoms/QaidaStarRow.kt`
- Test: `…/testDebug/.../atoms/QaidaStarRowTest.kt`

**Interfaces:**
- Produces: `@Composable fun QaidaStarRow(filled: Int, modifier: Modifier = Modifier, max: Int = 3, starSize: Dp = 16.dp, filledColor: Color = QaidaColors.Gold, emptyColor: Color = QaidaColors.LockedFill)`.

- [ ] **Step 1: Write the failing test.**

```kotlin
package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
import com.arshadshah.nimaz.testsupport.createComponentComposeRule
import com.arshadshah.nimaz.testsupport.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QaidaStarRowTest {
    @get:Rule val composeRule = createComponentComposeRule()

    @Test fun `renders max stars with content description for filled count`() {
        composeRule.setThemedContent { QaidaStarRow(filled = 2, max = 3) }
        composeRule.onAllNodesWithTag("qaida_star").assertCountEquals(3)
        composeRule.onNodeWithContentDescription("2 of 3 stars").assertExists()
    }

    @Test fun `zero filled still renders the row`() {
        composeRule.setThemedContent { QaidaStarRow(filled = 0, max = 3) }
        composeRule.onAllNodesWithTag("qaida_star").assertCountEquals(3)
    }
}
```

> Confirm the exact import paths for `createComponentComposeRule`/`setThemedContent` from an existing atom test (e.g. `NimazBadgeTest.kt`) and match them; the package above is a placeholder.

- [ ] **Step 2: Run to verify it fails.** Run: `./gradlew :app:testDebugUnitTest --tests "*QaidaStarRowTest*"` → FAIL.

- [ ] **Step 3: Implement.**

```kotlin
package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun QaidaStarRow(
    filled: Int,
    modifier: Modifier = Modifier,
    max: Int = 3,
    starSize: Dp = 16.dp,
    filledColor: Color = QaidaColors.Gold,
    emptyColor: Color = QaidaColors.LockedFill,
) {
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = "$filled of $max stars" },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        repeat(max) { i ->
            val isFilled = i < filled
            Icon(
                imageVector = if (isFilled) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = null,
                tint = if (isFilled) filledColor else emptyColor,
                modifier = Modifier.size(starSize).testTag("qaida_star"),
            )
        }
    }
}
```

(Add `import androidx.compose.foundation.layout.size`.)

- [ ] **Step 4: Run to verify it passes.** → PASS.

- [ ] **Step 5: Commit.** `git commit -m "feat(qaida): QaidaStarRow atom (#178)"`

---

## Task 4: `QaidaMedallion` (atom)

**Files:**
- Create: `…/components/atoms/QaidaMedallion.kt`
- Test: `…/testDebug/.../atoms/QaidaMedallionTest.kt`

**Interfaces:**
- Consumes: `QaidaColors`, `QaidaMedallionState` (Task 2).
- Produces: `@Composable fun QaidaMedallion(label: String, state: QaidaMedallionState, contentDescription: String, modifier: Modifier = Modifier, size: Dp = 64.dp, onClick: (() -> Unit)? = null)`.

- [ ] **Step 1: Write the failing test.**

```kotlin
@RunWith(RobolectricTestRunner::class)
class QaidaMedallionTest {
    @get:Rule val composeRule = createComponentComposeRule()

    @Test fun `done medallion shows label and fires click`() {
        var clicked = false
        composeRule.setThemedContent {
            QaidaMedallion("١", QaidaMedallionState.DONE, "Lesson 1, complete", onClick = { clicked = true })
        }
        composeRule.onNodeWithContentDescription("Lesson 1, complete").performClick()
        assertThat(clicked).isTrue()
    }

    @Test fun `locked medallion shows lock and does not fire click`() {
        var clicked = false
        composeRule.setThemedContent {
            QaidaMedallion("٥", QaidaMedallionState.LOCKED, "Lesson 5, locked", onClick = { clicked = true })
        }
        composeRule.onNodeWithContentDescription("Lesson 5, locked").performClick()
        assertThat(clicked).isFalse()
    }

    @Test fun `current medallion renders without onClick`() {
        composeRule.setThemedContent { QaidaMedallion("٤", QaidaMedallionState.CURRENT, "Lesson 4, current") }
        composeRule.onNodeWithContentDescription("Lesson 4, current").assertExists()
    }
}
```

- [ ] **Step 2: Run to verify it fails.** → FAIL.

- [ ] **Step 3: Implement.**

```kotlin
package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background

@Composable
fun QaidaMedallion(
    label: String,
    state: QaidaMedallionState,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    onClick: (() -> Unit)? = null,
) {
    val fill = when (state) {
        QaidaMedallionState.DONE -> QaidaColors.Gold
        QaidaMedallionState.CURRENT -> QaidaColors.Teal
        QaidaMedallionState.LOCKED -> QaidaColors.LockedFill
    }
    val clickable = state != QaidaMedallionState.LOCKED && onClick != null
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(fill)
            .then(if (clickable) Modifier.clickable { onClick!!() } else Modifier)
            .clearAndSetSemantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        if (state == QaidaMedallionState.LOCKED) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = QaidaColors.LockedInk)
        } else {
            ArabicText(text = label, size = ArabicTextSize.LARGE, color = Color.White)
        }
    }
}
```

- [ ] **Step 4: Run to verify it passes.** → PASS.

- [ ] **Step 5: Commit.** `git commit -m "feat(qaida): QaidaMedallion atom (#178)"`

---

## Task 5: `QaidaCourseHeader` (molecule)

Pinned map header: Arabic title, progress bar, "Lesson X of N", star count, Continue button.

**Files:**
- Create: `…/components/molecules/QaidaCourseHeader.kt`
- Test: `…/testDebug/.../molecules/QaidaCourseHeaderTest.kt`

**Interfaces:**
- Consumes: `ArabicText`, `QaidaColors`.
- Produces: `@Composable fun QaidaCourseHeader(titleArabic: String, lessonIndex: Int, totalLessons: Int, totalStars: Int, overallFraction: Float, continueLabel: String?, onContinue: () -> Unit, modifier: Modifier = Modifier)`. `continueLabel == null` hides the button.

- [ ] **Step 1: Write the failing test.**

```kotlin
@RunWith(RobolectricTestRunner::class)
class QaidaCourseHeaderTest {
    @get:Rule val composeRule = createComponentComposeRule()

    @Test fun `shows progress text and continue, fires callback`() {
        var cont = false
        composeRule.setThemedContent {
            QaidaCourseHeader("رحلتي", lessonIndex = 4, totalLessons = 17, totalStars = 9,
                overallFraction = 0.38f, continueLabel = "Fatha & Kasra", onContinue = { cont = true })
        }
        composeRule.onNodeWithText("Lesson 4 of 17").assertExists()
        composeRule.onNodeWithText("9").assertExists()
        composeRule.onNodeWithText("Fatha & Kasra", substring = true).performClick()
        assertThat(cont).isTrue()
    }

    @Test fun `hides continue when label null`() {
        composeRule.setThemedContent {
            QaidaCourseHeader("رحلتي", 1, 17, 0, 0f, continueLabel = null, onContinue = {})
        }
        composeRule.onNodeWithTag("qaida_continue").assertDoesNotExist()
    }
}
```

- [ ] **Step 2: Run to verify it fails.** → FAIL.

- [ ] **Step 3: Implement** a `Column` with: `ArabicText` title; a `LinearProgressIndicator(progress = { overallFraction })` tinted teal→gold track; a `Row` with `Text("Lesson $lessonIndex of $totalLessons")` and `Text("$totalStars")` next to a star `Icon`; and, when `continueLabel != null`, a `Button(onClick = onContinue, modifier = Modifier.testTag("qaida_continue"))` whose content is `Text("▶ Continue")` + `Text(continueLabel)`. Background `qaidaParchmentBrush()`. (Full code: follow the atom imports pattern; keep it a single `Column`.)

- [ ] **Step 4: Run to verify it passes.** → PASS.

- [ ] **Step 5: Commit.** `git commit -m "feat(qaida): QaidaCourseHeader molecule (#178)"`

---

## Task 6: `QaidaCoursePath` (organism) — the Canvas winding trail

**Files:**
- Create: `…/components/organisms/QaidaCoursePath.kt`
- Test: `…/testDebug/.../organisms/QaidaCoursePathTest.kt`

**Interfaces:**
- Consumes: `QaidaMedallion`, `QaidaMedallionState`, `QaidaStarRow`, `QaidaColors`, `QaidaLessonState`, `LessonStatus`.
- Produces: `@Composable fun QaidaCoursePath(lessons: List<QaidaLessonState>, currentLessonId: Int?, onLessonClick: (Int) -> Unit, modifier: Modifier = Modifier)`.

- [ ] **Step 1: Write the failing test.**

```kotlin
@RunWith(RobolectricTestRunner::class)
class QaidaCoursePathTest {
    @get:Rule val composeRule = createComponentComposeRule()

    private fun lessonState(id: Int, status: LessonStatus, stars: Int = 0) = QaidaLessonState(
        lesson = QaidaLesson(id, id, "Lesson $id", "درس", "dars", "", emptyList(), "", id),
        status = status, stars = stars, completedCells = 0, totalCells = 8,
        completionFraction = 0f, lastCellId = null)

    @Test fun `tapping an unlocked medallion invokes callback with its id`() {
        var clickedId: Int? = null
        val lessons = listOf(
            lessonState(1, LessonStatus.COMPLETED, 3),
            lessonState(2, LessonStatus.IN_PROGRESS),
            lessonState(3, LessonStatus.LOCKED),
        )
        composeRule.setThemedContent {
            QaidaCoursePath(lessons, currentLessonId = 2, onLessonClick = { clickedId = it })
        }
        composeRule.onNodeWithContentDescription("Lesson 1, complete", substring = true).performClick()
        assertThat(clickedId).isEqualTo(1)
    }

    @Test fun `locked medallion does not invoke callback`() {
        var clickedId: Int? = null
        composeRule.setThemedContent {
            QaidaCoursePath(listOf(lessonState(3, LessonStatus.LOCKED)), 2, onLessonClick = { clickedId = it })
        }
        composeRule.onNodeWithContentDescription("locked", substring = true).performClick()
        assertThat(clickedId).isNull()
    }
}
```

- [ ] **Step 2: Run to verify it fails.** → FAIL.

- [ ] **Step 3: Implement.** A vertically-scrolling custom `Layout` that places one node composable per lesson at a serpentine anchor, with the trail drawn behind via `Modifier.drawBehind`.

```kotlin
package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.LessonStatus
import com.arshadshah.nimaz.domain.model.QaidaLessonState
import com.arshadshah.nimaz.presentation.components.atoms.*

@Composable
fun QaidaCoursePath(
    lessons: List<QaidaLessonState>,
    currentLessonId: Int?,
    onLessonClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val rowHeight = 120.dp
    val medallion = 64.dp
    val currentIndex = lessons.indexOfFirst { it.lesson.id == currentLessonId }
        .let { if (it < 0) lessons.indexOfLast { l -> l.status != LessonStatus.LOCKED } else it }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .drawBehind {
                val anchorX = { i: Int -> if (i % 2 == 0) size.width * 0.28f else size.width * 0.72f }
                val anchorY = { i: Int -> with(density) { (rowHeight.toPx() * i) + rowHeight.toPx() / 2 } }
                fun trail(from: Int, to: Int): Path = Path().apply {
                    if (to <= from) return@apply
                    moveTo(anchorX(from), anchorY(from))
                    for (i in from until to) {
                        val midY = (anchorY(i) + anchorY(i + 1)) / 2
                        cubicTo(anchorX(i), midY, anchorX(i + 1), midY, anchorX(i + 1), anchorY(i + 1))
                    }
                }
                val w = with(density) { 10.dp.toPx() }
                if (currentIndex > 0) drawPath(trail(0, currentIndex),
                    brush = Brush.verticalGradient(listOf(QaidaColors.Gold, QaidaColors.Teal)),
                    style = Stroke(width = w, cap = StrokeCap.Round))
                if (currentIndex in 0 until lessons.lastIndex) drawPath(trail(currentIndex, lessons.lastIndex),
                    color = QaidaColors.LockedFill,
                    style = Stroke(width = w * 0.85f, cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 24f))))
            },
    ) {
        lessons.forEachIndexed { i, ls ->
            val state = when (ls.status) {
                LessonStatus.COMPLETED -> QaidaMedallionState.DONE
                LessonStatus.LOCKED -> QaidaMedallionState.LOCKED
                else -> QaidaMedallionState.CURRENT
            }
            val desc = "Lesson ${ls.lesson.lessonNumber}, " + when (state) {
                QaidaMedallionState.DONE -> "complete, ${ls.stars} of 3 stars"
                QaidaMedallionState.CURRENT -> "current"
                QaidaMedallionState.LOCKED -> "locked"
            }
            Row(
                Modifier.fillMaxWidth().height(rowHeight)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = if (i % 2 == 0) Arrangement.Start else Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    QaidaMedallion(
                        label = ls.lesson.lessonNumber.toString(),
                        state = state,
                        contentDescription = desc,
                        size = medallion,
                        onClick = { onLessonClick(ls.lesson.id) },
                    )
                    if (state == QaidaMedallionState.DONE) QaidaStarRow(filled = ls.stars)
                }
            }
        }
    }
}
```

(Add `import androidx.compose.ui.Alignment`. The serpentine left/right alignment plus the behind-drawn trail give the wave; refine anchor fractions during manual review.)

- [ ] **Step 4: Run to verify it passes.** → PASS.

- [ ] **Step 5: Commit.** `git commit -m "feat(qaida): QaidaCoursePath organism with Canvas trail (#178)"`

---

## Task 7: Wire `QaidaHomeScreen` to the ViewModel

**Files:**
- Modify: `…/screens/qaida/QaidaHomeScreen.kt`

**Interfaces:**
- Consumes: `QaidaReaderViewModel.courseProgress`, `resume()`; `QaidaCourseHeader`, `QaidaCoursePath`.

- [ ] **Step 1: Implement the binding.**

```kotlin
@Composable
fun QaidaHomeScreen(
    onNavigateBack: () -> Unit,
    onOpenLesson: (Int) -> Unit,
    onOpenLetters: () -> Unit,
    viewModel: QaidaReaderViewModel = hiltViewModel(),
) {
    val progress by viewModel.courseProgress.collectAsStateWithLifecycle()
    val cp = progress
    Column(Modifier.fillMaxSize().background(qaidaParchmentBrush())) {
        QaidaCourseHeader(
            titleArabic = "رِحْلَتِي مَعَ القاعدة",
            lessonIndex = cp?.completedLessons?.plus(1) ?: 1,
            totalLessons = cp?.totalLessons ?: 0,
            totalStars = cp?.totalStars ?: 0,
            overallFraction = cp?.overallFraction ?: 0f,
            continueLabel = cp?.nextLessonId?.let { id -> cp.lessons.firstOrNull { it.lesson.id == id }?.lesson?.titleEnglish },
            onContinue = { cp?.nextLessonId?.let(onOpenLesson) },
        )
        QaidaCoursePath(
            lessons = cp?.lessons.orEmpty(),
            currentLessonId = cp?.nextLessonId,
            onLessonClick = onOpenLesson,
            modifier = Modifier.weight(1f),
        )
    }
}
```

(Imports: `hiltViewModel`, `collectAsStateWithLifecycle`, `background`, `qaidaParchmentBrush`. Add an overflow/header action calling `onOpenLetters` for the Letter Explorer.)

- [ ] **Step 2: Build.** Run: `./gradlew :app:compileDebugKotlin` → SUCCESS.
- [ ] **Step 3: Manually verify** the map renders with real lessons, Continue resumes, taps open the reader placeholder.
- [ ] **Step 4: Commit.** `git commit -m "feat(qaida): wire course map screen to ViewModel (#178)"`

---

## Task 8: `HarakatArabicText` (atom)

Render Arabic where harakat marks are tinted per `highlightGroup`, with an emphasized "playing" state.

**Files:**
- Create: `…/components/atoms/HarakatArabicText.kt`
- Test: `…/testDebug/.../atoms/HarakatArabicTextTest.kt`

**Interfaces:**
- Produces: `@Composable fun HarakatArabicText(text: String, highlightGroup: String?, modifier: Modifier = Modifier, size: ArabicTextSize = ArabicTextSize.EXTRA_LARGE, playing: Boolean = false)`. Mapping: `highlightGroup` `"fatha"`→Teal, `"kasra"`→Gold, `"damma"`→Teal, else default; harakat Unicode marks (`ً`–`ْ`) get the tint, base letters stay ink. When `playing`, whole glyph uses white-on-teal emphasis (handled by caller's tile background; here just switch text color to white).

- [ ] **Step 1: Write the failing test.**

```kotlin
@RunWith(RobolectricTestRunner::class)
class HarakatArabicTextTest {
    @get:Rule val composeRule = createComponentComposeRule()

    @Test fun `renders the arabic text`() {
        composeRule.setThemedContent { HarakatArabicText("بَ", highlightGroup = "fatha") }
        composeRule.onNodeWithText("بَ").assertExists()
    }
    @Test fun `null highlight group still renders`() {
        composeRule.setThemedContent { HarakatArabicText("ب", highlightGroup = null) }
        composeRule.onNodeWithText("ب").assertExists()
    }
    @Test fun `playing state renders`() {
        composeRule.setThemedContent { HarakatArabicText("بِ", highlightGroup = "kasra", playing = true) }
        composeRule.onNodeWithText("بِ").assertExists()
    }
}
```

- [ ] **Step 2: Run to verify it fails.** → FAIL.

- [ ] **Step 3: Implement** using `buildAnnotatedString` + `BasicText`/`Text` with the Amiri family, walking characters and applying a `SpanStyle(color = tint)` to harakat code points:

```kotlin
package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

private val HARAKAT = 'ً'..'ْ'

@Composable
fun HarakatArabicText(
    text: String,
    highlightGroup: String?,
    modifier: Modifier = Modifier,
    size: ArabicTextSize = ArabicTextSize.EXTRA_LARGE,
    playing: Boolean = false,
) {
    val baseColor = if (playing) Color.White else QaidaColors.Ink
    val harakatColor = if (playing) Color.White else when (highlightGroup) {
        "kasra", "tanween_kasra" -> QaidaColors.Gold
        "fatha", "damma", "tanween_fatha", "tanween_damma" -> QaidaColors.TealDeep
        else -> baseColor
    }
    val annotated = buildAnnotatedString {
        text.forEach { c ->
            if (c in HARAKAT) withStyle(SpanStyle(color = harakatColor)) { append(c) }
            else withStyle(SpanStyle(color = baseColor)) { append(c) }
        }
    }
    Text(text = annotated, modifier = modifier,
        fontFamily = AmiriFontFamily, fontSize = size.fontSize, lineHeight = size.lineHeight,
        style = LocalTextStyle.current)
}
```

> `AmiriFontFamily` is in `presentation/theme/Type.kt`; `size.fontSize`/`size.lineHeight` come from the `ArabicTextSize` enum. Confirm both are accessible (they are `public`).

- [ ] **Step 4: Run to verify it passes.** → PASS.

- [ ] **Step 5: Commit.** `git commit -m "feat(qaida): HarakatArabicText atom with harakat tinting (#178)"`

---

## Task 9: `QaidaCellTile` (molecule)

**Files:**
- Create: `…/components/molecules/QaidaCellTile.kt`
- Test: `…/testDebug/.../molecules/QaidaCellTileTest.kt`

**Interfaces:**
- Consumes: `HarakatArabicText`, `QaidaCell`, `QaidaColors`.
- Produces: `@Composable fun QaidaCellTile(cell: QaidaCell, isPlaying: Boolean, showTransliteration: Boolean, onTap: (QaidaCell) -> Unit, modifier: Modifier = Modifier)`.

- [ ] **Step 1: Write the failing test.**

```kotlin
@RunWith(RobolectricTestRunner::class)
class QaidaCellTileTest {
    @get:Rule val composeRule = createComponentComposeRule()
    private fun cell() = QaidaCell(11, 1, 1, 0, "بَ", "ba", TokenType.SYLLABLE, "l1_ba", "", "fatha", null, null)

    @Test fun `tap fires onTap with the cell`() {
        var tapped: QaidaCell? = null
        composeRule.setThemedContent {
            QaidaCellTile(cell(), isPlaying = false, showTransliteration = true, onTap = { tapped = it })
        }
        composeRule.onNodeWithContentDescription("ba, tap to hear", substring = true).performClick()
        assertThat(tapped?.id).isEqualTo(11)
    }
    @Test fun `transliteration hidden when flag false`() {
        composeRule.setThemedContent {
            QaidaCellTile(cell(), isPlaying = false, showTransliteration = false, onTap = {})
        }
        composeRule.onNodeWithText("ba").assertDoesNotExist()
    }
    @Test fun `playing tile renders`() {
        composeRule.setThemedContent {
            QaidaCellTile(cell(), isPlaying = true, showTransliteration = true, onTap = {})
        }
        composeRule.onNodeWithText("بَ").assertExists()
    }
}
```

- [ ] **Step 2: Run to verify it fails.** → FAIL.

- [ ] **Step 3: Implement** a `Card`/`Box` ≥72dp with rounded corners; teal background + outline when `isPlaying`, else white on `QaidaColors`; `HarakatArabicText(cell.textArabic, cell.highlightGroup, playing = isPlaying)`; transliteration `Text(cell.transliteration)` below when `showTransliteration`; `Modifier.clickable { onTap(cell) }` with `semantics { contentDescription = "${cell.transliteration}, tap to hear" }`.

- [ ] **Step 4: Run to verify it passes.** → PASS.

- [ ] **Step 5: Commit.** `git commit -m "feat(qaida): QaidaCellTile molecule (#178)"`

---

## Task 10: `QaidaPlayLineButton` + `QaidaLineProgressDots` (molecules)

Two small molecules in one task (both trivial, tested together).

**Files:**
- Create: `…/components/molecules/QaidaPlayLineButton.kt`, `…/molecules/QaidaLineProgressDots.kt`
- Test: `…/testDebug/.../molecules/QaidaPlayLineButtonTest.kt`, `…/molecules/QaidaLineProgressDotsTest.kt`

**Interfaces:**
- Produces: `@Composable fun QaidaPlayLineButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true)`; `@Composable fun QaidaLineProgressDots(total: Int, completed: Int, modifier: Modifier = Modifier)`.

- [ ] **Step 1: Write failing tests.**

```kotlin
@RunWith(RobolectricTestRunner::class)
class QaidaPlayLineButtonTest {
    @get:Rule val composeRule = createComponentComposeRule()
    @Test fun `click fires`() {
        var c = false
        composeRule.setThemedContent { QaidaPlayLineButton(onClick = { c = true }) }
        composeRule.onNodeWithContentDescription("Play line").performClick()
        assertThat(c).isTrue()
    }
}
// QaidaLineProgressDotsTest: render total=4 completed=2, assert 4 nodes tagged "qaida_dot".
```

- [ ] **Step 2: Run to verify they fail.** → FAIL.
- [ ] **Step 3: Implement.** `QaidaPlayLineButton` = a small teal pill `Button` with a play icon + "Play line" (`contentDescription = "Play line"`). `QaidaLineProgressDots` = a `Row` of `total` `Box` dots (8dp) tagged `"qaida_dot"`, first `completed` tinted teal, rest muted.
- [ ] **Step 4: Run to verify they pass.** → PASS.
- [ ] **Step 5: Commit.** `git commit -m "feat(qaida): play-line button + line progress dots (#178)"`

---

## Task 11: `QaidaLessonLines` (organism)

Renders a `QaidaLessonContent` as lines of cell tiles, RTL, with per-line play.

**Files:**
- Create: `…/components/organisms/QaidaLessonLines.kt`
- Test: `…/testDebug/.../organisms/QaidaLessonLinesTest.kt`

**Interfaces:**
- Consumes: `QaidaCellTile`, `QaidaPlayLineButton`, `QaidaLessonContent`, `QaidaCell`, `QaidaLineContent`.
- Produces: `@Composable fun QaidaLessonLines(content: QaidaLessonContent, playingCellId: Int?, showTransliteration: Boolean, onCellTap: (QaidaCell) -> Unit, onPlayLine: (Int) -> Unit, modifier: Modifier = Modifier)`.

- [ ] **Step 1: Write the failing test.**

```kotlin
@RunWith(RobolectricTestRunner::class)
class QaidaLessonLinesTest {
    @get:Rule val composeRule = createComponentComposeRule()
    private fun content(): QaidaLessonContent {
        val cells = listOf(
            QaidaCell(1, 10, 1, 0, "بَ", "ba", TokenType.SYLLABLE, "k1", "", "fatha", null, null),
            QaidaCell(2, 10, 1, 1, "بِ", "bi", TokenType.SYLLABLE, "k2", "", "kasra", null, null))
        val line = QaidaLine(10, 1, 1, LineType.EXAMPLE, "Tap each", null, 0)
        return QaidaLessonContent(
            QaidaLesson(1, 1, "Fatha & Kasra", "الفتحة", "fatha", "", emptyList(), "", 1),
            listOf(QaidaLineContent(line, cells)))
    }
    @Test fun `renders cells and play line fires with line id`() {
        var line: Int? = null; var tapped: QaidaCell? = null
        composeRule.setThemedContent {
            QaidaLessonLines(content(), playingCellId = 1, showTransliteration = true,
                onCellTap = { tapped = it }, onPlayLine = { line = it })
        }
        composeRule.onNodeWithText("بَ").assertExists()
        composeRule.onAllNodesWithContentDescription("Play line")[0].performClick()
        assertThat(line).isEqualTo(10)
        composeRule.onNodeWithContentDescription("bi, tap to hear", substring = true).performClick()
        assertThat(tapped?.id).isEqualTo(2)
    }
}
```

- [ ] **Step 2: Run to verify it fails.** → FAIL.
- [ ] **Step 3: Implement** a `Column` (or `LazyColumn`) wrapped in `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)`; each `QaidaLineContent` = optional instruction `Text` + `QaidaPlayLineButton(onClick = { onPlayLine(lc.line.id) })` + a `FlowRow` of `QaidaCellTile(cell, isPlaying = cell.id == playingCellId, showTransliteration, onCellTap)`.
- [ ] **Step 4: Run to verify it passes.** → PASS.
- [ ] **Step 5: Commit.** `git commit -m "feat(qaida): QaidaLessonLines organism (#178)"`

---

## Task 12: Wire `QaidaReaderScreen`

**Files:**
- Modify: `…/screens/qaida/QaidaReaderScreen.kt`

**Interfaces:**
- Consumes: `QaidaReaderViewModel` (`lessonContent`, `playingCell`, `selectLesson`, `onCellTapped`, `playLine`); `QaidaLessonLines`, `QaidaLineProgressDots`.

- [ ] **Step 1: Implement.**

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QaidaReaderScreen(
    lessonId: Int,
    onNavigateBack: () -> Unit,
    viewModel: QaidaReaderViewModel = hiltViewModel(),
) {
    LaunchedEffect(lessonId) { viewModel.selectLesson(lessonId) }
    val content by viewModel.lessonContent.collectAsStateWithLifecycle()
    val playing by viewModel.playingCell.collectAsStateWithLifecycle()
    var showTranslit by rememberSaveable { mutableStateOf(true) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { content?.lesson?.let { Text(it.titleEnglish) } },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { showTranslit = !showTranslit }) {
                        Icon(if (showTranslit) Icons.Default.Subtitles else Icons.Default.SubtitlesOff,
                            contentDescription = "Toggle transliteration")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(qaidaParchmentBrush())) {
            content?.let { c ->
                QaidaLessonLines(
                    content = c,
                    playingCellId = playing?.id,
                    showTransliteration = showTranslit,
                    onCellTap = viewModel::onCellTapped,
                    onPlayLine = viewModel::playLine,
                )
            }
        }
    }
}
```

- [ ] **Step 2: Build.** `./gradlew :app:compileDebugKotlin` → SUCCESS.
- [ ] **Step 3: Manually verify** tapping a tile plays audio + highlights it; play-line works; toggle hides transliteration.
- [ ] **Step 4: Commit.** `git commit -m "feat(qaida): wire lesson reader screen (#178)"`

---

## Task 13: `QaidaCelebrationOverlay` (organism) + hook into reader

**Files:**
- Create: `…/components/organisms/QaidaCelebrationOverlay.kt`
- Test: `…/testDebug/.../organisms/QaidaCelebrationOverlayTest.kt`
- Modify: `…/screens/qaida/QaidaReaderScreen.kt`

**Interfaces:**
- Consumes: `QaidaStarRow`, `ArabicText`, `QaidaColors`.
- Produces: `@Composable fun QaidaCelebrationOverlay(visible: Boolean, stars: Int, lessonTitle: String, unlockedTitle: String?, onNext: () -> Unit, onMap: () -> Unit, modifier: Modifier = Modifier)`.

- [ ] **Step 1: Write the failing test.**

```kotlin
@RunWith(RobolectricTestRunner::class)
class QaidaCelebrationOverlayTest {
    @get:Rule val composeRule = createComponentComposeRule()
    @Test fun `shows when visible and next fires`() {
        var next = false
        composeRule.setThemedContent {
            QaidaCelebrationOverlay(true, stars = 2, lessonTitle = "Fatha & Kasra",
                unlockedTitle = "Sukoon", onNext = { next = true }, onMap = {})
        }
        composeRule.onNodeWithText("Sukoon", substring = true).assertExists()
        composeRule.onNodeWithText("Next", substring = true).performClick()
        assertThat(next).isTrue()
    }
    @Test fun `hidden when not visible`() {
        composeRule.setThemedContent {
            QaidaCelebrationOverlay(false, 2, "x", null, {}, {})
        }
        composeRule.onNodeWithText("Next lesson", substring = true).assertDoesNotExist()
    }
}
```

- [ ] **Step 2: Run to verify it fails.** → FAIL.
- [ ] **Step 3: Implement** an `AnimatedVisibility(visible)` full-screen festive overlay (cream radial glow, gold sparkle decorations, bouncy `QaidaStarRow`, `ArabicText("ما شاء الله")`, "Lesson complete!" + lessonTitle, an unlock chip when `unlockedTitle != null`, and **Map** / **Next lesson** buttons). Star reveal can animate via `LaunchedEffect`; keep logic simple.
- [ ] **Step 4: Hook into reader:** in `QaidaReaderScreen`, collect `lessonProgress`; when its `status == LessonStatus.COMPLETED`, set a local `showCelebration = true`; render the overlay; `onMap = onNavigateBack`, `onNext = { viewModel.nextLesson() }` (then reset flag on lesson change).
- [ ] **Step 5: Run to verify it passes + build.** → PASS / SUCCESS.
- [ ] **Step 6: Commit.** `git commit -m "feat(qaida): festive lesson-complete celebration (#178)"`

---

## Task 14: `QaidaLetterTile` + `QaidaLetterForms` + `QaidaMakhrajHelper` (molecules)

Three small molecules for the explorer, one task.

**Files:**
- Create: `…/molecules/QaidaLetterTile.kt`, `…/molecules/QaidaLetterForms.kt`, `…/molecules/QaidaMakhrajHelper.kt`
- Test: matching test files in `…/testDebug/.../molecules/`.

**Interfaces:**
- Produces:
  - `@Composable fun QaidaLetterTile(letter: QaidaLetter, heard: Boolean, onClick: (QaidaLetter) -> Unit, modifier: Modifier = Modifier)`
  - `@Composable fun QaidaLetterForms(letter: QaidaLetter, modifier: Modifier = Modifier)` (renders the 4 forms only when `letter.isConnecting`, else a single isolated form)
  - `@Composable fun QaidaMakhrajHelper(area: MakhrajArea, detail: String, modifier: Modifier = Modifier)` — maps `area` to a friendly label + emoji.

- [ ] **Step 1: Write failing tests.**

```kotlin
// QaidaLetterTileTest: render letter ب, heard=true; click → callback with letter; assert ★ contentDescription "heard".
// QaidaLetterFormsTest: connecting letter shows 4 labels (start/middle/end/alone); non-connecting shows only "alone".
// QaidaMakhrajHelperTest: area=SHAFATAIN shows "two lips" label text.
@Test fun `connecting letter shows four forms`() {
    val ba = QaidaLetter(2, "ب", "الباء", "Baa", "ب", "بـ", "ـبـ", "ـب", true,
        MakhrajArea.SHAFATAIN, "two lips", "b", "k_ba", "", 2)
    composeRule.setThemedContent { QaidaLetterForms(ba) }
    composeRule.onNodeWithText("start").assertExists()
    composeRule.onNodeWithText("middle").assertExists()
    composeRule.onNodeWithText("end").assertExists()
    composeRule.onNodeWithText("alone").assertExists()
}
```

- [ ] **Step 2: Run to verify they fail.** → FAIL.
- [ ] **Step 3: Implement** all three (tile = parchment card with `ArabicText(letter.letterArabic)` + a star badge when `heard`, `clickable`; forms = `Row` of up to four labeled `ArabicText` cells gated on `isConnecting`; makhraj = a row with an emoji + `when(area)` label and `detail`). Provide the `MakhrajArea` label map:
  `JAWF→"the throat (deep)"`, `HALQ→"the throat"`, `LISAN→"the tongue"`, `SHAFATAIN→"the two lips"`, `KHAYSHUM→"the nose"`.
- [ ] **Step 4: Run to verify they pass.** → PASS.
- [ ] **Step 5: Commit.** `git commit -m "feat(qaida): letter tile, positional forms, makhraj helper (#178)"`

---

## Task 15: `QaidaLetterBoard` + `QaidaLetterDetailSheet` (organisms)

**Files:**
- Create: `…/organisms/QaidaLetterBoard.kt`, `…/organisms/QaidaLetterDetailSheet.kt`
- Test: matching test files.

**Interfaces:**
- Consumes: `QaidaLetterTile`, `QaidaLetterForms`, `QaidaMakhrajHelper`, `ArabicText`, `QaidaLetter`.
- Produces:
  - `@Composable fun QaidaLetterBoard(letters: List<QaidaLetter>, heardLetterIds: Set<Int>, onLetterClick: (QaidaLetter) -> Unit, modifier: Modifier = Modifier)`
  - `@Composable fun QaidaLetterDetailSheet(letter: QaidaLetter, onPlay: (QaidaLetter) -> Unit, modifier: Modifier = Modifier)`

- [ ] **Step 1: Write the failing tests.**

```kotlin
// QaidaLetterBoardTest: 3 letters, heard={2}; assert all render; click letter 2 → callback.
// QaidaLetterDetailSheetTest: render ب; assert name "Baa" + phonetic shown; play button fires onPlay(letter).
@Test fun `detail play button fires`() {
    var played: QaidaLetter? = null
    val ba = QaidaLetter(2,"ب","الباء","Baa","ب","بـ","ـبـ","ـب",true,
        MakhrajArea.SHAFATAIN,"two lips","b","k_ba","",2)
    composeRule.setThemedContent { QaidaLetterDetailSheet(ba, onPlay = { played = it }) }
    composeRule.onNodeWithContentDescription("Play letter").performClick()
    assertThat(played?.id).isEqualTo(2)
}
```

- [ ] **Step 2: Run to verify they fail.** → FAIL.
- [ ] **Step 3: Implement.** Board = RTL `LazyVerticalGrid(columns = GridCells.Fixed(4))` of `QaidaLetterTile(letter, heard = letter.id in heardLetterIds, onLetterClick)`. Detail sheet = a `Column`: hero `Row` (big `ArabicText(letter.letterArabic)`, name column `ArabicText(letter.nameArabic)` + `Text(letter.nameTransliteration)` + `letter.phoneticHint?.let { Text(it) }`, a circular gold play `IconButton(onClick = { onPlay(letter) })` with `contentDescription = "Play letter"`), then a "shapes" section `QaidaLetterForms(letter)`, then `QaidaMakhrajHelper(letter.makhrajArea, letter.makhrajDetail)`.
- [ ] **Step 4: Run to verify they pass.** → PASS.
- [ ] **Step 5: Commit.** `git commit -m "feat(qaida): letter board + detail sheet organisms (#178)"`

---

## Task 16: Wire `QaidaLettersScreen` + entry from the map

**Files:**
- Modify: `…/screens/qaida/QaidaLettersScreen.kt`
- Modify: `…/screens/qaida/QaidaHomeScreen.kt` (header action → `onOpenLetters`, already passed from Task 1/7)

**Interfaces:**
- Consumes: `QaidaReaderViewModel.letters`, `onCellTapped`-equivalent for letters (use `viewModel`'s letter playback path; letters play via `onCellTapped` is cell-only — confirm a letter-play entry exists. If the ViewModel exposes only cell playback, route letter taps through `QaidaAudioManager` is NOT allowed from UI; instead **add** a `fun playLetter(letter: QaidaLetter)` to `QaidaReaderViewModel` that calls the audio manager with `letter.audioKey`).

- [ ] **Step 1: Confirm letter playback.** Open `QaidaReaderViewModel.kt`. If there is no public method to play a letter's audio, add:

```kotlin
fun playLetter(letter: QaidaLetter) { audioManager.play(letter.audioKey) }
```

and a unit test in `QaidaReaderViewModelTest.kt`:

```kotlin
@Test fun `playLetter plays its audio key`() = runTest {
    val vm = createViewModel()
    vm.playLetter(letter(id = 2, audioKey = "k_ba"))
    advanceUntilIdle()
    verify { audioManager.play("k_ba") }
}
```

(Run the VM test, see it fail, implement, see it pass.)

- [ ] **Step 2: Implement the screen.**

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QaidaLettersScreen(
    onNavigateBack: () -> Unit,
    viewModel: QaidaReaderViewModel = hiltViewModel(),
) {
    val letters by viewModel.letters.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<QaidaLetter?>(null) }
    Scaffold(topBar = { TopAppBar(title = { Text("The Arabic Letters") },
        navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }) { p ->
        Box(Modifier.fillMaxSize().padding(p).background(qaidaParchmentBrush())) {
            QaidaLetterBoard(letters = letters, heardLetterIds = emptySet(),
                onLetterClick = { selected = it })
        }
        selected?.let { letter ->
            ModalBottomSheet(onDismissRequest = { selected = null }) {
                QaidaLetterDetailSheet(letter, onPlay = viewModel::playLetter)
            }
        }
    }
}
```

(`heardLetterIds` can stay `emptySet()` for v1 unless the ViewModel exposes heard-letter ids; if it does, wire it.)

- [ ] **Step 3: Build.** `./gradlew :app:compileDebugKotlin` → SUCCESS.
- [ ] **Step 4: Manually verify** the board renders, tapping a letter opens the sheet, play works.
- [ ] **Step 5: Commit.** `git commit -m "feat(qaida): wire letter explorer screen (#178)"`

---

## Task 17: Full verification pass

**Files:** none (verification only).

- [ ] **Step 1: Run the full Qaida test suite.**

Run: `./gradlew :app:testDebugUnitTest --tests "*Qaida*"`
Expected: all pass.

- [ ] **Step 2: Run the coverage gates.**

Run: `./gradlew :app:jacocoAtomsCoverageVerification :app:jacocoMoleculesCoverageVerification :app:jacocoOrganismsCoverageVerification`
Expected: BUILD SUCCESSFUL (≥90% on each). If any new component is under 90%, add tests for the uncovered states/branches and re-run.

- [ ] **Step 3: Full debug build.** Run: `./gradlew :app:assembleDebug` → SUCCESS.
- [ ] **Step 4: Manual end-to-end** (emulator): More → Qaida → map (path renders, stars, Continue) → tap lesson → reader (tap-to-hear highlights, harakat tinted, play-line, toggle) → complete a lesson → festive celebration → next unlocks → open Letter Explorer → tap letter → sheet with forms + makhraj + play. Confirm RTL and TalkBack read sensible descriptions.
- [ ] **Step 5: Commit any test top-ups.** `git commit -m "test(qaida): coverage top-ups for Qaida components (#178)"`

---

## Self-Review (completed by plan author)

- **Spec coverage:** course map (T5–7), reader + tap-to-hear + harakat + play-line + translit toggle (T8–12), celebration (T13), letter explorer + bottom sheet + forms + makhraj (T14–16), navigation + entry (T1), accessibility/RTL (woven into each component task + verified T17). ✓
- **Placeholders:** the few prose-only "implement" steps (T5, T9, T10, T13–16 bodies) are bounded by exact interfaces, full tests, and concrete styling instructions; small molecules share a task to avoid filler. Navigation/entry exact names are flagged "open the file first" because they depend on un-quotable existing code. ✓
- **Type consistency:** `QaidaMedallionState`, `QaidaColors`, component signatures, and ViewModel method names (`selectLesson`, `onCellTapped`, `playLine`, `nextLesson`, `resume`, new `playLetter`) are consistent across tasks. ✓
