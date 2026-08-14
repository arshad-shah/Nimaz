# Qur'an Redesign — Phase 3: Reader Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Halve the reader's per-ayah cost by moving ayah actions into a sheet, unify reading-mode switching behind one top-bar control, and give the mushaf its paper register.

**Architecture:** Most of what the spec asks for already exists in some form — the page view, the 16-line renderer, and page-level khatam marking all ship today. This phase mostly *unifies and re-skins* rather than building: the local `usePageView` flag and the persisted `mushafScript` preference become one mode concept behind one control, the per-ayah action pill collapses into a sheet, and `MushafPage` adopts the Phase 1 paper tokens.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3 `ModalBottomSheet`, Robolectric + Truth.

**Spec:** [`docs/superpowers/specs/2026-08-13-quran-redesign-design.md`](../specs/2026-08-13-quran-redesign-design.md) §5.4

**Depends on:** Phase 1 (paper palette, `AyahReference`), Phase 2 (the reader is now reached from `QuranBrowse`).

## Global Constraints

- Rules 1–8 from CLAUDE.md apply. In particular: no `Color(0xFF…)` in screens — the paper register comes from `QuranSurfaceColors` (Phase 1); and a whole-row tap target is `NimazCard(onClick = …)`, never a wrapping `.clickable`.
- **Yellow/gold is ornament, never selection.** The follow-along highlight is the one deliberate exception and is specified in Phase 4, not here.
- The reader is the app's most-used screen. Every task must leave it working; do not stack half-migrations.
- `./gradlew :app:assembleDebugAndroidTest` is **not** required unless a `ScreenTags` entry changes. Task 2 adds one, so run it there.

---

## What already exists — read this before starting

Verified in the working tree:

| Thing | Where | State |
|-------|-------|-------|
| Page (mushaf) view | `QuranReaderScreen.kt:119` `var usePageView by rememberSaveable`, toggled from the overflow item at `:437` | Ships. Local UI state, not persisted. |
| `ReadingMode.PAGE` | `state.readingMode`, combined at `:126` as `isInPageMode` | Ships |
| 16-line renderer | `QuranReaderUiState.mushafScript`, `useLineAccurateLayout get() = mushafScript.isLineAccurate` (`QuranUiState.kt:108`) | Ships, driven by the **`SettingsQuran` preference**, not by the reader |
| Page-level khatam marking | `QuranMushafPageBar.kt:38-40,101-116` — `isKhatamActive`, `khatamReadAyahIds`, `onKhatamTogglePage`, already gated on `isKhatamActive` | **Ships, and already matches the spec's decision** |
| Per-ayah action pill | `QuranAyahItem` organism | Ships, always visible — this is what moves to a sheet |
| Mushaf frame colours | `QuranSurfaceColors.frameGold`, `frameTeal`, `pageSurface` | Ships — being replaced by `paper*` for the page |

**Consequence:** the spec's "read tracking becomes page-level and khatam-conditional" is *already true in page view*. The work here is only to (a) apply the same `isKhatamActive` gating to the list view's per-ayah circle and (b) add "Mark read for khatam" to the new ayah sheet.

## A design conflict to resolve before Task 2

The spec (§5.4) calls for three reading modes — Translation, Mushaf, 16-line — behind one top-bar control. But in the code **16-line is not a view mode; it is a *script***: `MushafScript` picks Madani vs IndoPak, and that choice also changes `pagination.totalPages` (604 vs 548 vs 610 vs 847). It is a persisted setting in `SettingsQuran`, and `docs/ARCHITECTURE.md` §9 records it as a deliberate accepted pattern.

Putting it in a view-mode menu conflates *how the page is rendered* with *which mushaf edition you are reading*, and would leave two places setting the same preference.

**Recommended model, to confirm with the design owner before writing code:**

- The top-bar control offers **two** modes: **Translation** and **Mushaf**.
- **Script stays in reader settings** (where it already lives), and the mushaf renders whichever script is selected — 16-line included.

Task 2 is written for the two-mode model. If the three-mode model is chosen instead, Task 2's menu gains a third entry that writes `SettingsEvent.SetMushafScript`, and the "two modes" assertions change accordingly.

---

## File Structure

