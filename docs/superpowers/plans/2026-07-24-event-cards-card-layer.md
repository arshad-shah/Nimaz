# Event Cards — Card Layer Implementation Plan (Plan 1 of 3)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the reusable `EventCard` organism, its occasion styling, rebuild `JumuahCard` on top of it, and place event cards into the existing horizontal `NimazCarousel` on Home — all verifiable in Android Studio previews before any FCM/data work.

**Architecture:** `EventCard` is a new presentational organism composing existing atoms (`NimazCard`, `NimazIcon` CONTAINED well, `QuranOrnamentalDivider`, `QaidaCelebrationBurst`, `NimazPatternBackground`, `NimazButton`, `ArabicText`). A presentation-only `EventOccasion` enum maps each occasion → accent/icon/background ornament. `JumuahCard` becomes a thin wrapper over `EventCard`. A new `EventsCarousel` organism mirrors `TodayCarousel`: it builds a `List<EventCardUi>` and feeds `NimazCarousel(count, pageHeight) { EventCard(...) }`. On Home, Jumu'ah stops being a standalone stacked item and becomes one page in that carousel.

**Tech Stack:** Kotlin, Jetpack Compose, Material3, Hilt (not touched here), Robolectric + Compose UI test.

## Global Constraints

- Package root: `com.arshadshah.nimaz`. App name **Nimaz**.
- No hardcoded `Color(0xFF…)` in screens/organisms — use `MaterialTheme.colorScheme.*` / `NimazColors.*` / `NimazPalette.*`. (Occasion accents come from `NimazPalette`.)
- Reuse existing atoms — no hand-rolled `Box` icon wells or `Box` dividers.
- Contrast (light/white surface): accent lives in icon well, chip tint, border, divider only. Body copy stays `onSurface`/`onSurfaceVariant`. Gold is structural, never text.
- Ornaments render only through `NimazPatternBackground` / `QaidaCelebrationBurst` (both respect `LocalShowIslamicPatterns` / `LocalInspectionMode`). Never read `LocalShowIslamicPatterns` directly. No emoji/ASCII ornaments.
- Tests: `app/src/testDebug/java/...`, `@RunWith(RobolectricTestRunner::class)`, `createComponentComposeRule()` + `setThemedContent { }` from the layer's `*TestSupport.kt`. Truth `assertThat`.
- Verify command: `./gradlew :app:testDebugUnitTest`. Compile check: `./gradlew :app:compileDebugKotlin`.
- Requires JDK 21 + Android SDK (compileSdk 36).
- `IslamicEventCard` molecule is **left untouched** (future cleanup — see spec §8).
- Do not push to `dev`. Work on branch `feat/event-cards-celebration-routing`.

---

## File Structure

- Create `presentation/components/organisms/EventCard.kt` — the presentational organism + `EventOrnament` + `EventAction`.
- Create `presentation/components/organisms/EventCardVisuals.kt` — `EventOccasion` enum + `eventCardVisualsFor(occasion)` mapping + occasion preview matrix.
- Modify `presentation/components/organisms/JumuahCard.kt` — rebuild on `EventCard`.
- Create `presentation/components/organisms/EventsCarousel.kt` — carousel + `EventCardUi` model.
- Modify `presentation/screens/home/HomeScreen.kt` — swap the standalone `JumuahCard` item for an `EventsCarousel` item (compact + tablet).
- Create `app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/organisms/EventCardTest.kt`
- Create `app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/organisms/EventsCarouselTest.kt`

---

## Task 0: Verify the organism test-support helper exists

**Files:**
- Inspect: `app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/organisms/OrganismTestSupport.kt`
- Reference: `app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/atoms/AtomTestSupport.kt`

**Interfaces:**
- Produces: `createComponentComposeRule(): ComposeContentTestRule` and `ComposeContentTestRule.setThemedContent(content)` usable from organism tests.

- [ ] **Step 1: Check the helper file**

Run: `sed -n '1,40p' app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/organisms/OrganismTestSupport.kt`
Expected: it defines `createComponentComposeRule()` and `setThemedContent { }` (same names as `AtomTestSupport.kt`).

- [ ] **Step 2: If missing or differently named, add it**

If the file does not exist or lacks those helpers, create it mirroring `AtomTestSupport.kt`:

```kotlin
package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule

internal fun ComposeContentTestRule.setThemedContent(content: @Composable () -> Unit) {
    setContent { MaterialTheme { content() } }
}

@Suppress("DEPRECATION")
internal fun createComponentComposeRule(): ComposeContentTestRule = createComposeRule()
```

- [ ] **Step 3: Commit only if you created the file**

```bash
git add app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/organisms/OrganismTestSupport.kt
git commit -m "test(organisms): add compose test-support helper"
```

(If the helper already existed, skip the commit.)

---

