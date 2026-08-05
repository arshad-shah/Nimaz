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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.BuildConfig
import com.arshadshah.nimaz.LocalInAppUpdateManager
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.util.UpdateState
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeEmphasis
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazLoadingState
import com.arshadshah.nimaz.presentation.components.atoms.NimazLoadingVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionTitle
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuItem
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.about.updatePrompt
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
    val updateState = updateManager?.updateState?.collectAsStateWithLifecycle()?.value ?: UpdateState.Idle
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

    NimazScreenScaffold(
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
        NimazBadge(
            text = stringResource(
                R.string.version_detail_format,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE
            ),
            tone = NimazTone.ACCENT,
            emphasis = NimazBadgeEmphasis.SOFT,
            size = NimazBadgeSize.MEDIUM,
            icon = Icons.Filled.Verified
        )
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
    val fg =
        if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    NimazCard(
        modifier = modifier,
        style = NimazCardStyle.FILLED,
        tone = if (primary) NimazTone.PROMINENT else NimazTone.MUTED,
        shape = RoundedCornerShape(14.dp),
        elevation = 0.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            NimazIcon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                iconSize = 21.dp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = fg
            )
        }
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

    NimazCard(
        modifier = modifier.fillMaxWidth(),
        style = NimazCardStyle.ELEVATED,
        shape = RoundedCornerShape(16.dp),
        tone = NimazTone.NEUTRAL
    ) {
        NimazMenuItem(
            title = stringResource(R.string.contact_support),
            subtitle = stringResource(R.string.contact_email),
            onClick = onContactUs,
            icon = Icons.Default.Email,
        )
        NimazMenuItem(
            icon = Icons.Default.Language,
            title = stringResource(R.string.website),
            subtitle = stringResource(R.string.website_url_display),
            onClick = { uriHandler.openUri("https://nimaz.arshadshah.com") },
        )
        NimazMenuItem(
            icon = Icons.Default.Shield,
            title = stringResource(R.string.privacy_policy),
            subtitle = stringResource(R.string.privacy_policy_subtitle),
            onClick = onNavigateToPrivacyPolicy,
        )

        NimazMenuItem(
            icon = Icons.Default.Description,
            title = stringResource(R.string.terms_of_service),
            subtitle = stringResource(R.string.terms_of_service_subtitle),
            onClick = onNavigateToTerms,
        )
        NimazMenuItem(
            icon = Icons.Default.Gavel,
            title = stringResource(R.string.open_source_licenses),
            subtitle = stringResource(R.string.open_source_licenses_subtitle),
            onClick = onNavigateToLicenses
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
    // Which label, which icon, and whether a tap does anything is decided by
    // `updatePrompt` and unit-tested there. This composable only paints it.
    val prompt = updatePrompt(updateState)

    val accent = when {
        prompt.isError -> MaterialTheme.colorScheme.error
        prompt.isHighlighted -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    NimazMenuItem(
        modifier = modifier,
        title = stringResource(R.string.check_for_updates),
        subtitle = stringResource(prompt.label),
        icon = prompt.icon,
        onClick = onClick,
        enabled = !prompt.isBusy,
        subtitleStyle = TextStyle(
            color = accent
        )
    )
}

@Composable
private fun DeveloperCard(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    NimazCard(
        modifier = modifier.fillMaxWidth(),
        style = NimazCardStyle.ELEVATED,
        shape = RoundedCornerShape(16.dp),
        tone = NimazTone.NEUTRAL
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
        NimazIcon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            iconSize = 17.dp
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
    NimazCard(
        modifier = modifier,
        style = NimazCardStyle.ELEVATED,
        shape = RoundedCornerShape(12.dp),
        tone = NimazTone.NEUTRAL
    ) {
        Column(
            modifier = Modifier
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
            NimazIcon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                iconSize = 14.dp,
                modifier = Modifier.padding(horizontal = 2.dp)
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
