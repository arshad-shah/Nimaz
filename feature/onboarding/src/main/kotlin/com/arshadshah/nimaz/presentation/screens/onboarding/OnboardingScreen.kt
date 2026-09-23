package com.arshadshah.nimaz.presentation.screens.onboarding

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazDivider
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconType
import com.arshadshah.nimaz.presentation.components.atoms.NimazPageIndicator
import com.arshadshah.nimaz.presentation.components.atoms.NimazPager
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.rememberNimazPagerState
import com.arshadshah.nimaz.presentation.theme.AdaptiveSpacing
import com.arshadshah.nimaz.presentation.theme.AmiriFontFamily
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.OnboardingArtColors
import com.arshadshah.nimaz.presentation.viewmodel.onboarding.OnboardingEvent
import com.arshadshah.nimaz.presentation.viewmodel.onboarding.OnboardingViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

internal const val ONBOARDING_PAGE_COUNT = 5
internal const val ONBOARDING_MOTION_MS = 420

/** How far the artwork lags its page during a swipe: 0.6 of the page travel is cancelled out. */
private const val ART_PARALLAX = 0.6f

private val Ivory = OnboardingArtColors.Ivory
private val Champagne = OnboardingArtColors.Champagne
private val Floor = NimazColors.OnboardingBgTop

/** Space the pinned indicator + primary action occupy above the navigation bar. */
private val FooterHeight = 136.dp

/**
 * One intro page. [focus] frames the 2:3 artwork on a tall screen — the horizontal bias keeps
 * each scene's focal point (arch, figure, sun) in view once the sides are cropped. [detail]
 * continues the body in the same paragraph, for a page whose body alone is a single line.
 */
private data class IntroPage(
    val arabic: Int,
    val title: Int,
    val body: Int,
    val art: OnboardingIllustration,
    val focus: Float,
    val detail: Int? = null,
)