## Task 1: `EventCard` organism (presentational core)

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/components/organisms/EventCard.kt`
- Test: `app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/organisms/EventCardTest.kt`

**Interfaces:**
- Consumes: `NimazCard`, `NimazTone`, `NimazCardStyle` (`atoms/NimazCard.kt`); `NimazIcon`, `NimazIconType`, `NimazIconContainerShape` (`atoms/NimazIcon.kt`); `ArabicText`, `ArabicTextSize` (`atoms/ArabicText.kt`); `QuranOrnamentalDivider` (`atoms/QuranOrnamentalDivider.kt`); `QaidaCelebrationBurst` (`atoms/QaidaCelebrationBurst.kt`); `NimazPatternBackground` (`atoms/NimazPatternBackground.kt`); `NimazPatternStyle` (`theme/NimazPatternStyle.kt`); `NimazButton`, `NimazButtonVariant`, `NimazButtonSize` (`atoms/NimazButton.kt`).
- Produces:
  - `EventCard(accent, icon, eyebrow, arabic, headline, body, modifier, transliteration, proof, trailing, highlight, ornament, primaryAction, secondaryAction, onDismiss, fillHeight, containerAccent)` composable.
  - `sealed interface EventOrnament { None; data class Pattern(style: NimazPatternStyle); data class Burst(play: Boolean); Divider }`.
  - `data class EventAction(label: String, onClick: () -> Unit)`.

**Design notes (read before coding):**
- `ornament` controls the **background** treatment only: `Pattern` wraps content in `NimazPatternBackground(style)`; `Burst` draws `QaidaCelebrationBurst` behind content; `None`/`Divider` draw no background. The **ornamental `QuranOrnamentalDivider`** under the eyebrow is part of the base anatomy and is shown whenever `arabic != null` (the unwan rule). So the spec's "Burst + Divider" (Eid) = `ornament = Burst` with an `arabic` line present; "Pattern(CORNER_MEDALLION) + Divider" = `ornament = Pattern(CORNER_MEDALLION)` with `arabic` present.
- The **plain 1.dp divider** above the CTA row is shown only when at least one action is present.
- `containerAccent` defaults to `accent`; Eid uses `accent = GoldDark` (text-safe) but `containerAccent = Gold500` for the well/border tint.
- `fillHeight = true` makes the card fill a fixed carousel height (mirror `TodaysProgressCard(fillHeight = true)`); previews pass `false`.

- [ ] **Step 1: Write the failing test**

Create `app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/organisms/EventCardTest.kt`:

```kotlin
package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EventCardTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders headline and body`() {
        composeRule.setThemedContent {
            EventCard(
                accent = Color(0xFF2E7D32),
                icon = Icons.Filled.Celebration,
                eyebrow = "Eid al-Fitr",
                arabic = "عيد مبارك",
                headline = "Eid Mubarak",
                body = "Thirty days behind you.",
            )
        }
        composeRule.onNodeWithText("Eid Mubarak").assertExists()
        composeRule.onNodeWithText("Thirty days behind you.").assertExists()
    }

    @Test
    fun `proof chip is hidden when proof is null`() {
        composeRule.setThemedContent {
            EventCard(
                accent = Color(0xFF2E7D32),
                icon = Icons.Filled.Celebration,
                eyebrow = "e", arabic = null, headline = "h", body = "b",
                proof = null,
            )
        }
        composeRule.onNodeWithText("Al-Baqarah 2:185", substring = true).assertDoesNotExist()
    }

    @Test
    fun `proof chip renders ref and text when present`() {
        composeRule.setThemedContent {
            EventCard(
                accent = Color(0xFF2E7D32),
                icon = Icons.Filled.Celebration,
                eyebrow = "e", arabic = null, headline = "h", body = "b",
                proof = "Al-Baqarah 2:185" to "…complete the count.",
            )
        }
        composeRule.onNodeWithText("Al-Baqarah 2:185", substring = true).assertExists()
        composeRule.onNodeWithText("…complete the count.", substring = true).assertExists()
    }

    @Test
    fun `primary action fires onClick`() {
        var clicked = false
        composeRule.setThemedContent {
            EventCard(
                accent = Color(0xFF2E7D32),
                icon = Icons.Filled.Celebration,
                eyebrow = "e", arabic = null, headline = "h", body = "b",
                primaryAction = EventAction("Go") { clicked = true },
            )
        }
        composeRule.onNodeWithText("Go").performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `dismiss button fires onDismiss and is hidden when null`() {
        var dismissed = false
        composeRule.setThemedContent {
            EventCard(
                accent = Color(0xFF2E7D32),
                icon = Icons.Filled.Celebration,
                eyebrow = "e", arabic = null, headline = "h", body = "b",
                onDismiss = { dismissed = true },
            )
        }
        composeRule.onNodeWithContentDescription("Dismiss").performClick()
        assertThat(dismissed).isTrue()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.presentation.components.organisms.EventCardTest"`
Expected: FAIL — compilation error, `EventCard` unresolved.

- [ ] **Step 3: Write the implementation**

Create `app/src/main/java/com/arshadshah/nimaz/presentation/components/organisms/EventCard.kt`:

