package com.arshadshah.nimaz.presentation.screens.help

import android.content.Intent
import android.net.Uri
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
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import androidx.core.net.toUri

private data class FaqItem(val question: String, val answer: String)

private val faqItems = listOf(
    FaqItem(
        "How are prayer times calculated?",
        "Prayer times are calculated using your location and the calculation method you select in Prayer Settings. Different methods are used by different regions and Islamic organizations."
    ),
    FaqItem(
        "Why are my prayer times different from my local mosque?",
        "Different mosques may use different calculation methods or manual adjustments. You can change your calculation method in Prayer Settings, or use per-prayer time adjustments."
    ),
    FaqItem(
        "How do I set my location?",
        "Go to More > Prayer Settings > Location. You can search for your city or use GPS to detect your current location automatically."
    ),
    FaqItem(
        "How does the Qibla compass work?",
        "The Qibla compass uses your phone's magnetometer and GPS location to calculate the direction of the Kaaba in Makkah. Make sure to calibrate your compass by moving your phone in a figure-8 pattern."
    ),
    FaqItem(
        "Can I track my prayers?",
        "Yes! Tap on any prayer time on the home screen to mark it as prayed. You can view your prayer history and streaks in Prayer Stats."
    ),
    FaqItem(
        "How do I change the Quran translation?",
        "Go to More > App Settings > Quran Settings (or the Quran tab) and select your preferred translator."
    ),
    FaqItem(
        "Why am I not receiving notifications?",
        "Make sure notifications are enabled in the app (More > Notifications) and in your device's system settings. Also check that battery optimization is disabled for Nimaz."
    ),
    FaqItem(
        "How do I use the Tasbih counter?",
        "Go to the Tasbih tab, select a preset or create your own, and tap the counter to increment. You can set a target count and the app will notify you when reached."
    )
)

private data class FeatureGuide(val title: String, val description: String)

private val featureGuides = listOf(
    FeatureGuide("Prayer Times", "View daily prayer times, mark prayers as completed, and track your consistency."),
    FeatureGuide("Quran Reader", "Read the Quran with translations, transliteration, bookmarks, and audio recitation."),
    FeatureGuide("Qibla Compass", "Find the direction of the Kaaba using your phone's compass and GPS."),
    FeatureGuide("Tasbih Counter", "Digital counter for dhikr with customizable presets and history tracking."),
    FeatureGuide("Notifications", "Get notified before each prayer time with optional adhan sounds."),
    FeatureGuide("Hadith Collection", "Browse authentic hadith collections organized by book and chapter."),
    FeatureGuide("Fasting Tracker", "Track your fasts during Ramadan and throughout the year."),
    FeatureGuide("Zakat Calculator", "Calculate your Zakat obligation based on your assets and savings."),
    FeatureGuide("Islamic Calendar", "View the Hijri calendar alongside the Gregorian calendar with Islamic events.")
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
                title = "Help & Support",
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
                NimazSectionHeader(title = "Frequently Asked Questions")
            }

            items(faqItems) { faq ->
                FaqCard(faq)
            }

            // Feature Guides Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                NimazSectionHeader(title = "Feature Guides")
            }

            items(featureGuides) { guide ->
                FeatureGuideCard(guide)
            }

            // Contact Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                NimazSectionHeader(title = "Contact Us")
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = "mailto:support@nimaz.app".toUri()
                                putExtra(Intent.EXTRA_SUBJECT, "Nimaz Support Request")
                            }
                            context.startActivity(Intent.createChooser(intent, "Send Email"))
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
                                text = "Email Support",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "support@nimaz.app",
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
                    text = faq.question,
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
                    text = faq.answer,
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
                text = guide.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = guide.description,
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
                "How are prayer times calculated?",
                "Prayer times are calculated using your location and the calculation method you select in Prayer Settings."
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
                "Prayer Times",
                "View daily prayer times, mark prayers as completed, and track your consistency."
            )
        )
    }
}
