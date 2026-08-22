package com.arshadshah.nimaz.presentation.components.organisms

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.core.share.ContentShareManager
import com.arshadshah.nimaz.core.share.Shareables
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.model.TranslationLanguage
import com.arshadshah.nimaz.presentation.components.molecules.AyahTooltip
import kotlinx.coroutines.launch

/**
 * What happens when a reader taps a verse: the tooltip, its seven actions, and the sheet the
 * translation button opens.
 *
 * This is one behaviour with two renderers. [MushafPage] draws continuous text and
 * [MushafLinePage] draws a line-accurate layout, but a tap means the same thing in both — and
 * it was written twice, 93 lines of it, byte-identical apart from one condition. The audit
 * (§1.2) called for the shared per-verse action host to be extracted from the two renderers,
 * and this is it: they keep their layout, which is all they actually differ in, and share this.
 *
 * The state lives in [MushafAyahActionsState] rather than in the host so a renderer can *open*
 * the tooltip from wherever its own tap handling lives — a word in a printed line, or an offset
 * in a run of continuous text — without the host knowing anything about either.
 */
@Stable
class MushafAyahActionsState {

    /** The verse whose tooltip is open, or null. */
    var tooltipAyah by mutableStateOf<Ayah?>(null)
        private set

    /** Where the tooltip's beak points, in viewport coordinates. */
    var tooltipTapY by mutableFloatStateOf(0f)
        private set

    /** The verse whose translation sheet is open, or null. */
    var translationAyah by mutableStateOf<Ayah?>(null)
        internal set

    /**
     * Optimistic bookmark state while the tooltip is open.
     *
     * The tooltip's icon has to flip on tap, but the real value arrives back through a Flow from
     * the database a frame or two later. Null means "no local opinion — trust the ayah".
     */
    internal var bookmarkOverride by mutableStateOf<Boolean?>(null)

    /** The same, per ayah, for favourites — a page can have several toggled before it recomposes. */
    internal var favoriteOverrides by mutableStateOf<Map<Int, Boolean>>(emptyMap())

    /** Open the tooltip for [ayah], with its beak at [tapY]. */
    fun show(ayah: Ayah, tapY: Float) {
        // Cleared here rather than in a LaunchedEffect keyed on the ayah: the override belongs to
        // one opening of the tooltip, and a new opening is exactly when it stops applying.
        bookmarkOverride = null
        tooltipAyah = ayah
        tooltipTapY = tapY
    }

    /** Close the tooltip, leaving any open sheet alone. */
    fun dismiss() {
        tooltipAyah = null
    }
}

@Composable
fun rememberMushafAyahActionsState(): MushafAyahActionsState = remember { MushafAyahActionsState() }

/**
 * The tooltip and translation sheet for [state]'s current verse. Renders nothing when no verse
 * is selected, so it can sit unconditionally at the end of a renderer's `Box`.
 *
 * @param parentHeight the renderer's height, which decides whether the tooltip opens above or
 *   below the tap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MushafAyahActions(
    state: MushafAyahActionsState,
    parentHeight: Float,
    surahMap: Map<Int, Surah>,
    favoriteAyahIds: Set<Int>,
    isKhatamActive: Boolean,
    khatamReadAyahIds: Set<Int>,
    showTranslation: Boolean,
    showTransliteration: Boolean,
    translationLanguage: TranslationLanguage,
    onPlayClick: (Ayah) -> Unit,
    onBookmarkClick: (Ayah) -> Unit,
    onFavoriteClick: (Ayah) -> Unit,
    onCopyClick: (Ayah) -> Unit,
    onShareClick: (Ayah) -> Unit,
    onTafseerClick: (Ayah) -> Unit,
    onKhatamToggle: (Ayah) -> Unit,
) {
    val context = LocalContext.current
    val shareScope = rememberCoroutineScope()
    val copiedMessage = stringResource(R.string.ayah_copied_to_clipboard)
    val sheetState = rememberModalBottomSheetState()

    state.tooltipAyah?.let { ayah ->
        val currentBookmarked = state.bookmarkOverride ?: ayah.isBookmarked
        val currentFavorite = state.favoriteOverrides[ayah.id] ?: (ayah.id in favoriteAyahIds)

        AyahTooltip(
            tapY = state.tooltipTapY,
            parentHeight = parentHeight,
            isBookmarked = currentBookmarked,
            isFavorite = currentFavorite,
            isKhatamActive = isKhatamActive,
            isKhatamRead = ayah.id in khatamReadAyahIds,
            showTranslationButton = showsTranslationButton(
                ayah = ayah,
                showTranslation = showTranslation,
                showTransliteration = showTransliteration,
            ),
            onDismiss = state::dismiss,
            onPlayClick = {
                onPlayClick(ayah)
                state.dismiss()
            },
            onBookmarkClick = {
                state.bookmarkOverride = !currentBookmarked
                onBookmarkClick(ayah)
            },
            onFavoriteClick = {
                state.favoriteOverrides = state.favoriteOverrides + (ayah.id to !currentFavorite)
                onFavoriteClick(ayah)
            },
            onCopyClick = {
                copyAyahToClipboard(context, ayah, copiedMessage)
                onCopyClick(ayah)
                state.dismiss()
            },
            onShareClick = {
                shareScope.launch {
                    ContentShareManager.shareBranded(context, Shareables.ayah(context, ayah))
                }
                onShareClick(ayah)
                state.dismiss()
            },
            onTafseerClick = {
                onTafseerClick(ayah)
                state.dismiss()
            },
            onKhatamToggle = { onKhatamToggle(ayah) },
            onTranslationClick = {
                // Read before dismissing: `dismiss()` clears the ayah the sheet needs.
                val forSheet = ayah
                state.dismiss()
                state.translationAyah = forSheet
            },
        )
    }

    state.translationAyah?.let { ayah ->
        AyahTranslationBottomSheet(
            translationLanguage = translationLanguage,
            ayah = ayah,
            surahName = surahMap[ayah.surahNumber]?.nameEnglish,
            showTranslation = showTranslation,
            showTransliteration = showTransliteration,
            sheetState = sheetState,
            onDismissRequest = { state.translationAyah = null },
        )
    }
}

/**
 * Whether the tooltip offers its "Translation" button.
 *
 * The two renderers disagreed here, and this is the stricter of the two answers. [MushafPage]
 * asked only whether the reader has translation or transliteration *switched on*;
 * [MushafLinePage] also required the verse to actually carry one, because it can synthesise a
 * minimal [Ayah] from the printed layout when the host supplies no content lookup, and such an
 * ayah never has either.
 *
 * The strict answer is right for both: a button that opens an empty sheet is a button that
 * should not have been there. The looser one could reach it whenever a verse's translation had
 * not been downloaded yet.
 */
internal fun showsTranslationButton(
    ayah: Ayah,
    showTranslation: Boolean,
    showTransliteration: Boolean,
): Boolean = (showTranslation || showTransliteration) &&
        (ayah.translation != null || ayah.transliteration != null)

/** The verse, its translation if it has one, and its reference — onto the clipboard. */
internal fun copyAyahToClipboard(context: Context, ayah: Ayah, copiedMessage: String) {
    val textToCopy = buildString {
        appendLine(ayah.textArabic)
        if (!ayah.translation.isNullOrBlank()) {
            appendLine(); appendLine(ayah.translation)
        }
        appendLine()
        append(
            context.getString(
                R.string.quran_copy_reference_format,
                ayah.surahNumber,
                ayah.ayahNumber
            )
        )
    }
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(
        ClipData.newPlainText(context.getString(R.string.quran_clipboard_label), textToCopy)
    )
    Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
}