```kotlin
package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconContainerShape
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconType
import com.arshadshah.nimaz.presentation.components.atoms.NimazPatternBackground
import com.arshadshah.nimaz.presentation.components.atoms.QaidaCelebrationBurst
import com.arshadshah.nimaz.presentation.components.atoms.QuranOrnamentalDivider
import com.arshadshah.nimaz.presentation.theme.NimazPatternStyle
import com.arshadshah.nimaz.presentation.theme.NimazTone

/** Background/emphasis treatment for an [EventCard]. */
sealed interface EventOrnament {
    data object None : EventOrnament
    data class Pattern(val style: NimazPatternStyle) : EventOrnament
    data class Burst(val play: Boolean) : EventOrnament
    data object Divider : EventOrnament
}

/** A labelled call-to-action on an [EventCard]. */
data class EventAction(val label: String, val onClick: () -> Unit)

/**
 * White-surface occasion card (Jumu'ah, Eid, Ramadan, …) in the house style:
 * accented icon well, English + Arabic headline, an optional proof chip, and up
 * to two CTAs. Accent lives only in the well/chip/border/divider; body copy stays
 * neutral (contrast rule). Reuses existing atoms — no hand-rolled wells or dividers.
 */
@Composable
fun EventCard(
    accent: Color,
    icon: ImageVector,
    eyebrow: String,
    arabic: String?,
    headline: String,
    body: String,
    modifier: Modifier = Modifier,
    containerAccent: Color = accent,
    transliteration: String? = null,
    proof: Pair<String, String>? = null,
    trailing: (@Composable () -> Unit)? = null,
    highlight: (@Composable () -> Unit)? = null,
    ornament: EventOrnament = EventOrnament.None,
    primaryAction: EventAction? = null,
    secondaryAction: EventAction? = null,
    onDismiss: (() -> Unit)? = null,
    fillHeight: Boolean = false,
) {
    NimazCard(
        tone = NimazTone.NEUTRAL,
        style = NimazCardStyle.ELEVATED,
        modifier = modifier.fillMaxWidth(),
    ) {
        EventCardOrnamentScope(ornament) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (fillHeight) Modifier.fillMaxSize() else Modifier)
                    .padding(15.dp)
            ) {
                // Header: well + eyebrow/arabic + trailing + dismiss
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(11.dp)
                    ) {
                        NimazIcon(
                            imageVector = icon,
                            contentDescription = null,
                            type = NimazIconType.CONTAINED,
                            containerShape = NimazIconContainerShape.ROUNDED_SQUARE,
                            tint = accent,
                            containerColor = containerAccent.copy(alpha = 0.12f),
                            containerSize = 38.dp,
                            iconSize = 20.dp,
                        )
                        Column {
                            Text(
                                text = eyebrow,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (arabic != null) {
                                ArabicText(
                                    text = arabic,
                                    size = ArabicTextSize.SMALL,
                                    color = accent
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        trailing?.invoke()
                        if (onDismiss != null) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (arabic != null) {
                    QuranOrnamentalDivider(
                        color = accent.copy(alpha = 0.5f),
                        horizontalPadding = 8.dp,
                        verticalPadding = 10.dp,
                    )
                } else {
                    Spacer(Modifier.height(12.dp))
                }

                Text(
                    text = headline,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (transliteration != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = transliteration,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = accent
                    )
                }

                highlight?.let {
                    Spacer(Modifier.height(12.dp))
                    it()
                }

                if (proof != null) {
                    Spacer(Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(11.dp))
                            .background(accent.copy(alpha = 0.08f))
                            .padding(horizontal = 13.dp, vertical = 11.dp)
                    ) {
                        Text(
                            text = proof.first,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = proof.second,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (primaryAction != null || secondaryAction != null) {
                    Spacer(Modifier.height(11.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    )
                    Spacer(Modifier.height(11.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        primaryAction?.let {
                            NimazButton(
                                text = it.label,
                                onClick = it.onClick,
                                variant = NimazButtonVariant.TONAL,
                                size = NimazButtonSize.SMALL,
                                accent = accent,
                            )
                        }
                        secondaryAction?.let {
                            NimazButton(
                                text = it.label,
                                onClick = it.onClick,
                                variant = NimazButtonVariant.TEXT,
                                size = NimazButtonSize.SMALL,
                                accent = accent,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Wraps [content] in the chosen background ornament, respecting user pattern prefs. */
@Composable
private fun EventCardOrnamentScope(
    ornament: EventOrnament,
    content: @Composable () -> Unit,
) {
    when (ornament) {
        is EventOrnament.Pattern ->
            NimazPatternBackground(
                style = ornament.style,
                surface = MaterialTheme.colorScheme.surface,
                alphaScale = 0.6f,
            ) { content() }

        is EventOrnament.Burst ->
            Box {
                QaidaCelebrationBurst(play = ornament.play)
                content()
            }

        EventOrnament.None, EventOrnament.Divider -> content()
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.presentation.components.organisms.EventCardTest"`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/components/organisms/EventCard.kt \
        app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/organisms/EventCardTest.kt
git commit -m "feat(ui): EventCard organism (presentational core)"
```

---

## Task 2: `EventOccasion` styling + full preview matrix

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/components/organisms/EventCardVisuals.kt`

