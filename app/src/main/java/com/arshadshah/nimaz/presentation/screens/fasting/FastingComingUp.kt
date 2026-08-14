package com.arshadshah.nimaz.presentation.screens.fasting

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.core.util.formatDayMonth
import com.arshadshah.nimaz.domain.model.FastRecord
import com.arshadshah.nimaz.domain.model.FastStatus
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazDivider
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * One upcoming fast worth knowing about.
 *
 * @param whenLabel already resolved to "Today" / "Tomorrow" / "in 4 days" / a date, because the
 *   phrasing needs `stringResource` and this is built inside composition anyway.
 */
data class ComingUpFast(
    val whenLabel: String,
    val name: String,
    val why: String,
    val date: LocalDate,
    val isLogged: Boolean,
)

/**
 * The upcoming voluntary fasts: the weekly sunnah days, Ayyām al-Bīḍ, and the nearest occurrences
 * of the fixed Hijri ones.
 *
 * The derivation is **moved unchanged** from the old `RecommendedFastsSection`. The redesign
 * rewrites how these are presented, not which days they are — changing the Hijri arithmetic in the
 * same commit as the layout would make any regression impossible to attribute.
 *
 * @param daysUntilAyyamAlBeed counted by the ViewModel, which is where the clock and the user's
 *   Hijri offset both live. Computing it here would read the clock at composition and ignore the
 *   offset, which is the bug it was moved out of this file to fix.
 */
@Composable
fun rememberComingUpFasts(
    records: List<FastRecord>,
    daysUntilAyyamAlBeed: Int,
    today: LocalDate,
): List<ComingUpFast> {
    val todayText = stringResource(R.string.fasting_today)
    val tomorrowText = stringResource(R.string.fasting_tomorrow)

    val fastedDates = remember(records) {
        records.filter { it.status == FastStatus.FASTED }
            .map { LocalDate.ofEpochDay(it.date / MILLIS_PER_DAY) }
            .toSet()
    }

    val nextMonday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
    val mondayText = if (nextMonday == today) todayText else stringResource(
        R.string.fasting_next_format,
        nextMonday.formatDayMonth()
    )

    val nextThursday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY))
    val thursdayText = if (nextThursday == today) todayText else stringResource(
        R.string.fasting_next_format,
        nextThursday.formatDayMonth()
    )

    val ayyamText = when {
        daysUntilAyyamAlBeed == 0 -> todayText
        daysUntilAyyamAlBeed == 1 -> tomorrowText
        else -> pluralStringResource(
            R.plurals.fasting_in_days_format,
            daysUntilAyyamAlBeed,
            daysUntilAyyamAlBeed
        )
    }

    val ayyamDate = remember(today) { nextAyyamAlBeed(today) }

    val sunnahDesc = stringResource(R.string.fasting_sunnah_desc)
    val mondayName = stringResource(R.string.fasting_monday)
    val thursdayName = stringResource(R.string.fasting_thursday)
    val ayyamName = stringResource(R.string.fasting_ayyam_al_beed)
    val ayyamDesc = stringResource(R.string.fasting_ayyam_desc)

    val ashuraName = stringResource(R.string.fasting_event_ashura_name)
    val ashuraDesc = stringResource(R.string.fasting_event_ashura_description)
    val arafahName = stringResource(R.string.fasting_event_arafah_name)
    val arafahDesc = stringResource(R.string.fasting_event_arafah_description)
    val shawwalName = stringResource(R.string.fasting_event_shawwal_name)
    val shawwalDesc = stringResource(R.string.fasting_event_shawwal_description)
    val midShabanName = stringResource(R.string.fasting_event_mid_shaban_name)
    val midShabanDesc = stringResource(R.string.fasting_event_mid_shaban_description)

    val hijriToday = remember(today) { HijriDateCalculator.toHijri(today) }
    val islamicFasts = remember(hijriToday.year, today) {
        val events = HijriDateCalculator.getIslamicEvents(hijriToday.year) +
                HijriDateCalculator.getIslamicEvents(hijriToday.year + 1)

        buildList {
            // Events span two Hijri years, so keep only the nearest upcoming occurrence of each
            // — otherwise the same day appears twice.
            events.filter { it.name == "Day of Ashura" }
                .map { it.toGregorianDate() }
                .filter { !it.isBefore(today) }
                .minOrNull()
                ?.let { add(Triple(ashuraName, it, ashuraDesc)) }

            events.filter { it.name == "Day of Arafah" }
                .map { it.toGregorianDate() }
                .filter { !it.isBefore(today) }
                .minOrNull()
                ?.let { add(Triple(arafahName, it, arafahDesc)) }

            runCatching { HijriDateCalculator.toGregorian(2, 10, hijriToday.year) }
                .getOrNull()
                ?.takeIf { !it.isBefore(today) }
                ?.let { add(Triple(shawwalName, it, shawwalDesc)) }

            runCatching { HijriDateCalculator.toGregorian(15, 8, hijriToday.year) }
                .getOrNull()
                ?.takeIf { !it.isBefore(today) }
                ?.let { add(Triple(midShabanName, it, midShabanDesc)) }
        }.sortedBy { it.second }.take(3)
    }

    // Sorted, because "Coming up" is a promise about order. Built unsorted, this listed next
    // Monday before today — the weekly days are added in fixed Mon/Thu sequence, which is only
    // chronological for part of the week.
    return buildList {
        add(
            ComingUpFast(
                whenLabel = mondayText,
                name = mondayName,
                why = sunnahDesc,
                date = nextMonday,
                isLogged = nextMonday in fastedDates,
            )
        )
        add(
            ComingUpFast(
                whenLabel = thursdayText,
                name = thursdayName,
                why = sunnahDesc,
                date = nextThursday,
                isLogged = nextThursday in fastedDates,
            )
        )
        add(
            ComingUpFast(
                whenLabel = ayyamText,
                name = ayyamName,
                why = ayyamDesc,
                date = ayyamDate,
                isLogged = ayyamDate in fastedDates,
            )
        )
        islamicFasts.forEach { (name, date, why) ->
            val daysUntil = ChronoUnit.DAYS.between(today, date).toInt()
            add(
                ComingUpFast(
                    whenLabel = when (daysUntil) {
                        0 -> todayText
                        1 -> tomorrowText
                        else -> date.formatDayMonth()
                    },
                    name = name,
                    why = why,
                    date = date,
                    isLogged = date in fastedDates,
                )
            )
        }
    }.sortedBy { it.date }
}

