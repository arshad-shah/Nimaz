package com.arshadshah.nimaz.testing

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onLast

/**
 * The tappable settings **row** carrying [title], as opposed to the `Text` that renders the title.
 *
 * Two problems make a bare `onNodeWithText(title)` wrong on these screens, and both of them read
 * as the screen being broken rather than as a test artefact:
 *
 * - **A section header repeats its rows' words.** "Translation" is a section *and* a row on the
 *   Quran settings screen; "Mushaf Script" is a header and a dropdown label. `onNodeWithText`
 *   finds two nodes and fails with "expected exactly 1".
 * - **A disabled row is not merged.** `NimazSettingsItem` only attaches `Modifier.clickable` when
 *   it is enabled, so on a disabled row there is no merged parent and the match lands on the raw
 *   `Text` — which reports neither `enabled` nor `disabled`, and `assertIsNotEnabled` fails
 *   against a row that is, in fact, correctly disabled.
 *
 * Requiring a click action solves both: it picks the merged row over the header, and it is the
 * exact property "this row is off-limits" is about.
 */
fun ComposeContentTestRule.settingsRow(title: String): SemanticsNodeInteraction =
    onNode(hasText(title) and hasClickAction())

/**
 * Asserts that no tappable row carries [title] — the shape a *disabled* settings row takes.
 *
 * `NimazSettingsItem` expresses "disabled" by dimming to 50% and dropping its `clickable`, so the
 * assertion that matters is that nothing is left to tap. A row that merely looked disabled and
 * still fired its event would be worse than one that was never dimmed.
 */
fun ComposeContentTestRule.assertSettingsRowNotTappable(title: String) {
    onNode(hasText(title) and hasClickAction()).assertDoesNotExist()
}

/**
 * The same two, for a row that sits **inside an accordion**.
 *
 * An accordion is a clickable card, so it merges everything in its body into one node — a merged
 * node that carries every row's text *and* the card's own `OnClick`. `settingsRow` therefore
 * matches the card rather than the row, and its disabled counterpart matches the card too, which
 * makes "this row is off-limits" unassertable in the merged tree. In the unmerged tree the rows
 * keep their own nodes and the card's click action stays on the card.
 */
fun ComposeContentTestRule.accordionRow(title: String): SemanticsNodeInteraction =
    onAllNodes(hasAnyDescendant(hasText(title)) and hasClickAction(), useUnmergedTree = true)
        .onLast()

/**
 * How many tappable things enclose [title] in the unmerged tree.
 *
 * Inside an accordion the answer is the assertion. An **enabled** row gives two — the accordion
 * card, which collapses the row, and the row itself; a **disabled** one gives only the card,
 * because `NimazSettingsItem` expresses "disabled" by dropping its `clickable` entirely.
 */
fun ComposeContentTestRule.tappableAncestorCount(title: String): Int =
    onAllNodes(hasAnyDescendant(hasText(title)) and hasClickAction(), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .size