| File | Responsibility |
|------|----------------|
| `presentation/components/molecules/AyahActionSheet.kt` (create) | The tap-to-open ayah sheet: all per-ayah actions |
| `presentation/components/organisms/QuranAyahItem.kt` (modify) | Drop the action pill; the row becomes tappable; plain translation; khatam-gated read mark |
| `presentation/screens/quran/QuranReaderScreen.kt` (modify) | Mode control in the app bar; anchor bar; host the sheet |
| `presentation/components/molecules/ReaderAnchorBar.kt` (create) | Surah / juz / page shown once, with "Go to…" |
| `presentation/components/organisms/MushafPage.kt` (modify) | Paper register |
| `presentation/components/molecules/QuranMushafPageBar.kt` (modify) | Paper register; hairline position indicator |

---

### Task 1: The ayah action sheet

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/components/molecules/AyahActionSheet.kt`
- Test: `app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/molecules/AyahActionSheetTest.kt`

**Interfaces:**
- Consumes: `AyahReference` (Phase 1).
- Produces:

```kotlin
data class AyahSheetActions(
    val onPlayFromHere: () -> Unit,
    val onRepeatAyah: () -> Unit,
    val onBookmark: () -> Unit,
    val onFavourite: () -> Unit,
    val onNote: () -> Unit,
    val onTafseer: () -> Unit,
    val onSubjects: () -> Unit,
    val onCopy: () -> Unit,
    val onShare: () -> Unit,
    val onMarkReadForKhatam: () -> Unit,
)

@Composable
fun AyahActionSheet(
    reference: AyahReference,
    arabic: String,
    translation: String?,
    juzNumber: Int,
    pageNumber: Int,
    isBookmarked: Boolean,
    isFavourite: Boolean,
    isKhatamActive: Boolean,
    actions: AyahSheetActions,
    onDismiss: () -> Unit,
)
```

  Task 2 hosts this from `QuranReaderScreen`. Phase 4 supplies `onPlayFromHere` / `onRepeatAyah` with real behaviour; until then they are wired to the existing play event.

**Notes.** `isKhatamActive == false` hides the "Mark read for khatam" action entirely — matching the gating `QuranMushafPageBar` already applies. Bookmark and favourite are **toggles**: the sheet shows their current state, so a reader can un-bookmark from here.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.domain.model.AyahReference
import com.arshadshah.nimaz.presentation.components.atoms.createComponentComposeRule
import com.arshadshah.nimaz.presentation.components.atoms.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AyahActionSheetTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun noopActions(
        onBookmark: () -> Unit = {},
        onMarkRead: () -> Unit = {},
    ) = AyahSheetActions(
        onPlayFromHere = {}, onRepeatAyah = {}, onBookmark = onBookmark,
        onFavourite = {}, onNote = {}, onTafseer = {}, onSubjects = {},
        onCopy = {}, onShare = {}, onMarkReadForKhatam = onMarkRead,
    )

    private fun show(isKhatamActive: Boolean = false, actions: AyahSheetActions = noopActions()) {
        composeRule.setThemedContent {
            AyahActionSheet(
                reference = AyahReference(18, 54, "Al-Kahf"),
                arabic = "ٱلْحَمْدُ لِلَّهِ",
                translation = "All praise belongs to God.",
                juzNumber = 15,
                pageNumber = 299,
                isBookmarked = false,
                isFavourite = false,
                isKhatamActive = isKhatamActive,
                actions = actions,
                onDismiss = {},
            )
        }
    }

    @Test
    fun `the sheet titles itself with the shared reference format`() {
        show()
        composeRule.onNodeWithText("Al-Kahf 18:54").assertIsDisplayed()
    }

    @Test
    fun `the khatam action is hidden without an active khatam`() {
        show(isKhatamActive = false)
        composeRule.onNodeWithText("Mark read for khatam").assertDoesNotExist()
    }

    @Test
    fun `the khatam action appears with an active khatam`() {
        show(isKhatamActive = true)
        composeRule.onNodeWithText("Mark read for khatam").assertIsDisplayed()
    }

    @Test
    fun `tapping bookmark reports it once`() {
        var calls = 0
        show(actions = noopActions(onBookmark = { calls++ }))
        composeRule.onNodeWithText("Bookmark").performClick()
        assertThat(calls).isEqualTo(1)
    }

    @Test
    fun `tapping mark-read reports it once`() {
        var calls = 0
        show(isKhatamActive = true, actions = noopActions(onMarkRead = { calls++ }))
        composeRule.onNodeWithText("Mark read for khatam").performClick()
        assertThat(calls).isEqualTo(1)
    }

    @Test
    fun `the sheet shows the verse it is acting on`() {
        show()
        composeRule.onNodeWithText("All praise belongs to God.").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "*AyahActionSheetTest*"
```

