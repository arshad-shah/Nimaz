package com.arshadshah.nimaz.ui.screens.tasbih

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_CHAPTERS
import com.arshadshah.nimaz.data.local.models.LocalChapter
import com.arshadshah.nimaz.ui.screens.tasbih.SharedPreferencesUtil.getLastVisibleItemIndex
import com.arshadshah.nimaz.ui.screens.tasbih.SharedPreferencesUtil.saveLastVisibleItemIndex
import com.arshadshah.nimaz.viewModel.DuaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterList(
    viewModel: DuaViewModel = viewModel(key = AppConstants.DUA_CHAPTERS_VIEWMODEL_KEY),
    navController: NavHostController,
    onNavigateToChapter: (Int, String) -> Unit,
    categoryId: String
) {
    val context = LocalContext.current
    val chapterState = viewModel.chapters.collectAsState()
    val listState = rememberLazyListState()
    val lastVisibleItemIndexState = remember { mutableIntStateOf(getLastVisibleItemIndex(context)) }

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
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Chapters")
                },
                navigationIcon = {
                    OutlinedIconButton(
                        modifier = Modifier
                            .testTag("backButton")
                            .padding(start = 8.dp),
                        onClick = {
                            navController.popBackStack()
                        }) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(id = R.drawable.back_icon),
                            contentDescription = "Back"
                        )
                    }
                },
            )
        },
    ) {
        Card(
            modifier = Modifier
                .padding(it)
                .padding(16.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp,
                pressedElevation = 4.dp
            )
        ) {
            LazyColumn(
                modifier = Modifier.testTag(TEST_TAG_CHAPTERS),
                state = listState,
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(chapterState.value.size) { index ->
                    ChapterListItem(
                        chapter = chapterState.value[index],
                        onNavigateToChapter = onNavigateToChapter,
                        loading = false,
                        isLastItem = index == chapterState.value.size - 1
                    )

                    if (chapterState.value[index] != chapterState.value.last()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChapterListItem(
    chapter: LocalChapter,
    onNavigateToChapter: (Int, String) -> Unit,
    loading: Boolean,
    isLastItem: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        onClick = { onNavigateToChapter(chapter._id, chapter.english_title) },
        enabled = !loading,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
                        )
                    )
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            ) {
                Text(
                    text = chapter.english_title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "Navigate to ${chapter.english_title}",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
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
