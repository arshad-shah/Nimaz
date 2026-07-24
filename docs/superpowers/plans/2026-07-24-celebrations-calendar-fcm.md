# Celebrations, Calendar & FCM — Implementation Plan (Plan 3 of 3)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the event cards live — a Hijri day-offset preference (app + widgets), a `celebration` FCM announcement type, a local calendar event source, and a merge that feeds real occasion cards (local + pushed) into the Home carousel.

**Architecture:** Add `hijriDayOffset` to `SettingsRepository`/`PreferencesDataStore` and an `offsetDays` param to `HijriDateCalculator.today()`; surface it in settings and thread it into both Hijri widgets. Add `AnnouncementType.CELEBRATION`, a `CelebrationEvent` domain enum, and new nullable `Announcement` fields (mirrored in the persistence entity + mapper + `PAYLOAD_KEYS`). A domain `HomeEventCard` model flows from a merge use case (`ObserveEventCardsUseCase`) that combines local calendar matches (`IslamicEvents.events` vs `today(offset)`) with pushed celebrations; the ViewModel maps `HomeEventCard`→`EventCardUi` (attaching callbacks) and HomeScreen renders them in the existing `EventsCarousel`.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, DataStore (Preferences), Glance/WorkManager widgets, kotlinx.serialization, plain JUnit4 + Truth (+ Robolectric only where a test needs Android).

## Global Constraints

