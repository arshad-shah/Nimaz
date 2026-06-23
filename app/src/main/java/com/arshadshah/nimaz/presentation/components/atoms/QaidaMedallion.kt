package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * A circular lesson node on the Qaida course map. Gold (done), teal (current) or
 * a muted fill (locked). The label is rendered as Arabic-Indic digits via
 * [ArabicText]. Locked medallions are not clickable. Colours come from a
 * [QaidaPalette] so the node matches the active theme.
 */
@Composable
fun QaidaMedallion(
    label: String,
    state: QaidaMedallionState,
    contentDescription: String,
    palette: QaidaPalette,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    onClick: (() -> Unit)? = null,
) {
    val fill = when (state) {
        QaidaMedallionState.DONE -> palette.done
        QaidaMedallionState.CURRENT -> palette.current
        QaidaMedallionState.LOCKED -> palette.locked
    }
    val content = when (state) {
        QaidaMedallionState.DONE -> palette.onDone
        QaidaMedallionState.CURRENT -> palette.onCurrent
        QaidaMedallionState.LOCKED -> palette.onLocked
    }
    val isClickable = state != QaidaMedallionState.LOCKED && onClick != null
    // A soft gold halo ring on the current node gives the "illuminated" lift.
    val ring = when (state) {
        QaidaMedallionState.CURRENT -> BorderStroke(3.dp, palette.gold)
        else -> null
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(fill)
            .then(if (ring != null) Modifier.border(ring, CircleShape) else Modifier)
            .then(if (isClickable) Modifier.clickable { onClick() } else Modifier)
            .clearAndSetSemantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        if (state == QaidaMedallionState.LOCKED) {
            NimazIcon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = content,
            )
        } else {
            ArabicText(
                text = label,
                size = ArabicTextSize.LARGE,
                color = content,
            )
        }
    }
}


// ==================== PREVIEWS ====================

@Composable
private fun QaidaMedallionShowcase() {
    val palette = rememberQaidaPalette()
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        QaidaMedallion(
            label = "١",
            state = QaidaMedallionState.DONE,
            contentDescription = "Lesson 1, complete",
            palette = palette,
            onClick = {},
        )
        QaidaMedallion(
            label = "٢",
            state = QaidaMedallionState.CURRENT,
            contentDescription = "Lesson 2, current",
            palette = palette,
            onClick = {},
        )
        QaidaMedallion(
            label = "٣",
            state = QaidaMedallionState.LOCKED,
            contentDescription = "Lesson 3, locked",
            palette = palette,
        )
    }
}

@Preview(showBackground = true, name = "Qaida Medallion — Light")
@Composable
private fun QaidaMedallionLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        QaidaMedallionShowcase()
    }
}

@Preview(showBackground = true, name = "Qaida Medallion — Dark")
@Composable
private fun QaidaMedallionDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        QaidaMedallionShowcase()
    }
}
