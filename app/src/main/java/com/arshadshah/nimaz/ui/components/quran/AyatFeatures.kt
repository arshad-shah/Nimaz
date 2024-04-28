package com.arshadshah.nimaz.ui.components.quran

import android.content.Intent
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
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
    val titleOfDialog = remember {
        mutableStateOf("")
    }
    val openDialog = remember {
        mutableStateOf(false)
    }
    val messageOfDialog = remember {
        mutableStateOf("")
    }
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Badge(
            modifier = Modifier
                .size(48.dp)
                .padding(4.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Text(
                text = aya.ayaNumberInSurah.toString(),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {

            if (aya.sajda) {
                SajdaButton(aya.sajdaType, isLoading)
            }
            TogglableAyatFeature(
                icon = if (isBookMarkedVerse.value) painterResource(id = R.drawable.bookmark_icon) else painterResource(
                    id = R.drawable.bookmark_icon_unselected
                ),
                iconDescription = if (isBookMarkedVerse.value) "Remove Bookmark" else "Add Bookmark",
                onClick = {
                    if (isBookMarkedVerse.value) {
                        titleOfDialog.value = "Remove from Bookmarks"
                        messageOfDialog.value =
                            "Are you sure you want to remove this verse from your bookmarks?"
                        openDialog.value = true
                    } else {
                        isBookMarkedVerse.value = !isBookMarkedVerse.value
                        aya.bookmark = isBookMarkedVerse.value
                        handleEvents(
                            QuranViewModel.AyaEvent.BookmarkAya(
                                aya.ayaNumberInSurah,
                                aya.suraNumber,
                                aya.ayaNumberInSurah,
                                isBookMarkedVerse.value
                            )
                        )
                    }
                },
                isLoading = isLoading
            )

            TogglableAyatFeature(
                icon = if (isFavouredVerse.value) painterResource(id = R.drawable.favorite_icon) else painterResource(
                    id = R.drawable.favorite_icon_unseletced
                ),
                iconDescription = if (isFavouredVerse.value) "Remove Favourite" else "Add Favourite",
                onClick = {
                    if (isFavouredVerse.value) {
                        titleOfDialog.value = "Remove from Favourites"
                        messageOfDialog.value =
                            "Are you sure you want to remove this verse from your favourites?"
                        openDialog.value = true
                    } else {
                        isFavouredVerse.value = !isFavouredVerse.value
                        aya.favorite = isFavouredVerse.value
                        handleEvents(
                            QuranViewModel.AyaEvent.FavoriteAya(
                                aya.ayaNumberInSurah,
                                aya.suraNumber,
                                aya.ayaNumberInSurah,
                                isFavouredVerse.value
                            )
                        )
                    }
                },
                isLoading = isLoading
            )
            IconButton(
                onClick = {
                    if (aya.note.isNotEmpty()) {
                        titleOfDialog.value = "Edit Note"
                        showNoteDialog.value = true
                    } else {
                        titleOfDialog.value = "Add Note"
                        showNoteDialog.value = true
                    }
                },
                enabled = !isLoading,
            ) {
                Icon(
                    painter = if (aya.note.isNotEmpty()) painterResource(id = R.drawable.note_icon) else painterResource(
                        id = R.drawable.note_unselected
                    ),
                    contentDescription = "Note",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(20.dp)
                        .placeholder(
                            visible = isLoading,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(4.dp),
                            highlight = PlaceholderHighlight.shimmer(
                                highlightColor = Color.White,
                            )
                        )
                )
            }
            IconButton(
                modifier = Modifier.size(52.dp),
                onClick = {
                    //share the aya
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.type = "text/plain"
                    //create the share message
                    //with the aya text, aya translation
                    //the sura number followed by the aya number
                    shareIntent.putExtra(
                        Intent.EXTRA_TEXT,
                        "Chapter ${aya.suraNumber}: Verse ${aya.ayaNumberInSurah}\n\n${aya.ayaArabic}\n${aya.translationEnglish}"
                    )

                    //start the share intent
                    context.startActivity(Intent.createChooser(shareIntent, "Share Aya"))
                },
                enabled = true,
            ) {
                Icon(
                    modifier = Modifier
                        .size(20.dp),
                    painter = painterResource(id = R.drawable.share_icon),
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = "Share aya",
                )
            }
            if (aya.audioFileLocation.isEmpty()) {
                IconButton(
                    modifier = Modifier.size(52.dp),
                    onClick = {
                        downloadFile()
                    },
                ) {
                    Icon(
                        modifier = Modifier
                            .size(24.dp),
                        painter = painterResource(id = R.drawable.download_icon),
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = "Download Audio",
                    )
                }
            }
            PlayerForAyat(
                isPlaying = isPlaying,
                isPaused = isPaused,
                isStopped = isStopped,
                isDownloaded = isDownloaded,
                hasAudio = hasAudio,
                onPlayClicked = { playFile() },
                onPauseClicked = { pauseFile() },
                onStopClicked = { stopFile() },
                isLoading = false,
            )
        }
    }


    //the note dialog that appears when the user clicks on the note icon
    if (showNoteDialog.value) {
        NoteInput(
            showNoteDialog = showNoteDialog,
            titleOfDialog = titleOfDialog,
            noteContent = noteContent,
            onClick = {
                //update the note in the aya object if the note is not empty
                hasNote.value = noteContent.value.isNotEmpty()

                aya.note = noteContent.value
                handleEvents(
                    QuranViewModel.AyaEvent.AddNoteToAya(
                        aya.ayaNumberInSurah,
                        aya.suraNumber,
                        aya.ayaNumberInSurah,
                        noteContent.value
                    )
                )
                showNoteDialog.value = false
            }
        )
    }

    if (openDialog.value) {
        AlertDialogNimaz(
            topDivider = false,
            bottomDivider = false,
            contentDescription = "Ayat features dialog",
            title = titleOfDialog.value,
            contentToShow = {
                Text(
                    text = messageOfDialog.value,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(8.dp)
                )
            },
            onDismissRequest = {
                openDialog.value = false
            },
            contentHeight = 100.dp,
            confirmButtonText = "Yes",
            dismissButtonText = "No, Cancel",
            onConfirm = {
                if (titleOfDialog.value == "Remove from Bookmarks") {
                    handleEvents(
                        QuranViewModel.AyaEvent.deleteBookmarkFromAya(
                            aya.ayaNumberInSurah,
                            aya.suraNumber,
                            aya.ayaNumberInSurah
                        )
                    )
                    aya.bookmark = false
                    isBookMarkedVerse.value = false
                } else if (titleOfDialog.value == "Remove from Favourites") {
                    handleEvents(
                        QuranViewModel.AyaEvent.deleteFavoriteFromAya(
                            aya.ayaNumberInSurah,
                            aya.suraNumber,
                            aya.ayaNumberInSurah
                        )
                    )
                    aya.favorite = false
                    isFavouredVerse.value = false
                }
                openDialog.value = false
            },
            onDismiss = {
                openDialog.value = false
            })
    }
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

@Composable
fun TogglableAyatFeature(
    icon: Painter,
    iconDescription: String,
    onClick: () -> Unit,
    isLoading: Boolean
) {

    IconButton(
        onClick = { onClick() },
        enabled = !isLoading,
    ) {
        Icon(
            painter = icon,
            contentDescription = iconDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(20.dp)
                .placeholder(
                    visible = isLoading,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(4.dp),
                    highlight = PlaceholderHighlight.shimmer(
                        highlightColor = Color.White,
                    )
                )
        )
    }
}