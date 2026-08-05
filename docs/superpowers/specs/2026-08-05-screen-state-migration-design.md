# Screen states — loading, empty and error, produced and rendered the same way everywhere

Date: 2026-08-05
Status: approved, in implementation

Every screen in Nimaz can be loading, empty, failed, or populated. Three of those four are
currently improvised per screen, and one of them — failure — is frequently not rendered at
all. This spec defines one contract for all three, migrates every screen onto the three
design-system components, and closes the gap between a failure being *recorded* and a
failure being *seen*.

---

## 0. Where the problem actually is

The audit behind this spec found three distinct defects, not one inconsistency.

**A failure is often invisible.** Sixteen ViewModels set a non-null `error` on their state.
For half of them, no screen ever reads it. `HadithViewModel` sets four; none of
`HadithCollectionScreen`, `HadithChaptersScreen` or `HadithReaderScreen` contains the
substring `error`. The same holds for Help (3), Bookmarks (3), Tafseer, Search's local path,
Home and Zakat. The load fails, the spinner stops, the screen stays blank, and the user is
left to guess.

**A failure is sometimes rendered as an absence.** `SurahSubjectsScreen`,
`SurahPassagesScreen` and `SurahBackgroundScreen` evaluate `isEmpty()` before they consider
`error`, so a failed load reaches `NimazEmptyState` and reports "there is nothing here" for
content that exists and could not be fetched. That is worse than silence: it is wrong.

**A failure is never recorded in the first place.** The `launchSafely` seam added by #352
takes an `onFailure` callback precisely so a ViewModel can clear `isLoading` and set an
error. Of 47 call sites, **23 pass one**. The other 24 report to telemetry and return, so
the state they abandoned still says `isLoading = true`. No amount of rendering fixes those;
the error must be produced before it can be shown.

Underneath all three sits the absence of a rule. Loading is a bare
`Box(fillMaxSize, Center) { CircularProgressIndicator() }` — 25 call sites across 19
screens — an error is a red `Text` in nine, and `NimazEmptyState` is used for both
"nothing yet" and "it broke".
`docs/ARCHITECTURE.md` §8 says to reuse `NimazLoadingState`; it does not say when each of
the three applies, so nothing has held the line.

---

## 1. The three components

| Component | Meaning | Location |
|---|---|---|
| `NimazLoadingState` | the first load; there is nothing to show yet | `components/atoms` |
| `NimazEmptyState` | the load succeeded and there is genuinely nothing | `components/molecules` |
| `NimazErrorState` | the load failed | `components/atoms` |

`NimazErrorState` is new (this branch). It mirrors `NimazLoadingState`'s
`FULLSCREEN`/`SECTION`/`INLINE` variants so a screen can swap loading → error without
changing its layout, carries a `NimazErrorKind` that fixes the glyph and tone per failure
type, hides technical detail behind a "Show details" toggle, and announces itself as a
polite live region.

`NimazSkeleton` remains preferred over `NimazLoadingState` wherever the shape of the
incoming content is known; this spec does not change that guidance and does not convert
existing skeletons.

## 2. The rule

Every screen body evaluates the four states in this fixed order:

```kotlin
when {
    state.isLoading && state.items.isEmpty() -> NimazLoadingState(Modifier.padding(padding))
    state.error != null                      -> NimazErrorState(…, Modifier.padding(padding))
    state.items.isEmpty()                    -> NimazEmptyState(…)
    else                                     -> Content(…)
}
```

Three properties follow from the order, and each fixes a real defect:

1. **Error beats empty.** A failed load can never be reported as an absence.
2. **Loading only wins when the screen is bare.** A refresh over existing content does not
   blank it out.
3. **A failure over populated content is not full-screen.** It is `SECTION` or `INLINE`
   (or a `NimazBanner` where the message is advisory), because destroying good content to
   report that newer content failed to arrive is a worse outcome than the failure.

Variant follows scale: the whole screen body is `FULLSCREEN`, one section of an otherwise
populated screen is `SECTION`, a row or a form field is `INLINE`.

**Padding.** State components sit inside `NimazScreenScaffold`'s content lambda and must
receive its `paddingValues`. `NimazLoadingState` and `NimazErrorState` both fill the
available space and centre, so omitting it centres them against the window rather than the
content area, tucking them under the top bar. Two call sites already carry this bug on this
branch.

