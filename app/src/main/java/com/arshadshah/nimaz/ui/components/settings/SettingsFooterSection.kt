package com.arshadshah.nimaz.ui.components.settings

import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import java.time.LocalDateTime

@Composable
fun SettingsFooterSection(
    onNavigateToPrivacyPolicy: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToLicenses: () -> Unit,
    onRateApp: () -> Unit,
    onShareApp: () -> Unit,
    onNavigateToAbout: () -> Unit,
    isUpdateAvailable: Boolean,
    onUpdateApp: () -> Unit,
    isDebugMode: Boolean = false,
    onNavigateToDebug: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Legal Section
        SettingsSection(
            title = "Legal",
            icon = Icons.Default.Security,
            items = listOf(
                SettingsSectionItem(
                    title = "Privacy Policy",
                    subtitle = "How we handle your data",
                    icon = R.drawable.privacy_policy_icon,
                    onClick = onNavigateToPrivacyPolicy,
                ),
                SettingsSectionItem(
                    title = "Terms of Service",
                    subtitle = "App usage guidelines",
                    icon = R.drawable.document_icon,
                    onClick = onNavigateToTerms,
                )
            )
        )

        // Support Section
        SettingsSection(
            title = "Support",
            icon = Icons.AutoMirrored.Filled.Help,
            items = listOf(
                SettingsSectionItem(
                    title = "Help & FAQ",
                    subtitle = "Get assistance",
                    icon = R.drawable.help_icon,
                    onClick = onNavigateToHelp,
                ),
                SettingsSectionItem(
                    title = "License & Acknowledgements",
                    subtitle = "Open source libraries",
                    icon = R.drawable.license_icon,
                    onClick = onNavigateToLicenses,
                )
            )
        )

        // App Section
        SettingsSection(
            title = "App",
            icon = Icons.Default.Apps,
            items = buildList {
                add(
                    SettingsSectionItem(
                        title = "Rate Nimaz",
                        subtitle = "Share your feedback",
                        icon = R.drawable.rating_icon,
                        onClick = onRateApp
                    )
                )
                add(
                    SettingsSectionItem(
                        title = "Share Nimaz",
                        subtitle = "Spread the word",
                        icon = R.drawable.share_icon,
                        onClick = onShareApp
                    )
                )
                add(
                    SettingsSectionItem(
                        title = "About",
                        subtitle = if (isUpdateAvailable) "Update Available" else "Nimaz is up to date",
                        icon = R.drawable.info_icon,
                        onClick = onNavigateToAbout,
                        action = if (isUpdateAvailable) {
                            {
                                Button(
                                    onClick = onUpdateApp,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        "Update",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        } else null
                    )
                )
                if (isDebugMode) {
                    add(
                        SettingsSectionItem(
                            title = "Debug Tools",
                            subtitle = "For testing purposes only",
                            icon = R.drawable.debug_icon,
                            onClick = onNavigateToDebug,
                        )
                    )
                }
            }
        )

        AppCopyright()
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    items: List<SettingsSectionItem>,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Settings Items
            items.forEach { item ->
                Surface(
                    onClick = item.onClick,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icon Container
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = item.icon),
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxSize(),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        // Content
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            item.subtitle?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Action or Arrow
                        item.action?.invoke() ?: run {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.angle_right_icon),
                                    contentDescription = "Open",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun AppCopyright(
    modifier: Modifier = Modifier,
    appVersion: String = getAppVersion(LocalContext.current)
) {
    val currentYear = remember { LocalDateTime.now().year }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .semantics { contentDescription = "Copyright information" },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = buildAnnotatedString {
                    append("© $currentYear ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Nimaz")
                    }
                    append(" • Version: $appVersion")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

//
fun getAppVersion(context: Context): String {
    return try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        pInfo.versionName.toString() // Or use versionCode based on your need
    } catch (e: PackageManager.NameNotFoundException) {
        "Unknown"
    }
}


fun getAppID(context: Context): String {
    return try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        pInfo.packageName // Or use versionCode based on your need
    } catch (e: PackageManager.NameNotFoundException) {
        "Unknown"
    }
}

private data class SettingsSectionItem(
    val title: String,
    val subtitle: String? = null,
    @DrawableRes val icon: Int,
    val onClick: () -> Unit,
    val action: (@Composable () -> Unit)? = null
)