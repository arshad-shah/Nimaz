package com.arshadshah.nimaz.presentation.components.organisms

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.presentation.components.molecules.AyahTooltip
import com.arshadshah.nimaz.presentation.components.molecules.MushafContinuousText
import com.arshadshah.nimaz.presentation.components.molecules.MushafSurahHeader
import com.arshadshah.nimaz.presentation.components.molecules.sampleFatihahAyahs
import com.arshadshah.nimaz.presentation.components.molecules.sampleSurahBaqarah
import com.arshadshah.nimaz.presentation.components.molecules.sampleSurahFatihah
import com.arshadshah.nimaz.presentation.theme.NimazTheme

private val MushafFrameColor = Color(0xFF0F766E)
private val MushafFrameColorLight = Color(0xFF14B8A6)
private val MushafGoldAccent = Color(0xFFEAB308)

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
    val density = LocalDensity.current

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
                        MushafSurahHeader(
                            surah = surah,
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
                    copyAyahToClipboard(context, ayah)
                    onCopyClick(ayah)
                    tooltipAyah = null
                },
                onShareClick = {
                    shareAyah(context, ayah)
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
            .clip(RoundedCornerShape(4.dp))
            .border(3.dp, MushafFrameColor, RoundedCornerShape(4.dp))
            .padding(3.dp)
            .border(1.dp, MushafFrameColorLight, RoundedCornerShape(2.dp))
            .padding(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MushafOrnamentalLine()
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                content()
            }
            MushafOrnamentalLine()

            // Page number footer
            Text(
                text = pageNumber.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MushafFrameColor,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun MushafOrnamentalLine(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        HorizontalDivider(thickness = 1.dp, color = MushafGoldAccent.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(2.dp))
        HorizontalDivider(thickness = 2.dp, color = MushafFrameColor)
        Spacer(modifier = Modifier.height(2.dp))
        HorizontalDivider(thickness = 1.dp, color = MushafGoldAccent.copy(alpha = 0.6f))
    }
}

private fun copyAyahToClipboard(context: Context, ayah: Ayah) {
    val textToCopy = buildString {
        appendLine(ayah.textArabic)
        if (!ayah.translation.isNullOrBlank()) { appendLine(); appendLine(ayah.translation) }
        appendLine()
        append("- Surah ${ayah.surahNumber}, Ayah ${ayah.ayahNumber}")
    }
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Quran Ayah", textToCopy))
    Toast.makeText(context, context.getString(R.string.ayah_copied_to_clipboard), Toast.LENGTH_SHORT).show()
}

private fun shareAyah(context: Context, ayah: Ayah) {
    val textToShare = buildString {
        appendLine(ayah.textArabic)
        if (!ayah.translation.isNullOrBlank()) { appendLine(); appendLine(ayah.translation) }
        appendLine()
        append("- Surah ${ayah.surahNumber}, Ayah ${ayah.ayahNumber}")
    }
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, textToShare)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Ayah"))
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true, name = "Mushaf Page - Al-Fatihah")
@Composable
private fun MushafPagePreview() {
    NimazTheme {
        MushafPage(
            pageNumber = 1,
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