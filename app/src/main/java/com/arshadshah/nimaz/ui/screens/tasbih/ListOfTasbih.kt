package com.arshadshah.nimaz.ui.screens.tasbih

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.arshadshah.nimaz.ui.components.common.BackButton
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
    val selected = remember { mutableStateOf(sharedPref.getBoolean("selected", false)) }
    val indexSelected = remember { mutableIntStateOf(sharedPref.getInt("indexSelected", -1)) }

    LaunchedEffect(key1 = selected.value, key2 = indexSelected.intValue) {
        sharedPref.edit().putBoolean("selected", selected.value).apply()
        sharedPref.edit().putInt("indexSelected", indexSelected.intValue).apply()
    }

    val listState = rememberLazyListState()
    LaunchedEffect(key1 = indexSelected.intValue) {
        if (indexSelected.intValue != -1) {
            listState.animateScrollToItem(indexSelected.intValue)
        }
    }

    val englishNames = resources.getStringArray(R.array.tasbeehTransliteration)
    val arabicNames = resources.getStringArray(R.array.tasbeeharabic)
    val translationNames = resources.getStringArray(R.array.tasbeehTranslation)

    val tasbihCreated = viewModel.tasbihCreated.collectAsState()

    val titles = listOf("Tasbih List", "My Tasbih")
    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0F,
    ) { titles.size }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Tasbih") },
                navigationIcon = {
                    BackButton {
                        navController.popBackStack()
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .testTag(AppConstants.TEST_TAG_QURAN)
        ) {
            CustomTabsWithPager(pagerState, titles)

            HorizontalPager(
                pageSize = PageSize.Fill,
                state = pagerState,
            ) { page ->
                when (page) {
                    0 -> TasbihListTab(
                        englishNames = englishNames,
                        arabicNames = arabicNames,
                        translationNames = translationNames,
                        listState = listState,
                        tasbihCreated = tasbihCreated,
                        onNavigateToTasbihScreen = onNavigateToTasbihScreen,
                        onCreateTasbih = { arabicName, englishName, translationName, goal ->
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

                    1 -> MyTasbihTab(
                        viewModel = viewModel,
                        onNavigateToTasbihScreen = onNavigateToTasbihScreen
                    )
                }
            }
        }
    }
}

@Composable
private fun TasbihListTab(
    englishNames: Array<String>,
    arabicNames: Array<String>,
    translationNames: Array<String>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    tasbihCreated: androidx.compose.runtime.State<LocalTasbih>,
    onNavigateToTasbihScreen: (String, String, String, String) -> Unit,
    onCreateTasbih: (String, String, String, String) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .testTag(AppConstants.TEST_TAG_TASBIH_LIST),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(englishNames.size) { index ->
                TasbihRow(
                    arabicNames[index],
                    englishNames[index],
                    translationNames[index],
                    onNavigateToTasbihScreen,
                    tasbihCreated = tasbihCreated,
                    onCreateTasbih = { arabicName, englishName, translationName, goal ->
                        onCreateTasbih(arabicName, englishName, translationName, goal)
                    }
                )
            }
        }
    }
}

