package com.arshadshah.nimaz.presentation.screens.settings

import androidx.compose.ui.res.stringResource
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.R
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazCheckbox
import com.arshadshah.nimaz.presentation.components.atoms.NimazCheckboxSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCheckboxType
import com.arshadshah.nimaz.presentation.components.atoms.NimazCheckboxVariant
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.arshadshah.nimaz.core.navigation.ScreenTags
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.presentation.components.atoms.NimazDivider
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup
import com.arshadshah.nimaz.presentation.components.molecules.NimazSettingsItem
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
                title = stringResource(R.string.appearance),
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag(ScreenTags.AppearanceList)
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Theme Section
            item {
                NimazSectionHeader(title = stringResource(R.string.appearance_theme))
            }
            item {
                ThemeSelectionCard(
                    selectedTheme = generalState.theme,
                    onThemeSelected = { viewModel.onEvent(SettingsEvent.SetTheme(it)) }
                )
            }

            // Display Section
            item {
                NimazSectionHeader(title = stringResource(R.string.appearance_display))
            }
            item {
                DisplaySettingsCard(
                    hapticFeedback = generalState.hapticFeedback,
                    use24HourFormat = generalState.use24HourFormat,
                    showIslamicPatterns = generalState.showIslamicPatterns,
                    animationsEnabled = generalState.animationsEnabled,
                    onHapticFeedbackToggle = {
                        viewModel.onEvent(SettingsEvent.SetHapticFeedback(!generalState.hapticFeedback))
                    },
                    on24HourToggle = {
                        viewModel.onEvent(SettingsEvent.Set24HourFormat(!generalState.use24HourFormat))
                    },
                    onIslamicPatternsToggle = {
                        viewModel.onEvent(SettingsEvent.SetShowIslamicPatterns(!generalState.showIslamicPatterns))
                    },
                    onAnimationsToggle = {
                        viewModel.onEvent(SettingsEvent.SetAnimationsEnabled(!generalState.animationsEnabled))
                    }
                )
            }

            // Home Screen Section
            item {
                NimazSectionHeader(title = stringResource(R.string.appearance_home_screen))
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
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// --- Theme Selection ---

@Composable
private fun ThemeSelectionCard(
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit
) {
    NimazMenuGroup {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ThemePreviewOption(
                label = stringResource(R.string.theme_dark),
                isSelected = selectedTheme == AppTheme.DARK,
                onClick = { onThemeSelected(AppTheme.DARK) },
                modifier = Modifier.weight(1f)
            ) {
                // Dark preview
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NimazColors.Neutral950)
                ) {
                    ThemePreviewContent(contentColor = Color.White)
                }
            }
            ThemePreviewOption(
                label = stringResource(R.string.theme_light),
                isSelected = selectedTheme == AppTheme.LIGHT,
                onClick = { onThemeSelected(AppTheme.LIGHT) },
                modifier = Modifier.weight(1f)
            ) {
                // Light preview
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NimazColors.Neutral100)
                ) {
                    ThemePreviewContent(contentColor = NimazColors.Neutral950)
                }
            }
            ThemePreviewOption(
                label = stringResource(R.string.theme_system),
                isSelected = selectedTheme == AppTheme.SYSTEM,
                onClick = { onThemeSelected(AppTheme.SYSTEM) },
                modifier = Modifier.weight(1f)
            ) {
                // System preview: diagonal split
                Box(modifier = Modifier.fillMaxSize()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        drawRect(color = NimazColors.Neutral950)
                        drawPath(
                            path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(w, 0f)
                                lineTo(w, h)
                                lineTo(0f, h)
                                close()
                            },
                            color = NimazColors.Neutral100
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
        NimazCard(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.6f),
            style = NimazCardStyle.OUTLINED,
            selected = isSelected,
            shape = RoundedCornerShape(12.dp),
            colors = NimazCardDefaults.selectable(
                container = Color.Transparent,
                border = MaterialTheme.colorScheme.outlineVariant,
                borderWidth = 1.dp,
                activeContainer = Color.Transparent,
                activeBorder = MaterialTheme.colorScheme.primary,
                activeBorderWidth = 3.dp,
            ),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                previewContent()
                // Selected indicator — display-only circular check.
                if (isSelected) {
                    NimazCheckbox(
                        checked = true,
                        onCheckedChange = null,
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopEnd),
                        variant = NimazCheckboxVariant.PRIMARY,
                        size = NimazCheckboxSize.SMALL,
                        type = NimazCheckboxType.CIRCLE,
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

// --- Display Settings ---

@Composable
private fun DisplaySettingsCard(
    hapticFeedback: Boolean,
    use24HourFormat: Boolean,
    showIslamicPatterns: Boolean,
    animationsEnabled: Boolean,
    onHapticFeedbackToggle: () -> Unit,
    on24HourToggle: () -> Unit,
    onIslamicPatternsToggle: () -> Unit,
    onAnimationsToggle: () -> Unit
) {
    NimazMenuGroup {
        NimazSettingsItem(
            title = stringResource(R.string.appearance_islamic_patterns),
            subtitle = stringResource(R.string.appearance_islamic_patterns_subtitle),
            checked = showIslamicPatterns,
            onCheckedChange = { onIslamicPatternsToggle() }
        )
        NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
        NimazSettingsItem(
            title = stringResource(R.string.appearance_animations),
            subtitle = stringResource(R.string.appearance_animations_subtitle),
            checked = animationsEnabled,
            onCheckedChange = { onAnimationsToggle() }
        )
        NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
        NimazSettingsItem(
            title = stringResource(R.string.appearance_haptic),
            subtitle = stringResource(R.string.appearance_haptic_subtitle),
            checked = hapticFeedback,
            onCheckedChange = { onHapticFeedbackToggle() }
        )
        NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
        NimazSettingsItem(
            title = stringResource(R.string.appearance_24hour),
            subtitle = stringResource(R.string.appearance_24hour_subtitle),
            checked = use24HourFormat,
            onCheckedChange = { on24HourToggle() }
        )
    }
}

// --- Home Screen Settings ---

@Composable
private fun HomeScreenSettingsCard(
    useHijriPrimary: Boolean,
    onHijriPrimaryToggle: () -> Unit
) {
    NimazMenuGroup {
        NimazSettingsItem(
            title = stringResource(R.string.appearance_show_islamic_date),
            subtitle = stringResource(R.string.appearance_show_islamic_date_subtitle),
            checked = useHijriPrimary,
            onCheckedChange = { onHijriPrimaryToggle() }
        )
    }
}
