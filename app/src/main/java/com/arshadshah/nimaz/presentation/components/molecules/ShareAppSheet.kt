package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.share.QrCodes
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonType
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazQrCode
import com.arshadshah.nimaz.presentation.theme.NimazColors

/**
 * The "share the app" surface: a scannable Nimaz QR (straight to the Play Store)
 * shown in a modal bottom sheet, with a button to fire the system share sheet for
 * people who'd rather send the link. Reuses [NimazBottomSheet], [NimazQrCode] and
 * [NimazButton].
 *
 * @param onShareLink open the system share sheet with the app-invite link
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareAppSheet(
    onDismiss: () -> Unit,
    onShareLink: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NimazBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = stringResource(R.string.share_app_title),
        subtitle = stringResource(R.string.share_app_subtitle),
        icon = Icons.Filled.Share,
        onClose = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                NimazQrCode(
                    content = QrCodes.APP_URL,
                    size = 220.dp,
                    dark = NimazColors.Primary700,
                    modifier = Modifier.padding(18.dp),
                )
            }
            Text(
                text = stringResource(R.string.share_app_scan_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            NimazButton(
                text = stringResource(R.string.share_app_link_button),
                onClick = onShareLink,
                variant = NimazButtonVariant.TONAL,
                type = NimazButtonType.PILL,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
