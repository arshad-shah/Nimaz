package com.arshadshah.nimaz.presentation.screens.quran

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.molecules.SurahMetaStat
import com.arshadshah.nimaz.presentation.components.molecules.SurahMetaStrip
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuItem
import com.arshadshah.nimaz.presentation.components.molecules.SurahHeaderCartouche
import com.arshadshah.nimaz.presentation.components.organisms.BottomActions
import com.arshadshah.nimaz.presentation.components.organisms.NimazTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranEvent
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranViewModel
import com.arshadshah.nimaz.presentation.viewmodel.quran.SurahThematicEvent
import com.arshadshah.nimaz.presentation.viewmodel.quran.SurahThematicViewModel

/**
 * What a surah is, and where to go to learn more about it.
 *
 * A hub, not a document. It used to answer "what is this surah about" three times over — a
 * one-line description, a row of theme badges, and the whole long-form background — and then
 * carry the full passage outline underneath, which on Al-Baqarah made it a 300-row scroll whose
 * first fold was three stat cards. Worse, when a surah had no stored info the badges fell back
 * to four generic themes, so the screen invented content rather than showing none.
 *
 * Now: identity, the numbers, the source's own summary, and three counted ways in. Each of the
 * three is drawn only where there is something behind it — an install whose artifact predates
 * the thematic layer shows fewer rows, not an empty state, because that gap is normal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahInfoScreen(
    surahNumber: Int,
    onNavigateBack: () -> Unit,
    onStartReading: () -> Unit,
    onOpenBackground: () -> Unit = {},
    onOpenPassages: () -> Unit = {},
    onOpenSubjects: () -> Unit = {},
    viewModel: QuranViewModel = hiltViewModel(),
    // The thematic layer comes from the ViewModel that owns it and that the two prose screens
    // already use, rather than a second copy of the same three reads inside QuranViewModel.
    thematicViewModel: SurahThematicViewModel = hiltViewModel(),
) {
    val homeState by viewModel.homeState.collectAsStateWithLifecycle()
    val thematic by thematicViewModel.thematic.collectAsStateWithLifecycle()
    val audioState by viewModel.audioState.collectAsStateWithLifecycle()
    val surah = homeState.surahs.find { it.number == surahNumber }

    LaunchedEffect(surahNumber) {
        viewModel.onEvent(QuranEvent.LoadSurahInfo(surahNumber))
        thematicViewModel.onEvent(SurahThematicEvent.Load(surahNumber))
    }

    // The audio control bar should only reflect playback that belongs to THIS
    // surah. If a different surah is playing/paused in the background, this
    // screen should still show the Listen button so the user can start playback
    // for the surah they are looking at.
    val isAudioForThisSurah = audioState.isActive && audioState.currentSurahNumber == surahNumber

    NimazScreenScaffold(
        topBar = {
            NimazTopAppBar(
                title = stringResource(R.string.quran_home_surah_info),
                navigationIcon = {
                    NimazIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onNavigateBack,
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
            )
        },
        bottomBar = {
            BottomActions(
                isAudioActive = isAudioForThisSurah,
                isPlaying = isAudioForThisSurah && audioState.isPlaying,
                isDownloading = isAudioForThisSurah && audioState.isDownloading,
                isPreparing = isAudioForThisSurah && audioState.isPreparing,
                downloadProgress = if (isAudioForThisSurah) audioState.downloadProgress else 0f,
                downloadedCount = if (isAudioForThisSurah) audioState.downloadedCount else 0,
                totalToDownload = if (isAudioForThisSurah) audioState.totalToDownload else 0,
                currentAyah = if (isAudioForThisSurah) audioState.currentAyahIndex + 1 else 0,
                totalAyahs = if (isAudioForThisSurah) audioState.totalAyahs else 0,
                surahProgress = if (isAudioForThisSurah) audioState.surahProgress else 0f,
                onPlayAudio = {
                    // Always (re)start playback for this surah. If a different
                    // surah is currently playing, playSurahFromInfo replaces it.
                    viewModel.onEvent(QuranEvent.PlaySurahFromInfo(surahNumber))
                },
                onResumeAudio = { viewModel.onEvent(QuranEvent.ResumeAudio) },
                onPauseAudio = { viewModel.onEvent(QuranEvent.PauseAudio) },
                onStopAudio = { viewModel.onEvent(QuranEvent.StopAudio) },
                onStartReading = onStartReading
            )
        }
    ) { paddingValues ->
        if (surah != null) {
            // Still a LazyColumn, though the long content has moved to its own screens. The
            // summary is a paragraph and the go-deeper group is three rows, so this no longer
            // saves a 300-row compose — but it is a scrolling list of a scrolling screen, and
            // swapping it for a Column would be churn in the direction of less headroom.
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Surah identity — the same cartouche the reader and list use
                item(key = "cartouche") {
                    SurahHeaderCartouche(
                        surah = surah,
                        showBismillah = false
                    )
                }

                // Where the surah sits in the Mushaf, as one strip rather than three cards.
                item(key = "stats") {
                    SurahMetaStrip(
                        stats = listOf(
                            SurahMetaStat(
                                icon = Icons.Default.FormatListNumbered,
                                label = stringResource(R.string.quran_verses_label),
                                value = surah.ayahCount.toString(),
                            ),
                            SurahMetaStat(
                                icon = Icons.Default.Layers,
                                label = stringResource(R.string.quran_juz_label),
                                value = surah.juzStart.toString(),
                            ),
                            SurahMetaStat(
                                icon = Icons.AutoMirrored.Filled.MenuBook,
                                label = stringResource(R.string.quran_page_label),
                                value = surah.startPage.toString(),
                            ),
                        )
                    )
                }

                // What the surah is about, in the source's own words. The one field written
                // for exactly this — and clamped, because the screen's job is to answer the
                // question, not to be the answer.
                thematic.overview?.summary?.takeIf { it.isNotBlank() }?.let { summary ->
                    item(key = "summary") {
                        SurahSummary(summary = summary)
                    }
                }

                // Where the long content actually lives. Each row states its size, and each is
                // drawn only where there is something behind it — on an install whose artifact
                // predates the thematic layer there is simply one fewer row, no empty state and
                // no placeholder, because absence here is a normal state and not a fault.
                item(key = "go-deeper") {
                    GoDeeper(
                        sectionCount = thematic.overview?.sections?.size ?: 0,
                        passageCount = thematic.passages.size,
                        ayahCount = surah.ayahCount,
                        subjectCount = thematic.subjectCount,
                        onOpenBackground = onOpenBackground,
                        onOpenPassages = onOpenPassages,
                        onOpenSubjects = onOpenSubjects
                    )
                }
            }
        } else {
            // Loading or surah not found
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

/**
 * The source's own summary of the surah, clamped.
 *
 * Four lines is about what fits above the fold beside three stat cards, and the point of the
 * fold here is that the three ways in are visible without scrolling. A reader who wants the
 * whole paragraph taps once; a reader who wants the background taps a row that says how long
 * it is.
 */
