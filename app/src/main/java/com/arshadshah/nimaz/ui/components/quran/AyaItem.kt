package com.arshadshah.nimaz.ui.components.quran


import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.automirrored.outlined.Note
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer
import com.arshadshah.nimaz.ui.theme.almajeed
import com.arshadshah.nimaz.ui.theme.amiri
import com.arshadshah.nimaz.ui.theme.englishQuranTranslation
import com.arshadshah.nimaz.ui.theme.hidayat
import com.arshadshah.nimaz.ui.theme.quranFont
import com.arshadshah.nimaz.ui.theme.urduFont
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import com.arshadshah.nimaz.utils.DisplaySettings
import com.arshadshah.nimaz.viewModel.AudioState
import com.arshadshah.nimaz.viewModel.AyatViewModel

@Composable
fun AyaItem(
    aya: LocalAya,
    displaySettings: DisplaySettings,
    audioState: AudioState,
    onTafseerClick: (Int, Int) -> Unit,
    onEvent: (AyatViewModel.AyatEvent) -> Unit,
    loading: Boolean = false
) {
    // Only create states for values that need to trigger UI updates
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    if (aya.ayaNumberInSurah != 0) {
        ElevatedCard(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .scale(scale),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Features Section
                AyatFeatures(
                    aya = aya,
                    audioState = audioState,
                    onEvent = onEvent,
                    loading = loading
                )

                // Content Section
                AyatContent(
                    aya = aya,
                    displaySettings = displaySettings,
                    loading = loading
                )

                // Tafseer Section
                if (aya.ayaNumberInSurah != 0) {
                    TafseerSection(
                        aya = aya,
                        onOpenTafsir = onTafseerClick,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    } else {
        // Special Ayat
        SpecialAyat(
            aya = aya,
            displaySettings = displaySettings,
            loading = loading
        )
    }
}

@Composable
private fun SpecialAyat(
    aya: LocalAya,
    displaySettings: DisplaySettings,
    loading: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Content Section
            AyatContent(
                aya = aya,
                displaySettings = displaySettings,
                loading = loading
            )
        }
    }
}

@Composable
private fun AyatContent(
    aya: LocalAya,
    displaySettings: DisplaySettings,
    loading: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Arabic Text
            ArabicText(aya = aya, displaySettings = displaySettings, loading = loading)

            // Translation Text
            TranslationText(aya = aya, displaySettings = displaySettings, loading = loading)
        }
    }
}

@Composable
private fun ArabicText(
    aya: LocalAya,
    displaySettings: DisplaySettings,
    loading: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        SelectionContainer {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = aya.ayaArabic.cleanTextFromBackslash(),
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = displaySettings.arabicFontSize.sp,
                    fontFamily = getArabicFont(displaySettings.arabicFont),
                    textAlign = TextAlign.Justify,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .placeholder(
                            visible = loading,
                            highlight = PlaceholderHighlight.shimmer()
                        )
                )
            }
        }
    }
}

@Composable
private fun TranslationText(
    aya: LocalAya,
    displaySettings: DisplaySettings,
    loading: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            TranslationSection(
                urduText = aya.translationUrdu,
                englishText = aya.translationEnglish,
                displaySettings = displaySettings,
                loading = loading
            )
        }
    }
}


@Composable
fun TranslationContainer(
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        content()
    }
}

@Composable
fun UrduTranslation(
    text: String,
    fontSize: Int,
    loading: Boolean = false,
    modifier: Modifier = Modifier
) {
    TranslationContainer(content = {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
                text = "${text.cleanTextFromBackslash()} ۔",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = fontSize.sp,
                    fontFamily = urduFont,
                    textAlign = TextAlign.Justify,
                ),
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .placeholder(
                        visible = loading,
                        highlight = PlaceholderHighlight.shimmer(),
                    )
            )
        }
    })
}

@Composable
fun EnglishTranslation(
    text: String,
    fontSize: Int,
    loading: Boolean = false,
    modifier: Modifier = Modifier
) {
    TranslationContainer(content = {
        Text(
            text = text.cleanTextFromBackslash(),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = fontSize.sp,
                fontFamily = englishQuranTranslation,
                textAlign = TextAlign.Justify,
            ),
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
                .placeholder(
                    visible = loading,
                    highlight = PlaceholderHighlight.shimmer(),
                )
        )
    })
}

// Usage example:
@Composable
fun TranslationSection(
    urduText: String,
    englishText: String,
    displaySettings: DisplaySettings,
    loading: Boolean = false
) {
    when (displaySettings.translation) {
        "Urdu" -> UrduTranslation(
            text = urduText,
            fontSize = displaySettings.translationFontSize.toInt(),
            loading = loading
        )

        else -> EnglishTranslation(
            text = englishText,
            fontSize = displaySettings.translationFontSize.toInt(),
            loading = loading
        )
    }
}


// Helper Functions
private fun getArabicFont(fontName: String) = when (fontName) {
    "Default" -> utmaniQuranFont
    "Quranme" -> quranFont
    "Hidayat" -> hidayat
    "Amiri" -> amiri
    "IndoPak" -> almajeed
    else -> utmaniQuranFont
}


@Composable
private fun VerseNumberBadge(number: Int, isLoading: Boolean) {
    Badge(
        modifier = Modifier
            .size(48.dp)
            .padding(4.dp),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.placeholder(
                visible = isLoading,
                highlight = PlaceholderHighlight.shimmer()
            )
        )
    }
}

