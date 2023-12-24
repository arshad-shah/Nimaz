package com.arshadshah.nimaz.ui.components.quran

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarMenu(number: Int, isSurah: Boolean, getAllAyats: (Int, String) -> Unit) {

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

    //create a list with numbers from 1 to 114
    //its expensive to create a list every time the composable is recomposed
    //so we use remember to create the list only once
    val surahList = remember { mutableListOf<Int>() }
    val juzList = remember { mutableListOf<Int>() }
    val (selectedSurah, setSelectedSurah) = remember { mutableIntStateOf(number) }

    //create the list using a coroutine
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            if (isSurah) {
                for (i in 1..114) {
                    surahList.add(i)
                }
            } else {
                for (i in 1..30) {
                    juzList.add(i)
                }
            }
        }
    }

    val label = when (isSurah) {
        true -> "Surah"
        false -> "Juz"
    }

    val list = when (isSurah) {
        true -> surahList
        false -> juzList
    }

    val expanded = remember { mutableStateOf(false) }
    ElevatedCard(
        modifier = Modifier
            .width(160.dp)
            .height(48.dp)
            .padding(start = 8.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable {
                expanded.value = !expanded.value
            },
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            //the text
            Text(
                modifier = Modifier
                    .padding(8.dp),
                text = label,
                textAlign = TextAlign.Start,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge
            )
            Badge(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
            {
                Text(
                    text = selectedSurah.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
            }
            Crossfade(
                targetState = expanded.value,
                animationSpec = tween(durationMillis = 300), label = ""
            ) { expanded ->
                if (expanded) {
                    Icon(
                        painter = painterResource(id = R.drawable.arrow_up_icon),
                        contentDescription = "dropdown icon",
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(24.dp)
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.arrow_down_icon),
                        contentDescription = "dropdown icon",
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(24.dp)
                    )
                }
            }
        }
    }

    if (expanded.value) {

        Popup(
            alignment = Alignment.BottomEnd,
            onDismissRequest = { expanded.value = false },
        ) {
            DropdownMenuQuranSection(
                getAllAyats = getAllAyats,
                selectedSurah = selectedSurah,
                setSelectedSurah = setSelectedSurah,
                expanded = expanded,
                list = list,
                translation = translation,
                label = label
            )
        }
    }
}

//drop down menu
@Composable
fun DropdownMenuQuranSection(
    getAllAyats: (Int, String) -> Unit,
    expanded: MutableState<Boolean>,
    translation: String,
    label: String,
    list: List<Int>,
    setSelectedSurah: (Int) -> Unit,
    selectedSurah: Int,
) {

    val scope = rememberCoroutineScope()
    val onSelected = { surah: Int ->
        scope.launch {
            setSelectedSurah(surah)
            getAllAyats(surah, translation)
            expanded.value = false
        }
    }

    val listState = rememberLazyListState(selectedSurah, -500)

    ElevatedCard(
        modifier = Modifier
            .width(150.dp)
            .height(300.dp)
            .shadow(12.dp, MaterialTheme.shapes.medium)
            .clip(MaterialTheme.shapes.medium),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(32.dp),
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        LazyColumn(
            state = listState,
        ) {
            items(list.size) { index ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = if (index == 0 || index == list.size - 1) 3.dp else 1.dp,
                            horizontal = 3.dp
                        )
                        .clip(MaterialTheme.shapes.medium)
                        .clickable {
                            onSelected(list[index])
                        }
                        .size(48.dp)
                        .border(
                            width = 2.dp,
                            color = if (list[index] == selectedSurah) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceColorAtElevation(
                                64.dp
                            ),
                            shape = MaterialTheme.shapes.medium
                        )
                        .background(
                            color = if (list[index] == selectedSurah) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$label ${list[index]}",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(horizontal = 18.dp)
                            .fillMaxWidth(),
                        fontWeight = if (list[index] == selectedSurah) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (list[index] == selectedSurah) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DropdownMenuSurahPreview() {
    DropdownMenuQuranSection(
        getAllAyats = { _, _ -> },
        expanded = remember { mutableStateOf(false) },
        translation = "english",
        label = "Surah",
        list = (1..114).toList(),
        setSelectedSurah = { },
        selectedSurah = 1
    )
}

@Preview(showBackground = true)
@Composable
fun DropdownMenuJuzPreview() {
    DropdownMenuQuranSection(
        getAllAyats = { _, _ -> },
        expanded = remember { mutableStateOf(false) },
        translation = "english",
        label = "Juz",
        list = (1..30).toList(),
        setSelectedSurah = { },
        selectedSurah = 1
    )
}

@Preview(showBackground = true)
@Composable
fun DropdownMenuTogglerPreview() {
    TopBarMenu(number = 114, isSurah = true, getAllAyats = { _, _ -> })
}