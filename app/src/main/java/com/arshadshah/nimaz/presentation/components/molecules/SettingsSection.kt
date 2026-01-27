package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.ui.tooling.preview.Preview

/**
 * Settings section with header and grouped items.
 */
@Composable
fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Section header
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            if (description != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Section content
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content
            )
        }
    }
}

/**
 * Settings section without card container.
 */
@Composable
fun SettingsSectionFlat(
    title: String,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )

        if (showDivider) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

/**
 * Settings group with optional dividers between items.
 */
@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    showDividers: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

/**
 * Divider for use between settings items.
 */
@Composable
fun SettingsDivider(
    modifier: Modifier = Modifier,
    startIndent: Boolean = true
) {
    HorizontalDivider(
        modifier = modifier.padding(start = if (startIndent) 56.dp else 0.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

/**
 * Settings screen header with title and description.
 */
@Composable
fun SettingsScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (description != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Settings footer with additional info.
 */
@Composable
fun SettingsFooter(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

// Extension property for letter spacing
private val Int.sp: androidx.compose.ui.unit.TextUnit
    get() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)
@Preview(showBackground = true, name = "Settings Section")
@Composable
private fun SettingsSectionPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SettingsSection(
                title = "General",
                description = "App settings and preferences"
            ) {
                Text(
                    text = "Setting item 1",
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider()
                Text(
                    text = "Setting item 2",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Settings Section Flat")
@Composable
private fun SettingsSectionFlatPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SettingsSectionFlat(title = "Notifications") {
                Text(
                    text = "Enable notifications",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Settings Screen Header")
@Composable
private fun SettingsScreenHeaderPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SettingsScreenHeader(
                title = "Settings",
                description = "Customize your app experience"
            )
        }
    }
}

@Preview(showBackground = true, name = "Settings Footer")
@Composable
private fun SettingsFooterPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SettingsFooter(text = "Nimaz Pro v1.0.0 • Made with ❤️ for the Ummah")
        }
    }
}
