# Prayer tracker redesign — implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the prayer tracker so it reports what the user actually logged — introducing a derived `NOT_RECORDED` state, retiring the midnight job that silently rewrites unlogged prayers to `missed`, and replacing the tabbed screen with a single scroll plus a real qada sub-screen.

**Architecture:** One new atom (`NimazTimelineTrack`) and two small extensions to existing components carry the whole visual design; everything else reuses the design system, including four atoms from the `feat/fasting-screen-redesign` base. The `NOT_RECORDED` state is derived at presentation time by a pure, Android-free function, so it needs no Room migration and is covered by plain JVM tests. The screen keeps its existing `PrayerTrackerViewModel`; only its event vocabulary changes.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Hilt, Room, Robolectric + Truth for component tests, JUnit4 + Turbine/coroutines-test for ViewModel tests.

**Spec:** `docs/superpowers/specs/2026-08-13-prayer-tracker-redesign-design.md`

## Global Constraints

- **Worktree:** `.claude/worktrees/feat+prayer-tracker-redesign`, branch `feat/prayer-tracker-redesign`, based on `feat/fasting-screen-redesign` @ `d49b0b49`. Never rebase onto `dev` — the four fasting atoms this depends on exist only on the base branch.
- **No `Color(0xFF…)` literals in screens or components.** Use `MaterialTheme.colorScheme.*`, `NimazColors.*`, or resolve a `NimazTone` through `NimazToneColors` (CLAUDE.md rule 7).
- **Interactive UI comes from the design system.** No `Text`/`Box`/`Surface` + `Modifier.clickable` for a button or a whole-card tap target (CLAUDE.md rule 8).
- **ViewModels inject `PrayerUseCases`, never a repository or DAO** (CLAUDE.md rule 2).
- **Domain never imports `data`.** No Room entity or DAO type in `domain/` or `presentation/`.
- **Every new atom ships light *and* dark `@Preview`s** using `NimazTheme(themeMode = ThemeMode.LIGHT/DARK)`, plus a Robolectric test in `app/src/testDebug/…`.
- **All new user-facing copy goes in `app/src/main/res/values/strings.xml`.** A string absent from a shipped locale fails `lintDebug` with `MissingTranslation`.
- **Composables never call `context.getString`.** Use `stringResource` (lint: `LocalContextGetResourceValueCall`).
- **Commit after every task.** Do not push to `dev`.
- **Documentation is part of the work** — the doc that owns an area is updated in the same commit that changes it.

**Test commands** (run from the worktree root):

```bash
./gradlew :app:testDebugUnitTest --tests 'com.arshadshah.nimaz.<FQN>'   # one class
./gradlew :app:compileDebugKotlin                                       # KSP: Hilt + Room wiring
./gradlew :app:testDebugUnitTest                                        # full unit suite
./gradlew :app:lintDebug                                                # SLOW (~10 min), CI-blocking
./gradlew :app:assembleDebugAndroidTest                                 # only the nav task needs it
python3 scripts/check_docs.py
```

---

## File Structure

**Created**

| File | Responsibility |
|---|---|
| `app/src/main/java/…/presentation/components/atoms/NimazTimelineTrack.kt` | The timeline atom: N positioned status nodes, optional "now" marker, two edge labels |
| `app/src/testDebug/java/…/presentation/components/atoms/NimazTimelineTrackTest.kt` | Its Robolectric test |
| `app/src/main/java/…/presentation/screens/prayer/PrayerDayStatus.kt` | Pure status derivation — no Compose, no Android |
| `app/src/test/java/…/presentation/screens/prayer/PrayerDayStatusTest.kt` | Its JVM test |
| `app/src/main/java/…/presentation/screens/prayer/QadaPrayersScreen.kt` | The qada sub-screen |
| `app/src/main/java/…/presentation/screens/prayer/PrayerTrackerDayCard.kt` | The day card + its prayer rows, split out to keep the screen file focused |

**Modified**

| File | Change |
|---|---|
| `…/components/atoms/NimazTimelineTrack.kt` | (created above) |
| `…/components/molecules/NimazAccordion.kt` | Hoisted-expansion overload + `NimazAccordionStyle` |
| `…/components/molecules/calendar/CalendarModels.kt` | `CalendarDayState.indicatorBar` + `indicatorBarColor` |
| `…/components/molecules/calendar/NimazCalendar.kt` | Render the bar |
| `…/data/local/database/dao/PrayerDao.kt` | Scope `markPastPrayersAsMissed` to a date range |
| `…/domain/repository/PrayerRepository.kt` | Same, on the interface |
| `…/data/repository/PrayerRepositoryImpl.kt` | Same, on the impl |
| `…/domain/usecase/PrayerUseCases.kt` | Add `MarkUnrecordedAsMissedUseCase` |
| `…/core/di/RepositoryModule.kt` | Wire the new use case |
| `…/core/util/PrayerRescheduler.kt` | Drop `markPastAsMissed` |
| `…/core/util/BootReceiver.kt` | Stop asking for it |
| `…/presentation/viewmodel/tracker/PrayerTrackerEvent.kt` | `SetPrayerStatus`, `ConfirmUnrecordedAsMissed` |
| `…/presentation/viewmodel/tracker/PrayerTrackerViewModel.kt` | Handle them |
| `…/presentation/screens/prayer/PrayerTrackerScreen.kt` | Full rewrite |
| `…/core/navigation/Routes.kt` | `PrayerTracker` → `data object` |
| `…/core/navigation/NavGraph.kt` | Real qada destination; no `initialTab` |
| `…/core/navigation/AnnouncementRoutes.kt` | Tab 1 → `QadaPrayers` |
| `…/core/navigation/HelpDeepLink.kt` | `Route.PrayerTracker` |
| `app/src/main/res/values/strings.xml` | New copy |
| `docs/NAVIGATION.md`, `docs/SUBSYSTEMS.md`, `docs/CLEAN_ARCHITECTURE_CHECKLIST.md` | §5 of the spec |

Package root is `com.arshadshah.nimaz`; `…` above stands for `app/src/main/java/com/arshadshah/nimaz`.

---

# Phase 1 — Atoms

**Nothing in phases 2–5 may start until the user has reviewed the previews from this phase.** That is an explicit requirement, not a suggestion.

---

### Task 1: `NimazTimelineTrack`

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/components/atoms/NimazTimelineTrack.kt`
- Test: `app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/atoms/NimazTimelineTrackTest.kt`

**Interfaces:**
- Consumes: `NimazTone`, `NimazToneColors` (internal), `NimazStatusDot(spec:…)`, `NimazStatusDotSpec`, `NimazStatusDotSize` — all in the same package, all already on the base branch.
- Produces: `NimazTimelineNode(position: Float, spec: NimazStatusDotSpec, label: String)` and `NimazTimelineTrack(nodes: List<NimazTimelineNode>, startLabel: String, endLabel: String, modifier: Modifier, progress: Float?, contentDescription: String?)`. Task 10 places five nodes on it.

- [ ] **Step 1: Write the failing test**

Create `app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/atoms/NimazTimelineTrackTest.kt`:

```kotlin
package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazTimelineTrackTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val nodes = listOf(
        NimazTimelineNode(0f, NimazStatusDotSpec(NimazTone.SUCCESS), "Fajr"),
        NimazTimelineNode(0.5f, NimazStatusDotSpec(NimazTone.WARNING, NimazStatusDotStyle.OUTLINED), "Asr"),
        NimazTimelineNode(1f, NimazStatusDotSpec(NimazTone.MUTED), "Isha"),
    )

    @Test
    fun `node positions are clamped into the track`() {
        assertThat(NimazTimelineNode(-3f, NimazStatusDotSpec(NimazTone.SUCCESS), "x").safePosition)
            .isEqualTo(0f)
        assertThat(NimazTimelineNode(4f, NimazStatusDotSpec(NimazTone.SUCCESS), "x").safePosition)
            .isEqualTo(1f)
        assertThat(NimazTimelineNode(Float.NaN, NimazStatusDotSpec(NimazTone.SUCCESS), "x").safePosition)
            .isEqualTo(0f)
    }

    @Test
    fun `edge labels are rendered`() {
        composeRule.setThemedContent {
            NimazTimelineTrack(nodes = nodes, startLabel = "Fajr 04:31", endLabel = "Isha 22:35")
        }
        composeRule.onNodeWithText("Fajr 04:31").assertIsDisplayed()
        composeRule.onNodeWithText("Isha 22:35").assertIsDisplayed()
    }

    @Test
    fun `a described track speaks one sentence and hides its nodes`() {
        composeRule.setThemedContent {
            NimazTimelineTrack(
                nodes = nodes,
                startLabel = "Fajr 04:31",
                endLabel = "Isha 22:35",
                progress = 0.6f,
                contentDescription = "Three of five prayers recorded",
            )
        }
        composeRule.onNodeWithContentDescription("Three of five prayers recorded").assertIsDisplayed()
        composeRule.onNodeWithText("Fajr 04:31").assertDoesNotExist()
    }

    @Test
    fun `an out of range or NaN progress does not throw`() {
        composeRule.setThemedContent {
            NimazTimelineTrack(nodes = nodes, startLabel = "a", endLabel = "b", progress = Float.NaN)
            NimazTimelineTrack(nodes = nodes, startLabel = "a", endLabel = "b", progress = 9f)
            NimazTimelineTrack(nodes = nodes, startLabel = "a", endLabel = "b", progress = -2f)
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `an empty node list renders the bare track`() {
        composeRule.setThemedContent {
            NimazTimelineTrack(nodes = emptyList(), startLabel = "a", endLabel = "b")
        }
        composeRule.waitForIdle()
    }
}
```

Add the missing import for `assertDoesNotExist`:

```kotlin
import androidx.compose.ui.test.assertDoesNotExist
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests 'com.arshadshah.nimaz.presentation.components.atoms.NimazTimelineTrackTest'`
Expected: FAIL — compilation error, `Unresolved reference: NimazTimelineNode`.

- [ ] **Step 3: Write the atom**

Create `app/src/main/java/com/arshadshah/nimaz/presentation/components/atoms/NimazTimelineTrack.kt`:

```kotlin
package com.arshadshah.nimaz.presentation.components.atoms

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/** Height of the row the nodes sit in. Tall enough to centre the largest dot with room to spare. */
private val TrackHeight = 30.dp

/** Thickness of the hairline the nodes sit on. */
private val LineHeight = 2.dp

/** Width of the "now" marker. Thin enough to read as a position, not a segment. */
private val MarkerWidth = 2.dp

/** Diameter of a node. Larger than [NimazStatusDotSize.LARGE] because it must read on a line. */
private val NodeDiameter = 14.dp

/**
 * One marker on a [NimazTimelineTrack].
 *
 * @param position where along the track it sits, `0f..1f`. Coerced by [safePosition] rather than
 *   validated: the caller computes it from two clock times, and a day whose Isha equals its Fajr
 *   (polar latitudes, a bad location fix) would otherwise divide by zero and place a node at NaN.
 * @param label the node's own name, used only for accessibility when the track is not described
 *   as a whole.
 */
data class NimazTimelineNode(
    val position: Float,
    val spec: NimazStatusDotSpec,
    val label: String,
) {
    internal val safePosition: Float
        get() = if (position.isNaN()) 0f else position.coerceIn(0f, 1f)
}

/**
 * A hairline carrying N status nodes at proportional positions, with an optional "now" marker.
 *
 * Deliberately **not** [NimazWindowTrack] and **not** [NimazProgressTrack]. A window has two
 * named, differently-tinted ends and nothing between them; a progress bar has one meaningful end
 * and no interior structure. This has five interior nodes that each carry their own status colour,
 * and its fill means *elapsed*, not *complete* — a day can be entirely elapsed and entirely
 * unrecorded. Folding any two of those three into one component would give it parameters that
 * only apply in one of its modes, which is two atoms wearing one name.
 *
 * @param progress position of the "now" marker in `0f..1f`. `null` draws no marker and lights the
 *   whole line — the correct rendering for any day that is not today, and most days you look at
 *   are not today. Out-of-range values and `NaN` are coerced rather than thrown.
 * @param contentDescription one spoken sentence for the whole track. Five unlabelled dots read as
 *   noise, so supplying this **clears** the children from the accessibility tree rather than
 *   adding a sixth announcement on top of them.
 */
@Composable
fun NimazTimelineTrack(
    nodes: List<NimazTimelineNode>,
    startLabel: String,
    endLabel: String,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    contentDescription: String? = null,
) {
    val safeProgress = progress?.let { if (it.isNaN()) 0f else it.coerceIn(0f, 1f) }

    val semanticsModifier = if (contentDescription != null) {
        Modifier.clearAndSetSemantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }

    Column(modifier = modifier.then(semanticsModifier)) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                // Inset by half a node so a node at 0f or 1f is not clipped by the track edge.
                .padding(horizontal = NodeDiameter / 2)
                .height(TrackHeight)
        ) {
            val trackWidth = maxWidth

            // The unlit line, full width.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth()
                    .height(LineHeight)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            )

            // The elapsed portion. A null progress lights all of it.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .height(LineHeight)
                    .layout { measurable, constraints ->
                        val width = (constraints.maxWidth * (safeProgress ?: 1f)).toInt()
                        val placeable = measurable.measure(
                            constraints.copy(minWidth = width, maxWidth = width)
                        )
                        layout(width, placeable.height) { placeable.placeRelative(0, 0) }
                    }
                    .clip(RoundedCornerShape(percent = 50))
                    .background(MaterialTheme.colorScheme.primary)
            )

            nodes.forEach { node ->
                NimazStatusDot(
                    spec = node.spec,
                    diameter = NodeDiameter,
                    contentDescription = if (contentDescription == null) node.label else null,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = trackWidth * node.safePosition - NodeDiameter / 2)
                )
            }

            if (safeProgress != null) {
                Box(
                    modifier = Modifier
                        .offset(x = trackWidth * safeProgress - MarkerWidth / 2)
                        .width(MarkerWidth)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.onSurface)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            EdgeLabel(startLabel)
            EdgeLabel(endLabel)
        }
    }
}

