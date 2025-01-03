package com.arshadshah.nimaz.ui.screens.quran

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.data.local.models.LocalSurah
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.ui.components.common.DropdownListItem
import com.arshadshah.nimaz.ui.components.common.FeaturesDropDown
import com.arshadshah.nimaz.ui.components.quran.SurahCard
import com.arshadshah.nimaz.ui.components.tasbih.SwipeBackground
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.viewModel.QuranViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyQuranScreen(
    bookmarks: State<List<LocalAya>>,
    suraList: State<ArrayList<LocalSurah>>,
    favorites: State<List<LocalAya>>,
    notes: State<List<LocalAya>>,
    onNavigateToAyatScreen: (String, Boolean, String, Int?) -> Unit,
    handleEvents: (QuranViewModel.AyaEvent) -> Unit,
    isLoading: State<Boolean>,
) {
    val context = LocalContext.current
    val translation = remember {
        when (PrivateSharedPreferences(context)
            .getData(AppConstants.TRANSLATION_LANGUAGE, "English")) {
            "Urdu" -> "urdu"
            else -> "english"
        }
    }

    var dialogState by remember { mutableStateOf<DialogState?>(null) }

    LaunchedEffect(Unit) {
        handleEvents(QuranViewModel.AyaEvent.getBookmarks)
        handleEvents(QuranViewModel.AyaEvent.getFavorites)
        handleEvents(QuranViewModel.AyaEvent.getNotes)
    }

    val sections = listOf(
        SectionData("Bookmarks", bookmarks.value, DeleteType.BOOKMARK),
        SectionData("Favorites", favorites.value, DeleteType.FAVORITE),
        SectionData("Notes", notes.value, DeleteType.NOTE)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        sections.forEach { section ->
            FeaturesDropDown(
                modifier = Modifier.padding(4.dp),
                label = section.title,
                items = section.items,
                dropDownItem = { aya ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart) {
                                dialogState = DialogState(
                                    title = "Delete ${section.title}",
                                    message = "Are you sure you want to delete this ${
                                        section.title.dropLast(
                                            1
                                        )
                                    }?",
                                    item = aya,
                                    type = section.type
                                )
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
                                item = aya,
                                onClick = {
                                },
                                content = {
                                    val surah = suraList.value.find { it.number == aya.suraNumber }
                                    surah?.let {
                                        SurahCard(
                                            surah = it,
                                            { _, _, _, _ ->
                                                onNavigateToAyatScreen(
                                                    it.number.toString(),
                                                    true,
                                                    translation,
                                                    aya.ayaNumberInSurah
                                                )
                                            },
                                            loading = isLoading.value
                                        )
                                    }
                                }
                            )
                        })
                }
            )
        }

        FeaturesDropDown(
            modifier = Modifier.padding(4.dp),
            label = "Frequently Read Surahs",
            items = getFrequentlyReadSurahs().toList(),
            showBadge = false,
            dropDownItem = { (name, details) ->
                val surah = suraList.value.find { it.number == details.first.toInt() }
                surah?.let {
                    SurahCard(
                        surah = it,
                        { _, _, _, _ ->
                            onNavigateToAyatScreen(
                                details.first,
                                true,
                                translation,
                                details.second
                            )
                        },
                        loading = isLoading.value
                    )
                }
            }
        )
    }

    dialogState?.let { state ->
        AlertDialogNimaz(
            title = state.title,
            contentToShow = {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(8.dp)
                )
            },
            onDismissRequest = { dialogState = null },
            contentHeight = 100.dp,
            confirmButtonText = "Yes",
            dismissButtonText = "No",
            onConfirm = {
                handleEvents(getDeleteEvent(state))
                dialogState = null
            },
            onDismiss = { dialogState = null },
            contentDescription = "Delete ${state.type.name}"
        )
    }
}

private data class SectionData(
    val title: String,
    val items: List<LocalAya>,
    val type: DeleteType
)

private enum class DeleteType {
    BOOKMARK, FAVORITE, NOTE
}

private data class DialogState(
    val title: String,
    val message: String,
    val item: LocalAya,
    val type: DeleteType
)

private fun getDeleteEvent(state: DialogState): QuranViewModel.AyaEvent {
    val aya = state.item
    return when (state.type) {
        DeleteType.BOOKMARK -> QuranViewModel.AyaEvent.deleteBookmarkFromAya(
            aya.ayaNumberInSurah, aya.suraNumber, aya.ayaNumberInSurah
        )

        DeleteType.FAVORITE -> QuranViewModel.AyaEvent.deleteFavoriteFromAya(
            aya.ayaNumberInSurah, aya.suraNumber, aya.ayaNumberInSurah
        )

        DeleteType.NOTE -> QuranViewModel.AyaEvent.deleteNoteFromAya(
            aya.ayaNumberInSurah, aya.suraNumber, aya.ayaNumberInSurah
        )
    }
}

private fun getFrequentlyReadSurahs() = mapOf(
    "Al-Fatiha" to Triple("1", 1, "الْفَاتِحَة"),
    "Al-Baqarah" to Triple("2", 1, "الْبَقَرَة"),
    "Yaseen" to Triple("36", 1, "يس"),
    "Ar-Rahman" to Triple("55", 1, "الرَّحْمَٰن"),
    "Al-Mulk" to Triple("67", 1, "الْمُلْك"),
    "Al-Kawthar" to Triple("108", 1, "الْكَوْثَر")
)