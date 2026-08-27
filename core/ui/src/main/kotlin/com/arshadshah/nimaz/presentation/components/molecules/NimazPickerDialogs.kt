package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.components.molecules.calendar.NimazCalendar
import com.arshadshah.nimaz.presentation.foundation.time.NimazTime
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * Date picker in the app's own dialog shell, driven by the app's own [NimazCalendar].
 *
 * Replaces Material3's `DatePickerDialog`, which brings its own header, typography and
 * shape language and reads as a different app the moment it opens.
 *
 * @param selectedDateMillis current selection as epoch millis, or null for none.
 * @param minDate dates before this are not selectable — defaults to today, since the
 *   only current caller is a deadline.
 */
@Composable
fun NimazDatePickerDialog(
    selectedDateMillis: Long?,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    minDate: LocalDate? = LocalDate.now(),
) {
    val zone = remember { ZoneId.systemDefault() }
    val initialDate = remember(selectedDateMillis) {
        selectedDateMillis
            ?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
            ?: LocalDate.now()
    }

    var selected by remember { mutableStateOf(initialDate) }
    var month by remember { mutableStateOf(YearMonth.from(initialDate)) }

    NimazDialog(
        title = title,
        onDismiss = onDismiss,
        modifier = modifier,
        titleIcon = Icons.Default.CalendarMonth,
        // The calendar draws its own surface, so the dialog's inset card would
        // double up the container.
        wrapContent = true,
        actions = {
            NimazDialogCancelButton(onClick = onDismiss)
            NimazDialogConfirmButton(
                text = stringResource(android.R.string.ok),
                onClick = {
                    onConfirm(selected.atStartOfDay(zone).toInstant().toEpochMilli())
                },
            )
        },
    ) {
        NimazCalendar(
            displayedMonth = month,
            selectedDate = selected,
            onDateSelected = { date ->
                if (minDate == null || !date.isBefore(minDate)) selected = date
            },
            onPreviousMonth = { month = month.minusMonths(1) },
            onNextMonth = { month = month.plusMonths(1) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Time picker in the app's own dialog shell, driven by [NimazTimePicker].
 */
@Composable
fun NimazTimePickerDialog(
    initialTime: NimazTime,
    onConfirm: (NimazTime) -> Unit,
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    minuteStep: Int = 5,
) {
    var time by remember { mutableStateOf(initialTime) }

    NimazDialog(
        title = title,
        subtitle = time.toStorageString(),
        onDismiss = onDismiss,
        modifier = modifier,
        titleIcon = Icons.Default.Schedule,
        actions = {
            NimazDialogCancelButton(onClick = onDismiss)
            NimazDialogConfirmButton(
                text = stringResource(android.R.string.ok),
                onClick = { onConfirm(time) },
            )
        },
    ) {
        NimazTimePicker(
            value = time,
            onValueChange = { time = it },
            minuteStep = minuteStep,
        )
    }
}

// ---- Previews ----

@Preview(showBackground = true, widthDp = 400, heightDp = 640, name = "Date Dialog — Light")
@Composable
private fun NimazDatePickerDialogLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        Column {
            NimazDatePickerDialog(
                selectedDateMillis = null,
                onConfirm = {},
                onDismiss = {},
                title = stringResource(R.string.khatam_field_deadline),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 640, name = "Date Dialog — Dark")
@Composable
private fun NimazDatePickerDialogDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        Column {
            NimazDatePickerDialog(
                selectedDateMillis = null,
                onConfirm = {},
                onDismiss = {},
                title = stringResource(R.string.khatam_field_deadline),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 500, name = "Time Dialog — Light")
@Composable
private fun NimazTimePickerDialogLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        Column {
            NimazTimePickerDialog(
                initialTime = NimazTime(6, 0),
                onConfirm = {},
                onDismiss = {},
                title = stringResource(R.string.notification_settings_reminder_time),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 500, name = "Time Dialog — Dark")
@Composable
private fun NimazTimePickerDialogDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        Column {
            NimazTimePickerDialog(
                initialTime = NimazTime(21, 30),
                onConfirm = {},
                onDismiss = {},
                title = stringResource(R.string.notification_settings_reminder_time),
            )
        }
    }
}
