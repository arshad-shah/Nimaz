package com.arshadshah.nimaz.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import com.arshadshah.nimaz.ui.theme.CustomThemes.DarkColorsDefault
import com.arshadshah.nimaz.ui.theme.CustomThemes.LightColorsDefault

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

@Composable
fun NimazTheme(
	darkTheme : Boolean = false ,
	dynamicColor : Boolean = false ,
	ThemeName : String = "Default" ,
	content : @Composable () -> Unit ,
			  )
{
	val colorScheme = when
	{
		//if the name of the theme is not default then use the custom theme
		ThemeName != "Default" && ! dynamicColor ->
			CustomThemes.getTheme(ThemeName , darkTheme)

		dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
		{
			val context = LocalContext.current
			if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
		}

		else -> if (darkTheme) DarkColorsDefault else LightColorsDefault
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