private val introPages = listOf(
    IntroPage(R.string.onboarding_intro_welcome_arabic, R.string.onboarding_intro_welcome_title,
        R.string.onboarding_intro_welcome_body, OnboardingIllustration.WELCOME, focus = 0f,
        detail = R.string.onboarding_intro_welcome_caption),
    IntroPage(R.string.onboarding_intro_prayer_arabic, R.string.onboarding_intro_prayer_title,
        R.string.onboarding_intro_prayer_body, OnboardingIllustration.PRAYER, focus = 0.28f),
    IntroPage(R.string.onboarding_intro_learning_arabic, R.string.onboarding_intro_learning_title,
        R.string.onboarding_intro_learning_body, OnboardingIllustration.LEARNING, focus = -0.24f),
    IntroPage(R.string.onboarding_intro_progress_arabic, R.string.onboarding_intro_progress_title,
        R.string.onboarding_intro_progress_body, OnboardingIllustration.PROGRESS, focus = 0f),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val pager = rememberNimazPagerState(pageCount = { ONBOARDING_PAGE_COUNT })
    var navigationJob by remember { mutableStateOf<Job?>(null) }
    var completing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val view = LocalView.current
    if (!view.isInEditMode) {
        DisposableEffect(view) {
            val controller = WindowCompat.getInsetsController((view.context as Activity).window, view)
            val previous = controller.isAppearanceLightStatusBars
            controller.isAppearanceLightStatusBars = false
            onDispose { controller.isAppearanceLightStatusBars = previous }
        }
    }
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.onEvent(OnboardingEvent.UpdatePermissionStatus(location =
            it[Manifest.permission.ACCESS_FINE_LOCATION] == true || it[Manifest.permission.ACCESS_COARSE_LOCATION] == true))
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.onEvent(OnboardingEvent.UpdatePermissionStatus(notification = it))
    }
    val batteryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.onEvent(OnboardingEvent.CheckBatteryOptimization)
    }
    LaunchedEffect(state.error) {
        state.error?.let { snackbar.showSnackbar(it); viewModel.onEvent(OnboardingEvent.DismissError) }
    }
    // Report settled pages only. Swiping through a partial page is not a completed funnel step.
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.distinctUntilChanged()
            .collect { viewModel.onEvent(OnboardingEvent.SetCurrentPage(it)) }
    }
    val complete: () -> Unit = {
        if (!completing) {
            completing = true
            viewModel.onEvent(OnboardingEvent.CompleteOnboarding)
            onComplete()
        }
    }
    val moveTo: (Int) -> Unit = { page ->
        if (navigationJob?.isActive != true && !pager.isScrollInProgress) {
            navigationJob = scope.launch {
                pager.animateScrollToPage(
                    page.coerceIn(0, ONBOARDING_PAGE_COUNT - 1),
                    animationSpec = tween(ONBOARDING_MOTION_MS, easing = FastOutSlowInEasing),
                )
            }
        }
    }
    val permissionRows: @Composable ColumnScope.() -> Unit = {
        PermissionRow(Icons.Default.LocationOn, stringResource(R.string.onboarding_location_title),
            stringResource(R.string.onboarding_location_reason), state.locationPermissionGranted,
            if (state.locationDetected) state.locationName else stringResource(R.string.onboarding_location_granted)) {
            locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
        PermissionRow(Icons.Default.Notifications, stringResource(R.string.onboarding_notification_title),
            stringResource(R.string.onboarding_notification_reason), state.notificationPermissionGranted,
            stringResource(R.string.onboarding_notification_granted)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else viewModel.onEvent(OnboardingEvent.UpdatePermissionStatus(notification = true))
        }
        PermissionRow(Icons.Default.BatteryChargingFull, stringResource(R.string.onboarding_battery_title),
            stringResource(R.string.onboarding_battery_reason), state.batteryOptimizationDisabled,
            stringResource(R.string.onboarding_battery_granted)) {
            batteryLauncher.launch(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .apply { data = "package:${context.packageName}".toUri() })
        }
    }
    val lastPage = ONBOARDING_PAGE_COUNT - 1
    // Back steps through the pages; on the first one it falls through to the system (leave the app).
    BackHandler(enabled = pager.settledPage > 0) { moveTo(pager.settledPage - 1) }
    val busy = pager.isScrollInProgress || navigationJob?.isActive == true || completing
    NimazScreenScaffold(
        containerColor = Floor,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { insets ->
        Box(Modifier.fillMaxSize()) {
            NimazPager(state = pager, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { index ->
                // Positive when this page sits left of the viewport, negative when right of it.
                val offset = { (pager.currentPage - index) + pager.currentPageOffsetFraction }
                val revealed = pager.settledPage == index
                if (index == lastPage) {
                    SetupPage(insets, offset, revealed, permissionRows)
                } else {
                    IntroPageContent(introPages[index], insets, offset, revealed)
                }
            }
            // Skip leads to setup rather than out: it is the page that makes prayer times and
            // reminders work, so nobody should miss it by skipping the introductions.
            AnimatedVisibility(
                visible = pager.settledPage < lastPage,
                enter = fadeIn(tween(220)), exit = fadeOut(tween(180)),
                modifier = Modifier.align(Alignment.TopEnd)
                    .padding(top = insets.calculateTopPadding() + 4.dp, end = AdaptiveSpacing.screenPadding() - 8.dp),
            ) {
                NimazButton(stringResource(R.string.onboarding_skip), { moveTo(lastPage) },
                    variant = NimazButtonVariant.TEXT, enabled = !busy,
                    colors = ButtonDefaults.textButtonColors(contentColor = Ivory.copy(alpha = 0.72f)))
            }
            Column(
                Modifier.align(Alignment.BottomCenter).widthIn(max = 600.dp).fillMaxWidth()
                    .padding(horizontal = AdaptiveSpacing.screenPadding())
                    .padding(bottom = insets.calculateBottomPadding() + 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                NimazPageIndicator(
                    pager,
                    activeColor = Champagne,
                    inactiveColor = Ivory.copy(alpha = 0.26f),
                    dotSize = 7.dp,
                    activeWidth = 28.dp,
                    spacing = 5.dp,
                    followDrag = true,
                    glowColor = Champagne,
                )
                Spacer(Modifier.height(28.dp))
                Crossfade(
                    targetState = pager.settledPage == lastPage,
                    animationSpec = tween(220), label = "onboarding-primary-action",
                ) { last ->
                    NimazButton(
                        text = stringResource(if (last) R.string.onboarding_intro_begin else R.string.onboarding_continue),
                        onClick = { if (last) complete() else moveTo(pager.settledPage + 1) },
                        fullWidth = true,
                        enabled = !busy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Ivory, contentColor = Floor,
                            disabledContainerColor = Ivory, disabledContentColor = Floor,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Rises into place in a light stagger once its page settles, so a page arrives as one gesture
 * rather than all at once. A page that is only passing through keeps its copy still.
 */
@Composable
private fun Reveal(revealed: Boolean, order: Int, content: @Composable () -> Unit) {
    val progress by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = if (revealed) tween(600, delayMillis = order * 70, easing = FastOutSlowInEasing) else tween(0),
        label = "onboarding-reveal-$order",
    )
    Box(Modifier.graphicsLayer { alpha = progress; translationY = (1f - progress) * 24.dp.toPx() }) { content() }
}

@Composable
private fun IntroPageContent(page: IntroPage, insets: PaddingValues, offset: () -> Float, revealed: Boolean) {
    // Clipped: the parallax shifts the art sideways, and unclipped it paints over the next page.
    Box(Modifier.fillMaxSize().clipToBounds()) {
        Image(
            painter = painterResource(page.art.resource),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = BiasAlignment(page.focus, 0f),
            modifier = Modifier.fillMaxSize().graphicsLayer { translationX = offset() * size.width * ART_PARALLAX },
        )
        // Status-bar scrim at the top; the art's own dark floor carries the copy at the bottom.
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0f to Floor.copy(alpha = 0.75f),
            0.16f to Floor.copy(alpha = 0f),
            0.42f to Floor.copy(alpha = 0f),
            0.66f to Floor.copy(alpha = 0.9f),
            0.84f to Floor,
        )))
        Column(
            Modifier.align(Alignment.BottomCenter).widthIn(max = 600.dp).fillMaxWidth()
                .padding(horizontal = AdaptiveSpacing.screenPadding())
                .padding(bottom = insets.calculateBottomPadding() + FooterHeight + 20.dp),
        ) {
            Reveal(revealed, 0) {
                Text(stringResource(page.arabic), fontFamily = AmiriFontFamily, fontSize = 30.sp, color = Champagne)
            }
            Spacer(Modifier.height(10.dp))
            Reveal(revealed, 1) {
                Text(stringResource(page.title),
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Light),
                    color = Ivory, modifier = Modifier.semantics { heading() })
            }
            Spacer(Modifier.height(12.dp))
            val body = stringResource(page.body)
            val detail = page.detail?.let { stringResource(it).replace('\n', ' ') }
            Reveal(revealed, 2) {
                Text(if (detail == null) body else "$body $detail", style = MaterialTheme.typography.bodyLarge,
                    color = OnboardingArtColors.TextSoft.copy(alpha = 0.78f))
            }
        }
    }
}

@Composable
private fun SetupPage(
    insets: PaddingValues,
    offset: () -> Float,
    revealed: Boolean,
    rows: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val artHeight = maxHeight * 0.44f
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(bottom = insets.calculateBottomPadding() + FooterHeight + 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth().height(artHeight).clipToBounds()) {
                Image(
                    painter = painterResource(OnboardingIllustration.PERMISSIONS.resource),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = BiasAlignment(0f, -0.1f),
                    modifier = Modifier.fillMaxSize().graphicsLayer { translationX = offset() * size.width * ART_PARALLAX },
                )
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                    0f to Floor.copy(alpha = 0.6f),
                    0.25f to Floor.copy(alpha = 0f),
                    0.7f to Floor.copy(alpha = 0.35f),
                    1f to Floor,
                )))
            }
            Column(Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(horizontal = AdaptiveSpacing.screenPadding())) {
                Reveal(revealed, 0) {
                    Text(stringResource(R.string.onboarding_setup_title),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Light),
                        color = Ivory, modifier = Modifier.semantics { heading() })
                }
                Spacer(Modifier.height(10.dp))
                Reveal(revealed, 1) {
                    Text(stringResource(R.string.onboarding_intro_setup_body), style = MaterialTheme.typography.bodyLarge,
                        color = OnboardingArtColors.TextSoft.copy(alpha = 0.78f))
                }
                Spacer(Modifier.height(20.dp))
                Reveal(revealed, 2) {
                    Column {
                        NimazDivider(color = Ivory.copy(alpha = 0.12f))
                        rows()
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    reason: String,
    granted: Boolean,
    grantedLabel: String,
    onRequest: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        NimazIcon(
            imageVector = if (granted) Icons.Default.Check else icon,
            contentDescription = null,
            type = NimazIconType.CONTAINED,
            tint = if (granted) Floor else Champagne,
            containerColor = if (granted) Champagne else Champagne.copy(alpha = 0.12f),
            containerSize = 40.dp,
            iconSize = 18.dp,
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Ivory)
            Text(if (granted) grantedLabel else reason, style = MaterialTheme.typography.bodySmall,
                color = if (granted) Champagne else OnboardingArtColors.TextSoft.copy(alpha = 0.72f))
        }
        // Hidden, not disabled: an Allow beside a granted result is the state this row rules out.
        if (!granted) {
            NimazButton(stringResource(R.string.onboarding_allow), onRequest, variant = NimazButtonVariant.TEXT,
                colors = ButtonDefaults.textButtonColors(contentColor = Champagne))
        }
    }
    NimazDivider(color = Ivory.copy(alpha = 0.12f))
}
