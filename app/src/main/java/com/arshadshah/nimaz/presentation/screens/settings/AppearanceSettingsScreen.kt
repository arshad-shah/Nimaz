package com.arshadshah.nimaz.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.navigation.ScreenTags
import com.arshadshah.nimaz.presentation.components.atoms.NimazDivider
import com.arshadshah.nimaz.presentation.components.atoms.NimazPatternBackground
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup
import com.arshadshah.nimaz.presentation.components.molecules.NimazNumberStepper
import com.arshadshah.nimaz.presentation.components.molecules.NimazSettingsItem
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.LocalIsDarkTheme
import com.arshadshah.nimaz.presentation.theme.NimazPatternStyle
import com.arshadshah.nimaz.presentation.viewmodel.AppTheme
import com.arshadshah.nimaz.presentation.viewmodel.SettingsEvent
import com.arshadshah.nimaz.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val generalState by viewModel.generalState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    NimazScreenScaffold(
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

            // Background Pattern Section
            item {
                NimazSectionHeader(title = stringResource(R.string.appearance_pattern))
            }
            item {
                PatternStyleCard(
                    selected = generalState.patternStyle,
                    onSelect = { viewModel.onEvent(SettingsEvent.SetPatternStyle(it)) }
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
                    animationsEnabled = generalState.animationsEnabled,
                    onHapticFeedbackToggle = {
                        viewModel.onEvent(SettingsEvent.SetHapticFeedback(!generalState.hapticFeedback))
                    },
                    on24HourToggle = {
                        viewModel.onEvent(SettingsEvent.Set24HourFormat(!generalState.use24HourFormat))
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
                    },
                    hijriDayOffset = generalState.hijriDayOffset,
                    onHijriDayOffsetChange = {
                        viewModel.onEvent(SettingsEvent.SetHijriDayOffset(it))
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

/**
 * Bespoke theme picker: two real "sky" preview cards — **Day** (light) and
 * **Night** (dark) — with a **Follow device** switch below.
 *
 * "System" is deliberately *not* a third card: it is a rule, not a colour, so it
 * reads as a switch that sits above the two concrete looks (the pattern iOS uses).
 * When following the device, both cards dim and the one the device currently
 * resolves to is not shown as an explicit pick. Choosing a card turns following
 * off; the switch turns it back on.
 */
@Composable
private fun ThemeSelectionCard(
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
) {
    val resolvedDark = LocalIsDarkTheme.current
    val followSystem = selectedTheme == AppTheme.SYSTEM
    val chosenDark = when (selectedTheme) {
        AppTheme.DARK -> true
        AppTheme.LIGHT -> false
        AppTheme.SYSTEM -> resolvedDark
    }

    NimazMenuGroup {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ThemePreviewCard(
                    modifier = Modifier.weight(1f),
                    dark = false,
                    label = stringResource(R.string.theme_light),
                    icon = Icons.Filled.WbSunny,
                    selected = !followSystem && !chosenDark,
                    dimmed = followSystem,
                    onClick = { onThemeSelected(AppTheme.LIGHT) },
                )
                ThemePreviewCard(
                    modifier = Modifier.weight(1f),
                    dark = true,
                    label = stringResource(R.string.theme_dark),
                    icon = Icons.Filled.NightlightRound,
                    selected = !followSystem && chosenDark,
                    dimmed = followSystem,
                    onClick = { onThemeSelected(AppTheme.DARK) },
                )
            }
            NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
            NimazSettingsItem(
                title = stringResource(R.string.theme_follow_device),
                subtitle = stringResource(R.string.theme_follow_device_subtitle),
                checked = followSystem,
                onCheckedChange = { on ->
                    onThemeSelected(
                        if (on) AppTheme.SYSTEM
                        else if (resolvedDark) AppTheme.DARK else AppTheme.LIGHT
                    )
                },
            )
        }
    }
}

/** One theme preview: a mini sky over mock content, labelled, gold-ringed when picked. */
@Composable
private fun ThemePreviewCard(
    dark: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    dimmed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (dark) ThemePreviewColors.DarkBg else ThemePreviewColors.LightBg
    val skyTop = if (dark) ThemePreviewColors.NightSky else ThemePreviewColors.DaySky
    val celestial = if (dark) ThemePreviewColors.Moon else ThemePreviewColors.Sun
    val line = if (dark) ThemePreviewColors.DarkLine else ThemePreviewColors.LightLine
    val cardShape = RoundedCornerShape(16.dp)
    val border = when {
        selected -> ThemePreviewColors.Gold
        dark -> ThemePreviewColors.DarkBorder
        else -> ThemePreviewColors.LightBorder
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .semantics { role = Role.RadioButton },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.15f)
                .clip(cardShape)
                .border(if (selected) 2.dp else 1.dp, border, cardShape)
                .background(bg)
                .alpha(if (dimmed) 0.55f else 1f),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.52f)
                        .background(Brush.verticalGradient(listOf(skyTop, bg))),
                    contentAlignment = Alignment.TopEnd,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = celestial,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(22.dp),
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.48f)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ThemePreviewBar(fraction = 0.5f, color = ThemePreviewColors.Teal, height = 7.dp)
                    ThemePreviewBar(fraction = 0.85f, color = line, height = 6.dp)
                    ThemePreviewBar(fraction = 0.65f, color = line, height = 6.dp)
                }
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(ThemePreviewColors.Gold),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = ThemePreviewColors.OnGold,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val labelColor = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            Icon(
                icon,
                contentDescription = null,
                tint = labelColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = labelColor,
            )
        }
    }
}

