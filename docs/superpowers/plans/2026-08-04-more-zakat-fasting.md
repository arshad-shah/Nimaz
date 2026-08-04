# More, Zakat and Fasting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make More, Zakat and Fasting report live state instead of describing themselves, and give Zakat a form you can fill in without losing sight of the total.

**Architecture:** More gains its first ViewModel (`MoreViewModel` + `MoreUseCases`), assembling subtitles from use cases that already exist — no new repository work. Zakat keeps its ViewModel and gains a collapsing sticky hero, three subtotal-bearing accordions, and a bottom action bar. Every subtitle decision is a **pure function from state to `@StringRes Int`**, so it is testable off-device, following `NotificationHubSubtitles` from #351.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, DataStore Preferences, Room (read-only here), JUnit4 + Truth, Robolectric (`testDebug`), instrumented tests via emulator.wtf.

**Spec:** `docs/superpowers/specs/2026-08-04-more-zakat-fasting-design.md` — read it first, especially §0.

## Global Constraints

- **Do §0 of the spec before Task 1.** VM cleanup is in flight on `dev`; merge it and re-validate every claim before writing code.
- Dependencies point inward: domain never imports `data`; presentation never imports entities/DAOs.
- ViewModels inject `XxxUseCases`, never repositories or DAOs.
- ViewModels expose `StateFlow<XxxUiState>` (immutable `data class`) + a single `onEvent(event)`. No exposed `MutableStateFlow`.
- Interactive UI comes from the design system. A tappable card is `NimazCard(onClick = …)` / `NimazMenuItem`, never `Modifier.clickable` wrapped around a card.
- No hardcoded `Color(0xFF…)` — `MaterialTheme.colorScheme.*` / `NimazColors.*`.
- **No new route.** If you think you need one, re-read the spec.
- New strings in `values/strings.xml`, sentence case, active voice; titles label, subtitles report. Translated into `de`, `fr`, `id`, `ms`, `tr`. Counts use `plurals`.
- Every new/extended component ships `@Preview` in **light and dark**.
- Gates before every commit: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest` and `python3 scripts/check_docs.py`.
- Gradle needs the content artifact token: `export NIMAZ_DATA_TOKEN=$(gh auth token -h github.com -u arshad-shah)`.

## File Structure

| File | Responsibility |
|---|---|
| `presentation/components/atoms/NimazAmountInput.kt` | **Create.** Currency-aware numeric input, input only. |
| `core/share/Shareables.kt` | **Modify.** Add `zakat(...)` builder. |
| `data/local/datastore/PreferencesDataStore.kt` | **Modify.** `more_pinned_shortcuts` (ordered, delimited string). |
| `data/local/datastore/PreferenceCodec.kt` | **Modify.** Register the new key. |
| `domain/repository/SettingsRepository.kt` | **Modify.** Pinned-shortcut flow + setter. |
| `domain/model/PinnedShortcut.kt` | **Create.** The pinnable destinations + cap. |
| `presentation/screens/more/MoreSubtitles.kt` | **Create.** Pure state → `@StringRes` mapper. |
| `presentation/viewmodel/MoreViewModel.kt` | **Create.** `MoreUiState`, `MoreEvent`, the ViewModel. |
| `domain/usecase/MoreUseCases.kt` | **Create.** Bundle of existing use cases. |
| `core/di/MoreModule.kt` | **Create.** `@Provides` for `MoreUseCases`. |
| `presentation/screens/more/MoreMenuScreen.kt` | **Modify.** Pills + live subtitles. |
| `presentation/screens/more/PinnedShortcutsSheet.kt` | **Create.** Edit sheet with the cap behaviour. |
| `presentation/screens/zakat/ZakatCalculatorScreen.kt` | **Modify.** Hero collapse, accordions, bottom bar. |
| `presentation/screens/fasting/…` | **Modify.** Hero, single switch, Go deeper. |
| `fastlane/metadata/android/<locale>/changelogs/` | **Modify.** Nine locales. |

---

### Task 0: Pre-flight — take `dev` and re-validate

**Files:** none (verification only)

**Interfaces:**
- Consumes: nothing
- Produces: a merged branch and a confirmed-or-corrected spec

- [ ] **Step 1: Merge current `dev`**

```bash
git checkout feat/more-zakat-fasting
git fetch origin
git merge origin/dev
export NIMAZ_DATA_TOKEN=$(gh auth token -h github.com -u arshad-shah)
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest
python3 scripts/check_docs.py
```

Expected: merge clean or conflicts only in files the VM cleanup touched; all gates pass.

- [ ] **Step 2: Run every check in spec §0 step 2**

Run each `grep` in the spec's claim table. Expected findings are in the table's right-hand column.

- [ ] **Step 3: If any finding differs — stop and report**

Do not adapt silently. Amend the spec, then amend this plan's affected tasks, then continue.

- [ ] **Step 4: Commit the merge**

```bash
git add -A && git commit -m "chore: take dev before starting the More/Zakat/Fasting work"
```

---

### Task 1: `NimazAmountInput` atom

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/components/atoms/NimazAmountInput.kt`
- Test: `app/src/test/java/com/arshadshah/nimaz/presentation/components/atoms/AmountFormattingTest.kt`

