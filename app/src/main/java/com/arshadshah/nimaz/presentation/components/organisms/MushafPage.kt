package com.arshadshah.nimaz.presentation.components.organisms

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.share.ContentShareManager
import com.arshadshah.nimaz.core.share.Shareables
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.presentation.components.atoms.DiamondFloret
import com.arshadshah.nimaz.presentation.components.atoms.ShamsaMedallion
import com.arshadshah.nimaz.presentation.components.molecules.AyahTooltip
import com.arshadshah.nimaz.presentation.components.molecules.MushafContinuousText
import com.arshadshah.nimaz.presentation.components.molecules.SurahHeaderCartouche
import com.arshadshah.nimaz.presentation.components.molecules.sampleFatihahAyahs
import com.arshadshah.nimaz.presentation.components.molecules.sampleSurahBaqarah
import kotlinx.coroutines.launch
import com.arshadshah.nimaz.presentation.components.molecules.sampleSurahFatihah
import com.arshadshah.nimaz.presentation.theme.AmiriFontFamily
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme

// Frame palette — gold outer keyline + teal inner keyline, matching the cartouche.
private val MushafFrameTeal = NimazColors.Primary700
private val MushafGoldAccent = NimazColors.Gold500

/**
 * Main Mushaf page component.
 *
 * Flow:
 *   Ayah tap → inline highlight + [AyahTooltip] with beak pointing at tapped line
 *          → actions: Play, Bookmark, Favorite, Copy, Share, Tafseer, Khatam
 *          → "Translation" button (if showTranslation || showTransliteration)
 *              → [AyahTranslationBottomSheet]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MushafPage(
    pageNumber: Int,
    ayahs: List<Ayah>,
    surahMap: Map<Int, Surah>,
    modifier: Modifier = Modifier,
    arabicFontSize: Float = 28f,
    arabicFontFamily: FontFamily = AmiriFontFamily,
    highlightedAyahId: Int? = null,
    favoriteAyahIds: Set<Int> = emptySet(),
    showTajweed: Boolean = false,
    showTranslation: Boolean = true,
    showTransliteration: Boolean = false,
    onBookmarkClick: (Ayah) -> Unit = {},
    onFavoriteClick: (Ayah) -> Unit = {},
    onPlayClick: (Ayah) -> Unit = {},
    onShareClick: (Ayah) -> Unit = {},
    onCopyClick: (Ayah) -> Unit = {},
    isKhatamActive: Boolean = false,
    khatamReadAyahIds: Set<Int> = emptySet(),
    onKhatamToggle: (Ayah) -> Unit = {},
    onTafseerClick: (Ayah) -> Unit = {}
) {
    val context = LocalContext.current
    val shareScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val copiedMessage = stringResource(R.string.ayah_copied_to_clipboard)

    // Tooltip state
    var tooltipAyah by remember { mutableStateOf<Ayah?>(null) }
    var tooltipTapY by remember { mutableFloatStateOf(0f) }
    var parentHeight by remember { mutableFloatStateOf(0f) }

    // Translation bottom sheet state
    var translationAyah by remember { mutableStateOf<Ayah?>(null) }
    val sheetState = rememberModalBottomSheetState()

    // Live overrides for bookmark/favorite while tooltip is open
    var bookmarkOverride by remember { mutableStateOf<Boolean?>(null) }
    var favoriteOverrides by remember { mutableStateOf<Map<Int, Boolean>>(emptyMap()) }

    LaunchedEffect(tooltipAyah) {
        bookmarkOverride = null
    }

    // Whether translation button appears in tooltip
    val showTranslationButton = showTranslation || showTransliteration

    // Group ayahs by surah
    val ayahsBySurah = remember(ayahs) {
        ayahs.groupBy { it.surahNumber }
    }

    // Track scroll offset to convert local tap Y to page-level Y
    val scrollState = rememberScrollState()

    // Track parent window Y for tooltip position correction
    var parentWindowY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                parentHeight = coordinates.size.height.toFloat()
                parentWindowY = coordinates.positionInWindow().y
            }
    ) {
        // Mushaf frame fills all available space; content scrolls inside
        MushafFrame(
            pageNumber = pageNumber,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(vertical = 8.dp)
            ) {
                ayahsBySurah.forEach { (surahNumber, surahAyahs) ->
                    val surah = surahMap[surahNumber]
                    val isNewSurah = surahAyahs.firstOrNull()?.ayahNumber == 1

                    if (isNewSurah && surah != null) {
                        SurahHeaderCartouche(
                            surah = surah,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            showBismillah = surahNumber != 1 && surahNumber != 9
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Track this text block's window position for tooltip
                    var textWindowY by remember { mutableFloatStateOf(0f) }

                    Box(
                        modifier = Modifier.onGloballyPositioned { coords ->
                            textWindowY = coords.positionInWindow().y
                        }
                    ) {
                        MushafContinuousText(
                            ayahs = surahAyahs,
                            onAyahClick = { ayah, localTapY ->
                                tooltipAyah = ayah
                                // Convert local Y within text to viewport-relative Y
                                // positionInWindow already accounts for scroll state
                                tooltipTapY = (textWindowY - parentWindowY) + localTapY
                            },
                            highlightedAyahId = highlightedAyahId,
                            selectedAyahId = tooltipAyah?.id,
                            arabicFontSize = arabicFontSize,
                            arabicFontFamily = arabicFontFamily,
                            showTajweed = showTajweed,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        // Tooltip overlay (anchored near tap position)
        tooltipAyah?.let { ayah ->
            val currentBookmarked = bookmarkOverride ?: ayah.isBookmarked
            val currentFavorite =
                favoriteOverrides[ayah.id] ?: (ayah.id in favoriteAyahIds)

            AyahTooltip(
                tapY = tooltipTapY,
                parentHeight = parentHeight,
                isBookmarked = currentBookmarked,
                isFavorite = currentFavorite,
                isKhatamActive = isKhatamActive,
                isKhatamRead = ayah.id in khatamReadAyahIds,
                showTranslationButton = showTranslationButton,
                onDismiss = { tooltipAyah = null },
                onPlayClick = {
                    onPlayClick(ayah)
                    tooltipAyah = null
                },
                onBookmarkClick = {
                    bookmarkOverride = !currentBookmarked
                    onBookmarkClick(ayah)
                },
                onFavoriteClick = {
                    favoriteOverrides = favoriteOverrides + (ayah.id to !currentFavorite)
                    onFavoriteClick(ayah)
                },
                onCopyClick = {
                    copyAyahToClipboard(context, ayah, copiedMessage)
                    onCopyClick(ayah)
                    tooltipAyah = null
                },
                onShareClick = {
                    shareScope.launch {
                        ContentShareManager.shareBranded(context, Shareables.ayah(context, ayah))
                    }
                    onShareClick(ayah)
                    tooltipAyah = null
                },
                onTafseerClick = {
                    onTafseerClick(ayah)
                    tooltipAyah = null
                },
                onKhatamToggle = {
                    onKhatamToggle(ayah)
                },
                onTranslationClick = {
                    val forSheet = ayah
                    tooltipAyah = null
                    translationAyah = forSheet
                }
            )
        }
    }

    // Translation bottom sheet
    translationAyah?.let { ayah ->
        val surah = surahMap[ayah.surahNumber]

        AyahTranslationBottomSheet(
            ayah = ayah,
            surahName = surah?.nameEnglish,
            showTranslation = showTranslation,
            showTransliteration = showTransliteration,
            sheetState = sheetState,
            onDismissRequest = { translationAyah = null }
        )
    }
}

// ---------------------------------------------------------------------------
// Internal sub-components
// ---------------------------------------------------------------------------

@Composable
private fun MushafFrame(
    pageNumber: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .border(1.5.dp, MushafGoldAccent, RoundedCornerShape(5.dp))
            .padding(3.dp)
            .border(1.dp, MushafFrameTeal, RoundedCornerShape(3.dp))
            .padding(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MushafOrnamentalLine()
            Box(modifier = Modifier
                .weight(1f)
                .fillMaxWidth()) {
                content()
            }
            MushafOrnamentalLine()

            // Page number footer — shamsa medallion, matching the surah number ornament
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                ShamsaMedallion(
                    number = pageNumber,
                    size = 45.dp,
                    numberStyle = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

/**
 * Ornamental divider line: a gold hairline fading out to both margins with a
 * central diamond floret — the same floret as the cartouche's Basmala line.
 */
