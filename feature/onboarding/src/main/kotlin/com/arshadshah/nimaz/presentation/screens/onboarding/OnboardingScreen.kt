package com.arshadshah.nimaz.presentation.screens.onboarding

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RadioButtonUnchecked
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcons
import com.arshadshah.nimaz.presentation.components.atoms.NimazPageIndicator
import com.arshadshah.nimaz.presentation.components.atoms.NimazPager
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.rememberNimazPagerState
import com.arshadshah.nimaz.presentation.theme.AdaptiveSpacing
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.onboarding.OnboardingEvent
import com.arshadshah.nimaz.presentation.viewmodel.onboarding.OnboardingViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs

internal const val ONBOARDING_PAGE_COUNT = 5
internal const val ONBOARDING_MOTION_MS = 420

private data class IntroPage(val title: Int, val description: Int, val caption: Int, val art: OnboardingIllustration)

private val introPages = listOf(
    IntroPage(R.string.onboarding_intro_welcome_title, R.string.onboarding_intro_welcome_body,
        R.string.onboarding_intro_welcome_caption, OnboardingIllustration.WELCOME),
    IntroPage(R.string.onboarding_intro_prayer_title, R.string.onboarding_intro_prayer_body,
        R.string.onboarding_intro_prayer_caption, OnboardingIllustration.PRAYER),
    IntroPage(R.string.onboarding_intro_learning_title, R.string.onboarding_intro_learning_body,
        R.string.onboarding_intro_learning_caption, OnboardingIllustration.LEARNING),
    IntroPage(R.string.onboarding_intro_progress_title, R.string.onboarding_intro_progress_body,
        R.string.onboarding_intro_progress_caption, OnboardingIllustration.PROGRESS),
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
    val move: (Int) -> Unit = { delta ->
        if (navigationJob?.isActive != true && !pager.isScrollInProgress) {
            navigationJob = scope.launch {
                pager.animateScrollToPage(
                    (pager.settledPage + delta).coerceIn(0, ONBOARDING_PAGE_COUNT - 1),
                    animationSpec = tween(ONBOARDING_MOTION_MS, easing = FastOutSlowInEasing),
                )
            }
        }
    }
    val permissionCards: @Composable () -> Unit = {
            PermissionCard(Icons.Default.LocationOn, stringResource(R.string.onboarding_location_title),
                stringResource(R.string.onboarding_location_description), state.locationPermissionGranted,
                if (state.locationDetected) state.locationName else stringResource(R.string.onboarding_location_granted)) {
                locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
            PermissionCard(Icons.Default.Notifications, stringResource(R.string.onboarding_notification_title),
                stringResource(R.string.onboarding_notification_description), state.notificationPermissionGranted,
                stringResource(R.string.onboarding_notification_granted)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else viewModel.onEvent(OnboardingEvent.UpdatePermissionStatus(notification = true))
            }
            PermissionCard(Icons.Default.BatteryChargingFull, stringResource(R.string.onboarding_battery_title),
                stringResource(R.string.onboarding_battery_description), state.batteryOptimizationDisabled,
                stringResource(R.string.onboarding_battery_granted)) {
                batteryLauncher.launch(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .apply { data = "package:${context.packageName}".toUri() })
            }
    }
    BackHandler(enabled = pager.settledPage > 0) { move(-1) }
    val busy = pager.isScrollInProgress || navigationJob?.isActive == true || completing
    NimazScreenScaffold(
        containerColor = NimazColors.OnboardingBgTop,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { insets ->
        Box(Modifier.fillMaxSize()) {
            // Each page owns its artwork: foreground and background now move together,
            // rather than swapping the full-screen image halfway through a swipe.
            NimazPager(state = pager, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { index ->
                Box(Modifier.fillMaxSize().graphicsLayer {
                    val distance = abs((pager.currentPage - index) + pager.currentPageOffsetFraction)
                    alpha = 1f - distance.coerceIn(0f, 1f) * 0.18f
                }) {
                    if (index == ONBOARDING_PAGE_COUNT - 1) {
                        PermissionContent(insets, permissionCards)
                    } else {
                        IllustratedOnboardingBackground(introPages[index].art, Modifier.fillMaxSize())
                        IntroContent(introPages[index], index, insets)
                    }
                }
            }
            Row(
                Modifier.align(Alignment.TopCenter).widthIn(max = 700.dp).fillMaxWidth()
                    .padding(top = insets.calculateTopPadding(), start = AdaptiveSpacing.screenPadding(),
                        end = AdaptiveSpacing.screenPadding()),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Keep this slot even on page zero so controls never jump sideways.
                Box(Modifier.weight(1f).height(48.dp)) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = pager.settledPage > 0,
                        enter = fadeIn(tween(220)), exit = fadeOut(tween(180)),
                    ) {
                        NimazButton(stringResource(R.string.onboarding_back), { move(-1) },
                            variant = NimazButtonVariant.TEXT, leadingIcon = NimazIcons.Previous,
                            enabled = !busy, colors = ButtonDefaults.textButtonColors(contentColor = IllumCream))
                    }
                }
                Box(Modifier.weight(1f).height(48.dp), contentAlignment = Alignment.CenterEnd) {
                    androidx.compose.animation.AnimatedVisibility(pager.settledPage < ONBOARDING_PAGE_COUNT - 1,
                        enter = fadeIn(tween(220)), exit = fadeOut(tween(180))) {
                        NimazButton(stringResource(R.string.onboarding_skip), complete,
                            variant = NimazButtonVariant.TEXT, enabled = !busy,
                            colors = ButtonDefaults.textButtonColors(contentColor = IllumCream))
                    }
                }
            }
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(NimazColors.OnboardingBgTop.copy(alpha = 0f),
                        NimazColors.OnboardingBgTop)))
                    .padding(bottom = insets.calculateBottomPadding() + 16.dp, top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                NimazPageIndicator(pager, activeColor = IllumGold,
                    inactiveColor = IllumTextSoft.copy(alpha = 0.3f))
                Spacer(Modifier.height(16.dp))
                Crossfade(
                    targetState = pager.settledPage == ONBOARDING_PAGE_COUNT - 1,
                    animationSpec = tween(220), label = "onboarding-primary-action",
                    modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
                        .padding(horizontal = AdaptiveSpacing.screenPadding()),
                ) { last ->
                    NimazButton(
                        text = stringResource(if (last) R.string.onboarding_get_started else R.string.onboarding_next),
                        onClick = { if (last) complete() else move(1) },
                        fullWidth = true, leadingIcon = if (last) Icons.Default.Check else NimazIcons.Next,
                        enabled = !busy,
                    )
                }
                // Keep footer height stable while the optional exit fades in on page five.
                Box(Modifier.height(48.dp)) {
                    androidx.compose.animation.AnimatedVisibility(
                        pager.settledPage == ONBOARDING_PAGE_COUNT - 1,
                        enter = fadeIn(tween(220)), exit = fadeOut(tween(180)),
                    ) {
                        NimazButton(stringResource(R.string.onboarding_intro_not_now), complete,
                            variant = NimazButtonVariant.TEXT, enabled = !busy,
                            colors = ButtonDefaults.textButtonColors(contentColor = IllumCream))
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionContent(insets: PaddingValues, cards: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(top = insets.calculateTopPadding() + 64.dp,
                bottom = insets.calculateBottomPadding() + 184.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = 600.dp).fillMaxWidth()
            .padding(horizontal = AdaptiveSpacing.screenPadding()),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.onboarding_permissions_title),
                style = MaterialTheme.typography.headlineMedium, color = IllumCream,
                modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.onboarding_intro_setup_body),
                style = MaterialTheme.typography.bodyMedium, color = IllumTextSoft,
                textAlign = TextAlign.Center)
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(OnboardingIllustration.PERMISSIONS.resource),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(240.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                alignment = androidx.compose.ui.BiasAlignment(0f, -0.3f),
            )
            cards()
        }
    }
}

