package com.arshadshah.nimaz.ui.screens.tasbih

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_CHAPTERS
import com.arshadshah.nimaz.ui.components.tasbih.ChapterListItem
import com.arshadshah.nimaz.ui.screens.tasbih.SharedPreferencesUtil.getLastVisibleItemIndex
import com.arshadshah.nimaz.ui.screens.tasbih.SharedPreferencesUtil.saveLastVisibleItemIndex
import com.arshadshah.nimaz.viewModel.DuaViewModel

@Composable
fun ChapterList(
    viewModel: DuaViewModel = viewModel(
        key = AppConstants.DUA_CHAPTERS_VIEWMODEL_KEY
    ),
    paddingValues: PaddingValues,
    onNavigateToChapter: (Int, String) -> Unit,
    categoryId: String
) {
    val context = LocalContext.current
    val chapterState = viewModel.chapters.collectAsState()

    val listState = rememberLazyListState()
    val lastVisibleItemIndexState = remember {
        mutableIntStateOf(getLastVisibleItemIndex(context))
    }

    LaunchedEffect(categoryId) {
        viewModel.getChapters(categoryId.toInt())
    }

    LaunchedEffect(remember { derivedStateOf { listState.firstVisibleItemIndex } }) {
        saveLastVisibleItemIndex(context, listState.firstVisibleItemIndex)
    }

    LaunchedEffect(lastVisibleItemIndexState) {
        if (lastVisibleItemIndexState.intValue != -1) {
            listState.scrollToItem(lastVisibleItemIndexState.intValue)
            lastVisibleItemIndexState.intValue = -1
        }
    }

    LazyColumn(
        modifier = Modifier.testTag(TEST_TAG_CHAPTERS),
        contentPadding = paddingValues,
        state = listState
    ) {
        items(chapterState.value.size) {
            ChapterListItem(
                chapter = chapterState.value[it],
                onNavigateToChapter = onNavigateToChapter,
                loading = false
            )
        }
    }
}

object SharedPreferencesUtil {
    fun saveLastVisibleItemIndex(context: Context, index: Int) {
        val sharedPref = context.getSharedPreferences("dua", Context.MODE_PRIVATE)
        sharedPref.edit().putInt("visibleItemIndex", index).apply()
    }

    fun getLastVisibleItemIndex(context: Context): Int {
        val sharedPref = context.getSharedPreferences("dua", Context.MODE_PRIVATE)
        return sharedPref.getInt("visibleItemIndex", -1)
    }
}
