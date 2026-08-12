package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.MushafLineType
import com.arshadshah.nimaz.domain.model.MushafPageLayout
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.model.TranslationLanguage
import com.arshadshah.nimaz.presentation.components.molecules.AyahTooltip
import com.arshadshah.nimaz.presentation.components.molecules.MushafDivisionMarks
import com.arshadshah.nimaz.presentation.components.molecules.MushafLineLayout
import com.arshadshah.nimaz.presentation.components.molecules.QuranFrame
import com.arshadshah.nimaz.presentation.components.molecules.QuranFrameVariant
import com.arshadshah.nimaz.presentation.components.molecules.sampleMushafPageLayout
import com.arshadshah.nimaz.presentation.components.molecules.sampleSurahFatihah
import com.arshadshah.nimaz.presentation.theme.IndoPakFontFamily
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * The 16-line IndoPak Mushaf page organism (sub-task 5/7 of #263) — the line-accurate
 * counterpart to [MushafPage]. It hosts the line renderer [MushafLineLayout] inside the shared
 * [QuranFrame] and layers on the exact same interactions as the ayah-keyed page:
 *
 *   Ayah tap → inline highlight + [AyahTooltip] (Play, Bookmark, Favorite, Copy, Share,
 *              Tafseer, Khatam) → "Translation" → [AyahTranslationBottomSheet].
 *
 * Unlike [MushafPage] it is driven by a [MushafPageLayout] (grouped by printed line) rather
 * than a `List<Ayah>`. Ayah *content* needed for the tooltip's translation sheet / copy /
 * share is resolved through [ayahLookup]; when the host can't supply a full [Ayah] the page
 * still works for every id-only action (play, bookmark, favourite, tafseer, khatam) by
 * reconstructing a minimal [Ayah] from the layout itself, so no interaction is lost.
 *
 * @param ayahLookup resolves a full [Ayah] (with translation/transliteration) by global ayah
 *   id; return `null` when unavailable and the page falls back to layout-derived data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MushafLinePage(
    pageNumber: Int,
    layout: MushafPageLayout,
    surahMap: Map<Int, Surah>,
    modifier: Modifier = Modifier,
    arabicFontSize: Float = 24f,
    arabicFontFamily: FontFamily = IndoPakFontFamily,
    highlightedAyahId: Int? = null,
    favoriteAyahIds: Set<Int> = emptySet(),
    showTranslation: Boolean = true,
    showTransliteration: Boolean = false,
    /** Language of the translation prose in the ayah sheet — decides face and leading. */
    translationLanguage: TranslationLanguage = TranslationLanguage.ENGLISH,
    ayahLookup: (Int) -> Ayah? = { null },
    onBookmarkClick: (Ayah) -> Unit = {},
    onFavoriteClick: (Ayah) -> Unit = {},
    onPlayClick: (Ayah) -> Unit = {},
    onShareClick: (Ayah) -> Unit = {},
    onCopyClick: (Ayah) -> Unit = {},
    isKhatamActive: Boolean = false,
    khatamReadAyahIds: Set<Int> = emptySet(),
    onKhatamToggle: (Ayah) -> Unit = {},
    onTafseerClick: (Ayah) -> Unit = {},
) {
    // The tooltip, its actions and the translation sheet — shared with MushafPage, which draws
    // a different page and means the same thing by a tap.
    val actions = rememberMushafAyahActionsState()
    var parentHeight by remember { mutableFloatStateOf(0f) }
    var parentWindowY by remember { mutableFloatStateOf(0f) }

    // Per-ayah metadata reconstructed from the layout — lets id-only taps produce a usable
    // Ayah for the tooltip even when the host supplies no full-content lookup.
    val ayahMeta = remember(layout) { buildAyahMeta(layout) }

    // The page's structural signs, resolved once per page rather than per word. A page is
    // ~15 lines of ~8 words, and ayahLookup reads the database; doing this inline in the
    // renderer would turn one map build into a hundred lookups on every recomposition.
    val divisionMarks: Map<Int, MushafDivisionMarks> = remember(layout, ayahLookup) {
        layout.lines
            .asSequence()
            .flatMap { it.words.asSequence() }
            .map { it.ayahId }
            .distinct()
            .mapNotNull { id ->
                val ayah = ayahLookup(id) ?: return@mapNotNull null
                val marks = MushafDivisionMarks(
                    opensQuarter = ayah.isRubStart,
                    closesRuku = ayah.isRukuEnd,
                    hasSajda = ayah.sajdaType != null,
                )
                // Only verses that actually carry a sign are worth keeping.
                if (marks == MushafDivisionMarks.NONE) null else id to marks
            }
            .toMap()
    }

    fun resolveAyah(ayahId: Int): Ayah? {
        ayahLookup(ayahId)?.let { return it }
        val meta = ayahMeta[ayahId] ?: return null
        return Ayah(
            id = ayahId,
            surahNumber = meta.surahId,
            ayahNumber = meta.ayahNumber,
            textArabic = meta.text,
            textSimple = meta.text,
            juzNumber = 0,
            hizbNumber = 0,
            rubNumber = 0,
            pageNumber = layout.page,
            sajdaType = null,
            sajdaNumber = null,
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                parentHeight = coordinates.size.height.toFloat()
                parentWindowY = coordinates.positionInWindow().y
            }
    ) {
        QuranFrame(
            variant = QuranFrameVariant.READER,
            number = pageNumber,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            ) {
                MushafLineLayout(
                    lines = layout.lines,
                    surahMap = surahMap,
                    arabicFontSize = arabicFontSize,
                    arabicFontFamily = arabicFontFamily,
                    highlightedAyahId = highlightedAyahId,
                    selectedAyahId = actions.tooltipAyah?.id,
                    divisionMarks = { divisionMarks[it] ?: MushafDivisionMarks.NONE },
                    onAyahClick = { ayahId, tapWindowY ->
                        resolveAyah(ayahId)?.let { ayah ->
                            actions.show(ayah, tapWindowY - parentWindowY)
                        }
                    },
                )
            }
        }

        MushafAyahActions(
            state = actions,
            parentHeight = parentHeight,
            surahMap = surahMap,
            favoriteAyahIds = favoriteAyahIds,
            isKhatamActive = isKhatamActive,
            khatamReadAyahIds = khatamReadAyahIds,
            showTranslation = showTranslation,
            showTransliteration = showTransliteration,
            translationLanguage = translationLanguage,
            onPlayClick = onPlayClick,
            onBookmarkClick = onBookmarkClick,
            onFavoriteClick = onFavoriteClick,
            onCopyClick = onCopyClick,
            onShareClick = onShareClick,
            onTafseerClick = onTafseerClick,
            onKhatamToggle = onKhatamToggle,
        )
    }
}