@Composable
private fun MushafOrnamentalLine(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, MushafGoldAccent)))
        )
        DiamondFloret(color = MushafGoldAccent, size = 7.dp)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(MushafGoldAccent, Color.Transparent)))
        )
    }
}

private fun copyAyahToClipboard(context: Context, ayah: Ayah, copiedMessage: String) {
    val textToCopy = buildString {
        appendLine(ayah.textArabic)
        if (!ayah.translation.isNullOrBlank()) {
            appendLine(); appendLine(ayah.translation)
        }
        appendLine()
        append("- Surah ${ayah.surahNumber}, Ayah ${ayah.ayahNumber}")
    }
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Quran Ayah", textToCopy))
    Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true, name = "Mushaf Page - Al-Fatihah")
@Composable
private fun MushafPagePreview() {
    NimazTheme {
        MushafPage(
            pageNumber = 604,
            ayahs = sampleFatihahAyahs,
            surahMap = mapOf(1 to sampleSurahFatihah),
            arabicFontSize = 28f
        )
    }
}

@Preview(showBackground = true, name = "Mushaf Page - With Highlight")
@Composable
private fun MushafPageWithHighlightPreview() {
    NimazTheme {
        MushafPage(
            pageNumber = 1,
            ayahs = sampleFatihahAyahs,
            surahMap = mapOf(1 to sampleSurahFatihah),
            arabicFontSize = 28f,
            highlightedAyahId = 3
        )
    }
}

