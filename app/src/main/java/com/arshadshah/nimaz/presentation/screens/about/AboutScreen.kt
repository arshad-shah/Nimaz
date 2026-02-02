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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkOutline
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.BuildConfig
import com.arshadshah.nimaz.LocalInAppUpdateManager
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.util.UpdateState
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionTitle
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import java.time.LocalDate

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
                title = stringResource(R.string.about),
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
                    text = stringResource(R.string.links),
                    modifier = Modifier.padding(start = 5.dp, top = 4.dp, bottom = 0.dp)
                )
            }

            item {
                LinksCard(
                    onRateApp = onRateApp,
                    onContactUs = onContactUs,
                    onNavigateToPrivacyPolicy = onNavigateToPrivacyPolicy,
                    onNavigateToTerms = onNavigateToTerms,
                    onNavigateToLicenses = onNavigateToLicenses
                )
            }

            // Developer Section
            item {
                NimazSectionTitle(
                    text = stringResource(R.string.developer),
                    modifier = Modifier.padding(start = 5.dp, top = 4.dp, bottom = 0.dp)
                )
            }

            item {
                DeveloperCard()
            }

            // Credits Section
            item {
                NimazSectionTitle(
                    text = stringResource(R.string.data_sources_credits),
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
        Image(
            painter = painterResource(R.mipmap.ic_launcher_foreground),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(24.dp))
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = stringResource(R.string.version_detail_format, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(15.dp))

        Text(
            text = stringResource(R.string.app_description),
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
    onNavigateToLicenses: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

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
            title = stringResource(R.string.rate_play_store),
            subtitle = stringResource(R.string.rate_play_store_subtitle),
            onClick = onRateApp,
            showDivider = true
        )
        LinkItem(
            icon = Icons.Default.Email,
            title = stringResource(R.string.contact_support),
            subtitle = stringResource(R.string.contact_email),
            onClick = onContactUs,
            showDivider = true
        )
        LinkItem(
            icon = Icons.Default.Language,
            title = stringResource(R.string.website),
            subtitle = stringResource(R.string.website_url_display),
            onClick = { uriHandler.openUri("https://nimaz.arshadshah.com") },
            showDivider = true
        )
        LinkItem(
            icon = Icons.Default.Shield,
            title = stringResource(R.string.privacy_policy),
            subtitle = stringResource(R.string.privacy_policy_subtitle),
            onClick = onNavigateToPrivacyPolicy,
            showDivider = true
        )
        LinkItem(
            icon = Icons.Default.Description,
            title = stringResource(R.string.terms_of_service),
            subtitle = stringResource(R.string.terms_of_service_subtitle),
            onClick = onNavigateToTerms,
            showDivider = true
        )
        LinkItem(
            icon = Icons.Default.Gavel,
            title = stringResource(R.string.open_source_licenses),
            subtitle = stringResource(R.string.open_source_licenses_subtitle),
            onClick = onNavigateToLicenses,
            showDivider = true
        )
        val updateManager = LocalInAppUpdateManager.current
        val updateState = updateManager?.updateState?.collectAsState()?.value ?: UpdateState.Idle
        val updateSubtitle = when (updateState) {
            is UpdateState.UpdateAvailable -> stringResource(R.string.update_new_version)
            is UpdateState.Downloading -> stringResource(R.string.update_downloading)
            is UpdateState.Downloaded -> stringResource(R.string.update_downloaded)
            is UpdateState.NoUpdateAvailable -> stringResource(R.string.update_up_to_date)
            is UpdateState.Error -> stringResource(R.string.update_check_failed)
            else -> stringResource(R.string.update_tap_to_check)
        }
        LinkItem(
            icon = Icons.Default.Refresh,
            title = stringResource(R.string.check_for_updates),
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
private fun DeveloperCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.developer_name),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.developer_role),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.developer_education),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.developer_location),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
            stringResource(R.string.credit_quran_text) to stringResource(R.string.credit_tanzil),
            stringResource(R.string.credit_translations) to stringResource(R.string.credit_sahih_international),
            stringResource(R.string.credit_hadith_data) to stringResource(R.string.credit_sunnah),
            stringResource(R.string.credit_prayer_times) to stringResource(R.string.credit_aladhan),
            stringResource(R.string.credit_recitations) to stringResource(R.string.credit_quran_com),
            stringResource(R.string.credit_hijri_calendar) to stringResource(R.string.credit_islamic_finder)
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

    data class SocialLink(val icon: ImageVector, val url: String, val label: String)

    val socials = listOf(
        SocialLink(Icons.Default.Code, "https://github.com/arshad-shah", stringResource(R.string.github)),
        SocialLink(Icons.Default.WorkOutline, "https://linkedin.com/in/arshadshah", stringResource(R.string.linkedin)),
        SocialLink(Icons.Default.Email, "mailto:arshad@arshadshah.com", stringResource(R.string.email)),
        SocialLink(Icons.Default.Language, "https://arshadshah.com", stringResource(R.string.website))
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        socials.forEachIndexed { index, social ->
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { uriHandler.openUri(social.url) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = social.icon,
                    contentDescription = social.label,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.made_with),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = stringResource(R.string.for_the_ummah),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.copyright_format, LocalDate.now().year),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}
