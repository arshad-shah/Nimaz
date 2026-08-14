# Qur'an Redesign — Phase 4: Recitation Player Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Grow the collapsed audio bar into a real player — seek, prev/next ayah, reciter inline, repeat, speed, follow-along and visible downloads.

**Architecture:** Far less new machinery than the spec implies. `AudioState` already carries download counts and position/duration, and `QuranAudioManager.seekToTotal()` already exists — so seek, the download strip and follow-along are **UI-only**. Only **repeat** and **speed** reach into the manager. Everything is exposed as state and driven by `QuranEvent`; the manager stays private.

**Tech Stack:** Kotlin, Compose, Media3/ExoPlayer, Hilt, Robolectric + Truth.

**Spec:** [`docs/superpowers/specs/2026-08-13-quran-redesign-design.md`](../specs/2026-08-13-quran-redesign-design.md) §7

**Depends on:** Phase 1 (`NimazSegmentedTabs` for the repeat and speed selectors), Phase 3 (the ayah sheet gains "Play from here" / "Repeat this ayah").

## Global Constraints

- **The manager stays private and is never handed to a screen.** `QuranViewModel.kt:82` declares `private val audioManager`; only `audioState: StateFlow<AudioState>` is exposed and every command goes through `onEvent`. `docs/ARCHITECTURE.md` §9 records this as an **accepted pattern** and the "audio engines handed to screens whole" deviation as **resolved**. Do not regress it — no new public manager field, no `audioManager.` call from a composable.
- Rules 1–8 from CLAUDE.md apply.
- Every user-visible label is a string resource.
- **Yellow/gold is ornament, not selection** — with one deliberate exception granted here: the follow-along highlight on the currently-reciting ayah. It marks *where the recitation is*, which is neither selection nor chrome.

---

## What already exists — read this first

Verified in `app/src/main/java/com/arshadshah/nimaz/data/audio/QuranAudioManager.kt`:

| Capability | Status |
|------------|--------|
| `AudioState.position` / `.duration` | **Exists** (`:48-49`) |
| `AudioState.isDownloading` / `.downloadProgress` / `.downloadedCount` / `.totalToDownload` | **Exists** (`:43-44,59-60`) |
| `AudioState.currentAyahId` / `.currentAyahIndex` / `.totalAyahs` / `.surahProgress` | **Exists** (`:45,56-57,64`) |
| `AudioState.reciterName` | **Exists** (`:52`) |
| `seekToTotal(totalPositionMs: Long)` | **Exists** (`:150`) — whole-surah coordinates |
| `setReciter(reciterId: String?)` | **Exists** (`:207`) |
| `setContinuousPlayback(enabled: Boolean)` | **Exists** (`:115`) |
| **Repeat (ayah / range / surah)** | **Missing** |
| **Playback speed** | **Missing** |

The June design (`2026-06-22-tafseer-reskin-and-quran-player-design.md`) said "no seek/scrub added". That was a **UI** decision — the manager could already do it. This phase reverses that decision only.

`QuranEvent` today has `PlayAyahAudio`, `PreviewReciter`, `PauseAudio`, `ResumeAudio`, `StopAudio`, `PlaySurahFromInfo`.

---

### Task 1: Seek, and the expanded player bar

**Files:**
- Modify: `presentation/viewmodel/quran/QuranEvent.kt`
- Modify: `presentation/viewmodel/quran/QuranViewModel.kt`
- Modify: `presentation/components/molecules/QuranAudioBottomBar.kt`
- Test: `app/src/test/java/.../QuranViewModelAudioTest.kt`, `…/molecules/QuranAudioBottomBarTest.kt`

**Interfaces:**
- Produces:

```kotlin
// QuranEvent.kt
data class SeekAudioTo(val positionMs: Long) : QuranEvent
data object NextAyahAudio : QuranEvent
data object PreviousAyahAudio : QuranEvent
```

- [ ] **Step 1: Write the failing ViewModel test**

```kotlin
@Test fun `SeekAudioTo forwards the position to the audio manager`()
@Test fun `NextAyahAudio advances the playlist`()
@Test fun `PreviousAyahAudio steps back`()
@Test fun `seeking past the end clamps to the end`()
```

