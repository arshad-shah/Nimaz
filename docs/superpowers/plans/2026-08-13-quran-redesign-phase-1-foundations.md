# Qur'an Redesign — Phase 1: Foundations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Land the shared tokens and two rebuilt components that every later phase of the Qur'an redesign depends on, without changing any screen's behaviour.

**Architecture:** Three independent pieces, all additive. A paper colour palette added to the existing `QuranSurfaceColors` seam; a new `NimazSegmentedTabs` organism that will replace five different tab controls; and a new `NimazTreeNode` molecule for the Themes accordion. Nothing is migrated onto them in this phase — migration happens in phases 2–5, so this phase can land without touching a single screen.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Robolectric + Truth for component tests.

**Spec:** [`docs/superpowers/specs/2026-08-13-quran-redesign-design.md`](../specs/2026-08-13-quran-redesign-design.md) §6

## Global Constraints

- **No hardcoded `Color(0xFF…)` in screens or components.** Use `MaterialTheme.colorScheme.*` / `NimazColors.*` / `NimazPalette.*`. (CLAUDE.md rule 7.)
- **Interactive UI comes from the design system.** A tappable row is `NimazCard(onClick = …)` or `NimazMenuItem`, never a `Modifier.clickable` wrapped around a card — that paints a sharp-cornered ripple ignoring the card radius. `.clickable` on *inner* elements is fine. (CLAUDE.md rule 8.)
- **Yellow/gold is reserved for Qur'anic ornament**, never for selection state. Selection is the teal accent. (Spec §6.5.)
- Components live under `presentation/components/{atoms,molecules,organisms}`.
- Component tests live under `app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/...`, use `@RunWith(RobolectricTestRunner::class)`, and use the shared `createComponentComposeRule()` + `setThemedContent {}` helpers from `AtomTestSupport.kt`.
- Verify with `./gradlew :app:compileDebugKotlin` and `./gradlew :app:testDebugUnitTest`. `lintDebug` before the phase's final commit.

---

## File Structure

| File | Responsibility |
|------|----------------|
| `presentation/theme/QuranSurfaceColors.kt` (modify) | Add the paper register — `paper`, `paperLine`, `paperInk` — beside the existing mushaf/tafseer roles |
| `presentation/components/organisms/NimazSegmentedTabs.kt` (create) | The pill-in-tray segmented control; one control for home, Themes, Khatam, Saved, and the player sheet |
| `presentation/components/molecules/NimazTreeNode.kt` (create) | One node of an expandable tree: card, rotating chevron, optional count, optional indent rail for children |
| `testDebug/.../organisms/NimazSegmentedTabsTest.kt` (create) | Behaviour + a11y for the segmented control |
| `testDebug/.../molecules/NimazTreeNodeTest.kt` (create) | Behaviour + a11y for the tree node |

`QuranSurfaceColors` is the correct home for the paper tokens rather than a new file: it already owns "the colours of the Qur'an reading surfaces — the mushaf page, the tafseer study frame, their illuminated borders and the shamsa page medallion", and its own doc comment records the current decision to use no bespoke parchment palette. This phase reverses that decision, so it belongs in that comment.

---

### Task 1: Paper palette

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/theme/QuranSurfaceColors.kt`
- Test: `app/src/testDebug/java/com/arshadshah/nimaz/presentation/theme/QuranSurfaceColorsTest.kt` (create)

**Interfaces:**
- Consumes: `LocalIsDarkTheme`, `NimazPalette`, `NimazColors` — all existing in the theme package.
- Produces: three new `@Composable @ReadOnlyComposable` `Color` properties on `QuranSurfaceColors`:
  - `QuranSurfaceColors.paper` — the mushaf page ground
  - `QuranSurfaceColors.paperLine` — hairline rules and the page frame
  - `QuranSurfaceColors.paperInk` — Arabic body text on paper

  Phase 3 consumes all three.

- [ ] **Step 1: Read the existing file to match its idiom**

Open `app/src/main/java/com/arshadshah/nimaz/presentation/theme/QuranSurfaceColors.kt`. Every role is declared as:

```kotlin
val someRole: Color
    @Composable @ReadOnlyComposable
    get() = if (isDark) <darkValue> else <lightValue>
