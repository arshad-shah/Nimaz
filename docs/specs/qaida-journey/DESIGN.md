# Qaida journey

Approved direction: follow the supplied three-screen reference through serif headings,
compact chapter cards, a horizontal lesson path, circular playback, related sound tiles and
a fuller completion checklist. All UI colours come from the existing Nimaz theme: no custom
palette or raw colour values. Reuse the bundled Amiri font and book illustrations in light
and dark modes; Arabic is rendered as text, never baked into artwork. The chapter grouping follows the
real 17 lessons: 1–5 letters and sounds, 6–12 building reading, 13–17 confidence.

[All screens and states in light and dark](PREVIEWS.md) · [Letter anatomy](ARTICULATION.md)

Every Qaida destination uses the unified `NimazBackTopAppBar` with the page name **Qaida**.
The transparent `NimazScreenScaffold` allows the app-wide pattern and frosted top-bar effects
to remain visible, respecting global appearance preferences. The settings action is available
from the journey, reader and letter explorer; no independent theme is introduced.

## Pages and states

| Page | Purpose and behaviour |
| --- | --- |
| Journey | Illustrated welcome, actual daily activity, persisted progress, continue pointer, three expandable chapter cards and a four-lesson window around the current lesson. See all expands the curriculum. Locked lessons explain how to unlock. |
| Review | On-device queue from the learner’s confidence checks; an encouraging empty state. Open a lesson and choose its review action for due cards only. |
| Audio | Download explanation, actual disk use, confirmed removal without resetting learning. |
| Lesson introduction | Existing curriculum description and Arabic title, teacher guidance, begin/resume and due review. Opens only this lesson’s audio download. |
| Listen | Large vowel-highlighted Arabic, optional transliteration, preserved line instructions and notes, verified playback, repeat three times and pitch-preserving slower playback. |
| Repeat | The same focused sound, with the large play button repeating three times; normal playback and slower playback remain available. |
| Practise | Read first, reveal the reminder, then record “Feeling confident” or “Needs practice”. This is explicitly a self-check, never speech recognition or a pronunciation score. |
| All cards | Scrollable lesson overview; selecting a card returns to focused listening. |
| Completion | Existing book illustration, unique heard or self-practised cards this visit, honest activity checklist, encouragement and quick review of those cards (the lesson if none were practised), or return. Browsing alone does not fabricate a practice count. |
| Settings | Existing settings sections and switch rows control persistent transliteration reminders and slower audio. Confirmed audio removal and progress reset are separate. Reset preserves learning preferences and audio; resetting from a reader exits that session. |
| Letter explorer | Existing 29-letter grid, positional forms and a scrollable anatomy sheet with letter-specific tongue, lip, tooth and throat location guides; playback appears only when the lesson-one pack verifies. |

Download states: preparing with per-clip progress, saved/offline, unavailable recordings,
failed/retry. Reading and explicit practice remain available in every state. Back navigation
returns from a lesson subpage to its introduction; leaving the reader stops playback.

## Existing components

Reuse NimazScreenScaffold, NimazBackTopAppBar, NimazCard, NimazButton,
NimazIconButton, NimazSegmentedControl, NimazProgressTrack, NimazConfirmDialog, NimazBottomSheet,
ArabicText, HarakatArabicText, QaidaCourseHeader, QaidaLetterBoard and QaidaLetterDetailSheet.
Extend ArabicTextSize with DISPLAY for the focus card. Keep the shared QaidaCoursePath and
its Khatam consumer unchanged. No new general-purpose design system is introduced.

Two decorative, text-free PNG assets provide the journey and reward illustrations. They
have no accessibility description; the adjacent headings communicate the information.
Controls use standard semantics and labels, and lesson cards announce number, name and state.
Scroll containers accommodate compact screens and larger text. Manual device validation in
light/dark, RTL, TalkBack and 200% text remains a release gate.

## Local learning and remote recordings

Learning progress remains in the existing Room repository; resume and confidence-based review
intervals use private preferences. “Needs practice” returns in ten minutes. Successive
confidence checks schedule one, three, seven and fourteen days. No microphone permission,
recording upload, account or remote learner profile is introduced. Review preferences and
media cache are excluded from Android cloud backup; existing app database backup policy is
unchanged.

Audio is never bundled. Configure `-PqaidaAudioBaseUrl=https://YOUR-ACTUAL-WORKER-ROOT` when
building. With no endpoint, the UI accurately reports unavailable recordings. The base URL is
public and contains no credential. The data repository owns the Worker and upload procedure:
`audio-worker/AGENT_UPLOAD.md` and `scripts/qaida_audio_pack.py`.

The app requests `/lessons/{id}/manifest.json` only for the opened lesson (letter explorer uses
lesson 1), verifies the lesson ID, exact content fingerprint, keys, Arabic text, byte sizes and
SHA-256 hashes, then downloads `/clips/{sha256}.mp3`. Full verified packs commit atomically to
private storage. Incomplete downloads never become playable; old editions retire only after a
successful replacement. Disk limits are 512 KiB per manifest, 2 MiB per clip and 16 MiB per lesson.

Fingerprint v1 is SHA-256 of UTF-8 concatenation, sorted by numeric cell ID:
`cellId + "\n" + audioKey + "\n" + textArabic + "\n"` for every cell.

Recordings must match the content version actually shipped in `data.lock.json`. Merge and
release the corrected nimaz-data artifact through its normal pipeline, then update the app’s
lock through the existing sync task. Never guess release hashes or bypass the pinned artifact.
Teacher-reviewed recordings and a deployed Cloudflare endpoint are still required before
listening can be enabled in a release build.

## Verification

Data PR #21 has eight focused Python checks and seven Worker tests passing locally. GitHub
Actions currently fails before assigning a runner (no steps or logs), including a retry. It
must go green before merge; the UI PR follows that merge, as requested.

Local Android verification passed: 125 Qaida feature tests, 11 domain progress tests and
32 Arabic text component tests (168 total). The feature compiles with Java 21 / Gradle 9.5.1.
Four additional native Robolectric renders were inspected in light/dark themes; the temporary
render harness is not part of the production test suite. These checks cover missing/corrupt/
stale audio, cache recreation and removal, review timing, daily activity, actual heard counts,
explicit practice, and the reader/download/review flows.

The local test JVM required ByteBuddy's agent at startup because dynamic self-attachment was
unavailable in the sandbox. No repository test gates or coverage floors were weakened.
Full repository PR gates, coverage verification, APK/device testing, TalkBack and 200% text
checks remain required before release. The data CI startup failure prevents the requested
merge-then-UI-PR sequence from completing yet.
