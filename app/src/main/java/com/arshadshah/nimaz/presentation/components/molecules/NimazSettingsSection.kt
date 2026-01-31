package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.presentation.theme.NimazTheme

@Composable
fun NimazSettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    showCard: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )
        if (showCard) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(content = content)
            }
        } else {
            Column(content = content)
        }
    }
}

@Preview(showBackground = true, widthDp = 400, name = "NimazSettingsSection")
@Composable
private fun NimazSettingsSectionPreview() {
    NimazTheme {
        NimazSettingsSection(
            title = "PRAYER SETTINGS",
            modifier = Modifier.padding(16.dp)
        ) {
            NimazSettingsItem(
                title = "Calculation Method",
                subtitle = "Prayer time calculation settings",
                onClick = {}
            )
        }
    }
}
