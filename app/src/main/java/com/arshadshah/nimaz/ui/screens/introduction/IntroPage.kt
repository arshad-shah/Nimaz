package com.arshadshah.nimaz.ui.screens.introduction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.ui.navigation.BottomNavItem
import com.arshadshah.nimaz.utils.FirebaseLogger
import com.arshadshah.nimaz.viewModel.IntroductionViewModel
import kotlinx.coroutines.launch

@Composable
fun IntroPage(
    navController: NavHostController,
    viewModel: IntroductionViewModel = hiltViewModel()
) {
    val firebaseLogger = viewModel.firebaseLogger
    // Collect state from ViewModel
    val uiState by viewModel.uiState.collectAsState()
    val legalState by viewModel.legalSettingsState.collectAsState()

    // Check if legal terms are accepted (for final page navigation)
    val termsAccepted = legalState.termsAccepted && legalState.privacyPolicyAccepted

    // Initialize pager state
    val pagerState = rememberPagerState(0, 0f) { OnBoardingPages.size }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Get current category from pager state
    val currentCategory = remember(pagerState.currentPage) {
        OnBoardingPages[pagerState.currentPage]?.category ?: ""
    }

    // Check if the current page is the last one
    val isLastPage = pagerState.currentPage == OnBoardingPages.size - 1

    // Check if the current page is a Legal page
    val isLegalPage = OnBoardingPages[pagerState.currentPage]?.category == "Legal"

    // Log screen view when the composable is first displayed
    DisposableEffect(Unit) {
        firebaseLogger.logScreenView("intro_onboarding", "IntroPage")
        onDispose {}
    }

    // Sync page changes with ViewModel
    LaunchedEffect(pagerState.currentPage) {
        viewModel.handleEvent(IntroductionViewModel.IntroEvent.NavigateToPage(pagerState.currentPage))

        // Log page view
        val pageName = OnBoardingPages[pagerState.currentPage]?.title ?: "Unknown"
        firebaseLogger.logEvent(
            "onboarding_page_viewed",
            mapOf(
                "page_index" to pagerState.currentPage,
                "page_name" to pageName,
                "page_category" to currentCategory
            ),
            FirebaseLogger.Companion.EventCategory.SCREEN_VIEW
        )
    }

    // Show legal error snackbar if needed
    LaunchedEffect(uiState.showLegalError) {
        if (uiState.showLegalError) {
            snackbarHostState.showSnackbar(
                "Please accept the Terms of Service and Privacy Policy to continue."
            )
            viewModel.handleEvent(IntroductionViewModel.IntroEvent.ClearLegalError)
        }
    }

    // Error handling
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            // Show error
            snackbarHostState.showSnackbar(uiState.error ?: "An error occurred")
            viewModel.handleEvent(IntroductionViewModel.IntroEvent.ClearError)

            // Log error encountered
            firebaseLogger.logError(
                "intro_page_error",
                uiState.error ?: "Unknown error",
                mapOf(
                    "current_page" to pagerState.currentPage,
                    "page_name" to (OnBoardingPages[pagerState.currentPage]?.title ?: "Unknown")
                )
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Progress Header
            ProgressHeader(
                currentCategory = currentCategory,
                currentPage = pagerState.currentPage,
                totalPages = OnBoardingPages.size
            )

            // Content Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                pageSpacing = 16.dp,
                userScrollEnabled = false // Disable manual swiping
            ) { page ->
                OnBoardingPages[page]?.let { pageContent ->
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(300)
                        ) + fadeIn(animationSpec = tween(300)),
                        exit = slideOutHorizontally(
                            targetOffsetX = { -it },
                            animationSpec = tween(300)
                        ) + fadeOut(animationSpec = tween(300))
                    ) {
                        if (pageContent.category == "Setup" || pageContent.category == "Legal") {
                            SetupPageTemplate(
                                modifier = Modifier.fillMaxSize(),
                                page = pageContent,
                                navController = navController,
                                firebaseLogger = firebaseLogger
                            )
                        } else {
                            StandardPageTemplate(
                                modifier = Modifier.fillMaxSize(),
                                page = pageContent,
                                firebaseLogger = firebaseLogger
                            )
                        }
                    }
                }
            }

            // Navigation Button
            NavigationButton(
                isLastPage = isLastPage,
                isEnabled = if (isLegalPage) termsAccepted else true,
                onNext = {
                    if (pagerState.currentPage < OnBoardingPages.size - 1) {
                        scope.launch {
                            // Log button press - Continue
                            firebaseLogger.logEvent(
                                "onboarding_continue_clicked",
                                mapOf(
                                    "from_page" to pagerState.currentPage,
                                    "from_page_name" to (OnBoardingPages[pagerState.currentPage]?.title ?: "Unknown")
                                ),
                                FirebaseLogger.Companion.EventCategory.USER_ACTION
                            )

                            pagerState.animateScrollToPage(
                                page = pagerState.currentPage + 1,
                                animationSpec = tween(300)
                            )
                        }
                    } else {
                        // Checking again to be sure
                        if (termsAccepted) {
                            // Log button press - Complete
                            firebaseLogger.logEvent(
                                "onboarding_complete_clicked",
                                null,
                                FirebaseLogger.Companion.EventCategory.USER_ACTION
                            )

                            viewModel.handleEvent(IntroductionViewModel.IntroEvent.CompleteSetup)
                            navController.navigate(BottomNavItem.Dashboard.screen_route) {
                                popUpTo("Intro") { inclusive = true }
                            }
                        } else {
                            // Show legal error
                            viewModel.handleEvent(IntroductionViewModel.IntroEvent.ShowLegalError)

                            // Log error
                            firebaseLogger.logEvent(
                                "legal_terms_not_accepted",
                                null,
                                FirebaseLogger.Companion.EventCategory.APP_ERROR
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun NavigationButton(
    isLastPage: Boolean,
    isEnabled: Boolean,
    onNext: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    ) {
        Button(
            onClick = onNext,
            enabled = isEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 6.dp,
                pressedElevation = 0.dp,
                disabledElevation = 0.dp
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isLastPage) "Begin Journey" else "Continue",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun ProgressHeader(
    currentCategory: String,
    currentPage: Int,
    totalPages: Int
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = currentCategory,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalPages) { index ->
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .size(if (currentPage == index) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (currentPage == index)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun StandardPageTemplate(
    modifier: Modifier = Modifier,
    page: OnBoardingPage,
    firebaseLogger: FirebaseLogger
) {
    // Log template view
    LaunchedEffect(page) {
        firebaseLogger.logEvent(
            "standard_template_viewed",
            mapOf(
                "page_title" to page.title,
                "page_category" to page.category
            ),
            FirebaseLogger.Companion.EventCategory.SCREEN_VIEW
        )
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Image Section
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.6f),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                    tonalElevation = 4.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Background decoration
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {}

                        // Main image
                        Image(
                            painter = painterResource(id = page.image),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                // Content Section
                Column(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = page.title,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = page.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupPageTemplate(
    modifier: Modifier = Modifier,
    page: OnBoardingPage,
    navController: NavHostController,
    firebaseLogger: FirebaseLogger
) {
    // Log template view
    LaunchedEffect(page) {
        firebaseLogger.logEvent(
            "setup_template_viewed",
            mapOf(
                "page_title" to page.title,
                "page_category" to page.category
            ),
            FirebaseLogger.Companion.EventCategory.SCREEN_VIEW
        )
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Card with Image and Title
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Image
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Image(
                        painter = painterResource(id = page.image),
                        contentDescription = null,
                        modifier = Modifier.padding(16.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                // Text Content
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = page.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Setup Content with Analytics-Aware Composable
        val originalExtra = page.extra
        val analyticsExtraWrapper: @Composable (NavHostController) -> Unit = { nav ->
            // This wrapper allows us to add analytics to the extra content
            originalExtra(nav)
        }

        analyticsExtraWrapper(navController)
    }
}