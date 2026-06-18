package com.arshadshah.nimaz.presentation.components.molecules.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Showcase previews for the redesigned, reusable [NimazCalendar] dual-date /
 * Hijri capabilities. These live in a `calendar` sub-package to keep the
 * component file lean while documenting the new options in one place.
 *
 * Nothing here is wired into the app — it exists purely to drive Android
 * Studio's Preview pane (and its interactive mode).
 */

/** Which calendar system is rendered as the large, centered number in a cell. */
private enum class PrimaryCalendar { GREGORIAN, HIJRI }

private val EVENT_GOLD = Color(0xFFEAB308)
private val EVENT_GREEN = Color(0xFF22C55E)

/** Sample event dots for the showcase month (June 2026). */
private val SAMPLE_EVENTS: Map<LocalDate, Color> = mapOf(
    LocalDate.of(2026, 6, 5) to EVENT_GOLD,
    LocalDate.of(2026, 6, 26) to EVENT_GREEN
)

/**
 * Builds the per-day visual state for the dual-date showcase. Pure (non-Composable)
 * so the Ramadan tint colour is passed in from the calling Composable.
 *
 * - Gregorian-primary: Gregorian day centered, Hijri day as the muted corner number.
 * - Hijri-primary: Hijri day centered, Gregorian day as the corner number.
 * - The first day of a Hijri month is emphasized (accent + bold) on whichever
 *   number represents the Hijri date — the layout-stable month-start marker.
 */
private fun buildDualDayState(
    date: LocalDate,
    primary: PrimaryCalendar,
    showDual: Boolean,
    ramadanTint: Color
): CalendarDayState {
    val hijri = HijriDateCalculator.toHijri(date)
    val isHijriMonthStart = hijri.day == 1
    val isRamadan = hijri.month == 9
    val gregorianLabel = date.dayOfMonth.toString()
    val hijriLabel = hijri.day.toString()
    val background = if (isRamadan) ramadanTint else null
    val eventDot = SAMPLE_EVENTS[date]

    return if (primary == PrimaryCalendar.GREGORIAN) {
        CalendarDayState(
            primaryLabel = gregorianLabel,
            secondaryLabel = if (showDual) hijriLabel else null,
            emphasizeSecondary = showDual && isHijriMonthStart,
            backgroundColor = background,
            indicatorColor = eventDot
        )
    } else {
        CalendarDayState(
            primaryLabel = hijriLabel,
            emphasizePrimary = isHijriMonthStart,
            secondaryLabel = if (showDual) gregorianLabel else null,
            backgroundColor = background,
            indicatorColor = eventDot
        )
    }
}

/**
 * Stateless dual-date calendar. The same [NimazCalendar] every screen uses,
 * configured for the Hijri dual-date overlay.
 */
