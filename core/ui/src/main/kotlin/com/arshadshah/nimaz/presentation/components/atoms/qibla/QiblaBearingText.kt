package com.arshadshah.nimaz.presentation.components.atoms.qibla

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

/** "119° SE" style bearing display. */
@Composable
fun QiblaBearingText(
    bearing: Int,
    cardinal: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "$bearing° $cardinal",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = modifier,
    )
}