- [ ] **Step 3: Add the strings**

Add to `app/src/main/res/values/strings.xml` — every label must be a resource, or `lintDebug` will flag `MissingTranslation` once translations exist:

```xml
<string name="ayah_action_play_from_here">Play from here</string>
<string name="ayah_action_repeat">Repeat this ayah</string>
<string name="ayah_action_bookmark">Bookmark</string>
<string name="ayah_action_unbookmark">Remove bookmark</string>
<string name="ayah_action_favourite">Favourite</string>
<string name="ayah_action_unfavourite">Remove favourite</string>
<string name="ayah_action_note">Write a note</string>
<string name="ayah_action_tafseer">Tafseer</string>
<string name="ayah_action_subjects">Subjects</string>
<string name="ayah_action_copy">Copy</string>
<string name="ayah_action_share">Share</string>
<string name="ayah_action_mark_read">Mark read for khatam</string>
</string>
```

(Remove the stray closing tag when pasting — the last line above is `</string>` in error; the block ends after `ayah_action_mark_read`.)

- [ ] **Step 4: Implement the sheet**

A Material 3 `ModalBottomSheet` containing: the reference and `Juz N · page N`, the Arabic, the translation, then a two-column grid of actions. "Mark read for khatam" spans both columns and renders only when `isKhatamActive`.

- [ ] **Step 5: Run to verify it passes, then commit**

```bash
./gradlew :app:testDebugUnitTest --tests "*AyahActionSheetTest*"
git add app/src/main/java/com/arshadshah/nimaz/presentation/components/molecules/AyahActionSheet.kt \
        app/src/testDebug/java/com/arshadshah/nimaz/presentation/components/molecules/AyahActionSheetTest.kt \
        app/src/main/res/values/strings.xml
git commit -m "feat(quran): ayah actions move into a sheet

Every ayah carried a permanent five-icon pill, so five icons appeared for
every verse on screen. The sheet holds more actions than the pill ever did
and costs nothing until asked for."
```

---

### Task 2: One reading-mode control

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/quran/QuranReaderScreen.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/core/navigation/ScreenTags.kt`
- Test: `app/src/testDebug/java/.../screens/quran/QuranReaderModeControlTest.kt`

**Interfaces:**
- Consumes: nothing from Task 1.
- Produces: `ScreenTags.QuranReaderModeMenu = "quran_reader_mode_menu"` for the instrumented suite.

- [ ] **Step 1: Confirm the mode model with the design owner** — see "A design conflict to resolve" above. Do not start until answered.

- [ ] **Step 2: Write the failing test**

Assert: the app-bar control shows the current mode; opening it lists the modes; picking one switches; the reading position survives the switch.

- [ ] **Step 3: Replace the overflow item with an app-bar control**

`QuranReaderScreen.kt:410-440` currently holds a `canToggleView` overflow item reading "Switch to page view" / "Switch to list view". Replace it with an app-bar icon whose icon reflects the current mode and which opens a `NimazDropdownMenu` (the house action-menu component) of modes.

Keep `usePageView` as the backing state for now — this task changes the *control*, not the state model.

- [ ] **Step 4: Verify position is preserved**

The screen already has `LaunchedEffect(usePageView)` at `:174` and a `pendingScrollRestore` path at `:189`. Confirm both still fire from the new control; add a test that switching modes and back leaves `state.currentAyah` unchanged.

- [ ] **Step 5: Run, including the instrumented build**

```bash
./gradlew :app:testDebugUnitTest --tests "*QuranReaderModeControlTest*"
./gradlew :app:assembleDebugAndroidTest
```

- [ ] **Step 6: Commit**

```bash
git commit -m "feat(quran): one control for reading mode

