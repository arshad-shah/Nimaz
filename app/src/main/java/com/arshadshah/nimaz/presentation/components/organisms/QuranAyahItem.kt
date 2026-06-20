package com.arshadshah.nimaz.presentation.components.organisms

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.util.TajweedParser
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.SajdaType
import com.arshadshah.nimaz.presentation.components.atoms.QuranVerseText
import com.arshadshah.nimaz.presentation.components.atoms.toArabicNumber
import com.arshadshah.nimaz.presentation.theme.AmiriFontFamily
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme

internal const val BISMILLAH_TEXT = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"

/**
 * Strip bismillah from first ayah's Arabic text for all surahs EXCEPT:
 * - Surah 1 (Al-Fatiha) - bismillah IS ayah 1
 * - Surah 9 (At-Tawbah) - has no bismillah
 */
internal fun Ayah.getDisplayArabicText(): String {
    return if (numberInSurah == 1 && surahNumber != 1 && surahNumber != 9) {
        textArabic
            .removePrefix("$BISMILLAH_TEXT ")
            .removePrefix(BISMILLAH_TEXT)
            .trim()
    } else {
        textArabic
    }
}

/**
 * Process ayah text to append Arabic numeral with ornamental brackets at the end
 */
internal fun formatAyahWithEndMarker(arabicText: String, ayahNumber: Int): String {
    return "$arabicText ${formatAyahEndMarker(ayahNumber)}"
}

/**
 * Format just the ayah end marker with ornamental brackets
 */
internal fun formatAyahEndMarker(ayahNumber: Int): String {
    val unicodeAyaEndStart = "\uFD3F" // ﴿
    val unicodeAyaEndEnd = "\uFD3E"   // ﴾
    val arabicNumber = toArabicNumber(ayahNumber)
    return "$unicodeAyaEndStart$arabicNumber$unicodeAyaEndEnd"
}

@Composable
internal fun AyahItem(
    ayah: Ayah,
    showTranslation: Boolean,
    showTransliteration: Boolean = false,
    arabicFontSize: Float,
    arabicFontFamily: FontFamily = AmiriFontFamily,
    fontSize: Float,
    isHighlighted: Boolean = false,
    isAudioPlaying: Boolean = false,
    isFavorite: Boolean = false,
    isKhatamRead: Boolean = false,
    isKhatamMode: Boolean = false,
    showTajweed: Boolean = false,
    onBookmarkClick: () -> Unit,
    onFavoriteClick: () -> Unit = {},
    onPlayAyahClick: () -> Unit = {},
    onTafseerClick: () -> Unit = {},
    onKhatamToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()

    val bgColor by animateColorAsState(
        targetValue = if (isHighlighted)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else
            Color.Transparent,
        animationSpec = tween(300),
        label = "ayah_highlight"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 15.dp, vertical = 6.dp)
    ) {
        // Number badge + actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ayah.numberInSurah.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                if (isKhatamMode) {
                    IconButton(
                        onClick = onKhatamToggle,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isKhatamRead) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = if (isKhatamRead) "Mark as unread" else "Mark as read",
                            tint = if (isKhatamRead) Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = stringResource(R.string.cd_favorite),
                        tint = if (isFavorite) Color(0xFFEF4444)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onBookmarkClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (ayah.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = stringResource(R.string.cd_bookmark),
                        tint = if (ayah.isBookmarked) NimazColors.QuranColors.BookmarkPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = {
                        val textToShare =
                            "${ayah.textArabic}\n\n${ayah.translation ?: ""}\n\n- Surah ${ayah.surahNumber}, Ayah ${ayah.numberInSurah}"
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, textToShare)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Ayah"))
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = stringResource(R.string.cd_share),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onPlayAyahClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isAudioPlaying) "Pause" else "Play",
                        tint = if (isAudioPlaying || isHighlighted) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onTafseerClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = stringResource(R.string.cd_tafseer),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Arabic text with ayah end marker (with optional tajweed colors)
        val displayText = ayah.getDisplayArabicText()
        val textColor = MaterialTheme.colorScheme.onBackground

        if (showTajweed && ayah.textTajweed != null) {
            // Render with tajweed colors using BasicText
            val tajweedAnnotated = remember(ayah.textTajweed, isDarkTheme, ayah.numberInSurah) {
                val parsed = TajweedParser.parse(
                    tajweedText = ayah.textTajweed,
                    isDarkTheme = isDarkTheme,
                    defaultColor = textColor
                )
                // Append the end marker to the tajweed text
                buildAnnotatedString {
                    append(parsed)
                    append(" ")
                    append(formatAyahEndMarker(ayah.numberInSurah))
                }
            }
            BasicText(
                text = tajweedAnnotated,
                modifier = Modifier.fillMaxWidth(),
                style = TextStyle(
                    fontFamily = arabicFontFamily,
                    fontSize = arabicFontSize.sp,
                    lineHeight = (arabicFontSize * 2).sp,
                    textDirection = TextDirection.Rtl,
                    color = textColor
                )
            )
        } else {
            QuranVerseText(
                arabicText = displayText,
                verseNumber = ayah.numberInSurah,
                customFontSize = arabicFontSize.sp.value,
                fontFamily = arabicFontFamily
            )
        }

        // Translation
        if (showTranslation && ayah.translation != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Text(
                    text = ayah.translation,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.5f).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Transliteration
        if (showTransliteration && ayah.transliteration != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
            ) {
                Text(
                    text = ayah.transliteration,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.5f).sp
                    ),
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Indicators row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (ayah.sajdaType != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFDC2626).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (ayah.sajdaType == SajdaType.OBLIGATORY) "Sajdah (Wajib)" else "Sajdah",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFDC2626),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                if (ayah.rubNumber > 0 && ayah.numberInSurah == 1 || (ayah.rubNumber > 0)) {
                    val quarterLabel = when (ayah.rubNumber) {
                        1 -> "Hizb ${ayah.hizbNumber}"
                        2 -> "\u00BC Hizb ${ayah.hizbNumber}"
                        3 -> "\u00BD Hizb ${ayah.hizbNumber}"
                        4 -> "\u00BE Hizb ${ayah.hizbNumber}"
                        else -> ""
                    }
                    if (quarterLabel.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = quarterLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = "Juz ${ayah.juz} \u2022 Page ${ayah.page}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            thickness = 0.5.dp
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AyahItemPreview() {
    NimazTheme {
        AyahItem(
            ayah = Ayah(
                id = 1,
                surahNumber = 1,
                ayahNumber = 1,
                textArabic = "\u0628\u0650\u0633\u0652\u0645\u0650 \u0671\u0644\u0644\u0651\u064E\u0647\u0650 \u0671\u0644\u0631\u0651\u064E\u062D\u0652\u0645\u064E\u0670\u0646\u0650 \u0671\u0644\u0631\u0651\u064E\u062D\u0650\u064A\u0645\u0650",
                textSimple = "bismillah al-rahman al-raheem",
                juzNumber = 1,
                hizbNumber = 1,
                rubNumber = 0,
                pageNumber = 1,
                sajdaType = null,
                sajdaNumber = null,
                translation = "In the name of Allah, the Most Gracious, the Most Merciful.",
                isBookmarked = false
            ),
            showTranslation = true,
            arabicFontSize = 28f,
            fontSize = 16f,
            onBookmarkClick = {},
            onFavoriteClick = {},
            onPlayAyahClick = {},
            onTafseerClick = {}
        )
    }
}
