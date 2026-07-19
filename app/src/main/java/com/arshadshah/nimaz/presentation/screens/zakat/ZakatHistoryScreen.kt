package com.arshadshah.nimaz.presentation.screens.zakat

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeEmphasis
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.molecules.NimazEmptyState
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.ZakatEvent
import com.arshadshah.nimaz.domain.model.ZakatHistoryEntry
import com.arshadshah.nimaz.presentation.viewmodel.ZakatViewModel
import com.arshadshah.nimaz.core.util.formatCurrency
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZakatHistoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCalculator: () -> Unit,
    viewModel: ZakatViewModel = hiltViewModel()
) {
    val historyState by viewModel.historyState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.zakat_history_title),
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCalculator,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                NimazIcon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_new_calculation)
                )
            }
        }
    ) { paddingValues ->
        if (historyState.history.isEmpty() && !historyState.isLoading) {
            NimazEmptyState(
                title = stringResource(R.string.no_zakat_history),
                message = stringResource(R.string.zakat_history_empty_message),
                icon = Icons.Default.History,
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                actionLabel = stringResource(R.string.zakat_calculate),
                onAction = onNavigateToCalculator,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Summary Card
                item {
                    TotalPaidSummaryCard(
                        totalPaid = historyState.totalZakatPaid,
                        totalEntries = historyState.history.size
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.zakat_history_calculation_history),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                items(
                    items = historyState.history,
                    key = { it.id }
                ) { entry ->
                    HistoryEntryCard(
                        entry = entry,
                        onMarkAsPaid = { viewModel.onEvent(ZakatEvent.MarkAsPaid(entry.id)) },
                        onDelete = { viewModel.onEvent(ZakatEvent.DeleteCalculation(entry.id)) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun TotalPaidSummaryCard(
    totalPaid: Double,
    totalEntries: Int,
    modifier: Modifier = Modifier
) {
    val goldGradient = Brush.linearGradient(
        colors = listOf(
            NimazColors.ZakatColors.Gold,
            NimazColors.ZakatColors.GoldAccent
        )
    )

    NimazCard(
        modifier = modifier.fillMaxWidth(),
        style = NimazCardStyle.FILLED,
        shape = RoundedCornerShape(20.dp),
        tone = NimazTone.TRANSPARENT
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(goldGradient)
                .padding(25.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.zakat_history_total_paid),
                    style = MaterialTheme.typography.bodySmall,
                    color = NimazColors.Neutral900.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = formatCurrency(totalPaid),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = NimazColors.Neutral900
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = pluralStringResource(R.plurals.zakat_calculations_recorded, totalEntries, totalEntries),
                    style = MaterialTheme.typography.bodySmall,
                    color = NimazColors.Neutral900.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun HistoryEntryCard(
    entry: ZakatHistoryEntry,
    onMarkAsPaid: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy", LocalLocale.current.platformLocale)
    val dateString = dateFormat.format(Date(entry.calculatedAt))

    NimazCard(
        modifier = modifier.fillMaxWidth(),
        style = NimazCardStyle.FILLED,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row with date and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                NimazBadge(
                    text = if (entry.isPaid) stringResource(R.string.zakat_paid) else stringResource(R.string.zakat_unpaid),
                    size = NimazBadgeSize.LARGE,
                    colors = if (entry.isPaid) {
                        NimazBadgeDefaults.feature(
                            color = NimazColors.StatusColors.Prayed,
                            emphasis = NimazBadgeEmphasis.SOFT
                        )
                    } else {
                        NimazBadgeDefaults.colors(
                            tone = NimazTone.ERROR,
                            emphasis = NimazBadgeEmphasis.SOFT
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Financial details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.net_worth),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(entry.netWorth),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.zakat_due),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(entry.zakatDue),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = NimazColors.ZakatColors.Gold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Nisab info
            Text(
                text = stringResource(
                    R.string.nisab_label_format,
                    entry.nisabType.name.lowercase().replaceFirstChar { it.uppercase() },
                    formatCurrency(entry.nisabValue)
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Paid date if applicable
            entry.paidAt?.let { paidAt ->
                Text(
                    text = stringResource(R.string.zakat_paid_on_format, dateFormat.format(Date(paidAt))),
                    style = MaterialTheme.typography.labelSmall,
                    color = NimazColors.StatusColors.Prayed
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!entry.isPaid) {
                    Surface(
                        onClick = onMarkAsPaid,
                        shape = RoundedCornerShape(8.dp),
                        color = NimazColors.StatusColors.Prayed.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            NimazIcon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = NimazColors.StatusColors.Prayed,
                                size = NimazIconSize.SMALL
                            )
                            Text(
                                text = stringResource(R.string.zakat_mark_as_paid),
                                style = MaterialTheme.typography.labelMedium,
                                color = NimazColors.StatusColors.Prayed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                }

                IconButton(onClick = onDelete) {
                    NimazIcon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.cd_delete),
                        variant = NimazIconVariant.ERROR
                    )
                }
            }
        }
    }
}

