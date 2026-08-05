package com.arshadshah.nimaz.presentation.viewmodel.quran

import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.QuranTopic
import com.arshadshah.nimaz.domain.model.TafseerHighlight
import com.arshadshah.nimaz.domain.model.TafseerNote
import com.arshadshah.nimaz.domain.model.TafseerSource
import com.arshadshah.nimaz.domain.model.TafseerText
import com.arshadshah.nimaz.presentation.viewmodel.UiError
import kotlinx.coroutines.flow.first

data class TafseerUiState(
    val surahNumber: Int = 1,
    val ayahs: List<Ayah> = emptyList(),
    val currentAyahIndex: Int = 0,
    val selectedSource: TafseerSource = TafseerSource.IBN_KATHIR,
    val currentTafseer: TafseerText? = null,
    // Hoisted out of the reader composable so it survives an ayah-by-ayah swipe
    // within the same commentary block: it only resets to 0 when the block
    // itself changes, not on every ayah navigation.
    val currentTafseerPage: Int = 0,
    val highlights: List<TafseerHighlight> = emptyList(),
    val notes: List<TafseerNote> = emptyList(),
    val surahName: String = "",
    val isLoading: Boolean = true,
    // Sources whose seed data actually has non-empty text for the current ayah.
    // Used to recommend an alternate source when the selected one has no content.
    val availableSources: Set<TafseerSource> = emptySet(),
    /**
     * The subjects the corpus files this verse under (schemaVersion 24) — busiest first.
     *
     * The commentary screen is where a reader is studying one verse, which is exactly where
     * "what else does the Qur'an say about this" is the next question. Empty for a verse no
     * topic cites, and on an install whose artifact predates the topic index.
     */
    val topics: List<QuranTopic> = emptyList(),

    /**
     * A note that failed to save, update or delete.
     *
     * A write, so it does not replace the commentary a reader is in the middle of — but it
     * is not droppable either: from where the reader is standing, a note that silently
     * failed to save is a note they wrote and lost. It surfaces on a snackbar.
     */
    val noteError: UiError? = null,
)
