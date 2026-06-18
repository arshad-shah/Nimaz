# Names Screens Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the three generic "names" list/detail screens (99 Names of Allah, Names of the Prophet ﷺ, The Prophets) with one cohesive, accent-themed design built from shared composables.

**Architecture:** Extract three reusable molecules — `NameCard` (the locked "Refined Row" card, with an optional Prophets story variant), `NameFilterRow` (accent All/Favorites chips), and `NameDetailHeader` (calligraphic on-surface header) — plus a `NamesAccent` palette object carrying the per-screen accent (teal / purple / gold). Add a `colors` passthrough to `NimazBackTopAppBar` for the slim tinted top bar. Then rewire the three list screens and three detail screens to consume these. The tablet `Adaptive*Screen.kt` panes reuse the list/detail composables unchanged and inherit the redesign.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Hilt, Robolectric Compose UI tests (`createComponentComposeRule` / `setThemedContent` in `src/testDebug`).

## Global Constraints

- Card spec = Direction B "Refined Row": leading **gradient medallion** 52dp / `RoundedCornerShape(16.dp)` with the number; **left accent rail** 4dp; body = Arabic name (Amiri, accent-tinted) + transliteration (bold) + English meaning (muted); trailing favorite toggle. ONE reusable composable; accent is a parameter.
- Per-screen accents: 99 Names of Allah → **teal** (`primary`); Names of the Prophet ﷺ → **purple** (`tertiary`); The Prophets → **gold** (`secondary`).
- List header = **slim accent top bar** (tinted `NimazBackTopAppBar`), NOT a gradient hero banner.
- Detail header = **calligraphic on-surface** (large Amiri + accent-ringed medallion + accent divider), NOT a full-bleed gradient hero.
- Prophets list card = **layout B**: title line (`titleEnglish`, accent-tinted) + a single era chip (`era`). NO `storySummary` preview, NO Quran-mentions chip on the list card.
- New shared composables live in `presentation/components/molecules/`; their tests live in `app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/molecules/`.
- Tests use `@RunWith(RobolectricTestRunner::class)`, `createComponentComposeRule()`, `setThemedContent { … }` (see `MoleculeTestSupport.kt`).
- Run tests with: `./gradlew :app:testDebugUnitTest --tests "<FQN>"`. Compile with: `./gradlew :app:compileDebugKotlin`.
- Reuse the existing `NimazEmptyState` molecule for empty states (do not build a new one).
- Favorite-toggle content descriptions come from `R.string.add_to_favorites` ("Add to favorites") and `R.string.remove_from_favorites` ("Remove from favorites"). Verify these literal English values before asserting on them: `grep -n 'add_to_favorites\|remove_from_favorites' app/src/main/res/values/strings.xml`.

---

### Task 1: `NamesAccent` palette + `NameCard` (name variant)

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/components/molecules/NamesAccent.kt`
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/components/molecules/NameCard.kt`
- Test: `app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/molecules/NameCardTest.kt`

**Interfaces:**
- Produces:
  - `data class NamesAccent(val rail: Color, val medallion: List<Color>, val onMedallion: Color, val contentTint: Color, val chipContainer: Color, val onChipContainer: Color)`
  - `object NamesAccents { @Composable fun allah(): NamesAccent; @Composable fun prophetNames(): NamesAccent; @Composable fun prophets(): NamesAccent }`
  - `@Composable fun NameCard(number: Int, arabicName: String, primaryLabel: String, secondaryLabel: String, isFavorite: Boolean, accent: NamesAccent, onClick: () -> Unit, onFavoriteClick: () -> Unit, modifier: Modifier = Modifier, titleLabel: String? = null, eraChip: String? = null)`

- [ ] **Step 1: Write `NamesAccent.kt`**

