# Fasting Screen Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the fasting screen as one uninterrupted scroll that reports the day — window, progress, status — on five new design-system atoms, with make-up fasts promoted to its own destination.

**Architecture:** Five new leaf atoms land first and are reviewed on their previews before any screen consumes them. The ViewModel then gains selected-day state (record, week, that day's prayer schedule). `MakeupFastsTab` becomes a real destination behind `Route.MakeupFasts`. Finally `FastTrackerScreen` is rebuilt against the new atoms, retiring the tab row, the "Go deeper" group and the heavyweight day sheet.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Hilt, Room, Robolectric + Truth for component tests, JUnit + Turbine-style coroutine tests for ViewModels.

**Spec:** `docs/superpowers/specs/2026-08-13-fasting-screen-redesign-design.md`

## Global Constraints

- **No `Color(0xFF…)` literals** in any new code — `MaterialTheme.colorScheme.*` or `NimazColors.*` only (CLAUDE.md rule 7).
- **Interactive UI comes from the design system.** Tappable cards use `NimazCard(onClick = …)`; buttons are `NimazButton`/`NimazIconButton`. A wrapping `Modifier.clickable` around a card is forbidden — it paints a sharp-cornered ripple over the card radius (CLAUDE.md rule 8). `.clickable` on *inner* elements is fine.
- **Layer rules:** domain never imports `data`; presentation never imports entities or DAOs; ViewModels inject `XxxUseCases`, never repositories; ViewModels expose `StateFlow<XxxUiState>` + a single `onEvent(event)`.
- **Strings:** every user-visible string is a `strings.xml` entry prefixed `fasting_`, read with `stringResource` inside composables — **never** `context.getString` (lint error `LocalContextGetResourceValueCall`). A new string absent from a shipped locale fails `lintDebug` with `MissingTranslation`.
- **No clock reads at composition.** "Today" comes from `TodayProvider` via the ViewModel; "now" comes from `rememberNow(TickResolution.MINUTES)` at the leaf. No `LocalDate.now()` in a composable.
- **Atom test harness:** Robolectric, `@RunWith(RobolectricTestRunner::class)`, `createComponentComposeRule()` + `setThemedContent { }` from `AtomTestSupport.kt`, assertions via `com.google.common.truth.Truth.assertThat`.
- **Preview convention:** every atom file ends with a private `…Showcase()` composable plus a light and a dark `@Preview`, using `NimazTheme(themeMode = ThemeMode.LIGHT / ThemeMode.DARK)` and `uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL` on the dark one.
- **Commit discipline:** one commit per task; docs updated in the same commit as the code they describe. Commit trailer: `Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>`.
- **Branch:** `feat/fasting-screen-redesign`, based on `dev`. Do not push to `dev`.

---

## File Structure

**Created**

| File | Responsibility |
|---|---|
| `.../components/atoms/NimazToneColors.kt` | Shared `NimazTone` → colour resolvers, so five atoms do not each copy the `when` block |
| `.../components/atoms/NimazStatusDot.kt` | Filled/outlined status dot + `NimazStatusDotSpec` |
| `.../components/atoms/NimazProgressTrack.kt` | Determinate horizontal progress track |
| `.../components/atoms/NimazSegmentedControl.kt` | Icon+label segmented control with a nullable selection |
| `.../components/atoms/NimazWindowTrack.kt` | Suhoor→iftar span with a "now" marker |
| `.../components/atoms/NimazDayRail.kt` | Seven-cell week rail |
| `.../screens/fasting/MakeupFastsScreen.kt` | The make-up destination |
| `.../screens/fasting/FastExemptionSheet.kt` | Reason-only bottom sheet |
| `.../screens/fasting/FastNoteSheet.kt` | Note-only bottom sheet |
| `.../screens/fasting/FastingDayCard.kt` | The day card organism |
| `.../screens/fasting/FastingComingUp.kt` | "Coming up" derivation + horizontal card row |
| Six test files under `app/src/testDebug/.../components/atoms/` | One per atom |

**Modified**

| File | Change |
|---|---|
| `.../components/molecules/calendar/CalendarModels.kt` | `indicatorStyle` on `CalendarDayState` and `CalendarLegendItem` |
| `.../components/molecules/calendar/NimazCalendar.kt` | Draw indicators via `NimazStatusDot` |
| `.../components/atoms/NimazLegendItem.kt` | Delegate to `NimazStatusDot` (public API unchanged) |
| `.../components/molecules/RamadanCards.kt` | Ramadan strip rebuilt on `NimazProgressTrack` |
| `.../viewmodel/tracker/FastingUiState.kt` | Selected-day fields |
| `.../viewmodel/tracker/FastingEvent.kt` | Three new events, four retired |
| `.../viewmodel/tracker/FastingViewModel.kt` | Selected-day loading |
| `.../core/navigation/Route.kt`, `ScreenTags.kt`, `NavGraph.kt` | `Route.MakeupFasts` |
| `.../screens/fasting/FastTrackerScreen.kt` | Rebuilt |
| `app/src/main/res/values*/strings.xml` | New `fasting_` strings |
| `docs/NAVIGATION.md`, `docs/ARCHITECTURE.md` | Route + atoms |

**Deleted:** `FastingSubtitles.kt`, `FastingSubtitlesTest.kt`, `FastManagementBottomSheet.kt`, `MakeupFastsTab.kt` (content moves to `MakeupFastsScreen.kt`).

---

# Phase 1 — The atoms (review gate)

**Nothing in Phase 2 onward starts until the previews for Tasks 1–6 have been reviewed and approved.**

---

### Task 1: `NimazToneColors` — the shared tone resolver

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/components/atoms/NimazToneColors.kt`
- Test: `app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/atoms/NimazToneColorsTest.kt`

**Interfaces:**
- Consumes: `NimazTone` (already declared in `NimazCard.kt`).
- Produces: `internal object NimazToneColors` with three `@Composable` functions — `foreground(tone: NimazTone): Color`, `container(tone: NimazTone): Color`, `outline(tone: NimazTone): Color`. Tasks 2–6 all use these.

Why this exists: `NimazBadgeDefaults` already has these `when` blocks but they are `private`. Five new atoms copying them is five places for the tone vocabulary to drift.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazToneColorsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `every tone resolves a foreground colour`() {
        val resolved = mutableListOf<Color>()
        composeRule.setThemedContent {
            NimazTone.entries.forEach { resolved.add(NimazToneColors.foreground(it)) }
        }
        composeRule.waitForIdle()
        assertThat(resolved).hasSize(NimazTone.entries.size)
    }

    @Test
    fun `transparent tone resolves a transparent container`() {
        var container: Color? = null
        composeRule.setThemedContent {
            container = NimazToneColors.container(NimazTone.TRANSPARENT)
        }
        composeRule.waitForIdle()
        assertThat(container).isEqualTo(Color.Transparent)
    }

    @Test
    fun `success and error resolve to different foregrounds`() {
        var success: Color? = null
        var error: Color? = null
        composeRule.setThemedContent {
            success = NimazToneColors.foreground(NimazTone.SUCCESS)
            error = NimazToneColors.foreground(NimazTone.ERROR)
        }
        composeRule.waitForIdle()
        assertThat(success).isNotEqualTo(error)
    }
}
```

- [ ] **Step 2: Run the test and verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*NimazToneColorsTest*"`
Expected: FAIL — `Unresolved reference: NimazToneColors`.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * The one place [NimazTone] turns into colour for the atom layer.
 *
 * [NimazBadgeDefaults] grew its own private copies of these `when` blocks, and the five atoms
 * added for the fasting redesign would have been five more. Tone is a vocabulary; a vocabulary
 * with six private dialects is not one.
 */
internal object NimazToneColors {

    /** Text / icon / fill colour carrying the tone's meaning. */
    @Composable
    fun foreground(tone: NimazTone): Color = when (tone) {
        NimazTone.NEUTRAL, NimazTone.MUTED -> MaterialTheme.colorScheme.onSurfaceVariant
        NimazTone.ACCENT, NimazTone.PROMINENT -> MaterialTheme.colorScheme.primary
        NimazTone.SUCCESS -> MaterialTheme.colorScheme.tertiary
        NimazTone.WARNING -> MaterialTheme.colorScheme.secondary
        NimazTone.ERROR -> MaterialTheme.colorScheme.error
        NimazTone.TRANSPARENT -> LocalContentColor.current
    }

    /** The low-emphasis bed the tone's foreground sits on. */
    @Composable
    fun container(tone: NimazTone): Color = when (tone) {
        NimazTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceContainerHighest
        NimazTone.MUTED -> MaterialTheme.colorScheme.surfaceContainer
        NimazTone.ACCENT, NimazTone.PROMINENT -> MaterialTheme.colorScheme.primaryContainer
        NimazTone.SUCCESS -> MaterialTheme.colorScheme.tertiaryContainer
        NimazTone.WARNING -> MaterialTheme.colorScheme.secondaryContainer
        NimazTone.ERROR -> MaterialTheme.colorScheme.errorContainer
        NimazTone.TRANSPARENT -> Color.Transparent
    }

    /** Hairline colour for outlined treatments. */
    @Composable
    fun outline(tone: NimazTone): Color = when (tone) {
        NimazTone.NEUTRAL, NimazTone.MUTED -> MaterialTheme.colorScheme.outlineVariant
        NimazTone.TRANSPARENT -> Color.Transparent
        else -> foreground(tone)
    }
}
```

- [ ] **Step 4: Run the test and verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*NimazToneColorsTest*"`
Expected: PASS, 3 tests.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/components/atoms/NimazToneColors.kt \
        app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/atoms/NimazToneColorsTest.kt
git commit -m "feat(atoms): tone becomes one vocabulary, not six private dialects"
```

---

### Task 2: `NimazStatusDot`

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/components/atoms/NimazStatusDot.kt`
- Test: `app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/atoms/NimazStatusDotTest.kt`

**Interfaces:**
- Consumes: `NimazToneColors.foreground/outline` (Task 1).
- Produces: `NimazStatusDotStyle { FILLED, OUTLINED }`, `NimazStatusDotSize(val diameter: Dp) { SMALL(6.dp), MEDIUM(7.dp), LARGE(10.dp) }`, `data class NimazStatusDotSpec(tone: NimazTone, style: NimazStatusDotStyle = FILLED)`, and `@Composable fun NimazStatusDot(spec, modifier, size = MEDIUM, contentDescription: String? = null)`. Tasks 4 and 6 and the calendar consume `NimazStatusDotSpec`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazStatusDotTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `styles and sizes are complete`() {
        assertThat(NimazStatusDotStyle.entries).hasSize(2)
        assertThat(NimazStatusDotSize.entries).hasSize(3)
    }

    @Test
    fun `spec defaults to the filled style`() {
        assertThat(NimazStatusDotSpec(NimazTone.SUCCESS).style)
            .isEqualTo(NimazStatusDotStyle.FILLED)
    }

    @Test
    fun `a described dot is exposed to accessibility`() {
        composeRule.setThemedContent {
            NimazStatusDot(
                spec = NimazStatusDotSpec(NimazTone.SUCCESS),
                contentDescription = "fasted"
            )
        }
        composeRule.onNodeWithContentDescription("fasted").assertIsDisplayed()
    }

    @Test
    fun `an outlined dot renders without a description`() {
        composeRule.setThemedContent {
            NimazStatusDot(
                spec = NimazStatusDotSpec(NimazTone.NEUTRAL, NimazStatusDotStyle.OUTLINED)
            )
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `sizes are ordered smallest to largest`() {
        assertThat(NimazStatusDotSize.SMALL.diameter.value)
            .isLessThan(NimazStatusDotSize.MEDIUM.diameter.value)
        assertThat(NimazStatusDotSize.MEDIUM.diameter.value)
            .isLessThan(NimazStatusDotSize.LARGE.diameter.value)
    }
}
```

- [ ] **Step 2: Run the test and verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*NimazStatusDotTest*"`
Expected: FAIL — `Unresolved reference: NimazStatusDotStyle`.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.arshadshah.nimaz.presentation.components.atoms

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/** Whether the dot is a solid disc or a ring. */
enum class NimazStatusDotStyle {
    /** Solid disc — the state happened. */
    FILLED,

    /** Ring — the state was *recorded as not happening*, which is not the same as no record. */
    OUTLINED
}

/** Dot diameter. */
enum class NimazStatusDotSize(val diameter: Dp) {
    SMALL(6.dp),
    MEDIUM(7.dp),
    LARGE(10.dp)
}

/**
 * A dot's whole appearance in one value, so callers can carry "how this day looks" through a
 * list without carrying a colour and a boolean separately.
 */
data class NimazStatusDotSpec(
    val tone: NimazTone,
    val style: NimazStatusDotStyle = NimazStatusDotStyle.FILLED,
)

/**
 * The app's status dot.
 *
 * [NimazLegendItem] and the calendar each drew their own filled circle, and neither could draw a
 * hollow one — so "logged as not fasted" and "no record at all" rendered identically as an absent
 * dot. The ring is the whole reason this atom exists.
 *
 * @param spec tone and fill style.
 * @param size diameter rung.
 * @param contentDescription accessibility label; `null` leaves the dot decorative, which is
 *   correct when an adjacent label already says what it means.
 */
@Composable
fun NimazStatusDot(
    spec: NimazStatusDotSpec,
    modifier: Modifier = Modifier,
    size: NimazStatusDotSize = NimazStatusDotSize.MEDIUM,
    contentDescription: String? = null,
) {
    val color = NimazToneColors.foreground(spec.tone)

    val semanticsModifier = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }

    val paintModifier = when (spec.style) {
        NimazStatusDotStyle.FILLED -> Modifier.background(color, CircleShape)
        NimazStatusDotStyle.OUTLINED -> Modifier
            .background(Color.Transparent, CircleShape)
            .border(1.5.dp, color, CircleShape)
    }

    Box(
        modifier = modifier
            .size(size.diameter)
            .then(paintModifier)
            .then(semanticsModifier)
    )
}

// ==================== PREVIEWS ====================

@Composable
private fun NimazStatusDotShowcase() {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NimazStatusDotStyle.entries.forEach { style ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NimazStatusDotSize.entries.forEach { size ->
                    listOf(
                        NimazTone.SUCCESS,
                        NimazTone.WARNING,
                        NimazTone.NEUTRAL,
                        NimazTone.ACCENT
                    ).forEach { tone ->
                        NimazStatusDot(NimazStatusDotSpec(tone, style), size = size)
                    }
                }
                androidx.compose.material3.Text(
                    text = style.name,
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "NimazStatusDot — Light")
@Composable
private fun NimazStatusDotLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { NimazStatusDotShowcase() }
}

@Preview(
    showBackground = true, name = "NimazStatusDot — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazStatusDotDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { NimazStatusDotShowcase() }
}
```

- [ ] **Step 4: Run the test and verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*NimazStatusDotTest*"`
Expected: PASS, 5 tests.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/components/atoms/NimazStatusDot.kt \
        app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/atoms/NimazStatusDotTest.kt
git commit -m "feat(atoms): a dot that can be hollow

A missing dot cannot say 'logged as not fasted'."
```

---

### Task 3: `NimazProgressTrack`

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/components/atoms/NimazProgressTrack.kt`
- Test: `app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/atoms/NimazProgressTrackTest.kt`

**Interfaces:**
- Consumes: `NimazToneColors.foreground/container` (Task 1).
- Produces: `NimazProgressSize(val height: Dp) { THIN(4.dp), MEDIUM(6.dp), THICK(10.dp) }` and `@Composable fun NimazProgressTrack(progress: Float, modifier, tone = NimazTone.ACCENT, size = MEDIUM, gradient: Boolean = false, trackColor: Color? = null, contentDescription: String? = null)`. Task 10 (Ramadan strip) consumes it.

Coercion is the contract: `progress` is clamped to `0f..1f` inside the atom and `NaN` becomes `0f`. Eight existing hand-rolled call sites is eight chances to pass a bad float; a progress bar must never be what crashes a screen.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazProgressTrackTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `sizes are complete and ordered`() {
        assertThat(NimazProgressSize.entries).hasSize(3)
        assertThat(NimazProgressSize.THIN.height.value)
            .isLessThan(NimazProgressSize.THICK.height.value)
    }

    @Test
    fun `progress above one is coerced rather than thrown`() {
        composeRule.setThemedContent {
            NimazProgressTrack(progress = 4.2f, contentDescription = "over")
        }
        composeRule.onNodeWithContentDescription("over").assertIsDisplayed()
    }

    @Test
    fun `negative progress is coerced rather than thrown`() {
        composeRule.setThemedContent {
            NimazProgressTrack(progress = -1f, contentDescription = "under")
        }
        composeRule.onNodeWithContentDescription("under").assertIsDisplayed()
    }

    @Test
    fun `NaN progress renders as empty rather than crashing`() {
        composeRule.setThemedContent {
            NimazProgressTrack(progress = Float.NaN, contentDescription = "nan")
        }
        composeRule.onNodeWithContentDescription("nan").assertIsDisplayed()
    }

    @Test
    fun `the gradient variant renders`() {
        composeRule.setThemedContent {
            NimazProgressTrack(
                progress = 0.4f,
                gradient = true,
                size = NimazProgressSize.THICK,
                contentDescription = "gradient"
            )
        }
        composeRule.onNodeWithContentDescription("gradient").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run the test and verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*NimazProgressTrackTest*"`
Expected: FAIL — `Unresolved reference: NimazProgressSize`.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.arshadshah.nimaz.presentation.components.atoms

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/** Track thickness. */
enum class NimazProgressSize(val height: Dp) {
    THIN(4.dp),
    MEDIUM(6.dp),
    THICK(10.dp)
}

/**
 * The app's determinate progress track.
 *
 * Eight files hand-rolled `LinearProgressIndicator` with their own height, shape and colours
 * before this existed. [progress] is coerced here rather than at each of them: eight call sites
 * is eight chances for a `NaN` or an out-of-range float, and a progress bar should never be the
 * thing that takes a screen down.
 *
 * @param progress fraction complete; `NaN` reads as `0f` and the value is clamped to `0f..1f`.
 * @param tone semantic colour of the filled portion.
 * @param size thickness rung.
 * @param gradient ramps the fill from the tone into gold — reserved for celebratory progress
 *   (the Ramadan strip), not everyday bars.
 * @param trackColor overrides the unfilled bed; `null` uses the tone's container colour.
 * @param contentDescription accessibility label; `null` leaves the bar decorative, which is
 *   correct when an adjacent label already states the numbers.
 */
@Composable
fun NimazProgressTrack(
    progress: Float,
    modifier: Modifier = Modifier,
    tone: NimazTone = NimazTone.ACCENT,
    size: NimazProgressSize = NimazProgressSize.MEDIUM,
    gradient: Boolean = false,
    trackColor: Color? = null,
    contentDescription: String? = null,
) {
    val safeProgress = if (progress.isNaN()) 0f else progress.coerceIn(0f, 1f)
    val fillColor = NimazToneColors.foreground(tone)
    val bed = trackColor ?: NimazToneColors.container(tone)
    val shape = RoundedCornerShape(percent = 50)

    val semanticsModifier = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(size.height)
            .clip(shape)
            .background(bed)
            .then(semanticsModifier)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                // Measured rather than fillMaxWidth(fraction): a fraction of 0f still lays out a
                // zero-width node with a rounded clip, which paints a stray pip at 0%.
                .layout { measurable, constraints ->
                    val width = (constraints.maxWidth * safeProgress).toInt()
                    val placeable = measurable.measure(
                        constraints.copy(minWidth = width, maxWidth = width)
                    )
                    layout(width, placeable.height) { placeable.placeRelative(0, 0) }
                }
                .clip(shape)
                .background(
                    if (gradient) {
                        Brush.horizontalGradient(listOf(fillColor, NimazColors.Gold500))
                    } else {
                        Brush.horizontalGradient(listOf(fillColor, fillColor))
                    }
                )
        )
    }
}

// ==================== PREVIEWS ====================

@Composable
private fun NimazProgressTrackShowcase() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Sizes at 60%", style = MaterialTheme.typography.labelMedium)
        NimazProgressSize.entries.forEach { size ->
            NimazProgressTrack(progress = 0.6f, size = size)
        }

        Text("Tones at 45%", style = MaterialTheme.typography.labelMedium)
        listOf(
            NimazTone.ACCENT,
            NimazTone.SUCCESS,
            NimazTone.WARNING,
            NimazTone.ERROR,
            NimazTone.NEUTRAL
        ).forEach { tone ->
            NimazProgressTrack(progress = 0.45f, tone = tone)
        }

        Text("Edges — 0%, 100%, gradient", style = MaterialTheme.typography.labelMedium)
        NimazProgressTrack(progress = 0f)
        NimazProgressTrack(progress = 1f)
        NimazProgressTrack(progress = 0.4f, gradient = true, size = NimazProgressSize.THICK)
    }
}

