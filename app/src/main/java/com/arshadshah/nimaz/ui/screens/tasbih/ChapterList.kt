package com.arshadshah.nimaz.ui.screens.tasbih

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.local.models.LocalChapter
import com.arshadshah.nimaz.ui.components.common.BackButton
import com.arshadshah.nimaz.ui.components.common.NoResultFound
import com.arshadshah.nimaz.ui.components.common.PageErrorState
import com.arshadshah.nimaz.ui.components.common.PageLoading
import com.arshadshah.nimaz.viewModel.DuaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterList(
    viewModel: DuaViewModel = hiltViewModel(),
    navController: NavHostController,
    onNavigateToChapter: (Int, String) -> Unit,
    categoryId: String
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val listState = rememberLazyListState()
    val lastVisibleItemIndexState =
        remember { mutableIntStateOf(SharedPreferencesUtil.getLastVisibleItemIndex(context)) }

    LaunchedEffect(categoryId) {
        viewModel.getChapters(categoryId.toInt())
    }

    LaunchedEffect(remember { derivedStateOf { listState.firstVisibleItemIndex } }) {
        SharedPreferencesUtil.saveLastVisibleItemIndex(context, listState.firstVisibleItemIndex)
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
                    val category = viewModel.getCategoryById(categoryId.toInt())
                    Text(text = category?.name ?: "Chapters")
                },
                navigationIcon = {
                    BackButton {
                        navController.popBackStack()
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(paddingValues)
        ) {
            when (uiState) {
                is DuaViewModel.UiState.Loading -> PageLoading()
                is DuaViewModel.UiState.Error -> PageErrorState((uiState as DuaViewModel.UiState.Error).message)
                is DuaViewModel.UiState.Success<*> -> {
                    when {
                        chapters.isEmpty() -> NoResultFound(
                            title = "No Chapters Found",
                            subtitle = "No chapters found for this category",
                        )

                        else -> ChaptersContent(
                            chapters = chapters,
                            listState = listState,
                            onChapterClick = onNavigateToChapter
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChaptersContent(
    chapters: List<LocalChapter>,
    listState: LazyListState,
    onChapterClick: (Int, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            ChapterGroupCard(
                chapters = chapters,
                onChapterClick = onChapterClick
            )
        }
    }
}

@Composable
private fun ChapterGroupCard(
    chapters: List<LocalChapter>,
    onChapterClick: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Section
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chapters",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${chapters.size}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Chapters List
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                chapters.forEachIndexed { index, chapter ->
                    ChapterItem(
                        chapter = chapter,
                        index = index + 1,
                        onClick = { onChapterClick(chapter._id, chapter.english_title) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterItem(
    chapter: LocalChapter,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chapter Number Container
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = index.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Chapter Title
            Text(
                text = chapter.english_title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // Navigation Arrow
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.angle_small_right_icon),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
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