Switching between the translation and the page was buried in the overflow
next to Passages and Settings, and nothing on screen said which mode you
were in. The control now shows the current mode and switches from one tap."
```

---

### Task 3: The anchor bar, and the ayah row

**Files:**
- Create: `presentation/components/molecules/ReaderAnchorBar.kt`
- Modify: `presentation/components/organisms/QuranAyahItem.kt`
- Test: `…/molecules/ReaderAnchorBarTest.kt`, `…/organisms/QuranAyahItemTest.kt`

**Interfaces:**
- Consumes: `AyahReference` (Phase 1), `AyahActionSheet` (Task 1).
- Produces:

```kotlin
@Composable
fun ReaderAnchorBar(
    title: String,
    subtitle: String,
    onGoTo: () -> Unit,
    modifier: Modifier = Modifier,
)
```

**Changes to `QuranAyahItem`:**

1. **Remove the action pill.** The row becomes one tap target opening `AyahActionSheet`.
2. **Translation renders as plain text**, not inside an outlined box.
3. **Remove the repeated `Juz N · Page N` pill** — it moves to the anchor bar, shown once.
4. **The per-ayah read circle is gated on `isKhatamActive`**, matching what `QuranMushafPageBar` already does.

- [ ] **Step 1: Write the failing tests** covering all four changes, plus: tapping the row opens the sheet; the read circle is absent without an active khatam.
- [ ] **Step 2: Run to verify they fail.**
- [ ] **Step 3: Implement the anchor bar.**
- [ ] **Step 4: Strip `QuranAyahItem`** — this is a deletion-heavy task; check for now-unused imports and helper composables and remove them too.
- [ ] **Step 5: Run tests; measure the win.** Screenshot the reader before and after on the emulator and record ayahs-visible in the commit message.
- [ ] **Step 6: Commit.**

```bash
git commit -m "feat(quran): a lighter ayah row, and the location shown once

The juz and page were repeated on every ayah, the translation sat in its own
outlined box, and the read circle showed for readers with no khatam at all.
The location moves to the anchor bar, the box goes, and the circle follows
the same isKhatamActive gate the page bar already uses."
```

---

### Task 4: The paper register

**Files:**
- Modify: `presentation/components/organisms/MushafPage.kt`
- Modify: `presentation/components/molecules/QuranMushafPageBar.kt`
- Test: existing mushaf render tests (`MushafLinePageTest`) must keep passing

**Interfaces:**
- Consumes: `QuranSurfaceColors.paper`, `.paperLine`, `.paperInk` (Phase 1).

Replace, on the **page** only:

| Today | Becomes |
|-------|---------|
| `pageSurface` ground | `paper` |
| `frameGold` double border + `frameTeal` inner hairline | a single `paperLine` hairline frame |
| scalloped cartouche | a ruled cartouche: hairline, Arabic surah name, hairline |
| gold rosette page number | a small `paperLine`-bordered medallion at the foot |
| body text colour | `paperInk` |

Add to the page bar: a hairline position indicator across the active script's `pagination.totalPages`.

**Do not touch Tafseer.** The gold/teal frame stays there by decision (spec §5.9), and `frameGold`/`frameTeal` remain in `QuranSurfaceColors` for it.

- [ ] **Step 1: Screenshot the current mushaf** in light and dark for comparison.
- [ ] **Step 2: Apply the paper tokens.**
- [ ] **Step 3: Re-screenshot and compare** — the page must read as paper in light and as a calm dark page at night, never as a glaring white card.
- [ ] **Step 4: Run `MushafLinePageTest` and `MushafLayoutFidelityTest`** — the renderer's geometry must be untouched; only colour changed.
- [ ] **Step 5: Commit.**

```bash
git commit -m "feat(quran): the mushaf page gets a paper register

The page borrowed the app's card surfaces and a heavy gold frame, so it read
as chrome rather than as a printed page. Cream ground, hairline rules and a
ruled cartouche instead. Tafseer keeps the gold frame by decision."
```

---

### Task 5: Close the phase

- [ ] Run every gate:

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
python3 scripts/check_docs.py
./gradlew :app:assembleDebugAndroidTest
```

- [ ] Update `docs/ARCHITECTURE.md` §8 with `AyahActionSheet` and `ReaderAnchorBar`.
- [ ] Update `docs/SUBSYSTEMS.md` §5/§6 if the mode-control change altered how `mushafScript` is set.

## Phase exit criteria

- [ ] Ayah actions open in a sheet; no permanent pill.
- [ ] Translation is plain text; juz/page appears once.
- [ ] The read circle only appears with an active khatam.
- [ ] One app-bar control shows and switches reading mode; position survives.
- [ ] The mushaf renders in the paper register in both themes; Tafseer unchanged.
- [ ] Ayahs visible on a 6" screen has roughly doubled — record the before/after.
