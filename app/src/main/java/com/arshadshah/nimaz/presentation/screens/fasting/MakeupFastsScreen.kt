package com.arshadshah.nimaz.presentation.screens.fasting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.common.formatMediumDate
import com.arshadshah.nimaz.domain.model.MakeupFast
import com.arshadshah.nimaz.domain.model.MakeupFastStatus
import com.arshadshah.nimaz.presentation.components.atoms.GradientCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeEmphasis
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeShape
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconContainerShape
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconType
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.molecules.NimazBanner
import com.arshadshah.nimaz.presentation.components.molecules.NimazBannerVariant
import com.arshadshah.nimaz.presentation.components.molecules.NimazEmptyState
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.NimazStatData
import com.arshadshah.nimaz.presentation.components.organisms.NimazStatsGrid
import com.arshadshah.nimaz.presentation.viewmodel.tracker.FastingEvent
import com.arshadshah.nimaz.presentation.viewmodel.tracker.FastingViewModel
import com.arshadshah.nimaz.presentation.viewmodel.tracker.MakeupFastsUiState
import java.time.Instant
import java.time.ZoneId

/**
 * Make-up fasts: what is owed, and what has been settled by fasting or by fidya.
 *
 * A destination rather than a tab. It was a tab because the fast tracker had a tab row; the
 * redesign removed that row so the tracker could be one uninterrupted scroll, which left this
 * content with nowhere to live but its own route ([com.arshadshah.nimaz.core.navigation.Route.MakeupFasts]).
 *
 * The rows themselves are unchanged from the tab — this adds the scaffold and the back arrow, and
 * nothing about how a make-up fast is completed or paid.
 */
// NimazBackTopAppBar exposes a Material 3 TopAppBarScrollBehavior in its signature, so callers
// opt in too — the same annotation FastTrackerScreen carries.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MakeupFastsScreen(
    onNavigateBack: () -> Unit,
    viewModel: FastingViewModel = hiltViewModel(),
) {
    val makeupState by viewModel.makeupState.collectAsStateWithLifecycle()

    NimazScreenScaffold(
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.fasting_row_makeup),
                onBackClick = onNavigateBack,
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        ) {
            item {
                MakeupFastsContent(
                    makeupState = makeupState,
                    onCompleteMakeupFast = {
                        viewModel.onEvent(FastingEvent.CompleteMakeupFast(it))
                    },
                    onUpdateMakeupFast = {
                        viewModel.onEvent(FastingEvent.UpdateMakeupFast(it))
                    },
                    onPayFidya = { id, amount ->
                        viewModel.onEvent(FastingEvent.PayFidya(id, amount))
                    },
                )
            }
        }
    }
}

/**
 * The body of [MakeupFastsScreen].
 *
 * Still `internal` and still separate from the screen because [FastTrackerScreen] composes it in
 * its make-up tab until that tab is removed.
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
            val settledFasts = makeupState.allMakeupFasts.filter {
                it.status == MakeupFastStatus.COMPLETED || it.status == MakeupFastStatus.FIDYA_PAID
            }
            val settledCount = settledFasts.size
            val totalCount = makeupState.allMakeupFasts.size

            // Summary Card
            MakeupSummaryCard(pendingCount = makeupState.pendingCount)

            // Stats Grid
            NimazStatsGrid(
                stats = listOf(
                    NimazStatData(
                        "$settledCount",
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
                title = stringResource(R.string.fasting_makeup_info),
                variant = NimazBannerVariant.INFO,
            )

            // Owed — what is still outstanding.
            if (makeupState.pendingMakeupFasts.isNotEmpty()) {
                NimazSectionHeader(
                    title = stringResource(R.string.fasting_makeup_owed),
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

            // Settled — made up by fasting, or discharged by fidya. One section, because from
            // the reader's side both answer the same question: this one is no longer owed.
            if (settledFasts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                NimazSectionHeader(
                    title = stringResource(R.string.fasting_makeup_settled),
                    trailingText = pluralStringResource(
                        R.plurals.fasting_fasts_count,
                        settledFasts.size,
                        settledFasts.size
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                settledFasts.forEach { makeupFast ->
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