**Interfaces:**
- Consumes: `EventCard`, `EventOrnament`, `EventAction` (Task 1); `NimazPalette` (`theme/Palette.kt`); `NimazPatternStyle` (`theme/NimazPatternStyle.kt`); `NimazTheme`, `ThemeMode` (`theme/Theme.kt`).
- Produces:
  - `enum class EventOccasion { EID_AL_FITR, EID_AL_ADHA, RAMADAN, LAYLAT_AL_QADR, ARAFAH, ASHURA, MAWLID, HIJRI_NEW_YEAR, JUMUAH, GENERIC }`.
  - `data class EventCardVisuals(accent: Color, containerAccent: Color, icon: ImageVector, ornament: EventOrnament)`.
  - `fun eventCardVisualsFor(occasion: EventOccasion): EventCardVisuals`.

**Design note:** this is a pure, side-effect-free mapping (a `when`), so a Robolectric test is unnecessary; the preview matrix is the verification surface. Colours come from `NimazPalette`; icons from `material-icons-extended` (already a dependency — `JumuahCard` uses `Icons.Filled.Mosque`).

- [ ] **Step 1: Write the implementation**

Create `app/src/main/java/com/arshadshah/nimaz/presentation/components/organisms/EventCardVisuals.kt`:

```kotlin
package com.arshadshah.nimaz.presentation.components.organisms

import android.content.res.Configuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import com.arshadshah.nimaz.presentation.theme.NimazPatternStyle
import com.arshadshah.nimaz.presentation.theme.NimazPalette
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/** Occasion behind an [EventCard] — selects accent, icon, and background ornament. */
enum class EventOccasion {
    EID_AL_FITR, EID_AL_ADHA, RAMADAN, LAYLAT_AL_QADR, ARAFAH,
    ASHURA, MAWLID, HIJRI_NEW_YEAR, JUMUAH, GENERIC
}

/** Resolved visual treatment for an occasion. */
data class EventCardVisuals(
    val accent: Color,
    val containerAccent: Color,
    val icon: ImageVector,
    val ornament: EventOrnament,
)

/**
 * Maps an occasion to its house-style accent/icon/ornament (spec §3.3).
 * Accents are text-safe on white; gold is structural (well/border only), so Eid
 * uses GoldDark for the icon tint but Gold500 for the well container.
 */
fun eventCardVisualsFor(occasion: EventOccasion): EventCardVisuals = when (occasion) {
    EventOccasion.EID_AL_FITR -> EventCardVisuals(
        accent = NimazPalette.GoldDark,
        containerAccent = NimazPalette.Gold500,
        icon = Icons.Filled.Celebration,
        ornament = EventOrnament.Burst(play = true),
    )
    EventOccasion.EID_AL_ADHA -> EventCardVisuals(
        accent = NimazPalette.Teal700,
        containerAccent = NimazPalette.Teal700,
        icon = Icons.Filled.Mosque,
        ornament = EventOrnament.Pattern(NimazPatternStyle.CORNER_MEDALLION),
    )
    EventOccasion.RAMADAN -> EventCardVisuals(
        accent = NimazPalette.MatPurple,
        containerAccent = NimazPalette.MatPurple,
        icon = Icons.Filled.NightsStay,
        ornament = EventOrnament.Pattern(NimazPatternStyle.LATTICE),
    )
    EventOccasion.LAYLAT_AL_QADR -> EventCardVisuals(
        accent = NimazPalette.MatPurple,
        containerAccent = NimazPalette.MatPurple,
        icon = Icons.Filled.AutoAwesome,
        ornament = EventOrnament.Pattern(NimazPatternStyle.STAR_FIELD),
    )
    EventOccasion.ARAFAH -> EventCardVisuals(
        accent = NimazPalette.Teal700,
        containerAccent = NimazPalette.Teal700,
        icon = Icons.Outlined.Terrain,
        ornament = EventOrnament.Divider,
    )
    EventOccasion.ASHURA -> EventCardVisuals(
        accent = NimazPalette.Teal700,
        containerAccent = NimazPalette.Teal700,
        icon = Icons.Outlined.WaterDrop,
        ornament = EventOrnament.Divider,
    )
    EventOccasion.MAWLID -> EventCardVisuals(
        accent = NimazPalette.Amber700,
        containerAccent = NimazPalette.Amber700,
        icon = Icons.Filled.Star,
        ornament = EventOrnament.Divider,
    )
    EventOccasion.HIJRI_NEW_YEAR -> EventCardVisuals(
        accent = NimazPalette.Amber700,
        containerAccent = NimazPalette.Amber700,
        icon = Icons.Filled.CalendarMonth,
        ornament = EventOrnament.Divider,
    )
    EventOccasion.JUMUAH -> EventCardVisuals(
        accent = NimazPalette.GreenDeep,
        containerAccent = NimazPalette.GreenDeep,
        icon = Icons.Filled.Mosque,
        ornament = EventOrnament.Divider,
    )
    EventOccasion.GENERIC -> EventCardVisuals(
        accent = NimazPalette.Teal700,
        containerAccent = NimazPalette.Teal700,
        icon = Icons.Filled.Event,
        ornament = EventOrnament.Divider,
    )
}

// ---- Preview matrix (the "so I can see it" deliverable) ----

@Composable
private fun EventCardOccasionSample(occasion: EventOccasion) {
    val v = eventCardVisualsFor(occasion)
    EventCard(
        accent = v.accent,
        containerAccent = v.containerAccent,
        icon = v.icon,
        ornament = v.ornament,
        eyebrow = occasion.name.lowercase().replaceFirstChar { it.uppercase() },
        arabic = "عيد مبارك",
        headline = "Blessed occasion",
        body = "A short, warm line about the day and what it means.",
        transliteration = "taqabbal Allāhu minnā wa minkum",
        proof = "Al-Baqarah 2:185" to "…that you may complete the count and glorify God.",
        primaryAction = EventAction("Learn more") {},
        secondaryAction = EventAction("Later") {},
        modifier = Modifier.padding(16.dp),
    )
}

@Preview(showBackground = true, widthDp = 400, name = "Eid al-Fitr — light")
@Composable
private fun EventCard_EidFitr_Light() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { EventCardOccasionSample(EventOccasion.EID_AL_FITR) }
}

@Preview(
    showBackground = true, widthDp = 400, name = "Eid al-Fitr — dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun EventCard_EidFitr_Dark() {
    NimazTheme(themeMode = ThemeMode.DARK) { EventCardOccasionSample(EventOccasion.EID_AL_FITR) }
}

@Preview(showBackground = true, widthDp = 400, name = "Eid al-Adha — light")
@Composable
private fun EventCard_EidAdha_Light() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { EventCardOccasionSample(EventOccasion.EID_AL_ADHA) }
}

@Preview(showBackground = true, widthDp = 400, name = "Ramadan — light")
@Composable
private fun EventCard_Ramadan_Light() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { EventCardOccasionSample(EventOccasion.RAMADAN) }
}

@Preview(showBackground = true, widthDp = 400, name = "Laylat al-Qadr — light")
@Composable
private fun EventCard_Qadr_Light() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { EventCardOccasionSample(EventOccasion.LAYLAT_AL_QADR) }
}

@Preview(showBackground = true, widthDp = 400, name = "Arafah — light")
@Composable
private fun EventCard_Arafah_Light() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { EventCardOccasionSample(EventOccasion.ARAFAH) }
}

@Preview(showBackground = true, widthDp = 400, name = "Ashura — light")
@Composable
private fun EventCard_Ashura_Light() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { EventCardOccasionSample(EventOccasion.ASHURA) }
}

@Preview(showBackground = true, widthDp = 400, name = "Mawlid — light")
@Composable
private fun EventCard_Mawlid_Light() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { EventCardOccasionSample(EventOccasion.MAWLID) }
}

@Preview(showBackground = true, widthDp = 400, name = "Hijri new year — light")
@Composable
private fun EventCard_Hijri_Light() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { EventCardOccasionSample(EventOccasion.HIJRI_NEW_YEAR) }
}

@Preview(showBackground = true, widthDp = 400, name = "Generic — light")
@Composable
private fun EventCard_Generic_Light() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { EventCardOccasionSample(EventOccasion.GENERIC) }
}

@Preview(showBackground = true, widthDp = 400, name = "Generic — dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun EventCard_Generic_Dark() {
    NimazTheme(themeMode = ThemeMode.DARK) { EventCardOccasionSample(EventOccasion.GENERIC) }
}
```

