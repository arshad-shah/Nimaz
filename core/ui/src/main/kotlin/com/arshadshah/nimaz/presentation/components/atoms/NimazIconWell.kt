package com.arshadshah.nimaz.presentation.components.atoms

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * Size presets for a [NimazIconWell].
 *
 * - [SMALL] (32dp) — in sheets, dialogs and compact contexts.
 * - [STANDARD] (40dp) — in rows and cards; the overwhelming majority of uses.
 * - [LARGE] (48dp) — in empty states and hero positions.
 *
 * Deprecated aliases [MEDIUM] → [STANDARD] remain for source-compatibility.
 * [XSMALL] and [XLARGE] are deleted — they had zero call sites.
 */
enum class NimazIconWellSize(val container: Dp, val icon: Dp) {
    SMALL(32.dp, 16.dp),
    STANDARD(40.dp, 20.dp),
    LARGE(48.dp, 24.dp),

    /** @suppress Use [STANDARD]. */
    @Deprecated("Use STANDARD", ReplaceWith("STANDARD"))
    MEDIUM(40.dp, 20.dp),
}

/**
 * @suppress Shape is always rounded at [container / 3]. This enum is removed.
 */
@Deprecated("NimazIconWell shape is always rounded. Remove the shape parameter.")
enum class NimazIconWellShape { CIRCLE, ROUNDED }

/**
 * A tinted container holding a single icon — the small coloured squircle that sits
 * at the leading edge of list rows, settings entries, quick actions and empty states.
 *
 * **API:** pass the raw hue as [color]; the well applies [ACCENT_WELL_ALPHA] (14%)
 * automatically for the container and uses the hue at full strength for the icon.
 * The shape is always `RoundedCornerShape(container / 3)` — no shape override.
 *
 * ```kotlin
 * NimazIconWell(icon = Icons.Default.Mosque, color = MaterialTheme.colorScheme.primary)
 * NimazIconWell(icon = Icons.Default.Star, color = NimazColors.Gold500, size = NimazIconWellSize.LARGE)
 * ```
 *
 * @param color the raw hue for the icon and (at 14%) the container.
 * @param contentDescription describes the icon for accessibility. Leave null when
 *   the well is purely decorative and its meaning is already carried by adjacent text.
 */
@Composable
fun NimazIconWell(
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    size: NimazIconWellSize = NimazIconWellSize.STANDARD,
    contentDescription: String? = null,
) {
    val shape = RoundedCornerShape(size.container / 3)
    Box(
        modifier = modifier
            .size(size.container)
            .clip(shape)
            .background(color.copy(alpha = ACCENT_WELL_ALPHA)),
        contentAlignment = Alignment.Center,
    ) {
        NimazIcon(
            imageVector = icon,
            contentDescription = contentDescription,
            iconSize = size.icon,
            tint = color,
        )
    }
}

/** Fill alpha for the [NimazIconWell] container. Always 14%. */
const val ACCENT_WELL_ALPHA = 0.14f

// ── Previews ─────────────────────────────────────────────────────────────────

@Composable
private fun NimazIconWellShowcase() {
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NimazIconWell(icon = Icons.Default.Bookmark, color = MaterialTheme.colorScheme.primary, size = NimazIconWellSize.SMALL)
        NimazIconWell(icon = Icons.Default.Bookmark, color = MaterialTheme.colorScheme.primary)
        NimazIconWell(icon = Icons.Default.Bookmark, color = MaterialTheme.colorScheme.primary, size = NimazIconWellSize.LARGE)
        NimazIconWell(icon = Icons.Default.Notifications, color = MaterialTheme.colorScheme.secondary)
        NimazIconWell(icon = Icons.Default.Favorite, color = MaterialTheme.colorScheme.error)
        NimazIconWell(icon = Icons.Default.Favorite, color = MaterialTheme.colorScheme.tertiary)
    }
}

@Preview(showBackground = true, name = "Icon Well — Light")
@Composable
private fun NimazIconWellLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { NimazIconWellShowcase() }
}

@Preview(
    showBackground = true, name = "Icon Well — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazIconWellDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { NimazIconWellShowcase() }
}
