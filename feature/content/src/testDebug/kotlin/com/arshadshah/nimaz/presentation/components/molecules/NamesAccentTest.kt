package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NamesAccentTest {
    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `data class holds provided colors`() {
        val accent = NamesAccent(
            rail = Color.Red,
            medallion = listOf(Color.Green, Color.Blue),
            onMedallion = Color.White,
            contentTint = Color.Black,
            chipContainer = Color.Gray,
            onChipContainer = Color.Yellow,
        )
        assertThat(accent.rail).isEqualTo(Color.Red)
        assertThat(accent.medallion).hasSize(2)
        assertThat(accent.onMedallion).isEqualTo(Color.White)
    }

    @Test
    fun `accent factories produce two-stop medallions`() {
        var allahStops = 0
        var prophetNamesStops = 0
        var prophetsStops = 0
        composeRule.setThemedContent {
            CaptureMedallions { allah, prophetNames, prophets ->
                allahStops = allah
                prophetNamesStops = prophetNames
                prophetsStops = prophets
            }
        }
        composeRule.waitForIdle()
        assertThat(allahStops).isEqualTo(2)
        assertThat(prophetNamesStops).isEqualTo(2)
        assertThat(prophetsStops).isEqualTo(2)
    }
}

@Composable
private fun CaptureMedallions(onCapture: (Int, Int, Int) -> Unit) {
    // Touch MaterialTheme so the composables resolve their scheme-based colours.
    MaterialTheme.colorScheme
    onCapture(
        NamesAccents.allah().medallion.size,
        NamesAccents.prophetNames().medallion.size,
        NamesAccents.prophets().medallion.size,
    )
}
