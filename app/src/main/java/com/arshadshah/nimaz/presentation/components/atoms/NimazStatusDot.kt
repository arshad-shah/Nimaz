package com.arshadshah.nimaz.presentation.components.atoms

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/** Whether the dot is a solid disc or a ring. */
enum class NimazStatusDotStyle {
    /** Solid disc — the state happened. */
    FILLED,

    /**
     * Ring — the state was *recorded as not happening*, which is not the same as no record.
     * This distinction is the reason the atom exists at all.
     */
    OUTLINED
}

/** Dot diameter. */
enum class NimazStatusDotSize(val diameter: Dp) {
    SMALL(6.dp),
    MEDIUM(7.dp),
    LARGE(10.dp)
}

/**
 * A dot's whole appearance in one value, so a caller can carry "how this day looks" through a
 * list without threading a colour and a boolean separately.
 */
data class NimazStatusDotSpec(
    val tone: NimazTone,
    val style: NimazStatusDotStyle = NimazStatusDotStyle.FILLED,
)

/** Ring thickness. One value, because a ring that varies with diameter reads as a smudge. */
private val OutlineWidth = 1.5.dp

/**
 * The app's status dot.
 *
 * [NimazLegendItem] and the calendar each drew their own filled circle, and neither could draw a
 * hollow one — so "logged as not fasted" and "no record at all" both rendered as an absent dot,
 * two different facts sharing one appearance. The ring fixes that.
 *
 * @param spec tone and fill style.
 * @param size diameter rung.
 * @param contentDescription accessibility label; `null` leaves the dot decorative, which is
 *   correct when an adjacent label already says what it means.
 */
@Composable
fun NimazStatusDot(
    spec: NimazStatusDotSpec,
    modifier: Modifier = Modifier,
    size: NimazStatusDotSize = NimazStatusDotSize.MEDIUM,
    contentDescription: String? = null,
) {
    NimazStatusDot(
        color = NimazToneColors.foreground(spec.tone),
        modifier = modifier,
        style = spec.style,
        size = size,
        contentDescription = contentDescription,
    )
}

/**
 * Colour-driven overload for callers that already hold a resolved [Color].
 *
 * The calendar's `CalendarDayState` carries one — day colours there come from feature palettes
 * that predate the tone vocabulary — and making it invent a [NimazTone] just to have the atom
 * turn that back into a colour would be a round trip through a language it does not speak.
 *
 * @param diameter exact size, overriding [size]. For callers that already expose their own `Dp`
 *   and would otherwise be silently resized by snapping to the nearest rung.
 */
@Composable
fun NimazStatusDot(
    color: Color,
    modifier: Modifier = Modifier,
    style: NimazStatusDotStyle = NimazStatusDotStyle.FILLED,
    size: NimazStatusDotSize = NimazStatusDotSize.MEDIUM,
    diameter: Dp? = null,
    contentDescription: String? = null,
) {
    val semanticsModifier = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }

    val paintModifier = when (style) {
        NimazStatusDotStyle.FILLED -> Modifier.background(color, CircleShape)
        NimazStatusDotStyle.OUTLINED -> Modifier.border(OutlineWidth, color, CircleShape)
    }

    Box(
        modifier = modifier
            .size(diameter ?: size.diameter)
            .then(paintModifier)
            .then(semanticsModifier)
    )
}

// ==================== PREVIEWS ====================

private val previewTones = listOf(
    NimazTone.SUCCESS,
    NimazTone.WARNING,
    NimazTone.ERROR,
    NimazTone.ACCENT,
    NimazTone.NEUTRAL,
    NimazTone.MUTED,
)

@Composable
private fun NimazStatusDotShowcase() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        NimazStatusDotStyle.entries.forEach { style ->
            Text(
                text = style.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            NimazStatusDotSize.entries.forEach { size ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    previewTones.forEach { tone ->
                        NimazStatusDot(NimazStatusDotSpec(tone, style), size = size)
                    }
                    Text(
                        text = size.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Text(
            text = "Side by side — the fasting legend",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendSample("Fasted", NimazStatusDotSpec(NimazTone.SUCCESS))
            LegendSample(
                "Not fasting",
                NimazStatusDotSpec(NimazTone.NEUTRAL, NimazStatusDotStyle.OUTLINED)
            )
            LegendSample("Exempt", NimazStatusDotSpec(NimazTone.MUTED))
            LegendSample("Owed", NimazStatusDotSpec(NimazTone.WARNING))
        }
    }
}

@Composable
private fun LegendSample(label: String, spec: NimazStatusDotSpec) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NimazStatusDot(spec)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true, widthDp = 360, name = "NimazStatusDot — Light")
@Composable
private fun NimazStatusDotLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { NimazStatusDotShowcase() }
}

@Preview(
    showBackground = true, widthDp = 360, name = "NimazStatusDot — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazStatusDotDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { NimazStatusDotShowcase() }
}
