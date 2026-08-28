package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.core.share.QrCodes
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Renders [content] (usually a URL) as a QR code, encoded with [QrCodes] — the same
 * encoder used on the branded share card, so the in-app invite QR and the card QR
 * are identical. Defaults to brand teal modules on a white ground so it scans well
 * and stays on-brand in both themes.
 */
@Composable
fun NimazQrCode(
    content: String,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    dark: Color = MaterialTheme.colorScheme.primary,
    light: Color = Color.White,
) {
    val px = with(LocalDensity.current) { size.roundToPx() }.coerceAtLeast(1)
    val darkArgb = dark.toArgb()
    val lightArgb = light.toArgb()
    val image = remember(content, px, darkArgb, lightArgb) {
        runCatching { QrCodes.encode(content, px, darkArgb, lightArgb).asImageBitmap() }.getOrNull()
    }
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = null,
            modifier = modifier
                .size(size)
                .padding(2.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NimazQrCodePreview() {
    NimazTheme {
        NimazQrCode(content = QrCodes.APP_URL, size = 200.dp)
    }
}