```kotlin
package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.arshadshah.nimaz.presentation.theme.NimazColors

/**
 * Per-screen accent for the three names screens. Built from the active
 * colour-scheme (so light/dark both work) plus fixed brand medallion gradients.
 */
data class NamesAccent(
    val rail: Color,
    val medallion: List<Color>,
    val onMedallion: Color,
    val contentTint: Color,
    val chipContainer: Color,
    val onChipContainer: Color,
)

object NamesAccents {
    @Composable
    fun allah(): NamesAccent = NamesAccent(
        rail = MaterialTheme.colorScheme.primary,
        medallion = listOf(NimazColors.Primary400, NimazColors.Primary600),
        onMedallion = Color.White,
        contentTint = MaterialTheme.colorScheme.primary,
        chipContainer = MaterialTheme.colorScheme.primaryContainer,
        onChipContainer = MaterialTheme.colorScheme.onPrimaryContainer,
    )

    @Composable
    fun prophetNames(): NamesAccent = NamesAccent(
        rail = MaterialTheme.colorScheme.tertiary,
        medallion = listOf(Color(0xFF9575FF), NimazColors.Tertiary),
        onMedallion = Color.White,
        contentTint = MaterialTheme.colorScheme.tertiary,
        chipContainer = MaterialTheme.colorScheme.tertiaryContainer,
        onChipContainer = MaterialTheme.colorScheme.onTertiaryContainer,
    )

    @Composable
    fun prophets(): NamesAccent = NamesAccent(
        rail = MaterialTheme.colorScheme.secondary,
        medallion = listOf(NimazColors.Gold400, NimazColors.Gold500),
        onMedallion = Color(0xFF1C1917),
        contentTint = MaterialTheme.colorScheme.secondary,
        chipContainer = MaterialTheme.colorScheme.secondaryContainer,
        onChipContainer = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}
```

- [ ] **Step 2: Write the failing test `NameCardTest.kt`**

```kotlin
package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NameCardTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Composable
    private fun teal(): NamesAccent = NamesAccents.allah()

    @Test
    fun `renders number, arabic, transliteration and meaning`() {
        composeRule.setThemedContent {
            NameCard(
                number = 52,
                arabicName = "ArabicRahman",
                primaryLabel = "Ar-Rahman",
                secondaryLabel = "The Most Compassionate",
                isFavorite = false,
                accent = teal(),
                onClick = {},
                onFavoriteClick = {},
            )
        }
        composeRule.onNodeWithText("52").assertExists()
        composeRule.onNodeWithText("ArabicRahman").assertExists()
        composeRule.onNodeWithText("Ar-Rahman").assertExists()
        composeRule.onNodeWithText("The Most Compassionate").assertExists()
    }

    @Test
    fun `card click invokes callback`() {
        var clicked = false
        composeRule.setThemedContent {
            NameCard(
                number = 1, arabicName = "A", primaryLabel = "P", secondaryLabel = "S",
                isFavorite = false, accent = teal(),
                onClick = { clicked = true }, onFavoriteClick = {},
            )
        }
        composeRule.onNodeWithText("P").performClick()
        assertTrue(clicked)
    }

    @Test
    fun `favorite click invokes callback and shows add description when not favorite`() {
        var favClicked = false
        composeRule.setThemedContent {
            NameCard(
                number = 1, arabicName = "A", primaryLabel = "P", secondaryLabel = "S",
                isFavorite = false, accent = teal(),
                onClick = {}, onFavoriteClick = { favClicked = true },
            )
        }
        composeRule.onNodeWithContentDescription("Add to favorites").performClick()
        assertTrue(favClicked)
    }

    @Test
    fun `shows remove description when favorite`() {
        composeRule.setThemedContent {
            NameCard(
                number = 1, arabicName = "A", primaryLabel = "P", secondaryLabel = "S",
                isFavorite = true, accent = teal(),
                onClick = {}, onFavoriteClick = {},
            )
        }
        composeRule.onNodeWithContentDescription("Remove from favorites").assertExists()
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.presentation.components.molecules.NameCardTest"`
Expected: FAIL / compile error — `NameCard` unresolved.

