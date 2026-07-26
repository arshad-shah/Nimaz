package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.util.CountdownParts
import com.arshadshah.nimaz.core.util.CountdownUnit
import com.arshadshah.nimaz.core.util.EventProximity
import com.arshadshah.nimaz.core.util.formatClockTime
import kotlin.time.Duration
import com.arshadshah.nimaz.presentation.theme.LocalUse24HourFormat
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.time.Instant

/**
 * Localized countdown text.
 *
 * ## What changed and why
 *
 * The old path was `Instant → String` in a ViewModel, then `String → digits`
 * again in `CountdownTimer`, which split on spaces and matched the literal
 * suffixes `"h"`, `"m"` and `"s"`:
 *
 * ```
 * part.endsWith("h") -> hours = part.dropLast(1).padStart(2, '0')
 * ```
 *
 * That is the one time-display path in the app still hardcoded to Latin unit
 * letters, while [formatClockTime] is deliberately locale-aware — so it breaks
 * the moment the countdown strings are translated into any of the six shipped
 * locales, and it breaks on Arabic-Indic digits regardless.
 *
 * Numbers now travel as numbers. Only this file turns them into text, and it
 * does so through string resources.
 *
 * ## String resources to add
 *
 * ```xml
 * <string name="countdown_hms">%1$d h %2$d m %3$d s</string>
 * <string name="countdown_ms">%1$d m %2$d s</string>
 * <string name="countdown_s">%1$d s</string>
 * <string name="countdown_hm">%1$d h %2$d m</string>
 * <string name="countdown_m">%1$d m</string>
 * <string name="countdown_now">Now</string>
 * ```
 *
 * Translators can then reorder or re-suffix per locale without any parser
 * caring — which the old design made impossible.
 */
@Composable
fun countdownText(parts: CountdownParts, showSeconds: Boolean = true): String = when {
    parts.elapsed -> stringResource(R.string.countdown_now)

    showSeconds -> when (parts.leadUnit) {
        CountdownUnit.HOURS ->
            stringResource(R.string.countdown_hms, parts.hours, parts.minutes, parts.seconds)
        CountdownUnit.MINUTES ->
            stringResource(R.string.countdown_ms, parts.minutes, parts.seconds)
        CountdownUnit.SECONDS ->
            stringResource(R.string.countdown_s, parts.seconds)
    }

    else -> when (parts.leadUnit) {
        CountdownUnit.HOURS ->
            stringResource(R.string.countdown_hm, parts.hours, parts.minutes)
        else ->
            stringResource(R.string.countdown_m, parts.minutes)
    }
}

/**
 * A live countdown to [target]. Ticks itself — the caller passes an instant and
 * nothing else, so no ViewModel needs to hold a formatted string or run a loop.
 *
 * ## Tick resolution follows [showSeconds]
 *
 * The two must agree, and deriving one from the other is the only way to guarantee it. Ticking
 * every minute while rendering a seconds digit is not a cheaper countdown — it is a **visibly
 * broken** one: the seconds freeze for a minute and then jump 60 at once, which reads as "the timer
 * stopped" and appears to fix itself whenever something else recomposes the screen. That was the
 * Home hero's bug.
 *
 * So a seconds-showing countdown ticks at 1 Hz however far out the target is (the cost is one
 * `Text` re-measuring per second), and a minute-granularity countdown invalidates once a minute.
 */
@Composable
fun NimazCountdownText(
    target: Instant,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    showSeconds: Boolean = true,
    textAlign: TextAlign? = null,
) {
    val parts by rememberCountdownTo(
        target = target,
        // Showing seconds means needing seconds — at every distance, not just the last quarter-hour.
        fineGrainedWithin = if (showSeconds) Duration.INFINITE else EventProximity.IMMINENT_THRESHOLD,
    )
    Text(
        text = countdownText(parts, showSeconds = showSeconds),
        style = style,
        color = color,
        textAlign = textAlign,
        modifier = modifier,
    )
}

/**
 * A wall-clock time, honouring the user's 12/24-hour preference.
 *
 * ## Why this belongs here rather than in a ViewModel
 *
 * Four ViewModels (`Home`, `PrayerTimes`, `Fasting`, `MonthlyPrayerTimes`) each
 * keep a private `var use24HourFormat` mirrored from
 * `settingsRepository.use24HourFormat.collect {}`, *and* the theme already
 * publishes `LocalUse24HourFormat`. Two sources of truth for one boolean.
 *
 * The mirrors also make the toggle expensive and laggy:
 *
 *  - `HomeViewModel.observeTimeFormat()` calls `calculatePrayerTimes()` on every
 *    change — two full astronomical passes plus `refreshWorshipCard()`'s ~30
 *    DataStore reads, to reformat a string. It fires once on first collection
 *    too, adding another racing recompute at startup.
 *  - `FastingViewModel` reformats by reloading prayer times outright.
 *  - The worship card only re-renders on its 60 s loop, so after toggling the
 *    preference its time can display in the old format for up to a minute.
 *
 * Formatting at the leaf from the CompositionLocal makes the toggle a pure
 * recomposition: instant, free, and correct everywhere at once. The four VM
 * mirrors and `observeTimeFormat()` can then be deleted.
 */
/**
 * The wall-clock time of [instant] as a **string**, honouring the user's 12/24-hour preference.
 *
 * Same source of truth as [NimazClockText] — use this where the caller needs the text itself (a
 * row that styles it, a content description) rather than a `Text`. Because it reads
 * [LocalUse24HourFormat] at the leaf, toggling the preference is a pure recomposition; no
 * ViewModel needs to mirror the boolean or re-run a calculation to reformat.
 */
@Composable
fun clockTimeText(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String {
    val use24Hour = LocalUse24HourFormat.current
    val local = remember(instant, zone) {
        LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(instant.toEpochMilliseconds()),
            zone,
        )
    }
    return formatClockTime(local.hour, local.minute, use24Hour)
}

@Composable
fun NimazClockText(
    instant: Instant,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val use24Hour = LocalUse24HourFormat.current
    val local = remember(instant, zone) {
        LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(instant.toEpochMilliseconds()),
            zone,
        )
    }
    Text(
        text = formatClockTime(local.hour, local.minute, use24Hour),
        style = style,
        color = color,
        modifier = modifier,
    )
}
