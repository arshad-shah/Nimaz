package com.arshadshah.nimaz.ui.screens.tasbih

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.constants.AppConstants.CHAPTER_SCREEN_ROUTE
import com.arshadshah.nimaz.ui.components.common.BackButton
import com.arshadshah.nimaz.viewModel.DuaViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Categories(
    viewModel: DuaViewModel = hiltViewModel(),
    navController: NavHostController,
    onNavigateToChapterListScreen: (String, Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val categories by viewModel.categories.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.getCategories()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Hisnul Muslim",
                            style = MaterialTheme.typography.titleLarge
                        )
                        AnimatedVisibility(
                            visible = categories.isNotEmpty(),
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Text(
                                text = "${categories.size} Categories Available",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    BackButton {
                        navController.popBackStack()
                    }
                },
                actions = {
                    // Refresh action
                    FilledTonalIconButton(
                        onClick = {
                            viewModel.refreshData()
                            scope.launch {
                                snackbarHostState.showSnackbar("Refreshing categories...")
                            }
                        },
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        DuaTabs(
            viewModel = viewModel,
            navController = navController,
            paddingValues = paddingValues,
            onDuaClick = { dua ->
                scope.launch {
                    // Handle suspending functions in a coroutine
                    val chapter = viewModel.getChapterById(dua.chapter_id)
                    val category = viewModel.getCategoryById(chapter?.category_id ?: 0)

                    // Navigate after getting the data
                    navController.navigate(
                        CHAPTER_SCREEN_ROUTE
                            .replace(
                                "{chapterId}",
                                dua.chapter_id.toString()
                            )
                            .replace(
                                "{categoryName}",
                                category?.name ?: "Uncategorized"
                            )
                    )
                }
            },
            onNavigateToChapterListScreen = onNavigateToChapterListScreen
        )
    }
}