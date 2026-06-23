package com.arshadshah.nimaz.presentation.components.atoms

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * Semantic tint role for a [NimazIcon] — mirrors how [NimazButton]'s variant maps
 * onto the Material 3 scheme, so colour tracks theming automatically. Pass an
 * explicit `tint` to [NimazIcon] to escape these roles (brand `NimazColors.*`,
 * per-prayer colours, or a runtime colour).
 */
enum class NimazIconVariant {
    /** Inherits `LocalContentColor` — the faithful drop-in for a bare `Icon`,
     *  and the right choice inside buttons / coloured containers. */
    DEFAULT,

    /** De-emphasised. `onSurfaceVariant`. */
    MUTED,

    /** Brand emphasis. `primary`. */
    PRIMARY,

    /** For icons sitting on a filled `primary` background. `onPrimary`. */
    ON_ACCENT,

    /** Destructive / alert. `error`. */
    ERROR,

    /** Positive / completed. `NimazColors.Success`. */
    SUCCESS
}

/** Whether a [NimazIcon] is a bare glyph or wrapped in a coloured container. */
enum class NimazIconType {
    /** A plain glyph (size + tint) — replaces a raw Material 3 `Icon`. */
    PLAIN,

    /** The glyph centred in a tinted container — replaces the old
     *  `ContainedIcon` / `IconBadge` "chip" patterns. A rounded-square contained
     *  icon (`containerShape = ROUNDED_SQUARE`) is the reusable "badge" look. */
    CONTAINED
}

/**
 * Icon size presets: the glyph size plus the container/padding used by
 * [NimazIconType.CONTAINED]. Override per-call with [NimazIcon]'s `iconSize` /
 * `containerSize` for the odd bespoke dimension.
 */
enum class NimazIconSize(val iconSize: Dp, val containerSize: Dp, val padding: Dp) {
    EXTRA_SMALL(12.dp, 24.dp, 6.dp),
    SMALL(16.dp, 32.dp, 8.dp),
    MEDIUM(20.dp, 40.dp, 10.dp),
    LARGE(24.dp, 48.dp, 12.dp),
    EXTRA_LARGE(32.dp, 64.dp, 16.dp)
}

/** Container shape for a [NimazIconType.CONTAINED] icon. */
enum class NimazIconContainerShape {
    CIRCLE,
    ROUNDED_SQUARE,
    SQUARE
}

@Composable
private fun NimazIconVariant.resolveTint(): Color = when (this) {
    NimazIconVariant.DEFAULT -> LocalContentColor.current
    NimazIconVariant.MUTED -> MaterialTheme.colorScheme.onSurfaceVariant
    NimazIconVariant.PRIMARY -> MaterialTheme.colorScheme.primary
    NimazIconVariant.ON_ACCENT -> MaterialTheme.colorScheme.onPrimary
    NimazIconVariant.ERROR -> MaterialTheme.colorScheme.error
    NimazIconVariant.SUCCESS -> NimazColors.Success
}

/**
 * The app's single icon primitive.
 *
 * Replaces scattered raw Material 3 `Icon(...)` calls (each re-specifying tint and
 * `Modifier.size(...)`) with one variant/size/type-driven entry point, and folds
 * in the former `ContainedIcon` / `IconBadge` "chip" atoms via [NimazIconType].
 *
 * Colour comes from [variant] (a semantic theme role) unless [tint] is given;
 * size comes from the [size] preset unless [iconSize] overrides it. Container
 * parameters apply only when [type] is [NimazIconType.CONTAINED].
 *
 * @param variant semantic tint role; [NimazIconVariant.DEFAULT] inherits
 *   `LocalContentColor`, so it behaves exactly like a bare `Icon`.
 * @param size glyph/container size preset.
 * @param type plain glyph or tinted container.
 * @param tint escape hatch overriding [variant] (brand `NimazColors.*`, per-prayer
 *   or runtime colours).
 * @param containerShape container shape (CONTAINED only); `ROUNDED_SQUARE` is the
 *   reusable "badge".
 * @param containerColor container fill (CONTAINED only); defaults to the resolved
 *   tint at 15% alpha — the soft "chip" look.
 * @param containerSize overrides the [size] preset's container dimension (CONTAINED).
 * @param iconSize overrides the [size] preset's glyph dimension (PLAIN or CONTAINED).
 * @param cornerRadius overrides the `ROUNDED_SQUARE` corner radius (CONTAINED).
 */
