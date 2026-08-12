package com.arshadshah.nimaz.presentation.screens.fasting

import com.arshadshah.nimaz.core.util.formatLongDate
import com.arshadshah.nimaz.core.util.formatWeekdayDayMonth
import com.arshadshah.nimaz.core.util.formatDayMonth
import com.arshadshah.nimaz.core.util.formatMediumDate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.domain.model.FastRecord
import com.arshadshah.nimaz.domain.model.FastStatus
import com.arshadshah.nimaz.domain.model.FastType
import com.arshadshah.nimaz.domain.model.MakeupFast
import com.arshadshah.nimaz.domain.model.MakeupFastStatus
import com.arshadshah.nimaz.presentation.components.atoms.NimazSwitch
import com.arshadshah.nimaz.presentation.components.atoms.GradientCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeEmphasis
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeShape
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazBanner
import com.arshadshah.nimaz.presentation.components.atoms.NimazBannerVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazCheckbox
import com.arshadshah.nimaz.presentation.components.atoms.NimazCheckboxSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCheckboxType
import com.arshadshah.nimaz.presentation.components.atoms.NimazCheckboxVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconContainerShape
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconType
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazLegendItem
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.atoms.TickResolution
import com.arshadshah.nimaz.presentation.components.atoms.clockTimeText
import com.arshadshah.nimaz.presentation.components.atoms.countdownText
import com.arshadshah.nimaz.presentation.components.atoms.rememberCountdownTo
import com.arshadshah.nimaz.presentation.components.atoms.rememberNow
import com.arshadshah.nimaz.presentation.components.molecules.NimazEmptyState
import com.arshadshah.nimaz.presentation.components.molecules.calendar.CalendarDayState
import com.arshadshah.nimaz.presentation.components.molecules.calendar.CalendarLegendItem
import com.arshadshah.nimaz.presentation.components.molecules.calendar.NimazCalendar
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.NimazStatData
import com.arshadshah.nimaz.presentation.components.organisms.NimazStatsGrid
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.viewmodel.tracker.FastingEvent
import com.arshadshah.nimaz.presentation.viewmodel.tracker.FastingViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import com.arshadshah.nimaz.presentation.viewmodel.tracker.MakeupFastsUiState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import com.arshadshah.nimaz.presentation.theme.ThemeMode
import android.content.res.Configuration
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Restore
import com.arshadshah.nimaz.core.util.formatCurrency
import com.arshadshah.nimaz.presentation.components.atoms.NimazDivider
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuItem
import com.arshadshah.nimaz.presentation.screens.resolve

/**
 * The make-up fasts tab: what is owed, what has been paid as fidya, and what is done.
 *
 * A tab, not a route — `Route.MakeupFasts` was deleted because this lives inside
 * [FastTrackerScreen] — but at ~340 lines and five composables it is a whole second screen,
 * and it was living as private functions inside a 1,779-line file (audit §5.1). The registry
 * records the same shape being fixed for Khatam: "14 inline private composables across the
 * Khatam screens collapsed into 4 shared components".
 *
 * Its own file, so it can be read, previewed and eventually tested on its own terms. Nothing
 * about it changed in the move — this is a cut and paste with the visibility widened from
 * `private` to `internal` so [FastTrackerScreen] can still compose it.
 */