@Preview(showBackground = true, widthDp = 360, name = "NimazProgressTrack — Light")
@Composable
private fun NimazProgressTrackLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { NimazProgressTrackShowcase() }
}

@Preview(
    showBackground = true, widthDp = 360, name = "NimazProgressTrack — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazProgressTrackDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { NimazProgressTrackShowcase() }
}
```

`NimazColors.Gold500` is verified to exist (`theme/Color.kt:36`) — it is the app's secondary gold,
the same token `NimazColors.Secondary` aliases.

- [ ] **Step 4: Run the test and verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*NimazProgressTrackTest*"`
Expected: PASS, 5 tests.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/components/atoms/NimazProgressTrack.kt \
        app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/atoms/NimazProgressTrackTest.kt
git commit -m "feat(atoms): the progress bar eight files were each writing themselves"
```

---

### Task 4: `NimazSegmentedControl`

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/components/atoms/NimazSegmentedControl.kt`
- Test: `app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/atoms/NimazSegmentedControlTest.kt`

**Interfaces:**
- Consumes: `NimazToneColors` (Task 1), `NimazIcon` (existing).
- Produces: `data class NimazSegmentedOption(label: String, icon: ImageVector? = null, selectedTone: NimazTone = NimazTone.ACCENT, contentDescription: String? = null)`, `enum class NimazSegmentedSize { SMALL, MEDIUM }`, `@Composable fun NimazSegmentedControl(options: List<NimazSegmentedOption>, selectedIndex: Int?, onSelect: (Int) -> Unit, modifier, size = MEDIUM, enabled: Boolean = true)`. Task 11 (day card) consumes it.