@Composable
fun NimazIcon(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    variant: NimazIconVariant = NimazIconVariant.DEFAULT,
    size: NimazIconSize = NimazIconSize.LARGE,
    type: NimazIconType = NimazIconType.PLAIN,
    tint: Color? = null,
    containerShape: NimazIconContainerShape = NimazIconContainerShape.CIRCLE,
    containerColor: Color? = null,
    containerSize: Dp? = null,
    iconSize: Dp? = null,
    cornerRadius: Dp? = null,
) {
    val resolvedTint = tint ?: variant.resolveTint()
    val resolvedIconSize = iconSize ?: size.iconSize

    when (type) {
        NimazIconType.PLAIN -> Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = modifier.size(resolvedIconSize),
            tint = resolvedTint
        )

        NimazIconType.CONTAINED -> {
            val shape: Shape = when (containerShape) {
                NimazIconContainerShape.CIRCLE -> CircleShape
                NimazIconContainerShape.ROUNDED_SQUARE -> RoundedCornerShape(cornerRadius ?: 12.dp)
                NimazIconContainerShape.SQUARE -> RoundedCornerShape(0.dp)
            }
            Box(
                modifier = modifier
                    .size(containerSize ?: size.containerSize)
                    .clip(shape)
                    .background(containerColor ?: resolvedTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(resolvedIconSize),
                    tint = resolvedTint
                )
            }
        }
    }
}


// ==================== PREVIEWS ====================

@Composable
private fun NimazIconVariantsShowcase() {
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NimazIcon(Icons.Default.Star, variant = NimazIconVariant.DEFAULT)
        NimazIcon(Icons.Default.Star, variant = NimazIconVariant.MUTED)
        NimazIcon(Icons.Default.Star, variant = NimazIconVariant.PRIMARY)
        NimazIcon(Icons.Default.CheckCircle, variant = NimazIconVariant.SUCCESS)
        NimazIcon(Icons.Default.Delete, variant = NimazIconVariant.ERROR)
    }
}

@Composable
private fun NimazIconSizesShowcase() {
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NimazIcon(Icons.Default.Star, size = NimazIconSize.EXTRA_SMALL)
        NimazIcon(Icons.Default.Star, size = NimazIconSize.SMALL)
        NimazIcon(Icons.Default.Star, size = NimazIconSize.MEDIUM)
        NimazIcon(Icons.Default.Star, size = NimazIconSize.LARGE)
        NimazIcon(Icons.Default.Star, size = NimazIconSize.EXTRA_LARGE)
    }
}

@Composable
private fun NimazIconContainedShowcase() {
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circle chip (PRIMARY soft fill)
        NimazIcon(
            Icons.Default.Star,
            type = NimazIconType.CONTAINED,
            variant = NimazIconVariant.PRIMARY
        )
        // Rounded-square "badge"
        NimazIcon(
            Icons.Default.Favorite,
            type = NimazIconType.CONTAINED,
            variant = NimazIconVariant.ERROR,
            containerShape = NimazIconContainerShape.ROUNDED_SQUARE
        )
        // Square + granular sizes / explicit tint
        NimazIcon(
            Icons.Default.CheckCircle,
            type = NimazIconType.CONTAINED,
            containerShape = NimazIconContainerShape.SQUARE,
            tint = NimazColors.Success,
            containerSize = 38.dp,
            iconSize = 20.dp
        )
        // Solid container override
        NimazIcon(
            Icons.Default.Star,
            type = NimazIconType.CONTAINED,
            variant = NimazIconVariant.ON_ACCENT,
            containerColor = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun NimazIconShowcase() {
    Column(
        modifier = Modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Variants", style = MaterialTheme.typography.labelMedium)
        NimazIconVariantsShowcase()
        Text("Sizes", style = MaterialTheme.typography.labelMedium)
        NimazIconSizesShowcase()
        Text("Contained / badge", style = MaterialTheme.typography.labelMedium)
        NimazIconContainedShowcase()
    }
}

@Preview(showBackground = true, name = "NimazIcon — Light")
@Composable
private fun NimazIconLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { NimazIconShowcase() }
}

@Preview(
    showBackground = true, name = "NimazIcon — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazIconDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { NimazIconShowcase() }
}
