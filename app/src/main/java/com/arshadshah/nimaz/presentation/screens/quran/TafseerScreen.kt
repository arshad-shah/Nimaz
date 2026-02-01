package com.arshadshah.nimaz.presentation.screens.quran

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.presentation.components.organisms.TafseerPageContent
import com.arshadshah.nimaz.presentation.viewmodel.TafseerEvent
import com.arshadshah.nimaz.presentation.viewmodel.TafseerViewModel

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

    LaunchedEffect(surahNumber, ayahNumber) {
        viewModel.onEvent(TafseerEvent.LoadSurah(surahNumber, ayahNumber))
    }

    // Handle export
    LaunchedEffect(state.exportedText) {
        state.exportedText?.let { text ->
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share Annotations"))
            viewModel.onEvent(TafseerEvent.ClearExport)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.surahName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(TafseerEvent.ExportAnnotations) }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export Annotations"
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
                val pagerState = rememberPagerState(
                    initialPage = state.currentAyahIndex,
                    pageCount = { state.ayahs.size }
                )

                // Sync pager with ViewModel
                LaunchedEffect(pagerState.settledPage) {
                    if (pagerState.settledPage != state.currentAyahIndex) {
                        viewModel.onEvent(TafseerEvent.NavigateToAyah(pagerState.settledPage))
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val ayah = state.ayahs[page]
                    val isCurrentPage = page == state.currentAyahIndex

                    TafseerPageContent(
                        ayah = ayah,
                        tafseer = if (isCurrentPage) state.currentTafseer else null,
                        highlights = if (isCurrentPage) state.highlights else emptyList(),
                        totalAyahs = state.ayahs.size,
                        selectedSource = state.selectedSource,
                        onSourceSwitch = { source ->
                            viewModel.onEvent(TafseerEvent.SwitchSource(source))
                        },
                        onHighlightCreated = { start, end, color ->
                            viewModel.onEvent(TafseerEvent.AddHighlight(start, end, color))
                        },
                        onHighlightDeleted = { id ->
                            viewModel.onEvent(TafseerEvent.DeleteHighlight(id))
                        },
                        onHighlightNoteUpdated = { id, note ->
                            viewModel.onEvent(TafseerEvent.UpdateHighlightNote(id, note))
                        }
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No ayahs found.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