The nullable `selectedIndex` is the point: a day with no record has *no* selection, which is a state a `Switch` cannot express and the reason the switch is being replaced.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazSegmentedControlTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val options = listOf(
        NimazSegmentedOption("Fasted", Icons.Default.Check, NimazTone.SUCCESS),
        NimazSegmentedOption("Not fasting", null, NimazTone.NEUTRAL),
        NimazSegmentedOption("Exempt", null, NimazTone.WARNING),
    )

    @Test
    fun `sizes are complete`() {
        assertThat(NimazSegmentedSize.entries).hasSize(2)
    }

    @Test
    fun `the selected option reports itself as selected`() {
        composeRule.setThemedContent {
            NimazSegmentedControl(options = options, selectedIndex = 0, onSelect = {})
        }
        composeRule.onNodeWithText("Fasted").assertIsSelected()
        composeRule.onNodeWithText("Exempt").assertIsNotSelected()
    }

    @Test
    fun `a null selection selects nothing`() {
        composeRule.setThemedContent {
            NimazSegmentedControl(options = options, selectedIndex = null, onSelect = {})
        }
        composeRule.onNodeWithText("Fasted").assertIsNotSelected()
        composeRule.onNodeWithText("Not fasting").assertIsNotSelected()
        composeRule.onNodeWithText("Exempt").assertIsNotSelected()
    }

    @Test
    fun `tapping an option emits its index`() {
        var observed: Int? = null
        composeRule.setThemedContent {
            NimazSegmentedControl(
                options = options,
                selectedIndex = null,
                onSelect = { observed = it }
            )
        }
        composeRule.onNodeWithText("Exempt").performClick()
        assertThat(observed).isEqualTo(2)
    }

    @Test
    fun `tapping the already selected option still emits so callers can toggle off`() {
        var observed: Int? = null
        composeRule.setThemedContent {
            NimazSegmentedControl(
                options = options,
                selectedIndex = 0,
                onSelect = { observed = it }
            )
        }
        composeRule.onNodeWithText("Fasted").performClick()
        assertThat(observed).isEqualTo(0)
    }

    @Test
    fun `a disabled control does not emit`() {
        var observed: Int? = null
        composeRule.setThemedContent {
            NimazSegmentedControl(
                options = options,
                selectedIndex = null,
                onSelect = { observed = it },
                enabled = false
            )
        }
        composeRule.onNodeWithText("Fasted").performClick()
        assertThat(observed).isNull()
    }
}
```

- [ ] **Step 2: Run the test and verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*NimazSegmentedControlTest*"`
Expected: FAIL — `Unresolved reference: NimazSegmentedOption`.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.arshadshah.nimaz.presentation.components.atoms

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/** One cell of a [NimazSegmentedControl]. */
data class NimazSegmentedOption(
    val label: String,
    val icon: ImageVector? = null,
    /** The colour this cell takes **while selected** — cells may differ. */
    val selectedTone: NimazTone = NimazTone.ACCENT,
    val contentDescription: String? = null,
)

/** Cell padding and type scale. */
enum class NimazSegmentedSize(
    internal val verticalPadding: Dp,
    internal val iconSize: Dp,
) {
    SMALL(8.dp, 16.dp),
    MEDIUM(11.dp, 19.dp)
}

/**
 * A mutually-exclusive choice laid out as one inset row of cells.
 *
 * Distinct from [com.arshadshah.nimaz.presentation.components.organisms.NimazPillTabs], which
 * switches *views*, is text-only, and paints every selected tab `primary`. This carries an icon
 * per cell and lets each cell own the colour it takes when selected — "fasted" wanting green
 * while "exempt" wants amber is the case that made it necessary.
 *
 * [selectedIndex] is nullable, and that is the point: "nothing chosen yet" is a real state (a day
 * with no record), and a boolean toggle cannot express it.
 *
 * @param options the cells, left to right. Two or three; more than four belongs in a picker.
 * @param selectedIndex the chosen cell, or `null` when nothing is chosen.
 * @param onSelect invoked with the tapped index — **including when it is already selected**, so
 *   callers can implement tap-to-clear.
 * @param enabled when false, dims the control and blocks selection.
 */
@Composable
fun NimazSegmentedControl(
    options: List<NimazSegmentedOption>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    size: NimazSegmentedSize = NimazSegmentedSize.MEDIUM,
    enabled: Boolean = true,
) {
    val trackShape = RoundedCornerShape(15.dp)
    val cellShape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(trackShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, option ->
            val selected = selectedIndex == index

            val background by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                animationSpec = tween(180),
                label = "segment_background"
            )
            val contentColor by animateColorAsState(
                targetValue = when {
                    selected -> NimazToneColors.foreground(option.selectedTone)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = tween(180),
                label = "segment_content"
            )
            val resolvedContent =
                if (enabled) contentColor else contentColor.copy(alpha = 0.38f)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(cellShape)
                    .background(background)
                    .selectable(
                        selected = selected,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = { onSelect(index) }
                    )
                    .padding(vertical = size.verticalPadding, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                option.icon?.let { icon ->
                    NimazIcon(
                        imageVector = icon,
                        contentDescription = option.contentDescription,
                        iconSize = size.iconSize,
                        tint = resolvedContent
                    )
                }
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = resolvedContent
                )
            }
        }
    }
}

// ==================== PREVIEWS ====================

private val previewOptions: List<NimazSegmentedOption>
    @Composable get() = listOf(
        NimazSegmentedOption(
            "Fasted",
            androidx.compose.material.icons.Icons.Default.Check,
            NimazTone.SUCCESS
        ),
        NimazSegmentedOption(
            "Not fasting",
            androidx.compose.material.icons.Icons.Default.Clear,
            NimazTone.NEUTRAL
        ),
        NimazSegmentedOption(
            "Exempt",
            androidx.compose.material.icons.Icons.Default.Info,
            NimazTone.WARNING
        ),
    )

@Composable
private fun NimazSegmentedControlShowcase() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Nothing selected — a day with no record", style = MaterialTheme.typography.labelMedium)
        NimazSegmentedControl(previewOptions, selectedIndex = null, onSelect = {})

        Text("Each cell selected, showing its own tone", style = MaterialTheme.typography.labelMedium)
        previewOptions.indices.forEach { index ->
            NimazSegmentedControl(previewOptions, selectedIndex = index, onSelect = {})
        }

        Text("Small · disabled", style = MaterialTheme.typography.labelMedium)
        NimazSegmentedControl(
            previewOptions, selectedIndex = 0, onSelect = {},
            size = NimazSegmentedSize.SMALL
        )
        NimazSegmentedControl(
            previewOptions, selectedIndex = 1, onSelect = {},
            enabled = false
        )

        Text("Two cells", style = MaterialTheme.typography.labelMedium)
        NimazSegmentedControl(previewOptions.take(2), selectedIndex = 0, onSelect = {})
    }
}

@Preview(showBackground = true, widthDp = 360, name = "NimazSegmentedControl — Light")
@Composable
private fun NimazSegmentedControlLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { NimazSegmentedControlShowcase() }
}

@Preview(
    showBackground = true, widthDp = 360, name = "NimazSegmentedControl — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazSegmentedControlDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { NimazSegmentedControlShowcase() }
}
```

- [ ] **Step 4: Run the test and verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*NimazSegmentedControlTest*"`
Expected: PASS, 6 tests.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/components/atoms/NimazSegmentedControl.kt \
        app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/atoms/NimazSegmentedControlTest.kt
git commit -m "feat(atoms): a segmented control that can be unselected

'Not logged yet' is a state a switch cannot hold."
```

---

### Task 5: `NimazWindowTrack`

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/components/atoms/NimazWindowTrack.kt`
- Test: `app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/atoms/NimazWindowTrackTest.kt`