**What is out of scope.** Form-field validation stays on `TextField(isError, supportingText)`
— `KhatamFormScreen`, `AddPresetScreen`, and the Qur'an jump-to-ayah field are correct as
they are. Transient, non-blocking failures may remain snackbars. Glance widgets cannot use
Compose UI components and are untouched.

## 3. The state contract

```kotlin
// presentation/viewmodel/UiError.kt
data class UiError(
    @StringRes val message: Int,
    val kind: NimazErrorKind = NimazErrorKind.GENERIC,
    val details: String? = null,
)
```

- **User-facing copy is always a `@StringRes`.** Today `LocationViewModel` puts
  `"Failed to search locations: ${e.message}"` — English, untranslated, exception-shaped —
  directly on screen. The exception's text belongs in `details`, which `NimazErrorState`
  hides behind the toggle.
- **The ViewModel chooses the `kind`**, because it is the layer that knows whether the call
  was a network fetch, a missing row, or a denied permission.
- **Recovery is an event**: `XxxEvent.Retry`, `XxxEvent.DismissError`. Screens do not close
  over repositories or use cases in a retry lambda.
- A `UiState` that can fail carries `isLoading: Boolean` and `error: UiError?`. A `UiState`
  that cannot fail carries neither.

## 4. Producing the error

Every user-visible load path passes `onFailure`:

```kotlin
launchSafely(
    telemetry, "hadith", "load_chapter",
    onFailure = { _state.update { it.copy(isLoading = false, error = UiError(R.string.hadith_load_failed, NimazErrorKind.GENERIC, it.message)) } },
) { … }
```

and every collected flow that a screen depends on passes a `fallback` to `catchAndReport`.
The seam already exists and its KDoc already prescribes this shape — the work is to apply
it at the 24 sites that pass nothing.

**Vestigial fields.** `TasbihUiState.error`, `PrayerTrackerUiState.error` and
`FastingUiState.error` are never assigned by any ViewModel; `QuranUiState.error` is only
ever assigned `null`. Each is resolved one of two ways, never left as it is: if the screen
has a real failure path, that path gains a producer; if it does not, the field is deleted.

## 5. The About / Licenses slice

`AboutScreen`, `LicensesScreen` and `LicenseDetailScreen` have no ViewModel — not by
decision, but because they predate the pattern. `LicensesScreen.kt:57` and
`LicenseDetailScreen.kt:58` call `Libs.Builder().withContext(context).build()` — an
asset-backed data source — from a `LaunchedEffect`, and hold the result in
`remember { mutableStateOf }`. That breaks non-negotiable rules 1, 2 and 3 simultaneously.
`AboutScreen` additionally reads `LocalInAppUpdateManager`'s `UpdateState` and builds its
click behaviour as an inline `when` over update states, which is business logic in a
composable.

The slice, following the §10 recipe:

- `domain/model/OpenSourceLibrary.kt` — name, version, licence name, licence url, website,
  and the stable id the detail route already keys on.
- `domain/repository/LibraryRepository` — `suspend fun getLibraries(): List<OpenSourceLibrary>`,
  `suspend fun getLibrary(id: Int): OpenSourceLibrary?`.
- `data/repository/LibraryRepositoryImpl` — `@ApplicationContext`, builds `Libs` on
  `Dispatchers.IO`, maps to the domain model. The `aboutlibraries` types stop at this file.
- `domain/usecase/licenses/` — `GetLibrariesUseCase`, `GetLibraryUseCase`, bundled as
  `LicensesUseCases`.
- `core/di/LicensesModule` — `@Binds` for the repository, `@Provides` for the use-case
  bundle, `@Singleton` in `SingletonComponent`.
- `LicensesViewModel` — two `StateFlow`s (list and detail), which is the house pattern for
  a list/detail pair (`AsmaUlHusnaViewModel`), plus `onEvent` carrying `Retry`.
- `AboutViewModel` — app metadata and the update-state mapping, `UiState` + `onEvent`.

`LocalInAppUpdateManager` stays a CompositionLocal: the Play update flow needs an Activity,
and routing it through a ViewModel would put an Activity reference in one. The `when` moves
into a pure mapper the ViewModel owns, and the CompositionLocal is registered as an
**accepted pattern** in `docs/ARCHITECTURE.md` §9 so the next audit does not re-litigate it.

