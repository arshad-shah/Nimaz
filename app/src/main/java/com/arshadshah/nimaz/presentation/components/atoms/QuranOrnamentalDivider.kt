package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.QuranSurfaceColors
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * The shared Quran ornamental divider: a gold hairline fading out to both
 * margins around a central [DiamondFloret] — the same floret the cartouche uses
 * on its Basmala line and its right-tip finial.
 *
 * This is the one divider motif for every Quran reading surface (mushaf page,
 * tafseer study frame, surah info header); it replaced the tafseer's older
 * triple-line divider so both frames read as one system.
 */
@Composable
fun QuranOrnamentalDivider(
    modifier: Modifier = Modifier,
    color: Color = QuranSurfaceColors.frameGold,
    horizontalPadding: Dp = 20.dp,
    verticalPadding: Dp = 8.dp,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, color)))
        )
        DiamondFloret(color = color, size = 7.dp)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(color, Color.Transparent)))
        )
    }
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true, widthDp = 320, name = "Quran Ornamental Divider — Light")
@Composable
private fun QuranOrnamentalDividerLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        Column(Modifier.padding(vertical = 16.dp)) { QuranOrnamentalDivider() }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A08, widthDp = 320, name = "Quran Ornamental Divider — Dark")
@Composable
private fun QuranOrnamentalDividerDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        Column(Modifier.padding(vertical = 16.dp)) { QuranOrnamentalDivider() }
    }
}