/** Ayah metadata (surah, number, on-page glyph text) reconstructed from a page's lines. */
private data class LayoutAyahMeta(val surahId: Int, val ayahNumber: Int, val text: String)

private fun buildAyahMeta(layout: MushafPageLayout): Map<Int, LayoutAyahMeta> =
    buildMap {
        layout.lines.filter { it.type == MushafLineType.AYAH }.forEach { line ->
            line.words.forEach { word ->
                val existing = this[word.ayahId]
                val text = if (existing == null) word.text else "${existing.text} ${word.text}"
                put(
                    word.ayahId,
                    LayoutAyahMeta(
                        surahId = line.surahId,
                        ayahNumber = word.ayahNumber,
                        text = text
                    )
                )
            }
        }
    }

// ==================== PREVIEWS ====================

@Preview(showBackground = true, name = "Mushaf Line Page — Al-Fatihah (Light)")
@Composable
private fun MushafLinePageLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        MushafLinePage(
            pageNumber = 1,
            layout = sampleMushafPageLayout,
            surahMap = mapOf(1 to sampleSurahFatihah),
        )
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF0A0A08,
    name = "Mushaf Line Page — Al-Fatihah (Dark)"
)
@Composable
private fun MushafLinePageDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        MushafLinePage(
            pageNumber = 1,
            layout = sampleMushafPageLayout,
            surahMap = mapOf(1 to sampleSurahFatihah),
            highlightedAyahId = 5,
        )
    }
}
