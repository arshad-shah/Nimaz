package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.theme.NimazSpacing

/**
 * Master switch for the Qaida audio-playback UI. Temporarily `false` while the
 * lesson audio clips are being regenerated, so the reader and letter explorer
 * show text only (the Play-line pills and the letter play button are hidden).
 * Flip back to `true` once the new, higher-quality audio ships.
 */
const val QAIDA_AUDIO_UI_ENABLED = false

/**
 * Small "Play line" pill that plays a whole lesson line back-to-back. Tonal
 * teal so it reads as a secondary action next to the bigger cell tiles.
 */
@Composable
fun QaidaPlayLineButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String = stringResource(R.string.qaida_play_line),
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(percent = 50),
        modifier = modifier.semantics { contentDescription = label },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = label,
                modifier = Modifier.padding(start = NimazSpacing.ExtraSmall),
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            )
        }
    }
}
