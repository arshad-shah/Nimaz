# Qur'an Redesign — Phase 5: Content Screens & Khatam Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finish the section — rolled-up subject counts and ayahs, the Passages outline, tighter Subjects rows, Background's doubled headings, Search's counts and Subjects results, and the two Khatam defects.

**Architecture:** Mostly presentation, with two real domain additions: a recursive roll-up over the subject tree, and today's-portion for a khatam. Both are computed once and cached, never per composition. The two Khatam defects are each a one-line-ish fix in a place this phase is already editing.

**Tech Stack:** Kotlin, Compose, Robolectric + Truth, JUnit for the domain work.

**Spec:** [`docs/superpowers/specs/2026-08-13-quran-redesign-design.md`](../specs/2026-08-13-quran-redesign-design.md) §5.6–5.12, §10

**Depends on:** Phase 1 (`NimazSegmentedTabs`, `NimazTreeNode`, `AyahReference`).

## Global Constraints

- Rules 1–8 from CLAUDE.md apply.
- **Yellow is retired as a selection colour** (spec §6.5). This phase removes its last three uses: Background's `✓ Name` chip, Search's `All` chip, and Passages' "Reading" chip.
- **Content faults are not fixed here.** The duplicate subjects, the "Doctraine" typo and the passage prose faults are filed against `arshad-shah/nimaz-data` (spec §9). Do not dedupe in the app — that hides the data problem.
- Row density target ~64 px, matching Browse and Saved from Phase 2.

---

## What already exists — read this first

| Thing | Where | State |
|-------|-------|-------|
| `Themes · Kinds · Index` segment | `QuranTopicsScreen` | **Ships** — only needs re-skinning onto `NimazSegmentedTabs` |
| Tree rows | `QuranTopicsScreen.kt:184` `items(state.rows, key = { it.topic.id })` | Ships; bare text, no counts |
| `KhatamInsights` | `domain/model/KhatamModels.kt:98-115` — `daysActive`, `averagePace`, `currentStreak`, `longestStreak`, `juzCompleted`, `currentJuz`, `remainingAyahs`, `estimatedDaysRemaining`, `projectedCompletionAt`, `paceStatus` | Ships, rich |
| `KhatamPace` | `KhatamModels.kt:78-90` — `NOT_STARTED`, `ON_TRACK`, `SLIGHTLY_BEHIND`, `BEHIND` | Ships |
| `paceStatus(...)` | `KhatamModels.kt:186` | Ships — **and is the bug**, see Task 6 |
| Stat tiles | `KhatamDetailScreen.kt:164-177` → `NimazStatsGrid` | Ships, and **does** pass `"0"` — see Task 5 |
| Journey trail | `KhatamJourneyTrail` | Ships — kept |
| Passages timeline + reading marker | `SurahPassagesScreen` | Ships — kept, recoloured |
| Search chips + highlighting | `QuranSearchScreen` | Ships — kept, gains counts |

---

### Task 1: Rolled-up subject counts

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/domain/usecase/quran/RollUpTopicCounts.kt`
- Test: `app/src/test/java/.../RollUpTopicCountsTest.kt`

**Interfaces:**
- Produces:

```kotlin
/**
 * Total ayahs beneath each node, children included, keyed by topic id.
 * Computed once per tree load and cached in the ViewModel — never per composition.
 */
