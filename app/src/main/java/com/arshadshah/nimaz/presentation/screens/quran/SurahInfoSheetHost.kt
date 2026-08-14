package com.arshadshah.nimaz.presentation.screens.quran

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.presentation.components.molecules.SurahInfoSheet
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranEvent
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranViewModel
import com.arshadshah.nimaz.presentation.viewmodel.quran.SurahThematicEvent
import com.arshadshah.nimaz.presentation.viewmodel.quran.SurahThematicViewModel

/**
 * The state behind [SurahInfoSheet], in one place so every surface that raises it agrees.
 *
 * The sheet itself is a stateless molecule, which is what lets it be tested without Hilt. This
 * is the seam that feeds it: the same two ViewModels the retired `SurahInfoScreen` used — the
 * Qur'an one for the surah and for playback, the thematic one for the summary and the three
 * counts — so nothing about the sheet's content is a second implementation of what the screen
 * did.
 *
 * Renders nothing until the surah is known. The alternative is a sheet that slides up empty and
 * fills in, which for a control raised by a tap on the row you are already looking at reads as
 * a fault.
 */
@Composable
fun SurahInfoSheetHost(
    surahNumber: Int,
    onDismiss: () -> Unit,
    onReadSurah: () -> Unit,
    onOpenBackground: (Int) -> Unit = {},
    onOpenPassages: (Int) -> Unit = {},
    onOpenSubjects: (Int) -> Unit = {},
    viewModel: QuranViewModel = hiltViewModel(),
    thematicViewModel: SurahThematicViewModel = hiltViewModel(),
) {
    val homeState by viewModel.homeState.collectAsStateWithLifecycle()
    val thematic by thematicViewModel.thematic.collectAsStateWithLifecycle()

    LaunchedEffect(surahNumber) {
        viewModel.onEvent(QuranEvent.LoadSurahInfo(surahNumber))
        thematicViewModel.onEvent(SurahThematicEvent.Load(surahNumber))
    }

    val surah = homeState.surahs.find { it.number == surahNumber } ?: return
    val startPage = homeState.pagination.pageForAyah(firstAyahIdOf(surahNumber, homeState.surahs))
        ?: surah.startPage

    SurahInfoSheet(
        surah = surah,
        summary = thematic.overview?.summary,
        sectionCount = thematic.overview?.sections?.size ?: 0,
        passageCount = thematic.passages.size,
        subjectCount = thematic.subjectCount,
        startPage = startPage,
        juzNumber = homeState.pagination.juzForPage(startPage),
        onDismiss = onDismiss,
        onReadSurah = onReadSurah,
        onListen = { viewModel.onEvent(QuranEvent.PlaySurahFromInfo(surahNumber)) },
        onOpenBackground = { onOpenBackground(surahNumber) },
        onOpenPassages = { onOpenPassages(surahNumber) },
        onOpenSubjects = { onOpenSubjects(surahNumber) },
    )
}

/**
 * The global ayah id of a surah's first verse, from the cumulative verse counts.
 *
 * `Surah.startPage` cannot be used here: it is the Madani column, so under a line-accurate
 * edition it names a page the surah does not open on (#325). The pagination can answer the
 * question properly, but it is keyed by ayah id.
 */
private fun firstAyahIdOf(
    surahNumber: Int,
    surahs: List<com.arshadshah.nimaz.domain.model.Surah>,
): Int = surahs.asSequence()
    .filter { it.number < surahNumber }
    .sumOf { it.ayahCount } + 1
