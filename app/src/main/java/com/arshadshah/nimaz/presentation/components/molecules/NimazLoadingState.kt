package com.arshadshah.nimaz.presentation.components.molecules

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * How a [NimazLoadingState] occupies space.
 *
 * - [FULLSCREEN] — fills the available space and centres. For a screen body that
 *   has nothing to show yet.
 * - [SECTION] — fills width with generous vertical padding, for one section of a
 *   screen loading while the rest is already drawn.
 * - [INLINE] — a small spinner on a text baseline, for a row or a control that is
 *   working. Does not claim vertical space.
 */
enum class NimazLoadingVariant {
    FULLSCREEN,
    SECTION,
    INLINE
}

/**
 * The app's spinner.
 *
 * **Prefer [com.arshadshah.nimaz.presentation.components.atoms.NimazSkeleton] when the shape of the incoming content is known** — a
 * skeleton holds the layout still and reads as "this content is arriving", where
 * a spinner reads as "the app is busy" and lets the screen jump when data lands.
 * Reach for this when the result's shape is unknown, the wait is very short, or a
 * control is momentarily working.
 *
 * @param message optional label beneath the spinner. Also becomes the spoken
 *   description, so a screen reader announces what is loading rather than an
 *   unlabelled progress bar.
 * @param variant how much space the loader occupies; see [NimazLoadingVariant].
 */
@Composable
fun NimazLoadingState(
    modifier: Modifier = Modifier,
    variant: NimazLoadingVariant = NimazLoadingVariant.FULLSCREEN,
    message: String? = null,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val semanticsModifier = if (message != null) {
        Modifier.semantics { contentDescription = message }
    } else {
        Modifier
    }

    when (variant) {
        NimazLoadingVariant.INLINE -> Row(
            modifier = modifier.then(semanticsModifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(INLINE_SPINNER_SIZE),
                strokeWidth = INLINE_SPINNER_STROKE,
                color = color
            )
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        NimazLoadingVariant.FULLSCREEN, NimazLoadingVariant.SECTION -> Box(
            modifier = modifier
                .then(
                    if (variant == NimazLoadingVariant.FULLSCREEN) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = SECTION_VERTICAL_PADDING)
                    }
                )
                .then(semanticsModifier),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(color = color)
                if (message != null) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private val INLINE_SPINNER_SIZE = 16.dp
private val INLINE_SPINNER_STROKE = 2.dp
private val SECTION_VERTICAL_PADDING = 32.dp

// ==================== PREVIEWS ====================

@Composable
private fun NimazLoadingStateShowcase() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        NimazLoadingState(variant = NimazLoadingVariant.SECTION)
        NimazLoadingState(
            variant = NimazLoadingVariant.SECTION,
            message = "Loading surahs…"
        )
        NimazLoadingState(
            variant = NimazLoadingVariant.INLINE,
            message = "Checking location…"
        )
    }
}

@Preview(showBackground = true, name = "Loading — Light")
@Composable
private fun NimazLoadingStateLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        NimazLoadingStateShowcase()
    }
}

@Preview(
    showBackground = true, name = "Loading — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazLoadingStateDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        NimazLoadingStateShowcase()
    }
}