**Interfaces:**
- Consumes: `NimazToneColors` (Task 1).
- Produces: `@Composable fun NimazWindowTrack(startLabel: String, startValue: String, endLabel: String, endValue: String, modifier, progress: Float? = null, startTone: NimazTone = NimazTone.ACCENT, endTone: NimazTone = NimazTone.WARNING, contentDescription: String? = null)`. Task 11 consumes it.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazWindowTrackTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun content(progress: Float?) = @androidx.compose.runtime.Composable {
        NimazWindowTrack(
            startLabel = "Suhoor ends",
            startValue = "04:31",
            endLabel = "Iftar",
            endValue = "20:58",
            progress = progress,
        )
    }

    @Test
    fun `both ends are labelled and valued`() {
        composeRule.setThemedContent(content(progress = 0.5f))
        composeRule.onNodeWithText("Suhoor ends").assertIsDisplayed()
        composeRule.onNodeWithText("04:31").assertIsDisplayed()
        composeRule.onNodeWithText("Iftar").assertIsDisplayed()
        composeRule.onNodeWithText("20:58").assertIsDisplayed()
    }

    @Test
    fun `a null progress renders the whole band with no marker`() {
        composeRule.setThemedContent(content(progress = null))
        composeRule.onNodeWithText("04:31").assertIsDisplayed()
    }

    @Test
    fun `out of range progress is coerced rather than thrown`() {
        composeRule.setThemedContent(content(progress = 9f))
        composeRule.onNodeWithText("20:58").assertIsDisplayed()
        composeRule.setThemedContent(content(progress = Float.NaN))
        composeRule.onNodeWithText("20:58").assertIsDisplayed()
    }

    @Test
    fun `the band speaks as one node when described`() {
        composeRule.setThemedContent {
            NimazWindowTrack(
                startLabel = "Suhoor ends",
                startValue = "04:31",
                endLabel = "Iftar",
                endValue = "20:58",
                progress = 0.5f,
                contentDescription = "Fasting window from 04:31 to 20:58",
            )
        }
        composeRule.onNodeWithContentDescription("Fasting window from 04:31 to 20:58")
            .assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run the test and verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*NimazWindowTrackTest*"`
Expected: FAIL — `Unresolved reference: NimazWindowTrack`.

- [ ] **Step 3: Write the implementation**

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * A labelled span with an optional "now" marker inside it.
 *
 * Not a progress bar, which is why it is not [NimazProgressTrack] with extra parameters: a span
 * has two *named, differently-tinted ends* and a marker that sits inside the fill rather than
 * terminating it. Built for the suhoor→iftar window, where both ends are facts the reader wants
 * and the marker is where they currently are between them.
 *
 * @param progress position of the marker in `0f..1f`; `null` lights the whole band and draws no
 *   marker, which is the correct rendering for any day that is not today — and most days you look
 *   at are not today.
 * @param contentDescription one spoken sentence for the whole band. Four separate unlabelled text
 *   nodes read as noise, so when this is supplied the band's own children are cleared from the
 *   accessibility tree.
 */
@Composable
fun NimazWindowTrack(
    startLabel: String,
    startValue: String,
    endLabel: String,
    endValue: String,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    startTone: NimazTone = NimazTone.ACCENT,
    endTone: NimazTone = NimazTone.WARNING,
    contentDescription: String? = null,
) {
    val safeProgress = progress
        ?.let { if (it.isNaN()) 0f else it.coerceIn(0f, 1f) }
    val fillFraction = safeProgress ?: 1f

    val startColor = NimazToneColors.foreground(startTone)
    val endColor = NimazToneColors.foreground(endTone)
    val bandShape = RoundedCornerShape(12.dp)

    val semanticsModifier = if (contentDescription != null) {
        Modifier.clearAndSetSemantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }

    Column(modifier = modifier.then(semanticsModifier)) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(bandShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            val bandWidth = maxWidth

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .layout { measurable, constraints ->
                        val width = (constraints.maxWidth * fillFraction).toInt()
                        val placeable = measurable.measure(
                            constraints.copy(minWidth = width, maxWidth = width)
                        )
                        layout(width, placeable.height) { placeable.placeRelative(0, 0) }
                    }
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                startColor.copy(alpha = 0.20f),
                                endColor.copy(alpha = 0.26f)
                            )
                        )
                    )
            )

            if (safeProgress != null) {
                Box(
                    modifier = Modifier
                        .offset(x = bandWidth * safeProgress)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.onSurface)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            WindowEnd(startLabel, startValue, startColor, Alignment.Start)
            WindowEnd(endLabel, endValue, endColor, Alignment.End)
        }
    }
}

@Composable
private fun WindowEnd(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    alignment: Alignment.Horizontal,
) {
    Column(horizontalAlignment = alignment) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

// ==================== PREVIEWS ====================

@Composable
private fun NimazWindowTrackShowcase() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Today — marker at 80%", style = MaterialTheme.typography.labelMedium)
        NimazWindowTrack("Suhoor ends", "04:31", "Iftar", "20:58", progress = 0.8f)

        Text("Just after suhoor — 2%", style = MaterialTheme.typography.labelMedium)
        NimazWindowTrack("Suhoor ends", "04:31", "Iftar", "20:58", progress = 0.02f)

        Text("Another day — no marker", style = MaterialTheme.typography.labelMedium)
        NimazWindowTrack("Suhoor ends", "04:44", "Iftar", "20:31", progress = null)
    }
}

@Preview(showBackground = true, widthDp = 360, name = "NimazWindowTrack — Light")
@Composable
private fun NimazWindowTrackLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { NimazWindowTrackShowcase() }
}

@Preview(
    showBackground = true, widthDp = 360, name = "NimazWindowTrack — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazWindowTrackDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { NimazWindowTrackShowcase() }
}
```

- [ ] **Step 4: Run the test and verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*NimazWindowTrackTest*"`
Expected: PASS, 4 tests.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/components/atoms/NimazWindowTrack.kt \
        app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/atoms/NimazWindowTrackTest.kt
git commit -m "feat(atoms): the fasting window is a span, not a progress bar"
```

---

### Task 6: `NimazDayRail`

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/components/atoms/NimazDayRail.kt`
- Test: `app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/atoms/NimazDayRailTest.kt`

**Interfaces:**
- Consumes: `NimazStatusDot`, `NimazStatusDotSpec` (Task 2), `NimazToneColors` (Task 1).
- Produces: `data class NimazDayRailItem(weekdayLabel: String, dayLabel: String, marker: NimazStatusDotSpec? = null, isToday: Boolean = false, enabled: Boolean = true, contentDescription: String)` and `@Composable fun NimazDayRail(days: List<NimazDayRailItem>, selectedIndex: Int?, onSelect: (Int) -> Unit, modifier)`. Task 11 consumes it.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazDayRailTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val week = (10..16).mapIndexed { index, day ->
        NimazDayRailItem(
            weekdayLabel = listOf("M", "T", "W", "T", "F", "S", "S")[index],
            dayLabel = day.toString(),
            marker = if (day == 11) NimazStatusDotSpec(NimazTone.SUCCESS) else null,
            isToday = day == 13,
            enabled = day <= 13,
            contentDescription = "August $day",
        )
    }

    @Test
    fun `every day is exposed to accessibility`() {
        composeRule.setThemedContent {
            NimazDayRail(days = week, selectedIndex = 3, onSelect = {})
        }
        week.forEach {
            composeRule.onNodeWithContentDescription(it.contentDescription).assertIsDisplayed()
        }
    }

    @Test
    fun `the selected day reports itself as selected`() {
        composeRule.setThemedContent {
            NimazDayRail(days = week, selectedIndex = 3, onSelect = {})
        }
        composeRule.onNodeWithContentDescription("August 13").assertIsSelected()
        composeRule.onNodeWithContentDescription("August 10").assertIsNotSelected()
    }

    @Test
    fun `tapping a day emits its index`() {
        var observed: Int? = null
        composeRule.setThemedContent {
            NimazDayRail(days = week, selectedIndex = 3, onSelect = { observed = it })
        }
        composeRule.onNodeWithContentDescription("August 11").performClick()
        assertThat(observed).isEqualTo(1)
    }

    @Test
    fun `a disabled future day does not emit`() {
        var observed: Int? = null
        composeRule.setThemedContent {
            NimazDayRail(days = week, selectedIndex = 3, onSelect = { observed = it })
        }
        composeRule.onNodeWithContentDescription("August 16").performClick()
        assertThat(observed).isNull()
    }

    @Test
    fun `a null selection selects nothing`() {
        composeRule.setThemedContent {
            NimazDayRail(days = week, selectedIndex = null, onSelect = {})
        }
        composeRule.onNodeWithContentDescription("August 13").assertIsNotSelected()
    }
}
```

- [ ] **Step 2: Run the test and verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*NimazDayRailTest*"`
Expected: FAIL — `Unresolved reference: NimazDayRailItem`.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.arshadshah.nimaz.presentation.components.atoms

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * One cell of a [NimazDayRail]. Labels arrive already formatted — the rail knows nothing about
 * dates, locales or fasting.
 *
 * @param contentDescription required, not optional: "13" alone is not a date, and a rail of seven
 *   bare numbers is unusable with a screen reader.
 */
data class NimazDayRailItem(
    val weekdayLabel: String,
    val dayLabel: String,
    val marker: NimazStatusDotSpec? = null,
    val isToday: Boolean = false,
    val enabled: Boolean = true,
    val contentDescription: String,
)

/**
 * A week as seven equal cells, each with a weekday initial, a day number and an optional marker.
 *
 * A `Row`, not a `LazyRow`: a week is a fixed seven, and a lazy list here would add scroll state
 * to lose on recomposition for no benefit.
 *
 * @param selectedIndex the chosen cell, or `null` for none.
 * @param onSelect invoked with the tapped index; disabled cells never emit.
 */
@Composable
fun NimazDayRail(
    days: List<NimazDayRailItem>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        days.forEachIndexed { index, day ->
            val selected = selectedIndex == index
            val cellShape = RoundedCornerShape(16.dp)
            val dimmed = !day.enabled

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(cellShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.surface else Color.Transparent
                    )
                    .then(
                        if (selected) {
                            Modifier.border(
                                1.dp,
                                MaterialTheme.colorScheme.primary,
                                cellShape
                            )
                        } else {
                            Modifier
                        }
                    )
                    .selectable(
                        selected = selected,
                        enabled = day.enabled,
                        role = Role.Tab,
                        onClick = { onSelect(index) }
                    )
                    .clearAndSetSemantics { contentDescription = day.contentDescription }
                    .padding(vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    text = day.weekdayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                        .copy(alpha = if (dimmed) 0.38f else 1f)
                )
                Text(
                    text = day.dayLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
                    color = when {
                        day.isToday -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    }.copy(alpha = if (dimmed) 0.38f else 1f)
                )
                // The slot is always occupied so the cells stay the same height whether or not
                // a day carries a marker — a rail that reflows as records arrive reads as jitter.
                Box(modifier = Modifier.size(NimazStatusDotSize.MEDIUM.diameter)) {
                    day.marker?.let { NimazStatusDot(spec = it) }
                }
            }
        }
    }
}

// ==================== PREVIEWS ====================

