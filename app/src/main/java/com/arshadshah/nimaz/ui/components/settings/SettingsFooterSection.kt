package com.arshadshah.nimaz.ui.components.settings

import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.components.common.SettingsSection
import com.arshadshah.nimaz.ui.components.common.SettingsSectionItem
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
            .padding(8.dp),
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
fun AppCopyright(
    modifier: Modifier = Modifier,
    appVersion: String = getAppVersion(LocalContext.current)
) {
    val currentYear = remember { LocalDateTime.now().year }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Section
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.info_icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Nimaz",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "© $currentYear",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "v$appVersion",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
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