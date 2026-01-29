package com.arshadshah.nimaz.presentation.components.organisms

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
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.presentation.components.molecules.MushafContinuousText
import com.arshadshah.nimaz.presentation.components.molecules.MushafSurahHeader
import com.arshadshah.nimaz.presentation.components.molecules.sampleFatihahAyahs
import com.arshadshah.nimaz.presentation.components.molecules.sampleSurahBaqarah
import com.arshadshah.nimaz.presentation.components.molecules.sampleSurahFatihah
import com.arshadshah.nimaz.presentation.theme.NimazTheme

// Mushaf frame colors
private val MushafFrameColor = Color(0xFF0F766E)  // Teal border
private val MushafFrameColorLight = Color(0xFF14B8A6)  // Lighter teal for inner border
private val MushafGoldAccent = Color(0xFFEAB308)  // Gold accent

/**
 * Main Mushaf page component that displays Quran text in a traditional Mushaf style.
 *
 * Features:
 * - Page header with Juz, Hizb, and page info
 * - Page navigation bar
 * - Surah headers when a new surah starts
 * - Continuous Arabic text with clickable ayahs
 * - Bottom sheet for ayah actions (play, bookmark, favorite, share, copy)
 * - Tajweed color coding when enabled
 *
 * @param pageNumber The Quran page number (1-604)
 * @param ayahs List of ayahs on this page
 * @param surahMap Map of surah numbers to Surah objects for header display
 * @param modifier Modifier for the composable
 * @param arabicFontSize Font size for Arabic text
 * @param totalPages Total number of Quran pages (default 604)
 * @param highlightedAyahId ID of currently highlighted ayah (e.g., during audio playback)
 * @param favoriteAyahIds Set of ayah IDs that are favorited
 * @param showTajweed Whether to show tajweed color markers
 * @param onNavigatePrevious Callback when previous page navigation is clicked
 * @param onNavigateNext Callback when next page navigation is clicked
 * @param onBookmarkClick Callback when bookmark action is clicked
 * @param onFavoriteClick Callback when favorite action is clicked
 * @param onPlayClick Callback when play action is clicked
 * @param onShareClick Callback when share action is clicked
 * @param onCopyClick Callback when copy action is clicked
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MushafPage(
    pageNumber: Int,
    ayahs: List<Ayah>,
    surahMap: Map<Int, Surah>,
    modifier: Modifier = Modifier,
    arabicFontSize: Float = 28f,
    totalPages: Int = 604,
    highlightedAyahId: Int? = null,
    favoriteAyahIds: Set<Int> = emptySet(),
    showTajweed: Boolean = false,
    onNavigatePrevious: () -> Unit = {},
    onNavigateNext: () -> Unit = {},
    onBookmarkClick: (Ayah) -> Unit = {},
    onFavoriteClick: (Ayah) -> Unit = {},
    onPlayClick: (Ayah) -> Unit = {},
    onShareClick: (Ayah) -> Unit = {},
    onCopyClick: (Ayah) -> Unit = {}
) {
    // State for selected ayah and bottom sheet
    var selectedAyah by remember { mutableStateOf<Ayah?>(null) }
    val sheetState = rememberModalBottomSheetState()

    // Group ayahs by surah for rendering with headers
    val ayahsBySurah = remember(ayahs) {
        ayahs.groupBy { it.surahNumber }
    }

    // Find the first surah on this page (for header display)
    val firstAyah = ayahs.firstOrNull()
    val juzNumber = firstAyah?.juzNumber ?: 0
    val hizbNumber = firstAyah?.hizbNumber ?: 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Page info header (outside the frame)
        MushafPageHeader(
            pageNumber = pageNumber,
            juzNumber = juzNumber,
            hizbNumber = hizbNumber,
            ayahCount = ayahs.size
        )

        // Page navigation bar (outside the frame)
        MushafNavigationBar(
            currentPage = pageNumber,
            totalPages = totalPages,
            onNavigatePrevious = onNavigatePrevious,
            onNavigateNext = onNavigateNext
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Decorative Mushaf frame containing the Quran text
        MushafFrame(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                // Render each surah section
                ayahsBySurah.forEach { (surahNumber, surahAyahs) ->
                    val surah = surahMap[surahNumber]
                    val isNewSurah = surahAyahs.firstOrNull()?.ayahNumber == 1

                    // Show surah header if this is the start of a new surah
                    if (isNewSurah && surah != null) {
                        MushafSurahHeader(
                            surah = surah,
                            showBismillah = surahNumber != 1 && surahNumber != 9
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Continuous text for this surah's ayahs on this page
                    MushafContinuousText(
                        ayahs = surahAyahs,
                        onAyahClick = { ayah ->
                            selectedAyah = ayah
                        },
                        highlightedAyahId = highlightedAyahId,
                        arabicFontSize = arabicFontSize,
                        showTajweed = showTajweed,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        // Bottom padding
        Spacer(modifier = Modifier.height(16.dp))
    }

    // Bottom sheet for ayah actions
    selectedAyah?.let { ayah ->
        val surah = surahMap[ayah.surahNumber]

        AyahActionsBottomSheet(
            ayah = ayah,
            surahName = surah?.nameEnglish,
            isBookmarked = ayah.isBookmarked,
            isFavorite = ayah.id in favoriteAyahIds,
            sheetState = sheetState,
            onDismissRequest = { selectedAyah = null },
            onPlayClick = { clickedAyah ->
                onPlayClick(clickedAyah)
                selectedAyah = null
            },
            onBookmarkClick = { clickedAyah ->
                onBookmarkClick(clickedAyah)
            },
            onFavoriteClick = { clickedAyah ->
                onFavoriteClick(clickedAyah)
            },
            onShareClick = { clickedAyah ->
                onShareClick(clickedAyah)
            },
            onCopyClick = { clickedAyah ->
                onCopyClick(clickedAyah)
            }
        )
    }
}

/**
 * Page header showing page number, juz, hizb, and ayah count.
 */