- [ ] **Step 2: Verify icon imports resolve (compile)**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. If any of `Terrain`, `WaterDrop`, `NightsStay`, `AutoAwesome`, `CalendarMonth`, `Celebration`, `Event` is unresolved, replace it with a present `material-icons-extended` icon (e.g. `Icons.Filled.Star`, `Icons.Filled.Event`) and re-run. Do not invent icons.

- [ ] **Step 3: Visually confirm the preview matrix**

Open `EventCardVisuals.kt` in Android Studio; render the preview panel. Confirm every occasion shows its accent/well/ornament, gold is never body text, and dark variants keep contrast.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/components/organisms/EventCardVisuals.kt
git commit -m "feat(ui): EventOccasion styling map + EventCard preview matrix"
```

---

## Task 3: Rebuild `JumuahCard` on `EventCard`

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/components/organisms/JumuahCard.kt`

**Interfaces:**
- Consumes: `EventCard`, `EventOccasion`, `eventCardVisualsFor` (Tasks 1–2); existing strings `jumuah_mubarak`, `jumuah_arabic`, `jumuah_hadith_quote`, `jumuah_passed`, `time_until_jumuah`, `khutbah_time`.
- Produces: unchanged public signature `JumuahCard(jumuahTime, timeUntilJumuah, isJumuahPassed, modifier)` — call sites in `HomeScreen.kt` keep compiling.

**Design note:** the `jumuahTime`/`khutbah_time` block becomes `trailing`; the countdown/"passed" block becomes `highlight`; the hadith quote is the `body`; the Jumu'ah name goes in `eyebrow`. `fillHeight` is added so the carousel (Task 4) can size it; standalone previews pass `false`.