- [ ] **Step 4: Write `NameCard.kt`**

```kotlin
package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.theme.NimazSpacing

/**
 * Shared "Refined Row" card for all three names screens. Accent is a parameter.
 * Prophets pass [titleLabel] + [eraChip] for the story variant.
 */
@Composable
fun NameCard(
    number: Int,
    arabicName: String,
    primaryLabel: String,
    secondaryLabel: String,
    isFavorite: Boolean,
    accent: NamesAccent,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleLabel: String? = null,
    eraChip: String? = null,
) {
    NimazCard(
        modifier = modifier.fillMaxWidth(),
        style = NimazCardStyle.ELEVATED,
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left accent rail
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accent.rail)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(NimazSpacing.Medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gradient medallion
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(accent.medallion)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$number",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accent.onMedallion,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.width(NimazSpacing.Medium))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ArabicText(
                        text = arabicName,
                        size = ArabicTextSize.MEDIUM,
                        color = accent.contentTint,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = primaryLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = secondaryLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (titleLabel != null) {
                        Text(
                            text = titleLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = accent.contentTint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (eraChip != null) {
                        Surface(
                            shape = RoundedCornerShape(NimazSpacing.Small),
                            color = accent.chipContainer,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = eraChip,
                                style = MaterialTheme.typography.labelSmall,
                                color = accent.onChipContainer,
                                modifier = Modifier.padding(
                                    horizontal = NimazSpacing.Small,
                                    vertical = 2.dp
                                )
                            )
                        }
                    }
                }

                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isFavorite) {
                            stringResource(R.string.remove_from_favorites)
                        } else {
                            stringResource(R.string.add_to_favorites)
                        },
                        tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.presentation.components.molecules.NameCardTest"`
Expected: PASS (4 tests).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/components/molecules/NamesAccent.kt \
        app/src/main/java/com/arshadshah/nimaz/presentation/components/molecules/NameCard.kt \
        app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/molecules/NameCardTest.kt
git commit -m "feat(names): shared NameCard + NamesAccent palette"
```

---

### Task 2: `NameCard` Prophets story variant test

**Files:**
- Modify: `app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/molecules/NameCardTest.kt`

**Interfaces:**
- Consumes: `NameCard(... titleLabel, eraChip)` and `NamesAccents.prophets()` from Task 1.

- [ ] **Step 1: Add the failing test (append inside `NameCardTest`)**

```kotlin
    @Test
    fun `prophets variant renders title line and era chip and no story summary`() {
        composeRule.setThemedContent {
            NameCard(
                number = 10,
                arabicName = "ArabicYusuf",
                primaryLabel = "Yusuf",
                secondaryLabel = "The Chosen of Allah",
                isFavorite = false,
                accent = NamesAccents.prophets(),
                onClick = {},
                onFavoriteClick = {},
                titleLabel = "Safiyyullah",
                eraChip = "Ancient Egypt",
            )
        }
        composeRule.onNodeWithText("Yusuf").assertExists()
        composeRule.onNodeWithText("Safiyyullah").assertExists()
        composeRule.onNodeWithText("Ancient Egypt").assertExists()
    }
```

- [ ] **Step 2: Run the test**

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.presentation.components.molecules.NameCardTest"`
Expected: PASS (5 tests) — the variant params already exist from Task 1, so this confirms the contract.

- [ ] **Step 3: Commit**

```bash
git add app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/molecules/NameCardTest.kt
git commit -m "test(names): cover NameCard prophets story variant"
```

---