class RollUpTopicCounts @Inject constructor() {
    operator fun invoke(nodes: List<TopicNode>): Map<Int, Int>
}
```

  Use the tree's real node type from `QuranTopicsUiState` / the topics domain model rather than inventing `TopicNode`; confirm with:

```bash
grep -n "rows\|TopicRow\|topic" app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/quran/QuranTopicsUiState.kt | head
```

- [ ] **Step 1: Write the failing test**

```kotlin
@Test fun `a leaf rolls up to its own count`()
@Test fun `a parent includes its children`()
@Test fun `a grandparent includes the whole subtree`()
@Test fun `a node citing nothing itself still reports its children's total`()
@Test fun `an empty tree yields an empty map`()
@Test fun `a cycle does not hang`()          // defensive: visited-set guard
```

The fourth test is the point of the whole task — it is what makes a root showing "0 verses" impossible.

- [ ] **Step 2–5:** run-fail → implement with an explicit visited set → run-pass → commit.

---

### Task 2: Themes tree onto the new components

**Files:**
- Modify: `presentation/screens/quran/QuranTopicsScreen.kt`
- Modify: `presentation/viewmodel/quran/QuranTopicsViewModel.kt`, `QuranTopicsUiState.kt`
- Test: `…/screens/quran/QuranTopicsScreenTest.kt`

- [ ] Replace the segment with `NimazSegmentedTabs`.
- [ ] Replace the bare rows with `NimazTreeNode`, passing the rolled-up count.
- [ ] Cache the roll-up in the ViewModel; assert in a test that it is computed **once** per tree load, not per emission.
- [ ] **Decide the open question** (spec §5.10): chevron-expands / label-navigates, or whole-row. `NimazTreeNode` supports both — pass `onClick = null` for whole-row. Settle it on the emulator and record the choice in the commit message.

```bash
git commit -m "feat(quran): the subject tree gains scale

Home advertises 2,512 hand-indexed subjects and the tree then showed three
bare words with no sense of how much sat under any of them. Counts roll up
through the subtree, so a branch is never reported as empty."
```

---

### Task 3: Topic detail — ayahs everywhere

**Files:**
- Modify: `presentation/screens/quran/QuranTopicDetailScreen.kt`, its ViewModel and state
- Test: `…/QuranTopicDetailViewModelTest.kt`

A branch topic must list the ayahs of everything beneath it, so "Doctrine" is never empty and the `0 verses` state becomes unreachable.

- [ ] **Step 1: Write the failing ViewModel test** — a branch topic with no citations of its own yields its children's ayahs; ordering is stable; the `fromSurah` pin still works.
- [ ] **Step 2–5:** implement, using the roll-up from Task 1 for the count and a matching roll-up for the ayah list; run; commit.

Also here: show the Arabic name, and render subtopics as tappable rows with counts and chevrons rather than plain bullets.

---

### Task 4: Passages, Subjects, Background, Search

Four smaller screens, one commit each.

**4a — Passages** (`SurahPassagesScreen`)
- [ ] Clamp entries to two lines with an ellipsis; show the full text on the passage currently being read.
- [ ] **Recolour the reading marker to teal** — dot and chip.
- [ ] Retitle the screen as an outline; drop any "notable" framing.
- [ ] Fix the plural: "1 passages across 7 verses" → a `plurals` resource.
- [ ] Test: an 18-row surah renders 18 rows; the marked passage is the one containing `currentAyah`; a one-passage surah reads "1 passage".

**4b — Subjects** (`SurahSubjectsScreen`)
- [ ] Compress to ~64 px, **keeping** the Arabic name, the count badge and the "N more verses elsewhere" line — these beat the prototype and are the reason to keep the screen.
- [ ] Add a chevron; the rows carry no affordance today.
- [ ] Test: the "Every verse on this subject is in this surah" special case still renders.

**4c — Background** (`SurahBackgroundScreen`)
- [ ] **One heading per section** — delete the large duplicate, keep the eyebrow. Recovers ~90 px per section.
- [ ] Restyle the chip row as a **position indicator, not a filter**: no check mark, no selected-yellow.
- [ ] Add the pull-quote: cited ayahs in a gold-ruled block with their reference, tapping to the reader. Gold is correct here — it is ornament around scripture, not selection.
- [ ] Test: a section renders its name once; the chip row marks the visible section.

**4d — Search** (`QuranSearchScreen`)
- [ ] Put per-type counts on the chips (`Quran 42`, `Hadith 18`, …).
- [ ] **Add Subjects as a result type**, tapping into topic detail.
- [ ] Recolour the `All` chip off yellow.
- [ ] Keep the term highlighting.
- [ ] Test: counts match the per-type result totals; a Subjects result navigates to `QuranTopicDetail`.

---

### Task 5: The Khatam stat tiles

**Files:**
- Modify: `presentation/components/organisms/NimazStatsGrid.kt`
- Test: `…/organisms/NimazStatsGridTest.kt`

**Diagnosis first — the earlier reading of this was wrong.** `KhatamDetailScreen.kt:164-177` **does** pass `"0"` for all three tiles. The data is fine. The zero renders large and light enough that it reads as a bare outlined ellipse rather than a digit — a **presentation** defect in `NimazStatsGrid`, not a missing-value one.

- [ ] **Step 1: Confirm on the emulator** — a fresh khatam, screenshot the three tiles, and verify the glyph really is `0`. If it is genuinely blank, this task changes to finding why `NimazStatData.value` is dropped, and the commit message must say so.
- [ ] **Step 2: Write the failing test** — a stat with value `"0"` renders a readable `0`; contrast between the value and the tile meets the text bar.
- [ ] **Step 3: Fix the typography/colour** so a zero reads as a number. Consider a de-emphasised-but-legible treatment for zero rather than a dash, since "0 day streak" is true and informative where "—" is not.
- [ ] **Step 4–5:** run, commit.

---

### Task 6: A new khatam is not behind

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/domain/model/KhatamModels.kt:186`
- Test: `app/src/test/java/.../KhatamProgressCalculatorTest.kt`