/**
 * The next 13th of a Hijri month — the first of the three white days.
 *
 * Falls back to [today] if the conversion throws, which it can near a month boundary: showing the
 * card anchored to today is better than dropping a recommended fast off the list entirely.
 */
private fun nextAyyamAlBeed(today: LocalDate): LocalDate {
    val hijri = HijriDateCalculator.toHijri(today)
    val month = if (hijri.day > 15) {
        if (hijri.month == 12) 1 else hijri.month + 1
    } else {
        hijri.month
    }
    val year = if (hijri.day > 15 && hijri.month == 12) hijri.year + 1 else hijri.year
    return runCatching { HijriDateCalculator.toGregorian(13, month, year) }.getOrDefault(today)
}

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

/** Fixed width so the cards peek — a row of equal cards that fills the screen looks like a list. */
private val ComingUpCardWidth = 172.dp

/**
 * The upcoming fasts as a horizontally scrolling row.
 *
 * Was a vertical list folded behind a "Go deeper" row. Sideways and always visible, because these
 * are the days someone opens this screen to plan for, and a list that has to be revealed first is
 * a list most people never see.
 */
@Composable
fun ComingUpRow(
    fasts: List<ComingUpFast>,
    onLogFast: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(fasts, key = { it.name + it.date }) { fast ->
            ComingUpCard(fast = fast, onClick = { onLogFast(fast.date) })
        }
    }
}

@Composable
private fun ComingUpCard(
    fast: ComingUpFast,
    onClick: () -> Unit,
) {
    NimazCard(
        onClick = onClick,
        modifier = Modifier.width(ComingUpCardWidth),
        style = NimazCardStyle.ELEVATED,
        shape = RoundedCornerShape(16.dp),
        tone = NimazTone.NEUTRAL,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = fast.whenLabel.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = fast.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = fast.why,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 2,
                maxLines = 2,
            )
            Spacer(modifier = Modifier.height(9.dp))
            NimazDivider(alpha = 0.5f)
            Spacer(modifier = Modifier.height(9.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                NimazIcon(
                    imageVector = if (fast.isLogged) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = null,
                    size = NimazIconSize.SMALL,
                    // NimazColors.Success, not colorScheme.tertiary: this theme's tertiary is
                    // a deep purple, and "Logged" has to match the green the day card and the
                    // calendar legend both use for the same fact.
                    tint = if (fast.isLogged) {
                        NimazColors.Success
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (fast.isLogged) {
                        stringResource(R.string.fasting_logged)
                    } else {
                        stringResource(R.string.fasting_log_this_fast)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (fast.isLogged) {
                        NimazColors.Success
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

// ==================== PREVIEWS ====================

@Composable
private fun ComingUpShowcase() {
    val today = LocalDate.of(2026, 8, 13)
    ComingUpRow(
        fasts = listOf(
            ComingUpFast("Today", "Thursday fast", "Weekly sunnah", today, isLogged = false),
            ComingUpFast(
                "Mon 17 Aug", "Monday fast", "Weekly sunnah",
                today.plusDays(4), isLogged = true
            ),
            ComingUpFast(
                "In 4 days", "Ayyām al-Bīḍ", "13th, 14th and 15th of the lunar month",
                today.plusDays(4), isLogged = false
            ),
        ),
        onLogFast = {},
        modifier = Modifier.padding(vertical = 16.dp),
    )
}

@Preview(showBackground = true, widthDp = 400, name = "ComingUpRow — Light")
@Composable
private fun ComingUpRowLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { ComingUpShowcase() }
}

@Preview(
    showBackground = true, widthDp = 400, name = "ComingUpRow — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun ComingUpRowDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { ComingUpShowcase() }
}
