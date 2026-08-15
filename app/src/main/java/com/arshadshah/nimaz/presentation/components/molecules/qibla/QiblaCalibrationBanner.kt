package com.arshadshah.nimaz.presentation.components.molecules.qibla

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Red calibration prompt shown only when compass accuracy is UNRELIABLE or LOW.
 * A compact banner — not a dialog — so the user can still see the compass behind it.
 */
@Composable
fun QiblaCalibrationBanner(
    onCalibrate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val errorColor = MaterialTheme.colorScheme.error
    val errorContainerColor = MaterialTheme.colorScheme.errorContainer

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, errorColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
        color = errorContainerColor.copy(alpha = 0.6f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Warning icon in a tinted rounded square
            Surface(
                modifier = Modifier.size(34.dp),
                shape = RoundedCornerShape(11.dp),
                color = errorColor.copy(alpha = 0.16f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    NimazIcon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = errorColor,
                        size = NimazIconSize.SMALL,
                    )
                }
            }

            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.compass_is_unsure_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.compass_is_unsure_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Calibrate button — DESTRUCTIVE maps to error/onError colours
            NimazButton(
                text = stringResource(R.string.calibrate),
                onClick = onCalibrate,
                variant = NimazButtonVariant.DESTRUCTIVE,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 380, name = "Calibration Banner")
@Composable
private fun QiblaCalibrationBannerPreview() {
    NimazTheme {
        QiblaCalibrationBanner(onCalibrate = {})
    }
}
