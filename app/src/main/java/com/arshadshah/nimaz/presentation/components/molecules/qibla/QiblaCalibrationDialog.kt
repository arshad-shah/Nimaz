package com.arshadshah.nimaz.presentation.components.molecules.qibla

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.CompassAccuracy
import com.arshadshah.nimaz.presentation.components.atoms.qibla.CalibrationStep
import com.arshadshah.nimaz.presentation.components.molecules.NimazDialog
import com.arshadshah.nimaz.presentation.components.molecules.NimazDialogConfirmButton
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Compass calibration instructions dialog. Custom content (numbered steps + a
 * tip) so it opts out of the dialog's auto content-card wrap and provides its
 * own surface.
 */
@Composable
fun QiblaCalibrationDialog(
    accuracy: CompassAccuracy,
    onDismiss: () -> Unit,
) {
    NimazDialog(
        title = stringResource(R.string.calibrate_compass),
        onDismiss = onDismiss,
        titleIcon = Icons.Default.Warning,
        accentColor = MaterialTheme.colorScheme.error,
        showCloseButton = false,
        wrapContent = false,
        content = {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.current_accuracy_format,
                    accuracy.name.lowercase().replaceFirstChar { it.uppercase() }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.improve_accuracy),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                CalibrationStep("1", stringResource(R.string.calibration_step_1))
                CalibrationStep("2", stringResource(R.string.calibration_step_2))
                CalibrationStep("3", stringResource(R.string.calibration_step_3))
                CalibrationStep("4", stringResource(R.string.calibration_step_4))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.calibration_tip),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
        },
        actions = {
            NimazDialogConfirmButton(text = stringResource(R.string.got_it), onClick = onDismiss)
        }
    )
}

@Preview(showBackground = true, widthDp = 400, name = "Calibration Dialog")
@Composable
private fun QiblaCalibrationDialogPreview() {
    NimazTheme {
        QiblaCalibrationDialog(accuracy = CompassAccuracy.LOW, onDismiss = {})
    }
}