Use the existing fake/spy style in `app/src/test/java/com/arshadshah/nimaz/presentation/viewmodel/quran/`. Inspect a neighbouring test first:

```bash
ls app/src/test/java/com/arshadshah/nimaz/presentation/viewmodel/quran/
```

- [ ] **Step 2: Run to verify it fails.**

- [ ] **Step 3: Add the events and handle them** in `QuranViewModel.onEvent`, delegating to `audioManager.seekToTotal(...)` and the player's next/previous. The manager stays private.

- [ ] **Step 4: Rebuild the bar** — `QuranAudioBottomBar` gains:
  - now-playing (surah, `Ayah N of M`, page),
  - prev / play-pause / next / expand,
  - a seek row: elapsed, draggable rail, remaining,
  - a meta row: reciter name (tappable) · style · speed when ≠ 1× · repeat summary when on.

  All values come from `AudioState`; the bar computes nothing.

- [ ] **Step 5: Run tests and commit.**

```bash
git commit -m "feat(quran): the audio bar becomes a player

The manager has had whole-surah seek since the playlist work; only the UI
never offered it. Adds the seek rail, prev/next ayah and the reciter inline,
all from state the manager already publishes."
```

---

### Task 2: The download strip

**Files:**
- Modify: `presentation/components/molecules/QuranAudioBottomBar.kt`
- Test: `…/molecules/QuranAudioBottomBarTest.kt`

**UI-only.** `AudioState.isDownloading`, `.downloadedCount`, `.totalToDownload` already exist and are already maintained.

- [ ] **Step 1: Write the failing test**

```kotlin
@Test fun `the download strip appears while downloading`()
@Test fun `the download strip reports the counts`()          // "Downloading 42 of 110"
@Test fun `the download strip disappears when not downloading`()
@Test fun `the player shows even when only downloading, before playback starts`()
```

That last case matters: a reader who taps play on a surah with no cached audio must see *something* happen.

- [ ] **Step 2–4:** add the string `<string name="audio_downloading">Downloading %1$d of %2$d</string>`, render the strip above the player body, run, commit.

```bash
git commit -m "feat(quran): downloads become visible

Ayahs are fetched before playback and cached under quran_audio/, but the
reader saw nothing between tapping play and hearing sound. The counts were
already on AudioState; this only shows them."
```

---

### Task 3: Repeat

**Files:**
- Modify: `data/audio/QuranAudioManager.kt`
- Modify: `presentation/viewmodel/quran/QuranEvent.kt`, `QuranViewModel.kt`
- Create: `domain/model/RecitationRepeat.kt`
- Test: `app/src/test/java/.../domain/model/RecitationRepeatTest.kt`, `…/QuranViewModelAudioTest.kt`

**Interfaces:**
- Produces:

```kotlin
sealed interface RecitationRepeat {
    data object Off : RecitationRepeat
    data class Ayah(val times: Int) : RecitationRepeat          // times >= 2
    data class Range(val fromAyah: Int, val toAyah: Int) : RecitationRepeat
    data object Surah : RecitationRepeat
}

// QuranEvent.kt
data class SetRecitationRepeat(val repeat: RecitationRepeat) : QuranEvent

// AudioState gains:
val repeat: RecitationRepeat = RecitationRepeat.Off
```

**Implementation notes.**

- `Surah` maps to ExoPlayer's `Player.REPEAT_MODE_ALL` on the playlist.
- `Ayah(times)` cannot use `REPEAT_MODE_ONE` alone, because it must stop after N and move on. Count completions in the `onMediaItemTransition` / `onPlaybackStateChanged` listener the manager already has (`:229`, `:281`) and advance when the count is reached.
- `Range(from, to)` seeks back to `from` when the playlist passes `to`.
- **Guard the invariants in the domain type, not the manager**: `Ayah.times >= 2`, `Range.fromAyah <= Range.toAyah`. That is what `RecitationRepeatTest` covers, and it needs no Android.

- [ ] **Step 1: Write the failing domain test** — construction guards, and a `nextIndexAfter(currentIndex, completions)` helper if you introduce one.
- [ ] **Step 2: Write the failing ViewModel test** — the event reaches the manager and is reflected in `audioState.repeat`.
- [ ] **Step 3–5:** implement, run, commit.

