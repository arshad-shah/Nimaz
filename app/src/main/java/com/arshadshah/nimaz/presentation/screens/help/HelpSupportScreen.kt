package com.arshadshah.nimaz.presentation.screens.help

import android.content.Intent
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.molecules.NimazAccordion
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazTheme

private data class FaqItem(val questionResId: Int, val answerResId: Int)

private val faqItems = listOf(
    // Prayer Times
    FaqItem(R.string.faq_prayer_times_q, R.string.faq_prayer_times_a),
    FaqItem(R.string.faq_calculation_method_q, R.string.faq_calculation_method_a),
    FaqItem(R.string.faq_asr_method_q, R.string.faq_asr_method_a),
    FaqItem(R.string.faq_high_latitude_q, R.string.faq_high_latitude_a),
    FaqItem(R.string.faq_mosque_difference_q, R.string.faq_mosque_difference_a),
    FaqItem(R.string.faq_adjust_times_q, R.string.faq_adjust_times_a),
    // Location
    FaqItem(R.string.faq_set_location_q, R.string.faq_set_location_a),
    FaqItem(R.string.faq_location_permission_q, R.string.faq_location_permission_a),
    // Notifications and Adhan
    FaqItem(R.string.faq_notifications_q, R.string.faq_notifications_a),
    FaqItem(R.string.faq_adhan_q, R.string.faq_adhan_a),
    FaqItem(R.string.faq_reminder_q, R.string.faq_reminder_a),
    FaqItem(R.string.faq_dnd_q, R.string.faq_dnd_a),
    // Qibla
    FaqItem(R.string.faq_qibla_q, R.string.faq_qibla_a),
    // Quran
    FaqItem(R.string.faq_quran_translation_q, R.string.faq_quran_translation_a),
    FaqItem(R.string.faq_quran_audio_q, R.string.faq_quran_audio_a),
    // Tasbih
    FaqItem(R.string.faq_tasbih_q, R.string.faq_tasbih_a),
    // Prayer Tracking
    FaqItem(R.string.faq_track_prayers_q, R.string.faq_track_prayers_a),
    FaqItem(R.string.faq_qada_q, R.string.faq_qada_a),
    // Calendar and Hijri
    FaqItem(R.string.faq_hijri_q, R.string.faq_hijri_a),
    // Fasting
    FaqItem(R.string.faq_fasting_q, R.string.faq_fasting_a),
    // Zakat
    FaqItem(R.string.faq_zakat_q, R.string.faq_zakat_a),
    // General and Privacy
    FaqItem(R.string.faq_privacy_q, R.string.faq_privacy_a),
    FaqItem(R.string.faq_offline_q, R.string.faq_offline_a)
)

private data class FeatureGuide(val titleResId: Int, val descriptionResId: Int)

private val featureGuides = listOf(
    FeatureGuide(R.string.guide_prayer_times_title, R.string.guide_prayer_times_desc),
    FeatureGuide(R.string.guide_tracker_title, R.string.guide_tracker_desc),
    FeatureGuide(R.string.guide_quran_title, R.string.guide_quran_desc),
    FeatureGuide(R.string.guide_qibla_title, R.string.guide_qibla_desc),
    FeatureGuide(R.string.guide_tasbih_title, R.string.guide_tasbih_desc),
    FeatureGuide(R.string.guide_notifications_title, R.string.guide_notifications_desc),
    FeatureGuide(R.string.guide_hadith_title, R.string.guide_hadith_desc),
    FeatureGuide(R.string.guide_fasting_title, R.string.guide_fasting_desc),
    FeatureGuide(R.string.guide_zakat_title, R.string.guide_zakat_desc),
    FeatureGuide(R.string.guide_calendar_title, R.string.guide_calendar_desc)
)

private data class TroubleshootingItem(
    val titleResId: Int,
    val symptomResId: Int,
    val solutionResId: Int
)

