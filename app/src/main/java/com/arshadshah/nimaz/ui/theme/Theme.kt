package com.arshadshah.nimaz.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.arshadshah.nimaz.constants.AppConstants.THEME_DEFAULT
import com.arshadshah.nimaz.constants.AppConstants.THEME_SYSTEM
import com.arshadshah.nimaz.ui.theme.CustomThemes.DarkColorsDefault
import com.arshadshah.nimaz.ui.theme.CustomThemes.LightColorsDefault

@Composable
fun ProvideDimens(
    dimensions: Dimensions,
    content: @Composable () -> Unit,
) {
    val dimensionSet = remember { dimensions }
    CompositionLocalProvider(LocalAppDimens provides dimensionSet, content = content)
}

private val LocalAppDimens = staticCompositionLocalOf {
    smallDimensions
}

@Composable
fun NimazTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    themeName: String = THEME_SYSTEM,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        themeName != THEME_DEFAULT && !dynamicColor ->
            CustomThemes.getTheme(themeName, darkTheme)

        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        else -> if (darkTheme) DarkColorsDefault else LightColorsDefault
    }
    val configuration = LocalConfiguration.current
    //if screen is small then use small dimensions
    val dimensions = if (configuration.screenWidthDp >= 600) sw360Dimensions else smallDimensions
    val typography = if (configuration.screenWidthDp >= 600) TypographyMain else TypographySmall
    ProvideDimens(dimensions) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = nimazCardShapes(),
            typography = typography,
            content = content,
        )
    }
}