@Composable
private fun previewWeek(): List<NimazDayRailItem> = listOf(
    NimazDayRailItem("M", "10", NimazStatusDotSpec(NimazTone.SUCCESS), contentDescription = "Monday 10 August"),
    NimazDayRailItem("T", "11", NimazStatusDotSpec(NimazTone.NEUTRAL, NimazStatusDotStyle.OUTLINED), contentDescription = "Tuesday 11 August"),
    NimazDayRailItem("W", "12", NimazStatusDotSpec(NimazTone.WARNING), contentDescription = "Wednesday 12 August"),
    NimazDayRailItem("T", "13", isToday = true, contentDescription = "Thursday 13 August"),
    NimazDayRailItem("F", "14", enabled = false, contentDescription = "Friday 14 August"),
    NimazDayRailItem("S", "15", enabled = false, contentDescription = "Saturday 15 August"),
    NimazDayRailItem("S", "16", enabled = false, contentDescription = "Sunday 16 August"),
)

@Composable
private fun NimazDayRailShowcase() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("Today selected", style = MaterialTheme.typography.labelMedium)
        NimazDayRail(previewWeek(), selectedIndex = 3, onSelect = {})

        Text("A past day selected", style = MaterialTheme.typography.labelMedium)
        NimazDayRail(previewWeek(), selectedIndex = 0, onSelect = {})

        Text("Nothing selected", style = MaterialTheme.typography.labelMedium)
        NimazDayRail(previewWeek(), selectedIndex = null, onSelect = {})
    }
}

@Preview(showBackground = true, widthDp = 360, name = "NimazDayRail — Light")
@Composable
private fun NimazDayRailLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { NimazDayRailShowcase() }
}

@Preview(
    showBackground = true, widthDp = 360, name = "NimazDayRail — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazDayRailDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { NimazDayRailShowcase() }
}
```

- [ ] **Step 4: Run the test and verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*NimazDayRailTest*"`
Expected: PASS, 5 tests.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/components/atoms/NimazDayRail.kt \
        app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/atoms/NimazDayRailTest.kt
git commit -m "feat(atoms): a week as seven cells"
```

---

### Task 7: Calendar and legend adopt the dot

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/components/molecules/calendar/CalendarModels.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/components/molecules/calendar/NimazCalendar.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/components/atoms/NimazLegendItem.kt`
- Test: `app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/atoms/NimazLegendItemTest.kt` (extend)

**Interfaces:**
- Consumes: `NimazStatusDotStyle`, `NimazStatusDot` (Task 2).
- Produces: `CalendarDayState.indicatorStyle: NimazStatusDotStyle = FILLED` and `CalendarLegendItem.indicatorStyle: NimazStatusDotStyle = FILLED`. Task 12 sets them.

Both default to `FILLED`, so every existing caller behaves identically — this is purely additive.

- [ ] **Step 1: Write the failing test**

Append to `NimazLegendItemTest.kt`:

```kotlin
    @Test
    fun `a legend item renders an outlined swatch`() {
        composeRule.setThemedContent {
            NimazLegendItem(
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                label = "Not fasting",
                style = NimazStatusDotStyle.OUTLINED,
            )
        }
        composeRule.onNodeWithText("Not fasting").assertIsDisplayed()
    }
```

Create `app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/molecules/calendar/CalendarModelsTest.kt`:

```kotlin
package com.arshadshah.nimaz.presentation.components.molecules.calendar

import com.arshadshah.nimaz.presentation.components.atoms.NimazStatusDotStyle
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CalendarModelsTest {

    @Test
    fun `day state indicators default to filled so existing callers are unchanged`() {
        assertThat(CalendarDayState().indicatorStyle).isEqualTo(NimazStatusDotStyle.FILLED)
    }

    @Test
    fun `legend items default to filled so existing callers are unchanged`() {
        assertThat(
            CalendarLegendItem(
                color = androidx.compose.ui.graphics.Color.Red,
                label = "Fasted"
            ).indicatorStyle
        ).isEqualTo(NimazStatusDotStyle.FILLED)
    }

    @Test
    fun `an outlined indicator can be requested`() {
        assertThat(
            CalendarDayState(indicatorStyle = NimazStatusDotStyle.OUTLINED).indicatorStyle
        ).isEqualTo(NimazStatusDotStyle.OUTLINED)
    }
}
```

- [ ] **Step 2: Run the tests and verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "*CalendarModelsTest*" --tests "*NimazLegendItemTest*"`
Expected: FAIL — `No value passed for parameter 'style'` / `Unresolved reference: indicatorStyle`.

- [ ] **Step 3: Write the implementation**

In `CalendarModels.kt`, add the field to both data classes (keeping every existing parameter in
place and in order, so no positional caller breaks):

```kotlin
data class CalendarDayState(
    val indicatorColor: Color? = null,
    val indicatorPosition: IndicatorPosition = IndicatorPosition.BOTTOM_CENTER,
    val backgroundColor: Color? = null,
    val textColor: Color? = null,
    val fontWeight: FontWeight? = null,
    val primaryLabel: String? = null,
    val secondaryLabel: String? = null,
    val emphasizePrimary: Boolean = false,
    val emphasizeSecondary: Boolean = false,
    /**
     * Whether the indicator is a disc or a ring. Defaults to a disc, so every caller written
     * before the ring existed renders exactly as it did.
     */
    val indicatorStyle: NimazStatusDotStyle = NimazStatusDotStyle.FILLED,
)

data class CalendarLegendItem(
    val color: Color,
    val label: String,
    val indicatorStyle: NimazStatusDotStyle = NimazStatusDotStyle.FILLED,
)
```

In `NimazCalendar.kt`, replace the hand-drawn indicator `Box` and the hand-drawn legend swatch
with `NimazStatusDot`. Because `CalendarDayState` carries a raw `Color` rather than a `NimazTone`,
add a private overload alongside the atom rather than forcing the calendar to invent a tone:

```kotlin
// In NimazStatusDot.kt, add beneath the tone-driven overload:

/**
 * Colour-driven overload for callers that already hold a resolved [Color] — the calendar's
 * `CalendarDayState` carries one, and making it invent a [NimazTone] to get it back would be a
 * round trip through a vocabulary it does not speak.
 */
@Composable
fun NimazStatusDot(
    color: Color,
    modifier: Modifier = Modifier,
    style: NimazStatusDotStyle = NimazStatusDotStyle.FILLED,
    size: NimazStatusDotSize = NimazStatusDotSize.MEDIUM,
    contentDescription: String? = null,
) {
    val semanticsModifier = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }

    val paintModifier = when (style) {
        NimazStatusDotStyle.FILLED -> Modifier.background(color, CircleShape)
        NimazStatusDotStyle.OUTLINED -> Modifier
            .background(Color.Transparent, CircleShape)
            .border(1.5.dp, color, CircleShape)
    }

    Box(
        modifier = modifier
            .size(size.diameter)
            .then(paintModifier)
            .then(semanticsModifier)
    )
}
```

Then in `NimazLegendItem.kt`, add `style: NimazStatusDotStyle = NimazStatusDotStyle.FILLED` as the
last parameter and delegate its swatch to `NimazStatusDot(color = color, style = style)`.

- [ ] **Step 4: Run the tests and verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "*CalendarModelsTest*" --tests "*NimazLegendItemTest*" --tests "*NimazCalendar*"`
Expected: PASS. Then confirm nothing else regressed:
Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/components/molecules/calendar/ \
        app/src/main/java/com/arshadshah/nimaz/presentation/components/atoms/NimazStatusDot.kt \
        app/src/main/java/com/arshadshah/nimaz/presentation/components/atoms/NimazLegendItem.kt \
        app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/
git commit -m "feat(calendar): indicators can be rings, and every existing caller is unchanged"
```

---

### Task 8: Document the atoms and hold the review gate

**Files:**
- Modify: `docs/ARCHITECTURE.md` (§8, the component-system section)

- [ ] **Step 1: Add the five atoms to `docs/ARCHITECTURE.md` §8**

Find the bulleted component list in §8 (it already names `NimazButton`, `NimazCard`,
`NimazIcon`, `NimazSwitch`, `NimazCheckbox`, `NimazNumberStepper`, `NimazPager`) and add:

```markdown
- **`NimazSegmentedControl`** — mutually-exclusive choice in one inset row, icon + label per cell,
  and a **nullable** `selectedIndex` so "nothing chosen yet" is expressible. Each cell owns the
  tone it takes when selected. Not `NimazPillTabs`, which switches views and is text-only.
- **`NimazProgressTrack`** — the determinate progress primitive. `progress` is coerced (`NaN` → 0,
  clamped to `0f..1f`) at the atom. Reach for this before writing another `LinearProgressIndicator`.
- **`NimazWindowTrack`** — a labelled *span* with an optional "now" marker: two named,
  differently-tinted ends and a position between them. `progress = null` lights the whole band.
- **`NimazDayRail`** — a week as seven equal cells with an optional status marker each. A `Row`,
  not a `LazyRow`.
- **`NimazStatusDot`** — the status dot, filled **or outlined**. The ring is what distinguishes
  "recorded as not happening" from "no record", which an absent dot cannot do.
```

- [ ] **Step 2: Run the full atom-phase verification**

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.presentation.components.*"
python3 scripts/check_docs.py
```

Expected: all three succeed. Record the actual output — do not claim a pass you have not read.

- [ ] **Step 3: Commit**

```bash
git add docs/ARCHITECTURE.md
git commit -m "docs(architecture): the five atoms the fasting redesign added"
```

- [ ] **Step 4: STOP — review gate**

Report to the user: the five atoms exist, their previews render in both themes, and the tests
pass. Name the preview functions to open in Android Studio's preview pane:

- `NimazStatusDotLightPreview` / `NimazStatusDotDarkPreview`
- `NimazProgressTrackLightPreview` / `NimazProgressTrackDarkPreview`
- `NimazSegmentedControlLightPreview` / `NimazSegmentedControlDarkPreview`
- `NimazWindowTrackLightPreview` / `NimazWindowTrackDarkPreview`
- `NimazDayRailLightPreview` / `NimazDayRailDarkPreview`

**Do not start Task 9 until the user has approved the atoms.**

---

# Phase 2 — ViewModel