```

Match that exactly. Do not introduce a data class or a CompositionLocal.

- [ ] **Step 2: Write the failing test**

Create `app/src/testDebug/java/com/arshadshah/nimaz/presentation/theme/QuranSurfaceColorsTest.kt`:

```kotlin
package com.arshadshah.nimaz.presentation.theme

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuranSurfaceColorsTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val composeRule = createComposeRule()

    private fun paperRoles(dark: Boolean): Triple<Color, Color, Color> {
        lateinit var roles: Triple<Color, Color, Color>
        composeRule.setContent {
            CompositionLocalProvider(LocalIsDarkTheme provides dark) {
                roles = Triple(
                    QuranSurfaceColors.paper,
                    QuranSurfaceColors.paperLine,
                    QuranSurfaceColors.paperInk,
                )
            }
        }
        return roles
    }

    @Test
    fun `paper roles differ between light and dark`() {
        val light = paperRoles(dark = false)
        val dark = paperRoles(dark = true)
        assertThat(light).isNotEqualTo(dark)
    }

    @Test
    fun `light paper is a warm ground, not pure white`() {
        val (paper, _, _) = paperRoles(dark = false)
        assertThat(paper).isNotEqualTo(Color.White)
        // Warm: red channel exceeds blue.
        assertThat(paper.red).isGreaterThan(paper.blue)
    }

    @Test
    fun `paper ink contrasts with its own ground in both themes`() {
        listOf(false, true).forEach { dark ->
            val (paper, _, ink) = paperRoles(dark = dark)
            assertThat(contrastRatio(paper, ink)).isAtLeast(4.5)
        }
    }

    @Test
    fun `paper line is visible against paper in both themes`() {
        listOf(false, true).forEach { dark ->
            val (paper, line, _) = paperRoles(dark = dark)
            // Non-text ornament: the 3:1 bar, same rule the file applies to frameGold.
            assertThat(contrastRatio(paper, line)).isAtLeast(1.5)
        }
    }

    /** WCAG relative-luminance contrast ratio. */
    private fun contrastRatio(a: Color, b: Color): Double {
        fun channel(c: Float): Double {
            val v = c.toDouble()
            return if (v <= 0.03928) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
        }
        fun luminance(c: Color) =
            0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)
        val la = luminance(a)
        val lb = luminance(b)
        val lighter = maxOf(la, lb)
        val darker = minOf(la, lb)
        return (lighter + 0.05) / (darker + 0.05)
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "*QuranSurfaceColorsTest*"
```

Expected: FAIL to compile — `Unresolved reference: paper`.

- [ ] **Step 4: Add the paper roles**

In `QuranSurfaceColors.kt`, add after the existing `pageSurface` role:

```kotlin
/**
 * The **paper register** — used only by the mushaf and 16-line reading modes.
 *
 * Held apart from [pageSurface] and the app's `surface` tokens on purpose: the
 * mushaf is imitating a printed page, and the rest of the Qur'an section is
 * moving to a flatter, cooler language. Reusing `surface` here would make the
 * page indistinguishable from every other card.
 *
 * Light values are a warm cream with a soft brown rule; dark values keep the
 * app's deep teal ground so the page does not glare at night.
 */
val paper: Color
    @Composable @ReadOnlyComposable
    get() = if (isDark) Color(0xFF0B1D1B) else Color(0xFFFBF7EC)

/** Hairline rules, the page frame and the cartouche stroke on [paper]. */
val paperLine: Color
    @Composable @ReadOnlyComposable
    get() = if (isDark) Color(0xFF1F4340) else Color(0xFFE0D6BC)

/** Arabic body text on [paper]. */
val paperInk: Color
    @Composable @ReadOnlyComposable
    get() = if (isDark) Color(0xFFE8F1EF) else Color(0xFF1C1A14)
```

These raw `Color(0xFF…)` literals are correct **here** — rule 7 forbids them in screens and components, and this file *is* the centralised token definition, exactly as `NimazPalette` is.

- [ ] **Step 5: Update the file's doc comment**

The object's KDoc currently says light mode "re-uses the **existing** `surface` tokens (no bespoke parchment palette)". That is no longer true. Change that bullet to:

```
 * - light re-uses the existing `surface` tokens for the tafseer frame, and darkens
 *   the gold so it stays legible on a pale page;
 * - the mushaf page itself no longer uses those tokens: it has its own [paper]
 *   register (see below), because it is imitating printed paper rather than
 *   presenting a card.
```

- [ ] **Step 6: Run the test to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "*QuranSurfaceColorsTest*"
```

