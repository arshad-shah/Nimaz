package com.arshadshah.nimaz.presentation.components.atoms.qibla

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import kotlin.math.abs

/** Turn-direction arrow + "Turn right/left X°" text row. */
@Composable
fun QiblaTurnHint(
    turnRight: Boolean,
    degrees: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        NimazIcon(
            imageVector = if (turnRight) Icons.AutoMirrored.Filled.RotateRight
            else Icons.AutoMirrored.Filled.RotateLeft,
            contentDescription = null,
            tint = QiblaGold,
            iconSize = 18.dp,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (turnRight)
                stringResource(R.string.turn_right_format, abs(degrees))
            else
                stringResource(R.string.turn_left_format, abs(degrees)),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}