@Composable
internal fun MakeupFastsContent(
    makeupState: MakeupFastsUiState,
    onCompleteMakeupFast: (Long) -> Unit,
    onUpdateMakeupFast: (MakeupFast) -> Unit = {},
    onPayFidya: (Long, Double) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var editingMakeupFast by remember { mutableStateOf<MakeupFast?>(null) }

    // Makeup fast edit bottom sheet
    MakeupFastEditBottomSheet(
        makeupFast = editingMakeupFast,
        isVisible = editingMakeupFast != null,
        onDismiss = { editingMakeupFast = null },
        onSave = { updated ->
            onUpdateMakeupFast(updated)
            editingMakeupFast = null
        },
        onPayFidya = { id, amount ->
            onPayFidya(id, amount)
            editingMakeupFast = null
        }
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (makeupState.allMakeupFasts.isEmpty()) {
            NimazEmptyState(
                title = stringResource(R.string.fasting_no_makeup),
                message = stringResource(R.string.fasting_all_up_to_date),
                iconTint = GreenAccent
            )
        } else {
            val completedFasts = makeupState.allMakeupFasts.filter {
                it.status == MakeupFastStatus.COMPLETED || it.status == MakeupFastStatus.FIDYA_PAID
            }
            val completedCount = completedFasts.size
            val totalCount = makeupState.allMakeupFasts.size

            // Summary Card
            MakeupSummaryCard(pendingCount = makeupState.pendingCount)

            // Stats Grid
            NimazStatsGrid(
                stats = listOf(
                    NimazStatData(
                        "$completedCount",
                        stringResource(R.string.fasting_completed_label),
                        GreenAccent
                    ),
                    NimazStatData(
                        "${makeupState.pendingCount}",
                        stringResource(R.string.fasting_pending_label),
                        OrangeAccent
                    ),
                    NimazStatData("$totalCount", stringResource(R.string.fasting_total_label))
                )
            )

            // Info Banner
            NimazBanner(
                message = stringResource(R.string.fasting_makeup_info),
                variant = NimazBannerVariant.INFO,
                icon = Icons.Default.Info,
                showBorder = true
            )

            // Pending Section
            if (makeupState.pendingMakeupFasts.isNotEmpty()) {
                NimazSectionHeader(
                    title = stringResource(R.string.fasting_pending),
                    trailingText = stringResource(
                        R.string.fasting_pending_count,
                        makeupState.pendingCount
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                makeupState.pendingMakeupFasts.forEach { makeupFast ->
                    MakeupPendingFastCard(
                        makeupFast = makeupFast,
                        onComplete = { onCompleteMakeupFast(makeupFast.id) },
                        onEdit = { editingMakeupFast = makeupFast }
                    )
                }
            }

            // Completed Section
            if (completedFasts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                NimazSectionHeader(
                    title = stringResource(R.string.fasting_completed_label),
                    trailingText = pluralStringResource(
                        R.plurals.fasting_fasts_count,
                        completedFasts.size,
                        completedFasts.size
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                completedFasts.forEach { makeupFast ->
                    MakeupCompletedFastItem(makeupFast = makeupFast)
                }
            }
        }
    }
}

@Composable
internal fun MakeupSummaryCard(
    pendingCount: Int,
    modifier: Modifier = Modifier
) {
    GradientCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        gradientColors = listOf(OrangeAccent, OrangeDark)
    ) {
        Column(modifier = Modifier.padding(25.dp)) {
            Text(
                text = stringResource(R.string.fasting_fasts_to_makeup),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$pendingCount",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.fasting_remaining_to_complete),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun MakeupPendingFastCard(
    makeupFast: MakeupFast,
    onComplete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val missedDate = Instant.ofEpochMilli(makeupFast.originalDate)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .formatMediumDate()

    val displayDate = makeupFast.originalHijriDate ?: missedDate

    NimazCard(
        style = NimazCardStyle.FILLED,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp)
        ) {
            // Header: date + reason on left, status badge on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = displayDate,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = makeupFast.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                NimazBadge(
                    text = stringResource(R.string.fasting_pending),
                    shape = NimazBadgeShape.ROUNDED,
                    size = NimazBadgeSize.LARGE,
                    colors = NimazBadgeDefaults.feature(
                        color = OrangeAccent,
                        emphasis = NimazBadgeEmphasis.SOFT
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Edit button
                NimazCard(
                    onClick = onEdit,
                    shape = RoundedCornerShape(10.dp),
                    style = NimazCardStyle.OUTLINED,
                    tone = NimazTone.NEUTRAL,
                    elevation = 0.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        NimazIcon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            variant = NimazIconVariant.MUTED,
                            size = NimazIconSize.SMALL
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.fasting_edit),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Mark Complete button
                NimazCard(
                    onClick = onComplete,
                    shape = RoundedCornerShape(10.dp),
                    tone = NimazTone.ACCENT,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        NimazIcon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            size = NimazIconSize.SMALL
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.fasting_mark_complete),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MakeupCompletedFastItem(
    makeupFast: MakeupFast,
    modifier: Modifier = Modifier
) {
    val missedDate = Instant.ofEpochMilli(makeupFast.originalDate)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .formatMediumDate()

    val completedDateText = makeupFast.completedDate?.let {
        val date = Instant.ofEpochMilli(it)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .formatMediumDate()
        if (makeupFast.status == MakeupFastStatus.FIDYA_PAID)
            stringResource(R.string.fasting_fidya_paid_on, date)
        else
            stringResource(R.string.fasting_made_up_on, date)
    }
        ?: if (makeupFast.status == MakeupFastStatus.FIDYA_PAID) stringResource(R.string.fasting_fidya_paid) else stringResource(
            R.string.fasting_completed
        )

    val originalLabel = makeupFast.originalHijriDate?.let {
        stringResource(R.string.fasting_originally, it)
    } ?: stringResource(R.string.fasting_originally, missedDate)

    NimazCard(
        style = NimazCardStyle.FILLED,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            // Green check icon
            NimazIcon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                type = NimazIconType.CONTAINED,
                containerShape = NimazIconContainerShape.ROUNDED_SQUARE,
                tint = GreenAccent,
                containerColor = GreenAccent.copy(alpha = 0.2f),
                containerSize = 32.dp,
                iconSize = 18.dp,
                cornerRadius = 10.dp,
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = completedDateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = originalLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}
