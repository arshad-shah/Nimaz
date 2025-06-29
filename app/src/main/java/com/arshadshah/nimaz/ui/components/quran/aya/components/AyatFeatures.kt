package com.arshadshah.nimaz.ui.components.quran.aya.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.automirrored.outlined.Note
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.ui.components.quran.NoteDialog
import com.arshadshah.nimaz.viewModel.AudioState
import com.arshadshah.nimaz.viewModel.AyatViewModel

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
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Verse Number Badge
            VerseNumberBadge(number = aya.ayaNumberInSurah, isLoading = loading)

            // Feature Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                FeatureButton(
                    onClick = { shareAya(context, aya) },
                    icon = Icons.Outlined.Share,
                    description = "Share verse",
                    isLoading = loading
                )


                // Audio Controls

                if (!hasAudioFile) {
                    FeatureButton(
                        onClick = { onEvent(AyatViewModel.AyatEvent.DownloadAudio(aya)) },
                        icon = Icons.Default.Download,
                        description = "Download audio",
                        isLoading = loading || audioState.isDownloading
                    )
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