@Composable
private fun ThemePreviewBar(fraction: Float, color: Color, height: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth(fraction)
            .height(height)
            .clip(RoundedCornerShape(3.dp))
            .background(color),
    )
}

/**
 * Literal colours for the theme preview cards. Both the light and dark card are
 * shown at once, so MaterialTheme (a single active scheme) cannot supply them —
 * these mirror the real light/dark palette, the same way the onboarding art holds
 * its own tokens.
 */
private object ThemePreviewColors {
    val LightBg = Color(0xFFFAF7F2)
    val DarkBg = Color(0xFF1C1917)
    val DaySky = Color(0xFFFDE8C4)
    val NightSky = Color(0xFF0B1220)
    val Sun = Color(0xFFE8A317)
    val Moon = Color(0xFFF5C84B)
    val Teal = Color(0xFF14B8A6)
    val LightLine = Color(0xFFE2DBCF)
    val DarkLine = Color(0xFF3A3430)
    val LightBorder = Color(0xFFDDD8CF)
    val DarkBorder = Color(0xFF3A3430)
    val Gold = Color(0xFFEAB308)
    val OnGold = Color(0xFF3A2C00)
}

// --- Display Settings ---

@Composable
private fun DisplaySettingsCard(
    hapticFeedback: Boolean,
    use24HourFormat: Boolean,
    animationsEnabled: Boolean,
    onHapticFeedbackToggle: () -> Unit,
    on24HourToggle: () -> Unit,
    onAnimationsToggle: () -> Unit
) {
    NimazMenuGroup {
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

// --- Background Pattern ---

/** The user-facing label for each ornament style. */
private fun NimazPatternStyle.labelRes(): Int = when (this) {
    NimazPatternStyle.NONE -> R.string.pattern_none
    NimazPatternStyle.CORNER_MEDALLION -> R.string.pattern_medallion
    NimazPatternStyle.LATTICE -> R.string.pattern_lattice
    NimazPatternStyle.STAR_FIELD -> R.string.pattern_star_field
    NimazPatternStyle.ATELIER -> R.string.pattern_atelier
}

/**
 * The ornament picker: a horizontally-scrolling row of live swatches. "None" is the
 * first swatch, so this row is also the on/off control — there is no separate
 * toggle. Selecting a swatch updates the app-wide background immediately, and the
 * Appearance screen itself (shown through the transparent scaffold) is the live
 * preview.
 *
 * Swatches render the real geometry via [NimazPatternBackground] with a raised
 * [alphaScale] so the styles are distinguishable at thumbnail size; the applied
 * ornament stays subtle.
 */
@Composable
private fun PatternStyleCard(
    selected: NimazPatternStyle,
    onSelect: (NimazPatternStyle) -> Unit,
) {
    NimazMenuGroup {
        Column(modifier = Modifier.padding(vertical = 14.dp)) {
            Text(
                text = stringResource(R.string.appearance_pattern_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(NimazPatternStyle.entries, key = { it.name }) { style ->
                    PatternSwatch(
                        style = style,
                        label = stringResource(style.labelRes()),
                        selected = style == selected,
                        onClick = { onSelect(style) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PatternSwatch(
    style: NimazPatternStyle,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val swatchShape = RoundedCornerShape(12.dp)
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    Column(
        modifier = Modifier
            .width(96.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .semantics { role = Role.RadioButton },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(swatchShape)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = borderColor,
                    shape = swatchShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            NimazPatternBackground(
                modifier = Modifier.fillMaxSize(),
                style = style,
                enabled = style != NimazPatternStyle.NONE,
                surface = MaterialTheme.colorScheme.surface,
                alphaScale = 6f,
            ) {
                if (style == NimazPatternStyle.NONE) {
                    // An empty swatch reads as broken; a soft dash says "no ornament".
                    Text(
                        text = "—",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}

// --- Home Screen Settings ---

@Composable
private fun HomeScreenSettingsCard(
    useHijriPrimary: Boolean,
    onHijriPrimaryToggle: () -> Unit,
    hijriDayOffset: Int,
    onHijriDayOffsetChange: (Int) -> Unit
) {
    NimazMenuGroup {
        NimazSettingsItem(
            title = stringResource(R.string.appearance_show_islamic_date),
            subtitle = stringResource(R.string.appearance_show_islamic_date_subtitle),
            checked = useHijriPrimary,
            onCheckedChange = { onHijriPrimaryToggle() }
        )
        NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
        NimazNumberStepper(
            label = stringResource(R.string.hijri_day_offset_label),
            value = hijriDayOffset,
            onValueChange = onHijriDayOffsetChange,
            minValue = -2,
            maxValue = 2,
        )
    }
}
