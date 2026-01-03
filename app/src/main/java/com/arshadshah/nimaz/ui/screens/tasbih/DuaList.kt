package com.arshadshah.nimaz.ui.screens.tasbih

import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.local.models.LocalDua
import com.arshadshah.nimaz.ui.components.common.BackButton
import com.arshadshah.nimaz.ui.components.common.NoResultFound
import com.arshadshah.nimaz.ui.components.common.PageErrorState
import com.arshadshah.nimaz.ui.components.common.PageLoading
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import com.arshadshah.nimaz.viewModel.DuaViewModel
import kotlinx.coroutines.coroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuaList(
    chapterId: String,
    navController: NavHostController,
    viewModel: DuaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val duas by viewModel.duas.collectAsState()
    val listState = rememberLazyListState()

    val chapterName = remember { mutableStateOf("Supplications") }

    LaunchedEffect(Unit) {
        viewModel.getDuas(chapterId.toInt())
    }

    LaunchedEffect(chapterId) {
        val chapter = coroutineScope { viewModel.getChapterById(chapterId.toInt()) }
        chapterName.value = chapter?.english_title ?: "Supplications"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = chapterName.value)
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
                        duas.isEmpty() -> NoResultFound(
                            title = "No Duas Found",
                            subtitle = "This chapter appears to be empty"
                        )

                        else -> DuasContent(
                            duas = duas,
                            listState = listState,
                            onFavoriteClick = { dua -> viewModel.toggleFavorite(dua) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DuasContent(
    duas: List<LocalDua>,
    listState: LazyListState,
    onFavoriteClick: (LocalDua) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            DuaGroupCard(
                duas = duas,
                onFavoriteClick = onFavoriteClick
            )
        }
    }
}

@Composable
private fun DuaGroupCard(
    duas: List<LocalDua>,
    onFavoriteClick: (LocalDua) -> Unit,
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
                        text = "Duas",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${duas.size}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Duas List
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                duas.forEach { dua ->
                    DuaItem(
                        dua = dua,
                        onFavoriteClick = { onFavoriteClick(dua) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DuaItem(
    dua: LocalDua,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Favorite Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (dua.favourite == 1)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.size(40.dp)
                ) {
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            painter = painterResource(
                                id = if (dua.favourite == 1)
                                    R.drawable.favorite_icon
                                else R.drawable.favorite_icon_unseletced
                            ),
                            contentDescription = if (dua.favourite == 1)
                                "Remove from favorites"
                            else "Add to favorites",
                            tint = if (dua.favourite == 1)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Arabic Text
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = dua.arabic_dua.cleanText(),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = utmaniQuranFont,
                            fontSize = 28.sp,
                            lineHeight = 46.sp,
                            textAlign = TextAlign.Start
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        softWrap = true,
                        overflow = TextOverflow.Visible
                    )
                }
            }

            // Translation
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = dua.english_translation.cleanText(),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontStyle = FontStyle.Italic,
                        lineHeight = 28.sp,
                        letterSpacing = 0.3.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    softWrap = true,
                    overflow = TextOverflow.Visible
                )
            }

            // Reference
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            append("Reference: ")
                        }
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f)
                            )
                        ) {
                            append(dua.english_reference.formatReference())
                        }
                    },
                    style = MaterialTheme.typography.labelLarge.copy(
                        lineHeight = 20.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}


fun String.cleanText(): String {
    return this
        .replace(Regex("<[^>]*>"), "")  // Remove HTML tags
        .replace("\\r\\n", "\n")        // Convert literal "\r\n" to actual line break
        .replace("\\n", "\n")           // Convert literal "\n" to actual line break
        .replace("\\r", "\n")           // Convert literal "\r" to line break
        .trim()
}

/**
 * Formats a reference string by cleaning up quotes, backslashes and whitespace.
 */
private fun String.formatReference(): String = this
    .replace("\"", "")         // Remove quotes
    .replace("\\", "")         // Remove backslashes
    .replace(Regex("\\s+"), " ") // Normalize whitespace to single spaces
    .trim()                    // Remove leading/trailing whitespace