- [ ] **Step 1: Update the failing expectation (rewrite the two previews + add fillHeight)**

Replace the entire body of `JumuahCard.kt` with:

```kotlin
package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Friday-only Jumu'ah highlight, built on [EventCard]. English/Arabic name, khutbah
 * time (trailing), a countdown-to-khutbah or "passed" acknowledgement (highlight),
 * and a hadith. Public signature unchanged so Home call sites are untouched.
 */
@Composable
fun JumuahCard(
    jumuahTime: String,
    timeUntilJumuah: String,
    isJumuahPassed: Boolean,
    modifier: Modifier = Modifier,
    fillHeight: Boolean = false,
) {
    val v = eventCardVisualsFor(EventOccasion.JUMUAH)
    EventCard(
        accent = v.accent,
        containerAccent = v.containerAccent,
        icon = Icons.Filled.Mosque,
        ornament = v.ornament,
        eyebrow = stringResource(R.string.jumuah_mubarak),
        arabic = stringResource(R.string.jumuah_arabic),
        headline = stringResource(R.string.jumuah_mubarak),
        body = stringResource(R.string.jumuah_hadith_quote),
        fillHeight = fillHeight,
        trailing = if (jumuahTime.isNotEmpty()) {
            {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = jumuahTime,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.khutbah_time),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else null,
        highlight = {
            if (isJumuahPassed) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.jumuah_passed),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = v.accent,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (timeUntilJumuah.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.time_until_jumuah),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = timeUntilJumuah,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = v.accent
                    )
                }
            }
        },
        modifier = modifier,
    )
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun JumuahCard_Preview() {
    NimazTheme {
        JumuahCard(
            jumuahTime = "1:30 PM",
            timeUntilJumuah = "3h 15m",
            isJumuahPassed = false,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Passed")
@Composable
private fun JumuahCard_Passed_Preview() {
    NimazTheme {
        JumuahCard(
            jumuahTime = "1:30 PM",
            timeUntilJumuah = "",
            isJumuahPassed = true,
            modifier = Modifier.padding(16.dp)
        )
    }
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL — `HomeScreen.kt` still compiles against the unchanged `JumuahCard(...)` signature (the added `fillHeight` has a default).

- [ ] **Step 3: Confirm previews render**

Open `JumuahCard.kt` in Android Studio; confirm both previews (active + passed) render with the green accent, ornamental divider, khutbah time, and countdown/passed states.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/components/organisms/JumuahCard.kt
git commit -m "refactor(ui): rebuild JumuahCard on EventCard"
```

---

## Task 4: `EventsCarousel` + Home wiring (Jumu'ah becomes a carousel page)

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/components/organisms/EventsCarousel.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/home/HomeScreen.kt`
- Test: `app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/organisms/EventsCarouselTest.kt`

**Interfaces:**
- Consumes: `NimazCarousel` (`organisms/NimazCarousel.kt`); `EventCard`, `EventAction`, `EventOrnament` (Task 1); `EventOccasion`, `eventCardVisualsFor` (Task 2); `JumuahCard` (Task 3).
- Produces:
  - `data class EventCardUi(occasion, eyebrow, arabic, headline, body, transliteration, proof, primaryAction, secondaryAction, onDismiss, jumuahTime, timeUntilJumuah, isJumuahPassed)`.
  - `EventsCarousel(events: List<EventCardUi>, modifier, pageHeight, horizontalPadding)` composable — renders nothing when `events` is empty.

**Design note:** `NimazCarousel` uses one fixed `pageHeight` for all pages. Default `320.dp`, sized for the richest variant (Arabic + transliteration + proof + two CTAs); `EventCard(fillHeight = true)` distributes shorter cards. Jumu'ah is modelled as an `EventCardUi` with `occasion = JUMUAH` and the three jumuah fields set; the carousel renders it via `JumuahCard(...)` to keep the trailing/highlight blocks. On Home the standalone `if (isFriday) item { JumuahCard(...) }` blocks are replaced by an `EventsCarousel` item built from Home state (Jumu'ah only, for now — richer occasions arrive in Plan 3).

- [ ] **Step 1: Write the failing test**

Create `app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/organisms/EventsCarouselTest.kt`:

```kotlin
package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EventsCarouselTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders the first page headline`() {
        composeRule.setThemedContent {
            EventsCarousel(
                events = listOf(
                    EventCardUi(
                        occasion = EventOccasion.GENERIC,
                        eyebrow = "Occasion",
                        headline = "Blessed day",
                        body = "A warm line.",
                    )
                )
            )
        }
        composeRule.onNodeWithText("Blessed day").assertExists()
    }

    @Test
    fun `renders nothing when the list is empty`() {
        composeRule.setThemedContent {
            EventsCarousel(events = emptyList())
        }
        composeRule.onNodeWithText("Blessed day").assertDoesNotExist()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.presentation.components.organisms.EventsCarouselTest"`
Expected: FAIL — `EventsCarousel` / `EventCardUi` unresolved.

- [ ] **Step 3: Write the implementation**