Navigation is unchanged: the routes, their `ScreenTags` and their `taggedComposable`
wiring already exist.

## 6. Delivery — one stack, six layers

Integration branch `epic/screen-states`, cut from `origin/dev`. Layers ship as GitHub
stacked PRs (`gh stack add` / `gh stack submit`), each compiling green and each updating the
docs it invalidates. Writes go through the `arshad-shah` account; commits carry no
`Co-Authored-By` trailer.

| Layer | Branch | Contents |
|---|---|---|
| 1 | `epic/ss-01-contract` | `NimazErrorState`, `UiError`, the §2 rule in `ARCHITECTURE.md` §8, `ScreenStateConventionTest` with its accepted backlog seeded from today's state |
| 2 | `epic/ss-02-about-licenses` | the §5 slice; the three screens rebuilt on it |
| 3 | `epic/ss-03-silent-failures` | Hadith ×3, Help ×3, Bookmarks, Tafseer, SurahThematic ×3, Search, Home, Zakat — produce **and** render |
| 4 | `epic/ss-04-handrolled-errors` | Sync, Qibla, Dua ×4, Calendar, NightWorship |
| 5 | `epic/ss-05-loading-empty` | the 25 raw `CircularProgressIndicator` sites across 19 screens; empty-state audit |
| 6 | `epic/ss-06-vestigial` | vestigial fields resolved, backlog emptied, checklist ticked |

`epic/vm-41` and `epic/vm-42` are in flight against Settings and user-data; the overlap with
these files is near zero. Layer 3 touches `BookmarksViewModel` and `TafseerViewModel`, which
that stack does not.

## 7. Verification

**`ScreenStateConventionTest`** — a source-scanning unit test modelled on the existing
`AnalyticsReachabilityTest`, which established the pattern of asking a question no ordinary
test can. It fails on:

1. a `CircularProgressIndicator(` under `presentation/screens/` outside a `@Preview`.
   **Determinate progress bars are not loading states** and are not flagged: the six
   `LinearProgressIndicator(progress = …)` sites (widget pin progress, sync progress, the
   fasting day bar, the Ask thinking bar) report how far along a known-length operation is,
   which is not something `NimazLoadingState` expresses;
2. a `UiState` declaring `error` that no screen in its feature package reads;
3. a `launchSafely(` with no `onFailure` argument.

Each check carries an accepted-backlog set, seeded in layer 1 with exactly today's
violations and emptied as layers 2–6 land. When the last entry goes, the test is a pure
ratchet and the next regression fails its own PR.

**Per ViewModel** — a unit test with a fake use case that throws, asserting the state ends
with `isLoading = false` and a non-null `error`; and one asserting `Retry` clears the error
and re-issues the call.

**Per layer** — `./gradlew :app:compileDebugKotlin`, `./gradlew :app:testDebugUnitTest`,
`python3 scripts/check_docs.py`.

**Visual** — the migrated screens walked in light and dark before the epic merges, since
none of the three components has had on-device verification (`ARCHITECTURE.md` §9, open
item 8 makes the same point about the tone migration).

## 8. Documentation obligations

- `docs/ARCHITECTURE.md` §8 — the §2 rule, as a numbered sub-bullet beside the existing
  `NimazLoadingState` bullet.
- `docs/ARCHITECTURE.md` §9 — `LocalInAppUpdateManager` as an accepted pattern; tick any
  open item this resolves.
- `docs/CLEAN_ARCHITECTURE_CHECKLIST.md` — a new entry for the state contract, with the
  detection commands the convention test uses; tick it as the backlog empties.
- No `NAVIGATION.md` or `SUBSYSTEMS.md` obligation: no route, service, worker, widget,
  channel, DataStore file or schema version changes.

## 9. What this spec does not do

It does not redesign any screen's content, convert existing `NimazSkeleton` usage, touch
the Glance widgets, change navigation, or consolidate the multiple `StateFlow`s that
list/detail ViewModels expose by design. It does not convert advisory `NimazBanner` usage
into error states, and it does not turn snackbars into blocking states where the screen
remains usable without the failed data.
