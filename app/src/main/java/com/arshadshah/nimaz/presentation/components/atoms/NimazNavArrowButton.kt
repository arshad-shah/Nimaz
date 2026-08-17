package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcons
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * Which way a [NimazNavArrowButton] points. This is a *visual* direction — it only
 * decides which chevron is drawn (auto-mirrored for RTL layouts). Wire [onClick] to
 * whatever "previous/next" means for the content: usually PREVIOUS→back, NEXT→forward,
 * but right-to-left surfaces (e.g. the Quran Mushaf) may deliberately wire the
 * left-pointing arrow to advance.
 */
enum class NavArrowDirection { PREVIOUS, NEXT }

/**
 * The single, standard prev/next navigation button used across the app (readers, the
 * Quran Mushaf page bar, and the month steppers/calendar header). A circular bordered
 * chevron that tints [MaterialTheme.colorScheme.primary] when enabled and dims to
 * `outlineVariant` at the ends of a range.
 *
 * Replaces the previously duplicated private `NavChevron` / `PageNavChevron` chevrons
 * and the ad-hoc `IconButton` / `FilledTonalIconButton` steppers. See GitHub issue #227.
 */
@Composable
fun NimazNavArrowButton(
    direction: NavArrowDirection,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 48.dp
) {
    val icon = when (direction) {
        NavArrowDirection.PREVIOUS -> NimazIcons.Previous
        NavArrowDirection.NEXT -> NimazIcons.Next
    }
    val tint = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            NimazIcon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                size = NimazIconSize.LARGE
            )
        }
    }
}

// ==================== PREVIEWS ====================

@Composable
private fun NimazNavArrowButtonShowcase() {
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Enabled prev/next at reader size
        NimazNavArrowButton(
            direction = NavArrowDirection.PREVIOUS,
            onClick = {},
            contentDescription = "Previous"
        )
        NimazNavArrowButton(
            direction = NavArrowDirection.NEXT,
            onClick = {},
            contentDescription = "Next"
        )
        // Disabled end-state + denser stepper size
        NimazNavArrowButton(
            direction = NavArrowDirection.PREVIOUS,
            onClick = {},
            contentDescription = "Previous",
            enabled = false,
            size = 44.dp
        )
        NimazNavArrowButton(
            direction = NavArrowDirection.NEXT,
            onClick = {},
            contentDescription = "Next",
            size = 44.dp
        )
    }
}

@Preview(showBackground = true, name = "NavArrowButton — Light")
@Composable
private fun NimazNavArrowButtonLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        NimazNavArrowButtonShowcase()
    }
}

@Preview(showBackground = true, name = "NavArrowButton — Dark")
@Composable
private fun NimazNavArrowButtonDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        NimazNavArrowButtonShowcase()
    }
}