@Composable
private fun FeatureButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    isLoading: Boolean
) {
    IconButton(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier.size(36.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .size(20.dp)
                .placeholder(
                    visible = isLoading,
                    highlight = PlaceholderHighlight.shimmer()
                )
        )
    }
}

// AyatFeatures.kt optimizations
@Composable
fun AyatFeatures(
    aya: LocalAya,
    audioState: AudioState,
    onEvent: (AyatViewModel.AyatEvent) -> Unit,
    loading: Boolean
) {
    var showNoteDialog by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var confirmAction by remember { mutableStateOf<() -> Unit>({}) }
    var confirmMessage by remember { mutableStateOf("") }
    var confirmTitle by remember { mutableStateOf("") }

    val hasAudioFile = aya.audioFileLocation.isNotEmpty()
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Verse Number Badge
            VerseNumberBadge(number = aya.ayaNumberInSurah, isLoading = loading)

            // Feature Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sajda indicator if applicable
                if (aya.sajda) {
                    SajdaIndicator(
                        sajdaType = aya.sajdaType,
                        loading = loading
                    )
                }

                // Bookmark button
                FeatureButton(
                    onClick = {
                        if (aya.bookmark) {
                            confirmTitle = "Remove from Bookmarks"
                            confirmMessage =
                                "Are you sure you want to remove this verse from your bookmarks?"
                            confirmAction = { onEvent(AyatViewModel.AyatEvent.ToggleBookmark(aya)) }
                            showConfirmDialog = true
                        } else {
                            onEvent(AyatViewModel.AyatEvent.ToggleBookmark(aya))
                        }
                    },
                    icon = if (aya.bookmark) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    description = if (aya.bookmark) "Remove Bookmark" else "Add Bookmark",
                    isLoading = loading
                )

                // Favorite button
                FeatureButton(
                    onClick = {
                        if (aya.favorite) {
                            confirmTitle = "Remove from Favorites"
                            confirmMessage =
                                "Are you sure you want to remove this verse from your favorites?"
                            confirmAction = { onEvent(AyatViewModel.AyatEvent.ToggleFavorite(aya)) }
                            showConfirmDialog = true
                        } else {
                            onEvent(AyatViewModel.AyatEvent.ToggleFavorite(aya))
                        }
                    },
                    icon = if (aya.favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    description = if (aya.favorite) "Remove from Favorites" else "Add to Favorites",
                    isLoading = loading
                )

                // Note button
                FeatureButton(
                    onClick = { showNoteDialog = true },
                    icon = if (aya.note.isNotEmpty()) Icons.AutoMirrored.Filled.Note else Icons.AutoMirrored.Outlined.Note,
                    description = if (aya.note.isNotEmpty()) "Edit Note" else "Add Note",
                    isLoading = loading
                )

                // Share button
                IconButton(
                    onClick = { shareAya(context, aya) },
                    enabled = !loading
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share verse",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Audio Controls
                if (!hasAudioFile) {
                    IconButton(
                        onClick = { onEvent(AyatViewModel.AyatEvent.DownloadAudio(aya)) },
                        enabled = !loading && !audioState.isDownloading
                    ) {
                        if (audioState.isDownloading) {
                            CircularProgressIndicator(
                                progress = {
                                    audioState.downloadProgress
                                },
                                modifier = Modifier.size(24.dp),
                                trackColor = ProgressIndicatorDefaults.circularIndeterminateTrackColor,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download audio",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                } else {
                    AudioControls(
                        aya = aya,
                        audioState = audioState,
                        onEvent = onEvent,
                        loading = loading
                    )
                }
            }
        }
    }

    if (showNoteDialog) {
        NoteDialog(
            aya = aya,
            initialNote = aya.note,
            onDismiss = { showNoteDialog = false },
            onEvent = onEvent
        )
    }

    if (showConfirmDialog) {
        AlertDialogNimaz(
            title = confirmTitle,
            contentDescription = confirmMessage,
            contentHeight = 100.dp,
            contentToShow = {
                Column {
                    Text(confirmMessage)
                }
            },
            onDismissRequest = { showConfirmDialog = false },
            confirmButtonText = "Confirm",
            onConfirm = {
                confirmAction()
                showConfirmDialog = false
            },
            onDismiss = { showConfirmDialog = false }
        )
    }
}

@Composable
private fun SajdaIndicator(
    sajdaType: String,
    loading: Boolean
) {
    var showSajdaInfo by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { showSajdaInfo = true },
            enabled = !loading
        ) {
            Icon(
                painter = painterResource(id = R.drawable.sajad_icon),
                contentDescription = "Sajda indicator",
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }

        if (showSajdaInfo) {
            Popup(
                onDismissRequest = { showSajdaInfo = false },
                alignment = Alignment.BottomCenter,
                offset = IntOffset(0, -100)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp
                ) {
                    Text(
                        text = "$sajdaType sujood",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

private fun shareAya(context: Context, aya: LocalAya) {
    val shareText = buildString {
        append("Chapter ${aya.suraNumber}: Verse ${aya.ayaNumberInSurah}\n\n")
        append("${aya.ayaArabic}\n\n")
        append(aya.translationEnglish)
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "Share Verse"))
}


fun String.cleanTextFromBackslash(): String {
    return this
        .replace("\\\"", "\"")  // Handle escaped quotes first
        .replace("\\\\", "\\")  // Then handle double backslashes
        .replace("\\n", "\n")   // Handle newlines
        .replace("\\t", "\t")   // Handle tabs
        .replace("\\", "")      // Finally remove any remaining single backslashes
}