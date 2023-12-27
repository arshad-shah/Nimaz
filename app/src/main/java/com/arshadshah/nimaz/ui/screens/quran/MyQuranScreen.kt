package com.arshadshah.nimaz.ui.screens.quran

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDismissState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.ui.components.common.FeatureDropdownItem
import com.arshadshah.nimaz.ui.components.common.FeaturesDropDown
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer
import com.arshadshah.nimaz.ui.components.tasbih.SwipeBackground
import com.arshadshah.nimaz.ui.theme.almajeed
import com.arshadshah.nimaz.ui.theme.amiri
import com.arshadshah.nimaz.ui.theme.hidayat
import com.arshadshah.nimaz.ui.theme.quranFont
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.viewModel.QuranViewModel
import kotlin.reflect.KFunction1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyQuranScreen(
    bookmarks: State<List<LocalAya>>,
    favorites: State<List<LocalAya>>,
    notes: State<List<LocalAya>>,
    onNavigateToAyatScreen: (String, Boolean, String, Int?) -> Unit,
    handleEvents: KFunction1<QuranViewModel.AyaEvent, Unit>,
) {
    //execute the code below when the screen is loaded
    LaunchedEffect(Unit)
    {
        handleEvents(QuranViewModel.AyaEvent.getBookmarks)
        handleEvents(QuranViewModel.AyaEvent.getFavorites)
        handleEvents(QuranViewModel.AyaEvent.getNotes)
    }

    val translationType =
        PrivateSharedPreferences(LocalContext.current).getData(
            key = AppConstants.TRANSLATION_LANGUAGE,
            s = "English"
        )
    val translation = when (translationType) {
        "English" -> "english"
        "Urdu" -> "urdu"
        else -> "english"
    }

    val titleOfDialog = remember {
        mutableStateOf("")
    }
    val openDialog = remember {
        mutableStateOf(false)
    }
    val messageOfDialog = remember {
        mutableStateOf("")
    }
    val itemToDelete = remember {
        mutableStateOf<LocalAya?>(null)
    }

    val listOfMapOfDropdowns = listOf(
        mapOf(
            "label" to "Bookmarks",
            "messageTitle" to "Delete Bookmark",
            "message" to "Are you sure you want to delete this bookmark?",
            "items" to bookmarks.value
        ),
        mapOf(
            "label" to "Favorites",
            "messageTitle" to "Delete Favorite",
            "message" to "Are you sure you want to delete this favorite?",
            "items" to favorites.value
        ),
        mapOf(
            "label" to "Notes",
            "messageTitle" to "Delete Note",
            "message" to "Are you sure you want to delete this note?",
            "items" to notes.value
        )
    )

    val frequentlyReadSurahs = mapOf(
        "Al-Fatiha" to Triple("1", 1, "الْفَاتِحَة"),
        "Al-Baqarah" to Triple("2", 1, "الْبَقَرَة"),
        "Al-'Imran" to Triple("3", 1, "آلِ عِمْرَان"),
        "An-Nisa'" to Triple("4", 1, "النِّسَاء"),
        "Al-Ma'idah" to Triple("5", 1, "الْمَائِدَة"),
        "Al-Kahf" to Triple("18", 1, "الْكَهْف"),
        "Yaseen" to Triple("36", 1, "يس"),
        "Ar-Rahman" to Triple("55", 1, "الرَّحْمَٰن"),
        "Al-Mulk" to Triple("67", 1, "الْمُلْك"),
        "Al-Kawthar" to Triple("108", 1, "الْكَوْثَر")
        // Add more Surahs as per your requirement
    )


    LazyColumn(
        modifier = Modifier
            .testTag("MyQuranScreen")
            .fillMaxSize(),
        userScrollEnabled = true,
    ) {
        items(listOfMapOfDropdowns.size) {
            FeaturesDropDown(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
                ),
                label = listOfMapOfDropdowns[it]["label"] as String,
                items = listOfMapOfDropdowns[it]["items"] as List<LocalAya>,
                dropDownItem = { bookmark ->
                    val currentItem = rememberUpdatedState(newValue = bookmark)
                    val dismissState = rememberDismissState(
                        confirmValueChange = { newDismissValue ->
                            if (newDismissValue == DismissValue.DismissedToStart) {
                                itemToDelete.value = currentItem.value
                                titleOfDialog.value =
                                    listOfMapOfDropdowns[it]["messageTitle"] as String
                                messageOfDialog.value =
                                    listOfMapOfDropdowns[it]["message"] as String
                                openDialog.value = true
                            }
                            false
                        }
                    )

                    SwipeToDismiss(
                        directions = setOf(DismissDirection.EndToStart),
                        state = dismissState,
                        background = {
                            SwipeBackground(dismissState = dismissState)
                        },
                        dismissContent = {
                            FeatureDropdownItem(
                                item = bookmark,
                                onClick = { aya ->
                                    onNavigateToAyatScreen(
                                        aya.suraNumber.toString(),
                                        true,
                                        translation,
                                        aya.ayaNumberInSurah
                                    )
                                },
                                itemContent = { aya ->
                                    //the text
                                    Text(
                                        modifier = Modifier
                                            .padding(8.dp),
                                        text = "Chapter " + aya.suraNumber.toString() + ":" + " Verse " + aya.ayaNumberInSurah.toString(),
                                        textAlign = TextAlign.Start,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            )
                        })
                }
            )
        }
        item{
            FeaturesDropDown(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
                ),
                label = "Frequently Read Surahs",
                items = frequentlyReadSurahs.toList(),
                dropDownItem = { surahs ->
                    Log.d("Frequently Read Surahs", "Clicked: $surahs")
                    FeatureDropdownItem(
                        item = surahs.second,
                        onClick = { aya ->
                            onNavigateToAyatScreen(
                                aya.first,
                                true,
                                translation,
                                aya.second
                            )
                        },
                        itemContent = { aya ->

                                //the text
                                Text(
                                    modifier = Modifier
                                        .padding(8.dp),
                                    text = "${aya.first}.  ${surahs.first}",
                                    textAlign = TextAlign.Start,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                    Text(
                                        text = aya.third,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontSize = 24.sp,
                                        fontFamily = quranFont,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(4.dp)
                                    )
                                }
                        }
                    )
                }
            )
        }
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
                when (titleOfDialog.value) {
                    "Delete Bookmark" -> {
                        handleEvents(
                            QuranViewModel.AyaEvent.deleteBookmarkFromAya(
                                itemToDelete.value!!.ayaNumberInSurah,
                                itemToDelete.value!!.suraNumber,
                                itemToDelete.value!!.ayaNumberInSurah
                            )
                        )
                    }

                    "Delete Favorite" -> {
                        handleEvents(
                            QuranViewModel.AyaEvent.deleteFavoriteFromAya(
                                itemToDelete.value!!.ayaNumberInSurah,
                                itemToDelete.value!!.suraNumber,
                                itemToDelete.value!!.ayaNumberInSurah
                            )
                        )
                    }

                    "Delete Note" -> {
                        handleEvents(
                            QuranViewModel.AyaEvent.deleteNoteFromAya(
                                itemToDelete.value!!.ayaNumberInSurah,
                                itemToDelete.value!!.suraNumber,
                                itemToDelete.value!!.ayaNumberInSurah
                            )
                        )
                    }
                }
                openDialog.value = false
            },
            onDismiss = {
                openDialog.value = false
            })
    }
}