package com.arshadshah.nimaz.ui.components.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.models.Aya
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.ui.components.common.FeatureDropdownItem
import com.arshadshah.nimaz.ui.components.common.FeaturesDropDown
import com.arshadshah.nimaz.ui.components.common.Placeholder
import com.arshadshah.nimaz.ui.components.tasbih.SwipeBackground
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.viewModel.DashboardViewmodel
import kotlin.reflect.KFunction1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardQuranTracker(
    onNavigateToAyatScreen: (String, Boolean, String, Int) -> Unit,
    quranBookmarks: State<List<Aya>>,
    handleEvents: KFunction1<DashboardViewmodel.DashboardEvent, Unit>,
    isLoading: State<Boolean>
) {

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
        mutableStateOf<Aya?>(null)
    }
    if (quranBookmarks.value.isEmpty()) {
        Box(
            modifier = Modifier.clickable {
                onNavigateToAyatScreen(1.toString(), true, translation, 1)
            }
        ) {
            Placeholder(nameOfDropdown = "Quran Bookmarks")
        }
    } else {
        FeaturesDropDown(
            label = "Quran Bookmarks",
            items = quranBookmarks.value,
            dropDownItem = { Aya ->
                val currentItem = rememberUpdatedState(newValue = Aya)
                val dismissState = rememberDismissState(
                    confirmValueChange = {
                        if (it == DismissValue.DismissedToStart) {
                            titleOfDialog.value = "Delete Bookmark"
                            messageOfDialog.value =
                                "Are you sure you want to delete this bookmark?"
                            itemToDelete.value = currentItem.value
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
                            item = Aya,
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
                                    text = "Chapter " + aya.suraNumber.toString() + ":" + "Verse " + aya.ayaNumber.toString(),
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
                handleEvents(
                    DashboardViewmodel.DashboardEvent.DeleteBookmarkFromAya(
                        itemToDelete.value!!.ayaNumber,
                        itemToDelete.value!!.suraNumber,
                        itemToDelete.value!!.ayaNumberInSurah
                    )
                )
                openDialog.value = false
            },
            onDismiss = {
                openDialog.value = false
            })
    }
}
