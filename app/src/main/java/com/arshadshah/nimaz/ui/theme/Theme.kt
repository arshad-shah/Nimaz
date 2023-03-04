package com.arshadshah.nimaz.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat

@Composable
fun ProvideDimens(
	dimensions : Dimensions ,
	content : @Composable () -> Unit ,
				 )
{
	val dimensionSet = remember { dimensions }
	CompositionLocalProvider(LocalAppDimens provides dimensionSet , content = content)
}

private val LocalAppDimens = staticCompositionLocalOf {
	smallDimensions
}

private val LightColors = lightColorScheme(
		primary = md_theme_light_primary ,
		onPrimary = md_theme_light_onPrimary ,
		primaryContainer = md_theme_light_primaryContainer ,
		onPrimaryContainer = md_theme_light_onPrimaryContainer ,
		secondary = md_theme_light_secondary ,
		onSecondary = md_theme_light_onSecondary ,
		secondaryContainer = md_theme_light_secondaryContainer ,
		onSecondaryContainer = md_theme_light_onSecondaryContainer ,
		tertiary = md_theme_light_tertiary ,
		onTertiary = md_theme_light_onTertiary ,
		tertiaryContainer = md_theme_light_tertiaryContainer ,
		onTertiaryContainer = md_theme_light_onTertiaryContainer ,
		error = md_theme_light_error ,
		errorContainer = md_theme_light_errorContainer ,
		onError = md_theme_light_onError ,
		onErrorContainer = md_theme_light_onErrorContainer ,
		background = md_theme_light_background ,
		onBackground = md_theme_light_onBackground ,
		surface = md_theme_light_surface ,
		onSurface = md_theme_light_onSurface ,
		surfaceVariant = md_theme_light_surfaceVariant ,
		onSurfaceVariant = md_theme_light_onSurfaceVariant ,
		outline = md_theme_light_outline ,
		inverseOnSurface = md_theme_light_inverseOnSurface ,
		inverseSurface = md_theme_light_inverseSurface ,
		inversePrimary = md_theme_light_inversePrimary ,
										  )


private val DarkColors = darkColorScheme(
		primary = md_theme_dark_primary ,
		onPrimary = md_theme_dark_onPrimary ,
		primaryContainer = md_theme_dark_primaryContainer ,
		onPrimaryContainer = md_theme_dark_onPrimaryContainer ,
		secondary = md_theme_dark_secondary ,
		onSecondary = md_theme_dark_onSecondary ,
		secondaryContainer = md_theme_dark_secondaryContainer ,
		onSecondaryContainer = md_theme_dark_onSecondaryContainer ,
		tertiary = md_theme_dark_tertiary ,
		onTertiary = md_theme_dark_onTertiary ,
		tertiaryContainer = md_theme_dark_tertiaryContainer ,
		onTertiaryContainer = md_theme_dark_onTertiaryContainer ,
		error = md_theme_dark_error ,
		errorContainer = md_theme_dark_errorContainer ,
		onError = md_theme_dark_onError ,
		onErrorContainer = md_theme_dark_onErrorContainer ,
		background = md_theme_dark_background ,
		onBackground = md_theme_dark_onBackground ,
		surface = md_theme_dark_surface ,
		onSurface = md_theme_dark_onSurface ,
		surfaceVariant = md_theme_dark_surfaceVariant ,
		onSurfaceVariant = md_theme_dark_onSurfaceVariant ,
		outline = md_theme_dark_outline ,
		inverseOnSurface = md_theme_dark_inverseOnSurface ,
		inverseSurface = md_theme_dark_inverseSurface ,
		inversePrimary = md_theme_dark_inversePrimary ,
										)

@Composable
fun NimazTheme(
	darkTheme : Boolean = false ,
	dynamicColor : Boolean = false ,
	content : @Composable () -> Unit ,
			  )
{
	val colorScheme = when
	{
		dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
		{
			val context = LocalContext.current
			if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
		}

		darkTheme -> DarkColors
		else -> LightColors
	}
	val configuration = LocalConfiguration.current
	//if screen is small then use small dimensions
	val dimensions =
		if (configuration.screenWidthDp < 360 && configuration.screenHeightDp < 700) smallDimensions else sw360Dimensions
	val typography =
		if (configuration.screenHeightDp > 700 || configuration.screenWidthDp < 360) TypographyMain else TypographySmall
	val view = LocalView.current
	if (! view.isInEditMode)
	{
		SideEffect {
			(view.context as Activity).window.statusBarColor = colorScheme.surface.toArgb()
			// Set the status bar color to be light or dark, depending on the theme
			ViewCompat.getWindowInsetsController(view)?.isAppearanceLightStatusBars =
				! darkTheme
		}
	}

	ProvideDimens(dimensions) {
		MaterialTheme(
				colorScheme = colorScheme ,
				shapes = nimazCardShapes() ,
				typography = typography ,
				content = content ,
					 )
	}
}