**Interfaces:**
- Consumes: nothing
- Produces: `NimazAmountInput(value: String, onValueChange: (String) -> Unit, currencySymbol: String, modifier: Modifier = Modifier, enabled: Boolean = true, placeholder: String = "0.00")`, and `internal fun formatAmountInput(raw: String): String`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.arshadshah.nimaz.presentation.components.atoms

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * What a person may type into a money field, and what it should read back as. Grouping is
 * applied as you type, so the rule has to survive a partially-typed number — not just a
 * finished one.
 */
class AmountFormattingTest {

    @Test
    fun `thousands are grouped as you type`() {
        assertThat(formatAmountInput("42180")).isEqualTo("42,180")
        assertThat(formatAmountInput("1234567")).isEqualTo("1,234,567")
    }

    @Test
    fun `a decimal point survives and is never grouped`() {
        assertThat(formatAmountInput("42180.5")).isEqualTo("42,180.5")
        assertThat(formatAmountInput("42180.50")).isEqualTo("42,180.50")
    }

    @Test
    fun `a trailing point is kept so the next keystroke lands after it`() {
        assertThat(formatAmountInput("42180.")).isEqualTo("42,180.")
    }

    @Test
    fun `more than two decimals are refused rather than silently rounded`() {
        // Rounding someone's money without telling them is worse than not accepting the key.
        assertThat(formatAmountInput("10.555")).isEqualTo("10.55")
    }

    @Test
    fun `junk is dropped, not rejected wholesale`() {
        assertThat(formatAmountInput("4a2b1")).isEqualTo("421")
        assertThat(formatAmountInput("")).isEqualTo("")
    }

