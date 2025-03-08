package com.arshadshah.nimaz.ui.screens.tasbih

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.local.models.LocalTasbih
import com.arshadshah.nimaz.ui.components.common.CustomTabsWithPager
import com.arshadshah.nimaz.ui.components.common.DropDownHeader
import com.arshadshah.nimaz.ui.components.common.FeaturesDropDown
import com.arshadshah.nimaz.ui.components.common.NoResultFound
import com.arshadshah.nimaz.ui.components.tasbih.DeleteDialog
import com.arshadshah.nimaz.ui.components.tasbih.GoalEditDialog
import com.arshadshah.nimaz.ui.components.tasbih.TasbihDropdownItem
import com.arshadshah.nimaz.ui.components.tasbih.TasbihRow
import com.arshadshah.nimaz.viewModel.TasbihViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ListOfTasbih(
    navController: NavHostController,
    viewModel: TasbihViewModel = hiltViewModel(),
    onNavigateToTasbihScreen: (String, String, String, String) -> Unit,
) {
    val resources = LocalContext.current.resources
    val context = LocalContext.current

    val sharedPref = context.getSharedPreferences("tasbih", 0)
    val selected =
        remember { mutableStateOf(sharedPref.getBoolean("selected", false)) }
    val indexSelected =
        remember { mutableIntStateOf(sharedPref.getInt("indexSelected", -1)) }
    //if user leaves tis activity or the app, the selected item and indexSelected will be saved
    //buit if the count is 0, the selected item and indexSelected will be reset
    LaunchedEffect(
        key1 = selected.value,
        key2 = indexSelected.value,
    ) {
        sharedPref.edit().putBoolean("selected", selected.value).apply()
        sharedPref.edit().putInt("indexSelected", indexSelected.value).apply()
    }

    //if a new item is selected, then scroll to that item
    val listState = rememberLazyListState()
    LaunchedEffect(key1 = indexSelected.value) {
        if (indexSelected.value != -1) {
            listState.animateScrollToItem(indexSelected.value)
        }
    }

    //the state of the lazy column, it should scroll to the item where selected is true
    //get the arrays
    val englishNames = resources.getStringArray(R.array.tasbeehTransliteration)
    val arabicNames = resources.getStringArray(R.array.tasbeeharabic)
    val translationNames = resources.getStringArray(R.array.tasbeehTranslation)

    val tasbihCreated = viewModel.tasbihCreated.collectAsState()

    val titles = listOf("Tasbih List", "My Tasbih")
    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0F,
    ) {
        titles.size
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Tasbih")
                },
                navigationIcon = {
                    OutlinedIconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .testTag(AppConstants.TEST_TAG_QURAN)
        ) {

            CustomTabsWithPager(pagerState, titles)

            HorizontalPager(
                pageSize = PageSize.Fill,
                state = pagerState,
            ) { page ->
                when (page) {
                    0 -> {
                        Card(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),

                            ) {
                            LazyColumn(
                                modifier = Modifier.testTag(AppConstants.TEST_TAG_TASBIH_LIST),
                                state = listState,
                            ) {
                                items(englishNames.size) { index ->
                                    TasbihRow(
                                        arabicNames[index],
                                        englishNames[index],
                                        translationNames[index],
                                        onNavigateToTasbihScreen,
                                        tasbihCreated = tasbihCreated,
                                        onCreateTasbih = { arabicName: String, englishName: String, translationName: String, goal: String ->
                                            viewModel.createTasbih(
                                                LocalTasbih(
                                                    arabicName = arabicName,
                                                    englishName = englishName,
                                                    translationName = translationName,
                                                    goal = goal.toInt(),
                                                    count = 0,
                                                    date = LocalDate.now()
                                                )
                                            )
                                        }
                                    )
                                    if (index == englishNames.size - 1) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.background,
                                            thickness = 2.dp,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        viewModel.getAllTasbih()
                        val listOfTasbih = viewModel.tasbihList.collectAsState()
                        //if the list is empty, show a message
                        if (listOfTasbih.value.isEmpty()) {
                            NoResultFound(
                                title = "No Tasbih found",
                                subtitle = "You will see your Tasbih list here once you create one"
                            )
                        } else {
                            val showTasbihDialog = remember {
                                mutableStateOf(false)
                            }
                            val showDeleteDialog = remember {
                                mutableStateOf(false)
                            }
                            val tasbihToEdit = remember {
                                mutableStateOf(
                                    LocalTasbih(
                                        0,
                                        LocalDate.now(),
                                        "",
                                        "",
                                        "",
                                        0,
                                        0,
                                    )
                                )
                            }
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                userScrollEnabled = true,
                            ) {
                                //extract the dates from the list of tasbih
                                val dates = listOfTasbih.value.map { tasbih ->
                                    tasbih.date
                                }.distinct()

                                //find out what year the dates are in
                                val years = dates.map { date ->
                                    date
                                        .format(DateTimeFormatter.ofPattern("YYYY"))
                                }.distinct()

                                //find out for each year in what month the tasbih are in
                                val months = years.map { year ->
                                    dates.filter { date ->
                                        date
                                            .format(DateTimeFormatter.ofPattern("YYYY")) == year
                                    }.map { date ->
                                        date
                                            .format(DateTimeFormatter.ofPattern("MMMM"))
                                    }.distinct()
                                }

                                //if we have less than a months worth of tasbih, then we don't need to show the year header
                                if (dates.size < 20) {
                                    item {
                                        //for each date in the month, render the date header
                                        for (dateIndex in dates.indices) {
                                            FeaturesDropDown(
                                                //the list of tasbih for the date at the index
                                                items = listOfTasbih.value.filter { tasbih ->
                                                    tasbih.date == dates[dateIndex]
                                                },
                                                label = dates[dateIndex]
                                                    .format(
                                                        DateTimeFormatter.ofPattern(
                                                            "E dd MMMM"
                                                        )
                                                    ),
                                                dropDownItem = {
                                                    TasbihDropdownItem(
                                                        it,
                                                        onClick = { tasbih ->
                                                            onNavigateToTasbihScreen(
                                                                tasbih.id.toString(),
                                                                tasbih.arabicName,
                                                                tasbih.englishName,
                                                                tasbih.translationName
                                                            )
                                                        },
                                                        onDelete = { tasbih ->
                                                            showDeleteDialog.value = true
                                                            tasbihToEdit.value = tasbih
                                                        },
                                                    ) { tasbih ->
                                                        showTasbihDialog.value =
                                                            true
                                                        tasbihToEdit.value =
                                                            tasbih
                                                    }
                                                }

                                            )
                                        }
                                    }
                                } else {
                                    //for each year, render the year header
                                    for (index in years.indices) {
                                        item {
                                            FeaturesDropDown(
                                                //the list of tasbih for the date at the index
                                                items = months[index],
                                                label = years[index],
                                                dropDownItem = {
                                                    //for each month in the year, render the month header
                                                    for (monthIndex in months[index].indices) {
                                                        FeaturesDropDown(
                                                            //the list of tasbih for the date at the index
                                                            items = dates,
                                                            label = months[index][monthIndex],
                                                            dropDownItem = {
                                                                //for each date in the month, render the date header
                                                                for (dateIndex in dates.indices) {
                                                                    if (dates[dateIndex]
                                                                            .format(
                                                                                DateTimeFormatter.ofPattern(
                                                                                    "MMMM"
                                                                                )
                                                                            ) == months[index][monthIndex]
                                                                        &&
                                                                        dates[dateIndex]
                                                                            .format(
                                                                                DateTimeFormatter.ofPattern(
                                                                                    "YYYY"
                                                                                )
                                                                            ) == years[index]
                                                                    ) {
                                                                        FeaturesDropDown(
                                                                            //the list of tasbih for the date at the index
                                                                            items = listOfTasbih.value.filter { tasbih ->
                                                                                tasbih.date == dates[dateIndex]
                                                                            },
                                                                            label =
                                                                                dates[dateIndex]
                                                                                    .format(
                                                                                        DateTimeFormatter.ofPattern(
                                                                                            "E dd "
                                                                                        )
                                                                                    ),
                                                                            dropDownItem = {
                                                                                TasbihDropdownItem(
                                                                                    it,
                                                                                    onClick = { tasbih ->
                                                                                        onNavigateToTasbihScreen(
                                                                                            tasbih.id.toString(),
                                                                                            tasbih.arabicName,
                                                                                            tasbih.englishName,
                                                                                            tasbih.translationName
                                                                                        )
                                                                                    },
                                                                                    onDelete = { tasbih ->
                                                                                        showDeleteDialog.value =
                                                                                            true
                                                                                        tasbihToEdit.value =
                                                                                            tasbih
                                                                                    },
                                                                                ) { tasbih ->
                                                                                    showTasbihDialog.value =
                                                                                        true
                                                                                    tasbihToEdit.value =
                                                                                        tasbih
                                                                                }
                                                                            }

                                                                        )
                                                                    }
                                                                }
                                                            }

                                                        )
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            GoalEditDialog(
                                tasbih = tasbihToEdit.value,
                                showTasbihDialog,
                                onUpdateTasbih = { it: String ->
                                    val tasbih = tasbihToEdit.value
                                    //update the goal
                                    viewModel.updateTasbih(
                                        LocalTasbih(
                                            id = tasbih.id,
                                            arabicName = tasbih.arabicName,
                                            englishName = tasbih.englishName,
                                            translationName = tasbih.translationName,
                                            goal = it.toInt(),
                                            count = tasbih.count,
                                            date = LocalDate.now(),
                                        )
                                    )
                                })

                            DeleteDialog(
                                tasbih = tasbihToEdit.value,
                                showDeleteDialog,
                                onDeleteTasbih = { tasbih: LocalTasbih ->
                                    viewModel.deleteTasbih(tasbih)
                                })
                        }
                    }

                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
//MyTasbihDropDownItem
fun DefaultPreview() {
    DropDownHeader(headerLeft = "Name", headerMiddle = "Goal", headerRight = "Count")
}