- Package root `com.arshadshah.nimaz`. Do not push to `dev`. Branch `feat/event-cards-celebration-routing`.
- **Domain purity:** domain never imports `data` (entities/DAOs/DataStore) or presentation. The new domain `HomeEventCard` and `CelebrationEvent` carry no Compose/`EventOccasion`/lambda types. `CelebrationEvent`→`EventOccasion` mapping lives in **presentation**.
- **`PAYLOAD_KEYS` is load-bearing:** every new payload key MUST be added to the `KEY_*` consts AND the `PAYLOAD_KEYS` list AND parsed in `fromPayload` AND mirrored in the persistence `AnnouncementEntity` (`toDomain`/`toEntity`) — miss any and the field is silently dropped (on tray-tap, or on persistence).
- **Never push prayer times / countdowns / Hijri dates** in a payload (they derive from the user's own location/method). `title`/`body` stay required (tray fallback).
- Mapper keeps its null-on-malformed discipline: a present-but-malformed field rejects the whole payload (`?: return null`), except the proof pair which degrades locally (drop both if only one present).
- `CelebrationEvent.fromKey` degrades unknowns to `GENERIC` (never null). `event` is only read when `type == CELEBRATION`.
- Hijri offset range **−2..+2**, default **0**; app and both widgets must use the same preference.
- No hardcoded `Color(0xFF…)`. Reuse `NimazNumberStepper`, `EventsCarousel`, `EventCard`, `eventCardVisualsFor` — no hand-rolled equivalents.
- Verify: `./gradlew :app:compileDebugKotlin` and `./gradlew :app:testDebugUnitTest`. Compile runs KSP → validates Hilt + Room wiring.
- **Prune dismissed ids (spec step 10) is DEFERRED** (documented follow-up) — dismissed ids are a bare `Set<String>` with no expiry; pruning needs a storage change, out of scope here.

## Grounded facts (verified on this branch)

- `Announcement` (`domain/model/Announcement.kt`): `id, type, title, body, ctaLabel?, route?, minVersionCode?, maxVersionCode?, expiresAtMillis?, dismissable=true`. `isActiveFor(versionCode, nowMillis)` gates on expiry + version window. `AnnouncementType{FEATURE,PRIVACY,TOS,CHANGELOG}` + `fromKey` (trims/lowercases).
- Mapper (`data/announcement/AnnouncementPayloadMapper.kt`): entry `fromPayload(data: Map<String,String>)` + `fromIntentExtras(Bundle?)` (iterates `PAYLOAD_KEYS`). `KEY_*` consts + `PAYLOAD_KEYS` list.
- Persistence (`data/local/datastore/AnnouncementLocalDataSource.kt`): `@Serializable internal data class AnnouncementEntity` mirrors the domain model field-for-field with `toDomain()`/`toEntity()`; `Json { ignoreUnknownKeys = true }`.
- `IslamicEvents.events` (`domain/model/IslamicCalendarModels.kt`): 14 events, ids `islamic_new_year, ashura, mawlid, isra_miraj, shab_e_barat, ramadan_start, laylat_al_qadr_21/23/25/27/29, eid_al_fitr, day_of_arafah, eid_al_adha`, each with `hijriMonth, hijriDay, priority`. `IslamicEvent` field order ends `…, notes, priority`. `IslamicEventType{HOLIDAY,FAST,NIGHT,HISTORICAL}`.
- `HijriDateCalculator` (`core/util/HijriDateCalculator.kt`) is an `object`; `today(): HijriDate = toHijri(LocalDate.now())` (no offset param today). Nested `HijriDate(day,month,year)`.
- `SettingsRepository` (`domain/repository/SettingsRepository.kt`) impl `PreferencesDataStore` (`nimaz_preferences`): pref pattern `preference(key, default): Flow<T>` + `put(key,value)`; e.g. `notificationReminderMinutes: Flow<Int>` / `setNotificationReminderMinutes`.
- `NimazNumberStepper(value, onValueChange, …, label, minValue, maxValue, step, variant=INLINE)` — clamps internally; INLINE default shows `+value`. In `presentation/components/molecules/NimazNumberStepper.kt`.
- Hijri offset UI belongs in `AppearanceSettingsScreen.kt` (holds the Hijri toggle) + `SettingsViewModel` (`GeneralSettingsUiState`, `SettingsEvent`, optimistic-update-then-persist handler, load via `settingsRepository.x.first()`).
- Hijri widgets: `widget/hijridate/HijriDateWorker.kt` + `widget/hijricalendar/HijriCalendarWorker.kt`, both `@HiltWorker @AssistedInject CoroutineWorker` calling `HijriDateCalculator.today()` directly, no prefs injected.
- Home cards: no `events` field in `HomeUiState`; the Jumu'ah `EventCardUi` is built inline in `HomeScreen.kt` (`buildList` compact ~L307, `listOf` tablet ~L444) from `state.isFriday/jumuahTime/timeUntilJumuah/isJumuahPassed`. `EventsCarousel(events: List<EventCardUi>)`. `EventCardUi(occasion, eyebrow, headline, body, arabic?, transliteration?, proof?, primaryAction?, secondaryAction?, onDismiss?, jumuahTime, timeUntilJumuah, isJumuahPassed)`. `EventOccasion{EID_AL_FITR,EID_AL_ADHA,RAMADAN,LAYLAT_AL_QADR,ARAFAH,ASHURA,MAWLID,HIJRI_NEW_YEAR,JUMUAH,GENERIC}`. `EventAction(label, onClick)`.
- Home announcement flow (`HomeViewModel.kt`): `observeActiveAnnouncement()`; `AnnouncementUiState(announcement?, showCta)`; `dismissAnnouncement()` → `dismissAnnouncement(active.id)`.

---

## Task 1: Hijri day-offset preference + `HijriDateCalculator.today(offsetDays)`

**Files:**
- Modify: `domain/repository/SettingsRepository.kt`
- Modify: `data/local/datastore/PreferencesDataStore.kt`
- Modify: `core/util/HijriDateCalculator.kt`
- Test: `app/src/test/java/com/arshadshah/nimaz/core/util/HijriDateCalculatorOffsetTest.kt`

**Interfaces:**
- Produces: `SettingsRepository.hijriDayOffset: Flow<Int>` + `suspend fun setHijriDayOffset(days: Int)`; `HijriDateCalculator.today(offsetDays: Int = 0): HijriDate`.

- [ ] **Step 1: Failing test for the calculator offset**

Create `app/src/test/java/com/arshadshah/nimaz/core/util/HijriDateCalculatorOffsetTest.kt`:

```kotlin
package com.arshadshah.nimaz.core.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class HijriDateCalculatorOffsetTest {

    @Test
    fun `today with zero offset equals today of current date`() {
        val zero = HijriDateCalculator.today(0)
        val fromDate = HijriDateCalculator.toHijri(LocalDate.now())
        assertThat(zero).isEqualTo(fromDate)
    }

    @Test
    fun `positive offset advances the hijri day relative to zero`() {
        val zero = HijriDateCalculator.today(0)
        val plusOne = HijriDateCalculator.today(1)
        // +1 day is either the next day in the same month, or day 1 of the next month
        assertThat(plusOne).isNotEqualTo(zero)
        assertThat(plusOne).isEqualTo(HijriDateCalculator.toHijri(LocalDate.now().plusDays(1)))
    }

    @Test
    fun `negative offset matches yesterday`() {
        assertThat(HijriDateCalculator.today(-1))
            .isEqualTo(HijriDateCalculator.toHijri(LocalDate.now().minusDays(1)))
    }

    @Test
    fun `default arg is zero offset`() {
        assertThat(HijriDateCalculator.today()).isEqualTo(HijriDateCalculator.today(0))
    }
}
```

- [ ] **Step 2: Run → fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.core.util.HijriDateCalculatorOffsetTest"`
Expected: FAIL — `today(Int)` unresolved.

- [ ] **Step 3: Add the offset param**

In `core/util/HijriDateCalculator.kt`, replace `today()`:

```kotlin
fun today(offsetDays: Int = 0): HijriDate =
    toHijri(LocalDate.now().plusDays(offsetDays.toLong()))
```

Leave the other `LocalDate.now()`-based helpers (`isTodayRamadan`, `daysUntilNextRamadan`, …) unchanged — they are out of scope for this plan; note them in your report as offset-inconsistent so a follow-up can address them if needed.

- [ ] **Step 4: Add the preference (interface + impl)**

In `domain/repository/SettingsRepository.kt`, add near the other Int prefs:

```kotlin
val hijriDayOffset: Flow<Int>
suspend fun setHijriDayOffset(days: Int)
```

In `data/local/datastore/PreferencesDataStore.kt`, add the key in `PreferencesKeys`:

```kotlin
val HIJRI_DAY_OFFSET = intPreferencesKey("hijri_day_offset")
```

and the override members (mirror `notificationReminderMinutes`; clamp to the valid range on write so a bad value can't persist):

```kotlin
override val hijriDayOffset: Flow<Int> =
    preference(PreferencesKeys.HIJRI_DAY_OFFSET, 0)
override suspend fun setHijriDayOffset(days: Int) =
    put(PreferencesKeys.HIJRI_DAY_OFFSET, days.coerceIn(-2, 2))
```

- [ ] **Step 5: Run tests + compile**

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.core.util.HijriDateCalculatorOffsetTest"` → PASS.
Run: `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL (confirms the `SettingsRepository` interface addition is implemented — KSP/compiler would fail otherwise).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/domain/repository/SettingsRepository.kt \
        app/src/main/java/com/arshadshah/nimaz/data/local/datastore/PreferencesDataStore.kt \
        app/src/main/java/com/arshadshah/nimaz/core/util/HijriDateCalculator.kt \
        app/src/test/java/com/arshadshah/nimaz/core/util/HijriDateCalculatorOffsetTest.kt
git commit -m "feat(calendar): hijri day offset preference + today(offsetDays)"
```

---

## Task 2: Surface the offset in settings

**Files:**
- Modify: `presentation/viewmodel/SettingsViewModel.kt`
- Modify: `presentation/screens/settings/AppearanceSettingsScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `SettingsRepository.hijriDayOffset`/`setHijriDayOffset` (Task 1); `NimazNumberStepper`.
- Produces: `GeneralSettingsUiState.hijriDayOffset: Int`; `SettingsEvent.SetHijriDayOffset(days: Int)`.

- [ ] **Step 1: Add the string**

In `app/src/main/res/values/strings.xml`, near the appearance/calendar strings:

```xml
<string name="hijri_day_offset_label">Adjust Hijri date</string>
```

- [ ] **Step 2: ViewModel — state + event + handler + load**

In `SettingsViewModel.kt`:
- Add `val hijriDayOffset: Int = 0` to `GeneralSettingsUiState`.
- Add to `SettingsEvent`: `data class SetHijriDayOffset(val days: Int) : SettingsEvent`.
- Add a handler branch mirroring `SetHijriPrimary` (optimistic update then persist):

```kotlin
is SettingsEvent.SetHijriDayOffset -> {
    _generalState.update { it.copy(hijriDayOffset = event.days) }
    viewModelScope.launch { settingsRepository.setHijriDayOffset(event.days) }
}
```
- In the general-settings load block (where `useHijriPrimary` is read via `.first()`), read and fold in the offset:

```kotlin
val hijriOffset = settingsRepository.hijriDayOffset.first()
// …in the .copy(...) that builds general state, add:
hijriDayOffset = hijriOffset,
```
Read the file to place these in the exact existing patterns (state class, `when` handler, load `.copy`).

- [ ] **Step 3: Appearance screen — the stepper row**

In `AppearanceSettingsScreen.kt`, near the Hijri-primary toggle row, add a `NimazNumberStepper` bound to the offset (range −2..+2):

```kotlin
NimazNumberStepper(
    label = stringResource(R.string.hijri_day_offset_label),
    value = generalState.hijriDayOffset,
    onValueChange = { viewModel.onEvent(SettingsEvent.SetHijriDayOffset(it)) },
    minValue = -2,
    maxValue = 2,
)
```
Ensure `NimazNumberStepper` and `SettingsEvent` are imported. Read the surrounding rows to match layout/padding conventions (place it inside the same settings section container as the Hijri toggle).

- [ ] **Step 4: Compile + confirm**

Run: `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL.
There is no Robolectric harness for `AppearanceSettingsScreen`; confirm by reading that the stepper reads `generalState.hijriDayOffset` and emits `SetHijriDayOffset`, and the VM persists + reloads it. State this in your report.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/SettingsViewModel.kt \
        app/src/main/java/com/arshadshah/nimaz/presentation/screens/settings/AppearanceSettingsScreen.kt \
        app/src/main/res/values/strings.xml
git commit -m "feat(settings): Adjust Hijri date (-2..+2) control"
```

---

## Task 3: Thread the offset into both Hijri widgets

**Files:**
- Modify: `widget/hijridate/HijriDateWorker.kt`
- Modify: `widget/hijricalendar/HijriCalendarWorker.kt`

**Interfaces:**
- Consumes: `SettingsRepository.hijriDayOffset` (Task 1); `HijriDateCalculator.today(offsetDays)` (Task 1).

**Design note:** both workers are `@HiltWorker @AssistedInject CoroutineWorker`. Add `SettingsRepository` as an injected constructor param (alongside the existing `@Assisted` context/params). Read the offset once at the top of `doWork()` and apply it to `HijriDateCalculator.today(offset)` **and** to the Gregorian `LocalDate.now()` used for the same computation, and (calendar widget) to `getIslamicEvents`/day-highlight so "today" shifts consistently.

- [ ] **Step 1: HijriDateWorker**

Read `widget/hijridate/HijriDateWorker.kt`. Add to the constructor (mirroring how other `@HiltWorker`s inject repos): `private val settingsRepository: SettingsRepository`. In `doWork()`, replace:

```kotlin
val hijriDate = HijriDateCalculator.today()
val today = LocalDate.now()
```
with:
```kotlin
val offset = settingsRepository.hijriDayOffset.first()
val hijriDate = HijriDateCalculator.today(offset)
val today = LocalDate.now().plusDays(offset.toLong())
```
Add imports: `com.arshadshah.nimaz.domain.repository.SettingsRepository`, `kotlinx.coroutines.flow.first`.

- [ ] **Step 2: HijriCalendarWorker**

Read `widget/hijricalendar/HijriCalendarWorker.kt`. Same injection. In `doWork()` replace the `HijriDateCalculator.today()` + `LocalDate.now()` reads with the offset-applied versions (as Step 1), so `daysInMonth`, `firstOfMonth`, `getIslamicEvents(hijriDate.year)`, and the today-cell highlight (`it.day == hijriDate.day && it.month == hijriDate.month`) all use the offset-shifted `hijriDate`.

- [ ] **Step 3: Compile**

Run: `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL (KSP validates the Hilt worker wiring — a bad injection fails here).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/widget/hijridate/HijriDateWorker.kt \
        app/src/main/java/com/arshadshah/nimaz/widget/hijricalendar/HijriCalendarWorker.kt
git commit -m "feat(widgets): apply hijri day offset in both Hijri widgets"
```

---

## Task 4: Model — `CELEBRATION` type, `CelebrationEvent`, new fields, start gate, entity mirror

**Files:**
- Modify: `domain/model/Announcement.kt`
- Modify: `data/local/datastore/AnnouncementLocalDataSource.kt` (the `AnnouncementEntity` + `toDomain`/`toEntity`)
- Test: `app/src/test/java/com/arshadshah/nimaz/domain/model/AnnouncementStartGateTest.kt`

**Interfaces:**
- Produces: `AnnouncementType.CELEBRATION`; `enum CelebrationEvent(key)` + `fromKey`; `Announcement` gains `event: CelebrationEvent? = null, arabic: String? = null, transliteration: String? = null, proofRef: String? = null, proofText: String? = null, cta2Label: String? = null, route2: String? = null, startsAtMillis: Long? = null`; `isActiveFor` gains the start gate.

- [ ] **Step 1: Failing test for the start gate**

Create `app/src/test/java/com/arshadshah/nimaz/domain/model/AnnouncementStartGateTest.kt`:

```kotlin
package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AnnouncementStartGateTest {

    private fun ann(startsAt: Long?, expiresAt: Long?) = Announcement(
        id = "x", type = AnnouncementType.CELEBRATION, title = "t", body = "b",
        startsAtMillis = startsAt, expiresAtMillis = expiresAt,
    )

    @Test
    fun `not active before startsAt`() {
        assertThat(ann(startsAt = 2_000, expiresAt = null).isActiveFor(1, nowMillis = 1_000)).isFalse()
    }

    @Test
    fun `active at or after startsAt`() {
        assertThat(ann(startsAt = 2_000, expiresAt = null).isActiveFor(1, nowMillis = 2_000)).isTrue()
        assertThat(ann(startsAt = 2_000, expiresAt = null).isActiveFor(1, nowMillis = 3_000)).isTrue()
    }

    @Test
    fun `null startsAt means always started`() {
        assertThat(ann(startsAt = null, expiresAt = null).isActiveFor(1, nowMillis = 0)).isTrue()
    }

    @Test
    fun `start and expiry window both enforced`() {
        val a = ann(startsAt = 2_000, expiresAt = 4_000)
        assertThat(a.isActiveFor(1, 1_999)).isFalse()
        assertThat(a.isActiveFor(1, 2_000)).isTrue()
        assertThat(a.isActiveFor(1, 3_999)).isTrue()
        assertThat(a.isActiveFor(1, 4_000)).isFalse()
    }

    @Test
    fun `CelebrationEvent fromKey degrades unknown to GENERIC`() {
        assertThat(CelebrationEvent.fromKey("eid_al_fitr")).isEqualTo(CelebrationEvent.EID_AL_FITR)
        assertThat(CelebrationEvent.fromKey("nonsense")).isEqualTo(CelebrationEvent.GENERIC)
        assertThat(CelebrationEvent.fromKey(null)).isEqualTo(CelebrationEvent.GENERIC)
    }
}
```

- [ ] **Step 2: Run → fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.domain.model.AnnouncementStartGateTest"`
Expected: FAIL — `CelebrationEvent`/`startsAtMillis` unresolved.

- [ ] **Step 3: Update the model**

In `domain/model/Announcement.kt`:

Add `CELEBRATION("celebration")` to `AnnouncementType` (keep `fromKey` as-is).

Add the enum:
```kotlin
/** Occasion behind a CELEBRATION announcement. Keys match IslamicEvents.events ids. */
enum class CelebrationEvent(val key: String) {
    EID_AL_FITR("eid_al_fitr"),
    EID_AL_ADHA("eid_al_adha"),
    RAMADAN_START("ramadan_start"),
    RAMADAN_END("ramadan_end"),
    LAYLAT_AL_QADR("laylat_al_qadr"),
    ARAFAH("day_of_arafah"),
    ASHURA("ashura"),
    MAWLID("mawlid"),
    HIJRI_NEW_YEAR("islamic_new_year"),
    JUMUAH("jumuah"),
    GENERIC("generic");

    companion object {
        fun fromKey(key: String?): CelebrationEvent =
            entries.firstOrNull { it.key == key?.trim()?.lowercase() } ?: GENERIC
    }
}
```

Add the new fields to `Announcement` (all nullable defaults so existing serialized entities deserialize unchanged):
```kotlin
    val event: CelebrationEvent? = null,
    val arabic: String? = null,
    val transliteration: String? = null,
    val proofRef: String? = null,
    val proofText: String? = null,
    val cta2Label: String? = null,
    val route2: String? = null,
    val startsAtMillis: Long? = null,
```

Update `isActiveFor` to add the start gate:
```kotlin
fun isActiveFor(versionCode: Int, nowMillis: Long): Boolean =
    (startsAtMillis == null || nowMillis >= startsAtMillis) &&
    (expiresAtMillis == null || nowMillis < expiresAtMillis) &&
    (minVersionCode == null || versionCode >= minVersionCode) &&
    (maxVersionCode == null || versionCode <= maxVersionCode)
```

- [ ] **Step 4: Mirror the fields in the persistence entity**

In `data/local/datastore/AnnouncementLocalDataSource.kt`, the `@Serializable internal data class AnnouncementEntity` must carry the new fields or they are lost across process death. Add to `AnnouncementEntity` (all nullable defaults; `event` stored as its `key` string):
```kotlin
    val event: String? = null,
    val arabic: String? = null,
    val transliteration: String? = null,
    val proofRef: String? = null,
    val proofText: String? = null,
    val cta2Label: String? = null,
    val route2: String? = null,
    val startsAtMillis: Long? = null,
```
Update `toDomain()` to map them back (`event = CelebrationEvent.fromKey(event).takeIf { type == AnnouncementType.CELEBRATION.key }` — read `event` string only for celebration entities; for other types leave `event = null`). Update `toEntity()` to write them (`event = event?.key`, plus the string/long fields directly). Read the existing `toDomain`/`toEntity` bodies and extend them in the same style.

- [ ] **Step 5: Run tests + compile**

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.domain.model.AnnouncementStartGateTest"` → PASS.
Run: `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/domain/model/Announcement.kt \
        app/src/main/java/com/arshadshah/nimaz/data/local/datastore/AnnouncementLocalDataSource.kt \
        app/src/test/java/com/arshadshah/nimaz/domain/model/AnnouncementStartGateTest.kt
git commit -m "feat(fcm): CELEBRATION type, CelebrationEvent, celebration fields + start gate"
```

---

## Task 5: Mapper — new keys, PAYLOAD_KEYS, parsing

**Files:**
- Modify: `data/announcement/AnnouncementPayloadMapper.kt`
- Test: `app/src/test/java/com/arshadshah/nimaz/data/announcement/AnnouncementPayloadMapperTest.kt`

**Interfaces:**
- Consumes: the model additions from Task 4.
- Produces: `fromPayload` parses `event, arabic, transliteration, proof_ref, proof_text, cta2_label, route2, starts_at`; all 8 keys in `PAYLOAD_KEYS`.

- [ ] **Step 1: Extend the test**

Add to `app/src/test/java/com/arshadshah/nimaz/data/announcement/AnnouncementPayloadMapperTest.kt` (read it first, mirror its map-building helper):

```kotlin
@Test
fun `celebration payload parses event and rich fields`() {
    val a = mapper.fromPayload(
        mapOf(
            "id" to "2027-eid", "type" to "celebration", "event" to "eid_al_fitr",
            "title" to "Eid Mubarak", "body" to "…",
            "arabic" to "تقبل الله", "transliteration" to "taqabbal Allah",
            "proof_ref" to "Al-Baqarah 2:185", "proof_text" to "…complete the count.",
            "cta_label" to "Eid prayer", "route" to "prayer/times",
            "cta2_label" to "Takbir", "route2" to "dua/reader/takbir",
            "starts_at" to "2027-03-07T18:00:00Z",
        )
    )
    assertThat(a).isNotNull()
    assertThat(a!!.type).isEqualTo(AnnouncementType.CELEBRATION)
    assertThat(a.event).isEqualTo(CelebrationEvent.EID_AL_FITR)
    assertThat(a.arabic).isEqualTo("تقبل الله")
    assertThat(a.proofRef).isEqualTo("Al-Baqarah 2:185")
    assertThat(a.cta2Label).isEqualTo("Takbir")
    assertThat(a.route2).isEqualTo("dua/reader/takbir")
    assertThat(a.startsAtMillis).isNotNull()
}

@Test
fun `unknown event degrades to GENERIC not null`() {
    val a = mapper.fromPayload(
        mapOf("id" to "x", "type" to "celebration", "event" to "wat",
              "title" to "t", "body" to "b")
    )
    assertThat(a).isNotNull()
    assertThat(a!!.event).isEqualTo(CelebrationEvent.GENERIC)
}

@Test
fun `event ignored for non-celebration types`() {
    val a = mapper.fromPayload(
        mapOf("id" to "x", "type" to "feature", "event" to "eid_al_fitr",
              "title" to "t", "body" to "b")
    )
    assertThat(a!!.event).isNull()
}

@Test
fun `half a proof pair is dropped, rest survives`() {
    val a = mapper.fromPayload(
        mapOf("id" to "x", "type" to "celebration", "title" to "t", "body" to "b",
              "proof_ref" to "only ref")
    )
    assertThat(a).isNotNull()
    assertThat(a!!.proofRef).isNull()
    assertThat(a.proofText).isNull()
}

@Test
fun `malformed starts_at rejects the whole payload`() {
    val a = mapper.fromPayload(
        mapOf("id" to "x", "type" to "celebration", "title" to "t", "body" to "b",
              "starts_at" to "not-a-date")
    )
    assertThat(a).isNull()
}
```

- [ ] **Step 2: Run → fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.data.announcement.AnnouncementPayloadMapperTest"`
Expected: FAIL — new fields unresolved / not parsed.

- [ ] **Step 3: Extend the mapper**

In `AnnouncementPayloadMapper.kt` add the consts and append them to `PAYLOAD_KEYS`:
```kotlin
const val KEY_EVENT = "event"
const val KEY_ARABIC = "arabic"
const val KEY_TRANSLITERATION = "transliteration"
const val KEY_PROOF_REF = "proof_ref"
const val KEY_PROOF_TEXT = "proof_text"
const val KEY_CTA2_LABEL = "cta2_label"
const val KEY_ROUTE2 = "route2"
const val KEY_STARTS_AT = "starts_at"
```
```kotlin
val PAYLOAD_KEYS = listOf(
    KEY_ID, KEY_TYPE, KEY_TITLE, KEY_BODY, KEY_CTA_LABEL, KEY_ROUTE,
    KEY_MIN_VERSION_CODE, KEY_MAX_VERSION_CODE, KEY_EXPIRES_AT, KEY_DISMISSABLE,
    KEY_EVENT, KEY_ARABIC, KEY_TRANSLITERATION, KEY_PROOF_REF, KEY_PROOF_TEXT,
    KEY_CTA2_LABEL, KEY_ROUTE2, KEY_STARTS_AT,
)
```
In `fromPayload`, after the existing parses, add (keeping the null-on-malformed discipline; proof degrades locally):
```kotlin
    val startsAtMillis = data[KEY_STARTS_AT]?.let { raw ->
        runCatching { Instant.parse(raw.trim()).toEpochMilli() }.getOrNull() ?: return null
    }
    val event = if (type == AnnouncementType.CELEBRATION)
        CelebrationEvent.fromKey(data[KEY_EVENT]) else null
    val proofRef = data[KEY_PROOF_REF]?.trim()?.ifEmpty { null }
    val proofText = data[KEY_PROOF_TEXT]?.trim()?.ifEmpty { null }
    val bothProof = if (proofRef != null && proofText != null) proofRef to proofText else null
```
and extend the returned `Announcement(...)` with:
```kotlin
        event = event,
        arabic = data[KEY_ARABIC]?.trim()?.ifEmpty { null },
        transliteration = data[KEY_TRANSLITERATION]?.trim()?.ifEmpty { null },
        proofRef = bothProof?.first,
        proofText = bothProof?.second,
        cta2Label = data[KEY_CTA2_LABEL]?.trim()?.ifEmpty { null },
        route2 = data[KEY_ROUTE2]?.trim()?.ifEmpty { null },
        startsAtMillis = startsAtMillis,
```
(`Instant` is already imported in this file.)

- [ ] **Step 4: Run tests + compile**

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.data.announcement.AnnouncementPayloadMapperTest"` → PASS.
Run: `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/data/announcement/AnnouncementPayloadMapper.kt \
        app/src/test/java/com/arshadshah/nimaz/data/announcement/AnnouncementPayloadMapperTest.kt
git commit -m "feat(fcm): parse celebration payload keys (incl. PAYLOAD_KEYS)"
```

---

## Task 6: Local event source + `CelebrationEvent`↔`EventOccasion` mapping

**Files:**
- Create: `domain/model/HomeEventCard.kt`
- Create: `domain/usecase/ObserveLocalEventsUseCase.kt`
- Create: `presentation/components/organisms/CelebrationEventMapping.kt`
- Test: `app/src/test/java/com/arshadshah/nimaz/domain/usecase/ObserveLocalEventsUseCaseTest.kt`

**Interfaces:**
- Produces:
  - `data class HomeEventCard(event: CelebrationEvent, eyebrow: String, headline: String, body: String, arabic: String? = null, transliteration: String? = null, proofRef: String? = null, proofText: String? = null, ctaLabel: String? = null, route: String? = null, cta2Label: String? = null, route2: String? = null, announcementId: String? = null, dismissable: Boolean = false, priority: Int = 0)` (domain, no presentation types/lambdas).
  - `ObserveLocalEventsUseCase(settingsRepository: SettingsRepository, nowDate: () -> LocalDate = { LocalDate.now() })` with `operator fun invoke(): Flow<List<HomeEventCard>>` — matches `IslamicEvents.events` against `HijriDateCalculator.today(offset)` by `(hijriMonth, hijriDay)`, mapping each matched `IslamicEvent.id` to a `CelebrationEvent` (via `CelebrationEvent.fromKey`, with `laylat_al_qadr_*` normalised to `laylat_al_qadr`), sorted by priority desc.
  - `fun CelebrationEvent.toOccasion(): EventOccasion` (presentation).

**Design note:** `IslamicEvents.events` is a static list, and `today()` is wall-clock — so the use case re-emits when the offset changes (and the VM re-subscribes on resume). That is sufficient for a day-granularity card; a midnight ticker is out of scope (note it).

- [ ] **Step 1: Failing test**

Create `app/src/test/java/com/arshadshah/nimaz/domain/usecase/ObserveLocalEventsUseCaseTest.kt`:

```kotlin
package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.CelebrationEvent
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.time.LocalDate

class ObserveLocalEventsUseCaseTest {

    // Minimal fake exposing only hijriDayOffset; other members throw (unused here).
    private fun repo(offset: Int): SettingsRepository = FakeSettings(offset)

    @Test
    fun `emits eid card on 1 Shawwal`() = runBlocking {
        // 1 Shawwal 1448 ≈ a specific Gregorian date; use HijriDateCalculator to find one.
        val eidGregorian = com.arshadshah.nimaz.core.util.HijriDateCalculator
            .toGregorian(1, 10, com.arshadshah.nimaz.core.util.HijriDateCalculator.today().year + 1)
        val useCase = ObserveLocalEventsUseCase(repo(0), nowDate = { eidGregorian })
        val cards = useCase().first()
        assertThat(cards.map { it.event }).contains(CelebrationEvent.EID_AL_FITR)
    }

    @Test
    fun `emits empty list on an ordinary day`() = runBlocking {
        // 5th of month 2 (Safar) — no event in IslamicEvents.events
        val plainDay = com.arshadshah.nimaz.core.util.HijriDateCalculator.toGregorian(5, 2, 1448)
        val useCase = ObserveLocalEventsUseCase(repo(0), nowDate = { plainDay })
        assertThat(useCase().first()).isEmpty()
    }
}

private class FakeSettings(private val offset: Int) : SettingsRepository {
    override val hijriDayOffset: Flow<Int> = flowOf(offset)
    override suspend fun setHijriDayOffset(days: Int) {}
    // All other SettingsRepository members are unused by this use case.
    // Implement them as TODO()/no-op to satisfy the interface — the test never calls them.
}
```
NOTE to implementer: `FakeSettings` must implement the FULL `SettingsRepository` interface. Read `SettingsRepository.kt` and stub every other member (`= flowOf(default)` for Flows, `{}`/`TODO("unused")` for setters). Keep it in the test file. If the interface is very large, consider a `@Suppress` or delegate; the point is the use case only touches `hijriDayOffset`.

- [ ] **Step 2: Run → fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.domain.usecase.ObserveLocalEventsUseCaseTest"`
Expected: FAIL — `ObserveLocalEventsUseCase`/`HomeEventCard` unresolved.

- [ ] **Step 3: Create the domain model**

Create `domain/model/HomeEventCard.kt`:

```kotlin
package com.arshadshah.nimaz.domain.model

/** A resolved occasion card for the Home carousel — domain-only (no UI/lambda types). */
data class HomeEventCard(
    val event: CelebrationEvent,
    val eyebrow: String,
    val headline: String,
    val body: String,
    val arabic: String? = null,
    val transliteration: String? = null,
    val proofRef: String? = null,
    val proofText: String? = null,
    val ctaLabel: String? = null,
    val route: String? = null,
    val cta2Label: String? = null,
    val route2: String? = null,
    val announcementId: String? = null,
    val dismissable: Boolean = false,
    val priority: Int = 0,
)
```

- [ ] **Step 4: Create the use case**

Create `domain/usecase/ObserveLocalEventsUseCase.kt`:

```kotlin
package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.domain.model.CelebrationEvent
import com.arshadshah.nimaz.domain.model.HomeEventCard
import com.arshadshah.nimaz.domain.model.IslamicEvents
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Emits occasion cards for today's Hijri date (offset-adjusted) from the static calendar. */
class ObserveLocalEventsUseCase(
    private val settingsRepository: SettingsRepository,
    private val nowDate: () -> LocalDate = { LocalDate.now() },
) {
    operator fun invoke(): Flow<List<HomeEventCard>> =
        settingsRepository.hijriDayOffset.map { offset ->
            val today = HijriDateCalculator.toHijri(nowDate().plusDays(offset.toLong()))
            IslamicEvents.events
                .filter { it.hijriMonth == today.month && it.hijriDay == today.day }
                .sortedByDescending { it.priority }
                .map { ev ->
                    HomeEventCard(
                        event = CelebrationEvent.fromKey(normaliseId(ev.id)),
                        eyebrow = ev.nameEnglish,
                        headline = ev.nameEnglish,
                        body = ev.description.orEmpty(),
                        arabic = ev.nameArabic.ifBlank { null },
                        priority = ev.priority,
                    )
                }
        }

    /** Collapse the five dated Laylat al-Qadr ids onto the single CelebrationEvent key. */
    private fun normaliseId(id: String): String =
        if (id.startsWith("laylat_al_qadr")) "laylat_al_qadr" else id
}
```
(`HijriDateCalculator.today(offset)` and `toHijri(nowDate()+offset)` are equivalent; using `toHijri(nowDate()+offset)` lets the test inject `nowDate`.)

- [ ] **Step 5: Create the presentation mapping**

Create `presentation/components/organisms/CelebrationEventMapping.kt`:

```kotlin
package com.arshadshah.nimaz.presentation.components.organisms

import com.arshadshah.nimaz.domain.model.CelebrationEvent

/** Maps a domain occasion to its presentation styling occasion. */
fun CelebrationEvent.toOccasion(): EventOccasion = when (this) {
    CelebrationEvent.EID_AL_FITR -> EventOccasion.EID_AL_FITR
    CelebrationEvent.EID_AL_ADHA -> EventOccasion.EID_AL_ADHA
    CelebrationEvent.RAMADAN_START, CelebrationEvent.RAMADAN_END -> EventOccasion.RAMADAN
    CelebrationEvent.LAYLAT_AL_QADR -> EventOccasion.LAYLAT_AL_QADR
    CelebrationEvent.ARAFAH -> EventOccasion.ARAFAH
    CelebrationEvent.ASHURA -> EventOccasion.ASHURA
    CelebrationEvent.MAWLID -> EventOccasion.MAWLID
    CelebrationEvent.HIJRI_NEW_YEAR -> EventOccasion.HIJRI_NEW_YEAR
    CelebrationEvent.JUMUAH -> EventOccasion.JUMUAH
    CelebrationEvent.GENERIC -> EventOccasion.GENERIC
}
```

- [ ] **Step 6: Run tests + compile**

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.domain.usecase.ObserveLocalEventsUseCaseTest"` → PASS.
Run: `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/domain/model/HomeEventCard.kt \
        app/src/main/java/com/arshadshah/nimaz/domain/usecase/ObserveLocalEventsUseCase.kt \
        app/src/main/java/com/arshadshah/nimaz/presentation/components/organisms/CelebrationEventMapping.kt \
        app/src/test/java/com/arshadshah/nimaz/domain/usecase/ObserveLocalEventsUseCaseTest.kt
git commit -m "feat(events): local calendar event source + CelebrationEvent mapping"
```

---

## Task 7: Merge pushed celebrations with local events + Home wiring

**Files:**
- Create: `domain/usecase/ObserveEventCardsUseCase.kt`
- Modify: `core/di/AnnouncementModule.kt` (or the module that provides Home use cases — read to confirm) to provide the two new use cases
- Modify: `presentation/viewmodel/HomeViewModel.kt`
- Modify: `presentation/screens/home/HomeScreen.kt`
- Test: `app/src/test/java/com/arshadshah/nimaz/domain/usecase/ObserveEventCardsUseCaseTest.kt`

**Interfaces:**
- Consumes: `ObserveLocalEventsUseCase` (Task 6); `ObserveActiveAnnouncementUseCase` (existing); `CelebrationEvent`, `HomeEventCard` (Task 4/6); `CelebrationEvent.toOccasion()` (Task 6).
- Produces:
  - `ObserveEventCardsUseCase(local: ObserveLocalEventsUseCase, observeActiveAnnouncement: ObserveActiveAnnouncementUseCase)` → `Flow<List<HomeEventCard>>`: takes local cards; if an active announcement has `type == CELEBRATION`, builds a pushed `HomeEventCard` from it; **merge rule** — if `pushed.event.key == local.event.key` for some local card, that local card is replaced by a merged card (pushed fields win, local fills blanks); otherwise the pushed card is added. Sort by priority desc, pushed before local on ties. Cap at 2.
  - `HomeUiState.celebrationCards: List<HomeEventCard>` (default empty).

- [ ] **Step 1: Failing test**

Create `app/src/test/java/com/arshadshah/nimaz/domain/usecase/ObserveEventCardsUseCaseTest.kt`:

```kotlin
package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.Announcement
import com.arshadshah.nimaz.domain.model.AnnouncementType
import com.arshadshah.nimaz.domain.model.CelebrationEvent
import com.arshadshah.nimaz.domain.model.HomeEventCard
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ObserveEventCardsUseCaseTest {

    private fun localOf(vararg cards: HomeEventCard) =
        ObserveLocalEventsFake(cards.toList())

    @Test
    fun `pushed celebration matching local is merged, pushed fields win`() = runBlocking {
        val local = localOf(
            HomeEventCard(CelebrationEvent.EID_AL_FITR, "Eid al-Fitr", "Eid al-Fitr", "local body", priority = 10)
        )
        val pushed = Announcement(
            id = "p1", type = AnnouncementType.CELEBRATION, title = "Eid Mubarak", body = "pushed body",
            event = CelebrationEvent.EID_AL_FITR, arabic = "تقبل الله",
        )
        val useCase = ObserveEventCardsUseCase(local, observe = { flowOf(pushed) })
        val cards = useCase().first()
        assertThat(cards).hasSize(1)
        assertThat(cards[0].body).isEqualTo("pushed body")     // pushed wins
        assertThat(cards[0].arabic).isEqualTo("تقبل الله")     // pushed fills
        assertThat(cards[0].announcementId).isEqualTo("p1")    // dismissable pushed identity
    }

    @Test
    fun `non-matching pushed celebration is added alongside local`() = runBlocking {
        val local = localOf(
            HomeEventCard(CelebrationEvent.ARAFAH, "Arafah", "Arafah", "b", priority = 5)
        )
        val pushed = Announcement(
            id = "p2", type = AnnouncementType.CELEBRATION, title = "Special", body = "b2",
            event = CelebrationEvent.GENERIC,
        )
        val cards = ObserveEventCardsUseCase(local, observe = { flowOf(pushed) })().first()
        assertThat(cards.map { it.event })
            .containsExactly(CelebrationEvent.GENERIC, CelebrationEvent.ARAFAH)
    }

    @Test
    fun `non-celebration announcement is ignored`() = runBlocking {
        val local = localOf(HomeEventCard(CelebrationEvent.ASHURA, "Ashura", "Ashura", "b"))
        val feature = Announcement(id = "f", type = AnnouncementType.FEATURE, title = "t", body = "b")
        val cards = ObserveEventCardsUseCase(local, observe = { flowOf(feature) })().first()
        assertThat(cards.map { it.event }).containsExactly(CelebrationEvent.ASHURA)
    }

    @Test
    fun `caps at two cards`() = runBlocking {
        val local = localOf(
            HomeEventCard(CelebrationEvent.ARAFAH, "a", "a", "b", priority = 3),
            HomeEventCard(CelebrationEvent.ASHURA, "c", "c", "b", priority = 2),
            HomeEventCard(CelebrationEvent.MAWLID, "m", "m", "b", priority = 1),
        )
        val cards = ObserveEventCardsUseCase(local, observe = { flowOf(null) })().first()
        assertThat(cards).hasSize(2)
    }
}

// Fake local use case returning a fixed list. ObserveLocalEventsUseCase is a class, so this
// helper wraps a fixed flow; the merge use case must accept the local source as a Flow provider.
private class ObserveLocalEventsFake(private val cards: List<HomeEventCard>) {
    operator fun invoke() = flowOf(cards)
}
```
NOTE to implementer: to make the merge use case unit-testable without constructing a real `ObserveLocalEventsUseCase`, define `ObserveEventCardsUseCase` to take its local source as a **function** `local: () -> Flow<List<HomeEventCard>>` (or an interface), and `observe: () -> Flow<Announcement?>`. Adjust the test's `localOf`/construction to match your chosen shape — keep the four behaviors asserted (merge-wins, add-non-matching, ignore-non-celebration, cap-2). If you prefer passing the concrete `ObserveLocalEventsUseCase`, give it a testable seam; do NOT hit real wall-clock/DataStore in the test.

- [ ] **Step 2: Run → fail**, then implement.

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.domain.usecase.ObserveEventCardsUseCaseTest"` → FAIL.

- [ ] **Step 3: Create the merge use case**

Create `domain/usecase/ObserveEventCardsUseCase.kt`:

```kotlin
package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.Announcement
import com.arshadshah.nimaz.domain.model.AnnouncementType
import com.arshadshah.nimaz.domain.model.CelebrationEvent
import com.arshadshah.nimaz.domain.model.HomeEventCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Merges local calendar occasion cards with a pushed CELEBRATION announcement.
 * Pushed fields win on a same-event match; otherwise both render. Capped at two.
 */
class ObserveEventCardsUseCase(
    private val local: () -> Flow<List<HomeEventCard>>,
    private val observe: () -> Flow<Announcement?>,
) {
    operator fun invoke(): Flow<List<HomeEventCard>> =
        combine(local(), observe()) { localCards, announcement ->
            val pushed = announcement
                ?.takeIf { it.type == AnnouncementType.CELEBRATION }
                ?.let(::toCard)

            val merged: List<HomeEventCard> = when {
                pushed == null -> localCards
                localCards.any { it.event == pushed.event } ->
                    localCards.map { if (it.event == pushed.event) mergePushedOver(it, pushed) else it }
                else -> listOf(pushed) + localCards
            }
            merged
                .sortedWith(compareByDescending<HomeEventCard> { it.priority }
                    .thenByDescending { it.announcementId != null }) // pushed before local on ties
                .take(2)
        }

    private fun toCard(a: Announcement) = HomeEventCard(
        event = a.event ?: CelebrationEvent.GENERIC,
        eyebrow = a.title,
        headline = a.title,
        body = a.body,
        arabic = a.arabic,
        transliteration = a.transliteration,
        proofRef = a.proofRef,
        proofText = a.proofText,
        ctaLabel = a.ctaLabel,
        route = a.route,
        cta2Label = a.cta2Label,
        route2 = a.route2,
        announcementId = a.id,
        dismissable = a.dismissable,
        priority = 100, // pushed outranks local by default
    )

    /** Pushed fields win; local fills only where pushed is null/blank. */
    private fun mergePushedOver(local: HomeEventCard, pushed: HomeEventCard) = pushed.copy(
        eyebrow = pushed.eyebrow.ifBlank { local.eyebrow },
        headline = pushed.headline.ifBlank { local.headline },
        body = pushed.body.ifBlank { local.body },
        arabic = pushed.arabic ?: local.arabic,
        transliteration = pushed.transliteration ?: local.transliteration,
        proofRef = pushed.proofRef ?: local.proofRef,
        proofText = pushed.proofText ?: local.proofText,
    )
}
```

- [ ] **Step 4: Provide the use cases (DI)**

Read the Hilt module that provides Home dependencies (likely `core/di/AnnouncementModule.kt` provides `AnnouncementUseCases`; the Home VM may get use cases via a bundle or individually). Provide `ObserveLocalEventsUseCase(settingsRepository)` and `ObserveEventCardsUseCase(local = { observeLocalEvents() }, observe = { observeActiveAnnouncement() })`. Match the existing provider style (`@Provides @Singleton`), reusing the already-provided `ObserveActiveAnnouncementUseCase` and `SettingsRepository`. Read the module first to wire correctly.

- [ ] **Step 5: ViewModel — expose celebrationCards**

In `HomeViewModel.kt`: inject `ObserveEventCardsUseCase` (follow how `announcementUseCases` is injected). Add `celebrationCards: List<HomeEventCard> = emptyList()` to `HomeUiState`. Collect the use case into state (mirror the existing announcement collection — a `stateIn`/`combine` or an `onEach { _state.update { it.copy(celebrationCards = ...) } }`). Read the VM's state-assembly to place it in the existing pattern. Add import for `HomeEventCard`.

- [ ] **Step 6: HomeScreen — render merged cards**

In `HomeScreen.kt`, extend the inline `buildList` (compact) and the tablet list so that, after the Jumu'ah card, each `state.celebrationCards` entry is mapped to an `EventCardUi`:

```kotlin
state.celebrationCards.forEach { c ->
    add(
        EventCardUi(
            occasion = c.event.toOccasion(),
            eyebrow = c.eyebrow,
            headline = c.headline,
            body = c.body,
            arabic = c.arabic,
            transliteration = c.transliteration,
            proof = if (c.proofRef != null && c.proofText != null) c.proofRef to c.proofText else null,
            primaryAction = if (c.ctaLabel != null && c.route != null)
                EventAction(c.ctaLabel) { onOpenAnnouncementRoute(c.route) } else null,
            secondaryAction = if (c.cta2Label != null && c.route2 != null)
                EventAction(c.cta2Label) { onOpenAnnouncementRoute(c.route2) } else null,
            onDismiss = if (c.dismissable && c.announcementId != null)
                { { viewModel.onEvent(HomeEvent.DismissAnnouncement) } } else null,
        )
    )
}
```
Use the existing `onOpenAnnouncementRoute` lambda already wired for the banner CTA (confirm its name/signature in this screen; it takes a route string). Import `EventAction`, `HomeEventCard`'s `toOccasion` extension (`...organisms.toOccasion`). Keep the Jumu'ah card first. Ensure the `if (eventCards.isNotEmpty())` carousel item still guards emptiness. Apply the same mapping in the tablet layout's list.

- [ ] **Step 7: Run tests + compile**

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.domain.usecase.ObserveEventCardsUseCaseTest"` → PASS.
Run: `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL, then `./gradlew :app:testDebugUnitTest` → full suite green.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/domain/usecase/ObserveEventCardsUseCase.kt \
        app/src/main/java/com/arshadshah/nimaz/core/di/AnnouncementModule.kt \
        app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/HomeViewModel.kt \
        app/src/main/java/com/arshadshah/nimaz/presentation/screens/home/HomeScreen.kt \
        app/src/test/java/com/arshadshah/nimaz/domain/usecase/ObserveEventCardsUseCaseTest.kt
git commit -m "feat(events): merge pushed celebrations with local events into the carousel"
```

---

## Task 8: Docs

**Files:**
- Modify: `docs/SUBSYSTEMS.md`, `docs/ARCHITECTURE.md`, `docs/nimaz-pro-data-guide.md` (if it documents preferences)

- [ ] **Step 1: SUBSYSTEMS.md** — in notifications/announcements: document the `celebration` type, the new payload fields + `PAYLOAD_KEYS`, `startsAtMillis` start gate, and the `AnnouncementEntity` mirror requirement. In preferences: `hijri_day_offset` (−2..+2). In prayer-time/calendar: `HijriDateCalculator.today(offsetDays)`. In widgets: both Hijri widgets read `hijriDayOffset`. Add a "Home event cards" note: local (`IslamicEvents.events` × `today(offset)`) + pushed celebrations merged by `ObserveEventCardsUseCase` → `EventsCarousel`.
- [ ] **Step 2: ARCHITECTURE.md §9** — note the deferred dismissed-id prune (bare `Set<String>`, no expiry) as a known follow-up, and the offset-inconsistency of the other `HijriDateCalculator` `now()` helpers.
- [ ] **Step 3: Commit**

```bash
git add docs/SUBSYSTEMS.md docs/ARCHITECTURE.md docs/nimaz-pro-data-guide.md
git commit -m "docs: celebrations, hijri offset, local+pushed event merge"
```

---

## Deferred (follow-ups, not in this plan)

- **Prune dismissed ids past expiry** (spec step 10) — needs dismissed-id storage to carry expiry; deferred by decision.
- **Midnight/date-change refresh** of local event cards — the source re-emits on offset change and VM resubscribe, not on a wall-clock midnight tick.
- **Offset consistency across all `HijriDateCalculator.now()` helpers** (`isTodayRamadan`, `daysUntilNextRamadan`, …) — only `today()` gained the param.
- **`route2`/`route` validation before send** and **string-id destination empty states** — see Plan 2 deferred.

## Self-Review

**Spec coverage:** §2.1 CelebrationEvent+CELEBRATION+fields+start gate → Task 4; §2.2 mapper+PAYLOAD_KEYS → Task 5; §3.4 local source + merge → Tasks 6–7; §3.5 hijri offset + widgets → Tasks 1–3; §3.6 strings → Task 2 (label) — pushed content intentionally not translatable. §0.1 item 5 prune → deferred by decision. Persistence-entity mirror (grounded gap) → Task 4 Step 4.

**Placeholder scan:** no TBD; complete code for model/mapper/merge/use case; the fakes in Tasks 6–7 are explicitly flagged as "implement the full interface / choose a testable seam" adaptation steps, not vague directives.

**Type consistency:** `hijriDayOffset`, `today(offsetDays)`, `CelebrationEvent(key)`/`fromKey`, the 8 `Announcement` fields, `AnnouncementEntity` mirror, the 8 `KEY_*`/`PAYLOAD_KEYS`, `HomeEventCard`, `ObserveLocalEventsUseCase`, `ObserveEventCardsUseCase`, `CelebrationEvent.toOccasion()`, `HomeUiState.celebrationCards` are referenced identically across tasks. `EventCardUi`/`EventAction`/`EventOccasion` match Plan 1's verified signatures.
