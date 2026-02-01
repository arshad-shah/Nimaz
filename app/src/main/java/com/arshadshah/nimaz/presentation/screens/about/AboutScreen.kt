package com.arshadshah.nimaz.presentation.screens.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.LocalInAppUpdateManager
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.util.UpdateState
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionTitle
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazTheme

private const val APP_VERSION = "1.0.0"
private const val BUILD_NUMBER = 1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onNavigateToLicenses: () -> Unit,
    onRateApp: () -> Unit,
    onShareApp: () -> Unit,
    onContactUs: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NimazBackTopAppBar(
                title = "About",
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Info Header
            item {
                AppInfoSection()
            }

            // Links Section
            item {
                NimazSectionTitle(
                    text = "Links",
                    modifier = Modifier.padding(start = 5.dp, top = 4.dp, bottom = 0.dp)
                )
            }

            item {
                LinksCard(
                    onRateApp = onRateApp,
                    onContactUs = onContactUs,
                    onNavigateToPrivacyPolicy = onNavigateToPrivacyPolicy,
                    onNavigateToTerms = onNavigateToTerms
                )
            }

            // Credits Section
            item {
                NimazSectionTitle(
                    text = "Data Sources & Credits",
                    modifier = Modifier.padding(start = 5.dp, top = 4.dp, bottom = 0.dp)
                )
            }

            item {
                CreditsCard()
            }

            // Social Links
            item {
                SocialLinksRow()
            }

            // Footer
            item {
                FooterSection()
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun AppInfoSection(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Logo — use foreground layer directly since ic_launcher is an
        // adaptive icon XML that painterResource cannot handle
        Image(
            painter = painterResource(R.mipmap.ic_launcher_foreground),
            contentDescription = "Nimaz",
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(24.dp))
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Nimaz",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = "Version $APP_VERSION (Build $BUILD_NUMBER)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(15.dp))

        Text(
            text = "Your complete Islamic companion for prayer times, Quran, and daily worship",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 40.dp)
        )
    }
}

@Composable
private fun LinksCard(
    onRateApp: () -> Unit,
    onContactUs: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit,
    onNavigateToTerms: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
    ) {
        LinkItem(
            icon = Icons.Default.Star,
            title = "Rate on Play Store",
            subtitle = "Help us reach more Muslims",
            onClick = onRateApp,
            showDivider = true
        )
        LinkItem(
            icon = Icons.Default.Email,
            title = "Contact Support",
            subtitle = "support@nimaz.app",
            onClick = onContactUs,
            showDivider = true
        )
        val uriHandler = LocalUriHandler.current
        LinkItem(
            icon = Icons.Default.Public,
            title = "Website",
            subtitle = "nimaz.app",
            onClick = { uriHandler.openUri("https://nimaz.app") },
            showDivider = true
        )
        LinkItem(
            icon = Icons.Default.Description,
            title = "Privacy Policy",
            subtitle = "How we handle your data",
            onClick = onNavigateToPrivacyPolicy,
            showDivider = true
        )
        LinkItem(
            icon = Icons.Default.ListAlt,
            title = "Terms of Service",
            subtitle = "Usage terms",
            onClick = onNavigateToTerms,
            showDivider = true
        )
        val updateManager = LocalInAppUpdateManager.current
        val updateState = updateManager?.updateState?.collectAsState()?.value ?: UpdateState.Idle
        val updateSubtitle = when (updateState) {
            is UpdateState.UpdateAvailable -> "New version available"
            is UpdateState.Downloading -> "Downloading..."
            is UpdateState.Downloaded -> "Ready to install"
            is UpdateState.NoUpdateAvailable -> "You're up to date"
            is UpdateState.Error -> "Check failed"
            else -> "Tap to check"
        }
        LinkItem(
            icon = Icons.Default.Refresh,
            title = "Check for Updates",
            subtitle = updateSubtitle,
            onClick = {
                when (updateState) {
                    is UpdateState.UpdateAvailable -> updateManager?.startUpdate()
                    is UpdateState.Downloaded -> updateState.completeUpdate()
                    else -> updateManager?.checkForUpdate()
                }
            },
            showDivider = false
        )
    }
}

@Composable
private fun LinkItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showDivider: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(15.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }

        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .padding(horizontal = 16.dp)
                    .background(MaterialTheme.colorScheme.surface)
            )
        }
    }
}

@Composable
private fun CreditsCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val credits = listOf(
            "Quran Text" to "Tanzil.net",
            "Translations" to "Sahih International",
            "Hadith Data" to "Sunnah.com",
            "Prayer Times" to "Aladhan API",
            "Recitations" to "Quran.com",
            "Hijri Calendar" to "Islamic Finder"
        )

        credits.forEach { (name, source) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = source,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SocialLinksRow(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val socials = listOf(
        "X" to "https://x.com/nimaz",
        "IG" to "https://instagram.com/nimaz",
        "YT" to "https://youtube.com/@nimaz"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        socials.forEachIndexed { index, (label, url) ->
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { uriHandler.openUri(url) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (index < socials.lastIndex) {
                Spacer(modifier = Modifier.width(15.dp))
            }
        }
    }
}

@Composable
private fun FooterSection(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Made with love for the Ummah",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "\u00A9 2026 Nimaz. All rights reserved.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "LinkItem")
@Composable
private fun LinkItemPreview() {
    NimazTheme {
        LinkItem(
            icon = Icons.Default.Star,
            title = "Rate on Play Store",
            subtitle = "Help us reach more Muslims",
            onClick = {},
            showDivider = true
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "CreditsCard")
@Composable
private fun CreditsCardPreview() {
    NimazTheme {
        CreditsCard()
    }
}

@Preview(showBackground = true, widthDp = 400, name = "SocialLinksRow")
@Composable
private fun SocialLinksRowPreview() {
    NimazTheme {
        SocialLinksRow()
    }
}

@Preview(showBackground = true, widthDp = 400, name = "FooterSection")
@Composable
private fun FooterSectionPreview() {
    NimazTheme {
        FooterSection()
    }
}

