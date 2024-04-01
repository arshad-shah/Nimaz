package com.arshadshah.nimaz.ui.screens.tasbih

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.tasbih.DuaListItem
import com.arshadshah.nimaz.viewModel.DuaViewModel

@Composable
fun DuaList(chapterId: String, paddingValues: PaddingValues) {
    val context = LocalContext.current
    val viewModel = viewModel(
        key = AppConstants.DUA_CHAPTERS_VIEWMODEL_KEY,
        initializer = { DuaViewModel() },
        viewModelStoreOwner = LocalContext.current as ComponentActivity
    )

    LaunchedEffect(Unit) {
        viewModel.getDuas(chapterId.toInt())
    }

    val duaState = remember {
        viewModel.duas
    }.collectAsState()

    //if a new item is viewed, then scroll to that item
    val sharedPref = context.getSharedPreferences("dua", 0)
    val listState = rememberLazyListState()
    val visibleItemIndex =
        remember { mutableIntStateOf(sharedPref.getInt("visibleItemIndexDua-${chapterId}", -1)) }

    //when we close the app, we want to save the index of the last item viewed so that we can scroll to it when we open the app again
    LaunchedEffect(remember { derivedStateOf { listState.firstVisibleItemIndex } })
    {
        sharedPref.edit()
            .putInt("visibleItemIndexDua-${chapterId}", listState.firstVisibleItemIndex).apply()
    }

    //when we reopen the app, we want to scroll to the last item viewed
    LaunchedEffect(visibleItemIndex.value)
    {
        if (visibleItemIndex.value != -1) {
            listState.scrollToItem(visibleItemIndex.value)
            //set the value back to -1 so that we don't scroll to the same item again
            visibleItemIndex.value = -1
        }
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(paddingValues)
            .padding(8.dp)
    ) {
        LazyColumn(
            modifier = Modifier.testTag(AppConstants.TEST_TAG_CHAPTER),
            state = listState,
            content = {
                items(duaState.value.size)
                {
                    DuaListItem(
                        dua = duaState.value[it],
                        loading = false
                    )
                    if (it != duaState.value.size - 1) {
                        //add a divider
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.background,
                            thickness = 2.dp
                        )
                    }
                }
            })
    }
}