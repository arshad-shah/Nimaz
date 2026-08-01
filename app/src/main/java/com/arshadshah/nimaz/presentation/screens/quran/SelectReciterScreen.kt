@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.arshadshah.nimaz.presentation.screens.quran

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
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.QuranReciter
import com.arshadshah.nimaz.domain.model.RecitationStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeEmphasis
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.molecules.VoiceOptionCard
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.NimazSearchBar
import com.arshadshah.nimaz.presentation.viewmodel.QuranEvent
import com.arshadshah.nimaz.presentation.viewmodel.QuranViewModel
import com.arshadshah.nimaz.presentation.viewmodel.SettingsEvent
import com.arshadshah.nimaz.presentation.viewmodel.SettingsViewModel

/** Localised label for a reciter's [RecitationStyle]. */
@Composable
internal fun recitationStyleLabel(style: RecitationStyle): String = stringResource(
    when (style) {
        RecitationStyle.MURATTAL -> R.string.recitation_style_murattal
        RecitationStyle.MUJAWWAD -> R.string.recitation_style_mujawwad
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectReciterScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    quranViewModel: QuranViewModel = hiltViewModel()
) {
    val quranState by viewModel.quranState.collectAsState()
    // Resolves aliases from older builds too, so a stored "alafasy" still highlights Mishary.
    val currentReciter = QuranReciter.fromId(quranState.selectedReciterId)
    var searchQuery by remember { mutableStateOf("") }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Track audio state for preview feedback
    val audioState by quranViewModel.audioState.collectAsState()
    var previewingReciterId by remember { mutableStateOf<String?>(null) }

    val filteredReciters = remember(searchQuery) { QuranReciter.search(searchQuery) }

    NimazScreenScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.select_reciter_title),
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                NimazSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onClear = { searchQuery = "" },
                    placeholder = stringResource(R.string.select_reciter_search_hint)
                )
            }

            // Currently Selected Section
            item {
                Text(
                    text = stringResource(R.string.select_reciter_currently_selected),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            item {
                NimazCard(
                    style = NimazCardStyle.FILLED,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    tone = NimazTone.ACCENT
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            NimazIcon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                variant = NimazIconVariant.PRIMARY,
                                iconSize = 28.dp
                            )
                        }

                        Spacer(modifier = Modifier.width(15.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentReciter.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = currentReciter.country,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        NimazBadge(
                            text = stringResource(R.string.active),
                            tone = NimazTone.ACCENT,
                            emphasis = NimazBadgeEmphasis.SOFT,
                            size = NimazBadgeSize.LARGE,
                            icon = Icons.Filled.FiberManualRecord
                        )
                    }
                }
            }

            // Popular Reciters Section
            item {
                Text(
                    text = stringResource(R.string.select_reciter_popular),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }

            items(
                items = filteredReciters,
                key = { it.id }
            ) { reciter ->
                val isSelected = reciter == currentReciter
                val isThisPreviewing = previewingReciterId == reciter.id

                VoiceOptionCard(
                    name = reciter.displayName,
                    primaryTag = recitationStyleLabel(reciter.style),
                    secondaryTag = reciter.country,
                    isSelected = isSelected,
                    isPlaying = isThisPreviewing && audioState.isPlaying,
                    isDownloading = isThisPreviewing && audioState.isDownloading,
                    // Reciter audio streams — no separate download step to gate.
                    isDownloaded = true,
                    previewContentDescription = stringResource(R.string.cd_preview),
                    onClick = {
                        viewModel.onEvent(SettingsEvent.SetReciter(reciter.id))
                    },
                    onPreviewClick = {
                        if (isThisPreviewing && audioState.isPlaying) {
                            quranViewModel.onEvent(QuranEvent.StopAudio)
                            previewingReciterId = null
                        } else {
                            previewingReciterId = reciter.id
                            quranViewModel.audioManager.setReciter(reciter.id)
                            quranViewModel.onEvent(
                                QuranEvent.PlayAyahAudio(
                                    ayahGlobalId = 1,
                                    surahNumber = 1,
                                    ayahNumber = 1
                                )
                            )
                        }
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
