# Widget UI Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign all five Glance home-screen widgets into one "Refined Minimal" visual language, replacing every emoji/ASCII/unicode glyph with real vector-drawable iconography.

**Architecture:** UI-layer-only change. Each widget keeps its `GlanceAppWidget`, `provideGlance`, state definition, worker and receiver untouched — only the `Success` composable is rewritten. New monochrome vector drawables in `res/drawable/` are tinted at runtime via `ColorFilter.tint(ColorProvider)`. Repeated container/icon/label/pill UI is centralized as new atoms in `widget/core/WidgetUi.kt`.

**Tech Stack:** Kotlin, Jetpack Glance `1.2.0-rc01` (glance-appwidget), Android vector drawables, JUnit (JVM unit test for the glyph guard). minSdk 29, compileSdk 37.

## Global Constraints

- **No emoji, no ASCII art, no unicode symbol glyphs** in widget UI. The em-dash `—` (U+2014) is permitted ONLY as a last-resort empty-value text fallback. The checkmark `✓` (U+2713) and single-letter prayer labels (`F/D/A/M/I`) must be gone.
- Glance cannot use Compose `Icons.*` (`material-icons-extended`). All widget icons ship as **vector drawable XML** in `app/src/main/res/drawable/`.
- No hardcoded `Color(0xFF…)`; colors come from `res/color`/`res/values*/widget_colors.xml` via `ColorProvider(R.color.widget_*)`.
- No changes to data classes, state definitions, workers, receivers, `WidgetUpdateScheduler`, or the tracker's `togglePrayerStatus` behaviour.
- Widget package: `com.arshadshah.nimaz.widget`. App package: `com.arshadshah.nimaz`.
- Verify each task with `./gradlew :app:compileDebugKotlin` (runs KSP + resource processing, so it validates both Kotlin and the new drawable XML referenced by `R.drawable.*`). Develop on branch `feat/widget-ui-redesign`; do not push to `dev`.

---

## File Structure

**Create (drawables, `app/src/main/res/drawable/`):**
- `ic_widget_fajr.xml`, `ic_widget_dhuhr.xml`, `ic_widget_asr.xml`, `ic_widget_maghrib.xml`, `ic_widget_isha.xml` — celestial prayer icons (Next Prayer widget)
- `ic_widget_check.xml` — checkmark (Tracker)
- `ic_widget_crescent.xml` — crescent accent (Hijri Date)
- `ic_widget_event.xml`, `ic_widget_star.xml` — calendar event markers

