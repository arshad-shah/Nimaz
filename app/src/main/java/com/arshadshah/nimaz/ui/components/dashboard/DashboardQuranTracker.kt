package com.arshadshah.nimaz.ui.components.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.data.local.models.LocalSurah
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.ui.components.common.DropdownListItem
import com.arshadshah.nimaz.ui.components.common.EmptyStateCard
import com.arshadshah.nimaz.ui.components.common.FeaturesDropDown
import com.arshadshah.nimaz.ui.components.quran.SurahCard
import com.arshadshah.nimaz.ui.components.tasbih.SwipeBackground
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.viewModel.DashboardViewModel
import kotlin.reflect.KFunction1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardQuranTracker(
    suraList: List<LocalSurah>,
    onNavigateToAyatScreen: (String, Boolean, String, Int) -> Unit,
    quranBookmarks: State<List<LocalAya>>,
    handleEvents: KFunction1<DashboardViewModel.DashboardEvent, Unit>,
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
        mutableStateOf<LocalAya?>(null)
    }
    if (quranBookmarks.value.isEmpty()) {
        Box(
            modifier = Modifier.clickable {
                onNavigateToAyatScreen(1.toString(), true, translation, 1)
            }
        ) {
            EmptyStateCard(text = "No Bookmarks Found")
        }
    } else {
        FeaturesDropDown(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
            label = "Quran Bookmarks",
            items = quranBookmarks.value,
            dropDownItem = { LocalAya ->
                val currentItem = rememberUpdatedState(newValue = LocalAya)
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = {
                        if (it == SwipeToDismissBoxValue.EndToStart) {
                            titleOfDialog.value = "Delete Bookmark"
                            messageOfDialog.value =
                                "Are you sure you want to delete this bookmark?"
                            itemToDelete.value = currentItem.value
                            openDialog.value = true
                        }
                        false
                    }
                )

                SwipeToDismissBox(
                    enableDismissFromStartToEnd = false,
                    enableDismissFromEndToStart = true,
                    state = dismissState,
                    backgroundContent = {
                        SwipeBackground(dismissState = dismissState)
                    },
                    content = {
                        DropdownListItem(
                            item = LocalAya,
                            onClick = { aya ->
                                onNavigateToAyatScreen(
                                    aya.suraNumber.toString(),
                                    true,
                                    translation,
                                    aya.ayaNumberInSurah
                                )
                            },
                            content = { aya ->
                                val filteredSurah =
                                    suraList.filter { it.number == aya.suraNumber }.distinct()[0]
                                SurahCard(
                                    surah = filteredSurah,
                                    { suraNumber: String, isSurah: Boolean, translation: String, ayaNumber: Int? ->
                                        onNavigateToAyatScreen(
                                            suraNumber,
                                            isSurah,
                                            translation,
                                            ayaNumber ?: 0
                                        )
                                    },
                                    loading = isLoading.value
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
                    DashboardViewModel.DashboardEvent.DeleteBookmarkFromAya(
                        itemToDelete.value!!.ayaNumberInSurah,
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
