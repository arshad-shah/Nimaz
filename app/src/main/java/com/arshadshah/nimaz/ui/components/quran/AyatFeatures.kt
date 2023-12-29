package com.arshadshah.nimaz.ui.components.quran

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer
import com.arshadshah.nimaz.utils.LocalDataStore
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.viewModel.QuranViewModel

@Composable
fun AyatFeatures(
    isBookMarkedVerse: MutableState<Boolean>,
    isFavouredVerse: MutableState<Boolean>,
    hasNote: MutableState<Boolean>,
    handleEvents: (QuranViewModel.AyaEvent) -> Unit,
    aya: LocalAya,
    showNoteDialog: MutableState<Boolean>,
    noteContent: MutableState<String>,
    isLoading: Boolean,
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

        if (hasNote.value) {
            IconButton(
                onClick = {
                    handleEvents(
                        QuranViewModel.AyaEvent.getNoteForAya(
                            aya.ayaNumberInSurah,
                            aya.suraNumber,
                            aya.ayaNumberInSurah
                        )
                    )
                    showNoteDialog.value = true
                    noteContent.value = aya.note
                },
                enabled = !isLoading,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.note_icon),
                    contentDescription = "Note",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(4.dp)
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
    }


    //the note dialog that appears when the user clicks on the note icon
    if (showNoteDialog.value) {
        NoteInput(
            showNoteDialog = showNoteDialog,
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
            ElevatedCard(
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.elevatedCardElevation(
                    defaultElevation = 8.dp,
                )
            ) {
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
                .size(24.dp)
                .padding(4.dp)
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

@Preview(
    device = "id:pixel_5", showSystemUi = false, showBackground = true,
    backgroundColor = 0xFFFFFFFF
)
@Composable
fun AyatFeaturesPreview() {
    val context = LocalContext.current
    val sharedPreferencesRepository = remember { PrivateSharedPreferences(context) }
    val viewModel = QuranViewModel(sharedPreferencesRepository)
    LocalDataStore.init(LocalContext.current)
    //create a dummy aya
    val aya = LocalAya(
        ayaNumberInQuran = 1,
        ayaArabic = "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ",
        translationEnglish = "In the name of Allah, the Entirely Merciful, the Especially Merciful.",
        translationUrdu = "اللہ کا نام سے، جو بہت مہربان ہے اور جو بہت مہربان ہے",
        audioFileLocation = "https://download.quranicaudio.com/quran/abdulbasitmurattal/001.mp3",
        ayaNumberInSurah = 1,
        bookmark = true,
        favorite = true,
        note = "dsfhsdhsgdfhstgh",
        juzNumber = 1,
        suraNumber = 1,
        ruku = 1,
        sajda = true,
        sajdaType = "Recommended",
    )

    AyatFeatures(
        isBookMarkedVerse = remember { mutableStateOf(aya.bookmark) },
        isFavouredVerse = remember { mutableStateOf(aya.favorite) },
        hasNote = remember { mutableStateOf(aya.note.isNotEmpty()) },
        handleEvents = viewModel::handleAyaEvent,
        aya = aya,
        showNoteDialog = remember { mutableStateOf(false) },
        noteContent = remember { mutableStateOf("") },
        isLoading = false,
    )
}