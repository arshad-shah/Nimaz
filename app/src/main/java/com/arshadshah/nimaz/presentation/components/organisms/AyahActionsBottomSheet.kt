package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.res.stringResource
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.SajdaType
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.molecules.NimazBottomSheet
import com.arshadshah.nimaz.presentation.components.molecules.NimazSheetAction
import com.arshadshah.nimaz.presentation.components.molecules.NimazSheetActionRow
import com.arshadshah.nimaz.presentation.components.molecules.NimazSheetHeader
import com.arshadshah.nimaz.presentation.components.molecules.NimazSheetPreviewCard
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Bottom sheet displayed when user taps an ayah in Mushaf view.
 *
 * Built on the shared [NimazBottomSheet] kit: header (surah + ayah + juz/page
 * badge), an Arabic [NimazSheetPreviewCard], optional sajda indicator and
 * translation card, and a [NimazSheetActionRow] of Play / Bookmark / Favorite /
 * Copy / Share / (Khatam) / Tafseer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AyahActionsBottomSheet(
    ayah: Ayah,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    surahName: String? = null,
    isBookmarked: Boolean = ayah.isBookmarked,
    isFavorite: Boolean = false,
    isKhatamActive: Boolean = false,
    isKhatamRead: Boolean = false,
    sheetState: SheetState = rememberModalBottomSheetState(),
    onPlayClick: (Ayah) -> Unit = {},
    onBookmarkClick: (Ayah) -> Unit = {},
    onFavoriteClick: (Ayah) -> Unit = {},
    onShareClick: (Ayah) -> Unit = {},
    onCopyClick: (Ayah) -> Unit = {},
    onKhatamToggle: (Ayah) -> Unit = {},
    onTafseerClick: (Ayah) -> Unit = {}
) {
    NimazBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
    ) {
        AyahActionsContent(
            ayah = ayah,
            surahName = surahName,
            isBookmarked = isBookmarked,
            isFavorite = isFavorite,
            isKhatamActive = isKhatamActive,
            isKhatamRead = isKhatamRead,
            onPlayClick = onPlayClick,
            onBookmarkClick = onBookmarkClick,
            onFavoriteClick = onFavoriteClick,
            onShareClick = onShareClick,
            onCopyClick = onCopyClick,
            onKhatamToggle = onKhatamToggle,
            onTafseerClick = onTafseerClick
        )
    }
}

/**
 * Content of the ayah actions bottom sheet.
 * Can be used independently for previews or embedded content.
 */
