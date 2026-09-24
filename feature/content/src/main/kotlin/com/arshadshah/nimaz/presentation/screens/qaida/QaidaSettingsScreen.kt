package com.arshadshah.nimaz.presentation.screens.qaida

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.core.ui.R as CoreR
import com.arshadshah.nimaz.feature.content.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.molecules.NimazSettingsItem
import com.arshadshah.nimaz.presentation.components.molecules.NimazSettingsSection
import com.arshadshah.nimaz.presentation.components.molecules.NimazConfirmDialog
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.content.QaidaReaderEvent
import com.arshadshah.nimaz.presentation.viewmodel.content.QaidaReaderViewModel

/** Shared by journey, lessons and the alphabet. Preferences persist on this device. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QaidaSettingsScreen(onNavigateBack: () -> Unit, viewModel: QaidaReaderViewModel, onJourneyReset: () -> Unit = onNavigateBack) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val cacheBytes by viewModel.cacheBytes.collectAsStateWithLifecycle()
    var confirmation by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { viewModel.refreshCacheSize() }
    BackHandler { onNavigateBack() }
    NimazScreenScaffold(topBar = {
        NimazBackTopAppBar(title = stringResource(CoreR.string.qaida),
            subtitle = stringResource(R.string.qaida_settings), onBackClick = onNavigateBack)
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)) {
            item {
                Image(painterResource(R.drawable.qaida_letters_art), null,
                    Modifier.fillMaxWidth().height(130.dp))
            }
            item {
                NimazSettingsSection(stringResource(R.string.qaida_settings_learning)) {
                    NimazSettingsItem(title = stringResource(R.string.qaida_settings_transliteration),
                        subtitle = stringResource(R.string.qaida_settings_transliteration_hint), icon = Icons.Default.Translate,
                        checked = settings.showTransliteration,
                        modifier = Modifier.semantics {
                            role = Role.Switch
                            toggleableState = ToggleableState(settings.showTransliteration)
                        },
                        onCheckedChange = { viewModel.onEvent(QaidaReaderEvent.SetTransliteration(it)) })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    NimazSettingsItem(title = stringResource(R.string.qaida_settings_slow),
                        subtitle = stringResource(R.string.qaida_settings_slow_hint), icon = Icons.Default.Speed,
                        checked = settings.slowPlayback,
                        modifier = Modifier.semantics {
                            role = Role.Switch
                            toggleableState = ToggleableState(settings.slowPlayback)
                        },
                        onCheckedChange = { viewModel.onEvent(QaidaReaderEvent.SetSlow(it)) })
                }
            }
            item {
                NimazSettingsSection(stringResource(R.string.qaida_settings_storage)) {
                    Text(stringResource(R.string.qaida_downloads_description), Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium)
                    NimazSettingsItem(title = stringResource(R.string.qaida_clear_audio),
                        subtitle = stringResource(R.string.qaida_cache_size, "%.1f".format(cacheBytes / 1048576.0)),
                        icon = Icons.Default.DeleteOutline, enabled = cacheBytes > 0, onClick = { confirmation = 1 })
                }
            }
            item {
                NimazSettingsSection(stringResource(R.string.qaida_settings_progress)) {
                    NimazSettingsItem(title = stringResource(CoreR.string.qaida_reset_journey),
                        subtitle = stringResource(R.string.qaida_settings_reset_hint), icon = Icons.Default.RestartAlt,
                        onClick = { confirmation = 2 })
                }
            }
            item { Text(stringResource(R.string.qaida_settings_local), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
    if (confirmation != 0) {
        val reset = confirmation == 2
        NimazConfirmDialog(
            title = stringResource(if (reset) CoreR.string.qaida_reset_title else R.string.qaida_clear_audio),
            message = stringResource(if (reset) CoreR.string.qaida_reset_message else R.string.qaida_clear_audio_message),
            confirmText = stringResource(if (reset) CoreR.string.reset else R.string.qaida_clear_audio),
            cancelText = stringResource(CoreR.string.cancel), titleIcon = if (reset) Icons.Default.RestartAlt else Icons.Default.DeleteOutline,
            isDestructive = true, onDismiss = { confirmation = 0 },
            onConfirm = {
                viewModel.onEvent(if (reset) QaidaReaderEvent.ResetJourney else QaidaReaderEvent.ClearAudio)
                confirmation = 0
                if (reset) onJourneyReset()
            })
    }
}
