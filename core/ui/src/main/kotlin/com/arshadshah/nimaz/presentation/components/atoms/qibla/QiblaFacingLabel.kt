package com.arshadshah.nimaz.presentation.components.atoms.qibla

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.arshadshah.nimaz.core.ui.R

/** "Facing Qibla" label rendered inside the green capsule state. */
@Composable
fun QiblaFacingLabel(
    color: Color = Color.White,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(R.string.facing_qibla),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = modifier,
    )
}