Create `app/src/main/java/com/arshadshah/nimaz/presentation/components/organisms/EventsCarousel.kt`:

```kotlin
package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * A single event card's display data. Jumu'ah carries the three jumuah_* fields and
 * is rendered via [JumuahCard]; all other occasions render via [EventCard].
 */
data class EventCardUi(
    val occasion: EventOccasion,
    val eyebrow: String,
    val headline: String,
    val body: String,
    val arabic: String? = null,
    val transliteration: String? = null,
    val proof: Pair<String, String>? = null,
    val primaryAction: EventAction? = null,
    val secondaryAction: EventAction? = null,
    val onDismiss: (() -> Unit)? = null,
    val jumuahTime: String = "",
    val timeUntilJumuah: String = "",
    val isJumuahPassed: Boolean = false,
)

/**
 * Horizontal carousel of occasion cards, reusing [NimazCarousel] (edge-peek + dots,
 * swipe-only). One fixed [pageHeight] for every page. Renders nothing when empty.
 */
@Composable
fun EventsCarousel(
    events: List<EventCardUi>,
    modifier: Modifier = Modifier,
    pageHeight: Dp = 320.dp,
    horizontalPadding: Dp = 20.dp,
) {
    if (events.isEmpty()) return
    NimazCarousel(
        count = events.size,
        modifier = modifier,
        pageHeight = pageHeight,
        horizontalPadding = horizontalPadding,
        pageSpacing = 12.dp,
    ) { pageIndex ->
        val e = events[pageIndex]
        if (e.occasion == EventOccasion.JUMUAH) {
            JumuahCard(
                jumuahTime = e.jumuahTime,
                timeUntilJumuah = e.timeUntilJumuah,
                isJumuahPassed = e.isJumuahPassed,
                fillHeight = true,
            )
        } else {
            val v = eventCardVisualsFor(e.occasion)
            EventCard(
                accent = v.accent,
                containerAccent = v.containerAccent,
                icon = v.icon,
                ornament = v.ornament,
                eyebrow = e.eyebrow,
                arabic = e.arabic,
                headline = e.headline,
                body = e.body,
                transliteration = e.transliteration,
                proof = e.proof,
                primaryAction = e.primaryAction,
                secondaryAction = e.secondaryAction,
                onDismiss = e.onDismiss,
                fillHeight = true,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 380)
@Composable
private fun EventsCarousel_Preview() {
    NimazTheme {
        EventsCarousel(
            events = listOf(
                EventCardUi(
                    occasion = EventOccasion.EID_AL_FITR,
                    eyebrow = "Eid al-Fitr",
                    arabic = "عيد مبارك",
                    headline = "Eid Mubarak",
                    body = "Thirty days behind you. May every one be accepted.",
                    transliteration = "taqabbal Allāhu minnā wa minkum",
                    proof = "Al-Baqarah 2:185" to "…complete the count and glorify God.",
                    primaryAction = EventAction("Eid prayer time") {},
                ),
                EventCardUi(
                    occasion = EventOccasion.JUMUAH,
                    eyebrow = "Jumu'ah",
                    headline = "Jumu'ah Mubarak",
                    body = "\"The best day on which the sun rises is Friday.\"",
                    jumuahTime = "1:30 PM",
                    timeUntilJumuah = "3h 15m",
                ),
            ),
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.presentation.components.organisms.EventsCarouselTest"`
Expected: PASS (2 tests).

- [ ] **Step 5: Wire into HomeScreen (compact LazyColumn)**

In `app/src/main/java/com/arshadshah/nimaz/presentation/screens/home/HomeScreen.kt`, find the compact-layout block (guarded by `if (state.isFriday)`) that emits `item { JumuahCard(...) }` between the banners section and the `today_section` item. Replace that `if (state.isFriday) { item { JumuahCard(...) } }` block with:

```kotlin
val eventCards = buildList {
    if (state.isFriday) {
        add(
            EventCardUi(
                occasion = EventOccasion.JUMUAH,
                eyebrow = stringResource(R.string.jumuah_mubarak),
                headline = stringResource(R.string.jumuah_mubarak),
                body = stringResource(R.string.jumuah_hadith_quote),
                jumuahTime = state.jumuahTime,
                timeUntilJumuah = state.timeUntilJumuah,
                isJumuahPassed = state.isJumuahPassed,
            )
        )
    }
}
if (eventCards.isNotEmpty()) {
    item(key = "events") {
        EventsCarousel(events = eventCards)
    }
    item(key = "events_spacer") { Spacer(Modifier.height(16.dp)) }
}
```

Add imports if missing: `com.arshadshah.nimaz.presentation.components.organisms.EventsCarousel`, `EventCardUi`, `EventOccasion`, and confirm `androidx.compose.ui.res.stringResource`, `androidx.compose.foundation.layout.Spacer`, `androidx.compose.foundation.layout.height`, `androidx.compose.ui.unit.dp` are imported (they are used elsewhere in the file already). Keep the existing `JumuahCard` import only if still referenced; if the compact and tablet layouts both stop calling it directly, remove the now-unused import.

- [ ] **Step 6: Wire into HomeScreen (tablet two-pane)**

