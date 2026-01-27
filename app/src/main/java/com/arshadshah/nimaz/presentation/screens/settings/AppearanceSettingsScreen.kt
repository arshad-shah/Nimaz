package com.arshadshah.nimaz.presentation.screens.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.AppTheme
import com.arshadshah.nimaz.presentation.viewmodel.SettingsEvent
import com.arshadshah.nimaz.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val generalState by viewModel.generalState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = "Appearance",
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Theme Section
            item {
                SectionTitle(text = "Theme")
            }
            item {
                ThemeSelectionCard(
                    selectedTheme = generalState.theme,
                    onThemeSelected = { viewModel.onEvent(SettingsEvent.SetTheme(it)) }
                )
            }

            // Accent Color Section
            item {
                SectionTitle(text = "Accent Color")
            }
            item {
                AccentColorCard()
            }

            // App Icon Section
            item {
                SectionTitle(text = "App Icon", showProBadge = true)
            }
            item {
                AppIconCard()
            }

            // Display Section
            item {
                SectionTitle(text = "Display")
            }
            item {
                DisplaySettingsCard(
                    hapticFeedback = generalState.hapticFeedback,
                    use24HourFormat = generalState.use24HourFormat,
                    onHapticFeedbackToggle = {
                        viewModel.onEvent(SettingsEvent.SetHapticFeedback(!generalState.hapticFeedback))
                    },
                    on24HourToggle = {
                        viewModel.onEvent(SettingsEvent.Set24HourFormat(!generalState.use24HourFormat))
                    }
                )
            }

            // Home Screen Section
            item {
                SectionTitle(text = "Home Screen")
            }
            item {
                HomeScreenSettingsCard(
                    useHijriPrimary = generalState.useHijriPrimary,
                    onHijriPrimaryToggle = {
                        viewModel.onEvent(SettingsEvent.SetHijriPrimary(!generalState.useHijriPrimary))
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// --- Section Title ---

@Composable
private fun SectionTitle(
    text: String,
    showProBadge: Boolean = false
) {
    Row(
        modifier = Modifier.padding(start = 5.dp, top = 24.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (showProBadge) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "PRO",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                ),
                color = MaterialTheme.colorScheme.inverseSurface,
                modifier = Modifier
                    .background(
                        color = Color(0xFFEAB308),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

// --- Theme Selection ---

@Composable
private fun ThemeSelectionCard(
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ThemePreviewOption(
                label = "Dark",
                isSelected = selectedTheme == AppTheme.DARK,
                onClick = { onThemeSelected(AppTheme.DARK) },
                modifier = Modifier.weight(1f)
            ) {
                // Dark preview
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0C0A09))
                ) {
                    ThemePreviewContent(contentColor = Color.White)
                }
            }
            ThemePreviewOption(
                label = "Light",
                isSelected = selectedTheme == AppTheme.LIGHT,
                onClick = { onThemeSelected(AppTheme.LIGHT) },
                modifier = Modifier.weight(1f)
            ) {
                // Light preview
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF5F5F4))
                ) {
                    ThemePreviewContent(contentColor = Color(0xFF0C0A09))
                }
            }
            ThemePreviewOption(
                label = "System",
                isSelected = selectedTheme == AppTheme.SYSTEM,
                onClick = { onThemeSelected(AppTheme.SYSTEM) },
                modifier = Modifier.weight(1f)
            ) {
                // System preview: diagonal split
                Box(modifier = Modifier.fillMaxSize()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        drawRect(color = Color(0xFF0C0A09))
                        drawPath(
                            path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(w, 0f)
                                lineTo(w, h)
                                lineTo(0f, h)
                                close()
                            },
                            color = Color(0xFFF5F5F4)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemePreviewOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    previewContent: @Composable () -> Unit
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.6f)
                .clip(RoundedCornerShape(12.dp))
                .then(
                    if (isSelected) {
                        Modifier.border(
                            3.dp,
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(12.dp)
                        )
                    } else {
                        Modifier.border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(12.dp)
                        )
                    }
                )
        ) {
            previewContent()
            // Checkmark for selected
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ThemePreviewContent(contentColor: Color) {
    Column(modifier = Modifier.padding(8.dp)) {
        // Header bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(contentColor.copy(alpha = 0.2f))
        )
        Spacer(modifier = Modifier.height(6.dp))
        // Card 1
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(contentColor.copy(alpha = 0.1f))
        )
        Spacer(modifier = Modifier.height(4.dp))
        // Card 2
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(contentColor.copy(alpha = 0.1f))
        )
    }
}

