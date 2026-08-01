package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * A labelled slider row inside a [NimazMenuGroup] — a title on the left, the live value on the
 * right, and the track beneath.
 *
 * This shape was written out five separate times (Quran/Dua/Hadith Arabic size, Dua/Hadith
 * translation size), each with its own copy of the `SliderDefaults.colors(…)` triple; they had
 * already drifted apart on padding. One component means a slider restyle is a one-file edit.
 *
 * @param valueLabel the value as the user should read it (e.g. "24 sp"), shown beside the title.
 * @param contentDescription names the slider for TalkBack. The title `Text` beside it is a
 *   sibling node, not the slider's own label, so without this the control announces only its
 *   percentage — which says nothing about *what* is being sized. Set as a label rather than
 *   with `clearAndSetSemantics`, which would take the slider's adjust actions with it.
 */
@Composable
fun NimazSettingsSlider(
    title: String,
    valueLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    // Matches NimazSettingsItem's disabled treatment, so a disabled slider and a disabled
    // toggle in the same group read the same. Without it only the track dims and the label
    // still looks live.
    Column(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.5f)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(15.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            modifier = if (contentDescription != null) {
                Modifier.semantics { this.contentDescription = contentDescription }
            } else {
                Modifier
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "NimazSettingsSlider")
@Composable
private fun NimazSettingsSliderPreview() {
    NimazTheme {
        NimazMenuGroup(modifier = Modifier.padding(16.dp)) {
            NimazSettingsSlider(
                title = "Arabic Size",
                valueLabel = "28 sp",
                value = 28f,
                onValueChange = {},
                valueRange = 18f..42f
            )
        }
    }
}
