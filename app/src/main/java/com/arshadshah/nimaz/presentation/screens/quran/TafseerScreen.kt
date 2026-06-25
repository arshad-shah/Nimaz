package com.arshadshah.nimaz.presentation.screens.quran

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazPager
import com.arshadshah.nimaz.presentation.components.atoms.rememberNimazPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.core.util.TafseerPdfExporter
import com.arshadshah.nimaz.presentation.components.organisms.TafseerPageContent
import com.arshadshah.nimaz.presentation.viewmodel.TafseerEvent
import com.arshadshah.nimaz.presentation.viewmodel.TafseerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TafseerScreen(
    surahNumber: Int,
    ayahNumber: Int = 1,
    onNavigateBack: () -> Unit,
    viewModel: TafseerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(surahNumber, ayahNumber) {
        viewModel.onEvent(TafseerEvent.LoadSurah(surahNumber, ayahNumber))
    }

    // Build a branded, print/share-ready PDF of the current ayah's tafseer and
    // hand it to the system share sheet. Generation runs off the main thread.
    fun shareTafseerPdf() {
        val ayah = state.ayahs.getOrNull(state.currentAyahIndex) ?: return
        val tafseer = state.currentTafseer
        if (tafseer == null || tafseer.text.isBlank()) return
        val surahName = state.surahName
        val sourceLabel = state.selectedSource.displayName
        val highlights = state.highlights
        scope.launch(Dispatchers.Default) {
            runCatching {
                val file = TafseerPdfExporter.export(
                    context = context,
                    surahName = surahName,
                    ayah = ayah,
                    sourceLabel = sourceLabel,
                    tafseerText = tafseer.text,
                    highlights = highlights
                )
                val intent = TafseerPdfExporter.buildShareIntent(context, file)
                withContext(Dispatchers.Main) {
                    context.startActivity(
                        Intent.createChooser(
                            intent,
                            context.getString(R.string.tafseer_share_chooser)
                        )
                    )
                }
            }.onFailure { CrashReporter.recordException(it) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.surahName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val ayahNumber = state.ayahs.getOrNull(state.currentAyahIndex)?.ayahNumber
                        if (ayahNumber != null && state.ayahs.isNotEmpty()) {
                            Text(
                                text = stringResource(
                                    R.string.audio_position_ayah_format,
                                    ayahNumber,
                                    state.ayahs.size
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        NimazIcon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            } else if (state.ayahs.isNotEmpty()) {
                val pagerState = rememberNimazPagerState(
                    initialPage = state.currentAyahIndex,
                    pageCount = { state.ayahs.size }
                )

                // Sync pager with ViewModel
                LaunchedEffect(pagerState.settledPage) {
                    if (pagerState.settledPage != state.currentAyahIndex) {
                        viewModel.onEvent(TafseerEvent.NavigateToAyah(pagerState.settledPage))
                    }
                }

                NimazPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val ayah = state.ayahs[page]
                    val isCurrentPage = page == state.currentAyahIndex

                    TafseerPageContent(
                        ayah = ayah,
                        tafseer = if (isCurrentPage) state.currentTafseer else null,
                        highlights = if (isCurrentPage) state.highlights else emptyList(),
                        selectedSource = state.selectedSource,
                        availableSources = if (isCurrentPage) state.availableSources else emptySet(),
                        onSourceSwitch = { source ->
                            viewModel.onEvent(TafseerEvent.SwitchSource(source))
                        },
                        onHighlightCreated = { start, end, color, note ->
                            viewModel.onEvent(TafseerEvent.AddHighlight(start, end, color, note))
                        },
                        onHighlightUpdated = { id, color, note ->
                            viewModel.onEvent(TafseerEvent.UpdateHighlight(id, color, note))
                        },
                        onHighlightDeleted = { id ->
                            viewModel.onEvent(TafseerEvent.DeleteHighlight(id))
                        },
                        onShare = { shareTafseerPdf() }
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_ayahs_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