@Composable
private fun SurahSummary(summary: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var isTruncated by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else SUMMARY_COLLAPSED_LINES,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result ->
                // Only offer the toggle where there is something hidden. A three-line summary
                // with a "Read more" that reveals nothing is a control that lies.
                if (!expanded) isTruncated = result.hasVisualOverflow
            }
        )
        if (isTruncated) {
            NimazButton(
                text = stringResource(
                    if (expanded) R.string.surah_info_read_less else R.string.surah_info_read_more
                ),
                onClick = { expanded = !expanded },
                variant = NimazButtonVariant.TEXT
            )
        }
    }
}

/**
 * The three destinations, each carrying the size of what it holds.
 *
 * The counts are the point: "47 sections" and "282 passages across 286 verses" are what tell a
 * reader whether they are about to open a paragraph or an afternoon. A row with nothing behind
 * it is not drawn at all rather than drawn disabled — the screen still answers its question
 * with two rows, and an inert row would only advertise a gap the reader cannot close.
 */
@Composable
private fun GoDeeper(
    sectionCount: Int,
    passageCount: Int,
    ayahCount: Int,
    subjectCount: Int,
    onOpenBackground: () -> Unit,
    onOpenPassages: () -> Unit,
    onOpenSubjects: () -> Unit
) {
    if (sectionCount == 0 && passageCount == 0 && subjectCount == 0) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NimazSectionHeader(title = stringResource(R.string.surah_info_go_deeper))
        NimazMenuGroup {
            if (sectionCount > 0) {
                NimazMenuItem(
                    title = stringResource(R.string.surah_info_background),
                    subtitle = stringResource(
                        R.string.surah_info_background_subtitle,
                        sectionCount
                    ),
                    icon = Icons.AutoMirrored.Filled.Article,
                    onClick = onOpenBackground
                )
            }
            if (passageCount > 0) {
                NimazMenuItem(
                    title = stringResource(R.string.surah_info_passages),
                    subtitle = stringResource(
                        R.string.surah_info_passages_row_subtitle,
                        passageCount,
                        ayahCount
                    ),
                    icon = Icons.AutoMirrored.Filled.List,
                    onClick = onOpenPassages
                )
            }
            // Counted like the other two, and for the same reason: the row used to promise
            // "Browse what the Qur'an speaks about" and then open the global index at its
            // roots, which is a row whose subtitle was true and whose destination was wrong.
            if (subjectCount > 0) {
                NimazMenuItem(
                    title = stringResource(R.string.surah_info_subjects),
                    subtitle = stringResource(
                        R.string.surah_info_subjects_subtitle,
                        subjectCount
                    ),
                    icon = Icons.Default.AccountTree,
                    onClick = onOpenSubjects
                )
            }
        }
    }
}

private const val SUMMARY_COLLAPSED_LINES = 4