### Task 3: `NameFilterRow` molecule

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/components/molecules/NameFilterRow.kt`
- Test: `app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/molecules/NameFilterRowTest.kt`

**Interfaces:**
- Consumes: `NamesAccent` from Task 1.
- Produces: `@Composable fun NameFilterRow(showFavoritesOnly: Boolean, onShowAll: () -> Unit, onShowFavorites: () -> Unit, accent: NamesAccent, allLabel: String, favoritesLabel: String, modifier: Modifier = Modifier)`

- [ ] **Step 1: Write the failing test `NameFilterRowTest.kt`**

```kotlin
package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NameFilterRowTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders both labels`() {
        composeRule.setThemedContent {
            NameFilterRow(
                showFavoritesOnly = false,
                onShowAll = {}, onShowFavorites = {},
                accent = NamesAccents.allah(),
                allLabel = "All", favoritesLabel = "Favorites",
            )
        }
        composeRule.onNodeWithText("All").assertExists()
        composeRule.onNodeWithText("Favorites").assertExists()
    }

    @Test
    fun `clicking favorites invokes callback`() {
        var fav = false
        composeRule.setThemedContent {
            NameFilterRow(
                showFavoritesOnly = false,
                onShowAll = {}, onShowFavorites = { fav = true },
                accent = NamesAccents.allah(),
                allLabel = "All", favoritesLabel = "Favorites",
            )
        }
        composeRule.onNodeWithText("Favorites").performClick()
        assertTrue(fav)
    }

    @Test
    fun `clicking all invokes callback`() {
        var all = false
        composeRule.setThemedContent {
            NameFilterRow(
                showFavoritesOnly = true,
                onShowAll = { all = true }, onShowFavorites = {},
                accent = NamesAccents.allah(),
                allLabel = "All", favoritesLabel = "Favorites",
            )
        }
        composeRule.onNodeWithText("All").performClick()
        assertTrue(all)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.presentation.components.molecules.NameFilterRowTest"`
Expected: FAIL — `NameFilterRow` unresolved.

- [ ] **Step 3: Write `NameFilterRow.kt`**

```kotlin
package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arshadshah.nimaz.presentation.theme.NimazSpacing

/**
 * All / Favorites filter chips with the screen accent applied to the selected state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NameFilterRow(
    showFavoritesOnly: Boolean,
    onShowAll: () -> Unit,
    onShowFavorites: () -> Unit,
    accent: NamesAccent,
    allLabel: String,
    favoritesLabel: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NimazSpacing.Small)
    ) {
        FilterChip(
            selected = !showFavoritesOnly,
            onClick = { if (showFavoritesOnly) onShowAll() },
            label = { Text(allLabel) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = accent.chipContainer,
                selectedLabelColor = accent.onChipContainer
            )
        )
        FilterChip(
            selected = showFavoritesOnly,
            onClick = { if (!showFavoritesOnly) onShowFavorites() },
            label = { Text(favoritesLabel) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = accent.chipContainer,
                selectedLabelColor = accent.onChipContainer
            )
        )
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.presentation.components.molecules.NameFilterRowTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/components/molecules/NameFilterRow.kt \
        app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/molecules/NameFilterRowTest.kt
git commit -m "feat(names): accent-themed NameFilterRow"
```

---

### Task 4: `NameDetailHeader` molecule (calligraphic on-surface)

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/components/molecules/NameDetailHeader.kt`
- Test: `app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/molecules/NameDetailHeaderTest.kt`

**Interfaces:**
- Consumes: `NamesAccent` from Task 1.
- Produces: `@Composable fun NameDetailHeader(arabicName: String, accent: NamesAccent, modifier: Modifier = Modifier, number: Int? = null, primaryLabel: String? = null, secondaryLabel: String? = null)`

- [ ] **Step 1: Write the failing test `NameDetailHeaderTest.kt`**

```kotlin
package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NameDetailHeaderTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders number, arabic, primary and secondary`() {
        composeRule.setThemedContent {
            NameDetailHeader(
                arabicName = "ArabicRahman",
                accent = NamesAccents.allah(),
                number = 52,
                primaryLabel = "Ar-Rahman",
                secondaryLabel = "The Most Compassionate",
            )
        }
        composeRule.onNodeWithText("52").assertExists()
        composeRule.onNodeWithText("ArabicRahman").assertExists()
        composeRule.onNodeWithText("Ar-Rahman").assertExists()
        composeRule.onNodeWithText("The Most Compassionate").assertExists()
    }

    @Test
    fun `omits number when null`() {
        composeRule.setThemedContent {
            NameDetailHeader(
                arabicName = "ArabicMuhammad",
                accent = NamesAccents.prophets(),
                number = null,
                primaryLabel = "Muhammad",
                secondaryLabel = "Seal of the Prophets",
            )
        }
        composeRule.onNodeWithText("Muhammad").assertExists()
        composeRule.onNodeWithText("Seal of the Prophets").assertExists()
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.presentation.components.molecules.NameDetailHeaderTest"`
Expected: FAIL — `NameDetailHeader` unresolved.

