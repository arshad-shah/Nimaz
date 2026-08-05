# Screen States — Layer 3: Silent Failures Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make every failure these eight features already detect visible to the reader, and make the ones they don't detect yet exist at all.

**Architecture:** Each group follows one recipe — `error: String?` becomes `error: UiError?`, every read path gains an `onFailure`/`fallback` that sets it, and the screen renders the four states in the fixed order. Write failures are treated differently from read failures and never replace the screen.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, kotlinx.coroutines, JUnit4 + Truth + MockK + `kotlinx-coroutines-test`.

## Global Constraints

- Spec: `docs/superpowers/specs/2026-08-05-screen-state-migration-design.md`. Rule: `ARCHITECTURE.md` §8.
- Branch `epic/ss-03-silent-failures`, stacked on `epic/ss-02-about-licenses` (stack #447).
- All `CLAUDE.md` non-negotiables apply. Commits carry no `Co-Authored-By` trailer.
- **User-facing copy is a `@StringRes`.** No `error = e.message`, no `"Hadith not found"` literal.
  The exception text goes in `UiError.details` and nowhere else.
- Every group ends green: `./gradlew :app:compileDebugKotlin`, `:app:testDebugUnitTest`,
  `python3 scripts/check_docs.py`.
- Each group deletes its own entries from `ScreenStateConventionTest`'s backlogs. **Entries are
  only ever removed.** If a group cannot empty an entry, it stays and the reason goes in a comment.

## The two failure kinds

This layer's one real design decision, and it is not mechanical:

**A failed read** is the screen's state. Nothing valid is on screen, so `NimazErrorState` replaces
the body — `FULLSCREEN` when the screen is bare, `SECTION` when only one section failed.

**A failed write** — deleting a bookmark, saving a zakat calculation, updating a note — is *not*
the screen's state. The content on screen is still correct and still useful; replacing it with a
full-screen error because a delete failed destroys good content to report a bad button press.
Those get a transient surface (the screen's existing snackbar) or an `INLINE` error next to the
control, and they **never** touch `isLoading`.

Both still pass `onFailure`. The ratchet asks whether the failure is recorded, not how it is shown.

## The recipe

Applied once per group. Worked example is Task 1 (Hadith); every later group is the same five
moves against the files in its own row of the table.

1. **State** — `val error: String? = null` → `val error: UiError? = null`, importing
   `presentation.viewmodel.UiError`.
2. **Read paths** — every `viewModelScope.launch { … }` that a screen waits on becomes
   `launchSafely(telemetry, DOMAIN, "<type>", onFailure = { … })`, and every collected flow gains
   `.catchAndReport(telemetry, DOMAIN, "<type>") { … }` **inside** any `flatMapLatest`. The
   handler sets `isLoading = false` and a `UiError`.
3. **Write paths** — existing `launchSafely` calls with no `onFailure` gain one that surfaces
   transiently per the rule above, leaving `isLoading` untouched.
4. **Screen** — the four-state `when`, with the scaffold's `paddingValues` on each state
   component, and a `Retry` event wired to the primary action.
5. **Tests** — per ViewModel: a fake use case that throws → assert `isLoading = false` and a
   non-null `error`; and `Retry` clears it and re-issues. Then delete the group's backlog entries.

---

### Task 1: Hadith

**Files:**
- Modify: `presentation/viewmodel/content/HadithUiState.kt` (3 states carry `error`)
- Modify: `presentation/viewmodel/content/HadithViewModel.kt:142-263`
- Modify: `presentation/screens/hadith/{HadithCollectionScreen,HadithChaptersScreen,HadithReaderScreen}.kt`
- Create: `test/…/presentation/viewmodel/content/HadithFailureTest.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `UiError`, `NimazErrorKind`, `NimazErrorState`, `NimazErrorDefaults` (layer 1).
- Produces: `HadithEvent.Retry` — the pattern every later group copies.

Two defects specific to this group, beyond the missing rendering:

- `loadAllBooks()` and `loadHadithOfTheDay()` are bare `viewModelScope.launch` with a `collect`
  and **no `try`**. `viewModelScope` is a `SupervisorJob`, so a Room failure there is not
  contained — it reaches the thread's uncaught handler and takes the app down. This is the exact
  case `launchSafely` exists for.
- `startReaderLoad` sets `error = "Hadith not found"` — an English literal on a user's screen in
  a build that translates everything, and the wrong *kind*: a missing hadith is `NOT_FOUND`, not
  a generic failure.

- [ ] **Step 1: Add the strings**

```xml
<string name="hadith_books_load_failed">The hadith collections couldn\'t be loaded</string>
<string name="hadith_chapters_load_failed">This collection\'s chapters couldn\'t be loaded</string>
<string name="hadith_load_failed">This chapter couldn\'t be loaded</string>
<string name="hadith_not_found">That hadith isn\'t in the collection</string>
<string name="hadith_load_failed_body">The hadith library is stored on your device, so trying again usually works.</string>
```

- [ ] **Step 2: Write the failing tests**

`app/src/test/java/com/arshadshah/nimaz/presentation/viewmodel/content/HadithFailureTest.kt`,
with a `StandardTestDispatcher`, `RecordingTelemetry`, and `mockk(relaxed = true)` use cases —
the same shape as `LicensesViewModelTest`. Four cases:

1. `getAllBooks()` returns a flow that throws → collection state ends `isLoading = false` with
   `error?.message == R.string.hadith_books_load_failed`, and the app does not crash (the test
   completing at all is the assertion — before `launchSafely` this propagated).
2. `getChaptersByBook()` throws → chapters state carries `R.string.hadith_chapters_load_failed`
   and `details == "…"` (the exception's message, not the readable copy).
3. `getHadithById()` returns null → reader state carries `NimazErrorKind.NOT_FOUND` and
   `R.string.hadith_not_found`, and `telemetry.errors` is **empty** — a missing hadith is an
   answer, not a failure.
4. `Retry` after a failed chapter load clears `error` and re-issues `getChaptersByBook`.

- [ ] **Step 3: Run them and confirm they fail**

Run: `./gradlew :app:testDebugUnitTest --tests '*HadithFailureTest*'`
Expected: FAIL — `error` is a `String?`, so `error?.message` does not resolve.

- [ ] **Step 4: Migrate the three states**

`error: String?` → `error: UiError?` in `HadithCollectionUiState`, `HadithChaptersUiState`,
`HadithReaderUiState`.

- [ ] **Step 5: Migrate the ViewModel's read paths**

- `loadAllBooks` / `loadHadithOfTheDay` → `launchSafely(…, onFailure = { … })` with the collected
  flow guarded by `.catchAndReport`.
- `loadBook`'s `try`/`catch` → `launchSafely`, keeping `chaptersJob` as the returned handle.
- `startReaderLoad`'s `catch` → `launchSafely`, and its not-found branch to
  `UiError(R.string.hadith_not_found, NimazErrorKind.NOT_FOUND)` with no telemetry call.

Keep every existing `Job` handle: the cancel-and-replace semantics documented on `readerJob` and
`chaptersJob` are load-bearing, and `launchSafely` returns the `Job` precisely so they survive.

- [ ] **Step 6: Add `HadithEvent.Retry` and wire the three screens**

Each screen body becomes the four-state `when` from the recipe. `HadithCollectionScreen`'s
`if (state.isLoading)` at line 123 is the first branch; the error branch takes
`NimazErrorDefaults.retry { viewModel.onEvent(HadithEvent.Retry) }`.

- [ ] **Step 7: Run the tests and the ratchet**

```bash
./gradlew :app:testDebugUnitTest --tests '*HadithFailureTest*'
./gradlew :app:testDebugUnitTest --tests '*ScreenStateConventionTest*'
```
Expected: the first passes; the second **fails** saying `HadithUiState.kt` is a stale entry.

- [ ] **Step 8: Delete the group's backlog entries and re-run**

Remove `"HadithUiState.kt"` from `acceptedUnreadErrors` and `"HadithReaderScreen.kt"` from
`acceptedSpinners` (its spinner goes with the migration). Re-run — green.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/content/ \
        app/src/main/java/com/arshadshah/nimaz/presentation/screens/hadith/ \
        app/src/test/java/com/arshadshah/nimaz/presentation/viewmodel/content/HadithFailureTest.kt \
        app/src/test/java/com/arshadshah/nimaz/presentation/screens/ScreenStateConventionTest.kt \
        app/src/main/res/values/strings.xml
git commit -m "fix(hadith): a failed load says so, and a missing hadith stops crashing the app"
```

---

### Tasks 2-8: the remaining groups

Same five moves. Each is its own commit and its own ratchet deletion.

| # | Group | State file(s) | ViewModel sites | Screens | Notes specific to the group |
|---|---|---|---|---|---|
| 2 | Help | `help/HelpUiState.kt` ×3 | `HelpViewModel` — 3 `catchAndReport` fallbacks already set `error = throwable.message` | `HelpScreen`, `HelpTopicDetailScreen`, `HelpGuideScreen` | The recovery semantics are already right (guarded **inside** `flatMapLatest`, pinned by `HelpViewModelRecoveryTest`) — do not restructure them, only change what the fallback stores. `HelpTopicDetailScreen` also loses a raw spinner. |
| 3 | Bookmarks | `quran/BookmarksUiState.kt` | `BookmarksViewModel` — 3 read collectors at :106/:146/:182 set `error = throwable.message`; **6 write** `launchSafely` at :314-380 pass no `onFailure` | `BookmarksScreen` | The six writes are deletes, an undo, a note edit and clear-all. Per the two-failure-kinds rule they surface on the screen's existing snackbar and must not touch `isLoading` — a failed delete may not blank a list of bookmarks. |
| 4 | Tafseer | `quran/TafseerChaptersUiState.kt` | `TafseerChaptersViewModel:45`; `TafseerViewModel` — 3 `launchSafely` with no `onFailure` | `TafseerChaptersScreen`, `TafseerScreen` | Both screens also lose a raw spinner. |
| 5 | SurahThematic | `quran/SurahThematicUiState.kt` ×2 | `SurahThematicViewModel:96-110` — already has `onFailure`, only the type changes | `SurahSubjectsScreen`, `SurahPassagesScreen`, `SurahBackgroundScreen` | **The empty-before-error bug lives here.** All three evaluate `isEmpty()` before `error`, so a failed load reports "there is nothing here". Fixing the order is the point of this task; the `NimazEmptyState` branches stay, they just stop catching failures. All three lose a raw spinner. |
| 6 | Search | `search/SearchUiState.kt` | `SearchViewModel:145-163` — `e.message ?: "Search failed"` | `SearchScreen` | Only the **local library** search error; the AI path already renders `AskErrorCard` and is out of scope. A failed search is a `SECTION` error above the results, not a full-screen one — the query field must stay usable. |
| 7 | Home | `home/HomeUiState.kt` | `HomeViewModel:516-524`; nine bare `viewModelScope.launch` | `HomeScreen` | The dashboard is a stack of independent cards. A failure in one loader must **not** replace the whole dashboard: this is the clearest `SECTION` case in the app. Keep each card's failure to its own card. |
| 8 | Zakat | `tools/ZakatUiState.kt` | `ZakatViewModel:215` (calculate, a read); 5 write `launchSafely` at :234-261 | `ZakatCalculatorScreen` | A failed *calculation* is an `INLINE` error next to the result, not a full-screen one — the user's entered figures must survive it. The five writes follow the write rule. |

Each task's steps are Task 1's, with that row's files substituted. Do not batch groups into one
commit: the point of one-commit-per-group is that a reviewer can reject Home's section treatment
without rejecting Hadith's.

## Verification for the layer

- Per group: `compileDebugKotlin`, the group's new failure test, `ScreenStateConventionTest`.
- End of layer: full `testDebugUnitTest`, `check_docs.py`.
- `acceptedUnreadErrors` should end this layer holding only the four **vestigial** entries
  (`FastingUiState`, `PrayerTrackerUiState`, `QuranUiState`, `TasbihUiState`), which layer 6
  resolves. `acceptedSilentFailures` should be empty except `AskViewModel`, `CalendarViewModel`,
  `CatalogViewModel`, `DuaViewModel`, `PrayerTrackerViewModel` and `SearchSettingsViewModel`,
  which belong to layer 4.
- If a group's screens change behaviour visibly, note it for the layer's visual walk.
