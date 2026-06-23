package com.arshadshah.nimaz.presentation.screens.settings

import androidx.compose.ui.res.stringResource
import com.arshadshah.nimaz.R
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.data.sync.CancelReason
import com.arshadshah.nimaz.data.sync.ConnectionState
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.ActivityLogEntry
import com.arshadshah.nimaz.presentation.viewmodel.SyncDataSummary
import com.arshadshah.nimaz.presentation.viewmodel.SyncEvent
import com.arshadshah.nimaz.presentation.viewmodel.SyncMode
import com.arshadshah.nimaz.presentation.viewmodel.SyncUiState
import com.arshadshah.nimaz.presentation.viewmodel.SyncViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    onNavigateBack: () -> Unit,
    viewModel: SyncViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* permissions granted or denied — connection will fail gracefully if denied */ }

    val nearbyPermissions = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_ADVERTISE)
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_SCAN)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        add(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    Scaffold(
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.sync_data),
                onBackClick = {
                    viewModel.onEvent(SyncEvent.Cancel)
                    onNavigateBack()
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Role badge — shown whenever we have a mode selected and are not idle
            if (state.mode != SyncMode.NONE && state.connectionState !is ConnectionState.Idle) {
                RoleBadge(mode = state.mode)
            }

            when {
                state.mode == SyncMode.NONE -> {
                    ModeSelectionContent(
                        onSendClick = {
                            permissionLauncher.launch(nearbyPermissions.toTypedArray())
                            viewModel.onEvent(SyncEvent.StartSend)
                        },
                        onReceiveClick = {
                            permissionLauncher.launch(nearbyPermissions.toTypedArray())
                            viewModel.onEvent(SyncEvent.StartReceive)
                        }
                    )
                }

                state.connectionState is ConnectionState.Cancelled -> {
                    val cancelled = state.connectionState as ConnectionState.Cancelled
                    CancelledContent(
                        reason = cancelled.reason,
                        activityLog = state.activityLog,
                        onTryAgain = { viewModel.onEvent(SyncEvent.Cancel) },
                        onClose = {
                            viewModel.onEvent(SyncEvent.Cancel)
                            onNavigateBack()
                        }
                    )
                }

                state.connectionState is ConnectionState.Connecting -> {
                    val connecting = state.connectionState as ConnectionState.Connecting
                    AuthTokenContent(
                        endpointName = connecting.endpointName,
                        authToken = connecting.authToken,
                        mode = state.mode,
                        onAccept = { viewModel.onEvent(SyncEvent.AcceptConnection(connecting.endpointId)) },
                        onReject = { viewModel.onEvent(SyncEvent.RejectConnection(connecting.endpointId)) }
                    )
                }

                state.connectionState is ConnectionState.WaitingForPartnerAccept -> {
                    WaitingForPartnerContent(
                        onCancel = { viewModel.onEvent(SyncEvent.Cancel) }
                    )
                }

                state.error != null -> {
                    ErrorContent(
                        error = state.error!!,
                        activityLog = state.activityLog,
                        onRetry = { viewModel.onEvent(SyncEvent.Cancel) },
                        onDismiss = {
                            viewModel.onEvent(SyncEvent.Cancel)
                            onNavigateBack()
                        }
                    )
                }

                state.connectionState is ConnectionState.Completed -> {
                    CompletedContent(
                        state = state,
                        onDone = {
                            viewModel.onEvent(SyncEvent.Cancel)
                            onNavigateBack()
                        }
                    )
                }

                else -> {
                    ProgressContent(
                        state = state,
                        onCancel = { viewModel.onEvent(SyncEvent.Cancel) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RoleBadge(mode: SyncMode) {
    val (label, containerColor, contentColor) = when (mode) {
        SyncMode.SEND -> Triple(
            stringResource(R.string.sync_sending),
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )

        SyncMode.RECEIVE -> Triple(
            stringResource(R.string.sync_receiving),
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )

        SyncMode.NONE -> return
    }
    val icon = if (mode == SyncMode.SEND) Icons.AutoMirrored.Filled.Send else Icons.Default.Download

    NimazCard(
        style = NimazCardStyle.FILLED,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            NimazIcon(
                imageVector = icon,
                contentDescription = null,
                size = NimazIconSize.SMALL,
                tint = contentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}

@Composable
private fun ModeSelectionContent(
    onSendClick: () -> Unit,
    onReceiveClick: () -> Unit
) {
    Spacer(modifier = Modifier.height(32.dp))

    NimazIcon(
        imageVector = Icons.Default.Sync,
        contentDescription = null,
        iconSize = 64.dp,
        variant = NimazIconVariant.PRIMARY
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.sync_device_to_device),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )

    Text(
        text = stringResource(R.string.sync_intro),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp)
    )

    Spacer(modifier = Modifier.height(24.dp))

    NimazCard(
        style = NimazCardStyle.FILLED,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NimazButton(
                text = stringResource(R.string.sync_send_data),
                onClick = onSendClick,
                variant = NimazButtonVariant.FILLED,
                leadingIcon = Icons.AutoMirrored.Filled.Send,
                fullWidth = true
            )

            Text(
                text = stringResource(R.string.sync_send_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            NimazButton(
                text = stringResource(R.string.sync_receive_data),
                onClick = onReceiveClick,
                variant = NimazButtonVariant.OUTLINED,
                leadingIcon = Icons.Default.Download,
                fullWidth = true
            )

            Text(
                text = stringResource(R.string.sync_receive_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AuthTokenContent(
    endpointName: String,
    authToken: String,
    mode: SyncMode,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Spacer(modifier = Modifier.height(16.dp))

    NimazIcon(
        imageVector = Icons.Default.PhoneAndroid,
        contentDescription = null,
        iconSize = 48.dp,
        variant = NimazIconVariant.PRIMARY
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.sync_confirm_connection),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )

    Text(
        text = stringResource(R.string.sync_connecting_to_format, endpointName),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    // Role context
    val roleText = when (mode) {
        SyncMode.SEND -> stringResource(R.string.sync_role_sending)
        SyncMode.RECEIVE -> stringResource(R.string.sync_role_receiving)
        SyncMode.NONE -> ""
    }
    if (roleText.isNotEmpty()) {
        Text(
            text = roleText,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    NimazCard(
        style = NimazCardStyle.FILLED,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.sync_verification_code),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = authToken,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.sync_code_match_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NimazButton(
            text = stringResource(R.string.sync_reject),
            onClick = onReject,
            modifier = Modifier.weight(1f),
            variant = NimazButtonVariant.DESTRUCTIVE,
            leadingIcon = Icons.Default.Close
        )

        NimazButton(
            text = stringResource(R.string.sync_accept),
            onClick = onAccept,
            modifier = Modifier.weight(1f),
            variant = NimazButtonVariant.FILLED,
            leadingIcon = Icons.Default.Check
        )
    }
}

@Composable
private fun WaitingForPartnerContent(
    onCancel: () -> Unit
) {
    Spacer(modifier = Modifier.height(32.dp))

    CircularProgressIndicator(
        modifier = Modifier.size(64.dp),
        strokeWidth = 5.dp,
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = stringResource(R.string.sync_waiting_partner),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center
    )

    Text(
        text = stringResource(R.string.sync_waiting_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 24.dp)
    )

    Spacer(modifier = Modifier.height(32.dp))

    NimazButton(
        text = stringResource(R.string.cancel),
        onClick = onCancel,
        variant = NimazButtonVariant.OUTLINED
    )
}

@Composable
private fun CancelledContent(
    reason: CancelReason,
    activityLog: List<ActivityLogEntry>,
    onTryAgain: () -> Unit,
    onClose: () -> Unit
) {
    Spacer(modifier = Modifier.height(32.dp))

    val (icon, title, message) = when (reason) {
        CancelReason.BY_USER -> Triple(
            Icons.Default.SyncDisabled,
            stringResource(R.string.sync_cancelled),
            stringResource(R.string.sync_cancelled_by_you)
        )

        CancelReason.BY_PARTNER -> Triple(
            Icons.Default.PersonOff,
            stringResource(R.string.sync_cancelled),
            stringResource(R.string.sync_cancelled_by_partner)
        )

        CancelReason.CONNECTION_LOST -> Triple(
            Icons.Default.LinkOff,
            stringResource(R.string.sync_connection_lost),
            stringResource(R.string.sync_connection_lost_msg)
        )
    }

    NimazIcon(
        imageVector = icon,
        contentDescription = null,
        iconSize = 64.dp,
        variant = NimazIconVariant.MUTED
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )

    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp)
    )

    if (activityLog.isNotEmpty()) {
        ActivityLog(entries = activityLog)
    }

    Spacer(modifier = Modifier.height(24.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        NimazButton(
            text = stringResource(R.string.close),
            onClick = onClose,
            variant = NimazButtonVariant.OUTLINED
        )
        NimazButton(
            text = stringResource(R.string.try_again),
            onClick = onTryAgain,
            variant = NimazButtonVariant.FILLED
        )
    }
}

@Composable
private fun ProgressContent(
    state: SyncUiState,
    onCancel: () -> Unit
) {
    val overallProgress = if (state.totalSteps > 0) {
        state.stepsCompleted.toFloat() / state.totalSteps
    } else 0f

    val percentText = "${(overallProgress * 100).toInt()}%"

    Spacer(modifier = Modifier.height(16.dp))

    // Circular progress with percentage
    Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { overallProgress },
            modifier = Modifier.size(80.dp),
            strokeWidth = 6.dp,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
        Text(
            text = percentText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Current step description
    Text(
        text = state.currentStep.ifEmpty { "Preparing..." },
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.primary
    )

    // Overall progress bar
    LinearProgressIndicator(
        progress = { overallProgress },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )

    Text(
        text = stringResource(R.string.sync_step_format, state.stepsCompleted, state.totalSteps),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    // Sender sees partner importing
    val partnerState = state.connectionState as? ConnectionState.PartnerImporting
    if (partnerState != null) {
        NimazCard(
            style = NimazCardStyle.FILLED,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NimazIcon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        size = NimazIconSize.MEDIUM,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = stringResource(R.string.sync_data_sent),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                Text(
                    text = stringResource(R.string.sync_partner_importing_format, partnerState.label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                if (partnerState.total > 0) {
                    LinearProgressIndicator(
                        progress = { partnerState.step.toFloat() / partnerState.total },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        trackColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f),
                    )
                }
            }
        }
    }

    // Transfer progress (when sending/receiving bytes)
    AnimatedVisibility(
        visible = state.connectionState is ConnectionState.Transferring,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val transferProgress =
            (state.connectionState as? ConnectionState.Transferring)?.progress ?: 0f
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            NimazCard(
                style = NimazCardStyle.FILLED,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.sync_transfer_progress),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LinearProgressIndicator(
                        progress = { transferProgress },
                        modifier = Modifier.fillMaxWidth(),
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                    val dataSizeText = state.dataSummary?.totalBytes?.let {
                        if (it > 0) " (${SyncViewModel.formatBytes(it)})" else ""
                    } ?: ""
                    Text(
                        text = "${(transferProgress * 100).toInt()}% transferred$dataSizeText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Data summary card (once available)
    AnimatedVisibility(
        visible = state.dataSummary != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        state.dataSummary?.let { summary ->
            DataSummaryCard(
                summary = summary,
                title = if (state.mode == SyncMode.SEND) stringResource(R.string.sync_data_being_sent) else stringResource(R.string.sync_data_being_received)
            )
        }
    }

    // Activity log
    if (state.activityLog.isNotEmpty()) {
        ActivityLog(entries = state.activityLog)
    }

    Spacer(modifier = Modifier.height(16.dp))

    NimazButton(
        text = stringResource(R.string.cancel),
        onClick = onCancel,
        variant = NimazButtonVariant.TEXT
    )
}

@Composable
private fun ActivityLog(entries: List<ActivityLogEntry>) {
    NimazCard(
        style = NimazCardStyle.FILLED,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.sync_activity),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
            entries.forEach { entry ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NimazIcon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        iconSize = 14.dp,
                        variant = NimazIconVariant.PRIMARY
                    )
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Maps a [com.arshadshah.nimaz.data.sync.SyncCategory] key to its localized
 * label. Keys come from `SyncPayload.categories()`; if a new category is added
 * there, add its label here too (SyncPayloadCoverageTest guards the data side).
 */
@Composable
private fun syncCategoryLabel(key: String): String = stringResource(
    when (key) {
        "bookmarks" -> R.string.sync_item_bookmarks
        "favorites" -> R.string.sync_item_favorites
        "readingProgress" -> R.string.sync_item_reading_progress
        "prayerRecords" -> R.string.sync_item_prayer_records
        "fastRecords" -> R.string.sync_item_fast_records
        "makeupFasts" -> R.string.sync_item_makeup_fasts
        "tasbihPresets" -> R.string.sync_item_tasbih_presets
        "tasbihSessions" -> R.string.sync_item_tasbih_sessions
        "khatams" -> R.string.sync_item_khatams
        "khatamAyahs" -> R.string.sync_item_khatam_ayahs
        "khatamDailyLogs" -> R.string.sync_item_khatam_daily_logs
        "tafseerHighlights" -> R.string.sync_item_tafseer_highlights
        "tafseerNotes" -> R.string.sync_item_tafseer_notes
        "zakatHistory" -> R.string.sync_item_zakat
        "asmaUlHusnaBookmarks" -> R.string.sync_item_asma_ul_husna
        "asmaUnNabiBookmarks" -> R.string.sync_item_asma_un_nabi
        "prophetBookmarks" -> R.string.sync_item_prophets
        "hadithBookmarks" -> R.string.sync_item_hadith_bookmarks
        "duaBookmarks" -> R.string.sync_item_dua_bookmarks
        "duaProgress" -> R.string.sync_item_dua_progress
        "qaidaLessonProgress" -> R.string.sync_item_qaida_lessons
        "qaidaCellProgress" -> R.string.sync_item_qaida_cells
        "favoriteLocations" -> R.string.sync_item_locations
        "preferences" -> R.string.sync_item_preferences
        else -> R.string.sync_no_data
    }
)

@Composable
private fun DataSummaryCard(summary: SyncDataSummary, title: String) {
    NimazCard(
        style = NimazCardStyle.FILLED,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Driven entirely by SyncPayload.categories() so the screen always
            // reflects exactly what the payload carries — no manual list to keep
            // in sync as new features are added.
            val visible = summary.categories.filter { it.count > 0 }

            if (visible.isEmpty()) {
                Text(
                    text = stringResource(R.string.sync_no_data),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                visible.forEach { category ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = syncCategoryLabel(category.key),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (category.isFlag) "" else "${category.count}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (summary.totalBytes > 0) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.sync_total_size),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = SyncViewModel.formatBytes(summary.totalBytes),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletedContent(
    state: SyncUiState,
    onDone: () -> Unit
) {
    Spacer(modifier = Modifier.height(32.dp))

    NimazIcon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = null,
        iconSize = 72.dp,
        variant = NimazIconVariant.PRIMARY
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.sync_complete),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )

    Text(
        text = if (state.mode == SyncMode.SEND) stringResource(R.string.sync_sent_success) else stringResource(R.string.sync_imported_success),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    // Show what was synced
    state.dataSummary?.let { summary ->
        Spacer(modifier = Modifier.height(8.dp))
        DataSummaryCard(
            summary = summary,
            title = if (state.mode == SyncMode.SEND) stringResource(R.string.sync_data_sent_title) else stringResource(R.string.sync_data_imported_title)
        )
    }

    // Show activity log
    if (state.activityLog.isNotEmpty()) {
        ActivityLog(entries = state.activityLog)
    }

    Spacer(modifier = Modifier.height(24.dp))

    NimazButton(
        text = stringResource(R.string.done),
        onClick = onDone,
        variant = NimazButtonVariant.FILLED
    )
}

@Composable
private fun ErrorContent(
    error: String,
    activityLog: List<ActivityLogEntry>,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Spacer(modifier = Modifier.height(48.dp))

    NimazIcon(
        imageVector = Icons.Default.Close,
        contentDescription = null,
        iconSize = 64.dp,
        variant = NimazIconVariant.ERROR
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.sync_failed),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.error
    )

    Text(
        text = error,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp)
    )

    if (activityLog.isNotEmpty()) {
        ActivityLog(entries = activityLog)
    }

    Spacer(modifier = Modifier.height(32.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        NimazButton(
            text = stringResource(R.string.close),
            onClick = onDismiss,
            variant = NimazButtonVariant.OUTLINED
        )
        NimazButton(
            text = stringResource(R.string.try_again),
            onClick = onRetry,
            variant = NimazButtonVariant.FILLED
        )
    }
}