```bash
git commit -m "feat(quran): repeat an ayah, a range or the surah

Repeating a verse until it is memorised was the most obvious thing the
player could not do. Ayah repeat counts completions rather than using
REPEAT_MODE_ONE, because it has to stop after N and carry on."
```

---

### Task 4: Speed

**Files:**
- Modify: `data/audio/QuranAudioManager.kt`, `QuranEvent.kt`, `QuranViewModel.kt`
- Test: `…/QuranViewModelAudioTest.kt`

**Interfaces:**
- Produces: `data class SetPlaybackSpeed(val speed: Float) : QuranEvent`; `AudioState.speed: Float = 1f`.

Allowed values: `0.75f`, `1f`, `1.25f`, `1.5f`. Apply via ExoPlayer `setPlaybackSpeed`. Persist nothing — speed resets to 1× on a new session, because a reader who slowed one difficult passage does not want every later session slowed.

- [ ] **Step 1–5:** failing test (including that an out-of-set value is rejected or clamped) → implement → run → commit.

---

### Task 5: Follow-along, and the recitation sheet

**Files:**
- Create: `presentation/components/molecules/RecitationSheet.kt`
- Modify: `presentation/screens/quran/QuranReaderScreen.kt`, `QuranEvent.kt`
- Test: `…/molecules/RecitationSheetTest.kt`

**Interfaces:**
- Consumes: `NimazSegmentedTabs` (Phase 1) for the repeat and speed selectors; `RecitationRepeat` (Task 3).
- Produces: `data class SetFollowAlong(val enabled: Boolean) : QuranEvent`.

The sheet: reciter row → reciter picker; **Repeat** segment (Off / Ayah / Range / Surah) with a count stepper for Ayah and a from–to picker for Range; **Speed** segment; a "Follow along and turn the page" toggle; Stop / Done.

**Follow-along is UI-only** — `AudioState.currentAyahId` already identifies the reciting ayah. Highlight it in **all** reading modes, and when follow-along is on, scroll (list) or page (mushaf) to keep it visible.

- [ ] **Step 1: Write the failing tests** — each segment reports its selection; the count stepper floors at 2; the toggle reports both states; Stop dispatches `StopAudio`.
- [ ] **Step 2–5:** implement, wire the highlight into `QuranAyahItem` and `MushafPage`, run, commit.

---

### Task 6: Correct the stale subsystem doc, and close the phase

**Files:**
- Modify: `docs/SUBSYSTEMS.md` §1

`docs/SUBSYSTEMS.md` §1 currently states that ViewModels inject the audio manager directly and that "`QuranViewModel` even exposes the manager as a public field — a known clean-architecture deviation, not a pattern to copy."

**That is false.** `QuranViewModel.kt:82` is `private val audioManager`, only `audioState` is exposed, and `docs/ARCHITECTURE.md` §9 lists the deviation as resolved and flow-forwarding as an accepted pattern. The two documents contradict each other.

- [ ] **Step 1: Rewrite the paragraph** to say that playback ViewModels inject the manager and forward its `StateFlow`, that the manager is private in every one of them, and that commands are dispatched as events — cross-referencing `ARCHITECTURE.md` §9's accepted-patterns list rather than restating it.
- [ ] **Step 2: Document the new capability** in §1: repeat modes, speed, follow-along, and the download strip.
- [ ] **Step 3: Run every gate.**

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
python3 scripts/check_docs.py
```

- [ ] **Step 4: Commit.**

```bash
git commit -m "docs(subsystems): the audio deviation was fixed, the doc was not

SUBSYSTEMS.md still described QuranViewModel as exposing the audio manager
publicly, which ARCHITECTURE.md lists as resolved and the code contradicts.
Corrects it, and records repeat, speed, follow-along and the download strip."
```

## Phase exit criteria

- [ ] Seek, prev/next ayah, inline reciter, download strip, repeat, speed and follow-along all work.
- [ ] `audioManager` is still `private` in every ViewModel; no composable calls it.
- [ ] `AudioState` carries `repeat` and `speed`; both are driven by `QuranEvent`.
- [ ] `SUBSYSTEMS.md` §1 no longer contradicts `ARCHITECTURE.md` §9.
- [ ] All four gates pass.