**The bug.** `paceStatus` grants `NOT_STARTED` only when `daysActive <= 0`:

```kotlin
fun paceStatus(averagePace: Float, dailyTarget: Int, daysActive: Int): KhatamPace = when {
    daysActive <= 0 -> KhatamPace.NOT_STARTED
    dailyTarget <= 0 -> KhatamPace.ON_TRACK
    averagePace >= dailyTarget -> KhatamPace.ON_TRACK
    averagePace >= dailyTarget * BEHIND_TOLERANCE -> KhatamPace.SLIGHTLY_BEHIND
    else -> KhatamPace.BEHIND
}
```

On its **first** day a plan has `daysActive == 1` and `averagePace == 0f`, so it falls through to `BEHIND` — which is why a khatam created seconds ago opens saying "Behind pace" in red.

- [ ] **Step 1: Write the failing test**

```kotlin
@Test fun `a khatam on its first day is not yet behind`() {
    val pace = KhatamProgressCalculator.paceStatus(
        averagePace = 0f, dailyTarget = 20, daysActive = 1,
    )
    assertThat(pace).isEqualTo(KhatamPace.NOT_STARTED)
}

@Test fun `a khatam that has read nothing by day two is behind`() {
    val pace = KhatamProgressCalculator.paceStatus(
        averagePace = 0f, dailyTarget = 20, daysActive = 2,
    )
    assertThat(pace).isEqualTo(KhatamPace.BEHIND)
}

@Test fun `a khatam meeting its target on day one is on track, not not-started`() {
    val pace = KhatamProgressCalculator.paceStatus(
        averagePace = 20f, dailyTarget = 20, daysActive = 1,
    )
    assertThat(pace).isEqualTo(KhatamPace.ON_TRACK)
}
```

That third test matters: the grace must not swallow a genuinely good first day.

- [ ] **Step 2: Run to verify the first test fails** (it will report `BEHIND`).

- [ ] **Step 3: Fix**

```kotlin
fun paceStatus(averagePace: Float, dailyTarget: Int, daysActive: Int): KhatamPace = when {
    daysActive <= 0 -> KhatamPace.NOT_STARTED
    dailyTarget <= 0 -> KhatamPace.ON_TRACK
    averagePace >= dailyTarget -> KhatamPace.ON_TRACK
    // Day one has not finished yet, so "behind" cannot be true of it: a reader
    // who has done nothing at 9am is not behind, they have not started.
    daysActive == 1 && averagePace <= 0f -> KhatamPace.NOT_STARTED
    averagePace >= dailyTarget * BEHIND_TOLERANCE -> KhatamPace.SLIGHTLY_BEHIND
    else -> KhatamPace.BEHIND
}
```

