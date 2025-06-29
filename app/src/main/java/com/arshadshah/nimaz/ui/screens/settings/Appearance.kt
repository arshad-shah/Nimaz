package com.arshadshah.nimaz.ui.screens.settings

import android.content.Context
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants.THEME_DARK_RED
import com.arshadshah.nimaz.constants.AppConstants.THEME_DEFAULT
import com.arshadshah.nimaz.constants.AppConstants.THEME_RAISIN_BLACK
import com.arshadshah.nimaz.constants.AppConstants.THEME_RUSTIC_BROWN
import com.arshadshah.nimaz.constants.AppConstants.THEME_SYSTEM
import com.arshadshah.nimaz.ui.components.common.HeaderWithIcon
import com.arshadshah.nimaz.ui.components.settings.EnhancedThemeSelector
import com.arshadshah.nimaz.ui.components.settings.ThemeOption
import com.arshadshah.nimaz.ui.theme.Dark_Red_md_theme_dark_onPrimary
import com.arshadshah.nimaz.ui.theme.Dark_Red_md_theme_dark_primary
import com.arshadshah.nimaz.ui.theme.Dark_Red_md_theme_light_onPrimary
import com.arshadshah.nimaz.ui.theme.Dark_Red_md_theme_light_primary
import com.arshadshah.nimaz.ui.theme.md_theme_dark_onPrimary
import com.arshadshah.nimaz.ui.theme.md_theme_dark_primary
import com.arshadshah.nimaz.ui.theme.md_theme_light_onPrimary
import com.arshadshah.nimaz.ui.theme.md_theme_light_primary
import com.arshadshah.nimaz.ui.theme.raison_black_md_theme_dark_onPrimary
import com.arshadshah.nimaz.ui.theme.raison_black_md_theme_dark_primary
import com.arshadshah.nimaz.ui.theme.raison_black_md_theme_light_onPrimary
import com.arshadshah.nimaz.ui.theme.raison_black_md_theme_light_primary
import com.arshadshah.nimaz.ui.theme.rustic_md_theme_dark_onPrimary
import com.arshadshah.nimaz.ui.theme.rustic_md_theme_dark_primary
import com.arshadshah.nimaz.ui.theme.rustic_md_theme_light_onPrimary
import com.arshadshah.nimaz.ui.theme.rustic_md_theme_light_primary
import com.arshadshah.nimaz.viewModel.SettingsViewModel

@Composable
fun getThemeOptions(
    context: Context,
    isDarkMode: Boolean,
    themeState: String
): List<ThemeOption> {
    return listOf(
        ThemeOption(
            themeName = "Forest Green",
            themeKey = THEME_DEFAULT,
            themeColor = if (isDarkMode) md_theme_dark_primary else md_theme_light_primary,
            themeTextColor = if (isDarkMode) md_theme_dark_onPrimary else md_theme_light_onPrimary,
            isSelected = themeState == THEME_DEFAULT,
            description = "A calming forest green theme inspired by nature's tranquility"
        ),
        ThemeOption(
            themeName = "Raisin Black",
            themeKey = THEME_RAISIN_BLACK,
            themeColor = if (isDarkMode)
                raison_black_md_theme_light_primary
            else
                raison_black_md_theme_dark_primary,
            themeTextColor = if (isDarkMode)
                raison_black_md_theme_light_onPrimary
            else
                raison_black_md_theme_dark_onPrimary,
            isSelected = themeState == THEME_RAISIN_BLACK,
            description = "An elegant darker theme with sophisticated raisin black tones"
        ),
        ThemeOption(
            themeName = "Burgundy",
            themeKey = THEME_DARK_RED,
            themeColor = if (isDarkMode)
                Dark_Red_md_theme_dark_primary
            else
                Dark_Red_md_theme_light_primary,
            themeTextColor = if (isDarkMode)
                Dark_Red_md_theme_dark_onPrimary
            else
                Dark_Red_md_theme_light_onPrimary,
            isSelected = themeState == THEME_DARK_RED,
            description = "Rich burgundy tones for a classic and timeless appearance"
        ),
        ThemeOption(
            themeName = "Rustic Brown",
            themeKey = THEME_RUSTIC_BROWN,
            themeColor = if (isDarkMode)
                rustic_md_theme_dark_primary
            else
                rustic_md_theme_light_primary,
            themeTextColor = if (isDarkMode)
                rustic_md_theme_dark_onPrimary
            else
                rustic_md_theme_light_onPrimary,
            isSelected = themeState == THEME_RUSTIC_BROWN,
            description = "Warm rustic brown hues reminiscent of natural earth tones"
        ),
        ThemeOption(
            themeName = "System",
            themeKey = THEME_SYSTEM,
            themeColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (isDarkMode)
                    dynamicDarkColorScheme(context).primary
                else
                    dynamicLightColorScheme(context).primary
            } else {
                if (isDarkMode)
                    md_theme_dark_primary
                else
                    md_theme_light_primary
                md_theme_light_primary
            },
            themeTextColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (isDarkMode)
                    dynamicDarkColorScheme(context).onPrimary
                else
                    dynamicLightColorScheme(context).onPrimary
            } else {
                if (isDarkMode)
                    md_theme_dark_onPrimary
                else
                    md_theme_light_onPrimary
                md_theme_light_onPrimary
            },
            isSelected = themeState == THEME_SYSTEM,
            description = "Automatically matches your system's theme preferences"
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Appearance(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {


    val context = LocalContext.current
    val themeName by viewModel.themeName.collectAsState(initial = THEME_SYSTEM)
    val isDarkMode by viewModel.isDarkMode.collectAsState(initial = false)


    // Scroll behavior for collapsing toolbar
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Appearance",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    OutlinedIconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.shadow(4.dp)
            )
        }
    ) { paddingValues ->

        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(paddingValues)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                // Dark Mode Card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(56.dp)
                            ) {
                                AnimatedContent(
                                    targetState = isDarkMode,
                                    label = "theme_icon",
                                    transitionSpec = {
                                        fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
                                    }
                                ) { dark ->
                                    Icon(
                                        painter = painterResource(
                                            id = if (dark) R.drawable.dark_icon else R.drawable.light_icon
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .padding(12.dp)
                                            .fillMaxSize(),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }

                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (isDarkMode) "Dark Mode" else "Light Mode",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isDarkMode) "Optimized for low light" else "Classic bright theme",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = {
                                viewModel.handleEvent(
                                    SettingsViewModel.SettingsEvent.DarkMode(
                                        it
                                    )
                                )
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            }

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                HeaderWithIcon(
                    modifier = Modifier.padding(8.dp),
                    title = "Choose your theme",
                    contentDescription = "Choose your theme",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    icon = Icons.Default.ColorLens
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Theme Grid
                EnhancedThemeSelector(
                    themeOptions = getThemeOptions(
                        context = context,
                        isDarkMode = isDarkMode,
                        themeState = themeName
                    ),
                    onThemeOptionSelected = { theme ->
                        viewModel.handleEvent(SettingsViewModel.SettingsEvent.Theme(theme.themeKey))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}