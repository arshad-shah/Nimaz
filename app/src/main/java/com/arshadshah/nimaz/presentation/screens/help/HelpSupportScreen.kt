package com.arshadshah.nimaz.presentation.screens.help

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazTheme

private data class FaqItem(val questionResId: Int, val answerResId: Int)

private val faqItems = listOf(
    FaqItem(R.string.faq_prayer_times_q, R.string.faq_prayer_times_a),
    FaqItem(R.string.faq_mosque_difference_q, R.string.faq_mosque_difference_a),
    FaqItem(R.string.faq_set_location_q, R.string.faq_set_location_a),
    FaqItem(R.string.faq_qibla_q, R.string.faq_qibla_a),
    FaqItem(R.string.faq_track_prayers_q, R.string.faq_track_prayers_a),
    FaqItem(R.string.faq_quran_translation_q, R.string.faq_quran_translation_a),
    FaqItem(R.string.faq_notifications_q, R.string.faq_notifications_a),
    FaqItem(R.string.faq_tasbih_q, R.string.faq_tasbih_a)
)

private data class FeatureGuide(val titleResId: Int, val descriptionResId: Int)

private val featureGuides = listOf(
    FeatureGuide(R.string.guide_prayer_times_title, R.string.guide_prayer_times_desc),
    FeatureGuide(R.string.guide_quran_title, R.string.guide_quran_desc),
    FeatureGuide(R.string.guide_qibla_title, R.string.guide_qibla_desc),
    FeatureGuide(R.string.guide_tasbih_title, R.string.guide_tasbih_desc),
    FeatureGuide(R.string.guide_notifications_title, R.string.guide_notifications_desc),
    FeatureGuide(R.string.guide_hadith_title, R.string.guide_hadith_desc),
    FeatureGuide(R.string.guide_fasting_title, R.string.guide_fasting_desc),
    FeatureGuide(R.string.guide_zakat_title, R.string.guide_zakat_desc),
    FeatureGuide(R.string.guide_calendar_title, R.string.guide_calendar_desc)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // FAQ Section
            item {
                NimazSectionHeader(title = stringResource(R.string.faq_title))
            }

            items(faqItems) { faq ->
                FaqCard(faq)
            }

            // Feature Guides Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                NimazSectionHeader(title = stringResource(R.string.feature_guides))
            }

            items(featureGuides) { guide ->
                FeatureGuideCard(guide)
            }

            // Contact Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                NimazSectionHeader(title = stringResource(R.string.contact_us))
            }

            item {
                val supportSubject = stringResource(R.string.nimaz_support_request)
                val sendEmailLabel = stringResource(R.string.send_email)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = "mailto:support@nimaz.app".toUri()
                                putExtra(Intent.EXTRA_SUBJECT, supportSubject)
                            }
                            context.startActivity(Intent.createChooser(intent, sendEmailLabel))
                        },
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
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.email_support),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.support_email),
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

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun FaqCard(faq: FaqItem) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(faq.questionResId),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(visible = expanded) {
                Text(
                    text = stringResource(faq.answerResId),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun FeatureGuideCard(guide: FeatureGuide) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(guide.titleResId),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(guide.descriptionResId),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, name = "FaqCard")
@Composable
private fun FaqCardPreview() {
    NimazTheme {
        FaqCard(
            faq = FaqItem(
                R.string.faq_prayer_times_q,
                R.string.faq_prayer_times_a
            )
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "FeatureGuideCard")
@Composable
private fun FeatureGuideCardPreview() {
    NimazTheme {
        FeatureGuideCard(
            guide = FeatureGuide(
                R.string.guide_prayer_times_title,
                R.string.guide_prayer_times_desc
            )
        )
    }
}