In the tablet layout, the Jumu'ah card sits in the right `verticalScroll` `Column` (guarded by `if (state.isFriday)`), above `TodaysProgressCard`. Because event cards must remain a horizontal carousel, move them out of the right column to full-width, mirroring the `HomeBannerCarousel` placement. In the top full-width `Column` (which already holds `HomeHeader`, `AnnouncementBanner`, and `HomeBannerCarousel`), add directly below the banner carousel:

```kotlin
if (state.isFriday) {
    EventsCarousel(
        events = listOf(
            EventCardUi(
                occasion = EventOccasion.JUMUAH,
                eyebrow = stringResource(R.string.jumuah_mubarak),
                headline = stringResource(R.string.jumuah_mubarak),
                body = stringResource(R.string.jumuah_hadith_quote),
                jumuahTime = state.jumuahTime,
                timeUntilJumuah = state.timeUntilJumuah,
                isJumuahPassed = state.isJumuahPassed,
            )
        ),
        modifier = Modifier.padding(top = 8.dp),
    )
}
```

Then delete the `if (state.isFriday) { JumuahCard(...) }` call from the right-hand `verticalScroll` column.

- [ ] **Step 7: Compile + full test run**

Run: `./gradlew :app:compileDebugKotlin && ./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.presentation.components.organisms.*"`
Expected: BUILD SUCCESSFUL; `EventCardTest`, `EventsCarouselTest` pass. No other test regressions in the organisms package.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/components/organisms/EventsCarousel.kt \
        app/src/main/java/com/arshadshah/nimaz/presentation/screens/home/HomeScreen.kt \
        app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/organisms/EventsCarouselTest.kt
git commit -m "feat(home): event cards ride the home carousel; Jumu'ah becomes a page"
```

---

## Task 5: Docs update

**Files:**
- Modify: `docs/ARCHITECTURE.md` (§9 registry), `docs/CLEAN_ARCHITECTURE_CHECKLIST.md`

- [ ] **Step 1: Record the deviation + ticked cleanup**

In `docs/ARCHITECTURE.md` §9, add a row noting `EventCard` (organism) and `IslamicEventCard` (molecule) both render occasion cards, with `IslamicEventCard` migration deferred (spec §8). In `docs/CLEAN_ARCHITECTURE_CHECKLIST.md`, tick that `JumuahCard`'s hand-rolled `Box` icon well and `Box` divider were removed in favour of `NimazIcon(CONTAINED)` / `QuranOrnamentalDivider`.

- [ ] **Step 2: Commit**

```bash
git add docs/ARCHITECTURE.md docs/CLEAN_ARCHITECTURE_CHECKLIST.md
git commit -m "docs: record EventCard/IslamicEventCard overlap; tick JumuahCard atom reuse"
```

---

## Self-Review

**Spec coverage (card layer, spec §2–3):**
- EventCard organism + signature (§3.2) → Task 1. ✓
- EventOrnament sealed interface (§3.3) → Task 1. ✓ (background treatment; ornamental divider is base anatomy — interpretation documented)
- EventAction → Task 1. ✓
- Occasion → accent/ornament table (§3.3) → Task 2. ✓
- Contrast rule (gold structural only) → Task 2 accent/containerAccent split + Step 3 visual check. ✓
- `LocalShowIslamicPatterns` / `LocalInspectionMode` gating → Task 1 via `NimazPatternBackground`/`QaidaCelebrationBurst`. ✓
- JumuahCard rebuilt on EventCard, call site unchanged (§3.1) → Task 3. ✓
- Event cards in the same horizontal carousel, not vertical stack (new requirement) → Task 4 `EventsCarousel` + Home wiring. ✓
- Jumu'ah as a carousel page, never suppressed (§3.4 ordering) → Task 4. ✓
- Fixed pageHeight caveat (§3) → Task 4 default 320.dp + `fillHeight`. ✓
- Previews light+dark, every occasion, patterns-off is covered by the ornament gating; **200% font-scale preview** is a manual check — add a note in Task 2 Step 3.

**Deferred to later plans (not in scope here):** parameterised routing (spec §1) → Plan 2; celebration FCM type, Hijri offset, local source + merge, prune (spec §5–8 / steps 6–10) → Plan 3. The `EventCardUi` list on Home is Jumu'ah-only until Plan 3 populates other occasions from `ObserveEventCardUseCase`.

**Placeholder scan:** no TBD/TODO; every code step shows complete code; icon-fallback instruction (Task 2 Step 2) is explicit, not vague.

**Type consistency:** `EventCard(...)` params, `EventOrnament` cases, `EventAction`, `EventCardVisuals` fields, `eventCardVisualsFor`, `EventCardUi` fields, and `EventsCarousel(...)` signature are referenced identically across Tasks 1–4. `fillHeight` added to both `EventCard` and `JumuahCard` with defaults so Home call sites stay valid.

**Note added (Task 2 Step 3):** also render a 200% font-scale preview (`@Preview(fontScale = 2f)`) of the Eid card to confirm headline truncates and the Arabic line does not, per spec §5.