// --- Accent Color ---

@Composable
private fun AccentColorCard() {
    val colors = listOf(
        "Teal" to Color(0xFF14B8A6),
        "Gold" to Color(0xFFEAB308),
        "Purple" to Color(0xFFA855F7),
        "Blue" to Color(0xFF3B82F6),
        "Rose" to Color(0xFFF43F5E)
    )
    var selectedAccentColor by remember { mutableStateOf("Teal") }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            colors.forEach { (name, color) ->
                val isSelected = name == selectedAccentColor
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (isSelected) {
                                Modifier.border(
                                    3.dp,
                                    MaterialTheme.colorScheme.onSurface,
                                    CircleShape
                                )
                            } else {
                                Modifier
                            }
                        )
                        .clickable { selectedAccentColor = name },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// --- App Icon ---

private data class AppIconOption(
    val name: String,
    val emoji: String,
    val gradientColors: List<Color>
)

@Composable
private fun AppIconCard() {
    val iconOptions = listOf(
        AppIconOption("Default", "\uD83D\uDD4C", listOf(Color(0xFF14B8A6), Color(0xFF115E59))),
        AppIconOption("Dark", "\uD83D\uDD4C", listOf(Color(0xFF1C1917), Color(0xFF0C0A09))),
        AppIconOption("Ramadan", "\uD83C\uDF19", listOf(Color(0xFFEAB308), Color(0xFFCA8A04))),
        AppIconOption("Minimal", "\u2728", listOf(Color(0xFFA855F7), Color(0xFF7C3AED)))
    )
    var selectedAppIcon by remember { mutableStateOf("Default") }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            iconOptions.forEach { option ->
                val isSelected = option.name == selectedAppIcon
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { selectedAppIcon = option.name }
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = option.gradientColors,
                                    start = Offset.Zero,
                                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                )
                            )
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        3.dp,
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(16.dp)
                                    )
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option.emoji,
                            fontSize = 28.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = option.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// --- Display Settings ---

@Composable
private fun DisplaySettingsCard(
    hapticFeedback: Boolean,
    use24HourFormat: Boolean,
    onHapticFeedbackToggle: () -> Unit,
    on24HourToggle: () -> Unit
) {
    var showIslamicPatterns by remember { mutableStateOf(true) }
    var animationsEnabled by remember { mutableStateOf(true) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column {
            SettingToggleItem(
                label = "Islamic Patterns",
                description = "Show decorative patterns",
                isEnabled = showIslamicPatterns,
                onToggle = { showIslamicPatterns = !showIslamicPatterns }
            )
            SettingDivider()
            SettingToggleItem(
                label = "Animations",
                description = "Enable smooth transitions",
                isEnabled = animationsEnabled,
                onToggle = { animationsEnabled = !animationsEnabled }
            )
            SettingDivider()
            SettingToggleItem(
                label = "Haptic Feedback",
                description = "Vibration on interactions",
                isEnabled = hapticFeedback,
                onToggle = onHapticFeedbackToggle
            )
            SettingDivider()
            SettingToggleItem(
                label = "24-Hour Time",
                description = "Use 24-hour format",
                isEnabled = use24HourFormat,
                onToggle = on24HourToggle
            )
        }
    }
}

// --- Home Screen Settings ---

@Composable
private fun HomeScreenSettingsCard(
    useHijriPrimary: Boolean,
    onHijriPrimaryToggle: () -> Unit
) {
    var showCountdown by remember { mutableStateOf(true) }
    var showQuickActions by remember { mutableStateOf(true) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column {
            SettingToggleItem(
                label = "Show Islamic Date",
                description = "Display Hijri calendar",
                isEnabled = useHijriPrimary,
                onToggle = onHijriPrimaryToggle
            )
            SettingDivider()
            SettingToggleItem(
                label = "Show Countdown",
                description = "Next prayer countdown",
                isEnabled = showCountdown,
                onToggle = { showCountdown = !showCountdown }
            )
            SettingDivider()
            SettingToggleItem(
                label = "Show Quick Actions",
                description = "Quran, Tasbih, Qibla buttons",
                isEnabled = showQuickActions,
                onToggle = { showQuickActions = !showQuickActions }
            )
        }
    }
}

// --- Shared Components ---

@Composable
private fun SettingToggleItem(
    label: String,
    description: String,
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = { onToggle() }
        )
    }
}

@Composable
private fun SettingDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}
