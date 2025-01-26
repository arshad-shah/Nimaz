package com.arshadshah.nimaz.ui.components.quran

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer
import com.arshadshah.nimaz.viewModel.QuranViewModel

@Composable
fun AyatFeatures(
    isBookMarkedVerse: MutableState<Boolean>,
    isFavouredVerse: MutableState<Boolean>,
    hasNote: MutableState<Boolean>,
    handleEvents: (QuranViewModel.AyaEvent) -> Unit,
    downloadFile: () -> Unit,
    aya: LocalAya,
    showNoteDialog: MutableState<Boolean>,
    noteContent: MutableState<String>,
    isLoading: Boolean,
    isPlaying: MutableState<Boolean>,
    isPaused: MutableState<Boolean>,
    isStopped: MutableState<Boolean>,
    playFile: () -> Unit,
    pauseFile: () -> Unit,
    stopFile: () -> Unit,
    isDownloaded: MutableState<Boolean>,
    hasAudio: MutableState<Boolean>,
) {
    val dialogState = remember { DialogState() }
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Verse Number Badge
            VerseNumberBadge(number = aya.ayaNumberInSurah, isLoading = isLoading)

            // Features Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Sajda indicator if applicable
                if (aya.sajda) {
                    SajdaButton(aya.sajdaType, isLoading)
                }

                // Main feature buttons
                AyatActionButtons(
                    isBookMarked = isBookMarkedVerse.value,
                    isFavoured = isFavouredVerse.value,
                    hasNote = aya.note.isNotEmpty(),
                    isLoading = isLoading,
                    onBookmarkClick = {
                        if (isBookMarkedVerse.value) {
                            dialogState.showDeleteBookmarkDialog { confirmed ->
                                if (confirmed) {
                                    handleBookmarkDeletion(aya, handleEvents)
                                    isBookMarkedVerse.value = false
                                }
                            }
                        } else {
                            handleBookmarkAddition(aya, handleEvents)
                            isBookMarkedVerse.value = true
                        }
                    },
                    onFavoriteClick = {
                        if (isFavouredVerse.value) {
                            dialogState.showDeleteFavoriteDialog { confirmed ->
                                if (confirmed) {
                                    handleFavoriteDeletion(aya, handleEvents)
                                    isFavouredVerse.value = false
                                }
                            }
                        } else {
                            handleFavoriteAddition(aya, handleEvents)
                            isFavouredVerse.value = true
                        }
                    },
                    onNoteClick = {
                        dialogState.titleOfDialog =
                            if (aya.note.isNotEmpty()) "Edit Note" else "Add Note"
                        showNoteDialog.value = true
                    },
                    onShareClick = {
                        shareAya(context, aya)
                    }
                )

                // Audio controls
                AudioControls(
                    hasAudioFile = aya.audioFileLocation.isNotEmpty(),
                    isLoading = isLoading,
                    isPlaying = isPlaying,
                    isPaused = isPaused,
                    isStopped = isStopped,
                    isDownloaded = isDownloaded,
                    hasAudio = hasAudio,
                    onDownload = downloadFile,
                    onPlay = playFile,
                    onPause = pauseFile,
                    onStop = stopFile
                )
            }
        }
    }

    // Dialogs
    if (showNoteDialog.value) {
        NoteInput(
            showNoteDialog = showNoteDialog,
            titleOfDialog = dialogState.titleOfDialog,
            noteContent = noteContent,
            onClick = {
                handleNoteUpdate(aya, noteContent.value, handleEvents)
                hasNote.value = noteContent.value.isNotEmpty()
                showNoteDialog.value = false
            }
        )
    }

    dialogState.ShowConfirmationDialog(onConfirm = dialogState.onConfirm)
}