@Preview(showBackground = true, name = "Mushaf Page - Multi-Surah")
@Composable
private fun MushafPageMultiSurahPreview() {
    NimazTheme {
        val multiSurahAyahs = sampleFatihahAyahs + sampleBaqarahFirstAyahs
        MushafPage(
            pageNumber = 2,
            ayahs = multiSurahAyahs,
            surahMap = mapOf(1 to sampleSurahFatihah, 2 to sampleSurahBaqarah),
            arabicFontSize = 28f
        )
    }
}

private val sampleBaqarahFirstAyahs = listOf(
    Ayah(
        id = 8, surahNumber = 2, ayahNumber = 1,
        textArabic = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ الٓمٓ",
        textSimple = "الم",
        juzNumber = 1, hizbNumber = 1, rubNumber = 1, pageNumber = 2,
        sajdaType = null, sajdaNumber = null,
        translation = "Alif, Lam, Meem.",
        isBookmarked = false
    ),
    Ayah(
        id = 9, surahNumber = 2, ayahNumber = 2,
        textArabic = "ذَٰلِكَ ٱلْكِتَٰبُ لَا رَيْبَ فِيهِ هُدًى لِّلْمُتَّقِينَ",
        textSimple = "ذلك الكتاب لا ريب فيه هدى للمتقين",
        juzNumber = 1, hizbNumber = 1, rubNumber = 1, pageNumber = 2,
        sajdaType = null, sajdaNumber = null,
        translation = "This is the Book about which there is no doubt, a guidance for those conscious of Allah.",
        isBookmarked = false
    ),
    Ayah(
        id = 10, surahNumber = 2, ayahNumber = 3,
        textArabic = "ٱلَّذِينَ يُؤْمِنُونَ بِٱلْغَيْبِ وَيُقِيمُونَ ٱلصَّلَوٰةَ وَمِمَّا رَزَقْنَٰهُمْ يُنفِقُونَ",
        textSimple = "الذين يؤمنون بالغيب ويقيمون الصلاة ومما رزقناهم ينفقون",
        juzNumber = 1, hizbNumber = 1, rubNumber = 1, pageNumber = 2,
        sajdaType = null, sajdaNumber = null,
        translation = "Who believe in the unseen, establish prayer, and spend out of what We have provided for them.",
        isBookmarked = false
    )
)