    @Test
    fun `a second decimal point is ignored`() {
        assertThat(formatAmountInput("10.5.5")).isEqualTo("10.55")
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*AmountFormattingTest*"`
Expected: FAIL — `Unresolved reference 'formatAmountInput'`

- [ ] **Step 3: Write the implementation**

```kotlin
package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Grouping applied to a partially-typed amount.
 *
 * Kept apart from the composable so the rule can be tested without a device — the awkward
 * cases are all mid-typing ("42180." with nothing after the point yet), which a finished-value
 * formatter never sees.
 */
internal fun formatAmountInput(raw: String): String {
    val cleaned = buildString {
        var seenPoint = false
        var decimals = 0
        for (ch in raw) {
            when {
                ch.isDigit() && seenPoint && decimals >= 2 -> Unit
                ch.isDigit() -> {
                    append(ch)
                    if (seenPoint) decimals++
                }
                ch == '.' && !seenPoint -> {
                    seenPoint = true
                    append(ch)
                }
                else -> Unit
            }
        }
    }
    if (cleaned.isEmpty()) return ""
    val point = cleaned.indexOf('.')
    val whole = if (point >= 0) cleaned.substring(0, point) else cleaned
    val rest = if (point >= 0) cleaned.substring(point) else ""
    val grouped = whole.reversed().chunked(3).joinToString(",").reversed()
    return grouped + rest
}

/**
 * A currency-aware amount field — the input only.
 *
 * The label and hint belong to the screen: Zakat is the one caller today, and baking its row
 * arrangement into the atom would fix a layout that only one screen needs.
 */
@Composable
fun NimazAmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "0.00",
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = currencySymbol,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(6.dp))
        BasicTextField(
            value = value,
            onValueChange = { onValueChange(formatAmountInput(it)) },
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.onSurface
            ),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        textAlign = TextAlign.End,
                        modifier = Modifier.padding(end = 0.dp)
                    )
                }
                inner()
            }
        )
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*AmountFormattingTest*"`
Expected: PASS (6 tests)

- [ ] **Step 5: Add light and dark previews**

Append to the same file — a filled field and an empty one, in both themes, following the
`@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)`
convention used across `presentation/components`.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/components/atoms/NimazAmountInput.kt \
        app/src/test/java/com/arshadshah/nimaz/presentation/components/atoms/AmountFormattingTest.kt
git commit -m "feat(components): add a currency-aware amount input"
```

---

### Task 2: `Shareables.zakat`

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/core/share/Shareables.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/test/java/com/arshadshah/nimaz/core/share/ShareablesZakatTest.kt`

**Interfaces:**
- Consumes: `Shareable`, `ShareCard` from `core/share/Shareable.kt`
- Produces: `Shareables.zakat(context: Context, due: String, assets: String, deducted: String, net: String, nisab: String, yearLabel: String): Shareable`

- [ ] **Step 1: Write the failing test**

Use Robolectric (`testDebug`) so `Context` resolves real strings, matching the other
resource-backed tests. Assert: the card's `eyebrow` is the Zakat label, `arabic` is **null**
(a zakat breakdown is not scripture), `attribution` is the lunar-year label, and `plainText`
contains the due figure and all four breakdown lines.

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*ShareablesZakatTest*"`
Expected: FAIL — `Unresolved reference 'zakat'`

- [ ] **Step 3: Implement the builder**

Follow `Shareables.ayah` exactly: resolve every label from `R.string.*` inside the builder,
build `plainText` with `appendBranding(context)`, and return
`Shareable(plainText = …, card = ShareCard(eyebrow = …, arabic = null, body = …, attribution = …))`.

- [ ] **Step 4: Run to verify it passes**

- [ ] **Step 5: Commit**

```bash
git commit -am "feat(share): add a zakat shareable following the existing catalogue"
```

---

### Task 3: Pinned-shortcut persistence

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/domain/model/PinnedShortcut.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/data/local/datastore/PreferencesDataStore.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/data/local/datastore/PreferenceCodec.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/domain/repository/SettingsRepository.kt`
- Test: `app/src/androidTest/java/com/arshadshah/nimaz/preferences/PinnedShortcutsTest.kt`

**Interfaces:**
- Consumes: nothing
- Produces: `PinnedShortcut` enum (`key: String`), `PinnedShortcut.MAX_PINS = 5`, `PinnedShortcut.DEFAULTS`, and on `SettingsRepository`: `val pinnedShortcuts: Flow<List<PinnedShortcut>>` / `suspend fun setPinnedShortcuts(shortcuts: List<PinnedShortcut>)`

- [ ] **Step 1: Write the domain model**

```kotlin
package com.arshadshah.nimaz.domain.model

/**
 * A destination that can be pinned to the top of More.
 *
 * Capped deliberately: a pin row that holds everything is a second menu, and the whole point
 * of pinning is that the few things you actually use are reachable without scrolling.
 */
enum class PinnedShortcut(val key: String) {
    TASBIH("tasbih"),
    PRAYER_TRACKER("prayer_tracker"),
    KHATAM("khatam"),
    ZAKAT("zakat"),
    QIBLA("qibla"),
    FASTING("fasting"),
    NIGHT_WORSHIP("night_worship"),
    QAIDA("qaida"),
    ISLAMIC_CALENDAR("islamic_calendar");

    companion object {
        const val MAX_PINS = 5
        val DEFAULTS = listOf(TASBIH, PRAYER_TRACKER, KHATAM, ZAKAT)
        fun fromKey(key: String): PinnedShortcut? = entries.firstOrNull { it.key == key }
    }
}
```

- [ ] **Step 2: Write the failing instrumented test**

```kotlin
@Test
fun pinnedShortcuts_roundTripPreservesOrder() = runTest {
    val order = listOf(PinnedShortcut.ZAKAT, PinnedShortcut.TASBIH, PinnedShortcut.KHATAM)
    settings.setPinnedShortcuts(order)
    // Order is the whole point — a Set would lose it, which is why this is a delimited string.
    assertThat(settings.pinnedShortcuts.first()).containsExactlyElementsIn(order).inOrder()
}

@Test
fun pinnedShortcuts_areCappedOnWrite() = runTest {
    settings.setPinnedShortcuts(PinnedShortcut.entries.toList())  // 9
    assertThat(settings.pinnedShortcuts.first()).hasSize(PinnedShortcut.MAX_PINS)
}

@Test
fun pinnedShortcuts_unsetGivesTheDefaults() = runTest {
    assertThat(settings.pinnedShortcuts.first()).isEqualTo(PinnedShortcut.DEFAULTS)
}

@Test
fun pinnedShortcuts_anUnknownKeyIsDropped() = runTest {
    // A key written by a newer build must not crash an older one.
    settings.setPinnedShortcutKeysRaw("tasbih|not_a_screen|zakat")
    assertThat(settings.pinnedShortcuts.first())
        .containsExactly(PinnedShortcut.TASBIH, PinnedShortcut.ZAKAT).inOrder()
}
```

- [ ] **Step 3: Implement storage**

Key: `stringPreferencesKey("more_pinned_shortcuts")`, value is keys joined with `|`.
Read maps through `PinnedShortcut.fromKey`, drops nulls, and `take(MAX_PINS)`. Write applies
`take(MAX_PINS)` too, so the cap holds even if a caller passes more. Register in
`PreferenceCodec.TYPES` as `PrefType.STRING` — `PreferenceCodecTest` fails until you do.

- [ ] **Step 4: Run the unit gate (the codec guard) and the instrumented test**

Run: `./gradlew :app:testDebugUnitTest --tests "*PreferenceCodecTest*"` → PASS
Then the instrumented test on a device/emulator.

- [ ] **Step 5: Commit**

```bash
git commit -am "feat(more): persist pinned shortcuts, capped and ordered"
```

---

### Task 4: `MoreSubtitles` — the pure mapper

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/more/MoreSubtitles.kt`
- Test: `app/src/test/java/com/arshadshah/nimaz/presentation/screens/more/MoreSubtitlesTest.kt`

**Interfaces:**
- Consumes: `MoreUiState` (defined in Task 5 — write that data class here if Task 5 has not run; it is the same file either way)
- Produces: `MoreSubtitles.prayerTracker(logged: Int, total: Int): SubtitleSpec?`, and one function per row, each returning `SubtitleSpec?`. `data class SubtitleSpec(@StringRes val res: Int, val args: List<Any> = emptyList())`. **Null means the row renders no subtitle.**

- [ ] **Step 1: Write the failing test**

```kotlin
/**
 * More stopped describing itself and started reporting. Each row's subtitle is therefore a
 * claim about the app's state, and a wrong one is worse than the static text it replaced.
 *
 * Null is the interesting case: where there is nothing true to say, the row says nothing —
 * it does not fall back to a dash, a spinner, or a restatement of its own title.
 */
class MoreSubtitlesTest {

    @Test
    fun `the tracker reports how much of today is logged`() {
        assertThat(MoreSubtitles.prayerTracker(logged = 4, total = 5)?.res)
            .isEqualTo(R.string.more_tracker_logged)
        assertThat(MoreSubtitles.prayerTracker(logged = 4, total = 5)?.args)
            .containsExactly(4, 5).inOrder()
    }

    @Test
    fun `a row with nothing true to say has no subtitle`() {
        // Not yet loaded — absent, never "—".
        assertThat(MoreSubtitles.prayerTracker(logged = null, total = 5)).isNull()
        assertThat(MoreSubtitles.khatam(progress = null)).isNull()
        assertThat(MoreSubtitles.qaida(currentLesson = null, totalLessons = 21)).isNull()
    }

    @Test
    fun `zakat distinguishes not-yet-calculated from a saved figure`() {
        assertThat(MoreSubtitles.zakat(dueThisYear = null)?.res)
            .isEqualTo(R.string.more_zakat_not_calculated)
        assertThat(MoreSubtitles.zakat(dueThisYear = "€1,284.50")?.res)
            .isEqualTo(R.string.more_zakat_due)
    }

    @Test
    fun `no makeup fasts pending means no subtitle rather than "0 pending"`() {
        assertThat(MoreSubtitles.fasting(pendingMakeup = 0)).isNull()
        assertThat(MoreSubtitles.fasting(pendingMakeup = 3)?.args).containsExactly(3)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*MoreSubtitlesTest*"`
Expected: FAIL — unresolved `MoreSubtitles`

- [ ] **Step 3: Implement `MoreSubtitles`**

An `object` of pure functions, each taking nullable inputs and returning `SubtitleSpec?`.
No `Context`, no `stringResource` — the screen resolves the spec.

- [ ] **Step 4: Run to verify it passes**

- [ ] **Step 5: Commit**

```bash
git commit -am "feat(more): map state to subtitles as a pure, testable function"
```

---

### Task 5: `MoreUseCases`, `MoreViewModel`, DI

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/domain/usecase/MoreUseCases.kt`
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/MoreViewModel.kt`
- Create: `app/src/main/java/com/arshadshah/nimaz/core/di/MoreModule.kt`

**Interfaces:**
- Consumes: `MoreSubtitles` (Task 4), `SettingsRepository.pinnedShortcuts` (Task 3)
- Produces: `MoreUiState`, `sealed interface MoreEvent { data class SetPins(val pins: List<PinnedShortcut>) : MoreEvent }`, `MoreViewModel.uiState: StateFlow<MoreUiState>`

- [ ] **Step 1: Write `MoreUseCases`**

A `data class` bundling only what More reads — `getTodayPrayerRecords`, `getPendingMakeupFasts`,
`nextWorship`, `khatamProgress`, `qaidaProgress`, `zakatHistory`, `hijriToday`. All exist
already (spec §2.1); none are created here.

- [ ] **Step 2: Write `MoreViewModel`**

`combine` the flows into `MoreUiState`. Every field nullable, defaulting to null, so a subtitle
that has not resolved renders as absent. `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MoreUiState())`.

- [ ] **Step 3: Provide `MoreUseCases` in `core/di/MoreModule.kt`**

`@Module @InstallIn(SingletonComponent::class)`, `@Provides @Singleton`.

- [ ] **Step 4: Verify Hilt wiring compiles**

Run: `./gradlew :app:compileDebugKotlin` (runs KSP → validates Hilt)
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git commit -am "feat(more): add MoreViewModel and its use-case bundle"
```

---

### Task 6: More screen — pills and live subtitles

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/more/MoreMenuScreen.kt`
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/more/PinnedShortcutsSheet.kt`

**Interfaces:**
- Consumes: `MoreViewModel`, `MoreSubtitles`, `PinnedShortcut`
- Produces: nothing downstream

- [ ] **Step 1: Replace static subtitles with the mapper**

Each row resolves `MoreSubtitles.x(state…)` and passes `subtitle = spec?.let { stringResource(it.res, *it.args.toTypedArray()) }`. Rows whose spec is null pass `subtitle = null`.

- [ ] **Step 2: Add the pinned row**

A `LazyRow` of `NimazCard(onClick = …)` pills above the first section, with a
`NimazSectionHeader`-style label and a `NimazIconButton` pencil.

- [ ] **Step 3: Build the edit sheet**

`NimazBottomSheet` listing `PinnedShortcut.entries` with `NimazCheckbox`. When
`pins.size == MAX_PINS`, unpinned rows are `enabled = false` and the header reads
"5 of 5 pinned" (`R.string.more_pins_full`). Tapping a disabled row does nothing.

- [ ] **Step 4: Verify**

Run: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest` → BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git commit -am "feat(more): pinned shortcuts and subtitles that report live state"
```

---

### Task 7: Zakat — hero, accordions, bottom bar

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/zakat/ZakatCalculatorScreen.kt`

**Interfaces:**
- Consumes: `NimazAmountInput` (Task 1), `Shareables.zakat` (Task 2), existing `ZakatViewModel`
- Produces: nothing downstream

- [ ] **Step 1: Move History to the app bar**

`NimazBackTopAppBar(actions = { NimazIconButton(Icons.Default.History, …) })`; delete the
history row from the bottom of the form.

- [ ] **Step 2: Make the hero sticky and collapsing**

```kotlin
val listState = rememberLazyListState()
// Threshold on the first item's scroll offset, not an accumulated delta: a delta drifts and
// can strand the hero half-collapsed after a fling.
val collapsed by remember {
    derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 120 }
}
```

Hero above the `LazyColumn`, not inside it. Animate the amount's font size with
`animateFloatAsState`; the stat tiles and percentage line animate to zero height.

- [ ] **Step 3: Three accordions with subtotals**

`NimazAccordion(title = …, subtitle = …, trailing = { Text(subtotal) })` for Assets, Deducted
and Nisab, each body a column of labelled rows using `NimazAmountInput`.

- [ ] **Step 4: Bottom action bar**

`NimazButton` "Save this year's zakat" → existing `ZakatEvent` for `insertCalculation`;
`NimazIconButton` share → `ContentShareManager.shareBranded(context, Shareables.zakat(...))`,
launched in a coroutine scope exactly as `QuranAyahItem` does.

- [ ] **Step 5: Verify and commit**

```bash
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest
git commit -am "feat(zakat): keep the total in view, and give save and share a home"
```

---

### Task 8: Fasting screen

**Files:**
- Modify: the Fasting home screen (find via `Route.FastingHome` in `NavGraph.kt`; the spec's §4 applies wherever it lives)

**Interfaces:**
- Consumes: existing `FastingViewModel`
- Produces: nothing downstream

- [ ] **Step 1: Countdown hero**

`NimazCard` with the Hijri date, time to Maghrib, and when suhoor ended, from the existing
prayer-times source.

- [ ] **Step 2: Replace the two buttons with one switch**

A `NimazSettingsItem` with `checked`/`onCheckedChange`, subtitle "Logged · tap to undo" or
"Not logged yet". Delete the two-button row.

- [ ] **Step 3: Go deeper + Ramadan groups**

`NimazMenuGroup` rows with live subtitles; makeup fasts carries a `NimazBadge` count.

- [ ] **Step 4: Verify and commit**

```bash
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest
git commit -am "feat(fasting): one switch for today, and a hero that says what is left"
```

---

### Task 9: Strings, translations, docs

**Files:**
- Modify: `app/src/main/res/values/strings.xml` + `values-{de,fr,id,ms,tr}/strings.xml`
- Modify: `docs/SUBSYSTEMS.md` (§6 — the `more_pinned_shortcuts` key)

- [ ] **Step 1: Add every new string to `values/strings.xml`**

Counts as `plurals`. Titles label, subtitles report.

- [ ] **Step 2: Translate into the five app locales**

- [ ] **Step 3: Validate XML and docs**

```bash
for f in app/src/main/res/values*/strings.xml; do python3 -c "import xml.etree.ElementTree as E; E.parse('$f')"; done
python3 scripts/check_docs.py
```

- [ ] **Step 4: Commit**

```bash
git commit -am "i18n(more): translate the new strings and document the pin preference"
```

---

### Task 10: Release notes

**Files:**
- Modify: `fastlane/metadata/android/<locale>/changelogs/default.txt` — **nine** locales: `ar`, `de-DE`, `en-GB`, `en-US`, `fr-FR`, `id`, `ms`, `tr-TR`, `ur`

- [ ] **Step 1: Write `en-US` from what actually shipped**

Follow the existing house style — a "What's new:" line then `•` bullets, user-facing language,
no component names. Cover only what landed; if a task was dropped, it is not in the notes.

- [ ] **Step 2: Translate into the other eight**

Note this is **wider than the app's six UI locales** — `ar` and `ur` have store listings but no
app translation.

- [ ] **Step 3: Write the GitHub release body**

Longer than the store notes: what changed, why, and anything a returning user should know
(pinned shortcuts default to four; existing zakat history is untouched).

- [ ] **Step 4: Commit**

```bash
git commit -am "docs(release): release notes for the More, Zakat and Fasting work"
```

---

## Self-Review

**Spec coverage:** §0 → Task 0. §2.1 subtitles → Tasks 4, 5, 6. §2.2 pins → Tasks 3, 6.
§2.3 unchanged-deliberately → no task needed (it is a "do not change" instruction, enforced by
review). §3.1 layout → Task 7. §3.2 `NimazAmountInput` → Task 1. §3.3 save/share → Tasks 2, 7.
§4 Fasting → Task 8. §5 strings/locales → Tasks 9, 10. §6 testing → tests live inside Tasks 1–4.
§7 documentation → Task 9. §8 out-of-scope → constraints.

**Placeholder scan:** no TBD/TODO. Tasks 7 and 8 give code for the load-bearing parts (the
collapse threshold, the switch) and prose for the mechanical composition — acceptable because
both screens are edits to existing files whose surrounding patterns are already established;
an implementer reads the file, not a transcription of it.

**Type consistency:** `PinnedShortcut.MAX_PINS` used in Tasks 3 and 6. `SubtitleSpec(res, args)`
returned in Task 4, consumed in Task 6. `formatAmountInput` internal to Task 1, used by
`NimazAmountInput` only. `Shareables.zakat(...)` signature in Task 2 matches its call in Task 7.