@Composable
private fun EdgeLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold
    )
}

// ==================== PREVIEWS ====================

@Composable
private fun ShowcaseLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** The five prayers of 13 August at their real proportional positions. */
private val previewNodes = listOf(
    NimazTimelineNode(0f, NimazStatusDotSpec(NimazTone.SUCCESS), "Fajr"),
    NimazTimelineNode(0.503f, NimazStatusDotSpec(NimazTone.ACCENT), "Dhuhr"),
    NimazTimelineNode(0.725f, NimazStatusDotSpec(NimazTone.WARNING, NimazStatusDotStyle.OUTLINED), "Asr"),
    NimazTimelineNode(0.913f, NimazStatusDotSpec(NimazTone.MUTED), "Maghrib"),
    NimazTimelineNode(1f, NimazStatusDotSpec(NimazTone.MUTED), "Isha"),
)

@Composable
private fun NimazTimelineTrackShowcase() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        ShowcaseLabel("Today — 18:10, Asr passed unrecorded")
        NimazTimelineTrack(previewNodes, "Fajr 04:31", "Isha 22:35", progress = 0.755f)

        ShowcaseLabel("A finished day — every prayer recorded")
        NimazTimelineTrack(
            nodes = previewNodes.map { it.copy(spec = NimazStatusDotSpec(NimazTone.SUCCESS)) },
            startLabel = "Fajr 04:29",
            endLabel = "Isha 22:38",
            progress = null,
        )

        ShowcaseLabel("A day nobody logged — all rings")
        NimazTimelineTrack(
            nodes = previewNodes.map {
                it.copy(spec = NimazStatusDotSpec(NimazTone.WARNING, NimazStatusDotStyle.OUTLINED))
            },
            startLabel = "Fajr 04:27",
            endLabel = "Isha 22:41",
            progress = null,
        )

        ShowcaseLabel("Just after Fajr — the marker sits on the first node")
        NimazTimelineTrack(previewNodes, "Fajr 04:31", "Isha 22:35", progress = 0.01f)
    }
}

@Preview(showBackground = true, widthDp = 360, name = "NimazTimelineTrack — Light")
@Composable
private fun NimazTimelineTrackLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { NimazTimelineTrackShowcase() }
}