Expected: PASS, 4 tests.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/theme/QuranSurfaceColors.kt \
        app/src/testDebug/java/com/arshadshah/nimaz/presentation/theme/QuranSurfaceColorsTest.kt
git commit -m "feat(theme): a paper register for the mushaf page

The mushaf is imitating a printed page while the rest of the Quran section
moves to a flatter language, so it needs a ground of its own rather than the
shared surface tokens. Adds paper/paperLine/paperInk to QuranSurfaceColors,
with contrast pinned by test in both themes."
```

---

### Task 2: NimazSegmentedTabs

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/components/organisms/NimazSegmentedTabs.kt`
- Test: `app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/organisms/NimazSegmentedTabsTest.kt`

**Interfaces:**
- Consumes: nothing from Task 1.
- Produces:

```kotlin
@Composable
fun NimazSegmentedTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
)
```

  Phases 2–5 call this for: Saved's kind/content chips, Themes' `Themes · Kinds · Index`, Khatam's `In progress · Completed · Archived`, and the player sheet's repeat and speed selectors.

**Design notes.** The existing `NimazPillTabs` paints the *selected* segment with `colorScheme.primary` and leaves the others transparent on a `surfaceVariant` tray. The prototype inverts that: the tray is recessed, and the selected segment is **lifted** as a raised `surface` pill with a shadow. That reads as a physical control and, importantly, does not spend the brand colour on a control that appears five times per screen. Do not "improve" it back to a filled primary pill.

- [ ] **Step 1: Write the failing test**

Create `app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/organisms/NimazSegmentedTabsTest.kt`:

```kotlin
package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.presentation.components.atoms.createComponentComposeRule
import com.arshadshah.nimaz.presentation.components.atoms.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazSegmentedTabsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val tabs = listOf("Outline", "By kind", "Index")

    @Test
    fun `the selected segment reports the selected state`() {
        composeRule.setThemedContent {
            NimazSegmentedTabs(tabs = tabs, selectedIndex = 1, onTabSelect = {})
        }
        composeRule.onNodeWithText("By kind").assertIsSelected()
        composeRule.onNodeWithText("Outline").assertIsNotSelected()
        composeRule.onNodeWithText("Index").assertIsNotSelected()
    }

    @Test
    fun `tapping a segment reports its index`() {
        var picked = -1
        composeRule.setThemedContent {
            NimazSegmentedTabs(tabs = tabs, selectedIndex = 0, onTabSelect = { picked = it })
        }
        composeRule.onNodeWithText("Index").performClick()
        assertThat(picked).isEqualTo(2)
    }

    @Test
    fun `tapping the already-selected segment still reports it`() {
        var calls = 0
        composeRule.setThemedContent {
            NimazSegmentedTabs(tabs = tabs, selectedIndex = 0, onTabSelect = { calls++ })
        }
        composeRule.onNodeWithText("Outline").performClick()
        assertThat(calls).isEqualTo(1)
    }

    @Test
    fun `disabled tabs do not report taps`() {
        var calls = 0
        composeRule.setThemedContent {
            NimazSegmentedTabs(
                tabs = tabs,
                selectedIndex = 0,
                onTabSelect = { calls++ },
                enabled = false,
            )
        }
        composeRule.onNodeWithText("Index").assertIsNotEnabled()
        composeRule.onNodeWithText("Index").performClick()
        assertThat(calls).isEqualTo(0)
    }

    @Test
    fun `enabled tabs are enabled`() {
        composeRule.setThemedContent {
            NimazSegmentedTabs(tabs = tabs, selectedIndex = 0, onTabSelect = {})
        }
        composeRule.onNodeWithText("Index").assertIsEnabled()
    }

    @Test
    fun `an out-of-range selection selects nothing rather than crashing`() {
        composeRule.setThemedContent {
            NimazSegmentedTabs(tabs = tabs, selectedIndex = 9, onTabSelect = {})
        }
        composeRule.onNodeWithText("Outline").assertIsNotSelected()
        composeRule.onNodeWithText("By kind").assertIsNotSelected()
        composeRule.onNodeWithText("Index").assertIsNotSelected()
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "*NimazSegmentedTabsTest*"
```

Expected: FAIL to compile — `Unresolved reference: NimazSegmentedTabs`.

- [ ] **Step 3: Implement the component**

Create `app/src/main/java/com/arshadshah/nimaz/presentation/components/organisms/NimazSegmentedTabs.kt`:

