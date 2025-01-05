package com.arshadshah.nimaz.ui.screens.introduction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.common.BannerDuration
import com.arshadshah.nimaz.ui.components.common.BannerLarge
import com.arshadshah.nimaz.ui.components.common.BannerVariant
import com.arshadshah.nimaz.ui.navigation.BottomNavItem
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.viewModel.IntroductionViewModel
import kotlinx.coroutines.launch


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IntroPage(
    navController: NavHostController,
    viewModel: IntroductionViewModel = hiltViewModel()
) {
    // Collect states from ViewModel
    val pagerState = rememberPagerState(0, 0f) { OnBoardingPages.size }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentPage = viewModel.currentPage.collectAsState()
    val isLoading = viewModel.isLoading.collectAsState()
    val error = viewModel.error.collectAsState()

    // States for NextButton
    val locationName = viewModel.locationName.collectAsState()
    val longitude = viewModel.longitude.collectAsState()
    val latitude = viewModel.latitude.collectAsState()
    val notificationsAllowed = viewModel.areNotificationsAllowed.collectAsState()
    val setupCompleted = viewModel.isSetupComplete.collectAsState()

    LaunchedEffect(Unit) {
        if (setupCompleted.value) {
            navController.navigate(BottomNavItem.Dashboard.screen_route) {
                popUpTo("introPage") { inclusive = true }
            }
        }
    }

    // Effect to handle navigation based on currentPage
    LaunchedEffect(currentPage.value) {
        pagerState.animateScrollToPage(currentPage.value)
    }

    Scaffold {
        Box(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {
            if (error.value != null) {
                BannerLarge(
                    message = error.value,
                    onDismiss = {
                        viewModel.handleEvent(IntroductionViewModel.IntroEvent.ClearError)
                    },
                    variant = BannerVariant.Error,
                    title = "Error",
                    onClick = {},
                    showFor = BannerDuration.FOREVER.value,
                    isOpen = remember { mutableStateOf(true) }
                )
            }
            Column(
                modifier = Modifier
                    .background(color = MaterialTheme.colorScheme.background)
                    .padding(bottom = 20.dp)
                    .fillMaxSize()
            ) {
                // Page indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    PageIndicator(
                        totalPages = OnBoardingPages.size,
                        currentPage = currentPage.value
                    )
                }
                HorizontalPager(
                    modifier = Modifier
                        .weight(10f)
                        .testTag("introPager"),
                    state = pagerState,
                    userScrollEnabled = false,
                    pageSize = PageSize.Fill,
                    verticalAlignment = Alignment.Top
                ) { position ->
                    OnBoardingPages[position]?.let { page ->
                        PagerScreen(
                            onBoardingPage = page,
                            position = position
                        )
                    }
                }

                // Navigation buttons
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .testTag("introButtons"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (viewModel.isLastPage()) {
                        FinishButton(
                            modifier = Modifier.fillMaxWidth(),
                            pagerState = pagerState
                        ) {
                            scope.launch {
                                viewModel.handleEvent(IntroductionViewModel.IntroEvent.CompleteSetup)
                                PrivateSharedPreferences(context).saveDataBoolean(
                                    AppConstants.IS_FIRST_INSTALL,
                                    false
                                )
                                navController.navigate(BottomNavItem.Dashboard.screen_route) {
                                    popUpTo("introPage") { inclusive = true }
                                }
                            }
                        }
                    } else {
                        NextButton(
                            modifier = Modifier.fillMaxWidth(),
                            currentPage = pagerState.currentPage,
                            locationName = locationName.value,
                            longitude = longitude.value,
                            latitude = latitude.value,
                            notificationsAllowed = notificationsAllowed.value,
                            onClick = {
                                scope.launch {
                                    if (viewModel.canNavigateNext()) {
                                        viewModel.handleEvent(
                                            IntroductionViewModel.IntroEvent.NavigateToPage(
                                                pagerState.currentPage + 1
                                            )
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // Loading indicator
            if (isLoading.value) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .testTag("loadingIndicator")
                )
            }
        }
    }
}

@Composable
fun PagerScreen(
    onBoardingPage: OnBoardingPage,
    position: Int,
) {
    val hasExtra = onBoardingPage.extra != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("pagerScreen $position")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Top spacer for better vertical distribution
            Spacer(modifier = Modifier.height(32.dp))

            // Image section with animation
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.large)
                        .testTag("pagerScreenImage"),
                    painter = painterResource(id = onBoardingPage.image),
                    contentDescription = "Pager Image",
                    contentScale = ContentScale.Fit
                )
            }

            // Content section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (hasExtra) 1.2f else 0.8f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title with background accent
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = onBoardingPage.title,
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .testTag("pagerScreenTitle")
                        )

                        // Accent line under title
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .width(40.dp)
                                .height(3.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(1.5.dp)
                                )
                        )
                    }
                }

                // Description with subtle background
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = onBoardingPage.description,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(16.dp)
                            .testTag("pagerScreenDescription")
                    )
                }

                // Extra content if available
                if (hasExtra) {
                    ElevatedCard(
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .testTag("pagerScreenExtra"),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 6.dp
                        )
                    ) {
                        onBoardingPage.extra?.invoke()
                    }
                }
            }

            // Bottom spacer for better vertical distribution
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PageIndicator(
    totalPages: Int,
    currentPage: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        repeat(totalPages) { index ->
            Box(
                modifier = Modifier
                    .size(if (currentPage == index) 8.dp else 6.dp)
                    .background(
                        color = if (currentPage == index)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PagerScreenPreview() {
    NimazTheme {
        PagerScreen(
            onBoardingPage = OnBoardingPage(
                image = R.drawable.praying,
                title = "Assalamu alaykum",
                description = "A user-friendly, Beautifully designed app for Muslims, with accurate prayer times completely Ad-Free.",
                extra = null
            ),
            position = 0
        )
    }
}


@Composable
fun NextButton(
    modifier: Modifier,
    currentPage: Int,
    locationName: String,
    longitude: Double,
    latitude: Double,
    notificationsAllowed: Boolean,
    onClick: () -> Unit
) {
    val isLocationPage = currentPage == 4
    val isNotificationPage = currentPage == 3

    val isButtonEnabled = remember(locationName, longitude, latitude, isLocationPage) {
        if (isLocationPage) {
            locationName.isNotEmpty() || (longitude != 0.0 && latitude != 0.0)
        } else {
            true
        }
    }

    val buttonText = remember(isNotificationPage, notificationsAllowed) {
        if (isNotificationPage && !notificationsAllowed) "Skip" else "Next"
    }

    Button(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .testTag("nextButton"),
        onClick = onClick,
        enabled = isButtonEnabled
    ) {
        Text(text = buttonText)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FinishButton(
    modifier: Modifier,
    pagerState: PagerState,
    onClick: () -> Unit
) {
    AnimatedVisibility(
        visible = pagerState.currentPage == OnBoardingPages.size - 1
    ) {
        Button(
            modifier = modifier
                .padding(horizontal = 8.dp)
                .testTag("finishButton"),
            onClick = onClick
        ) {
            Text(text = "Finish")
        }
    }
}