@Composable
private fun IntroContent(page: IntroPage, index: Int, insets: PaddingValues) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxHeight < 600.dp
        val illustrationSpace = if (compact) 48.dp else maxHeight * 0.28f
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(top = insets.calculateTopPadding() + 64.dp,
                    bottom = insets.calculateBottomPadding() + 184.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(horizontal = AdaptiveSpacing.screenPadding()),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(page.title),
                    style = if (index == 0) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center, color = IllumCream, modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(12.dp))
                Text(stringResource(page.description), style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center, color = IllumTextSoft)
            }
            Spacer(Modifier.height(illustrationSpace))
            Box(Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(horizontal = AdaptiveSpacing.screenPadding())) {
                when (index) {
                    1 -> IntroCard {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            NimazIcon(Icons.Default.Notifications, contentDescription = null, tint = IllumCream)
                            Column {
                                Text(stringResource(R.string.onboarding_intro_reminder), style = MaterialTheme.typography.labelSmall)
                                Text(stringResource(R.string.onboarding_intro_fajr), style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(R.string.onboarding_intro_new_day), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    2 -> Box(Modifier.align(Alignment.CenterEnd).fillMaxWidth(0.5f)) {
                        IntroCard {
                            listOf(R.string.onboarding_intro_standing, R.string.onboarding_intro_bowing,
                                R.string.onboarding_intro_prostration, R.string.onboarding_intro_sitting,
                                R.string.onboarding_intro_salam).forEachIndexed { step, label ->
                                Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    NimazIcon(if (step == 0) Icons.Default.Circle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null, tint = if (step == 0) IllumGold else IllumTextSoft, iconSize = 12.dp)
                                    Text(stringResource(label), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    3 -> IntroCard {
                        Text(stringResource(R.string.onboarding_intro_your_progress), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.onboarding_intro_example_week), style = MaterialTheme.typography.labelSmall)
                        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            repeat(7) { day ->
                                NimazIcon(if (day in listOf(0, 1, 2, 4)) Icons.Default.Check else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = stringResource(if (day in listOf(0, 1, 2, 4))
                                        R.string.onboarding_intro_recorded else R.string.onboarding_intro_unrecorded),
                                    tint = if (day in listOf(0, 1, 2, 4)) NimazColors.Primary400 else IllumTextSoft,
                                    iconSize = 20.dp)
                            }
                        }
                    }
                    else -> Spacer(Modifier.height(72.dp))
                }
            }
            Text(stringResource(page.caption), style = MaterialTheme.typography.bodySmall, color = IllumTextSoft,
                textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = AdaptiveSpacing.screenPadding(), vertical = 20.dp))
        }
    }
}

@Composable
private fun IntroCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    NimazCard(colors = NimazCardDefaults.colors(
        container = NimazColors.OnboardingBgTop.copy(alpha = 0.96f), content = IllumCream,
        border = IllumTextSoft.copy(alpha = 0.25f)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun PermissionCard(icon: ImageVector, title: String, description: String, granted: Boolean,
    grantedLabel: String, onRequest: () -> Unit) {
    NimazCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        colors = NimazCardDefaults.colors(container = NimazColors.OnboardingBgBottom, content = IllumCream)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                NimazIcon(if (granted) Icons.Default.Check else icon, contentDescription = null, tint = IllumCream)
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    Text(if (granted) grantedLabel else description,
                        style = MaterialTheme.typography.bodySmall, color = IllumTextSoft)
                }
                if (!granted) NimazButton(stringResource(R.string.onboarding_grant), onRequest,
                    variant = NimazButtonVariant.QUIET)
            }
        }
    }
}
