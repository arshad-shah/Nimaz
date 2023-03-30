package com.arshadshah.nimaz.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

object CustomThemes
{

	//function to get the theme from the name of the theme
	fun getTheme(ThemeName : String , darkTheme : Boolean) : ColorScheme
	{
		return when (ThemeName)
		{
			"Raisin_Black" ->
			{
				if (darkTheme) DarkColorsRaisinBlack else LightColorsRaisinBlack
			}

			"Dark_Red" ->
			{
				if (darkTheme) DarkColorsDarkRed else LightColorsDarkRed
			}

			"Dark_Liver" ->
			{
				if (darkTheme) DarkColorsLiver else LightColorsLiver
			}

			"Rustic_brown" ->
			{
				if (darkTheme) DarkColorsRusticBrown else LightColorsRusticBrown
			}

			else -> if (darkTheme) DarkColorsDefault else LightColorsDefault
		}
	}

	val LightColorsDefault = lightColorScheme(
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


	val DarkColorsDefault = darkColorScheme(
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


	//theme 1
	private val LightColorsRaisinBlack = lightColorScheme(
			primary = raison_black_md_theme_light_primary ,
			onPrimary = raison_black_md_theme_light_onPrimary ,
			primaryContainer = raison_black_md_theme_light_primaryContainer ,
			onPrimaryContainer = raison_black_md_theme_light_onPrimaryContainer ,
			secondary = raison_black_md_theme_light_secondary ,
			onSecondary = raison_black_md_theme_light_onSecondary ,
			secondaryContainer = raison_black_md_theme_light_secondaryContainer ,
			onSecondaryContainer = raison_black_md_theme_light_onSecondaryContainer ,
			tertiary = raison_black_md_theme_light_tertiary ,
			onTertiary = raison_black_md_theme_light_onTertiary ,
			tertiaryContainer = raison_black_md_theme_light_tertiaryContainer ,
			onTertiaryContainer = raison_black_md_theme_light_onTertiaryContainer ,
			error = raison_black_md_theme_light_error ,
			errorContainer = raison_black_md_theme_light_errorContainer ,
			onError = raison_black_md_theme_light_onError ,
			onErrorContainer = raison_black_md_theme_light_onErrorContainer ,
			background = raison_black_md_theme_light_background ,
			onBackground = raison_black_md_theme_light_onBackground ,
			surface = raison_black_md_theme_light_surface ,
			onSurface = raison_black_md_theme_light_onSurface ,
			surfaceVariant = raison_black_md_theme_light_surfaceVariant ,
			onSurfaceVariant = raison_black_md_theme_light_onSurfaceVariant ,
			outline = raison_black_md_theme_light_outline ,
			inverseOnSurface = raison_black_md_theme_light_inverseOnSurface ,
			inverseSurface = raison_black_md_theme_light_inverseSurface ,
			inversePrimary = raison_black_md_theme_light_inversePrimary ,
														 )


	private val DarkColorsRaisinBlack = darkColorScheme(
			primary = raison_black_md_theme_dark_primary ,
			onPrimary = raison_black_md_theme_dark_onPrimary ,
			primaryContainer = raison_black_md_theme_dark_primaryContainer ,
			onPrimaryContainer = raison_black_md_theme_dark_onPrimaryContainer ,
			secondary = raison_black_md_theme_dark_secondary ,
			onSecondary = raison_black_md_theme_dark_onSecondary ,
			secondaryContainer = raison_black_md_theme_dark_secondaryContainer ,
			onSecondaryContainer = raison_black_md_theme_dark_onSecondaryContainer ,
			tertiary = raison_black_md_theme_dark_tertiary ,
			onTertiary = raison_black_md_theme_dark_onTertiary ,
			tertiaryContainer = raison_black_md_theme_dark_tertiaryContainer ,
			onTertiaryContainer = raison_black_md_theme_dark_onTertiaryContainer ,
			error = raison_black_md_theme_dark_error ,
			errorContainer = raison_black_md_theme_dark_errorContainer ,
			onError = raison_black_md_theme_dark_onError ,
			onErrorContainer = raison_black_md_theme_dark_onErrorContainer ,
			background = raison_black_md_theme_dark_background ,
			onBackground = raison_black_md_theme_dark_onBackground ,
			surface = raison_black_md_theme_dark_surface ,
			onSurface = raison_black_md_theme_dark_onSurface ,
			surfaceVariant = raison_black_md_theme_dark_surfaceVariant ,
			onSurfaceVariant = raison_black_md_theme_dark_onSurfaceVariant ,
			outline = raison_black_md_theme_dark_outline ,
			inverseOnSurface = raison_black_md_theme_dark_inverseOnSurface ,
			inverseSurface = raison_black_md_theme_dark_inverseSurface ,
			inversePrimary = raison_black_md_theme_dark_inversePrimary ,
													   )


	//dark red theme
	//each color starts as Dark_Red_md_theme_
	private val LightColorsDarkRed = lightColorScheme(
			primary = Dark_Red_md_theme_light_primary ,
			onPrimary = Dark_Red_md_theme_light_onPrimary ,
			primaryContainer = Dark_Red_md_theme_light_primaryContainer ,
			onPrimaryContainer = Dark_Red_md_theme_light_onPrimaryContainer ,
			secondary = Dark_Red_md_theme_light_secondary ,
			onSecondary = Dark_Red_md_theme_light_onSecondary ,
			secondaryContainer = Dark_Red_md_theme_light_secondaryContainer ,
			onSecondaryContainer = Dark_Red_md_theme_light_onSecondaryContainer ,
			tertiary = Dark_Red_md_theme_light_tertiary ,
			onTertiary = Dark_Red_md_theme_light_onTertiary ,
			tertiaryContainer = Dark_Red_md_theme_light_tertiaryContainer ,
			onTertiaryContainer = Dark_Red_md_theme_light_onTertiaryContainer ,
			error = Dark_Red_md_theme_light_error ,
			errorContainer = Dark_Red_md_theme_light_errorContainer ,
			onError = Dark_Red_md_theme_light_onError ,
			onErrorContainer = Dark_Red_md_theme_light_onErrorContainer ,
			background = Dark_Red_md_theme_light_background ,
			onBackground = Dark_Red_md_theme_light_onBackground ,
			surface = Dark_Red_md_theme_light_surface ,
			onSurface = Dark_Red_md_theme_light_onSurface ,
			surfaceVariant = Dark_Red_md_theme_light_surfaceVariant ,
			onSurfaceVariant = Dark_Red_md_theme_light_onSurfaceVariant ,
			outline = Dark_Red_md_theme_light_outline ,
			inverseOnSurface = Dark_Red_md_theme_light_inverseOnSurface ,
			inverseSurface = Dark_Red_md_theme_light_inverseSurface ,
			inversePrimary = Dark_Red_md_theme_light_inversePrimary ,
													 )

	private val DarkColorsDarkRed = darkColorScheme(
			primary = Dark_Red_md_theme_dark_primary ,
			onPrimary = Dark_Red_md_theme_dark_onPrimary ,
			primaryContainer = Dark_Red_md_theme_dark_primaryContainer ,
			onPrimaryContainer = Dark_Red_md_theme_dark_onPrimaryContainer ,
			secondary = Dark_Red_md_theme_dark_secondary ,
			onSecondary = Dark_Red_md_theme_dark_onSecondary ,
			secondaryContainer = Dark_Red_md_theme_dark_secondaryContainer ,
			onSecondaryContainer = Dark_Red_md_theme_dark_onSecondaryContainer ,
			tertiary = Dark_Red_md_theme_dark_tertiary ,
			onTertiary = Dark_Red_md_theme_dark_onTertiary ,
			tertiaryContainer = Dark_Red_md_theme_dark_tertiaryContainer ,
			onTertiaryContainer = Dark_Red_md_theme_dark_onTertiaryContainer ,
			error = Dark_Red_md_theme_dark_error ,
			errorContainer = Dark_Red_md_theme_dark_errorContainer ,
			onError = Dark_Red_md_theme_dark_onError ,
			onErrorContainer = Dark_Red_md_theme_dark_onErrorContainer ,
			background = Dark_Red_md_theme_dark_background ,
			onBackground = Dark_Red_md_theme_dark_onBackground ,
			surface = Dark_Red_md_theme_dark_surface ,
			onSurface = Dark_Red_md_theme_dark_onSurface ,
			surfaceVariant = Dark_Red_md_theme_dark_surfaceVariant ,
			onSurfaceVariant = Dark_Red_md_theme_dark_onSurfaceVariant ,
			outline = Dark_Red_md_theme_dark_outline ,
			inverseOnSurface = Dark_Red_md_theme_dark_inverseOnSurface ,
			inverseSurface = Dark_Red_md_theme_dark_inverseSurface ,
			inversePrimary = Dark_Red_md_theme_dark_inversePrimary ,
												   )


	//liver_md_theme_
	//each color starts as liver_md_theme_
	private val LightColorsLiver = lightColorScheme(
			primary = liver_md_theme_light_primary ,
			onPrimary = liver_md_theme_light_onPrimary ,
			primaryContainer = liver_md_theme_light_primaryContainer ,
			onPrimaryContainer = liver_md_theme_light_onPrimaryContainer ,
			secondary = liver_md_theme_light_secondary ,
			onSecondary = liver_md_theme_light_onSecondary ,
			secondaryContainer = liver_md_theme_light_secondaryContainer ,
			onSecondaryContainer = liver_md_theme_light_onSecondaryContainer ,
			tertiary = liver_md_theme_light_tertiary ,
			onTertiary = liver_md_theme_light_onTertiary ,
			tertiaryContainer = liver_md_theme_light_tertiaryContainer ,
			onTertiaryContainer = liver_md_theme_light_onTertiaryContainer ,
			error = liver_md_theme_light_error ,
			errorContainer = liver_md_theme_light_errorContainer ,
			onError = liver_md_theme_light_onError ,
			onErrorContainer = liver_md_theme_light_onErrorContainer ,
			background = liver_md_theme_light_background ,
			onBackground = liver_md_theme_light_onBackground ,
			surface = liver_md_theme_light_surface ,
			onSurface = liver_md_theme_light_onSurface ,
			surfaceVariant = liver_md_theme_light_surfaceVariant ,
			onSurfaceVariant = liver_md_theme_light_onSurfaceVariant ,
			outline = liver_md_theme_light_outline ,
			inverseOnSurface = liver_md_theme_light_inverseOnSurface ,
			inverseSurface = liver_md_theme_light_inverseSurface ,
			inversePrimary = liver_md_theme_light_inversePrimary ,
												   )

	private val DarkColorsLiver = darkColorScheme(
			primary = liver_md_theme_dark_primary ,
			onPrimary = liver_md_theme_dark_onPrimary ,
			primaryContainer = liver_md_theme_dark_primaryContainer ,
			onPrimaryContainer = liver_md_theme_dark_onPrimaryContainer ,
			secondary = liver_md_theme_dark_secondary ,
			onSecondary = liver_md_theme_dark_onSecondary ,
			secondaryContainer = liver_md_theme_dark_secondaryContainer ,
			onSecondaryContainer = liver_md_theme_dark_onSecondaryContainer ,
			tertiary = liver_md_theme_dark_tertiary ,
			onTertiary = liver_md_theme_dark_onTertiary ,
			tertiaryContainer = liver_md_theme_dark_tertiaryContainer ,
			onTertiaryContainer = liver_md_theme_dark_onTertiaryContainer ,
			error = liver_md_theme_dark_error ,
			errorContainer = liver_md_theme_dark_errorContainer ,
			onError = liver_md_theme_dark_onError ,
			onErrorContainer = liver_md_theme_dark_onErrorContainer ,
			background = liver_md_theme_dark_background ,
			onBackground = liver_md_theme_dark_onBackground ,
			surface = liver_md_theme_dark_surface ,
			onSurface = liver_md_theme_dark_onSurface ,
			surfaceVariant = liver_md_theme_dark_surfaceVariant ,
			onSurfaceVariant = liver_md_theme_dark_onSurfaceVariant ,
			outline = liver_md_theme_dark_outline ,
			inverseOnSurface = liver_md_theme_dark_inverseOnSurface ,
			inverseSurface = liver_md_theme_dark_inverseSurface ,
			inversePrimary = liver_md_theme_dark_inversePrimary ,
												 )

	//DarkColorsRusticBrown
	//LightColorsRusticBrown
	//rustic_md_theme
	//each color starts as rustic_md_theme_
	private val LightColorsRusticBrown = lightColorScheme(
			primary = rustic_md_theme_light_primary ,
			onPrimary = rustic_md_theme_light_onPrimary ,
			primaryContainer = rustic_md_theme_light_primaryContainer ,
			onPrimaryContainer = rustic_md_theme_light_onPrimaryContainer ,
			secondary = rustic_md_theme_light_secondary ,
			onSecondary = rustic_md_theme_light_onSecondary ,
			secondaryContainer = rustic_md_theme_light_secondaryContainer ,
			onSecondaryContainer = rustic_md_theme_light_onSecondaryContainer ,
			tertiary = rustic_md_theme_light_tertiary ,
			onTertiary = rustic_md_theme_light_onTertiary ,
			tertiaryContainer = rustic_md_theme_light_tertiaryContainer ,
			onTertiaryContainer = rustic_md_theme_light_onTertiaryContainer ,
			error = rustic_md_theme_light_error ,
			errorContainer = rustic_md_theme_light_errorContainer ,
			onError = rustic_md_theme_light_onError ,
			onErrorContainer = rustic_md_theme_light_onErrorContainer ,
			background = rustic_md_theme_light_background ,
			onBackground = rustic_md_theme_light_onBackground ,
			surface = rustic_md_theme_light_surface ,
			onSurface = rustic_md_theme_light_onSurface ,
			surfaceVariant = rustic_md_theme_light_surfaceVariant ,
			onSurfaceVariant = rustic_md_theme_light_onSurfaceVariant ,
			outline = rustic_md_theme_light_outline ,
			inverseOnSurface = rustic_md_theme_light_inverseOnSurface ,
			inverseSurface = rustic_md_theme_light_inverseSurface ,
			inversePrimary = rustic_md_theme_light_inversePrimary ,
														 )

	private val DarkColorsRusticBrown = darkColorScheme(
			primary = rustic_md_theme_dark_primary ,
			onPrimary = rustic_md_theme_dark_onPrimary ,
			primaryContainer = rustic_md_theme_dark_primaryContainer ,
			onPrimaryContainer = rustic_md_theme_dark_onPrimaryContainer ,
			secondary = rustic_md_theme_dark_secondary ,
			onSecondary = rustic_md_theme_dark_onSecondary ,
			secondaryContainer = rustic_md_theme_dark_secondaryContainer ,
			onSecondaryContainer = rustic_md_theme_dark_onSecondaryContainer ,
			tertiary = rustic_md_theme_dark_tertiary ,
			onTertiary = rustic_md_theme_dark_onTertiary ,
			tertiaryContainer = rustic_md_theme_dark_tertiaryContainer ,
			onTertiaryContainer = rustic_md_theme_dark_onTertiaryContainer ,
			error = rustic_md_theme_dark_error ,
			errorContainer = rustic_md_theme_dark_errorContainer ,
			onError = rustic_md_theme_dark_onError ,
			onErrorContainer = rustic_md_theme_dark_onErrorContainer ,
			background = rustic_md_theme_dark_background ,
			onBackground = rustic_md_theme_dark_onBackground ,
			surface = rustic_md_theme_dark_surface ,
			onSurface = rustic_md_theme_dark_onSurface ,
			surfaceVariant = rustic_md_theme_dark_surfaceVariant ,
			onSurfaceVariant = rustic_md_theme_dark_onSurfaceVariant ,
			outline = rustic_md_theme_dark_outline ,
			inverseOnSurface = rustic_md_theme_dark_inverseOnSurface ,
			inverseSurface = rustic_md_theme_dark_inverseSurface ,
			inversePrimary = rustic_md_theme_dark_inversePrimary ,
													   )

}