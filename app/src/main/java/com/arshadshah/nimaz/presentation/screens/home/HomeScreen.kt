package com.arshadshah.nimaz.presentation.screens.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.LocalInAppUpdateManager
import com.arshadshah.nimaz.core.util.UpdateState
import com.arshadshah.nimaz.presentation.components.molecules.PermissionAlertCard
// Prayer-specific accent colors matching the design prototype
import com.arshadshah.nimaz.presentation.viewmodel.HomeEvent
import com.arshadshah.nimaz.presentation.viewmodel.HomeViewModel
import com.arshadshah.nimaz.presentation.theme.LocalAnimationsEnabled
import com.arshadshah.nimaz.presentation.theme.LocalUseHijriPrimary
import com.arshadshah.nimaz.presentation.viewmodel.PrayerTimeDisplay
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    onNavigateToQuran: () -> Unit,
    onNavigateToHadith: () -> Unit,
    onNavigateToDua: () -> Unit,
    onNavigateToTasbih: () -> Unit,
    onNavigateToQibla: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToFasting: () -> Unit,
    onNavigateToZakat: () -> Unit,
    onNavigateToPrayerTracker: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPrayerSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val updateManager = LocalInAppUpdateManager.current
    val updateState = updateManager?.updateState?.collectAsState()?.value ?: UpdateState.Idle

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                // Permission Alert Cards
                if (!state.hasNotificationPermission) {
                    item {
                        PermissionAlertCard(
                            icon = Icons.Default.Notifications,
                            title = "Notifications Disabled",
                            description = "Prayer notifications need permission to alert you at prayer times.",
                            actionLabel = "Enable",
                            onAction = onNavigateToSettings,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                        )
                    }
                }
                if (!state.hasLocationPermission) {
                    item {
                        PermissionAlertCard(
                            icon = Icons.Default.LocationOn,
                            title = "Location Permission Needed",
                            description = "Location is needed to calculate accurate prayer times for your area.",
                            actionLabel = "Grant",
                            onAction = onNavigateToSettings,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                        )
                    }
                }
                if (state.isBatteryOptimized) {
                    item {
                        PermissionAlertCard(
                            icon = Icons.Default.BatteryAlert,
                            title = "Battery Optimization Active",
                            description = "Battery optimization may prevent timely prayer notifications.",
                            actionLabel = "Fix",
                            onAction = onNavigateToSettings,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                        )
                    }
                }

                // In-App Update Banner
                when (val currentUpdateState = updateState) {
                    is UpdateState.UpdateAvailable -> {
                        item {
                            UpdateBanner(
                                message = "A new version of Nimaz is available",
                                actionLabel = "Update",
                                onAction = { updateManager?.startUpdate() },
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                            )
                        }
                    }
                    is UpdateState.Downloading -> {
                        item {
                            UpdateBanner(
                                message = "Downloading update...",
                                actionLabel = null,
                                onAction = {},
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                            )
                        }
                    }
                    is UpdateState.Downloaded -> {
                        item {
                            UpdateBanner(
                                message = "Update ready to install",
                                actionLabel = "Restart",
                                onAction = { currentUpdateState.completeUpdate() },
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                            )
                        }
                    }
                    else -> {}
                }

                // Header with Prayer Info
                item {
                    HomeHeader(
                        locationName = state.locationName,
                        hijriDate = state.hijriDate,
                        gregorianDate = java.time.LocalDate.now().format(
                            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")
                        ),
                        nextPrayer = state.nextPrayer,
                        nextPrayerTime = state.prayerTimes.find { it.type == state.nextPrayer }?.time ?: "",
                        timeUntilNextPrayer = state.timeUntilNextPrayer,
                        onSettingsClick = onNavigateToSettings
                    )
                }

                // Jumu'ah Card (Friday only)
                if (state.isFriday) {
                    item {
                        JumuahCard(
                            jumuahTime = state.jumuahTime,
                            timeUntilJumuah = state.timeUntilJumuah,
                            isJumuahPassed = state.isJumuahPassed,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }

                // Today's Progress
                item {
                    TodaysProgressCard(
                        prayerTimes = state.prayerTimes,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                // Today Info Cards
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    SectionHeader(
                        title = "Today",
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TodayInfoCards(
                        fastingToday = state.fastingToday,
                        dailyHadith = state.dailyHadith,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                // Prayer Times
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader(title = "Prayer Times")
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onNavigateToPrayerSettings() }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Prayer List
                items(state.prayerTimes) { prayer ->
                    PrayerTimeCard(
                        prayer = prayer,
                        isActive = prayer.type == state.nextPrayer,
                        onClick = { onNavigateToPrayerTracker() },
                        onToggle = {
                            viewModel.onEvent(HomeEvent.TogglePrayerStatus(prayer.type))
                        },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun HomeHeader(
    locationName: String,
    hijriDate: String,
    gregorianDate: String,
    nextPrayer: PrayerType?,
    nextPrayerTime: String,
    timeUntilNextPrayer: String,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 8.dp)
        ) {
            // Top row with location and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Location
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = locationName.ifEmpty { "Location" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HeaderIconButton(
                        icon = Icons.Default.Settings,
                        onClick = onSettingsClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Current Prayer Section
            val useHijriPrimary = LocalUseHijriPrimary.current
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Primary Date
                Text(
                    text = if (useHijriPrimary) hijriDate.ifEmpty { "" } else gregorianDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Secondary Date
                Text(
                    text = if (useHijriPrimary) gregorianDate else hijriDate.ifEmpty { "" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Next Prayer Label
                Text(
                    text = "NEXT PRAYER",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Prayer Name
                Text(
                    text = nextPrayer?.displayName ?: "\u2014",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Arabic Name
                ArabicText(
                    text = getArabicPrayerName(nextPrayer),
                    size = ArabicTextSize.MEDIUM,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Countdown Timer
                CountdownTimer(timeUntilNextPrayer = timeUntilNextPrayer)

                Spacer(modifier = Modifier.height(12.dp))

                // Prayer Time
                Text(
                    text = "at $nextPrayerTime",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun HeaderIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun CountdownTimer(
    timeUntilNextPrayer: String,
    modifier: Modifier = Modifier
) {
    // Parse time string (e.g., "2h 34m" or "34m 12s")
    val parts = timeUntilNextPrayer.split(" ")
    var hours = "00"
    var minutes = "00"
    var seconds = "00"

    parts.forEach { part ->
        when {
            part.endsWith("h") -> hours = part.dropLast(1).padStart(2, '0')
            part.endsWith("m") -> minutes = part.dropLast(1).padStart(2, '0')
            part.endsWith("s") -> seconds = part.dropLast(1).padStart(2, '0')
        }
    }

    // Animation for pulsing effect — disabled when animations are off
    val animationsEnabled = LocalAnimationsEnabled.current
    val infiniteTransition = rememberInfiniteTransition(label = "countdown_pulse")
    val alpha by if (animationsEnabled) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.7f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
    } else {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "alpha_static"
        )
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CountdownUnit(value = hours, label = "Hours", alpha = alpha)
        CountdownSeparator()
        CountdownUnit(value = minutes, label = "Minutes", alpha = alpha)
        CountdownSeparator()
        CountdownUnit(value = seconds, label = "Seconds", alpha = alpha)
    }
}

@Composable
private fun CountdownUnit(
    value: String,
    label: String,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .alpha(alpha),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun CountdownSeparator(modifier: Modifier = Modifier) {
    Text(
        text = ":",
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 8.dp, vertical = 0.dp)
    )
}

@Composable
private fun TodaysProgressCard(
    prayerTimes: List<PrayerTimeDisplay>,
    modifier: Modifier = Modifier
) {
    val mainPrayers = prayerTimes.filter {
        it.type in listOf(PrayerType.FAJR, PrayerType.DHUHR, PrayerType.ASR, PrayerType.MAGHRIB, PrayerType.ISHA)
    }
    val completedCount = mainPrayers.count { it.prayerStatus == PrayerStatus.PRAYED }
    val totalCount = mainPrayers.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Progress",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$completedCount of $totalCount prayers",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(15.dp))

                // Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(15.dp))

                // Prayer dots
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    mainPrayers.forEach { prayer ->
                        ProgressPrayerDot(
                            label = prayer.name.take(5),
                            isCompleted = prayer.prayerStatus == PrayerStatus.PRAYED,
                            isCurrent = prayer.isCurrent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressPrayerDot(
    label: String,
    isCompleted: Boolean,
    isCurrent: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isCurrent -> MaterialTheme.colorScheme.secondary
                        isCompleted -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline
                    }
                )
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier
    )
}

@Composable
private fun TodayInfoCards(
    fastingToday: Boolean,
    dailyHadith: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Fasting Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LightMode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Fasting",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (fastingToday) "Today: Fasting" else "No fast today",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Daily Hadith Card
        if (dailyHadith != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF3B82F6).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = null,
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Hadith of the Day",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = dailyHadith,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PrayerTimeCard(
    prayer: PrayerTimeDisplay,
    isActive: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prayerColor = getPrayerColor(prayer.type)
    val isPrayed = prayer.prayerStatus == PrayerStatus.PRAYED
    val isSunrise = prayer.type == PrayerType.SUNRISE

    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (prayer.isPassed && !isActive) 0.6f else 1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (isActive) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        } else null,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Prayer Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(prayerColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getPrayerIcon(prayer.type),
                    contentDescription = null,
                    tint = prayerColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(15.dp))

            // Prayer Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = prayer.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                ArabicText(
                    text = getArabicPrayerName(prayer.type),
                    size = ArabicTextSize.SMALL,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Prayer Time
            Text(
                text = prayer.time,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.width(15.dp))

            // Prayer Status - toggleable independently (but not for Sunrise)
            if (!isSunrise) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onToggle)
                        .then(
                            if (isPrayed) {
                                Modifier.background(MaterialTheme.colorScheme.primary)
                            } else {
                                Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPrayed) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Prayed",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            } else {
                // Empty spacer to maintain layout consistency for Sunrise
                Spacer(modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun JumuahCard(
    jumuahTime: String,
    timeUntilJumuah: String,
    isJumuahPassed: Boolean,
    modifier: Modifier = Modifier
) {
    val jumuahGreen = Color(0xFF2E7D32)
    val jumuahGreenLight = Color(0xFF43A047)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(jumuahGreen, jumuahGreenLight)
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mosque,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Jumu'ah Mubarak",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "\u0627\u0644\u062C\u0645\u0639\u0629",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    if (jumuahTime.isNotEmpty()) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = jumuahTime,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Khutbah time",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Countdown or passed status
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (isJumuahPassed) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Jumu'ah prayer time has passed",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                        }
                    } else if (timeUntilJumuah.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Time until Jumu'ah",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = timeUntilJumuah,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Friday reminder text
                Text(
                    text = "\"The best day on which the sun rises is Friday.\" \u2014 Sahih Muslim",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun getPrayerColor(prayerType: PrayerType?): Color {
    return when (prayerType) {
        PrayerType.FAJR -> Color(0xFF6366F1)      // Indigo
        PrayerType.SUNRISE -> Color(0xFFF59E0B)    // Amber
        PrayerType.DHUHR -> Color(0xFFEAB308)       // Yellow
        PrayerType.ASR -> Color(0xFFF97316)          // Orange
        PrayerType.MAGHRIB -> Color(0xFFEF4444)    // Red
        PrayerType.ISHA -> Color(0xFF8B5CF6)        // Violet
        else -> MaterialTheme.colorScheme.primary
    }
}

private fun getPrayerIcon(prayerType: PrayerType?): ImageVector {
    return when (prayerType) {
        PrayerType.FAJR -> Icons.Default.DarkMode
        PrayerType.SUNRISE -> Icons.Default.WbSunny
        PrayerType.DHUHR -> Icons.Default.WbSunny
        PrayerType.ASR -> Icons.Default.LightMode
        PrayerType.MAGHRIB -> Icons.Default.WbSunny
        PrayerType.ISHA -> Icons.Default.DarkMode
        else -> Icons.Default.LightMode
    }
}

private fun getArabicPrayerName(prayerType: PrayerType?): String {
    return when (prayerType) {
        PrayerType.FAJR -> "\u0627\u0644\u0641\u062C\u0631"
        PrayerType.SUNRISE -> "\u0627\u0644\u0634\u0631\u0648\u0642"
        PrayerType.DHUHR -> "\u0627\u0644\u0638\u0647\u0631"
        PrayerType.ASR -> "\u0627\u0644\u0639\u0635\u0631"
        PrayerType.MAGHRIB -> "\u0627\u0644\u0645\u063A\u0631\u0628"
        PrayerType.ISHA -> "\u0627\u0644\u0639\u0634\u0627\u0621"
        else -> ""
    }
}

@Composable
private fun UpdateBanner(
    message: String,
    actionLabel: String?,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            if (actionLabel != null) {
                Spacer(modifier = Modifier.width(12.dp))
                Card(
                    modifier = Modifier.clickable { onAction() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = actionLabel,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}