@Composable
private fun VerseNumberBadge(number: Int, isLoading: Boolean) {
    Badge(
        modifier = Modifier
            .size(40.dp)
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
private fun AyatActionButtons(
    isBookMarked: Boolean,
    isFavoured: Boolean,
    hasNote: Boolean,
    isLoading: Boolean,
    onBookmarkClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onNoteClick: () -> Unit,
    onShareClick: () -> Unit
) {
    FeatureButton(
        isSelected = isBookMarked,
        selectedIcon = R.drawable.bookmark_icon,
        unselectedIcon = R.drawable.bookmark_icon_unselected,
        description = if (isBookMarked) "Remove Bookmark" else "Add Bookmark",
        onClick = onBookmarkClick,
        isLoading = isLoading
    )

    FeatureButton(
        isSelected = isFavoured,
        selectedIcon = R.drawable.favorite_icon,
        unselectedIcon = R.drawable.favorite_icon_unseletced,
        description = if (isFavoured) "Remove Favourite" else "Add Favourite",
        onClick = onFavoriteClick,
        isLoading = isLoading
    )

    FeatureButton(
        isSelected = hasNote,
        selectedIcon = R.drawable.note_icon,
        unselectedIcon = R.drawable.note_unselected,
        description = if (hasNote) "Edit Note" else "Add Note",
        onClick = onNoteClick,
        isLoading = isLoading
    )

    IconButton(
        onClick = onShareClick,
        enabled = !isLoading,
        modifier = Modifier.size(48.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.share_icon),
            contentDescription = "Share aya",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(20.dp)
                .placeholder(
                    visible = isLoading,
                    highlight = PlaceholderHighlight.shimmer()
                )
        )
    }
}

@Composable
private fun AudioControls(
    hasAudioFile: Boolean,
    isLoading: Boolean,
    isPlaying: MutableState<Boolean>,
    isPaused: MutableState<Boolean>,
    isStopped: MutableState<Boolean>,
    isDownloaded: MutableState<Boolean>,
    hasAudio: MutableState<Boolean>,
    onDownload: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit
) {
    if (!hasAudioFile) {
        IconButton(
            onClick = onDownload,
            modifier = Modifier.size(48.dp),
            enabled = !isLoading
        ) {
            Icon(
                painter = painterResource(R.drawable.download_icon),
                contentDescription = "Download Audio",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(24.dp)
                    .placeholder(
                        visible = isLoading,
                        highlight = PlaceholderHighlight.shimmer()
                    )
            )
        }
    }

    PlayerForAyat(
        isPlaying = isPlaying,
        isPaused = isPaused,
        isStopped = isStopped,
        isDownloaded = isDownloaded,
        hasAudio = hasAudio,
        onPlayClicked = onPlay,
        onPauseClicked = onPause,
        onStopClicked = onStop,
        isLoading = isLoading
    )
}

@Composable
private fun FeatureButton(
    isSelected: Boolean,
    @DrawableRes selectedIcon: Int,
    @DrawableRes unselectedIcon: Int,
    description: String,
    onClick: () -> Unit,
    isLoading: Boolean
) {
    IconButton(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier.size(48.dp)
    ) {
        Icon(
            painter = painterResource(if (isSelected) selectedIcon else unselectedIcon),
            contentDescription = description,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(20.dp)
                .placeholder(
                    visible = isLoading,
                    highlight = PlaceholderHighlight.shimmer()
                )
        )
    }
}

private class DialogState {
    var openDialog by mutableStateOf(false)
    var titleOfDialog by mutableStateOf("")
    var messageOfDialog by mutableStateOf("")
    var onConfirm: () -> Unit by mutableStateOf({})

    fun showDeleteBookmarkDialog(onConfirmed: (Boolean) -> Unit) {
        titleOfDialog = "Remove from Bookmarks"
        messageOfDialog = "Are you sure you want to remove this verse from your bookmarks?"
        onConfirm = { onConfirmed(true) }
        openDialog = true
    }

    fun showDeleteFavoriteDialog(onConfirmed: (Boolean) -> Unit) {
        titleOfDialog = "Remove from Favourites"
        messageOfDialog = "Are you sure you want to remove this verse from your favourites?"
        onConfirm = { onConfirmed(true) }
        openDialog = true
    }

    @Composable
    fun ShowConfirmationDialog(onConfirm: () -> Unit) {
        if (openDialog) {
            AlertDialogNimaz(
                topDivider = false,
                bottomDivider = false,
                contentDescription = "Ayat features dialog",
                title = titleOfDialog,
                contentToShow = {
                    Text(
                        text = messageOfDialog,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(8.dp)
                    )
                },
                onDismissRequest = { openDialog = false },
                contentHeight = 100.dp,
                confirmButtonText = "Yes",
                dismissButtonText = "No, Cancel",
                onConfirm = {
                    onConfirm()
                    openDialog = false
                },
                onDismiss = { openDialog = false }
            )
        }
    }
}

private fun handleBookmarkAddition(aya: LocalAya, handleEvents: (QuranViewModel.AyaEvent) -> Unit) {
    aya.bookmark = true
    handleEvents(
        QuranViewModel.AyaEvent.BookmarkAya(
            aya.ayaNumberInSurah,
            aya.suraNumber,
            aya.ayaNumberInSurah,
            true
        )
    )
}

private fun handleBookmarkDeletion(aya: LocalAya, handleEvents: (QuranViewModel.AyaEvent) -> Unit) {
    aya.bookmark = false
    handleEvents(
        QuranViewModel.AyaEvent.deleteBookmarkFromAya(
            aya.ayaNumberInSurah,
            aya.suraNumber,
            aya.ayaNumberInSurah
        )
    )
}

private fun handleFavoriteAddition(aya: LocalAya, handleEvents: (QuranViewModel.AyaEvent) -> Unit) {
    aya.favorite = true
    handleEvents(
        QuranViewModel.AyaEvent.FavoriteAya(
            aya.ayaNumberInSurah,
            aya.suraNumber,
            aya.ayaNumberInSurah,
            true
        )
    )
}

private fun handleFavoriteDeletion(aya: LocalAya, handleEvents: (QuranViewModel.AyaEvent) -> Unit) {
    aya.favorite = false
    handleEvents(
        QuranViewModel.AyaEvent.deleteFavoriteFromAya(
            aya.ayaNumberInSurah,
            aya.suraNumber,
            aya.ayaNumberInSurah
        )
    )
}

private fun handleNoteUpdate(
    aya: LocalAya,
    noteContent: String,
    handleEvents: (QuranViewModel.AyaEvent) -> Unit
) {
    aya.note = noteContent
    handleEvents(
        QuranViewModel.AyaEvent.AddNoteToAya(
            aya.ayaNumberInSurah,
            aya.suraNumber,
            aya.ayaNumberInSurah,
            noteContent
        )
    )
}

private fun shareAya(context: Context, aya: LocalAya) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(
            Intent.EXTRA_TEXT,
            buildString {
                append("Chapter ${aya.suraNumber}: Verse ${aya.ayaNumberInSurah}\n\n")
                append("${aya.ayaArabic}\n")
                append(aya.translationEnglish)
            }
        )
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Aya"))
}

@Composable
fun SajdaButton(sajdaType: String, isLoading: Boolean) {
    val sajdahPopUpOpen = remember {
        mutableStateOf(false)
    }
    //a button that opens a popup menu
    IconButton(
        onClick = {
            sajdahPopUpOpen.value = !sajdahPopUpOpen.value
        },
        enabled = !isLoading,
    ) {
        Icon(
            modifier = Modifier
                .size(48.dp)
                .placeholder(
                    visible = isLoading,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(4.dp),
                    highlight = PlaceholderHighlight.shimmer(
                        highlightColor = Color.White,
                    )
                ),
            painter = painterResource(id = R.drawable.sajad_icon),
            contentDescription = "Sajda at this Ayat",
            tint = MaterialTheme.colorScheme.tertiary
        )
    }
    if (sajdahPopUpOpen.value) {
        Popup(
            onDismissRequest = {
                sajdahPopUpOpen.value = false
            },
            alignment = Alignment.BottomCenter,
            offset = IntOffset(0, -100),
        ) {
            ElevatedCard {
                Text(
                    text = "$sajdaType sujood",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