### Task 9: Selected-day state

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/tracker/FastingUiState.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/tracker/FastingEvent.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/tracker/FastingViewModel.kt:197` (`selectDate`), `:128` (`loadPrayerTimes`)
- Test: `app/src/test/java/com/arshadshah/nimaz/presentation/viewmodel/tracker/FastingViewModelTest.kt` (extend)

**Interfaces:**
- Consumes: `FastingUseCases.getFastRecordForDate`, `FastingUseCases.getFastRecordsInRange`, `FastingUseCases.insertFastRecord`, `FastingUseCases.deleteFastRecordByDate`, `PrayerUseCases.getDaySchedule` — all already exist.
- Produces:
  - `FastingTrackerUiState` fields `selectedRecord: FastRecord?`, `weekRecords: List<FastRecord>`, `selectedSuhoorAt: kotlin.time.Instant?`, `selectedIftarAt: kotlin.time.Instant?`, `isSelectedToday: Boolean`.
  - `FastingEvent.SetFastStatus(date: LocalDate, status: FastStatus)`, `FastingEvent.SaveExemption(date: LocalDate, reason: ExemptionReason)`, `FastingEvent.SaveNote(date: LocalDate, note: String)`.
  - Tasks 11 and 12 consume all of these.

`SetFastStatus` semantics: writing the status a day *already has* deletes the record. That is what
makes the segmented control tap-to-clear, and it is the behaviour the prototype has.

- [ ] **Step 1: Write the failing tests**

Add to `FastingViewModelTest.kt`, following the existing fakes and `runTest` setup in that file:

```kotlin
    @Test
    fun `selecting a date loads that day's record`() = runTest {
        val target = LocalDate.of(2026, 8, 11)
        fakeRepository.putRecord(target, FastStatus.FASTED)

        viewModel.onEvent(FastingEvent.SelectDate(target))
        advanceUntilIdle()

        val state = viewModel.trackerState.value
        assertThat(state.selectedDate).isEqualTo(target)
        assertThat(state.selectedRecord?.status).isEqualTo(FastStatus.FASTED)
        assertThat(state.isSelectedToday).isFalse()
    }

    @Test
    fun `selecting a date loads the whole week around it`() = runTest {
        // 2026-08-11 is a Tuesday; its week is Mon 10 to Sun 16.
        val target = LocalDate.of(2026, 8, 11)
        fakeRepository.putRecord(LocalDate.of(2026, 8, 10), FastStatus.FASTED)
        fakeRepository.putRecord(LocalDate.of(2026, 8, 16), FastStatus.NOT_FASTED)

        viewModel.onEvent(FastingEvent.SelectDate(target))
        advanceUntilIdle()

        assertThat(viewModel.trackerState.value.weekRecords).hasSize(2)
    }

    @Test
    fun `selecting a date loads that date's own suhoor and iftar`() = runTest {
        val target = LocalDate.of(2026, 8, 11)

        viewModel.onEvent(FastingEvent.SelectDate(target))
        advanceUntilIdle()

        val state = viewModel.trackerState.value
        assertThat(state.selectedSuhoorAt).isNotNull()
        assertThat(state.selectedIftarAt).isNotNull()
        assertThat(fakePrayerUseCases.lastRequestedDate).isEqualTo(target)
    }

    @Test
    fun `setting a status writes a record for that day`() = runTest {
        val target = LocalDate.of(2026, 8, 11)

        viewModel.onEvent(FastingEvent.SetFastStatus(target, FastStatus.FASTED))
        advanceUntilIdle()

        assertThat(fakeRepository.recordFor(target)?.status).isEqualTo(FastStatus.FASTED)
    }

    @Test
    fun `setting the status a day already has clears the record`() = runTest {
        val target = LocalDate.of(2026, 8, 11)
        fakeRepository.putRecord(target, FastStatus.FASTED)
        viewModel.onEvent(FastingEvent.SelectDate(target))
        advanceUntilIdle()

        viewModel.onEvent(FastingEvent.SetFastStatus(target, FastStatus.FASTED))
        advanceUntilIdle()

        assertThat(fakeRepository.recordFor(target)).isNull()
    }

    @Test
    fun `a Ramadan day is written as a Ramadan fast and any other day as voluntary`() = runTest {
        val ordinary = LocalDate.of(2026, 8, 11)

        viewModel.onEvent(FastingEvent.SetFastStatus(ordinary, FastStatus.FASTED))
        advanceUntilIdle()

        assertThat(fakeRepository.recordFor(ordinary)?.fastType).isEqualTo(FastType.VOLUNTARY)
    }

    @Test
    fun `saving an exemption stores the reason`() = runTest {
        val target = LocalDate.of(2026, 8, 11)

        viewModel.onEvent(FastingEvent.SaveExemption(target, ExemptionReason.TRAVEL))
        advanceUntilIdle()

        val record = fakeRepository.recordFor(target)
        assertThat(record?.status).isEqualTo(FastStatus.EXEMPTED)
        assertThat(record?.exemptionReason).isEqualTo(ExemptionReason.TRAVEL)
    }

    @Test
    fun `saving a note keeps the existing status`() = runTest {
        val target = LocalDate.of(2026, 8, 11)
        fakeRepository.putRecord(target, FastStatus.FASTED)

        viewModel.onEvent(FastingEvent.SaveNote(target, "felt easy"))
        advanceUntilIdle()

        val record = fakeRepository.recordFor(target)
        assertThat(record?.status).isEqualTo(FastStatus.FASTED)
        assertThat(record?.note).isEqualTo("felt easy")
    }
```

**Implementer note:** read the existing `FastingViewModelTest.kt` first and reuse its fakes. If the
fake repository has no `putRecord`/`recordFor` helper, add them there rather than inventing a new
fake; if the fake prayer use cases do not record the requested date, add a `lastRequestedDate`
field to that fake.

- [ ] **Step 2: Run the tests and verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "*FastingViewModelTest*"`
Expected: FAIL — `Unresolved reference: SetFastStatus`, `Unresolved reference: selectedRecord`.

- [ ] **Step 3: Write the implementation**

In `FastingUiState.kt`, add to `FastingTrackerUiState`:

```kotlin
    /** The record for [selectedDate] — `null` when that day has not been logged. */
    val selectedRecord: FastRecord? = null,
    /** Records for the Monday–Sunday containing [selectedDate]; a week can span two months. */
    val weekRecords: List<FastRecord> = emptyList(),
    /** [selectedDate]'s own Fajr / Maghrib — not today's. */
    val selectedSuhoorAt: kotlin.time.Instant? = null,
    val selectedIftarAt: kotlin.time.Instant? = null,
    /** Whether [selectedDate] is today, which decides whether the window shows a marker. */
    val isSelectedToday: Boolean = true,
```

In `FastingEvent.kt`, add the three events and delete `ToggleTodayFast`, `OpenFastSheet`,
`DismissFastSheet` and `SaveFastForDate`:

```kotlin
    /**
     * Writes [status] for [date]. Passing the status the day **already** has deletes the record —
     * that is what makes the segmented control tap-to-clear.
     */
    data class SetFastStatus(val date: LocalDate, val status: FastStatus) : FastingEvent
    data class SaveExemption(val date: LocalDate, val reason: ExemptionReason) : FastingEvent
    data class SaveNote(val date: LocalDate, val note: String) : FastingEvent
```

In `FastingViewModel.kt`, extend `selectDate` and add the three handlers:

```kotlin
    private fun selectDate(date: LocalDate) {
        _trackerState.update {
            it.copy(
                selectedDate = date,
                isSelectedToday = date == todayProvider.today(),
                isLoading = true
            )
        }
        viewModelScope.launch {
            val record = fastingUseCases.getFastRecordForDate(date.toEpochMillis())

            // Monday–Sunday around the selection. `calendarState.records` cannot serve this:
            // a week straddling a month boundary is half missing from a single month's query.
            val weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val weekEnd = weekStart.plusDays(6)
            val week = fastingUseCases
                .getFastRecordsInRange(weekStart.toEpochMillis(), weekEnd.toEpochMillis())
                .first()

            _trackerState.update {
                it.copy(
                    selectedRecord = record,
                    weekRecords = week,
                    isLoading = false
                )
            }
            loadScheduleFor(date)
        }
    }

    /**
     * That day's Fajr and Maghrib. `getDaySchedule` always took a date; only this ViewModel's
     * hardcoded `todayProvider.today()` kept the screen pinned to today.
     */
    private fun loadScheduleFor(date: LocalDate) {
        viewModelScope.launch {
            val settings = settingsFlow.first()
            val schedule = prayerUseCases.getDaySchedule(date, settings)
            _trackerState.update {
                it.copy(
                    selectedSuhoorAt = schedule.find { p -> p.type == PrayerType.FAJR }?.time,
                    selectedIftarAt = schedule.find { p -> p.type == PrayerType.MAGHRIB }?.time,
                )
            }
        }
    }

    private fun setFastStatus(date: LocalDate, status: FastStatus) {
        viewModelScope.launch {
            val existing = fastingUseCases.getFastRecordForDate(date.toEpochMillis())
            if (existing?.status == status) {
                fastingUseCases.deleteFastRecordByDate(date.toEpochMillis())
            } else {
                fastingUseCases.insertFastRecord(
                    buildRecord(date, status, exemptionReason = null, note = existing?.note)
                )
            }
            selectDate(date)
        }
    }

    private fun saveExemption(date: LocalDate, reason: ExemptionReason) {
        viewModelScope.launch {
            val existing = fastingUseCases.getFastRecordForDate(date.toEpochMillis())
            fastingUseCases.insertFastRecord(
                buildRecord(date, FastStatus.EXEMPTED, reason, existing?.note)
            )
            selectDate(date)
        }
    }

    private fun saveNote(date: LocalDate, note: String) {
        viewModelScope.launch {
            val existing = fastingUseCases.getFastRecordForDate(date.toEpochMillis())
            fastingUseCases.insertFastRecord(
                buildRecord(
                    date = date,
                    status = existing?.status ?: FastStatus.NOT_FASTED,
                    exemptionReason = existing?.exemptionReason,
                    note = note,
                )
            )
            selectDate(date)
        }
    }

    /**
     * Fast type is inferred, not chosen: a Ramadan day is a Ramadan fast, anything else is
     * voluntary. The type dropdown that used to ask went with the heavyweight day sheet.
     */
    private fun buildRecord(
        date: LocalDate,
        status: FastStatus,
        exemptionReason: ExemptionReason?,
        note: String?,
    ): FastRecord { /* mirror the construction already in saveFastForDate at :476 */ }
```

**Implementer note:** `buildRecord` must mirror the `FastRecord` construction already in
`saveFastForDate` (`FastingViewModel.kt:476`) — the same hijri fields, timestamps and id handling.
Read that method and factor it out rather than writing a second, subtly different constructor.
Wire the three new events into the `onEvent` `when` and delete the four retired branches.

- [ ] **Step 4: Run the tests and verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "*Fasting*"`
Expected: PASS. `FastingToggleTest` will fail to compile because `ToggleTodayFast` is gone — rewrite
its cases against `SetFastStatus` in this same task; that test's subject is the toggle behaviour,
which now lives in `SetFastStatus`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/tracker/ \
        app/src/test/java/com/arshadshah/nimaz/presentation/viewmodel/tracker/
git commit -m "feat(fasting): the selected day is a real day, not a relabelled today

Any day now carries its own record, its own week and its own prayer schedule."
```