private val troubleshootingItems = listOf(
    TroubleshootingItem(
        R.string.trouble_times_wrong_title,
        R.string.trouble_times_wrong_symptom,
        R.string.trouble_times_wrong_solution
    ),
    TroubleshootingItem(
        R.string.trouble_no_notifications_title,
        R.string.trouble_no_notifications_symptom,
        R.string.trouble_no_notifications_solution
    ),
    TroubleshootingItem(
        R.string.trouble_adhan_silent_title,
        R.string.trouble_adhan_silent_symptom,
        R.string.trouble_adhan_silent_solution
    ),
    TroubleshootingItem(
        R.string.trouble_location_title,
        R.string.trouble_location_symptom,
        R.string.trouble_location_solution
    ),
    TroubleshootingItem(
        R.string.trouble_qibla_title,
        R.string.trouble_qibla_symptom,
        R.string.trouble_qibla_solution
    ),
    TroubleshootingItem(
        R.string.trouble_background_title,
        R.string.trouble_background_symptom,
        R.string.trouble_background_solution
    ),
    TroubleshootingItem(
        R.string.trouble_hijri_title,
        R.string.trouble_hijri_symptom,
        R.string.trouble_hijri_solution
    ),
    TroubleshootingItem(
        R.string.trouble_quran_audio_title,
        R.string.trouble_quran_audio_symptom,
        R.string.trouble_quran_audio_solution
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.help_support),
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Intro / hero card
            item {
                HelpIntroCard()
                Spacer(modifier = Modifier.height(6.dp))
            }

            // FAQ Section
            item {
                HelpSectionHeader(R.string.faq_title)
            }
            items(faqItems) { faq ->
                NimazAccordion(
                    title = stringResource(faq.questionResId),
                    leadingIcon = Icons.AutoMirrored.Filled.HelpOutline,
                ) {
                    Text(
                        text = stringResource(faq.answerResId),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }

            // Feature Guides Section
            item {
                HelpSectionHeader(R.string.feature_guides, topSpacing = true)
            }
            items(featureGuides) { guide ->
                NimazAccordion(
                    title = stringResource(guide.titleResId),
                    leadingIcon = Icons.AutoMirrored.Filled.MenuBook,
                ) {
                    Text(
                        text = stringResource(guide.descriptionResId),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }

            // Troubleshooting Section
            item {
                HelpSectionHeader(R.string.troubleshooting_title, topSpacing = true)
            }
            items(troubleshootingItems) { item ->
                NimazAccordion(
                    title = stringResource(item.titleResId),
                    leadingIcon = Icons.Default.Build,
                ) {
                    Text(
                        text = stringResource(item.symptomResId),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 20.sp
                    )
                    Text(
                        text = stringResource(item.solutionResId),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                        lineHeight = 20.sp
                    )
                }
            }

            // Contact Section
            item {
                HelpSectionHeader(R.string.contact_us, topSpacing = true)
            }
            item {
                val supportSubject = stringResource(R.string.nimaz_support_request)
                val sendEmailLabel = stringResource(R.string.send_email)
                val supportEmail = stringResource(R.string.support_email)
                ContactCard(
                    email = supportEmail,
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = "mailto:$supportEmail".toUri()
                            putExtra(Intent.EXTRA_SUBJECT, supportSubject)
                        }
                        context.startActivity(Intent.createChooser(intent, sendEmailLabel))
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun HelpSectionHeader(titleResId: Int, topSpacing: Boolean = false) {
    if (topSpacing) {
        Spacer(modifier = Modifier.height(8.dp))
    }
    NimazSectionHeader(title = stringResource(titleResId))
}

@Composable
private fun HelpIntroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SupportAgent,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.help_intro_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.help_intro_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun ContactCard(email: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContactIcon(Icons.Default.Email)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.email_support),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ContactIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Help intro")
@Composable
private fun HelpIntroCardPreview() {
    NimazTheme {
        HelpIntroCard()
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Contact card")
@Composable
private fun ContactCardPreview() {
    NimazTheme {
        ContactCard(email = "info@arshadshah.com", onClick = {})
    }
}
