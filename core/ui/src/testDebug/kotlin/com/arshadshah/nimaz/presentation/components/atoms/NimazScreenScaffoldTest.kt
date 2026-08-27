package com.arshadshah.nimaz.presentation.components.atoms

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.testing.brightness
import com.arshadshah.nimaz.testing.drawToBitmap
import com.arshadshah.nimaz.testing.region
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The sanctioned screen root, and the one thing it exists to change.
 *
 * A bare `Scaffold` paints `colorScheme.background` opaquely, which covers the app-wide ornament
 * `MainActivity` draws underneath every screen. `NimazScreenScaffold` defaults its container to
 * **transparent** so the pattern shows through, and that single default is the whole component —
 * a "tidy up the defaults" change that restored Material's is invisible in code review and removes
 * the ornament from every screen at once.
 *
 * So this is a pixel test: something drawn *behind* the scaffold has to still be visible in front
 * of it. Passing a colour explicitly is the documented escape hatch for screens that own their own
 * backdrop, and it has to keep working — including the `contentColorFor` branch that picks the ink
 * to go with it.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-mdpi")
class NimazScreenScaffoldTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `the default container is transparent, so what is drawn behind it survives`() {
        // A magenta field behind two scaffolds: the default one lets it through, the one given an
        // explicit black container covers it. If the default ever went back to Material's, both
        // halves would read the same and every screen would lose the app's ornament.
        val bitmap = composeRule.drawToBitmap {
            androidx.compose.foundation.layout.Column {
                Box(Modifier.size(200.dp).background(Color.Magenta)) {
                    NimazScreenScaffold(modifier = Modifier.fillMaxSize()) {}
                }
                Box(Modifier.size(200.dp).background(Color.Magenta)) {
                    NimazScreenScaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Black,
                    ) {}
                }
            }
        }

        val seeThrough = bitmap.region(20, 20, 100, 100).brightness()
        val painted = bitmap.region(20, 220, 100, 100).brightness()

        assertThat(seeThrough).isGreaterThan(painted)
    }

    @Test
    fun `every slot the scaffold offers is rendered`() {
        composeRule.setContent {
            MaterialTheme {
                NimazScreenScaffold(
                    topBar = { Text("top") },
                    bottomBar = { Text("bottom") },
                    snackbarHost = { SnackbarHost(SnackbarHostState()) },
                    floatingActionButton = { Text("fab") },
                    floatingActionButtonPosition = FabPosition.Center,
                ) { padding ->
                    Box(Modifier.padding(padding)) { Text("body") }
                }
            }
        }

        listOf("top", "bottom", "fab", "body").forEach {
            composeRule.onNodeWithText(it).assertExists()
        }
    }

    @Test
    fun `a screen that owns its backdrop can say so`() {
        // The escape hatch, and the `contentColorFor` branch that comes with it — an explicit
        // container has to bring its own ink, or a dark backdrop gets dark text.
        composeRule.setContent {
            MaterialTheme {
                NimazScreenScaffold(containerColor = Color.Black) { Text("owned") }
            }
        }

        composeRule.onNodeWithText("owned").assertExists()
    }
}
