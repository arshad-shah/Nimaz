package com.arshadshah.nimaz.presentation.screens.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.audio.AdhanSound
import com.arshadshah.nimaz.data.audio.DownloadState
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButtonSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.atoms.NimazSwitch
import com.arshadshah.nimaz.presentation.components.organisms.NimazListPicker
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup
import com.arshadshah.nimaz.presentation.components.organisms.NimazPickerItem
import com.arshadshah.nimaz.presentation.components.molecules.NimazSettingsItem
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.settings.SettingsEvent
import com.arshadshah.nimaz.presentation.viewmodel.settings.SettingsViewModel

/**
 * Sound & delivery subscreen (#301): the global adhan toggle, muezzin (voice) selection with
 * preview/download, vibration, and the Do Not Disturb honour toggle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSoundScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val notificationState by viewModel.notificationState.collectAsStateWithLifecycle()
    val downloadState by viewModel.adhanDownloadState.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isAdhanPlaying.collectAsStateWithLifecycle()
    val currentlyPlaying by viewModel.currentlyPlayingAdhan.collectAsStateWithLifecycle()
    val adhanPreviewError by viewModel.adhanPreviewError.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(adhanPreviewError) {
        adhanPreviewError?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            viewModel.clearAdhanPreviewError()
        }
    }

    val selectedAdhanName = notificationState.selectedAdhanSound
    var voicePickerOpen by remember { mutableStateOf(false) }

    NimazScreenScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.notif_hub_sound_title),
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag(com.arshadshah.nimaz.core.navigation.ScreenTags.NotificationsList)
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            item {
                NimazSectionHeader(title = stringResource(R.string.notification_settings_adhan_section))
            }
            item {
                NimazMenuGroup {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.notification_settings_enable_adhan),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.notification_settings_enable_adhan_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        NimazSwitch(
                            checked = notificationState.adhanEnabled,
                            onCheckedChange = {
                                viewModel.onEvent(SettingsEvent.SetAdhanEnabled(!notificationState.adhanEnabled))
                            }
                        )
                    }
                }
            }

            if (notificationState.adhanEnabled) {
                item {
                    NimazMenuGroup {
                        NimazSettingsItem(
                            title = stringResource(R.string.notif_sound_voice),
                            subtitle = AdhanSound.fromName(selectedAdhanName).origin,
                            value = AdhanSound.fromName(selectedAdhanName).displayName,
                            onClick = { voicePickerOpen = true },
                            showArrow = true
                        )
                    }
                }
            }

            item {
                NimazMenuGroup {
                    NimazSettingsItem(
                        title = stringResource(R.string.notification_settings_vibration),
                        subtitle = stringResource(R.string.notification_settings_vibration_subtitle),
                        checked = notificationState.vibrationEnabled,
                        onCheckedChange = {
                            viewModel.onEvent(SettingsEvent.SetVibrationEnabled(!notificationState.vibrationEnabled))
                        }
                    )
                    NimazSettingsItem(
                        title = stringResource(R.string.notification_settings_dnd),
                        subtitle = stringResource(R.string.notification_settings_dnd_subtitle),
                        checked = notificationState.respectDnd,
                        onCheckedChange = {
                            viewModel.onEvent(SettingsEvent.SetRespectDnd(!notificationState.respectDnd))
                        }
                    )
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (voicePickerOpen) {
        // Dismissal is funnelled through one lambda so the preview stops whichever way the
        // sheet closes — Done, Cancel, a swipe down, or the back gesture.
        val closePicker = {
            viewModel.onEvent(SettingsEvent.StopAdhanPreview)
            voicePickerOpen = false
        }

        NimazListPicker(
            title = stringResource(R.string.notif_sound_voice),
            items = AdhanSound.entries.map { sound ->
                NimazPickerItem(
                    value = sound.name,
                    title = sound.displayName,
                    description = sound.origin,
                )
            },
            selected = selectedAdhanName,
            onSelected = { viewModel.onEvent(SettingsEvent.SetAdhanSound(it)) },
            onDismiss = closePicker,
            // You audition several voices before settling on one, so the sheet stays open
            // on selection rather than closing under the person listening.
            autoDismiss = false,
            trailingContent = { item ->
                val sound = AdhanSound.fromName(item.value)
                val isThisPlaying = isPlaying && currentlyPlaying == sound
                val isDownloading = downloadState[sound] is DownloadState.Downloading
                NimazIconButton(
                    icon = when {
                        isThisPlaying -> Icons.Default.Stop
                        isDownloading -> Icons.Default.Downloading
                        else -> Icons.Default.PlayArrow
                    },
                    contentDescription = stringResource(R.string.notification_settings_preview),
                    enabled = !isDownloading,
                    size = NimazIconButtonSize.SMALL,
                    onClick = {
                        if (isThisPlaying) {
                            viewModel.onEvent(SettingsEvent.StopAdhanPreview)
                        } else {
                            viewModel.onEvent(SettingsEvent.SetAdhanSound(sound.name))
                            viewModel.onEvent(SettingsEvent.PreviewAdhanSound)
                        }
                    }
                )
            }
        )
    }
}
