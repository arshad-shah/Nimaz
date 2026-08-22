package com.arshadshah.nimaz.presentation.components.atoms

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * Semantic colour role for a [NimazSwitch]: it tints the **checked** track. The thumb,
 * borders, check glyph and the whole unchecked pill use fixed scheme colours so the OFF
 * state always stays legible; disabled states fall back to the Material 3 defaults. Roles
 * map onto the app's Material 3 scheme so they track theming automatically — pass an
 * explicit `trackTint` to [NimazSwitch] to escape them.
 */
enum class NimazSwitchVariant {
    /** The faithful default toggle tint. `primary`. */
    DEFAULT,

    /** Brand emphasis. `primary`. */
    PRIMARY,

    /** Positive / "active" semantics. `NimazColors.Success`. */
    SUCCESS,

    /** Destructive / alert. `error`. */
    ERROR
}

/**
 * The app's single on/off toggle primitive.
 *
 * Replaces the mix of bare Material 3 `Switch`es and the one hand-styled, fully
 * custom-coloured switch (with its broken all-`surface` disabled state) that the partial
 * migration left in [com.arshadshah.nimaz.presentation.components.molecules.NimazSettingsItem],
 * `NotificationSettingsScreen`, the tasbih sheets and the calendar preview — with one
 * variant-driven entry point so every toggle in the app reads identically.
 *
 * It wraps Material 3 `Switch` (so it keeps the platform drag gesture, the `Role.Switch`
 * accessibility semantics and the tuned thumb animation) and layers on the canonical
 * Nimaz look: a brand-tinted track and a light thumb with a check glyph when checked, and
 * a clearly contrasted outlined pill (raised thumb on a surface track) when off. Only the
 * disabled colours are left to `SwitchDefaults` — which dims them correctly — fixing the
 * old custom switch that hard-coded every disabled colour to `surface` and turned invisible.
 *
 * Genuine multi-choice pickers keep their `RadioButton`; the bordered check box is
 * [NimazCheckbox].
 *
 * Pass `onCheckedChange = null` when an enclosing clickable row owns the toggle (e.g. a
 * whole settings row is tappable): the switch then renders its state and reports it for
 * accessibility but does not handle its own clicks.
 *
 * @param checked whether the switch is currently on.
 * @param onCheckedChange invoked with the toggled value on tap; `null` defers the click
 *   to an enclosing clickable (the row-driven settings drop-in).
 * @param variant semantic colour role for the checked track / glyph; [trackTint] overrides it.
 * @param enabled when false, dims the switch (via Material defaults) and blocks interaction.
 * @param thumbIcon glyph shown on the thumb while checked; `null` hides it for a plain thumb.
 * @param trackTint escape hatch overriding [variant]'s checked-track colour (brand
 *   `NimazColors.*` or a runtime colour).
 * @param contentDescription accessibility label for the toggle.
 */
@Composable
fun NimazSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    variant: NimazSwitchVariant = NimazSwitchVariant.PRIMARY,
    enabled: Boolean = true,
    thumbIcon: ImageVector? = Icons.Default.Check,
    trackTint: Color? = null,
    contentDescription: String? = null,
) {
    val trackColor = trackTint ?: variant.resolveTrack()

    val descriptionModifier = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }

    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.then(descriptionModifier),
        enabled = enabled,
        thumbContent = if (checked && thumbIcon != null) {
            {
                NimazIcon(
                    imageVector = thumbIcon,
                    contentDescription = null,
                    iconSize = 16.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            null
        },
        // The full enabled palette from the original enhanced switch (so the OFF thumb stays
        // visible against the track); only the variant drives the checked track colour. The
        // disabled colours are left to SwitchDefaults — the old switch hard-coded every
        // disabled colour to `surface`, which made a disabled switch invisible.
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.surface,
            checkedTrackColor = trackColor,
            checkedBorderColor = MaterialTheme.colorScheme.outline,
            checkedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            uncheckedThumbColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            uncheckedTrackColor = MaterialTheme.colorScheme.surface,
            uncheckedBorderColor = MaterialTheme.colorScheme.outline,
            uncheckedIconColor = MaterialTheme.colorScheme.onSurface,
        )
    )
}

/** Checked-track / glyph colour for the variant. */
@Composable
private fun NimazSwitchVariant.resolveTrack(): Color = when (this) {
    NimazSwitchVariant.DEFAULT -> MaterialTheme.colorScheme.primary
    NimazSwitchVariant.PRIMARY -> MaterialTheme.colorScheme.primary
    NimazSwitchVariant.SUCCESS -> NimazColors.Success
    NimazSwitchVariant.ERROR -> MaterialTheme.colorScheme.error
}


// ==================== PREVIEWS ====================

@Composable
private fun NimazSwitchVariantsShowcase() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NimazSwitchVariant.entries.forEach { variant ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NimazSwitch(checked = false, onCheckedChange = {}, variant = variant)
                NimazSwitch(checked = true, onCheckedChange = {}, variant = variant)
                Text(variant.name, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun NimazSwitchStatesShowcase() {
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Disabled on / off.
        NimazSwitch(checked = true, onCheckedChange = {}, enabled = false)
        NimazSwitch(checked = false, onCheckedChange = {}, enabled = false)
        // Plain thumb (no glyph).
        NimazSwitch(checked = true, onCheckedChange = {}, thumbIcon = null)
        // Row-driven (no own handler).
        NimazSwitch(checked = true, onCheckedChange = null)
    }
}

@Composable
private fun NimazSwitchShowcase() {
    Column(
        modifier = Modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Variants (off · on)", style = MaterialTheme.typography.labelMedium)
        NimazSwitchVariantsShowcase()
        Text("Disabled · plain thumb · row-driven", style = MaterialTheme.typography.labelMedium)
        NimazSwitchStatesShowcase()
    }
}

@Preview(showBackground = true, name = "NimazSwitch — Light")
@Composable
private fun NimazSwitchLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { NimazSwitchShowcase() }
}

@Preview(
    showBackground = true, name = "NimazSwitch — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazSwitchDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { NimazSwitchShowcase() }
}