@Composable
fun AyahActionsContent(
    ayah: Ayah,
    modifier: Modifier = Modifier,
    surahName: String? = null,
    isBookmarked: Boolean = ayah.isBookmarked,
    isFavorite: Boolean = false,
    isKhatamActive: Boolean = false,
    isKhatamRead: Boolean = false,
    onPlayClick: (Ayah) -> Unit = {},
    onBookmarkClick: (Ayah) -> Unit = {},
    onFavoriteClick: (Ayah) -> Unit = {},
    onShareClick: (Ayah) -> Unit = {},
    onCopyClick: (Ayah) -> Unit = {},
    onKhatamToggle: (Ayah) -> Unit = {},
    onTafseerClick: (Ayah) -> Unit = {}
) {
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxWidth()) {
        NimazSheetHeader(
            title = surahName ?: stringResource(R.string.surah_number_format, ayah.surahNumber),
            subtitle = stringResource(R.string.ayah_number_format, ayah.ayahNumber),
            badge = stringResource(R.string.juz_page_format, ayah.juzNumber, ayah.pageNumber)
        )

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
            // Arabic text preview
            NimazSheetPreviewCard {
                ArabicText(
                    text = ayah.textArabic,
                    size = ArabicTextSize.MEDIUM,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Sajda indicator (if applicable)
            if (ayah.sajdaType != null) {
                Spacer(modifier = Modifier.height(12.dp))
                SajdaIndicator(sajdaType = ayah.sajdaType, withGlyph = false)
            }

            // Translation text
            if (!ayah.translation.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                NimazSheetPreviewCard {
                    Text(
                        text = ayah.translation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action buttons row
            NimazSheetActionRow(
                actions = buildAyahActions(
                    ayah = ayah,
                    context = context,
                    isBookmarked = isBookmarked,
                    isFavorite = isFavorite,
                    isKhatamActive = isKhatamActive,
                    isKhatamRead = isKhatamRead,
                    onPlayClick = onPlayClick,
                    onBookmarkClick = onBookmarkClick,
                    onFavoriteClick = onFavoriteClick,
                    onShareClick = onShareClick,
                    onCopyClick = onCopyClick,
                    onKhatamToggle = onKhatamToggle,
                    onTafseerClick = onTafseerClick
                )
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun buildAyahActions(
    ayah: Ayah,
    context: Context,
    isBookmarked: Boolean,
    isFavorite: Boolean,
    isKhatamActive: Boolean,
    isKhatamRead: Boolean,
    onPlayClick: (Ayah) -> Unit,
    onBookmarkClick: (Ayah) -> Unit,
    onFavoriteClick: (Ayah) -> Unit,
    onShareClick: (Ayah) -> Unit,
    onCopyClick: (Ayah) -> Unit,
    onKhatamToggle: (Ayah) -> Unit,
    onTafseerClick: (Ayah) -> Unit
): List<NimazSheetAction> = buildList {
    add(
        NimazSheetAction(
            icon = Icons.Default.PlayArrow,
            label = stringResource(R.string.action_play),
            onClick = { onPlayClick(ayah) },
            tint = MaterialTheme.colorScheme.primary
        )
    )
    add(
        NimazSheetAction(
            icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
            label = stringResource(R.string.bookmark),
            onClick = { onBookmarkClick(ayah) },
            tint = if (isBookmarked) NimazColors.QuranColors.BookmarkPrimary else null,
            selected = isBookmarked
        )
    )
    add(
        NimazSheetAction(
            icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            label = stringResource(R.string.action_favorite),
            onClick = { onFavoriteClick(ayah) },
            tint = if (isFavorite) Color(0xFFEF4444) else null,
            selected = isFavorite
        )
    )
    add(
        NimazSheetAction(
            icon = Icons.Default.ContentCopy,
            label = stringResource(R.string.action_copy),
            onClick = {
                copyAyahToClipboard(context, ayah)
                onCopyClick(ayah)
            }
        )
    )
    add(
        NimazSheetAction(
            icon = Icons.Default.Share,
            label = stringResource(R.string.share),
            onClick = {
                shareAyah(context, ayah)
                onShareClick(ayah)
            }
        )
    )
    if (isKhatamActive) {
        add(
            NimazSheetAction(
                icon = if (isKhatamRead) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                label = if (isKhatamRead) stringResource(R.string.unread) else stringResource(R.string.read),
                onClick = { onKhatamToggle(ayah) },
                tint = if (isKhatamRead) Color(0xFF22C55E) else null,
                selected = isKhatamRead
            )
        )
    }
    add(
        NimazSheetAction(
            icon = Icons.AutoMirrored.Filled.MenuBook,
            label = stringResource(R.string.action_tafseer),
            onClick = { onTafseerClick(ayah) },
            tint = MaterialTheme.colorScheme.primary
        )
    )
}

/**
 * Small inline sajda chip. [withGlyph] prefixes the prostration glyph (۩) used in
 * the translation sheet; the actions sheet omits it.
 */
@Composable
internal fun SajdaIndicator(
    sajdaType: SajdaType,
    modifier: Modifier = Modifier,
    withGlyph: Boolean = false
) {
    val label = when (sajdaType) {
        SajdaType.OBLIGATORY -> stringResource(R.string.sajdah_wajib)
        else -> stringResource(R.string.sajdah_recommended)
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFDC2626).copy(alpha = 0.15f),
        modifier = modifier
    ) {
        Text(
            text = if (withGlyph) "۩ $label" else label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFDC2626),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/**
 * Copy ayah text to clipboard.
 */
private fun copyAyahToClipboard(context: Context, ayah: Ayah) {
    val textToCopy = buildString {
        appendLine(ayah.textArabic)
        if (!ayah.translation.isNullOrBlank()) {
            appendLine()
            appendLine(ayah.translation)
        }
        appendLine()
        append("- Surah ${ayah.surahNumber}, Ayah ${ayah.ayahNumber}")
    }

    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Quran Ayah", textToCopy)
    clipboard.setPrimaryClip(clip)

    Toast.makeText(
        context,
        context.getString(R.string.ayah_copied_to_clipboard),
        Toast.LENGTH_SHORT
    ).show()
}

/**
 * Share ayah via intent.
 */
private fun shareAyah(context: Context, ayah: Ayah) {
    val textToShare = buildString {
        appendLine(ayah.textArabic)
        if (!ayah.translation.isNullOrBlank()) {
            appendLine()
            appendLine(ayah.translation)
        }
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

@Preview(showBackground = true, name = "Ayah Actions Content - Basic")
@Composable
private fun AyahActionsContentPreview() {
    NimazTheme {
        Surface {
            AyahActionsContent(
                ayah = sampleAyahForBottomSheet,
                surahName = "Al-Fatihah"
            )
        }
    }
}

@Preview(showBackground = true, name = "Ayah Actions Content - Bookmarked & Favorited")
@Composable
private fun AyahActionsContentBookmarkedPreview() {
    NimazTheme {
        Surface {
            AyahActionsContent(
                ayah = sampleAyahForBottomSheet,
                surahName = "Al-Fatihah",
                isBookmarked = true,
                isFavorite = true
            )
        }
    }
}

@Preview(showBackground = true, name = "Ayah Actions Content - With Sajda")
@Composable
private fun AyahActionsContentWithSajdaPreview() {
    NimazTheme {
        Surface {
            AyahActionsContent(
                ayah = sampleAyahWithSajda,
                surahName = "Al-A'raf"
            )
        }
    }
}

@Preview(showBackground = true, name = "Ayah Actions Content - No Translation")
@Composable
private fun AyahActionsContentNoTranslationPreview() {
    NimazTheme {
        Surface {
            AyahActionsContent(
                ayah = sampleAyahNoTranslation,
                surahName = "Al-Fatihah"
            )
        }
    }
}

// Sample data for previews
private val sampleAyahForBottomSheet = Ayah(
    id = 2,
    surahNumber = 1,
    ayahNumber = 2,
    textArabic = "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ",
    textSimple = "الحمد لله رب العالمين",
    juzNumber = 1,
    hizbNumber = 1,
    rubNumber = 0,
    pageNumber = 1,
    sajdaType = null,
    sajdaNumber = null,
    translation = "All praise is due to Allah, Lord of the worlds.",
    isBookmarked = false
)

private val sampleAyahWithSajda = Ayah(
    id = 1160,
    surahNumber = 7,
    ayahNumber = 206,
    textArabic = "إِنَّ ٱلَّذِينَ عِندَ رَبِّكَ لَا يَسْتَكْبِرُونَ عَنْ عِبَادَتِهِۦ وَيُسَبِّحُونَهُۥ وَلَهُۥ يَسْجُدُونَ",
    textSimple = "إن الذين عند ربك لا يستكبرون عن عبادته ويسبحونه وله يسجدون",
    juzNumber = 9,
    hizbNumber = 18,
    rubNumber = 4,
    pageNumber = 176,
    sajdaType = SajdaType.OBLIGATORY,
    sajdaNumber = 1,
    translation = "Indeed, those who are near your Lord are not prevented by arrogance from His worship, and they exalt Him, and to Him they prostrate.",
    isBookmarked = false
)

private val sampleAyahNoTranslation = Ayah(
    id = 1,
    surahNumber = 1,
    ayahNumber = 1,
    textArabic = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
    textSimple = "بسم الله الرحمن الرحيم",
    juzNumber = 1,
    hizbNumber = 1,
    rubNumber = 0,
    pageNumber = 1,
    sajdaType = null,
    sajdaNumber = null,
    translation = null,
    isBookmarked = false
)