- [ ] **Step 3: Write `NameDetailHeader.kt`**

```kotlin
package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.theme.NimazSpacing

/**
 * Calligraphic on-surface header for the names detail screens. Large Amiri Arabic
 * leads, with an accent-ringed number medallion (optional) and an accent divider.
 */
@Composable
fun NameDetailHeader(
    arabicName: String,
    accent: NamesAccent,
    modifier: Modifier = Modifier,
    number: Int? = null,
    primaryLabel: String? = null,
    secondaryLabel: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = NimazSpacing.Large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NimazSpacing.Small)
    ) {
        if (number != null) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .border(BorderStroke(2.dp, accent.rail), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$number",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent.contentTint
                )
            }
        }

        ArabicText(
            text = arabicName,
            size = ArabicTextSize.LARGE,
            color = accent.contentTint,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        if (primaryLabel != null) {
            Text(
                text = primaryLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }

        if (secondaryLabel != null) {
            Text(
                text = secondaryLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Box(
            modifier = Modifier
                .padding(top = NimazSpacing.Small)
                .width(60.dp)
                .height(3.dp)
                .background(accent.rail, RoundedCornerShape(3.dp))
        )
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.presentation.components.molecules.NameDetailHeaderTest"`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/components/molecules/NameDetailHeader.kt \
        app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/molecules/NameDetailHeaderTest.kt
git commit -m "feat(names): calligraphic NameDetailHeader"
```

---

### Task 5: Add `colors` passthrough to `NimazBackTopAppBar`

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/components/organisms/TopAppBar.kt:72-94`

**Interfaces:**
- Produces: `NimazBackTopAppBar(... colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors())` forwarded to `NimazTopAppBar`.

- [ ] **Step 1: Add the `colors` parameter**

In `NimazBackTopAppBar` (currently parameters end at `scrollBehavior`), add a `colors` parameter and forward it. The function body calls `NimazTopAppBar(...)`, which already accepts `colors`. Updated signature + call:

```kotlin
fun NimazBackTopAppBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    colors: androidx.compose.material3.TopAppBarColors =
        androidx.compose.material3.TopAppBarDefaults.topAppBarColors()
) {
    NimazTopAppBar(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back)
                )
            }
        },
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = colors
    )
}
```

(Keep existing imports; `TopAppBarColors`/`TopAppBarDefaults` are referenced fully-qualified to avoid touching the import block. The file already opts into `ExperimentalMaterial3Api`.)

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/components/organisms/TopAppBar.kt
git commit -m "feat(ui): NimazBackTopAppBar colors passthrough for per-screen tint"
```

---

### Task 6: Rewire 99 Names of Allah (list + detail)

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/asma/AsmaUlHusnaListScreen.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/asma/AsmaUlHusnaDetailScreen.kt`

**Interfaces:**
- Consumes: `NameCard`, `NameFilterRow`, `NameDetailHeader`, `NamesAccents.allah()` (Tasks 1,3,4); `NimazBackTopAppBar(colors=…)` (Task 5); existing `NimazEmptyState`.

- [ ] **Step 1: Rewire `AsmaUlHusnaListScreen.kt`**

Replace the private `AsmaUlHusnaNameCard` function (lines ~192-283) entirely — delete it. In the composable body:
1. Capture the accent at the top of `AsmaUlHusnaListScreen`: `val accent = NamesAccents.allah()`.
2. Tint the top bar:

```kotlin
NimazBackTopAppBar(
    title = stringResource(R.string.asma_ul_husna_title),
    onBackClick = onNavigateBack,
    colors = TopAppBarDefaults.topAppBarColors(
        titleContentColor = accent.contentTint,
        navigationIconContentColor = accent.contentTint
    )
)
```

3. Replace the inline `Row { FilterChip … FilterChip … }` (lines ~92-132) with:

```kotlin
NameFilterRow(
    showFavoritesOnly = state.showFavoritesOnly,
    onShowAll = { viewModel.onEvent(AsmaUlHusnaEvent.ToggleFavoritesFilter) },
    onShowFavorites = { viewModel.onEvent(AsmaUlHusnaEvent.ToggleFavoritesFilter) },
    accent = accent,
    allLabel = stringResource(R.string.all),
    favoritesLabel = stringResource(R.string.favorites),
    modifier = Modifier.padding(
        horizontal = NimazSpacing.Large,
        vertical = NimazSpacing.ExtraSmall
    )
)
```

4. Replace the `AsmaUlHusnaNameCard(...)` call inside `items {}` with:

```kotlin
NameCard(
    number = name.id,
    arabicName = name.nameArabic,
    primaryLabel = name.nameTransliteration,
    secondaryLabel = name.nameEnglish,
    isFavorite = name.isFavorite,
    accent = accent,
    onClick = { onNavigateToDetail(name.id) },
    onFavoriteClick = { viewModel.onEvent(AsmaUlHusnaEvent.ToggleFavorite(name.id)) },
)
```

5. Replace the empty-state `Box { Text(...) }` (lines ~166-185) with:

```kotlin
if (displayList.isEmpty()) {
    item {
        NimazEmptyState(
            title = if (state.showFavoritesOnly) {
                stringResource(R.string.no_favorites_yet)
            } else {
                stringResource(R.string.asma_ul_husna_no_names_found)
            },
            message = "",
            icon = Icons.Filled.Favorite,
            iconTint = accent.contentTint,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 48.dp)
        )
    }
}
```

6. Fix imports: add `com.arshadshah.nimaz.presentation.components.molecules.NameCard`, `…NameFilterRow`, `…NamesAccents`, `com.arshadshah.nimaz.presentation.components.molecules.NimazEmptyState`, `androidx.compose.material3.TopAppBarDefaults`. Remove now-unused imports (`FilterChip`, `FilterChipDefaults`, `NimazCard`, `NimazCardStyle`, `CardDefaults`, `CircleShape`, `ArabicText`, `ArabicTextSize`, the favorite icon imports if no longer referenced, etc.). Let the compiler in Step 3 flag leftovers.

- [ ] **Step 2: Rewire `AsmaUlHusnaDetailScreen.kt`**

1. Capture accent + tint top bar (same pattern as list): `val accent = NamesAccents.allah()`, and pass `colors = TopAppBarDefaults.topAppBarColors(titleContentColor = accent.contentTint, navigationIconContentColor = accent.contentTint)` to `NimazBackTopAppBar`.
2. Recolor the FAB to the accent:

```kotlin
FloatingActionButton(
    onClick = { viewModel.onEvent(AsmaUlHusnaEvent.ToggleFavorite(name.id)) },
    containerColor = accent.chipContainer,
    contentColor = accent.onChipContainer
) { /* existing Icon, keep tint = if favorite error else accent.onChipContainer */ }
```

3. Replace the gradient header `item { NimazCard { Box(gradient) … } }` (lines ~129-194) with:

```kotlin
item {
    NameDetailHeader(
        arabicName = name.nameArabic,
        accent = accent,
        number = name.id,
        primaryLabel = name.nameTransliteration,
        secondaryLabel = name.nameEnglish,
    )
}
```

