package com.arshadshah.nimaz.presentation.components.organisms

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.SajdaType
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazBottomSheet
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Bottom sheet displayed when user taps an ayah in Mushaf view.
 *
 * Contents:
 * - Header: Surah name + Ayah number + Juz/Page info
 * - Arabic text preview
 * - Sajda indicator (if applicable)
 * - Translation text
 * - Action buttons row: Play, Bookmark, Favorite, Copy, Share
 *
 * @param ayah The ayah to show actions for
 * @param surahName Optional surah name for display
 * @param isBookmarked Whether the ayah is bookmarked
 * @param isFavorite Whether the ayah is favorited
 * @param onDismissRequest Callback when sheet is dismissed
 * @param onPlayClick Callback when play is clicked
 * @param onBookmarkClick Callback when bookmark is clicked
 * @param onFavoriteClick Callback when favorite is clicked
 * @param onShareClick Callback when share is clicked
 * @param onCopyClick Callback when copy is clicked
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
    sheetState: SheetState = rememberModalBottomSheetState(),
    onPlayClick: (Ayah) -> Unit = {},
    onBookmarkClick: (Ayah) -> Unit = {},
    onFavoriteClick: (Ayah) -> Unit = {},
    onShareClick: (Ayah) -> Unit = {},
    onCopyClick: (Ayah) -> Unit = {}
) {
    NimazBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier
    ) {
        AyahActionsContent(
            ayah = ayah,
            surahName = surahName,
            isBookmarked = isBookmarked,
            isFavorite = isFavorite,
            onPlayClick = onPlayClick,
            onBookmarkClick = onBookmarkClick,
            onFavoriteClick = onFavoriteClick,
            onShareClick = onShareClick,
            onCopyClick = onCopyClick
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
    onPlayClick: (Ayah) -> Unit = {},
    onBookmarkClick: (Ayah) -> Unit = {},
    onFavoriteClick: (Ayah) -> Unit = {},
    onShareClick: (Ayah) -> Unit = {},
    onCopyClick: (Ayah) -> Unit = {}
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Header: Surah name + Ayah number + Juz/Page info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = surahName ?: "Surah ${ayah.surahNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Ayah ${ayah.ayahNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Juz and Page info
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Text(
                    text = "Juz ${ayah.juzNumber} | P${ayah.pageNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Arabic text preview
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            ArabicText(
                text = ayah.textArabic,
                size = ArabicTextSize.MEDIUM,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }

        // Sajda indicator (if applicable)
        if (ayah.sajdaType != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFDC2626).copy(alpha = 0.15f)
            ) {
                Text(
                    text = if (ayah.sajdaType == SajdaType.OBLIGATORY) {
                        "Sajdah (Wajib)"
                    } else {
                        "Sajdah (Recommended)"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFDC2626),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Translation text
        if (!ayah.translation.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = ayah.translation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play button
            ActionButton(
                icon = Icons.Default.PlayArrow,
                label = "Play",
                onClick = { onPlayClick(ayah) },
                tint = MaterialTheme.colorScheme.primary
            )

            // Bookmark button
            ActionButton(
                icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                label = "Bookmark",
                onClick = { onBookmarkClick(ayah) },
                tint = if (isBookmarked) NimazColors.QuranColors.BookmarkPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Favorite button
            ActionButton(
                icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                label = "Favorite",
                onClick = { onFavoriteClick(ayah) },
                tint = if (isFavorite) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Copy button
            ActionButton(
                icon = Icons.Default.ContentCopy,
                label = "Copy",
                onClick = {
                    copyAyahToClipboard(context, ayah)
                    onCopyClick(ayah)
                },
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Share button
            ActionButton(
                icon = Icons.Default.Share,
                label = "Share",
                onClick = {
                    shareAyah(context, ayah)
                    onShareClick(ayah)
                },
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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

    Toast.makeText(context, "Ayah copied to clipboard", Toast.LENGTH_SHORT).show()
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