---

# Phase 3 — The make-up destination

### Task 10: `MakeupFastsScreen` and its route

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/fasting/MakeupFastsScreen.kt`
- Delete: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/fasting/MakeupFastsTab.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/core/navigation/Route.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/core/navigation/ScreenTags.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/core/navigation/NavGraph.kt:888-920`
- Modify: `docs/NAVIGATION.md` §2, §3
- Test: `app/src/androidTest/.../FeatureNavigationTest.kt` (extend)

**Interfaces:**
- Consumes: `FastingViewModel.makeupState`, `FastingEvent.CompleteMakeupFast`, `FastingEvent.PayFidya`, `FastingEvent.UpdateMakeupFast` — all unchanged.
- Produces: `Route.MakeupFasts`, `ScreenTags.MakeupFasts`, `@Composable fun MakeupFastsScreen(onNavigateBack: () -> Unit, viewModel: FastingViewModel = hiltViewModel())`. Task 12's make-up row navigates to it.

- [ ] **Step 1: Add the route and the tag**

In `Route.kt`, beside the other fasting routes:

```kotlin
    @Serializable
    data object MakeupFasts : Route
```

In `ScreenTags.kt`, beside the other fasting tags:

```kotlin
    const val MakeupFasts = "makeup_fasts_screen"
```

- [ ] **Step 2: Write the screen**

Create `MakeupFastsScreen.kt` by moving the content of `MakeupFastsTab.kt` under a scaffold and
splitting it into Owed / Settled:

```kotlin
@Composable
fun MakeupFastsScreen(
    onNavigateBack: () -> Unit,
    viewModel: FastingViewModel = hiltViewModel(),
) {
    val makeupState by viewModel.makeupState.collectAsStateWithLifecycle()

    NimazScreenScaffold(
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.fasting_makeup_title),
                onBackClick = onNavigateBack,
            )
        }
    ) { paddingValues ->
        val owed = makeupState.allMakeupFasts.filter { it.status == MakeupFastStatus.PENDING }
        val settled = makeupState.allMakeupFasts.filter { it.status != MakeupFastStatus.PENDING }

        if (owed.isEmpty() && settled.isEmpty()) {
            NimazEmptyState(
                title = stringResource(R.string.fasting_makeup_empty_title),
                message = stringResource(R.string.fasting_makeup_empty_message),
                modifier = Modifier.padding(paddingValues),
            )
            return@NimazScreenScaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (owed.isNotEmpty()) {
                item {
                    MakeupGroup(
                        title = stringResource(R.string.fasting_makeup_owed),
                        countLabel = pluralStringResource(
                            R.plurals.fasting_makeup_days, owed.size, owed.size
                        ),
                        fasts = owed,
                        currency = makeupState.currency,
                        onComplete = { viewModel.onEvent(FastingEvent.CompleteMakeupFast(it)) },
                        onPayFidya = { id, amount ->
                            viewModel.onEvent(FastingEvent.PayFidya(id, amount))
                        },
                    )
                }
            }
            if (settled.isNotEmpty()) {
                item {
                    MakeupGroup(
                        title = stringResource(R.string.fasting_makeup_settled),
                        countLabel = pluralStringResource(
                            R.plurals.fasting_makeup_days, settled.size, settled.size
                        ),
                        fasts = settled,
                        currency = makeupState.currency,
                        onComplete = null,
                        onPayFidya = null,
                    )
                }
            }
        }
    }
}
```

`MakeupGroup` is a private composable: a `NimazSectionHeader(title, trailingText = countLabel)`
above a single `NimazCard` containing one row per fast, separated by `NimazDivider`. Each row shows
`formatMediumDate(originalDate)`, the reason, and either a "Mark done" `NimazButton(variant = TEXT)`
(when `onComplete != null`) or a settled label — `formatCurrency(fidyaAmount, currency)` for
`FIDYA_PAID`, the completed date for `COMPLETED`. Carry over the existing row rendering from
`MakeupFastsTab.kt` rather than reinventing it.

- [ ] **Step 3: Wire it into `NavGraph`**

```kotlin
            taggedComposable<Route.MakeupFasts>(ScreenTags.MakeupFasts) {
                MakeupFastsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
```

- [ ] **Step 4: Add the strings**

Add to `app/src/main/res/values/strings.xml` (and every shipped locale, or `lintDebug` fails on
`MissingTranslation`):

```xml
    <string name="fasting_makeup_title">Make-up fasts</string>
    <string name="fasting_makeup_owed">Owed</string>
    <string name="fasting_makeup_settled">Settled</string>
    <string name="fasting_makeup_empty_title">Nothing to make up</string>
    <string name="fasting_makeup_empty_message">All your fasts are accounted for.</string>
    <plurals name="fasting_makeup_days">
        <item quantity="one">%d day</item>
        <item quantity="other">%d days</item>
    </plurals>
```

Reuse any of these that already exist rather than adding a duplicate — check with
`grep -n "fasting_makeup" app/src/main/res/values/strings.xml` first.

- [ ] **Step 5: Update `docs/NAVIGATION.md`**

Add `Route.MakeupFasts` to the §3 route reference table with its `ScreenTags` value and a
one-line description; bump the destination count wherever §1/§3 states it; add the
`FastingHome → MakeupFasts` edge to the §2 mermaid map.

- [ ] **Step 6: Verify**

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:assembleDebugAndroidTest
python3 scripts/check_docs.py
npm install --no-save mermaid jsdom && node scripts/check_mermaid.mjs
```

Expected: all pass. `check_docs.py` fails loudly if the route is undocumented or the count is stale
— that is the check doing its job, so read its message rather than guessing.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/screens/fasting/ \
        app/src/main/java/com/arshadshah/nimaz/core/navigation/ \
        app/src/main/res/values/strings.xml docs/NAVIGATION.md
git rm app/src/main/java/com/arshadshah/nimaz/presentation/screens/fasting/MakeupFastsTab.kt
git commit -m "feat(fasting): make-up fasts becomes a place you go, not a tab you find"
```

---

# Phase 4 — The screen

### Task 11: The day card, the sheets and the rail

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/fasting/FastingDayCard.kt`
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/fasting/FastExemptionSheet.kt`
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/fasting/FastNoteSheet.kt`
- Delete: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/fasting/FastManagementBottomSheet.kt`
- Test: `app/src/testDebug/java/com/arshadshah/nimaz/presentation/screens/fasting/FastingDayCardTest.kt`

