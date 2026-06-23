package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazSurfaceCard
import com.arshadshah.nimaz.presentation.theme.NimazPalette
import com.arshadshah.nimaz.presentation.theme.NimazTheme

private val JumuahGreen = NimazPalette.GreenDeep

/**
 * Friday-only highlight card for Jumu'ah, shown above the home "Today" section.
 * A white surface with a green left accent and tinted chip (matching the rest of
 * the redesigned cards) carrying the prayer name in English/Arabic, khutbah time,
 * a countdown-to-khutbah (or a "Jumu'ah passed" acknowledgement), and a hadith.
 */
@Composable
fun JumuahCard(
    jumuahTime: String,
    timeUntilJumuah: String,
    isJumuahPassed: Boolean,
    modifier: Modifier = Modifier
) {
    NimazSurfaceCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(15.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(11.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(JumuahGreen.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            NimazIcon(
                                imageVector = Icons.Default.Mosque,
                                contentDescription = null,
                                tint = JumuahGreen,
                                size = NimazIconSize.MEDIUM
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.jumuah_mubarak),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            ArabicText(
                                text = stringResource(R.string.jumuah_arabic),
                                size = ArabicTextSize.SMALL,
                                color = JumuahGreen
                            )
                        }
                    }

                    if (jumuahTime.isNotEmpty()) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = jumuahTime,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.khutbah_time),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(11.dp))
                        .background(JumuahGreen.copy(alpha = 0.08f))
                        .padding(horizontal = 13.dp, vertical = 11.dp)
                ) {
                    if (isJumuahPassed) {
                            Text(
                                text = stringResource(R.string.jumuah_passed),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = JumuahGreen
                            )
                    } else if (timeUntilJumuah.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.time_until_jumuah),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = timeUntilJumuah,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = JumuahGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(11.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                )

                Spacer(modifier = Modifier.height(11.dp))

                Text(
                    text = stringResource(R.string.jumuah_hadith_quote),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun JumuahCard_Preview() {
    NimazTheme {
        JumuahCard(
            jumuahTime = "1:30 PM",
            timeUntilJumuah = "3h 15m",
            isJumuahPassed = false,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Passed")
@Composable
private fun JumuahCard_Passed_Preview() {
    NimazTheme {
        JumuahCard(
            jumuahTime = "1:30 PM",
            timeUntilJumuah = "",
            isJumuahPassed = true,
            modifier = Modifier.padding(16.dp)
        )
    }
}
