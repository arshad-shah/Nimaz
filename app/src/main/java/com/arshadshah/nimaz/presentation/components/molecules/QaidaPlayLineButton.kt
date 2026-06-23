package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonType
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

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
    NimazButton(
        text = label,
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = label },
        variant = NimazButtonVariant.TONAL,
        type = NimazButtonType.PILL,
        leadingIcon = Icons.Filled.PlayArrow,
        enabled = enabled,
    )
}


// ==================== PREVIEWS ====================

@Composable
private fun QaidaPlayLineButtonShowcase() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        QaidaPlayLineButton(onClick = {}, label = "Play line")
        QaidaPlayLineButton(onClick = {}, enabled = false, label = "Play line")
    }
}

@Preview(showBackground = true, name = "Qaida Play Line Button — Light")
@Composable
private fun QaidaPlayLineButtonLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        QaidaPlayLineButtonShowcase()
    }
}

@Preview(showBackground = true, name = "Qaida Play Line Button — Dark")
@Composable
private fun QaidaPlayLineButtonDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        QaidaPlayLineButtonShowcase()
    }
}