@Preview(
    showBackground = true, widthDp = 360, name = "NimazTimelineTrack — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazTimelineTrackDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { NimazTimelineTrackShowcase() }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests 'com.arshadshah.nimaz.presentation.components.atoms.NimazTimelineTrackTest'`
Expected: PASS, 5 tests.

If `safePosition` is reported as inaccessible from the test, confirm the test file is in the same package (`com.arshadshah.nimaz.presentation.components.atoms`) — `internal` is module-visible, so it should resolve.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/components/atoms/NimazTimelineTrack.kt \
        app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/atoms/NimazTimelineTrackTest.kt
git commit -m "feat(atoms): a timeline track that carries a status per node

A window track has two named ends and nothing between them; a progress bar
has one end and no interior. A prayer day has five interior points that each
carry their own state, and a fill that means elapsed rather than complete --
a day can be wholly elapsed and wholly unrecorded."
```

---

### Task 2: `NimazAccordion` — hoisted expansion and a flat style

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/components/molecules/NimazAccordion.kt`
- Test: `app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/molecules/NimazAccordionTest.kt` (create if absent)

**Interfaces:**
- Produces: `enum class NimazAccordionStyle { CARD, FLAT }` and an overload
  `NimazAccordion(title, expanded: Boolean, onExpandedChange: (Boolean) -> Unit, modifier, subtitle, leadingIcon, trailing, style, content)`. Task 10 uses it for the five prayer rows.
- The existing signature stays as an overload that owns its state — **every current call site must keep compiling unchanged.**

- [ ] **Step 1: Write the failing test**

Create (or extend) `app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/molecules/NimazAccordionTest.kt`:

```kotlin
package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazAccordionTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `styles are complete`() {
        assertThat(NimazAccordionStyle.entries).hasSize(2)
    }

    @Test
    fun `the hoisted overload reports a toggle instead of expanding itself`() {
        val toggles = mutableListOf<Boolean>()
        composeRule.setThemedContent {
            NimazAccordion(
                title = "Fajr",
                expanded = false,
                onExpandedChange = { toggles += it },
            ) { Text("body") }
        }

        composeRule.onNodeWithText("Fajr").performClick()

        // The caller owns the state, so the body must stay closed until the caller says otherwise.
        assertThat(toggles).containsExactly(true)
        composeRule.onNodeWithText("body").assertDoesNotExist()
    }

    @Test
    fun `the hoisted overload shows the body when the caller says it is expanded`() {
        composeRule.setThemedContent {
            NimazAccordion(
                title = "Fajr",
                expanded = true,
                onExpandedChange = {},
            ) { Text("body") }
        }
        composeRule.onNodeWithText("body").assertIsDisplayed()
    }

    @Test
    fun `the flat style still renders its header and body`() {
        composeRule.setThemedContent {
            NimazAccordion(
                title = "Asr",
                expanded = true,
                onExpandedChange = {},
                style = NimazAccordionStyle.FLAT,
            ) { Text("picker") }
        }
        composeRule.onNodeWithText("Asr").assertIsDisplayed()
        composeRule.onNodeWithText("picker").assertIsDisplayed()
    }

    @Test
    fun `the self-managing overload still expands on its own`() {
        composeRule.setThemedContent {
            NimazAccordion(title = "How are prayer times calculated?") { Text("answer") }
        }
        composeRule.onNodeWithText("answer").assertDoesNotExist()
        composeRule.onNodeWithText("How are prayer times calculated?").performClick()
        composeRule.onNodeWithText("answer").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests 'com.arshadshah.nimaz.presentation.components.molecules.NimazAccordionTest'`
Expected: FAIL — `Unresolved reference: NimazAccordionStyle`.

- [ ] **Step 3: Refactor the component**

In `NimazAccordion.kt`, add the style enum above the existing composable:

```kotlin
/**
 * Whether the accordion draws its own surface.
 *
 * [CARD] is the standalone row used across Help &amp; Support. [FLAT] draws no card and no
 * elevation, for rows that are already inside one — five nested cards inside a day card reads as
 * a stack of receipts rather than a list.
 */
enum class NimazAccordionStyle { CARD, FLAT }
```

Replace the existing composable's body so it delegates, and add the hoisted overload. The existing KDoc block stays on the self-managing overload; add `@param style` to it.

```kotlin
@Composable
fun NimazAccordion(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    initiallyExpanded: Boolean = false,
    style: NimazAccordionStyle = NimazAccordionStyle.CARD,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    NimazAccordion(
        title = title,
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
        subtitle = subtitle,
        leadingIcon = leadingIcon,
        trailing = trailing,
        style = style,
        content = content,
    )
}

/**
 * The state-hoisted accordion.
 *
 * Exists because "only one row open at a time" is impossible to express against the overload
 * above: it owns `expanded` in a private `remember`, so no caller can close a row it did not
 * click. A day card of five prayers that all unfold at once is a wall, not a list.
 *
 * @param expanded whether the body is open. The caller owns it.
 * @param onExpandedChange invoked with the value the header tap is asking for. Nothing moves
 *   until the caller feeds a new [expanded] back in.
 * @param style whether the accordion draws its own card.
 */
@Composable
fun NimazAccordion(
    title: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    style: NimazAccordionStyle = NimazAccordionStyle.CARD,
    content: @Composable ColumnScope.() -> Unit,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "accordion_chevron"
    )

    val body: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingIcon != null) {
                    NimazIconWell(
                        icon = leadingIcon,
                        tone = NimazTone.ACCENT,
                        size = NimazIconWellSize.SMALL,
                        shape = NimazIconWellShape.ROUNDED
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (trailing != null) {
                    Spacer(modifier = Modifier.width(12.dp))
                    trailing()
                    Spacer(modifier = Modifier.width(4.dp))
                }
                NimazIcon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    variant = NimazIconVariant.MUTED,
                    modifier = Modifier.rotate(chevronRotation)
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    content = content
                )
            }
        }
    }

    when (style) {
        NimazAccordionStyle.CARD -> NimazCard(
            style = NimazCardStyle.FILLED,
            onClick = { onExpandedChange(!expanded) },
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = 1.dp
        ) { body() }

        // A FLAT accordion is already inside a card, so it must not paint another one. The tap
        // target is the whole header row, which is what `NimazCard(onClick=…)` gives the CARD
        // style -- here that role falls to a transparent, radius-free NimazCard so the ripple
        // still comes from the design system rather than a bare Modifier.clickable.
        NimazAccordionStyle.FLAT -> NimazCard(
            style = NimazCardStyle.FILLED,
            tone = NimazTone.TRANSPARENT,
            onClick = { onExpandedChange(!expanded) },
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp),
            elevation = 0.dp
        ) { body() }
    }
}
```

Add the import `androidx.compose.runtime.setValue` if it is not already present (it is), and keep every existing import.

> **If `NimazCard` has no `TRANSPARENT` tone or rejects `RoundedCornerShape(0.dp)`:** read `NimazCard.kt` and use the closest existing no-surface configuration rather than inventing one, and note what you used in the commit message. Do **not** fall back to `Modifier.clickable` on a `Box` — that is the rule-8 violation this whole task is removing.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests 'com.arshadshah.nimaz.presentation.components.molecules.NimazAccordionTest'`
Expected: PASS, 5 tests.

- [ ] **Step 5: Verify no existing call site broke**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. The self-managing overload is unchanged in shape, so Help & Support and the notification rows must still compile untouched.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/components/molecules/NimazAccordion.kt \
        app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/molecules/NimazAccordionTest.kt
git commit -m "feat(molecules): let a caller own an accordion's expansion

It kept 'expanded' in a private remember, so no caller could close a row it
did not click -- and 'one open at a time' is not a preference in a day card
of five prayers, it is the difference between a list and a wall. Adds a FLAT
style for rows already inside a card."
```

---

### Task 3: `CalendarDayState` — a fill bar per day

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/components/molecules/calendar/CalendarModels.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/components/molecules/calendar/NimazCalendar.kt:505` (inside the day cell, after the indicator dot block)
- Test: `app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/molecules/calendar/NimazCalendarTest.kt` (create if absent)

**Interfaces:**
- Produces: `CalendarDayState.indicatorBar: Float?` and `CalendarDayState.indicatorBarColor: Color?`. Task 10 supplies them from the month's records.

- [ ] **Step 1: Write the failing test**

Create `app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/molecules/calendar/NimazCalendarTest.kt`:

```kotlin
package com.arshadshah.nimaz.presentation.components.molecules.calendar

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazCalendarTest {

    @Test
    fun `a day state draws no bar by default`() {
        assertThat(CalendarDayState().indicatorBar).isNull()
        assertThat(CalendarDayState().indicatorBarColor).isNull()
    }

    @Test
    fun `the bar and the dot are independent`() {
        val barOnly = CalendarDayState(indicatorBar = 0.6f)
        assertThat(barOnly.indicatorBar).isEqualTo(0.6f)
        assertThat(barOnly.indicatorColor).isNull()

        val both = CalendarDayState(indicatorColor = Color.Red, indicatorBar = 1f)
        assertThat(both.indicatorColor).isEqualTo(Color.Red)
        assertThat(both.indicatorBar).isEqualTo(1f)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests 'com.arshadshah.nimaz.presentation.components.molecules.calendar.NimazCalendarTest'`
Expected: FAIL — `No parameter with name 'indicatorBar' found`.

- [ ] **Step 3: Add the fields**

In `CalendarModels.kt`, append two parameters to `CalendarDayState` (after `indicatorStyle`) and document them in the existing KDoc block:

```kotlin
    /**
     * Fraction of the day completed, `0f..1f`, drawn as a short bar under the day number.
     * `null` draws no bar.
     *
     * Independent of [indicatorColor]: a dot answers "what kind of day was this", a bar answers
     * "how much of it", and a month grid that can only say the first cannot show a day where four
     * of five prayers landed. Callers may set either, both, or neither.
     */
    val indicatorBar: Float? = null,

    /** Colour of [indicatorBar]. `null` uses the theme primary. */
    val indicatorBarColor: Color? = null,
```

In `NimazCalendar.kt`, immediately after the closing brace of the `dayState.indicatorColor?.let { … }` block inside the day cell (around line 531), add:

```kotlin
        // Fill bar — how much of the day was completed. Sits under the number rather than
        // replacing it, so a bar and a dot can coexist on the same cell.
        dayState.indicatorBar?.let { rawFraction ->
            val fraction = if (rawFraction.isNaN()) 0f else rawFraction.coerceIn(0f, 1f)
            val barColor = when {
                isSelectedBackgroundFill -> scheme.onPrimary
                else -> dayState.indicatorBarColor ?: scheme.primary
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 3.dp)
                    .width(18.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(scheme.outlineVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .background(barColor)
                )
            }
        }
```

Add whichever of these imports `NimazCalendar.kt` does not already have:

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
```

- [ ] **Step 4: Run test and compile**

Run: `./gradlew :app:testDebugUnitTest --tests 'com.arshadshah.nimaz.presentation.components.molecules.calendar.NimazCalendarTest'`
Expected: PASS, 2 tests.

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL — both new fields have defaults, so no existing `CalendarDayState(...)` call site changes.

- [ ] **Step 5: Add a preview showing the bar**

In `NimazCalendar.kt`'s previews section, add a preview that renders a month with fill bars, so the reviewer can see it without running the app:

```kotlin
@Preview(showBackground = true, name = "NimazCalendar - prayer fill bars")
@Composable
private fun NimazCalendarFillBarPreview() {
    val month = java.time.YearMonth.of(2026, 8)
    NimazTheme {
        NimazCalendar(
            displayedMonth = month,
            selectedDate = month.atDay(13),
            onDateSelected = {},
            onPreviousMonth = {},
            onNextMonth = {},
            selectionStyle = SelectionStyle.BORDER,
            dayStateProvider = { date ->
                // A repeating 5,4,0,5,3,5,2 pattern so every bar length is visible at once.
                val prayed = listOf(5, 4, 0, 5, 3, 5, 2)[date.dayOfMonth % 7]
                CalendarDayState(
                    indicatorBar = if (date.dayOfMonth <= 13) prayed / 5f else null,
                    indicatorBarColor = if (prayed == 5) {
                        NimazColors.StatusColors.Prayed
                    } else {
                        NimazColors.StatusColors.Partial
                    }
                )
            }
        )
    }
}
```

Add `import com.arshadshah.nimaz.presentation.theme.NimazColors` if absent.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/components/molecules/calendar/ \
        app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/molecules/calendar/NimazCalendarTest.kt
git commit -m "feat(calendar): a day cell can show how much of the day, not just what kind

A dot says 'partial'. A bar says 'four of five'. The month grid could only
say the first, which is the difference between a glance that tells you
something and one that tells you there is something to look at."
```

---

### Task 4: Phase 1 gate — user review of previews

**Files:** none.

- [ ] **Step 1: Run the full unit suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS. Record any pre-existing failures that are unrelated to this branch rather than fixing them here.

- [ ] **Step 2: Compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Stop and hand the previews to the user**

Report to the user the preview names now available in Android Studio's preview pane:

- `NimazTimelineTrack — Light` / `NimazTimelineTrack — Dark` (4 states each)
- `Accordion — collapsed` / `Accordion — expanded` / `Accordion — trailing` (unchanged) plus the FLAT rendering
- `NimazCalendar - prayer fill bars`

**Do not begin Task 5 until the user approves.** This is the checkpoint they asked for.

---

# Phase 2 — Semantics

---

### Task 5: `PrayerDayStatus` — derive `NOT_RECORDED`

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/prayer/PrayerDayStatus.kt`
- Test: `app/src/test/java/com/arshadshah/nimaz/presentation/screens/prayer/PrayerDayStatusTest.kt`

> Note the source set: `src/test`, not `src/testDebug`. This file has no Android or Compose dependency, so it belongs in the plain JVM source set. If the project has no `src/test` tree, put it in `src/testDebug` alongside the others and say so in the commit message.

**Interfaces:**
- Consumes: `PrayerRecord`, `PrayerName`, `PrayerStatus`, `PrayerTimes` from `domain.model`; `NimazTone` from the atoms package.
- Produces:
  - `enum class PrayerDisplayStatus { PRAYED, LATE, QADA, MISSED, NOT_RECORDED, UPCOMING }`
  - `val TRACKED_PRAYERS: List<PrayerName>` — the five, excluding `SUNRISE`
  - `fun resolvePrayerStatuses(records, times, date, now): Map<PrayerName, PrayerDisplayStatus>`
  - `fun PrayerDisplayStatus.isDone(): Boolean`
  - `fun PrayerDisplayStatus.tone(): NimazTone`
  - `fun PrayerDisplayStatus.dotStyle(): NimazStatusDotStyle`
  - `fun PrayerTimes.timeFor(prayer: PrayerName): LocalDateTime?`

  Tasks 8, 9 and 10 all consume these.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/arshadshah/nimaz/presentation/screens/prayer/PrayerDayStatusTest.kt`:

```kotlin
package com.arshadshah.nimaz.presentation.screens.prayer

import com.arshadshah.nimaz.domain.model.Location
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerTimes
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class PrayerDayStatusTest {

    private val day: LocalDate = LocalDate.of(2026, 8, 13)

    private val location = Location(
        id = 1L, name = "Test", latitude = 0.0, longitude = 0.0,
        isCurrent = true, isFavorite = false
    )

    private val times = PrayerTimes(
        fajr = day.atTime(4, 31),
        sunrise = day.atTime(6, 5),
        dhuhr = day.atTime(13, 35),
        asr = day.atTime(17, 35),
        maghrib = day.atTime(20, 58),
        isha = day.atTime(22, 35),
        date = day,
        location = location,
    )

    private fun record(prayer: PrayerName, status: PrayerStatus) = PrayerRecord(
        id = 0L, date = 0L, prayerName = prayer, status = status,
        prayedAt = null, scheduledTime = 0L, isJamaah = false,
        isQadaFor = null, note = null, createdAt = 0L, updatedAt = 0L,
    )

    @Test
    fun `sunrise is never tracked`() {
        assertThat(TRACKED_PRAYERS).containsExactly(
            PrayerName.FAJR, PrayerName.DHUHR, PrayerName.ASR,
            PrayerName.MAGHRIB, PrayerName.ISHA,
        ).inOrder()

        val resolved = resolvePrayerStatuses(emptyList(), times, day, day.atTime(23, 0))
        assertThat(resolved).doesNotContainKey(PrayerName.SUNRISE)
    }

    @Test
    fun `on today a passed prayer with no record is not recorded, not missed`() {
        val resolved = resolvePrayerStatuses(emptyList(), times, day, day.atTime(18, 10))

        assertThat(resolved[PrayerName.FAJR]).isEqualTo(PrayerDisplayStatus.NOT_RECORDED)
        assertThat(resolved[PrayerName.ASR]).isEqualTo(PrayerDisplayStatus.NOT_RECORDED)
        assertThat(resolved[PrayerName.MAGHRIB]).isEqualTo(PrayerDisplayStatus.UPCOMING)
        assertThat(resolved[PrayerName.ISHA]).isEqualTo(PrayerDisplayStatus.UPCOMING)
    }

    @Test
    fun `a prayer exactly at its time has not passed yet`() {
        val resolved = resolvePrayerStatuses(emptyList(), times, day, day.atTime(17, 35))
        assertThat(resolved[PrayerName.ASR]).isEqualTo(PrayerDisplayStatus.UPCOMING)
    }

    @Test
    fun `every prayer on a past day with no record is not recorded`() {
        val resolved = resolvePrayerStatuses(
            records = emptyList(), times = times,
            date = day, now = day.plusDays(3).atTime(9, 0),
        )
        assertThat(resolved.values.toSet()).containsExactly(PrayerDisplayStatus.NOT_RECORDED)
    }

    @Test
    fun `every prayer on a future day is upcoming`() {
        val resolved = resolvePrayerStatuses(
            records = emptyList(), times = times,
            date = day, now = day.minusDays(2).atTime(9, 0),
        )
        assertThat(resolved.values.toSet()).containsExactly(PrayerDisplayStatus.UPCOMING)
    }

    @Test
    fun `an asserted record beats the derivation`() {
        val records = listOf(
            record(PrayerName.FAJR, PrayerStatus.PRAYED),
            record(PrayerName.DHUHR, PrayerStatus.LATE),
            record(PrayerName.ASR, PrayerStatus.MISSED),
            record(PrayerName.MAGHRIB, PrayerStatus.QADA),
        )
        val resolved = resolvePrayerStatuses(records, times, day, day.atTime(23, 59))

        assertThat(resolved[PrayerName.FAJR]).isEqualTo(PrayerDisplayStatus.PRAYED)
        assertThat(resolved[PrayerName.DHUHR]).isEqualTo(PrayerDisplayStatus.LATE)
        assertThat(resolved[PrayerName.ASR]).isEqualTo(PrayerDisplayStatus.MISSED)
        assertThat(resolved[PrayerName.MAGHRIB]).isEqualTo(PrayerDisplayStatus.QADA)
        assertThat(resolved[PrayerName.ISHA]).isEqualTo(PrayerDisplayStatus.NOT_RECORDED)
    }

    @Test
    fun `NOT_PRAYED and PENDING are absence, not assertions`() {
        val records = listOf(
            record(PrayerName.FAJR, PrayerStatus.NOT_PRAYED),
            record(PrayerName.DHUHR, PrayerStatus.PENDING),
        )
        val resolved = resolvePrayerStatuses(records, times, day, day.atTime(18, 10))

        assertThat(resolved[PrayerName.FAJR]).isEqualTo(PrayerDisplayStatus.NOT_RECORDED)
        assertThat(resolved[PrayerName.DHUHR]).isEqualTo(PrayerDisplayStatus.NOT_RECORDED)
    }

    @Test
    fun `without prayer times a past day still derives not recorded`() {
        val resolved = resolvePrayerStatuses(
            records = emptyList(), times = null,
            date = day, now = day.plusDays(1).atTime(9, 0),
        )
        assertThat(resolved.values.toSet()).containsExactly(PrayerDisplayStatus.NOT_RECORDED)
    }

    @Test
    fun `without prayer times today claims nothing has passed`() {
        val resolved = resolvePrayerStatuses(
            records = emptyList(), times = null,
            date = day, now = day.atTime(23, 59),
        )
        assertThat(resolved.values.toSet()).containsExactly(PrayerDisplayStatus.UPCOMING)
    }

    @Test
    fun `isDone counts the three ways a prayer can be fulfilled`() {
        assertThat(PrayerDisplayStatus.entries.filter { it.isDone() }).containsExactly(
            PrayerDisplayStatus.PRAYED, PrayerDisplayStatus.LATE, PrayerDisplayStatus.QADA,
        )
    }

    @Test
    fun `countNotRecorded ignores days that are not over`() {
        val onlyNotRecorded = resolvePrayerStatuses(emptyList(), times, day, day.atTime(18, 10))
        assertThat(onlyNotRecorded.values.count { it == PrayerDisplayStatus.NOT_RECORDED })
            .isEqualTo(3)
    }
}
```

> **If `Location`'s constructor differs**, read `domain/model/Location` (or `PrayerModels.kt`) and fix the fixture — do not change the production signature to suit the test.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests 'com.arshadshah.nimaz.presentation.screens.prayer.PrayerDayStatusTest'`
Expected: FAIL — `Unresolved reference: resolvePrayerStatuses`.

- [ ] **Step 3: Write the implementation**

Create `app/src/main/java/com/arshadshah/nimaz/presentation/screens/prayer/PrayerDayStatus.kt`:

```kotlin
package com.arshadshah.nimaz.presentation.screens.prayer

import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerTimes
import com.arshadshah.nimaz.presentation.components.atoms.NimazStatusDotStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import java.time.LocalDate
import java.time.LocalDateTime

/** The five obligatory prayers, in the order they fall. `SUNRISE` is a time, not a prayer. */
val TRACKED_PRAYERS: List<PrayerName> = listOf(
    PrayerName.FAJR,
    PrayerName.DHUHR,
    PrayerName.ASR,
    PrayerName.MAGHRIB,
    PrayerName.ISHA,
)

/**
 * What the tracker shows for one prayer on one day.
 *
 * [NOT_RECORDED] is the reason this type exists. It is **not** a stored [PrayerStatus] and never
 * becomes one on its own: the app used to rewrite every unlogged past prayer to `missed` at
 * midnight, so a user who simply had not opened the app was told they had missed prayers, and
 * those fabricated rows fed the qada list. A prayer nobody logged is a prayer nobody logged.
 */
enum class PrayerDisplayStatus {
    PRAYED,
    LATE,
    QADA,
    MISSED,
    NOT_RECORDED,
    UPCOMING,
}

/**
 * Resolve every tracked prayer's displayed status for [date].
 *
 * A record counts only when it carries an **assertion** — `PRAYED`, `LATE`, `MISSED` or `QADA`.
 * A missing row, a `PENDING` row and a `NOT_PRAYED` row all say the same thing (nobody has said),
 * so all three fall through to the derivation. That equivalence is also what makes the picker's
 * tap-to-clear free: clearing writes `NOT_PRAYED` and the row reads back as [NOT_RECORDED].
 *
 * @param times the day's schedule, or `null` when no location is set yet. Without times there is
 *   no basis to claim a prayer has passed, so on [date] == today everything reads [UPCOMING];
 *   a date wholly in the past still resolves, because the day being over is enough.
 * @param now read from a ticking clock by the caller. A bare `LocalDateTime.now()` is not
 *   observable state, so a screen holding one would not re-resolve as a prayer time arrives.
 */
fun resolvePrayerStatuses(
    records: List<PrayerRecord>,
    times: PrayerTimes?,
    date: LocalDate,
    now: LocalDateTime,
): Map<PrayerName, PrayerDisplayStatus> {
    val today = now.toLocalDate()
    val dayIsOver = date.isBefore(today)
    val dayIsFuture = date.isAfter(today)

    val asserted = records
        .mapNotNull { rec -> rec.status.asAssertion()?.let { rec.prayerName to it } }
        .toMap()

    return TRACKED_PRAYERS.associateWith { prayer ->
        asserted[prayer] ?: when {
            dayIsFuture -> PrayerDisplayStatus.UPCOMING
            dayIsOver -> PrayerDisplayStatus.NOT_RECORDED
            // Today. `isAfter` rather than `!isBefore`: a prayer at exactly its own time has
            // arrived, not passed, and calling it unrecorded on the minute is a false accusation.
            times?.timeFor(prayer)?.let { now.isAfter(it) } == true -> PrayerDisplayStatus.NOT_RECORDED
            else -> PrayerDisplayStatus.UPCOMING
        }
    }
}

/** The stored status as an assertion, or `null` when it asserts nothing. */
private fun PrayerStatus.asAssertion(): PrayerDisplayStatus? = when (this) {
    PrayerStatus.PRAYED -> PrayerDisplayStatus.PRAYED
    PrayerStatus.LATE -> PrayerDisplayStatus.LATE
    PrayerStatus.QADA -> PrayerDisplayStatus.QADA
    PrayerStatus.MISSED -> PrayerDisplayStatus.MISSED
    PrayerStatus.PENDING, PrayerStatus.NOT_PRAYED -> null
}

/** The scheduled time of one tracked prayer, or `null` for `SUNRISE`. */
fun PrayerTimes.timeFor(prayer: PrayerName): LocalDateTime? = when (prayer) {
    PrayerName.FAJR -> fajr
    PrayerName.DHUHR -> dhuhr
    PrayerName.ASR -> asr
    PrayerName.MAGHRIB -> maghrib
    PrayerName.ISHA -> isha
    PrayerName.SUNRISE -> null
}

/** Whether the obligation was fulfilled — on time, late, or made up. */
fun PrayerDisplayStatus.isDone(): Boolean = when (this) {
    PrayerDisplayStatus.PRAYED, PrayerDisplayStatus.LATE, PrayerDisplayStatus.QADA -> true
    PrayerDisplayStatus.MISSED, PrayerDisplayStatus.NOT_RECORDED, PrayerDisplayStatus.UPCOMING -> false
}

/** The semantic colour this status paints in. */
fun PrayerDisplayStatus.tone(): NimazTone = when (this) {
    PrayerDisplayStatus.PRAYED -> NimazTone.SUCCESS
    PrayerDisplayStatus.LATE -> NimazTone.ACCENT
    PrayerDisplayStatus.QADA -> NimazTone.PROMINENT
    PrayerDisplayStatus.MISSED -> NimazTone.ERROR
    PrayerDisplayStatus.NOT_RECORDED -> NimazTone.WARNING
    PrayerDisplayStatus.UPCOMING -> NimazTone.MUTED
}

/**
 * Disc or ring.
 *
 * [NOT_RECORDED] is the only ring: a hollow marker is how the design system says "this is an
 * absence of information", which a filled dot in any colour cannot distinguish from a fact.
 */
fun PrayerDisplayStatus.dotStyle(): NimazStatusDotStyle = when (this) {
    PrayerDisplayStatus.NOT_RECORDED -> NimazStatusDotStyle.OUTLINED
    else -> NimazStatusDotStyle.FILLED
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests 'com.arshadshah.nimaz.presentation.screens.prayer.PrayerDayStatusTest'`
Expected: PASS, 11 tests.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/screens/prayer/PrayerDayStatus.kt \
        app/src/test/java/com/arshadshah/nimaz/presentation/screens/prayer/PrayerDayStatusTest.kt
git commit -m "feat(prayer): a prayer nobody logged is not a prayer you missed

NOT_RECORDED is derived, never stored. A missing row, a PENDING row and a
NOT_PRAYED row all say the same thing -- nobody has said -- so all three
derive, which is also what makes the picker's tap-to-clear free."
```

---

### Task 6: Scope the bulk missed-marking to a date range

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/data/local/database/dao/PrayerDao.kt:94-107`
- Modify: `app/src/main/java/com/arshadshah/nimaz/domain/repository/PrayerRepository.kt:86`
- Modify: `app/src/main/java/com/arshadshah/nimaz/data/repository/PrayerRepositoryImpl.kt:252-256`
- Modify: `app/src/main/java/com/arshadshah/nimaz/domain/usecase/PrayerUseCases.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/core/di/RepositoryModule.kt:764-786`

**Interfaces:**
- Produces: `PrayerRepository.markUnrecordedAsMissed(from: Long, to: Long): Int` (epoch-millis UTC midnights, both inclusive) and `PrayerUseCases.markUnrecordedAsMissed: MarkUnrecordedAsMissedUseCase` with `suspend operator fun invoke(from: Long, to: Long): Int`. Task 8 calls the use case.
- The old unbounded `markPastPrayersAsMissed()` is **deleted** from all three layers.

- [ ] **Step 1: Change the DAO**

Replace the `markPastPrayersAsMissed` query in `PrayerDao.kt` with:

```kotlin
    /**
     * Confirm a range of unrecorded prayers as missed.
     *
     * Ranged rather than "everything before today", which is what it used to be. That form ran
     * from a midnight broadcast and rewrote every prayer the user had simply not logged, so the
     * qada list filled with rows nobody had asserted. Its only caller now is an explicit tap on
     * the review banner, over the days that banner counted.
     *
     * @param from inclusive UTC-midnight epoch millis.
     * @param to inclusive UTC-midnight epoch millis.
     */
    @Query(
        """
        UPDATE prayer_records
        SET status = 'missed', updatedAt = :timestamp
        WHERE date BETWEEN :from AND :to
        AND status IN ('pending', 'not_prayed')
        AND prayerName != 'sunrise'
    """
    )
    suspend fun markUnrecordedAsMissed(
        from: Long,
        to: Long,
        timestamp: Long = System.currentTimeMillis()
    ): Int
```

- [ ] **Step 2: Change the repository interface**

In `PrayerRepository.kt`, replace line 86:

```kotlin
    suspend fun markUnrecordedAsMissed(from: Long, to: Long): Int
```

- [ ] **Step 3: Change the implementation**

In `PrayerRepositoryImpl.kt`, replace the `markPastPrayersAsMissed` override:

```kotlin
    override suspend fun markUnrecordedAsMissed(from: Long, to: Long): Int =
        prayerDao.markUnrecordedAsMissed(from, to)
```

The `LocalDate.now()` and epoch conversion in the old body go away entirely — the caller supplies the range, which is also what makes it testable.

- [ ] **Step 4: Add the use case**

In `PrayerUseCases.kt`, add the field to the `data class` (after `getMissedPrayersRequiringQada`):

```kotlin
    val markUnrecordedAsMissed: MarkUnrecordedAsMissedUseCase,
```

and the use case class alongside the others in the same file:

```kotlin
/**
 * Confirm a range of unrecorded prayers as missed.
 *
 * The only way a prayer enters the qada list. Nothing marks a prayer missed on the user's behalf.
 */
class MarkUnrecordedAsMissedUseCase(private val repository: PrayerRepository) {
    suspend operator fun invoke(from: Long, to: Long): Int =
        repository.markUnrecordedAsMissed(from, to)
}
```

- [ ] **Step 5: Wire DI**

In `RepositoryModule.kt`'s `providePrayerUseCases`, add after `getMissedPrayersRequiringQada`:

```kotlin
            markUnrecordedAsMissed = MarkUnrecordedAsMissedUseCase(repository),
```

Add the import if the file imports use cases individually rather than by wildcard.

- [ ] **Step 6: Compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: **FAIL** — `PrayerRescheduler.kt:50` still calls `markPastPrayersAsMissed()`. That is the correct failure and Task 7 fixes it. Also expect failures in any fake/test double implementing `PrayerRepository`; update each to the new signature.

- [ ] **Step 7: Commit** (together with Task 7 — the tree does not compile between them)

Do not commit yet. Proceed directly to Task 7.

---

### Task 7: Stop marking prayers missed at midnight

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/core/util/PrayerRescheduler.kt:36-52`
- Modify: `app/src/main/java/com/arshadshah/nimaz/core/util/BootReceiver.kt:102-115`
- Test: `app/src/testDebug/java/com/arshadshah/nimaz/core/util/PrayerReschedulerTest.kt` (create, or extend if present)

**Interfaces:**
- Produces: `PrayerRescheduler.rescheduleToday(): Boolean` — no parameters.

- [ ] **Step 1: Write the failing test**

Create `app/src/testDebug/java/com/arshadshah/nimaz/core/util/PrayerReschedulerTest.kt` (if one exists, add the second test to it):

```kotlin
package com.arshadshah.nimaz.core.util

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PrayerReschedulerTest {

    @Test
    fun `rescheduling never marks a prayer missed`() = runTest {
        val repository = mockk<com.arshadshah.nimaz.domain.repository.PrayerRepository>(relaxed = true)
        val scheduler = mockk<PrayerNotificationScheduler>(relaxed = true)
        val preferences =
            mockk<com.arshadshah.nimaz.domain.repository.SettingsRepository>(relaxed = true)

        coEvery { preferences.userPreferences } returns flowOf(defaultPreferencesForTest())

        val rescheduler = PrayerRescheduler(preferences, scheduler, repository)

        assertThat(rescheduler.rescheduleToday()).isTrue()

        coVerify(exactly = 0) { repository.markUnrecordedAsMissed(any(), any()) }
        coVerify(exactly = 1) { scheduler.scheduleTodaysPrayerNotifications(any(), any(), any(), any(), any(), any(), any()) }
    }
}
```

> **Two things to resolve against the real code before running this:**
> 1. `defaultPreferencesForTest()` — use whatever `UserPreferences` fixture the repo already has (search `app/src/testDebug` for an existing one; ViewModel tests almost certainly build one). If none exists, construct `UserPreferences()` directly if it has full defaults.
> 2. The `scheduleTodaysPrayerNotifications` `any()` count must match its real arity — read `PrayerNotificationScheduler` and adjust.
>
> The assertion that matters and must not be weakened is `coVerify(exactly = 0) { repository.markUnrecordedAsMissed(any(), any()) }`.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests 'com.arshadshah.nimaz.core.util.PrayerReschedulerTest'`
Expected: FAIL — compilation error on `rescheduleToday()` taking no argument is fine too; either way it is red.

- [ ] **Step 3: Remove the parameter from `PrayerRescheduler`**

In `PrayerRescheduler.kt`, delete the `prayerRepository` marking block and the parameter. The method becomes:

```kotlin
    /**
     * Re-arm today's notifications.
     *
     * It used to take a `markPastAsMissed` flag, true on a date change, which rewrote every
     * prayer the user had not logged into a `missed` record — and those records were what the
     * qada list read. Confirming a prayer missed is now something only the user does, from the
     * tracker's review banner. Rescheduling and record-keeping are separate concerns and this
     * only does the first.
     *
     * @return true when the reschedule completed. Failures are reported and swallowed — a
     *   receiver has nowhere to propagate to, and crashing on boot is worse than one missed
     *   re-arm — so the return value exists for tests and callers that want to know.
     */
    suspend fun rescheduleToday(): Boolean = try {
        val prefs = preferences.userPreferences.first()
        // … the existing scheduler.scheduleTodaysPrayerNotifications(…) call, unchanged …
```

Then remove the now-unused `prayerRepository` constructor parameter **only if nothing else in the class uses it** — check first; if it is unused, drop it and also drop the `PrayerRepository` import.

Update the class KDoc's second paragraph, which currently explains the two-copies history in terms of the `markPastPrayersAsMissed()` call, so it no longer describes code that is gone.

- [ ] **Step 4: Update `BootReceiver`**

At `BootReceiver.kt:103` and `:114`, both call sites become `prayerRescheduler.rescheduleToday()`. The two private methods `reschedulePrayerNotifications()` and `markMissedPrayersAndReschedule()` are now identical, so collapse them into one — keep `reschedulePrayerNotifications()` and point the `ACTION_MIDNIGHT_RESCHEDULE` branch at it. Update the KDoc on the surviving method to say the midnight chain re-arms and no longer marks.

- [ ] **Step 5: Run the test and compile**

Run: `./gradlew :app:testDebugUnitTest --tests 'com.arshadshah.nimaz.core.util.PrayerReschedulerTest'`
Expected: PASS.

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL — this closes the failure Task 6 left open.

- [ ] **Step 6: Update `docs/SUBSYSTEMS.md`**

Two places (§4, around lines 416 and 432):

- The mermaid sequence line `BR->>Sched: mark missed prayers + reschedule` becomes `BR->>Sched: reschedule`.
- The **Firing** prose `ACTION_MIDNIGHT_RESCHEDULE → mark missed prayers + reschedule (self-perpetuating daily chain)` becomes `ACTION_MIDNIGHT_RESCHEDULE → reschedule (self-perpetuating daily chain)`.

Add a sentence to that paragraph recording why:

> The midnight chain used to also rewrite every unlogged past prayer to `missed`. It no longer does: a prayer nobody logged is not a prayer the user missed, and those rows fed the qada list. Confirming them is an explicit action in the prayer tracker.

- [ ] **Step 7: Validate the diagram and docs**

```bash
npm install --no-save mermaid jsdom && node scripts/check_mermaid.mjs
python3 scripts/check_docs.py
```
Expected: both pass.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/data/local/database/dao/PrayerDao.kt \
        app/src/main/java/com/arshadshah/nimaz/domain/repository/PrayerRepository.kt \
        app/src/main/java/com/arshadshah/nimaz/data/repository/PrayerRepositoryImpl.kt \
        app/src/main/java/com/arshadshah/nimaz/domain/usecase/PrayerUseCases.kt \
        app/src/main/java/com/arshadshah/nimaz/core/di/RepositoryModule.kt \
        app/src/main/java/com/arshadshah/nimaz/core/util/PrayerRescheduler.kt \
        app/src/main/java/com/arshadshah/nimaz/core/util/BootReceiver.kt \
        app/src/testDebug/java/com/arshadshah/nimaz/core/util/PrayerReschedulerTest.kt \
        docs/SUBSYSTEMS.md
git commit -m "fix(prayer): stop deciding at midnight that you missed a prayer

A broadcast rewrote every unlogged past prayer to 'missed' every night, and
the qada list read exactly those rows -- so a user who had not opened the app
was shown make-up prayers they had never been asked about. Rescheduling now
only reschedules; the bulk update survives as a ranged call with one caller,
the tracker's review banner, behind an explicit tap."
```

---

# Phase 3 — ViewModel

---

### Task 8: `SetPrayerStatus` and `ConfirmUnrecordedAsMissed`

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/tracker/PrayerTrackerEvent.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/tracker/PrayerTrackerViewModel.kt:107-125` (the `onEvent` branches) and `:210-217` (the private mark methods)
- Test: `app/src/testDebug/java/com/arshadshah/nimaz/presentation/viewmodel/tracker/PrayerTrackerViewModelTest.kt` (extend if present, create if not)

**Interfaces:**
- Consumes: `PrayerUseCases.markUnrecordedAsMissed` (Task 6), `TRACKED_PRAYERS` (Task 5).
- Produces: `PrayerTrackerEvent.SetPrayerStatus(prayerName, status: PrayerStatus?)` and `PrayerTrackerEvent.ConfirmUnrecordedAsMissed(from: LocalDate, to: LocalDate)`. Task 10 emits both.
- **Removes:** `MarkPrayerPrayed`, `MarkPrayerMissed`.

- [ ] **Step 1: Write the failing test**

Add to `PrayerTrackerViewModelTest.kt` (match the file's existing fixture and dispatcher-rule style — read it first; if the file does not exist, model it on another ViewModel test in the same directory):

```kotlin
    @Test
    fun `SetPrayerStatus writes the status the user chose`() = runTest {
        viewModel.onEvent(
            PrayerTrackerEvent.SetPrayerStatus(PrayerName.ASR, PrayerStatus.LATE)
        )
        advanceUntilIdle()

        coVerify {
            updatePrayerStatusUseCase.invoke(any(), PrayerName.ASR, PrayerStatus.LATE, any(), false)
        }
    }

    @Test
    fun `SetPrayerStatus with a null status clears the record back to unrecorded`() = runTest {
        viewModel.onEvent(PrayerTrackerEvent.SetPrayerStatus(PrayerName.ASR, null))
        advanceUntilIdle()

        // Clearing is a write of NOT_PRAYED, not a delete: the derivation treats NOT_PRAYED as
        // absence, so the row reads back as "not recorded" with no new DAO method.
        coVerify {
            updatePrayerStatusUseCase.invoke(
                any(), PrayerName.ASR, PrayerStatus.NOT_PRAYED, null, false
            )
        }
    }

    @Test
    fun `SetPrayerStatus stamps prayedAt only for a fulfilled prayer`() = runTest {
        viewModel.onEvent(
            PrayerTrackerEvent.SetPrayerStatus(PrayerName.FAJR, PrayerStatus.MISSED)
        )
        advanceUntilIdle()

        coVerify {
            updatePrayerStatusUseCase.invoke(any(), PrayerName.FAJR, PrayerStatus.MISSED, null, false)
        }
    }

    @Test
    fun `ConfirmUnrecordedAsMissed passes the inclusive range it was given`() = runTest {
        val from = LocalDate.of(2026, 8, 6)
        val to = LocalDate.of(2026, 8, 12)

        viewModel.onEvent(PrayerTrackerEvent.ConfirmUnrecordedAsMissed(from, to))
        advanceUntilIdle()

        coVerify {
            markUnrecordedAsMissedUseCase.invoke(
                from.toUtcMidnightMillis(), to.toUtcMidnightMillis()
            )
        }
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests 'com.arshadshah.nimaz.presentation.viewmodel.tracker.PrayerTrackerViewModelTest'`
Expected: FAIL — `Unresolved reference: SetPrayerStatus`.

- [ ] **Step 3: Change the event vocabulary**

In `PrayerTrackerEvent.kt`, delete `MarkPrayerPrayed` and `MarkPrayerMissed` and add:

```kotlin
    /**
     * Set — or clear — one prayer's status on the selected date.
     *
     * Replaces the old prayed/missed pair, which between them could express only two of the four
     * states the app already stored and displayed: `LATE` and `QADA` were renderable but not
     * settable from anywhere in the UI.
     *
     * @param status the user's assertion, or `null` to withdraw it. Clearing writes
     *   [PrayerStatus.NOT_PRAYED], which the display derivation reads as "not recorded".
     */
    data class SetPrayerStatus(
        val prayerName: PrayerName,
        val status: PrayerStatus?,
    ) : PrayerTrackerEvent

    /**
     * Confirm every unrecorded prayer in an inclusive date range as missed.
     *
     * The only path into the qada list. Raised by the tracker's review banner.
     */
    data class ConfirmUnrecordedAsMissed(
        val from: LocalDate,
        val to: LocalDate,
    ) : PrayerTrackerEvent
```

- [ ] **Step 4: Handle them in the ViewModel**

In `onEvent`, replace the `MarkPrayerPrayed` and `MarkPrayerMissed` branches with:

```kotlin
            is PrayerTrackerEvent.SetPrayerStatus -> {
                telemetry.prayerTracked(
                    event.prayerName.name,
                    event.status?.name ?: "cleared",
                    false
                )
                setPrayerStatus(event.prayerName, event.status)
            }

            is PrayerTrackerEvent.ConfirmUnrecordedAsMissed -> {
                telemetry.featureUsed(DOMAIN, "confirm_unrecorded_missed")
                confirmUnrecordedAsMissed(event.from, event.to)
            }
```

Replace the two private `markPrayerPrayed` / `markPrayerMissed` methods with:

```kotlin
    private fun setPrayerStatus(prayerName: PrayerName, status: PrayerStatus?) {
        // Clearing is a write of NOT_PRAYED rather than a delete. The display derivation treats
        // NOT_PRAYED as absence, so the row reads back as "not recorded" — and a status the user
        // withdrew leaving no row at all would be indistinguishable from one never touched,
        // which is a distinction sync and the widget both care about.
        updatePrayerStatus(prayerName, status ?: PrayerStatus.NOT_PRAYED, isJamaah = false)
    }

    private fun confirmUnrecordedAsMissed(from: LocalDate, to: LocalDate) {
        launchSafely(telemetry, DOMAIN, "confirm_unrecorded_missed") {
            prayerUseCases.markUnrecordedAsMissed(
                from.toUtcMidnightMillis(),
                to.toUtcMidnightMillis(),
            )
            // No reload. The qada list, the stats and the selected day are all Room-backed
            // observers of this table, so the write re-emits to every one of them.
        }
    }
```

`updatePrayerStatus` already computes `prayedAt` as non-null only for `PRAYED`/`LATE`, so `MISSED`, `QADA` and `NOT_PRAYED` all pass `null` without further change.

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests 'com.arshadshah.nimaz.presentation.viewmodel.tracker.PrayerTrackerViewModelTest'`
Expected: PASS.

Run: `./gradlew :app:compileDebugKotlin`
Expected: **FAIL** at `PrayerTrackerScreen.kt` — it still emits the deleted events. Task 10 fixes it. Do not patch the old screen to compile; it is about to be replaced.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/tracker/ \
        app/src/testDebug/java/com/arshadshah/nimaz/presentation/viewmodel/tracker/PrayerTrackerViewModelTest.kt
git commit -m "feat(tracker): one event that can set any status, including none

MarkPrayerPrayed and MarkPrayerMissed between them reached two of the four
states the app already stored and rendered -- LATE and QADA were displayable
and unreachable. Adds the explicit range-scoped confirmation that is now the
only way a prayer enters the qada list."
```

---

# Phase 4 — Screens

---

### Task 9: `QadaPrayersScreen`

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/prayer/QadaPrayersScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `PrayerTrackerViewModel.qadaState`, `PrayerTrackerEvent.MarkQadaCompleted`, `NimazQadaPrayerItem`.
- Produces: `QadaPrayersScreen(onNavigateBack: () -> Unit, viewModel: PrayerTrackerViewModel = hiltViewModel())`. Task 11 wires it.

- [ ] **Step 1: Add the strings**

In `app/src/main/res/values/strings.xml`:

```xml
    <string name="qada_prayers_title">Qada prayers</string>
    <string name="qada_outstanding">Outstanding</string>
    <string name="qada_count_format">%1$d prayers</string>
    <string name="qada_empty_title">Nothing to make up</string>
    <string name="qada_empty_body">Prayers you mark as missed appear here. Nothing is added on your behalf.</string>
    <string name="qada_mark_made_up">Mark made up</string>
```

Reuse `R.string.qada` if it already exists rather than adding a duplicate — check first.

- [ ] **Step 2: Write the screen**

Create `app/src/main/java/com/arshadshah/nimaz/presentation/screens/prayer/QadaPrayersScreen.kt`:

```kotlin
package com.arshadshah.nimaz.presentation.screens.prayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.molecules.NimazEmptyState
import com.arshadshah.nimaz.presentation.components.molecules.NimazQadaPrayerItem
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.tracker.PrayerTrackerEvent
import com.arshadshah.nimaz.presentation.viewmodel.tracker.PrayerTrackerViewModel

/**
 * The make-up list: every prayer the user has **explicitly** marked missed.
 *
 * A pushed destination rather than a tab, and it shares [PrayerTrackerViewModel] rather than
 * owning one: the qada state, its event and its use cases already live there, and a second
 * ViewModel would mean a second collector on the same Room flow for no gain.
 */
@Composable
fun QadaPrayersScreen(
    onNavigateBack: () -> Unit,
    viewModel: PrayerTrackerViewModel = hiltViewModel(),
) {
    val qadaState by viewModel.qadaState.collectAsStateWithLifecycle()

    NimazScreenScaffold(
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.qada_prayers_title),
                onBackClick = onNavigateBack,
            )
        }
    ) { paddingValues ->
        if (qadaState.missedPrayers.isEmpty()) {
            NimazEmptyState(
                icon = Icons.Default.Restore,
                title = stringResource(R.string.qada_empty_title),
                description = stringResource(R.string.qada_empty_body),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    NimazSectionHeader(
                        title = stringResource(R.string.qada_outstanding),
                        trailingText = stringResource(
                            R.string.qada_count_format,
                            qadaState.missedPrayers.size
                        ),
                    )
                }
                items(qadaState.missedPrayers, key = { "${it.date}-${it.prayerName}" }) { prayer ->
                    NimazQadaPrayerItem(
                        prayer = prayer,
                        actionText = stringResource(R.string.qada_mark_made_up),
                        onMarkCompleted = {
                            viewModel.onEvent(PrayerTrackerEvent.MarkQadaCompleted(prayer))
                        },
                    )
                }
            }
        }
    }
}
```

Add `import androidx.compose.foundation.layout.padding`.

> **Check the real signatures before compiling:** `NimazEmptyState` and `NimazSectionHeader` parameter names (`icon`/`title`/`description`, `trailingText`) must match the components as they exist. Read them and adjust the call, not the components.

- [ ] **Step 3: Compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: still FAIL at the *old* `PrayerTrackerScreen.kt` only (Task 8's open failure). Confirm no new error names `QadaPrayersScreen.kt`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/screens/prayer/QadaPrayersScreen.kt \
        app/src/main/res/values/strings.xml
git commit -m "feat(prayer): a real qada screen instead of a redirect

Route.QadaPrayers existed, was tagged, and rendered the prayer tracker with
tab index 1 -- NAVIGATION.md has claimed a QadaPrayersScreen for some time.
Now there is one. The empty state says what puts a prayer here, because
nothing does it on the user's behalf any more."
```

---

### Task 10: The tracker screen

**Files:**
- Rewrite: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/prayer/PrayerTrackerScreen.kt`
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/prayer/PrayerTrackerDayCard.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: everything produced by Tasks 1, 2, 3, 5, 8.
- Produces: `PrayerTrackerScreen(onNavigateBack: () -> Unit, onNavigateToStats: () -> Unit, onNavigateToQada: () -> Unit, viewModel: PrayerTrackerViewModel = hiltViewModel())` — note `initialTab` is gone and `onNavigateToQada` is new. Task 11 wires it.

- [ ] **Step 1: Add the strings**

```xml
    <string name="prayer_status_not_recorded">Not recorded</string>
    <string name="prayer_status_on_time">On time</string>
    <string name="prayer_recorded_count_format">%1$d of %2$d recorded</string>
    <string name="prayer_streak_format">%1$d-day streak</string>
    <string name="back_to_today">Back to today</string>
    <string name="prayer_not_recorded_note">This prayer\'s time has passed with nothing recorded. It isn\'t counted as missed until you say so.</string>
    <string name="prayer_unrecorded_banner_format">%1$d prayers from the past week have no record. They aren\'t in your qada list until you mark them.</string>
    <string name="prayer_unrecorded_banner_action">Review</string>
    <string name="prayer_month_section">Your month</string>
    <string name="prayer_complete_days_format">%1$d complete days</string>
    <string name="prayer_legend_all_five">All five</string>
    <string name="prayer_legend_some">Some</string>
    <string name="prayer_legend_none">None</string>
    <string name="prayer_legend_not_recorded">Not recorded</string>
    <string name="qada_summary_title">Qada prayers</string>
    <string name="qada_summary_subtitle">Missed prayers waiting to be made up</string>
    <string name="qada_summary_empty">Nothing outstanding</string>
```

Reuse `R.string.late`, `R.string.missed`, `R.string.made_up`, `R.string.upcoming` and `R.string.on_time` — all already exist (the old screen used them). Drop `prayer_status_on_time` above if `on_time` is present.

- [ ] **Step 2: Write the day card**

Create `app/src/main/java/com/arshadshah/nimaz/presentation/screens/prayer/PrayerTrackerDayCard.kt`. It holds the day card and its rows so the screen file stays a layout of sections rather than a 600-line file with a component library inside it.

```kotlin
package com.arshadshah.nimaz.presentation.screens.prayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.util.formatClock
import com.arshadshah.nimaz.core.util.formatFullDate
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerTimes
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeEmphasis
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazDivider
import com.arshadshah.nimaz.presentation.components.atoms.NimazSegmentedControl
import com.arshadshah.nimaz.presentation.components.atoms.NimazSegmentedOption
import com.arshadshah.nimaz.presentation.components.atoms.NimazStatusDotSpec
import com.arshadshah.nimaz.presentation.components.atoms.NimazTimelineNode
import com.arshadshah.nimaz.presentation.components.atoms.NimazTimelineTrack
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.molecules.NimazAccordion
import com.arshadshah.nimaz.presentation.components.molecules.NimazAccordionStyle
import com.arshadshah.nimaz.presentation.theme.LocalUse24HourFormat
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/** The four assertions a user can make, in picker order. */
private val PICKER_STATUSES = listOf(
    PrayerStatus.PRAYED,
    PrayerStatus.LATE,
    PrayerStatus.MISSED,
    PrayerStatus.QADA,
)

/**
 * The selected day: what its schedule was, how far through it you are, and what you logged.
 *
 * @param expandedPrayer the one open row, or `null`. Hoisted so exactly one can be open — see
 *   [NimazAccordion]'s hoisted overload.
 */
@Composable
fun PrayerTrackerDayCard(
    selectedDate: LocalDate,
    statuses: Map<PrayerName, PrayerDisplayStatus>,
    times: PrayerTimes?,
    now: LocalDateTime,
    streak: Int,
    expandedPrayer: PrayerName?,
    onExpandedChange: (PrayerName?) -> Unit,
    onSetStatus: (PrayerName, PrayerStatus?) -> Unit,
    onBackToToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val use24Hour = LocalUse24HourFormat.current
    val isToday = selectedDate == now.toLocalDate()
    val doneCount = TRACKED_PRAYERS.count { statuses[it]?.isDone() == true }

    NimazCard(style = NimazCardStyle.FILLED, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(top = 18.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedDate.formatFullDate(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (streak > 0) {
                            NimazBadge(
                                text = stringResource(R.string.prayer_streak_format, streak),
                                icon = Icons.Default.LocalFireDepartment,
                                size = NimazBadgeSize.SMALL,
                                colors = NimazBadgeDefaults.colors(
                                    tone = NimazTone.WARNING,
                                    emphasis = NimazBadgeEmphasis.SOFT,
                                ),
                            )
                        }
                        Text(
                            text = stringResource(
                                R.string.prayer_recorded_count_format,
                                doneCount,
                                TRACKED_PRAYERS.size,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (!isToday) {
                    NimazButton(
                        text = stringResource(R.string.back_to_today),
                        onClick = onBackToToday,
                        variant = NimazButtonVariant.TERTIARY,
                        size = NimazButtonSize.SMALL,
                    )
                }
            }

            DayTimeline(
                statuses = statuses,
                times = times,
                now = now,
                isToday = isToday,
                use24Hour = use24Hour,
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp),
            )

            NimazDivider(modifier = Modifier.padding(top = 16.dp))

            TRACKED_PRAYERS.forEachIndexed { index, prayer ->
                if (index > 0) NimazDivider()
                val status = statuses[prayer] ?: PrayerDisplayStatus.UPCOMING
                PrayerRow(
                    prayer = prayer,
                    status = status,
                    time = times?.timeFor(prayer)?.formatClock(use24Hour),
                    canBeMadeUp = status != PrayerDisplayStatus.UPCOMING,
                    expanded = expandedPrayer == prayer,
                    onExpandedChange = { open -> onExpandedChange(if (open) prayer else null) },
                    onSetStatus = { newStatus -> onSetStatus(prayer, newStatus) },
                )
            }
        }
    }
}

@Composable
private fun DayTimeline(
    statuses: Map<PrayerName, PrayerDisplayStatus>,
    times: PrayerTimes?,
    now: LocalDateTime,
    isToday: Boolean,
    use24Hour: Boolean,
    modifier: Modifier = Modifier,
) {
    if (times == null) return

    val start = times.fajr
    val end = times.isha
    val span = Duration.between(start, end).toMinutes().toFloat()

    // A day whose Isha is not after its Fajr is not a day this can draw. It happens at extreme
    // latitudes and after a bad location fix, and the atom would clamp every node onto the same
    // point -- five dots in a stack reads as one dot, which is worse than no timeline.
    if (span <= 0f) return

    fun positionOf(at: LocalDateTime) =
        Duration.between(start, at).toMinutes().toFloat() / span

    val nodes = TRACKED_PRAYERS.mapNotNull { prayer ->
        val at = times.timeFor(prayer) ?: return@mapNotNull null
        val status = statuses[prayer] ?: PrayerDisplayStatus.UPCOMING
        NimazTimelineNode(
            position = positionOf(at),
            spec = NimazStatusDotSpec(status.tone(), status.dotStyle()),
            label = prayer.displayName(),
        )
    }

    NimazTimelineTrack(
        nodes = nodes,
        startLabel = "${PrayerName.FAJR.displayName()} ${start.formatClock(use24Hour)}",
        endLabel = "${PrayerName.ISHA.displayName()} ${end.formatClock(use24Hour)}",
        progress = if (isToday) positionOf(now) else null,
        modifier = modifier,
    )
}

@Composable
private fun PrayerRow(
    prayer: PrayerName,
    status: PrayerDisplayStatus,
    time: String?,
    canBeMadeUp: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSetStatus: (PrayerStatus?) -> Unit,
) {
    val options = PICKER_STATUSES
        .filter { canBeMadeUp || it != PrayerStatus.QADA }

    NimazAccordion(
        title = prayer.displayName(),
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        subtitle = time,
        style = NimazAccordionStyle.FLAT,
        trailing = {
            NimazBadge(
                text = status.label(),
                size = NimazBadgeSize.LARGE,
                colors = NimazBadgeDefaults.colors(
                    tone = status.tone(),
                    emphasis = if (status == PrayerDisplayStatus.NOT_RECORDED) {
                        NimazBadgeEmphasis.OUTLINED
                    } else {
                        NimazBadgeEmphasis.SOFT
                    },
                ),
            )
        },
    ) {
        NimazSegmentedControl(
            options = options.map { candidate ->
                NimazSegmentedOption(
                    label = candidate.pickerLabel(),
                    selectedTone = candidate.displayed().tone(),
                )
            },
            selectedIndex = options.indexOfFirst { it.displayed() == status }.takeIf { it >= 0 },
            // The control reports a tap even on the selected cell, which is how tap-to-clear
            // reaches us: choosing what you already chose withdraws the assertion.
            onSelect = { index ->
                val chosen = options[index]
                onSetStatus(if (chosen.displayed() == status) null else chosen)
            },
        )
        if (status == PrayerDisplayStatus.NOT_RECORDED) {
            Text(
                text = stringResource(R.string.prayer_not_recorded_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

/** The display status a stored status maps to, for matching the picker against the row. */
private fun PrayerStatus.displayed(): PrayerDisplayStatus = when (this) {
    PrayerStatus.PRAYED -> PrayerDisplayStatus.PRAYED
    PrayerStatus.LATE -> PrayerDisplayStatus.LATE
    PrayerStatus.MISSED -> PrayerDisplayStatus.MISSED
    PrayerStatus.QADA -> PrayerDisplayStatus.QADA
    PrayerStatus.PENDING, PrayerStatus.NOT_PRAYED -> PrayerDisplayStatus.NOT_RECORDED
}

@Composable
private fun PrayerStatus.pickerLabel(): String = when (this) {
    PrayerStatus.PRAYED -> stringResource(R.string.on_time)
    PrayerStatus.LATE -> stringResource(R.string.late)
    PrayerStatus.MISSED -> stringResource(R.string.missed)
    PrayerStatus.QADA -> stringResource(R.string.made_up)
    PrayerStatus.PENDING, PrayerStatus.NOT_PRAYED -> stringResource(R.string.prayer_status_not_recorded)
}

@Composable
private fun PrayerDisplayStatus.label(): String = when (this) {
    PrayerDisplayStatus.PRAYED -> stringResource(R.string.on_time)
    PrayerDisplayStatus.LATE -> stringResource(R.string.late)
    PrayerDisplayStatus.QADA -> stringResource(R.string.made_up)
    PrayerDisplayStatus.MISSED -> stringResource(R.string.missed)
    PrayerDisplayStatus.NOT_RECORDED -> stringResource(R.string.prayer_status_not_recorded)
    PrayerDisplayStatus.UPCOMING -> stringResource(R.string.upcoming)
}
```

> **Check `NimazBadge`'s parameter names** (`icon`, `colors`, `size`) and `NimazButton`'s (`text`, `variant`, `size`) against the real components before compiling. Adjust the calls, never the components.

- [ ] **Step 3: Rewrite the screen**

Replace `PrayerTrackerScreen.kt` entirely:

```kotlin
package com.arshadshah.nimaz.presentation.screens.prayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.util.toUtcMidnightMillis
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBanner
import com.arshadshah.nimaz.presentation.components.atoms.NimazBannerVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazDayRail
import com.arshadshah.nimaz.presentation.components.atoms.NimazDayRailItem
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.atoms.NimazStatusDotSpec
import com.arshadshah.nimaz.presentation.components.atoms.TickResolution
import com.arshadshah.nimaz.presentation.components.atoms.rememberNow
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuItem
import com.arshadshah.nimaz.presentation.components.molecules.calendar.CalendarDayState
import com.arshadshah.nimaz.presentation.components.molecules.calendar.NimazCalendar
import com.arshadshah.nimaz.presentation.components.molecules.calendar.SelectionStyle
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.tracker.PrayerTrackerEvent
import com.arshadshah.nimaz.presentation.viewmodel.tracker.PrayerTrackerViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/** How far back the review banner looks. One week is a period a user can actually remember. */
private const val REVIEW_WINDOW_DAYS = 7L

@Composable
fun PrayerTrackerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToQada: () -> Unit,
    viewModel: PrayerTrackerViewModel = hiltViewModel(),
) {
    val state by viewModel.trackerState.collectAsStateWithLifecycle()
    val statsState by viewModel.statsState.collectAsStateWithLifecycle()
    val historyState by viewModel.historyState.collectAsStateWithLifecycle()
    val qadaState by viewModel.qadaState.collectAsStateWithLifecycle()

    // Read the clock through the shared ticker rather than calling LocalDateTime.now() directly:
    // a bare now() is not observable state, so the "passed / not recorded" flag would only flip
    // when something *else* happened to recompose this screen -- sit on the tracker as a prayer
    // time arrives and nothing would change. Minute resolution is the granularity of the decision.
    val nowInstant by rememberNow(TickResolution.MINUTES)
    val now = remember(nowInstant) {
        LocalDateTime.ofInstant(
            Instant.ofEpochMilli(nowInstant.toEpochMilliseconds()),
            ZoneId.systemDefault(),
        )
    }
    val today = now.toLocalDate()

    var displayedMonth by remember(state.selectedDate) {
        mutableStateOf(YearMonth.from(state.selectedDate))
    }
    var expandedPrayer by rememberSaveable { mutableStateOf<PrayerName?>(null) }

    LaunchedEffect(displayedMonth) {
        viewModel.onEvent(
            PrayerTrackerEvent.LoadHistory(
                displayedMonth.atDay(1).minusDays(REVIEW_WINDOW_DAYS),
                displayedMonth.atEndOfMonth(),
            )
        )
    }

    val recordsByDate = remember(historyState.records) {
        historyState.records.groupBy { it.date }
    }

    fun statusesOn(date: LocalDate) = resolvePrayerStatuses(
        records = recordsByDate[date.toUtcMidnightMillis()].orEmpty(),
        // The month view has records but not schedules, so a past day resolves from the day
        // being over. Only the selected day gets real times, which is the only day that needs
        // per-prayer precision.
        times = if (date == state.selectedDate) state.prayerTimes else null,
        date = date,
        now = now,
    )

    val unrecordedCount = remember(recordsByDate, now) {
        (1..REVIEW_WINDOW_DAYS).sumOf { back ->
            statusesOn(today.minusDays(back))
                .values.count { it == PrayerDisplayStatus.NOT_RECORDED }
        }
    }

    NimazScreenScaffold(
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.prayer_tracker_title),
                onBackClick = onNavigateBack,
                actions = {
                    NimazIconButton(
                        onClick = onNavigateToStats,
                        icon = Icons.Default.BarChart,
                        contentDescription = stringResource(R.string.view_statistics),
                    )
                },
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                WeekRail(
                    selectedDate = state.selectedDate,
                    today = today,
                    statusesOn = ::statusesOn,
                    onSelect = { date ->
                        expandedPrayer = null
                        viewModel.onEvent(PrayerTrackerEvent.SelectDate(date))
                    },
                )
            }

            item {
                PrayerTrackerDayCard(
                    selectedDate = state.selectedDate,
                    statuses = statusesOn(state.selectedDate),
                    times = state.prayerTimes,
                    now = now,
                    streak = statsState.currentStreak,
                    expandedPrayer = expandedPrayer,
                    onExpandedChange = { expandedPrayer = it },
                    onSetStatus = { prayer, status ->
                        expandedPrayer = null
                        viewModel.onEvent(PrayerTrackerEvent.SetPrayerStatus(prayer, status))
                    },
                    onBackToToday = {
                        expandedPrayer = null
                        viewModel.onEvent(PrayerTrackerEvent.SelectDate(today))
                    },
                )
            }

            if (unrecordedCount > 0) {
                item {
                    NimazBanner(
                        message = stringResource(
                            R.string.prayer_unrecorded_banner_format,
                            unrecordedCount,
                        ),
                        variant = NimazBannerVariant.WARNING,
                        actionLabel = stringResource(R.string.prayer_unrecorded_banner_action),
                        onAction = {
                            viewModel.onEvent(
                                PrayerTrackerEvent.ConfirmUnrecordedAsMissed(
                                    from = today.minusDays(REVIEW_WINDOW_DAYS),
                                    to = today.minusDays(1),
                                )
                            )
                        },
                    )
                }
            }

            item {
                MonthSection(
                    displayedMonth = displayedMonth,
                    selectedDate = state.selectedDate,
                    today = today,
                    statusesOn = ::statusesOn,
                    onMonthChange = { displayedMonth = it },
                    onDateSelected = { date ->
                        expandedPrayer = null
                        viewModel.onEvent(PrayerTrackerEvent.SelectDate(date))
                    },
                )
            }

            item {
                NimazMenuItem(
                    title = stringResource(R.string.qada_summary_title),
                    subtitle = if (qadaState.missedPrayers.isEmpty()) {
                        stringResource(R.string.qada_summary_empty)
                    } else {
                        stringResource(R.string.qada_summary_subtitle)
                    },
                    icon = Icons.Default.Restore,
                    onClick = onNavigateToQada,
                    trailing = {
                        if (qadaState.missedPrayers.isNotEmpty()) {
                            NimazBadge(text = qadaState.missedPrayers.size.toString())
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun WeekRail(
    selectedDate: LocalDate,
    today: LocalDate,
    statusesOn: (LocalDate) -> Map<PrayerName, PrayerDisplayStatus>,
    onSelect: (LocalDate) -> Unit,
) {
    val weekStart = remember(selectedDate) {
        selectedDate.minusDays((selectedDate.dayOfWeek.value - 1).toLong())
    }
    val days = remember(weekStart, today) { (0L..6L).map { weekStart.plusDays(it) } }

    NimazDayRail(
        days = days.map { date ->
            val statuses = statusesOn(date)
            NimazDayRailItem(
                weekdayLabel = date.dayOfWeek
                    .getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                dayLabel = date.dayOfMonth.toString(),
                marker = if (date.isAfter(today)) null else statuses.railMarker(),
                isToday = date == today,
                enabled = !date.isAfter(today),
                contentDescription = date.toString(),
            )
        },
        selectedIndex = days.indexOf(selectedDate).takeIf { it >= 0 },
        onSelect = { onSelect(days[it]) },
    )
}

@Composable
private fun MonthSection(
    displayedMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    statusesOn: (LocalDate) -> Map<PrayerName, PrayerDisplayStatus>,
    onMonthChange: (YearMonth) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val completeDays = remember(displayedMonth, today) {
        (1..displayedMonth.lengthOfMonth())
            .map(displayedMonth::atDay)
            .filter { !it.isAfter(today) }
            .count { date -> statusesOn(date).values.count { it.isDone() } == TRACKED_PRAYERS.size }
    }

    NimazSectionHeader(
        title = stringResource(R.string.prayer_month_section),
        trailingText = stringResource(R.string.prayer_complete_days_format, completeDays),
    )

    NimazCalendar(
        displayedMonth = displayedMonth,
        selectedDate = selectedDate,
        onDateSelected = onDateSelected,
        onPreviousMonth = { onMonthChange(displayedMonth.minusMonths(1)) },
        onNextMonth = { onMonthChange(displayedMonth.plusMonths(1)) },
        selectionStyle = SelectionStyle.BORDER,
        dayStateProvider = { date ->
            if (date.isAfter(today)) return@NimazCalendar CalendarDayState()
            val statuses = statusesOn(date)
            val done = statuses.values.count { it.isDone() }
            CalendarDayState(
                indicatorBar = done.toFloat() / TRACKED_PRAYERS.size,
                indicatorBarColor = when {
                    done == TRACKED_PRAYERS.size -> NimazColors.StatusColors.Prayed
                    done > 0 -> NimazColors.StatusColors.Partial
                    statuses.values.any { it == PrayerDisplayStatus.MISSED } ->
                        NimazColors.StatusColors.Missed
                    else -> MaterialTheme.colorScheme.outlineVariant
                },
            )
        },
    )
}

/** One dot summarising a whole day for the week rail. */
private fun Map<PrayerName, PrayerDisplayStatus>.railMarker(): NimazStatusDotSpec {
    val done = values.count { it.isDone() }
    val allUnrecorded = values.all { it == PrayerDisplayStatus.NOT_RECORDED }
    return when {
        done == TRACKED_PRAYERS.size -> NimazStatusDotSpec(NimazTone.SUCCESS)
        // A day nobody touched gets a ring, not a red dot -- the whole point of the redesign is
        // that "no record" and "you missed these" are different claims.
        allUnrecorded -> NimazStatusDotSpec(NimazTone.WARNING, NimazStatusDotStyle.OUTLINED)
        done > 0 -> NimazStatusDotSpec(NimazTone.WARNING)
        else -> NimazStatusDotSpec(NimazTone.ERROR)
    }
}
```

Add imports `androidx.compose.foundation.layout.padding`, `com.arshadshah.nimaz.presentation.components.atoms.NimazTone`, and `com.arshadshah.nimaz.presentation.components.atoms.NimazStatusDotStyle`.

> **`return@NimazCalendar` inside `dayStateProvider`** must match the lambda's actual label — if `dayStateProvider` is a named parameter of a non-inline lambda type, use `return@dayStateProvider` or restructure as an `if/else`. Compile and follow the error.
>
> **`NimazColors.StatusColors.*` are raw colours,** which rule 7 permits because they are `NimazColors.*` and not `Color(0xFF…)` literals in the screen. Keep them.

- [ ] **Step 4: Compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: FAIL only at `NavGraph.kt` — the screen's signature changed. Task 11 fixes it. Fix any error inside the two prayer files first.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/screens/prayer/PrayerTrackerScreen.kt \
        app/src/main/java/com/arshadshah/nimaz/presentation/screens/prayer/PrayerTrackerDayCard.kt \
        app/src/main/res/values/strings.xml
git commit -m "feat(tracker): one scroll that reports the day

Drops the tab row, the gradient streak hero and the hand-rolled check row.
The day now reads as a timeline with a status per prayer, each row opening
onto a picker that can finally reach all four states -- and a prayer whose
time passed unlogged says so plainly instead of accusing you."
```

---

# Phase 5 — Navigation, docs, gates

---

### Task 11: Wire the routes and close the docs

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/core/navigation/Routes.kt:115`
- Modify: `app/src/main/java/com/arshadshah/nimaz/core/navigation/NavGraph.kt:858-880`
- Modify: `app/src/main/java/com/arshadshah/nimaz/core/navigation/AnnouncementRoutes.kt:30,136`
- Modify: `app/src/main/java/com/arshadshah/nimaz/core/navigation/HelpDeepLink.kt:18`
- Modify: `docs/NAVIGATION.md:238,462`
- Modify: `docs/CLEAN_ARCHITECTURE_CHECKLIST.md` (AP-8)

- [ ] **Step 1: Make the route parameterless**

`Routes.kt:115`:

```kotlin
    data object PrayerTracker : Route
```

- [ ] **Step 2: Wire both destinations**

In `NavGraph.kt`, replace the two `taggedComposable` blocks:

```kotlin
            taggedComposable<Route.PrayerTracker>(ScreenTags.PrayerTracker) {
                PrayerTrackerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToStats = { navController.navigate(Route.PrayerStats) },
                    onNavigateToQada = { navController.navigate(Route.QadaPrayers) },
                )
            }
```

and

```kotlin
            taggedComposable<Route.QadaPrayers>(ScreenTags.QadaPrayers) {
                QadaPrayersScreen(onNavigateBack = { navController.popBackStack() })
            }
```

Delete the `// Redirect QadaPrayers to PrayerTracker with Qada tab selected` comment, drop the now-unused `toRoute<Route.PrayerTracker>()` line, and add the import for `QadaPrayersScreen`.

Fix `NavGraph.kt:336` — `Route.PrayerTracker()` becomes `Route.PrayerTracker`.

- [ ] **Step 3: Fix the announcement and deep-link grammars**

`AnnouncementRoutes.kt:30`: `"prayer/tracker" -> Route.PrayerTracker`

`AnnouncementRoutes.kt:136`: replace the `int(2, 0..10)?.let { Route.PrayerTracker(it) }` branch with:

```kotlin
            // The `{tab}` segment predates the tab row's removal. Shipped announcements still
            // carry it, so it keeps resolving: 1 meant the qada tab and is now its own screen.
            int(2, 0..10)?.let { tab ->
                if (tab == 1) Route.QadaPrayers else Route.PrayerTracker
            }
```

`HelpDeepLink.kt:18`: `"prayer_tracker" -> Route.PrayerTracker`

- [ ] **Step 4: Compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. Follow any remaining `Route.PrayerTracker()` call site the compiler names.

- [ ] **Step 5: Update `docs/NAVIGATION.md`**

Line 238 — the params column:

```
| `PrayerTracker` | — | PrayerTrackerScreen |
```

Line 462 — the announcement grammar row:

```
| `prayer/tracker/{tab}` | `PrayerTracker` / `QadaPrayers` | tab index 0–10, kept for shipped announcements written before the tab row was removed; `1` resolves to `QadaPrayers`, everything else to `PrayerTracker` |
```

Line 240 needs no edit — it already names `QadaPrayersScreen`, which now exists.

- [ ] **Step 6: Update `docs/CLEAN_ARCHITECTURE_CHECKLIST.md`**

In the AP-8 list, add a resolved entry in the same style as the `JumuahCard` one:

```markdown
- [x] ~~**`PrayerCheckItem`'s hand-rolled `Box` checkbox circle.**~~ **Resolved.** The prayer
  tracker's row drew its own 24dp `Box` with `.background()` / `.border()` and a raw `Check` icon.
  Replaced by `NimazAccordion(style = FLAT)` with a `NimazBadge` status and a
  `NimazSegmentedControl` picker, so the row's tap target and ripple come from the design system.
```

- [ ] **Step 7: Run every gate**

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebugAndroidTest
python3 scripts/check_docs.py
npm install --no-save mermaid jsdom && node scripts/check_mermaid.mjs
./gradlew :app:lintDebug
```

Expected: all pass. `lintDebug` is last because it takes ~10 minutes; it is CI-blocking and must not be skipped. If it reports `MissingTranslation` for the strings added in Tasks 9 and 10, add them to every shipped locale's `strings.xml`.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/core/navigation/ docs/
git commit -m "feat(nav): qada is a screen, and the tracker has no tabs to index

Route.QadaPrayers was tagged and documented as QadaPrayersScreen while
rendering the tracker at tab index 1; the doc is now true. PrayerTracker
loses initialTab, and the announcement grammar keeps accepting the segment
so announcements already in the wild still resolve."
```

---

## Self-Review

**Spec coverage**

| Spec section | Task |
|---|---|
| §1.1 derivation rule (incl. `PENDING`/`NOT_PRAYED` as absence) | 5 |
| §1.2(a) midnight auto-missing retired | 7 |
| §1.2(b) scoped `markUnrecordedAsMissed` | 6 |
| §1.2(c) qada unchanged | — (verified no-op; `PrayerDao:28` untouched) |
| §1.3 stats consequence | Accepted, documented; no code |
| §2.1 `NimazTimelineTrack` | 1 |
| §2.2 `NimazAccordion` | 2 |
| §2.3 `CalendarDayState` | 3 |
| §2.4 reuse table | 10 |
| §2.5 deletions | 10 |
| §3.1 tracker screen | 10 |
| §3.2 `QadaPrayersScreen` | 9 |
| §3.3 `PrayerDayStatus.kt` | 5 |
| §3.4 ViewModel events | 8 |
| §3.5 navigation | 11 |
| §4 tests | 1, 2, 3, 5, 7, 8, 11 |
| §5 docs | 7 (SUBSYSTEMS), 11 (NAVIGATION, CHECKLIST) |
| §7 phasing | Phase order; gate at Task 4 |
| §8 verification | Task 11 step 7 |

**Naming consistency checked across tasks:** `markUnrecordedAsMissed` (Tasks 6→8), `TRACKED_PRAYERS` / `resolvePrayerStatuses` / `isDone` / `tone` / `dotStyle` / `timeFor` (Task 5 → 9, 10), `NimazAccordionStyle.FLAT` (2→10), `indicatorBar` / `indicatorBarColor` (3→10), `NimazTimelineNode.safePosition` (1→1 test), `SetPrayerStatus` / `ConfirmUnrecordedAsMissed` (8→10), `onNavigateToQada` (10→11).

**Deliberate open questions left to the implementer**, each flagged inline where it occurs and each with a stated fallback: `NimazCard`'s transparent configuration (Task 2), the `UserPreferences` test fixture and `scheduleTodaysPrayerNotifications` arity (Task 7), `NimazEmptyState`/`NimazSectionHeader`/`NimazBadge`/`NimazButton` exact parameter names (Tasks 9, 10), and the `dayStateProvider` lambda label (Task 10). These are signature look-ups against code on the branch, not design decisions.
