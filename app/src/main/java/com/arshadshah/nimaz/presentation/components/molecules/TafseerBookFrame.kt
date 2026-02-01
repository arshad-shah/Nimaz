package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Decorative book-like frame for the Tafseer reader, adapted from MushafFrame
 * with softer aesthetics using MaterialTheme colors and rounded corners.
 */
@Composable
fun TafseerBookFrame(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryLight = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 2.dp,
                color = primaryColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(3.dp)
            .border(
                width = 1.dp,
                color = primaryLight,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TafseerOrnamentalDivider()
            content()
            TafseerOrnamentalDivider()
        }
    }
}

/**
 * Ornamental triple-line horizontal divider for section separation within the book frame.
 * Uses gold-primary-gold pattern similar to MushafOrnamentalLine.
 */
@Composable
fun TafseerOrnamentalDivider(
    modifier: Modifier = Modifier
) {
    val goldAccent = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        HorizontalDivider(
            thickness = 1.dp,
            color = goldAccent
        )
        Spacer(modifier = Modifier.height(2.dp))
        HorizontalDivider(
            thickness = 2.dp,
            color = primaryColor
        )
        Spacer(modifier = Modifier.height(2.dp))
        HorizontalDivider(
            thickness = 1.dp,
            color = goldAccent
        )
    }
}
