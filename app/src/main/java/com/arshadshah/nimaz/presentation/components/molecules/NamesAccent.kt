package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.arshadshah.nimaz.presentation.theme.NimazColors

/**
 * Per-screen accent for the three names screens. Built from the active
 * colour-scheme (so light/dark both work) plus fixed brand medallion gradients.
 */
data class NamesAccent(
    val rail: Color,
    val medallion: List<Color>,
    val onMedallion: Color,
    val contentTint: Color,
    val chipContainer: Color,
    val onChipContainer: Color,
)

object NamesAccents {
    @Composable
    fun allah(): NamesAccent = NamesAccent(
        rail = MaterialTheme.colorScheme.primary,
        medallion = listOf(NimazColors.Primary400, NimazColors.Primary600),
        onMedallion = Color.White,
        contentTint = MaterialTheme.colorScheme.primary,
        chipContainer = MaterialTheme.colorScheme.primaryContainer,
        onChipContainer = MaterialTheme.colorScheme.onPrimaryContainer,
    )

    @Composable
    fun prophetNames(): NamesAccent = NamesAccent(
        rail = MaterialTheme.colorScheme.tertiary,
        medallion = listOf(Color(0xFF9575FF), NimazColors.Tertiary),
        onMedallion = Color.White,
        contentTint = MaterialTheme.colorScheme.tertiary,
        chipContainer = MaterialTheme.colorScheme.tertiaryContainer,
        onChipContainer = MaterialTheme.colorScheme.onTertiaryContainer,
    )

    @Composable
    fun prophets(): NamesAccent = NamesAccent(
        rail = MaterialTheme.colorScheme.secondary,
        medallion = listOf(NimazColors.Gold400, NimazColors.Gold500),
        onMedallion = Color(0xFF1C1917),
        contentTint = MaterialTheme.colorScheme.secondary,
        chipContainer = MaterialTheme.colorScheme.secondaryContainer,
        onChipContainer = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}