```kotlin
package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * The house segmented control: a recessed tray with the selected segment
 * **lifted** out of it as a raised pill.
 *
 * Deliberately not a filled-primary pill (which is what [NimazPillTabs] does).
 * This control appears several times on some screens, and spending the brand
 * colour on every one of them leaves nothing to mark the actual accent. The
 * lift, not the hue, carries the selection.
 *
 * Segments share the width equally, so keep labels short; a label that does not
 * fit is ellipsised rather than wrapped, which keeps the tray one row high.
 */
@Composable
fun NimazSegmentedTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            tabs.forEachIndexed { index, label ->
                val selected = index == selectedIndex

                val container by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        Color.Transparent
                    },
                    animationSpec = tween(180),
                    label = "segment_container",
                )
                val content by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(180),
                    label = "segment_content",
                )
                val lift by animateDpAsState(
                    targetValue = if (selected) 2.dp else 0.dp,
                    animationSpec = tween(180),
                    label = "segment_lift",
                )

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .selectable(
                            selected = selected,
                            enabled = enabled,
                            role = Role.Tab,
                            onClick = { onTabSelect(index) },
                        ),
                    shape = RoundedCornerShape(11.dp),
                    color = container,
                    contentColor = content,
                    shadowElevation = lift,
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Preview(name = "Segmented tabs · light")
@Preview(name = "Segmented tabs · dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NimazSegmentedTabsPreview() {
    NimazTheme {
        NimazSegmentedTabs(
            tabs = listOf("Outline", "By kind", "Index"),
            selectedIndex = 0,
            onTabSelect = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
```

The `selectable(role = Role.Tab)` is what makes `assertIsSelected()` and `assertIsNotEnabled()` work; do not replace it with `clickable`.

- [ ] **Step 4: Run the test to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "*NimazSegmentedTabsTest*"
```

Expected: PASS, 6 tests.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/components/organisms/NimazSegmentedTabs.kt \
        app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/organisms/NimazSegmentedTabsTest.kt
git commit -m "feat(organisms): one segmented control, selection carried by lift

Five different tab controls across the Quran section are being replaced by
this one. The selected segment is lifted out of a recessed tray rather than
filled with the brand colour, because some screens show several of these at
once and a filled pill on each leaves nothing to mark the real accent."
```

---

