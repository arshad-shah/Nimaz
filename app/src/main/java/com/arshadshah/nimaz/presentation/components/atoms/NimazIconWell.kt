package com.arshadshah.nimaz.presentation.components.atoms

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * Size presets for a [NimazIconWell] — the container size and the glyph inside it.
 */
enum class NimazIconWellSize(val container: Dp, val icon: Dp) {
    XSMALL(24.dp, 13.dp),
    SMALL(32.dp, 16.dp),
    MEDIUM(40.dp, 20.dp),
    LARGE(48.dp, 24.dp),
    XLARGE(56.dp, 28.dp)
}

/**
 * Silhouette of an icon well: a circle, or a squircle with soft corners.
 */
enum class NimazIconWellShape {
    CIRCLE,
    ROUNDED
}

/**
 * A tinted container holding a single icon — the small coloured disc or squircle
 * that sits at the leading edge of list rows, settings entries, quick actions and
 * empty states.
 *
 * This exists because the pattern was hand-rolled in roughly fifteen places, each
 * repeating `Box(Modifier.size(n).clip(shape).background(colour), Center) { Icon }`
 * with its own size, alpha and tint. It is deliberately **not** a [NimazCard]: a
 * well is a fixed-size decorative holder for a glyph, not a content surface, so
 * the card separation rule (elevate on a page, outline when nested) does not apply.
 *
 * Colour comes from a [NimazTone] by default. Pass [accent] for Islamic feature
 * art — a per-prayer colour, a status colour — the same escape hatch
 * [NimazButton] and [NimazBadgeDefaults.feature] provide.
 *
 * @param tone semantic colour of the well; ignored when [accent] is set.
 * @param containerSize overrides [size]'s container dimension. An escape hatch for
 *   a well whose size is genuinely load-bearing — one sitting in a fixed grid cell,
 *   or overlapping other art. Reach for a [NimazIconWellSize] first: the presets
 *   exist so wells stay consistent, and an override is how that erodes.
 * @param iconSize overrides [size]'s glyph dimension; defaults to proportional.
 * @param accent feature-art colour override; the container becomes a soft tint of
 *   it and the glyph takes it at full strength.
 * @param contentDescription describes the icon for accessibility. Leave null when
 *   the well is purely decorative and its meaning is already carried by adjacent
 *   text — which is the common case in a list row.
 */
@Composable
fun NimazIconWell(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tone: NimazTone = NimazTone.ACCENT,
    accent: Color? = null,
    size: NimazIconWellSize = NimazIconWellSize.MEDIUM,
    shape: NimazIconWellShape = NimazIconWellShape.CIRCLE,
    contentDescription: String? = null,
    containerSize: Dp? = null,
    iconSize: Dp? = null,
) {
    val container = containerSize ?: size.container
    val glyphSize = iconSize ?: containerSize?.let { it / 2 } ?: size.icon
    val resolvedShape: Shape = when (shape) {
        NimazIconWellShape.CIRCLE -> CircleShape
        NimazIconWellShape.ROUNDED -> RoundedCornerShape(container / 3)
    }
    val colors = NimazBadgeDefaults.colors(tone = tone, emphasis = NimazBadgeEmphasis.SOFT)
    val containerColor = accent?.copy(alpha = ACCENT_WELL_ALPHA) ?: colors.containerColor
    val glyph = accent ?: colors.contentColor

    Box(
        modifier = modifier
            .size(container)
            .clip(resolvedShape)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        NimazIcon(
            imageVector = icon,
            contentDescription = contentDescription,
            iconSize = glyphSize,
            tint = glyph
        )
    }
}

/**
 * Tint strength for an [NimazIconWell] container built from a feature-art colour,
 * which has no `onXxxContainer` counterpart to pair with.
 */
private const val ACCENT_WELL_ALPHA = 0.14f

// ==================== PREVIEWS ====================

@Composable
private fun NimazIconWellShowcase() {
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NimazIconWellSize.entries.forEach { size ->
            NimazIconWell(icon = Icons.Default.Bookmark, size = size)
        }
        NimazIconWell(
            icon = Icons.Default.Notifications,
            tone = NimazTone.WARNING,
            shape = NimazIconWellShape.ROUNDED
        )
        NimazIconWell(icon = Icons.Default.Favorite, tone = NimazTone.ERROR)
        NimazIconWell(
            icon = Icons.Default.Favorite,
            accent = MaterialTheme.colorScheme.tertiary
        )
    }
}

@Preview(showBackground = true, name = "Icon Well — Light")
@Composable
private fun NimazIconWellLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        NimazIconWellShowcase()
    }
}

@Preview(
    showBackground = true, name = "Icon Well — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazIconWellDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        NimazIconWellShowcase()
    }
}