@Composable
private fun DualCalendar(
    primary: PrimaryCalendar,
    showDual: Boolean,
    alignment: CalendarHeaderAlignment,
    month: YearMonth,
    selectedDate: LocalDate?,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val ramadanTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)

    // Header content follows the primary calendar, so asking for Hijri makes the
    // Hijri month the main label and the Gregorian range the subtitle.
    val midHijri = HijriDateCalculator.toHijri(month.atDay(15))
    val gregorianTitle =
        "${month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${month.year}"
    val hijriTitle = "${midHijri.monthName} ${midHijri.year}"
    val title = if (primary == PrimaryCalendar.HIJRI) hijriTitle else gregorianTitle
    val subtitleText = when {
        !showDual -> null
        primary == PrimaryCalendar.HIJRI -> gregorianTitle
        else -> hijriTitle
    }

    NimazCalendar(
        displayedMonth = month,
        selectedDate = selectedDate,
        onDateSelected = onDateSelected,
        onPreviousMonth = onPreviousMonth,
        onNextMonth = onNextMonth,
        headerTitle = title,
        headerAlignment = alignment,
        headerSubtitle = if (subtitleText != null) {
            @Composable {
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else null,
        dayStateProvider = { date ->
            buildDualDayState(date, primary, showDual, ramadanTint)
        },
        legendItems = listOf(
            CalendarLegendItem(EVENT_GOLD, "Event"),
            CalendarLegendItem(MaterialTheme.colorScheme.primary, "Hijri month start")
        )
    )
}

// ==================== INTERACTIVE SHOWCASE ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarShowcase() {
    var primary by remember { mutableStateOf(PrimaryCalendar.GREGORIAN) }
    var alignment by remember { mutableStateOf(CalendarHeaderAlignment.START) }
    var showDual by remember { mutableStateOf(true) }
    var month by remember { mutableStateOf(YearMonth.of(2026, 6)) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(LocalDate.of(2026, 6, 20)) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Calendar — interactive",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Run interactive preview, then flip these controls.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))

        ControlLabel("Primary (centered) calendar")
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            PrimaryCalendar.values().forEachIndexed { index, option ->
                SegmentedButton(
                    selected = primary == option,
                    onClick = { primary = option },
                    shape = SegmentedButtonDefaults.itemShape(index, PrimaryCalendar.values().size)
                ) {
                    Text(if (option == PrimaryCalendar.GREGORIAN) "Gregorian" else "Hijri")
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        ControlLabel("Header alignment")
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            CalendarHeaderAlignment.values().forEachIndexed { index, option ->
                SegmentedButton(
                    selected = alignment == option,
                    onClick = { alignment = option },
                    shape = SegmentedButtonDefaults.itemShape(
                        index,
                        CalendarHeaderAlignment.values().size
                    )
                ) {
                    Text(option.name.lowercase().replaceFirstChar { it.uppercase() })
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Dual date (show both calendars)",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(checked = showDual, onCheckedChange = { showDual = it })
        }

        Spacer(Modifier.height(16.dp))

        DualCalendar(
            primary = primary,
            showDual = showDual,
            alignment = alignment,
            month = month,
            selectedDate = selectedDate,
            onPreviousMonth = { month = month.minusMonths(1) },
            onNextMonth = { month = month.plusMonths(1) },
            onDateSelected = { selectedDate = it }
        )
    }
}

@Composable
private fun ControlLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

// ==================== STATIC PREVIEWS ====================

@Preview(showBackground = true, name = "Calendar - Interactive showcase", heightDp = 1000)
@Composable
private fun CalendarShowcasePreview() {
    NimazTheme { CalendarShowcase() }
}

@Preview(showBackground = true, name = "Dual - Gregorian primary")
@Composable
private fun DualGregorianPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            DualCalendar(
                primary = PrimaryCalendar.GREGORIAN,
                showDual = true,
                alignment = CalendarHeaderAlignment.START,
                month = YearMonth.of(2026, 6),
                selectedDate = LocalDate.of(2026, 6, 20),
                onPreviousMonth = {},
                onNextMonth = {},
                onDateSelected = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Dual - Hijri primary, centered header")
@Composable
private fun DualHijriPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            DualCalendar(
                primary = PrimaryCalendar.HIJRI,
                showDual = true,
                alignment = CalendarHeaderAlignment.CENTER,
                month = YearMonth.of(2026, 6),
                selectedDate = LocalDate.of(2026, 6, 20),
                onPreviousMonth = {},
                onNextMonth = {},
                onDateSelected = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Single date (dual off) - end header")
@Composable
private fun SingleDateEndHeaderPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            DualCalendar(
                primary = PrimaryCalendar.GREGORIAN,
                showDual = false,
                alignment = CalendarHeaderAlignment.END,
                month = YearMonth.of(2026, 6),
                selectedDate = LocalDate.of(2026, 6, 20),
                onPreviousMonth = {},
                onNextMonth = {},
                onDateSelected = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Header alignments - START / CENTER / END")
@Composable
private fun HeaderAlignmentsPreview() {
    NimazTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            CalendarHeaderAlignment.values().forEach { alignment ->
                DualCalendar(
                    primary = PrimaryCalendar.GREGORIAN,
                    showDual = true,
                    alignment = alignment,
                    month = YearMonth.of(2026, 6),
                    selectedDate = LocalDate.of(2026, 6, 20),
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onDateSelected = {}
                )
            }
        }
    }
}