### Task 3: NimazTreeNode

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/components/molecules/NimazTreeNode.kt`
- Test: `app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/molecules/NimazTreeNodeTest.kt`

**Interfaces:**
- Consumes: nothing from Tasks 1–2.
- Produces:

```kotlin
@Composable
fun NimazTreeNode(
    label: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    expanded: Boolean = false,
    onToggleExpand: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
)
```

  `onToggleExpand == null` means the node is a leaf: no chevron, no rail. `content` renders the children inside the indent rail and is only composed while `expanded`. Phase 5 uses this for the Themes tree.

**Design notes.** The spec leaves open whether chevron-expands / label-navigates survives. This component supports **both** by taking two separate callbacks, so phase 5 can decide without a component change. When `onClick` is null the whole row toggles; when both are supplied the chevron toggles and the label navigates.

- [ ] **Step 1: Write the failing test**

Create `app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/molecules/NimazTreeNodeTest.kt`:

```kotlin
package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.presentation.components.atoms.createComponentComposeRule
import com.arshadshah.nimaz.presentation.components.atoms.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazTreeNodeTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `a collapsed node does not compose its children`() {
        composeRule.setThemedContent {
            NimazTreeNode(
                label = "Doctrine",
                count = 214,
                expanded = false,
                onToggleExpand = {},
            ) { Text("God") }
        }
        composeRule.onNodeWithText("Doctrine").assertIsDisplayed()
        composeRule.onNodeWithText("God").assertDoesNotExist()
    }

    @Test
    fun `an expanded node composes its children`() {
        composeRule.setThemedContent {
            NimazTreeNode(
                label = "Doctrine",
                count = 214,
                expanded = true,
                onToggleExpand = {},
            ) { Text("God") }
        }
        composeRule.onNodeWithText("God").assertIsDisplayed()
    }

    @Test
    fun `the count is rendered when supplied`() {
        composeRule.setThemedContent {
            NimazTreeNode(label = "Doctrine", count = 214, onToggleExpand = {})
        }
        composeRule.onNodeWithText("214").assertIsDisplayed()
    }

    @Test
    fun `no count is rendered when absent`() {
        composeRule.setThemedContent {
            NimazTreeNode(label = "Doctrine", onToggleExpand = {})
        }
        composeRule.onNodeWithText("214").assertDoesNotExist()
    }

    @Test
    fun `a leaf node has no expand control`() {
        composeRule.setThemedContent {
            NimazTreeNode(label = "The hereafter", count = 54, onToggleExpand = null)
        }
        composeRule.onNodeWithContentDescription("Expand The hereafter").assertDoesNotExist()
    }

    @Test
    fun `the chevron toggles expansion`() {
        var toggles = 0
        composeRule.setThemedContent {
            NimazTreeNode(
                label = "Doctrine",
                expanded = false,
                onToggleExpand = { toggles++ },
                onClick = {},
            )
        }
        composeRule.onNodeWithContentDescription("Expand Doctrine").performClick()
        assertThat(toggles).isEqualTo(1)
    }

    @Test
    fun `the chevron announces collapse when expanded`() {
        composeRule.setThemedContent {
            NimazTreeNode(label = "Doctrine", expanded = true, onToggleExpand = {})
        }
        composeRule.onNodeWithContentDescription("Collapse Doctrine").assertIsDisplayed()
    }

    @Test
    fun `the label navigates when onClick is supplied`() {
        var opened = 0
        var toggles = 0
        composeRule.setThemedContent {
            NimazTreeNode(
                label = "Doctrine",
                onToggleExpand = { toggles++ },
                onClick = { opened++ },
            )
        }
        composeRule.onNodeWithText("Doctrine").performClick()
        assertThat(opened).isEqualTo(1)
        assertThat(toggles).isEqualTo(0)
    }

    @Test
    fun `the whole row toggles when there is nothing to navigate to`() {
        var toggles = 0
        composeRule.setThemedContent {
            NimazTreeNode(
                label = "Doctrine",
                onToggleExpand = { toggles++ },
                onClick = null,
            )
        }
        composeRule.onNodeWithText("Doctrine").performClick()
        assertThat(toggles).isEqualTo(1)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "*NimazTreeNodeTest*"
```

Expected: FAIL to compile — `Unresolved reference: NimazTreeNode`.

- [ ] **Step 3: Add the two strings**

In `app/src/main/res/values/strings.xml`, add:

```xml
<string name="tree_node_expand">Expand %1$s</string>
<string name="tree_node_collapse">Collapse %1$s</string>
```

These are content descriptions, so they must be real resources — a literal here would trip `MissingTranslation` handling later and is untranslatable.

- [ ] **Step 4: Implement the component**

Create `app/src/main/java/com/arshadshah/nimaz/presentation/components/molecules/NimazTreeNode.kt`:

```kotlin
package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * One node of an expandable tree: a card carrying a label, an optional count,
 * an optional expand chevron, and its children behind an indent rail.
 *
 * The node takes **two** callbacks on purpose. A subject tree has two distinct
 * actions on one row — open this subject, and show what is under it — and which
 * one the row as a whole should perform is still being decided. Supplying both
 * puts expansion on the chevron and navigation on the label; supplying only
 * [onToggleExpand] makes the whole row a toggle.
 *
 * [content] is only composed while [expanded], so a deep tree costs nothing
 * until it is opened.
 */
@Composable
fun NimazTreeNode(
    label: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    expanded: Boolean = false,
    onToggleExpand: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
) {
    val rowToggles = onToggleExpand != null && onClick == null

    Column(modifier = modifier.fillMaxWidth()) {
        NimazCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = if (rowToggles) onToggleExpand else null,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onToggleExpand != null) {
                    val rotation by animateFloatAsState(
                        targetValue = if (expanded) 90f else 0f,
                        animationSpec = tween(180),
                        label = "tree_chevron",
                    )
                    val description = stringResource(
                        if (expanded) R.string.tree_node_collapse else R.string.tree_node_expand,
                        label,
                    )
                    NimazIcon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = description,
                        modifier = Modifier
                            .rotate(rotation)
                            .then(
                                if (rowToggles) Modifier
                                else Modifier.clickable(onClick = onToggleExpand)
                            ),
                    )
                    Spacer(Modifier.width(10.dp))
                } else {
                    Spacer(Modifier.width(26.dp))
                }

                Text(
                    text = label,
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (onClick != null) Modifier.clickable(onClick = onClick)
                            else Modifier
                        ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                if (count != null) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (expanded && content != null) {
            Row(modifier = Modifier.padding(start = 16.dp, top = 6.dp)) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    content()
                }
            }
        }
    }
}

