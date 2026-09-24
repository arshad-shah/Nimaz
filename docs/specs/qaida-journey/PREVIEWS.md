# Qaida screen previews

**Current review:** [interactive browser review](qaida-review.html), built from the current artwork
and six-locale string resources. It is a reconstruction, not a native Android capture.

**Historical screenshots below:** these predate the latest raster artwork, banners and navigation.
The current native suite contains 358 cases (25 flows × 6 locales × 2 themes, plus 29 letter
details × 2 themes). It has not run successfully in this environment: Gradle cannot resolve
`org.gradle.kotlin.kotlin-dsl:6.5.7`. Do not treat the old images or results as current validation.


[Mouth anatomy: all 29 letters, light and dark](ARTICULATION.md)

Every Qaida destination and its important states, rendered from the actual Compose UI at
411 × 900 dp in both Nimaz themes. The unified **Qaida** top bar, frosted pills and app-wide
pattern are included. All colours and effects come from the existing app theme.

These are native Robolectric captures, not generated mockups. Sample progress and audio
availability are simulated; no recordings ship with this branch. The lesson examples use
three short vowels in lesson 4. The letter explorer fixture uses the repository alphabet
catalogue. Completion checks reflect the simulated actions, not invented listening credit.

Settings are available from the journey, reader and letter explorer. Transliteration and
slower playback persist on this device. Clearing audio keeps learning history; resetting
progress keeps learning preferences and downloaded audio. Both destructive actions ask for
confirmation. App-wide appearance and effects continue to follow Nimaz settings.

Screens can scroll: captures show one phone viewport, including the relevant state or dialog.
The expanded chapter capture scrolls to the lesson list; audio-state captures scroll to the
status panel. Device, TalkBack, RTL and large-text testing remain release validation tasks.

## Journey and navigation

| Screen | Light | Dark |
| --- | --- | --- |
| Journey | ![Journey, light](previews/journey.png) | ![Journey, dark](previews/journey-dark.png) |
| Expanded chapter | ![Expanded chapter, light](previews/chapters.png) | ![Expanded chapter, dark](previews/chapters-dark.png) |
| Review queue | ![Review queue, light](previews/review.png) | ![Review queue, dark](previews/review-dark.png) |
| Review — caught up | ![Review — caught up, light](previews/review-empty.png) | ![Review — caught up, dark](previews/review-empty-dark.png) |
| Downloaded audio | ![Downloaded audio, light](previews/audio.png) | ![Downloaded audio, dark](previews/audio-dark.png) |
| Audio — no downloads | ![Audio — no downloads, light](previews/audio-empty.png) | ![Audio — no downloads, dark](previews/audio-empty-dark.png) |

## Learning

| Screen | Light | Dark |
| --- | --- | --- |
| Lesson introduction | ![Lesson introduction, light](previews/intro.png) | ![Lesson introduction, dark](previews/intro-dark.png) |
| Listen | ![Listen, light](previews/focus.png) | ![Listen, dark](previews/focus-dark.png) |
| Repeat | ![Repeat, light](previews/repeat.png) | ![Repeat, dark](previews/repeat-dark.png) |
| Practise — read first | ![Practise — read first, light](previews/practise.png) | ![Practise — read first, dark](previews/practise-dark.png) |
| Practise — self-check | ![Practise — self-check, light](previews/self-check.png) | ![Practise — self-check, dark](previews/self-check-dark.png) |
| All lesson cards | ![All lesson cards, light](previews/all-cards.png) | ![All lesson cards, dark](previews/all-cards-dark.png) |
| Due-card review | ![Due-card review, light](previews/due-review.png) | ![Due-card review, dark](previews/due-review-dark.png) |
| Session completion | ![Session completion, light](previews/reward.png) | ![Session completion, dark](previews/reward-dark.png) |

## Letters and settings

| Screen | Light | Dark |
| --- | --- | --- |
| Letter explorer | ![Letter explorer, light](previews/letters.png) | ![Letter explorer, dark](previews/letters-dark.png) |
| Letter detail sheet | ![Letter detail sheet, light](previews/letter-detail.png) | ![Letter detail sheet, dark](previews/letter-detail-dark.png) |
| Qaida settings | ![Qaida settings, light](previews/settings.png) | ![Qaida settings, dark](previews/settings-dark.png) |
| Remove audio confirmation | ![Remove audio confirmation, light](previews/clear-audio.png) | ![Remove audio confirmation, dark](previews/clear-audio-dark.png) |
| Reset progress confirmation | ![Reset progress confirmation, light](previews/reset.png) | ![Reset progress confirmation, dark](previews/reset-dark.png) |

## Loading and unavailable content

| Screen | Light | Dark |
| --- | --- | --- |
| Loading lesson | ![Loading lesson, light](previews/loading.png) | ![Loading lesson, dark](previews/loading-dark.png) |
| Empty lesson | ![Empty lesson, light](previews/lesson-empty.png) | ![Empty lesson, dark](previews/lesson-empty-dark.png) |
| Audio download progress | ![Audio download progress, light](previews/downloading.png) | ![Audio download progress, dark](previews/downloading-dark.png) |
| Recordings unavailable | ![Recordings unavailable, light](previews/audio-unavailable.png) | ![Recordings unavailable, dark](previews/audio-unavailable-dark.png) |
| Audio download failed | ![Audio download failed, light](previews/audio-error.png) | ![Audio download failed, dark](previews/audio-error-dark.png) |
| Playback failed | ![Playback failed, light](previews/playback-error.png) | ![Playback failed, dark](previews/playback-error-dark.png) |

Historical validation reported 177 behavioural/component tests and 108 native captures.
Current browser review has passed JavaScript syntax checks, but visual browser verification
was blocked by the cloud browser file-URL security policy. It remains a review aid, not a
validated native screenshot.

Current changes: resource compilation with AAPT2, six-locale key/placeholder parity and raster
file integrity pass. Kotlin compilation and the expanded native suite remain unverified.

## Regenerating

Run the native capture suite with the project Android SDK and JDK configured:

```sh
./gradlew :feature:content:testDebugUnitTest --tests '*QaidaVisualCheckTest' --rerun-tasks
```

It writes PNGs to `feature/content/build/qaida-previews`. Set `QAIDA_PREVIEW_DIR` to override
the output directory. Inspect all captures before copying them into this document’s
`previews/` directory. The suite navigates the actual screens, including dialogs, and uses
`NimazPatternBackground` exactly as the app root does.