@Composable
private fun MushafPageHeader(
    pageNumber: Int,
    juzNumber: Int,
    hizbNumber: Int,
    ayahCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Juz number
            HeaderInfoItem(
                value = juzNumber.toString(),
                label = "Juz",
                color = MaterialTheme.colorScheme.primary
            )

            VerticalDivider()

            // Hizb number
            HeaderInfoItem(
                value = hizbNumber.toString(),
                label = "Hizb",
                color = MaterialTheme.colorScheme.tertiary
            )

            VerticalDivider()

            // Ayah count
            HeaderInfoItem(
                value = ayahCount.toString(),
                label = "Ayahs",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HeaderInfoItem(
    value: String,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun VerticalDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(1.dp)
            .height(28.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    )
}

/**
 * Decorative frame that wraps the Quran text content, mimicking traditional Mushaf styling.
 * Features double border lines with ornamental appearance.
 */
@Composable
private fun MushafFrame(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .border(
                width = 3.dp,
                color = MushafFrameColor,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(3.dp)
            .border(
                width = 1.dp,
                color = MushafFrameColorLight,
                shape = RoundedCornerShape(2.dp)
            )
            .padding(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top ornamental line
            MushafOrnamentalLine()

            // Content
            content()

            // Bottom ornamental line
            MushafOrnamentalLine()
        }
    }
}

/**
 * Ornamental horizontal line used at top and bottom of the Mushaf frame.
 */
@Composable
private fun MushafOrnamentalLine(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Triple line ornament
        HorizontalDivider(
            thickness = 1.dp,
            color = MushafGoldAccent.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        HorizontalDivider(
            thickness = 2.dp,
            color = MushafFrameColor
        )
        Spacer(modifier = Modifier.height(2.dp))
        HorizontalDivider(
            thickness = 1.dp,
            color = MushafGoldAccent.copy(alpha = 0.6f)
        )
    }
}

/**
 * Navigation bar for page-by-page navigation in Mushaf mode.
 * Note: In RTL pager context, "previous" goes to higher page numbers (right swipe)
 * and "next" goes to lower page numbers (left swipe).
 */
@Composable
private fun MushafNavigationBar(
    currentPage: Int,
    totalPages: Int,
    onNavigatePrevious: () -> Unit,
    onNavigateNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous page button (goes to higher page number in RTL)
            IconButton(
                onClick = onNavigatePrevious,
                enabled = currentPage < totalPages,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                    contentDescription = "Previous Page",
                    tint = if (currentPage < totalPages)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = "Page $currentPage of $totalPages",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Next page button (goes to lower page number in RTL)
            IconButton(
                onClick = onNavigateNext,
                enabled = currentPage > 1,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                    contentDescription = "Next Page",
                    tint = if (currentPage > 1)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
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

@Preview(showBackground = true, name = "Mushaf Page - With Favorites")
@Composable
private fun MushafPageWithFavoritesPreview() {
    NimazTheme {
        MushafPage(
            pageNumber = 1,
            ayahs = sampleFatihahAyahs,
            surahMap = mapOf(1 to sampleSurahFatihah),
            arabicFontSize = 28f,
            favoriteAyahIds = setOf(2, 5)
        )
    }
}

@Preview(showBackground = true, name = "Mushaf Page - Multi-Surah")
@Composable
private fun MushafPageMultiSurahPreview() {
    NimazTheme {
        // Simulate a page with end of Al-Fatihah and start of Al-Baqarah
        val multiSurahAyahs = sampleFatihahAyahs + sampleBaqarahFirstAyahs

        MushafPage(
            pageNumber = 2,
            ayahs = multiSurahAyahs,
            surahMap = mapOf(
                1 to sampleSurahFatihah,
                2 to sampleSurahBaqarah
            ),
            arabicFontSize = 28f
        )
    }
}

@Preview(showBackground = true, name = "Mushaf Page Header")
@Composable
private fun MushafPageHeaderPreview() {
    NimazTheme {
        MushafPageHeader(
            pageNumber = 1,
            juzNumber = 1,
            hizbNumber = 1,
            ayahCount = 7
        )
    }
}

// Additional sample data for multi-surah preview
private val sampleBaqarahFirstAyahs = listOf(
    Ayah(
        id = 8,
        surahNumber = 2,
        ayahNumber = 1,
        textArabic = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ الٓمٓ",
        textSimple = "الم",
        juzNumber = 1,
        hizbNumber = 1,
        rubNumber = 1,
        pageNumber = 2,
        sajdaType = null,
        sajdaNumber = null,
        translation = "Alif, Lam, Meem.",
        isBookmarked = false
    ),
    Ayah(
        id = 9,
        surahNumber = 2,
        ayahNumber = 2,
        textArabic = "ذَٰلِكَ ٱلْكِتَٰبُ لَا رَيْبَ فِيهِ هُدًى لِّلْمُتَّقِينَ",
        textSimple = "ذلك الكتاب لا ريب فيه هدى للمتقين",
        juzNumber = 1,
        hizbNumber = 1,
        rubNumber = 1,
        pageNumber = 2,
        sajdaType = null,
        sajdaNumber = null,
        translation = "This is the Book about which there is no doubt, a guidance for those conscious of Allah.",
        isBookmarked = false
    ),
    Ayah(
        id = 10,
        surahNumber = 2,
        ayahNumber = 3,
        textArabic = "ٱلَّذِينَ يُؤْمِنُونَ بِٱلْغَيْبِ وَيُقِيمُونَ ٱلصَّلَوٰةَ وَمِمَّا رَزَقْنَٰهُمْ يُنفِقُونَ",
        textSimple = "الذين يؤمنون بالغيب ويقيمون الصلاة ومما رزقناهم ينفقون",
        juzNumber = 1,
        hizbNumber = 1,
        rubNumber = 1,
        pageNumber = 2,
        sajdaType = null,
        sajdaNumber = null,
        translation = "Who believe in the unseen, establish prayer, and spend out of what We have provided for them.",
        isBookmarked = false
    )
)