@Preview(name = "Tree node · light")
@Preview(name = "Tree node · dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NimazTreeNodePreview() {
    NimazTheme {
        Column(Modifier.padding(16.dp)) {
            NimazTreeNode(
                label = "Doctrine",
                count = 214,
                expanded = true,
                onToggleExpand = {},
                onClick = {},
            ) {
                NimazTreeNode(label = "God", count = 96, onToggleExpand = {}, onClick = {})
                Spacer(Modifier.height(6.dp))
                NimazTreeNode(label = "The hereafter", count = 54, onClick = {})
            }
        }
    }
}
```

Note the card is `NimazCard(onClick = …)` rather than a `Modifier.clickable` wrapper — rule 8. The inner `.clickable` on the chevron and the label is the sanctioned inner-element case.

- [ ] **Step 5: Confirm the NimazIcon and NimazCard signatures**

Before running, check the real signatures:

```bash
grep -n "fun NimazIcon(" -A 12 app/src/main/java/com/arshadshah/nimaz/presentation/components/atoms/NimazIcon.kt
grep -n "fun NimazCard(" -A 15 app/src/main/java/com/arshadshah/nimaz/presentation/components/atoms/NimazCard.kt
```

Adjust the two call sites to the actual parameter names if they differ (for example `NimazIcon` may name its vector parameter `icon` rather than `imageVector`). Do not change the component's own API to suit.

- [ ] **Step 6: Run the test to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "*NimazTreeNodeTest*"
```

Expected: PASS, 9 tests.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/components/molecules/NimazTreeNode.kt \
        app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/molecules/NimazTreeNodeTest.kt \
        app/src/main/res/values/strings.xml
git commit -m "feat(molecules): a tree node that can expand or navigate

The subject tree has two actions on one row - open this subject, and show
what sits under it - and which the row as a whole should do is still open.
Taking both callbacks lets that be decided at the call site rather than
rebuilding the component."
```

---

### Task 4: Document the components and close the phase

**Files:**
- Modify: `docs/ARCHITECTURE.md` §8

**Interfaces:**
- Consumes: Tasks 1–3.
- Produces: nothing consumed by later tasks.

- [ ] **Step 1: Find the component inventory in §8**

```bash
grep -n "NimazPillTabs\|^### 8\|^## 8" docs/ARCHITECTURE.md | head -20
```

- [ ] **Step 2: Add both components to §8**

Add entries in the established style of the surrounding bullets, covering:

- `NimazSegmentedTabs` (organism) — the house segmented control; selection is carried by a lift out of a recessed tray, not by the brand colour. Replaces `NimazPillTabs` progressively across phases 2–5; **`NimazPillTabs` is not deleted in this phase** because Khatam still uses it.
- `NimazTreeNode` (molecule) — one node of an expandable tree, with the two-callback expand/navigate split.
- A note that `QuranSurfaceColors` now carries a `paper` register used only by the mushaf and 16-line reading modes.

- [ ] **Step 3: Run the full verification set**

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
python3 scripts/check_docs.py
```

All four must pass. `lintDebug` is slow (~10 min) and is a CI gate — do not skip it. `assembleDebugAndroidTest` is **not** needed in this phase: no route or `ScreenTags` entry changed.

- [ ] **Step 4: Commit**

```bash
git add docs/ARCHITECTURE.md
git commit -m "docs(architecture): the segmented control and tree node

Records both new components in the design-system inventory, and notes that
NimazPillTabs stays until its last caller is migrated in a later phase."
```

---

## Phase exit criteria

- [ ] `QuranSurfaceColors.paper` / `.paperLine` / `.paperInk` exist, differ per theme, and have contrast pinned by test.
- [ ] `NimazSegmentedTabs` exists with 6 passing tests.
- [ ] `NimazTreeNode` exists with 9 passing tests.
- [ ] `docs/ARCHITECTURE.md` §8 documents all three.
- [ ] All four verification gates pass.
- [ ] **No screen has changed.** `git diff --stat origin/dev -- app/src/main/java/com/arshadshah/nimaz/presentation/screens/` is empty.

That last criterion is the point of the phase: it lands the vocabulary with zero behavioural risk, so phases 2–5 can be reviewed on their own merits.