Update the `NOT_STARTED` KDoc, which currently says "No `startedAt` yet, or fewer than a full day elapsed" — that was the intent; the code did not implement it.

- [ ] **Step 4: Run all three; check every caller.** `paceStatus` is documented as "the single place this is decided so the list, detail, home card and widget cannot disagree" — so this fix reaches the widget too. Verify no caller special-cases `NOT_STARTED` into "Behind".

- [ ] **Step 5: Commit**

```bash
git commit -m "fix(khatam): a plan created today is not behind pace

NOT_STARTED was only granted for daysActive <= 0, but a plan created today
has daysActive == 1 and no pace yet, so it fell through to BEHIND. Every new
khatam opened by telling the reader in red that they had already failed.
The KDoc already described the intended behaviour."
```

---

### Task 7: Today's portion

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/domain/usecase/khatam/GetTodaysPortion.kt`
- Modify: `presentation/screens/khatam/KhatamDetailScreen.kt`, `KhatamDetailUiState`
- Test: `app/src/test/java/.../GetTodaysPortionTest.kt`

**Interfaces:**
- Produces:

```kotlin
data class KhatamPortion(
    val fromSurah: Int,
    val fromAyah: Int,
    val toSurah: Int,
    val toAyah: Int,
    val label: String,      // e.g. "Juz 18 · Al-Mu'minun to An-Nur"
)

class GetTodaysPortion @Inject constructor(/* … */) {
    suspend operator fun invoke(khatamId: Long): KhatamPortion?
}
```

Derived from `Khatam.dailyTarget` and progress so far. Returns `null` when the plan is complete.

- [ ] **Step 1: Confirm the inputs exist** — `KhatamDetailUiState` already carries `khatam`, `juzProgress`, `dailyLogs`, `insights`, `nextUnreadSurah`, `nextUnreadAyah`. Check whether `nextUnreadSurah/Ayah` plus `dailyTarget` is enough before adding repository calls.
- [ ] **Step 2: Write the failing test** — a fresh plan's portion starts at the beginning; a partly-read plan's portion starts at the next unread ayah; a finished plan returns null; a plan behind schedule still returns *today's* portion, not the backlog.
- [ ] **Step 3–5:** implement, surface it as the detail screen's primary action ("Read today's portion") with resume secondary, add a recent-days list beneath the journey trail, run, commit.

---

### Task 8: Close the phase and the redesign

- [ ] Run every gate:

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
python3 scripts/check_docs.py
./gradlew :app:assembleDebugAndroidTest
npm install --no-save mermaid jsdom && node scripts/check_mermaid.mjs
```

- [ ] Update `docs/ARCHITECTURE.md` §8 for anything new; tick anything resolved in `docs/CLEAN_ARCHITECTURE_CHECKLIST.md`.
- [ ] File the content faults (spec §9) against `arshad-shah/nimaz-data`.
- [ ] Confirm the **bookmark-key crash** (spec §10) has been fixed on its own branch and merged — the redesign should not ship on top of a known home-screen crash.
- [ ] Re-walk all thirteen screens on the emulator in **light and dark**, against the prototypes.

## Phase exit criteria

- [ ] Tree nodes carry rolled-up counts; no topic reports "0 verses".
- [ ] Every topic lists ayahs, branches included.
- [ ] Passages clamps to two lines, is titled an outline, and its marker is teal.
- [ ] Subjects rows are ~64 px with a chevron, keeping the Arabic name and context line.
- [ ] Background prints each heading once; the chip row is a position indicator.
- [ ] Search chips carry counts; Subjects is a result type.
- [ ] Yellow no longer marks selection anywhere in the section.
- [ ] A khatam created today reads `NOT_STARTED`, not "Behind pace".
- [ ] The stat tiles read as numbers.
- [ ] Khatam detail leads with today's portion.