**Interfaces:**
- Consumes: `NimazSegmentedControl`, `NimazWindowTrack`, `NimazDayRail`, `NimazStatusDotSpec` (Phase 1); `FastingTrackerUiState` (Task 9).
- Produces:
  - `@Composable fun FastingDayCard(state: FastingTrackerUiState, ramadanDay: Int?, onSetStatus: (FastStatus) -> Unit, onOpenExemption: () -> Unit, onOpenNote: () -> Unit, onBackToToday: () -> Unit, modifier: Modifier = Modifier)`
  - `@Composable fun FastingWeekRail(state: FastingTrackerUiState, today: LocalDate, onSelectDate: (LocalDate) -> Unit, modifier: Modifier = Modifier)`
  - `@Composable fun FastExemptionSheet(isVisible: Boolean, date: LocalDate, initialReason: ExemptionReason?, onSave: (ExemptionReason) -> Unit, onDismiss: () -> Unit)`
  - `@Composable fun FastNoteSheet(isVisible: Boolean, date: LocalDate, initialNote: String, onSave: (String) -> Unit, onDismiss: () -> Unit)`
  - Task 12 composes all four.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.arshadshah.nimaz.presentation.screens.fasting

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.domain.model.FastStatus
import com.arshadshah.nimaz.presentation.components.atoms.createComponentComposeRule
import com.arshadshah.nimaz.presentation.components.atoms.setThemedContent
import com.arshadshah.nimaz.presentation.viewmodel.tracker.FastingTrackerUiState
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FastingDayCardTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun state(
        date: LocalDate = LocalDate.of(2026, 8, 13),
        isToday: Boolean = true,
        record: com.arshadshah.nimaz.domain.model.FastRecord? = null,
    ) = FastingTrackerUiState(
        selectedDate = date,
        selectedRecord = record,
        isSelectedToday = isToday,
        isLoading = false,
    )

    @Test
    fun `an unlogged day selects no status`() {
        composeRule.setThemedContent {
            FastingDayCard(
                state = state(),
                ramadanDay = null,
                onSetStatus = {},
                onOpenExemption = {},
                onOpenNote = {},
                onBackToToday = {},
            )
        }
        composeRule.onNodeWithText("Not logged yet").assertIsDisplayed()
    }

    @Test
    fun `tapping Fasted reports the fasted status`() {
        var observed: FastStatus? = null
        composeRule.setThemedContent {
            FastingDayCard(
                state = state(),
                ramadanDay = null,
                onSetStatus = { observed = it },
                onOpenExemption = {},
                onOpenNote = {},
                onBackToToday = {},
            )
        }
        composeRule.onNodeWithText("Fasted").performClick()
        assertThat(observed).isEqualTo(FastStatus.FASTED)
    }

    @Test
    fun `tapping Exempt opens the reason sheet instead of writing a status`() {
        var status: FastStatus? = null
        var opened = false
        composeRule.setThemedContent {
            FastingDayCard(
                state = state(),
                ramadanDay = null,
                onSetStatus = { status = it },
                onOpenExemption = { opened = true },
                onOpenNote = {},
                onBackToToday = {},
            )
        }
        composeRule.onNodeWithText("Exempt").performClick()
        assertThat(opened).isTrue()
        assertThat(status).isNull()
    }

    @Test
    fun `Back to today appears only when the selection is not today`() {
        composeRule.setThemedContent {
            FastingDayCard(
                state = state(date = LocalDate.of(2026, 8, 11), isToday = false),
                ramadanDay = null,
                onSetStatus = {},
                onOpenExemption = {},
                onOpenNote = {},
                onBackToToday = {},
            )
        }
        composeRule.onNodeWithText("Back to today").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run the test and verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*FastingDayCardTest*"`
Expected: FAIL — `Unresolved reference: FastingDayCard`.

- [ ] **Step 3: Write the implementations**

`FastingDayCard.kt` — a `NimazCard(style = ELEVATED, tone = NEUTRAL)` laying out, top to bottom:

1. A `Row`: `Column` with `state.selectedDate.formatWeekdayDayMonth()` (`titleMedium`, SemiBold) and
   the Hijri line (`HijriDateCalculator.toHijri(selectedDate).formatted()` plus the Arabic-numeral
   form); trailing `NimazBadge` with the Ramadan day when `ramadanDay != null`, else a
   `NimazButton(text = stringResource(R.string.fasting_back_to_today), variant = TEXT, onClick = onBackToToday)`
   shown only when `!state.isSelectedToday`.
2. The lede `Row`: a clock `NimazIcon` and text driven by `rememberNow(TickResolution.MINUTES)` —
   `fasting_window_suhoor_in` before suhoor, `fasting_window_iftar_in` between, `fasting_window_closed`
   after, and `fasting_window_length` when `!isSelectedToday`.
3. `NimazWindowTrack(startLabel = suhoor ends, startValue = clockTimeText(selectedSuhoorAt), endLabel = iftar, endValue = clockTimeText(selectedIftarAt), progress = if (isSelectedToday) fraction else null)`.
   The fraction is `(now - suhoor) / (iftar - suhoor)`, and it is only computed when both instants
   are non-null; render `"--:--"` for a missing time exactly as the current screen does.
4. A `fasting_this_day` label, then `NimazSegmentedControl` with three options —
   `Icons.Default.Check`/SUCCESS, `Icons.Default.Clear`/NEUTRAL, `Icons.Default.Info`/WARNING.
   `selectedIndex` maps from `state.selectedRecord?.status`: `FASTED` → 0, `NOT_FASTED` → 1,
   `EXEMPTED` and `MAKEUP_DUE` → 2, `null` → `null`. `onSelect` routes index 2 to `onOpenExemption`
   and the others to `onSetStatus`.
5. A footer `FlowRow` of `NimazChip`s plus a trailing "Add a note" `NimazButton(variant = TEXT)`.

`FastingWeekRail.kt` (in the same file) maps `state.weekRecords` into `NimazDayRailItem`s over the
Monday–Sunday of `state.selectedDate`, with `marker` from status —
`FASTED` → `NimazStatusDotSpec(SUCCESS)`, `NOT_FASTED` → `NimazStatusDotSpec(NEUTRAL, OUTLINED)`,
`EXEMPTED` → `NimazStatusDotSpec(MUTED)`, `MAKEUP_DUE` → `NimazStatusDotSpec(WARNING)` — and
`enabled = !date.isAfter(today)`.

`FastExemptionSheet.kt` — a `NimazBottomSheet` with a title, the date as a subtitle, a `FlowRow` of
`NimazFilterChip`s over `ExemptionReason.entries`, and Cancel / Save `NimazButton`s. Save emits the
selected reason, defaulting to `ExemptionReason.OTHER` when none is picked.

`FastNoteSheet.kt` — a `NimazBottomSheet` with an `OutlinedTextField` seeded from `initialNote` and
Cancel / Save `NimazButton`s.

Each of the four gets light and dark `@Preview`s.

- [ ] **Step 4: Run the test and verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*FastingDayCardTest*"`
Expected: PASS, 4 tests.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/screens/fasting/ \
        app/src/testDebug/java/com/arshadshah/nimaz/presentation/screens/fasting/ \
        app/src/main/res/values/strings.xml
git rm app/src/main/java/com/arshadshah/nimaz/presentation/screens/fasting/FastManagementBottomSheet.kt
git commit -m "feat(fasting): the day card says what the day is

One heavyweight sheet becomes an inline control and two small ones."
```

---

### Task 12: Rebuild `FastTrackerScreen`

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/fasting/FastTrackerScreen.kt`
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/fasting/FastingComingUp.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/components/molecules/RamadanCards.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/core/navigation/NavGraph.kt:888-903`
- Delete: `FastingSubtitles.kt`, `app/src/test/.../FastingSubtitlesTest.kt`

**Interfaces:**
- Consumes: everything produced by Tasks 1–11.
- Produces: `FastTrackerScreen(onNavigateBack, onNavigateToCalendar, onNavigateToMakeup, viewModel)` — note `onNavigateToHistory` is **replaced** by `onNavigateToMakeup`; update all three `NavGraph` call sites.

- [ ] **Step 1: Move the "Coming up" derivation into its own file**

Cut `RecommendedFastsSection`'s date derivation (`FastTrackerScreen.kt:719-839`) into
`FastingComingUp.kt` as a `@Composable fun rememberComingUpFasts(records: List<FastRecord>, daysUntilAyyamAlBeed: Int): List<ComingUpFast>`
returning `data class ComingUpFast(val whenLabel: String, val name: String, val why: String, val date: LocalDate, val isLogged: Boolean)`.
Move it **unchanged** — this task rewrites presentation, not the Hijri arithmetic, and changing
both at once makes a regression impossible to attribute.

Then add `@Composable fun ComingUpRow(fasts: List<ComingUpFast>, onLogFast: (LocalDate) -> Unit)`:
a `LazyRow` of fixed-width `NimazCard(onClick = …)`s showing when / name / why and a footer that
reads "Logged" with a check when `isLogged`, else "Log this fast" with a plus.

- [ ] **Step 2: Rebuild the Ramadan strip**

In `RamadanCards.kt`, replace `RamadanBanner`'s `LinearProgressIndicator` with
`NimazProgressTrack(progress = fastedDays.toFloat() / totalDays, tone = NimazTone.ACCENT, gradient = true, size = NimazProgressSize.THIN)`
and fold the three counts (fasted / missed / to go) into the banner as an inline stat row, so the
separate `NimazStatsGrid` and `RamadanMissedFastsTracker` items are no longer needed on the screen.
Guard the division: `totalDays` of zero must not produce `NaN` — though `NimazProgressTrack` coerces
it, a caller should not lean on that.

- [ ] **Step 3: Rebuild the screen body**

Replace `FastTrackerScreen`'s body with a single `LazyColumn` (no `PrimaryTabRow`, no
`selectedTab`, no `showCalendar`/`showRecommended`) holding, in order: the Ramadan strip or
countdown card, `FastingWeekRail`, `FastingDayCard`, the month section
(`NimazSectionHeader` + the existing `FastingCalendarSection`, now passing `indicatorStyle` for
`NOT_FASTED`), `NimazSectionHeader` + `ComingUpRow`, and the make-up `NimazCard(onClick = onNavigateToMakeup)`.

Hold the two sheets' visibility in `rememberSaveable` state in the screen:

```kotlin
    var exemptionSheetDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var noteSheetDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }
```

Delete `FastingGoDeeperGroup`, `LogFastButton`, `TodayFastSection`, `RecommendedFastsSection`,
`RecommendedFastCard`, and the previews that referenced them. Delete `FastingSubtitles.kt` and
`FastingSubtitlesTest.kt`.

- [ ] **Step 4: Update the three `NavGraph` call sites**

All three of `Route.FastingHome`, `Route.FastingTracker` and `Route.FastingStats` now pass:

```kotlin
                FastTrackerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCalendar = { navController.navigate(Route.IslamicCalendar) },
                    onNavigateToMakeup = { navController.navigate(Route.MakeupFasts) },
                )
```

- [ ] **Step 5: Run the full gate**

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebugAndroidTest
python3 scripts/check_docs.py
npm install --no-save mermaid jsdom && node scripts/check_mermaid.mjs
```

Expected: all six pass. `lintDebug` takes roughly ten minutes and is CI-blocking — do not skip it,
and do not report success until you have read its result.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/ \
        app/src/main/java/com/arshadshah/nimaz/core/navigation/NavGraph.kt \
        app/src/main/res/values/strings.xml
git rm app/src/main/java/com/arshadshah/nimaz/presentation/screens/fasting/FastingSubtitles.kt \
       app/src/test/java/com/arshadshah/nimaz/presentation/screens/fasting/FastingSubtitlesTest.kt
git commit -m "feat(fasting): one scroll that reports the day

The tab row, the Go deeper group and the two expanders are gone; what they
hid is simply on the screen."
```

---

### Task 13: Documentation sweep

**Files:**
- Modify: `docs/ARCHITECTURE.md` §9 (deviation registry), `docs/SUBSYSTEMS.md` if any behaviour listed there changed
- Modify: `docs/CLEAN_ARCHITECTURE_CHECKLIST.md`

- [ ] **Step 1: Record the follow-up**

Add to `docs/ARCHITECTURE.md` §9 as an **open** item:

```markdown
- **Open — eight hand-rolled progress bars.** `NimazProgressTrack` now exists, but
  `QaidaCourseHeader`, `QuranAudioBottomBar`, `QuranSurahInfoComponents`, `QuranSurahListItem`,
  `search/AskComponents`, `settings/SyncScreen` and `settings/WidgetsScreen` still call
  `LinearProgressIndicator` directly. Migrating them was deliberately kept out of the fasting
  redesign so an app-wide sweep did not ride along inside a screen change.
- **Open — `NimazPillTabs` vs `NimazSegmentedControl`.** Two components with overlapping looks and
  genuinely different jobs (switching views vs choosing a value). Consolidating them is its own
  decision, not a side effect.
```

- [ ] **Step 2: Tick anything resolved in the checklist**

If the redesign resolved an entry in `docs/CLEAN_ARCHITECTURE_CHECKLIST.md` (for example a screen
reading the clock at composition), tick it. Do not invent entries.

- [ ] **Step 3: Verify and commit**

```bash
python3 scripts/check_docs.py
git add docs/
git commit -m "docs: what the fasting redesign left for later"
```

---

## Self-Review

**Spec coverage.** Spec §1.1–§1.6 → Tasks 1–7. §2.1 items 1 and 5 → Task 12 steps 1–2; item 2 → Task 11; item 3 → Task 11; items 4 and 6 → Task 12 step 3. §2.2/§2.3 → Tasks 9 and 11. §2.4 → Task 10. §3 → Task 10. §4 → Task 9. §5 → the Global Constraints plus the string steps in Tasks 10–12. §6 → the test step of every task. §7 → Task 12 step 5 and Task 10 step 6. §8 → Task 13.

**Known soft spots the implementer must resolve by reading, not guessing:**
- Task 9's `buildRecord` must be factored out of the existing `saveFastForDate` rather than rewritten.
- Task 9's tests depend on helper methods in the existing `FastingViewModelTest` fakes; the task says to read that file and extend the fakes rather than introduce new ones.
- Task 11 describes the day card's layout in prose because the exact `Row`/`Column` nesting is not load-bearing; its four assertions are.

---

## Execution Handoff

Phase 1 (Tasks 1–8) ends at a review gate: the atoms exist with previews and tests, and nothing consumes them yet.
