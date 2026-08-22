package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.foundation.time.NimazTime
import com.arshadshah.nimaz.presentation.foundation.time.rememberTimeFormatter
import com.arshadshah.nimaz.presentation.theme.NimazSpacing

/**
 * A read-only slot showing a time, for opening [com.arshadshah.nimaz.presentation.components.molecules.NimazTimePicker] from a settings row.
 * Formats to the device's clock preference.
 */
@Composable
fun NimazTimeDisplay(
    time: NimazTime,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = null,
) {
    val format = rememberTimeFormatter()
    // The accent tint marks the *current* value, so the fill stays — routed through
    // the design system rather than a raw `.copy(alpha = …)` background.
    NimazCard(
        modifier = modifier.then(
            if (contentDescription != null) {
                Modifier.clearAndSetSemantics { this.contentDescription = contentDescription }
            } else Modifier
        ),
        style = NimazCardStyle.FILLED,
        shape = RoundedCornerShape(12.dp),
        colors = NimazCardDefaults.colors(
            container = accentColor.copy(alpha = 0.12f),
            content = accentColor,
        ),
        elevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = NimazSpacing.Medium, vertical = NimazSpacing.Small),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = stringResource(R.string.cd_edit_time),
                tint = accentColor,
                modifier = Modifier
                    .padding(end = NimazSpacing.Small)
                    .width(20.dp)
            )
            Text(
                text = format(time),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = accentColor,
            )
        }
    }
}


@Preview
@Composable
fun NimazTimeDisplayPreview() {
    NimazTimeDisplay(
        time = NimazTime(1, 2)
    )
}