**Create (color):** add `widget_on_primary` (#FFFFFF) to both `res/values/widget_colors.xml` and `res/values-night/widget_colors.xml`.

**Create (test):** `app/src/test/java/com/arshadshah/nimaz/widget/WidgetGlyphGuardTest.kt`

**Modify:**
- `app/src/main/java/com/arshadshah/nimaz/widget/core/WidgetUi.kt` — add `WidgetCard`, `WidgetIcon`, `WidgetLabel`, `WidgetPill`, `prayerIconRes`
- `widget/nextprayer/NextPrayerWidget.kt`, `widget/hijridate/HijriDateWidget.kt`, `widget/prayertimes/PrayerTimesWidget.kt`, `widget/prayertracker/PrayerTrackerWidget.kt`, `widget/hijricalendar/HijriCalendarWidget.kt` — rewrite the `Success` composable
- `app/src/main/res/layout/widget_*_preview.xml` (5 files) — match new look
- `docs/SUBSYSTEMS.md` — widgets section

---

## Task 1: Vector drawables + on-primary color

**Files:**
- Create: `app/src/main/res/drawable/ic_widget_fajr.xml`, `ic_widget_dhuhr.xml`, `ic_widget_asr.xml`, `ic_widget_maghrib.xml`, `ic_widget_isha.xml`, `ic_widget_check.xml`, `ic_widget_crescent.xml`, `ic_widget_event.xml`, `ic_widget_star.xml`
- Modify: `app/src/main/res/values/widget_colors.xml`, `app/src/main/res/values-night/widget_colors.xml`

**Interfaces:**
- Produces: `R.drawable.ic_widget_{fajr,dhuhr,asr,maghrib,isha,check,crescent,event,star}`, `R.color.widget_on_primary`. All icons are 24×24 monochrome (opaque strokes/fills) so `ColorFilter.tint` recolours them.

- [ ] **Step 1: Create the line-style prayer + accent icons**

`ic_widget_fajr.xml` (dawn — horizon, sun arc, up chevron):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="#FF000000" android:strokeWidth="1.9" android:fillColor="#00000000"
        android:strokeLineCap="round" android:strokeLineJoin="round"
        android:pathData="M3,18 L21,18 M6.5,18 a5.5,5.5 0 0 1 11,0 M12,4 L12,7 M9,8 L12,5 L15,8" />
</vector>
```

`ic_widget_dhuhr.xml` (zenith — full sun + rays):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="#FF000000" android:strokeWidth="1.9" android:fillColor="#00000000"
        android:strokeLineCap="round" android:strokeLineJoin="round"
        android:pathData="M12,7.5 a4.5,4.5 0 1 1 -0.01,0 M12,2 L12,4 M12,20 L12,22 M2,12 L4,12 M20,12 L22,12 M4.9,4.9 L6.3,6.3 M17.7,17.7 L19.1,19.1 M19.1,4.9 L17.7,6.3 M4.9,19.1 L6.3,17.7" />
</vector>
```

`ic_widget_asr.xml` (low sun above horizon):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="#FF000000" android:strokeWidth="1.9" android:fillColor="#00000000"
        android:strokeLineCap="round" android:strokeLineJoin="round"
        android:pathData="M3,20 L21,20 M12,9 a4,4 0 1 1 -0.01,0 M12,3 L12,4.5 M4.5,12 L6,12 M18,12 L19.5,12 M6.2,6.2 L7.2,7.2 M17.8,6.2 L16.8,7.2" />
</vector>
```

`ic_widget_maghrib.xml` (sunset — horizon, sun arc, down chevron):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="#FF000000" android:strokeWidth="1.9" android:fillColor="#00000000"
        android:strokeLineCap="round" android:strokeLineJoin="round"
        android:pathData="M3,18 L21,18 M6.5,18 a5.5,5.5 0 0 1 11,0 M9,7 L12,10 L15,7" />
</vector>
```

`ic_widget_isha.xml` (night — crescent, filled):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FF000000"
        android:pathData="M19,14.5 a7.5,7.5 0 0 1 -9.5,-9.5 a6,6 0 1 0 9.5,9.5 Z" />
</vector>
```

`ic_widget_crescent.xml` (same crescent shape, used by Hijri Date):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FF000000"
        android:pathData="M19,14.5 a7.5,7.5 0 0 1 -9.5,-9.5 a6,6 0 1 0 9.5,9.5 Z" />
</vector>
```

- [ ] **Step 2: Create the check, event and star icons**

`ic_widget_check.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="#FF000000" android:strokeWidth="2.6" android:fillColor="#00000000"
        android:strokeLineCap="round" android:strokeLineJoin="round"
        android:pathData="M5,12.5 L10,17 L19,7" />
</vector>
```

`ic_widget_event.xml` (calendar):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="#FF000000" android:strokeWidth="1.9" android:fillColor="#00000000"
        android:strokeLineCap="round" android:strokeLineJoin="round"
        android:pathData="M5,5 L19,5 a2,2 0 0 1 2,2 L21,19 a2,2 0 0 1 -2,2 L5,21 a2,2 0 0 1 -2,-2 L3,7 a2,2 0 0 1 2,-2 Z M3,10 L21,10 M8,3 L8,7 M16,3 L16,7" />
</vector>
```

`ic_widget_star.xml` (filled star — recommended fast / special day):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FF000000"
        android:pathData="M12,3 L14.5,8.6 L20.5,9.2 L16,13.2 L17.3,19 L12,15.9 L6.7,19 L8,13.2 L3.5,9.2 L9.5,8.6 Z" />
</vector>
```

- [ ] **Step 3: Add the on-primary color (white check on teal disc, identical in both modes)**

In `app/src/main/res/values/widget_colors.xml`, add inside `<resources>`:
```xml
    <color name="widget_on_primary">#FFFFFF</color>
```
In `app/src/main/res/values-night/widget_colors.xml`, add inside `<resources>`:
```xml
    <color name="widget_on_primary">#FFFFFF</color>
```

- [ ] **Step 4: Verify resources compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (resource processing parses every new vector XML and generates `R.drawable.*` / `R.color.widget_on_primary`). If a `pathData` is malformed, the build fails here.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/drawable/ic_widget_*.xml app/src/main/res/values/widget_colors.xml app/src/main/res/values-night/widget_colors.xml
git commit -m "feat(widget): add vector icon set + on-primary color for redesign"
```

---

## Task 2: Shared core atoms in WidgetUi.kt

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/widget/core/WidgetUi.kt`

**Interfaces:**
- Consumes: `R.drawable.ic_widget_*` (Task 1), existing `WidgetPalette`.
- Produces (all in package `com.arshadshah.nimaz.widget.core`):
  - `WidgetCard(background: ColorProvider, onClick: Action, modifier: GlanceModifier = GlanceModifier, cornerRadius: Dp = 16.dp, padding: Dp = 12.dp, content: @Composable () -> Unit)`
  - `WidgetIcon(resId: Int, tint: ColorProvider, size: Dp = 16.dp, contentDescription: String? = null)`
  - `WidgetLabel(text: String, color: ColorProvider, fontSize: TextUnit = 11.sp)`
  - `WidgetPill(container: ColorProvider, modifier: GlanceModifier = GlanceModifier, cornerRadius: Dp = 8.dp, content: @Composable () -> Unit)`
  - `prayerIconRes(prayerName: String): Int`

- [ ] **Step 1: Add imports to WidgetUi.kt**

Add these imports (the file already imports `Dp`, `dp`, `sp`, `GlanceModifier`, `LocalContext`, `Action`, `clickable`, `cornerRadius`, `background`, `Alignment`, `Box`, `Column`, `Spacer`, `fillMaxSize`, `height`, `Text`, `TextStyle`, `ColorProvider`, `R`):
```kotlin
import androidx.compose.ui.unit.TextUnit
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.ColorFilter
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
```

- [ ] **Step 2: Append the new atoms to WidgetUi.kt**

Add at the end of the file:
```kotlin
/**
 * The standard solid, rounded, tappable widget surface. Pass a Column/Row that
 * calls `fillMaxSize()` so `defaultWeight()` distributes inside it.
 */
@Composable
fun WidgetCard(
    background: ColorProvider,
    onClick: Action,
    modifier: GlanceModifier = GlanceModifier,
    cornerRadius: Dp = 16.dp,
    padding: Dp = 12.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(background)
            .cornerRadius(cornerRadius)
            .clickable(onClick)
            .padding(padding)
            .then(modifier),
        content = content,
    )
}

/** The single place widget icons are drawn — a tinted vector drawable. */
@Composable
fun WidgetIcon(
    resId: Int,
    tint: ColorProvider,
    size: Dp = 16.dp,
    contentDescription: String? = null,
) {
    Image(
        provider = ImageProvider(resId),
        contentDescription = contentDescription,
        modifier = GlanceModifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}

/** Small medium-weight caption used for eyebrow labels. */
@Composable
fun WidgetLabel(text: String, color: ColorProvider, fontSize: TextUnit = 11.sp) {
    Text(
        text = text,
        style = TextStyle(color = color, fontSize = fontSize, fontWeight = FontWeight.Medium),
    )
}

/** Rounded badge container for countdowns and the "next prayer" highlight. */
@Composable
fun WidgetPill(
    container: ColorProvider,
    modifier: GlanceModifier = GlanceModifier,
    cornerRadius: Dp = 8.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = GlanceModifier
            .background(container)
            .cornerRadius(cornerRadius)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .then(modifier),
        content = content,
    )
}

/** Maps a prayer name to its celestial drawable. Defaults to the zenith sun. */
fun prayerIconRes(prayerName: String): Int = when (prayerName.trim().lowercase()) {
    "fajr" -> R.drawable.ic_widget_fajr
    "dhuhr", "zuhr" -> R.drawable.ic_widget_dhuhr
    "asr" -> R.drawable.ic_widget_asr
    "maghrib" -> R.drawable.ic_widget_maghrib
    "isha" -> R.drawable.ic_widget_isha
    else -> R.drawable.ic_widget_dhuhr
}
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/widget/core/WidgetUi.kt
git commit -m "feat(widget): add WidgetCard/Icon/Label/Pill atoms + prayerIconRes"
```

---

## Task 3: Next Prayer widget rewrite

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/widget/nextprayer/NextPrayerWidget.kt` (replace `NextPrayerSuccessContent`, lines 92-175)

**Interfaces:**
- Consumes: `WidgetCard`, `WidgetIcon`, `WidgetLabel`, `WidgetPill`, `prayerIconRes` (Task 2); `NextPrayerData(prayerName, prayerTime, countdown, isValid, nextPrayerEpochMillis)`; `WidgetUpdateScheduler.computeCountdown`.

- [ ] **Step 1: Update imports**

In `NextPrayerWidget.kt`, add:
```kotlin
import androidx.glance.layout.width
import com.arshadshah.nimaz.widget.core.WidgetCard
import com.arshadshah.nimaz.widget.core.WidgetIcon
import com.arshadshah.nimaz.widget.core.WidgetLabel
import com.arshadshah.nimaz.widget.core.WidgetPill
import com.arshadshah.nimaz.widget.core.prayerIconRes
```

- [ ] **Step 2: Replace `NextPrayerSuccessContent`**

Replace the entire `NextPrayerSuccessContent` function (lines 92-175) with:
```kotlin
@Composable
private fun NextPrayerSuccessContent(
    data: NextPrayerData,
    backgroundColor: ColorProvider,
    textColor: ColorProvider,
    textSecondary: ColorProvider,
    primaryColor: ColorProvider
) {
    val context = LocalContext.current
    val liveCountdown = if (data.nextPrayerEpochMillis > 0L) {
        WidgetUpdateScheduler.computeCountdown(data.nextPrayerEpochMillis)
    } else {
        data.countdown.ifEmpty { "—" }
    }

    WidgetCard(
        background = backgroundColor,
        onClick = actionStartActivity<MainActivity>(),
        padding = 16.dp,
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WidgetIcon(
                    resId = prayerIconRes(data.prayerName),
                    tint = primaryColor,
                    size = 16.dp,
                    contentDescription = data.prayerName,
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                WidgetLabel(
                    text = context.getString(R.string.widget_next_prayer),
                    color = textSecondary,
                )
            }
            Spacer(modifier = GlanceModifier.height(10.dp))
            Text(
                text = data.prayerName.ifEmpty { "—" },
                style = TextStyle(color = primaryColor, fontSize = 20.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = data.prayerTime.ifEmpty { "—" },
                style = TextStyle(color = textColor, fontSize = 32.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            WidgetPill(container = ColorProvider(R.color.widget_primary_dim)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (data.isValid && liveCountdown != "—") "in " else "",
                        style = TextStyle(color = primaryColor, fontSize = 12.sp),
                    )
                    Text(
                        text = liveCountdown,
                        style = TextStyle(color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 3: Verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. (`Box`, `cornerRadius`, `clickable`, `background`, `fillMaxWidth` imports may now be unused — remove any the compiler flags as unused if it warns; unused imports don't fail the build.)

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/widget/nextprayer/NextPrayerWidget.kt
git commit -m "feat(widget): redesign Next Prayer widget (icon + refined layout)"
```

---

## Task 4: Hijri Date widget rewrite

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/widget/hijridate/HijriDateWidget.kt` (replace `HijriDateSuccessContent`, lines 89-143)

**Interfaces:**
- Consumes: `WidgetCard`, `WidgetIcon`, `WidgetLabel` (Task 2); `R.drawable.ic_widget_crescent`; `HijriDateData(hijriDay: Int, hijriMonth, hijriYear: Int, gregorianDayOfWeek, gregorianDate)`.

- [ ] **Step 1: Update imports**

In `HijriDateWidget.kt`, add:
```kotlin
import androidx.glance.layout.Row
import androidx.glance.layout.width
import com.arshadshah.nimaz.widget.core.WidgetCard
import com.arshadshah.nimaz.widget.core.WidgetIcon
import com.arshadshah.nimaz.widget.core.WidgetLabel
```

- [ ] **Step 2: Replace `HijriDateSuccessContent`**

Replace the entire `HijriDateSuccessContent` function (lines 89-143) with:
```kotlin
@Composable
private fun HijriDateSuccessContent(
    data: HijriDateData,
    backgroundColor: ColorProvider,
    textColor: ColorProvider,
    textSecondary: ColorProvider,
    primaryColor: ColorProvider
) {
    WidgetCard(
        background = backgroundColor,
        onClick = actionStartActivity<MainActivity>(),
        padding = 14.dp,
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = GlanceModifier.defaultWeight())
            Row(verticalAlignment = Alignment.CenterVertically) {
                WidgetIcon(resId = R.drawable.ic_widget_crescent, tint = primaryColor, size = 13.dp)
                Spacer(modifier = GlanceModifier.width(5.dp))
                WidgetLabel(text = data.gregorianDayOfWeek.ifEmpty { "—" }, color = textSecondary)
            }
            Spacer(modifier = GlanceModifier.height(6.dp))
            Text(
                text = data.hijriDay.toString(),
                style = TextStyle(color = primaryColor, fontSize = 52.sp, fontWeight = FontWeight.Bold),
            )
            Text(
                text = "${data.hijriMonth.ifEmpty { "—" }} ${data.hijriYear}",
                style = TextStyle(color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium),
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = data.gregorianDate.ifEmpty { "—" },
                style = TextStyle(color = textSecondary, fontSize = 11.sp),
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
        }
    }
}
```

- [ ] **Step 3: Verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/widget/hijridate/HijriDateWidget.kt
git commit -m "feat(widget): redesign Hijri Date widget (crescent accent + scale)"
```

---

## Task 5: Prayer Times widget rewrite — Clean Pills

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/widget/prayertimes/PrayerTimesWidget.kt` (replace `PrayerTimesSuccessContent` + `PrayerTimeItem`, lines 92-240)

**Interfaces:**
- Consumes: `WidgetCard` (Task 2); `PrayerTimesData(locationName, hijriDate, nextPrayerName, timeUntilNext, fajrTime..ishaTime, fajrPassed..ishaPassed, nextPrayerEpochMillis)`; `WidgetUpdateScheduler.computeCountdown`.
- Produces (file-private): `enum class PrayerCellState { PAST, NEXT, UPCOMING }`, `PrayerPill(...)`.

- [ ] **Step 1: Update imports**

In `PrayerTimesWidget.kt`, add:
```kotlin
import com.arshadshah.nimaz.widget.core.WidgetCard
```

- [ ] **Step 2: Replace `PrayerTimesSuccessContent` and `PrayerTimeItem`**

Replace both functions (lines 92-240) with:
```kotlin
private enum class PrayerCellState { PAST, NEXT, UPCOMING }

@Composable
private fun PrayerTimesSuccessContent(
    data: PrayerTimesData,
    backgroundColor: ColorProvider,
    textColor: ColorProvider,
    textSecondary: ColorProvider,
    primaryColor: ColorProvider
) {
    val liveCountdown = if (data.nextPrayerEpochMillis > 0L) {
        WidgetUpdateScheduler.computeCountdown(data.nextPrayerEpochMillis)
    } else {
        data.timeUntilNext
    }
    val rightLine = buildString {
        if (data.hijriDate.isNotEmpty()) append(data.hijriDate)
        if (data.nextPrayerName.isNotEmpty() && liveCountdown.isNotEmpty() && liveCountdown != "—") {
            if (isNotEmpty()) append(" · ")
            append("${data.nextPrayerName} in $liveCountdown")
        }
    }.ifEmpty { "—" }

    // Five (name, time, passed) cells in order; the next prayer is the first not-passed.
    val cells = listOf(
        Triple("Fajr", data.fajrTime, data.fajrPassed),
        Triple("Dhuhr", data.dhuhrTime, data.dhuhrPassed),
        Triple("Asr", data.asrTime, data.asrPassed),
        Triple("Maghrib", data.maghribTime, data.maghribPassed),
        Triple("Isha", data.ishaTime, data.ishaPassed),
    )
    val nextIndex = cells.indexOfFirst { !it.third }

    WidgetCard(
        background = backgroundColor,
        onClick = actionStartActivity<MainActivity>(),
        padding = 12.dp,
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = data.locationName.ifEmpty { "Location" },
                    style = TextStyle(color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.defaultWeight(),
                )
                Text(
                    text = rightLine,
                    style = TextStyle(color = textSecondary, fontSize = 10.sp),
                    maxLines = 1,
                )
            }
            Spacer(modifier = GlanceModifier.height(8.dp))
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                cells.forEachIndexed { index, (name, time, passed) ->
                    val state = when {
                        passed -> PrayerCellState.PAST
                        index == nextIndex -> PrayerCellState.NEXT
                        else -> PrayerCellState.UPCOMING
                    }
                    PrayerPill(
                        name = name,
                        time = time.ifEmpty { "—" },
                        state = state,
                        textColor = textColor,
                        textSecondary = textSecondary,
                        primaryColor = primaryColor,
                        modifier = GlanceModifier.defaultWeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PrayerPill(
    name: String,
    time: String,
    state: PrayerCellState,
    textColor: ColorProvider,
    textSecondary: ColorProvider,
    primaryColor: ColorProvider,
    modifier: GlanceModifier = GlanceModifier,
) {
    val onPrimary = ColorProvider(R.color.widget_on_primary)
    val nameColor = if (state == PrayerCellState.NEXT) onPrimary else textSecondary
    val timeColor = when (state) {
        PrayerCellState.PAST -> textSecondary
        PrayerCellState.NEXT -> onPrimary
        PrayerCellState.UPCOMING -> textColor
    }
    val inner = GlanceModifier.let {
        if (state == PrayerCellState.NEXT) {
            it.background(primaryColor).cornerRadius(12.dp).padding(vertical = 6.dp, horizontal = 4.dp)
        } else it
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(modifier = inner.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = name,
                style = TextStyle(color = nameColor, fontSize = 10.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
            )
            Text(
                text = time,
                style = TextStyle(color = timeColor, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
        }
    }
}
```

- [ ] **Step 3: Verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/widget/prayertimes/PrayerTimesWidget.kt
git commit -m "feat(widget): redesign Prayer Times widget (clean pills, next highlighted)"
```

---

## Task 6: Prayer Tracker widget rewrite — checkbox + glyph guard test

**Files:**
- Create: `app/src/test/java/com/arshadshah/nimaz/widget/WidgetGlyphGuardTest.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/widget/prayertracker/PrayerTrackerWidget.kt` (replace `PrayerTrackerSuccessContent` + `PrayerCheckbox`, lines 114-243). Leave `togglePrayerStatus`, the receiver and `provideGlance` untouched.

**Interfaces:**
- Consumes: `WidgetCard`, `WidgetIcon` (Task 2); `R.drawable.ic_widget_check`; `R.color.widget_on_primary`; `PrayerTrackerData(dateLabel, fajr, dhuhr, asr, maghrib, isha, prayedCount, totalCount)`; existing `togglePrayerStatus(context, prayerName)`.

- [ ] **Step 1: Write the failing glyph-guard test**

Create `app/src/test/java/com/arshadshah/nimaz/widget/WidgetGlyphGuardTest.kt`:
```kotlin
package com.arshadshah.nimaz.widget

import org.junit.Test
import java.io.File

/**
 * Regression guard: widget UI must use real vector drawables, never emoji or
 * unicode symbol glyphs. The em-dash (U+2014) is allowed as a text fallback.
 * Runs from the module dir, so source paths are relative to `app/`.
 */
class WidgetGlyphGuardTest {

    private val forbidden = setOf('✓', '✔', '✅', '☆', '★', '→', '←')

    @Test
    fun `widget sources contain no emoji or symbol glyphs`() {
        val dir = File("src/main/java/com/arshadshah/nimaz/widget")
        assert(dir.isDirectory) { "Widget source dir not found at ${dir.absolutePath}" }

        val offenders = mutableListOf<String>()
        dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
            file.readLines().forEachIndexed { i, line ->
                line.forEach { ch ->
                    val code = ch.code
                    val isSymbol = code in 0x2600..0x27BF // misc symbols + dingbats (incl. ✓)
                    val isEmoji = code in 0x1F000..0x1FAFF || Character.isSurrogate(ch)
                    if (ch in forbidden || isSymbol || isEmoji) {
                        offenders += "%s:%d U+%04X '%s'".format(file.name, i + 1, code, ch)
                    }
                }
            }
        }
        assert(offenders.isEmpty()) {
            "Forbidden glyph(s) in widget sources:\n" + offenders.joinToString("\n")
        }
    }
}
```

- [ ] **Step 2: Run the test to verify it FAILS**

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.widget.WidgetGlyphGuardTest"`
Expected: FAIL — reports `PrayerTrackerWidget.kt:222 U+2713 '✓'` (the current checkmark glyph).

- [ ] **Step 3: Update imports in PrayerTrackerWidget.kt**

Add:
```kotlin
import com.arshadshah.nimaz.widget.core.WidgetCard
import com.arshadshah.nimaz.widget.core.WidgetIcon
```

- [ ] **Step 4: Replace `PrayerTrackerSuccessContent` and `PrayerCheckbox`**

Replace both functions (lines 114-243) with:
```kotlin
@Composable
private fun PrayerTrackerSuccessContent(
    context: Context,
    data: PrayerTrackerData,
    backgroundColor: ColorProvider,
    textColor: ColorProvider,
    textSecondary: ColorProvider,
    primaryColor: ColorProvider
) {
    WidgetCard(
        background = backgroundColor,
        onClick = actionStartActivity<MainActivity>(),
        padding = 12.dp,
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = data.dateLabel,
                    style = TextStyle(color = textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = "${data.prayedCount} / ${data.totalCount}",
                    style = TextStyle(color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                )
            }
            Spacer(modifier = GlanceModifier.height(8.dp))
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                val prayers = listOf(
                    "Fajr" to data.fajr,
                    "Dhuhr" to data.dhuhr,
                    "Asr" to data.asr,
                    "Maghrib" to data.maghrib,
                    "Isha" to data.isha,
                )
                prayers.forEach { (name, isPrayed) ->
                    PrayerCheckbox(
                        prayerName = name,
                        isPrayed = isPrayed,
                        context = context,
                        backgroundColor = backgroundColor,
                        primaryColor = primaryColor,
                        textColor = textColor,
                        textSecondary = textSecondary,
                        modifier = GlanceModifier.defaultWeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PrayerCheckbox(
    prayerName: String,
    isPrayed: Boolean,
    context: Context,
    backgroundColor: ColorProvider,
    primaryColor: ColorProvider,
    textColor: ColorProvider,
    textSecondary: ColorProvider,
    modifier: GlanceModifier = GlanceModifier
) {
    val uncheckedColor = ColorProvider(R.color.widget_unchecked)
    val onPrimary = ColorProvider(R.color.widget_on_primary)
    Column(
        modifier = modifier.clickable { togglePrayerStatus(context, prayerName.lowercase()) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isPrayed) {
            // Filled teal disc + tinted check vector.
            Box(
                modifier = GlanceModifier.size(28.dp).cornerRadius(14.dp).background(primaryColor),
                contentAlignment = Alignment.Center,
            ) {
                WidgetIcon(resId = R.drawable.ic_widget_check, tint = onPrimary, size = 16.dp)
            }
        } else {
            // Outline ring built from two discs (Glance has no stroke modifier).
            Box(
                modifier = GlanceModifier.size(28.dp).cornerRadius(14.dp).background(uncheckedColor),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = GlanceModifier.size(24.dp).cornerRadius(12.dp).background(backgroundColor)) {}
            }
        }
        Spacer(modifier = GlanceModifier.height(5.dp))
        Text(
            text = prayerName,
            style = TextStyle(
                color = if (isPrayed) textColor else textSecondary,
                fontSize = 9.sp,
                fontWeight = if (isPrayed) FontWeight.Bold else FontWeight.Normal,
            ),
            maxLines = 1,
        )
    }
}
```

- [ ] **Step 5: Run the glyph guard + compile**

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.widget.WidgetGlyphGuardTest"`
Expected: PASS (the `✓` is gone; em-dash fallbacks in other widget files are allowed).
Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/test/java/com/arshadshah/nimaz/widget/WidgetGlyphGuardTest.kt app/src/main/java/com/arshadshah/nimaz/widget/prayertracker/PrayerTrackerWidget.kt
git commit -m "feat(widget): redesign Prayer Tracker checkbox + add glyph-guard test"
```

---

## Task 7: Hijri Calendar event icons

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/widget/hijricalendar/HijriCalendarWidget.kt` (replace `EventRow`, lines 394-431)

**Interfaces:**
- Consumes: `WidgetIcon` (Task 2); `R.drawable.ic_widget_event`, `R.drawable.ic_widget_star`; `HijriCalendarEventData(name, nameArabic, type)`.

- [ ] **Step 1: Update imports**

In `HijriCalendarWidget.kt`, add:
```kotlin
import com.arshadshah.nimaz.widget.core.WidgetIcon
```

- [ ] **Step 2: Replace `EventRow`**

Replace the entire `EventRow` function (lines 394-431) with:
```kotlin
@Composable
private fun EventRow(
    event: HijriCalendarEventData,
    textColor: ColorProvider,
    textSecondary: ColorProvider,
    primaryColor: ColorProvider,
) {
    val iconRes = if (
        event.type.contains("fast", ignoreCase = true) ||
        event.type.contains("recommend", ignoreCase = true)
    ) R.drawable.ic_widget_star else R.drawable.ic_widget_event

    Row(verticalAlignment = Alignment.Top) {
        Box(modifier = GlanceModifier.padding(top = 1.dp)) {
            WidgetIcon(resId = iconRes, tint = primaryColor, size = 12.dp)
        }
        Spacer(modifier = GlanceModifier.width(6.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = event.name,
                style = TextStyle(color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Medium),
                maxLines = 2,
            )
            Text(
                text = event.type.replace("_", " ").lowercase()
                    .replaceFirstChar { it.uppercase() },
                style = TextStyle(color = textSecondary, fontSize = 9.sp),
                maxLines = 1,
            )
        }
    }
}
```

- [ ] **Step 3: Verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/widget/hijricalendar/HijriCalendarWidget.kt
git commit -m "feat(widget): Hijri Calendar events use vector icons instead of dots"
```

---

## Task 8: Update widget preview layouts

**Files:**
- Modify: `app/src/main/res/layout/widget_next_prayer_preview.xml`, `widget_hijri_date_preview.xml`, `widget_prayer_times_preview.xml`, `widget_prayer_tracker_preview.xml`, `widget_hijri_calendar_preview.xml`

These static RemoteViews previews appear in the launcher's widget picker. Update them to echo the new look. They support `ImageView` with `android:tint` for icons.

- [ ] **Step 1: Update the Next Prayer preview**

Replace `app/src/main/res/layout/widget_next_prayer_preview.xml` with:
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:ignore="HardcodedText,ContentDescription"
    android:orientation="vertical"
    android:background="@color/widget_background"
    android:padding="16dp">

    <LinearLayout
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical">
        <ImageView
            android:layout_width="16dp"
            android:layout_height="16dp"
            android:src="@drawable/ic_widget_maghrib"
            android:tint="@color/widget_primary" />
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="6dp"
            android:text="Next Prayer"
            android:textColor="@color/widget_text_secondary"
            android:textSize="11sp" />
    </LinearLayout>

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="10dp"
        android:text="Maghrib"
        android:textColor="@color/widget_primary"
        android:textSize="20sp"
        android:textStyle="bold" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:text="6:15 PM"
        android:textColor="@color/widget_text"
        android:textSize="30sp"
        android:textStyle="bold" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:background="@color/widget_primary_dim"
        android:paddingHorizontal="12dp"
        android:paddingVertical="6dp"
        android:text="in 2h 30m"
        android:textColor="@color/widget_primary"
        android:textSize="12sp"
        android:textStyle="bold" />
</LinearLayout>
```

- [ ] **Step 2: Update the Hijri Date preview**

Replace `app/src/main/res/layout/widget_hijri_date_preview.xml` with:
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:ignore="HardcodedText,ContentDescription"
    android:orientation="vertical"
    android:background="@color/widget_background"
    android:gravity="center"
    android:padding="14dp">

    <LinearLayout
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical">
        <ImageView
            android:layout_width="13dp"
            android:layout_height="13dp"
            android:src="@drawable/ic_widget_crescent"
            android:tint="@color/widget_primary" />
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="5dp"
            android:text="Monday"
            android:textColor="@color/widget_text_secondary"
            android:textSize="11sp" />
    </LinearLayout>

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="15"
        android:textColor="@color/widget_primary"
        android:textSize="52sp"
        android:textStyle="bold" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Muharram 1446"
        android:textColor="@color/widget_text"
        android:textSize="14sp"
        android:textStyle="bold" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="4dp"
        android:text="June 22, 2024"
        android:textColor="@color/widget_text_secondary"
        android:textSize="11sp" />
</LinearLayout>
```

- [ ] **Step 3: Update the Prayer Times preview**

Replace `app/src/main/res/layout/widget_prayer_times_preview.xml` with:
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:ignore="HardcodedText"
    android:orientation="vertical"
    android:background="@color/widget_background"
    android:padding="12dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal">
        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="Dublin"
            android:textColor="@color/widget_text"
            android:textSize="13sp"
            android:textStyle="bold" />
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="15 Muharram · Maghrib in 2h 30m"
            android:textColor="@color/widget_text_secondary"
            android:textSize="10sp" />
    </LinearLayout>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:orientation="horizontal"
        android:weightSum="5">
        <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical" android:gravity="center">
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Fajr" android:textColor="@color/widget_text_secondary" android:textSize="10sp" />
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="5:02" android:textColor="@color/widget_text_secondary" android:textSize="15sp" android:textStyle="bold" />
        </LinearLayout>
        <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical" android:gravity="center">
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Dhuhr" android:textColor="@color/widget_text_secondary" android:textSize="10sp" />
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="1:18" android:textColor="@color/widget_text_secondary" android:textSize="15sp" android:textStyle="bold" />
        </LinearLayout>
        <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical" android:gravity="center">
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Asr" android:textColor="@color/widget_text_secondary" android:textSize="10sp" />
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="4:45" android:textColor="@color/widget_text_secondary" android:textSize="15sp" android:textStyle="bold" />
        </LinearLayout>
        <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical" android:gravity="center"
            android:background="@color/widget_primary" android:paddingVertical="6dp">
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Maghrib" android:textColor="@color/widget_on_primary" android:textSize="10sp" />
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="6:15" android:textColor="@color/widget_on_primary" android:textSize="15sp" android:textStyle="bold" />
        </LinearLayout>
        <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical" android:gravity="center">
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Isha" android:textColor="@color/widget_text_secondary" android:textSize="10sp" />
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="7:58" android:textColor="@color/widget_text" android:textSize="15sp" android:textStyle="bold" />
        </LinearLayout>
    </LinearLayout>
</LinearLayout>
```

- [ ] **Step 4: Update the Prayer Tracker preview**

Replace `app/src/main/res/layout/widget_prayer_tracker_preview.xml` with:
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:ignore="HardcodedText,ContentDescription,UseCompoundDrawables"
    android:orientation="vertical"
    android:background="@color/widget_background"
    android:padding="12dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal">
        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="Today · 15 Muharram"
            android:textColor="@color/widget_text_secondary"
            android:textSize="11sp" />
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="3 / 5"
            android:textColor="@color/widget_primary"
            android:textSize="12sp"
            android:textStyle="bold" />
    </LinearLayout>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:orientation="horizontal"
        android:weightSum="5">
        <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical" android:gravity="center">
            <ImageView android:layout_width="28dp" android:layout_height="28dp" android:src="@drawable/ic_widget_check" android:background="@color/widget_primary" android:tint="@color/widget_on_primary" android:padding="6dp" />
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Fajr" android:textColor="@color/widget_text" android:textSize="9sp" android:textStyle="bold" />
        </LinearLayout>
        <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical" android:gravity="center">
            <ImageView android:layout_width="28dp" android:layout_height="28dp" android:src="@drawable/ic_widget_check" android:background="@color/widget_primary" android:tint="@color/widget_on_primary" android:padding="6dp" />
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Dhuhr" android:textColor="@color/widget_text" android:textSize="9sp" android:textStyle="bold" />
        </LinearLayout>
        <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical" android:gravity="center">
            <ImageView android:layout_width="28dp" android:layout_height="28dp" android:src="@drawable/ic_widget_check" android:background="@color/widget_primary" android:tint="@color/widget_on_primary" android:padding="6dp" />
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Asr" android:textColor="@color/widget_text" android:textSize="9sp" android:textStyle="bold" />
        </LinearLayout>
        <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical" android:gravity="center">
            <View android:layout_width="28dp" android:layout_height="28dp" android:background="@color/widget_unchecked" />
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Maghrib" android:textColor="@color/widget_text_secondary" android:textSize="9sp" />
        </LinearLayout>
        <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical" android:gravity="center">
            <View android:layout_width="28dp" android:layout_height="28dp" android:background="@color/widget_unchecked" />
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Isha" android:textColor="@color/widget_text_secondary" android:textSize="9sp" />
        </LinearLayout>
    </LinearLayout>
</LinearLayout>
```

- [ ] **Step 5: Leave the Hijri Calendar preview as-is or lightly refresh**

The `widget_hijri_calendar_preview.xml` layout structure is fine; if it references no removed resources it needs no change. Open it and confirm it still references only existing `@color/widget_*` and (optionally) the new event icons. If it shows event "dots", optionally swap one `View` dot for `<ImageView android:src="@drawable/ic_widget_event" android:tint="@color/widget_primary" .../>` to match — otherwise no edit needed.

- [ ] **Step 6: Verify previews compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (resource processing validates all five preview layouts and their `@drawable`/`@color` references).

- [ ] **Step 7: Commit**

```bash
git add app/src/main/res/layout/widget_*_preview.xml
git commit -m "feat(widget): refresh picker preview layouts to match redesign"
```

---

## Task 9: Update documentation

**Files:**
- Modify: `docs/SUBSYSTEMS.md` (Glance widgets section, around lines 65-94)

- [ ] **Step 1: Read the current widgets section**

Read `docs/SUBSYSTEMS.md` lines 65-95 to confirm current wording before editing.

- [ ] **Step 2: Update the shared-core sentence (line ~89)**

Find the sentence beginning ``**Shared `widget/core/`.**`` and update the `WidgetUi.kt` parenthetical from:
```
`WidgetUi.kt` (`WidgetPalette`, `WidgetMessageBox`, `WidgetLoadingBox`)
```
to:
```
`WidgetUi.kt` (`WidgetPalette`, `WidgetMessageBox`, `WidgetLoadingBox`, plus the redesign atoms `WidgetCard`, `WidgetIcon`, `WidgetLabel`, `WidgetPill`, `prayerIconRes`)
```

- [ ] **Step 3: Add a UI-design note**

Immediately after the "Shared `widget/core/`" paragraph, add:
```markdown

**Widget UI design ("Refined Minimal").** Solid `widget_background` surface, `16dp`
corners, teal `widget_primary` accent. **No emoji/ASCII/unicode glyphs** — all icons
are monochrome vector drawables in `res/drawable/ic_widget_*.xml` drawn via
`WidgetIcon` (`Image` + `ColorFilter.tint`), so they follow light/dark + accent. Per
prayer the Next Prayer widget picks a celestial icon via `prayerIconRes`; the Tracker
uses a teal disc + `ic_widget_check` when prayed and a two-disc outline ring when not
(Glance has no stroke modifier); Prayer Times is a clean 5-cell pill row with the next
prayer filled teal and past prayers dimmed. A JVM regression test
(`WidgetGlyphGuardTest`) fails the build if any widget source reintroduces a glyph.
```

- [ ] **Step 4: Verify docs reference reality**

Confirm the names in the new prose (`WidgetCard`, `WidgetIcon`, `prayerIconRes`, `ic_widget_check`, `WidgetGlyphGuardTest`) all exist from earlier tasks.

- [ ] **Step 5: Commit**

```bash
git add docs/SUBSYSTEMS.md
git commit -m "docs: document Refined Minimal widget UI + new core atoms"
```

---

## Final verification

- [ ] **Run the full gates**

Run: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL; `WidgetGlyphGuardTest` passes.

- [ ] **Manual smoke test (device/emulator, JDK 21 + Android SDK)**

Add each of the five widgets to the launcher in **light** and **dark** mode and confirm:
1. No glyphs render anywhere; icons are crisp and tinted teal/white correctly.
2. Next Prayer shows the correct celestial icon for the upcoming prayer.
3. Prayer Times highlights the next prayer (teal pill) and dims past prayers.
4. Tapping a Tracker checkbox toggles it, persists, and the widget refreshes.
5. Tapping the Hijri Calendar opens the Islamic Calendar screen (deep link intact).

---

## Self-Review (completed during planning)

- **Spec coverage:** Design language §2 → Task 2 (atoms) + all widget tasks. Iconography §3 → Task 1 (drawables) + Task 2 (`prayerIconRes`). Color §4 → Task 1 (`widget_on_primary`), reuse elsewhere. Core §5 → Task 2. Per-widget §6.1–6.5 → Tasks 3,4,5,6,7. Previews §7 → Task 8. Verification §9 → Final verification. Docs §10 → Task 9. Glyph elimination (global constraint) → Task 6 guard test. All sections covered.
- **Placeholder scan:** No TBD/TODO; every code/XML step is complete and concrete.
- **Type consistency:** `WidgetCard`/`WidgetIcon`/`WidgetLabel`/`WidgetPill`/`prayerIconRes` signatures defined in Task 2 are used with matching arguments in Tasks 3–7. `widget_on_primary` created in Task 1, used in Tasks 5, 6, 8. `PrayerCellState` defined and used within Task 5 only.
```