@Composable
private fun MyTasbihTab(
    viewModel: TasbihViewModel,
    onNavigateToTasbihScreen: (String, String, String, String) -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.getAllTasbih()
    }

    val listOfTasbih = viewModel.tasbihList.collectAsState()

    if (listOfTasbih.value.isEmpty()) {
        NoResultFound(
            title = "No Tasbih found",
            subtitle = "You will see your Tasbih list here once you create one"
        )
    } else {
        val showTasbihDialog = remember { mutableStateOf(false) }
        val showDeleteDialog = remember { mutableStateOf(false) }
        val tasbihToEdit = remember {
            mutableStateOf(
                LocalTasbih(
                    id = 0,
                    date = LocalDate.now(),
                    arabicName = "",
                    englishName = "",
                    translationName = "",
                    goal = 0,
                    count = 0
                )
            )
        }

        ElevatedCard(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val dates = listOfTasbih.value.map { it.date }.distinct().sortedDescending()
                val years = dates.map { it.format(DateTimeFormatter.ofPattern("yyyy")) }.distinct()
                val months = years.map { year ->
                    dates.filter { it.format(DateTimeFormatter.ofPattern("yyyy")) == year }
                        .map { it.format(DateTimeFormatter.ofPattern("MMMM")) }
                        .distinct()
                }

                if (dates.size < 20) {
                    // Simple date-based grouping for small lists
                    items(dates.size) { dateIndex ->
                        DateGroupSection(
                            date = dates[dateIndex],
                            tasbihList = listOfTasbih.value.filter { it.date == dates[dateIndex] },
                            onNavigateToTasbihScreen = onNavigateToTasbihScreen,
                            onDelete = { tasbih ->
                                showDeleteDialog.value = true
                                tasbihToEdit.value = tasbih
                            },
                            onEdit = { tasbih ->
                                showTasbihDialog.value = true
                                tasbihToEdit.value = tasbih
                            }
                        )
                    }
                } else {
                    // Year/Month/Date hierarchy for large lists
                    items(years.size) { yearIndex ->
                        YearGroupSection(
                            year = years[yearIndex],
                            months = months[yearIndex],
                            dates = dates,
                            tasbihList = listOfTasbih.value,
                            onNavigateToTasbihScreen = onNavigateToTasbihScreen,
                            onDelete = { tasbih ->
                                showDeleteDialog.value = true
                                tasbihToEdit.value = tasbih
                            },
                            onEdit = { tasbih ->
                                showTasbihDialog.value = true
                                tasbihToEdit.value = tasbih
                            }
                        )
                    }
                }
            }
        }

        GoalEditDialog(
            tasbih = tasbihToEdit.value,
            showTasbihDialog,
            onUpdateTasbih = { newGoal ->
                viewModel.updateTasbih(
                    tasbihToEdit.value.copy(
                        goal = newGoal.toInt(),
                        date = LocalDate.now()
                    )
                )
            }
        )

        DeleteDialog(
            tasbih = tasbihToEdit.value,
            showDeleteDialog,
            onDeleteTasbih = { tasbih -> viewModel.deleteTasbih(tasbih) }
        )
    }
}

@Composable
private fun DateGroupSection(
    date: LocalDate,
    tasbihList: List<LocalTasbih>,
    onNavigateToTasbihScreen: (String, String, String, String) -> Unit,
    onDelete: (LocalTasbih) -> Unit,
    onEdit: (LocalTasbih) -> Unit
) {
    FeaturesDropDown(
        items = tasbihList,
        label = date.format(DateTimeFormatter.ofPattern("E dd MMMM")),
        dropDownItem = { tasbih ->
            TasbihDropdownItem(
                tasbih,
                onClick = { onNavigateToTasbihScreen(it.id.toString(), it.arabicName, it.englishName, it.translationName) },
                onDelete = onDelete,
                onEdit = onEdit
            )
        }
    )
}

@Composable
private fun YearGroupSection(
    year: String,
    months: List<String>,
    dates: List<LocalDate>,
    tasbihList: List<LocalTasbih>,
    onNavigateToTasbihScreen: (String, String, String, String) -> Unit,
    onDelete: (LocalTasbih) -> Unit,
    onEdit: (LocalTasbih) -> Unit
) {
    FeaturesDropDown(
        items = months,
        label = year,
        dropDownItem = { month ->
            MonthGroupSection(
                month = month,
                year = year,
                dates = dates,
                tasbihList = tasbihList,
                onNavigateToTasbihScreen = onNavigateToTasbihScreen,
                onDelete = onDelete,
                onEdit = onEdit
            )
        }
    )
}

@Composable
private fun MonthGroupSection(
    month: String,
    year: String,
    dates: List<LocalDate>,
    tasbihList: List<LocalTasbih>,
    onNavigateToTasbihScreen: (String, String, String, String) -> Unit,
    onDelete: (LocalTasbih) -> Unit,
    onEdit: (LocalTasbih) -> Unit
) {
    val filteredDates = dates.filter {
        it.format(DateTimeFormatter.ofPattern("MMMM")) == month &&
        it.format(DateTimeFormatter.ofPattern("yyyy")) == year
    }

    FeaturesDropDown(
        items = filteredDates,
        label = month,
        dropDownItem = { date ->
            DateGroupSection(
                date = date,
                tasbihList = tasbihList.filter { it.date == date },
                onNavigateToTasbihScreen = onNavigateToTasbihScreen,
                onDelete = onDelete,
                onEdit = onEdit
            )
        }
    )
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
fun DefaultPreview() {
    DropDownHeader(headerLeft = "Name", headerMiddle = "Goal", headerRight = "Count")
}