4. In the Quran-references `item`, recolor the `AssistChip` container to `accent.chipContainer` / label to `accent.onChipContainer` (replace the hardcoded `secondaryContainer`/`onSecondaryContainer`).
5. Fix imports: add `…molecules.NameDetailHeader`, `…molecules.NamesAccents`, `androidx.compose.material3.TopAppBarDefaults`; drop now-unused gradient imports (`Brush`, `CircleShape`, the header-only `ArabicText` usage stays only if still referenced — it is not after the swap, so remove `ArabicText`/`ArabicTextSize` if unused).

- [ ] **Step 3: Verify both screens compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (resolve any unused-import / unresolved-reference errors it reports).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/screens/asma/
git commit -m "feat(names): rewire 99 Names of Allah to shared components (teal)"
```

---

### Task 7: Rewire Names of the Prophet ﷺ (list + detail)

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/asmaunnabi/AsmaUnNabiListScreen.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/asmaunnabi/AsmaUnNabiDetailScreen.kt`

**Interfaces:**
- Consumes: same shared components, with `accent = NamesAccents.prophetNames()` (purple). `AsmaUnNabi` model fields: `id`, `nameArabic`, `nameTransliteration`, `nameEnglish`, `meaning`, `explanation`, `source`.

- [ ] **Step 1: Rewire `AsmaUnNabiListScreen.kt`**

Apply the exact same transformation as Task 6 Step 1, using `NamesAccents.prophetNames()`, the `AsmaUnNabiEvent` events (`Search`, `ClearSearch`, `ToggleFavoritesFilter`, `ToggleFavorite`), `state.filteredNames`, and `R.string.asma_un_nabi_*` strings (confirm exact names via `grep -n 'asma_un_nabi' app/src/main/res/values/strings.xml`). The `NameCard` call:

```kotlin
NameCard(
    number = name.id,
    arabicName = name.nameArabic,
    primaryLabel = name.nameTransliteration,
    secondaryLabel = name.nameEnglish,
    isFavorite = name.isFavorite,
    accent = accent,
    onClick = { onNavigateToDetail(name.id) },
    onFavoriteClick = { viewModel.onEvent(AsmaUnNabiEvent.ToggleFavorite(name.id)) },
)
```

Open the file first to read its existing event names / state field names / string resources and mirror them exactly (do not assume — this screen predates this plan and may differ slightly from AsmaUlHusna).

- [ ] **Step 2: Rewire `AsmaUnNabiDetailScreen.kt`**

Open the file. Apply the Task 6 Step 2 transformation: accent = `NamesAccents.prophetNames()`, tint top bar + FAB, replace the gradient header with:

```kotlin
NameDetailHeader(
    arabicName = name.nameArabic,
    accent = accent,
    number = name.id,
    primaryLabel = name.nameTransliteration,
    secondaryLabel = name.nameEnglish,
)
```

Keep the existing `meaning` / `explanation` / `source` section cards; tint their section-title labels to `accent.contentTint` for cohesion.

- [ ] **Step 3: Verify compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/screens/asmaunnabi/
git commit -m "feat(names): rewire Names of the Prophet to shared components (purple)"
```

---

### Task 8: Rewire The Prophets (list + detail)

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/prophets/ProphetsListScreen.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/prophets/ProphetDetailScreen.kt`

**Interfaces:**
- Consumes: shared components with `accent = NamesAccents.prophets()` (gold). `Prophet` model: `id`, `nameArabic`, `nameEnglish`, `nameTransliteration`, `titleEnglish`, `era`, `storySummary`, `keyLessons`, `quranMentions`, `lineage`, `yearsLived`, `placeOfPreaching`, `miracles`, `isFavorite`.

- [ ] **Step 1: Rewire `ProphetsListScreen.kt`**

Delete the private `ProphetCard` function (lines ~190-303). Apply the Task 6 Step 1 transformation with `NamesAccents.prophets()`, `ProphetEvent`, `state.filteredProphets`, `R.string.prophets_*`. Use the **story variant** of `NameCard`:

```kotlin
NameCard(
    number = prophet.id,
    arabicName = prophet.nameArabic,
    primaryLabel = prophet.nameEnglish,
    secondaryLabel = prophet.nameTransliteration,
    isFavorite = prophet.isFavorite,
    accent = accent,
    onClick = { onNavigateToDetail(prophet.id) },
    onFavoriteClick = { viewModel.onEvent(ProphetEvent.ToggleFavorite(prophet.id)) },
    titleLabel = prophet.titleEnglish,
    eraChip = prophet.era,
)
```

(Note: `storySummary` is intentionally NOT passed — no preview on the list card.)

- [ ] **Step 2: Rewire `ProphetDetailScreen.kt`**

Apply the Task 6 Step 2 header swap. The Prophet header has **no number medallion** and centers name + title:

```kotlin
NameDetailHeader(
    arabicName = prophet.nameArabic,
    accent = accent,
    number = null,
    primaryLabel = prophet.nameEnglish,
    secondaryLabel = prophet.titleEnglish,
)
```

Keep the existing story / key-lessons / Quran-mentions / timeline / miracles section cards. For cohesion: tint each section-title label and the bullet `Icon` (`Icons.Filled.Circle`) to `accent.contentTint`; recolor the Quran-mentions `AssistChip` container to `accent.chipContainer` / label `accent.onChipContainer`; recolor the FAB to `accent.chipContainer` / `accent.onChipContainer`; tint the top bar (`NamesAccents.prophets()`).

- [ ] **Step 3: Verify compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/screens/prophets/
git commit -m "feat(names): rewire The Prophets to shared components (gold, story variant)"
```

---

### Task 9: Full verification

**Files:** none (verification only).

- [ ] **Step 1: Run the full molecule test suite**

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.presentation.components.molecules.Name*"`
Expected: PASS — `NameCardTest` (5), `NameFilterRowTest` (3), `NameDetailHeaderTest` (2).

- [ ] **Step 2: Full debug build (covers the three Adaptive*Screen panes that reuse these screens)**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. The tablet `AdaptiveAsmaUlHusnaScreen` / `AdaptiveAsmaUnNabiScreen` / `AdaptiveProphetsScreen` host the list/detail composables unchanged, so they inherit the redesign — confirm no compile breaks.

- [ ] **Step 3: Manual smoke (real device/emulator)**

Use the `run` skill (or `./gradlew :app:installDebug`) and visually confirm on each of the three screens:
- Slim tinted top bar (teal / purple / gold), accent filter chips, shared `NameCard` rows (medallion + rail + Arabic/translit/meaning), Prophets cards show title + era chip and NO story preview.
- Detail screens show the calligraphic header (Amiri + ringed medallion + accent divider) and accent-cohesive section cards + FAB.

- [ ] **Step 4: Final commit (if any cleanup was needed)**

Scope the commit to the names files only — another agent may have unrelated
(e.g. Qibla) changes in the working tree; never `git add -A`.

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/components/molecules/Name*.kt \
        app/src/main/java/com/arshadshah/nimaz/presentation/screens/asma/ \
        app/src/main/java/com/arshadshah/nimaz/presentation/screens/asmaunnabi/ \
        app/src/main/java/com/arshadshah/nimaz/presentation/screens/prophets/ \
        app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/molecules/Name*.kt \
  && git commit -m "chore(names): redesign verification cleanup" || echo "nothing to commit"
```

---

## Notes for the implementer
- Open each screen file before editing — line numbers in this plan are from 2026-06-18 and may drift. The transformations (capture accent → tint top bar → `NameFilterRow` → `NameCard` → `NimazEmptyState` → delete inline card fn) are identical across the three list screens; only the accent, event class, state field, and string resources differ.
- After each screen rewire, the compiler is your checklist for stale imports — remove what it flags as unused.
- Do not change ViewModels, models, DAOs, repositories, navigation, or string resources — this is a pure presentation-layer redesign.
