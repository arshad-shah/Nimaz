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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.CircularProgressIndicator
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
    val updateManager = LocalInAppUpdateManager.current
    val updateState = updateManager?.updateState?.collectAsState()?.value ?: UpdateState.Idle
    val onUpdateClick = {
        when (updateState) {
            is UpdateState.UpdateAvailable -> updateManager?.startUpdate()
            is UpdateState.Downloaded -> updateState.completeUpdate()
            is UpdateState.Checking,
            is UpdateState.Starting,
            is UpdateState.Downloading -> Unit

            else -> updateManager?.checkForUpdate()
        }
        Unit
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NimazBackTopAppBar(title = stringResource(R.string.about), onBackClick = onNavigateBack)
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { AppInfoHero() }
            item {
                QuickActionsRow(
                    onRateApp = onRateApp,
                    onShareApp = onShareApp
                )
            }

            item {
                NimazSectionTitle(
                    text = stringResource(R.string.links),
                    modifier = Modifier.padding(start = 5.dp)
                )
            }
            item {
                LinksCard(
                    onContactUs = onContactUs,
                    onNavigateToPrivacyPolicy = onNavigateToPrivacyPolicy,
                    onNavigateToTerms = onNavigateToTerms,
                    onNavigateToLicenses = onNavigateToLicenses,
                    updateState = updateState,
                    onUpdateClick = onUpdateClick
                )
            }

            item {
                NimazSectionTitle(
                    text = stringResource(R.string.developer),
                    modifier = Modifier.padding(start = 5.dp)
                )
            }
            item { DeveloperCard() }

            item {
                NimazSectionTitle(
                    text = stringResource(R.string.data_sources_credits),
                    modifier = Modifier.padding(start = 5.dp)
                )
            }
            item { CreditsGrid() }

            item { FooterSection() }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun AppInfoHero(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.mipmap.ic_launcher_foreground),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(100))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                .padding(horizontal = 11.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = stringResource(
                    R.string.version_detail_format,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE
                ),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(11.dp))
        Text(
            text = stringResource(R.string.about_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 40.dp)
        )
    }
}

@Composable
private fun QuickActionsRow(
    onRateApp: () -> Unit,
    onShareApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        QuickActionButton(
            icon = Icons.Filled.Star,
            label = stringResource(R.string.about_rate),
            primary = true,
            onClick = onRateApp,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            icon = Icons.Filled.Share,
            label = stringResource(R.string.about_share),
            primary = false,
            onClick = onShareApp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg =
        if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg =
        if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(21.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = fg
        )
    }
}

@Composable
private fun LinksCard(
    onContactUs: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onNavigateToLicenses: () -> Unit,
    updateState: UpdateState,
    onUpdateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        LinkItem(
            Icons.Default.Email,
            stringResource(R.string.contact_support),
            stringResource(R.string.contact_email),
            onContactUs,
            true
        )
        LinkItem(
            Icons.Default.Language,
            stringResource(R.string.website),
            stringResource(R.string.website_url_display),
            { uriHandler.openUri("https://nimaz.arshadshah.com") },
            true
        )
        LinkItem(
            Icons.Default.Shield,
            stringResource(R.string.privacy_policy),
            stringResource(R.string.privacy_policy_subtitle),
            onNavigateToPrivacyPolicy,
            true
        )
        LinkItem(
            Icons.Default.Description,
            stringResource(R.string.terms_of_service),
            stringResource(R.string.terms_of_service_subtitle),
            onNavigateToTerms,
            true
        )
        LinkItem(
            Icons.Default.Gavel,
            stringResource(R.string.open_source_licenses),
            stringResource(R.string.open_source_licenses_subtitle),
            onNavigateToLicenses,
            true
        )
        UpdateStatusItem(
            updateState = updateState,
            onClick = onUpdateClick
        )
    }
}

@Composable
private fun UpdateStatusItem(
    updateState: UpdateState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val busy = updateState is UpdateState.Checking ||
            updateState is UpdateState.Starting ||
            updateState is UpdateState.Downloading
    val actionable = updateState is UpdateState.UpdateAvailable ||
            updateState is UpdateState.Downloaded
    val isError = updateState is UpdateState.Error

    val subtitle = when (updateState) {
        is UpdateState.Checking -> stringResource(R.string.update_checking)
        is UpdateState.UpdateAvailable -> stringResource(R.string.update_new_version)
        is UpdateState.Starting -> stringResource(R.string.update_starting)
        is UpdateState.Downloading -> stringResource(R.string.update_downloading)
        is UpdateState.Downloaded -> stringResource(R.string.update_downloaded)
        is UpdateState.NoUpdateAvailable -> stringResource(R.string.update_up_to_date)
        is UpdateState.Error -> stringResource(R.string.update_check_failed)
        else -> stringResource(R.string.update_tap_to_check)
    }

    val accent = when {
        isError -> MaterialTheme.colorScheme.error
        actionable || updateState is UpdateState.NoUpdateAvailable -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !busy, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    when {
                        actionable -> MaterialTheme.colorScheme.primary
                        isError -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surface
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                val icon = when (updateState) {
                    is UpdateState.UpdateAvailable -> Icons.Default.Download
                    is UpdateState.Downloaded -> Icons.Default.InstallMobile
                    is UpdateState.NoUpdateAvailable -> Icons.Default.CheckCircle
                    is UpdateState.Error -> Icons.Default.ErrorOutline
                    else -> Icons.Default.Refresh
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (actionable) MaterialTheme.colorScheme.onPrimary else accent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.check_for_updates),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (actionable) FontWeight.SemiBold else FontWeight.Normal,
                color = if (actionable || isError) accent else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!busy) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = if (actionable) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(15.dp)
            )
        }
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
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
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
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(15.dp)
            )
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .padding(horizontal = 14.dp)
                    .background(MaterialTheme.colorScheme.surface)
            )
        }
    }
}

@Composable
private fun DeveloperCard(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(100))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.developer_name),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.developer_role),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DeveloperSocial(Icons.Default.Code) { uriHandler.openUri("https://github.com/arshad-shah") }
        DeveloperSocial(Icons.Default.WorkOutline) { uriHandler.openUri("https://linkedin.com/in/arshadshah") }
    }
}

@Composable
private fun DeveloperSocial(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun CreditsGrid(modifier: Modifier = Modifier) {
    val credits = listOf(
        stringResource(R.string.credit_prayer_times) to stringResource(R.string.credit_aladhan),
        stringResource(R.string.credit_quran_text) to stringResource(R.string.credit_tanzil),
        stringResource(R.string.credit_translations) to stringResource(R.string.credit_sahih_international),
        stringResource(R.string.credit_hadith_data) to stringResource(R.string.credit_sunnah),
        stringResource(R.string.credit_recitations) to stringResource(R.string.credit_quran_com),
        stringResource(R.string.credit_hijri_calendar) to stringResource(R.string.credit_islamic_finder)
    )
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        credits.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                row.forEach { (label, provider) ->
                    CreditCell(label = label, provider = provider, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CreditCell(label: String, provider: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = provider,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun FooterSection(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
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
                modifier = Modifier
                    .size(14.dp)
                    .padding(horizontal = 2.dp)
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
